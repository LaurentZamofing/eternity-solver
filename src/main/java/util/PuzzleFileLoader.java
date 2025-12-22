package util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Loader for loading puzzles from text files.
 *
 * Expected file format:
 * <pre>
 * # Comments (lines starting with #)
 * id north east south west
 * 1 0 1 2 0
 * 2 0 2 3 1
 * ...
 * </pre>
 *
 * <p>This loader allows progressively moving puzzle data
 * from hardcoded arrays in PuzzleFactory to external files,
 * facilitating maintenance and addition of new puzzles.</p>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 * @see PuzzleFactory
 */
public class PuzzleFileLoader {

    /** Default directory for puzzle files */
    private static final String DEFAULT_PUZZLE_DIR = "data/puzzles/";

    /** Default file extension */
    private static final String DEFAULT_EXTENSION = ".txt";

    /**
     * Loads a puzzle from a file.
     *
     * @param puzzleName Puzzle name (without extension, e.g., "example_3x3")
     * @return Array of pieces in format {id, north, east, south, west}
     * @throws IOException If the file cannot be read
     * @throws IllegalArgumentException If the format is invalid
     */
    public static int[][] loadPuzzle(String puzzleName) throws IOException {
        String filename = DEFAULT_PUZZLE_DIR + puzzleName + DEFAULT_EXTENSION;
        return loadPuzzleFromFile(filename);
    }

    /**
     * Loads a puzzle from a specific file path.
     *
     * @param filepath Complete path to the file
     * @return Array of pieces in format {id, north, east, south, west}
     * @throws IOException If the file cannot be read
     * @throws IllegalArgumentException If the format is invalid
     */
    public static int[][] loadPuzzleFromFile(String filepath) throws IOException {
        Path path = Paths.get(filepath);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("Puzzle file not found: " + filepath);
        }

        List<int[]> pieces = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Ignore empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parse the line
                try {
                    int[] piece = parsePieceLine(line);
                    pieces.add(piece);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    throw new IllegalArgumentException(
                        "Invalid format at line " + lineNumber + " in " + filepath + ": " + line, e);
                }
            }
        }

        if (pieces.isEmpty()) {
            throw new IllegalArgumentException("No pieces found in file: " + filepath);
        }

        // Convert List<int[]> to int[][]
        return pieces.toArray(new int[0][]);
    }

    /**
     * Parses a piece definition line.
     *
     * Expected format: "id north east south west"
     * Example: "1 0 1 2 0"
     *
     * @param line Line to parse
     * @return Array [id, north, east, south, west]
     * @throws NumberFormatException If values are not integers
     * @throws ArrayIndexOutOfBoundsException If the number of values is incorrect
     */
    private static int[] parsePieceLine(String line) {
        String[] parts = line.split("\\s+");

        if (parts.length != 5) {
            throw new ArrayIndexOutOfBoundsException(
                "Expected 5 values (id north east south west), got " + parts.length);
        }

        int[] piece = new int[5];
        for (int i = 0; i < 5; i++) {
            piece[i] = Integer.parseInt(parts[i]);
        }

        return piece;
    }

    /**
     * Checks if a puzzle file exists.
     *
     * @param puzzleName Puzzle name (without extension)
     * @return true if the file exists, false otherwise
     */
    public static boolean puzzleFileExists(String puzzleName) {
        String filename = DEFAULT_PUZZLE_DIR + puzzleName + DEFAULT_EXTENSION;
        return Files.exists(Paths.get(filename));
    }

    /**
     * Saves a puzzle to a file.
     *
     * Useful for externalizing existing hardcoded puzzles.
     *
     * @param puzzleName Puzzle name (without extension)
     * @param pieces Array of pieces in format {id, north, east, south, west}
     * @param comment Optional comment to add at the beginning of file
     * @throws IOException If writing fails
     */
    public static void savePuzzleToFile(String puzzleName, int[][] pieces, String comment)
            throws IOException {
        String filename = DEFAULT_PUZZLE_DIR + puzzleName + DEFAULT_EXTENSION;
        Path dir = Paths.get(DEFAULT_PUZZLE_DIR);

        // Create directory if necessary
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Header
            writer.println("# Puzzle: " + puzzleName);
            writer.println("# Format: id north east south west");
            writer.println("# Convention: outer border = 0");

            if (comment != null && !comment.isEmpty()) {
                writer.println("# " + comment);
            }

            writer.println();

            // Pieces
            for (int[] piece : pieces) {
                if (piece.length != 5) {
                    throw new IllegalArgumentException(
                        "Invalid piece format: expected 5 values, got " + piece.length);
                }
                writer.printf("%d %d %d %d %d%n",
                    piece[0], piece[1], piece[2], piece[3], piece[4]);
            }
        }
    }

    /**
     * Lists all available puzzle files.
     *
     * @return List of puzzle names (without extension)
     * @throws IOException If the directory cannot be read
     */
    public static List<String> listAvailablePuzzles() throws IOException {
        Path dir = Paths.get(DEFAULT_PUZZLE_DIR);
        List<String> puzzles = new ArrayList<>();

        if (!Files.exists(dir)) {
            return puzzles; // Directory doesn't exist, return empty list
        }

        try (var stream = Files.list(dir)) {
            stream.filter(path -> path.toString().endsWith(DEFAULT_EXTENSION))
                  .forEach(path -> {
                      String filename = path.getFileName().toString();
                      String puzzleName = filename.substring(0, filename.length() - DEFAULT_EXTENSION.length());
                      puzzles.add(puzzleName);
                  });
        }

        return puzzles;
    }
}
