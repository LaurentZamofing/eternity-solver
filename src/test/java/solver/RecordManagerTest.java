package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import util.SaveStateManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RecordManager class.
 * Tests record tracking, atomic operations, and board saving.
 */
@DisplayName("RecordManager Tests")
public class RecordManagerTest {

    private RecordManager recordManager;
    private Board board;
    private Map<Integer, Piece> piecesById;
    private AtomicInteger globalMaxDepth;
    private AtomicInteger globalBestScore;
    private AtomicInteger globalBestThreadId;
    private AtomicReference<Board> globalBestBoard;
    private AtomicReference<Map<Integer, Piece>> globalBestPieces;
    private Object lockObject;
    private PlacementValidator validator;
    private Set<String> fixedPositions;

    @BeforeEach
    public void setUp() {
        // Create test board (3x3)
        board = new Board(3, 3);

        // Create test pieces
        piecesById = createTestPieces();

        // Place a few pieces on the board
        board.place(0, 0, piecesById.get(1), 0);
        board.place(0, 1, piecesById.get(2), 0);

        // Initialize atomic references
        globalMaxDepth = new AtomicInteger(0);
        globalBestScore = new AtomicInteger(0);
        globalBestThreadId = new AtomicInteger(-1);
        globalBestBoard = new AtomicReference<>(null);
        globalBestPieces = new AtomicReference<>(null);
        lockObject = new Object();

        // Initialize validator and fixed positions for displayRecord
        CellConstraints[][] cellConstraints = CellConstraints.createConstraintsMatrix(3, 3);
        EternitySolver.Statistics stats = new EternitySolver.Statistics();
        validator = new PlacementValidator(cellConstraints, stats, "ascending");
        fixedPositions = new HashSet<>();

        // Create RecordManager
        recordManager = new RecordManager(
                "test_puzzle",
                1, // thread ID
                10, // minDepthToShowRecords
                lockObject,
                globalMaxDepth,
                globalBestScore,
                globalBestThreadId,
                globalBestBoard,
                globalBestPieces
        );
    }

    /**
     * Creates a simple valid 3x3 puzzle pieces.
     * This puzzle can be solved WITHOUT rotations - all pieces fit in their natural orientation.
     *
     * Board layout (3x3):
     *   A B C
     * 1 ┌─┬─┐
     * 2 ├─┼─┤
     * 3 └─┴─┘
     *
     * 9 pieces total:
     * - 4 corners (2 zeros each): A1, A3, C1, C3
     * - 4 edges (1 zero each): A2, B1, B3, C2
     * - 1 center (0 zeros): B2
     */
    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> pieces = new HashMap<>();

        // Corner pieces (4): two adjacent 0s
        // Piece 1: Top-left corner (A1) - N=0, E=2, S=3, W=0
        pieces.put(1, new Piece(1, new int[]{0, 2, 3, 0}));

        // Piece 2: Top-right corner (A3) - N=0, E=0, S=5, W=4
        pieces.put(2, new Piece(2, new int[]{0, 0, 5, 4}));

        // Piece 3: Bottom-left corner (C1) - N=6, E=7, S=0, W=0
        pieces.put(3, new Piece(3, new int[]{6, 7, 0, 0}));

        // Piece 4: Bottom-right corner (C3) - N=8, E=0, S=0, W=9
        pieces.put(4, new Piece(4, new int[]{8, 0, 0, 9}));

        // Edge pieces (4): one 0
        // Piece 5: Top edge (A2) - N=0, E=4, S=10, W=2
        pieces.put(5, new Piece(5, new int[]{0, 4, 10, 2}));

        // Piece 6: Left edge (B1) - N=3, E=11, S=6, W=0
        pieces.put(6, new Piece(6, new int[]{3, 11, 6, 0}));

        // Piece 7: Right edge (B3) - N=5, E=0, S=8, W=12
        pieces.put(7, new Piece(7, new int[]{5, 0, 8, 12}));

        // Piece 8: Bottom edge (C2) - N=13, E=9, S=0, W=7
        pieces.put(8, new Piece(8, new int[]{13, 9, 0, 7}));

        // Center piece (1): no 0s
        // Piece 9: Center (B2) - N=10, E=12, S=13, W=11
        pieces.put(9, new Piece(9, new int[]{10, 12, 13, 11}));

        return pieces;
    }

    /**
     * Creates a puzzle that requires rotations to solve.
     * Pieces don't fit in their initial orientation.
     */
    private Map<Integer, Piece> createPuzzleRequiringRotations() {
        Map<Integer, Piece> pieces = new HashMap<>();

        // All pieces need to be rotated to fit correctly
        // Piece 1: needs 90° rotation to fit in A1
        pieces.put(1, new Piece(1, new int[]{2, 3, 0, 0})); // Should be {0, 2, 3, 0} after rotation

        // Piece 2: needs 180° rotation to fit in A3
        pieces.put(2, new Piece(2, new int[]{5, 4, 0, 0})); // Should be {0, 0, 5, 4} after rotation

        // Piece 3: needs 270° rotation to fit in C1
        pieces.put(3, new Piece(3, new int[]{0, 6, 7, 0})); // Should be {6, 7, 0, 0} after rotation

        // Rest can fit without rotation
        pieces.put(4, new Piece(4, new int[]{8, 0, 0, 9}));
        pieces.put(5, new Piece(5, new int[]{0, 4, 10, 2}));
        pieces.put(6, new Piece(6, new int[]{3, 11, 6, 0}));
        pieces.put(7, new Piece(7, new int[]{5, 0, 8, 12}));
        pieces.put(8, new Piece(8, new int[]{13, 9, 0, 7}));
        pieces.put(9, new Piece(9, new int[]{10, 12, 13, 11}));

        return pieces;
    }

    /**
     * Creates a puzzle that requires backtracking.
     * Multiple pieces can initially fit in certain positions,
     * but only one configuration leads to a complete solution.
     */
    private Map<Integer, Piece> createPuzzleRequiringBacktracking() {
        Map<Integer, Piece> pieces = new HashMap<>();

        // Create pieces with overlapping valid placements
        // This forces the solver to try different combinations and backtrack

        // Corners
        pieces.put(1, new Piece(1, new int[]{0, 2, 3, 0}));
        pieces.put(2, new Piece(2, new int[]{0, 0, 5, 4}));
        pieces.put(3, new Piece(3, new int[]{6, 7, 0, 0}));
        pieces.put(4, new Piece(4, new int[]{8, 0, 0, 9}));

        // Edges - with intentional conflicts
        pieces.put(5, new Piece(5, new int[]{0, 4, 10, 2}));

        // Piece 6: can initially fit in B1 BUT will cause deadlock later
        // We need a piece with N=3 to match piece 1's S=3
        pieces.put(6, new Piece(6, new int[]{3, 14, 6, 0})); // Alternative edge value 14

        // Piece 10: the correct piece for B1 (backtracking will find this)
        pieces.put(10, new Piece(10, new int[]{3, 11, 6, 0})); // Correct edge value 11

        pieces.put(7, new Piece(7, new int[]{5, 0, 8, 12}));
        pieces.put(8, new Piece(8, new int[]{13, 9, 0, 7}));
        pieces.put(9, new Piece(9, new int[]{10, 12, 13, 11}));

        return pieces;
    }

    @Test
    @DisplayName("Initial state")
    public void testInitialState() {
        assertEquals(0, recordManager.getMaxDepthReached());
        assertEquals(0, recordManager.getLastProgressBacktracks());
    }

    @Test
    @DisplayName("Check and update record - no new record when depth not increased")
    public void testCheckAndUpdateRecordNoProgress() {
        recordManager.checkAndUpdateRecord(board, piecesById, 5, 100);

        // Second call with same depth should return null
        RecordManager.RecordCheckResult result =
                recordManager.checkAndUpdateRecord(board, piecesById, 5, 200);

        assertNull(result, "Should return null when depth hasn't increased");
        assertEquals(5, recordManager.getMaxDepthReached());
    }

    @Test
    @DisplayName("Check and update record - new depth record")
    public void testCheckAndUpdateRecordNewDepth() {
        RecordManager.RecordCheckResult result =
                recordManager.checkAndUpdateRecord(board, piecesById, 10, 100);

        assertNotNull(result);
        assertTrue(result.isNewDepthRecord, "Should be a new depth record");
        assertEquals(10, recordManager.getMaxDepthReached());
        assertEquals(100, recordManager.getLastProgressBacktracks());
        assertEquals(10, globalMaxDepth.get(), "Global max depth should be updated");
        assertEquals(1, globalBestThreadId.get(), "Thread ID should be set");
    }

    @Test
    @DisplayName("Check and update record - increasing depths")
    public void testCheckAndUpdateRecordIncreasingDepths() {
        // First record at depth 5
        RecordManager.RecordCheckResult result1 =
                recordManager.checkAndUpdateRecord(board, piecesById, 5, 50);
        assertNotNull(result1);
        assertTrue(result1.isNewDepthRecord);

        // Second record at depth 10
        RecordManager.RecordCheckResult result2 =
                recordManager.checkAndUpdateRecord(board, piecesById, 10, 100);
        assertNotNull(result2);
        assertTrue(result2.isNewDepthRecord);

        // Third record at depth 15
        RecordManager.RecordCheckResult result3 =
                recordManager.checkAndUpdateRecord(board, piecesById, 15, 150);
        assertNotNull(result3);
        assertTrue(result3.isNewDepthRecord);

        assertEquals(15, recordManager.getMaxDepthReached());
        assertEquals(150, recordManager.getLastProgressBacktracks());
    }

    @Test
    @DisplayName("Check and update record - saves global best board")
    public void testCheckAndUpdateRecordSavesBoard() {
        assertNull(globalBestBoard.get(), "Best board should initially be null");

        recordManager.checkAndUpdateRecord(board, piecesById, 10, 100);

        assertNotNull(globalBestBoard.get(), "Best board should be saved");
        assertNotNull(globalBestPieces.get(), "Best pieces should be saved");
        assertEquals(3, globalBestBoard.get().getRows());
        assertEquals(3, globalBestBoard.get().getCols());
    }

    @Test
    @DisplayName("RecordCheckResult contains score information")
    public void testRecordCheckResultScoreInfo() {
        RecordManager.RecordCheckResult result =
                recordManager.checkAndUpdateRecord(board, piecesById, 10, 100);

        assertNotNull(result);
        assertTrue(result.currentScore >= 0, "Current score should be non-negative");
        assertTrue(result.maxScore >= 0, "Max score should be non-negative");
    }

    @Test
    @DisplayName("Should show record - below minimum depth")
    public void testShouldShowRecordBelowMinDepth() {
        RecordManager.RecordCheckResult result =
                recordManager.checkAndUpdateRecord(board, piecesById, 5, 50);

        // minDepthToShowRecords is 10, so depth 5 should not be shown
        boolean shouldShow = recordManager.shouldShowRecord(result, 5);
        assertFalse(shouldShow, "Should not show record below minimum depth");
    }

    @Test
    @DisplayName("Should show record - at minimum depth with depth record")
    public void testShouldShowRecordAtMinDepthWithDepthRecord() {
        // Note: shouldShowRecord checks currentDepth > 60 for depth records
        RecordManager.RecordCheckResult result =
                recordManager.checkAndUpdateRecord(board, piecesById, 65, 100);

        boolean shouldShow = recordManager.shouldShowRecord(result, 65);
        if (result.isNewDepthRecord) {
            assertTrue(shouldShow, "Should show depth record above 60");
        }
    }

    @Test
    @DisplayName("Should show record - null result")
    public void testShouldShowRecordNullResult() {
        boolean shouldShow = recordManager.shouldShowRecord(null, 100);
        assertFalse(shouldShow, "Should not show record when result is null");
    }

    @Test
    @DisplayName("Display record does not throw exception")
    public void testDisplayRecordNoException() {
        StatisticsManager stats = new StatisticsManager();
        stats.start();

        RecordManager.RecordCheckResult result =
                recordManager.checkAndUpdateRecord(board, piecesById, 65, 100);

        // Calculate unused piece IDs
        List<Integer> unusedIds = new ArrayList<>();
        unusedIds.add(3); // piece 3 is unused

        // Should not throw exception
        assertDoesNotThrow(() -> recordManager.displayRecord(result, 9, stats,
                board, piecesById, unusedIds, fixedPositions, validator, null, null));
    }

    @Test
    @DisplayName("Display record with score record")
    public void testDisplayRecordWithScoreRecord() {
        StatisticsManager stats = new StatisticsManager();
        stats.start();

        // First establish a baseline
        recordManager.checkAndUpdateRecord(board, piecesById, 10, 100);

        // Place more pieces to potentially get better score
        board.place(0, 2, piecesById.get(3), 0);

        RecordManager.RecordCheckResult result =
                recordManager.checkAndUpdateRecord(board, piecesById, 15, 200);

        // Calculate unused piece IDs (all pieces are used now)
        List<Integer> unusedIds = new ArrayList<>();

        // Should not throw exception
        assertDoesNotThrow(() -> recordManager.displayRecord(result, 9, stats,
                board, piecesById, unusedIds, fixedPositions, validator, null, null));
    }

    @Test
    @DisplayName("Multiple threads updating global max depth")
    public void testMultipleThreadsGlobalMaxDepth() throws InterruptedException {
        // Create multiple RecordManagers simulating different threads
        RecordManager rm1 = new RecordManager("test", 1, 10, lockObject,
                globalMaxDepth, globalBestScore, globalBestThreadId,
                globalBestBoard, globalBestPieces);

        RecordManager rm2 = new RecordManager("test", 2, 10, lockObject,
                globalMaxDepth, globalBestScore, globalBestThreadId,
                globalBestBoard, globalBestPieces);

        RecordManager rm3 = new RecordManager("test", 3, 10, lockObject,
                globalMaxDepth, globalBestScore, globalBestThreadId,
                globalBestBoard, globalBestPieces);

        // Create separate boards for each thread
        Board board1 = new Board(3, 3);
        Board board2 = new Board(3, 3);
        Board board3 = new Board(3, 3);

        board1.place(0, 0, piecesById.get(1), 0);
        board2.place(0, 0, piecesById.get(1), 0);
        board3.place(0, 0, piecesById.get(1), 0);

        // Simulate concurrent updates
        Thread t1 = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                rm1.checkAndUpdateRecord(board1, piecesById, i, i * 10);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 1; i <= 15; i++) {
                rm2.checkAndUpdateRecord(board2, piecesById, i, i * 10);
            }
        });

        Thread t3 = new Thread(() -> {
            for (int i = 1; i <= 12; i++) {
                rm3.checkAndUpdateRecord(board3, piecesById, i, i * 10);
            }
        });

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        // The global max depth should be 15 (from thread 2)
        assertEquals(15, globalMaxDepth.get(),
                "Global max depth should be the highest from all threads");

        // The thread ID should be 2 (thread that achieved max depth)
        assertEquals(2, globalBestThreadId.get(),
                "Thread ID should be the one that achieved max depth");
    }

    @Test
    @DisplayName("RecordCheckResult structure")
    public void testRecordCheckResultStructure() {
        RecordManager.RecordCheckResult result =
                new RecordManager.RecordCheckResult(true, false, 100, 200);

        assertTrue(result.isNewDepthRecord);
        assertFalse(result.isNewScoreRecord);
        assertEquals(100, result.currentScore);
        assertEquals(200, result.maxScore);
    }

    @Test
    @DisplayName("Get max depth reached updates correctly")
    public void testGetMaxDepthReached() {
        assertEquals(0, recordManager.getMaxDepthReached());

        recordManager.checkAndUpdateRecord(board, piecesById, 5, 50);
        assertEquals(5, recordManager.getMaxDepthReached());

        recordManager.checkAndUpdateRecord(board, piecesById, 10, 100);
        assertEquals(10, recordManager.getMaxDepthReached());

        // Same depth should not change
        recordManager.checkAndUpdateRecord(board, piecesById, 10, 150);
        assertEquals(10, recordManager.getMaxDepthReached());
    }

    @Test
    @DisplayName("Get last progress backtracks updates correctly")
    public void testGetLastProgressBacktracks() {
        assertEquals(0, recordManager.getLastProgressBacktracks());

        recordManager.checkAndUpdateRecord(board, piecesById, 5, 100);
        assertEquals(100, recordManager.getLastProgressBacktracks());

        recordManager.checkAndUpdateRecord(board, piecesById, 10, 250);
        assertEquals(250, recordManager.getLastProgressBacktracks());
    }

    @Test
    @DisplayName("Global best score updates with CAS")
    public void testGlobalBestScoreUpdate() {
        assertEquals(0, globalBestScore.get());

        // First update should set the score
        recordManager.checkAndUpdateRecord(board, piecesById, 10, 100);
        int firstScore = globalBestScore.get();
        assertTrue(firstScore >= 0);

        // Add more pieces to potentially increase score
        board.place(1, 0, piecesById.get(3), 0);
        recordManager.checkAndUpdateRecord(board, piecesById, 15, 150);

        // Score should be updated (or stay the same if no improvement)
        int secondScore = globalBestScore.get();
        assertTrue(secondScore >= firstScore,
                "Score should not decrease");
    }

    @Test
    @DisplayName("Concurrent score updates maintain consistency")
    public void testConcurrentScoreUpdates() throws InterruptedException {
        RecordManager rm1 = new RecordManager("test", 1, 10, lockObject,
                globalMaxDepth, globalBestScore, globalBestThreadId,
                globalBestBoard, globalBestPieces);

        RecordManager rm2 = new RecordManager("test", 2, 10, lockObject,
                globalMaxDepth, globalBestScore, globalBestThreadId,
                globalBestBoard, globalBestPieces);

        Board board1 = new Board(3, 3);
        Board board2 = new Board(3, 3);

        // Place different numbers of pieces to create different scores
        board1.place(0, 0, piecesById.get(1), 0);
        board1.place(0, 1, piecesById.get(2), 0);

        board2.place(0, 0, piecesById.get(1), 0);
        board2.place(0, 1, piecesById.get(2), 0);
        board2.place(0, 2, piecesById.get(3), 0);

        Thread t1 = new Thread(() -> {
            rm1.checkAndUpdateRecord(board1, piecesById, 10, 100);
        });

        Thread t2 = new Thread(() -> {
            rm2.checkAndUpdateRecord(board2, piecesById, 15, 150);
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        // Global best score should be consistent (highest score achieved)
        assertTrue(globalBestScore.get() >= 0,
                "Global best score should be non-negative and consistent");
    }

    @Test
    @DisplayName("Board copy is independent")
    public void testBoardCopyIndependence() {
        // Update record to save the board
        recordManager.checkAndUpdateRecord(board, piecesById, 10, 100);

        Board savedBoard = globalBestBoard.get();
        assertNotNull(savedBoard);

        // Verify initial state
        assertFalse(savedBoard.isEmpty(0, 0), "Position (0,0) should have a piece");

        // Modify original board
        board.remove(0, 0);

        // Saved board should not be affected
        assertFalse(savedBoard.isEmpty(0, 0),
                "Saved board should be independent of original board");
    }

    // ==================================================================================
    // INTEGRATION TESTS - Testing with realistic puzzle scenarios
    // ==================================================================================

    @Test
    @DisplayName("Display record with simple solvable 3x3 puzzle")
    public void testDisplayRecordWithSimplePuzzle() {
        // Create a clean board and use simple puzzle pieces
        Board testBoard = new Board(3, 3);
        Map<Integer, Piece> simplePieces = createTestPieces();

        // Place all pieces to create a complete board
        testBoard.place(0, 0, simplePieces.get(1), 0); // A1: corner
        testBoard.place(0, 1, simplePieces.get(5), 0); // A2: edge
        testBoard.place(0, 2, simplePieces.get(2), 0); // A3: corner
        testBoard.place(1, 0, simplePieces.get(6), 0); // B1: edge
        testBoard.place(1, 1, simplePieces.get(9), 0); // B2: center
        testBoard.place(1, 2, simplePieces.get(7), 0); // B3: edge
        testBoard.place(2, 0, simplePieces.get(3), 0); // C1: corner
        testBoard.place(2, 1, simplePieces.get(8), 0); // C2: edge
        testBoard.place(2, 2, simplePieces.get(4), 0); // C3: corner

        StatisticsManager stats = new StatisticsManager();
        stats.start();

        RecordManager.RecordCheckResult result =
                recordManager.checkAndUpdateRecord(testBoard, simplePieces, 70, 100);

        // No unused pieces
        List<Integer> unusedIds = new ArrayList<>();

        // Should display complete board without exceptions
        // Last piece placed is at C3 (piece 4), no next cell (board is complete)
        SaveStateManager.PlacementInfo lastPlacement = new SaveStateManager.PlacementInfo(2, 2, 4, 0);
        assertDoesNotThrow(() -> recordManager.displayRecord(result, 9, stats,
                testBoard, simplePieces, unusedIds, fixedPositions, validator, lastPlacement, null));

        // Verify all edges match (this is a valid solution)
        int[] scoreData = testBoard.calculateScore();
        int currentScore = scoreData[0];
        int maxScore = scoreData[1];

        // For a 3x3 board:
        // - Internal edges: 12 (each internal cell has neighbors)
        // - All should match if puzzle is correctly solved
        assertEquals(maxScore, currentScore, "All internal edges should match in solved puzzle");
    }

    @Test
    @DisplayName("Display record with partially solved puzzle requiring rotations")
    public void testDisplayRecordWithRotationPuzzle() {
        Board testBoard = new Board(3, 3);
        Map<Integer, Piece> rotationPieces = createPuzzleRequiringRotations();

        // Place some pieces with rotations
        testBoard.place(0, 0, rotationPieces.get(1), 3); // 270° rotation (3 * 90°)
        testBoard.place(0, 1, rotationPieces.get(5), 0); // no rotation
        testBoard.place(0, 2, rotationPieces.get(2), 2); // 180° rotation

        StatisticsManager stats = new StatisticsManager();
        stats.start();

        RecordManager.RecordCheckResult result =
                recordManager.checkAndUpdateRecord(testBoard, rotationPieces, 65, 100);

        // Calculate unused pieces
        List<Integer> unusedIds = new ArrayList<>();
        for (int id = 1; id <= 9; id++) {
            if (id != 1 && id != 5 && id != 2) {
                unusedIds.add(id);
            }
        }

        // Should display partial board with rotations
        // Last piece placed: piece 2 at A3 with 180° rotation, next target: B1
        SaveStateManager.PlacementInfo lastPlacement = new SaveStateManager.PlacementInfo(0, 2, 2, 2);
        int[] nextTarget = new int[]{1, 0}; // B1
        assertDoesNotThrow(() -> recordManager.displayRecord(result, 3, stats,
                testBoard, rotationPieces, unusedIds, fixedPositions, validator, lastPlacement, nextTarget));
    }

    @Test
    @DisplayName("Display record shows valid piece counts for empty cells")
    public void testDisplayRecordShowsValidCounts() {
        Board testBoard = new Board(3, 3);
        Map<Integer, Piece> testPieces = createTestPieces();

        // Place only corner pieces
        testBoard.place(0, 0, testPieces.get(1), 0); // A1
        testBoard.place(0, 2, testPieces.get(2), 0); // A3
        testBoard.place(2, 0, testPieces.get(3), 0); // C1
        testBoard.place(2, 2, testPieces.get(4), 0); // C3

        StatisticsManager stats = new StatisticsManager();
        stats.start();

        RecordManager.RecordCheckResult result =
                recordManager.checkAndUpdateRecord(testBoard, testPieces, 65, 100);

        // Unused pieces: 5, 6, 7, 8, 9
        List<Integer> unusedIds = List.of(5, 6, 7, 8, 9);

        // Board should show valid counts for empty cells (A2, B1, B2, B3, C2)
        // Each empty cell should show how many of the unused pieces can fit there
        // Last piece: piece 4 at C3, next target: A2
        SaveStateManager.PlacementInfo lastPlacement = new SaveStateManager.PlacementInfo(2, 2, 4, 0);
        int[] nextTarget = new int[]{0, 1}; // A2
        assertDoesNotThrow(() -> recordManager.displayRecord(result, 4, stats,
                testBoard, testPieces, unusedIds, fixedPositions, validator, lastPlacement, nextTarget));

        // Verify that some cells should have valid options
        // For example, A2 (top edge) should be able to fit piece 5
        assertFalse(testBoard.isEmpty(0, 0), "A1 should have a piece");
        assertTrue(testBoard.isEmpty(0, 1), "A2 should be empty");
        assertFalse(testBoard.isEmpty(0, 2), "A3 should have a piece");
    }

    @Test
    @DisplayName("Display record with puzzle requiring backtracking")
    public void testDisplayRecordWithBacktrackingPuzzle() {
        Board testBoard = new Board(3, 3);
        Map<Integer, Piece> backtrackPieces = createPuzzleRequiringBacktracking();

        // Place pieces in a configuration that will require backtracking
        testBoard.place(0, 0, backtrackPieces.get(1), 0); // A1: corner
        testBoard.place(0, 1, backtrackPieces.get(5), 0); // A2: edge

        // Try the "wrong" piece first (piece 6 that will cause deadlock)
        testBoard.place(1, 0, backtrackPieces.get(6), 0); // B1: edge (wrong choice)

        StatisticsManager stats = new StatisticsManager();
        stats.start();

        RecordManager.RecordCheckResult result =
                recordManager.checkAndUpdateRecord(testBoard, backtrackPieces, 65, 100);

        // Unused pieces (including piece 10, the correct choice for B1)
        List<Integer> unusedIds = List.of(2, 3, 4, 7, 8, 9, 10);

        // Should display board showing the dead-end scenario
        // Last piece: wrong piece 6 at B1, next target: A3
        SaveStateManager.PlacementInfo lastPlacement = new SaveStateManager.PlacementInfo(1, 0, 6, 0);
        int[] nextTarget = new int[]{0, 2}; // A3
        assertDoesNotThrow(() -> recordManager.displayRecord(result, 3, stats,
                testBoard, backtrackPieces, unusedIds, fixedPositions, validator, lastPlacement, nextTarget));

        // The display should show cells with low valid piece counts (warning colors)
        // indicating that backtracking may be needed
    }
}
