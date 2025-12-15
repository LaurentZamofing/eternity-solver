import model.Piece;
import util.SolverLogger;
import util.TimeConstants;
import java.io.*;
import java.util.*;

/**
 * Puzzle configuration with metadata
 */
public class PuzzleConfig {

    /**
     * Inner class to represent a fixed piece
     */
    public static class FixedPiece {
        public final int pieceId;
        public final int row;
        public final int col;
        public final int rotation;

        public FixedPiece(int pieceId, int row, int col, int rotation) {
            this.pieceId = pieceId;
            this.row = row;
            this.col = col;
            this.rotation = rotation;
        }
    }

    private String name;
    private String type;
    private int rows;
    private int cols;
    private String difficulty;
    private Integer fixedPiece;  // Maintained for compatibility
    private Integer fixedPieceRow;  // Maintained for compatibility
    private Integer fixedPieceCol;  // Maintained for compatibility
    private Integer fixedPieceRotation;  // Maintained for compatibility
    private List<FixedPiece> fixedPieces = new ArrayList<>();  // New approach
    private Map<Integer, Piece> pieces;
    private boolean verbose = false; // Detailed display (default: disabled)
    private int minDepthToShowRecords = Integer.MAX_VALUE; // Threshold to display records (default: never)
    private String sortOrder = "ascending"; // Sort order for pieces: "ascending" or "descending"
    private boolean prioritizeBorders = false; // Prioritize filling borders (default: disabled)

    public PuzzleConfig(String name, String type, int rows, int cols, String difficulty) {
        this.name = name;
        this.type = type;
        this.rows = rows;
        this.cols = cols;
        this.difficulty = difficulty;
        this.pieces = new HashMap<>();
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public String getDifficulty() { return difficulty; }
    public Integer getFixedPiece() { return fixedPiece; }
    public void setFixedPiece(Integer piece) { this.fixedPiece = piece; }
    public Integer getFixedPieceRow() { return fixedPieceRow; }
    public void setFixedPieceRow(Integer row) { this.fixedPieceRow = row; }
    public Integer getFixedPieceCol() { return fixedPieceCol; }
    public void setFixedPieceCol(Integer col) { this.fixedPieceCol = col; }
    public Integer getFixedPieceRotation() { return fixedPieceRotation; }
    public void setFixedPieceRotation(Integer rotation) { this.fixedPieceRotation = rotation; }
    public Map<Integer, Piece> getPieces() { return pieces; }
    public boolean isVerbose() { return verbose; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }
    public int getMinDepthToShowRecords() { return minDepthToShowRecords; }
    public void setMinDepthToShowRecords(int minDepth) { this.minDepthToShowRecords = minDepth; }
    public List<FixedPiece> getFixedPieces() { return fixedPieces; }
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
    public boolean isPrioritizeBorders() { return prioritizeBorders; }
    public void setPrioritizeBorders(boolean prioritizeBorders) { this.prioritizeBorders = prioritizeBorders; }

    /**
     * Load a puzzle from a file with metadata
     */
    public static PuzzleConfig loadFromFile(String filename) throws IOException {
        PuzzleConfig config = null;
        Map<Integer, Piece> pieces = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            String name = null, type = null, difficulty = null;
            int rows = 0, cols = 0;
            Integer fixedPiece = null;
            Integer fixedPieceRow = null, fixedPieceCol = null, fixedPieceRotation = null;
            List<FixedPiece> fixedPiecesList = new ArrayList<>();
            boolean verbose = false;
            int minDepthToShowRecords = Integer.MAX_VALUE;
            String sortOrder = "ascending";
            boolean prioritizeBorders = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Ignore empty lines
                if (line.isEmpty()) continue;

                // Parse metadata
                if (line.startsWith("#")) {
                    String metadata = line.substring(1).trim();

                    if (metadata.startsWith("Type:")) {
                        type = metadata.substring(5).trim();
                    } else if (metadata.startsWith("Dimensions:")) {
                        String dims = metadata.substring(11).trim();
                        String[] parts = dims.split("x");
                        rows = Integer.parseInt(parts[0]);
                        cols = Integer.parseInt(parts[1]);
                    } else if (metadata.startsWith("Difficulty:") || metadata.startsWith("Difficulté:")) {
                        difficulty = metadata.substring(11).trim();
                    } else if (metadata.startsWith("Fixed piece:") || metadata.startsWith("Pièce fixe:")) {
                        fixedPiece = Integer.parseInt(metadata.substring(metadata.indexOf(":") + 1).trim());
                    } else if (metadata.startsWith("Verbose:")) {
                        String val = metadata.substring(8).trim().toLowerCase();
                        verbose = val.equals("true") || val.equals("oui") || val.equals("yes");
                    } else if (metadata.startsWith("SeuillAffichage:")) {
                        minDepthToShowRecords = Integer.parseInt(metadata.substring(16).trim());
                    } else if (metadata.startsWith("SortOrder:")) {
                        sortOrder = metadata.substring(10).trim().toLowerCase();
                    } else if (metadata.startsWith("PrioritizeBorders:")) {
                        String val = metadata.substring(18).trim().toLowerCase();
                        prioritizeBorders = val.equals("true") || val.equals("oui") || val.equals("yes");
                    } else if (metadata.startsWith("PieceFixePosition:")) {
                        String pos = metadata.substring(18).trim();
                        String[] coords = pos.split("[,\\s]+");
                        if (coords.length >= 3) {
                            // For compatibility with old system (single piece)
                            fixedPieceRow = Integer.parseInt(coords[0]);
                            fixedPieceCol = Integer.parseInt(coords[1]);
                            fixedPieceRotation = Integer.parseInt(coords[2]);
                        }
                        // New approach: support multiple pieces  "pieceId row col rotation"
                        if (coords.length >= 4) {
                            int pieceId = Integer.parseInt(coords[0]);
                            int row = Integer.parseInt(coords[1]);
                            int col = Integer.parseInt(coords[2]);
                            int rotation = Integer.parseInt(coords[3]);
                            fixedPiecesList.add(new FixedPiece(pieceId, row, col, rotation));
                        }
                    } else if (!metadata.contains("=") && !metadata.startsWith("Format") &&
                               !metadata.startsWith("Total") && !metadata.startsWith("Mapping")) {
                        // This is probably the name
                        if (name == null && metadata.length() > 0) {
                            name = metadata;
                        }
                    }
                    continue;
                }

                // Parse pieces
                String[] parts = line.split("\\s+");
                if (parts.length >= 5) {
                    int id = Integer.parseInt(parts[0]);
                    int north = Integer.parseInt(parts[1]);
                    int east = Integer.parseInt(parts[2]);
                    int south = Integer.parseInt(parts[3]);
                    int west = Integer.parseInt(parts[4]);

                    int[] edges = {north, east, south, west};
                    pieces.put(id, new Piece(id, edges));
                }
            }

            // Create configuration
            if (name != null && type != null && rows > 0 && cols > 0) {
                config = new PuzzleConfig(name, type, rows, cols, difficulty != null ? difficulty : "unknown");
                config.pieces = pieces;
                config.fixedPiece = fixedPiece;
                config.fixedPieceRow = fixedPieceRow;
                config.fixedPieceCol = fixedPieceCol;
                config.fixedPieceRotation = fixedPieceRotation;
                config.fixedPieces = fixedPiecesList;
                config.verbose = verbose;
                config.minDepthToShowRecords = minDepthToShowRecords;
                config.sortOrder = sortOrder;
                config.prioritizeBorders = prioritizeBorders;
            }
        }

        return config;
    }

    /**
     * Display puzzle information
     */
    public void printInfo() {
        SolverLogger.info("╔═══════════════════════════════════════════════════════════════════╗");
        SolverLogger.info("║ " + String.format("%-65s", name) + " ║");
        SolverLogger.info("╠═══════════════════════════════════════════════════════════════════╣");
        SolverLogger.info("║ Type: " + String.format("%-59s", type) + " ║");
        SolverLogger.info("║ Dimensions: " + String.format("%-54s", rows + "×" + cols) + " ║");
        SolverLogger.info("║ Pieces: " + String.format("%-58s", pieces.size()) + " ║");
        SolverLogger.info("║ Difficulty: " + String.format("%-54s", difficulty) + " ║");
        if (fixedPiece != null) {
            SolverLogger.info("║ Fixed piece: " + String.format("%-53s", fixedPiece) + " ║");
        }
        SolverLogger.info("╚═══════════════════════════════════════════════════════════════════╝");
    }

    /**
     * Display a summary of results
     */
    public void printSummary(long duration, boolean solved) {
        SolverLogger.info("\n┌───────────────────────────────────────────────────────────────────┐");
        SolverLogger.info("│ SUMMARY: " + String.format("%-57s", name) + " │");
        SolverLogger.info("├───────────────────────────────────────────────────────────────────┤");
        SolverLogger.info("│ Result: " + String.format("%-58s", solved ? "✓ SOLVED" : "✗ NOT SOLVED") + " │");
        SolverLogger.info("│ Time: " + String.format("%-61s", formatDuration(duration)) + " │");
        SolverLogger.info("└───────────────────────────────────────────────────────────────────┘");
    }

    private String formatDuration(long ms) {
        if (ms < TimeConstants.MILLIS_PER_SECOND) {
            return ms + " ms";
        } else if (ms < TimeConstants.MILLIS_PER_MINUTE) {
            return String.format("%.2f s", ms / (double)TimeConstants.MILLIS_PER_SECOND);
        } else if (ms < TimeConstants.MILLIS_PER_HOUR) {
            return String.format("%.2f min", ms / (double)TimeConstants.MILLIS_PER_MINUTE);
        } else {
            return String.format("%.2f h", ms / (double)TimeConstants.MILLIS_PER_HOUR);
        }
    }
}
