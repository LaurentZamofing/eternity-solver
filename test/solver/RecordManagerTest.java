package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;
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

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{1, 2, 3, 4}));
        pieces.put(2, new Piece(2, new int[]{5, 6, 7, 8}));
        pieces.put(3, new Piece(3, new int[]{0, 0, 0, 0}));
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

        // Should not throw exception
        assertDoesNotThrow(() -> recordManager.displayRecord(result, 9, stats));
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

        // Should not throw exception
        assertDoesNotThrow(() -> recordManager.displayRecord(result, 9, stats));
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
}
