package solver;

import util.SolverLogger;

import model.Board;
import model.Piece;
import solver.BoardVisualizer;
import solver.heuristics.HeuristicStrategy;
import solver.heuristics.LeastConstrainingValueOrderer;
import solver.heuristics.MRVCellSelector;
import util.SaveStateManager;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Edge-matching puzzle solver (Eternity type) using backtracking.
 *
 * Limitations:
 * - Naive backtracking (sufficient for small instances 3x3, 4x4)
 * - For real 16x16 instances, would need to add:
 *   - MRV (Minimum Remaining Values) heuristic
 *   - Forward-checking
 *   - Arc propagation (AC-3)
 *   - Meta-heuristic methods
 */
public class EternitySolver implements Solver {

    /** Returns a fluent builder for configuring a new solver instance. */
    public static EternitySolverBuilder builder() {
        return new EternitySolverBuilder();
    }


    /** Type alias for backward compatibility - delegates to StatisticsManager. */
    public static class Statistics extends StatisticsManager { }

    Statistics stats = new Statistics();
    long startTimeMs = 0;  // Package-private for HistoricalSolver access

    /** Gets start time for timeout checking */
    public long getStartTimeMs() {
        return startTimeMs;
    }

    /** Gets statistics manager for tracking solver metrics */
    public Statistics getStats() {
        return stats;
    }

    private boolean useAC3 = true;
    private boolean useDomainCache = true;
    private boolean mrvIndexEnabled = false;

    /** Package-private toggle for tests that need to isolate AC-3 from the
     *  backtracking search path. Production code leaves it on. */
    public void setUseAC3(boolean enabled) { this.useAC3 = enabled; }

    /** Enables the MRV priority-queue index (O(log N) cell selection).
     *  Off by default because it regresses 4×4 by ~30% (PQ overhead beats
     *  the linear scan on small boards). Expected crossover is 6×6+.
     *  Used by benchmarks to measure the crossover point. */
    public void setMRVIndexEnabled(boolean enabled) { this.mrvIndexEnabled = enabled; }

    /** @return whether the MRV priority-queue index is configured on for the next solve */
    public boolean isMRVIndexEnabled() { return mrvIndexEnabled; }

    private DomainManager domainManager;
    private ConstraintPropagator constraintPropagator;
    private SingletonDetector singletonDetector;
    private MRVCellSelector cellSelector;
    private LeastConstrainingValueOrderer valueOrderer;
    /** Package-private for {@link HistoricalSolver} component coordination. */
    PlacementValidator validator;
    private BoardDisplayManager displayManager;
    private NeighborAnalyzer neighborAnalyzer;
    private PieceOrderingOptimizer pieceOrderingOptimizer;
    private AutoSaveManager autoSaveManager;
    private RecordManager recordManager;

    PlacementOrderTracker placementOrderTracker;
    private SymmetryBreakingManager symmetryBreakingManager;
    SolverConfiguration.Builder configBuilder = SolverConfiguration.builder();  // Package-private for HistoricalSolver
    FixedPieceDetector fixedPieceDetector = new FixedPieceDetector();  // Package-private for HistoricalSolver
    private SharedSearchState sharedState = new SharedSearchState();
    private SingletonPlacementStrategy singletonStrategy;
    private MRVPlacementStrategy mrvStrategy;
    private CellConstraints[][] cellConstraints;
    Random random = new Random();
    int threadId = -1;

    /** Resets shared search state between puzzle solving sessions. */
    public static void resetGlobalState() {
        // Keep for backward compatibility but recommend using instance method
        // TODO: Migrate callers to use resetSharedState() on instance
        new SharedSearchState().reset();
    }

    /** Resets shared search state. Recommended over static resetGlobalState(). */
    public void resetSharedState() {
        sharedState.reset();
    }

    /** Gets shared search state for this solver instance. */
    public SharedSearchState getSharedState() {
        return sharedState;
    }

    /** Sets shared search state (useful for sharing state across multiple solvers). */
    public void setSharedState(SharedSearchState sharedState) {
        this.sharedState = sharedState;
    }

    /** Sets display configuration */
    public void setDisplayConfig(boolean verbose, int minDepth) {
        configBuilder.verbose(verbose).minDepthToShowRecords(minDepth);
    }

    /** Sets puzzle name */
    public void setPuzzleName(String name) {
        configBuilder.puzzleName(name);
    }

    /** Sets sort order */
    public void setSortOrder(String order) {
        configBuilder.sortOrder(order);
    }

    /** Sets number of fixed pieces */
    public void setNumFixedPieces(int num) {
        configBuilder.numFixedPieces(num);
    }

    /** Sets maximum execution time */
    public void setMaxExecutionTime(long timeMs) {
        configBuilder.maxExecutionTimeMs(timeMs);
    }

    /** Sets thread label */
    public void setThreadLabel(String label) {
        configBuilder.threadLabel(label);
    }

    long randomSeed = 0;

    /** Delegates to {@link BoardVisualizer#printBoardWithCounts} */
    public void printBoardWithCounts(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces,
                                      int lastPlacedRow, int lastPlacedCol) {
        BoardVisualizer.printBoardWithCounts(board, piecesById, pieceUsed, totalPieces,
                                            lastPlacedRow, lastPlacedCol, this::fits);
    }

    /** Delegates to {@link PlacementOrderTracker#recordPlacement} */
    void recordPlacement(int row, int col, int pieceId, int rotation) {
        if (placementOrderTracker != null) {
            placementOrderTracker.recordPlacement(row, col, pieceId, rotation);
        }
    }

    /** Delegates to {@link PlacementOrderTracker#removeLastPlacement} */
    @Deprecated
    SaveStateManager.PlacementInfo removeLastPlacement() {
        // Deprecated - callers should use removePlacement(row, col)
        if (placementOrderTracker != null) {
            return placementOrderTracker.removeLastPlacement();
        }
        return null;
    }

    /** Removes a placement from tracking for a specific position */
    SaveStateManager.PlacementInfo removePlacement(int row, int col) {
        if (placementOrderTracker != null) {
            return placementOrderTracker.removePlacement(row, col);
        }
        return null;
    }

    /** Gets placement history from tracker */
    public java.util.List<SaveStateManager.PlacementInfo> getPlacementHistory() {
        if (placementOrderTracker != null) {
            return placementOrderTracker.getPlacementHistory();
        }
        return new java.util.ArrayList<>();
    }

    /**
     * Builds placement order map for display purposes.
     * Eliminates code duplication in debug/display code.
     *
     * @return Map of position to placement step number
     */
    public java.util.Map<util.PositionKey, Integer> buildPlacementOrderMap() {
        java.util.Map<util.PositionKey, Integer> placementOrderMap = new java.util.HashMap<>();
        java.util.List<SaveStateManager.PlacementInfo> allPlacements = getPlacementHistory();
        if (allPlacements != null) {
            int step = 1;
            for (SaveStateManager.PlacementInfo info : allPlacements) {
                util.PositionKey key = new util.PositionKey(info.row, info.col);
                placementOrderMap.put(key, step++);
            }
        }
        return placementOrderMap;
    }

    // State management delegates
    public int getStepCount() { return stats.getStepCount(); }
    public void incrementStepCount() { stats.incrementStepCount(); }
    public void setLastPlaced(int row, int col) { stats.setLastPlaced(row, col); }
    public int getLastPlacedRow() { return stats.getLastPlacedRow(); }
    public int getLastPlacedCol() { return stats.getLastPlacedCol(); }
    public void findAndSetLastPlaced(Board board) { stats.findAndSetLastPlaced(board); }

    /** Assigns initialized components to instance fields. */
    private void assignSolverComponents(SolverInitializer.InitializedComponents components) {
        this.cellConstraints = components.cellConstraints;
        this.validator = components.validator;
        this.displayManager = components.displayManager;
        this.domainManager = components.domainManager;
        this.constraintPropagator = components.constraintPropagator;
        this.singletonDetector = components.singletonDetector;
        this.cellSelector = components.cellSelector;
        this.valueOrderer = components.valueOrderer;
        this.neighborAnalyzer = components.neighborAnalyzer;
        this.pieceOrderingOptimizer = components.pieceOrderingOptimizer;
    }

    /** Initializes placement strategies. Package-private for {@link HistoricalSolver}. */
    void initializePlacementStrategies() {
        SolverConfiguration config = configBuilder.build();
        this.singletonStrategy = new SingletonPlacementStrategy(
            singletonDetector, config.isUseSingletons(), config.isVerbose(),
            symmetryBreakingManager, constraintPropagator, domainManager
        );
        this.mrvStrategy = new MRVPlacementStrategy(
            config.isVerbose(), valueOrderer, symmetryBreakingManager,
            constraintPropagator, domainManager
        );
        this.mrvStrategy.setSortOrder(config.getSortOrder());
        this.mrvStrategy.setDebugBacktracking(config.isDebugBacktracking());
        this.mrvStrategy.setDebugShowBoard(config.isDebugShowBoard());
    }

    /** Initializes managers. Package-private for {@link HistoricalSolver}. */
    void initializeManagers(Map<Integer, Piece> pieces) {
        SolverConfiguration config = configBuilder.build();

        this.autoSaveManager = new AutoSaveManager(
            config.getPuzzleName(),
            config.getNumFixedPieces(),
            config.getInitialFixedPieces(),
            placementOrderTracker
        );
        this.autoSaveManager.initializePiecesMap(pieces);

        configBuilder.threadId(threadId);
        SolverConfiguration configWithThread = configBuilder.build();
        this.recordManager = new RecordManager(
            configWithThread.getPuzzleName(),
            configWithThread.getThreadId(),
            configWithThread.getMinDepthToShowRecords(),
            sharedState.getLockObject(),
            sharedState.getGlobalMaxDepth(),
            sharedState.getGlobalBestScore(),
            sharedState.getGlobalBestThreadId(),
            sharedState.getGlobalBestBoard(),
            sharedState.getGlobalBestPieces()
        );
    }

    /** Initializes solver components. Package-private for {@link HistoricalSolver}. */
    void initializeComponents(Board board, Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {
        SolverConfiguration config = configBuilder.build();
        SolverInitializer initializer = new SolverInitializer(this, stats, config.getSortOrder(), config.isVerbose(),
            config.isPrioritizeBorders(), config.getFixedPositions(),
            config.isDebugBacktracking(), config.isDebugShowBoard(), config.isDebugShowAlternatives(),
            config.getDebugMaxCandidates(), config.isDebugStepByStep());
        SolverInitializer.InitializedComponents components = initializer.initializeComponents(
            board, pieces, pieceUsed, totalPieces);
        assignSolverComponents(components);
    }

    /** Initializes AC-3 domains. Package-private for {@link HistoricalSolver}. */
    void initializeDomains(Board board, Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {
        if (useAC3) {
            this.domainManager.initializeAC3Domains(board, pieces, pieceUsed, totalPieces);
            // Pre-filter (0,0) so AC-3 cannot produce singletons that the
            // sym-breaking rule would later reject. Without this, AC-3 and
            // SymmetryBreakingManager reason on divergent state spaces and
            // the solver dead-ends on puzzles like 4x4 easy — see fix for
            // the bug tracked in SymmetryBreakingBugTrackingTest.
            if (symmetryBreakingManager != null) {
                this.domainManager.restrictTopLeftDomain(
                    symmetryBreakingManager.computeCanonicalTopLeftPieceId(pieces),
                    symmetryBreakingManager.computeRequiredTopLeftRotation());
            }
        }
    }

    /** Creates BitSet for tracking piece usage (1-based, index 0 unused). Package-private for {@link HistoricalSolver}. */
    BitSet createPieceUsedBitSet(Map<Integer, Piece> pieces) {
        int maxPieceId = pieces.keySet().stream().max(Integer::compareTo).orElse(pieces.size());
        return new BitSet(maxPieceId + 1); // index 0 unused, 1-based
    }

    /** Delegates to {@link PlacementValidator#fits} */
    public boolean fits(Board board, int r, int c, int[] candidateEdges) {
        return validator.fits(board, r, c, candidateEdges);
    }

    /** Delegates to {@link PieceOrderingOptimizer#countUniquePieces} */
    public int countUniquePieces(Board board, int r, int c, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        return pieceOrderingOptimizer.countUniquePieces(board, r, c, piecesById, pieceUsed, totalPieces);
    }

    /** Delegates to {@link MRVCellSelector#selectNextCell} */
    public int[] findNextCellMRV(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        HeuristicStrategy.CellPosition pos = cellSelector.selectNextCell(board, piecesById, pieceUsed, totalPieces);
        if (pos == null) {
            return null;
        }
        return new int[]{pos.row(), pos.col()};
    }


    /** Finds next empty cell in row-major order. */
    public int[] findNextCell(Board board) {
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.isEmpty(r, c)) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    /** Delegates to {@link BacktrackingSolver#solve} */
    public boolean solveBacktracking(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        return new BacktrackingSolver(
            this, stats, sharedState.getSolutionFound(), configBuilder.build(),
            recordManager, autoSaveManager, singletonStrategy, mrvStrategy,
            threadId, randomSeed, startTimeMs
        ).solve(board, piecesById, pieceUsed, totalPieces);
    }

    /** Delegates to {@link HistoricalSolver#solveWithHistory} */
    public boolean solveWithHistory(Board board, Map<Integer, Piece> allPieces,
                                     List<Integer> unusedIds,
                                     List<SaveStateManager.PlacementInfo> preloadedOrder) {
        return new HistoricalSolver(this).solveWithHistory(board, allPieces, unusedIds, preloadedOrder);
    }

    /**
     * Solves the puzzle and returns true if a solution is found.
     *
     * @param board empty board to fill
     * @param pieces map of pieces by ID
     * @return true if the puzzle was solved
     */
    public boolean solve(Board board, Map<Integer, Piece> pieces) {
        stats.start();
        this.startTimeMs = System.currentTimeMillis();

        this.placementOrderTracker = new PlacementOrderTracker();
        placementOrderTracker.initialize();

        int totalPieces = pieces.size();
        BitSet pieceUsed = createPieceUsedBitSet(pieces);

        // Detect fixed pieces and initialize tracker with them
        FixedPieceDetector.FixedPieceInfo fixedPieceInfo = fixedPieceDetector.detectFromBoard(board, pieceUsed);
        configBuilder.numFixedPieces(fixedPieceInfo.numFixedPieces)
                    .fixedPositions(fixedPieceInfo.fixedPositions)
                    .initialFixedPieces(fixedPieceInfo.fixedPiecesList);
        placementOrderTracker.initializeWithFixedPieces(fixedPieceInfo.fixedPiecesList);

        initializeManagers(pieces);
        initializeComponents(board, pieces, pieceUsed, totalPieces);
        // Build sym-breaking manager BEFORE domain init so the TL-domain
        // pre-filter in initializeDomains can read its canonical piece and
        // required rotation. Dependency order: sym-breaking → AC-3 domains.
        initializeSymmetryBreaking(board);
        initializeDomains(board, pieces, pieceUsed, totalPieces);
        initializePlacementStrategies();

        boolean solved = solveBacktracking(board, pieces, pieceUsed, totalPieces);

        if (!solved) {
            stats.end();
            if (configBuilder.build().isVerbose()) {
                SolverLogger.info("\n========================================");
                SolverLogger.info("PAS DE SOLUTION TROUVÉE");
                SolverLogger.info("========================================");
                stats.print();
            }
        }

        // Close stats logger
        if (autoSaveManager != null) {
            autoSaveManager.close();
        }

        return solved;
    }

    /** Initializes symmetry breaking constraints. */
    private void initializeSymmetryBreaking(Board board) {
        if (board == null || board.getRows() == 0 || board.getCols() == 0) {
            return;
        }
        this.symmetryBreakingManager = new SymmetryBreakingManager(
            board.getRows(),
            board.getCols(),
            configBuilder.build().isVerbose()
        );
        // Apply pending overrides if a test/caller set them before solve()
        symmetryBreakingManager.setLexicographicOrdering(pendingLexFlag);
        symmetryBreakingManager.setRotationalFixing(pendingRotationFlag);
        symmetryBreakingManager.logConfiguration();
    }

    // Defaults ON after two fixes landed together (2026-04-17):
    //   1. TL-domain pre-filter in DomainManager aligns AC-3 with
    //      sym-breaking, so AC-3 cannot produce infeasible singletons at TL.
    //   2. restoreAC3Domains now recomputes every empty cell (not just the
    //      cell + 4 neighbors), fixing a long-standing bug where cascading
    //      AC-3 propagation left stale over-reduced domains after a
    //      backtrack. Together they unlock 4x4 easy AND 4x4 hard with
    //      lex+rotation flags on, per SymmetryBreakingBugTrackingTest.
    // Tests override explicitly via setSymmetryBreakingFlags(false, false)
    // when they want to exercise the unconstrained search path.
    private boolean pendingLexFlag = true;
    private boolean pendingRotationFlag = true;

    /**
     * Overrides the symmetry-breaking flags for the next call to {@link #solve}.
     * Must be called before {@code solve()} — the manager is freshly built
     * each solve and reads these values.
     */
    public void setSymmetryBreakingFlags(boolean lexicographic, boolean rotational) {
        this.pendingLexFlag = lexicographic;
        this.pendingRotationFlag = rotational;
    }

    /**
     * Returns the live SymmetryBreakingManager built for the last solve, or
     * null if {@link #solve} hasn't been called yet. Tests inspect rejection
     * counters via this accessor.
     */
    public SymmetryBreakingManager getSymmetryBreakingManager() {
        return symmetryBreakingManager;
    }

    /** Returns solver statistics. */
    public Statistics getStatistics() {
        return stats;
    }

    /** Sets whether to use singletons */
    public void setUseSingletons(boolean enabled) {
        configBuilder.useSingletons(enabled);
    }

    /** Sets whether to prioritize borders */
    public void setPrioritizeBorders(boolean enabled) {
        configBuilder.prioritizeBorders(enabled);
    }

    /** Sets verbose mode */
    public void setVerbose(boolean enabled) {
        configBuilder.verbose(enabled);
    }

    public void setDebugBacktracking(boolean enabled) {
        configBuilder.debugBacktracking(enabled);
    }

    public void setDebugShowBoard(boolean enabled) {
        configBuilder.debugShowBoard(enabled);
    }

    public void setDebugShowAlternatives(boolean enabled) {
        configBuilder.debugShowAlternatives(enabled);
    }

    public void setDebugMaxCandidates(int max) {
        configBuilder.debugMaxCandidates(max);
    }

    public void setDebugStepByStep(boolean enabled) {
        configBuilder.debugStepByStep(enabled);
    }

    /** Temporarily enable/disable debug logging in cell selector (for internal calls) */
    public void setCellSelectorSilentMode(boolean silent) {
        if (cellSelector != null) {
            cellSelector.setSilentMode(silent);
        }
    }

    /** Resets solver state and statistics. */
    public void reset() {
        stats = new Statistics();
        // Note: stats already has resetState() called in its constructor
    }

    /** Delegates to {@link ParallelSolverOrchestrator#solve} */
    public boolean solveParallel(Board board, Map<Integer, Piece> allPieces, Map<Integer, Piece> availablePieces, int numThreads) {
        ParallelSolverOrchestrator orchestrator = new ParallelSolverOrchestrator(
            this,
            allPieces,
            configBuilder.build().getPuzzleName(),
            useDomainCache,
            sharedState.getSolutionFound(),
            sharedState.getGlobalMaxDepth(),
            sharedState.getGlobalBestScore(),
            sharedState.getGlobalBestThreadId(),
            sharedState.getGlobalBestBoard(),
            sharedState.getGlobalBestPieces(),
            sharedState.getLockObject()
        );
        return orchestrator.solve(board, availablePieces, numThreads);
    }

    /** Delegates to {@link BoardDisplayManager#printBoardWithLabels} */
    public void printBoardWithLabels(Board board, Map<Integer, Piece> piecesById, List<Integer> unusedIds) {
        displayManager.printBoardWithLabels(board, piecesById, unusedIds);
    }

    /** Displays board with highlighting and placement order numbers */
    public void printBoardWithLabels(Board board, Map<Integer, Piece> piecesById, List<Integer> unusedIds,
                                    SaveStateManager.PlacementInfo lastPlacement, int[] nextCell,
                                    java.util.Map<util.PositionKey, Integer> placementOrderMap) {
        displayManager.printBoardWithLabels(board, piecesById, unusedIds, lastPlacement, nextCell, placementOrderMap, null);
    }

    /** Displays board with highlighting, placement order numbers, and removed cell indicator */
    public void printBoardWithLabels(Board board, Map<Integer, Piece> piecesById, List<Integer> unusedIds,
                                    SaveStateManager.PlacementInfo lastPlacement, int[] nextCell,
                                    java.util.Map<util.PositionKey, Integer> placementOrderMap, int[] removedCell) {
        displayManager.printBoardWithLabels(board, piecesById, unusedIds, lastPlacement, nextCell, placementOrderMap, removedCell);
    }

    /** Delegates to {@link BoardDisplayManager#printBoardWithComparison} */
    public void printBoardWithComparison(Board currentBoard, Board referenceBoard,
                                          Map<Integer, Piece> piecesById, List<Integer> unusedIds) {
        displayManager.printBoardWithComparison(currentBoard, referenceBoard, piecesById, unusedIds);
    }

}
