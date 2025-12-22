package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ParallelSearchManager.
 * Tests global state management, work-stealing parallelism, thread coordination, and CAS operations.
 */
@DisplayName("ParallelSearchManager Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ParallelSearchManagerTest {

    private ParallelSearchManager manager;
    private DomainManager domainManager;
    private SharedSearchState sharedState;
    private Board testBoard;
    private Map<Integer, Piece> testPieces;

    @BeforeEach
    void setUp() {
        // Create shared state instance for each test
        sharedState = new SharedSearchState();

        // Create test fixtures
        testBoard = new Board(3, 3);
        testPieces = createTestPieces();

        // Create simple FitChecker for testing
        DomainManager.FitChecker fitChecker = (board, r, c, candidateEdges) -> true;
        domainManager = new DomainManager(fitChecker);
        manager = new ParallelSearchManager(domainManager, sharedState);
    }

    @AfterEach
    void tearDown() {
        // Clean up shared state
        sharedState.reset();
    }

    // ==================== Constructor Tests ====================

    @Test
    @Order(1)
    @DisplayName("Should create manager with domain manager and shared state")
    void testConstructor() {
        SharedSearchState state = new SharedSearchState();
        ParallelSearchManager manager = new ParallelSearchManager(domainManager, state);
        assertNotNull(manager, "Manager should be created");
    }

    @Test
    @Order(2)
    @DisplayName("Should accept null domain manager without immediate error")
    void testConstructorWithNullDomainManager() {
        // Constructor doesn't validate, may fail later during use
        SharedSearchState state = new SharedSearchState();
        assertDoesNotThrow(() -> new ParallelSearchManager(null, state));
    }

    // ==================== Work-Stealing Pool Tests ====================

    @Test
    @Order(3)
    @DisplayName("Should enable work-stealing pool")
    void testEnableWorkStealing() {
        manager.enableWorkStealing(4);

        ForkJoinPool pool = sharedState.getWorkStealingPool();
        assertNotNull(pool, "Work-stealing pool should be created");
        assertFalse(pool.isShutdown(), "Pool should not be shut down");
    }

    @Test
    @Order(4)
    @DisplayName("Should not recreate pool if already enabled")
    void testEnableWorkStealingIdempotent() {
        manager.enableWorkStealing(4);
        ForkJoinPool firstPool = sharedState.getWorkStealingPool();

        manager.enableWorkStealing(4);
        ForkJoinPool secondPool = sharedState.getWorkStealingPool();

        assertSame(firstPool, secondPool, "Should reuse existing pool");
    }

    @Test
    @Order(5)
    @DisplayName("Should recreate pool after shutdown")
    void testEnableWorkStealingAfterShutdown() {
        manager.enableWorkStealing(4);
        ForkJoinPool firstPool = sharedState.getWorkStealingPool();

        sharedState.reset(); // Shuts down pool

        manager.enableWorkStealing(4);
        ForkJoinPool secondPool = sharedState.getWorkStealingPool();

        assertNotSame(firstPool, secondPool, "Should create new pool after shutdown");
        assertTrue(firstPool.isShutdown(), "First pool should be shut down");
    }

    // ==================== Global State Tests ====================

    @Test
    @Order(6)
    @DisplayName("Should reset all global state")
    void testResetGlobalState() {
        // Set some state
        sharedState.markSolutionFound();
        sharedState.updateGlobalMaxDepth(42);
        sharedState.updateGlobalBestScore(100);
        sharedState.setGlobalBestThreadId(3);
        sharedState.setGlobalBestBoard(testBoard);
        sharedState.setGlobalBestPieces(testPieces);
        manager.enableWorkStealing(4);

        // Reset
        sharedState.reset();

        // Verify all reset
        assertFalse(sharedState.isSolutionFound(), "Solution flag should be reset");
        assertEquals(0, sharedState.getGlobalMaxDepth().get(), "Max depth should be reset");
        assertEquals(0, sharedState.getGlobalBestScore().get(), "Best score should be reset");
        assertEquals(-1, sharedState.getGlobalBestThreadId().get(), "Thread ID should be reset");
        assertNull(sharedState.getGlobalBestBoard().get(), "Best board should be null");
        assertNull(sharedState.getGlobalBestPieces().get(), "Best pieces should be null");

        ForkJoinPool pool = sharedState.getWorkStealingPool();
        assertTrue(pool == null || pool.isShutdown(), "Pool should be shut down");
    }

    @Test
    @Order(7)
    @DisplayName("Should get and set solution found flag")
    void testSolutionFoundFlag() {
        assertFalse(sharedState.isSolutionFound(), "Initially false");

        sharedState.markSolutionFound();
        assertTrue(sharedState.isSolutionFound(), "Should be true after marking");

        AtomicBoolean flag = sharedState.getSolutionFound();
        assertTrue(flag.get(), "Atomic boolean should be true");
    }

    @Test
    @Order(8)
    @DisplayName("Should update global max depth with CAS")
    void testUpdateGlobalMaxDepth() {
        assertEquals(0, sharedState.getGlobalMaxDepth().get(), "Initially 0");

        // Update to higher value
        boolean updated1 = sharedState.updateGlobalMaxDepth(10);
        assertTrue(updated1, "Should update from 0 to 10");
        assertEquals(10, sharedState.getGlobalMaxDepth().get());

        // Update to even higher value
        boolean updated2 = sharedState.updateGlobalMaxDepth(20);
        assertTrue(updated2, "Should update from 10 to 20");
        assertEquals(20, sharedState.getGlobalMaxDepth().get());

        // Try to update to lower value
        boolean updated3 = sharedState.updateGlobalMaxDepth(15);
        assertFalse(updated3, "Should not update to lower value");
        assertEquals(20, sharedState.getGlobalMaxDepth().get(), "Should remain 20");
    }

    @Test
    @Order(9)
    @DisplayName("Should update global best score with CAS")
    void testUpdateGlobalBestScore() {
        assertEquals(0, sharedState.getGlobalBestScore().get(), "Initially 0");

        // Update to higher value
        boolean updated1 = sharedState.updateGlobalBestScore(50);
        assertTrue(updated1, "Should update from 0 to 50");
        assertEquals(50, sharedState.getGlobalBestScore().get());

        // Try to update to lower value
        boolean updated2 = sharedState.updateGlobalBestScore(30);
        assertFalse(updated2, "Should not update to lower value");
        assertEquals(50, sharedState.getGlobalBestScore().get(), "Should remain 50");

        // Update to equal value
        boolean updated3 = sharedState.updateGlobalBestScore(50);
        assertFalse(updated3, "Should not update to equal value");
    }

    @Test
    @Order(10)
    @DisplayName("Should set and get global best thread ID")
    void testGlobalBestThreadId() {
        assertEquals(-1, sharedState.getGlobalBestThreadId().get(), "Initially -1");

        sharedState.setGlobalBestThreadId(5);
        assertEquals(5, sharedState.getGlobalBestThreadId().get());

        AtomicInteger threadId = sharedState.getGlobalBestThreadId();
        assertEquals(5, threadId.get());
    }

    @Test
    @Order(11)
    @DisplayName("Should set and get global best board")
    void testGlobalBestBoard() {
        assertNull(sharedState.getGlobalBestBoard().get(), "Initially null");

        Board board = new Board(4, 4);
        sharedState.setGlobalBestBoard(board);

        assertSame(board, sharedState.getGlobalBestBoard().get());
    }

    @Test
    @Order(12)
    @DisplayName("Should set and get global best pieces")
    void testGlobalBestPieces() {
        assertNull(sharedState.getGlobalBestPieces().get(), "Initially null");

        Map<Integer, Piece> pieces = createTestPieces();
        sharedState.setGlobalBestPieces(pieces);

        assertSame(pieces, sharedState.getGlobalBestPieces().get());
    }

    @Test
    @Order(13)
    @DisplayName("Should get lock object")
    void testGetLockObject() {
        Object lock = sharedState.getLockObject();
        assertNotNull(lock, "Lock object should not be null");

        Object lock2 = sharedState.getLockObject();
        assertSame(lock, lock2, "Should return same lock object");
    }

    // ==================== Work-Stealing Solve Tests ====================

    @Test
    @Order(14)
    @DisplayName("Should throw exception if work-stealing not enabled")
    void testSolveWithWorkStealingNotEnabled() {
        BitSet pieceUsed = new BitSet(10);
        ParallelSearchManager.SequentialSolver solver = (b, p, pu, tp) -> false;

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> manager.solveWithWorkStealing(testBoard, testPieces, pieceUsed, 9, solver),
            "Should throw if pool not enabled"
        );

        assertTrue(exception.getMessage().contains("not enabled"),
                  "Exception message should mention not enabled");
    }

    @Test
    @Order(15)
    @DisplayName("Should solve with work-stealing when enabled")
    void testSolveWithWorkStealing() {
        manager.enableWorkStealing(2);

        BitSet pieceUsed = new BitSet(10);
        // Simple solver that returns false (no solution)
        ParallelSearchManager.SequentialSolver solver = (b, p, pu, tp) -> false;

        boolean result = manager.solveWithWorkStealing(testBoard, testPieces, pieceUsed, 9, solver);

        assertFalse(result, "Should return false when no solution found");
    }

    @Test
    @Order(16)
    @DisplayName("Should execute work-stealing without errors")
    void testSolveWithWorkStealingExecution() {
        manager.enableWorkStealing(2);

        BitSet pieceUsed = new BitSet(10);
        // Solver that always returns false (simpler test case)
        ParallelSearchManager.SequentialSolver solver = (b, p, pu, tp) -> {
            // Simulate some work - check if all pieces used
            return pu.cardinality() == tp;
        };

        // Should execute without throwing exceptions
        assertDoesNotThrow(() ->
            manager.solveWithWorkStealing(testBoard, testPieces, pieceUsed, 9, solver),
            "Work-stealing should execute without errors"
        );
    }

    // ==================== Thread Safety Tests ====================

    @Test
    @Order(17)
    @DisplayName("Should handle concurrent updates to global max depth")
    void testConcurrentMaxDepthUpdates() throws InterruptedException {
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            final int depth = (i + 1) * 10;
            threads[i] = new Thread(() -> {
                sharedState.updateGlobalMaxDepth(depth);
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Final depth should be the maximum
        int finalDepth = sharedState.getGlobalMaxDepth().get();
        assertEquals(100, finalDepth, "Should be maximum depth from all threads");
    }

    @Test
    @Order(18)
    @DisplayName("Should handle concurrent updates to global best score")
    void testConcurrentBestScoreUpdates() throws InterruptedException {
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            final int score = (i + 1) * 5;
            threads[i] = new Thread(() -> {
                sharedState.updateGlobalBestScore(score);
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Final score should be the maximum
        int finalScore = sharedState.getGlobalBestScore().get();
        assertEquals(50, finalScore, "Should be maximum score from all threads");
    }

    @Test
    @Order(19)
    @DisplayName("Should handle concurrent solution found flags")
    void testConcurrentSolutionFound() throws InterruptedException {
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                if (!sharedState.isSolutionFound()) {
                    sharedState.markSolutionFound();
                }
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Solution should be marked as found
        assertTrue(sharedState.isSolutionFound(),
                  "Solution should be marked found by one of the threads");
    }

    // ==================== Edge Cases ====================

    @Test
    @Order(20)
    @DisplayName("Should handle multiple resets")
    void testMultipleResets() {
        assertDoesNotThrow(() -> {
            sharedState.reset();
            sharedState.reset();
            sharedState.reset();
        }, "Multiple resets should not cause errors");

        assertFalse(sharedState.isSolutionFound());
    }

    @Test
    @Order(21)
    @DisplayName("Should handle work-stealing with single thread")
    void testWorkStealingWithSingleThread() {
        manager.enableWorkStealing(1);

        BitSet pieceUsed = new BitSet(10);
        ParallelSearchManager.SequentialSolver solver = (b, p, pu, tp) -> false;

        assertDoesNotThrow(() ->
            manager.solveWithWorkStealing(testBoard, testPieces, pieceUsed, 9, solver),
            "Should handle single thread work-stealing"
        );
    }

    // ==================== Helper Methods ====================

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));
        pieces.put(2, new Piece(2, new int[]{0, 2, 3, 1}));
        pieces.put(3, new Piece(3, new int[]{0, 3, 4, 2}));
        pieces.put(4, new Piece(4, new int[]{2, 4, 5, 0}));
        pieces.put(5, new Piece(5, new int[]{3, 5, 6, 4}));
        pieces.put(6, new Piece(6, new int[]{4, 6, 7, 5}));
        pieces.put(7, new Piece(7, new int[]{5, 7, 0, 0}));
        pieces.put(8, new Piece(8, new int[]{6, 8, 0, 7}));
        pieces.put(9, new Piece(9, new int[]{7, 9, 0, 8}));
        return pieces;
    }
}
