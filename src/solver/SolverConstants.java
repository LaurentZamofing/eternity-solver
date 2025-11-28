package solver;

/**
 * Centralized constants for Eternity solver parameters.
 *
 * This class groups all configuration values and thresholds
 * used by the different solver components, enabling easy
 * modification and better documentation of parameters.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public final class SolverConstants {

    // Prevents instantiation
    private SolverConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    // ═══════════════════════════════════════════════════════════════
    // ALGORITHMIC PARAMETERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Probability of randomizing the order of MRV candidates.
     * Value between 0.0 (no randomization) and 1.0 (always randomize).
     *
     * Default value: 0.3 (30% chance of shuffling)
     * Allows exploring different search paths.
     */
    public static final double DEFAULT_RANDOMIZATION_PROBABILITY = 0.3;

    /**
     * Stagnation threshold before partial restart.
     * Number of backtracks without progress before considering
     * that we are stuck and trying a different strategy.
     *
     * Default value: 50,000 backtracks
     * Adjust according to puzzle size and desired patience.
     */
    public static final int DEFAULT_STAGNATION_THRESHOLD = 50_000;

    /**
     * Work depth for work-stealing in parallel solver.
     * Number of levels to descend before a thread can "steal"
     * work from another thread.
     *
     * Default value: 5 levels
     * See: ParallelSearchManager.WORK_STEALING_DEPTH_THRESHOLD
     */
    public static final int WORK_STEALING_DEPTH_THRESHOLD = 5;

    // ═══════════════════════════════════════════════════════════════
    // TIMEOUTS AND INTERVALS (in milliseconds)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Automatic save interval for threads.
     * Frequency at which a thread saves its state.
     *
     * Value: 60,000 ms = 1 minute
     */
    public static final long THREAD_SAVE_INTERVAL_MS = 60_000;

    /**
     * Default timeout for puzzle execution.
     * Maximum execution time before automatic stop.
     *
     * Value: 600,000 ms = 10 minutes
     * See: MainSequential.PUZZLE_TIMEOUT
     */
    public static final long DEFAULT_PUZZLE_TIMEOUT_MS = 600_000;

    /**
     * Maximum execution limit (no timeout).
     * Used when no timeout is specified.
     *
     * Value: Long.MAX_VALUE (essentially infinite)
     */
    public static final long UNLIMITED_EXECUTION_TIME_MS = Long.MAX_VALUE;

    /**
     * Timeout check interval.
     * Frequency at which the solver checks if it should stop.
     *
     * Value: 100 ms
     */
    public static final long TIMEOUT_CHECK_INTERVAL_MS = 100;

    // ═══════════════════════════════════════════════════════════════
    // SAVE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Maximum number of backup saves to keep.
     * Older ones are automatically deleted.
     *
     * Value: 10 saves
     * See: SaveStateManager.MAX_BACKUP_SAVES
     */
    public static final int MAX_BACKUP_SAVES = 10;

    /**
     * Level interval for saves.
     * Save every N pieces placed.
     *
     * Value: 1 (save at each new piece)
     * See: SaveStateManager.SAVE_LEVEL_INTERVAL
     */
    public static final int SAVE_LEVEL_INTERVAL = 1;

    /**
     * Root directory for saves.
     *
     * Value: "saves/"
     */
    public static final String SAVE_DIRECTORY = "saves/";

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS AND DISPLAY
    // ═══════════════════════════════════════════════════════════════

    /**
     * Statistics display interval (in backtracks).
     * Display stats every N backtracks.
     *
     * Value: 10,000 backtracks
     */
    public static final int STATS_DISPLAY_INTERVAL = 10_000;

    /**
     * Record update interval (in backtracks).
     * Check for new records every N backtracks.
     *
     * Value: 1,000 backtracks
     */
    public static final int RECORD_CHECK_INTERVAL = 1_000;

    /**
     * Number of decimal digits for progress percentage precision.
     *
     * Value: 8 decimals
     */
    public static final int PROGRESS_PRECISION_DECIMALS = 8;

    // ═══════════════════════════════════════════════════════════════
    // OPTIMIZATIONS AND HEURISTICS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Minimum number of variables to enable AC-3.
     * Below this threshold, AC-3 can be too expensive.
     *
     * Value: 10 variables
     */
    public static final int MIN_VARIABLES_FOR_AC3 = 10;

    /**
     * Reduction factor for MRV candidate sorting.
     * Limits the number of candidates considered to speed up.
     *
     * Value: 0.5 (considers 50% of best candidates)
     */
    public static final double MRV_CANDIDATE_REDUCTION_FACTOR = 0.5;

    /**
     * Domain threshold to consider a cell as constrained.
     * Cells with fewer than N candidates are prioritized.
     *
     * Value: 5 candidates
     */
    public static final int DOMAIN_CONSTRAINT_THRESHOLD = 5;

    // ═══════════════════════════════════════════════════════════════
    // FILE FORMAT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Magic number for binary save files.
     * Identifies the file format: "ETER" in hexadecimal.
     *
     * Value: 0x45544552
     */
    public static final int BINARY_SAVE_MAGIC_NUMBER = 0x45544552;

    /**
     * Binary save format version.
     * Incremented during incompatible changes.
     *
     * Value: 1
     */
    public static final int BINARY_SAVE_FORMAT_VERSION = 1;

    // ═══════════════════════════════════════════════════════════════
    // PARALLELIZATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Default number of threads for parallel solver.
     * Used if not specified by user.
     *
     * Value: -1 (auto-detection based on CPU count)
     */
    public static final int DEFAULT_THREAD_COUNT = -1;

    /**
     * Maximum number of threads allowed.
     * Upper limit to avoid over-allocation.
     *
     * Value: 64 threads
     */
    public static final int MAX_THREAD_COUNT = 64;

    /**
     * Wait time for thread shutdown (in milliseconds).
     * Wait time before force-killing threads.
     *
     * Value: 5,000 ms = 5 seconds
     */
    public static final long THREAD_SHUTDOWN_TIMEOUT_MS = 5_000;
}
