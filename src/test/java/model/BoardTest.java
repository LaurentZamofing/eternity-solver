package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe Board.
 */
public class BoardTest {

    @Test
    void testConstructorValid() {
        Board board = new Board(3, 3);
        assertEquals(3, board.getRows());
        assertEquals(3, board.getCols());
    }

    @Test
    void testConstructorInvalidRows() {
        assertThrows(IllegalArgumentException.class, () -> new Board(0, 3));
        assertThrows(IllegalArgumentException.class, () -> new Board(-1, 3));
    }

    @Test
    void testConstructorInvalidCols() {
        assertThrows(IllegalArgumentException.class, () -> new Board(3, 0));
        assertThrows(IllegalArgumentException.class, () -> new Board(3, -1));
    }

    @Test
    void testIsEmptyInitialState() {
        Board board = new Board(3, 3);
        // Toutes les cases doivent être vides au début
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                assertTrue(board.isEmpty(r, c), "Cell (" + r + "," + c + ") should be empty");
            }
        }
    }

    @Test
    void testGetPlacementEmpty() {
        Board board = new Board(3, 3);
        assertNull(board.getPlacement(0, 0));
    }

    @Test
    void testPlaceAndGetPlacement() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{0, 1, 2, 3});

        board.place(0, 0, piece, 0);
        assertFalse(board.isEmpty(0, 0));

        Placement placement = board.getPlacement(0, 0);
        assertNotNull(placement);
        assertEquals(1, placement.getPieceId());
        assertEquals(0, placement.getRotation());
    }

    @Test
    void testPlaceWithRotation() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4}); // N=1, E=2, S=3, W=4

        board.place(0, 0, piece, 1); // rotation de 90°

        Placement placement = board.getPlacement(0, 0);
        assertEquals(1, placement.getRotation());
        // Après rotation de 90° CW: newN=W, newE=N, newS=E, newW=S
        assertArrayEquals(new int[]{4, 1, 2, 3}, placement.edges);
    }

    @Test
    void testRemove() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{0, 1, 2, 3});

        board.place(0, 0, piece, 0);
        assertFalse(board.isEmpty(0, 0));

        board.remove(0, 0);
        assertTrue(board.isEmpty(0, 0));
        assertNull(board.getPlacement(0, 0));
    }

    @Test
    void testMultiplePlacements() {
        Board board = new Board(3, 3);
        Piece piece1 = new Piece(1, new int[]{0, 1, 2, 3});
        Piece piece2 = new Piece(2, new int[]{4, 5, 6, 7});

        board.place(0, 0, piece1, 0);
        board.place(1, 1, piece2, 2);

        assertFalse(board.isEmpty(0, 0));
        assertFalse(board.isEmpty(1, 1));
        assertTrue(board.isEmpty(0, 1));

        assertEquals(1, board.getPlacement(0, 0).getPieceId());
        assertEquals(2, board.getPlacement(1, 1).getPieceId());
    }

    @Test
    void testInvalidCoordinatesIsEmpty() {
        Board board = new Board(3, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> board.isEmpty(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.isEmpty(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> board.isEmpty(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.isEmpty(0, 3));
    }

    @Test
    void testInvalidCoordinatesPlace() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{0, 1, 2, 3});

        assertThrows(IndexOutOfBoundsException.class, () -> board.place(-1, 0, piece, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.place(0, -1, piece, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.place(3, 0, piece, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.place(0, 3, piece, 0));
    }

    @Test
    void testInvalidCoordinatesRemove() {
        Board board = new Board(3, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> board.remove(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.remove(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> board.remove(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.remove(0, 3));
    }

    @Test
    void testOverwritePlacement() {
        Board board = new Board(3, 3);
        Piece piece1 = new Piece(1, new int[]{0, 1, 2, 3});
        Piece piece2 = new Piece(2, new int[]{4, 5, 6, 7});

        board.place(0, 0, piece1, 0);
        assertEquals(1, board.getPlacement(0, 0).getPieceId());

        // Écrasement de la position
        board.place(0, 0, piece2, 1);
        assertEquals(2, board.getPlacement(0, 0).getPieceId());
        assertEquals(1, board.getPlacement(0, 0).getRotation());
    }

    @Test
    void testScoreEmptyBoard() {
        Board board = new Board(3, 3);
        int[] score = board.calculateScore();
        assertEquals(0, score[0], "Empty board should have score 0");
    }

    @Test
    void testScoreSinglePiece() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{0, 1, 2, 3});
        board.place(0, 0, piece, 0);

        int[] score = board.calculateScore();
        // Single piece has no internal edges with neighbors, score should be 0
        assertEquals(0, score[0], "Single piece should have score 0");
    }

    @Test
    void testScoreMatchingEdges() {
        Board board = new Board(2, 2);
        // Pieces with matching edges
        Piece piece1 = new Piece(1, new int[]{0, 5, 3, 0}); // N=0(border), E=5, S=3, W=0(border)
        Piece piece2 = new Piece(2, new int[]{0, 0, 7, 5}); // N=0(border), E=0(border), S=7, W=5(matches piece1 E)

        board.place(0, 0, piece1, 0);
        board.place(0, 1, piece2, 0);

        int[] score = board.calculateScore();
        // One matching internal edge (piece1.E == piece2.W), score = 1
        assertEquals(1, score[0], "Two pieces with one matching edge should have score 1");
    }

    @Test
    void testScoreNonMatchingEdges() {
        Board board = new Board(2, 2);
        Piece piece1 = new Piece(1, new int[]{0, 5, 3, 0});
        Piece piece2 = new Piece(2, new int[]{0, 0, 7, 4}); // W=4 doesn't match piece1 E=5

        board.place(0, 0, piece1, 0);
        board.place(0, 1, piece2, 0);

        int[] score = board.calculateScore();
        // No matching edges, score = 0
        assertEquals(0, score[0], "Non-matching edges should have score 0");
    }

    @Test
    void testMaximumBoardSize() {
        // Test that we can create the maximum Eternity II board size
        Board board = new Board(16, 16);
        assertEquals(16, board.getRows());
        assertEquals(16, board.getCols());

        // Test placing pieces in corners
        Piece piece = new Piece(1, new int[]{0, 1, 2, 0});
        board.place(0, 0, piece, 0);
        board.place(15, 15, piece, 2);

        assertFalse(board.isEmpty(0, 0));
        assertFalse(board.isEmpty(15, 15));
    }

    @Test
    void testLargeBoardPerformance() {
        // Test that operations on large board are reasonable
        Board board = new Board(16, 16);
        Piece piece = new Piece(1, new int[]{0, 1, 2, 3});

        // Fill entire board
        for (int r = 0; r < 16; r++) {
            for (int c = 0; c < 16; c++) {
                board.place(r, c, piece, 0);
            }
        }

        // Verify all filled
        for (int r = 0; r < 16; r++) {
            for (int c = 0; c < 16; c++) {
                assertFalse(board.isEmpty(r, c), "Cell (" + r + "," + c + ") should not be empty");
            }
        }
    }

    @Test
    void testRotationCycles() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4}); // N=1, E=2, S=3, W=4

        // Test all 4 rotations
        board.place(0, 0, piece, 0);
        assertArrayEquals(new int[]{1, 2, 3, 4}, board.getPlacement(0, 0).edges, "Rotation 0");

        board.place(0, 0, piece, 1);
        assertArrayEquals(new int[]{4, 1, 2, 3}, board.getPlacement(0, 0).edges, "Rotation 90°");

        board.place(0, 0, piece, 2);
        assertArrayEquals(new int[]{3, 4, 1, 2}, board.getPlacement(0, 0).edges, "Rotation 180°");

        board.place(0, 0, piece, 3);
        assertArrayEquals(new int[]{2, 3, 4, 1}, board.getPlacement(0, 0).edges, "Rotation 270°");
    }

    @Test
    void testNullPiecePlacement() {
        Board board = new Board(3, 3);
        assertThrows(NullPointerException.class, () -> board.place(0, 0, null, 0),
            "Placing null piece should throw NullPointerException");
    }

    @Test
    void testGetPlacementNullSafety() {
        Board board = new Board(3, 3);
        // Empty cell should return null, not throw
        assertNull(board.getPlacement(0, 0), "Empty cell should return null");
    }

    // ========== Tests for border checking methods ==========

    @Test
    void testIsBorderCell_Corners() {
        Board board = new Board(5, 5);
        assertTrue(board.isBorderCell(0, 0), "Top-left corner is border");
        assertTrue(board.isBorderCell(0, 4), "Top-right corner is border");
        assertTrue(board.isBorderCell(4, 0), "Bottom-left corner is border");
        assertTrue(board.isBorderCell(4, 4), "Bottom-right corner is border");
    }

    @Test
    void testIsBorderCell_Edges() {
        Board board = new Board(5, 5);
        assertTrue(board.isBorderCell(0, 2), "Top edge is border");
        assertTrue(board.isBorderCell(4, 2), "Bottom edge is border");
        assertTrue(board.isBorderCell(2, 0), "Left edge is border");
        assertTrue(board.isBorderCell(2, 4), "Right edge is border");
    }

    @Test
    void testIsBorderCell_Interior() {
        Board board = new Board(5, 5);
        assertFalse(board.isBorderCell(1, 1), "Interior cell is not border");
        assertFalse(board.isBorderCell(2, 2), "Center cell is not border");
        assertFalse(board.isBorderCell(3, 3), "Interior cell is not border");
    }

    @Test
    void testIsTopBorder() {
        Board board = new Board(5, 5);
        assertTrue(board.isTopBorder(0), "Row 0 is top border");
        assertFalse(board.isTopBorder(1), "Row 1 is not top border");
        assertFalse(board.isTopBorder(4), "Row 4 is not top border");
    }

    @Test
    void testIsBottomBorder() {
        Board board = new Board(5, 5);
        assertTrue(board.isBottomBorder(4), "Row 4 is bottom border");
        assertFalse(board.isBottomBorder(0), "Row 0 is not bottom border");
        assertFalse(board.isBottomBorder(3), "Row 3 is not bottom border");
    }

    @Test
    void testIsLeftBorder() {
        Board board = new Board(5, 5);
        assertTrue(board.isLeftBorder(0), "Col 0 is left border");
        assertFalse(board.isLeftBorder(1), "Col 1 is not left border");
        assertFalse(board.isLeftBorder(4), "Col 4 is not left border");
    }

    @Test
    void testIsRightBorder() {
        Board board = new Board(5, 5);
        assertTrue(board.isRightBorder(4), "Col 4 is right border");
        assertFalse(board.isRightBorder(0), "Col 0 is not right border");
        assertFalse(board.isRightBorder(3), "Col 3 is not right border");
    }

    @Test
    void testIsCorner() {
        Board board = new Board(5, 5);
        assertTrue(board.isCorner(0, 0), "Top-left is corner");
        assertTrue(board.isCorner(0, 4), "Top-right is corner");
        assertTrue(board.isCorner(4, 0), "Bottom-left is corner");
        assertTrue(board.isCorner(4, 4), "Bottom-right is corner");

        // Edges are not corners
        assertFalse(board.isCorner(0, 2), "Top edge is not corner");
        assertFalse(board.isCorner(4, 2), "Bottom edge is not corner");
        assertFalse(board.isCorner(2, 0), "Left edge is not corner");
        assertFalse(board.isCorner(2, 4), "Right edge is not corner");

        // Interior is not corner
        assertFalse(board.isCorner(2, 2), "Interior is not corner");
    }

    @Test
    void testBorderMethods_SmallBoard2x2() {
        Board board = new Board(2, 2);
        // All cells are borders in 2x2
        assertTrue(board.isBorderCell(0, 0));
        assertTrue(board.isBorderCell(0, 1));
        assertTrue(board.isBorderCell(1, 0));
        assertTrue(board.isBorderCell(1, 1));

        // All cells are corners in 2x2
        assertTrue(board.isCorner(0, 0));
        assertTrue(board.isCorner(0, 1));
        assertTrue(board.isCorner(1, 0));
        assertTrue(board.isCorner(1, 1));
    }

    @Test
    void testBorderMethods_LargeBoard16x16() {
        Board board = new Board(16, 16);

        // Borders
        assertTrue(board.isBorderCell(0, 5));
        assertTrue(board.isBorderCell(15, 5));
        assertTrue(board.isBorderCell(5, 0));
        assertTrue(board.isBorderCell(5, 15));

        // Interior
        assertFalse(board.isBorderCell(8, 8));
        assertFalse(board.isBorderCell(1, 1));
        assertFalse(board.isBorderCell(14, 14));

        // Corners
        assertTrue(board.isCorner(0, 0));
        assertTrue(board.isCorner(0, 15));
        assertTrue(board.isCorner(15, 0));
        assertTrue(board.isCorner(15, 15));
    }

    @Test
    void testIsBorderCell_InvalidCoordinates() {
        Board board = new Board(5, 5);
        assertThrows(IndexOutOfBoundsException.class, () -> board.isBorderCell(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.isBorderCell(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> board.isBorderCell(5, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.isBorderCell(0, 5));
    }

    @Test
    void testIsCorner_InvalidCoordinates() {
        Board board = new Board(5, 5);
        assertThrows(IndexOutOfBoundsException.class, () -> board.isCorner(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.isCorner(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> board.isCorner(5, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> board.isCorner(0, 5));
    }

    // ========== Tests for hasConstraints() ==========

    @Test
    void testHasConstraints_TopLeftCorner() {
        Board board = new Board(5, 5);
        // Top-left corner (0,0) is on border
        assertTrue(board.hasConstraints(0, 0), "Top-left corner should have constraints (on border)");
    }

    @Test
    void testHasConstraints_TopRightCorner() {
        Board board = new Board(5, 5);
        // Top-right corner (0,4) is on border
        assertTrue(board.hasConstraints(0, 4), "Top-right corner should have constraints (on border)");
    }

    @Test
    void testHasConstraints_BottomLeftCorner() {
        Board board = new Board(5, 5);
        // Bottom-left corner (4,0) is on border
        assertTrue(board.hasConstraints(4, 0), "Bottom-left corner should have constraints (on border)");
    }

    @Test
    void testHasConstraints_BottomRightCorner() {
        Board board = new Board(5, 5);
        // Bottom-right corner (4,4) is on border
        assertTrue(board.hasConstraints(4, 4), "Bottom-right corner should have constraints (on border)");
    }

    @Test
    void testHasConstraints_TopBorder() {
        Board board = new Board(5, 5);
        // Top border (0,2) - middle of top edge
        assertTrue(board.hasConstraints(0, 2), "Top border cell should have constraints");
    }

    @Test
    void testHasConstraints_BottomBorder() {
        Board board = new Board(5, 5);
        // Bottom border (4,2) - middle of bottom edge
        assertTrue(board.hasConstraints(4, 2), "Bottom border cell should have constraints");
    }

    @Test
    void testHasConstraints_LeftBorder() {
        Board board = new Board(5, 5);
        // Left border (2,0) - middle of left edge
        assertTrue(board.hasConstraints(2, 0), "Left border cell should have constraints");
    }

    @Test
    void testHasConstraints_RightBorder() {
        Board board = new Board(5, 5);
        // Right border (2,4) - middle of right edge
        assertTrue(board.hasConstraints(2, 4), "Right border cell should have constraints");
    }

    @Test
    void testHasConstraints_InteriorNoNeighbors() {
        Board board = new Board(5, 5);
        // Interior cell (2,2) with no occupied neighbors
        assertFalse(board.hasConstraints(2, 2),
            "Interior cell with no neighbors should have NO constraints");
    }

    @Test
    void testHasConstraints_InteriorWithNorthNeighbor() {
        Board board = new Board(5, 5);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Place piece at north neighbor (1,2)
        board.place(1, 2, piece, 0);

        // Cell (2,2) should now have constraints (north neighbor occupied)
        assertTrue(board.hasConstraints(2, 2),
            "Interior cell with north neighbor should have constraints");
    }

    @Test
    void testHasConstraints_InteriorWithEastNeighbor() {
        Board board = new Board(5, 5);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Place piece at east neighbor (2,3)
        board.place(2, 3, piece, 0);

        // Cell (2,2) should now have constraints (east neighbor occupied)
        assertTrue(board.hasConstraints(2, 2),
            "Interior cell with east neighbor should have constraints");
    }

    @Test
    void testHasConstraints_InteriorWithSouthNeighbor() {
        Board board = new Board(5, 5);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Place piece at south neighbor (3,2)
        board.place(3, 2, piece, 0);

        // Cell (2,2) should now have constraints (south neighbor occupied)
        assertTrue(board.hasConstraints(2, 2),
            "Interior cell with south neighbor should have constraints");
    }

    @Test
    void testHasConstraints_InteriorWithWestNeighbor() {
        Board board = new Board(5, 5);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Place piece at west neighbor (2,1)
        board.place(2, 1, piece, 0);

        // Cell (2,2) should now have constraints (west neighbor occupied)
        assertTrue(board.hasConstraints(2, 2),
            "Interior cell with west neighbor should have constraints");
    }

    @Test
    void testHasConstraints_InteriorWithMultipleNeighbors() {
        Board board = new Board(5, 5);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Place pieces at multiple neighbors
        board.place(1, 2, piece, 0); // North
        board.place(2, 3, piece, 0); // East
        board.place(3, 2, piece, 0); // South

        // Cell (2,2) should have constraints (multiple neighbors)
        assertTrue(board.hasConstraints(2, 2),
            "Interior cell with multiple neighbors should have constraints");
    }

    @Test
    void testHasConstraints_AllInteriorCellsSurrounded() {
        Board board = new Board(5, 5);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Place pieces around cell (2,2) - all 4 neighbors
        board.place(1, 2, piece, 0); // North
        board.place(2, 3, piece, 0); // East
        board.place(3, 2, piece, 0); // South
        board.place(2, 1, piece, 0); // West

        assertTrue(board.hasConstraints(2, 2),
            "Interior cell completely surrounded should have constraints");
    }

    @Test
    void testHasConstraints_InvalidCoordinatesNegative() {
        Board board = new Board(5, 5);
        assertThrows(IndexOutOfBoundsException.class,
            () -> board.hasConstraints(-1, 2),
            "Negative row should throw IndexOutOfBoundsException");
        assertThrows(IndexOutOfBoundsException.class,
            () -> board.hasConstraints(2, -1),
            "Negative col should throw IndexOutOfBoundsException");
    }

    @Test
    void testHasConstraints_InvalidCoordinatesOutOfBounds() {
        Board board = new Board(5, 5);
        assertThrows(IndexOutOfBoundsException.class,
            () -> board.hasConstraints(5, 2),
            "Row >= rows should throw IndexOutOfBoundsException");
        assertThrows(IndexOutOfBoundsException.class,
            () -> board.hasConstraints(2, 5),
            "Col >= cols should throw IndexOutOfBoundsException");
    }

    @Test
    void testHasConstraints_SmallBoard2x2() {
        Board board = new Board(2, 2);
        // In a 2x2 board, all cells are on border
        assertTrue(board.hasConstraints(0, 0), "2x2 board: (0,0) is corner");
        assertTrue(board.hasConstraints(0, 1), "2x2 board: (0,1) is corner");
        assertTrue(board.hasConstraints(1, 0), "2x2 board: (1,0) is corner");
        assertTrue(board.hasConstraints(1, 1), "2x2 board: (1,1) is corner");
    }

    @Test
    void testHasConstraints_LargeBoard16x16Interior() {
        Board board = new Board(16, 16);
        // Cell (8,8) in center of 16x16 board, no neighbors
        assertFalse(board.hasConstraints(8, 8),
            "Center of 16x16 board with no neighbors should have NO constraints");

        // Place a piece far away
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
        board.place(0, 0, piece, 0);

        // Cell (8,8) should still have no constraints (neighbor too far)
        assertFalse(board.hasConstraints(8, 8),
            "Center cell should still have NO constraints (neighbor too far)");

        // Place piece adjacent to (8,8)
        board.place(8, 9, piece, 0); // East neighbor

        // Now (8,8) should have constraints
        assertTrue(board.hasConstraints(8, 8),
            "Center cell should now have constraints (adjacent neighbor)");
    }

    @Test
    void testHasConstraints_OccupiedCellStillHasConstraints() {
        Board board = new Board(5, 5);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Place piece at interior cell (2,2)
        board.place(2, 2, piece, 0);

        // Even though occupied, if it has no neighbors and not on border,
        // the method should report based on position, not occupancy
        // But since we're checking for constraint PROPAGATION purposes,
        // an occupied cell is irrelevant (we only check empty cells in practice)
        // However, the method itself should work correctly
        assertFalse(board.hasConstraints(2, 2),
            "Occupied interior cell with no neighbors has no constraints (positionally)");
    }
}
