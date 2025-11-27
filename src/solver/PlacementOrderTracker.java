package solver;

import util.SaveStateManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Suit l'ordre des placements de pièces pour permettre un retour arrière intelligent
 * et une restauration d'état.
 *
 * Cette classe maintient un historique de tous les placements effectués pendant la résolution,
 * permettant au solveur de revenir en arrière dans la séquence de placement et
 * d'essayer des options alternatives.
 *
 * Extrait de EternitySolver pour améliorer la gestion de l'état de retour arrière.
 */
public class PlacementOrderTracker {

    private List<SaveStateManager.PlacementInfo> placementHistory;
    private final List<SaveStateManager.PlacementInfo> initialFixedPieces;

    /**
     * Constructeur sans pièces fixes initiales
     */
    public PlacementOrderTracker() {
        this.placementHistory = new ArrayList<>();
        this.initialFixedPieces = new ArrayList<>();
    }

    /**
     * Constructeur avec pièces fixes initiales
     * @param initialFixedPieces pièces qui étaient fixées au démarrage du puzzle
     */
    public PlacementOrderTracker(List<SaveStateManager.PlacementInfo> initialFixedPieces) {
        this.placementHistory = new ArrayList<>();
        this.initialFixedPieces = new ArrayList<>(initialFixedPieces);
    }

    /**
     * Initialise l'historique de placement (appeler au début de la résolution)
     */
    public void initialize() {
        if (placementHistory == null) {
            placementHistory = new ArrayList<>();
        }
    }

    /**
     * Initialise l'historique de placement avec un ordre préchargé
     * @param preloadedOrder ordre de placement préchargé depuis un fichier de sauvegarde
     */
    public void initializeWithHistory(List<SaveStateManager.PlacementInfo> preloadedOrder) {
        if (preloadedOrder != null) {
            placementHistory = new ArrayList<>(preloadedOrder);
        } else {
            placementHistory = new ArrayList<>();
        }
    }

    /**
     * Enregistre un nouveau placement dans l'historique
     * @param row position de ligne
     * @param col position de colonne
     * @param pieceId ID de la pièce placée
     * @param rotation rotation de la pièce (0-3)
     */
    public void recordPlacement(int row, int col, int pieceId, int rotation) {
        if (placementHistory != null) {
            placementHistory.add(new SaveStateManager.PlacementInfo(row, col, pieceId, rotation));
        }
    }

    /**
     * Retire le dernier placement de l'historique (pour retour arrière)
     * @return les informations du placement retiré, ou null si l'historique est vide
     */
    public SaveStateManager.PlacementInfo removeLastPlacement() {
        if (placementHistory != null && !placementHistory.isEmpty()) {
            return placementHistory.remove(placementHistory.size() - 1);
        }
        return null;
    }

    /**
     * Obtient l'historique complet des placements
     * @return liste de tous les placements effectués (vue en lecture seule)
     */
    public List<SaveStateManager.PlacementInfo> getPlacementHistory() {
        if (placementHistory == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(placementHistory);
    }

    /**
     * Obtient les pièces fixes initiales
     * @return liste des pièces qui étaient fixées au démarrage du puzzle
     */
    public List<SaveStateManager.PlacementInfo> getInitialFixedPieces() {
        return new ArrayList<>(initialFixedPieces);
    }

    /**
     * Efface entièrement l'historique de placement
     */
    public void clearHistory() {
        if (placementHistory != null) {
            placementHistory.clear();
        }
    }

    /**
     * Obtient la profondeur actuelle (nombre de placements effectués)
     * @return nombre de placements dans l'historique
     */
    public int getCurrentDepth() {
        return placementHistory != null ? placementHistory.size() : 0;
    }

    /**
     * Vérifie si l'historique de placement est en suivi
     * @return true si le suivi est activé
     */
    public boolean isTracking() {
        return placementHistory != null;
    }

    /**
     * Définit les pièces fixes initiales (pour les puzzles avec pièces pré-placées)
     * @param fixedPieces liste des placements fixes
     */
    public void setInitialFixedPieces(List<SaveStateManager.PlacementInfo> fixedPieces) {
        if (fixedPieces != null) {
            initialFixedPieces.clear();
            initialFixedPieces.addAll(fixedPieces);
        }
    }

    /**
     * Obtient un placement spécifique de l'historique par index
     * @param index index dans l'historique
     * @return informations du placement à cet index, ou null si hors limites
     */
    public SaveStateManager.PlacementInfo getPlacement(int index) {
        if (placementHistory != null && index >= 0 && index < placementHistory.size()) {
            return placementHistory.get(index);
        }
        return null;
    }
}
