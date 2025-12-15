package runner;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.ShutdownManager;
import util.TimeConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrator for puzzle execution.
 * Separates business logic from CLI interface.
 */
public class PuzzleRunner {

    private static final Logger logger = LoggerFactory.getLogger(PuzzleRunner.class);

    private final Board board;
    private final Map<Integer, Piece> pieces;
    private final PuzzleRunnerConfig config;
    private final AtomicBoolean solved = new AtomicBoolean(false);
    private EternitySolver solver;
    private long startTime;
    private long endTime;

    /**
     * Configuration for puzzle execution
     */
    public static class PuzzleRunnerConfig {
        private boolean verbose = false;
        private boolean parallel = false;
        private boolean useSingletons = true;
        private int threads = -1; // -1 = auto
        private int minDepth = 0;
        private Integer timeoutSeconds = null;

        public PuzzleRunnerConfig() {}

        public PuzzleRunnerConfig setVerbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public PuzzleRunnerConfig setParallel(boolean parallel) {
            this.parallel = parallel;
            return this;
        }

        public PuzzleRunnerConfig setUseSingletons(boolean useSingletons) {
            this.useSingletons = useSingletons;
            return this;
        }

        public PuzzleRunnerConfig setThreads(int threads) {
            this.threads = threads;
            return this;
        }

        public PuzzleRunnerConfig setMinDepth(int minDepth) {
            this.minDepth = minDepth;
            return this;
        }

        public PuzzleRunnerConfig setTimeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public boolean isVerbose() { return verbose; }
        public boolean isParallel() { return parallel; }
        public boolean useSingletons() { return useSingletons; }
        public int getThreads() { return threads; }
        public int getMinDepth() { return minDepth; }
        public Integer getTimeoutSeconds() { return timeoutSeconds; }
    }

    /**
     * Execution result
     */
    public static class PuzzleResult {
        private final boolean solved;
        private final double durationSeconds;
        private final EternitySolver.Statistics statistics;
        private final Board board;

        public PuzzleResult(boolean solved, double durationSeconds,
                           EternitySolver.Statistics statistics, Board board) {
            this.solved = solved;
            this.durationSeconds = durationSeconds;
            this.statistics = statistics;
            this.board = board;
        }

        public boolean isSolved() { return solved; }
        public double getDurationSeconds() { return durationSeconds; }
        public EternitySolver.Statistics getStatistics() { return statistics; }
        public Board getBoard() { return board; }
    }

    /**
     * Constructor
     */
    public PuzzleRunner(Board board, Map<Integer, Piece> pieces, PuzzleRunnerConfig config) {
        if (board == null) throw new IllegalArgumentException("Board cannot be null");
        if (pieces == null) throw new IllegalArgumentException("Pieces cannot be null");
        if (config == null) throw new IllegalArgumentException("Config cannot be null");

        this.board = board;
        this.pieces = pieces;
        this.config = config;
    }

    /**
     * Configures the solver according to configuration
     */
    private void configureSolver() {
        solver = new EternitySolver();
        solver.setDisplayConfig(config.isVerbose(), config.getMinDepth());
        solver.setUseSingletons(config.useSingletons());

        logger.info("Solver configured: verbose={}, singletons={}, minDepth={}",
                   config.isVerbose(), config.useSingletons(), config.getMinDepth());
    }

    /**
     * Registers shutdown hooks
     */
    private void registerShutdownHooks() {
        ShutdownManager.registerShutdownHook();

        // Hook to display statistics
        ShutdownManager.addShutdownHook(
            new ShutdownManager.StatisticsShutdownHook(solver.getStatistics())
        );

        // Hook to save state
        ShutdownManager.addShutdownHook(
            new ShutdownManager.SolverShutdownHook(
                () -> logger.info("Save requested by shutdown hook"),
                "Puzzle"
            )
        );

        logger.debug("Shutdown hooks registered");
    }

    /**
     * Determines the number of threads to use
     */
    private int determineThreadCount() {
        if (config.getThreads() > 0) {
            return config.getThreads();
        }
        // Auto: 75% of available cores, minimum 4
        int cores = Runtime.getRuntime().availableProcessors();
        int threads = Math.max(4, (int)(cores * 0.75));
        logger.info("Threads auto-determined: {} (out of {} cores)", threads, cores);
        return threads;
    }

    /**
     * Executes the puzzle resolution
     */
    public PuzzleResult run() {
        logger.info("Starting puzzle resolution {}x{} with {} pieces",
                   board.getRows(), board.getCols(), pieces.size());

        configureSolver();
        registerShutdownHooks();

        startTime = System.currentTimeMillis();

        // Launch in separate thread to support interruption
        Thread solverThread = new Thread(() -> {
            try {
                if (config.isParallel()) {
                    int threads = determineThreadCount();
                    logger.info("Parallel mode with {} threads", threads);
                    solved.set(solver.solveParallel(board, pieces, pieces, threads));
                } else {
                    logger.info("Sequential mode");
                    solved.set(solver.solve(board, pieces));
                }
            } catch (RuntimeException e) {
                logger.error("Error during resolution", e);
                solved.set(false);
            }
        }, "PuzzleRunner-Solver");

        solverThread.start();

        // Wait with optional timeout
        try {
            if (config.getTimeoutSeconds() != null) {
                solverThread.join(config.getTimeoutSeconds() * 1000L);
                if (solverThread.isAlive()) {
                    logger.warn("Timeout reached ({}s), interrupting...", config.getTimeoutSeconds());
                    solverThread.interrupt();
                    solverThread.join(TimeConstants.DEFAULT_THREAD_JOIN_TIMEOUT_MS); // Wait 5s for graceful shutdown
                }
            } else {
                solverThread.join();
            }
        } catch (InterruptedException e) {
            logger.info("Main thread interrupted");
            solverThread.interrupt();
            ShutdownManager.requestShutdown();
        }

        endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / (double)TimeConstants.MILLIS_PER_SECOND;

        logger.info("Resolution completed: solved={}, duration={}s", solved.get(), duration);

        return new PuzzleResult(
            solved.get(),
            duration,
            solver.getStatistics(),
            board
        );
    }

    /**
     * Gets the solver (useful for tests)
     */
    public EternitySolver getSolver() {
        return solver;
    }

    /**
     * Checks if resolution is finished
     */
    public boolean isFinished() {
        return endTime > 0;
    }

    /**
     * Gets the execution duration (in seconds)
     */
    public double getDuration() {
        if (endTime == 0) return 0;
        return (endTime - startTime) / (double)TimeConstants.MILLIS_PER_SECOND;
    }
}
