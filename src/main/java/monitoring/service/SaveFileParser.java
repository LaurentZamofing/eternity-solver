package monitoring.service;

import monitoring.MonitoringConstants;
import monitoring.model.ConfigMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import util.TimeConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to parse save files from the saves/ directory.
 * Extracts metrics from the structured comment headers.
 *
 * File format example:
 * # Timestamp: 1763546144171
 * # Date: 2025-11-19_10-55-44
 * # Puzzle: indice2_ascending
 * # Dimensions: 6x12
 * # Depth: 72
 * # Progress: 8.85004471%
 * # TotalComputeTime: 262685 ms (0h 04m 22s)
 */
@Service
public class SaveFileParser {

    private static final Logger logger = LoggerFactory.getLogger(SaveFileParser.class);

    // Regex patterns for parsing header
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("# Timestamp: (\\d+)");
    private static final Pattern DATE_PATTERN = Pattern.compile("# Date: (.+)");
    private static final Pattern PUZZLE_PATTERN = Pattern.compile("# Puzzle: (.+)");
    private static final Pattern DIMENSIONS_PATTERN = Pattern.compile("# Dimensions: (\\d+)x(\\d+)");
    private static final Pattern DEPTH_PATTERN = Pattern.compile("# Depth: (\\d+).*");
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("# Progress: ([\\d.,]+)%.*");
    private static final Pattern COMPUTE_TIME_PATTERN = Pattern.compile("# TotalComputeTime: (\\d+)\\s*ms.*");

    /**
     * Parse a save file and extract metrics.
     *
     * @param file The save file to parse
     * @return ConfigMetrics object, or null if parsing fails
     */
    public ConfigMetrics parseSaveFile(File file) {
        if (!file.exists() || !file.isFile()) {
            logger.warn("File does not exist or is not a file: {}", file.getAbsolutePath());
            return null;
        }

        try {
            return parseFile(file);
        } catch (IOException e) {
            logger.error("Failed to parse file: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * Parse a save file by path.
     */
    public ConfigMetrics parseSaveFile(Path path) {
        return parseSaveFile(path.toFile());
    }

    /**
     * Parse a save file by path string.
     */
    public ConfigMetrics parseSaveFile(String filePath) {
        return parseSaveFile(new File(filePath));
    }

    /**
     * Internal parsing logic.
     */
    private ConfigMetrics parseFile(File file) throws IOException {
        ConfigMetrics metrics = new ConfigMetrics();

        // Determine config name from file path
        String configName = extractConfigName(file);
        metrics.setConfigName(configName);

        // Read and parse header lines, board state, and placement order
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineCount = 0;
            boolean inBoardSection = false;
            boolean inPlacementOrderSection = false;
            java.util.List<String> boardLines = new java.util.ArrayList<>();
            java.util.List<monitoring.model.PlacementInfo> placementOrder = new java.util.ArrayList<>();

            while ((line = reader.readLine()) != null) {
                lineCount++;

                if (line.startsWith("#")) {
                    // Parse header fields
                    parseHeaderLine(line, metrics);

                    // Detect start of board section
                    if (line.contains("VISUAL BOARD DISPLAY")) {
                        inBoardSection = true;
                        inPlacementOrderSection = false;
                    }
                    // Detect end of board section
                    else if (inBoardSection && line.contains("═══") && boardLines.size() > 0) {
                        inBoardSection = false;
                    }
                    // Detect start of placement order section
                    else if (line.contains("Placement Order") && line.contains("chronological")) {
                        inPlacementOrderSection = true;
                        inBoardSection = false;
                    }
                    // Detect end of placement order section (next major section)
                    else if (inPlacementOrderSection && (line.contains("Placements") || line.contains("Unused"))) {
                        inPlacementOrderSection = false;
                    }
                    // Collect board lines (skip empty comment lines and section headers)
                    else if (inBoardSection && line.length() > 2 && !line.contains("═══")) {
                        String boardLine = line.substring(1).trim(); // Remove leading '#'
                        if (!boardLine.isEmpty()) {
                            boardLines.add(boardLine);
                        }
                    }
                } else if (!line.trim().isEmpty()) {
                    // Parse non-comment data lines
                    if (inPlacementOrderSection) {
                        monitoring.model.PlacementInfo info = parsePlacementLine(line);
                        if (info != null) {
                            placementOrder.add(info);
                        }
                    }
                }

                // Stop after reading enough lines (increased limit to capture placement order)
                if (lineCount > MonitoringConstants.FileParsing.MAX_LINES_TO_READ) break;
            }

            // Parse board state if we have data
            if (!boardLines.isEmpty() && metrics.getRows() > 0 && metrics.getCols() > 0) {
                parseBoardState(boardLines, metrics);
            }

            // Set placement order
            if (!placementOrder.isEmpty()) {
                metrics.setPlacementOrder(placementOrder);
                logger.debug("Parsed {} placement order entries", placementOrder.size());
            }
        }

        // Calculate derived metrics
        calculateDerivedMetrics(metrics);

        // Determine status based on file type
        String fileName = file.getName();
        if (fileName.startsWith("current_")) {
            metrics.setStatus("active");
        } else if (fileName.startsWith("best_")) {
            metrics.setStatus("milestone");
        }

        logger.debug("Parsed save file: {} -> {}", file.getName(), metrics);

        return metrics;
    }

    /**
     * Extract configuration name from file path.
     * Example: /saves/eternity2/eternity2_p01_ascending/current_123.txt -> eternity2_p01_ascending
     */
    private String extractConfigName(File file) {
        Path path = file.toPath();
        Path parent = path.getParent();

        if (parent != null) {
            // Return parent directory name (e.g., "eternity2_p01_ascending")
            return parent.getFileName().toString();
        }

        // Fallback: extract from filename
        String fileName = file.getName();
        return fileName.replaceAll("_(current|best)_.*\\.txt", "");
    }

    /**
     * Parse a single header line and update metrics.
     */
    private void parseHeaderLine(String line, ConfigMetrics metrics) {
        Matcher matcher;

        // Timestamp
        matcher = TIMESTAMP_PATTERN.matcher(line);
        if (matcher.matches()) {
            metrics.setTimestamp(Long.parseLong(matcher.group(1)));
            return;
        }

        // Puzzle name
        matcher = PUZZLE_PATTERN.matcher(line);
        if (matcher.matches()) {
            String puzzleName = matcher.group(1).trim();
            // Override config name with puzzle name if available
            metrics.setConfigName(puzzleName);
            return;
        }

        // Dimensions
        matcher = DIMENSIONS_PATTERN.matcher(line);
        if (matcher.matches()) {
            metrics.setRows(Integer.parseInt(matcher.group(1)));
            metrics.setCols(Integer.parseInt(matcher.group(2)));
            metrics.setTotalPieces(metrics.getRows() * metrics.getCols());
            return;
        }

        // Depth
        matcher = DEPTH_PATTERN.matcher(line);
        if (matcher.matches()) {
            metrics.setDepth(Integer.parseInt(matcher.group(1)));
            return;
        }

        // Progress percentage
        matcher = PROGRESS_PATTERN.matcher(line);
        if (matcher.matches()) {
            String progressStr = matcher.group(1).replace(',', '.');
            metrics.setProgressPercentage(Double.parseDouble(progressStr));
            return;
        }

        // Compute time
        matcher = COMPUTE_TIME_PATTERN.matcher(line);
        if (matcher.matches()) {
            metrics.setTotalComputeTimeMs(Long.parseLong(matcher.group(1)));
            return;
        }
    }

    /**
     * Parse board state from visual board display lines.
     * Each cell is either "pieceId_rotation" or "." for empty.
     */
    private void parseBoardState(java.util.List<String> boardLines, ConfigMetrics metrics) {
        int rows = metrics.getRows();
        int cols = metrics.getCols();
        String[][] board = new String[rows][cols];

        // Parse each line (may contain multiple pieces separated by whitespace)
        int currentRow = 0;
        for (String line : boardLines) {
            if (currentRow >= rows) break;

            // Split by whitespace and filter empty strings
            String[] cells = line.trim().split("\\s+");

            for (int col = 0; col < Math.min(cells.length, cols); col++) {
                String cell = cells[col].trim();
                if (cell.equals(".") || cell.isEmpty()) {
                    board[currentRow][col] = null; // Empty cell
                } else {
                    board[currentRow][col] = cell; // "pieceId_rotation"
                }
            }
            currentRow++;
        }

        metrics.setBoardState(board);
        logger.debug("Parsed board state: {} rows x {} cols", currentRow, cols);
    }

    /**
     * Calculate derived metrics (pieces/sec, ETA, etc.)
     */
    private void calculateDerivedMetrics(ConfigMetrics metrics) {
        // Calculate physical progress percentage (actual pieces placed)
        if (metrics.getTotalPieces() > 0) {
            double physicalProgress = (metrics.getDepth() * 100.0) / metrics.getTotalPieces();
            metrics.setPhysicalProgressPercentage(physicalProgress);
        }

        // Calculate pieces per second
        if (metrics.getTotalComputeTimeMs() > 0 && metrics.getDepth() > 0) {
            double seconds = metrics.getTotalComputeTimeMs() / (double)TimeConstants.MILLIS_PER_SECOND;
            double piecesPerSecond = metrics.getDepth() / seconds;
            metrics.setPiecesPerSecond(piecesPerSecond);

            // Estimate time remaining
            if (metrics.getTotalPieces() > 0 && piecesPerSecond > 0) {
                int remainingPieces = metrics.getTotalPieces() - metrics.getDepth();
                if (remainingPieces > 0) {
                    long estimatedMs = (long) ((remainingPieces / piecesPerSecond) * TimeConstants.MILLIS_PER_SECOND);
                    metrics.setEstimatedTimeRemainingMs(estimatedMs);
                }
            }
        }

        // Check if solved
        if (metrics.getTotalPieces() > 0 && metrics.getDepth() >= metrics.getTotalPieces()) {
            metrics.setSolved(true);
        }
    }

    /**
     * Check if a file is a save file (current_*.txt or best_*.txt)
     */
    public boolean isSaveFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }

        String fileName = path.getFileName().toString();
        return fileName.matches("(current|best)_\\d+\\.txt");
    }

    /**
     * Check if a file is a "current" save (active state)
     */
    public boolean isCurrentSave(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.startsWith("current_");
    }

    /**
     * Check if a file is a "best" save (milestone)
     */
    public boolean isBestSave(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.startsWith("best_");
    }

    /**
     * Parse a placement line in format: "row,col pieceId rotation"
     * Example: "0,0 1 0" means piece 1 at (0,0) with rotation 0
     */
    private monitoring.model.PlacementInfo parsePlacementLine(String line) {
        try {
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 3) {
                String[] coords = parts[0].split(",");
                if (coords.length == 2) {
                    int row = Integer.parseInt(coords[0]);
                    int col = Integer.parseInt(coords[1]);
                    int pieceId = Integer.parseInt(parts[1]);
                    int rotation = Integer.parseInt(parts[2]);
                    return new monitoring.model.PlacementInfo(row, col, pieceId, rotation);
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse placement line: {}", line);
        }
        return null;
    }
}
