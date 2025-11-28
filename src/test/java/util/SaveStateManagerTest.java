package util;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for SaveStateManager.
 * Tests save state management, binary format control, and file operations.
 */
@DisplayName("SaveStateManager Tests")
class SaveStateManagerTest {

    private static final String TEST_PUZZLE = "test_puzzle";
    private Board testBoard;
    private Map<Integer, Piece> testPieces;

    @BeforeEach
    void setUp() {
        testBoard = new Board(3, 3);
        testPieces = createTestPieces();

        // Reset binary format to default
        SaveStateManager.disableBinaryFormat();
    }

    @AfterEach
    void tearDown() {
        // Cleanup test saves
        cleanupTestSaves();
    }

    // ==================== Binary Format Tests ====================

    @Test
    @DisplayName("Should disable binary format by default")
    void testBinaryFormatDefaultDisabled() {
        SaveStateManager.disableBinaryFormat();
        assertFalse(SaveStateManager.isBinaryFormatEnabled(),
                   "Binary format should be disabled by default");
    }

    @Test
    @DisplayName("Should enable binary format")
    void testEnableBinaryFormat() {
        SaveStateManager.enableBinaryFormat();
        assertTrue(SaveStateManager.isBinaryFormatEnabled(),
                  "Binary format should be enabled");
    }

    @Test
    @DisplayName("Should disable binary format")
    void testDisableBinaryFormat() {
        SaveStateManager.enableBinaryFormat();
        SaveStateManager.disableBinaryFormat();
        assertFalse(SaveStateManager.isBinaryFormatEnabled(),
                   "Binary format should be disabled");
    }

    @Test
    @DisplayName("Should toggle binary format multiple times")
    void testToggleBinaryFormat() {
        SaveStateManager.enableBinaryFormat();
        assertTrue(SaveStateManager.isBinaryFormatEnabled());

        SaveStateManager.disableBinaryFormat();
        assertFalse(SaveStateManager.isBinaryFormatEnabled());

        SaveStateManager.enableBinaryFormat();
        assertTrue(SaveStateManager.isBinaryFormatEnabled());
    }

    // ==================== PlacementInfo Tests ====================

    @Test
    @DisplayName("Should create PlacementInfo with all parameters")
    void testPlacementInfoFullConstructor() {
        SaveStateManager.PlacementInfo info =
            new SaveStateManager.PlacementInfo(1, 2, 5, 3);

        assertEquals(1, info.row, "Row should match");
        assertEquals(2, info.col, "Column should match");
        assertEquals(5, info.pieceId, "Piece ID should match");
        assertEquals(3, info.rotation, "Rotation should match");
    }

    @Test
    @DisplayName("Should create PlacementInfo with compatibility constructor")
    void testPlacementInfoCompatibilityConstructor() {
        SaveStateManager.PlacementInfo info =
            new SaveStateManager.PlacementInfo(5, 3);

        assertEquals(-1, info.row, "Row should be -1");
        assertEquals(-1, info.col, "Column should be -1");
        assertEquals(5, info.pieceId, "Piece ID should match");
        assertEquals(3, info.rotation, "Rotation should match");
    }

    @Test
    @DisplayName("Should create PlacementInfo with zero values")
    void testPlacementInfoZeroValues() {
        SaveStateManager.PlacementInfo info =
            new SaveStateManager.PlacementInfo(0, 0, 0, 0);

        assertEquals(0, info.row);
        assertEquals(0, info.col);
        assertEquals(0, info.pieceId);
        assertEquals(0, info.rotation);
    }

    // ==================== SaveState Tests ====================

    @Test
    @DisplayName("Should create SaveState with all parameters")
    void testSaveStateFullConstructor() {
        Map<String, SaveStateManager.PlacementInfo> placements = new HashMap<>();
        List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
        Set<Integer> unusedPieceIds = new HashSet<>();
        long timestamp = System.currentTimeMillis();

        SaveStateManager.SaveState state = new SaveStateManager.SaveState(
            TEST_PUZZLE, 3, 3, placements, placementOrder,
            unusedPieceIds, timestamp, 5, 1000L);

        assertEquals(TEST_PUZZLE, state.puzzleName);
        assertEquals(3, state.rows);
        assertEquals(3, state.cols);
        assertSame(placements, state.placements);
        assertSame(placementOrder, state.placementOrder);
        assertSame(unusedPieceIds, state.unusedPieceIds);
        assertEquals(timestamp, state.timestamp);
        assertEquals(5, state.depth);
        assertEquals(1000L, state.totalComputeTimeMs);
    }

    @Test
    @DisplayName("Should create SaveState with compatibility constructor")
    void testSaveStateCompatibilityConstructor() {
        Map<String, SaveStateManager.PlacementInfo> placements = new HashMap<>();
        List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
        Set<Integer> unusedPieceIds = new HashSet<>();
        long timestamp = System.currentTimeMillis();

        SaveStateManager.SaveState state = new SaveStateManager.SaveState(
            TEST_PUZZLE, 3, 3, placements, placementOrder,
            unusedPieceIds, timestamp, 5);

        assertEquals(TEST_PUZZLE, state.puzzleName);
        assertEquals(3, state.rows);
        assertEquals(3, state.cols);
        assertEquals(5, state.depth);
        assertEquals(0L, state.totalComputeTimeMs, "Should default to 0");
    }

    @Test
    @DisplayName("Should handle SaveState with empty collections")
    void testSaveStateEmptyCollections() {
        Map<String, SaveStateManager.PlacementInfo> placements = new HashMap<>();
        List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
        Set<Integer> unusedPieceIds = new HashSet<>();

        SaveStateManager.SaveState state = new SaveStateManager.SaveState(
            TEST_PUZZLE, 3, 3, placements, placementOrder,
            unusedPieceIds, 0L, 0);

        assertNotNull(state.placements);
        assertNotNull(state.placementOrder);
        assertNotNull(state.unusedPieceIds);
        assertTrue(state.placements.isEmpty());
        assertTrue(state.placementOrder.isEmpty());
        assertTrue(state.unusedPieceIds.isEmpty());
    }

    // ==================== Save Operations Tests ====================

    @Test
    @DisplayName("Should save state with minimal parameters")
    void testSaveStateMinimal() {
        List<Integer> unusedIds = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);

        assertDoesNotThrow(() -> {
            SaveStateManager.saveState(TEST_PUZZLE, testBoard, testPieces, unusedIds);
        }, "Should save state without errors");
    }

    @Test
    @DisplayName("Should save state with placement order")
    void testSaveStateWithPlacementOrder() {
        List<Integer> unusedIds = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();

        assertDoesNotThrow(() -> {
            SaveStateManager.saveState(TEST_PUZZLE, testBoard, testPieces,
                                      unusedIds, placementOrder);
        }, "Should save state with placement order");
    }

    @Test
    @DisplayName("Should save state with progress percentage")
    void testSaveStateWithProgress() {
        List<Integer> unusedIds = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();

        assertDoesNotThrow(() -> {
            SaveStateManager.saveState(TEST_PUZZLE, testBoard, testPieces,
                                      unusedIds, placementOrder, 50.0);
        }, "Should save state with progress percentage");
    }

    @Test
    @DisplayName("Should save state with all parameters")
    void testSaveStateFullParameters() {
        // Place one piece on board
        testBoard.place(0, 0, testPieces.get(1), 0);

        List<Integer> unusedIds = Arrays.asList(2, 3, 4, 5, 6, 7, 8, 9);
        List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
        placementOrder.add(new SaveStateManager.PlacementInfo(0, 0, 1, 0));

        List<SaveStateManager.PlacementInfo> fixedPieces = new ArrayList<>();

        assertDoesNotThrow(() -> {
            SaveStateManager.saveState(TEST_PUZZLE, testBoard, testPieces,
                                      unusedIds, placementOrder, 10.0, 5000L,
                                      0, fixedPieces);
        }, "Should save state with all parameters");
    }

    // ==================== Load Operations Tests ====================

    @Test
    @DisplayName("Should return null when loading non-existent puzzle")
    void testLoadStateNonExistent() {
        SaveStateManager.SaveState state =
            SaveStateManager.loadState("non_existent_puzzle_xyz");

        assertNull(state, "Should return null for non-existent puzzle");
    }

    @Test
    @DisplayName("Should find current save returns null for non-existent puzzle")
    void testFindCurrentSaveNonExistent() {
        File currentSave = SaveStateManager.findCurrentSave("non_existent_puzzle_xyz");

        assertNull(currentSave, "Should return null for non-existent puzzle");
    }

    @Test
    @DisplayName("Should read total compute time returns zero for non-existent puzzle")
    void testReadTotalComputeTimeNonExistent() {
        long computeTime = SaveStateManager.readTotalComputeTime("non_existent_puzzle_xyz");

        assertEquals(0L, computeTime, "Should return 0 for non-existent puzzle");
    }

    // ==================== Find Operations Tests ====================

    @Test
    @DisplayName("Should return empty list when finding saves for non-existent puzzle")
    void testFindAllSavesNonExistent() {
        List<File> saves = SaveStateManager.findAllSaves("non_existent_puzzle_xyz");

        assertNotNull(saves, "Should return a list");
        assertTrue(saves.isEmpty(), "Should return empty list for non-existent puzzle");
    }

    @Test
    @DisplayName("Should handle findAllSaves with no best files")
    void testFindAllSavesNoBestFiles() {
        // Save without reaching depth 10 (no best files created)
        List<Integer> unusedIds = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        SaveStateManager.saveState(TEST_PUZZLE, testBoard, testPieces, unusedIds);

        List<File> saves = SaveStateManager.findAllSaves(TEST_PUZZLE);

        assertNotNull(saves, "Should return a list");
        // May be empty since we didn't reach depth 10
    }

    // ==================== Delete Operations Tests ====================

    @Test
    @DisplayName("Should handle delete for non-existent save")
    void testDeleteNonExistentSave() {
        assertDoesNotThrow(() -> {
            SaveStateManager.deleteSave("non_existent_puzzle_xyz");
        }, "Should handle delete gracefully for non-existent save");
    }

    // ==================== Restore Operations Tests ====================

    @Test
    @DisplayName("Should handle restore with valid state")
    void testRestoreState() {
        // Create a simple state
        Map<String, SaveStateManager.PlacementInfo> placements = new HashMap<>();
        placements.put("0,0", new SaveStateManager.PlacementInfo(0, 0, 1, 0));

        List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
        placementOrder.add(new SaveStateManager.PlacementInfo(0, 0, 1, 0));

        Set<Integer> unusedPieceIds = new HashSet<>(Arrays.asList(2, 3, 4, 5, 6, 7, 8, 9));

        SaveStateManager.SaveState state = new SaveStateManager.SaveState(
            TEST_PUZZLE, 3, 3, placements, placementOrder,
            unusedPieceIds, System.currentTimeMillis(), 1);

        Board emptyBoard = new Board(3, 3);

        assertDoesNotThrow(() -> {
            SaveStateManager.restoreState(state, emptyBoard, testPieces);
        }, "Should restore state without errors");
    }

    @Test
    @DisplayName("Should handle restore with empty state")
    void testRestoreEmptyState() {
        Map<String, SaveStateManager.PlacementInfo> placements = new HashMap<>();
        List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
        Set<Integer> unusedPieceIds = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));

        SaveStateManager.SaveState state = new SaveStateManager.SaveState(
            TEST_PUZZLE, 3, 3, placements, placementOrder,
            unusedPieceIds, System.currentTimeMillis(), 0);

        Board emptyBoard = new Board(3, 3);

        assertDoesNotThrow(() -> {
            SaveStateManager.restoreState(state, emptyBoard, testPieces);
        }, "Should restore empty state without errors");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle save with empty unused pieces list")
    void testSaveWithEmptyUnusedPieces() {
        // All pieces used
        List<Integer> unusedIds = new ArrayList<>();

        assertDoesNotThrow(() -> {
            SaveStateManager.saveState(TEST_PUZZLE, testBoard, testPieces, unusedIds);
        }, "Should handle save with all pieces used");
    }

    @Test
    @DisplayName("Should handle save with null placement order")
    void testSaveWithNullPlacementOrder() {
        List<Integer> unusedIds = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);

        assertDoesNotThrow(() -> {
            SaveStateManager.saveState(TEST_PUZZLE, testBoard, testPieces,
                                      unusedIds, null);
        }, "Should handle save with null placement order");
    }

    @Test
    @DisplayName("Should handle puzzle name with special characters")
    void testSaveWithSpecialCharactersPuzzleName() {
        String specialName = "test-puzzle_2025!@#";
        List<Integer> unusedIds = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);

        assertDoesNotThrow(() -> {
            SaveStateManager.saveState(specialName, testBoard, testPieces, unusedIds);
        }, "Should handle puzzle name with special characters");

        // Cleanup
        SaveStateManager.deleteSave(specialName);
    }

    @Test
    @DisplayName("Should handle negative progress percentage")
    void testSaveWithNegativeProgress() {
        List<Integer> unusedIds = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();

        assertDoesNotThrow(() -> {
            SaveStateManager.saveState(TEST_PUZZLE, testBoard, testPieces,
                                      unusedIds, placementOrder, -1.0);
        }, "Should handle negative progress percentage");
    }

    @Test
    @DisplayName("Should handle zero elapsed time")
    void testSaveWithZeroElapsedTime() {
        List<Integer> unusedIds = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();

        assertDoesNotThrow(() -> {
            SaveStateManager.saveState(TEST_PUZZLE, testBoard, testPieces,
                                      unusedIds, placementOrder, 0.0, 0L, 0, null);
        }, "Should handle zero elapsed time");
    }

    // ==================== Helper Methods ====================

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));
        pieces.put(2, new Piece(2, new int[]{0, 2, 3, 1}));
        pieces.put(3, new Piece(3, new int[]{0, 3, 4, 2}));
        pieces.put(4, new Piece(4, new int[]{2, 4, 5, 0}));
        pieces.put(5, new Piece(5, new int[]{3, 5, 6, 4}));
        pieces.put(6, new Piece(6, new int[]{4, 6, 7, 5}));
        pieces.put(7, new Piece(7, new int[]{5, 7, 0, 0}));
        pieces.put(8, new Piece(8, new int[]{6, 8, 0, 7}));
        pieces.put(9, new Piece(9, new int[]{7, 0, 0, 8}));
        return pieces;
    }

    private void cleanupTestSaves() {
        // Cleanup test save files
        File savesDir = new File("saves");
        if (savesDir.exists() && savesDir.isDirectory()) {
            File[] testDirs = savesDir.listFiles((dir, name) ->
                name.contains("test") || name.contains("TEST"));

            if (testDirs != null) {
                for (File dir : testDirs) {
                    deleteRecursively(dir);
                }
            }
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteRecursively(f);
                }
            }
        }
        file.delete();
    }
}
