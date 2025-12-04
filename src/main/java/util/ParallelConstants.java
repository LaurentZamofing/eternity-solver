package util;

/**
 * Constants for parallel processing and multi-threading configuration.
 * Centralizes magic numbers related to parallel solver execution.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public final class ParallelConstants {

    // Prevent instantiation
    private ParallelConstants() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    // ========== Thread Pool Configuration ==========

    /** Minimum number of threads for parallel solving */
    public static final int MIN_THREADS = 4;

    /** Maximum number of threads (limit to prevent over-subscription) */
    public static final int MAX_THREADS = 32;

    /** Core utilization factor (use 75% of available cores by default) */
    public static final double CORE_UTILIZATION_FACTOR = 0.75;

    /** Default thread pool keep-alive time in seconds */
    public static final long THREAD_KEEP_ALIVE_SECONDS = 60L;

    // ========== Work Stealing and Load Balancing ==========

    /** Depth threshold for work stealing (from SolverConstants) */
    public static final int WORK_STEALING_DEPTH = 2;

    /** Minimum work size before splitting */
    public static final int MIN_WORK_SIZE = 10;

    /** Load balancing check interval in milliseconds */
    public static final long LOAD_BALANCE_INTERVAL_MS = 1000L;

    // ========== Configuration Rotation ==========

    /** Number of different search configurations to try */
    public static final int NUM_SEARCH_CONFIGURATIONS = 4;

    /** Timeout per configuration in minutes */
    public static final int TIMEOUT_PER_CONFIG_MINUTES = 10;

    /** Sleep time between configuration switches in milliseconds */
    public static final long CONFIG_SWITCH_SLEEP_MS = 1000L;

    // ========== Diversification Strategies ==========

    /** Corner positions for diversification (4 corners) */
    public static final int NUM_CORNER_POSITIONS = 4;

    /** Border priority depth threshold */
    public static final int BORDER_PRIORITY_DEPTH = 10;

    // ========== Synchronization ==========

    /** Queue capacity for parallel tasks */
    public static final int TASK_QUEUE_CAPACITY = 1000;

    /** Maximum pending tasks per thread */
    public static final int MAX_PENDING_TASKS_PER_THREAD = 10;

    /** Spin wait iterations before parking thread */
    public static final int SPIN_WAIT_ITERATIONS = 100;

    // ========== Helper Methods ==========

    /**
     * Calculates the optimal number of threads based on available processors.
     *
     * @return Recommended number of threads (MIN_THREADS to MAX_THREADS)
     */
    public static int getOptimalThreadCount() {
        int availableCores = Runtime.getRuntime().availableProcessors();
        int recommendedThreads = Math.max(
            MIN_THREADS,
            (int) (availableCores * CORE_UTILIZATION_FACTOR)
        );
        return Math.min(recommendedThreads, MAX_THREADS);
    }

    /**
     * Calculates thread count with a specific utilization factor.
     *
     * @param utilizationFactor Factor between 0.0 and 1.0
     * @return Recommended number of threads
     */
    public static int getThreadCount(double utilizationFactor) {
        if (utilizationFactor <= 0.0 || utilizationFactor > 1.0) {
            throw new IllegalArgumentException(
                "Utilization factor must be between 0.0 and 1.0, got: " + utilizationFactor
            );
        }

        int availableCores = Runtime.getRuntime().availableProcessors();
        int threads = Math.max(MIN_THREADS, (int) (availableCores * utilizationFactor));
        return Math.min(threads, MAX_THREADS);
    }

    /**
     * Determines if work stealing should be enabled at the given depth.
     *
     * @param currentDepth Current search depth
     * @return true if work stealing is recommended
     */
    public static boolean shouldEnableWorkStealing(int currentDepth) {
        return currentDepth >= WORK_STEALING_DEPTH;
    }

    /**
     * Determines if border prioritization should be enabled at the given depth.
     *
     * @param currentDepth Current search depth
     * @return true if border prioritization is recommended
     */
    public static boolean shouldPrioritizeBorder(int currentDepth) {
        return currentDepth < BORDER_PRIORITY_DEPTH;
    }
}
