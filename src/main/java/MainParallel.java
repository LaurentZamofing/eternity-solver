import util.SolverLogger;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.ConfigurationUtils;
import util.SaveStateManager;
import util.TimeConstants;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Parallel launcher for Eternity II
 * Launches multiple threads on different configurations intelligently:
 * 1. Priority to configurations never started
 * 2. Then resume oldest saves
 */
public class MainParallel {

    private static final String DATA_DIR = "data/";

    /**
     * Information about an available configuration
     */
    private static class ConfigInfo implements Comparable<ConfigInfo> {
        final String filepath;
        final PuzzleConfig config;
        final File currentSave;
        final long totalComputeTimeMs;
        final boolean hasBeenStarted;

        ConfigInfo(String filepath, PuzzleConfig config, File currentSave, long totalComputeTimeMs) {
            this.filepath = filepath;
            this.config = config;
            this.currentSave = currentSave;
            this.totalComputeTimeMs = totalComputeTimeMs;
            this.hasBeenStarted = (currentSave != null);
        }

        @Override
        public int compareTo(ConfigInfo other) {
            // 1. Priority to never-started configs
            if (!this.hasBeenStarted && other.hasBeenStarted) return -1;
            if (this.hasBeenStarted && !other.hasBeenStarted) return 1;

            // 2. Among started ones, sort by cumulative time (less time = priority)
            if (this.hasBeenStarted && other.hasBeenStarted) {
                return Long.compare(this.totalComputeTimeMs, other.totalComputeTimeMs);
            }

            // 3. Among non-started, alphabetical order
            return this.filepath.compareTo(other.filepath);
        }
    }

    /**
     * Finds all available Eternity II configurations
     */
    private static List<ConfigInfo> findAllConfigurations() throws IOException {
        List<ConfigInfo> configs = new ArrayList<>();

        File dataDir = new File(DATA_DIR + "eternity2/");
        File[] configFiles = dataDir.listFiles((dir, name) ->
            name.startsWith("eternity2_p") && name.endsWith(".txt")
        );

        if (configFiles == null || configFiles.length == 0) {
            SolverLogger.info("✗ No configuration found in " + DATA_DIR);
            return configs;
        }

        SolverLogger.info("📁 Analyzing " + configFiles.length + " available configurations...");
        SolverLogger.info("");

        for (File file : configFiles) {
            try {
                // Load the config
                PuzzleConfig config = PuzzleConfig.loadFromFile(file.getAbsolutePath());
                if (config == null) continue;

                // Extract configId from file name
                String configId = ConfigurationUtils.extractConfigId(file.getAbsolutePath());

                // Chercher une sauvegarde current pour cette config
                File currentSave = SaveStateManager.findCurrentSave(configId);

                // Read total cumulative compute time
                long totalComputeTimeMs = 0;
                if (currentSave != null) {
                    totalComputeTimeMs = SaveStateManager.readTotalComputeTime(configId);
                }

                configs.add(new ConfigInfo(file.getAbsolutePath(), config, currentSave, totalComputeTimeMs));

            } catch (IOException | RuntimeException e) {
                SolverLogger.warn("Error loading configuration " + file.getName() + ": " + e.getMessage());
            }
        }

        // Sort by priority
        Collections.sort(configs);

        return configs;
    }

    // Removed: ConfigurationUtils.extractConfigId() - now using ConfigurationUtils.ConfigurationUtils.extractConfigId()
    // Removed: ConfigurationUtils.createThreadLabel() - now using ConfigurationUtils.ConfigurationUtils.createThreadLabel()

    /**
     * Displays configuration statistics
     */
    private static void displayConfigStats(List<ConfigInfo> configs) {
        int notStarted = 0;
        int inProgress = 0;

        for (ConfigInfo info : configs) {
            if (!info.hasBeenStarted) {
                notStarted++;
            } else {
                inProgress++;
            }
        }

        SolverLogger.info("╔═══════════════════════════════════════════════════════════════════╗");
        SolverLogger.info("║              CONFIGURATION STATISTICS                     ║");
        SolverLogger.info("╚═══════════════════════════════════════════════════════════════════╝");
        SolverLogger.info("");
        SolverLogger.info("  📊 Total configurations : " + configs.size());
        SolverLogger.info("  🆕 Never started        : " + notStarted);
        SolverLogger.info("  🔄 In progress          : " + inProgress);
        SolverLogger.info("");
    }

    /**
     * Launches resolution of a configuration in a thread with timeout
     */
    private static class SolverTask implements Callable<Boolean> {
        private final ConfigInfo configInfo;
        private final int threadId;
        private final long timeoutMs;

        SolverTask(ConfigInfo configInfo, int threadId, long timeoutMs) {
            this.configInfo = configInfo;
            this.threadId = threadId;
            this.timeoutMs = timeoutMs;
        }

        @Override
        public Boolean call() {
            try {
                System.out.println("🚀 [Thread " + threadId + "] Starting: " + configInfo.config.getName());
                System.out.println("   File: " + new File(configInfo.filepath).getName());
                if (configInfo.hasBeenStarted) {
                    long totalSeconds = configInfo.totalComputeTimeMs / TimeConstants.MILLIS_PER_SECOND;
                    long hours = totalSeconds / TimeConstants.SECONDS_PER_HOUR;
                    long minutes = (totalSeconds % TimeConstants.SECONDS_PER_HOUR) / TimeConstants.SECONDS_PER_MINUTE;
                    long seconds = totalSeconds % 60;
                    System.out.println("   Status: RESUME (cumulative time: " +
                        String.format("%dh %02dm %02ds", hours, minutes, seconds) + ")");
                } else {
                    System.out.println("   Status: NEW");
                }
                System.out.println();

                // Load the puzzle
                PuzzleConfig config = configInfo.config;

                // Create a unique ID based on file name (e.g.: eternity2_p01_ascending)
                String configId = ConfigurationUtils.extractConfigId(configInfo.filepath);

                // Look for a current save for this specific config
                File currentSave = SaveStateManager.findCurrentSave(configId);

                if (currentSave != null && currentSave.exists()) {
                    // Resume from save
                    SaveStateManager.SaveState saveState = SaveStateManager.loadStateFromFile(currentSave, config.getType());

                    if (saveState != null) {
                        Board board = new Board(config.getRows(), config.getCols());
                        Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

                        boolean restored = SaveStateManager.restoreState(saveState, board, allPieces);
                        if (restored) {
                            List<Integer> unusedIds = new ArrayList<>(saveState.unusedPieceIds);

                            // Sort according to configured order
                            if ("descending".equalsIgnoreCase(config.getSortOrder())) {
                                Collections.sort(unusedIds, Collections.reverseOrder());
                            } else {
                                Collections.sort(unusedIds);
                            }

                            // Create and configure the solver
                            EternitySolver.resetGlobalState();
                            EternitySolver solver = new EternitySolver();
                            solver.setDisplayConfig(config.isVerbose(), config.getMinDepthToShowRecords());

                            // Use the already extracted configId
                            solver.setPuzzleName(configId);
                            solver.setSortOrder(config.getSortOrder());
                            solver.setPrioritizeBorders(config.isPrioritizeBorders());
                            solver.setNumFixedPieces(config.getFixedPieces().size());
                            solver.setThreadLabel(ConfigurationUtils.createThreadLabel(threadId, configId));
                            solver.setMaxExecutionTime(timeoutMs); // Configure timeout for rotation

                            System.out.println("   [Thread " + threadId + "] Resume: " + saveState.depth + " pieces placed");

                            // Solve
                            boolean solved = solver.solveWithHistory(board, allPieces, unusedIds,
                                                                     new ArrayList<>(saveState.placementOrder));

                            if (solved) {
                                System.out.println("✅ [Thread " + threadId + "] SOLUTION FOUND!");
                            }

                            return solved;
                        }
                    }
                }

                // Starting from scratch
                System.out.println("   [Thread " + threadId + "] Starting from scratch");

                Board board = new Board(config.getRows(), config.getCols());
                Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

                // Place fixed pieces
                for (PuzzleConfig.FixedPiece fp : config.getFixedPieces()) {
                    Piece piece = allPieces.get(fp.pieceId);
                    if (piece != null) {
                        board.place(fp.row, fp.col, piece, fp.rotation);
                        allPieces.remove(fp.pieceId);  // Remove from local copy, not from original config
                    }
                }

                // Solve
                EternitySolver.resetGlobalState();
                EternitySolver solver = new EternitySolver();
                solver.setDisplayConfig(config.isVerbose(), config.getMinDepthToShowRecords());

                // Use the already extracted configId
                solver.setPuzzleName(configId);
                solver.setSortOrder(config.getSortOrder());
                solver.setPrioritizeBorders(config.isPrioritizeBorders());
                solver.setThreadLabel(ConfigurationUtils.createThreadLabel(threadId, configId));
                solver.setMaxExecutionTime(timeoutMs); // Configure timeout

                System.out.println("   [Thread " + threadId + "] Pieces to place: " + allPieces.size() + " pieces");
                System.out.println("   [Thread " + threadId + "] Fixed pieces on board: " + config.getFixedPieces().size());
                System.out.println("   [Thread " + threadId + "] Configured timeout: " + (timeoutMs / TimeConstants.MILLIS_PER_SECOND) + " seconds");
                System.out.println("   [Thread " + threadId + "] Starting solver...");

                boolean solved = solver.solve(board, allPieces);

                System.out.println("   [Thread " + threadId + "] Solver finished. Result: " + (solved ? "SOLUTION FOUND" : "No solution"));

                if (solved) {
                    System.out.println("✅ [Thread " + threadId + "] SOLUTION FOUND!");
                }

                return solved;

            } catch (RuntimeException e) {
                SolverLogger.error("[Thread " + threadId + "] Error during solving: " + e.getMessage(), e);
                return false;
            }
        }
    }

    // Lock to prevent multiple threads from taking the same config
    private static final Object configSelectionLock = new Object();

    // Tracker for configs currently running
    private static final Set<String> runningConfigs = Collections.synchronizedSet(new HashSet<>());

    /**
     * Worker thread that loops with automatic rotation
     */
    private static void runWorkerWithRotation(int threadId, long timeoutMs,
                                               ExecutorService executor,
                                               Set<String> solvedConfigs) throws Exception {
        while (true) {
            ConfigInfo nextConfig = null;
            String configId = null;

            // Atomic selection of next available config
            synchronized (configSelectionLock) {
                // Reload configuration list to get updated priorities
                List<ConfigInfo> configs = findAllConfigurations();

                // Filter already solved or currently running configs
                for (ConfigInfo config : configs) {
                    String cid = ConfigurationUtils.extractConfigId(config.filepath);
                    if (!solvedConfigs.contains(cid) && !runningConfigs.contains(cid)) {
                        nextConfig = config;
                        configId = cid;
                        runningConfigs.add(configId); // Reserve this config
                        break;
                    }
                }
            }

            if (nextConfig == null) {
                System.out.println("🎉 [Thread " + threadId + "] All configurations are solved or in progress!");
                break;
            }

            try {
                // Display rotation
                if (nextConfig.hasBeenStarted) {
                    long totalSeconds = nextConfig.totalComputeTimeMs / TimeConstants.MILLIS_PER_SECOND;
                    long hours = totalSeconds / TimeConstants.SECONDS_PER_HOUR;
                    long minutes = (totalSeconds % TimeConstants.SECONDS_PER_HOUR) / TimeConstants.SECONDS_PER_MINUTE;
                    System.out.println("🔄 [Thread " + threadId + "] Rotating to: " + configId +
                        " (cumulative time: " + String.format("%dh%02dm", hours, minutes) + ")");
                } else {
                    System.out.println("🔄 [Thread " + threadId + "] Rotating to: " + configId + " (NEW)");
                }

                // Launch resolution directly (not via executor to avoid deadlock)
                SolverTask task = new SolverTask(nextConfig, threadId, timeoutMs);

                try {
                    // Execute directly in current thread
                    Boolean solved = task.call();

                    if (solved != null && solved) {
                        System.out.println("✅ [Thread " + threadId + "] SOLUTION FOUND for " + configId);
                        solvedConfigs.add(configId);
                    } else {
                        System.out.println("⏱️  [Thread " + threadId + "] Timeout reached for " + configId + " - rotation");
                    }

                } catch (Exception e) {
                    SolverLogger.error("[Thread " + threadId + "] Error during execution: " + e.getMessage(), e);
                }

            } finally {
                // Release config for other threads
                runningConfigs.remove(configId);
            }

            // Small pause before next iteration
            Thread.sleep(TimeConstants.DEFAULT_THREAD_SLEEP_MS);
        }
    }

    public static void main(String[] args) {
        SolverLogger.info("\n");
        SolverLogger.info("╔═══════════════════════════════════════════════════════════════════╗");
        SolverLogger.info("║          ETERNITY II - PARALLEL SOLVER                       ║");
        SolverLogger.info("╚═══════════════════════════════════════════════════════════════════╝");
        SolverLogger.info("");

        // Number of threads (default: number of available processors)
        int numThreads = Runtime.getRuntime().availableProcessors();

        // Duration per configuration in minutes (default: 1 minute for quick rotation)
        double timePerConfigMinutes = 1.0;

        if (args.length > 0) {
            try {
                numThreads = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                SolverLogger.warn("⚠️  Invalid argument 1, using " + numThreads + " threads");
            }
        }

        if (args.length > 1) {
            try {
                timePerConfigMinutes = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                SolverLogger.warn("⚠️  Invalid argument 2, using " + timePerConfigMinutes + " minutes per config");
            }
        }

        SolverLogger.info("⚙️  Number of threads: " + numThreads);
        SolverLogger.info("⏱️  Duration per configuration: " + timePerConfigMinutes + " minute(s)");
        SolverLogger.info("");

        try {
            // Find all configurations
            List<ConfigInfo> configs = findAllConfigurations();

            if (configs.isEmpty()) {
                System.out.println("✗ No configuration available");
                return;
            }

            // Display statistics
            displayConfigStats(configs);

            // Create thread pool
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
            System.out.println("║              LAUNCHING THREADS WITH ROTATION                 ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("📋 Rotation strategy:");
            System.out.println("   1. Each thread works " + timePerConfigMinutes + " min on a configuration");
            System.out.println("   2. After timeout, thread moves to least advanced config");
            System.out.println("   3. Continuous rotation to advance all configs");
            System.out.println();
            System.out.println("📋 Priority order:");
            System.out.println("   1. Never-started configurations");
            System.out.println("   2. Saves with least cumulative time");
            System.out.println();

            // Tracker for finished configs (solution found)
            Set<String> solvedConfigs = Collections.synchronizedSet(new HashSet<>());

            // Launch threads with rotation
            long timeoutMs = (long)(timePerConfigMinutes * TimeConstants.SECONDS_PER_MINUTE * TimeConstants.MILLIS_PER_SECOND);

            System.out.println("✓ Starting " + numThreads + " thread(s) with automatic rotation");
            System.out.println();
            System.out.println("═".repeat(70));
            System.out.println();

            // Launch initial threads
            for (int threadId = 1; threadId <= numThreads; threadId++) {
                final int tid = threadId;
                executor.submit(() -> {
                    try {
                        runWorkerWithRotation(tid, timeoutMs, executor, solvedConfigs);
                    } catch (Exception e) {
                        System.err.println("✗ [Thread " + tid + "] Fatal error: " + e.getMessage());
                        SolverLogger.error("Error occurred", e);
                    }
                });
            }

            System.out.println("⏳ Threads working with automatic rotation... (Ctrl+C to stop)");
            System.out.println();

            // Wait indefinitely (threads rotate continuously)
            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            System.err.println("✗ Fatal error: " + e.getMessage());
            SolverLogger.error("Error occurred", e);
        }
    }
}
