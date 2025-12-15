package monitoring.service;

import monitoring.model.ConfigMetrics;
import monitoring.model.HistoricalMetrics;
import monitoring.repository.MetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Backfills historical data from existing best_*.txt files.
 *
 * <h2>Purpose</h2>
 * Allows viewing historical progression even if the backend was offline
 * when milestones were reached. Each best_N.txt file represents a unique
 * milestone (a config reaches depth N only once).
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Scans directory tree for best_*.txt files</li>
 *   <li>Parses save files using SaveFileParser</li>
 *   <li>Checks for existing milestones in database (no duplicates)</li>
 *   <li>Inserts new historical metrics with "milestone" status</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * HistoricalDataBackfiller backfiller = new HistoricalDataBackfiller(
 *     savesDirectory,
 *     parser,
 *     repository
 * );
 *
 * BackfillResult result = backfiller.backfill();
 * logger.info("Backfilled {} milestones from {} files",
 *     result.metricsInserted, result.filesProcessed);
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class HistoricalDataBackfiller {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalDataBackfiller.class);

    private final String savesDirectory;
    private final SaveFileParser parser;
    private final MetricsRepository repository;

    /**
     * Creates a historical data backfiller.
     *
     * @param savesDirectory Root directory to scan for best_*.txt files
     * @param parser Parser for save files
     * @param repository Repository for storing historical metrics
     */
    public HistoricalDataBackfiller(String savesDirectory,
                                    SaveFileParser parser,
                                    MetricsRepository repository) {
        this.savesDirectory = savesDirectory;
        this.parser = parser;
        this.repository = repository;
    }

    /**
     * Performs backfill of historical data from best_*.txt files.
     *
     * @return BackfillResult with statistics about processing
     */
    public BackfillResult backfill() {
        logger.info("🔄 Backfilling historical data from best_*.txt files...");

        File savesDir = new File(savesDirectory);
        if (!savesDir.exists() || !savesDir.isDirectory()) {
            logger.warn("Saves directory does not exist: {}", savesDirectory);
            return new BackfillResult(0, 0);
        }

        AtomicInteger filesProcessed = new AtomicInteger(0);
        AtomicInteger metricsInserted = new AtomicInteger(0);

        try {
            // Walk the directory tree and find all best_*.txt files
            Files.walk(Paths.get(savesDirectory))
                    .filter(Files::isRegularFile)
                    .filter(this::isBestFile)
                    .forEach(path -> {
                        try {
                            processBackfillFile(path, metricsInserted, filesProcessed);
                        } catch (RuntimeException e) {
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

        return new BackfillResult(filesProcessed.get(), metricsInserted.get());
    }

    /**
     * Checks if a path represents a best file (best_*.txt).
     *
     * @param path Path to check
     * @return true if best file
     */
    private boolean isBestFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.startsWith("best_") && fileName.endsWith(".txt");
    }

    /**
     * Processes a single best file for backfilling.
     *
     * @param path Path to best file
     * @param metricsInserted Counter for inserted metrics
     * @param filesProcessed Counter for processed files
     */
    private void processBackfillFile(Path path,
                                     AtomicInteger metricsInserted,
                                     AtomicInteger filesProcessed) {
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

                logger.debug("Backfilled milestone: {} depth={}",
                    metrics.getConfigName(), metrics.getDepth());
            }

            filesProcessed.incrementAndGet();
        }
    }

    /**
     * Result of a backfill operation.
     */
    public static class BackfillResult {
        public final int filesProcessed;
        public final int metricsInserted;

        public BackfillResult(int filesProcessed, int metricsInserted) {
            this.filesProcessed = filesProcessed;
            this.metricsInserted = metricsInserted;
        }
    }
}
