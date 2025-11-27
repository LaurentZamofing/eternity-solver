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

    private int stepCount = 0;
    private int lastPlacedRow = -1;
    private int lastPlacedCol = -1;
    private Statistics stats = new Statistics();
    private boolean useSingletons = true; // Activer/désactiver l'optimisation singleton
    private boolean verbose = true; // Activer/désactiver l'affichage détaillé
    private int minDepthToShowRecords = 0; // Profondeur min pour afficher les records (0 = toujours afficher)
    private Set<String> fixedPositions = new HashSet<>(); // Positions des pièces fixes (format: "row,col")
    private int numFixedPieces = 0; // Nombre de pièces fixes au démarrage
    private List<SaveStateManager.PlacementInfo> initialFixedPieces = new ArrayList<>(); // Pièces fixes INITIALES (du config)
    private boolean prioritizeBorders = false; // Prioriser le remplissage des bords avant l'intérieur

    // Timeout management
    private long maxExecutionTimeMs = Long.MAX_VALUE; // Temps maximum d'exécution (défaut: illimité)
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

    // Pre-computed constraints for each cell (optimization)
    private CellConstraints[][] cellConstraints;

    // Randomisation pour éviter le thrashing
    private Random random = new Random();
    private double randomizationProbability = 0.3; // 30% de chance de mélanger l'ordre
    private int stagnationThreshold = 50000; // Seuil de backtracks sans progrès avant restart partiel

    // Parallel search - MIGRATED to ParallelSearchManager (Sprint 4)
    // Keep minimal references for backward compatibility
    private static AtomicBoolean solutionFound = ParallelSearchManager.getSolutionFound();
    private static AtomicInteger globalMaxDepth = ParallelSearchManager.getGlobalMaxDepth();
    private static AtomicInteger globalBestScore = ParallelSearchManager.getGlobalBestScore();
    private static AtomicInteger globalBestThreadId = ParallelSearchManager.getGlobalBestThreadId();
    private static AtomicReference<Board> globalBestBoard = ParallelSearchManager.getGlobalBestBoard();
    private static AtomicReference<Map<Integer, Piece>> globalBestPieces = ParallelSearchManager.getGlobalBestPieces();
    private static final Object lockObject = ParallelSearchManager.getLockObject();
    private int threadId = -1; // ID du thread pour ce solveur

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
        this.verbose = verbose; // Keep for backward compatibility
        this.minDepthToShowRecords = minDepth; // Keep for backward compatibility
    }

    /**
     * Configure le nom du puzzle pour la sauvegarde automatique
     * @param name nom du puzzle
     */
    public void setPuzzleName(String name) {
        configManager.setPuzzleName(name);
        this.puzzleName = name; // Keep for backward compatibility
    }

    /**
     * Configure l'ordre de tri des pièces
     * @param order "ascending" ou "descending"
     */
    public void setSortOrder(String order) {
        configManager.setSortOrder(order);
        this.sortOrder = order; // Keep for backward compatibility
    }

    public void setNumFixedPieces(int num) {
        configManager.setNumFixedPieces(num);
        this.numFixedPieces = num; // Keep for backward compatibility
    }

    /**
     * Définir le timeout maximum pour la résolution (en millisecondes)
     */
    public void setMaxExecutionTime(long timeMs) {
        configManager.setMaxExecutionTime(timeMs);
        this.maxExecutionTimeMs = timeMs; // Keep for backward compatibility
    }

    /**
     * Configure le label du thread pour les logs
     * @param label label à afficher dans les logs (ex: "[Thread 1 - p01_asc]")
     */
    public void setThreadLabel(String label) {
        configManager.setThreadLabel(label);
        this.threadLabel = label; // Keep for backward compatibility
    }

    // Sauvegarde périodique par thread
    private long randomSeed = 0; // Seed du random pour ce thread
    private long lastThreadSaveTime = 0; // Timestamp de la dernière sauvegarde thread
    private static final long THREAD_SAVE_INTERVAL = 60000; // 1 minute en millisecondes

    // Sauvegarde automatique périodique (nouveau système)
    private String puzzleName = "eternity2"; // Nom du puzzle pour le fichier de sauvegarde

    // Label du thread pour les logs (ex: "[Thread 1 - p01_asc]")
    private String threadLabel = "";

    // Ordre de tri des pièces pour parallélisation
    private String sortOrder = "ascending"; // "ascending" ou "descending"

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
    private void printBoardWithCounts(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces,
                                      int lastPlacedRow, int lastPlacedCol) {
        BoardVisualizer.printBoardWithCounts(board, piecesById, pieceUsed, totalPieces,
                                            lastPlacedRow, lastPlacedCol, this::fits);
    }

    // ==================== PlacementOrder Helpers ====================

    /**
     * Record a placement using PlacementOrderTracker
     */
    private void recordPlacement(int row, int col, int pieceId, int rotation) {
        if (placementOrderTracker != null) {
            placementOrderTracker.recordPlacement(row, col, pieceId, rotation);
        }
    }

    /**
     * Remove last placement using PlacementOrderTracker
     */
    private SaveStateManager.PlacementInfo removeLastPlacement() {
        if (placementOrderTracker != null) {
            return placementOrderTracker.removeLastPlacement();
        }
        return null;
    }

    // ==================== End PlacementOrder Helpers ====================

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
        for (int pid : PieceIterator.create(sortOrder, totalPieces, pieceUsed)) {
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
     * Compte le nombre de placements valides (pièce, rotation) pour une case donnée.
     *
     * @param board grille actuelle
     * @param r ligne
     * @param c colonne
     * @param piecesById map des pièces par ID
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @return nombre de combinaisons (pièce, rotation) valides
     */
    private int countValidPlacements(Board board, int r, int c, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        return getValidPlacements(board, r, c, piecesById, pieceUsed, totalPieces).size();
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
    private int countUniquePieces(Board board, int r, int c, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        List<Integer> validPieceIds = new ArrayList<>();
        // Iterate pieces in order specified by sortOrder (using PieceIterator to eliminate duplication)
        for (int pid : PieceIterator.create(sortOrder, totalPieces, pieceUsed)) {
            Piece piece = piecesById.get(pid);
            boolean foundValidRotation = false;
            for (int rot = 0; rot < 4 && !foundValidRotation; rot++) {
                int[] candidate = piece.edgesRotated(rot);
                if (fits(board, r, c, candidate)) {
                    validPieceIds.add(pid);
                    foundValidRotation = true;
                }
            }
        }
        return validPieceIds.size();
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
     * Ordonne les pièces selon l'heuristique "Least Constraining Value".
     * Les pièces qui laissent le plus d'options aux voisins sont essayées en premier.
     *
     * @param board grille actuelle
     * @param r ligne de la case à remplir
     * @param c colonne de la case à remplir
     * @param pieceIds liste des IDs de pièces à ordonner
     * @param piecesById toutes les pièces
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @return liste ordonnée des IDs de pièces
     */
    private List<Integer> orderPiecesByLeastConstraining(Board board, int r, int c,
                                                          List<Integer> pieceIds,
                                                          Map<Integer, Piece> piecesById,
                                                          BitSet pieceUsed, int totalPieces) {
        // Pour chaque pièce, calculer un score de "contrainte"
        // Plus le score est bas, moins la pièce contraint les voisins
        List<PieceOrderingOptimizer.PieceScore> scores = new ArrayList<>();

        for (int pid : pieceIds) {
            Piece piece = piecesById.get(pid);
            int minConstraint = Integer.MAX_VALUE;

            // Trouver la meilleure rotation pour cette pièce
            for (int rot = 0; rot < 4; rot++) {
                int[] candidate = piece.edgesRotated(rot);

                if (fits(board, r, c, candidate)) {
                    // Calculer combien cette pièce/rotation contraint les voisins
                    int constraint = calculateConstraintScore(board, r, c, candidate, piecesById, pieceUsed, totalPieces, pid);
                    minConstraint = Math.min(minConstraint, constraint);
                }
            }

            scores.add(new PieceOrderingOptimizer.PieceScore(pid, minConstraint));
        }

        // Trier par score croissant (moins contraignant d'abord)
        scores.sort((a, b) -> Integer.compare(a.score, b.score));

        // Retourner la liste ordonnée des IDs
        List<Integer> ordered = new ArrayList<>();
        for (PieceOrderingOptimizer.PieceScore ps : scores) {
            ordered.add(ps.pieceId);
        }

        return ordered;
    }

    /**
     * Calcule un score de contrainte pour un placement donné.
     * Plus le score est élevé, plus le placement contraint les voisins.
     *
     * @param board grille actuelle
     * @param r ligne
     * @param c colonne
     * @param candidateEdges edges de la pièce candidate
     * @param piecesById toutes les pièces
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @param excludePieceId pièce à exclure
     * @return score de contrainte (plus bas = mieux)
     */
    private int calculateConstraintScore(Board board, int r, int c, int[] candidateEdges,
                                          Map<Integer, Piece> piecesById,
                                          BitSet pieceUsed, int totalPieces, int excludePieceId) {
        int rows = board.getRows();
        int cols = board.getCols();
        int totalRemovedOptions = 0;

        // Pour chaque voisin vide, compter combien d'options il perdrait
        // Voisin du haut
        if (r > 0 && board.isEmpty(r - 1, c)) {
            int optionsBefore = countValidPieces(board, r - 1, c, -1, -1, piecesById, pieceUsed, totalPieces, -1);
            int optionsAfter = countValidPieces(board, r - 1, c, candidateEdges[0], 2, piecesById, pieceUsed, totalPieces, excludePieceId);
            totalRemovedOptions += (optionsBefore - optionsAfter);
        }

        // Voisin du bas
        if (r < rows - 1 && board.isEmpty(r + 1, c)) {
            int optionsBefore = countValidPieces(board, r + 1, c, -1, -1, piecesById, pieceUsed, totalPieces, -1);
            int optionsAfter = countValidPieces(board, r + 1, c, candidateEdges[2], 0, piecesById, pieceUsed, totalPieces, excludePieceId);
            totalRemovedOptions += (optionsBefore - optionsAfter);
        }

        // Voisin de gauche
        if (c > 0 && board.isEmpty(r, c - 1)) {
            int optionsBefore = countValidPieces(board, r, c - 1, -1, -1, piecesById, pieceUsed, totalPieces, -1);
            int optionsAfter = countValidPieces(board, r, c - 1, candidateEdges[3], 1, piecesById, pieceUsed, totalPieces, excludePieceId);
            totalRemovedOptions += (optionsBefore - optionsAfter);
        }

        // Voisin de droite
        if (c < cols - 1 && board.isEmpty(r, c + 1)) {
            int optionsBefore = countValidPieces(board, r, c + 1, -1, -1, piecesById, pieceUsed, totalPieces, -1);
            int optionsAfter = countValidPieces(board, r, c + 1, candidateEdges[1], 3, piecesById, pieceUsed, totalPieces, excludePieceId);
            totalRemovedOptions += (optionsBefore - optionsAfter);
        }

        return totalRemovedOptions;
    }

    /**
     * Compte le nombre de pièces valides pour une position donnée.
     *
     * @param board grille
     * @param r ligne
     * @param c colonne
     * @param requiredEdge arête requise (-1 si aucune contrainte spécifique)
     * @param edgeIndex index de l'arête (-1 si aucune contrainte)
     * @param piecesById toutes les pièces
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @param excludePieceId pièce à exclure
     * @return nombre de pièces valides
     */
    private int countValidPieces(Board board, int r, int c, int requiredEdge, int edgeIndex,
                                  Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces, int excludePieceId) {
        int count = 0;

        // Iterate pieces in order specified by sortOrder (using PieceIterator to eliminate duplication)
        for (int pid : PieceIterator.create(sortOrder, totalPieces, pieceUsed)) {
            if (pid == excludePieceId) continue;

            Piece piece = piecesById.get(pid);
            for (int rot = 0; rot < 4; rot++) {
                int[] edges = piece.edgesRotated(rot);

                // Vérifier la contrainte d'arête si spécifiée
                if (requiredEdge != -1 && edgeIndex != -1) {
                    if (edges[edgeIndex] != requiredEdge) continue;
                }

                if (fits(board, r, c, edges)) {
                    count++;
                }
            }
        }

        return count;
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
        int currentDepth = usedCount - numFixedPieces;

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
        if (currentTime - startTimeMs > maxExecutionTimeMs) {
            System.out.println("⏱️  " + threadLabel + " Timeout atteint (" + (maxExecutionTimeMs / 1000) + "s) - arrêt de la recherche");
            return false; // Timeout atteint
        }

        // Vérifier s'il reste des cases vides
        int[] cell = findNextCellMRV(board, piecesById, pieceUsed, totalPieces);
        if (cell == null) {
            // Aucune case vide -> solution trouvée
            solutionFound.set(true); // Signaler aux autres threads
            stats.end();
            if (verbose) {
                System.out.println("\n========================================");
                System.out.println("SOLUTION TROUVÉE !");
                System.out.println("========================================");
            }
            return true;
        }

        // ÉTAPE 1 : Vérifier s'il y a une pièce singleton (qui ne peut aller qu'à un seul endroit)
        // Seulement si l'optimisation singleton est activée
        SingletonDetector.SingletonInfo singleton = useSingletons ? singletonDetector.findSingletonPiece(board, piecesById, pieceUsed, totalPieces) : null;
        if (singleton != null) {
            // Forcer le placement du singleton
            int r = singleton.row;
            int c = singleton.col;
            int pid = singleton.pieceId;
            int rot = singleton.rotation;

            Piece piece = piecesById.get(pid);
            int[] candidate = piece.edgesRotated(rot);

            if (verbose) {
                int availableCount = 0;
                for (int i = 1; i <= totalPieces; i++) {
                    if (!pieceUsed.get(i)) availableCount++;
                }
                System.out.println("\n╔════════════════════════════════════════╗");
                System.out.println("║  Étape " + (++stepCount) + " - SINGLETON FORCÉ");
                System.out.println("║  Pièce " + pid + " → Case (" + r + ", " + c + ")");
                System.out.println("║  Pièces disponibles : " + availableCount);
                System.out.println("╚════════════════════════════════════════╝");
                // Afficher les stats compactes
                stats.printCompact();
            }

            // Symmetry Breaking: vérifier que ce singleton ne viole pas les contraintes
            if (symmetryBreakingManager != null &&
                !symmetryBreakingManager.isPlacementAllowed(board, r, c, pid, rot, piecesById)) {
                // Le singleton viole les contraintes de symétrie - impossible
                // Retourner false car pas de solution avec ce singleton
                return false;
            }

            // Placer
            board.place(r, c, piece, rot);
            pieceUsed.set(pid);
            lastPlacedRow = r;
            lastPlacedCol = c;
            stats.placements++;
            stats.singletonsPlaced++;

            // NOUVEAU: Tracker l'ordre de placement
            recordPlacement(r, c, pid, rot);

            // AC-3: Propagate constraints for singleton
            if (!constraintPropagator.propagateAC3(board, r, c, pid, rot, piecesById, pieceUsed, totalPieces)) {
                // Dead end detected by AC-3 for singleton - very rare but possible
                if (verbose) {
                    System.out.println("✗ AC-3 dead-end détecté pour singleton : ID=" + pid + " à (" + r + ", " + c + ")");
                }
                pieceUsed.clear(pid);
                board.remove(r, c);
                removeLastPlacement();
                domainManager.restoreAC3Domains(board, r, c, piecesById, pieceUsed, totalPieces);
                return false; // Singleton created dead-end, fail immediately
            }

            // Afficher le board
            if (verbose) {
                printBoardWithCounts(board, piecesById, pieceUsed, totalPieces, lastPlacedRow, lastPlacedCol);
                System.out.println("✓ Singleton posé : ID=" + pid + ", Rotation=" + (rot * 90) + "°, Arêtes=" + java.util.Arrays.toString(candidate));
            }

            // Appel récursif
            boolean solved = solveBacktracking(board, piecesById, pieceUsed, totalPieces);
            if (solved) {
                return true;
            }

            // Backtrack
            stats.backtracks++;
            if (verbose) {
                System.out.println("✗ BACKTRACK du singleton : Retrait de la pièce ID=" + pid + " à (" + r + ", " + c + ")");
            }
            pieceUsed.clear(pid);
            board.remove(r, c);

            // NOUVEAU: Retirer du tracking de l'ordre
            removeLastPlacement();

            // AC-3: Restore domains after singleton backtrack
            domainManager.restoreAC3Domains(board, r, c, piecesById, pieceUsed, totalPieces);

            // Trouver la vraie dernière pièce posée
            lastPlacedRow = -1;
            lastPlacedCol = -1;
            outer1: for (int rr = board.getRows() - 1; rr >= 0; rr--) {
                for (int cc = board.getCols() - 1; cc >= 0; cc--) {
                    if (!board.isEmpty(rr, cc)) {
                        lastPlacedRow = rr;
                        lastPlacedCol = cc;
                        break outer1;
                    }
                }
            }

            return false; // Le singleton n'a pas mené à une solution
        }

        // ÉTAPE 2 : Pas de singleton, utiliser MRV classique
        int r = cell[0];
        int c = cell[1];

        // Compter les possibilités pour cette case (pour l'affichage)
        if (verbose) {
            int uniquePieces = countUniquePieces(board, r, c, piecesById, pieceUsed, totalPieces);
            int availableCount = 0;
            for (int i = 1; i <= totalPieces; i++) {
                if (!pieceUsed.get(i)) availableCount++;
            }

            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║  Étape " + (++stepCount) + " - Case (" + r + ", " + c + ")");
            System.out.println("║  Pièces disponibles : " + availableCount);
            System.out.println("║  Pièces possibles ici : " + uniquePieces);
            System.out.println("╚════════════════════════════════════════╝");

            // Afficher les stats compactes
            stats.printCompact();

            // Afficher le board avec les comptes
            printBoardWithCounts(board, piecesById, pieceUsed, totalPieces, lastPlacedRow, lastPlacedCol);
        }

        // Essayer chaque pièce non utilisée
        // Construire la liste des pièces disponibles
        List<Integer> snapshot = new ArrayList<>();
        for (int i = 1; i <= totalPieces; i++) {
            if (!pieceUsed.get(i)) snapshot.add(i);
        }

        // SMART PIECE ORDERING:
        // Sort by difficulty (hardest pieces first for fail-fast)
        // If difficulty scores not available, fallback to ID order for determinism
        if (valueOrderer.getAllDifficultyScores() != null) {
            snapshot.sort(Comparator.comparingInt(pid -> valueOrderer.getAllDifficultyScores().getOrDefault(pid, Integer.MAX_VALUE)));
        } else {
            Collections.sort(snapshot);  // Fallback to ID order
        }

        // Enregistrer le nombre d'options à cette profondeur (seulement pour les 5 premières)
        // Réutiliser la variable currentDepth déjà calculée plus haut
        if (currentDepth < 5) {
            stats.registerDepthOptions(currentDepth, snapshot.size());
        }

        int tentatives = 0;
        int optionIndex = 0;
        for (int pid : snapshot) {
            // Incrémenter la progression pour cette profondeur
            if (currentDepth < 5 && optionIndex > 0) {
                stats.incrementDepthProgress(currentDepth);
            }
            optionIndex++;
            Piece piece = piecesById.get(pid);

            // Optimisation: ne tester que les rotations uniques
            int maxRotations = piece.getUniqueRotationCount();

            for (int rot = 0; rot < maxRotations; rot++) {
                int[] candidate = piece.edgesRotated(rot);
                tentatives++;

                if (fits(board, r, c, candidate)) {
                    // Symmetry Breaking: vérifier que ce placement ne viole pas les contraintes de symétrie
                    if (symmetryBreakingManager != null &&
                        !symmetryBreakingManager.isPlacementAllowed(board, r, c, pid, rot, piecesById)) {
                        continue; // Ce placement viole les contraintes de symétrie, l'ignorer
                    }

                    // Forward Checking : vérifier que ce placement ne crée pas de dead-end chez les voisins
                    if (!forwardCheck(board, r, c, candidate, piecesById, pieceUsed, totalPieces, pid)) {
                        stats.forwardCheckRejects++;
                        continue; // Ce placement créerait un dead-end, l'ignorer
                    }

                    // Placer
                    board.place(r, c, piece, rot);
                    pieceUsed.set(pid);
                    stats.placements++;

                    // NOUVEAU: Tracker l'ordre de placement
                    recordPlacement(r, c, pid, rot);

                    // Mettre à jour le cache des domaines
                    domainManager.updateCacheAfterPlacement(board, r, c, piecesById, pieceUsed, totalPieces);

                    // AC-3: Propagate constraints to neighbors
                    if (!constraintPropagator.propagateAC3(board, r, c, pid, rot, piecesById, pieceUsed, totalPieces)) {
                        // Dead end detected by AC-3 - backtrack immediately
                        if (verbose) {
                            System.out.println("✗ AC-3 dead-end détecté : ID=" + pid + " à (" + r + ", " + c + ")");
                        }
                        pieceUsed.clear(pid);
                        board.remove(r, c);
                        removeLastPlacement();
                        domainManager.restoreCacheAfterBacktrack(board, r, c, piecesById, pieceUsed, totalPieces);
                        domainManager.restoreAC3Domains(board, r, c, piecesById, pieceUsed, totalPieces);
                        continue; // Try next rotation/piece
                    }

                    // Mettre à jour la position de la dernière pièce posée
                    lastPlacedRow = r;
                    lastPlacedCol = c;

                    if (verbose) {
                        System.out.println("✓ Pièce posée : ID=" + pid + ", Rotation=" + (rot * 90) + "°, Arêtes=" + java.util.Arrays.toString(candidate));
                    }

                    // Appel récursif
                    boolean solved = solveBacktracking(board, piecesById, pieceUsed, totalPieces);
                    if (solved) {
                        return true;
                    }

                    // Backtrack
                    stats.backtracks++;
                    if (verbose) {
                        System.out.println("✗ BACKTRACK : Retrait de la pièce ID=" + pid + " à (" + r + ", " + c + ")");
                    }
                    pieceUsed.clear(pid);
                    board.remove(r, c);

                    // NOUVEAU: Retirer du tracking de l'ordre
                    removeLastPlacement();

                    // Restaurer le cache après le backtrack
                    domainManager.restoreCacheAfterBacktrack(board, r, c, piecesById, pieceUsed, totalPieces);

                    // AC-3: Restore domains after backtrack
                    domainManager.restoreAC3Domains(board, r, c, piecesById, pieceUsed, totalPieces);

                    // Trouver la vraie dernière pièce posée sur le board
                    lastPlacedRow = -1;
                    lastPlacedCol = -1;
                    outer2: for (int rr = board.getRows() - 1; rr >= 0; rr--) {
                        for (int cc = board.getCols() - 1; cc >= 0; cc--) {
                            if (!board.isEmpty(rr, cc)) {
                                lastPlacedRow = rr;
                                lastPlacedCol = cc;
                                break outer2;
                            }
                        }
                    }
                }
            }
        }

        // Aucune pièce possible ici
        if (verbose) {
            System.out.println("\n✗ Aucune solution trouvée pour (" + r + ", " + c + ") après " + tentatives + " tentatives");
        }
        stats.deadEndsDetected++;
        return false;
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
        long previousComputeTime = SaveStateManager.readTotalComputeTime(puzzleName);
        stats.start(previousComputeTime);

        // Initialiser PlacementOrderTracker avec l'historique fourni
        this.placementOrderTracker = new PlacementOrderTracker();
        this.placementOrderTracker.initializeWithHistory(preloadedOrder);

        // Détecter les positions fixes (celles qu'on ne doit JAMAIS backtracker)
        this.fixedPositions = new HashSet<>();
        // Pour l'instant, aucune position n'est vraiment "fixe" - on peut tout backtracker

        // Initialiser numFixedPieces et initialFixedPieces depuis le fichier de configuration
        numFixedPieces = configManager.calculateNumFixedPieces(puzzleName);
        configManager.buildInitialFixedPieces(preloadedOrder, numFixedPieces);

        // Update local fields for backward compatibility
        initialFixedPieces = new ArrayList<>(configManager.getInitialFixedPieces());

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
            threadLabel,
            stats);

        // Créer le tableau pieceUsed depuis unusedIds - dimensionner selon MAX piece ID
        int totalPieces = allPieces.size();
        int maxPieceId = allPieces.keySet().stream().max(Integer::compareTo).orElse(totalPieces);
        BitSet pieceUsed = new BitSet(maxPieceId + 1); // index 0 inutilisé, 1-based
        for (int pid : allPieces.keySet()) {
            if (!unusedIds.contains(pid)) {
                pieceUsed.set(pid);
            }
        }

        // Initialize all helper components using SolverInitializer
        SolverInitializer initializer = new SolverInitializer(this, stats, sortOrder, verbose,
            prioritizeBorders, fixedPositions);
        SolverInitializer.InitializedComponents components = initializer.initializeComponents(
            board, allPieces, pieceUsed, totalPieces);

        // Assign initialized components (CRITICAL: must be done before AC-3 initialization)
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

        // Update BacktrackingHistoryManager with initialized validator
        if (this.backtrackingHistoryManager != null) {
            this.backtrackingHistoryManager = new BacktrackingHistoryManager(
                this.validator,
                threadLabel,
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

        // Créer le tableau pieceUsed - doit être dimensionné selon MAX piece ID, pas le count
        int totalPieces = pieces.size();
        int maxPieceId = pieces.keySet().stream().max(Integer::compareTo).orElse(totalPieces);
        BitSet pieceUsed = new BitSet(maxPieceId + 1); // index 0 inutilisé, 1-based

        // Détecter et mémoriser les positions des pièces fixes (déjà placées au début)
        configManager.detectFixedPiecesFromBoard(board, pieceUsed,
            placementOrderTracker != null ? placementOrderTracker.getPlacementHistory() : new ArrayList<>());

        // Update local fields for backward compatibility
        fixedPositions = new HashSet<>(configManager.getFixedPositions());
        numFixedPieces = configManager.getNumFixedPieces();
        initialFixedPieces = new ArrayList<>(configManager.getInitialFixedPieces());

        // Initialize AutoSaveManager (AFTER fixed pieces detection)
        this.autoSaveManager = configManager.createAutoSaveManager(
            placementOrderTracker != null ? placementOrderTracker.getPlacementHistory() : new ArrayList<>(),
            pieces);

        // Initialize RecordManager (AFTER fixed pieces detection)
        configManager.setThreadId(threadId);
        this.recordManager = configManager.createRecordManager(lockObject, globalMaxDepth,
            globalBestScore, globalBestThreadId, globalBestBoard, globalBestPieces);

        // Initialize all helper components using SolverInitializer
        SolverInitializer initializer = new SolverInitializer(this, stats, sortOrder, verbose,
            prioritizeBorders, fixedPositions);
        SolverInitializer.InitializedComponents components = initializer.initializeComponents(
            board, pieces, pieceUsed, totalPieces);

        // Assign initialized components (CRITICAL: must be done before AC-3 initialization)
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

        // Use work-stealing if enabled
        // Note: Work-stealing currently uses sequential backtracking
        // Full work-stealing parallelism is in ParallelSearchManager
        boolean solved = solveBacktracking(board, pieces, pieceUsed, totalPieces);

        if (!solved) {
            stats.end();
            if (verbose) {
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
            verbose
        );

        // Log configuration
        symmetryBreakingManager.logConfiguration();
    }

    /**
     * Initialise le cache des domaines pour toutes les cases vides.
     */
    private void initializeDomainCache(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
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
        this.useSingletons = enabled; // Keep for backward compatibility
    }

    /**
     * Active ou désactive la priorisation des bords.
     * Quand activé, le solver remplit d'abord tous les bords avant de remplir l'intérieur.
     * @param enabled true pour activer, false pour désactiver
     */
    public void setPrioritizeBorders(boolean enabled) {
        configManager.setPrioritizeBorders(enabled);
        this.prioritizeBorders = enabled; // Keep for backward compatibility
    }

    /**
     * Active ou désactive l'affichage détaillé.
     * @param enabled true pour activer, false pour désactiver
     */
    public void setVerbose(boolean enabled) {
        configManager.setVerbose(enabled);
        this.verbose = enabled; // Keep for backward compatibility
    }

    /**
     * Vérifie si l'optimisation singleton est activée.
     */
    public boolean isUsingSingletons() {
        return useSingletons;
    }

    /**
     * Réinitialise le solveur pour une nouvelle résolution.
     * Réinitialise les statistiques, le compteur d'étapes, etc.
     */
    public void reset() {
        stats = new Statistics();
        stepCount = 0;
        lastPlacedRow = -1;
        lastPlacedCol = -1;
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
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           RECHERCHE PARALLÈLE AVEC " + numThreads + " THREADS            ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        // Réinitialiser les flags globaux
        solutionFound.set(false);
        globalMaxDepth.set(0);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Boolean>> futures = new ArrayList<>();

        // Lancer numThreads workers avec des seeds différentes
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;

            Future<Boolean> future = executor.submit(() -> {
                try {
                    Board localBoard;
                    Map<Integer, Piece> localPieces;
                    List<Integer> unusedIds;
                    long seed = System.currentTimeMillis() + threadId * 1000;
                    boolean loadedFromSave = false;

                    // Vérifier si ce thread a une sauvegarde
                    if (SaveManager.hasThreadState(threadId)) {
                        // Charger l'état sauvegardé du thread
                        Object[] savedState = SaveManager.loadThreadState(threadId, allPieces);
                        if (savedState != null) {
                            localBoard = (Board) savedState[0];
                            @SuppressWarnings("unchecked")
                            Set<Integer> usedPieceIds = (Set<Integer>) savedState[1];
                            int savedDepth = (int) savedState[2];
                            seed = (long) savedState[3]; // Restaurer le seed original

                            // Créer la liste des pièces non utilisées
                            // IMPORTANT: localPieces doit contenir TOUTES les pièces (pour récupérer par ID)
                            localPieces = new HashMap<>(allPieces);
                            unusedIds = new ArrayList<>();

                            // Ajouter seulement les pièces non utilisées à unusedIds
                            for (int pid : allPieces.keySet()) {
                                if (!usedPieceIds.contains(pid)) {
                                    unusedIds.add(pid);
                                }
                            }

                            loadedFromSave = true;
                            synchronized (System.out) {
                                System.out.println("📂 Thread " + threadId + " restauré depuis sauvegarde: " + savedDepth + " pièces placées");
                            }
                        } else {
                            // Erreur de chargement, commencer normalement
                            localBoard = new Board(board.getRows(), board.getCols());
                            for (int r = 0; r < board.getRows(); r++) {
                                for (int c = 0; c < board.getCols(); c++) {
                                    if (!board.isEmpty(r, c)) {
                                        Placement p = board.getPlacement(r, c);
                                        Piece piece = allPieces.get(p.getPieceId());
                                        if (piece != null) {
                                            localBoard.place(r, c, piece, p.getRotation());
                                        }
                                    }
                                }
                            }
                            localPieces = new HashMap<>(allPieces);
                            unusedIds = new ArrayList<>(availablePieces.keySet());
                        }
                    } else {
                        // Pas de sauvegarde - créer une copie du board pour ce thread
                        localBoard = new Board(board.getRows(), board.getCols());

                        // Copier les pièces déjà placées (hint)
                        for (int r = 0; r < board.getRows(); r++) {
                            for (int c = 0; c < board.getCols(); c++) {
                                if (!board.isEmpty(r, c)) {
                                    Placement p = board.getPlacement(r, c);
                                    Piece piece = allPieces.get(p.getPieceId());
                                    if (piece != null) {
                                        localBoard.place(r, c, piece, p.getRotation());
                                    }
                                }
                            }
                        }

                        // Créer une copie des pièces disponibles
                        localPieces = new HashMap<>(allPieces);
                        unusedIds = new ArrayList<>(availablePieces.keySet());
                    }

                    // Créer le tableau pieceUsed - dimensionner selon MAX piece ID
                    int totalPieces = localPieces.size();
                    int maxPieceId = localPieces.keySet().stream().max(Integer::compareTo).orElse(totalPieces);
                    BitSet pieceUsed = new BitSet(maxPieceId + 1);
                    // Marquer les pièces déjà utilisées
                    for (int pid : localPieces.keySet()) {
                        if (!unusedIds.contains(pid)) {
                            pieceUsed.set(pid);
                        }
                    }

                    // Stratégie de diversification: pré-placer un coin différent pour chaque thread
                    // Cela force les threads à explorer des branches complètement différentes
                    // Seulement si on n'a pas chargé depuis une sauvegarde
                    Integer cornerPieceId = null;
                    int cornerRow = -1, cornerCol = -1, cornerRot = -1;

                    if (!loadedFromSave && threadId < 4 && unusedIds.size() > 10) {
                        // Les 4 premiers threads: fixer les 4 coins avec des pièces différentes
                        // Identifier les pièces de coin (avec 2 bords à 0)
                        List<Integer> cornerPieces = new ArrayList<>();
                        for (int pid : unusedIds) {
                            Piece p = localPieces.get(pid);
                            int[] edges = p.getEdges();
                            int zeroCount = 0;
                            for (int e : edges) {
                                if (e == 0) zeroCount++;
                            }
                            if (zeroCount == 2) {
                                cornerPieces.add(pid);
                            }
                        }

                        if (threadId < cornerPieces.size()) {
                            cornerPieceId = cornerPieces.get(threadId);
                            // Position du coin selon le thread
                            switch (threadId) {
                                case 0: cornerRow = 0; cornerCol = 0; break;           // Haut-gauche
                                case 1: cornerRow = 0; cornerCol = 15; break;          // Haut-droite
                                case 2: cornerRow = 15; cornerCol = 0; break;          // Bas-gauche
                                case 3: cornerRow = 15; cornerCol = 15; break;         // Bas-droite
                            }

                            Piece cornerPiece = localPieces.get(cornerPieceId);
                            // Trouver la rotation qui met les 0 aux bons bords
                            for (int rot = 0; rot < 4; rot++) {
                                int[] rotEdges = cornerPiece.edgesRotated(rot);
                                boolean valid = false;
                                if (cornerRow == 0 && cornerCol == 0 && rotEdges[0] == 0 && rotEdges[3] == 0) valid = true;      // N=0, W=0
                                if (cornerRow == 0 && cornerCol == 15 && rotEdges[0] == 0 && rotEdges[1] == 0) valid = true;     // N=0, E=0
                                if (cornerRow == 15 && cornerCol == 0 && rotEdges[2] == 0 && rotEdges[3] == 0) valid = true;     // S=0, W=0
                                if (cornerRow == 15 && cornerCol == 15 && rotEdges[2] == 0 && rotEdges[1] == 0) valid = true;    // S=0, E=0

                                if (valid) {
                                    cornerRot = rot;
                                    localBoard.place(cornerRow, cornerCol, cornerPiece, rot);
                                    pieceUsed.set(cornerPieceId);
                                    break;
                                }
                            }
                        }
                    }

                    // Créer un solveur local avec une seed différente
                    EternitySolver localSolver = new EternitySolver();
                    localSolver.random = new Random(seed);
                    localSolver.randomSeed = seed; // Sauvegarder le seed pour la sauvegarde
                    localSolver.threadId = threadId; // Définir l'ID du thread
                    localSolver.puzzleName = puzzleName; // CRITIQUE: Nécessaire pour la sauvegarde des records
                    // Note: AutoSaveManager will be initialized with allPieces in solve()
                    localSolver.setVerbose(false); // Désactiver l'affichage pour les threads
                    localSolver.setUseSingletons(true);

                    synchronized (System.out) {
                        if (cornerPieceId != null) {
                            System.out.println("🚀 Thread " + threadId + " démarré (seed=" + seed + ") - Coin fixé: pièce " + cornerPieceId + " à (" + cornerRow + "," + cornerCol + ")");
                        } else {
                            System.out.println("🚀 Thread " + threadId + " démarré (seed=" + seed + ")");
                        }
                    }

                    // Initialiser le cache si activé
                    if (useDomainCache) {
                        localSolver.initializeDomainCache(localBoard, localPieces, pieceUsed, totalPieces);
                    }

                    // Lancer la recherche
                    localSolver.stats.start();
                    boolean solved = localSolver.solveBacktracking(localBoard, localPieces, pieceUsed, totalPieces);

                    if (solved) {
                        synchronized (System.out) {
                            System.out.println("\n" + "=".repeat(60));
                            System.out.println("🎉 Thread " + threadId + " a trouvé une SOLUTION! 🎉");
                            System.out.println("=".repeat(60));
                        }

                        synchronized (lockObject) {
                            globalBestBoard.set(localBoard);
                            globalBestPieces.set(localPieces);
                        }

                        return true;
                    }

                    synchronized (System.out) {
                        System.out.println("✗ Thread " + threadId + " terminé sans solution");
                    }
                    return false;

                } catch (Exception e) {
                    synchronized (System.err) {
                        System.err.println("✗ Thread " + threadId + " erreur: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return false;
                }
            });

            futures.add(future);
        }

        // Lancer un thread moniteur pour afficher le progrès
        Thread monitor = new Thread(() -> {
            try {
                while (!solutionFound.get() && !Thread.interrupted()) {
                    Thread.sleep(1800000); // Toutes les 30 minutes (1800000 ms)
                    int depth = globalMaxDepth.get();
                    int score = globalBestScore.get();

                    if (depth > 0) {
                        // Calculer le score maximal théorique pour un board 16x16
                        int maxScore = 480; // 480 arêtes internes seulement
                        double percentage = maxScore > 0 ? (score * 100.0 / maxScore) : 0.0;

                        System.out.println("\n╔════════════════════════════════════════════════════════╗");
                        System.out.println("║                  PROGRÈS - 30 minutes                  ║");
                        System.out.println("╚════════════════════════════════════════════════════════╝");
                        System.out.println("📊 Profondeur max:  " + depth + " pièces placées");
                        System.out.println("⭐ Meilleur score:  " + score + "/" + maxScore + " arêtes internes (" + String.format("%.1f%%", percentage) + ")");
                        System.out.println();
                    }
                }
            } catch (InterruptedException e) {
                // Normal lors de l'arrêt
            }
        });
        monitor.setDaemon(true);
        monitor.start();

        // Attendre que tous les threads se terminent ou qu'une solution soit trouvée
        boolean solved = false;
        try {
            for (Future<Boolean> future : futures) {
                try {
                    Boolean result = future.get();
                    if (result) {
                        solved = true;
                        // Annuler les autres threads
                        executor.shutdownNow();
                        monitor.interrupt();
                        break;
                    }
                } catch (Exception e) {
                    synchronized (System.err) {
                        System.err.println("Erreur dans un thread: " + e.getMessage());
                    }
                }
            }

            // Si aucune solution trouvée, attendre que tous terminent proprement
            if (!solved) {
                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.HOURS);
            }

        } catch (InterruptedException e) {
            System.err.println("Interruption: " + e.getMessage());
            executor.shutdownNow();
        }

        Board bestBoard = globalBestBoard.get();
        Map<Integer, Piece> bestPieces = globalBestPieces.get();
        if (solved && bestBoard != null) {
            // Copier la solution trouvée dans le board original
            synchronized (lockObject) {
                for (int r = 0; r < bestBoard.getRows(); r++) {
                    for (int c = 0; c < bestBoard.getCols(); c++) {
                        if (!bestBoard.isEmpty(r, c)) {
                            Placement p = bestBoard.getPlacement(r, c);
                            Piece piece = bestPieces.get(p.getPieceId());
                            board.place(r, c, piece, p.getRotation());
                        }
                    }
                }
            }
        }

        return solved;
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
