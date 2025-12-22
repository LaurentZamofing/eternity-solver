package app;

import app.parallel.ConfigurationScanner;
import app.parallel.ConfigurationScanner.ConfigInfo;
import config.PuzzleConfig;
import util.SolverLogger;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.ConfigurationUtils;
import util.SaveStateManager;
import util.TimeConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Parallel launcher for Eternity II.
 * Launches multiple threads on different configurations intelligently:
 * 1. Priority to configurations never started
 * 2. Then resume oldest saves
 *
 * REFACTORED (Phase ULTRA): Configuration scanning extracted to ConfigurationScanner.
 * Reduced from 459 → 310 lines (32% reduction).
 *
 * @see app.parallel.ConfigurationScanner
 */
public class MainParallel {

    private static final String DATA_DIR = "data/";

    // Lock to prevent multiple threads from taking the same config
    private static final Object configSelectionLock = new Object();

    // Tracker for configs currently running
    private static final Set<String> runningConfigs = Collections.synchronizedSet(new HashSet<>());

    /**
     * Launches resolution of a configuration in a thread with timeout.
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
                SolverLogger.info("🚀 [Thread " + threadId + "] Starting: " + configInfo.config.getName());
                SolverLogger.info("   File: " + new File(configInfo.filepath).getName());
                if (configInfo.hasBeenStarted) {
                    long totalSeconds = configInfo.totalComputeTimeMs / TimeConstants.MILLIS_PER_SECOND;
                    long hours = totalSeconds / TimeConstants.SECONDS_PER_HOUR;
                    long minutes = (totalSeconds % TimeConstants.SECONDS_PER_HOUR) / TimeConstants.SECONDS_PER_MINUTE;
                    long seconds = totalSeconds % 60;
                    SolverLogger.info("   Status: RESUME (cumulative time: " +
                        String.format("%dh %02dm %02ds", hours, minutes, seconds) + ")");
                } else {
                    SolverLogger.info("   Status: NEW");
                }
                SolverLogger.info("");

                PuzzleConfig config = configInfo.config;
                String configId = ConfigurationUtils.extractConfigId(configInfo.filepath);
                File currentSave = SaveStateManager.findCurrentSave(configId);

                if (currentSave != null && currentSave.exists()) {
                    // Resume from save
                    return resumeFromSave(config, configId, currentSave, threadId, timeoutMs);
                } else {
                    // Start from scratch
                    return solveFromScratch(config, configId, threadId, timeoutMs);
                }

            } catch (RuntimeException e) {
                SolverLogger.error("[Thread " + threadId + "] Error during solving: " + e.getMessage(), e);
                return false;
            }
        }

        private Boolean resumeFromSave(PuzzleConfig config, String configId, File saveFile, int threadId, long timeoutMs) {
            SaveStateManager.SaveState saveState = SaveStateManager.loadStateFromFile(saveFile, config.getType());
            if (saveState == null) return false;

            Board board = new Board(config.getRows(), config.getCols());
            Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

            boolean restored = SaveStateManager.restoreState(saveState, board, allPieces);
            if (!restored) return false;

            List<Integer> unusedIds = new ArrayList<>(saveState.unusedPieceIds);
            if ("descending".equalsIgnoreCase(config.getSortOrder())) {
                Collections.sort(unusedIds, Collections.reverseOrder());
            } else {
                Collections.sort(unusedIds);
            }

            EternitySolver solver = createSolver(config, configId, threadId, timeoutMs);
            SolverLogger.info("   [Thread " + threadId + "] Resume: " + saveState.depth + " pieces placed");

            boolean solved = solver.solveWithHistory(board, allPieces, unusedIds,
                                                     new ArrayList<>(saveState.placementOrder));
            if (solved) {
                SolverLogger.info("✅ [Thread " + threadId + "] SOLUTION FOUND!");
            }
            return solved;
        }

        private Boolean solveFromScratch(PuzzleConfig config, String configId, int threadId, long timeoutMs) {
            SolverLogger.info("   [Thread " + threadId + "] Starting from scratch");

            Board board = new Board(config.getRows(), config.getCols());
            Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

            // Place fixed pieces
            for (PuzzleConfig.FixedPiece fp : config.getFixedPieces()) {
                Piece piece = allPieces.get(fp.pieceId);
                if (piece != null) {
                    board.place(fp.row, fp.col, piece, fp.rotation);
                    allPieces.remove(fp.pieceId);
                }
            }

            EternitySolver solver = createSolver(config, configId, threadId, timeoutMs);

            SolverLogger.info("   [Thread " + threadId + "] Pieces to place: " + allPieces.size() + " pieces");
            SolverLogger.info("   [Thread " + threadId + "] Fixed pieces: " + config.getFixedPieces().size());
            SolverLogger.info("   [Thread " + threadId + "] Timeout: " + (timeoutMs / TimeConstants.MILLIS_PER_SECOND) + "s");
            SolverLogger.info("   [Thread " + threadId + "] Starting solver...");

            boolean solved = solver.solve(board, allPieces);
            SolverLogger.info("   [Thread " + threadId + "] Result: " + (solved ? "SOLUTION" : "No solution"));

            if (solved) {
                SolverLogger.info("✅ [Thread " + threadId + "] SOLUTION FOUND!");
            }
            return solved;
        }

        private EternitySolver createSolver(PuzzleConfig config, String configId, int threadId, long timeoutMs) {
            EternitySolver.resetGlobalState();
            EternitySolver solver = new EternitySolver();
            solver.setDisplayConfig(config.isVerbose(), config.getMinDepthToShowRecords());
            solver.setPuzzleName(configId);
            solver.setSortOrder(config.getSortOrder());
            solver.setPrioritizeBorders(config.isPrioritizeBorders());
            solver.setNumFixedPieces(config.getFixedPieces().size());
            solver.setThreadLabel(ConfigurationUtils.createThreadLabel(threadId, configId));
            solver.setMaxExecutionTime(timeoutMs);
            return solver;
        }
    }

    /**
     * Worker thread that loops with automatic rotation.
     */
    private static void runWorkerWithRotation(int threadId, long timeoutMs,
                                               ConfigurationScanner scanner,
                                               Set<String> solvedConfigs) throws Exception {
        while (true) {
            ConfigInfo nextConfig = null;
            String configId = null;

            // Atomic selection of next available config
            synchronized (configSelectionLock) {
                List<ConfigInfo> configs = scanner.scanConfigurations();

                for (ConfigInfo config : configs) {
                    String cid = ConfigurationUtils.extractConfigId(config.filepath);
                    if (!solvedConfigs.contains(cid) && !runningConfigs.contains(cid)) {
                        nextConfig = config;
                        configId = cid;
                        runningConfigs.add(configId);
                        break;
                    }
                }
            }

            if (nextConfig == null) {
                SolverLogger.info("🎉 [Thread " + threadId + "] All configurations solved/in-progress!");
                break;
            }

            try {
                // Display rotation message
                if (nextConfig.hasBeenStarted) {
                    long totalSeconds = nextConfig.totalComputeTimeMs / TimeConstants.MILLIS_PER_SECOND;
                    long hours = totalSeconds / TimeConstants.SECONDS_PER_HOUR;
                    long minutes = (totalSeconds % TimeConstants.SECONDS_PER_HOUR) / TimeConstants.SECONDS_PER_MINUTE;
                    SolverLogger.info("🔄 [Thread " + threadId + "] Rotating to: " + configId +
                        " (time: " + String.format("%dh%02dm", hours, minutes) + ")");
                } else {
                    SolverLogger.info("🔄 [Thread " + threadId + "] Rotating to: " + configId + " (NEW)");
                }

                SolverTask task = new SolverTask(nextConfig, threadId, timeoutMs);
                Boolean solved = task.call();

                if (solved != null && solved) {
                    SolverLogger.info("✅ [Thread " + threadId + "] SOLUTION for " + configId);
                    solvedConfigs.add(configId);
                } else {
                    SolverLogger.info("⏱️  [Thread " + threadId + "] Timeout for " + configId + " - rotating");
                }

            } catch (Exception e) {
                SolverLogger.error("[Thread " + threadId + "] Error: " + e.getMessage(), e);
            } finally {
                runningConfigs.remove(configId);
            }

            Thread.sleep(TimeConstants.DEFAULT_THREAD_SLEEP_MS);
        }
    }

    public static void main(String[] args) {
        SolverLogger.info("\n");
        SolverLogger.info("╔═══════════════════════════════════════════════════════════════════╗");
        SolverLogger.info("║          ETERNITY II - PARALLEL SOLVER                       ║");
        SolverLogger.info("╚═══════════════════════════════════════════════════════════════════╝");
        SolverLogger.info("");

        int numThreads = Runtime.getRuntime().availableProcessors();
        double timePerConfigMinutes = 1.0;

        if (args.length > 0) {
            try {
                numThreads = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                SolverLogger.error("⚠️  Invalid threads arg, using " + numThreads);
            }
        }

        if (args.length > 1) {
            try {
                timePerConfigMinutes = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                SolverLogger.error("⚠️  Invalid time arg, using " + timePerConfigMinutes + " min");
            }
        }

        SolverLogger.info("⚙️  Threads: " + numThreads);
        SolverLogger.info("⏱️  Time per config: " + timePerConfigMinutes + " min");
        SolverLogger.info("");

        try {
            ConfigurationScanner scanner = new ConfigurationScanner(DATA_DIR);
            List<ConfigInfo> configs = scanner.scanConfigurations();

            if (configs.isEmpty()) {
                SolverLogger.info("✗ No configurations found");
                return;
            }

            scanner.displayStatistics(configs);

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            SolverLogger.info("╔═══════════════════════════════════════════════════════════════════╗");
            SolverLogger.info("║              LAUNCHING THREADS WITH ROTATION                 ║");
            SolverLogger.info("╚═══════════════════════════════════════════════════════════════════╝");
            SolverLogger.info("");
            SolverLogger.info("📋 Strategy:");
            SolverLogger.info("   1. Each thread: " + timePerConfigMinutes + " min per config");
            SolverLogger.info("   2. Timeout → rotate to least advanced");
            SolverLogger.info("   3. Continuous rotation");
            SolverLogger.info("");
            SolverLogger.info("📋 Priority:");
            SolverLogger.info("   1. Never-started configs");
            SolverLogger.info("   2. Least cumulative time");
            SolverLogger.info("");

            Set<String> solvedConfigs = Collections.synchronizedSet(new HashSet<>());
            long timeoutMs = (long)(timePerConfigMinutes * TimeConstants.SECONDS_PER_MINUTE * TimeConstants.MILLIS_PER_SECOND);

            SolverLogger.info("✓ Starting " + numThreads + " threads with rotation");
            SolverLogger.info("");
            SolverLogger.info("═".repeat(70));
            SolverLogger.info("");

            for (int threadId = 1; threadId <= numThreads; threadId++) {
                final int tid = threadId;
                executor.submit(() -> {
                    try {
                        runWorkerWithRotation(tid, timeoutMs, scanner, solvedConfigs);
                    } catch (Exception e) {
                        SolverLogger.error("✗ [Thread " + tid + "] Fatal: " + e.getMessage());
                        SolverLogger.error("Error occurred", e);
                    }
                });
            }

            SolverLogger.info("⏳ Threads working... (Ctrl+C to stop)");
            SolverLogger.info("");

            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            SolverLogger.error("✗ Fatal error: " + e.getMessage());
            SolverLogger.error("Error occurred", e);
        }
    }
}
