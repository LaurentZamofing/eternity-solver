package solver;

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
        long previousComputeTime = SaveStateManager.readTotalComputeTime(solver.configManager.getPuzzleName());
        solver.stats.start(previousComputeTime);

        // Initialiser PlacementOrderTracker avec l'historique fourni
        solver.placementOrderTracker = new PlacementOrderTracker();
        solver.placementOrderTracker.initializeWithHistory(preloadedOrder);

        // Détecter les positions fixes (celles qu'on ne doit JAMAIS backtracker)
        // Pour l'instant, aucune position n'est vraiment "fixe" - on peut tout backtracker

        // Initialiser numFixedPieces et initialFixedPieces depuis le fichier de configuration
        int numFixed = solver.configManager.calculateNumFixedPieces(solver.configManager.getPuzzleName());
        solver.configManager.buildInitialFixedPieces(preloadedOrder, numFixed);

        // Créer le tableau pieceUsed depuis unusedIds
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

        System.out.println("  → Reprise avec " + preloadedOrder.size() + " pièces pré-chargées");
        System.out.println("  → Le backtracking pourra remonter à travers TOUTES les pièces");

        // Essayer de résoudre avec l'état actuel
        boolean result = solver.solveBacktracking(board, allPieces, pieceUsed, totalPieces);

        // Si échec, utiliser BacktrackingHistoryManager pour backtracker à travers l'historique
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
