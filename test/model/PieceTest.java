package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe Piece.
 */
public class PieceTest {

    @Test
    void testConstructorValidEdges() {
        int[] edges = {0, 1, 2, 3};
        Piece piece = new Piece(1, edges);

        assertEquals(1, piece.getId());
        assertArrayEquals(new int[]{0, 1, 2, 3}, piece.getEdges());
    }

    @Test
    void testConstructorInvalidEdgesLength() {
        int[] edges = {0, 1, 2}; // seulement 3 éléments
        assertThrows(IllegalArgumentException.class, () -> new Piece(1, edges));
    }

    @Test
    void testConstructorImmutability() {
        int[] edges = {0, 1, 2, 3};
        Piece piece = new Piece(1, edges);

        // Modifier le tableau original ne devrait pas affecter la pièce
        edges[0] = 99;
        assertArrayEquals(new int[]{0, 1, 2, 3}, piece.getEdges());
    }

    @Test
    void testGetEdgesImmutability() {
        int[] edges = {0, 1, 2, 3};
        Piece piece = new Piece(1, edges);

        // Modifier le tableau retourné ne devrait pas affecter la pièce
        int[] retrievedEdges = piece.getEdges();
        retrievedEdges[0] = 99;
        assertArrayEquals(new int[]{0, 1, 2, 3}, piece.getEdges());
    }

    @Test
    void testEdgesRotatedNoRotation() {
        int[] edges = {1, 2, 3, 4}; // N=1, E=2, S=3, W=4
        Piece piece = new Piece(1, edges);

        int[] rotated = piece.edgesRotated(0);
        assertArrayEquals(new int[]{1, 2, 3, 4}, rotated);
    }

    @Test
    void testEdgesRotated90Degrees() {
        int[] edges = {1, 2, 3, 4}; // N=1, E=2, S=3, W=4
        Piece piece = new Piece(1, edges);

        // Après rotation de 90° CW: newN=W, newE=N, newS=E, newW=S
        int[] rotated = piece.edgesRotated(1);
        assertArrayEquals(new int[]{4, 1, 2, 3}, rotated);
    }

    @Test
    void testEdgesRotated180Degrees() {
        int[] edges = {1, 2, 3, 4}; // N=1, E=2, S=3, W=4
        Piece piece = new Piece(1, edges);

        int[] rotated = piece.edgesRotated(2);
        assertArrayEquals(new int[]{3, 4, 1, 2}, rotated);
    }

    @Test
    void testEdgesRotated270Degrees() {
        int[] edges = {1, 2, 3, 4}; // N=1, E=2, S=3, W=4
        Piece piece = new Piece(1, edges);

        int[] rotated = piece.edgesRotated(3);
        assertArrayEquals(new int[]{2, 3, 4, 1}, rotated);
    }

    @Test
    void testEdgesRotated360Degrees() {
        int[] edges = {1, 2, 3, 4};
        Piece piece = new Piece(1, edges);

        // 360° = 4 * 90° = retour à la position initiale
        int[] rotated = piece.edgesRotated(4);
        assertArrayEquals(new int[]{1, 2, 3, 4}, rotated);
    }

    @Test
    void testEdgesRotatedNegativeRotation() {
        int[] edges = {1, 2, 3, 4};
        Piece piece = new Piece(1, edges);

        // -1 rotation = 3 rotations dans le sens horaire
        int[] rotated = piece.edgesRotated(-1);
        assertArrayEquals(new int[]{2, 3, 4, 1}, rotated);
    }

    @Test
    void testEqualsAndHashCode() {
        Piece piece1 = new Piece(1, new int[]{0, 1, 2, 3});
        Piece piece2 = new Piece(1, new int[]{0, 1, 2, 3});
        Piece piece3 = new Piece(2, new int[]{0, 1, 2, 3});

        assertEquals(piece1, piece2);
        assertEquals(piece1.hashCode(), piece2.hashCode());
        assertNotEquals(piece1, piece3);
    }

    @Test
    void testToString() {
        Piece piece = new Piece(1, new int[]{0, 1, 2, 3});
        String str = piece.toString();

        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("[0, 1, 2, 3]"));
    }
}
