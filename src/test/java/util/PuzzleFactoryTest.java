package util;

import model.Piece;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for PuzzleFactory.
 * Tests puzzle loading from files and programmatic creation.
 *
 * Critical for ensuring puzzle data integrity - corrupted puzzles waste hours of computation.
 */
@DisplayName("PuzzleFactory Tests")
class PuzzleFactoryTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("puzzle_test_");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    // ==================== Standard Format Loading Tests ====================

    @Test
    @DisplayName("Should load 3x3 example puzzle")
    void testLoad3x3Example() {
        Map<Integer, Piece> pieces = PuzzleFactory.createExample3x3();

        assertNotNull(pieces, "Puzzle should not be null");
        assertEquals(9, pieces.size(), "3x3 puzzle should have 9 pieces");

        // Verify pieces are correctly indexed
        for (int i = 1; i <= 9; i++) {
            assertTrue(pieces.containsKey(i), "Should contain piece " + i);
            assertEquals(i, pieces.get(i).getId(), "Piece ID should match map key");
        }
    }

    @Test
    @DisplayName("Should load 4x4 example puzzles (all variants)")
    void testLoad4x4Variants() {
        Map<Integer, Piece> easy = PuzzleFactory.createExample4x4Easy();
        Map<Integer, Piece> hardV1 = PuzzleFactory.createExample4x4HardV1();
        Map<Integer, Piece> hardV2 = PuzzleFactory.createExample4x4HardV2();
        Map<Integer, Piece> hardV3 = PuzzleFactory.createExample4x4HardV3();
        Map<Integer, Piece> ordered = PuzzleFactory.createExample4x4Ordered();
        Map<Integer, Piece> defaultPuzzle = PuzzleFactory.createExample4x4();

        // All variants should have 16 pieces
        assertEquals(16, easy.size(), "Easy variant should have 16 pieces");
        assertEquals(16, hardV1.size(), "Hard V1 should have 16 pieces");
        assertEquals(16, hardV2.size(), "Hard V2 should have 16 pieces");
        assertEquals(16, hardV3.size(), "Hard V3 should have 16 pieces");
        assertEquals(16, ordered.size(), "Ordered should have 16 pieces");
        assertEquals(16, defaultPuzzle.size(), "Default should have 16 pieces");

        // Default should be hardV3
        assertEquals(hardV3.get(1).getEdges()[0], defaultPuzzle.get(1).getEdges()[0],
                    "Default 4x4 should be hardV3");
    }

    @Test
    @DisplayName("Should load 5x5 example puzzles")
    void testLoad5x5Variants() {
        Map<Integer, Piece> hardV1 = PuzzleFactory.createExample5x5HardV1();
        Map<Integer, Piece> ordered = PuzzleFactory.createExample5x5Ordered();
        Map<Integer, Piece> defaultPuzzle = PuzzleFactory.createExample5x5();

        assertEquals(25, hardV1.size(), "Hard V1 should have 25 pieces");
        assertEquals(25, ordered.size(), "Ordered should have 25 pieces");
        assertEquals(25, defaultPuzzle.size(), "Default should have 25 pieces");

        // Default should be hardV1
        assertEquals(hardV1.get(1).getEdges()[0], defaultPuzzle.get(1).getEdges()[0],
                    "Default 5x5 should be hardV1");
    }

    @Test
    @DisplayName("Should load validation 6x6 puzzle")
    void testLoadValidation6x6() {
        Map<Integer, Piece> pieces = PuzzleFactory.createValidation6x6();

        assertNotNull(pieces, "Puzzle should not be null");
        assertEquals(36, pieces.size(), "6x6 puzzle should have 36 pieces");
    }

    @Test
    @DisplayName("Should load 6x12 puzzle")
    void testLoad6x12() {
        Map<Integer, Piece> pieces = PuzzleFactory.createPuzzle6x12();

        assertNotNull(pieces, "Puzzle should not be null");
        assertEquals(72, pieces.size(), "6x12 puzzle should have 72 pieces");
    }

    @Test
    @DisplayName("Should load 16x16 puzzle")
    void testLoad16x16() {
        Map<Integer, Piece> pieces = PuzzleFactory.createPuzzle16x16();

        assertNotNull(pieces, "Puzzle should not be null");
        assertEquals(256, pieces.size(), "16x16 puzzle should have 256 pieces");
    }

    @Test
    @DisplayName("Should load Eternity II puzzle")
    void testLoadEternityII() {
        Map<Integer, Piece> pieces = PuzzleFactory.createEternityII();

        assertNotNull(pieces, "Eternity II puzzle should not be null");
        assertEquals(256, pieces.size(), "Eternity II should have 256 pieces");

        // Verify IDs are auto-generated from 1 to 256
        for (int i = 1; i <= 256; i++) {
            assertTrue(pieces.containsKey(i), "Should contain piece " + i);
        }
    }

    // ==================== Piece Validation Tests ====================

    @Test
    @DisplayName("Should create pieces with valid edge values")
    void testPieceEdgeValidity() {
        Map<Integer, Piece> pieces = PuzzleFactory.createExample3x3();

        for (Piece piece : pieces.values()) {
            assertNotNull(piece, "Piece should not be null");
            assertTrue(piece.getId() > 0, "Piece ID should be positive");

            // Verify piece has 4 edges
            int[] edges = piece.getEdges();
            assertEquals(4, edges.length, "Piece should have 4 edges");
            for (int i = 0; i < 4; i++) {
                assertTrue(edges[i] >= 0, "Edge value should be non-negative");
            }
        }
    }

    @Test
    @DisplayName("Should preserve piece IDs from file")
    void testPieceIDPreservation() {
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4();

        for (Map.Entry<Integer, Piece> entry : pieces.entrySet()) {
            assertEquals(entry.getKey(), entry.getValue().getId(),
                        "Map key should match piece ID");
        }
    }

    // ==================== buildPiecesFromDefs Tests ====================

    @Test
    @DisplayName("Should build pieces from 2D array definitions")
    void testBuildPiecesFromDefs() {
        int[][] defs = {
            {1, 0, 1, 2, 0},   // id=1: N=0, E=1, S=2, W=0
            {2, 0, 1, 2, 1},   // id=2: N=0, E=1, S=2, W=1
            {3, 2, 1, 0, 0}    // id=3: N=2, E=1, S=0, W=0
        };

        Map<Integer, Piece> pieces = PuzzleFactory.buildPiecesFromDefs(defs);

        assertEquals(3, pieces.size(), "Should create 3 pieces");

        // Verify piece 1
        Piece p1 = pieces.get(1);
        assertEquals(1, p1.getId());
        int[] edges1 = p1.getEdges();
        assertEquals(0, edges1[0]); // North
        assertEquals(1, edges1[1]); // East
        assertEquals(2, edges1[2]); // South
        assertEquals(0, edges1[3]); // West

        // Verify piece 2
        Piece p2 = pieces.get(2);
        assertEquals(2, p2.getId());
        int[] edges2 = p2.getEdges();
        assertEquals(0, edges2[0]); // North
        assertEquals(1, edges2[1]); // East
        assertEquals(2, edges2[2]); // South
        assertEquals(1, edges2[3]); // West
    }

    @Test
    @DisplayName("Should throw exception for invalid def length")
    void testBuildPiecesFromDefsInvalidLength() {
        int[][] invalidDefs = {
            {1, 0, 1, 2}  // Only 4 values, needs 5
        };

        assertThrows(IllegalArgumentException.class, () -> {
            PuzzleFactory.buildPiecesFromDefs(invalidDefs);
        }, "Should throw exception for invalid definition length");
    }

    @Test
    @DisplayName("Should handle empty definitions array")
    void testBuildPiecesFromDefsEmpty() {
        int[][] emptyDefs = {};

        Map<Integer, Piece> pieces = PuzzleFactory.buildPiecesFromDefs(emptyDefs);

        assertNotNull(pieces, "Should return non-null map");
        assertEquals(0, pieces.size(), "Should return empty map");
    }

    // ==================== File Format Tests ====================

    @Test
    @DisplayName("Should load file with comments")
    void testLoadFileWithComments() throws IOException {
        String content = """
            # This is a comment
            1 0 1 2 0
            # Another comment
            2 0 1 2 1
            3 2 1 0 0
            """;

        Path testFile = tempDir.resolve("with_comments.txt");
        Files.writeString(testFile, content);

        Map<Integer, Piece> pieces = PuzzleFactory.loadFromFile(testFile.toString());

        assertEquals(3, pieces.size(), "Should load 3 pieces, skipping comments");
    }

    @Test
    @DisplayName("Should load file with empty lines")
    void testLoadFileWithEmptyLines() throws IOException {
        String content = """
            1 0 1 2 0

            2 0 1 2 1


            3 2 1 0 0
            """;

        Path testFile = tempDir.resolve("with_empty_lines.txt");
        Files.writeString(testFile, content);

        Map<Integer, Piece> pieces = PuzzleFactory.loadFromFile(testFile.toString());

        assertEquals(3, pieces.size(), "Should load 3 pieces, skipping empty lines");
    }

    @Test
    @DisplayName("Should handle whitespace correctly")
    void testLoadFileWithWhitespace() throws IOException {
        String content = """
              1   0   1   2   0
            2\t0\t1\t2\t1
            3    2    1    0    0
            """;

        Path testFile = tempDir.resolve("with_whitespace.txt");
        Files.writeString(testFile, content);

        Map<Integer, Piece> pieces = PuzzleFactory.loadFromFile(testFile.toString());

        assertEquals(3, pieces.size(), "Should handle various whitespace");
    }

    // ==================== Eternity II Format Tests ====================

    @Test
    @DisplayName("Should load Eternity II format correctly")
    void testLoadEternityIIFormat() throws IOException {
        // Eternity II format: N S W E (4 values, no ID)
        String content = """
            1 2 0 5
            9 3 1 7
            0 4 2 8
            """;

        Path testFile = tempDir.resolve("eternity_format.txt");
        Files.writeString(testFile, content);

        Map<Integer, Piece> pieces = PuzzleFactory.loadEternityIIFromFile(testFile.toString());

        assertEquals(3, pieces.size(), "Should load 3 pieces");

        // Verify auto-generated IDs (1, 2, 3)
        assertTrue(pieces.containsKey(1));
        assertTrue(pieces.containsKey(2));
        assertTrue(pieces.containsKey(3));

        // Verify first piece edges (format is N S W E, internal is N E S W)
        Piece p1 = pieces.get(1);
        int[] edges = p1.getEdges();
        assertEquals(1, edges[0]); // North = 1
        assertEquals(5, edges[1]); // East = 5
        assertEquals(2, edges[2]); // South = 2
        assertEquals(0, edges[3]); // West = 0
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should throw exception for non-existent file")
    void testLoadNonExistentFile() {
        String nonExistentFile = tempDir.resolve("does_not_exist.txt").toString();

        assertThrows(RuntimeException.class, () -> {
            PuzzleFactory.loadFromFile(nonExistentFile);
        }, "Should throw exception for non-existent file");
    }

    @Test
    @DisplayName("Should throw exception for malformed line (wrong field count)")
    void testLoadMalformedFileWrongFieldCount() throws IOException {
        String content = """
            1 0 1 2
            """;

        Path testFile = tempDir.resolve("malformed.txt");
        Files.writeString(testFile, content);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            PuzzleFactory.loadFromFile(testFile.toString());
        });
        assertTrue(exception.getMessage().contains("Invalid line format"),
                  "Error message should mention invalid format");
    }

    @Test
    @DisplayName("Should throw exception for invalid number format")
    void testLoadInvalidNumberFormat() throws IOException {
        String content = """
            1 0 abc 2 0
            """;

        Path testFile = tempDir.resolve("invalid_number.txt");
        Files.writeString(testFile, content);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            PuzzleFactory.loadFromFile(testFile.toString());
        });
        assertTrue(exception.getMessage().contains("Invalid number format"),
                  "Error message should mention invalid number format");
    }

    @Test
    @DisplayName("Should throw exception for Eternity II format with wrong field count")
    void testLoadEternityIIMalformed() throws IOException {
        String content = """
            1 2 0
            """;

        Path testFile = tempDir.resolve("eternity_malformed.txt");
        Files.writeString(testFile, content);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            PuzzleFactory.loadEternityIIFromFile(testFile.toString());
        });
        assertTrue(exception.getMessage().contains("Invalid line format"),
                  "Error message should mention invalid format");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle file with only comments")
    void testLoadFileOnlyComments() throws IOException {
        String content = """
            # Comment 1
            # Comment 2
            # Comment 3
            """;

        Path testFile = tempDir.resolve("only_comments.txt");
        Files.writeString(testFile, content);

        Map<Integer, Piece> pieces = PuzzleFactory.loadFromFile(testFile.toString());

        assertNotNull(pieces, "Should return non-null map");
        assertEquals(0, pieces.size(), "Should return empty map for file with only comments");
    }

    @Test
    @DisplayName("Should maintain insertion order")
    void testMaintainsInsertionOrder() {
        Map<Integer, Piece> pieces = PuzzleFactory.createExample3x3();

        // Verify map is LinkedHashMap (maintains insertion order)
        assertTrue(pieces instanceof LinkedHashMap, "Should use LinkedHashMap");

        // Verify IDs are in order
        List<Integer> ids = new ArrayList<>(pieces.keySet());
        for (int i = 0; i < ids.size() - 1; i++) {
            assertTrue(ids.get(i) < ids.get(i + 1), "IDs should be in ascending order");
        }
    }

    @Test
    @DisplayName("Should handle large puzzles efficiently")
    void testLargePuzzleLoading() {
        // Test with 16x16 puzzle (256 pieces)
        long startTime = System.currentTimeMillis();
        Map<Integer, Piece> pieces = PuzzleFactory.createPuzzle16x16();
        long loadTime = System.currentTimeMillis() - startTime;

        assertEquals(256, pieces.size(), "Should load all 256 pieces");
        assertTrue(loadTime < 1000, "Should load 256-piece puzzle in under 1 second");
    }
}
