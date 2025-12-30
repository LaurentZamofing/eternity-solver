package solver.display;

import model.Board;

/**
 * Formats cell content for board display with colors.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Format north/south edge lines</li>
 *   <li>Format middle line (west + piece ID + east)</li>
 *   <li>Format empty cells with valid piece count</li>
 *   <li>Apply appropriate colors from ColorStrategy</li>
 * </ul>
 *
 * <h2>Design</h2>
 * Extracted from BoardDisplayManager to separate formatting logic from rendering logic.
 * Uses Strategy Pattern for coloring, allowing different color schemes.
 *
 * <h2>Cell Format</h2>
 * Each cell is 9 characters wide and 3 lines tall:
 * <pre>
 *    12       (north edge)
 * 34 256 78   (west + ID + east)
 *    90       (south edge)
 * </pre>
 *
 * Empty cells show valid piece count:
 * <pre>
 *
 *   (42)      (valid count)
 *
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class CellFormatter {

    private final ColorStrategy colorStrategy;

    /**
     * Creates cell formatter with color strategy.
     *
     * @param colorStrategy Strategy for determining cell and edge colors
     */
    public CellFormatter(ColorStrategy colorStrategy) {
        this.colorStrategy = colorStrategy;
    }

    /**
     * Formats north edge line for a cell.
     * Returns 9-character string with edge value and appropriate color.
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @return Formatted string with color codes
     */
    public String formatNorthEdge(Board board, int row, int col) {
        if (board.isEmpty(row, col)) {
            return "         "; // 9 spaces
        }

        int[] edges = board.getPlacement(row, col).edges;
        int edgeNorth = edges[0];

        // Determine color (cell color takes precedence over edge color)
        String cellColor = colorStrategy.getCellColor(board, row, col);
        String edgeColor = colorStrategy.getEdgeColor(board, row, col, 0); // NORTH = 0

        String color = !cellColor.isEmpty() ? cellColor : edgeColor;

        if (!color.isEmpty()) {
            return String.format("%s   %2d    %s", color, edgeNorth, ColorStrategy.RESET);
        } else {
            return String.format("   %2d    ", edgeNorth);
        }
    }

    /**
     * Formats south edge line for a cell.
     * Returns 9-character string with edge value and appropriate color.
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @return Formatted string with color codes
     */
    public String formatSouthEdge(Board board, int row, int col) {
        if (board.isEmpty(row, col)) {
            return "         "; // 9 spaces
        }

        int[] edges = board.getPlacement(row, col).edges;
        int edgeSouth = edges[2];

        // Determine color (cell color takes precedence over edge color)
        String cellColor = colorStrategy.getCellColor(board, row, col);
        String edgeColor = colorStrategy.getEdgeColor(board, row, col, 2); // SOUTH = 2

        String color = !cellColor.isEmpty() ? cellColor : edgeColor;

        if (!color.isEmpty()) {
            return String.format("%s   %2d    %s", color, edgeSouth, ColorStrategy.RESET);
        } else {
            return String.format("   %2d    ", edgeSouth);
        }
    }

    /**
     * Formats middle line for a cell (west + piece ID + east).
     * Returns 9-character string with piece information and appropriate colors.
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @return Formatted string with color codes
     */
    public String formatMiddleLine(Board board, int row, int col) {
        if (board.isEmpty(row, col)) {
            // This should be handled by formatEmptyCell instead
            return "         "; // 9 spaces
        }

        int pieceId = board.getPlacement(row, col).getPieceId();
        int[] edges = board.getPlacement(row, col).edges;
        int edgeWest = edges[3];
        int edgeEast = edges[1];

        // Get colors
        String cellColor = colorStrategy.getCellColor(board, row, col);
        String westColor = colorStrategy.getEdgeColor(board, row, col, 3); // WEST = 3
        String eastColor = colorStrategy.getEdgeColor(board, row, col, 1); // EAST = 1

        // Build formatted string
        StringBuilder sb = new StringBuilder();

        // West edge
        String westDisplayColor = !cellColor.isEmpty() ? cellColor : westColor;
        if (!westDisplayColor.isEmpty()) {
            sb.append(westDisplayColor);
        }
        sb.append(String.format("%2d", edgeWest));
        if (!westDisplayColor.isEmpty()) {
            sb.append(ColorStrategy.RESET);
        }

        // Piece ID (always uses cell color if available)
        if (!cellColor.isEmpty()) {
            sb.append(cellColor);
        }
        sb.append(String.format(" %3d ", pieceId));
        if (!cellColor.isEmpty()) {
            sb.append(ColorStrategy.RESET);
        }

        // East edge
        String eastDisplayColor = !cellColor.isEmpty() ? cellColor : eastColor;
        if (!eastDisplayColor.isEmpty()) {
            sb.append(eastDisplayColor);
        }
        sb.append(String.format("%2d", edgeEast));
        if (!eastDisplayColor.isEmpty()) {
            sb.append(ColorStrategy.RESET);
        }

        return sb.toString();
    }

    /**
     * Formats empty cell with valid piece count.
     * Returns 9-character string with count in parentheses and appropriate color.
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @param validCount Number of valid pieces for this position
     * @return Formatted string with color codes
     */
    public String formatEmptyCell(Board board, int row, int col, int validCount) {
        // Get color from strategy (usually based on valid count)
        String color = colorStrategy.getCellColor(board, row, col);

        if (!color.isEmpty()) {
            return String.format("%s  (%3d)  %s", color, validCount, ColorStrategy.RESET);
        } else {
            return String.format("  (%3d)  ", validCount);
        }
    }

    /**
     * Formats empty cell with piece count and rotation count.
     * Displays as "P/R" where P=pieces, R=rotations.
     * Example: "(1/4)" means 1 piece with 4 valid rotations.
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @param numPieces Number of unique pieces that fit
     * @param numRotations Total number of valid rotations
     * @return Formatted string with color codes
     */
    public String formatEmptyCellWithRotations(Board board, int row, int col, int numPieces, int numRotations) {
        // Get color from strategy (usually based on total rotations)
        String color = colorStrategy.getCellColor(board, row, col);

        // Format: (PPP/RRR) = 9 chars exactly (consistent with save files)
        if (!color.isEmpty()) {
            return String.format("%s(%3d/%3d)%s", color, numPieces, numRotations, ColorStrategy.RESET);
        } else {
            return String.format("(%3d/%3d)", numPieces, numRotations);
        }
    }
}
