package solver;

import util.SolverLogger;

import model.Board;
import model.Piece;
import util.SaveManager;
import util.SaveStateManager;
import util.TimeConstants;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Core backtracking algorithm for solving Eternity puzzles.
 *
 * <p>Extracted from EternitySolver (Refactoring #16) to separate backtracking logic
 * from solver coordination, reducing complexity and improving testability.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Recursive backtracking algorithm execution</li>
 *   <li>Record tracking and display coordination</li>
 *   <li>Auto-save coordination (thread state + periodic saves)</li>
 *   <li>Timeout enforcement</li>
 *   <li>Strategy execution (singleton-first, then MRV)</li>
 *   <li>Solution detection and signaling across threads</li>
 * </ul>
 *
 * <h3>Thread Safety</h3>
 * <p>Designed for multi-threaded use with shared AtomicBoolean for solution signaling.
 * Each thread should have its own BacktrackingSolver instance.</p>
 *
 * <h3>Performance Optimizations</h3>
 * <ul>
 *   <li>Uses BitSet.cardinality() for O(1) depth calculation</li>
 *   <li>Exception handling around I/O prevents solver crashes</li>
 *   <li>Early termination when solution found by another thread</li>
 * </ul>
 *
 * <h3>Optional Dependencies</h3>
 * <p>RecordManager and AutoSaveManager can be null - gracefully degraded functionality.</p>
 *
 * @see EternitySolver
 * @see RecordManager
 * @see AutoSaveManager
 */
public class BacktrackingSolver {

    // Core dependencies
    private final EternitySolver solver;
    private final StatisticsManager stats;
    private final AtomicBoolean solutionFound;
    private final SolverConfiguration config;

    // Optional managers (can be null)
    private final RecordManager recordManager;
    private final AutoSaveManager autoSaveManager;

    // Placement strategies
    private final SingletonPlacementStrategy singletonStrategy;
    private final MRVPlacementStrategy mrvStrategy;

    // Thread management
    private final int threadId;
    private final long randomSeed;
    private long lastThreadSaveTime;

    // Timing
    private final long startTimeMs;

    // Constants
    private static final long THREAD_SAVE_INTERVAL = SolverConstants.THREAD_SAVE_INTERVAL_MS;

    /**
     * Creates a BacktrackingSolver with all required dependencies.
     *
     * @param solver reference to EternitySolver for callback methods
     * @param stats statistics tracker
     * @param solutionFound atomic flag for multi-threaded solution signaling
     * @param config solver configuration (immutable)
     * @param recordManager record manager (can be null)
     * @param autoSaveManager auto-save manager (can be null)
     * @param singletonStrategy singleton placement strategy
     * @param mrvStrategy MRV placement strategy
     * @param threadId thread ID for this solver (-1 if single-threaded)
     * @param randomSeed random seed for reproducibility
     * @param startTimeMs start time in milliseconds for timeout checking
     */
    public BacktrackingSolver(
            EternitySolver solver,
            StatisticsManager stats,
            AtomicBoolean solutionFound,
            SolverConfiguration config,
            RecordManager recordManager,
            AutoSaveManager autoSaveManager,
            SingletonPlacementStrategy singletonStrategy,
            MRVPlacementStrategy mrvStrategy,
            int threadId,
            long randomSeed,
            long startTimeMs) {

        // Validate required parameters (null checks)
        if (solver == null) {
            throw new IllegalArgumentException("solver cannot be null");
        }
        if (stats == null) {
            throw new IllegalArgumentException("stats cannot be null");
        }
        if (solutionFound == null) {
            throw new IllegalArgumentException("solutionFound cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        if (singletonStrategy == null) {
            throw new IllegalArgumentException("singletonStrategy cannot be null");
        }
        if (mrvStrategy == null) {
            throw new IllegalArgumentException("mrvStrategy cannot be null");
        }
        // Note: recordManager and autoSaveManager can be null (optional features)

        this.solver = solver;
        this.stats = stats;
        this.solutionFound = solutionFound;
        this.config = config;
        this.recordManager = recordManager;
        this.autoSaveManager = autoSaveManager;
        this.singletonStrategy = singletonStrategy;
        this.mrvStrategy = mrvStrategy;
        this.threadId = threadId;
        this.randomSeed = randomSeed;
        this.startTimeMs = startTimeMs;
        this.lastThreadSaveTime = 0;
    }

    /**
     * Solves the puzzle using recursive backtracking.
     *
     * This method implements the backtracking algorithm with:
     * - Depth record tracking
     * - Periodic saves (thread state + auto-save)
     * - Timeout checking
     * - Placement strategies (singleton first, then MRV)
     * - Solution signaling to other threads
     *
     * @param board board modified in place
     * @param piecesById map of original pieces by ID
     * @param pieceUsed array of used pieces
     * @param totalPieces total number of pieces
     * @return true if a solution was found
     */
    public boolean solve(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        stats.recursiveCalls++;

        // Check if another thread has found the solution
        if (solutionFound.get()) {
            return false; // Stop this branch
        }

        // Check if we've reached a new depth record
        // IMPORTANT: exclude fixed pieces from calculation (count only pieces placed by backtracking)
        // Optimization: Use BitSet.cardinality() instead of manual loop (O(1) vs O(n))
        int usedCount = pieceUsed.cardinality();
        int currentDepth = usedCount - config.getNumFixedPieces();

        // Check and update records using RecordManager
        if (recordManager != null) {
            RecordManager.RecordCheckResult recordResult =
                recordManager.checkAndUpdateRecord(board, piecesById, currentDepth, stats.backtracks);

            if (recordResult != null) {
                // Save record to disk if new record achieved
                // Note: Auto-save is optional - record is saved only if autoSaveManager is provided
                // This allows running without save functionality for testing or memory-constrained scenarios
                if (autoSaveManager != null) {
                    autoSaveManager.saveRecord(board, pieceUsed, totalPieces, stats, currentDepth);
                } // Intentionally silent if null - save is optional feature

                // Display record if it should be shown
                if (recordManager.shouldShowRecord(recordResult, currentDepth)) {
                    // Calculate unused piece IDs for board visualization
                    List<Integer> unusedIds = new ArrayList<>();
                    for (int id = 1; id < totalPieces + 1; id++) {
                        if (!pieceUsed.get(id)) {
                            unusedIds.add(id);
                        }
                    }

                    // Get last placed piece position for visual highlight
                    SaveStateManager.PlacementInfo lastPlacement =
                        (solver.placementOrderTracker != null) ? solver.placementOrderTracker.getLastPlacement() : null;

                    // Find next empty cell to try
                    int[] nextCell = solver.findNextCellMRV(board, piecesById, pieceUsed, totalPieces);

                    // Display record with board visualization
                    recordManager.displayRecord(recordResult, usedCount, stats,
                        board, piecesById, unusedIds,
                        config.getFixedPositions(), solver.validator,
                        lastPlacement, nextCell);
                }
            }
        }

        // Periodic thread state save (every 5 minutes)
        long currentTime = System.currentTimeMillis();
        if (threadId >= 0 && (currentTime - lastThreadSaveTime > THREAD_SAVE_INTERVAL)) {
            lastThreadSaveTime = currentTime;
            try {
                SaveManager.saveThreadState(board, piecesById, currentDepth, threadId, randomSeed);
            } catch (RuntimeException e) {
                // Log error but don't crash the solver - saving is optional
                SolverLogger.warn("Error saving thread " + threadId + " state: " + e.getMessage());
            }
        }

        // Periodic auto-save (every 60 seconds)
        if (autoSaveManager != null) {
            autoSaveManager.checkAndSave(board, pieceUsed, totalPieces, stats);
        }

        // Periodic stats logging (every 10 seconds)
        if (autoSaveManager != null) {
            autoSaveManager.checkAndLogStats(pieceUsed, totalPieces, stats);
        }

        // Note: Timeout check removed - now only checked after successful placement
        // This prevents interruption during backtracking, ensuring saved states
        // always contain stable configurations with implicit progress information

        // Check if any empty cells remain
        int[] cell = solver.findNextCellMRV(board, piecesById, pieceUsed, totalPieces);
        if (cell == null) {
            // No empty cells -> solution found
            solutionFound.set(true); // Signal to other threads
            stats.end();
            if (config.isVerbose()) {
                SolverLogger.info("\n========================================");
                SolverLogger.info("SOLUTION FOUND!");
                SolverLogger.info("========================================");
            }
            return true;
        }

        // Create backtracking context for strategies
        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, totalPieces, stats, config.getNumFixedPieces(),
            startTimeMs, config.getMaxExecutionTimeMs()
        );

        // STEP 1: Try singleton placement strategy first (most constrained)
        if (singletonStrategy.tryPlacement(context, solver)) {
            return true;
        }

        // STEP 2: Try MRV placement strategy
        return mrvStrategy.tryPlacement(context, solver);
    }
}
