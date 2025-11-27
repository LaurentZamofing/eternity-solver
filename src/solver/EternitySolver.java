package solver;

import model.Board;
import model.Piece;
import model.Placement;
import solver.BoardVisualizer;
import solver.heuristics.*;
import util.SaveManager;
import util.SaveStateManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
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
    // useSingletons removed - use configManager.isUseSingletons() (Refactoring #15)
    // verbose removed - use configManager.isVerbose() (Refactoring #15)
    // minDepthToShowRecords removed - use configManager.getMinDepthToShowRecords() (Refactoring #15)
    // fixedPositions removed - use configManager.getFixedPositions() (Refactoring #15)
    // numFixedPieces removed - use configManager.getNumFixedPieces() (Refactoring #15)
    // initialFixedPieces removed - use configManager.getInitialFixedPieces() (Refactoring #15)
    // prioritizeBorders removed - use configManager.isPrioritizeBorders() (Refactoring #15)

    // Timeout management
    // maxExecutionTimeMs removed - use configManager.getMaxExecutionTimeMs() (Refactoring #15)
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
    private PlacementValidator validator;
    private BoardDisplayManager displayManager;
    private NeighborAnalyzer neighborAnalyzer;
    private PieceOrderingOptimizer pieceOrderingOptimizer;
    private AutoSaveManager autoSaveManager;
    private RecordManager recordManager;

    // Sprint 3 extractions
    private ParallelSearchManager parallelSearchManager;
    private PlacementOrderTracker placementOrderTracker;
    private BacktrackingHistoryManager backtrackingHistoryManager;

    // Sprint 4 extractions
    private SymmetryBreakingManager symmetryBreakingManager;

    // Sprint 5 extractions
    private ConfigurationManager configManager = new ConfigurationManager();

    // Sprint 6 extractions - Strategy Pattern for placement
    private SingletonPlacementStrategy singletonStrategy;
    private MRVPlacementStrategy mrvStrategy;

    // Pre-computed constraints for each cell (optimization)
    private CellConstraints[][] cellConstraints;

    // Randomisation pour éviter le thrashing
    Random random = new Random(); // Package-private for ParallelSolverOrchestrator
    private double randomizationProbability = SolverConstants.DEFAULT_RANDOMIZATION_PROBABILITY;
    private int stagnationThreshold = SolverConstants.DEFAULT_STAGNATION_THRESHOLD;

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

    // Work-stealing parallelism - MIGRATED to ParallelSearchManager (Sprint 4)
    private static ForkJoinPool workStealingPool = ParallelSearchManager.getWorkStealingPool();
    private static final int WORK_STEALING_DEPTH_THRESHOLD = ParallelSearchManager.WORK_STEALING_DEPTH_THRESHOLD;

    /**
     * Enable work-stealing parallelism for a single puzzle
     */
    public void enableWorkStealing(int numThreads) {
        if (workStealingPool == null || workStealingPool.isShutdown()) {
            workStealingPool = new ForkJoinPool(numThreads);
        }
    }

    /**
     * Réinitialise toutes les variables statiques du solveur
     * À appeler entre chaque puzzle dans un run séquentiel
     */
    public static void resetGlobalState() {
        ParallelSearchManager.resetGlobalState();
    }

    /**
     * Configure les paramètres d'affichage du solveur
     * @param verbose activer/désactiver l'affichage détaillé
     * @param minDepth seuil minimum pour afficher les records
     */
    public void setDisplayConfig(boolean verbose, int minDepth) {
        configManager.setDisplayConfig(verbose, minDepth);
        // verbose and minDepthToShowRecords removed - ConfigurationManager is now single source of truth (Refactoring #15)
    }

    /**
     * Configure le nom du puzzle pour la sauvegarde automatique
     * @param name nom du puzzle
     */
    public void setPuzzleName(String name) {
        configManager.setPuzzleName(name);
        // puzzleName removed - ConfigurationManager is now single source of truth (Refactoring #15)
    }

    /**
     * Configure l'ordre de tri des pièces
     * @param order "ascending" ou "descending"
     */
    public void setSortOrder(String order) {
        configManager.setSortOrder(order);
        // sortOrder removed - ConfigurationManager is now single source of truth (Refactoring #15)
    }

    public void setNumFixedPieces(int num) {
        configManager.setNumFixedPieces(num);
        // numFixedPieces removed - ConfigurationManager is now single source of truth (Refactoring #15)
    }

    /**
     * Définir le timeout maximum pour la résolution (en millisecondes)
     */
    public void setMaxExecutionTime(long timeMs) {
        configManager.setMaxExecutionTime(timeMs);
        // maxExecutionTimeMs removed - ConfigurationManager is now single source of truth (Refactoring #15)
    }

    /**
     * Configure le label du thread pour les logs
     * @param label label à afficher dans les logs (ex: "[Thread 1 - p01_asc]")
     */
    public void setThreadLabel(String label) {
        configManager.setThreadLabel(label);
        // threadLabel removed - ConfigurationManager is now single source of truth (Refactoring #15)
    }

    // Sauvegarde périodique par thread
    long randomSeed = 0; // Seed du random pour ce thread (package-private for ParallelSolverOrchestrator)
    private long lastThreadSaveTime = 0; // Timestamp de la dernière sauvegarde thread
    private static final long THREAD_SAVE_INTERVAL = SolverConstants.THREAD_SAVE_INTERVAL_MS;

    // Sauvegarde automatique périodique (nouveau système)
    // puzzleName removed - use configManager.getPuzzleName() (Refactoring #15)

    // threadLabel removed - use configManager.getThreadLabel() (Refactoring #15)
    // sortOrder removed - use configManager.getSortOrder() (Refactoring #15)

    /**
     * Affiche le board de manière compacte avec les valeurs des arêtes et les bordures.
     * @param board grille actuelle
     * @param piecesById map des pièces par ID
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     */
    private void printBoardCompact(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        BoardVisualizer.printBoardCompact(board, piecesById, pieceUsed, totalPieces, this::fits);
    }

    /**
     * Affiche le board avec les pièces posées et le nombre de pièces possibles sur les cases vides.
     *
     * @param board grille actuelle
     * @param piecesById map des pièces par ID
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @param lastPlacedRow ligne de la dernière pièce posée (-1 si aucune)
     * @param lastPlacedCol colonne de la dernière pièce posée (-1 si aucune)
     */
    public void printBoardWithCounts(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces,
                                      int lastPlacedRow, int lastPlacedCol) {
        BoardVisualizer.printBoardWithCounts(board, piecesById, pieceUsed, totalPieces,
                                            lastPlacedRow, lastPlacedCol, this::fits);
    }

    // ==================== PlacementOrder Helpers ====================

    /**
     * Record a placement using PlacementOrderTracker
     */
    void recordPlacement(int row, int col, int pieceId, int rotation) {
        if (placementOrderTracker != null) {
            placementOrderTracker.recordPlacement(row, col, pieceId, rotation);
        }
    }

    /**
     * Remove last placement using PlacementOrderTracker
     */
    SaveStateManager.PlacementInfo removeLastPlacement() {
        if (placementOrderTracker != null) {
            return placementOrderTracker.removeLastPlacement();
        }
        return null;
    }

    // ==================== End PlacementOrder Helpers ====================

    // ==================== Step Count and Last Placed Accessors ====================

    // State management methods - delegate to SolverStateManager (Refactoring #11)
    public int getStepCount() { return stateManager.getStepCount(); }
    public void incrementStepCount() { stateManager.incrementStepCount(); }
    public void setLastPlaced(int row, int col) { stateManager.setLastPlaced(row, col); }
    public int getLastPlacedRow() { return stateManager.getLastPlacedRow(); }
    public int getLastPlacedCol() { return stateManager.getLastPlacedCol(); }
    public void findAndSetLastPlaced(Board board) { stateManager.findAndSetLastPlaced(board); }

    // ==================== End Step Count and Last Placed Accessors ====================

    /**
     * Assigns solver components from SolverInitializer to instance fields.
     * Extracted to eliminate duplication between solve() and solveWithHistory().
     *
     * @param components Initialized components from SolverInitializer
     */
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
     * Extracted to eliminate duplication between solve() and solveWithHistory().
     */
    private void initializePlacementStrategies() {
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
     * Creates a BitSet for tracking piece usage, sized according to the maximum piece ID.
     * Extracted to eliminate duplication between solve() and solveWithHistory() (Refactoring #14).
     *
     * @param pieces map of all pieces
     * @return empty BitSet sized for all pieces (index 0 unused, 1-based)
     */
    private BitSet createPieceUsedBitSet(Map<Integer, Piece> pieces) {
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
     * Forward Checking : vérifie qu'un placement ne va pas créer de dead-end chez les voisins vides.
     *
     * @param board grille actuelle
     * @param r ligne où on veut placer
     * @param c colonne où on veut placer
     * @param candidateEdges edges de la pièce qu'on veut placer
     * @param piecesById toutes les pièces disponibles
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @param excludePieceId ID de la pièce qu'on est en train de tester (à exclure)
     * @return true si le placement est safe, false s'il créerait un dead-end
     */
    private boolean forwardCheck(Board board, int r, int c, int[] candidateEdges,
                                  Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces, int excludePieceId) {
        // Delegate to PlacementValidator (refactored for better code organization)
        return validator.forwardCheck(board, r, c, candidateEdges, piecesById, pieceUsed, totalPieces, excludePieceId);
    }

    /**
     * Parallel search task using Fork/Join framework
     */
    // ParallelSearchTask REMOVED - now fully delegated to ParallelSearchManager (Sprint 4)

    /**
     * Trouve tous les placements valides (pièce, rotation) pour une case donnée.
     *
     * @param board grille actuelle
     * @param r ligne
     * @param c colonne
     * @param piecesById map des pièces par ID
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @return liste des placements valides
     */
    private List<DomainManager.ValidPlacement> getValidPlacements(Board board, int r, int c, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        List<DomainManager.ValidPlacement> validPlacements = new ArrayList<>();

        // Early rejection using edge compatibility tables (if available)
        Set<Integer> candidatePieces = null;
        if (valueOrderer.getAllDifficultyScores() != null) {
            // Get required edge values from neighbors
            Placement north = (r > 0) ? board.getPlacement(r - 1, c) : null;
            Placement south = (r < board.getRows() - 1) ? board.getPlacement(r + 1, c) : null;
            Placement west = (c > 0) ? board.getPlacement(r, c - 1) : null;
            Placement east = (c < board.getCols() - 1) ? board.getPlacement(r, c + 1) : null;

            // Build constraint set: pieces that COULD fit (intersection of compatible sets)
            if (north != null) {
                int requiredEdge = north.edges[2];  // Need to match north's south edge
                candidatePieces = new HashSet<>(valueOrderer.getNorthCompatiblePieces(requiredEdge));
            }
            if (south != null) {
                int requiredEdge = south.edges[0];  // Need to match south's north edge
                Set<Integer> compatible = valueOrderer.getSouthCompatiblePieces(requiredEdge);
                if (candidatePieces == null) {
                    candidatePieces = new HashSet<>(compatible);
                } else {
                    candidatePieces.retainAll(compatible);
                }
            }
            if (west != null) {
                int requiredEdge = west.edges[1];  // Need to match west's east edge
                Set<Integer> compatible = valueOrderer.getWestCompatiblePieces(requiredEdge);
                if (candidatePieces == null) {
                    candidatePieces = new HashSet<>(compatible);
                } else {
                    candidatePieces.retainAll(compatible);
                }
            }
            if (east != null) {
                int requiredEdge = east.edges[3];  // Need to match east's west edge
                Set<Integer> compatible = valueOrderer.getEastCompatiblePieces(requiredEdge);
                if (candidatePieces == null) {
                    candidatePieces = new HashSet<>(compatible);
                } else {
                    candidatePieces.retainAll(compatible);
                }
            }
        }

        // Iterate pieces in order specified by sortOrder (using PieceIterator to eliminate duplication)
        for (int pid : PieceIterator.create(configManager.getSortOrder(), totalPieces, pieceUsed)) {
            // Early rejection: if this piece can't match neighbors, skip entirely
            if (candidatePieces != null && !candidatePieces.contains(pid)) {
                stats.fitChecks++;  // Count as rejected fit check
                continue;
            }

            Piece piece = piecesById.get(pid);
            for (int rot = 0; rot < 4; rot++) {
                int[] candidate = piece.edgesRotated(rot);
                if (fits(board, r, c, candidate)) {
                    validPlacements.add(new DomainManager.ValidPlacement(pid, rot));
                }
            }
        }
        return validPlacements;
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


    /**
     * Trouve la prochaine case vide (ordre row-major simple).
     *
     * @param board grille actuelle
     * @return coordonnées [r, c] de la première case vide, ou null si aucune
     */
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

    /**
     * Résout le puzzle en utilisant le backtracking récursif.
     *
     * @param board grille modifiée en place
     * @param piecesById map des pièces originales par ID
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @return true si une solution a été trouvée
     */
    public boolean solveBacktracking(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        stats.recursiveCalls++;

        // Vérifier si un autre thread a trouvé la solution
        if (solutionFound.get()) {
            return false; // Arrêter cette branche
        }

        // Vérifier si on a atteint un nouveau record de profondeur
        // IMPORTANT: exclure les pièces fixes du calcul (on compte seulement les pièces posées par le backtracking)
        int usedCount = 0;
        for (int i = 1; i <= totalPieces; i++) {
            if (pieceUsed.get(i)) usedCount++;
        }
        int currentDepth = usedCount - configManager.getNumFixedPieces();

        // Check and update records using RecordManager
        if (recordManager != null) {
            RecordManager.RecordCheckResult recordResult =
                recordManager.checkAndUpdateRecord(board, piecesById, currentDepth, stats.backtracks);

            if (recordResult != null) {
                // Save record to disk if new record achieved
                if (autoSaveManager != null) {
                    autoSaveManager.saveRecord(board, pieceUsed, totalPieces, stats, currentDepth);
                }

                // Display record if it should be shown
                if (recordManager.shouldShowRecord(recordResult, currentDepth)) {
                    recordManager.displayRecord(recordResult, usedCount, stats);
                }
            }
        }

        // Affichage verbose désactivé pour réduire les logs console
        // if (verbose && currentDepth >= minDepthToShowRecords) {
        //     // Nettoyer l'écran (clear terminal)
        //     System.out.print("\033[H\033[2J");
        //     System.out.flush();
        //
        //     System.out.println("\n" + "=".repeat(60));
        //     System.out.println("🏆 NOUVEAU RECORD ! " + currentDepth + " pièces placées sur " + piecesById.size());
        //     System.out.println("=".repeat(60));
        //
        //     // Afficher les statistiques
        //     System.out.println("╔════════════════ STATISTIQUES ═══════════════════╗");
        //     System.out.println("║ Temps écoulé       : " + String.format("%.2f", (System.currentTimeMillis() - stats.startTime) / 1000.0) + " secondes");
        //     System.out.println("║ Appels récursifs   : " + stats.recursiveCalls);
        //     System.out.println("║ Placements testés  : " + stats.placements);
        //     System.out.println("║ Backtracks         : " + stats.backtracks);
        //     System.out.println("║ Vérifications fit  : " + stats.fitChecks);
        //     System.out.println("║ Singletons trouvés : " + stats.singletonsFound);
        //     System.out.println("║ Singletons posés   : " + stats.singletonsPlaced);
        //     System.out.println("║ Dead-ends détectés : " + stats.deadEndsDetected);
        //     System.out.println("╚══════════════════════════════════════════════════╝");
        //
        //     // Afficher le board actuel
        //     System.out.println("\nPuzzle actuel:");
        //     printBoardCompact(board, piecesById, unusedIds);
        //     System.out.println();
        // }

        // Sauvegarde périodique de l'état du thread (tous les 5 minutes)
        long currentTime = System.currentTimeMillis();
        if (threadId >= 0 && (currentTime - lastThreadSaveTime > THREAD_SAVE_INTERVAL)) {
            lastThreadSaveTime = currentTime;
            SaveManager.saveThreadState(board, piecesById, currentDepth, threadId, randomSeed);
        }

        // Sauvegarde automatique périodique (tous les 10 minutes)
        if (autoSaveManager != null) {
            autoSaveManager.checkAndSave(board, pieceUsed, totalPieces, stats);
        }

        // Vérifier le timeout
        if (currentTime - startTimeMs > configManager.getMaxExecutionTimeMs()) {
            System.out.println("⏱️  " + configManager.getThreadLabel() + " Timeout atteint (" + (configManager.getMaxExecutionTimeMs() / 1000) + "s) - arrêt de la recherche");
            return false; // Timeout atteint
        }

        // Vérifier s'il reste des cases vides
        int[] cell = findNextCellMRV(board, piecesById, pieceUsed, totalPieces);
        if (cell == null) {
            // Aucune case vide -> solution trouvée
            solutionFound.set(true); // Signaler aux autres threads
            stats.end();
            if (configManager.isVerbose()) {
                System.out.println("\n========================================");
                System.out.println("SOLUTION TROUVÉE !");
                System.out.println("========================================");
            }
            return true;
        }

        // Create backtracking context for strategies
        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, totalPieces, stats, configManager.getNumFixedPieces()
        );

        // ÉTAPE 1 : Try singleton placement strategy first (most constrained)
        if (singletonStrategy.tryPlacement(context, this)) {
            return true;
        }

        // ÉTAPE 2 : Try MRV placement strategy
        return mrvStrategy.tryPlacement(context, this);
    }

    /**
     * Résout le puzzle en reprenant depuis un état pré-chargé avec historique de placement.
     * Permet de backtracker à travers toutes les pièces pré-chargées, pas seulement celles
     * placées durant cette exécution.
     *
     * @param board grille avec pièces déjà placées
     * @param allPieces map de TOUTES les pièces (utilisées et non utilisées)
     * @param unusedIds liste des IDs de pièces non encore utilisées
     * @param preloadedOrder historique complet de l'ordre de placement (pour permettre le backtracking)
     * @return true si le puzzle a été résolu
     */
    public boolean solveWithHistory(Board board, Map<Integer, Piece> allPieces,
                                     List<Integer> unusedIds,
                                     List<SaveStateManager.PlacementInfo> preloadedOrder) {
        // Récupérer le temps déjà cumulé depuis les sauvegardes précédentes
        long previousComputeTime = SaveStateManager.readTotalComputeTime(configManager.getPuzzleName());
        stats.start(previousComputeTime);

        // Initialiser PlacementOrderTracker avec l'historique fourni
        this.placementOrderTracker = new PlacementOrderTracker();
        this.placementOrderTracker.initializeWithHistory(preloadedOrder);

        // Détecter les positions fixes (celles qu'on ne doit JAMAIS backtracker)
        // Pour l'instant, aucune position n'est vraiment "fixe" - on peut tout backtracker

        // Initialiser numFixedPieces et initialFixedPieces depuis le fichier de configuration
        int numFixed = configManager.calculateNumFixedPieces(configManager.getPuzzleName());
        configManager.buildInitialFixedPieces(preloadedOrder, numFixed);

        // fixedPositions and initialFixedPieces removed - use configManager directly (Refactoring #15)

        // Initialize AutoSaveManager (AFTER fixed pieces calculation)
        this.autoSaveManager = configManager.createAutoSaveManager(
            placementOrderTracker != null ? placementOrderTracker.getPlacementHistory() : new ArrayList<>(),
            allPieces);

        // Initialize RecordManager (AFTER fixed pieces calculation)
        configManager.setThreadId(threadId);
        this.recordManager = configManager.createRecordManager(lockObject, globalMaxDepth,
            globalBestScore, globalBestThreadId, globalBestBoard, globalBestPieces);

        // Initialize BacktrackingHistoryManager
        this.backtrackingHistoryManager = new BacktrackingHistoryManager(
            null, // validator will be set after SolverInitializer
            configManager.getThreadLabel(),
            stats);

        // Créer le tableau pieceUsed depuis unusedIds (Refactoring #14 - extracted to helper)
        int totalPieces = allPieces.size();
        BitSet pieceUsed = createPieceUsedBitSet(allPieces);
        for (int pid : allPieces.keySet()) {
            if (!unusedIds.contains(pid)) {
                pieceUsed.set(pid);
            }
        }

        // Initialize all helper components using SolverInitializer
        SolverInitializer initializer = new SolverInitializer(this, stats, configManager.getSortOrder(), configManager.isVerbose(),
            configManager.isPrioritizeBorders(), configManager.getFixedPositions());
        SolverInitializer.InitializedComponents components = initializer.initializeComponents(
            board, allPieces, pieceUsed, totalPieces);

        // Assign initialized components (CRITICAL: must be done before AC-3 initialization)
        assignSolverComponents(components);

        // Update BacktrackingHistoryManager with initialized validator
        if (this.backtrackingHistoryManager != null) {
            this.backtrackingHistoryManager = new BacktrackingHistoryManager(
                this.validator,
                configManager.getThreadLabel(),
                stats);
        }

        // Initialize domain cache if enabled (must be after component initialization)
        if (useDomainCache) {
            initializeDomainCache(board, allPieces, pieceUsed, totalPieces);
        }

        // Initialize AC-3 domains if enabled (must be after validator assignment)
        if (useAC3) {
            this.domainManager.initializeAC3Domains(board, allPieces, pieceUsed, totalPieces);
        }

        // Initialize placement strategies (Sprint 6 - Strategy Pattern)
        // CRITICAL: Must be initialized before solveBacktracking() is called
        this.singletonStrategy = new SingletonPlacementStrategy(
            singletonDetector, configManager.isUseSingletons(), configManager.isVerbose(),
            symmetryBreakingManager, constraintPropagator, domainManager
        );
        this.mrvStrategy = new MRVPlacementStrategy(
            configManager.isVerbose(), valueOrderer, symmetryBreakingManager,
            constraintPropagator, domainManager
        );

        System.out.println("  → Reprise avec " + preloadedOrder.size() + " pièces pré-chargées");
        System.out.println("  → Le backtracking pourra remonter à travers TOUTES les pièces");

        // Essayer de résoudre avec l'état actuel
        boolean result = solveBacktracking(board, allPieces, pieceUsed, totalPieces);

        // Si échec, utiliser BacktrackingHistoryManager pour backtracker à travers l'historique
        if (!result && this.backtrackingHistoryManager != null) {
            // Create a SequentialSolver callback that wraps solveBacktracking
            BacktrackingHistoryManager.SequentialSolver sequentialSolver =
                (b, pieces, used, total) -> solveBacktracking(b, pieces, used, total);

            result = this.backtrackingHistoryManager.backtrackThroughHistory(
                board, allPieces, pieceUsed,
                placementOrderTracker != null ? placementOrderTracker.getPlacementHistory() : new ArrayList<>(),
                sequentialSolver);
        }

        stats.end();
        return result;
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

        // fixedPositions, numFixedPieces, initialFixedPieces removed - use configManager directly (Refactoring #15)

        // Initialize AutoSaveManager (AFTER fixed pieces detection)
        this.autoSaveManager = configManager.createAutoSaveManager(
            placementOrderTracker != null ? placementOrderTracker.getPlacementHistory() : new ArrayList<>(),
            pieces);

        // Initialize RecordManager (AFTER fixed pieces detection)
        configManager.setThreadId(threadId);
        this.recordManager = configManager.createRecordManager(lockObject, globalMaxDepth,
            globalBestScore, globalBestThreadId, globalBestBoard, globalBestPieces);

        // Initialize all helper components using SolverInitializer
        SolverInitializer initializer = new SolverInitializer(this, stats, configManager.getSortOrder(), configManager.isVerbose(),
            configManager.isPrioritizeBorders(), configManager.getFixedPositions());
        SolverInitializer.InitializedComponents components = initializer.initializeComponents(
            board, pieces, pieceUsed, totalPieces);

        // Assign initialized components (CRITICAL: must be done before AC-3 initialization)
        assignSolverComponents(components);

        // Initialize domain cache if enabled (must be after component initialization)
        if (useDomainCache) {
            initializeDomainCache(board, pieces, pieceUsed, totalPieces);
        }

        // Initialize AC-3 domains if enabled (must be after validator assignment)
        if (useAC3) {
            this.domainManager.initializeAC3Domains(board, pieces, pieceUsed, totalPieces);
        }

        // Apply symmetry breaking constraints
        initializeSymmetryBreaking(board);

        // Initialize placement strategies (Sprint 6 - Strategy Pattern)
        initializePlacementStrategies();

        // Use work-stealing if enabled
        // Note: Work-stealing currently uses sequential backtracking
        // Full work-stealing parallelism is in ParallelSearchManager
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

    /**
     * Initialise le cache des domaines pour toutes les cases vides.
     */
    void initializeDomainCache(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) { // Package-private for ParallelSolverOrchestrator
        int cols = board.getCols();
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    int key = r * cols + c;
                }
            }
        }
    }
    /**
     * Initialize AC-3 domains for all empty cells.
     * This computes the initial valid placements for each empty cell and groups them by piece ID.
     */
    @SuppressWarnings("unchecked")
    /**
     * Filter an existing domain to keep only placements compatible with a neighbor constraint.
     * This is more efficient than recomputing the entire domain from scratch.
     *
     * @param currentDomain existing domain to filter
     * @param requiredEdge the edge value that must match
     * @param edgeIndex which edge of the piece must match (0=N, 1=E, 2=S, 3=W)
     * @param piecesById map of all pieces
     * @return filtered list of valid placements
     */
    private List<DomainManager.ValidPlacement> filterDomain(List<DomainManager.ValidPlacement> currentDomain, int requiredEdge,
                                               int edgeIndex, Map<Integer, Piece> piecesById) {
        if (currentDomain == null || currentDomain.isEmpty()) return new ArrayList<>();

        List<DomainManager.ValidPlacement> filtered = new ArrayList<>();
        for (DomainManager.ValidPlacement vp : currentDomain) {
            int[] edges = piecesById.get(vp.pieceId).edgesRotated(vp.rotation);
            if (edges[edgeIndex] == requiredEdge) {
                filtered.add(vp);
            }
        }
        return filtered;
    }

    /**
     * Retourne les statistiques de la dernière résolution.
     */
    public Statistics getStatistics() {
        return stats;
    }

    /**
     * Active ou désactive l'optimisation singleton.
     * @param enabled true pour activer, false pour désactiver
     */
    public void setUseSingletons(boolean enabled) {
        configManager.setUseSingletons(enabled);
        // useSingletons removed - ConfigurationManager is now single source of truth (Refactoring #15)
    }

    /**
     * Active ou désactive la priorisation des bords.
     * Quand activé, le solver remplit d'abord tous les bords avant de remplir l'intérieur.
     * @param enabled true pour activer, false pour désactiver
     */
    public void setPrioritizeBorders(boolean enabled) {
        configManager.setPrioritizeBorders(enabled);
        // prioritizeBorders removed - ConfigurationManager is now single source of truth (Refactoring #15)
    }

    /**
     * Active ou désactive l'affichage détaillé.
     * @param enabled true pour activer, false pour désactiver
     */
    public void setVerbose(boolean enabled) {
        configManager.setVerbose(enabled);
        // verbose removed - ConfigurationManager is now single source of truth (Refactoring #15)
    }

    /**
     * Vérifie si l'optimisation singleton est activée.
     */
    public boolean isUsingSingletons() {
        return configManager.isUseSingletons();
    }

    /**
     * Réinitialise le solveur pour une nouvelle résolution.
     * Réinitialise les statistiques, le compteur d'étapes, etc.
     */
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
