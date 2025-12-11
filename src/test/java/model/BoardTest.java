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
}
