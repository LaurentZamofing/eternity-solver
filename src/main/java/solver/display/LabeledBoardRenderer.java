package solver.display;

import model.Board;
import model.Piece;
import solver.PlacementValidator;
import util.PositionKey;

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
     * @param fixedPositions Set of fixed position keys (PositionKey)
     */
    public LabeledBoardRenderer(Board board, Map<Integer, Piece> piecesById,
                                 List<Integer> unusedIds, PlacementValidator validator,
                                 ValidPieceCounter validPieceCounter, Set<PositionKey> fixedPositions) {
        this(board, piecesById, unusedIds, validator, validPieceCounter, fixedPositions, null);
    }

    /**
     * Creates labeled board renderer with highlighted positions.
     *
     * @param board Board to render
     * @param piecesById Map of all pieces by ID
     * @param unusedIds List of unused piece IDs
     * @param validator Placement validator for edge checking
     * @param validPieceCounter Counter for valid pieces
     * @param fixedPositions Set of fixed position keys (PositionKey)
     * @param highlightedPositions Set of positions to highlight (PositionKey)
     */
    public LabeledBoardRenderer(Board board, Map<Integer, Piece> piecesById,
                                 List<Integer> unusedIds, PlacementValidator validator,
                                 ValidPieceCounter validPieceCounter, Set<PositionKey> fixedPositions,
                                 Set<PositionKey> highlightedPositions) {
        this(board, piecesById, unusedIds, validator, validPieceCounter, fixedPositions, highlightedPositions, null);
    }

    /**
     * Creates labeled board renderer with highlighted positions and placement order.
     *
     * @param board Board to render
     * @param piecesById Map of all pieces by ID
     * @param unusedIds List of unused piece IDs
     * @param validator Placement validator for edge checking
     * @param validPieceCounter Counter for valid pieces
     * @param fixedPositions Set of fixed position keys (PositionKey)
     * @param highlightedPositions Set of positions to highlight (PositionKey) - shown in magenta
     * @param placementOrderMap Map of position to placement order number (null = no order)
     */
    public LabeledBoardRenderer(Board board, Map<Integer, Piece> piecesById,
                                 List<Integer> unusedIds, PlacementValidator validator,
                                 ValidPieceCounter validPieceCounter, Set<PositionKey> fixedPositions,
                                 Set<PositionKey> highlightedPositions, java.util.Map<PositionKey, Integer> placementOrderMap) {
        this(board, piecesById, unusedIds, validator, validPieceCounter, fixedPositions,
             highlightedPositions, null, placementOrderMap);
    }

    /**
     * Creates labeled board renderer with highlighted, next cell, and placement order.
     *
     * @param board Board to render
     * @param piecesById Map of all pieces by ID
     * @param unusedIds List of unused piece IDs
     * @param validator Placement validator for edge checking
     * @param validPieceCounter Counter for valid pieces
     * @param fixedPositions Set of fixed position keys (PositionKey)
     * @param highlightedPositions Set of positions to highlight (PositionKey) - shown in magenta
     * @param nextCellPositions Set of next cells to process (PositionKey) - shown in blue
     * @param placementOrderMap Map of position to placement order number (null = no order)
     */
    public LabeledBoardRenderer(Board board, Map<Integer, Piece> piecesById,
                                 List<Integer> unusedIds, PlacementValidator validator,
                                 ValidPieceCounter validPieceCounter, Set<PositionKey> fixedPositions,
                                 Set<PositionKey> highlightedPositions, Set<PositionKey> nextCellPositions,
                                 java.util.Map<PositionKey, Integer> placementOrderMap) {
        this(board, piecesById, unusedIds, validator, validPieceCounter, fixedPositions,
             highlightedPositions, nextCellPositions, null, placementOrderMap);
    }

    /**
     * Creates labeled board renderer with highlighted, next cell, removed cell, and placement order.
     *
     * @param board Board to render
     * @param piecesById Map of all pieces by ID
     * @param unusedIds List of unused piece IDs
     * @param validator Placement validator for edge checking
     * @param validPieceCounter Counter for valid pieces
     * @param fixedPositions Set of fixed position keys (PositionKey)
     * @param highlightedPositions Set of positions to highlight (PositionKey) - shown in magenta
     * @param nextCellPositions Set of next cells to process (PositionKey) - shown in blue
     * @param removedCellPositions Set of removed cells during backtrack (PositionKey) - shown in red with ⬅
     * @param placementOrderMap Map of position to placement order number (null = no order)
     */
    public LabeledBoardRenderer(Board board, Map<Integer, Piece> piecesById,
                                 List<Integer> unusedIds, PlacementValidator validator,
                                 ValidPieceCounter validPieceCounter, Set<PositionKey> fixedPositions,
                                 Set<PositionKey> highlightedPositions, Set<PositionKey> nextCellPositions,
                                 Set<PositionKey> removedCellPositions,
                                 java.util.Map<PositionKey, Integer> placementOrderMap) {
        super(board, piecesById, unusedIds, validPieceCounter);

        // Color strategy for occupied cells (edge matching + fixed highlighting + highlights + next cell)
        EdgeMatchingColorStrategy occupiedColorStrategy =
            new EdgeMatchingColorStrategy(board, fixedPositions, highlightedPositions, nextCellPositions, removedCellPositions);
        this.occupiedCellFormatter = new CellFormatter(occupiedColorStrategy, placementOrderMap);

        // Color strategy for empty cells (valid count warnings + highlights + next cell + removed cells)
        ValidCountColorStrategy emptyColorStrategy =
            new ValidCountColorStrategy(validPieceCounter, piecesById, unusedIds, highlightedPositions, nextCellPositions, removedCellPositions);
        this.emptyCellFormatter = new CellFormatter(emptyColorStrategy, null); // No order for empty cells
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
            // Show valid piece count AND rotation count for empty cells
            ValidPieceCounter.ValidCountResult countResult =
                validPieceCounter.countValidPiecesAndRotations(board, row, col, piecesById, unusedIds);
            return emptyCellFormatter.formatEmptyCellWithRotations(
                board, row, col, countResult.numPieces, countResult.numRotations
            );
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
