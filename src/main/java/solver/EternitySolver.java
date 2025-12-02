package solver;

import model.Board;
import model.Piece;
import solver.BoardVisualizer;
import solver.heuristics.*;
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
public class EternitySolver {

    /** Type alias for backward compatibility - delegates to StatisticsManager. */
    public static class Statistics extends StatisticsManager { }

    private SolverStateManager stateManager = new SolverStateManager();
    Statistics stats = new Statistics();
    private long startTimeMs = 0;

    private boolean useAC3 = true;
    private boolean useDomainCache = true;

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
    ConfigurationManager configManager = new ConfigurationManager();
    private SingletonPlacementStrategy singletonStrategy;
    private MRVPlacementStrategy mrvStrategy;
    private CellConstraints[][] cellConstraints;
    Random random = new Random();
    int threadId = -1;

    /** Delegates to {@link ParallelSearchManager#resetGlobalState} */
    public static void resetGlobalState() {
        ParallelSearchManager.resetGlobalState();
    }

    /** Delegates to {@link ConfigurationManager#setDisplayConfig} */
    public void setDisplayConfig(boolean verbose, int minDepth) {
        configManager.setDisplayConfig(verbose, minDepth);
    }

    /** Delegates to {@link ConfigurationManager#setPuzzleName} */
    public void setPuzzleName(String name) {
        configManager.setPuzzleName(name);
    }

    /** Delegates to {@link ConfigurationManager#setSortOrder} */
    public void setSortOrder(String order) {
        configManager.setSortOrder(order);
    }

    /** Delegates to {@link ConfigurationManager#setNumFixedPieces} */
    public void setNumFixedPieces(int num) {
        configManager.setNumFixedPieces(num);
    }

    /** Delegates to {@link ConfigurationManager#setMaxExecutionTime} */
    public void setMaxExecutionTime(long timeMs) {
        configManager.setMaxExecutionTime(timeMs);
    }

    /** Delegates to {@link ConfigurationManager#setThreadLabel} */
    public void setThreadLabel(String label) {
        configManager.setThreadLabel(label);
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
    SaveStateManager.PlacementInfo removeLastPlacement() {
        if (placementOrderTracker != null) {
            return placementOrderTracker.removeLastPlacement();
        }
        return null;
    }

    // State management delegates
    public int getStepCount() { return stateManager.getStepCount(); }
    public void incrementStepCount() { stateManager.incrementStepCount(); }
    public void setLastPlaced(int row, int col) { stateManager.setLastPlaced(row, col); }
    public int getLastPlacedRow() { return stateManager.getLastPlacedRow(); }
    public int getLastPlacedCol() { return stateManager.getLastPlacedCol(); }
    public void findAndSetLastPlaced(Board board) { stateManager.findAndSetLastPlaced(board); }

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
        this.singletonStrategy = new SingletonPlacementStrategy(
            singletonDetector, configManager.isUseSingletons(), configManager.isVerbose(),
            symmetryBreakingManager, constraintPropagator, domainManager
        );
        this.mrvStrategy = new MRVPlacementStrategy(
            configManager.isVerbose(), valueOrderer, symmetryBreakingManager,
            constraintPropagator, domainManager
        );
        this.mrvStrategy.setSortOrder(configManager.getSortOrder());
    }

    /** Initializes managers. Package-private for {@link HistoricalSolver}. */
    void initializeManagers(Map<Integer, Piece> pieces) {
        this.autoSaveManager = configManager.createAutoSaveManager(
            placementOrderTracker != null ? placementOrderTracker.getPlacementHistory() : new ArrayList<>(),
            pieces);

        configManager.setThreadId(threadId);
        this.recordManager = configManager.createRecordManager(
            ParallelSearchManager.getLockObject(),
            ParallelSearchManager.getGlobalMaxDepth(),
            ParallelSearchManager.getGlobalBestScore(),
            ParallelSearchManager.getGlobalBestThreadId(),
            ParallelSearchManager.getGlobalBestBoard(),
            ParallelSearchManager.getGlobalBestPieces());
    }

    /** Initializes solver components. Package-private for {@link HistoricalSolver}. */
    void initializeComponents(Board board, Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {
        SolverInitializer initializer = new SolverInitializer(this, stats, configManager.getSortOrder(), configManager.isVerbose(),
            configManager.isPrioritizeBorders(), configManager.getFixedPositions());
        SolverInitializer.InitializedComponents components = initializer.initializeComponents(
            board, pieces, pieceUsed, totalPieces);
        assignSolverComponents(components);
    }

    /** Initializes AC-3 domains. Package-private for {@link HistoricalSolver}. */
    void initializeDomains(Board board, Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {
        if (useAC3) {
            this.domainManager.initializeAC3Domains(board, pieces, pieceUsed, totalPieces);
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
        return new int[]{pos.row, pos.col};
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
            this, stats, ParallelSearchManager.getSolutionFound(), configManager,
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

        configManager.detectFixedPiecesFromBoard(board, pieceUsed, placementOrderTracker.getPlacementHistory());

        initializeManagers(pieces);
        initializeComponents(board, pieces, pieceUsed, totalPieces);
        initializeDomains(board, pieces, pieceUsed, totalPieces);
        initializeSymmetryBreaking(board);
        initializePlacementStrategies();

        boolean solved = solveBacktracking(board, pieces, pieceUsed, totalPieces);

        if (!solved) {
            stats.end();
            if (configManager.isVerbose()) {
                System.out.println("\n========================================");
                System.out.println("PAS DE SOLUTION TROUVÉE");
                System.out.println("========================================");
                stats.print();
            }
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
            configManager.isVerbose()
        );
        symmetryBreakingManager.logConfiguration();
    }

    /** Returns solver statistics. */
    public Statistics getStatistics() {
        return stats;
    }

    /** Delegates to {@link ConfigurationManager#setUseSingletons} */
    public void setUseSingletons(boolean enabled) {
        configManager.setUseSingletons(enabled);
    }

    /** Delegates to {@link ConfigurationManager#setPrioritizeBorders} */
    public void setPrioritizeBorders(boolean enabled) {
        configManager.setPrioritizeBorders(enabled);
    }

    /** Delegates to {@link ConfigurationManager#setVerbose} */
    public void setVerbose(boolean enabled) {
        configManager.setVerbose(enabled);
    }

    /** Resets solver state and statistics. */
    public void reset() {
        stats = new Statistics();
        stateManager.reset();
    }

    /** Delegates to {@link ParallelSolverOrchestrator#solve} */
    public boolean solveParallel(Board board, Map<Integer, Piece> allPieces, Map<Integer, Piece> availablePieces, int numThreads) {
        ParallelSolverOrchestrator orchestrator = new ParallelSolverOrchestrator(
            this,
            allPieces,
            configManager.getPuzzleName(),
            useDomainCache,
            ParallelSearchManager.getSolutionFound(),
            ParallelSearchManager.getGlobalMaxDepth(),
            ParallelSearchManager.getGlobalBestScore(),
            ParallelSearchManager.getGlobalBestThreadId(),
            ParallelSearchManager.getGlobalBestBoard(),
            ParallelSearchManager.getGlobalBestPieces(),
            ParallelSearchManager.getLockObject()
        );
        return orchestrator.solve(board, availablePieces, numThreads);
    }

    /** Delegates to {@link BoardDisplayManager#printBoardWithLabels} */
    public void printBoardWithLabels(Board board, Map<Integer, Piece> piecesById, List<Integer> unusedIds) {
        displayManager.printBoardWithLabels(board, piecesById, unusedIds);
    }

    /** Delegates to {@link BoardDisplayManager#printBoardWithComparison} */
    public void printBoardWithComparison(Board currentBoard, Board referenceBoard,
                                          Map<Integer, Piece> piecesById, List<Integer> unusedIds) {
        displayManager.printBoardWithComparison(currentBoard, referenceBoard, piecesById, unusedIds);
    }

}
