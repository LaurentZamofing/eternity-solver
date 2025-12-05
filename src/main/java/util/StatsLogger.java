package util;

import solver.StatisticsManager;
import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Append-only logger for solver statistics in JSON Lines format.
 *
 * Writes periodic snapshots of solver state to enable:
 * - Complete historical reconstruction even if monitoring backend is offline
 * - High-frequency data capture (every 10 seconds vs 60s for full saves)
 * - Internal solver statistics (backtracks, recursive calls, etc.)
 *
 * Format: One JSON object per line (JSON Lines / NDJSON)
 * Example: {"ts":1764767919,"depth":48,"progress":0.0,"computeMs":300026,"backtracks":15234}
 *
 * Features:
 * - Automatic directory creation
 * - File rotation at 10MB to prevent unbounded growth
 * - Buffered writes for performance
 * - Thread-safe (synchronized writes)
 */
public class StatsLogger {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final String STATS_FILENAME = "stats_history.jsonl";

    private final String puzzleName;
    private final Gson gson;
    private final Path baseDir;
    private BufferedWriter writer;
    private Path currentLogPath;
    private long bytesWritten;

    /**
     * Creates a stats logger for the given puzzle using default base directory.
     *
     * @param puzzleName Name of the puzzle (e.g., "eternity2_p11_ascending")
     */
    public StatsLogger(String puzzleName) {
        this(puzzleName, null);
    }

    /**
     * Creates a stats logger for the given puzzle with custom base directory.
     *
     * @param puzzleName Name of the puzzle (e.g., "eternity2_p11_ascending")
     * @param baseDir Base directory for saves (null = use current working directory)
     */
    public StatsLogger(String puzzleName, Path baseDir) {
        this.puzzleName = puzzleName;
        this.gson = new Gson();
        this.baseDir = baseDir;
        this.bytesWritten = 0;
        initialize();
    }

    /**
     * Initializes the log file and writer.
     */
    private void initialize() {
        try {
            // Construct path: {baseDir}/saves/eternity2/{puzzleName}/stats_history.jsonl
            Path root = (baseDir != null) ? baseDir : Paths.get(".");
            Path saveDir = root.resolve("saves/eternity2").resolve(puzzleName);

            // Create directory if it doesn't exist
            if (!Files.exists(saveDir)) {
                Files.createDirectories(saveDir);
            }

            currentLogPath = saveDir.resolve(STATS_FILENAME);

            // Check if file exists and get current size
            if (Files.exists(currentLogPath)) {
                bytesWritten = Files.size(currentLogPath);

                // If file is too large, rotate it
                if (bytesWritten >= MAX_FILE_SIZE_BYTES) {
                    rotateLogFile();
                }
            }

            // Open in append mode
            writer = new BufferedWriter(new FileWriter(currentLogPath.toFile(), true));

        } catch (IOException e) {
            System.err.println("Failed to initialize StatsLogger for " + puzzleName + ": " + e.getMessage());
            writer = null;
        }
    }

    /**
     * Rotates the log file by renaming it with a timestamp.
     */
    private void rotateLogFile() throws IOException {
        if (Files.exists(currentLogPath)) {
            // Rename to stats_history_{timestamp}.jsonl
            String timestamp = String.valueOf(System.currentTimeMillis());
            Path rotatedPath = currentLogPath.getParent().resolve("stats_history_" + timestamp + ".jsonl");
            Files.move(currentLogPath, rotatedPath);
            bytesWritten = 0;
        }
    }

    /**
     * Appends a statistics snapshot to the log file.
     *
     * @param depth Current search depth (pieces placed)
     * @param progressPercentage Search space progress estimation
     * @param totalComputeTimeMs Total compute time in milliseconds
     * @param stats Statistics manager with internal metrics
     */
    public synchronized void appendStats(int depth, double progressPercentage,
                                        long totalComputeTimeMs, StatisticsManager stats) {
        if (writer == null) {
            return; // Logger not initialized (likely IO error)
        }

        try {
            // Build compact JSON object
            Map<String, Object> data = new HashMap<>();
            data.put("ts", System.currentTimeMillis()); // Unix epoch milliseconds
            data.put("depth", depth);
            data.put("progress", Math.round(progressPercentage * 100.0) / 100.0); // 2 decimals
            data.put("computeMs", totalComputeTimeMs);

            // Add internal statistics if available
            if (stats != null) {
                data.put("backtracks", stats.backtracks);
                data.put("calls", stats.recursiveCalls);
                data.put("placements", stats.placements);
                data.put("singletons", stats.singletonsPlaced);
                data.put("deadEnds", stats.deadEndsDetected);
                data.put("fitChecks", stats.fitChecks);
            }

            // Calculate pieces per second
            if (totalComputeTimeMs > 0 && depth > 0) {
                double piecesPerSec = (depth * TimeConstants.MILLIS_PER_SECOND) / totalComputeTimeMs;
                data.put("piecesPerSec", Math.round(piecesPerSec * 100.0) / 100.0);
            } else {
                data.put("piecesPerSec", 0.0);
            }

            // Write as single line JSON
            String jsonLine = gson.toJson(data);
            writer.write(jsonLine);
            writer.newLine();
            writer.flush(); // Ensure data is written immediately

            // Update byte counter
            bytesWritten += jsonLine.length() + 1; // +1 for newline

            // Check if rotation is needed
            if (bytesWritten >= MAX_FILE_SIZE_BYTES) {
                writer.close();
                rotateLogFile();
                writer = new BufferedWriter(new FileWriter(currentLogPath.toFile(), true));
            }

        } catch (IOException e) {
            System.err.println("Failed to write stats log: " + e.getMessage());
        }
    }

    /**
     * Closes the log file writer.
     * Should be called when solver terminates.
     */
    public synchronized void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                System.err.println("Failed to close stats logger: " + e.getMessage());
            }
            writer = null;
        }
    }

    /**
     * Gets the current log file path.
     */
    public Path getLogPath() {
        return currentLogPath;
    }
}
