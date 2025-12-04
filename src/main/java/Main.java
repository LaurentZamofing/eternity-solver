import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.BoardRenderer;
import util.ComparisonAnalyzer;
import util.ParallelConstants;
import util.PuzzleFactory;
import util.SaveManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Main entry point of the application.
 * Runs the solver on the predefined 3x3 example.
 *
 * TODO: REFACTORING NEEDED (Current: 538 lines, Target: ~100 lines)
 * See REFACTORING_ROADMAP.md for detailed plan.
 *
 * Remaining work:
 * 1. Extract all run* methods to ExamplePuzzles and LargePuzzleRunner utilities
 * 2. Use existing runner/PuzzleRunner.java for execution logic
 * 3. Simplify main() to simple dispatcher or delegation
 * 4. Remove deprecated BoardRenderer.printBoardWithCoordinates() and compareWithAndWithoutSingletons()
 *
 * Estimated effort: 4-6 hours
 * Priority: HIGH
 * See: REFACTORING_ROADMAP.md Section 1
 */
public class Main {

    public static void main(String[] args) {
        // CHOOSE THE PUZZLE TO RUN:

        // Puzzle 16x16 (rows A-P, columns 1-16)
        // runPuzzle16x16();

        // Puzzle 6x12 (rows A-F, columns 1-12)
        runPuzzle6x12();

        // Validation puzzle 6x6 (rows A-F, columns 1-6)
        // runValidation6x6();

        // Or uncomment to run the real Eternity II 16x16 puzzle
        // runEternityII();

        // Other available examples:
        // ComparisonAnalyzer.compareWithAndWithoutSingletons();  // 5x5
        // runExample4x4();
        // runExample4x4Easy();
        // runExample4x4Ordered();
    }

    /**
     * Runs the solver on the predefined 4x4 example (hard version).
     */
    private static void runExample4x4() {
        new runner.PuzzleExecutor().execute(runner.PuzzleDefinition.EXAMPLE_4X4_HARD);
    }

    /**
     * Runs the solver on the 4x4 example (easy version).
     */
    private static void runExample4x4Easy() {
        new runner.PuzzleExecutor().execute(runner.PuzzleDefinition.EXAMPLE_4X4_EASY);
    }

    /**
     * Runs the solver on the 4x4 example (ordered/trivial version).
     */
    private static void runExample4x4Ordered() {
        new runner.PuzzleExecutor().execute(runner.PuzzleDefinition.EXAMPLE_4X4_ORDERED);
    }

    /**
     * Runs the solver on the predefined 3x3 example.
     */
    private static void runExample3x3() {
        new runner.PuzzleExecutor().execute(runner.PuzzleDefinition.EXAMPLE_3X3);
    }

    /**
     * Runs the solver on the real Eternity II puzzle (16x16, 256 pieces).
     */
    private static void runEternityII() {
        int rows = 16, cols = 16;

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              ETERNITY II - PUZZLE 16x16                  ║");
        System.out.println("║                  256 pieces - 22 patterns                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        System.out.println("Loading the 256 official pieces...");
        Map<Integer, Piece> allPieces = PuzzleFactory.createEternityII();
        System.out.println("✓ " + allPieces.size() + " pieces loaded\n");

        Board board;
        Map<Integer, Piece> pieces;

        // Check if a save exists
        if (SaveManager.hasSavedState()) {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              SAVE DETECTED                         ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            Object[] savedState = SaveManager.loadBestState(allPieces);
            if (savedState != null) {
                board = (Board) savedState[0];
                @SuppressWarnings("unchecked")
                Set<Integer> usedPieceIds = (Set<Integer>) savedState[1];
                int savedDepth = (int) savedState[2];
                int savedThread = (int) savedState[3];

                // Create the map of remaining pieces
                pieces = new java.util.HashMap<>(allPieces);
                for (int usedId : usedPieceIds) {
                    pieces.remove(usedId);
                }

                System.out.println("✓ State restored: " + savedDepth + " pieces placed");
                System.out.println("  Remaining pieces: " + pieces.size() + "\n");
            } else {
                System.out.println("✗ Loading error - starting new\n");
                board = new Board(rows, cols);
                pieces = new java.util.HashMap<>(allPieces);

                // Pre-place the mandatory hint piece (piece 139)
                System.out.println("╔══════════════════════════════════════════════════════════╗");
                System.out.println("║           PRE-PLACING THE HINT PIECE            ║");
                System.out.println("╚══════════════════════════════════════════════════════════╝\n");

                int hintPieceId = 139;
                Piece hintPiece = pieces.get(hintPieceId);
                if (hintPiece != null) {
                    board.place(8, 7, hintPiece, 3);
                    pieces.remove(hintPieceId);
                    System.out.println("✓ Piece " + hintPieceId + " placed at (8, 7) with rotation 3");
                    System.out.println("  Remaining pieces: " + pieces.size() + "\n");
                }
            }
        } else {
            board = new Board(rows, cols);
            pieces = new java.util.HashMap<>(allPieces);

            // Pre-place the mandatory hint piece (piece 139)
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║           PRE-PLACING THE HINT PIECE            ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            int hintPieceId = 139;
            int hintRow = 8;
            int hintCol = 7;
            int hintRotation = 3;

            Piece hintPiece = pieces.get(hintPieceId);
            if (hintPiece == null) {
                System.out.println("✗ ERROR: Hint piece " + hintPieceId + " not found!");
                return;
            }

            board.place(hintRow, hintCol, hintPiece, hintRotation);
            pieces.remove(hintPieceId);

            System.out.println("✓ Piece " + hintPieceId + " placed at (" + hintRow + ", " + hintCol + ") with rotation " + hintRotation);
            System.out.println("  Edges: " + java.util.Arrays.toString(hintPiece.getEdges()));
            System.out.println("  Remaining pieces: " + pieces.size() + "\n");
        }

        EternitySolver solver = new EternitySolver();
        solver.setUseSingletons(true);
        solver.setVerbose(false); // Disable detailed display

        // Determine the number of threads (use optimal thread count based on available cores)
        int numCores = Runtime.getRuntime().availableProcessors();
        int numThreads = ParallelConstants.getOptimalThreadCount();

        System.out.println("Starting solver with optimizations...");
        System.out.println("- MRV heuristic (Minimum Remaining Values)");
        System.out.println("- Singleton detection (forced moves)");
        System.out.println("- Dead-end detection");
        System.out.println("- Anti-thrashing randomization");
        System.out.println("- Parallel search (" + numThreads + " threads on " + numCores + " cores)");
        System.out.println("- Mandatory hint piece pre-placed\n");

        System.out.println("⚠ WARNING: This puzzle has never been solved!");
        System.out.println("The solver will explore the search space...\n");

        // Use parallel search
        // IMPORTANT: pass allPieces (all 256 pieces) to be able to reconstruct the board
        // and pieces (remaining pieces) for the search
        boolean solved = solver.solveParallel(board, allPieces, pieces, numThreads);

        if (!solved) {
            System.out.println("\n⚠ No solution found (or timeout)");
        } else {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🎉 SOLUTION FOUND! 🎉");
            System.out.println("=".repeat(60) + "\n");
            board.prettyPrint(pieces);
        }

        // Display final statistics
        solver.getStatistics().print();
    }

    /**
     * Runs the 16x16 puzzle.
     */
    private static void runPuzzle16x16() {
        new runner.PuzzleExecutor().execute(runner.PuzzleDefinition.PUZZLE_16X16);
    }

    /**
     * Runs the 6x12 puzzle.
     */
    private static void runPuzzle6x12() {
        new runner.PuzzleExecutor().execute(runner.PuzzleDefinition.PUZZLE_6X12);
    }

    /**
     * Runs the validation puzzle 6x6.
     */
    private static void runValidation6x6() {
        new runner.PuzzleExecutor().execute(runner.PuzzleDefinition.VALIDATION_6X6);
    }

}
