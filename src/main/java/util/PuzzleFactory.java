package util;

import model.Piece;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Factory for creating puzzle instances from data files.
 * All puzzle data is externalized to data/puzzles/ directory.
 *
 * Refactored in Sprint 8 to eliminate 865 lines of hardcoded constants.
 *
 * @author Eternity Solver Team
 * @version 2.0.0
 */
public class PuzzleFactory {

    private static final String PUZZLES_DIR = "data/puzzles/";

    // ==================== PUBLIC FACTORY METHODS ====================

    /**
     * Creates the 3x3 example puzzle (9 pieces).
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> createExample3x3() {
        return loadPuzzleFile("example_3x3.txt");
    }

    /**
     * Creates the 4x4 example puzzle - default hard variant (16 pieces).
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> createExample4x4() {
        return createExample4x4HardV3();
    }

    /**
     * Creates the 4x4 puzzle - hard variant 1 (16 pieces).
     * Centers grouped first for maximum difficulty.
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> createExample4x4HardV1() {
        return loadPuzzleFile("example_4x4_hard_v1.txt");
    }

    /**
     * Creates the 4x4 puzzle - hard variant 2 (16 pieces).
     * Alternating piece types for complexity.
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> createExample4x4HardV2() {
        return loadPuzzleFile("example_4x4_hard_v2.txt");
    }

    /**
     * Creates the 4x4 puzzle - hard variant 3 (16 pieces).
     * Similar pieces grouped for confusion.
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> createExample4x4HardV3() {
        return loadPuzzleFile("example_4x4_hard_v3.txt");
    }

    /**
     * Creates the 4x4 puzzle - easy variant (16 pieces).
     * Pieces ordered for simpler solving.
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> createExample4x4Easy() {
        return loadPuzzleFile("example_4x4_easy.txt");
    }

    /**
     * Creates the 4x4 puzzle - ordered variant (16 pieces).
     * Pieces in final position, only rotations need to be found.
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> createExample4x4Ordered() {
        return loadPuzzleFile("example_4x4_ordered.txt");
    }

    /**
     * Creates the 5x5 example puzzle - default hard variant (25 pieces).
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> createExample5x5() {
        return createExample5x5HardV1();
    }

    /**
     * Creates the 5x5 puzzle - ordered variant (25 pieces).
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> createExample5x5Ordered() {
        return loadPuzzleFile("example_5x5_ordered.txt");
    }

    /**
     * Creates the 5x5 puzzle - hard variant 1 (25 pieces).
     * Uses only 4 patterns for maximum confusion.
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> createExample5x5HardV1() {
        return loadPuzzleFile("example_5x5_hard_v1.txt");
    }

    /**
     * Creates the validation 6x6 puzzle (36 pieces).
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> createValidation6x6() {
        return loadPuzzleFile("validation_6x6.txt");
    }

    /**
     * Creates the 6x12 puzzle (72 pieces).
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> createPuzzle6x12() {
        return loadPuzzleFile("puzzle_6x12.txt");
    }

    /**
     * Creates the 16x16 puzzle (256 pieces).
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> createPuzzle16x16() {
        return loadPuzzleFile("puzzle_16x16.txt");
    }

    /**
     * Loads the real Eternity II puzzle (16x16, 256 pieces).
     * Uses special format: "north south west east" (no ID in file).
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> createEternityII() {
        return loadEternityIIFromFile("data/archives/eternity2_256_pieces.txt");
    }

    // ==================== FILE LOADING METHODS ====================

    /**
     * Loads a puzzle from the standard puzzle directory.
     * Format: "id north east south west" (one piece per line, # for comments).
     *
     * @param filename Name of the puzzle file (without path)
     * @return map of pieces indexed by ID
     * @throws RuntimeException if file cannot be read or is malformed
     */
    private static Map<Integer, Piece> loadPuzzleFile(String filename) {
        return loadFromFile(PUZZLES_DIR + filename);
    }

    /**
     * Loads a puzzle from a file using standard format.
     * Format: "id north east south west" (one piece per line, # for comments).
     *
     * @param filepath Full path to the puzzle file
     * @return map of pieces indexed by ID
     * @throws RuntimeException if file cannot be read or is malformed
     */
    public static Map<Integer, Piece> loadFromFile(String filepath) {
        Map<Integer, Piece> pieces = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length != 5) {
                    throw new IllegalArgumentException(
                        String.format("Invalid line format at line %d (expected 5 values: id N E S W): %s",
                                    lineNumber, line)
                    );
                }

                int id = Integer.parseInt(parts[0]);
                int north = Integer.parseInt(parts[1]);
                int east = Integer.parseInt(parts[2]);
                int south = Integer.parseInt(parts[3]);
                int west = Integer.parseInt(parts[4]);

                int[] edges = new int[]{north, east, south, west};
                pieces.put(id, new Piece(id, edges));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading puzzle file '" + filepath + "': " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number format in puzzle file '" + filepath + "': " + e.getMessage(), e);
        }
        return pieces;
    }

    /**
     * Loads Eternity II pieces from a file using special format.
     * Format: "north south west east" (4 values per line, no ID).
     * Piece IDs are auto-assigned starting from 1.
     *
     * @param filepath Path to the Eternity II puzzle file
     * @return map of pieces indexed by auto-generated ID
     * @throws RuntimeException if file cannot be read or is malformed
     */
    public static Map<Integer, Piece> loadEternityIIFromFile(String filepath) {
        Map<Integer, Piece> pieces = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            int id = 1;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length != 4) {
                    throw new IllegalArgumentException(
                        String.format("Invalid line format at line %d (expected 4 values: N S W E): %s",
                                    lineNumber, line)
                    );
                }

                // File format: N, S, W, E
                // Internal format: N, E, S, W
                int north = Integer.parseInt(parts[0]);
                int south = Integer.parseInt(parts[1]);
                int west = Integer.parseInt(parts[2]);
                int east = Integer.parseInt(parts[3]);

                int[] edges = new int[]{north, east, south, west};
                pieces.put(id, new Piece(id, edges));
                id++;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading Eternity II file '" + filepath + "': " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number format in Eternity II file '" + filepath + "': " + e.getMessage(), e);
        }
        return pieces;
    }

    /**
     * Builds pieces from a 2D array definition.
     * Format: int[]{id, north, east, south, west} for each piece.
     *
     * @param defs Array of piece definitions
     * @return map of pieces indexed by ID
     */
    public static Map<Integer, Piece> buildPiecesFromDefs(int[][] defs) {
        Map<Integer, Piece> pieces = new LinkedHashMap<>();
        for (int[] def : defs) {
            if (def.length != 5) {
                throw new IllegalArgumentException("Each piece definition must have 5 values: id, N, E, S, W");
            }
            int id = def[0];
            int[] edges = new int[]{def[1], def[2], def[3], def[4]};
            pieces.put(id, new Piece(id, edges));
        }
        return pieces;
    }
}
