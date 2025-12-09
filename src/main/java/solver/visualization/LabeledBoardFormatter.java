package solver.visualization;

import util.SolverLogger;

import model.Board;
import model.Piece;
import model.Placement;
import solver.BoardVisualizer.FitsChecker;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static solver.visualization.AnsiColorHelper.*;
import static solver.visualization.GridDrawingHelper.*;

/**
 * Labeled board formatter with coordinate labels and edge validation.
 *
 * Features:
 * - Row labels (A-Z) and column numbers (1-N)
 * - Unicode box-drawing characters (─, │, ┼)
 * - Edge matching visualization: GREEN (match) vs RED (mismatch)
 * - Fixed positions highlighted in BRIGHT CYAN
 * - Piece count color coding:
 *   - BRIGHT RED + BOLD: 0 pieces (dead end!)
 *   - BRIGHT YELLOW: 1-5 pieces (critical)
 *   - YELLOW: 6-20 pieces (warning)
 *
 * Example output:
 * <pre>
 *        1       2       3
 *    ─────────────────────
 *    │         │   12    │
 *  A │  <span style="color:green">05</span>  <span style="color:cyan">123</span>  <span style="color:green">18</span> │  (245) │
 *    │         │   22    │
 *    ─────────┼─────────────
 * </pre>
 *
 * This format is ideal for:
 * - Puzzle design and validation
 * - Manual solving with coordinates
 * - Edge constraint verification
 */
public class LabeledBoardFormatter implements BoardFormatter {

    private static final int CELL_WIDTH = 9;

    @Override
    public void format(FormatterContext context) {
        Board board = context.getBoard();
        Map<Integer, Piece> piecesById = context.getPieces();
        List<Integer> unusedIds = context.getUnusedIds();
        Set<String> fixedPositions = context.getFixedPositions();
        FitsChecker fitsChecker = context.getFitsChecker();

        int rows = board.getRows();
        int cols = board.getCols();

        // Header with column numbers
        printColumnHeader(cols);

        // Top line
        printTopLine(cols);

        // Rows
        for (int r = 0; r < rows; r++) {
            printNorthEdgeLine(board, r, cols, fixedPositions);
            printMiddleLine(board, piecesById, unusedIds, fitsChecker, r, cols, fixedPositions);
            printSouthEdgeLine(board, r, cols, fixedPositions);

            // Separator between rows (not after last row)
            if (r < rows - 1) {
                printSeparatorLine(cols);
            }
        }

        // Bottom line
        printBottomLine(cols);
    }

    private void printColumnHeader(int cols) {
        System.out.print("     ");  // Left margin
        for (int c = 0; c < cols; c++) {
            System.out.printf("  %2d     ", (c + 1));
            if (c < cols - 1) System.out.print(" ");
        }
        SolverLogger.info("");
    }

    private void printTopLine(int cols) {
        System.out.print("   " + HORIZONTAL);
        for (int c = 0; c < cols; c++) {
            System.out.print(repeat(HORIZONTAL, CELL_WIDTH));
            if (c < cols - 1) System.out.print(HORIZONTAL);
        }
        SolverLogger.info("");
    }

    private void printNorthEdgeLine(Board board, int row, int cols, Set<String> fixedPositions) {
        System.out.print("   " + VERTICAL);
        for (int c = 0; c < cols; c++) {
            printNorthEdge(board, row, c, fixedPositions);
            System.out.print(VERTICAL);
        }
        SolverLogger.info("");
    }

    private void printNorthEdge(Board board, int row, int col, Set<String> fixedPositions) {
        boolean isFixed = fixedPositions != null && fixedPositions.contains(row + "," + col);

        if (board.isEmpty(row, col)) {
            System.out.print(spaces(CELL_WIDTH));
        } else {
            int[] edges = board.getPlacement(row, col).edges;
            int edgeNorth = edges[0];

            // Check if edge matches neighbor
            String edgeColor = getEdgeMatchColorForNorth(board, row, col);

            if (isFixed) {
                System.out.print(BOLD_BRIGHT_CYAN);
            } else if (!edgeColor.isEmpty()) {
                System.out.print(edgeColor);
            }

            System.out.printf("   %2d    ", edgeNorth);
            System.out.print(RESET);
        }
    }

    private void printMiddleLine(Board board, Map<Integer, Piece> piecesById, List<Integer> unusedIds,
                                  FitsChecker fitsChecker, int row, int cols, Set<String> fixedPositions) {
        char rowLabel = rowLabel(row);
        System.out.print(" " + rowLabel + " " + VERTICAL);

        for (int c = 0; c < cols; c++) {
            printCellContent(board, piecesById, unusedIds, fitsChecker, row, c, cols, fixedPositions);
            System.out.print(VERTICAL);
        }
        SolverLogger.info("");
    }

    private void printCellContent(Board board, Map<Integer, Piece> piecesById, List<Integer> unusedIds,
                                   FitsChecker fitsChecker, int row, int col, int cols, Set<String> fixedPositions) {
        boolean isFixed = fixedPositions != null && fixedPositions.contains(row + "," + col);

        if (board.isEmpty(row, col)) {
            // Count valid pieces
            int validCount = countValidPiecesForPosition(board, row, col, piecesById, unusedIds, fitsChecker);

            // Color according to count
            String countColor = getCountColor(validCount);

            if (!countColor.isEmpty()) System.out.print(countColor);
            System.out.printf("  (%3d)  ", validCount);
            if (!countColor.isEmpty()) System.out.print(RESET);

        } else {
            int pieceId = board.getPlacement(row, col).getPieceId();
            int[] edges = board.getPlacement(row, col).edges;
            int edgeWest = edges[3];
            int edgeEast = edges[1];

            // Check West and East matches
            String westColor = getEdgeMatchColorForWest(board, row, col);
            String eastColor = getEdgeMatchColorForEast(board, row, col, cols);

            // Display West
            if (isFixed) {
                System.out.print(BOLD_BRIGHT_CYAN);
            } else if (!westColor.isEmpty()) {
                System.out.print(westColor);
            }
            System.out.printf("%2d", edgeWest);
            System.out.print(RESET);

            // Display ID (always cyan if fixed)
            if (isFixed) System.out.print(BOLD_BRIGHT_CYAN);
            System.out.printf(" %3d ", pieceId);
            System.out.print(RESET);

            // Display East
            if (isFixed) {
                System.out.print(BOLD_BRIGHT_CYAN);
            } else if (!eastColor.isEmpty()) {
                System.out.print(eastColor);
            }
            System.out.printf("%2d", edgeEast);
            System.out.print(RESET);
        }
    }

    private void printSouthEdgeLine(Board board, int row, int cols, Set<String> fixedPositions) {
        System.out.print("   " + VERTICAL);
        for (int c = 0; c < cols; c++) {
            printSouthEdge(board, row, c, fixedPositions);
            System.out.print(VERTICAL);
        }
        SolverLogger.info("");
    }

    private void printSouthEdge(Board board, int row, int col, Set<String> fixedPositions) {
        boolean isFixed = fixedPositions != null && fixedPositions.contains(row + "," + col);

        if (board.isEmpty(row, col)) {
            System.out.print(spaces(CELL_WIDTH));
        } else {
            int[] edges = board.getPlacement(row, col).edges;
            int edgeSouth = edges[2];

            // Check if edge matches neighbor below
            String edgeColor = getEdgeMatchColorForSouth(board, row, col);

            if (isFixed) {
                System.out.print(BOLD_BRIGHT_CYAN);
            } else if (!edgeColor.isEmpty()) {
                System.out.print(edgeColor);
            }

            System.out.printf("   %2d    ", edgeSouth);
            System.out.print(RESET);
        }
    }

    private void printSeparatorLine(int cols) {
        System.out.print("   " + HORIZONTAL);
        for (int c = 0; c < cols; c++) {
            System.out.print(repeat(HORIZONTAL, CELL_WIDTH));
            if (c < cols - 1) System.out.print(CROSS);
        }
        SolverLogger.info("");
    }

    private void printBottomLine(int cols) {
        System.out.print("   " + HORIZONTAL);
        for (int c = 0; c < cols; c++) {
            System.out.print(repeat(HORIZONTAL, CELL_WIDTH));
            if (c < cols - 1) System.out.print(HORIZONTAL);
        }
        SolverLogger.info("");
    }

    // ===== Edge Matching Helpers =====

    private String getEdgeMatchColorForNorth(Board board, int row, int col) {
        if (row > 0 && !board.isEmpty(row - 1, col)) {
            int edgeNorth = board.getPlacement(row, col).edges[0];
            int neighborSouth = board.getPlacement(row - 1, col).edges[2];
            return getEdgeMatchColor(edgeNorth == neighborSouth);
        }
        return "";
    }

    private String getEdgeMatchColorForSouth(Board board, int row, int col) {
        int rows = board.getRows();
        if (row < rows - 1 && !board.isEmpty(row + 1, col)) {
            int edgeSouth = board.getPlacement(row, col).edges[2];
            int neighborNorth = board.getPlacement(row + 1, col).edges[0];
            return getEdgeMatchColor(edgeSouth == neighborNorth);
        }
        return "";
    }

    private String getEdgeMatchColorForWest(Board board, int row, int col) {
        if (col > 0 && !board.isEmpty(row, col - 1)) {
            int edgeWest = board.getPlacement(row, col).edges[3];
            int neighborEast = board.getPlacement(row, col - 1).edges[1];
            return getEdgeMatchColor(edgeWest == neighborEast);
        }
        return "";
    }

    private String getEdgeMatchColorForEast(Board board, int row, int col, int cols) {
        if (col < cols - 1 && !board.isEmpty(row, col + 1)) {
            int edgeEast = board.getPlacement(row, col).edges[1];
            int neighborWest = board.getPlacement(row, col + 1).edges[3];
            return getEdgeMatchColor(edgeEast == neighborWest);
        }
        return "";
    }

    // ===== Piece Counting Helper =====

    private int countValidPiecesForPosition(Board board, int r, int c,
                                            Map<Integer, Piece> piecesById,
                                            List<Integer> unusedIds,
                                            FitsChecker fitsChecker) {
        if (unusedIds == null) {
            return 0;
        }

        int count = 0;
        for (int pieceId : unusedIds) {
            Piece piece = piecesById.get(pieceId);
            if (piece != null) {
                // Test all 4 rotations
                for (int rotation = 0; rotation < 4; rotation++) {
                    int[] rotatedEdges = piece.edgesRotated(rotation);
                    if (fitsChecker.fits(board, r, c, rotatedEdges)) {
                        count++;
                        break; // One rotation is enough to count this piece
                    }
                }
            }
        }
        return count;
    }
}
