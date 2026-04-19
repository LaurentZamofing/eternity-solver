package solver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ZobristHasher")
class ZobristHasherTest {

    @Test
    @DisplayName("keys are non-zero")
    void keysNonZero() {
        ZobristHasher h = new ZobristHasher(4, 4, 16, 42L);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                for (int pid = 1; pid <= 16; pid++) {
                    for (int rot = 0; rot < 4; rot++) {
                        long k = h.keyOf(r, c, pid, rot);
                        assertNotEquals(0L, k, "key(" + r + "," + c + "," + pid + "," + rot + ") must not be 0");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("same (cell, pid, rot) → identical key")
    void deterministic() {
        ZobristHasher h1 = new ZobristHasher(4, 4, 16, 42L);
        assertEquals(h1.keyOf(1, 2, 5, 3), h1.keyOf(1, 2, 5, 3));
    }

    @Test
    @DisplayName("different seed → different tables")
    void seedChangesKeys() {
        ZobristHasher h1 = new ZobristHasher(4, 4, 16, 42L);
        ZobristHasher h2 = new ZobristHasher(4, 4, 16, 99L);
        assertNotEquals(h1.keyOf(0, 0, 1, 0), h2.keyOf(0, 0, 1, 0));
    }

    @Test
    @DisplayName("XOR is self-inverse — applying key twice restores state")
    void xorSelfInverse() {
        ZobristHasher h = new ZobristHasher(4, 4, 16, 42L);
        long k = h.keyOf(2, 3, 7, 1);
        long hash = 0xDEADBEEFCAFEBABEL;
        long after = hash ^ k;
        long restored = after ^ k;
        assertEquals(hash, restored);
    }

    @Test
    @DisplayName("XOR is commutative — order of placements doesn't matter")
    void xorCommutative() {
        ZobristHasher h = new ZobristHasher(4, 4, 16, 42L);
        long kA = h.keyOf(0, 0, 1, 0);
        long kB = h.keyOf(1, 1, 2, 1);
        long kC = h.keyOf(2, 2, 3, 2);
        long order1 = 0L ^ kA ^ kB ^ kC;
        long order2 = 0L ^ kC ^ kA ^ kB;
        long order3 = 0L ^ kB ^ kC ^ kA;
        assertEquals(order1, order2);
        assertEquals(order2, order3);
    }

    @Test
    @DisplayName("out-of-range args return 0 (safe no-op XOR)")
    void outOfRangeReturnsZero() {
        ZobristHasher h = new ZobristHasher(4, 4, 16, 42L);
        assertEquals(0L, h.keyOf(-1, 0, 1, 0));
        assertEquals(0L, h.keyOf(4, 0, 1, 0));
        assertEquals(0L, h.keyOf(0, -1, 1, 0));
        assertEquals(0L, h.keyOf(0, 4, 1, 0));
        assertEquals(0L, h.keyOf(0, 0, 17, 0)); // pid past max
    }

    @Test
    @DisplayName("rotation is normalized modulo 4")
    void rotationModulo() {
        ZobristHasher h = new ZobristHasher(4, 4, 16, 42L);
        assertEquals(h.keyOf(0, 0, 1, 0), h.keyOf(0, 0, 1, 4));
        assertEquals(h.keyOf(0, 0, 1, 1), h.keyOf(0, 0, 1, 5));
    }
}
