package solver.visualization;

import model.Board;
import model.Piece;
import model.Placement;
import solver.BoardVisualizer.FitsChecker;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static solver.visualization.AnsiColorHelper.*;

/**
 * Detailed board formatter with piece counts, highlighting, and color coding.
 *
 * Features:
 * - Shows placed pieces with piece ID and edges
 * - Shows empty cells with count of possible pieces
 * - Highlights last placed piece in GREEN
 * - Highlights cell with minimum remaining values (MRV) in CYAN
 * - Colors dead ends (0 pieces) in RED
 * - Uses BOLD for emphasized cells
 *
 * Example output:
 * <pre>
 * Puzzle state:
 * ===============
 * |---------|---------|
 * |         |   12    |
 * | <span style="color:green">05  123  18</span> |   245   |   (bold = last placed, cyan = MRV)
 * |         |   22    |
 * |---------|---------|
 * </pre>
 *
 * This format is ideal for:
 * - Interactive solving sessions
 * - Debugging constraint propagation
 * - Monitoring MRV heuristic behavior
 */
public class DetailedBoardFormatter implements BoardFormatter {

    private static final int CELL_WIDTH = 9;  // Width of each cell content

    @Override
    public void format(FormatterContext context) {
        Board board = context.getBoard();
        Map<Integer, Piece> piecesById = context.getPieces();
        BitSet pieceUsed = context.getPieceUsed();
        int totalPieces = context.getTotalPieces();
        int lastPlacedRow = context.getLastPlacedRow();
        int lastPlacedCol = context.getLastPlacedCol();
        FitsChecker fitsChecker = context.getFitsChecker();

        System.out.println("\nPuzzle state:");
        System.out.println("===============");

        int rows = board.getRows();
        int cols = board.getCols();

        // Calculate counts for all empty cells and find MRV cell
        int[][] counts = new int[rows][cols];
        CellCoord mrvCell = findMinimumRemainingValues(board, piecesById, pieceUsed, totalPieces, fitsChecker, counts);

        // Display grid
        for (int r = 0; r < rows; r++) {
            printTopBorder(board, r, lastPlacedRow, lastPlacedCol, mrvCell);
            printNorthEdges(board, r, lastPlacedRow, lastPlacedCol, mrvCell);
            printMiddleLine(board, piecesById, counts, r, lastPlacedRow, lastPlacedCol, mrvCell);
            printSouthEdges(board, r, lastPlacedRow, lastPlacedCol, mrvCell);
        }

        // Final border
        printFinalBorder(cols);
        System.out.println();
    }

    /**
     * Finds cell with minimum remaining values (MRV heuristic).
     */
    private CellCoord findMinimumRemainingValues(Board board, Map<Integer, Piece> piecesById,
                                                  BitSet pieceUsed, int totalPieces,
                                                  FitsChecker fitsChecker, int[][] counts) {
        int minCount = Integer.MAX_VALUE;
        CellCoord mrvCell = new CellCoord(-1, -1);

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.isEmpty(r, c)) {
                    counts[r][c] = countUniquePieces(board, r, c, piecesById, pieceUsed, totalPieces, fitsChecker);
                    if (counts[r][c] > 0 && counts[r][c] < minCount) {
                        minCount = counts[r][c];
                        mrvCell = new CellCoord(r, c);
                    }
                }
            }
        }

        return mrvCell;
    }

    private void printTopBorder(Board board, int row, int lastRow, int lastCol, CellCoord mrvCell) {
        System.out.print("|");
        for (int c = 0; c < board.getCols(); c++) {
            boolean highlight = shouldHighlight(row, c, lastRow, lastCol, mrvCell);
            if (highlight) {
                System.out.print(BOLD + "---------" + RESET);
            } else {
                System.out.print("---------");
            }
            System.out.print("|");
        }
        System.out.println();
    }

    private void printNorthEdges(Board board, int row, int lastRow, int lastCol, CellCoord mrvCell) {
        for (int c = 0; c < board.getCols(); c++) {
            boolean highlight = shouldHighlight(row, c, lastRow, lastCol, mrvCell);
            Placement p = board.getPlacement(row, c);

            if (highlight) System.out.print(BOLD);

            if (p == null) {
                System.out.print("|         ");  // 9 spaces
            } else {
                System.out.printf("|   %2d    ", p.edges[0]);
            }

            if (highlight) System.out.print(RESET);
        }
        System.out.println("|");
    }

    private void printMiddleLine(Board board, Map<Integer, Piece> piecesById, int[][] counts,
                                  int row, int lastRow, int lastCol, CellCoord mrvCell) {
        for (int c = 0; c < board.getCols(); c++) {
            Placement p = board.getPlacement(row, c);

            // Determine color
            String color = getCellColor(row, c, lastRow, lastCol, mrvCell, p, counts);

            if (!color.isEmpty()) System.out.print(color);

            if (p == null) {
                // Empty cell - show count
                System.out.printf("|   %3d   ", counts[row][c]);
            } else {
                // Placed piece - show edges and ID
                System.out.printf("|%2d %3d %2d", p.edges[3], p.getPieceId(), p.edges[1]);
            }

            if (!color.isEmpty()) System.out.print(RESET);
        }
        System.out.println("|");
    }

    private void printSouthEdges(Board board, int row, int lastRow, int lastCol, CellCoord mrvCell) {
        for (int c = 0; c < board.getCols(); c++) {
            boolean highlight = shouldHighlight(row, c, lastRow, lastCol, mrvCell);
            Placement p = board.getPlacement(row, c);

            if (highlight) System.out.print(BOLD);

            if (p == null) {
                System.out.print("|         ");  // 9 spaces
            } else {
                System.out.printf("|   %2d    ", p.edges[2]);
            }

            if (highlight) System.out.print(RESET);
        }
        System.out.println("|");
    }

    private void printFinalBorder(int cols) {
        System.out.print("|");
        for (int c = 0; c < cols; c++) {
            System.out.print("---------|");
        }
        System.out.println();
    }

    private boolean shouldHighlight(int row, int col, int lastRow, int lastCol, CellCoord mrvCell) {
        return (row == lastRow && col == lastCol) || (row == mrvCell.row && col == mrvCell.col);
    }

    private String getCellColor(int row, int col, int lastRow, int lastCol, CellCoord mrvCell,
                                 Placement p, int[][] counts) {
        if (row == lastRow && col == lastCol) {
            return GREEN;  // Last placed piece
        } else if (row == mrvCell.row && col == mrvCell.col) {
            return CYAN;   // MRV cell
        } else if (p == null && counts[row][col] == 0) {
            return RED;    // Dead end
        }
        return "";
    }

    private int countUniquePieces(Board board, int r, int c, Map<Integer, Piece> piecesById,
                                   BitSet pieceUsed, int totalPieces, FitsChecker fitsChecker) {
        Set<Integer> validPieceIds = new HashSet<>();
        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed != null && pieceUsed.get(pid)) continue;
            Piece piece = piecesById.get(pid);
            if (piece == null) continue;

            for (int rot = 0; rot < 4; rot++) {
                int[] candidate = piece.edgesRotated(rot);
                if (fitsChecker.fits(board, r, c, candidate)) {
                    validPieceIds.add(pid);
                    break;
                }
            }
        }
        return validPieceIds.size();
    }

    /** Simple coordinate holder. */
    private static class CellCoord {
        final int row;
        final int col;

        CellCoord(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }
}
