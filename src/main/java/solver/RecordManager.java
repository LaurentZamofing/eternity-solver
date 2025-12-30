package solver;

import util.SolverLogger;
import util.SaveStateManager;
import model.Board;
import model.Piece;
import model.Placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages depth records and score tracking during solving.
 * Extracted from EternitySolver for better code organization.
 */
public class RecordManager {

    private final String puzzleName;
    private final int threadId;
    private final int minDepthToShowRecords;
    private final Object lockObject;

    // Atomic references for parallel solving
    private final AtomicInteger globalMaxDepth;
    private final AtomicInteger globalBestScore;
    private final AtomicInteger globalBestThreadId;
    private final AtomicReference<Board> globalBestBoard;
    private final AtomicReference<Map<Integer, Piece>> globalBestPieces;

    private int maxDepthReached = 0;
    private long lastProgressBacktracks = 0;

    /**
     * Constructor for RecordManager.
     *
     * @param puzzleName puzzle name
     * @param threadId thread ID (-1 for non-parallel)
     * @param minDepthToShowRecords minimum depth to display records
     * @param lockObject lock object for synchronized operations
     * @param globalMaxDepth atomic reference to global maximum depth
     * @param globalBestScore atomic reference to global best score
     * @param globalBestThreadId atomic reference to best thread ID
     * @param globalBestBoard atomic reference to best board
     * @param globalBestPieces atomic reference to best pieces
     */
    public RecordManager(String puzzleName, int threadId, int minDepthToShowRecords,
                        Object lockObject, AtomicInteger globalMaxDepth,
                        AtomicInteger globalBestScore, AtomicInteger globalBestThreadId,
                        AtomicReference<Board> globalBestBoard,
                        AtomicReference<Map<Integer, Piece>> globalBestPieces) {
        this.puzzleName = puzzleName;
        this.threadId = threadId;
        this.minDepthToShowRecords = minDepthToShowRecords;
        this.lockObject = lockObject;
        this.globalMaxDepth = globalMaxDepth;
        this.globalBestScore = globalBestScore;
        this.globalBestThreadId = globalBestThreadId;
        this.globalBestBoard = globalBestBoard;
        this.globalBestPieces = globalBestPieces;
    }

    /**
     * Result of checking for a new record.
     */
    public static class RecordCheckResult {
        public final boolean isNewDepthRecord;
        public final boolean isNewScoreRecord;
        public final int currentScore;
        public final int maxScore;

        public RecordCheckResult(boolean isNewDepthRecord, boolean isNewScoreRecord,
                                int currentScore, int maxScore) {
            this.isNewDepthRecord = isNewDepthRecord;
            this.isNewScoreRecord = isNewScoreRecord;
            this.currentScore = currentScore;
            this.maxScore = maxScore;
        }
    }

    /**
     * Gets the maximum depth reached.
     *
     * @return maximum depth reached
     */
    public int getMaxDepthReached() {
        return maxDepthReached;
    }

    /**
     * Gets the number of backtracks at last progress.
     *
     * @return last progress backtracks
     */
    public long getLastProgressBacktracks() {
        return lastProgressBacktracks;
    }

    /**
     * Checks if a new record has been reached and updates state.
     *
     * @param board current board state
     * @param piecesById map of pieces by ID
     * @param currentDepth current depth (excluding fixed pieces)
     * @param currentBacktracks current number of backtracks
     * @return result indicating if new records were reached
     */
    public RecordCheckResult checkAndUpdateRecord(Board board, Map<Integer, Piece> piecesById,
                                                  int currentDepth, long currentBacktracks) {
        // Check if we've reached a new local depth
        if (currentDepth <= maxDepthReached) {
            return null; // No new record
        }

        // Update local maximum depth
        maxDepthReached = currentDepth;
        lastProgressBacktracks = currentBacktracks;

        // Calculate current score
        int[] scoreData = board.calculateScore();
        int currentScore = scoreData[0];
        int maxScore = scoreData[1];

        // Update global records using CAS (Compare-And-Swap) for lock-free updates
        boolean isNewGlobalDepthRecord = updateGlobalDepthRecord(currentDepth);
        boolean isNewGlobalScoreRecord = updateGlobalScoreRecord(currentScore);

        // If it's a new global record, save the board
        if (isNewGlobalDepthRecord || isNewGlobalScoreRecord) {
            saveGlobalBestBoard(board, piecesById);
        }

        return new RecordCheckResult(isNewGlobalDepthRecord, isNewGlobalScoreRecord,
                                     currentScore, maxScore);
    }

    /**
     * Updates global maximum depth using atomic CAS.
     *
     * @param currentDepth current depth
     * @return true if it's a new global record
     */
    private boolean updateGlobalDepthRecord(int currentDepth) {
        int oldMaxDepth, newMaxDepth;
        do {
            oldMaxDepth = globalMaxDepth.get();
            newMaxDepth = Math.max(oldMaxDepth, currentDepth);
        } while (oldMaxDepth < newMaxDepth && !globalMaxDepth.compareAndSet(oldMaxDepth, newMaxDepth));

        if (newMaxDepth > oldMaxDepth) {
            globalBestThreadId.set(threadId);
            return true;
        }
        return false;
    }

    /**
     * Updates global best score using atomic CAS.
     *
     * @param currentScore current score
     * @return true if it's a new global score record
     */
    private boolean updateGlobalScoreRecord(int currentScore) {
        int oldMaxScore, newMaxScore;
        do {
            oldMaxScore = globalBestScore.get();
            newMaxScore = Math.max(oldMaxScore, currentScore);
        } while (oldMaxScore < newMaxScore && !globalBestScore.compareAndSet(oldMaxScore, newMaxScore));

        return newMaxScore > oldMaxScore;
    }

    /**
     * Saves current board as global best (synchronized).
     *
     * @param board current board
     * @param piecesById map of pieces by ID
     */
    private void saveGlobalBestBoard(Board board, Map<Integer, Piece> piecesById) {
        synchronized (lockObject) {
            // Create a copy of current board
            Board newBestBoard = new Board(board.getRows(), board.getCols());
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (!board.isEmpty(r, c)) {
                        Placement p = board.getPlacement(r, c);
                        Piece piece = piecesById.get(p.getPieceId());
                        if (piece != null) {
                            newBestBoard.place(r, c, piece, p.getRotation());
                        }
                    }
                }
            }
            globalBestBoard.set(newBestBoard);
            globalBestPieces.set(new HashMap<>(piecesById));
        }
    }

    /**
     * Determines if a record should be displayed.
     *
     * @param result record check result
     * @param currentDepth current depth
     * @return true if record should be displayed
     */
    public boolean shouldShowRecord(RecordCheckResult result, int currentDepth) {
        if (result == null) return false;

        return currentDepth >= minDepthToShowRecords &&
               ((result.isNewDepthRecord && currentDepth > 60) || result.isNewScoreRecord);
    }

    /**
     * Displays record information with board visualization.
     *
     * @param result record check result
     * @param usedCount total number of pieces used
     * @param stats statistics manager for progress
     * @param board current board state
     * @param piecesById map of pieces by ID
     * @param unusedIds list of unused piece IDs
     * @param fixedPositions set of fixed position keys ("row,col")
     * @param validator placement validator for edge checking
     * @param lastPlacement last placed piece info (can be null)
     * @param nextCell next empty cell coordinates [row, col] (can be null if board is full)
     * @param placementOrderTracker tracker for placement order (can be null)
     */
    public void displayRecord(RecordCheckResult result, int usedCount, StatisticsManager stats,
                             Board board, Map<Integer, Piece> piecesById, List<Integer> unusedIds,
                             Set<String> fixedPositions, PlacementValidator validator,
                             SaveStateManager.PlacementInfo lastPlacement, int[] nextCell,
                             PlacementOrderTracker placementOrderTracker) {
        // Note: Using synchronized block to ensure atomic multi-line output
        synchronized (SolverLogger.getLogger()) {
            SolverLogger.info("\n" + "=".repeat(80));

            if (result.isNewDepthRecord) {
                SolverLogger.info("EXCEPTIONAL RECORD! {} pieces placed (Thread {})", usedCount, threadId);
                SolverLogger.info("Puzzle: {}", puzzleName);
            }

            if (result.isNewScoreRecord) {
                double percentage = result.maxScore > 0 ? (result.currentScore * 100.0 / result.maxScore) : 0.0;
                SolverLogger.info("BEST SCORE! {}/{} internal edges ({}%)",
                                 result.currentScore, result.maxScore, String.format("%.1f", percentage));
                if (!result.isNewDepthRecord) {
                    SolverLogger.info("Puzzle: {}", puzzleName);
                }
            }

            // Display last placed piece and next target
            if (lastPlacement != null) {
                char rowLabel = (char) ('A' + lastPlacement.row);
                int colLabel = lastPlacement.col + 1;
                Piece piece = piecesById.get(lastPlacement.pieceId);
                String rotationStr = lastPlacement.rotation > 0 ? " (rotated " + (lastPlacement.rotation * 90) + "°)" : "";
                SolverLogger.info("Last placed: Piece {} at {}{}{}",
                    lastPlacement.pieceId, rowLabel, colLabel, rotationStr);
            }

            if (nextCell != null) {
                char rowLabel = (char) ('A' + nextCell[0]);
                int colLabel = nextCell[1] + 1;
                SolverLogger.info("Next target: {}{}", rowLabel, colLabel);
            }

            // Display progress percentage
            double progress = stats.getProgressPercentage();
            if (progress > 0.0 && progress < 99.9) {
                SolverLogger.info("Estimated progress: {}% (based on first 5 depths)",
                                 String.format("%.8f", progress));
            } else if (progress >= 99.9) {
                SolverLogger.info("Progress: exploring beyond tracked depths (>5)");
            } else {
                SolverLogger.info("Progress: calculating... (waiting for data from initial depths)");
            }

            SolverLogger.info("=".repeat(80));

            // Build placement order map
            java.util.Map<String, Integer> placementOrderMap = null;
            if (placementOrderTracker != null) {
                placementOrderMap = new java.util.HashMap<>();
                java.util.List<SaveStateManager.PlacementInfo> allPlacements = placementOrderTracker.getPlacementHistory();
                int step = 1;
                for (SaveStateManager.PlacementInfo info : allPlacements) {
                    String key = info.row + "," + info.col;
                    placementOrderMap.put(key, step++);
                }
            }

            // Display board visualization with pieces and available options
            SolverLogger.info("\nBoard state:\n");
            BoardDisplayManager displayManager = new BoardDisplayManager(fixedPositions, validator);
            displayManager.printBoardWithLabels(board, piecesById, unusedIds, lastPlacement, nextCell, placementOrderMap);

            SolverLogger.info("");
        }
    }
}
