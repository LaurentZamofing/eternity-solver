package util;

import util.SolverLogger;

import model.Board;
import model.Piece;

import java.util.Map;

/**
 * Utility class for rendering boards with coordinates and labels.
 * Extracted from Main.java to improve maintainability and testability.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public final class BoardRenderer {

    // Prevent instantiation
    private BoardRenderer() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    /**
     * Displays the board with coordinates A-F (rows) and 1-12 (columns).
     * Each cell displays the piece number in the center with edge values around it.
     *
     * @param board The board to display
     * @param pieces Map of all pieces (for reference)
     */
    public static void printBoardWithCoordinates(Board board, Map<Integer, Piece> pieces) {
        int rows = board.getRows();
        int cols = board.getCols();

        // Header with column numbers (right-aligned on 2 characters)
        System.out.print("     ");
        for (int c = 0; c < cols; c++) {
            System.out.printf("  %2d    ", (c + 1));
            if (c < cols - 1) System.out.print(" ");
        }
        SolverLogger.info("");

        // Top line
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("────────");
            if (c < cols - 1) System.out.print("─");
        }
        SolverLogger.info("");

        for (int r = 0; r < rows; r++) {
            char rowLabel = (char) ('A' + r);

            // Line 1: North Edge
            System.out.print("   │");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    System.out.print("        ");
                } else {
                    int[] edges = board.getPlacement(r, c).edges;
                    System.out.printf("   %2d   ", edges[0]); // North
                }
                System.out.print("│");
            }
            SolverLogger.info("");

            // Line 2: West + Piece ID + East
            System.out.print(" " + rowLabel + " │");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    System.out.print("   --   ");
                } else {
                    int pieceId = board.getPlacement(r, c).getPieceId();
                    int[] edges = board.getPlacement(r, c).edges;
                    System.out.printf("%2d %2d %2d", edges[3], pieceId, edges[1]); // West, ID, East
                }
                System.out.print("│");
            }
            SolverLogger.info("");

            // Line 3: South Edge
            System.out.print("   │");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    System.out.print("        ");
                } else {
                    int[] edges = board.getPlacement(r, c).edges;
                    System.out.printf("   %2d   ", edges[2]); // South
                }
                System.out.print("│");
            }
            SolverLogger.info("");

            // Separator between rows
            if (r < rows - 1) {
                System.out.print("   ─");
                for (int c = 0; c < cols; c++) {
                    System.out.print("────────");
                    if (c < cols - 1) System.out.print("┼");
                }
                SolverLogger.info("");
            }
        }

        // Bottom line
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("────────");
            if (c < cols - 1) System.out.print("─");
        }
        SolverLogger.info("\n");
    }
}
