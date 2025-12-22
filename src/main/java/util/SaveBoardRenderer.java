package util;

import model.Board;
import model.Piece;

import java.io.PrintWriter;
import java.util.Map;

/**
 * Handles rendering of board state for save files.
 * Provides visual ASCII representations of the puzzle board.
 *
 * Extracted from SaveStateManager for better separation of concerns.
 * Delegates actual rendering to BoardTextRenderer.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 * @deprecated Use {@link solver.display.BoardDisplayService} for unified visualization API.
 *             This class is maintained for backward compatibility.
 * @see solver.display.BoardDisplayService
 */
@Deprecated
public class SaveBoardRenderer {

    /**
     * Generates a visual ASCII representation of the board (simple, without edges).
     *
     * @param writer PrintWriter to write the visual representation
     * @param board Board to render
     */
    public static void generateBoardVisual(PrintWriter writer, Board board) {
        // Delegate to BoardTextRenderer (refactored for better code organization)
        BoardTextRenderer.generateBoardVisual(writer, board);
    }

    /**
     * Generates a detailed visual ASCII representation with edge information.
     *
     * @param writer PrintWriter to write the visual representation
     * @param board Board to render
     * @param allPieces Map of all pieces for displaying edge details
     */
    public static void generateBoardVisualDetailed(PrintWriter writer, Board board,
                                                  Map<Integer, Piece> allPieces) {
        // Delegate to BoardTextRenderer (refactored for better code organization)
        BoardTextRenderer.generateBoardVisualDetailed(writer, board, allPieces);
    }
}
