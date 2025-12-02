package solver;

import model.Board;
import model.Piece;
import util.SaveStateManager;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Manages periodic automatic saving of solver state.
 * Extracted from EternitySolver for better code organization.
 */
public class AutoSaveManager {

    private static final long AUTO_SAVE_INTERVAL = 60000; // 1 minute in milliseconds

    private final String puzzleName;
    private final int numFixedPieces;
    private final List<SaveStateManager.PlacementInfo> initialFixedPieces;
    private final List<SaveStateManager.PlacementInfo> placementOrder;

    private Map<Integer, Piece> allPiecesMap = null;
    private long lastAutoSaveTime = 0;

    /**
     * Constructor for AutoSaveManager.
     *
     * @param puzzleName name of puzzle being solved
     * @param numFixedPieces number of fixed pieces at start
     * @param initialFixedPieces list of initial fixed pieces
     * @param placementOrder list following placement order
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
     * Initializes the pieces map for automatic save functionality.
     *
     * @param allPieces map of all pieces
     */
    public void initializePiecesMap(Map<Integer, Piece> allPieces) {
        this.allPiecesMap = allPieces;
    }

    /**
     * Retrieves the pieces map (required for external operations).
     *
     * @return map of all pieces
     */
    public Map<Integer, Piece> getAllPiecesMap() {
        return allPiecesMap;
    }

    /**
     * Checks if automatic save should be performed and executes it if necessary.
     *
     * @param board current board state
     * @param pieceUsed bitset of used pieces
     * @param totalPieces total number of pieces
     * @param stats statistics manager for progress tracking
     */
    public void checkAndSave(Board board, BitSet pieceUsed, int totalPieces, StatisticsManager stats) {
        long currentTime = System.currentTimeMillis();

        if (allPiecesMap != null && (currentTime - lastAutoSaveTime > AUTO_SAVE_INTERVAL)) {
            lastAutoSaveTime = currentTime;
            performSave(board, pieceUsed, totalPieces, stats);
        }
    }

    /**
     * Performs an immediate save (used for records).
     *
     * @param board current board state
     * @param pieceUsed bitset of used pieces
     * @param totalPieces total number of pieces
     * @param stats statistics manager for progress tracking
     * @param currentDepth current depth for threshold verification
     */
    public void saveRecord(Board board, BitSet pieceUsed, int totalPieces,
                          StatisticsManager stats, int currentDepth) {
        // Save from 10 pieces AND at each new depth record
        if (currentDepth >= 10 && allPiecesMap != null) {
            System.out.println("  📝 AutoSaveManager: Triggering immediate save for depth " + currentDepth + " (new record detected)");
            performSave(board, pieceUsed, totalPieces, stats);
        } else if (currentDepth < 10) {
            System.out.println("  ⏭️  AutoSaveManager: Skipping save for depth " + currentDepth + " (< 10)");
        } else if (allPiecesMap == null) {
            System.err.println("  ⚠️  AutoSaveManager: Cannot save - piecesMap not initialized!");
        }
    }

    /**
     * Performs the actual save operation.
     *
     * @param board current board state
     * @param pieceUsed bitset of used pieces
     * @param totalPieces total number of pieces
     * @param stats statistics manager for progress tracking
     */
    private void performSave(Board board, BitSet pieceUsed, int totalPieces, StatisticsManager stats) {
        double progress = stats.getProgressPercentage();
        long elapsedTime = stats.getElapsedTimeMs();

        // Build list of unused pieces for save
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
