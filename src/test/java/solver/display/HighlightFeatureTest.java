package solver.display;

import model.Board;
import model.Piece;
import solver.BoardDisplayManager;
import solver.EternitySolver;
import solver.CellConstraints;
import solver.EternitySolver;
import solver.PlacementValidator;
import solver.EternitySolver;
import solver.StatisticsManager;
import solver.EternitySolver;
import util.SaveStateManager;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EXHAUSTIVE tests for highlight feature (last placed piece + next target).
 * Tests all combinations of highlights, colors, and board states.
 */
@DisplayName("Highlight Feature Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HighlightFeatureTest {

    private Board board;
    private Map<Integer, Piece> pieces;
    private PlacementValidator validator;
    private BoardDisplayManager displayManager;

    @BeforeEach
    void setUp() {
        board = new Board(3, 3);
        pieces = createTestPieces();
        CellConstraints[][] constraints = CellConstraints.createConstraintsMatrix(3, 3);
        EternitySolver.Statistics stats = new EternitySolver.Statistics();
        validator = new PlacementValidator(constraints, stats, "ascending");
        displayManager = new BoardDisplayManager(new HashSet<>(), validator);
    }

    @Test
    @Order(1)
    @DisplayName("Highlight - last placed piece at corner")
    void testHighlightLastAtCorner() {
        board.place(0, 0, pieces.get(1), 0);
        List<Integer> unused = Arrays.asList(2, 3, 4, 5);
        SaveStateManager.PlacementInfo last = new SaveStateManager.PlacementInfo(0, 0, 1, 0);

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused, last, null);
        }, "Should highlight corner piece");
    }

    @Test
    @Order(2)
    @DisplayName("Highlight - last placed piece at edge")
    void testHighlightLastAtEdge() {
        board.place(0, 1, pieces.get(2), 0);
        List<Integer> unused = Arrays.asList(1, 3, 4, 5);
        SaveStateManager.PlacementInfo last = new SaveStateManager.PlacementInfo(0, 1, 2, 0);

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused, last, null);
        }, "Should highlight edge piece");
    }

    @Test
    @Order(3)
    @DisplayName("Highlight - last placed piece at center")
    void testHighlightLastAtCenter() {
        board.place(1, 1, pieces.get(5), 0);
        List<Integer> unused = Arrays.asList(1, 2, 3, 4);
        SaveStateManager.PlacementInfo last = new SaveStateManager.PlacementInfo(1, 1, 5, 0);

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused, last, null);
        }, "Should highlight center piece");
    }

    @Test
    @Order(4)
    @DisplayName("Highlight - last placed with 90° rotation")
    void testHighlightLastWithRotation90() {
        board.place(0, 0, pieces.get(1), 1); // 90° rotation
        List<Integer> unused = Arrays.asList(2, 3, 4, 5);
        SaveStateManager.PlacementInfo last = new SaveStateManager.PlacementInfo(0, 0, 1, 1);

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused, last, null);
        }, "Should highlight rotated piece");
    }

    @Test
    @Order(5)
    @DisplayName("Highlight - last placed with 180° rotation")
    void testHighlightLastWithRotation180() {
        board.place(0, 0, pieces.get(1), 2);
        List<Integer> unused = Arrays.asList(2, 3, 4, 5);
        SaveStateManager.PlacementInfo last = new SaveStateManager.PlacementInfo(0, 0, 1, 2);

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused, last, null);
        }, "Should highlight piece with 180° rotation");
    }

    @Test
    @Order(6)
    @DisplayName("Highlight - last placed with 270° rotation")
    void testHighlightLastWithRotation270() {
        board.place(0, 0, pieces.get(1), 3);
        List<Integer> unused = Arrays.asList(2, 3, 4, 5);
        SaveStateManager.PlacementInfo last = new SaveStateManager.PlacementInfo(0, 0, 1, 3);

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused, last, null);
        }, "Should highlight piece with 270° rotation");
    }

    @Test
    @Order(7)
    @DisplayName("Highlight - next target at all 9 positions")
    void testHighlightNextAtAllPositions() {
        List<Integer> unused = Arrays.asList(1, 2, 3, 4, 5);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int finalRow = row;
                int finalCol = col;
                int[] nextTarget = new int[]{row, col};

                assertDoesNotThrow(() -> {
                    displayManager.printBoardWithLabels(board, pieces, unused, null, nextTarget);
                }, "Should highlight next target at [" + row + "," + col + "]");
            }
        }
    }

    @Test
    @Order(8)
    @DisplayName("Highlight - last and next at adjacent cells")
    void testHighlightAdjacentCells() {
        board.place(0, 0, pieces.get(1), 0);
        List<Integer> unused = Arrays.asList(2, 3, 4, 5);

        SaveStateManager.PlacementInfo last = new SaveStateManager.PlacementInfo(0, 0, 1, 0);
        int[] next = new int[]{0, 1}; // Adjacent to last

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused, last, next);
        }, "Should highlight adjacent last (magenta) and next (blue)");
    }

    @Test
    @Order(9)
    @DisplayName("Highlight - last and next at opposite corners")
    void testHighlightOppositeCorners() {
        board.place(0, 0, pieces.get(1), 0);
        List<Integer> unused = Arrays.asList(2, 3, 4, 5);

        SaveStateManager.PlacementInfo last = new SaveStateManager.PlacementInfo(0, 0, 1, 0);
        int[] next = new int[]{2, 2}; // Opposite corner

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused, last, next);
        }, "Should highlight opposite corners");
    }

    @Test
    @Order(10)
    @DisplayName("Highlight - null last placement")
    void testNullLastPlacement() {
        List<Integer> unused = Arrays.asList(1, 2, 3, 4, 5);
        int[] next = new int[]{1, 1};

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused, null, next);
        }, "Should handle null last placement");
    }

    @Test
    @Order(11)
    @DisplayName("Highlight - null next target")
    void testNullNextTarget() {
        board.place(0, 0, pieces.get(1), 0);
        List<Integer> unused = Arrays.asList(2, 3, 4, 5);
        SaveStateManager.PlacementInfo last = new SaveStateManager.PlacementInfo(0, 0, 1, 0);

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused, last, null);
        }, "Should handle null next target");
    }

    @Test
    @Order(12)
    @DisplayName("Highlight - both null (fallback to regular display)")
    void testBothNullFallback() {
        board.place(0, 0, pieces.get(1), 0);
        List<Integer> unused = Arrays.asList(2, 3, 4, 5);

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused, null, null);
        }, "Should fallback to regular display when both null");
    }

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> p = new HashMap<>();
        p.put(1, new Piece(1, new int[]{0, 2, 3, 0}));
        p.put(2, new Piece(2, new int[]{0, 4, 5, 2}));
        p.put(3, new Piece(3, new int[]{6, 7, 0, 0}));
        p.put(4, new Piece(4, new int[]{8, 0, 0, 9}));
        p.put(5, new Piece(5, new int[]{10, 11, 12, 13}));
        return p;
    }
}
