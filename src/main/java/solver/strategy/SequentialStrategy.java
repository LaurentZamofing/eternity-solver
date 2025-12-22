package solver.strategy;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.SolverLogger;

import java.util.Map;

/**
 * Sequential backtracking strategy.
 * Uses single-threaded depth-first search with backtracking.
 *
 * This is the default strategy for small to medium puzzles.
 * Features:
 * - MRV (Minimum Remaining Values) heuristic
 * - AC-3 constraint propagation
 * - Singleton detection
 * - Auto-save for resume capability
 */
public class SequentialStrategy implements SolverStrategy {

    private final SolverContext context;

    public SequentialStrategy(SolverContext context) {
        this.context = context;
    }

    @Override
    public SolverResult solve() {
        long startTime = System.currentTimeMillis();
        EternitySolver solver = context.getSolver();

        if (context.getConfiguration() != null && context.getConfiguration().isVerbose()) {
            SolverLogger.info("Using SequentialStrategy (single-threaded backtracking)");
        }

        // Delegate to existing backtracking implementation
        boolean solved = solver.solveBacktracking(
            context.getBoard(),
            context.getPieces(),
            context.getPieceUsed(),
            context.getTotalPieces()
        );

        long elapsed = System.currentTimeMillis() - startTime;
        int nodesExplored = (int) solver.getStats().recursiveCalls;

        if (solved) {
            return SolverResult.success(
                context.getBoard(),
                context.getPieces(),
                elapsed,
                nodesExplored
            );
        } else {
            return SolverResult.failure(elapsed, nodesExplored);
        }
    }

    @Override
    public String getName() {
        return "Sequential Backtracking";
    }
}
