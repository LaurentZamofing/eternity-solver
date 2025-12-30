package solver;

import model.Board;
import model.Piece;
import solver.display.ComparisonBoardRenderer;
import solver.display.LabeledBoardRenderer;
import solver.display.ValidPieceCounter;
import util.SaveStateManager;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facade for board visualization and display operations.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Provide simple API for board display</li>
 *   <li>Create and configure appropriate renderers</li>
 *   <li>Maintain backward compatibility with existing code</li>
 * </ul>
 *
 * <h2>Design Pattern: Facade</h2>
 * Simplifies access to complex display subsystem:
 * <ul>
 *   <li>GridStructureRenderer: ASCII grid structure</li>
 *   <li>EdgeMatchingValidator: Edge matching logic</li>
 *   <li>ValidPieceCounter: Valid piece counting</li>
 *   <li>ColorStrategy implementations: Color schemes</li>
 *   <li>CellFormatter: Cell content formatting</li>
 *   <li>AbstractBoardRenderer + implementations: Template Method rendering</li>
 * </ul>
 *
 * <h2>Refactoring History</h2>
 * Originally 346 lines with 150-line method and 97% duplication.
 * Refactored using Strategy and Template Method patterns.
 * Result: 6 extracted components, 12 testable classes, facade reduced to ~50 lines.
 *
 * <h3>Phase 5 Consolidation (2025)</h3>
 * This class remains for backward compatibility.
 * New code should use {@link solver.display.BoardDisplayService} which provides
 * a simpler, more unified API for all visualization needs.
 *
 * @author Eternity Solver Team
 * @version 2.0.0
 * @see solver.display.BoardDisplayService
 */
public class BoardDisplayManager {

    private final Set<String> fixedPositions;
    private final PlacementValidator validator;

    /**
     * Creates display manager with fixed positions and placement validator.
     *
     * @param fixedPositions Set of fixed position keys ("row,col") for hint pieces
     * @param validator Placement validator for edge checking
     */
    public BoardDisplayManager(Set<String> fixedPositions, PlacementValidator validator) {
        this.fixedPositions = fixedPositions;
        this.validator = validator;
    }

    /**
     * Displays board with labels and color-coded edge matching.
     *
     * <h3>Color Scheme</h3>
     * <ul>
     *   <li>Bright Cyan: Fixed positions (hints)</li>
     *   <li>Green: Matching edges</li>
     *   <li>Red: Mismatched edges</li>
     *   <li>Bright Red: Deadends (0 valid pieces)</li>
     *   <li>Bright Yellow: Critical (1-5 valid pieces)</li>
     *   <li>Yellow: Warning (6-20 valid pieces)</li>
     * </ul>
     *
     * @param board Board to display
     * @param piecesById Map of all pieces by ID
     * @param unusedIds List of unused piece IDs
     */
    public void printBoardWithLabels(Board board, Map<Integer, Piece> piecesById, List<Integer> unusedIds) {
        printBoardWithLabels(board, piecesById, unusedIds, null, null, null);
    }

    /**
     * Displays board with labels, highlighting last placed piece and next target cell.
     *
     * <h3>Color Scheme</h3>
     * <ul>
     *   <li>Bright Magenta: Last placed piece (highlighted borders)</li>
     *   <li>Bold Blue: Next target cell (highlighted)</li>
     *   <li>Bright Cyan: Fixed positions (hints)</li>
     *   <li>Green: Matching edges</li>
     *   <li>Red: Mismatched edges</li>
     *   <li>Bright Red: Deadends (0 valid pieces)</li>
     *   <li>Bright Yellow: Critical (1-5 valid pieces)</li>
     *   <li>Yellow: Warning (6-20 valid pieces)</li>
     * </ul>
     *
     * @param board Board to display
     * @param piecesById Map of all pieces by ID
     * @param unusedIds List of unused piece IDs
     * @param lastPlacement Last placed piece info (can be null)
     * @param nextCell Next target cell [row, col] (can be null)
     * @param placementOrderMap Map of position to placement order (can be null)
     */
    public void printBoardWithLabels(Board board, Map<Integer, Piece> piecesById, List<Integer> unusedIds,
                                     SaveStateManager.PlacementInfo lastPlacement, int[] nextCell,
                                     java.util.Map<String, Integer> placementOrderMap) {
        // Create valid piece counter
        ValidPieceCounter validPieceCounter = new ValidPieceCounter(validator);

        // Build set of highlighted positions
        Set<String> highlightedPositions = new HashSet<>();
        if (lastPlacement != null) {
            highlightedPositions.add(lastPlacement.row + "," + lastPlacement.col);
        }
        if (nextCell != null) {
            highlightedPositions.add(nextCell[0] + "," + nextCell[1]);
        }

        // Create renderer with edge matching and valid count colors
        LabeledBoardRenderer renderer = new LabeledBoardRenderer(
            board, piecesById, unusedIds, validator, validPieceCounter, fixedPositions,
            highlightedPositions, placementOrderMap
        );

        // Render board
        renderer.render();
    }

    /**
     * Displays board comparison showing differences from reference board.
     *
     * <h3>Color Scheme</h3>
     * <ul>
     *   <li>Bold Magenta: Regression (piece removed)</li>
     *   <li>Bold Yellow: Progress (new piece placed)</li>
     *   <li>Bold Cyan: Stability (unchanged piece)</li>
     *   <li>Bold Orange: Change (different piece or rotation)</li>
     *   <li>Bright Red: Deadends in unchanged empty cells</li>
     *   <li>Bright Yellow: Critical positions in unchanged empty cells</li>
     * </ul>
     *
     * @param currentBoard Current board state
     * @param referenceBoard Reference board to compare against
     * @param piecesById Map of all pieces by ID
     * @param unusedIds List of unused piece IDs
     */
    public void printBoardWithComparison(Board currentBoard, Board referenceBoard,
                                          Map<Integer, Piece> piecesById, List<Integer> unusedIds) {
        // Create valid piece counter
        ValidPieceCounter validPieceCounter = new ValidPieceCounter(validator);

        // Create renderer with comparison colors
        ComparisonBoardRenderer renderer = new ComparisonBoardRenderer(
            currentBoard, referenceBoard, piecesById, unusedIds, validPieceCounter
        );

        // Render board
        renderer.render();
    }
}
