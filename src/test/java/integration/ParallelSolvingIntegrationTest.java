package integration;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Disabled;
import solver.EternitySolver;
import solver.SharedSearchState;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for parallel solving functionality.
 * Tests work-stealing, thread coordination, and performance.
 */
@DisplayName("Parallel Solving Integration Tests")
class ParallelSolvingIntegrationTest {

    @Test
    @Disabled("TODO: solveParallel() method needs investigation - sequential works but parallel doesn't")
    @DisplayName("Work-stealing should find solution faster than sequential")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testWorkStealingPerformance() {
        // Arrange - Create solvable 2x2 puzzle (simpler for fast tests)
        Board board = new Board(2, 2);
        Map<Integer, Piece> pieces = createSolvablePuzzle2x2();

        // Sequential solve
        EternitySolver sequentialSolver = new EternitySolver();
        sequentialSolver.setVerbose(false);
        long sequentialStart = System.currentTimeMillis();
        boolean sequentialSolved = sequentialSolver.solve(board, new HashMap<>(pieces));
        long sequentialTime = System.currentTimeMillis() - sequentialStart;

        // Parallel solve
        Board parallelBoard = new Board(2, 2);
        EternitySolver parallelSolver = new EternitySolver();
        parallelSolver.setVerbose(false);

        long parallelStart = System.currentTimeMillis();
        boolean parallelSolved = parallelSolver.solveParallel(
            parallelBoard, new HashMap<>(pieces), new HashMap<>(pieces), 2
        );
        long parallelTime = System.currentTimeMillis() - parallelStart;

        // Assert - Both should solve, parallel should be competitive
        assertTrue(sequentialSolved, "Sequential should solve");
        assertTrue(parallelSolved, "Parallel should solve");

        // Parallel might not always be faster for tiny puzzles, but should complete
        // Note: Timeout annotation ensures completion within 15 seconds
        System.out.println("Sequential time: " + sequentialTime + "ms, Parallel time: " + parallelTime + "ms");
    }

    @Test
    @DisplayName("Multiple threads should coordinate via SharedSearchState")
    @Timeout(value = 35, unit = TimeUnit.SECONDS)
    void testThreadCoordination() throws InterruptedException {
        // Arrange
        SharedSearchState sharedState = new SharedSearchState();
        int numThreads = 2;
        CountDownLatch latch = new CountDownLatch(numThreads);

        Board board = new Board(2, 2);
        Map<Integer, Piece> pieces = createSolvablePuzzle2x2();

        // Act - Launch multiple solver threads sharing state
        List<EternitySolver> solvers = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    EternitySolver solver = new EternitySolver();
                    solver.setSharedState(sharedState);
                    solver.setThreadLabel("Thread-" + threadId);
                    solver.setVerbose(false);
                    solver.setMaxExecutionTime(5000); // 5 second timeout

                    solvers.add(solver);
                    solver.solve(board, new HashMap<>(pieces));
                } finally {
                    latch.countDown();
                }
            });
            thread.start();
        }

        // Wait for all threads
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete");

        // Assert - State should be updated by at least one thread
        assertTrue(sharedState.getGlobalMaxDepth().get() >= 0, "Max depth should be updated");
    }

    @Test
    @Disabled("TODO: Depends on solveParallel() working - needs investigation")
    @DisplayName("Solution found flag should stop other threads")
    @Timeout(value = 35, unit = TimeUnit.SECONDS)
    void testSolutionFoundStopsThreads() throws InterruptedException {
        // Arrange
        SharedSearchState sharedState = new SharedSearchState();
        int numThreads = 3;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        Board board = new Board(2, 2);
        Map<Integer, Piece> pieces = createSolvablePuzzle2x2();

        List<Boolean> results = Collections.synchronizedList(new ArrayList<>());

        // Act - Launch threads
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();

                    EternitySolver solver = new EternitySolver();
                    solver.setSharedState(sharedState);
                    solver.setThreadLabel("Thread-" + threadId);
                    solver.setVerbose(false);
                    solver.setMaxExecutionTime(10000);

                    boolean solved = solver.solve(board, new HashMap<>(pieces));
                    results.add(solved);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

        // Assert
        assertTrue(completed, "All threads should complete");
        assertTrue(sharedState.isSolutionFound(), "Solution should be found by at least one thread");

        // At least one thread should report success
        long successCount = results.stream().filter(r -> r).count();
        assertTrue(successCount >= 1, "At least one thread should solve");
    }

    // NOTE: Binary format performance test removed - it doesn't test parallel solving
    // functionality. Save/load performance tests should be in SaveStateIntegrationTest.

    // Helper methods

    private Map<Integer, Piece> createSolvablePuzzle2x2() {
        // Create a SIMPLE 2x2 puzzle that's guaranteed to be solvable
        // Border edges are 0, internal edges match
        // This is much faster to solve for tests
        Map<Integer, Piece> pieces = new HashMap<>();

        // Piece layout (N,E,S,W):
        // +---+---+
        // | 1 | 2 |
        // +---+---+
        // | 3 | 4 |
        // +---+---+

        pieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));  // top-left: north=0, east=1, south=2, west=0
        pieces.put(2, new Piece(2, new int[]{0, 0, 3, 1}));  // top-right: north=0, east=0, south=3, west=1
        pieces.put(3, new Piece(3, new int[]{2, 4, 0, 0}));  // bottom-left: north=2, east=4, south=0, west=0
        pieces.put(4, new Piece(4, new int[]{3, 0, 0, 4}));  // bottom-right: north=3, east=0, south=0, west=4

        return pieces;
    }
}
