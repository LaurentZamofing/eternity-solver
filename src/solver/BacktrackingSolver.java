package solver;

import model.Board;
import model.Piece;
import util.SaveManager;

import java.util.BitSet;
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
    private final ConfigurationManager configManager;

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
     * @param configManager configuration manager (single source of truth)
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
            ConfigurationManager configManager,
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
        if (configManager == null) {
            throw new IllegalArgumentException("configManager cannot be null");
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
        this.configManager = configManager;
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
     * Résout le puzzle en utilisant le backtracking récursif.
     *
     * Cette méthode implémente l'algorithme de backtracking avec:
     * - Suivi des records de profondeur
     * - Sauvegardes périodiques (thread state + auto-save)
     * - Vérification de timeout
     * - Stratégies de placement (singleton en premier, puis MRV)
     * - Signalisation de solution aux autres threads
     *
     * @param board grille modifiée en place
     * @param piecesById map des pièces originales par ID
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @return true si une solution a été trouvée
     */
    public boolean solve(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        stats.recursiveCalls++;

        // Vérifier si un autre thread a trouvé la solution
        if (solutionFound.get()) {
            return false; // Arrêter cette branche
        }

        // Vérifier si on a atteint un nouveau record de profondeur
        // IMPORTANT: exclure les pièces fixes du calcul (on compte seulement les pièces posées par le backtracking)
        // Optimization: Use BitSet.cardinality() instead of manual loop (O(1) vs O(n))
        int usedCount = pieceUsed.cardinality();
        int currentDepth = usedCount - configManager.getNumFixedPieces();

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
            try {
                SaveManager.saveThreadState(board, piecesById, currentDepth, threadId, randomSeed);
            } catch (Exception e) {
                // Log error but don't crash the solver - saving is optional
                if (configManager.isVerbose()) {
                    System.err.println("⚠️  Erreur lors de la sauvegarde thread " + threadId + ": " + e.getMessage());
                }
            }
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
        int[] cell = solver.findNextCellMRV(board, piecesById, pieceUsed, totalPieces);
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
        if (singletonStrategy.tryPlacement(context, solver)) {
            return true;
        }

        // ÉTAPE 2 : Try MRV placement strategy
        return mrvStrategy.tryPlacement(context, solver);
    }
}
