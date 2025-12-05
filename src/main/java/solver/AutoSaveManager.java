package solver;

import model.Board;
import model.Piece;
import util.SaveStateManager;
import util.StatsLogger;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Manages periodic automatic saving of solver state and statistics logging.
 * Extracted from EternitySolver for better code organization.
 *
 * Features:
 * - Periodic full state saves (every 60 seconds)
 * - High-frequency stats logging (every 10 seconds)
 * - Depth-based redundancy elimination
 */
public class AutoSaveManager {

    private static final long AUTO_SAVE_INTERVAL = SolverConstants.THREAD_SAVE_INTERVAL_MS;
    private static final long STATS_LOG_INTERVAL = SolverConstants.STATS_LOG_INTERVAL_MS;

    private final String puzzleName;
    private final int numFixedPieces;
    private final List<SaveStateManager.PlacementInfo> initialFixedPieces;
    private final PlacementOrderTracker placementOrderTracker;

    private Map<Integer, Piece> allPiecesMap = null;
    private long lastAutoSaveTime = 0;
    private int lastSavedDepth = -1; // Track last saved depth to avoid redundant saves

    // Stats logging
    private StatsLogger statsLogger;
    private long lastStatsLogTime = 0;
    private boolean statsLoggingEnabled = true;

    /**
     * Constructor for AutoSaveManager.
     *
     * @param puzzleName name of puzzle being solved
     * @param numFixedPieces number of fixed pieces at start
     * @param initialFixedPieces list of initial fixed pieces
     * @param placementOrderTracker tracker for placement order (reference, not copy)
     */
    public AutoSaveManager(String puzzleName, int numFixedPieces,
                          List<SaveStateManager.PlacementInfo> initialFixedPieces,
                          PlacementOrderTracker placementOrderTracker) {
        this.puzzleName = puzzleName;
        this.numFixedPieces = numFixedPieces;
        this.initialFixedPieces = initialFixedPieces;
        this.placementOrderTracker = placementOrderTracker;
        this.lastAutoSaveTime = System.currentTimeMillis();
        this.lastStatsLogTime = System.currentTimeMillis();

        // Initialize stats logger
        if (statsLoggingEnabled) {
            this.statsLogger = new StatsLogger(puzzleName);
        }
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
     * Only saves if depth has changed since last save to avoid redundant saves.
     *
     * @param board current board state
     * @param pieceUsed bitset of used pieces
     * @param totalPieces total number of pieces
     * @param stats statistics manager for progress tracking
     */
    public void checkAndSave(Board board, BitSet pieceUsed, int totalPieces, StatisticsManager stats) {
        long currentTime = System.currentTimeMillis();
        int currentDepth = totalPieces - pieceUsed.cardinality() + numFixedPieces;

        if (allPiecesMap != null && (currentTime - lastAutoSaveTime > AUTO_SAVE_INTERVAL)) {
            // Only save if depth has changed to avoid redundant saves during backtracking
            if (currentDepth != lastSavedDepth) {
                lastAutoSaveTime = currentTime;
                lastSavedDepth = currentDepth;
                performSave(board, pieceUsed, totalPieces, stats);
            } else {
                // Reset timer even if we don't save, to check again in next interval
                lastAutoSaveTime = currentTime;
            }
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
            lastSavedDepth = currentDepth; // Update last saved depth
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

        // Get current placement order from tracker (fresh copy with latest placements)
        List<SaveStateManager.PlacementInfo> currentPlacementOrder =
            (placementOrderTracker != null) ? placementOrderTracker.getPlacementHistory() : new ArrayList<>();

        SaveStateManager.saveState(puzzleName, board, allPiecesMap, unusedIds,
            currentPlacementOrder, progress, elapsedTime, numFixedPieces, initialFixedPieces);
    }

    /**
     * Checks if stats should be logged and logs them if necessary.
     * Called frequently during search (every iteration or every N iterations).
     *
     * @param pieceUsed bitset of used pieces
     * @param totalPieces total number of pieces
     * @param stats statistics manager with internal metrics
     */
    public void checkAndLogStats(BitSet pieceUsed, int totalPieces, StatisticsManager stats) {
        if (!statsLoggingEnabled || statsLogger == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStatsLogTime > STATS_LOG_INTERVAL) {
            // Calculate current depth
            int currentDepth = totalPieces - pieceUsed.cardinality() + numFixedPieces;

            // Get progress and compute time from stats
            double progress = stats.getProgressPercentage();
            long elapsedTime = stats.getElapsedTimeMs();

            // Log stats
            statsLogger.appendStats(currentDepth, progress, elapsedTime, stats);

            // Update last log time
            lastStatsLogTime = currentTime;
        }
    }

    /**
     * Closes the stats logger.
     * Should be called when solver terminates.
     */
    public void close() {
        if (statsLogger != null) {
            statsLogger.close();
        }
    }
}
