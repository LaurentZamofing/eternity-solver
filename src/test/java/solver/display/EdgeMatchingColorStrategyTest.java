package solver.display;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for EdgeMatchingColorStrategy.
 * Tests edge validation colors, fixed positions, and highlight features.
 */
@DisplayName("EdgeMatchingColorStrategy Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EdgeMatchingColorStrategyTest {

    private Board board;
    private Set<String> fixedPositions;
    private Set<String> highlightedPositions;
    private EdgeMatchingColorStrategy strategy;

    @BeforeEach
    void setUp() {
        board = new Board(3, 3);
        fixedPositions = new HashSet<>();
        highlightedPositions = new HashSet<>();
    }

    // ==================== Fixed Position Tests ====================

    @Test
    @Order(1)
    @DisplayName("getCellColor - fixed position returns bright cyan")
    void testFixedPositionColor() {
        fixedPositions.add("1,1");
        strategy = new EdgeMatchingColorStrategy(board, fixedPositions);

        String color = strategy.getCellColor(board, 1, 1);

        assertEquals(ColorStrategy.BRIGHT_CYAN, color,
            "Fixed position should return bright cyan");
    }

    @Test
    @Order(2)
    @DisplayName("getCellColor - non-fixed position returns empty")
    void testNonFixedPositionColor() {
        strategy = new EdgeMatchingColorStrategy(board, fixedPositions);

        String color = strategy.getCellColor(board, 1, 1);

        assertEquals("", color, "Non-fixed position should return empty string");
    }

    // ==================== Highlighted Position Tests (NEW) ====================

    @Test
    @Order(3)
    @DisplayName("getCellColor - highlighted position returns bright magenta")
    void testHighlightedPositionColor() {
        highlightedPositions.add("1,1");
        strategy = new EdgeMatchingColorStrategy(board, fixedPositions, highlightedPositions);

        String color = strategy.getCellColor(board, 1, 1);

        assertEquals(ColorStrategy.BRIGHT_MAGENTA, color,
            "Highlighted position (last placed piece) should return bright magenta");
    }

    @Test
    @Order(4)
    @DisplayName("getCellColor - highlighted takes precedence over fixed")
    void testHighlightedOverridesFixed() {
        fixedPositions.add("1,1");
        highlightedPositions.add("1,1");
        strategy = new EdgeMatchingColorStrategy(board, fixedPositions, highlightedPositions);

        String color = strategy.getCellColor(board, 1, 1);

        assertEquals(ColorStrategy.BRIGHT_MAGENTA, color,
            "Highlighted should take precedence over fixed");
    }

    @Test
    @Order(5)
    @DisplayName("getEdgeColor - highlighted cell has magenta edges")
    void testHighlightedEdgeColor() {
        board.place(1, 1, new Piece(1, new int[]{1, 2, 3, 4}), 0);
        highlightedPositions.add("1,1");
        strategy = new EdgeMatchingColorStrategy(board, fixedPositions, highlightedPositions);

        String northColor = strategy.getEdgeColor(board, 1, 1, 0);
        String eastColor = strategy.getEdgeColor(board, 1, 1, 1);
        String southColor = strategy.getEdgeColor(board, 1, 1, 2);
        String westColor = strategy.getEdgeColor(board, 1, 1, 3);

        assertEquals(ColorStrategy.BRIGHT_MAGENTA, northColor, "North edge should be magenta");
        assertEquals(ColorStrategy.BRIGHT_MAGENTA, eastColor, "East edge should be magenta");
        assertEquals(ColorStrategy.BRIGHT_MAGENTA, southColor, "South edge should be magenta");
        assertEquals(ColorStrategy.BRIGHT_MAGENTA, westColor, "West edge should be magenta");
    }

    // ==================== Edge Matching Tests ====================

    @Test
    @Order(6)
    @DisplayName("getEdgeColor - matching north edge returns green")
    void testMatchingNorthEdge() {
        // Place two pieces with matching edges
        board.place(0, 1, new Piece(1, new int[]{0, 2, 5, 4}), 0);  // South=5
        board.place(1, 1, new Piece(2, new int[]{5, 6, 7, 8}), 0);  // North=5 (matches!)

        strategy = new EdgeMatchingColorStrategy(board, fixedPositions);

        String color = strategy.getEdgeColor(board, 1, 1, 0); // North

        assertEquals(ColorStrategy.GREEN, color, "Matching north edge should be green");
    }

    @Test
    @Order(7)
    @DisplayName("getEdgeColor - mismatched north edge returns red")
    void testMismatchedNorthEdge() {
        // Place two pieces with non-matching edges
        board.place(0, 1, new Piece(1, new int[]{0, 2, 5, 4}), 0);  // South=5
        board.place(1, 1, new Piece(2, new int[]{99, 6, 7, 8}), 0); // North=99 (doesn't match!)

        strategy = new EdgeMatchingColorStrategy(board, fixedPositions);

        String color = strategy.getEdgeColor(board, 1, 1, 0); // North

        assertEquals(ColorStrategy.RED, color, "Mismatched north edge should be red");
    }

    @Test
    @Order(8)
    @DisplayName("getEdgeColor - matching east edge returns green")
    void testMatchingEastEdge() {
        board.place(1, 1, new Piece(1, new int[]{1, 7, 3, 4}), 0);  // East=7
        board.place(1, 2, new Piece(2, new int[]{5, 6, 7, 7}), 0);  // West=7 (matches!)

        strategy = new EdgeMatchingColorStrategy(board, fixedPositions);

        String color = strategy.getEdgeColor(board, 1, 1, 1); // East

        assertEquals(ColorStrategy.GREEN, color, "Matching east edge should be green");
    }

    @Test
    @Order(9)
    @DisplayName("getEdgeColor - matching south edge returns green")
    void testMatchingSouthEdge() {
        board.place(1, 1, new Piece(1, new int[]{1, 2, 9, 4}), 0);  // South=9
        board.place(2, 1, new Piece(2, new int[]{9, 6, 0, 8}), 0);  // North=9 (matches!)

        strategy = new EdgeMatchingColorStrategy(board, fixedPositions);

        String color = strategy.getEdgeColor(board, 1, 1, 2); // South

        assertEquals(ColorStrategy.GREEN, color, "Matching south edge should be green");
    }

    @Test
    @Order(10)
    @DisplayName("getEdgeColor - matching west edge returns green")
    void testMatchingWestEdge() {
        board.place(1, 0, new Piece(1, new int[]{1, 11, 3, 0}), 0); // East=11
        board.place(1, 1, new Piece(2, new int[]{5, 6, 7, 11}), 0); // West=11 (matches!)

        strategy = new EdgeMatchingColorStrategy(board, fixedPositions);

        String color = strategy.getEdgeColor(board, 1, 1, 3); // West

        assertEquals(ColorStrategy.GREEN, color, "Matching west edge should be green");
    }

    @Test
    @Order(11)
    @DisplayName("getEdgeColor - border edge with no neighbor returns empty")
    void testBorderEdgeNoNeighbor() {
        board.place(0, 0, new Piece(1, new int[]{0, 2, 3, 0}), 0);
        strategy = new EdgeMatchingColorStrategy(board, fixedPositions);

        String northColor = strategy.getEdgeColor(board, 0, 0, 0); // North (no neighbor)
        String westColor = strategy.getEdgeColor(board, 0, 0, 3);  // West (no neighbor)

        assertEquals("", northColor, "Border edge with no neighbor should return empty");
        assertEquals("", westColor, "Border edge with no neighbor should return empty");
    }

    @Test
    @Order(12)
    @DisplayName("getEdgeColor - fixed position edges return empty (cell color used)")
    void testFixedPositionEdges() {
        board.place(1, 1, new Piece(1, new int[]{1, 2, 3, 4}), 0);
        fixedPositions.add("1,1");
        strategy = new EdgeMatchingColorStrategy(board, fixedPositions);

        // Fixed positions use cell color (bright cyan), not edge colors
        String northColor = strategy.getEdgeColor(board, 1, 1, 0);

        assertEquals("", northColor, "Fixed position edges should return empty (cell color used)");
    }

    // ==================== Null/Empty Set Handling ====================

    @Test
    @Order(13)
    @DisplayName("handles null fixedPositions gracefully")
    void testNullFixedPositions() {
        assertDoesNotThrow(() -> {
            strategy = new EdgeMatchingColorStrategy(board, null);
            strategy.getCellColor(board, 0, 0);
        }, "Should handle null fixedPositions");
    }

    @Test
    @Order(14)
    @DisplayName("handles null highlightedPositions gracefully")
    void testNullHighlightedPositions() {
        assertDoesNotThrow(() -> {
            strategy = new EdgeMatchingColorStrategy(board, fixedPositions, null);
            strategy.getCellColor(board, 0, 0);
        }, "Should handle null highlightedPositions");
    }

    @Test
    @Order(15)
    @DisplayName("handles empty sets gracefully")
    void testEmptySets() {
        strategy = new EdgeMatchingColorStrategy(board, new HashSet<>(), new HashSet<>());

        String color = strategy.getCellColor(board, 1, 1);

        assertEquals("", color, "Empty sets should return no color");
    }
}
