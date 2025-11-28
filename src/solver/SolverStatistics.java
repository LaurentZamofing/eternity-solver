package solver;

import util.SolverLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to store puzzle solving statistics.
 * Tracks performance metrics like recursive calls, placements, backtracks,
 * and provides a progress estimate based on search tree depth.
 */
public class SolverStatistics {
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
        return getElapsedTimeMs() / 1000.0;
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
     * Calculates an estimated progress percentage based on the first depths.
     * Uses the first 5 depths for estimation.
     *
     * IMPORTANT: This percentage represents only the progress in the search tree
     * of the first 5 depths. It does NOT indicate that 100% of the total search is complete,
     * as exploration continues beyond these depths.
     */
    public double getProgressPercentage() {
        // Limit tracking to first 5 depths for performance
        final int MAX_DEPTH_TRACKED = 5;

        // Count how many depths have been completely explored
        int completedDepths = 0;

        for (int d = 0; d < MAX_DEPTH_TRACKED; d++) {
            ProgressTracker tracker = depthTrackers.get(d);
            if (tracker == null || tracker.totalOptions == 0) {
                // This depth has not been explored yet
                break;
            }

            // Check if this depth is complete
            if (tracker.currentOption >= tracker.totalOptions - 1) {
                completedDepths++;
            } else {
                // Found the first incomplete depth
                // The percentage represents progress in THIS depth only
                double depthProgress = (double) tracker.currentOption / tracker.totalOptions;
                return depthProgress * 100.0;
            }
        }

        // If we get here, the first 5 depths are complete
        // But this does NOT mean the search is at 100%!
        // Return an indicator that this phase is complete (but there is more work)
        return (completedDepths >= MAX_DEPTH_TRACKED) ? 100.0 : 0.0;
    }

    public void print() {
        SolverLogger.stats("\n╔════════════════ STATISTICS ═════════════════════╗");
        SolverLogger.stats("║ Elapsed time       : " + String.format("%.2f", getElapsedTimeSec()) + " seconds");
        SolverLogger.stats("║ Recursive calls    : " + recursiveCalls);
        SolverLogger.stats("║ Placements tested  : " + placements);
        SolverLogger.stats("║ Backtracks         : " + backtracks);
        SolverLogger.stats("║ Fit checks         : " + fitChecks);
        SolverLogger.stats("║ Forward check rejects : " + forwardCheckRejects);
        SolverLogger.stats("║ Singletons found   : " + singletonsFound);
        SolverLogger.stats("║ Singletons placed  : " + singletonsPlaced);
        SolverLogger.stats("║ Dead-ends detected : " + deadEndsDetected);
        SolverLogger.stats("╚══════════════════════════════════════════════════╝");
    }

    public void printCompact() {
        SolverLogger.stats("Stats: Recursive={} | Placements={} | Backtracks={} | Singletons={}/{} | Dead-ends={} | Time={}s",
                recursiveCalls, placements, backtracks, singletonsPlaced, singletonsFound, deadEndsDetected,
                String.format("%.1f", getElapsedTimeSec()));
    }
}
