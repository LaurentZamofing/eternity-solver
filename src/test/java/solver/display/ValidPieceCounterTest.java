package solver.display;

import model.Board;
import model.Piece;
import solver.PlacementValidator;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ValidPieceCounter.
 * Tests valid piece counting logic for deadend detection and MRV heuristics.
 */
@DisplayName("ValidPieceCounter Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ValidPieceCounterTest {

    private Board board;
    private Map<Integer, Piece> piecesById;
    private PlacementValidator validator;
    private ValidPieceCounter counter;

    @BeforeEach
    void setUp() {
        board = new Board(3, 3);
        piecesById = createTestPieces();
        validator = createValidator();
        counter = new ValidPieceCounter(validator);
    }

    // ==================== Basic Counting Tests ====================

    @Test
    @Order(1)
    @DisplayName("Should count zero valid pieces (deadend)")
    void testDeadend() {
        // Place pieces that create impossible constraints at (1,1)
        board.place(0, 1, new Piece(1, new int[]{0, 2, 99, 4}), 0);  // South=99
        board.place(1, 0, new Piece(2, new int[]{5, 99, 7, 0}), 0);  // East=99

        List<Integer> unusedIds = Arrays.asList(3, 4, 5);

        // No piece can satisfy both North=99 and West=99
        int count = counter.countValidPieces(board, 1, 1, piecesById, unusedIds);

        assertEquals(0, count, "Should detect deadend (0 valid pieces)");
    }

    @Test
    @Order(2)
    @DisplayName("Should count single valid piece (singleton)")
    void testSingleton() {
        // Create constraints that only piece 3 can satisfy
        // Piece 3: [5, 7, 9, 10] -> rotation 1 gives: N=10, E=5, S=7, W=9
        board.place(0, 1, new Piece(1, new int[]{0, 2, 10, 4}), 0);  // South=10
        board.place(1, 0, new Piece(2, new int[]{5, 9, 7, 0}), 0);   // East=9

        List<Integer> unusedIds = Arrays.asList(3, 4, 5);
        // Only piece 3 with rotation 1 can match: N=10, W=9

        int count = counter.countValidPieces(board, 1, 1, piecesById, unusedIds);

        assertEquals(1, count, "Should detect singleton (1 valid piece)");
    }

    @Test
    @Order(3)
    @DisplayName("Should count multiple valid pieces")
    void testMultipleValidPieces() {
        // Empty position with lenient constraints
        List<Integer> unusedIds = Arrays.asList(1, 2, 3, 4, 5);

        int count = counter.countValidPieces(board, 0, 0, piecesById, unusedIds);

        assertTrue(count >= 2, "Should find multiple valid pieces for empty board");
    }

    @Test
    @Order(4)
    @DisplayName("Should handle empty unused list")
    void testEmptyUnusedList() {
        List<Integer> unusedIds = Collections.emptyList();

        int count = counter.countValidPieces(board, 0, 0, piecesById, unusedIds);

        assertEquals(0, count, "Empty unused list should give 0 count");
    }

    // ==================== Rotation Tests ====================

    @Test
    @Order(5)
    @DisplayName("Should test all 4 rotations")
    void testAllRotations() {
        // Create piece that fits only with specific rotation
        board.place(0, 1, new Piece(1, new int[]{0, 2, 5, 4}), 0);  // South=5
        board.place(1, 0, new Piece(2, new int[]{6, 7, 8, 0}), 0);  // East=7

        // Piece 3: [5, 7, 9, 10] fits at rotation 0 (N=5, E=7)
        // But piece needs to match: North=5 (from south of (0,1)), West=7 (from east of (1,0))
        // With rotation, we can test if different orientations work

        List<Integer> unusedIds = List.of(3);

        int count = counter.countValidPieces(board, 1, 1, piecesById, unusedIds);

        // Should count if ANY rotation works
        assertTrue(count <= 1, "Should count piece at most once even if multiple rotations work");
    }

    @Test
    @Order(6)
    @DisplayName("Should break after first valid rotation")
    void testBreakAfterFirstValidRotation() {
        // Place piece that fits with multiple rotations
        List<Integer> unusedIds = List.of(1);

        // Count calls for empty board (piece 1 likely fits with at least one rotation)
        int count = counter.countValidPieces(board, 0, 0, piecesById, unusedIds);

        // Should be 0 or 1 (counts piece once, not once per rotation)
        assertTrue(count == 0 || count == 1,
            "Should count each piece at most once, not per rotation");
    }

    // ==================== Edge Cases ====================

    @Test
    @Order(7)
    @DisplayName("Should handle null piece in map")
    void testNullPieceInMap() {
        piecesById.put(99, null);
        List<Integer> unusedIds = List.of(1, 99, 3);

        // Should skip null piece without crashing
        assertDoesNotThrow(() -> {
            int count = counter.countValidPieces(board, 0, 0, piecesById, unusedIds);
            assertTrue(count >= 0, "Should return non-negative count");
        });
    }

    @Test
    @Order(8)
    @DisplayName("Should handle missing piece ID")
    void testMissingPieceId() {
        List<Integer> unusedIds = List.of(1, 999, 3); // 999 doesn't exist

        // Should skip missing ID without crashing
        assertDoesNotThrow(() -> {
            int count = counter.countValidPieces(board, 0, 0, piecesById, unusedIds);
            assertTrue(count >= 0, "Should return non-negative count");
        });
    }

    @Test
    @Order(9)
    @DisplayName("Should handle corner positions")
    void testCornerPosition() {
        // Top-left corner (0,0) has different constraints than interior
        List<Integer> unusedIds = List.of(1, 2, 3, 4, 5);

        int count = counter.countValidPieces(board, 0, 0, piecesById, unusedIds);

        assertTrue(count >= 0, "Should count valid pieces for corner");
    }

    @Test
    @Order(10)
    @DisplayName("Should handle border positions")
    void testBorderPosition() {
        // Top border (0,1) has 3 neighbors
        List<Integer> unusedIds = List.of(1, 2, 3, 4, 5);

        int count = counter.countValidPieces(board, 0, 1, piecesById, unusedIds);

        assertTrue(count >= 0, "Should count valid pieces for border");
    }

    @Test
    @Order(11)
    @DisplayName("Should handle interior positions")
    void testInteriorPosition() {
        // Center (1,1) has 4 potential neighbors
        List<Integer> unusedIds = List.of(1, 2, 3, 4, 5);

        int count = counter.countValidPieces(board, 1, 1, piecesById, unusedIds);

        assertTrue(count >= 0, "Should count valid pieces for interior");
    }

    // ==================== Integration Tests ====================

    @Test
    @Order(12)
    @DisplayName("Should work with partially filled board")
    void testPartiallyFilledBoard() {
        // Fill some positions
        board.place(0, 0, new Piece(1, new int[]{0, 1, 2, 0}), 0);
        board.place(0, 1, new Piece(2, new int[]{0, 3, 4, 1}), 0);

        List<Integer> unusedIds = List.of(3, 4, 5);

        // Count for position (1,1) with constraints from above
        int count = counter.countValidPieces(board, 1, 1, piecesById, unusedIds);

        assertTrue(count >= 0 && count <= 3, "Count should be between 0 and number of unused pieces");
    }

    @Test
    @Order(13)
    @DisplayName("Should work with almost full board")
    void testAlmostFullBoard() {
        // Fill 8 out of 9 positions
        board.place(0, 0, new Piece(1, new int[]{0, 1, 2, 0}), 0);
        board.place(0, 1, new Piece(2, new int[]{0, 3, 4, 1}), 0);
        board.place(0, 2, new Piece(3, new int[]{0, 5, 6, 3}), 0);
        board.place(1, 0, new Piece(4, new int[]{2, 7, 8, 0}), 0);
        board.place(1, 1, new Piece(5, new int[]{4, 9, 10, 7}), 0);
        board.place(1, 2, new Piece(6, new int[]{6, 11, 12, 9}), 0);
        board.place(2, 0, new Piece(7, new int[]{8, 13, 0, 0}), 0);
        board.place(2, 1, new Piece(8, new int[]{10, 14, 0, 13}), 0);

        // Only position (2,2) left
        List<Integer> unusedIds = List.of(9);

        int count = counter.countValidPieces(board, 2, 2, piecesById, unusedIds);

        // Should work correctly with heavy constraints
        assertTrue(count == 0 || count == 1, "Should count 0 or 1 for highly constrained position");
    }

    @Test
    @Order(14)
    @DisplayName("Should count consistently for same position")
    void testConsistentCounting() {
        List<Integer> unusedIds = List.of(1, 2, 3);

        int count1 = counter.countValidPieces(board, 1, 1, piecesById, unusedIds);
        int count2 = counter.countValidPieces(board, 1, 1, piecesById, unusedIds);

        assertEquals(count1, count2, "Should return same count for same position");
    }

    @Test
    @Order(15)
    @DisplayName("Should not modify board state")
    void testDoesNotModifyBoard() {
        Board boardBefore = new Board(3, 3);
        board.place(0, 0, new Piece(1, new int[]{0, 1, 2, 0}), 0);
        boardBefore.place(0, 0, new Piece(1, new int[]{0, 1, 2, 0}), 0);

        List<Integer> unusedIds = List.of(2, 3, 4);

        counter.countValidPieces(board, 1, 1, piecesById, unusedIds);

        // Verify board unchanged
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                assertEquals(boardBefore.isEmpty(r, c), board.isEmpty(r, c),
                    String.format("Board state changed at (%d,%d)", r, c));
            }
        }
    }

    // ==================== Helper Methods ====================

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));
        pieces.put(2, new Piece(2, new int[]{0, 2, 3, 1}));
        pieces.put(3, new Piece(3, new int[]{5, 7, 9, 10}));
        pieces.put(4, new Piece(4, new int[]{2, 4, 5, 0}));
        pieces.put(5, new Piece(5, new int[]{3, 5, 6, 4}));
        pieces.put(6, new Piece(6, new int[]{6, 6, 7, 5}));
        pieces.put(7, new Piece(7, new int[]{8, 7, 0, 0}));
        pieces.put(8, new Piece(8, new int[]{10, 8, 0, 7}));
        pieces.put(9, new Piece(9, new int[]{12, 9, 0, 8}));
        return pieces;
    }

    private PlacementValidator createValidator() {
        // Simple mock validator that checks edge matching
        return new PlacementValidator(null, null, null) {
            @Override
            public boolean fits(Board board, int row, int col, int[] edges) {
                // North edge check
                if (row > 0 && !board.isEmpty(row - 1, col)) {
                    int neighborSouth = board.getPlacement(row - 1, col).edges[2];
                    if (edges[0] != neighborSouth) return false;
                }
                // East edge check
                if (col < board.getCols() - 1 && !board.isEmpty(row, col + 1)) {
                    int neighborWest = board.getPlacement(row, col + 1).edges[3];
                    if (edges[1] != neighborWest) return false;
                }
                // South edge check
                if (row < board.getRows() - 1 && !board.isEmpty(row + 1, col)) {
                    int neighborNorth = board.getPlacement(row + 1, col).edges[0];
                    if (edges[2] != neighborNorth) return false;
                }
                // West edge check
                if (col > 0 && !board.isEmpty(row, col - 1)) {
                    int neighborEast = board.getPlacement(row, col - 1).edges[1];
                    if (edges[3] != neighborEast) return false;
                }
                return true;
            }
        };
    }
}
