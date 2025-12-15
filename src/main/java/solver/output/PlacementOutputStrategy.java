package solver.output;

import model.Board;
import model.Piece;
import solver.BacktrackingContext;
import solver.EternitySolver;

import java.util.Map;

/**
 * Strategy pattern for outputting placement information during solving.
 * Separates logging/UI concerns from core algorithm logic.
 *
 * Implementations:
 * - VerbosePlacementOutput: Detailed step-by-step logging for debugging
 * - QuietPlacementOutput: Minimal/no output for production runs
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public interface PlacementOutputStrategy {

    /**
     * Logs information about cell selection and constraints.
     *
     * @param context Backtracking context
     * @param solver Solver instance
     * @param row Selected row
     * @param col Selected column
     * @param uniquePieces Number of unique valid pieces for this cell
     * @param availableCount Total available pieces
     */
    void logCellSelection(BacktrackingContext context, EternitySolver solver,
                         int row, int col, int uniquePieces, int availableCount);

    /**
     * Logs information about attempting to place a specific piece.
     *
     * @param pieceId Piece ID being tested
     * @param rotation Rotation (0-3)
     * @param row Target row
     * @param col Target column
     * @param optionIndex Current piece index in iteration
     * @param totalOptions Total pieces being tried
     * @param edges Rotated edges [N, E, S, W]
     */
    void logPlacementAttempt(int pieceId, int rotation, int row, int col,
                            int optionIndex, int totalOptions, int[] edges);

    /**
     * Logs rejection due to edge constraints.
     */
    void logEdgeRejection();

    /**
     * Logs rejection due to symmetry breaking.
     */
    void logSymmetryRejection();

    /**
     * Logs successful constraint satisfaction before placement.
     */
    void logConstraintsSatisfied();

    /**
     * Logs successful placement and board state.
     *
     * @param context Backtracking context
     * @param solver Solver instance
     * @param row Placed row
     * @param col Placed column
     */
    void logSuccessfulPlacement(BacktrackingContext context, EternitySolver solver,
                               int row, int col);

    /**
     * Logs AC-3 dead-end detection.
     *
     * @param pieceId Piece that caused dead-end
     */
    void logAC3DeadEnd(int pieceId);

    /**
     * Logs timeout reached.
     *
     * @param pieceId Piece being placed when timeout occurred
     * @param row Row position
     * @param col Column position
     */
    void logTimeout(int pieceId, int row, int col);

    /**
     * Logs backtracking event.
     *
     * @param pieceId Piece being removed
     * @param row Row position
     * @param col Column position
     * @param totalBacktracks Total backtracks so far
     */
    void logBacktrack(int pieceId, int row, int col, int totalBacktracks);

    /**
     * Logs exhaustion of all options for a cell.
     *
     * @param row Row position
     * @param col Column position
     * @param attemptedPieces Number of pieces attempted
     */
    void logExhaustedOptions(int row, int col, int attemptedPieces);

    /**
     * Waits for user input (for step-by-step debugging).
     * Should be no-op in quiet mode.
     */
    void waitForUser();
}
