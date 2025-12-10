// SaveStateRestorationService.java (moved to default package for PuzzleConfig access)
import model.Board;
import model.Piece;
import util.ConfigurationUtils;
import util.SaveStateManager;
import util.SolverLogger;

import java.io.File;
import java.util.*;

/**
 * Service for loading and restoring puzzle save states.
 * Handles save file discovery, loading, validation, and board restoration.
 */
public class SaveStateRestorationService {

    private final SolutionDisplayService displayService;

    public SaveStateRestorationService() {
        this.displayService = new SolutionDisplayService();
    }

    /**
     * Result of save state restoration attempt.
     */
    public static class RestorationResult {
        public final boolean success;
        public final SaveStateManager.SaveState saveState;
        public final Board board;
        public final List<Integer> unusedIds;

        public RestorationResult(boolean success, SaveStateManager.SaveState saveState,
                                Board board, List<Integer> unusedIds) {
            this.success = success;
            this.saveState = saveState;
            this.board = board;
            this.unusedIds = unusedIds;
        }

        public static RestorationResult failure() {
            return new RestorationResult(false, null, null, null);
        }

        public static RestorationResult success(SaveStateManager.SaveState saveState,
                                               Board board, List<Integer> unusedIds) {
            return new RestorationResult(true, saveState, board, unusedIds);
        }
    }

    /**
     * Load and restore save state if available.
     * Returns null if no save found or restoration fails.
     *
     * @param puzzleType Puzzle type (e.g., "eternity2")
     * @param config Puzzle configuration
     * @return RestorationResult with loaded state, or failure result
     */
    public RestorationResult loadAndRestoreSaveState(String puzzleType, PuzzleConfig config) {
        File currentSave = SaveStateManager.findCurrentSave(puzzleType);

        if (currentSave == null) {
            SolverLogger.info("  → No current save found");
            return RestorationResult.failure();
        }

        SolverLogger.info("  → 📂 Current save found");
        SolverLogger.info("  → Resuming solving from saved state...");

        // Load the save
        SaveStateManager.SaveState saveState = SaveStateManager.loadStateFromFile(currentSave, puzzleType);
        if (saveState == null) {
            SolverLogger.info("  → ⚠️  Loading error, starting from scratch...");
            return RestorationResult.failure();
        }

        SolverLogger.info("  → Saved state: " + saveState.depth + " pieces placed");

        // Check if puzzle is already solved
        int totalPieces = config.getRows() * config.getCols();
        if (saveState.depth == totalPieces) {
            SolverLogger.info("  → ✅ Puzzle already solved! (" + totalPieces + "/" + totalPieces + " pieces)");

            Board board = new Board(config.getRows(), config.getCols());
            Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

            boolean restored = SaveStateManager.restoreState(saveState, board, allPieces);
            if (restored) {
                SolverLogger.info("");
                displayService.displaySolution(board, allPieces, totalPieces);
            }

            // Return success but with null unusedIds to signal "already solved"
            return RestorationResult.success(saveState, board, null);
        }

        // Create a new board and restore the complete state
        Board board = new Board(config.getRows(), config.getCols());
        Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

        boolean restored = SaveStateManager.restoreState(saveState, board, allPieces);
        if (!restored) {
            SolverLogger.info("  → ⚠️  Restoration error, starting from scratch...");
            return RestorationResult.failure();
        }

        // Prepare unused pieces
        List<Integer> unusedIds = new ArrayList<>(saveState.unusedPieceIds);

        // Sort according to configured order
        ConfigurationUtils.sortPiecesByOrder(unusedIds, config.getSortOrder());

        SolverLogger.info("  → Resuming from: " + saveState.depth + " pieces (ALL pieces can be backtracked)");
        SolverLogger.info("  → " + unusedIds.size() + " pieces remaining to place");
        SolverLogger.info("  → Sort order: " + config.getSortOrder());

        return RestorationResult.success(saveState, board, unusedIds);
    }

    /**
     * Display best solutions found so far.
     * Shows the best state reached and compares with current state.
     *
     * @param puzzleType Puzzle type
     * @param config Puzzle configuration
     * @param currentBoard Current board state for comparison
     */
    public void displayBestSolutionsIfAvailable(String puzzleType, PuzzleConfig config, Board currentBoard) {
        List<File> bestSaves = SaveStateManager.findAllSaves(puzzleType);
        if (bestSaves.isEmpty()) {
            return;
        }

        SolverLogger.info("  → 📊 " + bestSaves.size() + " best score(s) saved");

        // Display the best solution found so far
        File bestSave = bestSaves.get(0); // The first is the best (sorted by depth)
        SaveStateManager.SaveState bestState = SaveStateManager.loadStateFromFile(bestSave, puzzleType);

        if (bestState == null) {
            return;
        }

        SolverLogger.info("  → 🏆 Best solution reached: " + bestState.depth + " pieces");
        SolverLogger.info("");

        // Create a board to display the best solution
        Board bestBoard = new Board(config.getRows(), config.getCols());
        Map<Integer, Piece> bestPieces = new HashMap<>(config.getPieces());

        boolean bestRestored = SaveStateManager.restoreState(bestState, bestBoard, bestPieces);
        if (!bestRestored) {
            return;
        }

        List<Integer> bestUnusedIds = new ArrayList<>(bestState.unusedPieceIds);

        // Display with comparison to current state
        displayService.displayBestSolution(bestBoard, currentBoard, bestPieces, bestUnusedIds);
    }
}
