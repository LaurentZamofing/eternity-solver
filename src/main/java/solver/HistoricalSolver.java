package solver;

import util.SolverLogger;

import model.Board;
import model.Piece;
import util.SaveStateManager;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Handles solving puzzles from saved states with full backtracking history.
 *
 * <p>Extracted from EternitySolver (Refactoring #25) to separate historical solving
 * logic from standard solving, improving maintainability and testability.</p>
 *
 * <h3>Key Features</h3>
 * <ul>
 *   <li>Loads and initializes from saved state files</li>
 *   <li>Preserves full placement history for backtracking</li>
 *   <li>Accumulates compute time across sessions</li>
 *   <li>Supports backtracking through pre-loaded pieces</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <p>This solver is used when resuming from a saved state. Unlike standard solving,
 * it can backtrack through pieces that were placed in previous sessions.</p>
 *
 * <h3>Initialization Order</h3>
 * <ol>
 *   <li>Load placement history and unused pieces</li>
 *   <li>Initialize solver managers and components</li>
 *   <li>Create BacktrackingHistoryManager with initialized validator</li>
 *   <li>Attempt to solve from current state</li>
 *   <li>If failed, backtrack through history</li>
 * </ol>
 *
 * @see EternitySolver
 * @see SaveStateManager
 * @see BacktrackingHistoryManager
 */
public class HistoricalSolver {

    private final EternitySolver solver;

    public HistoricalSolver(EternitySolver solver) {
        this.solver = solver;
    }

    /**
     * Solves the puzzle by resuming from a pre-loaded state with placement history.
     * Allows backtracking through all pre-loaded pieces, not just those
     * placed during this execution.
     *
     * @param board board with pieces already placed
     * @param allPieces map of ALL pieces (used and unused)
     * @param unusedIds list of IDs of pieces not yet used
     * @param preloadedOrder complete history of placement order (to allow backtracking)
     * @return true if puzzle was solved
     */
    public boolean solveWithHistory(Board board, Map<Integer, Piece> allPieces,
                                     List<Integer> unusedIds,
                                     List<SaveStateManager.PlacementInfo> preloadedOrder) {
        // Retrieve time already accumulated from previous saves
        long previousComputeTime = SaveStateManager.readTotalComputeTime(solver.configManager.getPuzzleName());
        solver.stats.start(previousComputeTime);

        // IMPORTANT: Reset startTimeMs to NOW for timeout calculation
        // The timeout should apply to THIS session only, not cumulative time
        solver.startTimeMs = System.currentTimeMillis();

        // Initialize PlacementOrderTracker with provided history
        solver.placementOrderTracker = new PlacementOrderTracker();
        solver.placementOrderTracker.initializeWithHistory(preloadedOrder);

        // Detect fixed positions (those we should NEVER backtrack)
        // For now, no position is truly "fixed" - we can backtrack everything

        // Initialize numFixedPieces and initialFixedPieces from configuration file
        int numFixed = solver.configManager.calculateNumFixedPieces(solver.configManager.getPuzzleName());
        solver.configManager.buildInitialFixedPieces(preloadedOrder, numFixed);

        // Create pieceUsed array from unusedIds
        int totalPieces = allPieces.size();
        BitSet pieceUsed = solver.createPieceUsedBitSet(allPieces);
        for (int pid : allPieces.keySet()) {
            if (!unusedIds.contains(pid)) {
                pieceUsed.set(pid);
            }
        }

        // Initialize managers, components, and strategies FIRST
        // This ensures validator is available before creating BacktrackingHistoryManager
        solver.initializeManagers(allPieces);
        solver.initializeComponents(board, allPieces, pieceUsed, totalPieces);
        solver.initializeDomains(board, allPieces, pieceUsed, totalPieces);
        solver.initializePlacementStrategies();

        // Now create BacktrackingHistoryManager with valid validator (no redundant creation)
        BacktrackingHistoryManager backtrackingHistoryManager = new BacktrackingHistoryManager(
            solver.validator,  // validator is now properly initialized
            solver.configManager.getThreadLabel(),
            solver.stats);

        // Configure timeout for backtracking
        backtrackingHistoryManager.setTimeoutConfig(
            solver.getStartTimeMs(),
            solver.configManager.getMaxExecutionTimeMs());

        System.out.println("  → Resuming with " + preloadedOrder.size() + " pre-loaded pieces");
        SolverLogger.info("  → Backtracking can go back through ALL pieces");

        // Try to solve with current state
        boolean result = solver.solveBacktracking(board, allPieces, pieceUsed, totalPieces);

        // If failed, use BacktrackingHistoryManager to backtrack through history
        if (!result && backtrackingHistoryManager != null) {
            // Create a SequentialSolver callback that wraps solveBacktracking
            BacktrackingHistoryManager.SequentialSolver sequentialSolver =
                (b, pieces, used, total) -> solver.solveBacktracking(b, pieces, used, total);

            result = backtrackingHistoryManager.backtrackThroughHistory(
                board, allPieces, pieceUsed,
                solver.placementOrderTracker != null ? solver.placementOrderTracker.getPlacementHistory() : new ArrayList<>(),
                sequentialSolver);
        }

        solver.stats.end();
        return result;
    }
}
