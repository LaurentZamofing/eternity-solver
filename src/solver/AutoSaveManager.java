package solver;

import model.Board;
import model.Piece;
import util.SaveStateManager;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Gère la sauvegarde automatique périodique de l'état du solveur.
 * Extrait de EternitySolver pour une meilleure organisation du code.
 */
public class AutoSaveManager {

    private static final long AUTO_SAVE_INTERVAL = 60000; // 1 minute en millisecondes

    private final String puzzleName;
    private final int numFixedPieces;
    private final List<SaveStateManager.PlacementInfo> initialFixedPieces;
    private final List<SaveStateManager.PlacementInfo> placementOrder;

    private Map<Integer, Piece> allPiecesMap = null;
    private long lastAutoSaveTime = 0;

    /**
     * Constructeur de AutoSaveManager.
     *
     * @param puzzleName nom du puzzle en cours de résolution
     * @param numFixedPieces nombre de pièces fixes au départ
     * @param initialFixedPieces liste des pièces fixes initiales
     * @param placementOrder liste suivant l'ordre de placement
     */
    public AutoSaveManager(String puzzleName, int numFixedPieces,
                          List<SaveStateManager.PlacementInfo> initialFixedPieces,
                          List<SaveStateManager.PlacementInfo> placementOrder) {
        this.puzzleName = puzzleName;
        this.numFixedPieces = numFixedPieces;
        this.initialFixedPieces = initialFixedPieces;
        this.placementOrder = placementOrder;
        this.lastAutoSaveTime = System.currentTimeMillis();
    }

    /**
     * Initialise la carte des pièces pour la fonctionnalité de sauvegarde automatique.
     *
     * @param allPieces carte de toutes les pièces
     */
    public void initializePiecesMap(Map<Integer, Piece> allPieces) {
        this.allPiecesMap = allPieces;
    }

    /**
     * Récupère la carte des pièces (nécessaire pour les opérations externes).
     *
     * @return carte de toutes les pièces
     */
    public Map<Integer, Piece> getAllPiecesMap() {
        return allPiecesMap;
    }

    /**
     * Vérifie si la sauvegarde automatique doit être effectuée et l'exécute si nécessaire.
     *
     * @param board état actuel du plateau
     * @param pieceUsed bitset des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @param stats gestionnaire de statistiques pour le suivi de la progression
     */
    public void checkAndSave(Board board, BitSet pieceUsed, int totalPieces, StatisticsManager stats) {
        long currentTime = System.currentTimeMillis();

        if (allPiecesMap != null && (currentTime - lastAutoSaveTime > AUTO_SAVE_INTERVAL)) {
            lastAutoSaveTime = currentTime;
            performSave(board, pieceUsed, totalPieces, stats);
        }
    }

    /**
     * Effectue une sauvegarde immédiate (utilisée pour les records).
     *
     * @param board état actuel du plateau
     * @param pieceUsed bitset des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @param stats gestionnaire de statistiques pour le suivi de la progression
     * @param currentDepth profondeur actuelle pour la vérification du seuil
     */
    public void saveRecord(Board board, BitSet pieceUsed, int totalPieces,
                          StatisticsManager stats, int currentDepth) {
        // Sauvegarde dès 10 pièces ET à chaque nouveau record de profondeur
        if (currentDepth >= 10 && allPiecesMap != null) {
            performSave(board, pieceUsed, totalPieces, stats);
        }
    }

    /**
     * Effectue l'opération de sauvegarde réelle.
     *
     * @param board état actuel du plateau
     * @param pieceUsed bitset des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @param stats gestionnaire de statistiques pour le suivi de la progression
     */
    private void performSave(Board board, BitSet pieceUsed, int totalPieces, StatisticsManager stats) {
        double progress = stats.getProgressPercentage();
        long elapsedTime = stats.getElapsedTimeMs();

        // Construire la liste des pièces non utilisées pour la sauvegarde
        List<Integer> unusedIds = new ArrayList<>();
        for (int i = 1; i <= totalPieces; i++) {
            if (!pieceUsed.get(i)) {
                unusedIds.add(i);
            }
        }

        SaveStateManager.saveState(puzzleName, board, allPiecesMap, unusedIds,
            placementOrder, progress, elapsedTime, numFixedPieces, initialFixedPieces);
    }
}
