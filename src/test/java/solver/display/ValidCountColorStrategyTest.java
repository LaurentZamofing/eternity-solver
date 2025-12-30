package solver.display;

import model.Board;
import model.Piece;
import solver.PlacementValidator;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ValidCountColorStrategy.
 * Tests deadend detection, critical warnings, and highlight colors.
 */
@DisplayName("ValidCountColorStrategy Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ValidCountColorStrategyTest {

    private Board board;
    private Map<Integer, Piece> piecesById;
    private List<Integer> unusedIds;
    private ValidPieceCounter counter;
    private Set<String> highlightedPositions;
    private ValidCountColorStrategy strategy;

    @BeforeEach
    void setUp() {
        board = new Board(3, 3);
        piecesById = createTestPieces();
        unusedIds = Arrays.asList(1, 2, 3, 4, 5);
        counter = new ValidPieceCounter(createMockValidator());
        highlightedPositions = new HashSet<>();
    }

    // ==================== Deadend Detection Tests ====================

    @Test
    @Order(1)
    @DisplayName("getCellColor - deadend (0 rotations) returns bright red")
    void testDeadendColor() {
        // Create mock counter that returns 0 rotations
        ValidPieceCounter deadendCounter = new ValidPieceCounter(createDeadendValidator());
        strategy = new ValidCountColorStrategy(deadendCounter, piecesById, unusedIds);

        String color = strategy.getCellColor(board, 0, 0);

        assertEquals(ColorStrategy.BRIGHT_RED, color,
            "Deadend (0 rotations) should return bright red");
    }

    @Test
    @Order(2)
    @DisplayName("getCellColor - critical (1-5 rotations) returns bright yellow")
    void testCriticalColor() {
        // Mock counter that returns 3 rotations
        ValidPieceCounter criticalCounter = new ValidPieceCounter(createLimitedValidator(3));
        strategy = new ValidCountColorStrategy(criticalCounter, piecesById, unusedIds);

        String color = strategy.getCellColor(board, 0, 0);

        assertEquals(ColorStrategy.BRIGHT_YELLOW, color,
            "Critical (1-5 rotations) should return bright yellow");
    }

    @Test
    @Order(3)
    @DisplayName("getCellColor - warning (6-20 rotations) returns yellow")
    void testWarningColor() {
        // Mock counter that returns 15 rotations
        ValidPieceCounter warningCounter = new ValidPieceCounter(createLimitedValidator(15));
        strategy = new ValidCountColorStrategy(warningCounter, piecesById, unusedIds);

        String color = strategy.getCellColor(board, 0, 0);

        assertEquals(ColorStrategy.YELLOW, color,
            "Warning (6-20 rotations) should return yellow");
    }

    @Test
    @Order(4)
    @DisplayName("getCellColor - plenty of options (21+ rotations) returns empty")
    void testPlentyOfOptionsColor() {
        // Mock counter that returns 30 rotations
        ValidPieceCounter plentyCounter = new ValidPieceCounter(createLimitedValidator(30));
        strategy = new ValidCountColorStrategy(plentyCounter, piecesById, unusedIds);

        String color = strategy.getCellColor(board, 0, 0);

        assertEquals("", color, "Plenty of options (21+ rotations) should return no color");
    }

    // ==================== Highlighted Position Tests (NEW) ====================

    @Test
    @Order(5)
    @DisplayName("getCellColor - highlighted empty cell (next target) returns bold blue")
    void testHighlightedEmptyCellColor() {
        highlightedPositions.add("0,0");
        strategy = new ValidCountColorStrategy(counter, piecesById, unusedIds, highlightedPositions);

        String color = strategy.getCellColor(board, 0, 0);

        assertEquals(ColorStrategy.BOLD + ColorStrategy.BLUE, color,
            "Highlighted empty cell (next target) should return bold blue");
    }

    @Test
    @Order(6)
    @DisplayName("getCellColor - highlighted overrides deadend color")
    void testHighlightedOverridesDeadend() {
        highlightedPositions.add("0,0");
        ValidPieceCounter deadendCounter = new ValidPieceCounter(createDeadendValidator());
        strategy = new ValidCountColorStrategy(deadendCounter, piecesById, unusedIds, highlightedPositions);

        String color = strategy.getCellColor(board, 0, 0);

        assertEquals(ColorStrategy.BOLD + ColorStrategy.BLUE, color,
            "Highlighted should override deadend red");
    }

    @Test
    @Order(7)
    @DisplayName("getCellColor - highlighted overrides critical color")
    void testHighlightedOverridesCritical() {
        highlightedPositions.add("0,0");
        ValidPieceCounter criticalCounter = new ValidPieceCounter(createLimitedValidator(3));
        strategy = new ValidCountColorStrategy(criticalCounter, piecesById, unusedIds, highlightedPositions);

        String color = strategy.getCellColor(board, 0, 0);

        assertEquals(ColorStrategy.BOLD + ColorStrategy.BLUE, color,
            "Highlighted should override critical yellow");
    }

    // ==================== Occupied Cell Tests ====================

    @Test
    @Order(8)
    @DisplayName("getCellColor - occupied cell returns empty")
    void testOccupiedCellColor() {
        board.place(0, 0, new Piece(1, new int[]{0, 1, 2, 0}), 0);
        strategy = new ValidCountColorStrategy(counter, piecesById, unusedIds);

        String color = strategy.getCellColor(board, 0, 0);

        assertEquals("", color, "Occupied cell should return empty (no coloring)");
    }

    @Test
    @Order(9)
    @DisplayName("getEdgeColor - always returns empty (cell colors only)")
    void testEdgeColorAlwaysEmpty() {
        strategy = new ValidCountColorStrategy(counter, piecesById, unusedIds);

        String northColor = strategy.getEdgeColor(board, 0, 0, 0);
        String eastColor = strategy.getEdgeColor(board, 0, 0, 1);
        String southColor = strategy.getEdgeColor(board, 0, 0, 2);
        String westColor = strategy.getEdgeColor(board, 0, 0, 3);

        assertEquals("", northColor, "ValidCount mode doesn't color edges");
        assertEquals("", eastColor, "ValidCount mode doesn't color edges");
        assertEquals("", southColor, "ValidCount mode doesn't color edges");
        assertEquals("", westColor, "ValidCount mode doesn't color edges");
    }

    // ==================== Boundary Tests ====================

    @Test
    @Order(10)
    @DisplayName("getCellColor - exactly 5 rotations returns bright yellow")
    void testBoundaryFiveRotations() {
        ValidPieceCounter boundaryCounter = new ValidPieceCounter(createLimitedValidator(5));
        strategy = new ValidCountColorStrategy(boundaryCounter, piecesById, unusedIds);

        String color = strategy.getCellColor(board, 0, 0);

        assertEquals(ColorStrategy.BRIGHT_YELLOW, color,
            "Exactly 5 rotations should return bright yellow (critical)");
    }

    @Test
    @Order(11)
    @DisplayName("getCellColor - exactly 6 rotations returns yellow")
    void testBoundarySixRotations() {
        ValidPieceCounter boundaryCounter = new ValidPieceCounter(createLimitedValidator(6));
        strategy = new ValidCountColorStrategy(boundaryCounter, piecesById, unusedIds);

        String color = strategy.getCellColor(board, 0, 0);

        assertEquals(ColorStrategy.YELLOW, color,
            "Exactly 6 rotations should return yellow (warning)");
    }

    @Test
    @Order(12)
    @DisplayName("getCellColor - exactly 20 rotations returns yellow")
    void testBoundaryTwentyRotations() {
        ValidPieceCounter boundaryCounter = new ValidPieceCounter(createLimitedValidator(20));
        strategy = new ValidCountColorStrategy(boundaryCounter, piecesById, unusedIds);

        String color = strategy.getCellColor(board, 0, 0);

        assertEquals(ColorStrategy.YELLOW, color,
            "Exactly 20 rotations should return yellow (warning)");
    }

    @Test
    @Order(13)
    @DisplayName("getCellColor - exactly 21 rotations returns empty")
    void testBoundaryTwentyOneRotations() {
        ValidPieceCounter boundaryCounter = new ValidPieceCounter(createLimitedValidator(21));
        strategy = new ValidCountColorStrategy(boundaryCounter, piecesById, unusedIds);

        String color = strategy.getCellColor(board, 0, 0);

        assertEquals("", color, "Exactly 21 rotations should return no color");
    }

    // ==================== Helper Methods ====================

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));
        pieces.put(2, new Piece(2, new int[]{0, 2, 3, 1}));
        pieces.put(3, new Piece(3, new int[]{5, 7, 9, 10}));
        pieces.put(4, new Piece(4, new int[]{2, 4, 5, 0}));
        pieces.put(5, new Piece(5, new int[]{3, 5, 6, 4}));
        return pieces;
    }

    private PlacementValidator createMockValidator() {
        return new PlacementValidator(null, null, null) {
            @Override
            public boolean fits(Board board, int row, int col, int[] edges) {
                return true; // Accept everything for basic tests
            }
        };
    }

    private PlacementValidator createDeadendValidator() {
        return new PlacementValidator(null, null, null) {
            @Override
            public boolean fits(Board board, int row, int col, int[] edges) {
                return false; // Reject everything -> 0 rotations
            }
        };
    }

    private PlacementValidator createLimitedValidator(int targetRotations) {
        return new PlacementValidator(null, null, null) {
            private int callCount = 0;

            @Override
            public boolean fits(Board board, int row, int col, int[] edges) {
                callCount++;
                // Return true for first N calls to simulate N valid rotations
                return callCount <= targetRotations;
            }
        };
    }
}
