package solver.strategy;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.SaveStateManager;
import util.SolverLogger;

import java.util.List;
import java.util.Map;

/**
 * Historical/resume strategy for continuing from saved state.
 * Loads previous placement order and continues search from that point.
 *
 * Features:
 * - Resume from auto-saved state
 * - Preserves placement history
 * - Maintains fixed pieces
 * - Can switch strategies after resume
 *
 * Usage:
 * <pre>
 * HistoricalStrategy strategy = new HistoricalStrategy(
 *     context,
 *     unusedPieceIds,
 *     preloadedOrder
 * );
 * SolverResult result = strategy.solve();
 * </pre>
 */
public class HistoricalStrategy implements SolverStrategy {

    private final SolverContext context;
    private final List<Integer> unusedIds;
    private final List<SaveStateManager.PlacementInfo> preloadedOrder;

    /**
     * Creates historical strategy with saved state information.
     *
     * @param context Solver context
     * @param unusedIds List of piece IDs not yet placed
     * @param preloadedOrder Previous placement order to restore
     */
    public HistoricalStrategy(
            SolverContext context,
            List<Integer> unusedIds,
            List<SaveStateManager.PlacementInfo> preloadedOrder) {
        this.context = context;
        this.unusedIds = unusedIds;
        this.preloadedOrder = preloadedOrder;
    }

    @Override
    public SolverResult solve() {
        long startTime = System.currentTimeMillis();
        EternitySolver solver = context.getSolver();

        if (context.getConfiguration() != null && context.getConfiguration().isVerbose()) {
            SolverLogger.info("Using HistoricalStrategy (resuming from saved state)");
            SolverLogger.info("  - Preloaded placements: " + preloadedOrder.size());
            SolverLogger.info("  - Unused pieces: " + unusedIds.size());
        }

        // Delegate to existing historical implementation
        boolean solved = solver.solveWithHistory(
            context.getBoard(),
            context.getPieces(),
            unusedIds,
            preloadedOrder
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
        return "Historical Resume";
    }

    public List<Integer> getUnusedIds() {
        return unusedIds;
    }

    public List<SaveStateManager.PlacementInfo> getPreloadedOrder() {
        return preloadedOrder;
    }
}
