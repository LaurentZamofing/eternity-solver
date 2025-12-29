package solver.display;

import util.SolverLogger;
import model.Board;
import model.Piece;
import model.Placement;

import java.io.PrintWriter;
import java.util.Map;

/**
 * Unified facade for board visualization.
 * Provides simple, commonly-used display methods.
 *
 * <h2>Purpose (Phase 5 Consolidation)</h2>
 * This service consolidates fragmented visualization code across:
 * <ul>
 *   <li>solver/BoardVisualizer.java</li>
 *   <li>solver/BoardDisplayManager.java - Advanced rendering with color strategies</li>
 *   <li>util/BoardRenderer.java - Deprecated, use this instead</li>
 *   <li>util/BoardTextRenderer.java - Deprecated, use this instead</li>
 *   <li>util/SaveBoardRenderer.java - Deprecated, use this instead</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * BoardDisplayService display = new BoardDisplayService();
 *
 * // Simple display
 * display.displayBoard(board);
 *
 * // With frame
 * display.displayBoardWithFrame(board, "Current Solution");
 * </pre>
 *
 * <h2>Advanced Features</h2>
 * For color-coded displays with edge validation, use:
 * <ul>
 *   <li>{@link BoardDisplayManager} - Full-featured with validators</li>
 *   <li>{@link LabeledBoardRenderer} - Direct renderer access</li>
 *   <li>{@link ComparisonBoardRenderer} - Board comparison</li>
 * </ul>
 *
 * @author Eternity Solver Team
 * @version 2.0.0 (Phase 5 refactoring)
 */
public class BoardDisplayService {

    /**
     * Displays a board with simple text formatting.
     *
     * @param board Board to display
     */
    public void displayBoard(Board board) {
        SolverLogger.info(formatBoardSimple(board));
    }

    /**
     * Displays a board with frame and title.
     *
     * @param board Board to display
     * @param title Title to display
     */
    public void displayBoardWithFrame(Board board, String title) {
        printBoardWithFrame(board, title);
    }

    /**
     * Formats board as simple text grid.
     * Each cell shows: pieceId:rotation
     *
     * @param board Board to format
     * @return Formatted string
     */
    public String formatBoardSimple(Board board) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.isEmpty(r, c)) {
                    sb.append("  --  ");
                } else {
                    model.Placement placement = board.getPlacement(r, c);
                    sb.append(String.format(" %3d:%d ", placement.getPieceId(), placement.getRotation()));
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Prints board to console with decorative frame/border.
     *
     * @param board Board to print
     * @param title Title to display
     */
    public void printBoardWithFrame(Board board, String title) {
        int width = board.getCols() * 6 + 2;
        String border = "═".repeat(Math.max(0, width));

        SolverLogger.info("\n╔" + border + "╗");
        SolverLogger.info("║ " + centerText(title, width - 2) + " ║");
        SolverLogger.info("╠" + border + "╣");

        String boardStr = formatBoardSimple(board);

        for (String line : boardStr.split("\n")) {
            if (!line.trim().isEmpty()) {
                SolverLogger.info("║ " + line);
            }
        }

        SolverLogger.info("╚" + border + "╝\n");
    }

    /**
     * Centers text within a given width.
     *
     * @param text Text to center
     * @param width Total width
     * @return Centered text with padding
     */
    private String centerText(String text, int width) {
        if (text == null) text = "";
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int leftPad = (width - text.length()) / 2;
        int rightPad = width - text.length() - leftPad;
        return " ".repeat(Math.max(0, leftPad)) + text + " ".repeat(Math.max(0, rightPad));
    }

    /**
     * Generates a visual ASCII representation of the board for save files.
     * Replaces deprecated SaveBoardRenderer.generateBoardVisual().
     *
     * @param writer PrintWriter to write the visual representation
     * @param board Board to render
     */
    public static void writeToSaveFile(PrintWriter writer, Board board) {
        int rows = board.getRows();
        int cols = board.getCols();

        // Top border
        writer.print("# ┌");
        for (int c = 0; c < cols; c++) {
            writer.print("──────");
            if (c < cols - 1) writer.print("┬");
        }
        writer.println("┐");

        // Board content with pipes
        for (int r = 0; r < rows; r++) {
            writer.print("# │");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    writer.print("  --  ");
                } else {
                    Placement p = board.getPlacement(r, c);
                    writer.printf(" %3d:%d ", p.getPieceId(), p.getRotation());
                }
                writer.print("│");
            }
            writer.println();

            // Row separator (except after last row)
            if (r < rows - 1) {
                writer.print("# ├");
                for (int c = 0; c < cols; c++) {
                    writer.print("──────");
                    if (c < cols - 1) writer.print("┼");
                }
                writer.println("┤");
            }
        }

        // Bottom border
        writer.print("# └");
        for (int c = 0; c < cols; c++) {
            writer.print("──────");
            if (c < cols - 1) writer.print("┴");
        }
        writer.println("┘");
    }

    /**
     * Generates a detailed visual ASCII representation with edge information for save files.
     * Replaces deprecated SaveBoardRenderer.generateBoardVisualDetailed().
     *
     * @param writer PrintWriter to write the visual representation
     * @param board Board to render
     * @param allPieces Map of all pieces for displaying edge details
     * @param domainManager Optional DomainManager for AC-3 domains (null = use estimation)
     */
    public static void writeToSaveFileDetailed(PrintWriter writer, Board board, Map<Integer, Piece> allPieces, solver.DomainManager domainManager) {
        int rows = board.getRows();
        int cols = board.getCols();

        // Generate a horizontal separator
        StringBuilder separator = new StringBuilder("# ");
        for (int c = 0; c < cols; c++) {
            separator.append("─────────");
            if (c < cols - 1) separator.append("┬");
        }
        writer.println(separator.toString());

        for (int r = 0; r < rows; r++) {
            // Line 1: north edges
            StringBuilder line1 = new StringBuilder("# ");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    // Count pieces and rotations
                    CandidateCount count;
                    if (domainManager != null && domainManager.isAC3Initialized()) {
                        java.util.Map<Integer, java.util.List<solver.DomainManager.ValidPlacement>> domain =
                            domainManager.getDomain(r, c);
                        int numPieces = (domain != null) ? domain.size() : 0;
                        // For AC-3, we don't have rotation info, so use pieces count for both
                        count = new CandidateCount(numPieces, numPieces);
                    } else {
                        count = countCandidatesWithRotations(board, r, c, allPieces);
                    }

                    if (count.numPieces > 0 || count.numRotations > 0) {
                        line1.append(String.format(" (%d/%2d) ", count.numPieces, count.numRotations));
                    } else {
                        line1.append("   ∅     ");  // Empty set symbol if no candidates
                    }
                } else {
                    Placement p = board.getPlacement(r, c);
                    // Use edges from Placement (already rotated)
                    int northEdge = p.edges[0];
                    line1.append(String.format("   %2d    ", northEdge));
                }
                if (c < cols - 1) line1.append("│");
            }
            writer.println(line1.toString());

            // Line 2: piece ID with west and east edges OR candidate count
            StringBuilder line2 = new StringBuilder("# ");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    line2.append("    .    ");
                } else {
                    Placement p = board.getPlacement(r, c);
                    // Use edges from Placement (already rotated)
                    int westEdge = p.edges[3];  // WEST = 3
                    int eastEdge = p.edges[1];  // EAST = 1
                    line2.append(String.format("%2d %3d %2d", westEdge, p.getPieceId(), eastEdge));
                }
                if (c < cols - 1) line2.append("│");
            }
            writer.println(line2.toString());

            // Line 3: south edges
            StringBuilder line3 = new StringBuilder("# ");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    line3.append("    .    ");
                } else {
                    Placement p = board.getPlacement(r, c);
                    // Use edges from Placement (already rotated)
                    int southEdge = p.edges[2];  // SOUTH = 2
                    line3.append(String.format("   %2d    ", southEdge));
                }
                if (c < cols - 1) line3.append("│");
            }
            writer.println(line3.toString());

            // Separator between rows
            if (r < rows - 1) {
                StringBuilder sep = new StringBuilder("# ");
                for (int c = 0; c < cols; c++) {
                    sep.append("─────────");
                    if (c < cols - 1) sep.append("┼");
                }
                writer.println(sep.toString());
            }
        }

        // Bottom separator
        StringBuilder bottomSep = new StringBuilder("# ");
        for (int c = 0; c < cols; c++) {
            bottomSep.append("─────────");
            if (c < cols - 1) bottomSep.append("┴");
        }
        writer.println(bottomSep.toString());
    }

    /**
     * Counts the number of candidate pieces that can be placed in an empty cell.
     * Useful for debugging and understanding solver progress.
     *
     * @param board Current board state
     * @param row Row of empty cell
     * @param col Column of empty cell
     * @param allPieces All pieces (to check which ones are available)
     * @return Number of pieces that could fit in this cell
     */
    private static int countCandidates(Board board, int row, int col, Map<Integer, Piece> allPieces) {
        if (allPieces == null) return 0;

        int count = 0;

        // Check each piece
        for (Piece piece : allPieces.values()) {
            // Skip if piece is already placed
            boolean isPlaced = false;
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (!board.isEmpty(r, c)) {
                        Placement p = board.getPlacement(r, c);
                        if (p.getPieceId() == piece.getId()) {
                            isPlaced = true;
                            break;
                        }
                    }
                }
                if (isPlaced) break;
            }

            if (isPlaced) continue;

            // Try all 4 rotations
            for (int rotation = 0; rotation < 4; rotation++) {
                if (canPlacePiece(board, row, col, piece, rotation, allPieces)) {
                    count++;
                    break; // Count piece once, not each rotation
                }
            }
        }

        return count;
    }

    /**
     * Checks if a piece can be placed at a specific position with constraints.
     *
     * @param board Current board
     * @param row Row position
     * @param col Column position
     * @param piece Piece to place
     * @param rotation Rotation to try
     * @param allPieces All pieces map
     * @return true if piece fits all edge constraints
     */
    private static boolean canPlacePiece(Board board, int row, int col, Piece piece, int rotation, Map<Integer, Piece> allPieces) {
        int[] edges = piece.edgesRotated(rotation);
        int north = edges[0];
        int east = edges[1];
        int south = edges[2];
        int west = edges[3];

        // IMPORTANT: Interior cells cannot have 0 edges
        boolean isTopBorder = (row == 0);
        boolean isBottomBorder = (row == board.getRows() - 1);
        boolean isLeftBorder = (col == 0);
        boolean isRightBorder = (col == board.getCols() - 1);

        // Interior cells: no edge can be 0
        if (!isTopBorder && north == 0) return false;
        if (!isBottomBorder && south == 0) return false;
        if (!isLeftBorder && west == 0) return false;
        if (!isRightBorder && east == 0) return false;

        // Check north neighbor
        if (row > 0) {
            if (board.isEmpty(row - 1, col)) {
                // No constraint yet
            } else {
                Placement northPlacement = board.getPlacement(row - 1, col);
                Piece northPiece = findPieceById(board, allPieces, northPlacement.getPieceId());
                if (northPiece != null) {
                    int[] northEdges = northPiece.edgesRotated(northPlacement.getRotation());
                    int northSouth = northEdges[2]; // South edge of north piece
                    if (northSouth != north) return false;
                }
            }
        } else {
            // Top border
            if (north != 0) return false;
        }

        // Check east neighbor
        if (col < board.getCols() - 1) {
            if (board.isEmpty(row, col + 1)) {
                // No constraint yet
            } else {
                Placement eastPlacement = board.getPlacement(row, col + 1);
                Piece eastPiece = findPieceById(board, allPieces, eastPlacement.getPieceId());
                if (eastPiece != null) {
                    int[] eastEdges = eastPiece.edgesRotated(eastPlacement.getRotation());
                    int eastWest = eastEdges[3]; // West edge of east piece
                    if (eastWest != east) return false;
                }
            }
        } else {
            // Right border
            if (east != 0) return false;
        }

        // Check south neighbor
        if (row < board.getRows() - 1) {
            if (board.isEmpty(row + 1, col)) {
                // No constraint yet
            } else {
                Placement southPlacement = board.getPlacement(row + 1, col);
                Piece southPiece = findPieceById(board, allPieces, southPlacement.getPieceId());
                if (southPiece != null) {
                    int[] southEdges = southPiece.edgesRotated(southPlacement.getRotation());
                    int southNorth = southEdges[0]; // North edge of south piece
                    if (southNorth != south) return false;
                }
            }
        } else {
            // Bottom border
            if (south != 0) return false;
        }

        // Check west neighbor
        if (col > 0) {
            if (board.isEmpty(row, col - 1)) {
                // No constraint yet
            } else {
                Placement westPlacement = board.getPlacement(row, col - 1);
                Piece westPiece = findPieceById(board, allPieces, westPlacement.getPieceId());
                if (westPiece != null) {
                    int[] westEdges = westPiece.edgesRotated(westPlacement.getRotation());
                    int westEast = westEdges[1]; // East edge of west piece
                    if (westEast != west) return false;
                }
            }
        } else {
            // Left border
            if (west != 0) return false;
        }

        return true;
    }

    /**
     * Finds a piece by ID from the allPieces map.
     */
    private static Piece findPieceById(Board board, Map<Integer, Piece> allPieces, int pieceId) {
        return allPieces.get(pieceId);
    }

    /**
     * Counts both the number of pieces and total rotations that can fit in a cell.
     * Similar to countCandidates() but also counts rotations.
     *
     * @param board Current board state
     * @param row Row of empty cell
     * @param col Column of empty cell
     * @param allPieces All pieces (to check which ones are available)
     * @return CandidateCount with both pieces and rotations count
     */
    private static CandidateCount countCandidatesWithRotations(Board board, int row, int col, Map<Integer, Piece> allPieces) {
        if (allPieces == null) return new CandidateCount(0, 0);

        int numPieces = 0;
        int numRotations = 0;

        // Check each piece
        for (Piece piece : allPieces.values()) {
            // Skip if piece is already placed
            boolean isPlaced = false;
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (!board.isEmpty(r, c)) {
                        Placement p = board.getPlacement(r, c);
                        if (p.getPieceId() == piece.getId()) {
                            isPlaced = true;
                            break;
                        }
                    }
                }
                if (isPlaced) break;
            }

            if (isPlaced) continue;

            // Count valid rotations for this piece
            int validRotationsForThisPiece = 0;
            for (int rotation = 0; rotation < 4; rotation++) {
                if (canPlacePiece(board, row, col, piece, rotation, allPieces)) {
                    validRotationsForThisPiece++;
                    numRotations++;
                }
            }

            // Count this piece if at least one rotation works
            if (validRotationsForThisPiece > 0) {
                numPieces++;
            }
        }

        return new CandidateCount(numPieces, numRotations);
    }

    /**
     * Simple holder for candidate count results.
     */
    private static class CandidateCount {
        final int numPieces;
        final int numRotations;

        CandidateCount(int numPieces, int numRotations) {
            this.numPieces = numPieces;
            this.numRotations = numRotations;
        }
    }
}
