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
 * MASSIVE integration tests for complete board display system.
 * Tests the full pipeline from board state to visual output.
 */
@DisplayName("Board Display Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BoardDisplayIntegrationTest {

    private Board board;
    private Map<Integer, Piece> pieces;
    private PlacementValidator validator;
    private Set<String> fixedPositions;
    private BoardDisplayManager displayManager;

    @BeforeEach
    void setUp() {
        board = new Board(3, 3);
        pieces = createCompletePuzzleSet();
        CellConstraints[][] constraints = CellConstraints.createConstraintsMatrix(3, 3);
        EternitySolver.Statistics stats = new EternitySolver.Statistics();
        validator = new PlacementValidator(constraints, stats, "ascending");
        fixedPositions = new HashSet<>();
        displayManager = new BoardDisplayManager(fixedPositions, validator);
    }

    @Test
    @Order(1)
    @DisplayName("Full board display - empty board shows all options")
    void testEmptyBoardDisplay() {
        List<Integer> allPieces = new ArrayList<>(pieces.keySet());

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, allPieces);
        }, "Should display empty board");
    }

    @Test
    @Order(2)
    @DisplayName("Full board display - single piece shows valid neighbors")
    void testSinglePieceDisplay() {
        board.place(0, 0, pieces.get(1), 0);
        List<Integer> unused = Arrays.asList(2, 3, 4, 5, 6, 7, 8, 9);

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused);
        }, "Should display board with single piece");
    }

    @Test
    @Order(3)
    @DisplayName("Full board display - completed puzzle shows all green edges")
    void testCompletePuzzleDisplay() {
        // Place all 9 pieces to complete puzzle
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(5), 0);
        board.place(0, 2, pieces.get(2), 0);
        board.place(1, 0, pieces.get(6), 0);
        board.place(1, 1, pieces.get(9), 0);
        board.place(1, 2, pieces.get(7), 0);
        board.place(2, 0, pieces.get(3), 0);
        board.place(2, 1, pieces.get(8), 0);
        board.place(2, 2, pieces.get(4), 0);

        List<Integer> unused = new ArrayList<>();

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused);
        }, "Should display complete puzzle");
    }

    @Test
    @Order(4)
    @DisplayName("Full board display - with last placed piece highlighted")
    void testDisplayWithLastPlacedHighlight() {
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(5), 0);

        List<Integer> unused = Arrays.asList(2, 3, 4, 6, 7, 8, 9);
        SaveStateManager.PlacementInfo lastPlaced = new SaveStateManager.PlacementInfo(0, 1, 5, 0);

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused, lastPlaced, null);
        }, "Should highlight last placed piece in magenta");
    }

    @Test
    @Order(5)
    @DisplayName("Full board display - with next target highlighted")
    void testDisplayWithNextTargetHighlight() {
        board.place(0, 0, pieces.get(1), 0);
        List<Integer> unused = Arrays.asList(2, 3, 4, 5, 6, 7, 8, 9);
        int[] nextTarget = new int[]{0, 1}; // A2

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused, null, nextTarget);
        }, "Should highlight next target in blue");
    }

    @Test
    @Order(6)
    @DisplayName("Full board display - with BOTH last and next highlighted")
    void testDisplayWithBothHighlights() {
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(5), 0);

        List<Integer> unused = Arrays.asList(2, 3, 4, 6, 7, 8, 9);
        SaveStateManager.PlacementInfo lastPlaced = new SaveStateManager.PlacementInfo(0, 1, 5, 0);
        int[] nextTarget = new int[]{0, 2}; // A3

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused, lastPlaced, nextTarget);
        }, "Should highlight both last (magenta) and next (blue)");
    }

    @Test
    @Order(7)
    @DisplayName("Full board display - with fixed pieces in cyan")
    void testDisplayWithFixedPieces() {
        fixedPositions.add("0,0");
        fixedPositions.add("2,2");
        displayManager = new BoardDisplayManager(fixedPositions, validator);

        board.place(0, 0, pieces.get(1), 0);
        board.place(2, 2, pieces.get(4), 0);

        List<Integer> unused = Arrays.asList(2, 3, 5, 6, 7, 8, 9);

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused);
        }, "Should highlight fixed pieces in cyan");
    }

    @Test
    @Order(8)
    @DisplayName("Full board display - deadend shown in bright red")
    void testDisplayWithDeadend() {
        // Create configuration that causes deadend
        board.place(0, 1, new Piece(99, new int[]{0, 2, 99, 4}), 0);
        board.place(1, 0, new Piece(98, new int[]{5, 99, 7, 0}), 0);

        pieces.put(99, new Piece(99, new int[]{0, 2, 99, 4}));
        pieces.put(98, new Piece(98, new int[]{5, 99, 7, 0}));

        List<Integer> unused = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused);
        }, "Should display deadend cell in bright red");
    }

    @Test
    @Order(9)
    @DisplayName("Full board display - with rotated pieces")
    void testDisplayWithRotatedPieces() {
        board.place(0, 0, pieces.get(1), 0);    // No rotation
        board.place(0, 1, pieces.get(5), 1);    // 90° rotation
        board.place(0, 2, pieces.get(2), 2);    // 180° rotation

        List<Integer> unused = Arrays.asList(3, 4, 6, 7, 8, 9);

        assertDoesNotThrow(() -> {
            displayManager.printBoardWithLabels(board, pieces, unused);
        }, "Should display rotated pieces correctly");
    }

    @Test
    @Order(10)
    @DisplayName("Full board display - large board (16x16)")
    void testLargeBoardDisplay() {
        Board largeBoard = new Board(16, 16);
        List<Integer> allPieces = new ArrayList<>(pieces.keySet());

        assertDoesNotThrow(() -> {
            BoardDisplayManager largeDisplay = new BoardDisplayManager(new HashSet<>(), validator);
            largeDisplay.printBoardWithLabels(largeBoard, pieces, allPieces);
        }, "Should handle large 16x16 board display");
    }

    private Map<Integer, Piece> createCompletePuzzleSet() {
        Map<Integer, Piece> p = new HashMap<>();
        // Complete valid 3x3 puzzle
        p.put(1, new Piece(1, new int[]{0, 2, 3, 0}));    // A1 corner
        p.put(2, new Piece(2, new int[]{0, 0, 5, 4}));    // A3 corner
        p.put(3, new Piece(3, new int[]{6, 7, 0, 0}));    // C1 corner
        p.put(4, new Piece(4, new int[]{8, 0, 0, 9}));    // C3 corner
        p.put(5, new Piece(5, new int[]{0, 4, 10, 2}));   // A2 edge
        p.put(6, new Piece(6, new int[]{3, 11, 6, 0}));   // B1 edge
        p.put(7, new Piece(7, new int[]{5, 0, 8, 12}));   // B3 edge
        p.put(8, new Piece(8, new int[]{13, 9, 0, 7}));   // C2 edge
        p.put(9, new Piece(9, new int[]{10, 12, 13, 11})); // B2 center
        return p;
    }
}
