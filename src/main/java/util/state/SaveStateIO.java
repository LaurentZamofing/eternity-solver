package util.state;

import util.SolverLogger;

import model.Board;
import util.SaveBoardRenderer;
import util.SaveStateManager.PlacementInfo;
import util.SaveStateManager.SaveState;
import util.TimeConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles file I/O operations for saving and loading puzzle states.
 * Manages reading from and writing to save files in text format.
 *
 * Extracted from SaveStateManager for better separation of concerns.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class SaveStateIO {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    /**
     * Writes a save state to a file.
     *
     * @param filename Output file path
     * @param puzzleName Name of the puzzle
     * @param board Current board state
     * @param depth Number of pieces placed by backtracking
     * @param placementOrder Order of piece placements
     * @param unusedIds List of unused piece IDs
     * @param progressPercentage Progress estimation (-1 if not available)
     * @param totalComputeTimeMs Total compute time in milliseconds
     * @param numFixedPieces Number of fixed pieces
     * @param initialFixedPieces List of initially fixed pieces
     * @throws IOException if file write fails
     */
    public static void writeToFile(String filename, String puzzleName, Board board, int depth,
                                  List<PlacementInfo> placementOrder, List<Integer> unusedIds,
                                  double progressPercentage, long totalComputeTimeMs,
                                  int numFixedPieces, List<PlacementInfo> initialFixedPieces)
            throws IOException {

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Write header
            writeHeader(writer, puzzleName, board, depth, progressPercentage, totalComputeTimeMs);

            // Write visual board representation
            writeBoardVisual(writer, board, depth, numFixedPieces, initialFixedPieces);

            // Write fixed pieces section
            writeFixedPiecesSection(writer, numFixedPieces, initialFixedPieces);

            // Write placement order section
            writePlacementOrderSection(writer, placementOrder, initialFixedPieces);

            // Write data sections for parser
            writeDataSections(writer, board, placementOrder, unusedIds, initialFixedPieces);
        }
    }

    /**
     * Reads a save state from a file.
     *
     * @param file Save file to read from
     * @param puzzleName Name of the puzzle
     * @return SaveState object, or null if read fails
     */
    public static SaveState readFromFile(File file, String puzzleName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int rows = 0, cols = 0;
            long timestamp = 0;
            long totalComputeTimeMs = 0;
            int depth = 0;
            Map<String, PlacementInfo> placements = new HashMap<>();
            List<PlacementInfo> placementOrder = new ArrayList<>();
            List<PlacementInfo> fixedPieces = new ArrayList<>();
            Set<Integer> unusedPieceIds = new HashSet<>();

            boolean readingFixedPieces = false;
            boolean readingPlacementOrder = false;
            boolean readingPlacements = false;
            boolean readingUnused = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                // Parse metadata
                if (line.startsWith("# Timestamp:")) {
                    timestamp = Long.parseLong(line.substring(12).trim());
                } else if (line.startsWith("# Dimensions:")) {
                    String dims = line.substring(13).trim();
                    String[] parts = dims.split("x");
                    rows = Integer.parseInt(parts[0]);
                    cols = Integer.parseInt(parts[1]);
                } else if (line.startsWith("# Depth:")) {
                    String depthStr = line.substring(8).trim();
                    int spaceIdx = depthStr.indexOf(' ');
                    if (spaceIdx > 0) {
                        depthStr = depthStr.substring(0, spaceIdx);
                    }
                    depth = Integer.parseInt(depthStr);
                } else if (line.startsWith("# TotalComputeTime:")) {
                    String timeStr = line.substring(19).trim();
                    int msIdx = timeStr.indexOf(" ms");
                    if (msIdx > 0) {
                        timeStr = timeStr.substring(0, msIdx);
                    }
                    totalComputeTimeMs = Long.parseLong(timeStr);
                } else if (line.startsWith("# Fixed Pieces")) {
                    readingFixedPieces = true;
                    readingPlacementOrder = false;
                    readingPlacements = false;
                    readingUnused = false;
                } else if (line.startsWith("# Placement Order")) {
                    readingFixedPieces = false;
                    readingPlacementOrder = true;
                    readingPlacements = false;
                    readingUnused = false;
                } else if (line.startsWith("# Placements")) {
                    readingFixedPieces = false;
                    readingPlacementOrder = false;
                    readingPlacements = true;
                    readingUnused = false;
                } else if (line.startsWith("# Unused")) {
                    readingFixedPieces = false;
                    readingPlacementOrder = false;
                    readingPlacements = false;
                    readingUnused = true;
                } else if (!line.startsWith("#")) {
                    // Data lines
                    if (readingFixedPieces) {
                        PlacementInfo info = parsePlacementLine(line);
                        if (info != null) {
                            fixedPieces.add(info);
                        }
                    } else if (readingPlacementOrder) {
                        PlacementInfo info = parsePlacementLine(line);
                        if (info != null) {
                            placementOrder.add(info);
                        }
                    } else if (readingPlacements) {
                        PlacementInfo info = parsePlacementLine(line);
                        if (info != null) {
                            placements.put(info.row + "," + info.col, info);
                        }
                    } else if (readingUnused) {
                        String[] parts = line.split("\\s+");
                        for (String part : parts) {
                            if (!part.isEmpty()) {
                                unusedPieceIds.add(Integer.parseInt(part));
                            }
                        }
                    }
                }
            }

            // Combine fixed pieces and placement order for full history
            List<PlacementInfo> fullOrder = new ArrayList<>(fixedPieces);
            fullOrder.addAll(placementOrder);

            // Reconstruct placement order if incomplete (important for backtracking)
            // The parsed placementOrder may be incomplete. We reconstruct a complete order
            // by including ALL pieces from the board. Note: The order won't be the exact
            // chronological order (we use row,col sorting), but it's better than an incomplete
            // order that prevents backtracking.
            if (placements.size() > fullOrder.size()) {
                SolverLogger.info("  ⚠️  Incomplete PlacementOrder: " + fullOrder.size() +
                                 " entries vs " + placements.size() + " pieces on board");
                SolverLogger.info("  ✓  Reconstructing complete order (approximate row,col)...");

                // Create a Set of pieces already in fullOrder
                Set<String> existingKeys = new HashSet<>();
                for (PlacementInfo p : fullOrder) {
                    existingKeys.add(p.row + "," + p.col);
                }

                // Sort all placements by (row, col)
                List<PlacementInfo> sortedAll = new ArrayList<>(placements.values());
                sortedAll.sort((p1, p2) -> {
                    if (p1.row != p2.row) return Integer.compare(p1.row, p2.row);
                    return Integer.compare(p1.col, p2.col);
                });

                // Reconstruct: keep existing order, add missing pieces
                List<PlacementInfo> reconstructed = new ArrayList<>(fullOrder);
                for (PlacementInfo p : sortedAll) {
                    String key = p.row + "," + p.col;
                    if (!existingKeys.contains(key)) {
                        reconstructed.add(p);
                    }
                }

                fullOrder = reconstructed;
                SolverLogger.info("  ✓  Reconstructed order: " + fullOrder.size() + " pieces");
            }

            SolverLogger.info("  📂 Save loaded: " + file.getName() + " (" + depth + " pieces)");
            SolverLogger.info("  📅 Date: " + DATE_FORMAT.format(new Date(timestamp)));
            SolverLogger.info("  📋 Placement order: " + fullOrder.size() + " placements tracked");

            return new SaveState(puzzleName, rows, cols, placements, fullOrder,
                               unusedPieceIds, timestamp, depth, totalComputeTimeMs);

        } catch (IOException | NumberFormatException e) {
            SolverLogger.error("  ⚠️  Error during load: " + e.getMessage());
            return null;
        }
    }

    // ==================== Private Helper Methods ====================

    private static void writeHeader(PrintWriter writer, String puzzleName, Board board,
                                   int depth, double progressPercentage, long totalComputeTimeMs) {
        writer.println("# Eternity II Save");
        writer.println("# Timestamp: " + System.currentTimeMillis());
        writer.println("# Date: " + DATE_FORMAT.format(new Date()));
        writer.println("# Puzzle: " + puzzleName);
        writer.println("# Dimensions: " + board.getRows() + "x" + board.getCols());
        writer.println("# Depth: " + depth + " (pieces placed by backtracking, excluding fixed)");

        if (progressPercentage >= 0.0) {
            writer.println("# Progress: " + String.format("%.8f%%", progressPercentage) +
                         " (estimate based on first 5 depths)");
        }

        // Write total compute time
        long totalSeconds = totalComputeTimeMs / TimeConstants.MILLIS_PER_SECOND;
        long hours = totalSeconds / TimeConstants.SECONDS_PER_HOUR;
        long minutes = (totalSeconds % TimeConstants.SECONDS_PER_HOUR) / TimeConstants.SECONDS_PER_MINUTE;
        long seconds = totalSeconds % 60;
        writer.println("# TotalComputeTime: " + totalComputeTimeMs + " ms (" +
                      String.format("%dh %02dm %02ds", hours, minutes, seconds) + ")");
        writer.println();
    }

    private static void writeBoardVisual(PrintWriter writer, Board board, int depth,
                                        int numFixedPieces, List<PlacementInfo> initialFixedPieces) {
        int fixedCount = (initialFixedPieces != null) ? initialFixedPieces.size() : 0;
        writer.println("# ═══════════════════════════════════════════════════════════");
        writer.println("# VISUAL BOARD DISPLAY (" + (depth + fixedCount) +
                      " pieces: " + fixedCount + " fixed + " + depth + " backtracking)");
        writer.println("# ═══════════════════════════════════════════════════════════");
        writer.println("#");
        SaveBoardRenderer.generateBoardVisual(writer, board);
        writer.println("#");
        writer.println("# ═══════════════════════════════════════════════════════════");
        writer.println();
    }

    private static void writeFixedPiecesSection(PrintWriter writer, int numFixedPieces,
                                               List<PlacementInfo> initialFixedPieces) {
        writer.println("# ═══════════════════════════════════════════════════════════");
        writer.println("# FIXED PIECES (pre-placed at startup)");
        writer.println("# ═══════════════════════════════════════════════════════════");
        writer.println("#");

        if (numFixedPieces > 0) {
            writer.println("# " + numFixedPieces + " fixed pieces (corners + hints - see configuration file)");
        } else {
            writer.println("# (no fixed pieces)");
        }

        writer.println("#");
        writer.println("# ═══════════════════════════════════════════════════════════");
        writer.println();
    }

    private static void writePlacementOrderSection(PrintWriter writer,
                                                  List<PlacementInfo> placementOrder,
                                                  List<PlacementInfo> initialFixedPieces) {
        int fixedCount = (initialFixedPieces != null) ? initialFixedPieces.size() : 0;
        int backtrackingCount = (placementOrder != null) ?
                               Math.max(0, placementOrder.size() - fixedCount) : 0;

        writer.println("# ═══════════════════════════════════════════════════════════");
        writer.println("# PLACEMENT ORDER (backtracking) - " + backtrackingCount + " pieces");
        writer.println("# ═══════════════════════════════════════════════════════════");
        writer.println("#");

        if (placementOrder != null && placementOrder.size() > fixedCount) {
            writer.println("# Step   Position    Piece  Rotation");
            writer.println("# ────── ─────────── ────── ────────");
            for (int i = fixedCount; i < placementOrder.size(); i++) {
                PlacementInfo info = placementOrder.get(i);
                writer.println(String.format("# %4d   (%2d,%2d)     %3d      %d (×90°)",
                    (i - fixedCount + 1), info.row, info.col, info.pieceId, info.rotation));
            }
        } else {
            writer.println("# (no pieces placed by backtracking)");
        }

        writer.println("#");
        writer.println("# ═══════════════════════════════════════════════════════════");
        writer.println();
    }

    private static void writeDataSections(PrintWriter writer, Board board,
                                         List<PlacementInfo> placementOrder,
                                         List<Integer> unusedIds,
                                         List<PlacementInfo> initialFixedPieces) throws IOException {
        // Fixed pieces data
        writer.println("# Fixed Pieces (row,col pieceId rotation) - pre-placed");
        if (initialFixedPieces != null && !initialFixedPieces.isEmpty()) {
            for (PlacementInfo info : initialFixedPieces) {
                writer.println(info.row + "," + info.col + " " + info.pieceId + " " + info.rotation);
            }
        }
        writer.println();

        // Placement order data (backtracking only)
        writer.println("# Placement Order (row,col pieceId rotation) - chronological backtracking order");
        int fixedCount = (initialFixedPieces != null) ? initialFixedPieces.size() : 0;
        if (placementOrder != null && placementOrder.size() > fixedCount) {
            for (int i = fixedCount; i < placementOrder.size(); i++) {
                PlacementInfo info = placementOrder.get(i);
                writer.println(info.row + "," + info.col + " " + info.pieceId + " " + info.rotation);
            }
        }
        writer.println();

        // Current placements
        writer.println("# Placements (row,col pieceId rotation)");
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.isEmpty(r, c)) {
                    model.Placement p = board.getPlacement(r, c);
                    writer.println(r + "," + c + " " + p.getPieceId() + " " + p.getRotation());
                }
            }
        }
        writer.println();

        // Unused pieces
        writer.println("# Unused pieces");
        for (int id : unusedIds) {
            writer.print(id + " ");
        }
        writer.println();
    }

    private static PlacementInfo parsePlacementLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 3) {
            String[] coords = parts[0].split(",");
            if (coords.length == 2) {
                int r = Integer.parseInt(coords[0]);
                int c = Integer.parseInt(coords[1]);
                int pieceId = Integer.parseInt(parts[1]);
                int rotation = Integer.parseInt(parts[2]);
                return new PlacementInfo(r, c, pieceId, rotation);
            }
        }
        return null;
    }
}
