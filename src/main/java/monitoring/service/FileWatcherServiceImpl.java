package monitoring.service;

import monitoring.MonitoringConstants;
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
import java.util.Map;
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

    @Autowired
    private MetricsCacheManager cacheManager;

    @Autowired
    private BestDepthCalculator bestDepthCalculator;

    @Value("${monitoring.saves-directory:./saves}")
    private String savesDirectory;

    @Value("${monitoring.auto-save-history:true}")
    private boolean autoSaveHistory;

    // Extracted components (Observer Pattern + SRP)
    private FileSystemWatcher fileSystemWatcher;
    private HistoricalDataBackfiller historicalDataBackfiller;
    private StatsFileProcessor statsFileProcessor;

    /**
     * Initialize and start the file watcher after Spring context is ready.
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing FileWatcherService...");
        logger.info("Watching directory: {}", savesDirectory);

        try {
            // Create extracted components
            fileSystemWatcher = new FileSystemWatcher(savesDirectory);
            historicalDataBackfiller = new HistoricalDataBackfiller(savesDirectory, parser, repository);
            statsFileProcessor = new StatsFileProcessor(repository);

            // Set up observers for file changes (Observer Pattern)
            fileSystemWatcher.setSaveFileObserver(this::handleSaveFileChange);
            fileSystemWatcher.setStatsFileObserver(this::handleStatsFileChange);

            // Initial scan of all existing files
            performInitialScan();

            // Backfill historical data from best_*.txt files
            if (autoSaveHistory) {
                historicalDataBackfiller.backfill();
            }

            // Start watching for changes
            fileSystemWatcher.start();

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
                        } catch (RuntimeException e) {
                            // File might have been deleted during scan (race condition with solver)
                            logger.debug("Skipping file during scan (might have been deleted): {}", path);
                        }
                    });

            logger.info("Initial scan complete. Scanned {} files, found {} configs in cache",
                    filesScanned.get(), cacheManager.size());

        } catch (IOException | java.io.UncheckedIOException e) {
            logger.warn("Error during initial scan (files may have been deleted during scan): {}",
                    e.getMessage());
            logger.info("Partial scan complete. Found {} configs in cache", cacheManager.size());
        }
    }

    /**
     * Observer callback for save file changes (Observer Pattern).
     * Called by FileSystemWatcher when a save file is created or modified.
     *
     * @param path Path to the changed save file
     */
    private void handleSaveFileChange(Path path) {
        try {
            // Small delay to ensure file is fully written
            Thread.sleep(MonitoringConstants.FileParsing.FILE_WRITE_DELAY_MS);

            // Parse the file
            ConfigMetrics metrics = parser.parseSaveFile(path);

            if (metrics != null) {
                // Calculate and set best depth ever from best_*.txt files
                int bestDepth = bestDepthCalculator.calculateBestDepthEver(metrics.getConfigName());
                metrics.setBestDepthEver(bestDepth);

                // Update cache
                cacheManager.put(metrics.getConfigName(), metrics);

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
            int bestDepth = bestDepthCalculator.calculateBestDepthEver(metrics.getConfigName());
            metrics.setBestDepthEver(bestDepth);

            // Update cache
            cacheManager.put(metrics.getConfigName(), metrics);

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
        return cacheManager.get(configName);
    }

    /**
     * Get all current metrics (latest for each config).
     */
    public Map<String, ConfigMetrics> getAllMetrics() {
        return cacheManager.getAll();
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
        int oldSize = cacheManager.clear();

        // Perform fresh scan
        performInitialScan();

        int newSize = cacheManager.size();
        logger.info("✅ Cache refreshed: {} configs found (was {})", newSize, oldSize);

        return newSize;
    }

    /**
     * Observer callback for stats file changes (Observer Pattern).
     * Called by FileSystemWatcher when a stats file is created or modified.
     *
     * @param path Path to the changed stats file
     */
    private void handleStatsFileChange(Path path) {
        statsFileProcessor.processStatsFile(path);
    }

    /**
     * Periodic health check and cleanup (runs every 5 minutes).
     */
    @Scheduled(fixedRate = MonitoringConstants.Time.HEALTH_CHECK_INTERVAL_MS)
    public void healthCheck() {
        logger.debug("Health check: {} configs in cache", cacheManager.size());
    }

    /**
     * Shutdown hook to clean up resources.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down FileWatcherService...");

        if (fileSystemWatcher != null) {
            fileSystemWatcher.stop(MonitoringConstants.Time.SHUTDOWN_TIMEOUT_SECONDS);
        }

        logger.info("✅ FileWatcherService shut down");
    }
}
