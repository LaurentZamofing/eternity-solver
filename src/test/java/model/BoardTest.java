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
}
