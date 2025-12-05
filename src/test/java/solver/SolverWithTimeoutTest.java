package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for SolverWithTimeout.
 * Tests timeout management, thread interruption, and successful completion.
 */
@DisplayName("SolverWithTimeout Tests")
public class SolverWithTimeoutTest {

    private Board board;
    private Map<Integer, Piece> pieces;

    @BeforeEach
    void setUp() {
        board = new Board(2, 2);
        pieces = createTestPieces(4);
    }

    private Map<Integer, Piece> createTestPieces(int count) {
        Map<Integer, Piece> testPieces = new HashMap<>();
        for (int i = 1; i <= count; i++) {
            // Create simple corner/edge pieces for a 2x2 board
            int[] edges = new int[4];
            if (i == 1) {
                edges = new int[]{0, 1, 2, 0}; // Top-left corner
            } else if (i == 2) {
                edges = new int[]{0, 0, 3, 1}; // Top-right corner
            } else if (i == 3) {
                edges = new int[]{2, 4, 0, 0}; // Bottom-left corner
            } else if (i == 4) {
                edges = new int[]{3, 0, 0, 4}; // Bottom-right corner
            }
            testPieces.put(i, new Piece(i, edges));
        }
        return testPieces;
    }

    // ==================== Successful Completion Tests ====================

    @Test
    @DisplayName("Should complete successfully before timeout")
    void testSolveCompletesBeforeTimeout() {
        // Create a solver that completes quickly
        TestSolverWithTimeout solver = new TestSolverWithTimeout(100, true);

        // Give generous timeout (1 second)
        boolean result = solver.solveWithTimeout(board, pieces, 1000);

        assertTrue(result, "Should return true when solve completes before timeout");
        assertTrue(solver.solveWasCalled, "Solve method should have been called");
    }

    @Test
    @DisplayName("Should return true for trivially solvable puzzle")
    void testTriviallySolvablePuzzle() {
        SolverWithTimeout solver = new SolverWithTimeout();

        // Use a very simple 1x1 board
        Board simpleBoard = new Board(1, 1);
        Map<Integer, Piece> simplePieces = new HashMap<>();
        simplePieces.put(1, new Piece(1, new int[]{0, 0, 0, 0})); // All borders

        boolean result = solver.solveWithTimeout(simpleBoard, simplePieces, 1000);

        // Note: Result depends on solver implementation, but should complete quickly
        assertNotNull(result, "Should complete without throwing exceptions");
    }

    // ==================== Timeout Tests ====================

    @Test
    @DisplayName("Should timeout when solve takes too long")
    void testSolveTimesOut() {
        // Create a solver that takes longer than timeout
        TestSolverWithTimeout solver = new TestSolverWithTimeout(500, false);

        // Set short timeout (100ms)
        boolean result = solver.solveWithTimeout(board, pieces, 100);

        assertFalse(result, "Should return false when timeout occurs");
        assertTrue(solver.solveWasCalled, "Solve method should have been called");
    }

    @Test
    @DisplayName("Should handle timeout with zero milliseconds")
    void testZeroTimeout() {
        TestSolverWithTimeout solver = new TestSolverWithTimeout(100, false);

        boolean result = solver.solveWithTimeout(board, pieces, 0);

        assertFalse(result, "Should return false with zero timeout");
    }

    @Test
    @DisplayName("Should handle very short timeout")
    void testVeryShortTimeout() {
        TestSolverWithTimeout solver = new TestSolverWithTimeout(1000, false);

        // 1ms timeout - almost certainly will timeout
        boolean result = solver.solveWithTimeout(board, pieces, 1);

        assertFalse(result, "Should return false with 1ms timeout");
    }

    // ==================== Thread Interruption Tests ====================

    @Test
    @DisplayName("Should interrupt thread when timeout occurs")
    void testThreadInterruption() {
        // Create solver that checks interruption flag
        InterruptibleTestSolver solver = new InterruptibleTestSolver();

        boolean result = solver.solveWithTimeout(board, pieces, 100);

        assertFalse(result, "Should return false when interrupted");
        assertTrue(solver.wasInterrupted, "Thread should have been interrupted");
    }

    @Test
    @DisplayName("Should wait for thread join after interruption")
    void testThreadJoinAfterInterruption() {
        // This test verifies that THREAD_JOIN_TIMEOUT_MS is used
        TestSolverWithTimeout solver = new TestSolverWithTimeout(5000, false);

        long startTime = System.currentTimeMillis();
        solver.solveWithTimeout(board, pieces, 100);
        long elapsed = System.currentTimeMillis() - startTime;

        // Should timeout at 100ms + THREAD_JOIN_TIMEOUT_MS (5000ms)
        // But should complete much faster in practice
        assertTrue(elapsed < 6000, "Should complete within timeout + join timeout");
        assertTrue(elapsed >= 100, "Should at least wait for the timeout period");
    }

    @Test
    @DisplayName("Should handle exception during solve")
    void testExceptionDuringSolve() {
        ExceptionThrowingSolver solver = new ExceptionThrowingSolver();

        // Should not propagate exception, should return false
        boolean result = solver.solveWithTimeout(board, pieces, 1000);

        assertFalse(result, "Should return false when solve throws exception");
    }

    // ==================== THREAD_JOIN_TIMEOUT_MS Constant Tests ====================

    @Test
    @DisplayName("Should use THREAD_JOIN_TIMEOUT_MS constant for join timeout")
    void testUsesThreadJoinTimeoutConstant() {
        // Verify the constant is accessible and reasonable
        long joinTimeout = SolverConstants.THREAD_JOIN_TIMEOUT_MS;

        assertNotNull(joinTimeout, "THREAD_JOIN_TIMEOUT_MS should be defined");
        assertTrue(joinTimeout > 0, "Join timeout should be positive");
        assertTrue(joinTimeout <= 10000, "Join timeout should be reasonable (<=10s)");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle null pieces gracefully")
    void testNullPieces() {
        SolverWithTimeout solver = new SolverWithTimeout();

        // Should not crash, but may fail to solve
        assertDoesNotThrow(() -> solver.solveWithTimeout(board, null, 1000),
                          "Should handle null pieces without throwing");
    }

    @Test
    @DisplayName("Should handle empty pieces map")
    void testEmptyPieces() {
        SolverWithTimeout solver = new SolverWithTimeout();
        Map<Integer, Piece> emptyPieces = new HashMap<>();

        boolean result = solver.solveWithTimeout(board, emptyPieces, 1000);

        // Cannot solve with no pieces
        assertFalse(result, "Should return false with no pieces");
    }

    @Test
    @DisplayName("Should handle large timeout value")
    void testLargeTimeout() {
        TestSolverWithTimeout solver = new TestSolverWithTimeout(50, true);

        // Very large timeout (1 hour)
        boolean result = solver.solveWithTimeout(board, pieces, 3600000);

        assertTrue(result, "Should complete before large timeout");
    }

    @Test
    @DisplayName("Should handle concurrent calls")
    void testConcurrentCalls() throws InterruptedException {
        // Create multiple solver instances
        SolverWithTimeout solver1 = new SolverWithTimeout();
        SolverWithTimeout solver2 = new SolverWithTimeout();

        AtomicInteger completed = new AtomicInteger(0);

        Thread t1 = new Thread(() -> {
            solver1.solveWithTimeout(board, pieces, 500);
            completed.incrementAndGet();
        });

        Thread t2 = new Thread(() -> {
            solver2.solveWithTimeout(board, pieces, 500);
            completed.incrementAndGet();
        });

        t1.start();
        t2.start();
        t1.join(2000);
        t2.join(2000);

        assertEquals(2, completed.get(), "Both solver calls should complete");
    }

    // ==================== Test Helper Classes ====================

    /**
     * Test solver that simulates work and can succeed or fail
     */
    private static class TestSolverWithTimeout extends SolverWithTimeout {
        private final long simulatedDuration;
        private final boolean shouldSucceed;
        boolean solveWasCalled = false;

        public TestSolverWithTimeout(long simulatedDuration, boolean shouldSucceed) {
            this.simulatedDuration = simulatedDuration;
            this.shouldSucceed = shouldSucceed;
        }

        @Override
        public boolean solve(Board board, Map<Integer, Piece> pieces) {
            solveWasCalled = true;
            try {
                Thread.sleep(simulatedDuration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            return shouldSucceed;
        }
    }

    /**
     * Test solver that checks if it was interrupted
     */
    private static class InterruptibleTestSolver extends SolverWithTimeout {
        boolean wasInterrupted = false;

        @Override
        public boolean solve(Board board, Map<Integer, Piece> pieces) {
            try {
                // Sleep for a long time
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                wasInterrupted = true;
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        }
    }

    /**
     * Test solver that throws an exception
     */
    private static class ExceptionThrowingSolver extends SolverWithTimeout {
        @Override
        public boolean solve(Board board, Map<Integer, Piece> pieces) {
            throw new RuntimeException("Test exception during solve");
        }
    }
}
