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
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of file watcher service that monitors the saves/ directory.
 * Uses Java WatchService API for efficient file system monitoring.
 *
 * Features:
 * - Monitors all subdirectories recursively
 * - Detects CREATE and MODIFY events
 * - Parses changed files and extracts metrics
 * - Publishes metrics via WebSocket for real-time updates
 * - Stores metrics in H2 database for historical tracking
 *
 * @see IFileWatcherService for the interface
 */
@Service
public class FileWatcherServiceImpl implements IFileWatcherService {

    private static final Logger logger = LoggerFactory.getLogger(FileWatcherServiceImpl.class);

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

        // Backfill historical data from best_*.txt files
        if (autoSaveHistory) {
            backfillHistoricalData();
        }
    }

    /**
     * Backfill historical data from existing best_*.txt files.
     * This allows viewing historical progression even if the backend was offline.
     */
    private void backfillHistoricalData() {
        logger.info("🔄 Backfilling historical data from best_*.txt files...");

        File savesDir = new File(savesDirectory);
        if (!savesDir.exists() || !savesDir.isDirectory()) {
            logger.warn("Saves directory does not exist: {}", savesDirectory);
            return;
        }

        AtomicInteger filesProcessed = new AtomicInteger(0);
        AtomicInteger metricsInserted = new AtomicInteger(0);

        try {
            // Walk the directory tree and find all best_*.txt files
            Files.walk(Paths.get(savesDirectory))
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.startsWith("best_") && fileName.endsWith(".txt");
                    })
                    .forEach(path -> {
                        try {
                            // Parse the best file
                            ConfigMetrics metrics = parser.parseSaveFile(path);

                            if (metrics != null) {
                                // Check if this milestone already exists in database
                                // We check by configName + depth since each best_N.txt represents
                                // a unique milestone (a config reaches depth N only once)
                                boolean exists = repository.existsByConfigNameAndDepth(
                                    metrics.getConfigName(),
                                    metrics.getDepth()
                                );

                                if (!exists) {
                                    // Save to database with original timestamp
                                    HistoricalMetrics historical = HistoricalMetrics.fromConfigMetrics(metrics);
                                    historical.setStatus("milestone"); // Mark as milestone from backfill
                                    repository.save(historical);
                                    metricsInserted.incrementAndGet();
                                }

                                filesProcessed.incrementAndGet();
                            }
                        } catch (Exception e) {
                            logger.debug("Skipping file during backfill (might be invalid or deleted): {}", path);
                        }
                    });

            logger.info("✅ Backfill complete. Processed {} best files, inserted {} new historical metrics",
                    filesProcessed.get(), metricsInserted.get());

        } catch (IOException | java.io.UncheckedIOException e) {
            logger.warn("Error during backfill (files may have been deleted during scan): {}",
                    e.getMessage());
            logger.info("Partial backfill complete. Processed {} files, inserted {} metrics",
                    filesProcessed.get(), metricsInserted.get());
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
                            } else if (isStatsFile(fullPath)) {
                                logger.debug("Stats file changed: {} ({})", fullPath, kind.name());
                                processStatsFileChange(fullPath);
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
     * Clear cache and rescan all files.
     * Useful when files have been deleted or modified externally.
     *
     * @return number of configs found after refresh
     */
    public int refreshCache() {
        logger.info("🔄 Refreshing cache - clearing and rescanning...");

        // Clear existing cache
        int oldSize = metricsCache.size();
        metricsCache.clear();
        logger.info("Cleared {} configs from cache", oldSize);

        // Perform fresh scan
        performInitialScan();

        int newSize = metricsCache.size();
        logger.info("✅ Cache refreshed: {} configs found (was {})", newSize, oldSize);

        return newSize;
    }

    /**
     * Checks if a file is a stats history file (stats_history.jsonl).
     */
    private boolean isStatsFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.equals("stats_history.jsonl") || fileName.matches("stats_history_\\d+\\.jsonl");
    }

    /**
     * Process stats file change by parsing new lines and inserting into database.
     */
    private void processStatsFileChange(Path path) {
        try {
            // Extract config name from path: saves/eternity2/{configName}/stats_history.jsonl
            String configName = path.getParent().getFileName().toString();

            // Read and parse the file (only new lines since last read)
            parseAndStoreStatsFile(path, configName);

        } catch (Exception e) {
            logger.error("Error processing stats file: {}", path, e);
        }
    }

    /**
     * Parse stats file and insert new entries into database.
     * Uses file position tracking to only read new lines.
     */
    private void parseAndStoreStatsFile(Path path, String configName) {
        try {
            // For simplicity, read all lines and check if they exist in DB
            // In production, would track last read position
            List<String> lines = Files.readAllLines(path);

            int inserted = 0;
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                try {
                    // Parse JSON line
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = gson.fromJson(line, Map.class);

                    // Extract fields
                    long timestamp = ((Number) data.get("ts")).longValue();
                    int depth = ((Number) data.get("depth")).intValue();
                    double progress = ((Number) data.get("progress")).doubleValue();
                    long computeMs = ((Number) data.get("computeMs")).longValue();

                    // Convert timestamp to LocalDateTime
                    java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
                    java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
                        instant, java.time.ZoneId.systemDefault());

                    // Check if already exists (avoid duplicates)
                    boolean exists = repository.existsByConfigNameAndDepthAndTimestamp(
                        configName, depth, dateTime);

                    if (!exists) {
                        // Create and save historical metrics
                        HistoricalMetrics metrics = new HistoricalMetrics();
                        metrics.setConfigName(configName);
                        metrics.setTimestamp(dateTime);
                        metrics.setDepth(depth);
                        metrics.setProgressPercentage(progress);
                        metrics.setTotalComputeTimeMs(computeMs);
                        metrics.setStatus("stats_log");

                        // Add performance metric
                        if (data.containsKey("piecesPerSec")) {
                            metrics.setPiecesPerSecond(((Number) data.get("piecesPerSec")).doubleValue());
                        }

                        repository.save(metrics);
                        inserted++;
                    }

                } catch (Exception e) {
                    logger.trace("Skipping invalid stats line: {}", line);
                }
            }

            if (inserted > 0) {
                logger.debug("Inserted {} stats entries from {} for {}", inserted, path.getFileName(), configName);
            }

        } catch (IOException e) {
            logger.error("Failed to read stats file: {}", path, e);
        }
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
