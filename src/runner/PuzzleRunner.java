package runner;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.ShutdownManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrateur pour l'exécution d'un puzzle.
 * Sépare la logique métier de l'interface CLI.
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
     * Configuration pour l'exécution du puzzle
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
     * Résultat de l'exécution
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
     * Constructeur
     */
    public PuzzleRunner(Board board, Map<Integer, Piece> pieces, PuzzleRunnerConfig config) {
        if (board == null) throw new IllegalArgumentException("Board ne peut pas être null");
        if (pieces == null) throw new IllegalArgumentException("Pieces ne peut pas être null");
        if (config == null) throw new IllegalArgumentException("Config ne peut pas être null");

        this.board = board;
        this.pieces = pieces;
        this.config = config;
    }

    /**
     * Configure le solver selon la configuration
     */
    private void configureSolver() {
        solver = new EternitySolver();
        solver.setDisplayConfig(config.isVerbose(), config.getMinDepth());
        solver.setUseSingletons(config.useSingletons());

        logger.info("Solver configuré: verbose={}, singletons={}, minDepth={}",
                   config.isVerbose(), config.useSingletons(), config.getMinDepth());
    }

    /**
     * Enregistre les hooks d'arrêt
     */
    private void registerShutdownHooks() {
        ShutdownManager.registerShutdownHook();

        // Hook pour afficher les statistiques
        ShutdownManager.addShutdownHook(
            new ShutdownManager.StatisticsShutdownHook(solver.getStatistics())
        );

        // Hook pour sauvegarder l'état
        ShutdownManager.addShutdownHook(
            new ShutdownManager.SolverShutdownHook(
                () -> logger.info("Sauvegarde demandée par shutdown hook"),
                "Puzzle"
            )
        );

        logger.debug("Shutdown hooks enregistrés");
    }

    /**
     * Détermine le nombre de threads à utiliser
     */
    private int determineThreadCount() {
        if (config.getThreads() > 0) {
            return config.getThreads();
        }
        // Auto: 75% des coeurs disponibles, minimum 4
        int cores = Runtime.getRuntime().availableProcessors();
        int threads = Math.max(4, (int)(cores * 0.75));
        logger.info("Threads auto-déterminés: {} (sur {} coeurs)", threads, cores);
        return threads;
    }

    /**
     * Exécute la résolution du puzzle
     */
    public PuzzleResult run() {
        logger.info("Démarrage résolution puzzle {}x{} avec {} pièces",
                   board.getRows(), board.getCols(), pieces.size());

        configureSolver();
        registerShutdownHooks();

        startTime = System.currentTimeMillis();

        // Lancer dans un thread séparé pour supporter l'interruption
        Thread solverThread = new Thread(() -> {
            try {
                if (config.isParallel()) {
                    int threads = determineThreadCount();
                    logger.info("Mode parallèle avec {} threads", threads);
                    solved.set(solver.solveParallel(board, pieces, pieces, threads));
                } else {
                    logger.info("Mode séquentiel");
                    solved.set(solver.solve(board, pieces));
                }
            } catch (Exception e) {
                logger.error("Erreur lors de la résolution", e);
                solved.set(false);
            }
        }, "PuzzleRunner-Solver");

        solverThread.start();

        // Attendre avec timeout optionnel
        try {
            if (config.getTimeoutSeconds() != null) {
                solverThread.join(config.getTimeoutSeconds() * 1000L);
                if (solverThread.isAlive()) {
                    logger.warn("Timeout atteint ({}s), interruption...", config.getTimeoutSeconds());
                    solverThread.interrupt();
                    solverThread.join(5000); // Attendre 5s pour l'arrêt gracieux
                }
            } else {
                solverThread.join();
            }
        } catch (InterruptedException e) {
            logger.info("Thread principal interrompu");
            solverThread.interrupt();
            ShutdownManager.requestShutdown();
        }

        endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;

        logger.info("Résolution terminée: solved={}, durée={}s", solved.get(), duration);

        return new PuzzleResult(
            solved.get(),
            duration,
            solver.getStatistics(),
            board
        );
    }

    /**
     * Obtient le solver (utile pour les tests)
     */
    public EternitySolver getSolver() {
        return solver;
    }

    /**
     * Vérifie si la résolution est terminée
     */
    public boolean isFinished() {
        return endTime > 0;
    }

    /**
     * Obtient la durée d'exécution (en secondes)
     */
    public double getDuration() {
        if (endTime == 0) return 0;
        return (endTime - startTime) / 1000.0;
    }
}
