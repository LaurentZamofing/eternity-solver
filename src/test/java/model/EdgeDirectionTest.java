package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EdgeDirectionTest {

    @Test
    void indicesMatchLegacyConvention() {
        assertEquals(0, EdgeDirection.NORTH.index);
        assertEquals(1, EdgeDirection.EAST.index);
        assertEquals(2, EdgeDirection.SOUTH.index);
        assertEquals(3, EdgeDirection.WEST.index);
    }

    @Test
    void oppositeIsInvolution() {
        for (EdgeDirection d : EdgeDirection.values()) {
            assertEquals(d, d.opposite().opposite(), "opposite() twice should return self");
        }
        assertEquals(EdgeDirection.SOUTH, EdgeDirection.NORTH.opposite());
        assertEquals(EdgeDirection.WEST, EdgeDirection.EAST.opposite());
    }

    @Test
    void rotateClockwiseCycle() {
        assertEquals(EdgeDirection.EAST, EdgeDirection.NORTH.rotateClockwise());
        assertEquals(EdgeDirection.SOUTH, EdgeDirection.EAST.rotateClockwise());
        assertEquals(EdgeDirection.WEST, EdgeDirection.SOUTH.rotateClockwise());
        assertEquals(EdgeDirection.NORTH, EdgeDirection.WEST.rotateClockwise());
    }

    @Test
    void ofValidIndex() {
        assertEquals(EdgeDirection.NORTH, EdgeDirection.of(0));
        assertEquals(EdgeDirection.WEST, EdgeDirection.of(3));
    }

    @Test
    void ofRejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> EdgeDirection.of(-1));
        assertThrows(IllegalArgumentException.class, () -> EdgeDirection.of(4));
    }
}
