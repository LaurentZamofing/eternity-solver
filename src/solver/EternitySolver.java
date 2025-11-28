package solver;

import model.Board;
import model.Piece;
import solver.BoardVisualizer;
import solver.heuristics.*;
import util.SaveManager;
import util.SaveStateManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Solveur de puzzle d'edge-matching (type "Eternity") utilisant le backtracking.
 *
 * Limitations :
 * - Backtracking naïf (suffisant pour petites instances 3x3, 4x4)
 * - Pour de vraies instances 16x16, il faudrait ajouter :
 *   - MRV (Minimum Remaining Values) heuristic
 *   - Forward-checking
 *   - Propagation d'arcs (AC-3)
 *   - Méthodes méta-heuristiques
 */
public class EternitySolver {

    private static final Logger logger = LoggerFactory.getLogger(EternitySolver.class);

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
    /**
     * Package-private fields for HistoricalSolver access.
     * These allow HistoricalSolver to initialize and coordinate solver components
     * when resuming from saved state without breaking encapsulation.
     */
    PlacementValidator validator;
    private BoardDisplayManager displayManager;
    private NeighborAnalyzer neighborAnalyzer;
    private PieceOrderingOptimizer pieceOrderingOptimizer;
    private AutoSaveManager autoSaveManager;
    private RecordManager recordManager;

    PlacementOrderTracker placementOrderTracker;
    private BacktrackingHistoryManager backtrackingHistoryManager;
    private SymmetryBreakingManager symmetryBreakingManager;
    ConfigurationManager configManager = new ConfigurationManager();
    private SingletonPlacementStrategy singletonStrategy;
    private MRVPlacementStrategy mrvStrategy;
    private CellConstraints[][] cellConstraints;
    Random random = new Random();

    // Parallel search delegates
    private static AtomicBoolean solutionFound = ParallelSearchManager.getSolutionFound();
    private static AtomicInteger globalMaxDepth = ParallelSearchManager.getGlobalMaxDepth();
    private static AtomicInteger globalBestScore = ParallelSearchManager.getGlobalBestScore();
    private static AtomicInteger globalBestThreadId = ParallelSearchManager.getGlobalBestThreadId();
    private static AtomicReference<Board> globalBestBoard = ParallelSearchManager.getGlobalBestBoard();
    private static AtomicReference<Map<Integer, Piece>> globalBestPieces = ParallelSearchManager.getGlobalBestPieces();
    private static final Object lockObject = ParallelSearchManager.getLockObject();
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

    /** Delegates to {@link BoardVisualizer#printBoardCompact} */
    private void printBoardCompact(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        BoardVisualizer.printBoardCompact(board, piecesById, pieceUsed, totalPieces, this::fits);
    }

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

    /**
     * Initializes placement strategies (singleton and MRV).
     *
     * <p>Package-private for HistoricalSolver access when resuming from saved state.
     * Extracted to eliminate duplication between solve() and solveWithHistory().</p>
     *
     * @see HistoricalSolver#solveWithHistory
     */
    void initializePlacementStrategies() {
        this.singletonStrategy = new SingletonPlacementStrategy(
            singletonDetector, configManager.isUseSingletons(), configManager.isVerbose(),
            symmetryBreakingManager, constraintPropagator, domainManager
        );
        this.mrvStrategy = new MRVPlacementStrategy(
            configManager.isVerbose(), valueOrderer, symmetryBreakingManager,
            constraintPropagator, domainManager
        );
    }

    /**
     * Initializes managers (AutoSaveManager and RecordManager).
     *
     * <p>Package-private for HistoricalSolver access when resuming from saved state.
     * Extracted to eliminate duplication between solve() and solveWithHistory().</p>
     *
     * @param pieces map of all puzzle pieces
     * @see HistoricalSolver#solveWithHistory
     */
    void initializeManagers(Map<Integer, Piece> pieces) {
        this.autoSaveManager = configManager.createAutoSaveManager(
            placementOrderTracker != null ? placementOrderTracker.getPlacementHistory() : new ArrayList<>(),
            pieces);

        configManager.setThreadId(threadId);
        this.recordManager = configManager.createRecordManager(lockObject, globalMaxDepth,
            globalBestScore, globalBestThreadId, globalBestBoard, globalBestPieces);
    }

    /**
     * Initializes helper components and assigns them to the solver.
     *
     * <p>Package-private for HistoricalSolver access when resuming from saved state.
     * Extracted to eliminate duplication between solve() and solveWithHistory().</p>
     *
     * @param board the puzzle board
     * @param pieces map of all puzzle pieces
     * @param pieceUsed bitset tracking used pieces
     * @param totalPieces total number of pieces
     * @see HistoricalSolver#solveWithHistory
     */
    void initializeComponents(Board board, Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {
        SolverInitializer initializer = new SolverInitializer(this, stats, configManager.getSortOrder(), configManager.isVerbose(),
            configManager.isPrioritizeBorders(), configManager.getFixedPositions());
        SolverInitializer.InitializedComponents components = initializer.initializeComponents(
            board, pieces, pieceUsed, totalPieces);
        assignSolverComponents(components);
    }

    /**
     * Initializes domains (AC-3 only - domain cache removed as unused).
     *
     * <p>Package-private for HistoricalSolver access when resuming from saved state.
     * Extracted to eliminate duplication between solve() and solveWithHistory().</p>
     *
     * @param board the puzzle board
     * @param pieces map of all puzzle pieces
     * @param pieceUsed bitset tracking used pieces
     * @param totalPieces total number of pieces
     * @see HistoricalSolver#solveWithHistory
     */
    void initializeDomains(Board board, Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {
        if (useAC3) {
            this.domainManager.initializeAC3Domains(board, pieces, pieceUsed, totalPieces);
        }
    }

    /**
     * Creates a BitSet for tracking piece usage, sized according to the maximum piece ID.
     *
     * <p>Package-private for HistoricalSolver access when resuming from saved state.
     * Extracted to eliminate duplication between solve() and solveWithHistory() (Refactoring #14).</p>
     *
     * @param pieces map of all pieces
     * @return empty BitSet sized for all pieces (index 0 unused, 1-based)
     * @see HistoricalSolver#solveWithHistory
     */
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
        BacktrackingSolver backtrackingSolver = new BacktrackingSolver(
            this,
            stats,
            solutionFound,
            configManager,
            recordManager,
            autoSaveManager,
            singletonStrategy,
            mrvStrategy,
            threadId,
            randomSeed,
            startTimeMs
        );
        return backtrackingSolver.solve(board, piecesById, pieceUsed, totalPieces);
    }

    /** Delegates to {@link HistoricalSolver#solveWithHistory} */
    public boolean solveWithHistory(Board board, Map<Integer, Piece> allPieces,
                                     List<Integer> unusedIds,
                                     List<SaveStateManager.PlacementInfo> preloadedOrder) {
        HistoricalSolver historicalSolver = new HistoricalSolver(this);
        return historicalSolver.solveWithHistory(board, allPieces, unusedIds, preloadedOrder);
    }

    /**
     * Résout le puzzle et retourne true si une solution est trouvée.
     *
     * @param board grille vide à remplir
     * @param pieces map des pièces par ID
     * @return true si le puzzle a été résolu
     */
    public boolean solve(Board board, Map<Integer, Piece> pieces) {
        stats.start();
        this.startTimeMs = System.currentTimeMillis();

        this.placementOrderTracker = new PlacementOrderTracker();
        this.placementOrderTracker.initialize();

        int totalPieces = pieces.size();
        BitSet pieceUsed = createPieceUsedBitSet(pieces);

        configManager.detectFixedPiecesFromBoard(board, pieceUsed,
            placementOrderTracker != null ? placementOrderTracker.getPlacementHistory() : new ArrayList<>());

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
            solutionFound,
            globalMaxDepth,
            globalBestScore,
            globalBestThreadId,
            globalBestBoard,
            globalBestPieces,
            lockObject
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
