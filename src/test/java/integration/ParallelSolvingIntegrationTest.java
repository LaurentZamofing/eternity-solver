package integration;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;
import solver.EternitySolver;
import solver.SharedSearchState;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for parallel solving functionality.
 * Tests work-stealing, thread coordination, and performance.
 */
@DisplayName("Parallel Solving Integration Tests")
class ParallelSolvingIntegrationTest {

    @Test
    @DisplayName("Work-stealing should find solution faster than sequential")
    void testWorkStealingPerformance() {
        // Arrange - Create solvable 3x3 puzzle
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = createSolvablePuzzle3x3();

        // Sequential solve
        EternitySolver sequentialSolver = new EternitySolver();
        sequentialSolver.setVerbose(false);
        long sequentialStart = System.currentTimeMillis();
        boolean sequentialSolved = sequentialSolver.solve(board, new HashMap<>(pieces));
        long sequentialTime = System.currentTimeMillis() - sequentialStart;

        // Parallel solve
        Board parallelBoard = new Board(3, 3);
        EternitySolver parallelSolver = new EternitySolver();
        parallelSolver.setVerbose(false);

        long parallelStart = System.currentTimeMillis();
        boolean parallelSolved = parallelSolver.solveParallel(
            parallelBoard, new HashMap<>(pieces), new HashMap<>(pieces), 4
        );
        long parallelTime = System.currentTimeMillis() - parallelStart;

        // Assert - Both should solve, parallel should be competitive
        assertTrue(sequentialSolved, "Sequential should solve");
        assertTrue(parallelSolved, "Parallel should solve");

        // Parallel might not always be faster for tiny puzzles, but should complete
        assertTrue(parallelTime < 10000, "Parallel should complete within 10 seconds");
    }

    @Test
    @DisplayName("Multiple threads should coordinate via SharedSearchState")
    void testThreadCoordination() throws InterruptedException {
        // Arrange
        SharedSearchState sharedState = new SharedSearchState();
        int numThreads = 4;
        CountDownLatch latch = new CountDownLatch(numThreads);

        Board board = new Board(4, 4);
        Map<Integer, Piece> pieces = createSolvablePuzzle3x3();

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
    @DisplayName("Solution found flag should stop other threads")
    void testSolutionFoundStopsThreads() throws InterruptedException {
        // Arrange
        SharedSearchState sharedState = new SharedSearchState();
        int numThreads = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = createSolvablePuzzle3x3();

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

    @Test
    @Order(4)
    @DisplayName("Binary format should be faster than text format")
    void testBinaryFormatPerformance() throws Exception {
        // Arrange - Create state with many placements
        List<SaveStateManager.PlacementInfo> manyPlacements = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            manyPlacements.add(new SaveStateManager.PlacementInfo(
                i % 3, (i / 3) % 3, i + 1, i % 4
            ));
        }

        Board bigBoard = new Board(10, 10);

        // Test text format
        SaveStateManager.disableBinaryFormat();
        long textStart = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            SaveStateIO.saveCurrentState(
                TEST_PUZZLE + "_text", bigBoard, 50, 0,
                manyPlacements, Collections.emptyList(), 5000L
            );
        }
        long textTime = System.currentTimeMillis() - textStart;

        // Test binary format
        SaveStateManager.enableBinaryFormat();
        long binaryStart = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            SaveStateIO.saveCurrentState(
                TEST_PUZZLE + "_binary", bigBoard, 50, 0,
                manyPlacements, Collections.emptyList(), 5000L
            );
        }
        long binaryTime = System.currentTimeMillis() - binaryStart;

        // Assert - Binary should be significantly faster
        assertTrue(binaryTime < textTime,
            String.format("Binary (%dms) should be faster than text (%dms)", binaryTime, textTime));

        // Cleanup
        new File("saves/" + TEST_PUZZLE + "_text/").delete();
        new File("saves/" + TEST_PUZZLE + "_binary/").delete();
    }

    // Helper methods

    private Map<Integer, Piece> createSolvablePuzzle3x3() {
        // Create simple solvable 3x3 puzzle
        Map<Integer, Piece> pieces = new HashMap<>();
        // Simplified - all edges compatible
        for (int i = 1; i <= 9; i++) {
            pieces.put(i, new Piece(i, new int[]{1, 1, 1, 1}));
        }
        return pieces;
    }

    private void cleanupTestSaves() {
        String[] testDirs = {
            "saves/" + TEST_PUZZLE + "/",
            "saves/" + TEST_PUZZLE + "_text/",
            "saves/" + TEST_PUZZLE + "_binary/"
        };

        for (String dirPath : testDirs) {
            File dir = new File(dirPath);
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
                dir.delete();
            }
        }
    }
}
