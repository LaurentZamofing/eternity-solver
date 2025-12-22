package app;

import util.SolverLogger;
import cli.CommandLineInterface;
import model.Board;
import model.Piece;
import runner.PuzzleRunner;
import runner.PuzzleRunner.PuzzleRunnerConfig;
import runner.PuzzleRunner.PuzzleResult;
import util.PuzzleFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Main entry point with professional CLI interface.
 * Replaces Main.java with full argument support.
 */
public class MainCLI {

    private static final Logger logger = LoggerFactory.getLogger(MainCLI.class);

    public static void main(String[] args) {
        CommandLineInterface cli = new CommandLineInterface();

        // Parse arguments
        if (!cli.parse(args)) {
            SolverLogger.error("Error: " + cli.getErrorMessage());
            SolverLogger.error("");
            cli.printHelp();
            System.exit(1);
        }

        // Display help if requested
        if (cli.isHelpRequested()) {
            cli.printHelp();
            System.exit(0);
        }

        // Display version if requested
        if (cli.isVersionRequested()) {
            cli.printVersion();
            System.exit(0);
        }

        // Check that a puzzle was specified
        String puzzleName = cli.getPuzzleName();
        if (puzzleName == null) {
            SolverLogger.error("Error: No puzzle specified");
            SolverLogger.error("");
            cli.printHelp();
            System.exit(1);
        }

        // Log configuration
        if (!cli.isQuiet()) {
            cli.printConfiguration();
        }

        // Create the puzzle according to name
        PuzzleInfo puzzleInfo = createPuzzle(puzzleName);
        if (puzzleInfo == null) {
            SolverLogger.error("Error: Unknown puzzle: " + puzzleName);
            SolverLogger.error("");
            SolverLogger.error("Available puzzles:");
            SolverLogger.error("  - puzzle_6x12, puzzle_16x16, validation_6x6");
            SolverLogger.error("  - example_3x3, example_4x4, example_4x4_easy");
            SolverLogger.error("  - eternity2_p* (voir data/eternity2/)");
            System.exit(1);
        }

        // Display header
        if (!cli.isQuiet()) {
            printHeader(puzzleInfo.name, puzzleInfo.board.getRows(), puzzleInfo.board.getCols(),
                       puzzleInfo.pieces.size());
        }

        // Configure the runner
        PuzzleRunnerConfig config = new PuzzleRunnerConfig()
            .setVerbose(cli.isVerbose() && !cli.isQuiet())
            .setParallel(cli.isParallel())
            .setUseSingletons(cli.useSingletons())
            .setMinDepth(cli.getMinDepth() != null ? cli.getMinDepth() : 0);

        if (cli.getThreads() != null) {
            config.setThreads(cli.getThreads());
        }
        if (cli.getTimeout() != null) {
            config.setTimeoutSeconds(cli.getTimeout());
        }

        // Create and run the runner
        PuzzleRunner runner = new PuzzleRunner(puzzleInfo.board, puzzleInfo.pieces, config);
        PuzzleResult result = runner.run();

        // Display result
        SolverLogger.info("");
        SolverLogger.info("═══════════════════════════════════════════════════════");
        SolverLogger.info("RESULT");
        SolverLogger.info("═══════════════════════════════════════════════════════");

        if (result.isSolved()) {
            SolverLogger.info("✓ PUZZLE SOLVED!");
            SolverLogger.info("Time: " + String.format("%.2f", result.getDurationSeconds()) + " seconds");
            SolverLogger.info("");

            if (!cli.isQuiet()) {
                result.getBoard().prettyPrint(puzzleInfo.pieces);
                result.getBoard().printScore();
            }
            System.exit(0);
        } else {
            SolverLogger.info("✗ No solution found");
            SolverLogger.info("Time: " + String.format("%.2f", result.getDurationSeconds()) + " seconds");
            System.exit(1);
        }
    }

    /**
     * Displays the program header
     */
    private static void printHeader(String puzzleName, int rows, int cols, int numPieces) {
        SolverLogger.info("╔═══════════════════════════════════════════════════════╗");
        SolverLogger.info("║          ETERNITY PUZZLE SOLVER                       ║");
        SolverLogger.info("╚═══════════════════════════════════════════════════════╝");
        SolverLogger.info("");
        SolverLogger.info("Puzzle: " + puzzleName);
        SolverLogger.info("Size: " + rows + "×" + cols + " (" + numPieces + " pieces)");
        SolverLogger.info("");
    }

    /**
     * Creates the puzzle according to name
     */
    private static PuzzleInfo createPuzzle(String name) {
        switch (name) {
            case "puzzle_6x12":
            case "6x12":
                return new PuzzleInfo("Puzzle 6×12", new Board(6, 12),
                                     PuzzleFactory.createPuzzle6x12());

            case "puzzle_16x16":
            case "16x16":
                return new PuzzleInfo("Puzzle 16×16", new Board(16, 16),
                                     PuzzleFactory.createPuzzle16x16());

            case "validation_6x6":
            case "6x6":
                return new PuzzleInfo("Validation 6×6", new Board(6, 6),
                                     PuzzleFactory.createValidation6x6());

            case "example_3x3":
            case "3x3":
                return new PuzzleInfo("Example 3×3", new Board(3, 3),
                                     PuzzleFactory.createExample3x3());

            case "example_4x4":
            case "4x4":
                return new PuzzleInfo("Example 4×4", new Board(4, 4),
                                     PuzzleFactory.createExample4x4());

            case "example_4x4_easy":
            case "4x4_easy":
                return new PuzzleInfo("Example 4×4 (easy)", new Board(4, 4),
                                     PuzzleFactory.createExample4x4Easy());

            case "example_4x4_ordered":
            case "4x4_ordered":
                return new PuzzleInfo("Example 4×4 (ordered)", new Board(4, 4),
                                     PuzzleFactory.createExample4x4Ordered());

            case "example_5x5":
            case "5x5":
                return new PuzzleInfo("Example 5×5", new Board(5, 5),
                                     PuzzleFactory.createExample5x5());

            case "eternity2":
            case "eternity_ii":
                return new PuzzleInfo("Eternity II 16×16", new Board(16, 16),
                                     PuzzleFactory.createEternityII());

            default:
                // Check if it's an eternity2_p* config
                if (name.startsWith("eternity2_p") || name.startsWith("indice_")) {
                    logger.warn("Configuration {} requires PuzzleConfig.loadFromFile()", name);
                }
                return null;
        }
    }

    /**
     * Inner class to store puzzle information
     */
    private static class PuzzleInfo {
        final String name;
        final Board board;
        final Map<Integer, Piece> pieces;

        PuzzleInfo(String name, Board board, Map<Integer, Piece> pieces) {
            this.name = name;
            this.board = board;
            this.pieces = pieces;
        }
    }
}
