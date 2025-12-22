package solver;

import model.Board;
import util.SolverLogger;

import util.TimeConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages statistics and progress tracking for puzzle solving.
 * Extracted from EternitySolver for better code organization.
 *
 * Merged with SolverStateManager (Phase 8 refactoring) to reduce manager count.
 */
public class StatisticsManager {

    public long startTime;
    public long endTime;
    public long previousTimeOffset = 0; // Time already accumulated in previous runs
    public long recursiveCalls = 0;
    public long placements = 0;
    public long backtracks = 0;
    public long singletonsFound = 0;
    public long singletonsPlaced = 0;
    public long deadEndsDetected = 0;
    public long fitChecks = 0;
    public long forwardCheckRejects = 0;

    // Solver state (merged from SolverStateManager)
    private int stepCount = 0;
    private int lastPlacedRow = -1;
    private int lastPlacedCol = -1;

    // Progress tracking (for % estimation)
    private Map<Integer, ProgressTracker> depthTrackers = new HashMap<>();

    /**
     * Inner class to track progress at a given depth
     */
    private static class ProgressTracker {
        int totalOptions;      // Total number of options at this depth
        int currentOption;     // Currently explored option (0-indexed)

        ProgressTracker(int totalOptions) {
            this.totalOptions = totalOptions;
            this.currentOption = 0;
        }
    }

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void start(long previousComputeTimeMs) {
        this.previousTimeOffset = previousComputeTimeMs;
        startTime = System.currentTimeMillis();
    }

    public void end() {
        endTime = System.currentTimeMillis();
    }

    public long getElapsedTimeMs() {
        long end = (endTime > 0) ? endTime : System.currentTimeMillis();
        return previousTimeOffset + (end - startTime);
    }

    public double getElapsedTimeSec() {
        return TimeConstants.toSeconds(getElapsedTimeMs());
    }

    /**
     * Registers the number of options at a given depth
     */
    public void registerDepthOptions(int depth, int numOptions) {
        if (!depthTrackers.containsKey(depth)) {
            depthTrackers.put(depth, new ProgressTracker(numOptions));
        }
    }

    /**
     * Increments the current option at a given depth
     */
    public void incrementDepthProgress(int depth) {
        ProgressTracker tracker = depthTrackers.get(depth);
        if (tracker != null) {
            tracker.currentOption++;
        }
    }

    /**
     * Calculates an estimated progress percentage based on the first depths
     * Uses the first 5 depths for estimation
     */
    public double getProgressPercentage() {
        // Limit tracking to first 5 depths for performance
        final int MAX_DEPTH_TRACKED = 5;

        double progress = 0.0;
        double weight = 1.0;

        for (int d = 0; d < MAX_DEPTH_TRACKED; d++) {
            ProgressTracker tracker = depthTrackers.get(d);
            if (tracker == null || tracker.totalOptions == 0) {
                break; // No more depth explored
            }

            // Percentage at this depth
            double depthProgress = (double) tracker.currentOption / tracker.totalOptions;
            progress += depthProgress * weight;
            weight /= tracker.totalOptions; // Decreasing weight
        }

        return Math.min(100.0, progress * 100.0);
    }

    public void print() {
        SolverLogger.info("\n╔════════════════ STATISTICS ═════════════════════╗");
        System.out.println("║ Elapsed time       : " + String.format("%.2f", getElapsedTimeSec()) + " seconds");
        SolverLogger.info("║ Recursive calls    : " + recursiveCalls);
        SolverLogger.info("║ Placements tested  : " + placements);
        SolverLogger.info("║ Backtracks         : " + backtracks);
        SolverLogger.info("║ Fit checks         : " + fitChecks);
        SolverLogger.info("║ Forward check rejects : " + forwardCheckRejects);
        SolverLogger.info("║ Singletons found   : " + singletonsFound);
        SolverLogger.info("║ Singletons placed  : " + singletonsPlaced);
        SolverLogger.info("║ Dead-ends detected : " + deadEndsDetected);
        SolverLogger.info("╚══════════════════════════════════════════════════╝");
    }

    public void printCompact() {
        System.out.printf("Stats: Recursive=%d | Placements=%d | Backtracks=%d | Singletons=%d/%d | Dead-ends=%d | Time=%.1fs\n",
                recursiveCalls, placements, backtracks, singletonsPlaced, singletonsFound, deadEndsDetected, getElapsedTimeSec());
    }

    // ==================== Solver State Methods (merged from SolverStateManager) ====================

    /**
     * Gets the current step count.
     * @return number of steps taken
     */
    public int getStepCount() {
        return stepCount;
    }

    /**
     * Increments the step count by 1.
     */
    public void incrementStepCount() {
        stepCount++;
    }

    /**
     * Resets the step count to 0.
     */
    public void resetStepCount() {
        stepCount = 0;
    }

    /**
     * Sets the last placed position.
     * @param row row index
     * @param col column index
     */
    public void setLastPlaced(int row, int col) {
        this.lastPlacedRow = row;
        this.lastPlacedCol = col;
    }

    /**
     * Gets the row of the last placed piece.
     * @return row index, or -1 if none placed
     */
    public int getLastPlacedRow() {
        return lastPlacedRow;
    }

    /**
     * Gets the column of the last placed piece.
     * @return column index, or -1 if none placed
     */
    public int getLastPlacedCol() {
        return lastPlacedCol;
    }

    /**
     * Scans the board to find and set the last placed position.
     * Useful after loading from a saved state.
     *
     * @param board Board to scan
     */
    public void findAndSetLastPlaced(Board board) {
        int rows = board.getRows();
        int cols = board.getCols();

        // Scan from bottom-right to top-left to find last placed piece
        for (int r = rows - 1; r >= 0; r--) {
            for (int c = cols - 1; c >= 0; c--) {
                if (!board.isEmpty(r, c)) {
                    setLastPlaced(r, c);
                    return;
                }
            }
        }

        // No pieces found - reset to -1
        setLastPlaced(-1, -1);
    }

    /**
     * Resets solver state (step count and last placed position).
     * Note: Does not reset statistics counters - use this for state-specific resets.
     */
    public void resetState() {
        stepCount = 0;
        lastPlacedRow = -1;
        lastPlacedCol = -1;
    }
}
