package util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PositionKey utility class.
 */
public class PositionKeyTest {

    @Test
    void testConstructor() {
        PositionKey key = new PositionKey(5, 10);
        assertEquals(5, key.getRow());
        assertEquals(10, key.getCol());
    }

    @Test
    void testToString() {
        PositionKey key = new PositionKey(2, 3);
        assertEquals("2,3", key.toString());
    }

    @Test
    void testToString_ZeroValues() {
        PositionKey key = new PositionKey(0, 0);
        assertEquals("0,0", key.toString());
    }

    @Test
    void testEquals_SameValues() {
        PositionKey key1 = new PositionKey(2, 3);
        PositionKey key2 = new PositionKey(2, 3);
        assertEquals(key1, key2);
    }

    @Test
    void testEquals_DifferentRow() {
        PositionKey key1 = new PositionKey(2, 3);
        PositionKey key2 = new PositionKey(3, 3);
        assertNotEquals(key1, key2);
    }

    @Test
    void testEquals_DifferentCol() {
        PositionKey key1 = new PositionKey(2, 3);
        PositionKey key2 = new PositionKey(2, 4);
        assertNotEquals(key1, key2);
    }

    @Test
    void testEquals_SameInstance() {
        PositionKey key = new PositionKey(2, 3);
        assertEquals(key, key);
    }

    @Test
    void testEquals_Null() {
        PositionKey key = new PositionKey(2, 3);
        assertNotEquals(key, null);
    }

    @Test
    void testEquals_DifferentClass() {
        PositionKey key = new PositionKey(2, 3);
        String notAKey = "2,3";
        assertNotEquals(key, notAKey);
    }

    @Test
    void testHashCode_SameValues() {
        PositionKey key1 = new PositionKey(2, 3);
        PositionKey key2 = new PositionKey(2, 3);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    void testHashCode_DifferentValues() {
        PositionKey key1 = new PositionKey(2, 3);
        PositionKey key2 = new PositionKey(3, 2);
        // Not guaranteed to be different, but very likely
        assertNotEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    void testAsMapKey() {
        Map<PositionKey, String> map = new HashMap<>();
        PositionKey key1 = new PositionKey(2, 3);
        PositionKey key2 = new PositionKey(2, 3); // Same values, different object

        map.put(key1, "value1");
        assertEquals("value1", map.get(key2), "Should retrieve value with equivalent key");
        assertTrue(map.containsKey(key2), "Map should contain equivalent key");
    }

    @Test
    void testAsMapKey_MultiplePairs() {
        Map<PositionKey, Integer> placementOrder = new HashMap<>();

        placementOrder.put(new PositionKey(0, 0), 1);
        placementOrder.put(new PositionKey(0, 1), 2);
        placementOrder.put(new PositionKey(1, 0), 3);

        assertEquals(1, placementOrder.get(new PositionKey(0, 0)));
        assertEquals(2, placementOrder.get(new PositionKey(0, 1)));
        assertEquals(3, placementOrder.get(new PositionKey(1, 0)));
        assertNull(placementOrder.get(new PositionKey(2, 2)));
    }

    @Test
    void testParse_Valid() {
        PositionKey key = PositionKey.parse("5,10");
        assertEquals(5, key.getRow());
        assertEquals(10, key.getCol());
    }

    @Test
    void testParse_ValidWithSpaces() {
        PositionKey key = PositionKey.parse(" 5 , 10 ");
        assertEquals(5, key.getRow());
        assertEquals(10, key.getCol());
    }

    @Test
    void testParse_ZeroValues() {
        PositionKey key = PositionKey.parse("0,0");
        assertEquals(0, key.getRow());
        assertEquals(0, key.getCol());
    }

    @Test
    void testParse_LargeValues() {
        PositionKey key = PositionKey.parse("1000,2000");
        assertEquals(1000, key.getRow());
        assertEquals(2000, key.getCol());
    }

    @Test
    void testParse_Null() {
        assertThrows(IllegalArgumentException.class, () -> PositionKey.parse(null));
    }

    @Test
    void testParse_Empty() {
        assertThrows(IllegalArgumentException.class, () -> PositionKey.parse(""));
    }

    @Test
    void testParse_MissingComma() {
        assertThrows(IllegalArgumentException.class, () -> PositionKey.parse("5 10"));
    }

    @Test
    void testParse_ExtraComma() {
        assertThrows(IllegalArgumentException.class, () -> PositionKey.parse("5,10,15"));
    }

    @Test
    void testParse_InvalidNumber() {
        assertThrows(IllegalArgumentException.class, () -> PositionKey.parse("five,10"));
    }

    @Test
    void testParse_InvalidNumber2() {
        assertThrows(IllegalArgumentException.class, () -> PositionKey.parse("5,ten"));
    }

    @Test
    void testParse_RoundTrip() {
        PositionKey original = new PositionKey(7, 13);
        String str = original.toString();
        PositionKey parsed = PositionKey.parse(str);
        assertEquals(original, parsed);
    }

    @Test
    void testNegativeValues() {
        // PositionKey allows negative values (no validation)
        // Validation should be done at Board level
        PositionKey key = new PositionKey(-1, -1);
        assertEquals(-1, key.getRow());
        assertEquals(-1, key.getCol());
    }

    @Test
    void testLargeBoard_16x16() {
        Map<PositionKey, Integer> positions = new HashMap<>();
        int counter = 0;

        // Fill 16x16 board positions
        for (int r = 0; r < 16; r++) {
            for (int c = 0; c < 16; c++) {
                positions.put(new PositionKey(r, c), counter++);
            }
        }

        assertEquals(256, positions.size());

        // Verify random accesses
        assertEquals(0, positions.get(new PositionKey(0, 0)));
        assertEquals(15, positions.get(new PositionKey(0, 15)));
        assertEquals(255, positions.get(new PositionKey(15, 15)));
    }
}
