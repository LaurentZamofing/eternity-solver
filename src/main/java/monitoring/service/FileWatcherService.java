package monitoring.service;

import monitoring.model.ConfigMetrics;
import monitoring.model.HistoricalMetrics;
import monitoring.repository.MetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service that watches the saves/ directory for file changes.
 * Uses Java WatchService API for efficient file system monitoring.
 *
 * Features:
 * - Monitors all subdirectories recursively
 * - Detects CREATE and MODIFY events
 * - Parses changed files and extracts metrics
 * - Publishes metrics via WebSocket for real-time updates
 * - Stores metrics in H2 database for historical tracking
 */
@Service
public class FileWatcherService {

    private static final Logger logger = LoggerFactory.getLogger(FileWatcherService.class);

    @Autowired
    private SaveFileParser parser;

    @Autowired
    private MetricsRepository repository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${monitoring.saves-directory:./saves}")
    private String savesDirectory;

    @Value("${monitoring.auto-save-history:true}")
    private boolean autoSaveHistory;

    // Watch service and thread management
    private WatchService watchService;
    private ExecutorService executorService;
    private volatile boolean running = false;

    // Cache of latest metrics for each config (in-memory state)
    private final Map<String, ConfigMetrics> metricsCache = new ConcurrentHashMap<>();

    // Watch keys for each directory
    private final Map<WatchKey, Path> watchKeys = new ConcurrentHashMap<>();

    /**
     * Initialize and start the file watcher after Spring context is ready.
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing FileWatcherService...");
        logger.info("Watching directory: {}", savesDirectory);

        try {
            // Create watch service
            watchService = FileSystems.getDefault().newWatchService();

            // Start executor for background processing
            executorService = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "FileWatcher-Thread");
                t.setDaemon(true);
                return t;
            });

            // Initial scan of all existing files
            performInitialScan();

            // Start watching for changes
            startWatching();

            logger.info("✅ FileWatcherService started successfully");

        } catch (IOException e) {
            logger.error("Failed to initialize FileWatcherService", e);
        }
    }

    /**
     * Perform initial scan of all existing save files.
     */
    private void performInitialScan() {
        logger.info("Performing initial scan of saves directory...");

        File savesDir = new File(savesDirectory);
        if (!savesDir.exists() || !savesDir.isDirectory()) {
            logger.warn("Saves directory does not exist: {}", savesDirectory);
            return;
        }

        AtomicInteger filesScanned = new AtomicInteger(0);

        try {
            // Walk the directory tree - only scan current_*.txt files for performance
            // (best_*.txt files will be scanned later by calculateBestDepthEver)
            Files.walk(Paths.get(savesDirectory))
                    .filter(Files::isRegularFile)
                    .filter(parser::isCurrentSave)
                    .forEach(path -> {
                        try {
                            processFileChangeQuick(path);
                            filesScanned.incrementAndGet();
                        } catch (Exception e) {
                            // File might have been deleted during scan (race condition with solver)
                            logger.debug("Skipping file during scan (might have been deleted): {}", path);
                        }
                    });

            logger.info("Initial scan complete. Scanned {} files, found {} configs in cache",
                    filesScanned.get(), metricsCache.size());

        } catch (IOException | java.io.UncheckedIOException e) {
            logger.warn("Error during initial scan (files may have been deleted during scan): {}",
                    e.getMessage());
            logger.info("Partial scan complete. Found {} configs in cache", metricsCache.size());
        }
    }

    /**
     * Register a directory and all subdirectories for watching.
     */
    private void registerDirectory(Path dir) throws IOException {
        if (Files.isDirectory(dir)) {
            WatchKey key = dir.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY
            );
            watchKeys.put(key, dir);
            logger.debug("Registered watch for: {}", dir);

            // Register subdirectories recursively
            Files.list(dir)
                    .filter(Files::isDirectory)
                    .forEach(subDir -> {
                        try {
                            registerDirectory(subDir);
                        } catch (IOException e) {
                            logger.error("Failed to register subdirectory: {}", subDir, e);
                        }
                    });
        }
    }

    /**
     * Start the file watching loop.
     */
    private void startWatching() throws IOException {
        // Register root saves directory and all subdirectories
        Path savesPath = Paths.get(savesDirectory);
        if (Files.exists(savesPath)) {
            registerDirectory(savesPath);
        }

        running = true;

        // Submit watch task to executor
        executorService.submit(() -> {
            logger.info("File watcher thread started");

            while (running) {
                try {
                    // Wait for events (blocks until events arrive or 1 second timeout)
                    WatchKey key = watchService.poll(1, TimeUnit.SECONDS);

                    if (key == null) {
                        continue;
                    }

                    Path dir = watchKeys.get(key);

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }

                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();
                        Path fullPath = dir.resolve(filename);

                        // Process file change
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE ||
                                kind == StandardWatchEventKinds.ENTRY_MODIFY) {

                            if (parser.isSaveFile(fullPath)) {
                                logger.debug("File changed: {} ({})", fullPath, kind.name());
                                processFileChange(fullPath);
                            }

                            // If a new directory was created, register it
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE &&
                                    Files.isDirectory(fullPath)) {
                                registerDirectory(fullPath);
                            }
                        }
                    }

                    // Reset the key to receive further events
                    boolean valid = key.reset();
                    if (!valid) {
                        watchKeys.remove(key);
                    }

                } catch (InterruptedException e) {
                    logger.info("File watcher interrupted");
                    break;
                } catch (Exception e) {
                    logger.error("Error in file watcher loop", e);
                }
            }

            logger.info("File watcher thread stopped");
        });
    }

    /**
     * Process a file change event.
     * Parses the file, updates cache, publishes to WebSocket, and saves to DB.
     */
    private void processFileChange(Path path) {
        try {
            // Small delay to ensure file is fully written
            Thread.sleep(100);

            // Parse the file
            ConfigMetrics metrics = parser.parseSaveFile(path);

            if (metrics != null) {
                // Calculate and set best depth ever from best_*.txt files
                int bestDepth = calculateBestDepthEver(metrics.getConfigName());
                metrics.setBestDepthEver(bestDepth);

                // Update cache
                metricsCache.put(metrics.getConfigName(), metrics);

                // Publish to WebSocket
                publishMetricsUpdate(metrics);

                // Save to database (if enabled)
                if (autoSaveHistory) {
                    saveToDatabase(metrics);
                }

                logger.debug("Processed metrics: {}", metrics);
            }

        } catch (Exception e) {
            logger.error("Error processing file: {}", path, e);
        }
    }

    /**
     * Process a file change during initial scan (no sleep for performance).
     * Parses the file, updates cache, publishes to WebSocket, and saves to DB.
     */
    private void processFileChangeQuick(Path path) {
        // Parse the file (no sleep - file is already on disk during initial scan)
        ConfigMetrics metrics = parser.parseSaveFile(path);

        if (metrics != null) {
            // Calculate and set best depth ever from best_*.txt files
            int bestDepth = calculateBestDepthEver(metrics.getConfigName());
            metrics.setBestDepthEver(bestDepth);

            // Update cache
            metricsCache.put(metrics.getConfigName(), metrics);

            // Don't publish to WebSocket during initial scan (no clients connected yet)
            // Don't save to DB during initial scan (will be saved on first update)

            logger.debug("Processed metrics: {}", metrics);
        }
    }

    /**
     * Publish metrics update via WebSocket.
     */
    private void publishMetricsUpdate(ConfigMetrics metrics) {
        try {
            // Publish to /topic/metrics for all subscribers
            messagingTemplate.convertAndSend("/topic/metrics", metrics);
            logger.debug("Published WebSocket update: {}", metrics.getConfigName());
        } catch (Exception e) {
            logger.error("Failed to publish WebSocket message", e);
        }
    }

    /**
     * Save metrics to H2 database.
     */
    private void saveToDatabase(ConfigMetrics metrics) {
        try {
            HistoricalMetrics historical = HistoricalMetrics.fromConfigMetrics(metrics);
            repository.save(historical);
            logger.trace("Saved to database: {}", metrics.getConfigName());
        } catch (Exception e) {
            logger.error("Failed to save metrics to database", e);
        }
    }

    /**
     * Get current metrics for a specific configuration.
     */
    public ConfigMetrics getMetrics(String configName) {
        return metricsCache.get(configName);
    }

    /**
     * Calculate the best depth ever reached for a configuration by scanning all best_*.txt files.
     */
    private int calculateBestDepthEver(String configName) {
        int maxDepth = 0;
        try {
            // Construct path to config directory: saves/eternity2/configName/
            Path configDir = Paths.get(savesDirectory, "eternity2", configName);

            if (Files.exists(configDir) && Files.isDirectory(configDir)) {
                // Find all best_*.txt files and extract depths
                maxDepth = Files.list(configDir)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.startsWith("best_") && fileName.endsWith(".txt");
                    })
                    .mapToInt(path -> {
                        String fileName = path.getFileName().toString();
                        try {
                            // Extract depth from "best_123.txt" -> 123
                            String depthStr = fileName.replace("best_", "").replace(".txt", "");
                            return Integer.parseInt(depthStr);
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    })
                    .max()
                    .orElse(0);
            }
        } catch (IOException e) {
            logger.debug("Could not calculate best depth for {}: {}", configName, e.getMessage());
        }
        return maxDepth;
    }

    /**
     * Get all current metrics (latest for each config).
     */
    public Map<String, ConfigMetrics> getAllMetrics() {
        return new HashMap<>(metricsCache);
    }

    /**
     * Periodic health check and cleanup (runs every 5 minutes).
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void healthCheck() {
        logger.debug("Health check: {} configs in cache", metricsCache.size());
    }

    /**
     * Shutdown hook to clean up resources.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down FileWatcherService...");
        running = false;

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.error("Error closing watch service", e);
            }
        }

        logger.info("✅ FileWatcherService shut down");
    }
}
