package monitoring.service;

import monitoring.model.HistoricalMetrics;
import monitoring.repository.MetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * Processes stats_history.jsonl files and inserts entries into database.
 *
 * <h2>Purpose</h2>
 * Stats files contain detailed progress logs in JSONL format (JSON Lines).
 * Each line represents a milestone or progress update with:
 * <ul>
 *   <li>Timestamp (ts)</li>
 *   <li>Depth reached</li>
 *   <li>Progress percentage</li>
 *   <li>Compute time (ms)</li>
 *   <li>Performance (piecesPerSec)</li>
 * </ul>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Extract config name from file path</li>
 *   <li>Parse JSONL format (Gson)</li>
 *   <li>Check for existing entries (no duplicates)</li>
 *   <li>Insert new historical metrics with "stats_log" status</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * StatsFileProcessor processor = new StatsFileProcessor(repository);
 *
 * // Process stats file change
 * int inserted = processor.processStatsFile(
 *     Paths.get("saves/eternity2/config1/stats_history.jsonl")
 * );
 * logger.info("Inserted {} new stats entries", inserted);
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class StatsFileProcessor {

    private static final Logger logger = LoggerFactory.getLogger(StatsFileProcessor.class);

    private final MetricsRepository repository;
    private final com.google.gson.Gson gson;

    /**
     * Creates a stats file processor.
     *
     * @param repository Repository for storing historical metrics
     */
    public StatsFileProcessor(MetricsRepository repository) {
        this.repository = repository;
        this.gson = new com.google.gson.Gson();
    }

    /**
     * Processes a stats file and inserts new entries into database.
     *
     * @param path Path to stats_history.jsonl file
     * @return Number of entries inserted
     */
    public int processStatsFile(Path path) {
        try {
            // Extract config name from path: saves/eternity2/{configName}/stats_history.jsonl
            String configName = extractConfigName(path);

            // Read and parse the file (only new lines since last read)
            return parseAndStoreStatsFile(path, configName);

        } catch (Exception e) {
            logger.error("Error processing stats file: {}", path, e);
            return 0;
        }
    }

    /**
     * Extracts config name from file path.
     *
     * @param path Path to stats file
     * @return Config name (parent directory name)
     */
    private String extractConfigName(Path path) {
        return path.getParent().getFileName().toString();
    }

    /**
     * Parse stats file and insert new entries into database.
     * Uses file position tracking to only read new lines.
     *
     * @param path Path to stats file
     * @param configName Configuration name
     * @return Number of entries inserted
     */
    private int parseAndStoreStatsFile(Path path, String configName) {
        try {
            // For simplicity, read all lines and check if they exist in DB
            // In production, would track last read position
            List<String> lines = Files.readAllLines(path);

            int inserted = 0;
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                try {
                    // Parse JSON line and insert if new
                    if (parseAndInsertStatsLine(line, configName)) {
                        inserted++;
                    }

                } catch (Exception e) {
                    logger.trace("Skipping invalid stats line: {}", line);
                }
            }

            if (inserted > 0) {
                logger.debug("Inserted {} stats entries from {} for {}",
                    inserted, path.getFileName(), configName);
            }

            return inserted;

        } catch (IOException e) {
            logger.error("Failed to read stats file: {}", path, e);
            return 0;
        }
    }

    /**
     * Parses a single JSONL line and inserts it if not already in database.
     *
     * @param line JSONL line to parse
     * @param configName Configuration name
     * @return true if inserted, false if already exists
     */
    private boolean parseAndInsertStatsLine(String line, String configName) {
        // Parse JSON line
        @SuppressWarnings("unchecked")
        Map<String, Object> data = gson.fromJson(line, Map.class);

        // Extract required fields
        long timestamp = ((Number) data.get("ts")).longValue();
        int depth = ((Number) data.get("depth")).intValue();
        double progress = ((Number) data.get("progress")).doubleValue();
        long computeMs = ((Number) data.get("computeMs")).longValue();

        // Convert timestamp to LocalDateTime
        Instant instant = Instant.ofEpochMilli(timestamp);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

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

            // Add performance metric if available
            if (data.containsKey("piecesPerSec")) {
                metrics.setPiecesPerSecond(((Number) data.get("piecesPerSec")).doubleValue());
            }

            repository.save(metrics);
            return true;
        }

        return false;
    }

    /**
     * Checks if a file name is a stats file.
     *
     * @param fileName File name to check
     * @return true if stats file (stats_history*.jsonl)
     */
    public static boolean isStatsFile(String fileName) {
        return fileName.equals("stats_history.jsonl") ||
               fileName.matches("stats_history_\\d+\\.jsonl");
    }
}
