package solver.strategy;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.SolverLogger;

import java.util.Map;

/**
 * Parallel search strategy using multiple threads.
 * Distributes work across threads with work-stealing and diversification.
 *
 * Features:
 * - Multi-threaded search with configurable thread count
 * - Work-stealing for load balancing
 * - Diversification strategies (different sort orders, random seeds)
 * - Shared best solution tracking
 *
 * Best for large puzzles (16x16+) where sequential search would take too long.
 */
public class ParallelStrategy implements SolverStrategy {

    private final SolverContext context;
    private final int numThreads;

    /**
     * Creates parallel strategy with specified thread count.
     *
     * @param context Solver context with board and configuration
     * @param numThreads Number of threads to use (typically CPU cores - 1)
     */
    public ParallelStrategy(SolverContext context, int numThreads) {
        this.context = context;
        this.numThreads = numThreads;
    }

    /**
     * Creates parallel strategy with default thread count (available processors - 1).
     */
    public ParallelStrategy(SolverContext context) {
        this(context, Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    }

    @Override
    public SolverResult solve() {
        long startTime = System.currentTimeMillis();
        EternitySolver solver = context.getSolver();

        if (context.getConfiguration() != null && context.getConfiguration().isVerbose()) {
            SolverLogger.info("Using ParallelStrategy with " + numThreads + " threads");
        }

        // Extract available pieces from context
        Map<Integer, Piece> availablePieces = context.getPieces();

        // Delegate to existing parallel implementation
        boolean solved = solver.solveParallel(
            context.getBoard(),
            context.getPieces(),
            availablePieces,
            numThreads
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
        return "Parallel Search (" + numThreads + " threads)";
    }

    public int getNumThreads() {
        return numThreads;
    }
}
