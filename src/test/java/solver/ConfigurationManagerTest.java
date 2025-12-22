package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.SaveStateManager;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigurationManager
 */
public class ConfigurationManagerTest {

    private ConfigurationManager config;
    private Board board;
    private Map<Integer, Piece> pieces;

    @BeforeEach
    void setUp() {
        config = new ConfigurationManager();
        board = new Board(3, 3);

        // Create test pieces
        pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 1, 1, 0}));
        pieces.put(2, new Piece(2, new int[]{0, 2, 2, 0}));
        pieces.put(3, new Piece(3, new int[]{1, 2, 3, 4}));
    }

    @Test
    void testDefaultConfiguration() {
        // Verify default values
        assertTrue(config.isUseSingletons());
        assertTrue(config.isVerbose());
        assertTrue(config.isUseAC3());
        assertTrue(config.isUseDomainCache());
        assertFalse(config.isPrioritizeBorders());
        assertEquals(0, config.getMinDepthToShowRecords());
        assertEquals("eternity2", config.getPuzzleName());
        assertEquals("", config.getThreadLabel());
        assertEquals("ascending", config.getSortOrder());
    }

    @Test
    void testSettersAndGetters() {
        config.setUseSingletons(false);
        assertFalse(config.isUseSingletons());

        config.setVerbose(false);
        assertFalse(config.isVerbose());

        config.setUseAC3(false);
        assertFalse(config.isUseAC3());

        config.setUseDomainCache(false);
        assertFalse(config.isUseDomainCache());

        config.setPrioritizeBorders(true);
        assertTrue(config.isPrioritizeBorders());

        config.setPuzzleName("test_puzzle");
        assertEquals("test_puzzle", config.getPuzzleName());

        config.setThreadLabel("[Thread 1]");
        assertEquals("[Thread 1]", config.getThreadLabel());

        config.setSortOrder("descending");
        assertEquals("descending", config.getSortOrder());

        config.setMaxExecutionTime(5000L);
        assertEquals(5000L, config.getMaxExecutionTimeMs());

        config.setThreadId(42);
        assertEquals(42, config.getThreadId());

        config.setRandomSeed(12345L);
        assertEquals(12345L, config.getRandomSeed());
    }

    @Test
    void testSetDisplayConfig() {
        config.setDisplayConfig(false, 10);
        assertFalse(config.isVerbose());
        assertEquals(10, config.getMinDepthToShowRecords());
    }

    @Test
    void testDetectFixedPiecesFromBoard_EmptyBoard() {
        BitSet pieceUsed = new BitSet(10);

        List<SaveStateManager.PlacementInfo> fixedPieces = config.detectFixedPiecesFromBoard(board, pieceUsed);

        assertEquals(0, config.getNumFixedPieces());
        assertTrue(config.getFixedPositions().isEmpty());
        assertTrue(config.getInitialFixedPieces().isEmpty());
        assertTrue(fixedPieces.isEmpty());
    }

    @Test
    void testDetectFixedPiecesFromBoard_WithPieces() {
        // Place two pieces on the board
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(2), 0);

        BitSet pieceUsed = new BitSet(10);

        List<SaveStateManager.PlacementInfo> fixedPieces = config.detectFixedPiecesFromBoard(board, pieceUsed);

        // Should detect 2 fixed pieces
        assertEquals(2, config.getNumFixedPieces());
        assertEquals(2, config.getFixedPositions().size());
        assertEquals(2, config.getInitialFixedPieces().size());
        assertEquals(2, fixedPieces.size());

        // Verify pieces are marked as used
        assertTrue(pieceUsed.get(1));
        assertTrue(pieceUsed.get(2));

        // Verify fixed positions
        assertTrue(config.getFixedPositions().contains("0,0"));
        assertTrue(config.getFixedPositions().contains("0,1"));
    }

    @Test
    void testDetectFixedPiecesFromBoard_SinglePiece() {
        // Test with a single placed piece
        board.place(0, 0, pieces.get(1), 0);
        BitSet pieceUsed = new BitSet(10);

        List<SaveStateManager.PlacementInfo> fixedPieces = config.detectFixedPiecesFromBoard(board, pieceUsed);

        assertEquals(1, config.getNumFixedPieces());
        assertEquals(1, fixedPieces.size());
        assertTrue(pieceUsed.get(1));
    }

    @Test
    void testCalculateNumFixedPieces_Eternity2() {
        int numFixed = config.calculateNumFixedPieces("eternity2");
        assertEquals(9, numFixed); // 4 corners + 5 hints
    }

    @Test
    void testCalculateNumFixedPieces_Indice() {
        int numFixed = config.calculateNumFixedPieces("indice_01");
        assertEquals(0, numFixed); // No fixed pieces for hint puzzles
    }

    @Test
    void testCalculateNumFixedPieces_Unknown() {
        int numFixed = config.calculateNumFixedPieces("unknown_puzzle");
        assertEquals(0, numFixed); // Default: no fixed pieces
    }

    @Test
    void testBuildInitialFixedPieces() {
        List<SaveStateManager.PlacementInfo> preloaded = new ArrayList<>();
        preloaded.add(new SaveStateManager.PlacementInfo(0, 0, 1, 0));
        preloaded.add(new SaveStateManager.PlacementInfo(0, 1, 2, 0));
        preloaded.add(new SaveStateManager.PlacementInfo(0, 2, 3, 0));

        config.buildInitialFixedPieces(preloaded, 2);

        // Should only take first 2 pieces as fixed
        assertEquals(2, config.getNumFixedPieces());
        assertEquals(2, config.getInitialFixedPieces().size());
    }

    @Test
    void testBuildInitialFixedPieces_MoreFixedThanAvailable() {
        List<SaveStateManager.PlacementInfo> preloaded = new ArrayList<>();
        preloaded.add(new SaveStateManager.PlacementInfo(0, 0, 1, 0));

        config.buildInitialFixedPieces(preloaded, 5);

        // Should only take what's available (1 piece)
        assertEquals(5, config.getNumFixedPieces()); // numFixedPieces set to 5
        assertEquals(1, config.getInitialFixedPieces().size()); // But only 1 available
    }

    @Test
    void testCreateAutoSaveManager() {
        config.setPuzzleName("test_puzzle");
        config.setNumFixedPieces(2);

        PlacementOrderTracker tracker = new PlacementOrderTracker();
        tracker.initialize();

        AutoSaveManager manager = config.createAutoSaveManager(tracker, pieces);

        assertNotNull(manager);
    }

    @Test
    void testCreateRecordManager() {
        config.setPuzzleName("test_puzzle");
        config.setThreadId(1);
        config.setMinDepthToShowRecords(5);

        Object lockObject = new Object();
        java.util.concurrent.atomic.AtomicInteger globalMaxDepth = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger globalBestScore = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger globalBestThreadId = new java.util.concurrent.atomic.AtomicInteger(-1);
        java.util.concurrent.atomic.AtomicReference<Board> globalBestBoard = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<Map<Integer, Piece>> globalBestPieces = new java.util.concurrent.atomic.AtomicReference<>(null);

        RecordManager manager = config.createRecordManager(
            lockObject, globalMaxDepth, globalBestScore, globalBestThreadId,
            globalBestBoard, globalBestPieces
        );

        assertNotNull(manager);
    }

    @Test
    void testLogConfiguration_DoesNotCrash() {
        config.setVerbose(true);
        assertDoesNotThrow(() -> config.logConfiguration());

        config.setVerbose(false);
        assertDoesNotThrow(() -> config.logConfiguration());
    }

    @Test
    void testMultipleConfigChanges() {
        // Change config multiple times
        config.setPuzzleName("puzzle1");
        config.setThreadId(1);
        config.setUseSingletons(false);

        assertEquals("puzzle1", config.getPuzzleName());
        assertEquals(1, config.getThreadId());
        assertFalse(config.isUseSingletons());

        // Change again
        config.setPuzzleName("puzzle2");
        config.setThreadId(2);
        config.setUseSingletons(true);

        assertEquals("puzzle2", config.getPuzzleName());
        assertEquals(2, config.getThreadId());
        assertTrue(config.isUseSingletons());
    }

    @Test
    void testGetFixedPositions_ReturnsNewSet() {
        // Modify returned set should not affect internal state
        Set<String> positions = config.getFixedPositions();
        positions.add("test");

        Set<String> positions2 = config.getFixedPositions();
        assertFalse(positions2.contains("test")); // Should not contain our modification
    }

    @Test
    void testGetInitialFixedPieces_ReturnsNewList() {
        // Modify returned list should not affect internal state
        List<SaveStateManager.PlacementInfo> pieces = config.getInitialFixedPieces();
        pieces.add(new SaveStateManager.PlacementInfo(0, 0, 99, 0));

        List<SaveStateManager.PlacementInfo> pieces2 = config.getInitialFixedPieces();
        assertEquals(0, pieces2.size()); // Should not contain our modification
    }

    @Test
    void testGetThreadSaveInterval() {
        assertEquals(60000L, ConfigurationManager.getThreadSaveInterval());
    }
}
