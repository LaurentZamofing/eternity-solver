package solver.visualization;

import util.SolverLogger;

import model.Board;
import model.Piece;
import model.Placement;
import solver.BoardVisualizer.FitsChecker;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Compact ASCII board formatter showing edges and piece counts.
 *
 * Format:
 * - Placed pieces: Display all 4 edges (N/E/S/W) around the cell
 * - Empty cells: Display number of possible pieces that fit
 * - Dead ends (0 pieces): Display "X"
 * - Overflow (>999 pieces): Display "999+"
 *
 * Example output:
 * <pre>
 * ---------------------
 * |     ||  12 ||     |
 * | 034 ||05 18||  X  |
 * |     ||  22 ||     |
 * ---------------------
 * </pre>
 *
 * This is the simplest visualization format, useful for:
 * - Quick debugging
 * - Monitoring solver progress
 * - Compact terminal displays
 */
public class CompactBoardFormatter implements BoardFormatter {

    @Override
    public void format(FormatterContext context) {
        Board board = context.getBoard();
        Map<Integer, Piece> piecesById = context.getPieces();
        BitSet pieceUsed = context.getPieceUsed();
        int totalPieces = context.getTotalPieces();
        FitsChecker fitsChecker = context.getFitsChecker();

        int rows = board.getRows();
        int cols = board.getCols();

        for (int r = 0; r < rows; r++) {
            // Top border line
            printTopBorder(cols);

            // Line with North edges
            printNorthEdges(board, r, cols);

            // Line with West/East edges (or piece count for empty cells)
            printWestEastOrCount(board, piecesById, pieceUsed, totalPieces, fitsChecker, r, cols);

            // Line with South edges
            printSouthEdges(board, r, cols);
        }

        // Final border line
        printTopBorder(cols);
    }

    /**
     * Prints top border line.
     */
    private void printTopBorder(int cols) {
        for (int c = 0; c < cols; c++) {
            System.out.print("-------");
        }
        SolverLogger.info("");
    }

    /**
     * Prints line showing North edges of all cells in row.
     */
    private void printNorthEdges(Board board, int row, int cols) {
        for (int c = 0; c < cols; c++) {
            Placement p = board.getPlacement(row, c);
            if (p == null) {
                System.out.print("|     |");
            } else {
                int n = p.edges[0];
                if (n == 0) {
                    System.out.print("|     |");
                } else {
                    System.out.printf("|  %2d |", n);
                }
            }
        }
        SolverLogger.info("");
    }

    /**
     * Prints line showing West/East edges (or piece count for empty cells).
     */
    private void printWestEastOrCount(Board board, Map<Integer, Piece> piecesById,
                                      BitSet pieceUsed, int totalPieces,
                                      FitsChecker fitsChecker, int row, int cols) {
        for (int c = 0; c < cols; c++) {
            Placement p = board.getPlacement(row, c);
            if (p == null) {
                // Empty cell: display number of possible pieces
                int count = countUniquePieces(board, row, c, piecesById, pieceUsed, totalPieces, fitsChecker);
                String countStr = GridDrawingHelper.formatPieceCount(count, 3);

                if (count > 999) {
                    System.out.print("| 999+|");
                } else if (count > 0) {
                    System.out.printf("| %3d |", count);
                } else {
                    System.out.print("|  X  |");  // Dead end
                }
            } else {
                // Placed piece: show West and East edges
                int w = p.edges[3];
                int e = p.edges[1];
                String wStr = GridDrawingHelper.formatEdge(w);
                String eStr = GridDrawingHelper.formatEdge(e);
                System.out.printf("|%s %s|", wStr, eStr);
            }
        }
        SolverLogger.info("");
    }

    /**
     * Prints line showing South edges of all cells in row.
     */
    private void printSouthEdges(Board board, int row, int cols) {
        for (int c = 0; c < cols; c++) {
            Placement p = board.getPlacement(row, c);
            if (p == null) {
                System.out.print("|     |");
            } else {
                int s = p.edges[2];
                if (s == 0) {
                    System.out.print("|     |");
                } else {
                    System.out.printf("|  %2d |", s);
                }
            }
        }
        SolverLogger.info("");
    }

    /**
     * Counts unique pieces that can be placed at position (ignoring rotations).
     */
    private int countUniquePieces(Board board, int r, int c, Map<Integer, Piece> piecesById,
                                   BitSet pieceUsed, int totalPieces, FitsChecker fitsChecker) {
        Set<Integer> validPieceIds = new HashSet<>();
        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed != null && pieceUsed.get(pid)) {
                continue; // Piece already used
            }
            Piece piece = piecesById.get(pid);
            if (piece == null) {
                continue;
            }

            boolean foundValidRotation = false;
            for (int rot = 0; rot < 4 && !foundValidRotation; rot++) {
                int[] candidate = piece.edgesRotated(rot);
                if (fitsChecker.fits(board, r, c, candidate)) {
                    validPieceIds.add(pid);
                    foundValidRotation = true;
                }
            }
        }
        return validPieceIds.size();
    }
}
