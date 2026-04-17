package solver;

import util.CellLabelFormatter;
import util.PositionKey;
import util.SaveStateManager;

import java.util.*;

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

    // Use LinkedHashMap to maintain insertion order while preventing duplicates
    private java.util.LinkedHashMap<PositionKey, SaveStateManager.PlacementInfo> placementMap;
    private final List<SaveStateManager.PlacementInfo> initialFixedPieces;

    /**
     * Constructor without initial fixed pieces
     */
    public PlacementOrderTracker() {
        this.placementMap = new java.util.LinkedHashMap<>();
        this.initialFixedPieces = new ArrayList<>();
    }

    /**
     * Constructor with initial fixed pieces
     * @param initialFixedPieces pieces that were fixed at puzzle startup
     */
    public PlacementOrderTracker(List<SaveStateManager.PlacementInfo> initialFixedPieces) {
        this.placementMap = new java.util.LinkedHashMap<>();
        this.initialFixedPieces = (initialFixedPieces != null)
            ? new ArrayList<>(initialFixedPieces)
            : new ArrayList<>();
    }

    /**
     * Initializes placement history (call at start of solving)
     */
    public void initialize() {
        if (placementMap == null) {
            placementMap = new java.util.LinkedHashMap<>();
        }
    }

    /**
     * Initializes placement history with fixed pieces
     * @param fixedPieces list of fixed pieces to add to placement history
     */
    public void initializeWithFixedPieces(List<SaveStateManager.PlacementInfo> fixedPieces) {
        if (placementMap == null) {
            placementMap = new java.util.LinkedHashMap<>();
        }
        if (fixedPieces != null) {
            for (SaveStateManager.PlacementInfo piece : fixedPieces) {
                PositionKey key = new PositionKey(piece.row, piece.col);
                placementMap.put(key, piece);
            }
        }
    }

    /**
     * Initializes placement history with preloaded order
     * @param preloadedOrder placement order preloaded from save file
     */
    public void initializeWithHistory(List<SaveStateManager.PlacementInfo> preloadedOrder) {
        placementMap = new java.util.LinkedHashMap<>();
        if (preloadedOrder != null) {
            for (SaveStateManager.PlacementInfo piece : preloadedOrder) {
                PositionKey key = new PositionKey(piece.row, piece.col);
                placementMap.put(key, piece);
            }
        }
    }

    /**
     * Records a new placement in history.
     * If a piece already exists at this position, it will be replaced (updated).
     * This prevents duplicates at the same position.
     *
     * @param row row position
     * @param col column position
     * @param pieceId ID of the placed piece
     * @param rotation piece rotation (0-3)
     */
    public void recordPlacement(int row, int col, int pieceId, int rotation) {
        if (placementMap != null) {
            PositionKey key = new PositionKey(row, col);
            placementMap.put(key, new SaveStateManager.PlacementInfo(row, col, pieceId, rotation));
        }
    }

    /**
     * Removes a placement from history for a specific position (for backtracking).
     * This should be called when a piece is removed from the board.
     *
     * @param row row position
     * @param col column position
     * @return information about the removed placement, or null if not found
     */
    public SaveStateManager.PlacementInfo removePlacement(int row, int col) {
        if (placementMap != null) {
            PositionKey key = new PositionKey(row, col);
            SaveStateManager.PlacementInfo removed = placementMap.remove(key);

            // Debug log to trace every removal for LIFO verification
            if (removed != null) {
                String cellLabel = CellLabelFormatter.format(removed.row, removed.col);
                int remainingPieces = placementMap.size();

                util.SolverLogger.info("      🗑 Removed from history: " + cellLabel + " piece " + removed.pieceId +
                                     " (remaining: " + remainingPieces + " pieces)");
            }
            return removed;
        }
        return null;
    }

    /**
     * Legacy method for backward compatibility.
     * Removes the LAST placement from history (LIFO).
     * NOTE: With the new Map-based implementation, this removes by position, not by time.
     * Callers should use removePlacement(row, col) instead.
     *
     * @return information about the removed placement, or null if history is empty
     * @deprecated Use removePlacement(row, col) instead
     */
    @Deprecated
    public SaveStateManager.PlacementInfo removeLastPlacement() {
        // This method is problematic with Map-based implementation
        // Return null to indicate it shouldn't be used
        util.SolverLogger.warn("⚠ removeLastPlacement() called but deprecated - use removePlacement(row, col)");
        return null;
    }

    /**
     * Gets the complete placement history in chronological order.
     * @return list of all placements made (read-only view)
     */
    public List<SaveStateManager.PlacementInfo> getPlacementHistory() {
        if (placementMap == null) {
            return new ArrayList<>();
        }
        // LinkedHashMap maintains insertion order, so values() returns chronological order
        return new ArrayList<>(placementMap.values());
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
        if (placementMap != null) {
            placementMap.clear();
        }
    }

    /**
     * Gets the current depth (number of placements made)
     * @return number of placements in history
     */
    public int getCurrentDepth() {
        return placementMap != null ? placementMap.size() : 0;
    }

    /**
     * Checks if placement history tracking is enabled
     * @return true if tracking is enabled
     */
    public boolean isTracking() {
        return placementMap != null;
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
     * Gets a specific placement from history by index (chronological order).
     * @param index index in history
     * @return placement information at this index, or null if out of bounds
     */
    public SaveStateManager.PlacementInfo getPlacement(int index) {
        if (placementMap != null && index >= 0 && index < placementMap.size()) {
            // Convert map values to list to get by index
            List<SaveStateManager.PlacementInfo> list = new ArrayList<>(placementMap.values());
            return list.get(index);
        }
        return null;
    }

    /**
     * Gets the last placement from history (most recently placed piece).
     * @return information about the last placement, or null if history is empty
     */
    public SaveStateManager.PlacementInfo getLastPlacement() {
        if (placementMap != null && !placementMap.isEmpty()) {
            // Get last entry from LinkedHashMap
            SaveStateManager.PlacementInfo last = null;
            for (SaveStateManager.PlacementInfo info : placementMap.values()) {
                last = info;
            }
            return last;
        }
        return null;
    }
}
