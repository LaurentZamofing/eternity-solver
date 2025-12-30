package solver;

import model.Board;
import model.Piece;
import util.SaveStateManager;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * END-TO-END INTEGRATION TESTS for complete record display flow.
 * Tests the full pipeline from record detection to visual display.
 */
@DisplayName("Record Display End-to-End Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RecordDisplayEnd2EndTest {

    private RecordManager recordManager;
    private Board board;
    private Map<Integer, Piece> pieces;
    private PlacementValidator validator;
    private Set<String> fixedPositions;
    private AtomicInteger globalMaxDepth;
    private AtomicInteger globalBestScore;
    private AtomicInteger globalBestThreadId;
    private AtomicReference<Board> globalBestBoard;
    private AtomicReference<Map<Integer, Piece>> globalBestPieces;

    @BeforeEach
    void setUp() {
        board = new Board(3, 3);
        pieces = createCompletePuzzle();
        CellConstraints[][] constraints = CellConstraints.createConstraintsMatrix(3, 3);
        EternitySolver.Statistics stats = new EternitySolver.Statistics();
        validator = new PlacementValidator(constraints, stats, "ascending");
        fixedPositions = new HashSet<>();

        globalMaxDepth = new AtomicInteger(0);
        globalBestScore = new AtomicInteger(0);
        globalBestThreadId = new AtomicInteger(-1);
        globalBestBoard = new AtomicReference<>(null);
        globalBestPieces = new AtomicReference<>(null);

        recordManager = new RecordManager(
            "test_puzzle",
            1,
            1, // minDepthToShowRecords = 1 (show from depth 1)
            new Object(),
            globalMaxDepth,
            globalBestScore,
            globalBestThreadId,
            globalBestBoard,
            globalBestPieces
        );
    }

    @Test
    @Order(1)
    @DisplayName("E2E - Record at depth 1 displays correctly")
    void testRecordAtDepth1() {
        board.place(0, 0, pieces.get(1), 0);
        EternitySolver.Statistics stats = new EternitySolver.Statistics();
        stats.start();

        RecordManager.RecordCheckResult result =
            recordManager.checkAndUpdateRecord(board, pieces, 1, 100);

        List<Integer> unused = Arrays.asList(2, 3, 4, 5, 6, 7, 8, 9);
        SaveStateManager.PlacementInfo last = new SaveStateManager.PlacementInfo(0, 0, 1, 0);
        int[] next = new int[]{0, 1};

        assertDoesNotThrow(() -> {
            recordManager.displayRecord(result, 1, stats, board, pieces, unused,
                fixedPositions, validator, last, next);
        }, "Should display record at depth 1");
    }

    @Test
    @Order(2)
    @DisplayName("E2E - Progressive records from depth 1 to 9")
    void testProgressiveRecords() {
        EternitySolver.Statistics stats = new EternitySolver.Statistics();
        stats.start();

        List<int[][]> positions = Arrays.asList(
            new int[][]{{0,0}},
            new int[][]{{0,0}, {0,1}},
            new int[][]{{0,0}, {0,1}, {0,2}},
            new int[][]{{0,0}, {0,1}, {0,2}, {1,0}},
            new int[][]{{0,0}, {0,1}, {0,2}, {1,0}, {1,1}},
            new int[][]{{0,0}, {0,1}, {0,2}, {1,0}, {1,1}, {1,2}},
            new int[][]{{0,0}, {0,1}, {0,2}, {1,0}, {1,1}, {1,2}, {2,0}},
            new int[][]{{0,0}, {0,1}, {0,2}, {1,0}, {1,1}, {1,2}, {2,0}, {2,1}},
            new int[][]{{0,0}, {0,1}, {0,2}, {1,0}, {1,1}, {1,2}, {2,0}, {2,1}, {2,2}}
        );

        for (int depth = 1; depth <= 9; depth++) {
            // Reset board
            board = new Board(3, 3);

            // Place pieces up to current depth
            int[][] piecePositions = positions.get(depth - 1);
            for (int i = 0; i < piecePositions.length; i++) {
                int row = piecePositions[i][0];
                int col = piecePositions[i][1];
                board.place(row, col, pieces.get(i + 1), 0);
            }

            RecordManager.RecordCheckResult result =
                recordManager.checkAndUpdateRecord(board, pieces, depth, 100 * depth);

            // Build unused list
            List<Integer> unused = new ArrayList<>();
            for (int id = depth + 1; id <= 9; id++) {
                unused.add(id);
            }

            // Last placement
            int lastIdx = depth - 1;
            SaveStateManager.PlacementInfo last = new SaveStateManager.PlacementInfo(
                piecePositions[lastIdx][0],
                piecePositions[lastIdx][1],
                depth,
                0
            );

            // Next cell (if not complete)
            int[] next = (depth < 9) ? findNextEmptyCell(board) : null;

            int finalDepth = depth;
            assertDoesNotThrow(() -> {
                recordManager.displayRecord(result, finalDepth, stats, board, pieces, unused,
                    fixedPositions, validator, last, next);
            }, "Should display record at depth " + depth);
        }
    }

    @Test
    @Order(3)
    @DisplayName("E2E - Record with all pieces having different rotations")
    void testRecordWithVariedRotations() {
        board.place(0, 0, pieces.get(1), 0);   // 0°
        board.place(0, 1, pieces.get(5), 1);   // 90°
        board.place(0, 2, pieces.get(2), 2);   // 180°
        board.place(1, 0, pieces.get(6), 3);   // 270°

        EternitySolver.Statistics stats = new EternitySolver.Statistics();
        stats.start();

        RecordManager.RecordCheckResult result =
            recordManager.checkAndUpdateRecord(board, pieces, 4, 400);

        List<Integer> unused = Arrays.asList(3, 4, 7, 8, 9);
        SaveStateManager.PlacementInfo last = new SaveStateManager.PlacementInfo(1, 0, 6, 3);
        int[] next = new int[]{1, 1};

        assertDoesNotThrow(() -> {
            recordManager.displayRecord(result, 4, stats, board, pieces, unused,
                fixedPositions, validator, last, next);
        }, "Should display record with varied rotations");
    }

    @Test
    @Order(4)
    @DisplayName("E2E - Record displays rotation counts correctly")
    void testRecordDisplaysRotationCounts() {
        // Place 4 corners only
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 2, pieces.get(2), 0);
        board.place(2, 0, pieces.get(3), 0);
        board.place(2, 2, pieces.get(4), 0);

        EternitySolver.Statistics stats = new EternitySolver.Statistics();
        stats.start();

        RecordManager.RecordCheckResult result =
            recordManager.checkAndUpdateRecord(board, pieces, 4, 400);

        List<Integer> unused = Arrays.asList(5, 6, 7, 8, 9);
        SaveStateManager.PlacementInfo last = new SaveStateManager.PlacementInfo(2, 2, 4, 0);
        int[] next = new int[]{0, 1}; // A2

        // When displayed, empty cells should show (pieces/rotations) format
        assertDoesNotThrow(() -> {
            recordManager.displayRecord(result, 4, stats, board, pieces, unused,
                fixedPositions, validator, last, next);
        }, "Should display with rotation counts in format (P/R)");
    }

    @Test
    @Order(5)
    @DisplayName("E2E - Complete puzzle solution display")
    void testCompleteSolutionDisplay() {
        // Place all 9 pieces
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(5), 0);
        board.place(0, 2, pieces.get(2), 0);
        board.place(1, 0, pieces.get(6), 0);
        board.place(1, 1, pieces.get(9), 0);
        board.place(1, 2, pieces.get(7), 0);
        board.place(2, 0, pieces.get(3), 0);
        board.place(2, 1, pieces.get(8), 0);
        board.place(2, 2, pieces.get(4), 0);

        EternitySolver.Statistics stats = new EternitySolver.Statistics();
        stats.start();

        RecordManager.RecordCheckResult result =
            recordManager.checkAndUpdateRecord(board, pieces, 9, 1000);

        List<Integer> unused = new ArrayList<>();
        SaveStateManager.PlacementInfo last = new SaveStateManager.PlacementInfo(2, 2, 4, 0);

        assertDoesNotThrow(() -> {
            recordManager.displayRecord(result, 9, stats, board, pieces, unused,
                fixedPositions, validator, last, null);
        }, "Should display complete solution");

        // Verify all edges match (solution is valid)
        int[] scoreData = board.calculateScore();
        assertEquals(scoreData[1], scoreData[0], "Complete solution should have all edges matching");
    }

    private int[] findNextEmptyCell(Board board) {
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.isEmpty(r, c)) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    private Map<Integer, Piece> createCompletePuzzle() {
        Map<Integer, Piece> p = new HashMap<>();
        p.put(1, new Piece(1, new int[]{0, 2, 3, 0}));
        p.put(2, new Piece(2, new int[]{0, 0, 5, 4}));
        p.put(3, new Piece(3, new int[]{6, 7, 0, 0}));
        p.put(4, new Piece(4, new int[]{8, 0, 0, 9}));
        p.put(5, new Piece(5, new int[]{0, 4, 10, 2}));
        p.put(6, new Piece(6, new int[]{3, 11, 6, 0}));
        p.put(7, new Piece(7, new int[]{5, 0, 8, 12}));
        p.put(8, new Piece(8, new int[]{13, 9, 0, 7}));
        p.put(9, new Piece(9, new int[]{10, 12, 13, 11}));
        return p;
    }
}
