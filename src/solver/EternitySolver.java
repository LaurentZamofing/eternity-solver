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

    /**
     * Type alias for backward compatibility.
     * Statistics functionality has been moved to StatisticsManager.
     */
    public static class Statistics extends StatisticsManager {
        // Empty class - all functionality inherited from StatisticsManager
    }

    // State management (Refactoring #11 - extracted to SolverStateManager)
    private SolverStateManager stateManager = new SolverStateManager();
    Statistics stats = new Statistics(); // Package-private for ParallelSolverOrchestrator
    private long startTimeMs = 0; // Temps de démarrage de la résolution

    // Configuration flags
    private boolean useAC3 = true; // Enable/disable AC-3
    private boolean useDomainCache = true; // Activer/désactiver le cache des domaines

    // Extracted utility classes for better code organization
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
    PlacementValidator validator; // Used by HistoricalSolver to validate pre-loaded pieces
    private BoardDisplayManager displayManager;
    private NeighborAnalyzer neighborAnalyzer;
    private PieceOrderingOptimizer pieceOrderingOptimizer;
    private AutoSaveManager autoSaveManager;
    private RecordManager recordManager;

    // Sprint 3 extractions
    PlacementOrderTracker placementOrderTracker; // Used by HistoricalSolver to initialize placement history
    private BacktrackingHistoryManager backtrackingHistoryManager;

    // Sprint 4 extractions
    private SymmetryBreakingManager symmetryBreakingManager;

    // Sprint 5 extractions
    ConfigurationManager configManager = new ConfigurationManager(); // Used by HistoricalSolver for configuration access

    // Sprint 6 extractions - Strategy Pattern for placement
    private SingletonPlacementStrategy singletonStrategy;
    private MRVPlacementStrategy mrvStrategy;

    // Pre-computed constraints for each cell (optimization)
    private CellConstraints[][] cellConstraints;

    // Randomisation pour éviter le thrashing
    Random random = new Random(); // Package-private for ParallelSolverOrchestrator

    // Parallel search - MIGRATED to ParallelSearchManager (Sprint 4)
    // Keep minimal references for backward compatibility
    private static AtomicBoolean solutionFound = ParallelSearchManager.getSolutionFound();
    private static AtomicInteger globalMaxDepth = ParallelSearchManager.getGlobalMaxDepth();
    private static AtomicInteger globalBestScore = ParallelSearchManager.getGlobalBestScore();
    private static AtomicInteger globalBestThreadId = ParallelSearchManager.getGlobalBestThreadId();
    private static AtomicReference<Board> globalBestBoard = ParallelSearchManager.getGlobalBestBoard();
    private static AtomicReference<Map<Integer, Piece>> globalBestPieces = ParallelSearchManager.getGlobalBestPieces();
    private static final Object lockObject = ParallelSearchManager.getLockObject();
    int threadId = -1; // ID du thread pour ce solveur (package-private for ParallelSolverOrchestrator)

    /**
     * Réinitialise toutes les variables statiques du solveur
     * À appeler entre chaque puzzle dans un run séquentiel
     */
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

    // Sauvegarde périodique par thread
    long randomSeed = 0; // Seed du random pour ce thread (package-private for ParallelSolverOrchestrator)

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

    // ==================== PlacementOrder Helpers ====================
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

    // ==================== Step Count and Last Placed Accessors ====================

    // State management methods - delegate to SolverStateManager (Refactoring #11)
    public int getStepCount() { return stateManager.getStepCount(); }
    public void incrementStepCount() { stateManager.incrementStepCount(); }
    public void setLastPlaced(int row, int col) { stateManager.setLastPlaced(row, col); }
    public int getLastPlacedRow() { return stateManager.getLastPlacedRow(); }
    public int getLastPlacedCol() { return stateManager.getLastPlacedCol(); }
    public void findAndSetLastPlaced(Board board) { stateManager.findAndSetLastPlaced(board); }

    // ==================== End Step Count and Last Placed Accessors ====================

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

    /**
     * Vérifie si une pièce candidate peut être placée en (r,c).
     * Convention : bord extérieur doit être 0 (modifiable selon besoin).
     *
     * @param board grille actuelle
     * @param r ligne
     * @param c colonne
     * @param candidateEdges arêtes de la pièce candidate [N, E, S, W]
     * @return true si la pièce peut être placée
     */
    public boolean fits(Board board, int r, int c, int[] candidateEdges) {
        // Delegate to PlacementValidator (refactored for better code organization)
        return validator.fits(board, r, c, candidateEdges);
    }


    /**
     * Compte le nombre de pièces uniques qui peuvent être placées (sans considérer les rotations multiples).
     *
     * @param board grille actuelle
     * @param r ligne
     * @param c colonne
     * @param piecesById map des pièces par ID
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @return nombre de pièces distinctes pouvant être placées
     */
    public int countUniquePieces(Board board, int r, int c, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        // Delegate to PieceOrderingOptimizer (Refactoring #12 - eliminate duplication)
        return pieceOrderingOptimizer.countUniquePieces(board, r, c, piecesById, pieceUsed, totalPieces);
    }

    /**
     * Wrapper method for findNextCellMRV to maintain backward compatibility.
     * Delegates to the extracted MRVCellSelector and converts the result.
     *
     * @param board current board state
     * @param piecesById map of all pieces
     * @param pieceUsed array tracking used pieces
     * @param totalPieces total number of pieces
     * @return [row, col] of the most constrained cell, or null if no empty cells
     */
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
        this.startTimeMs = System.currentTimeMillis(); // Marquer le temps de démarrage

        // Initialize PlacementOrderTracker
        this.placementOrderTracker = new PlacementOrderTracker();
        this.placementOrderTracker.initialize();

        // Créer le tableau pieceUsed (Refactoring #14 - extracted to helper)
        int totalPieces = pieces.size();
        BitSet pieceUsed = createPieceUsedBitSet(pieces);

        // Détecter et mémoriser les positions des pièces fixes (déjà placées au début)
        configManager.detectFixedPiecesFromBoard(board, pieceUsed,
            placementOrderTracker != null ? placementOrderTracker.getPlacementHistory() : new ArrayList<>());

        // Initialize managers, components, and strategies (Refactoring #17 - consolidated initialization)
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

    /**
     * Initialize symmetry breaking constraints
     * Delegates to SymmetryBreakingManager (extracted in Sprint 4)
     */
    private void initializeSymmetryBreaking(Board board) {
        if (board == null || board.getRows() == 0 || board.getCols() == 0) {
            return;
        }

        // Create SymmetryBreakingManager
        this.symmetryBreakingManager = new SymmetryBreakingManager(
            board.getRows(),
            board.getCols(),
            configManager.isVerbose()
        );

        // Log configuration
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

    /**
     * Résout le puzzle en parallèle avec plusieurs threads.
     * Chaque thread explore l'espace de recherche avec une seed aléatoire différente.
     *
     * @param board grille avec hint déjà placé
     * @param pieces map des pièces restantes
     * @param numThreads nombre de threads à lancer
     * @return true si une solution a été trouvée
     */
    public boolean solveParallel(Board board, Map<Integer, Piece> allPieces, Map<Integer, Piece> availablePieces, int numThreads) {
        // Delegate to ParallelSolverOrchestrator (Refactoring #13)
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

    /**
     * Affiche le board avec labels (public pour permettre l'affichage depuis MainSequential)
     */
    public void printBoardWithLabels(Board board, Map<Integer, Piece> piecesById, List<Integer> unusedIds) {
        displayManager.printBoardWithLabels(board, piecesById, unusedIds);
    }

    /**
     * Affiche le board en comparant avec un autre board (pour voir les différences)
     * Code couleur :
     * - Magenta : Case occupée dans referenceBoard mais vide dans currentBoard (régression)
     * - Orange : Case occupée dans les deux mais pièce différente (changement)
     * - Jaune : Case vide dans referenceBoard mais occupée dans currentBoard (progression)
     * - Cyan : Case identique dans les deux boards (stabilité)
     */
    public void printBoardWithComparison(Board currentBoard, Board referenceBoard,
                                          Map<Integer, Piece> piecesById, List<Integer> unusedIds) {
        displayManager.printBoardWithComparison(currentBoard, referenceBoard, piecesById, unusedIds);
    }

}
