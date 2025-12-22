package app;

/**
 * Main entry point for the Eternity Puzzle Solver.
 *
 * This class serves as a simple dispatcher to various puzzle configurations.
 * All puzzle definitions and execution logic are delegated to:
 * - runner.PuzzleDefinition: Enum of all available puzzles
 * - runner.PuzzleExecutor: Execution engine for puzzle solving
 *
 * REFACTORING STATUS: ✓ COMPLETE (110 lines, target: ~100 lines)
 * Previously: 697L → Reduced by 84% in commit 3377560
 *
 * To run a different puzzle, uncomment the desired line in main().
 */
public class Main {

    public static void main(String[] args) {
        // ═══════════════════════════════════════════════════════════
        // CHOOSE THE PUZZLE TO RUN (uncomment one line):
        // ═══════════════════════════════════════════════════════════

        // Puzzle 6x12 (rows A-F, columns 1-12) - Currently active
        runPuzzle6x12();

        // Puzzle 16x16 (rows A-P, columns 1-16)
        // runPuzzle16x16();

        // Validation puzzle 6x6 (rows A-F, columns 1-6)
        // runValidation6x6();

        // Real Eternity II 16x16 puzzle (256 pieces)
        // runEternityII();

        // Small examples for testing:
        // runExample4x4();      // Hard version
        // runExample4x4Easy();  // Easy version
        // runExample4x4Ordered(); // Trivial/ordered version
        // runExample3x3();      // 3x3 example
    }

    // ═══════════════════════════════════════════════════════════
    // PUZZLE EXECUTION METHODS - Simple delegates to PuzzleExecutor
    // ═══════════════════════════════════════════════════════════

    private static void runExample4x4() {
        execute(runner.PuzzleDefinition.EXAMPLE_4X4_HARD);
    }

    private static void runExample4x4Easy() {
        execute(runner.PuzzleDefinition.EXAMPLE_4X4_EASY);
    }

    private static void runExample4x4Ordered() {
        execute(runner.PuzzleDefinition.EXAMPLE_4X4_ORDERED);
    }

    private static void runExample3x3() {
        execute(runner.PuzzleDefinition.EXAMPLE_3X3);
    }

    private static void runEternityII() {
        execute(runner.PuzzleDefinition.ETERNITY_II);
    }

    private static void runPuzzle16x16() {
        execute(runner.PuzzleDefinition.PUZZLE_16X16);
    }

    private static void runPuzzle6x12() {
        execute(runner.PuzzleDefinition.PUZZLE_6X12);
    }

    private static void runValidation6x6() {
        execute(runner.PuzzleDefinition.VALIDATION_6X6);
    }

    /** Execute a puzzle definition using PuzzleExecutor. */
    private static void execute(runner.PuzzleDefinition definition) {
        new runner.PuzzleExecutor().execute(definition);
    }
}
