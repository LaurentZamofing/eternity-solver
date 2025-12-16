package solver.display;

import model.Board;
import model.Piece;

import java.util.List;
import java.util.Map;

/**
 * Board renderer with comparison visualization.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Highlight regressions (pieces removed) in bold magenta</li>
 *   <li>Highlight progress (new pieces) in bold yellow</li>
 *   <li>Highlight stability (unchanged pieces) in bold cyan</li>
 *   <li>Highlight changes (different pieces) in bold orange</li>
 *   <li>Show valid counts for unchanged empty cells</li>
 * </ul>
 *
 * <h2>Design</h2>
 * Extends AbstractBoardRenderer and uses ComparisonColorStrategy for cells.
 * Falls back to ValidCountColorStrategy for empty cells that were empty in reference.
 *
 * <h2>Usage</h2>
 * <pre>
 * ComparisonBoardRenderer renderer = new ComparisonBoardRenderer(
 *     currentBoard, referenceBoard, pieces, unusedIds, counter
 * );
 * renderer.render();
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class ComparisonBoardRenderer extends AbstractBoardRenderer {

    private final Board referenceBoard;
    private final CellFormatter comparisonFormatter;
    private final CellFormatter validCountFormatter;

    /**
     * Creates comparison board renderer.
     *
     * @param currentBoard Current board state
     * @param referenceBoard Reference board to compare against
     * @param piecesById Map of all pieces by ID
     * @param unusedIds List of unused piece IDs
     * @param validPieceCounter Counter for valid pieces
     */
    public ComparisonBoardRenderer(Board currentBoard, Board referenceBoard,
                                    Map<Integer, Piece> piecesById, List<Integer> unusedIds,
                                    ValidPieceCounter validPieceCounter) {
        super(currentBoard, piecesById, unusedIds, validPieceCounter);
        this.referenceBoard = referenceBoard;

        // Color strategy for comparison (regression/progress/stability/change)
        ComparisonColorStrategy comparisonColorStrategy =
            new ComparisonColorStrategy(referenceBoard);
        this.comparisonFormatter = new CellFormatter(comparisonColorStrategy);

        // Color strategy for empty cells that were empty in reference
        ValidCountColorStrategy validCountColorStrategy =
            new ValidCountColorStrategy(validPieceCounter, piecesById, unusedIds);
        this.validCountFormatter = new CellFormatter(validCountColorStrategy);
    }

    @Override
    protected String formatNorthEdge(int row, int col) {
        // Use comparison formatter for all cells
        return comparisonFormatter.formatNorthEdge(board, row, col);
    }

    @Override
    protected String formatMiddleContent(int row, int col) {
        if (board.isEmpty(row, col)) {
            // Empty cell - show valid count
            int validCount = validPieceCounter.countValidPieces(
                board, row, col, piecesById, unusedIds
            );

            // Check if this is a regression (was occupied in reference)
            if (!referenceBoard.isEmpty(row, col)) {
                // Regression - use comparison color (magenta)
                return comparisonFormatter.formatEmptyCell(board, row, col, validCount);
            } else {
                // Was empty in reference too - use valid count coloring
                return validCountFormatter.formatEmptyCell(board, row, col, validCount);
            }
        } else {
            // Occupied cell - use comparison formatting
            return comparisonFormatter.formatMiddleLine(board, row, col);
        }
    }

    @Override
    protected String formatSouthEdge(int row, int col) {
        // Use comparison formatter for all cells
        return comparisonFormatter.formatSouthEdge(board, row, col);
    }
}
