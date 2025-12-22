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

        // Simple representation: piece:rotation format
        for (int r = 0; r < rows; r++) {
            writer.print("# ");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    writer.print("  --  ");
                } else {
                    Placement p = board.getPlacement(r, c);
                    writer.printf(" %3d:%d ", p.getPieceId(), p.getRotation());
                }
            }
            writer.println();
        }
    }

    /**
     * Generates a detailed visual ASCII representation with edge information for save files.
     * Replaces deprecated SaveBoardRenderer.generateBoardVisualDetailed().
     *
     * @param writer PrintWriter to write the visual representation
     * @param board Board to render
     * @param allPieces Map of all pieces for displaying edge details
     */
    public static void writeToSaveFileDetailed(PrintWriter writer, Board board, Map<Integer, Piece> allPieces) {
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
                    line1.append("    .    ");
                } else {
                    Placement p = board.getPlacement(r, c);
                    Piece piece = allPieces.get(p.getPieceId());
                    if (piece != null) {
                        int[] edges = piece.getEdges();
                        int northEdge = edges[p.getRotation()];
                        line1.append(String.format("   %2d    ", northEdge));
                    } else {
                        line1.append("   ??    ");
                    }
                }
                if (c < cols - 1) line1.append("│");
            }
            writer.println(line1.toString());

            // Line 2: piece ID with west and east edges
            StringBuilder line2 = new StringBuilder("# ");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    line2.append("    .    ");
                } else {
                    Placement p = board.getPlacement(r, c);
                    Piece piece = allPieces.get(p.getPieceId());
                    if (piece != null) {
                        int[] edges = piece.getEdges();
                        int westEdge = edges[(3 + p.getRotation()) % 4];
                        int eastEdge = edges[(1 + p.getRotation()) % 4];
                        line2.append(String.format("%2d %3d %2d", westEdge, p.getPieceId(), eastEdge));
                    } else {
                        line2.append(" ? ??? ?");
                    }
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
                    Piece piece = allPieces.get(p.getPieceId());
                    if (piece != null) {
                        int[] edges = piece.getEdges();
                        int southEdge = edges[(2 + p.getRotation()) % 4];
                        line3.append(String.format("   %2d    ", southEdge));
                    } else {
                        line3.append("   ??    ");
                    }
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
}
