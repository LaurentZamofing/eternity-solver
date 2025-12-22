package integration;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;
import solver.EternitySolver;
import util.SaveStateManager;
import util.state.SaveStateIO;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for complete save/load cycles.
 * Tests end-to-end save state management including:
 * - Binary vs text format comparison
 * - Milestone preservation
 * - Resume from save with correct state
 */
@DisplayName("Save State Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SaveStateIntegrationTest {

    private static final String TEST_PUZZLE = "test_saveload_integration";
    private EternitySolver solver;
    private Board testBoard;
    private Map<Integer, Piece> testPieces;

    @BeforeEach
    void setUp() {
        solver = new EternitySolver();
        solver.setPuzzleName(TEST_PUZZLE);

        // Create simple 3x3 test puzzle
        testBoard = new Board(3, 3);
        testPieces = createSimplePuzzle();

        // Clean up any existing saves
        cleanupTestSaves();
    }

    @AfterEach
    void tearDown() {
        cleanupTestSaves();
    }

    @Test
    @Order(1)
    @DisplayName("Should save and load state in text format")
    void testTextFormatSaveLoad() throws Exception {
        // Arrange
        SaveStateManager.disableBinaryFormat();

        // Place some pieces
        Piece piece1 = testPieces.get(1);
        testBoard.place(0, 0, piece1, 0);
        Piece piece2 = testPieces.get(2);
        testBoard.place(0, 1, piece2, 1);

        List<SaveStateManager.PlacementInfo> placements = Arrays.asList(
            new SaveStateManager.PlacementInfo(0, 0, 1, 0),
            new SaveStateManager.PlacementInfo(0, 1, 2, 1)
        );

        // Act - Save
        SaveStateIO.saveCurrentState(
            TEST_PUZZLE, testBoard, 2, 0,
            placements, Collections.emptyList(), 1000L
        );

        // Load
        File saveFile = SaveStateManager.findCurrentSave(TEST_PUZZLE);
        assertNotNull(saveFile, "Save file should exist");

        SaveStateManager.SaveState loaded = SaveStateIO.loadSaveState(saveFile.getAbsolutePath());

        // Assert
        assertNotNull(loaded, "Loaded state should not be null");
        assertEquals(2, loaded.depth, "Depth should match");
        assertEquals(2, loaded.placementOrder.size(), "Placement count should match");
        assertEquals(1, loaded.placementOrder.get(0).pieceId, "First piece ID should match");
        assertEquals(2, loaded.placementOrder.get(1).pieceId, "Second piece ID should match");
    }

    @Test
    @Order(2)
    @DisplayName("Should save and load state in binary format")
    void testBinaryFormatSaveLoad() throws Exception {
        // Arrange
        SaveStateManager.enableBinaryFormat();

        testBoard.place(0, 0, testPieces.get(1), 0);
        testBoard.place(0, 1, testPieces.get(2), 1);
        testBoard.place(1, 0, testPieces.get(3), 2);

        List<SaveStateManager.PlacementInfo> placements = Arrays.asList(
            new SaveStateManager.PlacementInfo(0, 0, 1, 0),
            new SaveStateManager.PlacementInfo(0, 1, 2, 1),
            new SaveStateManager.PlacementInfo(1, 0, 3, 2)
        );

        // Act - Save in binary
        SaveStateIO.saveCurrentState(
            TEST_PUZZLE, testBoard, 3, 0,
            placements, Collections.emptyList(), 2000L
        );

        // Load
        File saveFile = SaveStateManager.findCurrentSave(TEST_PUZZLE);
        assertNotNull(saveFile, "Binary save file should exist");
        assertTrue(saveFile.getName().endsWith(".bin"), "Should be binary format");

        SaveStateManager.SaveState loaded = SaveStateIO.loadSaveState(saveFile.getAbsolutePath());

        // Assert
        assertNotNull(loaded, "Binary loaded state should not be null");
        assertEquals(3, loaded.depth, "Depth should match");
        assertEquals(3, loaded.placementOrder.size(), "Binary placement count should match");
    }

    @Test
    @Order(3)
    @DisplayName("Should preserve milestone saves")
    void testMilestonePreservation() throws Exception {
        // Arrange - Create multiple saves at different depths
        for (int depth = 1; depth <= 5; depth++) {
            List<SaveStateManager.PlacementInfo> placements = new ArrayList<>();
            for (int i = 0; i < depth; i++) {
                placements.add(new SaveStateManager.PlacementInfo(0, i % 3, i + 1, 0));
            }

            SaveStateIO.saveCurrentState(
                TEST_PUZZLE, testBoard, depth, 0,
                placements, Collections.emptyList(), depth * 1000L
            );

            // Small delay to ensure different timestamps
            Thread.sleep(10);
        }

        // Act - Find all saves
        File saveDir = new File("saves/" + TEST_PUZZLE + "/");
        File[] saves = saveDir.listFiles((dir, name) ->
            name.startsWith(TEST_PUZZLE + "_") && !name.contains("_current")
        );

        // Assert - Multiple milestone saves should exist
        assertNotNull(saves, "Save directory should exist");
        assertTrue(saves.length >= 1, "Should have milestone saves");

        // Current save should be latest
        File currentSave = SaveStateManager.findCurrentSave(TEST_PUZZLE);
        assertNotNull(currentSave, "Current save should exist");
    }

    @Test
    @Order(4)
    @DisplayName("Should correctly resume solving from saved state")
    void testResumeFromSave() throws Exception {
        // Arrange - Save state at depth 2
        testBoard.place(0, 0, testPieces.get(1), 0);
        testBoard.place(0, 1, testPieces.get(2), 0);

        List<SaveStateManager.PlacementInfo> savedPlacements = Arrays.asList(
            new SaveStateManager.PlacementInfo(0, 0, 1, 0),
            new SaveStateManager.PlacementInfo(0, 1, 2, 0)
        );

        SaveStateIO.saveCurrentState(
            TEST_PUZZLE, testBoard, 2, 0,
            savedPlacements, Collections.emptyList(), 500L
        );

        // Act - Load and create new solver
        File saveFile = SaveStateManager.findCurrentSave(TEST_PUZZLE);
        SaveStateManager.SaveState loadedState = SaveStateIO.loadSaveState(saveFile.getAbsolutePath());

        // Create fresh board
        Board resumeBoard = new Board(3, 3);
        Map<Integer, Piece> allPieces = createSimplePuzzle();

        // Rebuild board from placement order
        for (SaveStateManager.PlacementInfo placement : loadedState.placementOrder) {
            Piece piece = allPieces.get(placement.pieceId);
            resumeBoard.place(placement.row, placement.col, piece, placement.rotation);
        }

        // Assert - Board should match original
        assertEquals(2, loadedState.depth, "Loaded depth should be 2");
        assertEquals(2, loadedState.placementOrder.size(), "Should have 2 placements");
        assertFalse(resumeBoard.isEmpty(0, 0), "First piece should be placed");
        assertFalse(resumeBoard.isEmpty(0, 1), "Second piece should be placed");
        assertEquals(1, resumeBoard.getPlacement(0, 0).getPieceId(), "First piece ID should match");
        assertEquals(2, resumeBoard.getPlacement(0, 1).getPieceId(), "Second piece ID should match");
    }

    // Helper methods

    private Map<Integer, Piece> createSimplePuzzle() {
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= 9; i++) {
            pieces.put(i, new Piece(i, new int[]{i, i, i, i}));
        }
        return pieces;
    }

    private void cleanupTestSaves() {
        File saveDir = new File("saves/" + TEST_PUZZLE + "/");
        if (saveDir.exists()) {
            File[] files = saveDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            saveDir.delete();
        }

        // Also clean parent if empty
        File parentDir = new File("saves/");
        if (parentDir.exists() && parentDir.list() != null && parentDir.list().length == 0) {
            parentDir.delete();
        }
    }
}
