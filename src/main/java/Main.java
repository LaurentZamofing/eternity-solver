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
        new runner.PuzzleExecutor().execute(runner.PuzzleDefinition.ETERNITY_II);
    }

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
