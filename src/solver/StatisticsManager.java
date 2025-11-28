package solver;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages statistics and progress tracking for puzzle solving.
 * Extracted from EternitySolver for better code organization.
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
        System.out.println("\n╔════════════════ STATISTICS ═════════════════════╗");
        System.out.println("║ Elapsed time       : " + String.format("%.2f", getElapsedTimeSec()) + " seconds");
        System.out.println("║ Recursive calls    : " + recursiveCalls);
        System.out.println("║ Placements tested  : " + placements);
        System.out.println("║ Backtracks         : " + backtracks);
        System.out.println("║ Fit checks         : " + fitChecks);
        System.out.println("║ Forward check rejects : " + forwardCheckRejects);
        System.out.println("║ Singletons found   : " + singletonsFound);
        System.out.println("║ Singletons placed  : " + singletonsPlaced);
        System.out.println("║ Dead-ends detected : " + deadEndsDetected);
        System.out.println("╚══════════════════════════════════════════════════╝");
    }

    public void printCompact() {
        System.out.printf("Stats: Recursive=%d | Placements=%d | Backtracks=%d | Singletons=%d/%d | Dead-ends=%d | Time=%.1fs\n",
                recursiveCalls, placements, backtracks, singletonsPlaced, singletonsFound, deadEndsDetected, getElapsedTimeSec());
    }
}
