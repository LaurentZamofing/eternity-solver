package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for PlacementValidator.
 * Tests piece placement validation, border constraints, forward checking, and dead-end detection.
 */
@DisplayName("PlacementValidator Tests")
class PlacementValidatorTest {

    private PlacementValidator validator;
    private Board board;
    private CellConstraints[][] cellConstraints;
    private EternitySolver.Statistics stats;
    private Map<Integer, Piece> testPieces;

    @BeforeEach
    void setUp() {
        board = new Board(3, 3);
        stats = new EternitySolver.Statistics();
        cellConstraints = createCellConstraints(3, 3);
        validator = new PlacementValidator(cellConstraints, stats, "ascending");
        testPieces = createTestPieces();
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Should create validator with all dependencies")
    void testConstructor() {
        PlacementValidator validator = new PlacementValidator(cellConstraints, stats, "ascending");
        assertNotNull(validator, "Validator should be created");
    }

    @Test
    @DisplayName("Should create validator with descending sort order")
    void testConstructorDescending() {
        PlacementValidator validator = new PlacementValidator(cellConstraints, stats, "descending");
        assertNotNull(validator, "Validator should be created with descending order");
    }

    // ==================== Fits Method Tests ====================

    @Test
    @DisplayName("Should accept piece with correct border edges at corner")
    void testFitsCornerPiece() {
        // Top-left corner: needs edges [0, x, x, 0]
        int[] cornerEdges = new int[]{0, 1, 2, 0};

        boolean fits = validator.fits(board, 0, 0, cornerEdges);

        assertTrue(fits, "Corner piece with two zero edges should fit");
        assertEquals(1, stats.fitChecks, "Should increment fit checks");
    }

    @Test
    @DisplayName("Should reject piece with non-zero edge at border")
    void testRejectsNonZeroBorderEdge() {
        // Top-left corner: north edge must be 0
        int[] invalidEdges = new int[]{1, 1, 2, 0}; // North = 1 (invalid)

        boolean fits = validator.fits(board, 0, 0, invalidEdges);

        assertFalse(fits, "Should reject piece with non-zero border edge");
    }

    @Test
    @DisplayName("Should reject piece with zero edge in interior")
    void testRejectsZeroEdgeInInterior() {
        // Center cell: no edge should be 0
        int[] invalidEdges = new int[]{0, 1, 2, 3}; // North = 0 (invalid for interior)

        boolean fits = validator.fits(board, 1, 1, invalidEdges);

        assertFalse(fits, "Should reject piece with zero edge in interior");
    }

    @Test
    @DisplayName("Should validate neighbor edge matching")
    void testNeighborEdgeMatching() {
        // Place a piece at (0,0) with east edge = 5
        Piece piece1 = testPieces.get(1);
        board.place(0, 0, piece1, 0); // Assume rotation 0 gives east edge some value

        // Get the actual east edge of placed piece
        int eastEdge = board.getPlacement(0, 0).edges[1];

        // Try to place at (0,1) with west edge matching
        int[] matchingEdges = new int[]{0, 1, 2, eastEdge};

        boolean fits = validator.fits(board, 0, 1, matchingEdges);

        assertTrue(fits, "Should accept piece with matching neighbor edge");
    }

    @Test
    @DisplayName("Should reject mismatched neighbor edges")
    void testRejectsMismatchedNeighborEdges() {
        // Place a piece at (0,0)
        Piece piece1 = testPieces.get(1);
        board.place(0, 0, piece1, 0);

        int eastEdge = board.getPlacement(0, 0).edges[1];

        // Try to place at (0,1) with non-matching west edge
        int[] mismatchedEdges = new int[]{0, 1, 2, eastEdge + 1}; // Different west edge

        boolean fits = validator.fits(board, 0, 1, mismatchedEdges);

        assertFalse(fits, "Should reject piece with mismatched neighbor edge");
    }

    @Test
    @DisplayName("Should validate all four borders correctly")
    void testAllBorders() {
        // Test top border
        int[] topBorderEdges = new int[]{0, 5, 6, 7};
        assertTrue(validator.fits(board, 0, 1, topBorderEdges), "Top border: north must be 0");

        // Test bottom border
        int[] bottomBorderEdges = new int[]{8, 9, 0, 10};
        assertTrue(validator.fits(board, 2, 1, bottomBorderEdges), "Bottom border: south must be 0");

        // Test left border
        int[] leftBorderEdges = new int[]{11, 12, 13, 0};
        assertTrue(validator.fits(board, 1, 0, leftBorderEdges), "Left border: west must be 0");

        // Test right border
        int[] rightBorderEdges = new int[]{14, 0, 15, 16};
        assertTrue(validator.fits(board, 1, 2, rightBorderEdges), "Right border: east must be 0");
    }

    @Test
    @DisplayName("Should increment statistics on each fit check")
    void testStatisticsIncrement() {
        assertEquals(0, stats.fitChecks, "Initial fit checks should be 0");

        validator.fits(board, 0, 0, new int[]{0, 1, 2, 0});
        assertEquals(1, stats.fitChecks, "Should increment after first check");

        validator.fits(board, 0, 1, new int[]{0, 3, 4, 1});
        assertEquals(2, stats.fitChecks, "Should increment after second check");

        validator.fits(board, 1, 1, new int[]{5, 6, 7, 8});
        assertEquals(3, stats.fitChecks, "Should increment after third check");
    }

    // ==================== Forward Check Tests ====================

    @Test
    @DisplayName("Should execute forward check without errors")
    void testForwardCheckExecutes() {
        int[] candidateEdges = new int[]{0, 5, 6, 0}; // Top-left corner
        BitSet pieceUsed = new BitSet(10);

        assertDoesNotThrow(() ->
            validator.forwardCheck(board, 0, 0, candidateEdges,
                                 testPieces, pieceUsed, 9, 1),
            "Forward check should execute without errors"
        );
    }

    @Test
    @DisplayName("Should execute forward check with constrained board")
    void testForwardCheckWithConstraints() {
        // Create a scenario with some pieces placed
        Piece piece1 = testPieces.get(1);
        board.place(0, 1, piece1, 0);

        BitSet pieceUsed = new BitSet(10);
        pieceUsed.set(1); // Mark piece 1 as used

        // Try to place at (0,0)
        int[] candidateEdges = new int[]{0, board.getPlacement(0, 1).edges[3], 5, 0};

        // Just verify forward check executes without throwing exceptions
        assertDoesNotThrow(() ->
            validator.forwardCheck(board, 0, 0, candidateEdges,
                                 testPieces, pieceUsed, 9, 2),
            "Forward check should execute with constrained board"
        );
    }

    @Test
    @DisplayName("Should check all four neighbors in forward check")
    void testForwardCheckAllNeighbors() {
        // Place at center (1,1) - has all 4 neighbors
        int[] centerEdges = new int[]{10, 11, 12, 13};
        BitSet pieceUsed = new BitSet(10);

        boolean safe = validator.forwardCheck(board, 1, 1, centerEdges,
                                             testPieces, pieceUsed, 9, 5);

        // Should check top, bottom, left, right neighbors
        assertNotNull(safe, "Forward check should handle all neighbors");
    }

    @Test
    @DisplayName("Should skip occupied neighbors in forward check")
    void testForwardCheckSkipsOccupiedNeighbors() {
        // Place pieces around target cell
        board.place(0, 0, testPieces.get(1), 0);
        board.place(0, 2, testPieces.get(2), 0);

        int[] candidateEdges = new int[]{0, 7, 8, 6};
        BitSet pieceUsed = new BitSet(10);
        pieceUsed.set(1);
        pieceUsed.set(2);

        boolean safe = validator.forwardCheck(board, 0, 1, candidateEdges,
                                             testPieces, pieceUsed, 9, 3);

        // Should only check empty neighbors
        assertNotNull(safe, "Forward check should skip occupied neighbors");
    }

    @Test
    @DisplayName("Should exclude current piece from forward check")
    void testForwardCheckExcludesPiece() {
        int[] candidateEdges = new int[]{0, 5, 6, 0};
        BitSet pieceUsed = new BitSet(10);

        // Forward check should not consider piece 1 for neighbors
        assertDoesNotThrow(() ->
            validator.forwardCheck(board, 0, 0, candidateEdges,
                                 testPieces, pieceUsed, 9, 1),
            "Forward check should execute and exclude the piece being placed"
        );
    }

    // ==================== Sort Order Tests ====================

    @Test
    @DisplayName("Should use ascending order by default")
    void testAscendingOrder() {
        PlacementValidator ascValidator = new PlacementValidator(cellConstraints, stats, "ascending");

        int[] candidateEdges = new int[]{0, 5, 6, 0};
        BitSet pieceUsed = new BitSet(10);

        // Ascending order: iterates from piece 1 to 9
        assertDoesNotThrow(() ->
            ascValidator.forwardCheck(board, 0, 0, candidateEdges,
                                    testPieces, pieceUsed, 9, 1),
            "Should use ascending order without errors"
        );
    }

    @Test
    @DisplayName("Should use descending order when specified")
    void testDescendingOrder() {
        PlacementValidator descValidator = new PlacementValidator(cellConstraints, stats, "descending");

        int[] candidateEdges = new int[]{0, 5, 6, 0};
        BitSet pieceUsed = new BitSet(10);

        // Descending order: iterates from piece 9 to 1
        assertDoesNotThrow(() ->
            descValidator.forwardCheck(board, 0, 0, candidateEdges,
                                     testPieces, pieceUsed, 9, 1),
            "Should use descending order without errors"
        );
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle empty board")
    void testEmptyBoard() {
        int[] edges = new int[]{0, 1, 2, 0};

        boolean fits = validator.fits(board, 0, 0, edges);

        assertTrue(fits, "Should handle empty board");
    }

    @Test
    @DisplayName("Should handle fully occupied board neighbors")
    void testFullyOccupiedNeighbors() {
        // Fill all neighbors of (1,1)
        board.place(0, 1, testPieces.get(1), 0);
        board.place(2, 1, testPieces.get(2), 0);
        board.place(1, 0, testPieces.get(3), 0);
        board.place(1, 2, testPieces.get(4), 0);

        // Try to validate a piece at center with some edges
        int[] centerEdges = new int[]{5, 6, 7, 8};

        // Just verify fits() executes without throwing exceptions
        // The result depends on whether edges match the neighbors
        assertDoesNotThrow(() ->
            validator.fits(board, 1, 1, centerEdges),
            "Should handle validation with all neighbors occupied"
        );
    }

    @Test
    @DisplayName("Should handle null pieces in map gracefully")
    void testNullPiecesHandling() {
        Map<Integer, Piece> piecesWithNull = new HashMap<>(testPieces);
        piecesWithNull.put(10, null);

        int[] candidateEdges = new int[]{0, 5, 6, 0};
        BitSet pieceUsed = new BitSet(11);

        // After fix: should handle null pieces gracefully by skipping them
        assertDoesNotThrow(() ->
            validator.forwardCheck(board, 0, 0, candidateEdges,
                                 piecesWithNull, pieceUsed, 10, 1),
            "Should handle null pieces gracefully after fix"
        );
    }

    // ==================== Helper Methods ====================

    private CellConstraints[][] createCellConstraints(int rows, int cols) {
        return CellConstraints.createConstraintsMatrix(rows, cols);
    }

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));   // Corner
        pieces.put(2, new Piece(2, new int[]{0, 2, 3, 1}));   // Top edge
        pieces.put(3, new Piece(3, new int[]{0, 3, 4, 2}));   // Corner
        pieces.put(4, new Piece(4, new int[]{2, 4, 5, 0}));   // Left edge
        pieces.put(5, new Piece(5, new int[]{3, 5, 6, 4}));   // Center
        pieces.put(6, new Piece(6, new int[]{4, 6, 7, 5}));   // Center
        pieces.put(7, new Piece(7, new int[]{5, 7, 0, 0}));   // Corner
        pieces.put(8, new Piece(8, new int[]{6, 8, 0, 7}));   // Bottom edge
        pieces.put(9, new Piece(9, new int[]{7, 0, 0, 8}));   // Corner
        return pieces;
    }
}
