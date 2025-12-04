package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Placement class.
 * Tests constructor, getters, rotation normalization, equals/hashCode contract, and toString.
 */
@DisplayName("Placement Tests")
public class PlacementTest {

    @Test
    @DisplayName("Constructor creates placement with correct values")
    void testConstructorBasic() {
        int[] edges = {1, 2, 3, 4};
        Placement placement = new Placement(5, 2, edges);

        assertEquals(5, placement.getPieceId());
        assertEquals(2, placement.getRotation());
        assertArrayEquals(new int[]{1, 2, 3, 4}, placement.edges);
    }

    @Test
    @DisplayName("Constructor copies edges array (immutability)")
    void testConstructorCopiesEdges() {
        int[] edges = {1, 2, 3, 4};
        Placement placement = new Placement(10, 0, edges);

        // Modify original array
        edges[0] = 999;

        // Placement should have original values
        assertArrayEquals(new int[]{1, 2, 3, 4}, placement.edges);
    }

    @Test
    @DisplayName("Rotation 0-3 is unchanged")
    void testRotationNoNormalization() {
        int[] edges = {1, 2, 3, 4};

        assertEquals(0, new Placement(1, 0, edges).getRotation());
        assertEquals(1, new Placement(1, 1, edges).getRotation());
        assertEquals(2, new Placement(1, 2, edges).getRotation());
        assertEquals(3, new Placement(1, 3, edges).getRotation());
    }

    @Test
    @DisplayName("Rotation 4+ is normalized modulo 4")
    void testRotationNormalizationModulo4() {
        int[] edges = {1, 2, 3, 4};

        assertEquals(0, new Placement(1, 4, edges).getRotation());   // 4 mod 4 = 0
        assertEquals(1, new Placement(1, 5, edges).getRotation());   // 5 mod 4 = 1
        assertEquals(3, new Placement(1, 7, edges).getRotation());   // 7 mod 4 = 3
        assertEquals(0, new Placement(1, 8, edges).getRotation());   // 8 mod 4 = 0
        assertEquals(0, new Placement(1, 100, edges).getRotation()); // 100 mod 4 = 0
    }

    @Test
    @DisplayName("getPieceId returns correct piece ID")
    void testGetPieceId() {
        Placement placement = new Placement(42, 1, new int[]{1, 2, 3, 4});
        assertEquals(42, placement.getPieceId());
    }

    @Test
    @DisplayName("getRotation returns correct rotation")
    void testGetRotation() {
        Placement placement = new Placement(10, 3, new int[]{1, 2, 3, 4});
        assertEquals(3, placement.getRotation());
    }

    @Test
    @DisplayName("toString formats correctly")
    void testToString() {
        Placement placement = new Placement(7, 2, new int[]{1, 2, 3, 4});
        assertEquals("07 r2", placement.toString());

        Placement placement2 = new Placement(15, 0, new int[]{5, 6, 7, 8});
        assertEquals("15 r0", placement2.toString());

        Placement placement3 = new Placement(1, 3, new int[]{0, 0, 0, 0});
        assertEquals("01 r3", placement3.toString());
    }

    @Test
    @DisplayName("equals returns true for identical placements")
    void testEqualsIdentical() {
        Placement p1 = new Placement(5, 2, new int[]{1, 2, 3, 4});
        Placement p2 = new Placement(5, 2, new int[]{1, 2, 3, 4});

        assertEquals(p1, p2);
    }

    @Test
    @DisplayName("equals returns true for same object")
    void testEqualsSameObject() {
        Placement p = new Placement(5, 2, new int[]{1, 2, 3, 4});
        assertEquals(p, p);
    }

    @Test
    @DisplayName("equals returns false for different piece ID")
    void testEqualsDifferentPieceId() {
        Placement p1 = new Placement(5, 2, new int[]{1, 2, 3, 4});
        Placement p2 = new Placement(6, 2, new int[]{1, 2, 3, 4});

        assertNotEquals(p1, p2);
    }

    @Test
    @DisplayName("equals returns false for different rotation")
    void testEqualsDifferentRotation() {
        Placement p1 = new Placement(5, 2, new int[]{1, 2, 3, 4});
        Placement p2 = new Placement(5, 3, new int[]{1, 2, 3, 4});

        assertNotEquals(p1, p2);
    }

    @Test
    @DisplayName("equals returns false for different edges")
    void testEqualsDifferentEdges() {
        Placement p1 = new Placement(5, 2, new int[]{1, 2, 3, 4});
        Placement p2 = new Placement(5, 2, new int[]{1, 2, 3, 5});

        assertNotEquals(p1, p2);
    }

    @Test
    @DisplayName("equals returns false for null")
    void testEqualsNull() {
        Placement p = new Placement(5, 2, new int[]{1, 2, 3, 4});
        assertNotEquals(p, null);
    }

    @Test
    @DisplayName("equals returns false for different class")
    void testEqualsDifferentClass() {
        Placement p = new Placement(5, 2, new int[]{1, 2, 3, 4});
        assertNotEquals(p, "not a placement");
    }

    @Test
    @DisplayName("hashCode is consistent")
    void testHashCodeConsistent() {
        Placement p = new Placement(5, 2, new int[]{1, 2, 3, 4});
        int hash1 = p.hashCode();
        int hash2 = p.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("hashCode is equal for equal objects")
    void testHashCodeEqualObjects() {
        Placement p1 = new Placement(5, 2, new int[]{1, 2, 3, 4});
        Placement p2 = new Placement(5, 2, new int[]{1, 2, 3, 4});

        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    @DisplayName("hashCode is different for different objects (likely)")
    void testHashCodeDifferentObjects() {
        Placement p1 = new Placement(5, 2, new int[]{1, 2, 3, 4});
        Placement p2 = new Placement(6, 2, new int[]{1, 2, 3, 4});
        Placement p3 = new Placement(5, 3, new int[]{1, 2, 3, 4});
        Placement p4 = new Placement(5, 2, new int[]{5, 6, 7, 8});

        // Not guaranteed, but very likely to be different
        assertNotEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1.hashCode(), p3.hashCode());
        assertNotEquals(p1.hashCode(), p4.hashCode());
    }

    @Test
    @DisplayName("Placement with zero rotation")
    void testZeroRotation() {
        Placement p = new Placement(1, 0, new int[]{10, 20, 30, 40});
        assertEquals(0, p.getRotation());
        assertArrayEquals(new int[]{10, 20, 30, 40}, p.edges);
    }

    @Test
    @DisplayName("Placement with large piece ID")
    void testLargePieceId() {
        Placement p = new Placement(256, 1, new int[]{1, 2, 3, 4});
        assertEquals(256, p.getPieceId());
    }

    @Test
    @DisplayName("Edges array has exactly 4 elements")
    void testEdgesArrayLength() {
        int[] edges = {1, 2, 3, 4};
        Placement p = new Placement(1, 0, edges);
        assertEquals(4, p.edges.length);
    }

    @Test
    @DisplayName("Edge values are preserved correctly")
    void testEdgeValuesPreserved() {
        int[] edges = {0, 100, 255, 42};
        Placement p = new Placement(1, 0, edges);

        assertEquals(0, p.edges[0]);
        assertEquals(100, p.edges[1]);
        assertEquals(255, p.edges[2]);
        assertEquals(42, p.edges[3]);
    }

    @Test
    @DisplayName("Multiple placements can coexist")
    void testMultiplePlacements() {
        Placement p1 = new Placement(1, 0, new int[]{1, 2, 3, 4});
        Placement p2 = new Placement(2, 1, new int[]{5, 6, 7, 8});
        Placement p3 = new Placement(3, 2, new int[]{9, 10, 11, 12});

        assertEquals(1, p1.getPieceId());
        assertEquals(2, p2.getPieceId());
        assertEquals(3, p3.getPieceId());

        assertEquals(0, p1.getRotation());
        assertEquals(1, p2.getRotation());
        assertEquals(2, p3.getRotation());
    }

    @Test
    @DisplayName("Equals is symmetric")
    void testEqualsSymmetric() {
        Placement p1 = new Placement(5, 2, new int[]{1, 2, 3, 4});
        Placement p2 = new Placement(5, 2, new int[]{1, 2, 3, 4});

        assertEquals(p1, p2);
        assertEquals(p2, p1);
    }

    @Test
    @DisplayName("Equals is transitive")
    void testEqualsTransitive() {
        Placement p1 = new Placement(5, 2, new int[]{1, 2, 3, 4});
        Placement p2 = new Placement(5, 2, new int[]{1, 2, 3, 4});
        Placement p3 = new Placement(5, 2, new int[]{1, 2, 3, 4});

        assertEquals(p1, p2);
        assertEquals(p2, p3);
        assertEquals(p1, p3);
    }
}
