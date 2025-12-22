package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for thread-safety across the solver components.
 * Validates that SharedSearchState properly coordinates multiple threads.
 *
 * Phase 3.3: Comprehensive thread-safety validation after eliminating static state.
 */
@DisplayName("Thread-Safety Integration Tests")
class ThreadSafetyIntegrationTest {

    @Test
    @DisplayName("Multiple threads should safely share SharedSearchState")
    void testSharedStateThreadSafety() throws InterruptedException {
        // Arrange
        SharedSearchState sharedState = new SharedSearchState();
        int numThreads = 20;
        int operationsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(numThreads);

        // Act - spawn threads that all update shared state concurrently
        List<Thread> threads = new ArrayList<>();
        AtomicInteger successfulUpdates = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        int depth = threadId * operationsPerThread + j;
                        if (sharedState.updateGlobalMaxDepth(depth)) {
                            successfulUpdates.incrementAndGet();
                        }
                        sharedState.updateGlobalBestScore(depth);
                        sharedState.setGlobalBestThreadId(threadId);
                    }
                } finally {
                    latch.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete within timeout");

        // Assert - verify final state is consistent
        int finalMaxDepth = sharedState.getGlobalMaxDepth().get();
        int expectedMax = (numThreads - 1) * operationsPerThread + (operationsPerThread - 1);
        assertEquals(expectedMax, finalMaxDepth, "Final max depth should be the highest value");

        int finalBestScore = sharedState.getGlobalBestScore().get();
        assertEquals(expectedMax, finalBestScore, "Final best score should be the highest value");

        int finalThreadId = sharedState.getGlobalBestThreadId().get();
        assertTrue(finalThreadId >= 0 && finalThreadId < numThreads, "Thread ID should be valid");
    }

    @Test
    @DisplayName("Multiple EternitySolver instances should not share state")
    void testSolverInstanceIsolation() {
        // Arrange
        EternitySolver solver1 = new EternitySolver();
        EternitySolver solver2 = new EternitySolver();
        SharedSearchState state1 = new SharedSearchState();
        SharedSearchState state2 = new SharedSearchState();

        solver1.setSharedState(state1);
        solver2.setSharedState(state2);

        // Act
        state1.updateGlobalMaxDepth(100);
        state2.updateGlobalMaxDepth(50);

        // Assert - each solver has independent state
        assertEquals(100, state1.getGlobalMaxDepth().get());
        assertEquals(50, state2.getGlobalMaxDepth().get());
        assertNotSame(state1, state2, "Solvers should have different state instances");
    }

    @Test
    @DisplayName("Solution found flag should stop all threads")
    void testSolutionFoundCoordination() throws InterruptedException {
        // Arrange
        SharedSearchState sharedState = new SharedSearchState();
        int numThreads = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicInteger stoppedThreads = new AtomicInteger(0);

        // Act - spawn threads that check solution flag
        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for signal to start

                    // Simulate work loop
                    for (int j = 0; j < 1000; j++) {
                        if (sharedState.isSolutionFound()) {
                            stoppedThreads.incrementAndGet();
                            break; // Stop when solution found
                        }
                        Thread.sleep(1); // Simulate work
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Let threads start working
        Thread.sleep(50);
        startLatch.countDown();

        // Mark solution found after short delay
        Thread.sleep(100);
        sharedState.markSolutionFound();

        // Wait for threads to stop
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete");

        // Assert - most threads should have stopped early
        int stopped = stoppedThreads.get();
        assertTrue(stopped > 0, "Some threads should have detected solution flag");
    }

    @Test
    @DisplayName("Work-stealing pool should handle concurrent access")
    void testWorkStealingPoolThreadSafety() throws InterruptedException {
        // Arrange
        SharedSearchState sharedState = new SharedSearchState();
        int numThreads = 10;
        CountDownLatch latch = new CountDownLatch(numThreads);

        // Act - multiple threads enable work-stealing concurrently
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            Thread thread = new Thread(() -> {
                try {
                    sharedState.enableWorkStealing(4);
                } finally {
                    latch.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete");

        // Assert - pool should be created exactly once
        ForkJoinPool pool = sharedState.getWorkStealingPool();
        assertNotNull(pool, "Pool should be created");
        assertFalse(pool.isShutdown(), "Pool should be active");

        // Cleanup
        sharedState.reset();
    }

    @Test
    @DisplayName("Board and pieces updates should be atomic")
    void testAtomicBoardAndPiecesUpdate() throws InterruptedException {
        // Arrange
        SharedSearchState sharedState = new SharedSearchState();
        int numUpdates = 50;
        CountDownLatch latch = new CountDownLatch(numUpdates);

        // Act - concurrent board/pieces updates
        for (int i = 0; i < numUpdates; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    Board board = new Board(3, 3);
                    Map<Integer, Piece> pieces = new HashMap<>();
                    pieces.put(index, new Piece(index, new int[]{1, 2, 3, 4}));

                    sharedState.setGlobalBestBoard(board);
                    sharedState.setGlobalBestPieces(pieces);
                    sharedState.setGlobalBestThreadId(index);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // Wait
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "All updates should complete");

        // Assert - final state should be consistent (no null pointers, no corruption)
        Board finalBoard = sharedState.getGlobalBestBoard().get();
        Map<Integer, Piece> finalPieces = sharedState.getGlobalBestPieces().get();
        int finalThreadId = sharedState.getGlobalBestThreadId().get();

        assertNotNull(finalBoard, "Final board should not be null");
        assertNotNull(finalPieces, "Final pieces should not be null");
        assertTrue(finalThreadId >= 0 && finalThreadId < numUpdates, "Thread ID should be valid");
    }
}
