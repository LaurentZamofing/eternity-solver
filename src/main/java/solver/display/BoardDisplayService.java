package solver.display;

import model.Board;
import model.Piece;

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
        System.out.println(formatBoardSimple(board));
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

        System.out.println("\n╔" + border + "╗");
        System.out.println("║ " + centerText(title, width - 2) + " ║");
        System.out.println("╠" + border + "╣");

        String boardStr = formatBoardSimple(board);

        for (String line : boardStr.split("\n")) {
            if (!line.trim().isEmpty()) {
                System.out.println("║ " + line);
            }
        }

        System.out.println("╚" + border + "╝\n");
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
}
