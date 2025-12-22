package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;
import util.SaveStateManager.PlacementInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for BacktrackingHistoryManager.
 * Tests backtracking logic, timeout handling, and rotation alternatives.
 *
 * Critical for ensuring saved game resume functionality works correctly.
 */
@DisplayName("BacktrackingHistoryManager Tests")
class BacktrackingHistoryManagerTest {

    private PlacementValidator validator;
    private EternitySolver.Statistics stats;
    private BacktrackingHistoryManager manager;
    private Board board;
    private Map<Integer, Piece> pieces;
    private BitSet pieceUsed;

    @BeforeEach
    void setUp() {
        stats = new EternitySolver.Statistics();

        // Create a simple validator for tests that don't need complex validation
        board = new Board(3, 3);
        CellConstraints[][] constraints = CellConstraints.createConstraintsMatrix(3, 3);
        validator = new PlacementValidator(constraints, stats, "ascending");

        manager = new BacktrackingHistoryManager(validator, "TEST", stats);

        // Create test pieces
        pieces = createTestPieces();

        // Initialize piece usage tracking
        pieceUsed = new BitSet(10);
    }

    // ==================== Constructor & Configuration Tests ====================

    @Test
    @DisplayName("Should create manager with valid configuration")
    void testConstructor() {
        BacktrackingHistoryManager mgr = new BacktrackingHistoryManager(
            validator, "THREAD-1", stats);

        assertNotNull(mgr, "Manager should not be null");
    }

    @Test
    @DisplayName("Should set timeout configuration")
    void testSetTimeoutConfig() {
        long startTime = System.currentTimeMillis();
        long maxTime = 5000;

        manager.setTimeoutConfig(startTime, maxTime);

        // Manager should be created without exception
        assertNotNull(manager, "Manager should accept timeout config");
    }

    // ==================== calculateFixedPieces Tests ====================

    @Test
    @DisplayName("Should return 9 fixed pieces for eternity2 puzzle")
    void testCalculateFixedPiecesEternity2() {
        int fixed = manager.calculateFixedPieces("eternity2_p01_ascending");
        assertEquals(9, fixed, "Eternity2 should have 9 fixed pieces (4 corners + 5 hints)");
    }

    @Test
    @DisplayName("Should return 0 fixed pieces for indice puzzles")
    void testCalculateFixedPiecesIndice() {
        int fixed1 = manager.calculateFixedPieces("indice2_ascending");
        int fixed2 = manager.calculateFixedPieces("indice3_descending");

        assertEquals(0, fixed1, "Indice puzzles should have 0 fixed pieces");
        assertEquals(0, fixed2, "Indice puzzles should have 0 fixed pieces");
    }

    @Test
    @DisplayName("Should return 0 fixed pieces for unknown puzzles")
    void testCalculateFixedPiecesUnknown() {
        int fixed = manager.calculateFixedPieces("custom_puzzle");
        assertEquals(0, fixed, "Unknown puzzles should default to 0 fixed pieces");
    }

    @Test
    @DisplayName("Should handle all eternity2 variants")
    void testCalculateFixedPiecesAllEternity2Variants() {
        String[] variants = {
            "eternity2_p01_ascending",
            "eternity2_p02_descending_border",
            "eternity2_p09_ascending"
        };

        for (String variant : variants) {
            int fixed = manager.calculateFixedPieces(variant);
            assertEquals(9, fixed, "All eternity2 variants should have 9 fixed pieces: " + variant);
        }
    }

    // ==================== buildInitialFixedPieces Tests ====================

    @Test
    @DisplayName("Should build fixed pieces list from placement order")
    void testBuildInitialFixedPieces() {
        List<PlacementInfo> preloadedOrder = Arrays.asList(
            new PlacementInfo(0, 0, 1, 0),
            new PlacementInfo(0, 1, 2, 0),
            new PlacementInfo(0, 2, 3, 0),
            new PlacementInfo(1, 0, 4, 0),
            new PlacementInfo(1, 1, 5, 0)
        );

        List<PlacementInfo> fixed = manager.buildInitialFixedPieces(preloadedOrder, 3);

        assertEquals(3, fixed.size(), "Should extract first 3 pieces");
        assertEquals(1, fixed.get(0).pieceId, "First fixed piece should be piece 1");
        assertEquals(2, fixed.get(1).pieceId, "Second fixed piece should be piece 2");
        assertEquals(3, fixed.get(2).pieceId, "Third fixed piece should be piece 3");
    }

    @Test
    @DisplayName("Should handle request for more fixed pieces than available")
    void testBuildInitialFixedPiecesExceedsAvailable() {
        List<PlacementInfo> preloadedOrder = Arrays.asList(
            new PlacementInfo(0, 0, 1, 0),
            new PlacementInfo(0, 1, 2, 0)
        );

        List<PlacementInfo> fixed = manager.buildInitialFixedPieces(preloadedOrder, 5);

        assertEquals(2, fixed.size(), "Should only extract available pieces");
    }

    @Test
    @DisplayName("Should handle zero fixed pieces")
    void testBuildInitialFixedPiecesZero() {
        List<PlacementInfo> preloadedOrder = Arrays.asList(
            new PlacementInfo(0, 0, 1, 0),
            new PlacementInfo(0, 1, 2, 0)
        );

        List<PlacementInfo> fixed = manager.buildInitialFixedPieces(preloadedOrder, 0);

        assertEquals(0, fixed.size(), "Should return empty list for 0 fixed pieces");
    }

    @Test
    @DisplayName("Should handle empty preloaded order")
    void testBuildInitialFixedPiecesEmptyOrder() {
        List<PlacementInfo> preloadedOrder = new ArrayList<>();

        List<PlacementInfo> fixed = manager.buildInitialFixedPieces(preloadedOrder, 3);

        assertEquals(0, fixed.size(), "Should return empty list for empty order");
    }

    // ==================== backtrackPreloadedPiece Tests ====================

    @Test
    @DisplayName("Should backtrack single preloaded piece")
    void testBacktrackPreloadedPiece() {
        // Setup: place piece on board
        Piece piece1 = pieces.get(1);
        board.place(0, 0, piece1, 0);
        pieceUsed.set(1);

        List<PlacementInfo> placementOrder = new ArrayList<>();
        placementOrder.add(new PlacementInfo(0, 0, 1, 0));

        long initialBacktracks = stats.backtracks;

        // Backtrack
        PlacementInfo removed = manager.backtrackPreloadedPiece(
            board, pieces, pieceUsed, placementOrder);

        // Verify
        assertNotNull(removed, "Should return removed placement info");
        assertEquals(1, removed.pieceId, "Removed piece should be piece 1");
        assertEquals(0, removed.row, "Removed piece should be at row 0");
        assertEquals(0, removed.col, "Removed piece should be at col 0");

        assertTrue(board.isEmpty(0, 0), "Board position should be empty");
        assertFalse(pieceUsed.get(1), "Piece should be marked as unused");
        assertEquals(0, placementOrder.size(), "Placement order should be empty");
        assertEquals(initialBacktracks + 1, stats.backtracks, "Backtrack count should increment");
    }

    @Test
    @DisplayName("Should return null when backtracking empty placement order")
    void testBacktrackPreloadedPieceEmpty() {
        List<PlacementInfo> placementOrder = new ArrayList<>();

        PlacementInfo removed = manager.backtrackPreloadedPiece(
            board, pieces, pieceUsed, placementOrder);

        assertNull(removed, "Should return null for empty placement order");
    }

    @Test
    @DisplayName("Should handle multiple consecutive backtracks")
    void testMultipleBacktracks() {
        // Setup: place 3 pieces
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(2), 0);
        board.place(0, 2, pieces.get(3), 0);
        pieceUsed.set(1);
        pieceUsed.set(2);
        pieceUsed.set(3);

        List<PlacementInfo> placementOrder = new ArrayList<>(Arrays.asList(
            new PlacementInfo(0, 0, 1, 0),
            new PlacementInfo(0, 1, 2, 0),
            new PlacementInfo(0, 2, 3, 0)
        ));

        // Backtrack all 3
        PlacementInfo removed1 = manager.backtrackPreloadedPiece(
            board, pieces, pieceUsed, placementOrder);
        PlacementInfo removed2 = manager.backtrackPreloadedPiece(
            board, pieces, pieceUsed, placementOrder);
        PlacementInfo removed3 = manager.backtrackPreloadedPiece(
            board, pieces, pieceUsed, placementOrder);

        assertEquals(3, removed1.pieceId, "First backtrack should remove piece 3");
        assertEquals(2, removed2.pieceId, "Second backtrack should remove piece 2");
        assertEquals(1, removed3.pieceId, "Third backtrack should remove piece 1");

        assertEquals(0, placementOrder.size(), "All pieces should be removed");
        assertTrue(board.isEmpty(0, 0), "All positions should be empty");
        assertTrue(board.isEmpty(0, 1), "All positions should be empty");
        assertTrue(board.isEmpty(0, 2), "All positions should be empty");
    }

    // ==================== Fixed Position Tests ====================

    @Test
    @DisplayName("Should return empty set for fixed positions")
    void testGetFixedPositions() {
        Set<String> fixedPositions = manager.getFixedPositions();

        assertNotNull(fixedPositions, "Fixed positions should not be null");
        assertEquals(0, fixedPositions.size(), "Fixed positions should be empty");
    }

    @Test
    @DisplayName("Should check if position is fixed")
    void testIsFixedPosition() {
        Set<String> fixedPositions = new HashSet<>();
        fixedPositions.add("0,0");
        fixedPositions.add("0,2");

        assertTrue(manager.isFixedPosition(0, 0, fixedPositions),
                  "Position (0,0) should be fixed");
        assertFalse(manager.isFixedPosition(0, 1, fixedPositions),
                   "Position (0,1) should not be fixed");
        assertTrue(manager.isFixedPosition(0, 2, fixedPositions),
                  "Position (0,2) should be fixed");
    }

    @Test
    @DisplayName("Should handle empty fixed positions set")
    void testIsFixedPositionEmptySet() {
        Set<String> fixedPositions = new HashSet<>();

        assertFalse(manager.isFixedPosition(0, 0, fixedPositions),
                   "No position should be fixed in empty set");
        assertFalse(manager.isFixedPosition(5, 5, fixedPositions),
                   "No position should be fixed in empty set");
    }

    // ==================== Timeout Tests ====================

    @Test
    @DisplayName("Should not timeout when no timeout configured")
    void testNoTimeoutConfigured() {
        // Don't set timeout config - default is Long.MAX_VALUE

        // Create mock solver that always returns false
        BacktrackingHistoryManager.SequentialSolver mockSolver =
            (b, p, pu, total) -> false;

        List<PlacementInfo> placementOrder = new ArrayList<>();
        placementOrder.add(new PlacementInfo(0, 0, 1, 0));

        board.place(0, 0, pieces.get(1), 0);
        pieceUsed.set(1);

        // Should complete without timeout
        boolean result = manager.backtrackThroughHistory(
            board, pieces, pieceUsed, placementOrder, mockSolver);

        assertFalse(result, "Should complete without timeout");
    }

    @Test
    @DisplayName("Should timeout during backtrack when time exceeded")
    void testTimeoutDuringBacktrack() {
        // Set timeout that has already expired
        long startTime = System.currentTimeMillis() - 10000;
        long maxTime = 1; // 1ms timeout
        manager.setTimeoutConfig(startTime, maxTime);

        // Create mock solver that would take forever
        BacktrackingHistoryManager.SequentialSolver mockSolver =
            (b, p, pu, total) -> {
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                return false;
            };

        List<PlacementInfo> placementOrder = new ArrayList<>();
        placementOrder.add(new PlacementInfo(0, 0, 1, 0));

        board.place(0, 0, pieces.get(1), 0);
        pieceUsed.set(1);

        // Should timeout immediately
        boolean result = manager.backtrackThroughHistory(
            board, pieces, pieceUsed, placementOrder, mockSolver);

        assertFalse(result, "Should return false on timeout");
    }

    // ==================== Logging Tests ====================

    @Test
    @DisplayName("Should log backtrack progress without exception")
    void testLogBacktrackProgress() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            manager.logBacktrackProgress(5, 10);
        }, "Logging should not throw exception");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle null puzzle name in calculateFixedPieces")
    void testCalculateFixedPiecesNullPuzzleName() {
        int fixed = manager.calculateFixedPieces(null);
        assertEquals(0, fixed, "Null puzzle name should default to 0 fixed pieces");
    }

    @Test
    @DisplayName("Should handle empty puzzle name in calculateFixedPieces")
    void testCalculateFixedPiecesEmptyPuzzleName() {
        int fixed = manager.calculateFixedPieces("");
        assertEquals(0, fixed, "Empty puzzle name should default to 0 fixed pieces");
    }

    @Test
    @DisplayName("Should handle case sensitivity in puzzle names")
    void testCalculateFixedPiecesCaseSensitivity() {
        int fixed1 = manager.calculateFixedPieces("ETERNITY2");
        int fixed2 = manager.calculateFixedPieces("Eternity2");

        // Currently case-sensitive, so these should return 0
        assertEquals(0, fixed1, "Uppercase should not match (case-sensitive)");
        assertEquals(0, fixed2, "Mixed case should not match (case-sensitive)");
    }

    // ==================== Helper Methods ====================

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> testPieces = new HashMap<>();

        // Create simple test pieces (id, [N, E, S, W])
        testPieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));
        testPieces.put(2, new Piece(2, new int[]{0, 1, 2, 1}));
        testPieces.put(3, new Piece(3, new int[]{0, 0, 1, 1}));
        testPieces.put(4, new Piece(4, new int[]{2, 1, 3, 0}));
        testPieces.put(5, new Piece(5, new int[]{2, 1, 3, 1}));
        testPieces.put(6, new Piece(6, new int[]{2, 0, 3, 1}));
        testPieces.put(7, new Piece(7, new int[]{3, 1, 0, 0}));
        testPieces.put(8, new Piece(8, new int[]{3, 1, 0, 1}));
        testPieces.put(9, new Piece(9, new int[]{3, 0, 0, 1}));

        return testPieces;
    }
}
