import model.Board;
import model.Piece;
import model.Placement;
import solver.EternitySolver;
import util.ConfigurationUtils;
import util.FormattingUtils;
import util.SaveStateManager;
import util.TimeConstants;
import java.io.*;
import java.util.*;

/**
 * Sequential solver for all Eternity II puzzles
 * Solve in order: Online → Indices 1-4 → Eternity 2
 */
public class MainSequential {

    private static final String DATA_DIR = "data/";

    // Removed: extractConfigId() - now using ConfigurationUtils.extractConfigId()

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

    public static void main(String[] args) {
        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║          ETERNITY II - SEQUENTIAL PUZZLE SOLVER                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        List<PuzzleResult> results = new ArrayList<>();
        long totalStartTime = System.currentTimeMillis();

        // Solve each puzzle in order (with rotation every 10 minutes)
        while (true) {
            for (String puzzleFile : PUZZLE_FILES) {
                String filepath = DATA_DIR + puzzleFile;

                try {
                    // Load the puzzle
                    PuzzleConfig config = PuzzleConfig.loadFromFile(filepath);

                    if (config == null) {
                        System.out.println("✗ Unable to load: " + puzzleFile);
                        System.out.println();
                        continue;
                    }

                    // Display information
                    config.printInfo();
                    System.out.println();

                    // Solve the puzzle with 10 minute timeout
                    long startTime = System.currentTimeMillis();
                    boolean solved = solvePuzzleWithTimeout(config, filepath, PUZZLE_TIMEOUT);
                    long duration = System.currentTimeMillis() - startTime;

                    // Record the result
                    results.add(new PuzzleResult(config.getName(), config.getType(),
                                                 config.getPieces().size(), solved, duration));

                    // Display summary
                    config.printSummary(duration, solved);
                    System.out.println();

                    // If solved, we can move to the next
                    if (solved) {
                        System.out.println("  → ✓ Puzzle solved, moving to next...");
                        System.out.println();
                    } else {
                        System.out.println("  → ⏱ 10 minute timeout reached, moving to next puzzle...");
                        System.out.println();
                    }

                } catch (IOException e) {
                    System.out.println("✗ Error loading " + puzzleFile + ": " + e.getMessage());
                    System.out.println();
                }
            }

            // Once all puzzles have been processed, restart from the beginning
            System.out.println("\n═══════════════════════════════════════════════════════════════════");
            System.out.println("  Complete cycle finished, restarting from first puzzle...");
            System.out.println("═══════════════════════════════════════════════════════════════════\n");
        }

        // Note: This section is never reached because the loop is infinite
        // The program runs continuously and changes puzzles every 10 minutes
    }

    /**
     * Solves a given puzzle
     * Loads the current save if available, otherwise starts from scratch
     * Backtracking is done in memory by the solver
     */
    private static boolean solvePuzzle(PuzzleConfig config, String filepath) {
        try {
            // Check if a "current" save exists
            File currentSave = SaveStateManager.findCurrentSave(config.getType());

            if (currentSave != null) {
                System.out.println("  → 📂 Current save found");
                System.out.println("  → Resuming solving from saved state...");

                // Load the save
                SaveStateManager.SaveState saveState = SaveStateManager.loadStateFromFile(currentSave, config.getType());
                if (saveState == null) {
                    System.out.println("  → ⚠️  Loading error, starting from scratch...");
                    return solvePuzzleFromScratch(config, filepath);
                }

                System.out.println("  → Saved state: " + saveState.depth + " pieces placed");

                // Check if the puzzle is already completely solved
                int totalPieces = config.getRows() * config.getCols();
                if (saveState.depth == totalPieces) {
                    System.out.println("  → ✅ Puzzle already solved! (" + totalPieces + "/" + totalPieces + " pieces)");

                    // Create a board to display the solution
                    Board board = new Board(config.getRows(), config.getCols());
                    Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

                    boolean restored = SaveStateManager.restoreState(saveState, board, allPieces);
                    if (restored) {
                        System.out.println();
                        if (totalPieces <= 72) {
                            // Detailed display for small puzzles
                            displayDetailedSolution(board, allPieces);
                        } else {
                            // Simple display for large puzzles
                            displaySolution(board);
                        }
                    }

                    return true;
                }

                // NEW: Complete backtracking with history
                // We don't remove ANY piece, we pass the complete history to the solver
                // The solver will be able to backtrack through ALL pre-loaded pieces
                System.out.println("  → Resuming from: " + saveState.depth + " pieces (ALL pieces can be backtracked)");

                // Create a new board and restore the complete state
                Board board = new Board(config.getRows(), config.getCols());
                Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

                boolean restored = SaveStateManager.restoreState(saveState, board, allPieces);
                if (!restored) {
                    System.out.println("  → ⚠️  Restoration error, starting from scratch...");
                    return solvePuzzleFromScratch(config, filepath);
                }

                // Prepare unused pieces
                List<Integer> unusedIds = new ArrayList<>(saveState.unusedPieceIds);

                // Sort according to configured order (ascending/descending)
                ConfigurationUtils.sortPiecesByOrder(unusedIds, config.getSortOrder());

                System.out.println("  → " + unusedIds.size() + " pieces remaining to place");
                System.out.println("  → Sort order: " + config.getSortOrder());

                // Check available best scores
                List<File> bestSaves = SaveStateManager.findAllSaves(config.getType());
                if (!bestSaves.isEmpty()) {
                    System.out.println("  → 📊 " + bestSaves.size() + " best score(s) saved");

                    // Display the best solution found so far
                    File bestSave = bestSaves.get(0); // The first is the best (sorted by depth)
                    SaveStateManager.SaveState bestState = SaveStateManager.loadStateFromFile(bestSave, config.getType());

                    if (bestState != null) {
                        System.out.println("  → 🏆 Best solution reached: " + bestState.depth + " pieces");
                        System.out.println();

                        // Create a board to display the best solution
                        Board bestBoard = new Board(config.getRows(), config.getCols());
                        Map<Integer, Piece> bestPieces = new HashMap<>(config.getPieces());

                        boolean bestRestored = SaveStateManager.restoreState(bestState, bestBoard, bestPieces);
                        if (bestRestored) {
                            System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
                            System.out.println("║              BEST SOLUTION REACHED (RECORD)                      ║");
                            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
                            System.out.println();
                            System.out.println("State with the most pieces placed so far:");
                            System.out.println();
                            System.out.println("Color legend (RECORD vs current CURRENT comparison):");
                            System.out.println("  - \033[1;35mMagenta\033[0m: Cell occupied in RECORD but empty in CURRENT (regression)");
                            System.out.println("  - \033[1;38;5;208mOrange\033[0m: Different piece between RECORD and CURRENT (change)");
                            System.out.println("  - \033[1;33mYellow\033[0m: Cell empty in RECORD but occupied in CURRENT (progression)");
                            System.out.println("  - \033[1;36mCyan\033[0m: Identical cell in RECORD and CURRENT (stability)");
                            System.out.println();

                            // Create a temporary solver for display with comparison
                            EternitySolver tempSolver = new EternitySolver();
                            List<Integer> bestUnusedIds = new ArrayList<>(bestState.unusedPieceIds);

                            // Use comparison to show differences with current
                            tempSolver.printBoardWithComparison(bestBoard, board, bestPieces, bestUnusedIds);
                            System.out.println();

                            bestBoard.printScore();
                            System.out.println();
                            System.out.println("═".repeat(70));
                            System.out.println();
                        }
                    }
                }

                // Create and configure the solver
                EternitySolver.resetGlobalState();
                EternitySolver solver = new EternitySolver();
                solver.setDisplayConfig(config.isVerbose(), config.getMinDepthToShowRecords());
                String configId = ConfigurationUtils.extractConfigId(filepath);
                solver.setPuzzleName(configId);
                solver.setSortOrder(config.getSortOrder());

                System.out.println("  → Backtracking will be able to go back through ALL " + saveState.depth + " pre-loaded pieces");
                System.out.println();

                // Display the complete state of the loaded puzzle for validation
                System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
                System.out.println("║              LOADED PUZZLE STATE (VALIDATION)                    ║");
                System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
                System.out.println();
                System.out.println("Legend:");
                System.out.println("  - Placed pieces: Piece ID with edge values (N/E/S/W)");
                System.out.println("  - Empty cells: (XXX) = number of valid pieces possible");
                System.out.println("  - \033[93mYellow\033[0m: critical cells (≤20 possibilities)");
                System.out.println("  - \033[1;91mRed\033[0m: dead-end (0 possibilities)");
                System.out.println();
                solver.printBoardWithLabels(board, allPieces, unusedIds);
                System.out.println();

                // Display current score
                board.printScore();
                System.out.println();
                System.out.println("═".repeat(70));
                System.out.println();

                // Solve with complete backtracking (new method with history)
                boolean solved = solver.solveWithHistory(board, allPieces, unusedIds,
                                                         new ArrayList<>(saveState.placementOrder));

                if (solved) {
                    System.out.println("\n  → ✅ Solution found!");
                    System.out.println();
                    if (totalPieces <= 72) {
                        // Detailed display for small puzzles
                        displayDetailedSolution(board, allPieces);
                    } else {
                        // Simple display for large puzzles
                        displaySolution(board);
                    }
                } else {
                    System.out.println("  → ✗ No solution found");
                }

                return solved;
            }

            // No current save - classic start
            System.out.println("  → No current save found");
            return solvePuzzleFromScratch(config, filepath);

        } catch (Exception e) {
            System.out.println("  → ✗ Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Solves a puzzle with a timeout (for automatic puzzle rotation)
     */
    private static boolean solvePuzzleWithTimeout(PuzzleConfig config, String filepath, long timeout) {
        final boolean[] solved = {false};
        final Exception[] exception = {null};

        Thread solverThread = new Thread(() -> {
            try {
                solved[0] = solvePuzzle(config, filepath);
            } catch (Exception e) {
                exception[0] = e;
            }
        });

        solverThread.start();

        try {
            solverThread.join(timeout);

            if (solverThread.isAlive()) {
                // Timeout reached - interrupt the thread
                System.out.println("\n  → ⏱ 10 minute timeout reached");
                System.out.println("  → Interrupting solving and saving state...");
                solverThread.interrupt();
                solverThread.join(TimeConstants.DEFAULT_THREAD_JOIN_TIMEOUT_MS); // Wait 5 seconds for thread to terminate cleanly

                if (solverThread.isAlive()) {
                    // Force kill if thread doesn't terminate
                    System.out.println("  → Forced stop of solving thread");
                }

                return false; // Not solved within allotted time
            }

            if (exception[0] != null) {
                System.out.println("  → ✗ Error: " + exception[0].getMessage());
                exception[0].printStackTrace();
                return false;
            }

            return solved[0];

        } catch (InterruptedException e) {
            System.out.println("  → Main thread interrupted");
            solverThread.interrupt();
            return false;
        }
    }

    /**
     * Solves a puzzle from scratch (without save)
     */
    private static boolean solvePuzzleFromScratch(PuzzleConfig config, String filepath) {
        try {
            System.out.println("  → Starting from scratch...");

            Board board = new Board(config.getRows(), config.getCols());
            Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

            // Place fixed pieces
            for (PuzzleConfig.FixedPiece fp : config.getFixedPieces()) {
                Piece piece = config.getPieces().get(fp.pieceId);
                if (piece != null) {
                    board.place(fp.row, fp.col, piece, fp.rotation);
                    config.getPieces().remove(fp.pieceId);
                    System.out.println("  → Fixed piece " + fp.pieceId + " placed at [" + fp.row + "," + fp.col + "] rotation " + fp.rotation);
                }
            }

            // Solve
            EternitySolver.resetGlobalState();
            EternitySolver solver = new EternitySolver();
            solver.setDisplayConfig(config.isVerbose(), config.getMinDepthToShowRecords());
            String configId = ConfigurationUtils.extractConfigId(filepath);
            solver.setPuzzleName(configId);
            solver.setSortOrder(config.getSortOrder());

            System.out.println("  → Solving in progress...");
            System.out.println("  → Sort order: " + config.getSortOrder());
            System.out.println("  → Automatic save every 1 minute");
            System.out.println("  → Puzzle change every 10 minutes");

            boolean solved = solver.solve(board, allPieces);

            if (solved) {
                System.out.println("  → ✓ Solution found!");
                System.out.println();
                int totalPieces = config.getRows() * config.getCols();
                if (totalPieces <= 72) {
                    // Detailed display for small puzzles
                    displayDetailedSolution(board, allPieces);
                } else {
                    // Simple display for large puzzles
                    displaySolution(board);
                }
            } else {
                System.out.println("  → ✗ No solution found");
            }

            return solved;

        } catch (Exception e) {
            System.out.println("  → ✗ Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the timeout based on difficulty
     */
    private static long getTimeoutForDifficulty(String difficulty) {
        switch (difficulty.toLowerCase()) {
            case "facile":
                return TimeConstants.LONG_TIMEOUT_MS;  // 30 seconds
            case "moyen":
                return 2 * TimeConstants.MILLIS_PER_MINUTE; // 2 minutes
            case "difficile":
                return 5 * TimeConstants.MILLIS_PER_MINUTE; // 5 minutes
            case "extreme":
                return 30 * TimeConstants.MILLIS_PER_MINUTE; // 30 minutes
            default:
                return TimeConstants.MILLIS_PER_MINUTE;   // 1 minute by default
        }
    }

    // Removed: sortPiecesByOrder() - now using ConfigurationUtils.sortPiecesByOrder()

    /**
     * Displays the solution of a board (simple version, backward-compatible)
     */
    private static void displaySolution(Board board) {
        int rows = board.getRows();
        int cols = board.getCols();

        System.out.println("  Solution:");
        System.out.println("  ┌" + "─".repeat(cols * 4 + 1) + "┐");

        for (int r = 0; r < rows; r++) {
            System.out.print("  │");
            for (int c = 0; c < cols; c++) {
                Placement p = board.getPlacement(r, c);
                if (p != null) {
                    System.out.print(String.format(" %3d", p.getPieceId()));
                } else {
                    System.out.print("  · ");
                }
            }
            System.out.println(" │");
        }

        System.out.println("  └" + "─".repeat(cols * 4 + 1) + "┘");
    }

    /**
     * Displays the solution of a board (detailed version with edges)
     */
    private static void displayDetailedSolution(Board board, Map<Integer, Piece> allPieces) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        SOLUTION FOUND                            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Legend:");
        System.out.println("  - Each piece displays: Piece ID with edge values (N/E/S/W)");
        System.out.println("  - \033[32mGreen\033[0m: edges that match with neighbors");
        System.out.println("  - \033[91mRed\033[0m: edges that do NOT match (error!)");
        System.out.println();

        // Create a temporary solver to use its display method
        solver.EternitySolver tempSolver = new solver.EternitySolver();
        List<Integer> emptyList = new ArrayList<>();
        tempSolver.printBoardWithLabels(board, allPieces, emptyList);

        System.out.println();
        board.printScore();
        System.out.println();
        System.out.println("═".repeat(70));
    }

    /**
     * Displays the final report
     */
    private static void printFinalReport(List<PuzzleResult> results, long totalDuration) {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        FINAL REPORT                               ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");

        int solved = 0;
        int total = results.size();

        for (PuzzleResult result : results) {
            String status = result.solved ? "✓" : "✗";
            String name = String.format("%-35s", result.name);
            String pieces = String.format("%3d pieces", result.pieceCount);
            String time = String.format("%12s", FormattingUtils.formatDuration(result.duration));

            System.out.println("║ " + status + " " + name + " " + pieces + " " + time + " ║");

            if (result.solved) solved++;
        }

        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Solved: " + String.format("%-59s", solved + " / " + total) + " ║");
        System.out.println("║ Total time: " + String.format("%-54s", FormattingUtils.formatDuration(totalDuration)) + " ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    // Removed: formatDuration() - now using FormattingUtils.formatDuration()

    /**
     * Inner class to store results
     */
    private static class PuzzleResult {
        String name;
        String type;
        int pieceCount;
        boolean solved;
        long duration;

        PuzzleResult(String name, String type, int pieceCount, boolean solved, long duration) {
            this.name = name;
            this.type = type;
            this.pieceCount = pieceCount;
            this.solved = solved;
            this.duration = duration;
        }
    }
}
