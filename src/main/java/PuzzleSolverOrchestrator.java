// PuzzleSolverOrchestrator.java (moved to default package for PuzzleConfig access)
import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.ConfigurationUtils;
import util.SaveStateManager;
import util.SolverLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates puzzle solving process.
 * Handles solver configuration, execution, and result display.
 */
public class PuzzleSolverOrchestrator {

    private final SaveStateRestorationService restorationService;
    private final SolutionDisplayService displayService;

    public PuzzleSolverOrchestrator() {
        this.restorationService = new SaveStateRestorationService();
        this.displayService = new SolutionDisplayService();
    }

    /**
     * Solve a puzzle, either from scratch or resuming from save.
     *
     * @param config Puzzle configuration
     * @param filepath Path to puzzle file
     * @return true if solved, false otherwise
     */
    public boolean solvePuzzle(PuzzleConfig config, String filepath) {
        try {
            String puzzleType = config.getType();

            // Try to restore from save
            SaveStateRestorationService.RestorationResult restoration =
                    restorationService.loadAndRestoreSaveState(puzzleType, config);

            if (!restoration.success) {
                // No save or restoration failed - start from scratch
                return solvePuzzleFromScratch(config, filepath);
            }

            // Check if puzzle is already solved (unusedIds == null signals this)
            if (restoration.unusedIds == null) {
                return true;
            }

            // Display best solutions if available
            restorationService.displayBestSolutionsIfAvailable(
                    puzzleType, config, restoration.board);

            SolverLogger.info("  → Backtracking will be able to go back through ALL "
                    + restoration.saveState.depth + " pre-loaded pieces");
            SolverLogger.info("");

            // Display current state for validation
            displayService.displayPuzzleStateForValidation(
                    restoration.board,
                    new HashMap<>(config.getPieces()),
                    restoration.unusedIds);

            // Solve with complete backtracking
            boolean solved = solveWithHistory(
                    config,
                    filepath,
                    restoration.board,
                    restoration.saveState,
                    restoration.unusedIds);

            if (solved) {
                SolverLogger.info("\n  → ✅ Solution found!");
                SolverLogger.info("");
                int totalPieces = config.getRows() * config.getCols();
                displayService.displaySolution(restoration.board,
                        new HashMap<>(config.getPieces()), totalPieces);
            } else {
                SolverLogger.info("  → ✗ No solution found");
            }

            return solved;

        } catch (RuntimeException e) {
            SolverLogger.error("Error during puzzle solving from save: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Solve puzzle from scratch (no save file).
     *
     * @param config Puzzle configuration
     * @param filepath Path to puzzle file
     * @return true if solved, false otherwise
     */
    public boolean solvePuzzleFromScratch(PuzzleConfig config, String filepath) {
        try {
            SolverLogger.info("  → Starting from scratch...");

            Board board = new Board(config.getRows(), config.getCols());
            Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

            // Place fixed pieces
            placeFixedPieces(config, board);

            // Configure and run solver
            EternitySolver solver = createAndConfigureSolver(config, filepath);

            SolverLogger.info("  → Solving in progress...");
            SolverLogger.info("  → Sort order: " + config.getSortOrder());
            SolverLogger.info("  → Automatic save every 1 minute");
            SolverLogger.info("  → Puzzle change every 10 minutes");

            boolean solved = solver.solve(board, allPieces);

            if (solved) {
                SolverLogger.info("  → ✓ Solution found!");
                SolverLogger.info("");
                int totalPieces = config.getRows() * config.getCols();
                displayService.displaySolution(board, allPieces, totalPieces);
            } else {
                SolverLogger.info("  → ✗ No solution found");
            }

            return solved;

        } catch (RuntimeException e) {
            SolverLogger.error("Error during puzzle solving from scratch: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Solve puzzle with history (resuming from save).
     *
     * @param config Puzzle configuration
     * @param filepath Path to puzzle file
     * @param board Restored board state
     * @param saveState Loaded save state
     * @param unusedIds Unused piece IDs
     * @return true if solved, false otherwise
     */
    private boolean solveWithHistory(PuzzleConfig config, String filepath,
                                     Board board,
                                     SaveStateManager.SaveState saveState,
                                     List<Integer> unusedIds) {
        EternitySolver solver = createAndConfigureSolver(config, filepath);

        Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

        return solver.solveWithHistory(
                board,
                allPieces,
                unusedIds,
                new ArrayList<>(saveState.placementOrder));
    }

    /**
     * Create and configure solver for given puzzle.
     *
     * @param config Puzzle configuration
     * @param filepath Path to puzzle file
     * @return Configured solver instance
     */
    private EternitySolver createAndConfigureSolver(PuzzleConfig config, String filepath) {
        EternitySolver.resetGlobalState();
        EternitySolver solver = new EternitySolver();

        solver.setDisplayConfig(config.isVerbose(), config.getMinDepthToShowRecords());

        String configId = ConfigurationUtils.extractConfigId(filepath);
        solver.setPuzzleName(configId);
        solver.setSortOrder(config.getSortOrder());

        return solver;
    }

    /**
     * Place all fixed pieces on the board.
     *
     * @param config Puzzle configuration
     * @param board Board to place pieces on
     */
    private void placeFixedPieces(PuzzleConfig config, Board board) {
        for (PuzzleConfig.FixedPiece fp : config.getFixedPieces()) {
            Piece piece = config.getPieces().get(fp.pieceId);
            if (piece != null) {
                board.place(fp.row, fp.col, piece, fp.rotation);
                config.getPieces().remove(fp.pieceId);
                SolverLogger.info("  → Fixed piece " + fp.pieceId + " placed at ["
                        + fp.row + "," + fp.col + "] rotation " + fp.rotation);
            }
        }
    }
}
