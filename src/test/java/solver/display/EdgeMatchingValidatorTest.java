package solver.display;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for EdgeMatchingValidator.
 * Tests edge matching logic for all 4 directions with various board configurations.
 */
@DisplayName("EdgeMatchingValidator Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EdgeMatchingValidatorTest {

    private Board board;
    private EdgeMatchingValidator validator;

    @BeforeEach
    void setUp() {
        // Create 3x3 test board
        board = new Board(3, 3);
        validator = new EdgeMatchingValidator(board);
    }

    // ==================== Neighbor Detection Tests ====================

    @Test
    @Order(1)
    @DisplayName("Should detect north neighbor correctly")
    void testHasNorthNeighbor() {
        // Top row - no north neighbor
        assertFalse(validator.hasNorthNeighbor(0, 0), "Top-left has no north");
        assertFalse(validator.hasNorthNeighbor(0, 1), "Top-middle has no north");
        assertFalse(validator.hasNorthNeighbor(0, 2), "Top-right has no north");

        // Middle row - has north neighbor if occupied
        board.place(0, 0, new Piece(1, new int[]{1, 2, 3, 4}), 0);
        assertTrue(validator.hasNorthNeighbor(1, 0), "Middle-left has north when occupied");

        // Empty north neighbor returns false
        assertFalse(validator.hasNorthNeighbor(1, 1), "No north when empty");
    }

    @Test
    @Order(2)
    @DisplayName("Should detect south neighbor correctly")
    void testHasSouthNeighbor() {
        // Bottom row - no south neighbor
        assertFalse(validator.hasSouthNeighbor(2, 0), "Bottom-left has no south");
        assertFalse(validator.hasSouthNeighbor(2, 1), "Bottom-middle has no south");
        assertFalse(validator.hasSouthNeighbor(2, 2), "Bottom-right has no south");

        // Top row - has south neighbor if occupied
        board.place(1, 0, new Piece(1, new int[]{1, 2, 3, 4}), 0);
        assertTrue(validator.hasSouthNeighbor(0, 0), "Top-left has south when occupied");

        // Empty south neighbor returns false
        assertFalse(validator.hasSouthNeighbor(0, 1), "No south when empty");
    }

    @Test
    @Order(3)
    @DisplayName("Should detect east neighbor correctly")
    void testHasEastNeighbor() {
        // Right column - no east neighbor
        assertFalse(validator.hasEastNeighbor(0, 2), "Top-right has no east");
        assertFalse(validator.hasEastNeighbor(1, 2), "Middle-right has no east");
        assertFalse(validator.hasEastNeighbor(2, 2), "Bottom-right has no east");

        // Left column - has east neighbor if occupied
        board.place(0, 1, new Piece(1, new int[]{1, 2, 3, 4}), 0);
        assertTrue(validator.hasEastNeighbor(0, 0), "Top-left has east when occupied");

        // Empty east neighbor returns false
        assertFalse(validator.hasEastNeighbor(1, 0), "No east when empty");
    }

    @Test
    @Order(4)
    @DisplayName("Should detect west neighbor correctly")
    void testHasWestNeighbor() {
        // Left column - no west neighbor
        assertFalse(validator.hasWestNeighbor(0, 0), "Top-left has no west");
        assertFalse(validator.hasWestNeighbor(1, 0), "Middle-left has no west");
        assertFalse(validator.hasWestNeighbor(2, 0), "Bottom-left has no west");

        // Right column - has west neighbor if occupied
        board.place(0, 0, new Piece(1, new int[]{1, 2, 3, 4}), 0);
        assertTrue(validator.hasWestNeighbor(0, 1), "Top-middle has west when occupied");

        // Empty west neighbor returns false
        assertFalse(validator.hasWestNeighbor(0, 2), "No west when empty");
    }

    // ==================== Edge Matching Tests ====================

    @Test
    @Order(5)
    @DisplayName("Should match north edges correctly")
    void testNorthEdgeMatches() {
        // Place piece at (0,0) with south edge = 5
        board.place(0, 0, new Piece(1, new int[]{1, 2, 5, 4}), 0);

        // Place piece at (1,0) with north edge = 5 (matches)
        board.place(1, 0, new Piece(2, new int[]{5, 6, 7, 8}), 0);

        assertTrue(validator.northEdgeMatches(1, 0), "North edge should match");

        // Place piece with non-matching north edge = 9
        board.place(1, 1, new Piece(3, new int[]{9, 10, 11, 12}), 0);
        board.place(0, 1, new Piece(4, new int[]{13, 14, 15, 16}), 0);

        assertFalse(validator.northEdgeMatches(1, 1), "North edge should not match");
    }

    @Test
    @Order(6)
    @DisplayName("Should match south edges correctly")
    void testSouthEdgeMatches() {
        // Place piece at (1,0) with north edge = 5
        board.place(1, 0, new Piece(1, new int[]{5, 2, 3, 4}), 0);

        // Place piece at (0,0) with south edge = 5 (matches)
        board.place(0, 0, new Piece(2, new int[]{6, 7, 5, 9}), 0);

        assertTrue(validator.southEdgeMatches(0, 0), "South edge should match");

        // Place piece with non-matching south edge = 99
        board.place(0, 1, new Piece(3, new int[]{10, 11, 99, 13}), 0);
        board.place(1, 1, new Piece(4, new int[]{14, 15, 16, 17}), 0);

        assertFalse(validator.southEdgeMatches(0, 1), "South edge should not match");
    }

    @Test
    @Order(7)
    @DisplayName("Should match east edges correctly")
    void testEastEdgeMatches() {
        // Place piece at (0,1) with west edge = 8
        board.place(0, 1, new Piece(1, new int[]{1, 2, 3, 8}), 0);

        // Place piece at (0,0) with east edge = 8 (matches)
        board.place(0, 0, new Piece(2, new int[]{5, 8, 7, 9}), 0);

        assertTrue(validator.eastEdgeMatches(0, 0), "East edge should match");

        // Place piece with non-matching east edge = 77
        board.place(1, 0, new Piece(3, new int[]{10, 77, 12, 13}), 0);
        board.place(1, 1, new Piece(4, new int[]{14, 15, 16, 17}), 0);

        assertFalse(validator.eastEdgeMatches(1, 0), "East edge should not match");
    }

    @Test
    @Order(8)
    @DisplayName("Should match west edges correctly")
    void testWestEdgeMatches() {
        // Place piece at (0,0) with east edge = 6
        board.place(0, 0, new Piece(1, new int[]{1, 6, 3, 4}), 0);

        // Place piece at (0,1) with west edge = 6 (matches)
        board.place(0, 1, new Piece(2, new int[]{5, 7, 8, 6}), 0);

        assertTrue(validator.westEdgeMatches(0, 1), "West edge should match");

        // Place piece with non-matching west edge = 88
        board.place(1, 0, new Piece(3, new int[]{10, 11, 12, 13}), 0);
        board.place(1, 1, new Piece(4, new int[]{14, 15, 16, 88}), 0);

        assertFalse(validator.westEdgeMatches(1, 1), "West edge should not match");
    }

    // ==================== Edge Cases ====================

    @Test
    @Order(9)
    @DisplayName("Should handle corner pieces (2 neighbors)")
    void testCornerPieces() {
        // Top-left corner: only south and east neighbors
        board.place(0, 0, new Piece(1, new int[]{0, 2, 3, 0}), 0);
        board.place(1, 0, new Piece(2, new int[]{3, 5, 6, 0}), 0);  // South: matches
        board.place(0, 1, new Piece(3, new int[]{0, 7, 8, 2}), 0);  // East: matches

        assertFalse(validator.hasNorthNeighbor(0, 0), "Corner has no north");
        assertFalse(validator.hasWestNeighbor(0, 0), "Corner has no west");
        assertTrue(validator.hasSouthNeighbor(0, 0), "Corner has south");
        assertTrue(validator.hasEastNeighbor(0, 0), "Corner has east");

        assertTrue(validator.southEdgeMatches(0, 0), "South edge should match");
        assertTrue(validator.eastEdgeMatches(0, 0), "East edge should match");
    }

    @Test
    @Order(10)
    @DisplayName("Should handle border pieces (3 neighbors)")
    void testBorderPieces() {
        // Top border (middle): no north, but has south, east, west
        board.place(0, 1, new Piece(1, new int[]{0, 2, 3, 4}), 0);
        board.place(1, 1, new Piece(2, new int[]{3, 6, 7, 8}), 0);  // South: N=3 matches S=3
        board.place(0, 0, new Piece(3, new int[]{0, 4, 9, 0}), 0);  // West: E=4 matches W=4
        board.place(0, 2, new Piece(4, new int[]{0, 11, 12, 2}), 0); // East: W=2 matches E=2

        assertFalse(validator.hasNorthNeighbor(0, 1), "Top border has no north");
        assertTrue(validator.hasSouthNeighbor(0, 1), "Top border has south");
        assertTrue(validator.hasEastNeighbor(0, 1), "Top border has east");
        assertTrue(validator.hasWestNeighbor(0, 1), "Top border has west");

        assertTrue(validator.southEdgeMatches(0, 1), "South edge should match");
        assertTrue(validator.eastEdgeMatches(0, 1), "East edge should match");
        assertTrue(validator.westEdgeMatches(0, 1), "West edge should match");
    }

    @Test
    @Order(11)
    @DisplayName("Should handle interior pieces (4 neighbors)")
    void testInteriorPieces() {
        // Center piece (1,1): has all 4 neighbors
        board.place(1, 1, new Piece(1, new int[]{1, 2, 3, 4}), 0);
        board.place(0, 1, new Piece(2, new int[]{5, 6, 1, 8}), 0);  // North
        board.place(2, 1, new Piece(3, new int[]{3, 10, 11, 12}), 0); // South
        board.place(1, 2, new Piece(4, new int[]{13, 14, 15, 2}), 0); // East
        board.place(1, 0, new Piece(5, new int[]{16, 4, 18, 19}), 0); // West

        assertTrue(validator.hasNorthNeighbor(1, 1), "Interior has north");
        assertTrue(validator.hasSouthNeighbor(1, 1), "Interior has south");
        assertTrue(validator.hasEastNeighbor(1, 1), "Interior has east");
        assertTrue(validator.hasWestNeighbor(1, 1), "Interior has west");

        assertTrue(validator.northEdgeMatches(1, 1), "North edge should match");
        assertTrue(validator.southEdgeMatches(1, 1), "South edge should match");
        assertTrue(validator.eastEdgeMatches(1, 1), "East edge should match");
        assertTrue(validator.westEdgeMatches(1, 1), "West edge should match");
    }

    @Test
    @Order(12)
    @DisplayName("Should return false when no neighbor exists")
    void testNoNeighborReturnsFalse() {
        board.place(0, 0, new Piece(1, new int[]{1, 2, 3, 4}), 0);

        // No north neighbor at top row
        assertFalse(validator.northEdgeMatches(0, 0), "Should return false when no north");

        // No west neighbor at left column
        assertFalse(validator.westEdgeMatches(0, 0), "Should return false when no west");
    }

    @Test
    @Order(13)
    @DisplayName("Should handle empty board")
    void testEmptyBoard() {
        // All positions should have no neighbors
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                assertFalse(validator.hasNorthNeighbor(r, c),
                    String.format("Empty board (%d,%d) should have no north", r, c));
                assertFalse(validator.hasSouthNeighbor(r, c),
                    String.format("Empty board (%d,%d) should have no south", r, c));
                assertFalse(validator.hasEastNeighbor(r, c),
                    String.format("Empty board (%d,%d) should have no east", r, c));
                assertFalse(validator.hasWestNeighbor(r, c),
                    String.format("Empty board (%d,%d) should have no west", r, c));
            }
        }
    }

    @Test
    @Order(14)
    @DisplayName("Should handle board boundaries correctly")
    void testBoardBoundaries() {
        board.place(0, 0, new Piece(1, new int[]{1, 2, 3, 4}), 0);

        // Test out of bounds protection
        assertFalse(validator.hasNorthNeighbor(0, 0), "Row 0 has no north");
        assertFalse(validator.hasWestNeighbor(0, 0), "Col 0 has no west");

        board.place(2, 2, new Piece(2, new int[]{5, 6, 7, 8}), 0);

        assertFalse(validator.hasSouthNeighbor(2, 2), "Bottom row has no south");
        assertFalse(validator.hasEastNeighbor(2, 2), "Right col has no east");
    }

    // ==================== Integration Tests ====================

    @Test
    @Order(15)
    @DisplayName("Should validate complete valid 2x2 puzzle")
    void testValid2x2Puzzle() {
        Board small = new Board(2, 2);
        EdgeMatchingValidator v = new EdgeMatchingValidator(small);

        // Create matching 2x2 puzzle
        // (0,0): [0,1,2,0]  (0,1): [0,3,4,1]
        // (1,0): [2,5,0,0]  (1,1): [4,0,0,5]
        small.place(0, 0, new Piece(1, new int[]{0, 1, 2, 0}), 0);
        small.place(0, 1, new Piece(2, new int[]{0, 3, 4, 1}), 0);
        small.place(1, 0, new Piece(3, new int[]{2, 5, 0, 0}), 0);
        small.place(1, 1, new Piece(4, new int[]{4, 0, 0, 5}), 0);

        // Verify all edges match
        assertTrue(v.eastEdgeMatches(0, 0), "(0,0) east matches");
        assertTrue(v.southEdgeMatches(0, 0), "(0,0) south matches");
        assertTrue(v.westEdgeMatches(0, 1), "(0,1) west matches");
        assertTrue(v.southEdgeMatches(0, 1), "(0,1) south matches");
        assertTrue(v.northEdgeMatches(1, 0), "(1,0) north matches");
        assertTrue(v.eastEdgeMatches(1, 0), "(1,0) east matches");
        assertTrue(v.northEdgeMatches(1, 1), "(1,1) north matches");
        assertTrue(v.westEdgeMatches(1, 1), "(1,1) west matches");
    }

    @Test
    @Order(16)
    @DisplayName("Should detect mismatches in complete puzzle")
    void testInvalid2x2Puzzle() {
        Board small = new Board(2, 2);
        EdgeMatchingValidator v = new EdgeMatchingValidator(small);

        // Create mismatched 2x2 puzzle
        small.place(0, 0, new Piece(1, new int[]{0, 1, 2, 0}), 0);
        small.place(0, 1, new Piece(2, new int[]{0, 99, 4, 1}), 0); // East matches
        small.place(1, 0, new Piece(3, new int[]{99, 5, 0, 0}), 0); // North MISMATCH (2 vs 99)
        small.place(1, 1, new Piece(4, new int[]{4, 0, 0, 5}), 0);

        // Verify mismatches detected
        assertTrue(v.eastEdgeMatches(0, 0), "(0,0) east matches");
        assertFalse(v.southEdgeMatches(0, 0), "(0,0) south should NOT match (2 vs 99)");
        assertFalse(v.northEdgeMatches(1, 0), "(1,0) north should NOT match");
    }
}
