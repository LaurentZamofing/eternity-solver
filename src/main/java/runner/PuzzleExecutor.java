package runner;

import model.Board;
import model.Piece;
import runner.PuzzleDefinition.HintPlacement;
import util.SaveManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Executes puzzle solving with proper setup, orchestration, and display.
 * Extracted from Main.java to separate puzzle execution logic from CLI.
 *
 * Responsibilities:
 * - Board setup with hints
 * - Solver configuration and execution
 * - Result display
 * - Save/load integration (for supported puzzles)
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class PuzzleExecutor {

    /**
     * Executes a puzzle from start to finish.
     *
     * @param puzzleDef Puzzle definition to execute
     */
    public void execute(PuzzleDefinition puzzleDef) {
        // Display header
        displayHeader(puzzleDef);

        // Load pieces
        System.out.println("Loading " + puzzleDef.getTotalPieces() + " pieces...");
        Map<Integer, Piece> allPieces = puzzleDef.loadPieces();
        System.out.println("✓ " + allPieces.size() + " pieces loaded\n");

        Board board;
        Map<Integer, Piece> piecesToPlace;

        // Handle save/load for supported puzzles
        if (puzzleDef.supportsSaveLoad() && SaveManager.hasSavedState()) {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              SAVE DETECTED                         ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            Object[] savedState = SaveManager.loadBestState(allPieces);
            if (savedState != null) {
                board = (Board) savedState[0];
                @SuppressWarnings("unchecked")
                Set<Integer> usedPieceIds = (Set<Integer>) savedState[1];
                int savedDepth = (int) savedState[2];

                // Create map of remaining pieces
                piecesToPlace = new HashMap<>(allPieces);
                for (int usedId : usedPieceIds) {
                    piecesToPlace.remove(usedId);
                }

                System.out.println("✓ State restored: " + savedDepth + " pieces placed");
                System.out.println("  Remaining pieces: " + piecesToPlace.size() + "\n");
            } else {
                System.out.println("✗ Loading error - starting new\n");
                board = new Board(puzzleDef.getRows(), puzzleDef.getCols());
                piecesToPlace = new HashMap<>(allPieces);
                setupHints(board, piecesToPlace, puzzleDef.getHints());
            }
        } else {
            // Normal puzzle start
            board = new Board(puzzleDef.getRows(), puzzleDef.getCols());
            piecesToPlace = new HashMap<>(allPieces);
            setupHints(board, piecesToPlace, puzzleDef.getHints());
        }

        // Configure and run solver
        PuzzleRunner.PuzzleRunnerConfig config = new PuzzleRunner.PuzzleRunnerConfig()
            .setVerbose(puzzleDef.isVerboseByDefault())
            .setParallel(puzzleDef.isParallelByDefault())
            .setUseSingletons(true);

        PuzzleRunner runner = new PuzzleRunner(board, piecesToPlace, config);

        System.out.println("Starting solver for " + puzzleDef.getDisplayName() + "...");
        PuzzleRunner.PuzzleResult result = runner.run();

        // Display result
        displayResult(result, puzzleDef);
    }

    /**
     * Displays puzzle header.
     */
    private void displayHeader(PuzzleDefinition puzzleDef) {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        String title = "  " + puzzleDef.getDisplayName();
        int padding = 56 - title.length();
        System.out.println("║" + title + " ".repeat(Math.max(0, padding)) + "  ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
    }

    /**
     * Sets up hint placements on the board.
     *
     * @param board Board to place hints on
     * @param pieces All available pieces
     * @param hints List of hint placements
     * @return Set of piece IDs that were used
     */
    private Set<Integer> setupHints(Board board, Map<Integer, Piece> pieces,
                                    List<HintPlacement> hints) {
        Set<Integer> usedPieces = new HashSet<>();

        if (hints.isEmpty()) {
            return usedPieces;
        }

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("PLACING HINTS");
        System.out.println("═══════════════════════════════════════════════════════");

        for (HintPlacement hint : hints) {
            Piece piece = pieces.get(hint.pieceId);
            if (piece != null) {
                board.place(hint.row, hint.col, piece, hint.rotation);
                usedPieces.add(hint.pieceId);
                pieces.remove(hint.pieceId);

                System.out.printf("  ✓ Piece %d (rotation %d) placed at %c%d%n",
                    hint.pieceId, hint.rotation, hint.getRowLabel(), hint.getColLabel());
            }
        }

        System.out.println();
        System.out.printf("  → %d pieces pre-placed, %d remaining pieces to place%n%n",
            usedPieces.size(), pieces.size());

        return usedPieces;
    }

    /**
     * Displays the puzzle result.
     */
    private void displayResult(PuzzleRunner.PuzzleResult result, PuzzleDefinition puzzleDef) {
        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("RESULT");
        System.out.println("═══════════════════════════════════════════════════════\n");

        if (result.isSolved()) {
            System.out.println("✓ PUZZLE SOLVED!");
            System.out.printf("Time: %.2f seconds%n%n", result.getDurationSeconds());

            // Display board
            result.getBoard().prettyPrint(puzzleDef.loadPieces());
        } else {
            System.out.println("✗ No solution found");
            System.out.printf("Time: %.2f seconds%n%n", result.getDurationSeconds());
        }

        // Display statistics if available
        if (result.getStatistics() != null) {
            result.getStatistics().print();
        }
    }
}
