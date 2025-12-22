package solver.strategy;

import util.SaveStateManager;

import java.util.List;

/**
 * Factory for creating solver strategies.
 * Provides convenient methods for common strategy configurations.
 *
 * Usage:
 * <pre>
 * // Sequential solving
 * SolverStrategy strategy = StrategyFactory.sequential(context);
 *
 * // Parallel solving with 4 threads
 * SolverStrategy strategy = StrategyFactory.parallel(context, 4);
 *
 * // Resume from saved state
 * SolverStrategy strategy = StrategyFactory.historical(context, unusedIds, order);
 * </pre>
 */
public class StrategyFactory {

    /**
     * Creates a sequential backtracking strategy.
     *
     * @param context Solver context with board and configuration
     * @return SequentialStrategy instance
     */
    public static SolverStrategy sequential(SolverContext context) {
        return new SequentialStrategy(context);
    }

    /**
     * Creates a parallel strategy with specified thread count.
     *
     * @param context Solver context
     * @param numThreads Number of threads to use
     * @return ParallelStrategy instance
     */
    public static SolverStrategy parallel(SolverContext context, int numThreads) {
        return new ParallelStrategy(context, numThreads);
    }

    /**
     * Creates a parallel strategy with default thread count (CPU cores - 1).
     *
     * @param context Solver context
     * @return ParallelStrategy instance
     */
    public static SolverStrategy parallelAuto(SolverContext context) {
        return new ParallelStrategy(context);
    }

    /**
     * Creates a historical strategy for resuming from saved state.
     *
     * @param context Solver context
     * @param unusedIds List of unused piece IDs
     * @param preloadedOrder Previous placement order
     * @return HistoricalStrategy instance
     */
    public static SolverStrategy historical(
            SolverContext context,
            List<Integer> unusedIds,
            List<SaveStateManager.PlacementInfo> preloadedOrder) {
        return new HistoricalStrategy(context, unusedIds, preloadedOrder);
    }

    /**
     * Creates an appropriate strategy based on context.
     * Decision logic:
     * - If saved state exists: HistoricalStrategy
     * - If puzzle is large (>100 pieces): ParallelStrategy
     * - Otherwise: SequentialStrategy
     *
     * @param context Solver context
     * @return Appropriate strategy for the context
     */
    public static SolverStrategy auto(SolverContext context) {
        int totalPieces = context.getTotalPieces();

        // For large puzzles, prefer parallel
        if (totalPieces > 100) {
            return parallelAuto(context);
        }

        // Default to sequential for small/medium puzzles
        return sequential(context);
    }
}
