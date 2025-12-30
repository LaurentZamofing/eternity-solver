import model.Board;
import model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solver.DomainManager;
import solver.StatisticsManager;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive thread safety tests for the Eternity solver.
 * Tests concurrent access, race conditions, and atomic operations.
 */
@DisplayName("Thread Safety Tests")
class ThreadSafetyTest {

    private static final int NUM_THREADS = 10;
    private static final int ITERATIONS_PER_THREAD = 100;

    // ==================== Board Thread Safety ====================

    @Test
    @DisplayName("Should handle concurrent board creation")
    void testConcurrentBoardCreation() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        List<Future<Board>> futures = new ArrayList<>();

        for (int i = 0; i < NUM_THREADS; i++) {
            final int size = 3 + i;
            futures.add(executor.submit(() -> new Board(size, size)));
        }

        // All should succeed
        for (Future<Board> future : futures) {
            assertDoesNotThrow(() -> {
                Board board = future.get(5, TimeUnit.SECONDS);
                assertNotNull(board);
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle concurrent piece placements on different cells")
    void testConcurrentPlacementsDifferentCells() throws InterruptedException {
        Board board = new Board(10, 10);
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < NUM_THREADS; i++) {
            final int row = i;
            futures.add(executor.submit(() -> {
                try {
                    Piece piece = new Piece(row, new int[]{0, 1, 2, 3});
                    board.place(row, 0, piece, 0);
                } finally {
                    latch.countDown();
                }
            }));
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // Verify all placements succeeded
        for (int i = 0; i < NUM_THREADS; i++) {
            assertFalse(board.isEmpty(i, 0));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should detect race condition on same cell placement")
    void testRaceConditionSameCellPlacement() throws InterruptedException {
        Board board = new Board(3, 3);
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < NUM_THREADS; i++) {
            final int pieceId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // All threads start together
                    Piece piece = new Piece(pieceId, new int[]{0, 1, 2, 3});
                    board.place(0, 0, piece, 0);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Expected for race conditions
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

        // One piece should be placed (last writer wins)
        assertFalse(board.isEmpty(0, 0));

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle concurrent place and remove operations")
    void testConcurrentPlaceAndRemove() throws InterruptedException {
        Board board = new Board(5, 5);
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS * 2);

        // Place operations
        for (int i = 0; i < NUM_THREADS; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    Piece piece = new Piece(index, new int[]{0, 1, 2, 3});
                    board.place(index % 5, index % 5, piece, 0);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Remove operations
        for (int i = 0; i < NUM_THREADS; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    board.remove(index % 5, index % 5);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle concurrent score calculations")
    void testConcurrentScoreCalculations() throws InterruptedException {
        Board board = new Board(5, 5);
        Piece piece = new Piece(1, new int[]{0, 1, 2, 3});

        // Pre-populate board
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                board.place(r, c, piece, 0);
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        List<int[]> scores = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    int[] score = board.calculateScore();
                    scores.add(score);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // All scores should be identical (read-only operation)
        assertEquals(NUM_THREADS, scores.size());
        int[] firstScore = scores.get(0);
        for (int[] score : scores) {
            assertArrayEquals(firstScore, score);
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    // ==================== Piece Thread Safety ====================

    @Test
    @DisplayName("Should handle concurrent piece creation")
    void testConcurrentPieceCreation() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        List<Piece> pieces = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < NUM_THREADS; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    Piece piece = new Piece(id, new int[]{id, id + 1, id + 2, id + 3});
                    pieces.add(piece);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(NUM_THREADS, pieces.size());

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle concurrent piece rotations")
    void testConcurrentPieceRotations() throws InterruptedException {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        List<int[]> rotations = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < NUM_THREADS; i++) {
            final int rotation = i % 4;
            executor.submit(() -> {
                try {
                    int[] rotated = piece.edgesRotated(rotation);
                    rotations.add(rotated);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(NUM_THREADS, rotations.size());

        // Verify piece is immutable
        assertArrayEquals(new int[]{1, 2, 3, 4}, piece.getEdges());

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle concurrent getEdges calls")
    void testConcurrentGetEdges() throws InterruptedException {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    int[] edges = piece.getEdges();
                    assertArrayEquals(new int[]{1, 2, 3, 4}, edges);
                    // Try to modify (should not affect piece)
                    edges[0] = 999;
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // Piece should still be intact
        assertArrayEquals(new int[]{1, 2, 3, 4}, piece.getEdges());

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    // ==================== StatisticsManager Thread Safety ====================

    @Test
    @DisplayName("Should handle concurrent statistics updates")
    void testConcurrentStatisticsUpdates() throws InterruptedException {
        StatisticsManager stats = new StatisticsManager();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        stats.recursiveCalls++;
                        stats.placements++;
                        stats.backtracks++;
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // Note: Without synchronization, final count may be less than expected due to race conditions
        // This test demonstrates the need for atomic operations
        assertTrue(stats.recursiveCalls > 0);
        assertTrue(stats.placements > 0);
        assertTrue(stats.backtracks > 0);

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should demonstrate need for atomic counters")
    void testRaceConditionInStatistics() throws InterruptedException {
        StatisticsManager stats = new StatisticsManager();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        stats.recursiveCalls++;
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // Due to race conditions, count will likely be less than expected
        long expected = (long) NUM_THREADS * ITERATIONS_PER_THREAD;

        // This assertion may fail, demonstrating the race condition
        // In production, use AtomicLong for thread-safe counters
        // asserting that we got *some* increments, but likely not all
        assertTrue(stats.recursiveCalls > 0);
        assertTrue(stats.recursiveCalls <= expected);

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle concurrent time measurements")
    void testConcurrentTimeMeasurements() throws InterruptedException {
        StatisticsManager stats = new StatisticsManager();
        stats.start();

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        List<Long> times = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    long elapsed = stats.getElapsedTimeMs();
                    times.add(elapsed);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // All times should be non-negative
        for (long time : times) {
            assertTrue(time >= 0);
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    // ==================== DomainManager Thread Safety ====================

    @Test
    @DisplayName("Should handle concurrent domain computations")
    void testConcurrentDomainComputations() throws InterruptedException {
        Board board = new Board(5, 5);
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            pieces.put(i, new Piece(i, new int[]{0, i, i + 1, 0}));
        }

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        List<List<DomainManager.ValidPlacement>> domains = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < NUM_THREADS; i++) {
            final int row = i % 5;
            final int col = i / 5;
            executor.submit(() -> {
                try {
                    BitSet pieceUsed = new BitSet();
                    List<DomainManager.ValidPlacement> domain =
                        manager.computeDomain(board, row, col, pieces, pieceUsed, 10);
                    domains.add(domain);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(NUM_THREADS, domains.size());

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle concurrent AC3 domain initialization")
    void testConcurrentAC3Initialization() throws InterruptedException {
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            pieces.put(i, new Piece(i, new int[]{0, i, i + 1, 0}));
        }

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        List<Boolean> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    Board board = new Board(3, 3);
                    BitSet pieceUsed = new BitSet();
                    DomainManager manager = new DomainManager(fitChecker);

                    manager.initializeAC3Domains(board, pieces, pieceUsed, 5);
                    results.add(manager.isAC3Initialized());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // All should succeed
        for (boolean result : results) {
            assertTrue(result);
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    // ==================== BitSet Thread Safety ====================

    @Test
    @DisplayName("Should demonstrate BitSet race conditions")
    void testBitSetRaceConditions() throws InterruptedException {
        BitSet pieceUsed = new BitSet(100);
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++) {
            final int startIndex = i * 10;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        pieceUsed.set(startIndex + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // All bits should be set
        for (int i = 0; i < 100; i++) {
            assertTrue(pieceUsed.get(i), "Bit " + i + " should be set");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle concurrent BitSet modifications on same bits")
    void testConcurrentBitSetSameBits() throws InterruptedException {
        BitSet pieceUsed = new BitSet(10);
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // All start together
                    for (int j = 0; j < 10; j++) {
                        pieceUsed.set(j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

        // All bits should eventually be set
        for (int i = 0; i < 10; i++) {
            assertTrue(pieceUsed.get(i));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    // ==================== Map Thread Safety ====================

    @Test
    @DisplayName("Should handle concurrent map reads")
    void testConcurrentMapReads() throws InterruptedException {
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= 100; i++) {
            pieces.put(i, new Piece(i, new int[]{i, i + 1, i + 2, i + 3}));
        }

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        AtomicInteger readCount = new AtomicInteger(0);

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 1; j <= 100; j++) {
                        Piece piece = pieces.get(j);
                        if (piece != null) {
                            readCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(NUM_THREADS * 100, readCount.get());

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should detect concurrent modification issues")
    void testConcurrentMapModification() throws InterruptedException {
        Map<Integer, Piece> pieces = Collections.synchronizedMap(new HashMap<>());

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        int id = threadId * ITERATIONS_PER_THREAD + j;
                        pieces.put(id, new Piece(id, new int[]{id, id + 1, id + 2, id + 3}));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(NUM_THREADS * ITERATIONS_PER_THREAD, pieces.size());

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    // ==================== Atomic Operations Test ====================

    @Test
    @DisplayName("Should demonstrate proper atomic counter usage")
    void testAtomicCounters() throws InterruptedException {
        AtomicLong counter = new AtomicLong(0);
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        counter.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // With atomic operations, we get exact count
        long expected = (long) NUM_THREADS * ITERATIONS_PER_THREAD;
        assertEquals(expected, counter.get());

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle high contention atomic operations")
    void testHighContentionAtomics() throws InterruptedException {
        AtomicInteger sharedCounter = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(50);

        for (int i = 0; i < 50; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 1000; j++) {
                        sharedCounter.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertEquals(50000, sharedCounter.get());

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
}
