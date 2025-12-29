package solver;

import util.SaveStateManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks piece placement order to enable intelligent backtracking
 * and state restoration.
 *
 * This class maintains a history of all placements made during solving,
 * allowing the solver to backtrack through the placement sequence and
 * try alternative options.
 *
 * Extracted from EternitySolver to improve backtracking state management.
 */
public class PlacementOrderTracker {

    private List<SaveStateManager.PlacementInfo> placementHistory;
    private final List<SaveStateManager.PlacementInfo> initialFixedPieces;

    /**
     * Constructor without initial fixed pieces
     */
    public PlacementOrderTracker() {
        this.placementHistory = new ArrayList<>();
        this.initialFixedPieces = new ArrayList<>();
    }

    /**
     * Constructor with initial fixed pieces
     * @param initialFixedPieces pieces that were fixed at puzzle startup
     */
    public PlacementOrderTracker(List<SaveStateManager.PlacementInfo> initialFixedPieces) {
        this.placementHistory = new ArrayList<>();
        this.initialFixedPieces = (initialFixedPieces != null)
            ? new ArrayList<>(initialFixedPieces)
            : new ArrayList<>();
    }

    /**
     * Initializes placement history (call at start of solving)
     */
    public void initialize() {
        if (placementHistory == null) {
            placementHistory = new ArrayList<>();
        }
    }

    /**
     * Initializes placement history with fixed pieces
     * @param fixedPieces list of fixed pieces to add to placement history
     */
    public void initializeWithFixedPieces(List<SaveStateManager.PlacementInfo> fixedPieces) {
        if (placementHistory == null) {
            placementHistory = new ArrayList<>();
        }
        if (fixedPieces != null) {
            placementHistory.addAll(fixedPieces);
        }
    }

    /**
     * Initializes placement history with preloaded order
     * @param preloadedOrder placement order preloaded from save file
     */
    public void initializeWithHistory(List<SaveStateManager.PlacementInfo> preloadedOrder) {
        if (preloadedOrder != null) {
            placementHistory = new ArrayList<>(preloadedOrder);
        } else {
            placementHistory = new ArrayList<>();
        }
    }

    /**
     * Records a new placement in history
     * @param row row position
     * @param col column position
     * @param pieceId ID of the placed piece
     * @param rotation piece rotation (0-3)
     */
    public void recordPlacement(int row, int col, int pieceId, int rotation) {
        if (placementHistory != null) {
            placementHistory.add(new SaveStateManager.PlacementInfo(row, col, pieceId, rotation));
        }
    }

    /**
     * Removes the last placement from history (for backtracking)
     * @return information about the removed placement, or null if history is empty
     */
    public SaveStateManager.PlacementInfo removeLastPlacement() {
        if (placementHistory != null && !placementHistory.isEmpty()) {
            return placementHistory.remove(placementHistory.size() - 1);
        }
        return null;
    }

    /**
     * Gets the complete placement history
     * @return list of all placements made (read-only view)
     */
    public List<SaveStateManager.PlacementInfo> getPlacementHistory() {
        if (placementHistory == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(placementHistory);
    }

    /**
     * Gets the initial fixed pieces
     * @return list of pieces that were fixed at puzzle startup
     */
    public List<SaveStateManager.PlacementInfo> getInitialFixedPieces() {
        return new ArrayList<>(initialFixedPieces);
    }

    /**
     * Clears the entire placement history
     */
    public void clearHistory() {
        if (placementHistory != null) {
            placementHistory.clear();
        }
    }

    /**
     * Gets the current depth (number of placements made)
     * @return number of placements in history
     */
    public int getCurrentDepth() {
        return placementHistory != null ? placementHistory.size() : 0;
    }

    /**
     * Checks if placement history tracking is enabled
     * @return true if tracking is enabled
     */
    public boolean isTracking() {
        return placementHistory != null;
    }

    /**
     * Sets the initial fixed pieces (for puzzles with pre-placed pieces)
     * @param fixedPieces list of fixed placements
     */
    public void setInitialFixedPieces(List<SaveStateManager.PlacementInfo> fixedPieces) {
        if (fixedPieces != null) {
            initialFixedPieces.clear();
            initialFixedPieces.addAll(fixedPieces);
        }
    }

    /**
     * Gets a specific placement from history by index
     * @param index index in history
     * @return placement information at this index, or null if out of bounds
     */
    public SaveStateManager.PlacementInfo getPlacement(int index) {
        if (placementHistory != null && index >= 0 && index < placementHistory.size()) {
            return placementHistory.get(index);
        }
        return null;
    }

    /**
     * Gets the last placement from history (most recently placed piece)
     * @return information about the last placement, or null if history is empty
     */
    public SaveStateManager.PlacementInfo getLastPlacement() {
        if (placementHistory != null && !placementHistory.isEmpty()) {
            return placementHistory.get(placementHistory.size() - 1);
        }
        return null;
    }
}
