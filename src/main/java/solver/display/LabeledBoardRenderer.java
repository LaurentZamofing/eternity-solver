package solver.display;

import model.Board;
import model.Piece;
import solver.PlacementValidator;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Board renderer with edge matching and valid count visualization.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Render occupied cells with edge matching colors (green/red)</li>
 *   <li>Highlight fixed positions in bright cyan</li>
 *   <li>Render empty cells with valid count colors (red/yellow for deadends)</li>
 * </ul>
 *
 * <h2>Design</h2>
 * Extends AbstractBoardRenderer and provides dual coloring:
 * - Occupied cells: EdgeMatchingColorStrategy (green/red edge matching)
 * - Empty cells: ValidCountColorStrategy (red/yellow deadend warnings)
 *
 * <h2>Usage</h2>
 * <pre>
 * LabeledBoardRenderer renderer = new LabeledBoardRenderer(
 *     board, pieces, unusedIds, validator, counter, fixedPositions
 * );
 * renderer.render();
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class LabeledBoardRenderer extends AbstractBoardRenderer {

    private final CellFormatter occupiedCellFormatter;
    private final CellFormatter emptyCellFormatter;

    /**
     * Creates labeled board renderer with edge matching and valid count colors.
     *
     * @param board Board to render
     * @param piecesById Map of all pieces by ID
     * @param unusedIds List of unused piece IDs
     * @param validator Placement validator for edge checking
     * @param validPieceCounter Counter for valid pieces
     * @param fixedPositions Set of fixed position keys ("row,col")
     */
    public LabeledBoardRenderer(Board board, Map<Integer, Piece> piecesById,
                                 List<Integer> unusedIds, PlacementValidator validator,
                                 ValidPieceCounter validPieceCounter, Set<String> fixedPositions) {
        super(board, piecesById, unusedIds, validPieceCounter);

        // Color strategy for occupied cells (edge matching + fixed highlighting)
        EdgeMatchingColorStrategy occupiedColorStrategy =
            new EdgeMatchingColorStrategy(board, fixedPositions);
        this.occupiedCellFormatter = new CellFormatter(occupiedColorStrategy);

        // Color strategy for empty cells (valid count warnings)
        ValidCountColorStrategy emptyColorStrategy =
            new ValidCountColorStrategy(validPieceCounter, piecesById, unusedIds);
        this.emptyCellFormatter = new CellFormatter(emptyColorStrategy);
    }

    @Override
    protected String formatNorthEdge(int row, int col) {
        if (board.isEmpty(row, col)) {
            return emptyCellFormatter.formatNorthEdge(board, row, col);
        } else {
            return occupiedCellFormatter.formatNorthEdge(board, row, col);
        }
    }

    @Override
    protected String formatMiddleContent(int row, int col) {
        if (board.isEmpty(row, col)) {
            // Show valid piece count for empty cells
            int validCount = validPieceCounter.countValidPieces(
                board, row, col, piecesById, unusedIds
            );
            return emptyCellFormatter.formatEmptyCell(board, row, col, validCount);
        } else {
            return occupiedCellFormatter.formatMiddleLine(board, row, col);
        }
    }

    @Override
    protected String formatSouthEdge(int row, int col) {
        if (board.isEmpty(row, col)) {
            return emptyCellFormatter.formatSouthEdge(board, row, col);
        } else {
            return occupiedCellFormatter.formatSouthEdge(board, row, col);
        }
    }
}
