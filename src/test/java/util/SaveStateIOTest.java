package util;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;
import util.SaveStateManager.PlacementInfo;
import util.SaveStateManager.SaveState;
import util.state.SaveStateIO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for SaveStateIO.
 * Tests file I/O operations for saving and loading puzzle states.
 *
 * Critical for data integrity - file corruption can lose hours of computation.
 */
@DisplayName("SaveStateIO Tests")
class SaveStateIOTest {

    private static final String TEST_PUZZLE = "test_puzzle_io";
    private Path tempDir;
    private Board testBoard;
    private Map<Integer, Piece> testPieces;
    private List<PlacementInfo> testPlacementOrder;
    private List<PlacementInfo> testFixedPieces;
    private List<Integer> testUnusedIds;

    @BeforeEach
    void setUp() throws IOException {
        // Create temp directory for test files
        tempDir = Files.createTempDirectory("savestate_test_");

        // Create test board (3x3)
        testBoard = new Board(3, 3);
        testPieces = createTestPieces();

        // Place some pieces on board
        testBoard.place(0, 0, testPieces.get(1), 0);
        testBoard.place(0, 1, testPieces.get(2), 0);
        testBoard.place(1, 0, testPieces.get(4), 0);

        // Create test data
        testPlacementOrder = Arrays.asList(
            new PlacementInfo(0, 0, 1, 0),
            new PlacementInfo(0, 1, 2, 0),
            new PlacementInfo(1, 0, 4, 0)
        );

        testFixedPieces = Arrays.asList(
            new PlacementInfo(0, 0, 1, 0)
        );

        testUnusedIds = Arrays.asList(3, 5, 6, 7, 8, 9);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Cleanup temp directory
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    // ==================== Write/Read Round-Trip Tests ====================

    @Test
    @DisplayName("Should write and read save state successfully")
    void testWriteAndReadRoundTrip() throws IOException {
        String filename = tempDir.resolve("test_save.txt").toString();

        // Write
        SaveStateIO.writeToFile(
            filename,
            TEST_PUZZLE,
            testBoard,
            3, // depth
            testPlacementOrder,
            testUnusedIds,
            25.5, // progress percentage
            1500L, // total compute time ms
            1, // num fixed pieces
            testFixedPieces,
            testPieces
        );

        // Verify file exists
        File savedFile = new File(filename);
        assertTrue(savedFile.exists(), "Save file should exist");
        assertTrue(savedFile.length() > 0, "Save file should not be empty");

        // Read
        SaveState loadedState = SaveStateIO.readFromFile(savedFile, TEST_PUZZLE);

        // Verify loaded state
        assertNotNull(loadedState, "Loaded state should not be null");
        assertEquals(3, loadedState.rows, "Rows should match");
        assertEquals(3, loadedState.cols, "Cols should match");
        assertEquals(3, loadedState.depth, "Depth should match");

        // Verify placements via placements map
        assertNotNull(loadedState.placements, "Placements should not be null");
        assertTrue(loadedState.placements.containsKey("0,0"), "Should have placement at (0,0)");
        assertTrue(loadedState.placements.containsKey("0,1"), "Should have placement at (0,1)");
        assertTrue(loadedState.placements.containsKey("1,0"), "Should have placement at (1,0)");
        assertEquals(1, loadedState.placements.get("0,0").pieceId, "Piece at (0,0) should match");
        assertEquals(2, loadedState.placements.get("0,1").pieceId, "Piece at (0,1) should match");
        assertEquals(4, loadedState.placements.get("1,0").pieceId, "Piece at (1,0) should match");

        // Verify placement order
        assertNotNull(loadedState.placementOrder, "Placement order should not be null");
        assertTrue(loadedState.placementOrder.size() >= 3,
                  "Placement order should have at least 3 entries");
    }

    @Test
    @DisplayName("Should handle empty board write and read")
    void testEmptyBoardRoundTrip() throws IOException {
        Board emptyBoard = new Board(3, 3);
        String filename = tempDir.resolve("empty_save.txt").toString();

        SaveStateIO.writeToFile(
            filename,
            TEST_PUZZLE,
            emptyBoard,
            0,
            Collections.emptyList(),
            Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
            0.0,
            0L,
            0,
            Collections.emptyList(),
            null
        );

        SaveState loadedState = SaveStateIO.readFromFile(new File(filename), TEST_PUZZLE);

        assertNotNull(loadedState, "Loaded state should not be null");
        assertEquals(0, loadedState.depth, "Depth should be 0 for empty board");
        assertNotNull(loadedState.placementOrder, "Placement order should not be null");
        assertTrue(loadedState.placementOrder.isEmpty() ||
                  loadedState.placementOrder.size() == 0,
                  "Placement order should be empty");
    }

    @Test
    @DisplayName("Should preserve all metadata in round-trip")
    void testMetadataPreservation() throws IOException {
        String filename = tempDir.resolve("metadata_test.txt").toString();
        long expectedTime = 12345;
        int expectedDepth = 5;

        SaveStateIO.writeToFile(
            filename,
            TEST_PUZZLE,
            testBoard,
            expectedDepth,
            testPlacementOrder,
            testUnusedIds,
            42.5,
            expectedTime,
            1,
            testFixedPieces,
            testPieces
        );

        SaveState loaded = SaveStateIO.readFromFile(new File(filename), TEST_PUZZLE);

        assertEquals(expectedDepth, loaded.depth, "Depth should be preserved");
        assertEquals(expectedTime, loaded.totalComputeTimeMs,
                    "Compute time should be preserved");
        assertTrue(loaded.timestamp > 0, "Timestamp should be set");
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should return null when reading non-existent file")
    void testReadNonExistentFile() {
        File nonExistent = new File(tempDir.resolve("does_not_exist.txt").toString());

        SaveState result = SaveStateIO.readFromFile(nonExistent, TEST_PUZZLE);

        assertNull(result, "Should return null for non-existent file");
    }

    @Test
    @DisplayName("Should handle malformed file gracefully")
    void testReadMalformedFile() throws IOException {
        String filename = tempDir.resolve("malformed.txt").toString();

        // Write malformed file
        Files.writeString(Path.of(filename), "This is not a valid save file\nRandom data\n");

        SaveState result = SaveStateIO.readFromFile(new File(filename), TEST_PUZZLE);

        // Should handle gracefully - either return null or partial state
        // (behavior depends on implementation)
        assertNotNull(result, "Should handle malformed file without crashing");
    }

    @Test
    @DisplayName("Should throw IOException when writing to invalid path")
    void testWriteToInvalidPath() {
        String invalidPath = "/invalid/path/that/does/not/exist/save.txt";

        assertThrows(IOException.class, () -> {
            SaveStateIO.writeToFile(
                invalidPath,
                TEST_PUZZLE,
                testBoard,
                0,
                Collections.emptyList(),
                Collections.emptyList(),
                0.0,
                0L,
                0,
                Collections.emptyList(),
                null
            );
        }, "Should throw IOException for invalid path");
    }

    // ==================== Edge Cases Tests ====================

    @Test
    @DisplayName("Should handle large board (16x16)")
    void testLargeBoard() throws IOException {
        Board largeBoard = new Board(16, 16);
        List<PlacementInfo> largePlacementOrder = new ArrayList<>();

        // Create larger piece set for this test
        Map<Integer, Piece> largePieces = new HashMap<>();
        for (int i = 1; i <= 16; i++) {
            largePieces.put(i, new Piece(i, new int[]{0, 1, 2, 3}));
        }

        // Place pieces in first row
        for (int col = 0; col < 16; col++) {
            largeBoard.place(0, col, largePieces.get(col + 1), 0);
            largePlacementOrder.add(new PlacementInfo(0, col, col + 1, 0));
        }

        String filename = tempDir.resolve("large_board.txt").toString();

        SaveStateIO.writeToFile(
            filename,
            "large_puzzle",
            largeBoard,
            16,
            largePlacementOrder,
            Collections.emptyList(),
            6.25,
            5000L,
            0,
            Collections.emptyList(),
            largePieces
        );

        SaveState loaded = SaveStateIO.readFromFile(new File(filename), "large_puzzle");

        assertNotNull(loaded, "Should load large board");
        assertEquals(16, loaded.rows, "Rows should match");
        assertEquals(16, loaded.cols, "Cols should match");
        assertEquals(16, loaded.depth, "Depth should match");
    }

    @Test
    @DisplayName("Should handle board with all rotations")
    void testAllRotations() throws IOException {
        Board rotationBoard = new Board(2, 2);
        Map<Integer, Piece> pieces = createTestPieces();
        rotationBoard.place(0, 0, pieces.get(1), 0); // rotation 0
        rotationBoard.place(0, 1, pieces.get(2), 1); // rotation 1
        rotationBoard.place(1, 0, pieces.get(3), 2); // rotation 2
        rotationBoard.place(1, 1, pieces.get(4), 3); // rotation 3

        List<PlacementInfo> placements = Arrays.asList(
            new PlacementInfo(0, 0, 1, 0),
            new PlacementInfo(0, 1, 2, 1),
            new PlacementInfo(1, 0, 3, 2),
            new PlacementInfo(1, 1, 4, 3)
        );

        String filename = tempDir.resolve("rotations.txt").toString();

        SaveStateIO.writeToFile(
            filename,
            TEST_PUZZLE,
            rotationBoard,
            4,
            placements,
            Collections.emptyList(),
            100.0,
            1000L,
            0,
            Collections.emptyList(),
            pieces
        );

        SaveState loaded = SaveStateIO.readFromFile(new File(filename), TEST_PUZZLE);

        assertNotNull(loaded, "Should load board with rotations");
        assertEquals(4, loaded.depth, "Should have 4 pieces placed");

        // Verify rotations are preserved via placements map
        assertEquals(0, loaded.placements.get("0,0").rotation, "Rotation at (0,0) should be 0");
        assertEquals(1, loaded.placements.get("0,1").rotation, "Rotation at (0,1) should be 1");
        assertEquals(2, loaded.placements.get("1,0").rotation, "Rotation at (1,0) should be 2");
        assertEquals(3, loaded.placements.get("1,1").rotation, "Rotation at (1,1) should be 3");
    }

    @Test
    @DisplayName("Should handle file with special characters in puzzle name")
    void testSpecialCharactersInPuzzleName() throws IOException {
        String specialPuzzleName = "test_puzzle-v2.0_config[1]";
        String filename = tempDir.resolve("special_chars.txt").toString();

        SaveStateIO.writeToFile(
            filename,
            specialPuzzleName,
            testBoard,
            3,
            testPlacementOrder,
            testUnusedIds,
            25.5,
            1500L,
            1,
            testFixedPieces,
            testPieces
        );

        // Should not throw exception
        assertTrue(new File(filename).exists(), "File should be created");

        SaveState loaded = SaveStateIO.readFromFile(new File(filename), specialPuzzleName);
        assertNotNull(loaded, "Should load file with special chars in name");
    }

    // ==================== Helper Methods ====================

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> pieces = new HashMap<>();

        // Create simple test pieces (id, [N, E, S, W])
        pieces.put(1, new Piece(1, new int[]{0, 1, 2, 0})); // corner
        pieces.put(2, new Piece(2, new int[]{0, 1, 2, 1})); // top edge
        pieces.put(3, new Piece(3, new int[]{0, 0, 1, 1})); // corner
        pieces.put(4, new Piece(4, new int[]{2, 1, 3, 0})); // left edge
        pieces.put(5, new Piece(5, new int[]{2, 1, 3, 1})); // center
        pieces.put(6, new Piece(6, new int[]{2, 0, 3, 1})); // right edge
        pieces.put(7, new Piece(7, new int[]{3, 1, 0, 0})); // corner
        pieces.put(8, new Piece(8, new int[]{3, 1, 0, 1})); // bottom edge
        pieces.put(9, new Piece(9, new int[]{3, 0, 0, 1})); // corner

        return pieces;
    }
}
