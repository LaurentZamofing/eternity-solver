package solver.display;

import model.Board;
import model.Piece;
import util.PositionKey;
import util.SolverLogger;

import java.io.PrintWriter;
import java.util.Map;

/**
 * Simple console board display (non-file rendering).
 *
 * <p>Phase 2 refactor: the 556-line original mixed console display,
 * save-file rendering, and candidate-counting logic. File I/O and
 * candidate counting now live in {@link SaveFileRenderer}; this class
 * keeps the concise text/framed display helpers used during a solve.</p>
 *
 * <p>The legacy {@code writeToSaveFile*} static entry points are kept
 * as thin delegations so existing callers (SaveStateIO and a handful
 * of tests) don't need to change.</p>
 */
public class BoardDisplayService {

    /** Displays a board as plain text via the logger. */
    public void displayBoard(Board board) {
        SolverLogger.info(formatBoardSimple(board));
    }

    /** Displays a board inside a decorative frame with a title. */
    public void displayBoardWithFrame(Board board, String title) {
        printBoardWithFrame(board, title);
    }

    /**
     * Formats the board as a simple text grid (one line per row, each
     * cell shown as "pieceId:rotation" or "--" for empty cells).
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

    /** Prints the board with a Unicode frame and a centered title. */
    public void printBoardWithFrame(Board board, String title) {
        String content = formatBoardSimple(board);
        String[] lines = content.split("\n");

        int maxLineLength = title.length();
        for (String line : lines) {
            if (line.length() > maxLineLength) maxLineLength = line.length();
        }

        int frameWidth = maxLineLength + 4;

        SolverLogger.info("╔" + "═".repeat(frameWidth - 2) + "╗");
        SolverLogger.info("║ " + centerText(title, frameWidth - 4) + " ║");
        SolverLogger.info("╠" + "═".repeat(frameWidth - 2) + "╣");

        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                SolverLogger.info("║ " + String.format("%-" + (frameWidth - 4) + "s", line) + " ║");
            }
        }

        SolverLogger.info("╚" + "═".repeat(frameWidth - 2) + "╝");
    }

    private String centerText(String text, int width) {
        if (text.length() >= width) return text;
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text + " ".repeat(width - text.length() - padding);
    }

    // ════════════════════════════════════════════════════════════════
    // Legacy save-file entry points — delegate to SaveFileRenderer.
    // Kept for backward compatibility with SaveStateIO and existing
    // tests. Prefer SaveFileRenderer.* in new code.
    // ════════════════════════════════════════════════════════════════

    public static void writeToSaveFile(PrintWriter writer, Board board) {
        SaveFileRenderer.writeToSaveFile(writer, board);
    }

    public static void writeToSaveFileDetailed(PrintWriter writer, Board board,
                                               Map<Integer, Piece> allPieces,
                                               solver.DomainManager domainManager,
                                               Map<PositionKey, Integer> placementOrderMap) {
        SaveFileRenderer.writeToSaveFileDetailed(writer, board, allPieces, domainManager, placementOrderMap);
    }
}
