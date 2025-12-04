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
 * 4. Remove deprecated printBoardWithCoordinates() and compareWithAndWithoutSingletons()
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
        // compareWithAndWithoutSingletons();  // 5x5
        // runExample4x4();
        // runExample4x4Easy();
        // runExample4x4Ordered();
    }

    /**
     * Compares performance with and without singleton optimization.
     *
     * @deprecated Use {@link ComparisonAnalyzer#compareWithAndWithoutSingletons()} instead.
     */
    @Deprecated
    private static void compareWithAndWithoutSingletons() {
        ComparisonAnalyzer.compareWithAndWithoutSingletons();
    }

    /**
     * Runs the solver on the predefined 4x4 example (hard version).
     */
    private static void runExample4x4() {
        int rows = 4, cols = 4;
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4();
        Board board = new Board(rows, cols);
        EternitySolver solver = new EternitySolver();

        System.out.println("Starting solver for 4x4 example (HARD)...");
        boolean solved = solver.solve(board, pieces);

        if (!solved) {
            System.out.println("No solution found for 4x4 example.");
        } else {
            System.out.println("Solution found:\n");
            board.prettyPrint(pieces);
        }
    }

    /**
     * Runs the solver on the 4x4 example (easy version).
     */
    private static void runExample4x4Easy() {
        int rows = 4, cols = 4;
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4Easy();
        Board board = new Board(rows, cols);
        EternitySolver solver = new EternitySolver();

        System.out.println("Starting solver for 4x4 example (EASY)...");
        boolean solved = solver.solve(board, pieces);

        if (!solved) {
            System.out.println("No solution found for 4x4 example.");
        } else {
            System.out.println("Solution found:\n");
            board.prettyPrint(pieces);
        }
    }

    /**
     * Runs the solver on the 4x4 example (ordered/trivial version).
     */
    private static void runExample4x4Ordered() {
        int rows = 4, cols = 4;
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4Ordered();
        Board board = new Board(rows, cols);
        EternitySolver solver = new EternitySolver();

        System.out.println("Starting solver for 4x4 example (TRIVIAL - ordered)...");
        boolean solved = solver.solve(board, pieces);

        if (!solved) {
            System.out.println("No solution found for 4x4 example.");
        } else {
            System.out.println("Solution found:\n");
            board.prettyPrint(pieces);
        }
    }

    /**
     * Runs the solver on the predefined 3x3 example.
     */
    private static void runExample3x3() {
        int rows = 3, cols = 3;
        Map<Integer, Piece> pieces = PuzzleFactory.createExample3x3();
        Board board = new Board(rows, cols);
        EternitySolver solver = new EternitySolver();

        System.out.println("Starting solver for 3x3 example...");
        boolean solved = solver.solve(board, pieces);

        if (!solved) {
            System.out.println("No solution found for 3x3 example.");
        } else {
            System.out.println("Solution found:\n");
            board.prettyPrint(pieces);
        }
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
     * Rows: A-P (0-15)
     * Columns: 1-16 (0-15)
     */
    private static void runPuzzle16x16() {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║             PUZZLE 16×16 (256 pieces)                  ║");
        System.out.println("║        Rows: A-P / Columns: 1-16                    ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        // Load the pieces
        Map<Integer, Piece> pieces = PuzzleFactory.createPuzzle16x16();
        System.out.println("✓ " + pieces.size() + " pieces loaded\n");

        // Display the pieces
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("PUZZLE PIECES");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("ID  | N  E  S  W");
        System.out.println("────┼────────────");
        for (Map.Entry<Integer, Piece> entry : pieces.entrySet()) {
            int id = entry.getKey();
            int[] edges = entry.getValue().getEdges();
            System.out.printf("%3d | %2d %2d %2d %2d%n",
                id, edges[0], edges[1], edges[2], edges[3]);
        }
        System.out.println();

        // Create the board
        Board board = new Board(16, 16);

        // TEST: Disable hints to test if the solver works with the new data
        // Format: row (0-indexed), column (0-indexed), piece, rotation
        // Source: https://github.com/TheSil/edge_puzzle/blob/main/data/eternity2/eternity2_256_all_hints.csv
        int[][] clues = {
            // {8, 7, 139, 3},   // I8
            // {13, 2, 181, 0},  // N3
            // {2, 13, 255, 0},  // C14
            // {2, 2, 208, 0},   // C3
            // {13, 13, 249, 1}  // N14
        };

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("PLACING HINTS");
        System.out.println("═══════════════════════════════════════════════════════");

        Set<Integer> usedPieceIds = new HashSet<>();
        for (int[] clue : clues) {
            int row = clue[0];    // CSV is already 0-indexed
            int col = clue[1];    // CSV is already 0-indexed
            int pieceId = clue[2];
            int rotation = clue[3];

            Piece piece = pieces.get(pieceId);
            if (piece != null) {
                board.place(row, col, piece, rotation);
                usedPieceIds.add(pieceId);
                char rowLabel = (char) ('A' + row);
                System.out.printf("  ✓ Piece %d (rotation %d) placed at %c%d%n",
                    pieceId, rotation, rowLabel, col + 1);
            }
        }
        System.out.println();
        System.out.printf("  → %d pieces pre-placed, %d remaining pieces to place%n%n",
            usedPieceIds.size(), pieces.size() - usedPieceIds.size());

        // Display the board with hints placed
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("BOARD WITH HINTS (16×16)");
        System.out.println("═══════════════════════════════════════════════════════");
        printBoardWithCoordinates(board, pieces);

        // Display the maximum score
        int[] score = board.calculateScore();
        System.out.println("Theoretical maximum score:");
        System.out.println("  - Horizontal internal edges: " + ((16-1) * 16) + " (15 × 16)");
        System.out.println("  - Vertical internal edges: " + (16 * (16-1)) + " (16 × 15)");
        System.out.println("  - Total: " + score[1] + " internal edges");
        System.out.println();

        // Create a copy of the pieces map without the pre-placed pieces
        Map<Integer, Piece> remainingPieces = new HashMap<>(pieces);
        for (int usedId : usedPieceIds) {
            remainingPieces.remove(usedId);
        }

        // Run the solver
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("STARTING THE SOLVER");
        System.out.println("═══════════════════════════════════════════════════════\n");
        System.out.println("Note: The solver may take several hours/days");
        System.out.println("for a 16×16 puzzle with 251 pieces to place.\n");

        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false); // Disable verbose, only display records
        long startTime = System.currentTimeMillis();

        boolean solved = solver.solve(board, remainingPieces);

        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;

        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("RESULT");
        System.out.println("═══════════════════════════════════════════════════════");

        if (solved) {
            System.out.println("✓ PUZZLE SOLVED!");
            System.out.println("Time: " + String.format("%.2f", duration) + " seconds\n");

            printBoardWithCoordinates(board, pieces);
            board.printScore();
        } else {
            System.out.println("✗ No solution found");
            System.out.println("Time: " + String.format("%.2f", duration) + " seconds\n");
        }
    }

    /**
     * Runs the 6x12 puzzle.
     * Rows: A-F (0-5)
     * Columns: 1-12 (0-11)
     */
    private static void runPuzzle6x12() {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║              PUZZLE 6×12 (72 pieces)                   ║");
        System.out.println("║        Rows: A-F / Columns: 1-12                    ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        // Load the pieces
        Map<Integer, Piece> pieces = PuzzleFactory.createPuzzle6x12();
        System.out.println("✓ " + pieces.size() + " pieces loaded\n");

        // Display the pieces
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("PUZZLE PIECES");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("ID  | N  E  S  W");
        System.out.println("────┼────────────");
        for (Map.Entry<Integer, Piece> entry : pieces.entrySet()) {
            int id = entry.getKey();
            int[] edges = entry.getValue().getEdges();
            System.out.printf("%2d  | %2d %2d %2d %2d%n",
                id, edges[0], edges[1], edges[2], edges[3]);
        }
        System.out.println();

        // Create the board
        Board board = new Board(6, 12);

        // Display the empty board with coordinates A-F and 1-12
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("EMPTY BOARD (6×12)");
        System.out.println("═══════════════════════════════════════════════════════");
        printBoardWithCoordinates(board, pieces);

        // Display the maximum score
        int[] score = board.calculateScore();
        System.out.println("Theoretical maximum score:");
        System.out.println("  - Horizontal internal edges: " + ((6-1) * 12) + " (5 × 12)");
        System.out.println("  - Vertical internal edges: " + (6 * (12-1)) + " (6 × 11)");
        System.out.println("  - Total: " + score[1] + " internal edges");
        System.out.println();

        // Lancer le solver
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("LANCEMENT DU SOLVER");
        System.out.println("═══════════════════════════════════════════════════════\n");

        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false); // N'afficher que les records, pas chaque placement
        long startTime = System.currentTimeMillis();

        boolean solved = solver.solve(board, pieces);

        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;

        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("RÉSULTAT");
        System.out.println("═══════════════════════════════════════════════════════");

        if (solved) {
            System.out.println("✓ PUZZLE RÉSOLU!");
            System.out.println("Temps: " + String.format("%.2f", duration) + " secondes\n");

            printBoardWithCoordinates(board, pieces);
            board.printScore();
        } else {
            System.out.println("✗ No solution found");
            System.out.println("Temps: " + String.format("%.2f", duration) + " secondes\n");
        }
    }

    /**
     * Runs the validation puzzle 6x6.
     * Rows: A-F (0-5)
     * Columns: 1-6 (0-5)
     */
    private static void runValidation6x6() {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║        VALIDATION TEST - PUZZLE 6×6                 ║");
        System.out.println("║        Rows: A-F / Columns: 1-6                     ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        // Load the pieces
        Map<Integer, Piece> pieces = PuzzleFactory.createValidation6x6();
        System.out.println("✓ " + pieces.size() + " pieces loaded\n");

        // Display the pieces
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("PUZZLE PIECES");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("ID  | N  E  S  W");
        System.out.println("────┼────────────");
        for (Map.Entry<Integer, Piece> entry : pieces.entrySet()) {
            int id = entry.getKey();
            int[] edges = entry.getValue().getEdges();
            System.out.printf("%2d  | %d  %d  %d  %d%n",
                id, edges[0], edges[1], edges[2], edges[3]);
        }
        System.out.println();

        // Create the board
        Board board = new Board(6, 6);

        // Display the empty board with coordinates A-F and 1-6
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("EMPTY BOARD (6×6)");
        System.out.println("═══════════════════════════════════════════════════════");
        printBoardWithCoordinates(board, pieces);

        // Display the maximum score
        int[] score = board.calculateScore();
        System.out.println("Theoretical maximum score:");
        System.out.println("  - Horizontal internal edges: " + ((6-1) * 6) + " (5 × 6)");
        System.out.println("  - Vertical internal edges: " + (6 * (6-1)) + " (6 × 5)");
        System.out.println("  - Total: " + score[1] + " internal edges");
        System.out.println();

        // Lancer le solver
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("LANCEMENT DU SOLVER");
        System.out.println("═══════════════════════════════════════════════════════\n");

        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false); // N'afficher que les records, pas chaque placement
        long startTime = System.currentTimeMillis();

        boolean solved = solver.solve(board, pieces);

        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;

        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("RÉSULTAT");
        System.out.println("═══════════════════════════════════════════════════════");

        if (solved) {
            System.out.println("✓ PUZZLE RÉSOLU!");
            System.out.println("Temps: " + String.format("%.2f", duration) + " secondes\n");

            printBoardWithCoordinates(board, pieces);
            board.printScore();
        } else {
            System.out.println("✗ No solution found");
            System.out.println("Temps: " + String.format("%.2f", duration) + " secondes\n");
        }
    }

    /**
     * Displays the board with coordinates A-F (rows) and 1-12 (columns).
     * Each cell displays the piece number in the center with edge values around it.
     *
     * @deprecated Use {@link BoardRenderer#printBoardWithCoordinates(Board, Map)} instead.
     */
    @Deprecated
    private static void printBoardWithCoordinates(Board board, Map<Integer, Piece> pieces) {
        BoardRenderer.printBoardWithCoordinates(board, pieces);
    }
}
