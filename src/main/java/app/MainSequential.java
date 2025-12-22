package app;

import config.PuzzleConfig;
import service.PuzzleSolverOrchestrator;
import service.TimeoutExecutor;
import util.SolverLogger;
import util.TimeConstants;

import java.io.IOException;

/**
 * Sequential solver for all Eternity II puzzles.
 * Solves in order: Online → Indices 1-4 → Eternity 2.
 *
 * This class has been refactored to delegate responsibilities:
 * - PuzzleSolverOrchestrator: Handles puzzle solving orchestration
 * - SaveStateRestorationService: Handles save state loading/restoration
 * - SolutionDisplayService: Handles solution display
 * - TimeoutExecutor: Handles timeout execution
 */
public class MainSequential {

    private static final String DATA_DIR = "data/";

    // List of puzzles in order of resolution
    private static final String[] PUZZLE_FILES = {
        "online/online.txt",
        "indice1/indice1.txt",
        "indice2/indice2.txt",
        "indice3/indice3.txt",
        "indice4/indice4.txt",
        "eternity2/eternity2.txt"  // 256 pieces
    };

    // Timeout for each puzzle (10 minutes)
    private static final long PUZZLE_TIMEOUT = TimeConstants.DEFAULT_PUZZLE_TIMEOUT_MS;

    private final PuzzleSolverOrchestrator orchestrator;
    private final TimeoutExecutor timeoutExecutor;

    public MainSequential() {
        this.orchestrator = new PuzzleSolverOrchestrator();
        this.timeoutExecutor = new TimeoutExecutor();
    }

    public static void main(String[] args) {
        MainSequential main = new MainSequential();
        main.run();
    }

    /**
     * Main execution loop.
     * Continuously cycles through all puzzles with timeout.
     */
    public void run() {
        printBanner();

        // Solve each puzzle in order (with rotation every 10 minutes)
        while (true) {
            for (String puzzleFile : PUZZLE_FILES) {
                solveSinglePuzzle(puzzleFile);
            }

            printCycleComplete();
        }
    }

    /**
     * Solve a single puzzle with timeout.
     *
     * @param puzzleFile Relative path to puzzle file
     */
    private void solveSinglePuzzle(String puzzleFile) {
        String filepath = DATA_DIR + puzzleFile;

        try {
            // Load the puzzle
            PuzzleConfig config = PuzzleConfig.loadFromFile(filepath);

            if (config == null) {
                SolverLogger.info("✗ Unable to load: " + puzzleFile);
                SolverLogger.info("");
                return;
            }

            // Display information
            config.printInfo();
            SolverLogger.info("");

            // Solve the puzzle with timeout
            long startTime = System.currentTimeMillis();
            boolean solved = solvePuzzleWithTimeout(config, filepath);
            long duration = System.currentTimeMillis() - startTime;

            // Display summary
            config.printSummary(duration, solved);
            SolverLogger.info("");

            // Print completion message
            if (solved) {
                SolverLogger.info("  → ✓ Puzzle solved, moving to next...");
            } else {
                SolverLogger.info("  → ⏱ 10 minute timeout reached, moving to next puzzle...");
            }
            SolverLogger.info("");

        } catch (IOException e) {
            SolverLogger.error("✗ Error loading " + puzzleFile + ": " + e.getMessage(), e);
            SolverLogger.info("");
        }
    }

    /**
     * Solve puzzle with timeout using TimeoutExecutor.
     *
     * @param config Puzzle configuration
     * @param filepath Path to puzzle file
     * @return true if solved, false if timeout or error
     */
    private boolean solvePuzzleWithTimeout(PuzzleConfig config, String filepath) {
        TimeoutExecutor.TimeoutResult<Boolean> result = timeoutExecutor.executeBooleanWithTimeout(
                () -> orchestrator.solvePuzzle(config, filepath),
                PUZZLE_TIMEOUT);

        if (result.timedOut) {
            return false;
        }

        if (result.exception != null) {
            SolverLogger.error("  → ✗ Error: " + result.exception.getMessage(), result.exception);
            return false;
        }

        return result.result != null && result.result;
    }

    /**
     * Print application banner.
     */
    private void printBanner() {
        SolverLogger.info("\n");
        SolverLogger.info("╔═══════════════════════════════════════════════════════════════════╗");
        SolverLogger.info("║          ETERNITY II - SEQUENTIAL PUZZLE SOLVER                  ║");
        SolverLogger.info("╚═══════════════════════════════════════════════════════════════════╝");
        SolverLogger.info("");
    }

    /**
     * Print cycle complete message.
     */
    private void printCycleComplete() {
        SolverLogger.info("\n═══════════════════════════════════════════════════════════════════");
        SolverLogger.info("  Complete cycle finished, restarting from first puzzle...");
        SolverLogger.info("═══════════════════════════════════════════════════════════════════\n");
    }
}
