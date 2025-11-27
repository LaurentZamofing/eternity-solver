package solver;

import model.Board;
import model.Piece;
import util.SaveStateManager;

import java.util.*;

/**
 * Gère le retour arrière historique à travers les états pré-chargés, permettant
 * la reprise depuis des parties sauvegardées avec capacité complète de backtracking.
 *
 * Cette classe gère la logique complexe du retour arrière à travers les placements
 * de pièces qui ont été chargés depuis un état sauvegardé, incluant l'essai de
 * rotations alternatives avant un retour arrière plus profond.
 *
 * Extrait de EternitySolver pour isoler la complexité de reprise/backtracking.
 */
public class BacktrackingHistoryManager {

    private final PlacementValidator validator;
    private final String threadLabel;
    private final EternitySolver.Statistics stats;

    /**
     * Constructeur avec dépendances
     * @param validator validateur de placement pour vérifier l'ajustement
     * @param threadLabel étiquette pour la journalisation
     * @param stats gestionnaire de statistiques
     */
    public BacktrackingHistoryManager(PlacementValidator validator,
                                     String threadLabel,
                                     EternitySolver.Statistics stats) {
        this.validator = validator;
        this.threadLabel = threadLabel;
        this.stats = stats;
    }

    /**
     * Calcule le nombre de pièces fixes pour un puzzle
     * Les pièces fixes sont celles qui ne doivent pas subir de retour arrière (coins, indices)
     *
     * @param puzzleName nom du puzzle
     * @return nombre de pièces fixes
     */
    public int calculateFixedPieces(String puzzleName) {
        if (puzzleName.startsWith("eternity2")) {
            return 9; // 4 coins + 5 indices pour Eternity II
        } else if (puzzleName.startsWith("indice")) {
            return 0; // Pas de pièces fixes pour les puzzles d'indices
        } else {
            return 0; // Par défaut : pas de pièces fixes
        }
    }

    /**
     * Construit la liste des pièces fixes initiales depuis l'historique de placement
     *
     * @param preloadedOrder historique complet de placement
     * @param numFixedPieces nombre de pièces fixes
     * @return liste des placements fixes
     */
    public List<SaveStateManager.PlacementInfo> buildInitialFixedPieces(
            List<SaveStateManager.PlacementInfo> preloadedOrder,
            int numFixedPieces) {
        List<SaveStateManager.PlacementInfo> fixedPieces = new ArrayList<>();
        for (int i = 0; i < Math.min(numFixedPieces, preloadedOrder.size()); i++) {
            fixedPieces.add(preloadedOrder.get(i));
        }
        return fixedPieces;
    }

    /**
     * Effectue un retour arrière à travers les placements de pièces pré-chargés,
     * en essayant des rotations alternatives avant un retour arrière plus profond.
     *
     * @param board état actuel du plateau
     * @param allPieces toutes les pièces du puzzle
     * @param pieceUsed bitset des pièces utilisées
     * @param placementOrder historique des placements
     * @param sequentialSolver callback vers le solveur séquentiel
     * @return true si une solution est trouvée pendant le retour arrière
     */
    public boolean backtrackThroughHistory(Board board,
                                          Map<Integer, Piece> allPieces,
                                          BitSet pieceUsed,
                                          List<SaveStateManager.PlacementInfo> placementOrder,
                                          SequentialSolver sequentialSolver) {
        int backtrackCount = 0;
        boolean result = false;

        while (!result && !placementOrder.isEmpty()) {
            // Retirer la dernière pièce de l'historique
            SaveStateManager.PlacementInfo lastPlacement =
                placementOrder.remove(placementOrder.size() - 1);

            int row = lastPlacement.row;
            int col = lastPlacement.col;
            int pieceId = lastPlacement.pieceId;
            int oldRotation = lastPlacement.rotation;

            // Retirer la pièce du plateau
            board.remove(row, col);

            // Remettre la pièce dans les pièces disponibles
            pieceUsed.clear(pieceId);

            stats.backtracks++;
            backtrackCount++;

            System.out.println(threadLabel + "  → Backtrack pré-chargé #" + backtrackCount +
                             ": retrait pièce " + pieceId + " (rot=" + oldRotation + ") à [" +
                             row + "," + col + "]");
            System.out.println(threadLabel + "  → Profondeur réduite à: " +
                             placementOrder.size() + " pièces");

            // Essayer des rotations alternatives avant de reculer davantage
            result = tryAlternativeRotations(board, allPieces, pieceUsed, placementOrder,
                                           row, col, pieceId, oldRotation, sequentialSolver);

            if (result) {
                return true;
            }

            // Aucune rotation alternative n'a fonctionné, essayer de résoudre avec moins de pièces
            int totalPieces = allPieces.size();
            result = sequentialSolver.solve(board, allPieces, pieceUsed, totalPieces);
        }

        return result;
    }

    /**
     * Essaie des rotations alternatives de la même pièce à la même position
     * avant de continuer le retour arrière.
     *
     * @param board état actuel du plateau
     * @param allPieces toutes les pièces du puzzle
     * @param pieceUsed bitset des pièces utilisées
     * @param placementOrder historique de placement
     * @param row position de ligne
     * @param col position de colonne
     * @param pieceId ID de la pièce à essayer
     * @param oldRotation rotation qui a échoué
     * @param sequentialSolver callback vers le solveur séquentiel
     * @return true si une solution est trouvée avec une rotation alternative
     */
    public boolean tryAlternativeRotations(Board board,
                                          Map<Integer, Piece> allPieces,
                                          BitSet pieceUsed,
                                          List<SaveStateManager.PlacementInfo> placementOrder,
                                          int row, int col, int pieceId, int oldRotation,
                                          SequentialSolver sequentialSolver) {
        Piece piece = allPieces.get(pieceId);
        if (piece == null) {
            return false;
        }

        int maxRotations = piece.getUniqueRotationCount();

        // Essayer les rotations après celle qui a échoué
        for (int rot = (oldRotation + 1) % 4; rot < maxRotations; rot++) {
            int[] candidate = piece.edgesRotated(rot);

            if (validator.fits(board, row, col, candidate)) {
                System.out.println(threadLabel + "  → Tentative rotation alternative " + rot +
                                 " pour pièce " + pieceId + " à [" + row + "," + col + "]");

                // Placer avec la nouvelle rotation
                board.place(row, col, piece, rot);
                pieceUsed.set(pieceId);
                placementOrder.add(new SaveStateManager.PlacementInfo(row, col, pieceId, rot));

                // Essayer de résoudre avec cette rotation
                int totalPieces = allPieces.size();
                boolean result = sequentialSolver.solve(board, allPieces, pieceUsed, totalPieces);

                if (result) {
                    return true;
                }

                // Cette rotation n'a pas fonctionné, la retirer
                board.remove(row, col);
                pieceUsed.clear(pieceId);
                placementOrder.remove(placementOrder.size() - 1);
                stats.backtracks++;
            }
        }

        return false;
    }

    /**
     * Interface pour le callback du solveur séquentiel
     */
    public interface SequentialSolver {
        boolean solve(Board board, Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces);
    }

    /**
     * Effectue le retour arrière d'une seule pièce pré-chargée et essaie de continuer
     *
     * @param board état actuel du plateau
     * @param allPieces toutes les pièces
     * @param pieceUsed bitset des pièces utilisées
     * @param placementOrder historique de placement
     * @return les informations de placement retirées
     */
    public SaveStateManager.PlacementInfo backtrackPreloadedPiece(
            Board board,
            Map<Integer, Piece> allPieces,
            BitSet pieceUsed,
            List<SaveStateManager.PlacementInfo> placementOrder) {

        if (placementOrder.isEmpty()) {
            return null;
        }

        // Retirer la dernière pièce de l'historique
        SaveStateManager.PlacementInfo lastPlacement =
            placementOrder.remove(placementOrder.size() - 1);

        int row = lastPlacement.row;
        int col = lastPlacement.col;
        int pieceId = lastPlacement.pieceId;

        // Retirer la pièce du plateau
        board.remove(row, col);

        // Remettre la pièce dans les pièces disponibles
        pieceUsed.clear(pieceId);

        stats.backtracks++;

        return lastPlacement;
    }

    /**
     * Obtient les positions fixes qui ne doivent pas subir de retour arrière
     * Retourne actuellement un ensemble vide (pas de positions vraiment fixes)
     *
     * @return ensemble de clés de positions fixes (format: "row,col")
     */
    public Set<String> getFixedPositions() {
        return new HashSet<>(); // Pas de positions fixes pour l'instant
    }

    /**
     * Vérifie si une position est fixe et ne doit pas subir de retour arrière
     *
     * @param row position de ligne
     * @param col position de colonne
     * @param fixedPositions ensemble des positions fixes
     * @return true si la position est fixe
     */
    public boolean isFixedPosition(int row, int col, Set<String> fixedPositions) {
        return fixedPositions.contains(row + "," + col);
    }

    /**
     * Journalise la progression du retour arrière
     *
     * @param backtrackCount nombre de retours arrière effectués
     * @param currentDepth profondeur actuelle après le retour arrière
     */
    public void logBacktrackProgress(int backtrackCount, int currentDepth) {
        System.out.println(threadLabel + "  → Total backtracks dans historique: " + backtrackCount);
        System.out.println(threadLabel + "  → Profondeur actuelle: " + currentDepth + " pièces");
    }
}
