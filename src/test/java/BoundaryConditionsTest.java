import model.Board;
import model.Piece;
import model.Placement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solver.DomainManager;
import solver.StatisticsManager;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive boundary condition tests for the Eternity solver.
 * Tests min/max values, empty/full boards, edge cases with 0 pieces, 256 pieces, etc.
 */
@DisplayName("Boundary Conditions Tests")
class BoundaryConditionsTest {

    // ==================== Board Size Boundaries ====================

    @Test
    @DisplayName("Should create board with minimum valid size (1x1)")
    void testMinimumBoardSize() {
        Board board = new Board(1, 1);
        assertEquals(1, board.getRows());
        assertEquals(1, board.getCols());
        assertTrue(board.isEmpty(0, 0));
    }

    @Test
    @DisplayName("Should reject board with zero rows")
    void testZeroRows() {
        assertThrows(IllegalArgumentException.class, () -> new Board(0, 1));
    }

    @Test
    @DisplayName("Should reject board with zero columns")
    void testZeroColumns() {
        assertThrows(IllegalArgumentException.class, () -> new Board(1, 0));
    }

    @Test
    @DisplayName("Should reject board with negative rows")
    void testNegativeRows() {
        assertThrows(IllegalArgumentException.class, () -> new Board(-1, 5));
        assertThrows(IllegalArgumentException.class, () -> new Board(-100, 5));
        assertThrows(IllegalArgumentException.class, () -> new Board(Integer.MIN_VALUE, 5));
    }

    @Test
    @DisplayName("Should reject board with negative columns")
    void testNegativeColumns() {
        assertThrows(IllegalArgumentException.class, () -> new Board(5, -1));
        assertThrows(IllegalArgumentException.class, () -> new Board(5, -100));
        assertThrows(IllegalArgumentException.class, () -> new Board(5, Integer.MIN_VALUE));
    }

    @Test
    @DisplayName("Should handle very large board dimensions")
    void testVeryLargeBoard() {
        // Test 100x100 board (10,000 cells)
        Board board = new Board(100, 100);
        assertEquals(100, board.getRows());
        assertEquals(100, board.getCols());

        // Test corner access
        assertTrue(board.isEmpty(0, 0));
        assertTrue(board.isEmpty(99, 99));
    }

    @Test
    @DisplayName("Should handle non-square boards")
    void testNonSquareBoards() {
        Board tall = new Board(100, 1);
        assertEquals(100, tall.getRows());
        assertEquals(1, tall.getCols());

        Board wide = new Board(1, 100);
        assertEquals(1, wide.getRows());
        assertEquals(100, wide.getCols());
    }

    @Test
    @DisplayName("Should handle maximum practical board size (256x256)")
    void testMaximumPracticalBoardSize() {
        Board board = new Board(256, 256);
        assertEquals(256, board.getRows());
        assertEquals(256, board.getCols());
    }

    // ==================== Piece ID Boundaries ====================

    @Test
    @DisplayName("Should handle piece with ID 0")
    void testPieceIdZero() {
        Piece piece = new Piece(0, new int[]{0, 1, 2, 3});
        assertEquals(0, piece.getId());
    }

    @Test
    @DisplayName("Should handle piece with negative ID")
    void testPieceIdNegative() {
        Piece piece = new Piece(-1, new int[]{0, 1, 2, 3});
        assertEquals(-1, piece.getId());

        Piece piece2 = new Piece(Integer.MIN_VALUE, new int[]{0, 1, 2, 3});
        assertEquals(Integer.MIN_VALUE, piece2.getId());
    }

    @Test
    @DisplayName("Should handle piece with maximum integer ID")
    void testPieceIdMaximum() {
        Piece piece = new Piece(Integer.MAX_VALUE, new int[]{0, 1, 2, 3});
        assertEquals(Integer.MAX_VALUE, piece.getId());
    }

    @Test
    @DisplayName("Should handle edge values in piece edges")
    void testPieceEdgeValues() {
        Piece piece1 = new Piece(1, new int[]{0, 0, 0, 0});
        assertArrayEquals(new int[]{0, 0, 0, 0}, piece1.getEdges());

        Piece piece2 = new Piece(2, new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE});
        assertArrayEquals(new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}, piece2.getEdges());

        Piece piece3 = new Piece(3, new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE});
        assertArrayEquals(new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE}, piece3.getEdges());
    }

    // ==================== Rotation Boundaries ====================

    @Test
    @DisplayName("Should handle rotation value 0")
    void testRotationZero() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
        board.place(0, 0, piece, 0);

        Placement placement = board.getPlacement(0, 0);
        assertEquals(0, placement.getRotation());
    }

    @Test
    @DisplayName("Should handle rotation value 3")
    void testRotationThree() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
        board.place(0, 0, piece, 3);

        Placement placement = board.getPlacement(0, 0);
        assertEquals(3, placement.getRotation());
    }

    @Test
    @DisplayName("Should handle large rotation values (modulo behavior)")
    void testLargeRotationValues() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Test rotation % 4 behavior
        int[] rot4 = piece.edgesRotated(4);
        int[] rot0 = piece.edgesRotated(0);
        assertArrayEquals(rot0, rot4);

        int[] rot5 = piece.edgesRotated(5);
        int[] rot1 = piece.edgesRotated(1);
        assertArrayEquals(rot1, rot5);

        int[] rot100 = piece.edgesRotated(100);
        assertArrayEquals(rot0, rot100);
    }

    @Test
    @DisplayName("Should handle negative rotation values")
    void testNegativeRotationValues() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // -1 should be equivalent to 3
        int[] rotMinus1 = piece.edgesRotated(-1);
        int[] rot3 = piece.edgesRotated(3);
        assertArrayEquals(rot3, rotMinus1);

        // -4 should be equivalent to 0
        int[] rotMinus4 = piece.edgesRotated(-4);
        int[] rot0 = piece.edgesRotated(0);
        assertArrayEquals(rot0, rotMinus4);
    }

    // ==================== Empty Board Edge Cases ====================

    @Test
    @DisplayName("Should calculate score for empty board")
    void testEmptyBoardScore() {
        Board board = new Board(3, 3);
        int[] score = board.calculateScore();
        assertEquals(0, score[0]);
        assertTrue(score[1] > 0); // Max score should be positive
    }

    @Test
    @DisplayName("Should calculate score for empty 1x1 board")
    void testEmpty1x1BoardScore() {
        Board board = new Board(1, 1);
        int[] score = board.calculateScore();
        assertEquals(0, score[0]);
        assertEquals(0, score[1]); // No internal edges on 1x1
    }

    @Test
    @DisplayName("Should handle operations on empty board")
    void testEmptyBoardOperations() {
        Board board = new Board(5, 5);

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                assertTrue(board.isEmpty(r, c));
                assertNull(board.getPlacement(r, c));
            }
        }
    }

    // ==================== Full Board Edge Cases ====================

    @Test
    @DisplayName("Should handle fully filled board")
    void testFullyFilledBoard() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Fill entire board
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                board.place(r, c, piece, 0);
            }
        }

        // Verify all cells filled
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                assertFalse(board.isEmpty(r, c));
                assertNotNull(board.getPlacement(r, c));
            }
        }
    }

    @Test
    @DisplayName("Should handle filling and emptying large board")
    void testFillAndEmptyLargeBoard() {
        Board board = new Board(16, 16);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Fill
        for (int r = 0; r < 16; r++) {
            for (int c = 0; c < 16; c++) {
                board.place(r, c, piece, r % 4);
            }
        }

        // Empty
        for (int r = 0; r < 16; r++) {
            for (int c = 0; c < 16; c++) {
                board.remove(r, c);
            }
        }

        // Verify all empty
        for (int r = 0; r < 16; r++) {
            for (int c = 0; c < 16; c++) {
                assertTrue(board.isEmpty(r, c));
            }
        }
    }

    // ==================== Coordinate Boundaries ====================

    @Test
    @DisplayName("Should reject negative coordinates")
    void testNegativeCoordinates() {
        Board board = new Board(3, 3);

        assertThrows(IndexOutOfBoundsException.class, () -> board.isEmpty(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.isEmpty(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> board.getPlacement(-1, -1));
    }

    @Test
    @DisplayName("Should reject coordinates at board boundaries")
    void testCoordinatesAtBoundary() {
        Board board = new Board(3, 3);

        assertThrows(IndexOutOfBoundsException.class, () -> board.isEmpty(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.isEmpty(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> board.isEmpty(3, 3));
    }

    @Test
    @DisplayName("Should accept coordinates at valid boundaries")
    void testValidBoundaryCoordinates() {
        Board board = new Board(3, 3);

        // All corners should be valid
        assertTrue(board.isEmpty(0, 0));
        assertTrue(board.isEmpty(0, 2));
        assertTrue(board.isEmpty(2, 0));
        assertTrue(board.isEmpty(2, 2));
    }

    // ==================== BitSet Boundaries ====================

    @Test
    @DisplayName("Should handle empty BitSet")
    void testEmptyBitSet() {
        BitSet pieceUsed = new BitSet();
        assertFalse(pieceUsed.get(0));
        assertFalse(pieceUsed.get(1));
        assertFalse(pieceUsed.get(100));
    }

    @Test
    @DisplayName("Should handle BitSet with all pieces used")
    void testAllPiecesUsed() {
        BitSet pieceUsed = new BitSet(256);
        for (int i = 1; i <= 256; i++) {
            pieceUsed.set(i);
        }

        for (int i = 1; i <= 256; i++) {
            assertTrue(pieceUsed.get(i));
        }
    }

    @Test
    @DisplayName("Should handle BitSet with large indices")
    void testLargeBitSetIndices() {
        BitSet pieceUsed = new BitSet();
        pieceUsed.set(10000);
        assertTrue(pieceUsed.get(10000));
        assertFalse(pieceUsed.get(9999));
        assertFalse(pieceUsed.get(10001));
    }

    // ==================== Statistics Manager Boundaries ====================

    @Test
    @DisplayName("Should handle statistics with zero values")
    void testStatisticsZeroValues() {
        StatisticsManager stats = new StatisticsManager();

        assertEquals(0, stats.recursiveCalls);
        assertEquals(0, stats.placements);
        assertEquals(0, stats.backtracks);
        assertEquals(0, stats.singletonsFound);
        assertEquals(0, stats.singletonsPlaced);
        assertEquals(0, stats.deadEndsDetected);
    }

    @Test
    @DisplayName("Should handle statistics with maximum long values")
    void testStatisticsMaxValues() {
        StatisticsManager stats = new StatisticsManager();

        stats.recursiveCalls = Long.MAX_VALUE;
        stats.placements = Long.MAX_VALUE;
        stats.backtracks = Long.MAX_VALUE;

        assertEquals(Long.MAX_VALUE, stats.recursiveCalls);
        assertEquals(Long.MAX_VALUE, stats.placements);
        assertEquals(Long.MAX_VALUE, stats.backtracks);
    }

    @Test
    @DisplayName("Should handle elapsed time at boundaries")
    void testElapsedTimeBoundaries() {
        StatisticsManager stats = new StatisticsManager();

        // Immediate elapsed time (should be ~0)
        stats.start();
        long elapsed = stats.getElapsedTimeMs();
        assertTrue(elapsed >= 0 && elapsed < 100);
    }

    @Test
    @DisplayName("Should handle negative previous time offset")
    void testNegativePreviousTimeOffset() {
        StatisticsManager stats = new StatisticsManager();
        stats.previousTimeOffset = -1000;
        stats.start();

        // Should handle negative offset
        long elapsed = stats.getElapsedTimeMs();
        assertTrue(elapsed <= 100); // Should be close to negative offset
    }

    // ==================== DomainManager Boundaries ====================

    @Test
    @DisplayName("Should handle empty domain computation")
    void testEmptyDomainComputation() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>();
        BitSet pieceUsed = new BitSet();

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> false;
        DomainManager manager = new DomainManager(fitChecker);

        List<DomainManager.ValidPlacement> domain = manager.computeDomain(board, 0, 0, pieces, pieceUsed, 0);
        assertTrue(domain.isEmpty());
    }

    @Test
    @DisplayName("Should handle domain with zero pieces")
    void testDomainWithZeroPieces() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>();
        BitSet pieceUsed = new BitSet();

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        List<DomainManager.ValidPlacement> domain = manager.computeDomain(board, 0, 0, pieces, pieceUsed, 0);
        assertTrue(domain.isEmpty());
    }

    @Test
    @DisplayName("Should handle domain with 256 pieces")
    void testDomainWith256Pieces() {
        Board board = new Board(16, 16);
        Map<Integer, Piece> pieces = new HashMap<>();

        // Create 256 pieces
        for (int i = 1; i <= 256; i++) {
            pieces.put(i, new Piece(i, new int[]{0, 1, 2, 0}));
        }

        BitSet pieceUsed = new BitSet(257);
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        List<DomainManager.ValidPlacement> domain = manager.computeDomain(board, 0, 0, pieces, pieceUsed, 256);
        assertFalse(domain.isEmpty());
        // Each piece has up to 4 rotations, but corner pieces need specific edges
    }

    // ==================== Score Calculation Boundaries ====================

    @Test
    @DisplayName("Should calculate maximum score for perfect 2x2 board")
    void testPerfect2x2Score() {
        Board board = new Board(2, 2);
        int[] score = board.calculateScore();

        // 2x2 has 4 internal edges: (0,0)-(0,1), (0,0)-(1,0), (0,1)-(1,1), (1,0)-(1,1)
        assertEquals(4, score[1]); // Max score
    }

    @Test
    @DisplayName("Should calculate maximum score for 16x16 board")
    void testMaxScore16x16() {
        Board board = new Board(16, 16);
        int[] score = board.calculateScore();

        // 16x16: horizontal = 15*16 = 240, vertical = 16*15 = 240, total = 480
        assertEquals(480, score[1]);
    }

    @Test
    @DisplayName("Should handle score for board with single row")
    void testScoreSingleRow() {
        Board board = new Board(1, 10);
        int[] score = board.calculateScore();

        // Single row: 9 horizontal internal edges, 0 vertical
        assertEquals(9, score[1]);
    }

    @Test
    @DisplayName("Should handle score for board with single column")
    void testScoreSingleColumn() {
        Board board = new Board(10, 1);
        int[] score = board.calculateScore();

        // Single column: 0 horizontal, 9 vertical internal edges
        assertEquals(9, score[1]);
    }

    // ==================== Piece Edge Cases ====================

    @Test
    @DisplayName("Should reject piece with too few edges")
    void testPieceTooFewEdges() {
        assertThrows(IllegalArgumentException.class, () -> new Piece(1, new int[]{0, 1, 2}));
        assertThrows(IllegalArgumentException.class, () -> new Piece(1, new int[]{0}));
        assertThrows(IllegalArgumentException.class, () -> new Piece(1, new int[]{}));
    }

    @Test
    @DisplayName("Should reject piece with too many edges")
    void testPieceTooManyEdges() {
        assertThrows(IllegalArgumentException.class, () -> new Piece(1, new int[]{0, 1, 2, 3, 4}));
        assertThrows(IllegalArgumentException.class, () -> new Piece(1, new int[]{0, 1, 2, 3, 4, 5, 6, 7}));
    }

    @Test
    @DisplayName("Should handle piece with all identical edges")
    void testPieceAllIdenticalEdges() {
        Piece piece = new Piece(1, new int[]{5, 5, 5, 5});
        assertEquals(1, piece.getUniqueRotationCount());

        // All rotations should be identical
        assertArrayEquals(piece.edgesRotated(0), piece.edgesRotated(1));
        assertArrayEquals(piece.edgesRotated(0), piece.edgesRotated(2));
        assertArrayEquals(piece.edgesRotated(0), piece.edgesRotated(3));
    }

    @Test
    @DisplayName("Should handle piece with two-fold symmetry")
    void testPieceTwoFoldSymmetry() {
        Piece piece = new Piece(1, new int[]{1, 2, 1, 2});
        assertEquals(2, piece.getUniqueRotationCount());

        // Rotation 0 and 2 should be identical
        assertArrayEquals(piece.edgesRotated(0), piece.edgesRotated(2));
        // Rotation 1 and 3 should be identical
        assertArrayEquals(piece.edgesRotated(1), piece.edgesRotated(3));
    }

    @Test
    @DisplayName("Should handle piece with no symmetry")
    void testPieceNoSymmetry() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
        assertEquals(4, piece.getUniqueRotationCount());
    }
}
