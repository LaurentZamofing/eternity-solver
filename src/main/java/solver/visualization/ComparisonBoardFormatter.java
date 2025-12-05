package solver.visualization;

import model.Board;
import model.Piece;
import model.Placement;
import solver.BoardVisualizer.FitsChecker;

import java.util.List;
import java.util.Map;

import static solver.visualization.AnsiColorHelper.*;
import static solver.visualization.GridDrawingHelper.*;

/**
 * Comparison board formatter showing differences between current and reference boards.
 *
 * Color coding:
 * - BRIGHT MAGENTA + BOLD: Regression (was occupied, now empty)
 * - BRIGHT YELLOW + BOLD: Progress (was empty, now occupied)
 * - BRIGHT CYAN + BOLD: Stable (same piece and rotation)
 * - ORANGE: Changed (different piece or rotation)
 *
 * Example output:
 * <pre>
 *        1       2       3
 *    ─────────────────────
 *    │         │   12    │
 *  A │  <span style="color:cyan">05  123  18</span> │ <span style="color:orange">03  456  12</span> │   (cyan = stable, orange = changed)
 *    │         │   22    │
 *    ─────────┼─────────────
 * </pre>
 *
 * This format is ideal for:
 * - Tracking solver progress over time
 * - Comparing different solving strategies
 * - Debugging backtracking behavior
 * - Visualizing solution evolution
 */
public class ComparisonBoardFormatter implements BoardFormatter {

    private static final int CELL_WIDTH = 9;

    @Override
    public void format(FormatterContext context) {
        Board currentBoard = context.getBoard();
        Board referenceBoard = context.getReferenceBoard();
        Map<Integer, Piece> piecesById = context.getPieces();
        List<Integer> unusedIds = context.getUnusedIds();
        FitsChecker fitsChecker = context.getFitsChecker();

        if (referenceBoard == null) {
            throw new IllegalArgumentException("Reference board is required for comparison formatting");
        }

        int rows = currentBoard.getRows();
        int cols = currentBoard.getCols();

        // Header with column numbers
        printColumnHeader(cols);

        // Top line
        printTopLine(cols);

        // Rows
        for (int r = 0; r < rows; r++) {
            printNorthEdgeLine(currentBoard, referenceBoard, r, cols);
            printMiddleLine(currentBoard, referenceBoard, piecesById, unusedIds, fitsChecker, r, cols);
            printSouthEdgeLine(currentBoard, referenceBoard, r, cols);

            // Separator between rows
            if (r < rows - 1) {
                printSeparatorLine(cols);
            }
        }

        // Bottom line
        printBottomLine(cols);
    }

    private void printColumnHeader(int cols) {
        System.out.print("     ");
        for (int c = 0; c < cols; c++) {
            System.out.printf("  %2d     ", (c + 1));
            if (c < cols - 1) System.out.print(" ");
        }
        System.out.println();
    }

    private void printTopLine(int cols) {
        System.out.print("   " + HORIZONTAL);
        for (int c = 0; c < cols; c++) {
            System.out.print(repeat(HORIZONTAL, CELL_WIDTH));
            if (c < cols - 1) System.out.print(HORIZONTAL);
        }
        System.out.println();
    }

    private void printNorthEdgeLine(Board current, Board reference, int row, int cols) {
        System.out.print("   " + VERTICAL);
        for (int c = 0; c < cols; c++) {
            String cellColor = getCellComparisonColor(current, reference, row, c);

            if (current.isEmpty(row, c)) {
                System.out.print(spaces(CELL_WIDTH));
            } else {
                int[] edges = current.getPlacement(row, c).edges;
                int edgeNorth = edges[0];

                System.out.print(cellColor);
                System.out.printf("   %2d    ", edgeNorth);
                System.out.print(RESET);
            }
            System.out.print(VERTICAL);
        }
        System.out.println();
    }

    private void printMiddleLine(Board current, Board reference, Map<Integer, Piece> piecesById,
                                  List<Integer> unusedIds, FitsChecker fitsChecker, int row, int cols) {
        char rowLabel = rowLabel(row);
        System.out.print(" " + rowLabel + " " + VERTICAL);

        for (int c = 0; c < cols; c++) {
            String cellColor = getCellComparisonColor(current, reference, row, c);

            if (current.isEmpty(row, c)) {
                // Count valid pieces
                int validCount = countValidPiecesForPosition(current, row, c, piecesById, unusedIds, fitsChecker);

                // Use comparison color if it was occupied in reference
                if (!reference.isEmpty(row, c)) {
                    System.out.print(cellColor); // Magenta for regression
                } else {
                    // Color according to piece count
                    String countColor = getCountColor(validCount);
                    if (!countColor.isEmpty()) {
                        System.out.print(countColor);
                    }
                }

                System.out.printf("  (%3d)  ", validCount);
                System.out.print(RESET);

            } else {
                int pieceId = current.getPlacement(row, c).getPieceId();
                int[] edges = current.getPlacement(row, c).edges;
                int edgeWest = edges[3];
                int edgeEast = edges[1];

                // Display with comparison color
                System.out.print(cellColor);
                System.out.printf("%2d %3d %2d", edgeWest, pieceId, edgeEast);
                System.out.print(RESET);
            }
            System.out.print(VERTICAL);
        }
        System.out.println();
    }

    private void printSouthEdgeLine(Board current, Board reference, int row, int cols) {
        System.out.print("   " + VERTICAL);
        for (int c = 0; c < cols; c++) {
            String cellColor = getCellComparisonColor(current, reference, row, c);

            if (current.isEmpty(row, c)) {
                System.out.print(spaces(CELL_WIDTH));
            } else {
                int[] edges = current.getPlacement(row, c).edges;
                int edgeSouth = edges[2];

                System.out.print(cellColor);
                System.out.printf("   %2d    ", edgeSouth);
                System.out.print(RESET);
            }
            System.out.print(VERTICAL);
        }
        System.out.println();
    }

    private void printSeparatorLine(int cols) {
        System.out.print("   " + HORIZONTAL);
        for (int c = 0; c < cols; c++) {
            System.out.print(repeat(HORIZONTAL, CELL_WIDTH));
            if (c < cols - 1) System.out.print(CROSS);
        }
        System.out.println();
    }

    private void printBottomLine(int cols) {
        System.out.print("   " + HORIZONTAL);
        for (int c = 0; c < cols; c++) {
            System.out.print(repeat(HORIZONTAL, CELL_WIDTH));
            if (c < cols - 1) System.out.print(HORIZONTAL);
        }
        System.out.println();
    }

    /**
     * Returns cell comparison color based on differences between current and reference.
     *
     * @param current Current board state
     * @param reference Reference board state to compare against
     * @param row Row index
     * @param col Column index
     * @return ANSI color code:
     *         - Empty string if both empty (no change)
     *         - BOLD_BRIGHT_MAGENTA if regression (was occupied, now empty)
     *         - BOLD + YELLOW if progress (was empty, now occupied)
     *         - BOLD_BRIGHT_CYAN if stable (same content)
     *         - ORANGE if changed (different content)
     */
    private String getCellComparisonColor(Board current, Board reference, int row, int col) {
        boolean currentEmpty = current.isEmpty(row, col);
        boolean refEmpty = reference.isEmpty(row, col);

        if (refEmpty && currentEmpty) {
            // Both empty - no special color
            return "";
        } else if (!refEmpty && currentEmpty) {
            // Was occupied, now empty - REGRESSION (Magenta)
            return BOLD_BRIGHT_MAGENTA;
        } else if (refEmpty && !currentEmpty) {
            // Was empty, now occupied - PROGRESS (Yellow)
            return BOLD + YELLOW;
        } else {
            // Both occupied - compare pieces
            Placement currentP = current.getPlacement(row, col);
            Placement refP = reference.getPlacement(row, col);

            int currentPieceId = currentP.getPieceId();
            int currentRotation = currentP.getRotation();
            int refPieceId = refP.getPieceId();
            int refRotation = refP.getRotation();

            if (currentPieceId == refPieceId && currentRotation == refRotation) {
                // Identical - STABILITY (Cyan)
                return BOLD_BRIGHT_CYAN;
            } else {
                // Different piece - CHANGE (Orange)
                return ORANGE;
            }
        }
    }

    /**
     * Counts valid pieces for position from list of unused IDs.
     */
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
