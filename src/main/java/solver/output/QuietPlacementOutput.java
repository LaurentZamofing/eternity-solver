package solver.output;

import solver.BacktrackingContext;
import solver.EternitySolver;

/**
 * Quiet (no-op) implementation of PlacementOutputStrategy.
 * Produces no output for production runs where performance is critical.
 *
 * This is the default strategy for:
 * - Batch solving
 * - Parallel execution
 * - Production environments
 * - Automated testing
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class QuietPlacementOutput implements PlacementOutputStrategy {

    @Override
    public void logCellSelection(BacktrackingContext context, EternitySolver solver,
                                int row, int col, int uniquePieces, int availableCount) {
        // No-op: silent execution
    }

    @Override
    public void logPlacementAttempt(int pieceId, int rotation, int row, int col,
                                   int optionIndex, int totalOptions, int[] edges) {
        // No-op: silent execution
    }

    @Override
    public void logEdgeRejection() {
        // No-op: silent execution
    }

    @Override
    public void logSymmetryRejection() {
        // No-op: silent execution
    }

    @Override
    public void logConstraintsSatisfied() {
        // No-op: silent execution
    }

    @Override
    public void logSuccessfulPlacement(BacktrackingContext context, EternitySolver solver,
                                      int row, int col) {
        // No-op: silent execution
    }

    @Override
    public void logAC3DeadEnd(int pieceId) {
        // No-op: silent execution
    }

    @Override
    public void logTimeout(int pieceId, int row, int col) {
        // No-op: silent execution
    }

    @Override
    public void logBacktrack(int pieceId, int row, int col, int totalBacktracks) {
        // No-op: silent execution
    }

    @Override
    public void logExhaustedOptions(int row, int col, int attemptedPieces) {
        // No-op: silent execution
    }

    @Override
    public void waitForUser() {
        // No-op: never wait in quiet mode
    }
}
