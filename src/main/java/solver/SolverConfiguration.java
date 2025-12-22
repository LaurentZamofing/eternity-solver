package solver;

import util.SaveStateManager;
import util.SolverLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable configuration for the Eternity solver.
 * Contains all solver flags, puzzle metadata, and fixed piece information.
 *
 * Use the Builder pattern to create instances:
 * <pre>
 * SolverConfiguration config = SolverConfiguration.builder()
 *     .puzzleName("eternity2")
 *     .verbose(true)
 *     .useSingletons(true)
 *     .build();
 * </pre>
 */
public class SolverConfiguration {

    // Main configuration flags
    private final boolean useSingletons;
    private final boolean verbose;
    private final boolean useAC3;
    private final boolean useDomainCache;
    private final boolean prioritizeBorders;

    // Display and logging configuration
    private final int minDepthToShowRecords;

    // Timeout configuration
    private final long maxExecutionTimeMs;

    // Puzzle metadata
    private final String puzzleName;
    private final String threadLabel;
    private final String sortOrder;
    private final int threadId;

    // Fixed pieces state
    private final int numFixedPieces;
    private final Set<String> fixedPositions;
    private final List<SaveStateManager.PlacementInfo> initialFixedPieces;

    // Thread management and saving
    private final long randomSeed;
    private static final long THREAD_SAVE_INTERVAL = SolverConstants.THREAD_SAVE_INTERVAL_MS;

    private SolverConfiguration(Builder builder) {
        this.useSingletons = builder.useSingletons;
        this.verbose = builder.verbose;
        this.useAC3 = builder.useAC3;
        this.useDomainCache = builder.useDomainCache;
        this.prioritizeBorders = builder.prioritizeBorders;
        this.minDepthToShowRecords = builder.minDepthToShowRecords;
        this.maxExecutionTimeMs = builder.maxExecutionTimeMs;
        this.puzzleName = builder.puzzleName;
        this.threadLabel = builder.threadLabel;
        this.sortOrder = builder.sortOrder;
        this.threadId = builder.threadId;
        this.numFixedPieces = builder.numFixedPieces;
        this.fixedPositions = new HashSet<>(builder.fixedPositions);
        this.initialFixedPieces = new ArrayList<>(builder.initialFixedPieces);
        this.randomSeed = builder.randomSeed;
    }

    // ============ Getters ============

    public boolean isUseSingletons() {
        return useSingletons;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isUseAC3() {
        return useAC3;
    }

    public boolean isUseDomainCache() {
        return useDomainCache;
    }

    public boolean isPrioritizeBorders() {
        return prioritizeBorders;
    }

    public int getMinDepthToShowRecords() {
        return minDepthToShowRecords;
    }

    public long getMaxExecutionTimeMs() {
        return maxExecutionTimeMs;
    }

    public String getPuzzleName() {
        return puzzleName;
    }

    public String getThreadLabel() {
        return threadLabel;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public int getThreadId() {
        return threadId;
    }

    public int getNumFixedPieces() {
        return numFixedPieces;
    }

    public Set<String> getFixedPositions() {
        return new HashSet<>(fixedPositions);
    }

    public List<SaveStateManager.PlacementInfo> getInitialFixedPieces() {
        return new ArrayList<>(initialFixedPieces);
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public static long getThreadSaveInterval() {
        return THREAD_SAVE_INTERVAL;
    }

    /** Logs current configuration parameters if verbose enabled. */
    public void logConfiguration() {
        if (verbose) {
            SolverLogger.info("\n═══════════════════════════════════════");
            SolverLogger.info("  Configuration:");
            SolverLogger.info("  - Puzzle: " + puzzleName);
            SolverLogger.info("  - Thread: " + threadLabel);
            System.out.println("  - Singletons: " + (useSingletons ? "✓" : "✗"));
            System.out.println("  - AC-3: " + (useAC3 ? "✓" : "✗"));
            System.out.println("  - Domain Cache: " + (useDomainCache ? "✓" : "✗"));
            System.out.println("  - Prioritize Borders: " + (prioritizeBorders ? "✓" : "✗"));
            SolverLogger.info("  - Fixed Pieces: " + numFixedPieces);
            SolverLogger.info("═══════════════════════════════════════\n");
        }
    }

    // ============ Builder ============

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized with values from this configuration.
     * Useful for creating modified copies.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private boolean useSingletons = true;
        private boolean verbose = true;
        private boolean useAC3 = true;
        private boolean useDomainCache = true;
        private boolean prioritizeBorders = false;
        private int minDepthToShowRecords = 0;
        private long maxExecutionTimeMs = Long.MAX_VALUE;
        private String puzzleName = "eternity2";
        private String threadLabel = "";
        private String sortOrder = "ascending";
        private int threadId = -1;
        private int numFixedPieces = 0;
        private Set<String> fixedPositions = new HashSet<>();
        private List<SaveStateManager.PlacementInfo> initialFixedPieces = new ArrayList<>();
        private long randomSeed = 0;

        public Builder() {
        }

        private Builder(SolverConfiguration config) {
            this.useSingletons = config.useSingletons;
            this.verbose = config.verbose;
            this.useAC3 = config.useAC3;
            this.useDomainCache = config.useDomainCache;
            this.prioritizeBorders = config.prioritizeBorders;
            this.minDepthToShowRecords = config.minDepthToShowRecords;
            this.maxExecutionTimeMs = config.maxExecutionTimeMs;
            this.puzzleName = config.puzzleName;
            this.threadLabel = config.threadLabel;
            this.sortOrder = config.sortOrder;
            this.threadId = config.threadId;
            this.numFixedPieces = config.numFixedPieces;
            this.fixedPositions = new HashSet<>(config.fixedPositions);
            this.initialFixedPieces = new ArrayList<>(config.initialFixedPieces);
            this.randomSeed = config.randomSeed;
        }

        public Builder useSingletons(boolean useSingletons) {
            this.useSingletons = useSingletons;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder useAC3(boolean useAC3) {
            this.useAC3 = useAC3;
            return this;
        }

        public Builder useDomainCache(boolean useDomainCache) {
            this.useDomainCache = useDomainCache;
            return this;
        }

        public Builder prioritizeBorders(boolean prioritizeBorders) {
            this.prioritizeBorders = prioritizeBorders;
            if (verbose && prioritizeBorders) {
                SolverLogger.info("  🔲 Border prioritization enabled - borders will be filled first");
            }
            return this;
        }

        public Builder minDepthToShowRecords(int minDepth) {
            this.minDepthToShowRecords = minDepth;
            return this;
        }

        public Builder maxExecutionTimeMs(long timeMs) {
            this.maxExecutionTimeMs = timeMs;
            return this;
        }

        public Builder puzzleName(String name) {
            this.puzzleName = name;
            return this;
        }

        public Builder threadLabel(String label) {
            this.threadLabel = label;
            return this;
        }

        public Builder sortOrder(String order) {
            this.sortOrder = order;
            return this;
        }

        public Builder threadId(int id) {
            this.threadId = id;
            return this;
        }

        public Builder numFixedPieces(int num) {
            this.numFixedPieces = num;
            return this;
        }

        public Builder fixedPositions(Set<String> positions) {
            this.fixedPositions = new HashSet<>(positions);
            return this;
        }

        public Builder initialFixedPieces(List<SaveStateManager.PlacementInfo> pieces) {
            this.initialFixedPieces = new ArrayList<>(pieces);
            return this;
        }

        public Builder randomSeed(long seed) {
            this.randomSeed = seed;
            return this;
        }

        public SolverConfiguration build() {
            return new SolverConfiguration(this);
        }
    }
}
