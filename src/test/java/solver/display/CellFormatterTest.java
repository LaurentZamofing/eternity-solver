package solver.display;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for CellFormatter.
 * Tests formatting of occupied cells, empty cells, and rotation display.
 */
@DisplayName("CellFormatter Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CellFormatterTest {

    private Board board;
    private ColorStrategy mockColorStrategy;
    private CellFormatter formatter;

    @BeforeEach
    void setUp() {
        board = new Board(3, 3);

        // Mock color strategy that returns no colors (for basic tests)
        mockColorStrategy = new ColorStrategy() {
            @Override
            public String getCellColor(Board board, int row, int col) {
                return "";
            }

            @Override
            public String getEdgeColor(Board board, int row, int col, int direction) {
                return "";
            }
        };

        formatter = new CellFormatter(mockColorStrategy);
    }

    // ==================== Empty Cell Formatting Tests ====================

    @Test
    @Order(1)
    @DisplayName("formatEmptyCell - basic formatting without color")
    void testFormatEmptyCellBasic() {
        String result = formatter.formatEmptyCell(board, 0, 0, 42);

        assertEquals("  ( 42)  ", result, "Should format valid count with padding");
    }

    @Test
    @Order(2)
    @DisplayName("formatEmptyCell - zero count (deadend)")
    void testFormatEmptyCellDeadend() {
        String result = formatter.formatEmptyCell(board, 0, 0, 0);

        assertEquals("  (  0)  ", result, "Should format zero with padding");
    }

    @Test
    @Order(3)
    @DisplayName("formatEmptyCell - single digit count")
    void testFormatEmptyCellSingleDigit() {
        String result = formatter.formatEmptyCell(board, 0, 0, 5);

        assertEquals("  (  5)  ", result, "Should pad single digit correctly");
    }

    @Test
    @Order(4)
    @DisplayName("formatEmptyCell - large count")
    void testFormatEmptyCellLargeCount() {
        String result = formatter.formatEmptyCell(board, 0, 0, 256);

        assertEquals("  (256)  ", result, "Should format large number correctly");
    }

    // ==================== Rotation Display Tests (NEW) ====================

    @Test
    @Order(5)
    @DisplayName("formatEmptyCellWithRotations - 1 piece with 1 rotation")
    void testFormatWithRotations_1p1r() {
        String result = formatter.formatEmptyCellWithRotations(board, 0, 0, 1, 1);

        assertEquals("(  1/  1)", result, "Should display 1/1 with fixed width");
    }

    @Test
    @Order(6)
    @DisplayName("formatEmptyCellWithRotations - 1 piece with 4 rotations")
    void testFormatWithRotations_1p4r() {
        String result = formatter.formatEmptyCellWithRotations(board, 1, 1, 1, 4);

        assertEquals("(  1/  4)", result, "Should display 1/4 with fixed width");
    }

    @Test
    @Order(7)
    @DisplayName("formatEmptyCellWithRotations - multiple pieces with varying rotations")
    void testFormatWithRotations_MultipleP() {
        String result = formatter.formatEmptyCellWithRotations(board, 0, 0, 5, 12);

        assertEquals("(  5/ 12)", result, "Should display 5/12 with fixed width");
    }

    @Test
    @Order(8)
    @DisplayName("formatEmptyCellWithRotations - deadend (0/0)")
    void testFormatWithRotations_Deadend() {
        String result = formatter.formatEmptyCellWithRotations(board, 0, 0, 0, 0);

        assertEquals("(  0/  0)", result, "Should display 0/0 with fixed width");
    }

    @Test
    @Order(9)
    @DisplayName("formatEmptyCellWithRotations - many pieces and rotations")
    void testFormatWithRotations_ManyOptions() {
        String result = formatter.formatEmptyCellWithRotations(board, 0, 0, 50, 120);

        assertTrue(result.contains("50"), "Should contain piece count");
        assertTrue(result.contains("120"), "Should contain rotation count");
    }

    @Test
    @Order(10)
    @DisplayName("formatEmptyCellWithRotations - width consistency")
    void testFormatWithRotations_WidthConsistency() {
        // All formatted cells should have same width for alignment
        String result1 = formatter.formatEmptyCellWithRotations(board, 0, 0, 1, 1);
        String result2 = formatter.formatEmptyCellWithRotations(board, 0, 0, 9, 36);

        // Both should be 9 characters (without ANSI codes)
        assertEquals(result1.length(), result2.length(),
            "Formatted cells should have consistent width");
    }

    // ==================== Occupied Cell Formatting Tests ====================

    @Test
    @Order(11)
    @DisplayName("formatMiddleLine - occupied cell basic")
    void testFormatMiddleLineOccupied() {
        board.place(0, 0, new Piece(42, new int[]{1, 2, 3, 4}), 0);

        String result = formatter.formatMiddleLine(board, 0, 0);

        assertTrue(result.contains("42"), "Should contain piece ID");
        assertTrue(result.contains(" 4"), "Should contain west edge");
        assertTrue(result.contains(" 2"), "Should contain east edge");
    }

    @Test
    @Order(12)
    @DisplayName("formatNorthEdge - occupied cell")
    void testFormatNorthEdgeOccupied() {
        board.place(0, 0, new Piece(1, new int[]{15, 2, 3, 4}), 0);

        String result = formatter.formatNorthEdge(board, 0, 0);

        assertTrue(result.contains("15"), "Should contain north edge value");
    }

    @Test
    @Order(13)
    @DisplayName("formatSouthEdge - occupied cell")
    void testFormatSouthEdgeOccupied() {
        board.place(0, 0, new Piece(1, new int[]{1, 2, 27, 4}), 0);

        String result = formatter.formatSouthEdge(board, 0, 0);

        assertTrue(result.contains("27"), "Should contain south edge value");
    }

    @Test
    @Order(14)
    @DisplayName("formatNorthEdge - empty cell returns spaces")
    void testFormatNorthEdgeEmpty() {
        String result = formatter.formatNorthEdge(board, 0, 0);

        assertEquals("         ", result, "Empty cell should return 9 spaces");
    }

    @Test
    @Order(15)
    @DisplayName("formatSouthEdge - empty cell returns spaces")
    void testFormatSouthEdgeEmpty() {
        String result = formatter.formatSouthEdge(board, 0, 0);

        assertEquals("         ", result, "Empty cell should return 9 spaces");
    }

    // ==================== Color Strategy Tests ====================

    @Test
    @Order(16)
    @DisplayName("formatEmptyCell - with color strategy")
    void testFormatEmptyCellWithColor() {
        // Create color strategy that returns red for deadends
        ColorStrategy coloredStrategy = new ColorStrategy() {
            @Override
            public String getCellColor(Board board, int row, int col) {
                return BRIGHT_RED; // Deadend color
            }

            @Override
            public String getEdgeColor(Board board, int row, int col, int direction) {
                return "";
            }
        };

        CellFormatter coloredFormatter = new CellFormatter(coloredStrategy);
        String result = coloredFormatter.formatEmptyCell(board, 0, 0, 0);

        assertTrue(result.contains(ColorStrategy.BRIGHT_RED), "Should include color code");
        assertTrue(result.contains(ColorStrategy.RESET), "Should include reset code");
        assertTrue(result.contains("0"), "Should contain count value");
    }

    @Test
    @Order(17)
    @DisplayName("formatEmptyCellWithRotations - with color strategy")
    void testFormatWithRotationsWithColor() {
        // Create color strategy for critical cells
        ColorStrategy coloredStrategy = new ColorStrategy() {
            @Override
            public String getCellColor(Board board, int row, int col) {
                return BRIGHT_YELLOW; // Critical color
            }

            @Override
            public String getEdgeColor(Board board, int row, int col, int direction) {
                return "";
            }
        };

        CellFormatter coloredFormatter = new CellFormatter(coloredStrategy);
        String result = coloredFormatter.formatEmptyCellWithRotations(board, 0, 0, 2, 5);

        assertTrue(result.contains(ColorStrategy.BRIGHT_YELLOW), "Should include color code");
        assertTrue(result.contains(ColorStrategy.RESET), "Should include reset code");
        assertTrue(result.contains("2"), "Should contain piece count");
        assertTrue(result.contains("5"), "Should contain rotation count");
    }

    @Test
    @Order(18)
    @DisplayName("formatMiddleLine - with edge colors")
    void testFormatMiddleLineWithEdgeColors() {
        board.place(0, 0, new Piece(1, new int[]{1, 2, 3, 4}), 0);

        // Create strategy that colors edges green/red
        ColorStrategy coloredStrategy = new ColorStrategy() {
            @Override
            public String getCellColor(Board board, int row, int col) {
                return "";
            }

            @Override
            public String getEdgeColor(Board board, int row, int col, int direction) {
                return (direction == 1) ? GREEN : RED; // East=green, West=red
            }
        };

        CellFormatter coloredFormatter = new CellFormatter(coloredStrategy);
        String result = coloredFormatter.formatMiddleLine(board, 0, 0);

        assertTrue(result.contains(ColorStrategy.GREEN), "Should include green for east");
        assertTrue(result.contains(ColorStrategy.RED), "Should include red for west");
    }
}
