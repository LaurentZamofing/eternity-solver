package runner;

import model.Piece;
import util.PuzzleFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Enumeration of all available puzzles with their metadata and configuration.
 * Centralizes puzzle definitions that were previously scattered in Main.java.
 *
 * Each puzzle knows how to:
 * - Load its pieces
 * - Provide dimensions
 * - Configure solver defaults
 * - Define hint placements (if any)
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public enum PuzzleDefinition {

    /**
     * Simple 3x3 example puzzle (9 pieces).
     */
    EXAMPLE_3X3("3x3 Example", 3, 3, false),

    /**
     * 4x4 example puzzle - hard version (16 pieces).
     */
    EXAMPLE_4X4_HARD("4x4 Example (HARD)", 4, 4, false),

    /**
     * 4x4 example puzzle - easy version (16 pieces).
     */
    EXAMPLE_4X4_EASY("4x4 Example (EASY)", 4, 4, false),

    /**
     * 4x4 example puzzle - ordered/trivial version (16 pieces).
     */
    EXAMPLE_4X4_ORDERED("4x4 Example (TRIVIAL - ordered)", 4, 4, false),

    /**
     * 5x5 example for singleton comparison (25 pieces).
     */
    EXAMPLE_5X5("5x5 Example", 5, 5, false),

    /**
     * Validation puzzle 6x6 (36 pieces).
     */
    VALIDATION_6X6("Validation Test - Puzzle 6×6", 6, 6, false),

    /**
     * Medium puzzle 6x12 (72 pieces).
     */
    PUZZLE_6X12("Puzzle 6×12", 6, 12, false),

    /**
     * Large puzzle 16x16 (256 pieces).
     */
    PUZZLE_16X16("Puzzle 16×16", 16, 16, true),

    /**
     * Eternity II - The real challenge (16x16, 256 pieces).
     */
    ETERNITY_II("ETERNITY II - PUZZLE 16x16", 16, 16, true);

    // ========== Fields ==========

    private final String displayName;
    private final int rows;
    private final int cols;
    private final boolean hasHints;

    // ========== Constructor ==========

    PuzzleDefinition(String displayName, int rows, int cols, boolean hasHints) {
        this.displayName = displayName;
        this.rows = rows;
        this.cols = cols;
        this.hasHints = hasHints;
    }

    // ========== Getters ==========

    /**
     * Gets the display name of the puzzle.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the number of rows.
     */
    public int getRows() {
        return rows;
    }

    /**
     * Gets the number of columns.
     */
    public int getCols() {
        return cols;
    }

    /**
     * Checks if this puzzle has predefined hints.
     */
    public boolean hasHints() {
        return hasHints;
    }

    /**
     * Gets the total number of pieces.
     */
    public int getTotalPieces() {
        return rows * cols;
    }

    // ========== Piece Loading ==========

    /**
     * Loads the pieces for this puzzle.
     *
     * @return Map of pieces indexed by ID
     */
    public Map<Integer, Piece> loadPieces() {
        return switch (this) {
            case EXAMPLE_3X3 -> PuzzleFactory.createExample3x3();
            case EXAMPLE_4X4_HARD -> PuzzleFactory.createExample4x4();
            case EXAMPLE_4X4_EASY -> PuzzleFactory.createExample4x4Easy();
            case EXAMPLE_4X4_ORDERED -> PuzzleFactory.createExample4x4Ordered();
            case EXAMPLE_5X5 -> PuzzleFactory.createExample5x5();
            case VALIDATION_6X6 -> PuzzleFactory.createValidation6x6();
            case PUZZLE_6X12 -> PuzzleFactory.createPuzzle6x12();
            case PUZZLE_16X16 -> PuzzleFactory.createPuzzle16x16();
            case ETERNITY_II -> PuzzleFactory.createEternityII();
        };
    }

    // ========== Hints/Clues ==========

    /**
     * Gets the hint placements for this puzzle.
     * Returns empty list if no hints.
     *
     * @return List of hint placements
     */
    public List<HintPlacement> getHints() {
        List<HintPlacement> hints = new ArrayList<>();

        switch (this) {
            case ETERNITY_II:
                // Mandatory hint piece for Eternity II
                hints.add(new HintPlacement(8, 7, 139, 3));
                break;

            case PUZZLE_16X16:
                // Hints can be added here if needed
                // Currently disabled for testing
                break;

            default:
                // No hints for simple puzzles
                break;
        }

        return hints;
    }

    // ========== Configuration Hints ==========

    /**
     * Checks if this puzzle should use verbose output by default.
     */
    public boolean isVerboseByDefault() {
        return getTotalPieces() <= 25; // Small puzzles show details
    }

    /**
     * Checks if this puzzle should use parallel solving by default.
     */
    public boolean isParallelByDefault() {
        return getTotalPieces() > 100; // Large puzzles benefit from parallelization
    }

    /**
     * Checks if this puzzle supports save/load functionality.
     */
    public boolean supportsSaveLoad() {
        return this == ETERNITY_II; // Only Eternity II for now
    }

    // ========== Display Helpers ==========

    /**
     * Gets the coordinate labels for rows (A, B, C, ...).
     */
    public List<String> getRowLabels() {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            labels.add(String.valueOf((char) ('A' + i)));
        }
        return labels;
    }

    /**
     * Gets the coordinate labels for columns (1, 2, 3, ...).
     */
    public List<String> getColLabels() {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < cols; i++) {
            labels.add(String.valueOf(i + 1));
        }
        return labels;
    }

    // ========== Inner Classes ==========

    /**
     * Represents a hint/clue piece placement.
     */
    public static class HintPlacement {
        public final int row;
        public final int col;
        public final int pieceId;
        public final int rotation;

        public HintPlacement(int row, int col, int pieceId, int rotation) {
            this.row = row;
            this.col = col;
            this.pieceId = pieceId;
            this.rotation = rotation;
        }

        public char getRowLabel() {
            return (char) ('A' + row);
        }

        public int getColLabel() {
            return col + 1;
        }
    }
}
