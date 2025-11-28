package solver;

import model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EdgeCompatibilityIndex class.
 * Tests edge compatibility lookup tables and piece querying.
 */
@DisplayName("EdgeCompatibilityIndex Tests")
public class EdgeCompatibilityIndexTest {

    private Map<Integer, Piece> pieces;
    private EdgeCompatibilityIndex index;

    @BeforeEach
    public void setUp() {
        pieces = createTestPieces();
    }

    /**
     * Create a small set of test pieces with known edge values.
     * Piece 1: [1, 2, 3, 4] (North, East, South, West)
     * Piece 2: [3, 5, 1, 2] (matches Piece 1's South with its North)
     * Piece 3: [0, 0, 0, 0] (border piece)
     * Piece 4: [2, 3, 4, 5]
     */
    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{1, 2, 3, 4}));
        pieces.put(2, new Piece(2, new int[]{3, 5, 1, 2}));
        pieces.put(3, new Piece(3, new int[]{0, 0, 0, 0}));
        pieces.put(4, new Piece(4, new int[]{2, 3, 4, 5}));
        return pieces;
    }

    @Test
    @DisplayName("Build tables successfully with valid pieces")
    public void testBuildTablesSuccessfully() {
        assertDoesNotThrow(() -> {
            index = new EdgeCompatibilityIndex(pieces, false);
        });
    }

    @Test
    @DisplayName("Get north compatible pieces for edge value 3")
    public void testGetNorthCompatiblePieces() {
        index = new EdgeCompatibilityIndex(pieces, false);

        // Piece 1 has north=1, south=3 (rotated 180° → north=3)
        // Piece 2 has north=3
        // Piece 4 has east=3 (rotated 270° → north=3)
        Set<Integer> compatible = index.getNorthCompatible(3);

        assertNotNull(compatible);
        assertTrue(compatible.contains(1), "Piece 1 should provide north=3 when rotated");
        assertTrue(compatible.contains(2), "Piece 2 should provide north=3");
        assertTrue(compatible.contains(4), "Piece 4 should provide north=3 when rotated");
    }

    @Test
    @DisplayName("Get pieces for border edge value 0")
    public void testGetBorderCompatiblePieces() {
        index = new EdgeCompatibilityIndex(pieces, false);

        // Only Piece 3 has edge value 0
        Set<Integer> compatible = index.getNorthCompatible(0);

        assertNotNull(compatible);
        assertTrue(compatible.contains(3), "Piece 3 should provide north=0");
    }

    @Test
    @DisplayName("Get empty set for non-existent edge value")
    public void testGetNonExistentEdgeValue() {
        index = new EdgeCompatibilityIndex(pieces, false);

        // Edge value 99 doesn't exist in any piece
        Set<Integer> compatible = index.getNorthCompatible(99);

        // Should return empty set, not null
        assertNotNull(compatible);
        assertTrue(compatible.isEmpty(), "Non-existent edge value should return empty set");
    }

    @Test
    @DisplayName("Get edge compatible pieces with multiple constraints")
    public void testGetEdgeCompatiblePiecesMultipleConstraints() {
        index = new EdgeCompatibilityIndex(pieces, false);

        // Looking for pieces that can provide:
        // - South edge = 3 (to match a neighbor's North edge)
        // Using the new API: getSouthCompatible(edgeValue)
        Set<Integer> compatible = index.getSouthCompatible(3);

        assertNotNull(compatible);
        assertTrue(compatible.contains(1), "Piece 1 has south=3");
        // Piece 4 rotated 90° has south=3
        assertTrue(compatible.contains(4), "Piece 4 can provide south=3 when rotated");
    }

    @Test
    @DisplayName("All four directional queries work correctly")
    public void testAllDirectionalQueries() {
        index = new EdgeCompatibilityIndex(pieces, false);

        // Test all four directions
        Set<Integer> north = index.getNorthCompatible(1);
        Set<Integer> east = index.getEastCompatible(2);
        Set<Integer> south = index.getSouthCompatible(3);
        Set<Integer> west = index.getWestCompatible(4);

        assertNotNull(north);
        assertNotNull(east);
        assertNotNull(south);
        assertNotNull(west);

        // Piece 1 has [1, 2, 3, 4]
        assertTrue(north.contains(1), "Piece 1 has north=1");
        assertTrue(east.contains(1), "Piece 1 has east=2");
        assertTrue(south.contains(1), "Piece 1 has south=3");
        assertTrue(west.contains(1), "Piece 1 has west=4");
    }

    @Test
    @DisplayName("Empty pieces map creates empty tables")
    public void testEmptyPiecesMap() {
        Map<Integer, Piece> emptyPieces = new HashMap<>();
        index = new EdgeCompatibilityIndex(emptyPieces, false);

        Set<Integer> compatible = index.getNorthCompatible(1);

        assertNotNull(compatible);
        assertTrue(compatible.isEmpty(), "Empty pieces should produce empty compatibility sets");
    }

    @Test
    @DisplayName("Rotations are correctly indexed")
    public void testRotationsAreIndexed() {
        index = new EdgeCompatibilityIndex(pieces, false);

        // Piece 1 has [1, 2, 3, 4] (N, E, S, W)
        // When rotated 90° clockwise: [4, 1, 2, 3]
        // So piece 1 can provide north=4 when rotated 90°

        Set<Integer> northFour = index.getNorthCompatible(4);
        assertTrue(northFour.contains(1), "Piece 1 should be indexed for north=4 (rotated 90°)");

        // When rotated 180°: [3, 4, 1, 2]
        Set<Integer> northThree = index.getNorthCompatible(3);
        assertTrue(northThree.contains(1), "Piece 1 should be indexed for north=3 (rotated 180°)");

        // When rotated 270°: [2, 3, 4, 1]
        Set<Integer> northTwo = index.getNorthCompatible(2);
        assertTrue(northTwo.contains(1), "Piece 1 should be indexed for north=2 (rotated 270°)");
    }

    @Test
    @DisplayName("Multiple pieces can provide same edge")
    public void testMultiplePiecesSameEdge() {
        index = new EdgeCompatibilityIndex(pieces, false);

        // Both Piece 1 and Piece 4 have edge value 2
        // Piece 1 has east=2
        // Piece 4 has north=2
        Set<Integer> northTwo = index.getNorthCompatible(2);

        assertTrue(northTwo.contains(4), "Piece 4 has north=2");
        assertTrue(northTwo.contains(1), "Piece 1 has east=2, which becomes north when rotated");
    }

    // Note: getDifficultyScore() method was removed during refactoring
    // This functionality is now handled by LeastConstrainingValueOrderer.computePieceDifficulty()
    // @Test
    // @DisplayName("Get difficulty score for piece")
    // public void testGetDifficultyScore() {
    //     index = new EdgeCompatibilityIndex(pieces, false);
    //     // Test removed - functionality moved to different class
    // }

    @Test
    @DisplayName("Verbose mode doesn't throw exceptions")
    public void testVerboseMode() {
        assertDoesNotThrow(() -> {
            index = new EdgeCompatibilityIndex(pieces, true);
            index.getNorthCompatible(1);
        });
    }
}
