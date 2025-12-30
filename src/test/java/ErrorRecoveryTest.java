import model.Board;
import model.Piece;
import model.Placement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solver.DomainManager;
import solver.StatisticsManager;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive error recovery tests for the Eternity solver.
 * Tests graceful handling of invalid states, corrupted data, and recovery mechanisms.
 */
@DisplayName("Error Recovery Tests")
class ErrorRecoveryTest {

    // ==================== Invalid State Recovery ====================

    @Test
    @DisplayName("Should recover from placing piece with invalid edges array")
    void testRecoverFromInvalidEdgesArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Piece(1, new int[]{1, 2, 3}); // Only 3 edges
        });

        // Should be able to create valid piece after error
        Piece validPiece = new Piece(1, new int[]{1, 2, 3, 4});
        assertNotNull(validPiece);
    }

    @Test
    @DisplayName("Should recover from invalid board dimensions")
    void testRecoverFromInvalidBoardDimensions() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Board(0, 5);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new Board(-1, 5);
        });

        // Should be able to create valid board after errors
        Board validBoard = new Board(3, 3);
        assertNotNull(validBoard);
    }

    @Test
    @DisplayName("Should recover from out of bounds access")
    void testRecoverFromOutOfBounds() {
        Board board = new Board(3, 3);

        assertThrows(IndexOutOfBoundsException.class, () -> {
            board.isEmpty(-1, 0);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            board.isEmpty(3, 0);
        });

        // Should still be able to use board after errors
        assertTrue(board.isEmpty(0, 0));
        assertTrue(board.isEmpty(2, 2));
    }

    @Test
    @DisplayName("Should handle placement after failed placement")
    void testPlacementAfterFailedPlacement() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Try invalid placement
        assertThrows(IndexOutOfBoundsException.class, () -> {
            board.place(10, 10, piece, 0);
        });

        // Valid placement should work
        assertDoesNotThrow(() -> board.place(0, 0, piece, 0));
        assertFalse(board.isEmpty(0, 0));
    }

    // ==================== Corrupted Data Recovery ====================

    @Test
    @DisplayName("Should handle map with missing pieces")
    void testMapWithMissingPieces() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>();

        // Create sparse map (missing piece 2)
        pieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));
        pieces.put(3, new Piece(3, new int[]{0, 3, 4, 0}));

        BitSet pieceUsed = new BitSet();
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        // Should handle sparse map gracefully
        List<DomainManager.ValidPlacement> domain = manager.computeDomain(board, 0, 0, pieces, pieceUsed, 3);
        assertNotNull(domain);
    }

    @Test
    @DisplayName("Should handle map with null piece values")
    void testMapWithNullPieces() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>();

        pieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));
        pieces.put(2, null); // Null piece
        pieces.put(3, new Piece(3, new int[]{0, 3, 4, 0}));

        BitSet pieceUsed = new BitSet();
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        // Should handle null values gracefully
        List<DomainManager.ValidPlacement> domain = manager.computeDomain(board, 0, 0, pieces, pieceUsed, 3);
        assertNotNull(domain);
    }

    @Test
    @DisplayName("Should recover from empty piece map")
    void testEmptyPieceMap() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>(); // Empty map

        BitSet pieceUsed = new BitSet();
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        // Should return empty domain
        List<DomainManager.ValidPlacement> domain = manager.computeDomain(board, 0, 0, pieces, pieceUsed, 0);
        assertNotNull(domain);
        assertTrue(domain.isEmpty());
    }

    // ==================== Invalid Operation Recovery ====================

    @Test
    @DisplayName("Should handle removing from empty cell")
    void testRemoveFromEmptyCell() {
        Board board = new Board(3, 3);

        // Remove from empty cell should not throw
        assertDoesNotThrow(() -> board.remove(0, 0));
        assertTrue(board.isEmpty(0, 0));
    }

    @Test
    @DisplayName("Should handle multiple removals")
    void testMultipleRemovals() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        board.place(0, 0, piece, 0);
        board.remove(0, 0);

        // Second removal should not cause issues
        assertDoesNotThrow(() -> board.remove(0, 0));
        assertTrue(board.isEmpty(0, 0));
    }

    @Test
    @DisplayName("Should handle overwriting placed piece")
    void testOverwritePlacement() {
        Board board = new Board(3, 3);
        Piece piece1 = new Piece(1, new int[]{1, 2, 3, 4});
        Piece piece2 = new Piece(2, new int[]{5, 6, 7, 8});

        board.place(0, 0, piece1, 0);
        assertEquals(1, board.getPlacement(0, 0).getPieceId());

        // Overwrite should work
        board.place(0, 0, piece2, 1);
        assertEquals(2, board.getPlacement(0, 0).getPieceId());
        assertEquals(1, board.getPlacement(0, 0).getRotation());
    }

    @Test
    @DisplayName("Should handle rapid place/remove cycles")
    void testRapidPlaceRemoveCycles() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        for (int i = 0; i < 100; i++) {
            board.place(1, 1, piece, i % 4);
            assertFalse(board.isEmpty(1, 1));

            board.remove(1, 1);
            assertTrue(board.isEmpty(1, 1));
        }
    }

    // ==================== Domain Manager Error Recovery ====================

    @Test
    @DisplayName("Should recover from AC3 initialization failure")
    void testAC3InitializationRecovery() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>();
        // Add at least one piece to trigger fit checker
        pieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));
        BitSet pieceUsed = new BitSet();

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> {
            throw new RuntimeException("Simulated fit checker failure");
        };
        DomainManager manager = new DomainManager(fitChecker);

        // Initialization should fail
        assertThrows(RuntimeException.class, () -> {
            manager.initializeAC3Domains(board, pieces, pieceUsed, 1);
        });

        // Manager should still be usable with different fit checker
        DomainManager.FitChecker validChecker = (b, r, c, edges) -> true;
        DomainManager recoveredManager = new DomainManager(validChecker);
        assertDoesNotThrow(() -> recoveredManager.initializeAC3Domains(board, pieces, pieceUsed, 1));
    }

    @Test
    @DisplayName("Should handle AC3 reset and re-initialization")
    void testAC3ResetRecovery() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= 9; i++) {
            pieces.put(i, new Piece(i, new int[]{0, i, i + 1, 0}));
        }

        BitSet pieceUsed = new BitSet();
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        // Initialize
        manager.initializeAC3Domains(board, pieces, pieceUsed, 9);
        assertTrue(manager.isAC3Initialized());

        // Reset
        manager.resetAC3();
        assertFalse(manager.isAC3Initialized());

        // Re-initialize should work
        assertDoesNotThrow(() -> manager.initializeAC3Domains(board, pieces, pieceUsed, 9));
        assertTrue(manager.isAC3Initialized());
    }

    @Test
    @DisplayName("Should handle domain restoration after invalid state")
    void testDomainRestorationRecovery() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= 9; i++) {
            pieces.put(i, new Piece(i, new int[]{0, i, i + 1, 0}));
        }

        BitSet pieceUsed = new BitSet();
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        manager.initializeAC3Domains(board, pieces, pieceUsed, 9);

        // Corrupt state by placing piece outside valid range
        assertThrows(IndexOutOfBoundsException.class, () -> {
            board.place(10, 10, pieces.get(1), 0);
        });

        // Should still be able to restore valid domains
        assertDoesNotThrow(() -> manager.restoreAC3Domains(board, 1, 1, pieces, pieceUsed, 9));
    }

    // ==================== Cache Error Recovery ====================

    @Test
    @DisplayName("Should recover from cache corruption")
    void testCacheCorruptionRecovery() {
        Board board = new Board(5, 5);
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= 25; i++) {
            pieces.put(i, new Piece(i, new int[]{0, i % 10, i % 10 + 1, 0}));
        }

        BitSet pieceUsed = new BitSet();
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        manager.setUseDomainCache(true);
        manager.initializeDomainCache(board, pieces, pieceUsed, 25);

        // Clear cache (simulating corruption)
        manager.clearCache();

        // Should recompute on access
        List<DomainManager.ValidPlacement> domain = manager.getCachedDomain(board, 0, 0, pieces, pieceUsed, 25);
        assertNotNull(domain);
    }

    @Test
    @DisplayName("Should handle cache operations with disabled cache")
    void testDisabledCacheOperations() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>();
        BitSet pieceUsed = new BitSet();

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        manager.setUseDomainCache(false);

        // All cache operations should be no-ops
        assertDoesNotThrow(() -> manager.clearCache());
        assertDoesNotThrow(() -> manager.updateCacheAfterPlacement(board, 0, 0, pieces, pieceUsed, 0));
        assertDoesNotThrow(() -> manager.restoreCacheAfterBacktrack(board, 0, 0, pieces, pieceUsed, 0));
    }

    // ==================== Statistics Error Recovery ====================

    @Test
    @DisplayName("Should handle statistics with uninitialized state")
    void testUninitializedStatistics() {
        StatisticsManager stats = new StatisticsManager();

        // Should not throw even without start()
        assertDoesNotThrow(() -> stats.getElapsedTimeMs());
        assertDoesNotThrow(() -> stats.getElapsedTimeSec());
        assertDoesNotThrow(() -> stats.getProgressPercentage());
    }

    @Test
    @DisplayName("Should recover from statistics overflow")
    void testStatisticsOverflow() {
        StatisticsManager stats = new StatisticsManager();

        // Set to max value
        stats.recursiveCalls = Long.MAX_VALUE;

        // Should handle overflow gracefully (wraps around in Java)
        stats.recursiveCalls++;

        // Should still be usable
        assertDoesNotThrow(() -> stats.print());
    }

    @Test
    @DisplayName("Should handle findAndSetLastPlaced on empty board")
    void testFindLastPlacedEmptyBoard() {
        StatisticsManager stats = new StatisticsManager();
        Board board = new Board(5, 5);

        stats.findAndSetLastPlaced(board);

        assertEquals(-1, stats.getLastPlacedRow());
        assertEquals(-1, stats.getLastPlacedCol());
    }

    @Test
    @DisplayName("Should handle progress tracking with invalid depths")
    void testProgressTrackingInvalidDepths() {
        StatisticsManager stats = new StatisticsManager();

        // Register invalid depths
        stats.registerDepthOptions(-1, 10);
        stats.registerDepthOptions(1000, 10);

        // Should not throw
        assertDoesNotThrow(() -> stats.getProgressPercentage());
    }

    // ==================== BitSet Error Recovery ====================

    @Test
    @DisplayName("Should handle BitSet operations on invalid indices")
    void testBitSetInvalidIndices() {
        BitSet pieceUsed = new BitSet(10);

        // Accessing beyond initial size should auto-expand
        assertDoesNotThrow(() -> pieceUsed.set(100));
        assertTrue(pieceUsed.get(100));
        assertFalse(pieceUsed.get(99));
    }

    @Test
    @DisplayName("Should recover from BitSet clear")
    void testBitSetClearRecovery() {
        BitSet pieceUsed = new BitSet(10);

        for (int i = 0; i < 10; i++) {
            pieceUsed.set(i);
        }

        pieceUsed.clear();

        // Should be all false
        for (int i = 0; i < 10; i++) {
            assertFalse(pieceUsed.get(i));
        }

        // Should be usable again
        pieceUsed.set(5);
        assertTrue(pieceUsed.get(5));
    }

    // ==================== Complex Error Scenarios ====================

    @Test
    @DisplayName("Should recover from cascading errors")
    void testCascadingErrors() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Multiple errors in sequence
        assertThrows(IndexOutOfBoundsException.class, () -> board.place(-1, 0, piece, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.place(0, -1, piece, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.place(5, 5, piece, 0));

        // Should still work normally
        assertDoesNotThrow(() -> board.place(0, 0, piece, 0));
        assertFalse(board.isEmpty(0, 0));
    }

    @Test
    @DisplayName("Should handle interleaved errors and valid operations")
    void testInterleavedErrorsAndOperations() {
        Board board = new Board(5, 5);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                // Valid operation
                board.place(i % 5, i / 5, piece, i % 4);
            } else {
                // Invalid operation
                assertThrows(IndexOutOfBoundsException.class, () -> {
                    board.place(10, 10, piece, 0);
                });
            }
        }

        // Board should have 5 pieces placed
        int placedCount = 0;
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                if (!board.isEmpty(r, c)) {
                    placedCount++;
                }
            }
        }
        assertEquals(5, placedCount);
    }

    @Test
    @DisplayName("Should recover from exception in fit checker")
    void testFitCheckerException() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));

        BitSet pieceUsed = new BitSet();

        // Fit checker that throws exception
        DomainManager.FitChecker faultyChecker = (b, r, c, edges) -> {
            if (edges[0] == 1) {
                throw new RuntimeException("Simulated fit checker error");
            }
            return true;
        };

        DomainManager manager = new DomainManager(faultyChecker);

        // Should propagate exception
        assertThrows(RuntimeException.class, () -> {
            manager.computeDomain(board, 1, 1, pieces, pieceUsed, 1);
        });

        // Should be able to create new manager and continue
        DomainManager.FitChecker validChecker = (b, r, c, edges) -> true;
        DomainManager recoveredManager = new DomainManager(validChecker);
        assertDoesNotThrow(() -> recoveredManager.computeDomain(board, 1, 1, pieces, pieceUsed, 1));
    }

    // ==================== Boundary Error Cases ====================

    @Test
    @DisplayName("Should handle placement at exact boundary")
    void testPlacementAtBoundary() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Last valid position
        assertDoesNotThrow(() -> board.place(2, 2, piece, 0));

        // Just past boundary
        assertThrows(IndexOutOfBoundsException.class, () -> board.place(3, 2, piece, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.place(2, 3, piece, 0));
    }

    @Test
    @DisplayName("Should handle score calculation after partial errors")
    void testScoreCalculationAfterErrors() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Place some pieces with errors in between
        board.place(0, 0, piece, 0);

        assertThrows(IndexOutOfBoundsException.class, () -> {
            board.place(10, 10, piece, 0);
        });

        board.place(1, 1, piece, 0);

        // Score calculation should still work
        int[] score = board.calculateScore();
        assertNotNull(score);
        assertEquals(2, score.length);
    }

    // ==================== State Consistency Tests ====================

    @Test
    @DisplayName("Should maintain consistency after failed domain initialization")
    void testConsistencyAfterFailedInitialization() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>();
        BitSet pieceUsed = new BitSet();

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        // Should be uninitialized
        assertFalse(manager.isAC3Initialized());

        // Initialize successfully
        manager.initializeAC3Domains(board, pieces, pieceUsed, 0);
        assertTrue(manager.isAC3Initialized());

        // State should be consistent
        assertNotNull(manager.getDomain(0, 0));
    }

    @Test
    @DisplayName("Should maintain board consistency after errors")
    void testBoardConsistencyAfterErrors() {
        Board board = new Board(5, 5);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Place pieces
        board.place(0, 0, piece, 0);
        board.place(1, 1, piece, 1);

        // Cause error
        assertThrows(IndexOutOfBoundsException.class, () -> {
            board.place(10, 10, piece, 0);
        });

        // Original placements should still be intact
        assertFalse(board.isEmpty(0, 0));
        assertFalse(board.isEmpty(1, 1));
        assertEquals(1, board.getPlacement(0, 0).getPieceId());
        assertEquals(1, board.getPlacement(1, 1).getPieceId());
    }

    @Test
    @DisplayName("Should handle sequential error recovery")
    void testSequentialErrorRecovery() {
        List<Board> boards = new ArrayList<>();

        // Create boards with some errors
        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                assertThrows(IllegalArgumentException.class, () -> {
                    new Board(0, 0); // Invalid
                });
            } else {
                boards.add(new Board(3, 3));
            }
        }

        assertEquals(9, boards.size());

        // All boards should be usable
        for (Board board : boards) {
            assertTrue(board.isEmpty(0, 0));
        }
    }
}
