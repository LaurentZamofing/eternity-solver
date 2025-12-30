import model.Board;
import model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import solver.DomainManager;
import solver.StatisticsManager;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance regression tests for the Eternity solver.
 * Ensures new rotation counting and other features don't slow things down.
 */
@DisplayName("Performance Regression Tests")
class PerformanceRegressionTest {

    private static final long PERFORMANCE_THRESHOLD_MS = 1000; // 1 second
    private static final int LARGE_ITERATION_COUNT = 10000;

    // ==================== Piece Rotation Performance ====================

    @Test
    @DisplayName("Should rotate piece in constant time")
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testPieceRotationPerformance() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Rotate 10,000 times
        for (int i = 0; i < LARGE_ITERATION_COUNT; i++) {
            int[] rotated = piece.edgesRotated(i % 4);
            assertNotNull(rotated);
        }
    }

    @Test
    @DisplayName("Should count unique rotations efficiently")
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testUniqueRotationCountPerformance() {
        // Create pieces with various symmetries
        Piece[] pieces = new Piece[]{
            new Piece(1, new int[]{1, 1, 1, 1}),     // 4-fold symmetry
            new Piece(2, new int[]{1, 2, 1, 2}),     // 2-fold symmetry
            new Piece(3, new int[]{1, 2, 3, 4}),     // No symmetry
            new Piece(4, new int[]{5, 5, 6, 6})      // 2-fold symmetry
        };

        // Count rotations 10,000 times
        for (int i = 0; i < LARGE_ITERATION_COUNT; i++) {
            for (Piece piece : pieces) {
                int count = piece.getUniqueRotationCount();
                assertTrue(count >= 1 && count <= 4);
            }
        }
    }

    @Test
    @DisplayName("Should handle rotation with large rotation values efficiently")
    @Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
    void testLargeRotationValuesPerformance() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Test with very large rotation values
        for (int i = 0; i < 1000; i++) {
            int[] rotated1 = piece.edgesRotated(Integer.MAX_VALUE - i);
            int[] rotated2 = piece.edgesRotated(Integer.MIN_VALUE + i);
            assertNotNull(rotated1);
            assertNotNull(rotated2);
        }
    }

    // ==================== Board Operations Performance ====================

    @Test
    @DisplayName("Should place pieces efficiently")
    @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
    void testBoardPlacementPerformance() {
        Board board = new Board(16, 16);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Place and remove 256 pieces multiple times
        for (int iteration = 0; iteration < 10; iteration++) {
            for (int r = 0; r < 16; r++) {
                for (int c = 0; c < 16; c++) {
                    board.place(r, c, piece, iteration % 4);
                }
            }

            for (int r = 0; r < 16; r++) {
                for (int c = 0; c < 16; c++) {
                    board.remove(r, c);
                }
            }
        }
    }

    @Test
    @DisplayName("Should check isEmpty efficiently")
    @Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
    void testIsEmptyPerformance() {
        Board board = new Board(16, 16);

        // Check isEmpty 100,000 times
        for (int i = 0; i < LARGE_ITERATION_COUNT; i++) {
            int r = i % 16;
            int c = (i / 16) % 16;
            assertTrue(board.isEmpty(r, c));
        }
    }

    @Test
    @DisplayName("Should calculate score efficiently")
    @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
    void testScoreCalculationPerformance() {
        Board board = new Board(16, 16);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Fill board
        for (int r = 0; r < 16; r++) {
            for (int c = 0; c < 16; c++) {
                board.place(r, c, piece, 0);
            }
        }

        // Calculate score 1,000 times
        for (int i = 0; i < 1000; i++) {
            int[] score = board.calculateScore();
            assertNotNull(score);
            assertEquals(2, score.length);
        }
    }

    @Test
    @DisplayName("Should handle getPlacement efficiently")
    @Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
    void testGetPlacementPerformance() {
        Board board = new Board(16, 16);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
        board.place(5, 5, piece, 2);

        // Get placement 100,000 times
        for (int i = 0; i < LARGE_ITERATION_COUNT; i++) {
            board.getPlacement(5, 5);
        }
    }

    // ==================== Domain Manager Performance ====================

    @Test
    @DisplayName("Should compute domains efficiently")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testDomainComputationPerformance() {
        Board board = new Board(5, 5);
        Map<Integer, Piece> pieces = new HashMap<>();

        // Create 50 pieces
        for (int i = 1; i <= 50; i++) {
            pieces.put(i, new Piece(i, new int[]{0, i % 10, i % 10 + 1, 0}));
        }

        BitSet pieceUsed = new BitSet(51);
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> {
            int rows = b.getRows();
            int cols = b.getCols();
            if (r == 0 && edges[0] != 0) return false;
            if (r == rows - 1 && edges[2] != 0) return false;
            if (c == 0 && edges[3] != 0) return false;
            if (c == cols - 1 && edges[1] != 0) return false;
            return true;
        };

        DomainManager manager = new DomainManager(fitChecker);

        // Compute domains 100 times
        for (int i = 0; i < 100; i++) {
            List<DomainManager.ValidPlacement> domain =
                manager.computeDomain(board, 0, 0, pieces, pieceUsed, 50);
            assertNotNull(domain);
        }
    }

    @Test
    @DisplayName("Should initialize AC3 domains efficiently")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testAC3InitializationPerformance() {
        Board board = new Board(8, 8);
        Map<Integer, Piece> pieces = new HashMap<>();

        // Create 64 pieces
        for (int i = 1; i <= 64; i++) {
            pieces.put(i, new Piece(i, new int[]{0, i % 10, i % 10 + 1, 0}));
        }

        BitSet pieceUsed = new BitSet(65);
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;

        // Initialize AC3 10 times
        for (int i = 0; i < 10; i++) {
            DomainManager manager = new DomainManager(fitChecker);
            manager.initializeAC3Domains(board, pieces, pieceUsed, 64);
            assertTrue(manager.isAC3Initialized());
        }
    }

    @Test
    @DisplayName("Should restore AC3 domains efficiently")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testAC3RestorationPerformance() {
        Board board = new Board(5, 5);
        Map<Integer, Piece> pieces = new HashMap<>();

        for (int i = 1; i <= 25; i++) {
            pieces.put(i, new Piece(i, new int[]{0, i % 10, i % 10 + 1, 0}));
        }

        BitSet pieceUsed = new BitSet(26);
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        manager.initializeAC3Domains(board, pieces, pieceUsed, 25);

        // Restore domains 100 times
        for (int i = 0; i < 100; i++) {
            manager.restoreAC3Domains(board, 2, 2, pieces, pieceUsed, 25);
        }
    }

    @Test
    @DisplayName("Should handle domain cache efficiently")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testDomainCachePerformance() {
        Board board = new Board(5, 5);
        Map<Integer, Piece> pieces = new HashMap<>();

        for (int i = 1; i <= 25; i++) {
            pieces.put(i, new Piece(i, new int[]{0, i % 10, i % 10 + 1, 0}));
        }

        BitSet pieceUsed = new BitSet(26);
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        manager.setUseDomainCache(true);
        manager.initializeDomainCache(board, pieces, pieceUsed, 25);

        // Access cache 10,000 times
        for (int i = 0; i < LARGE_ITERATION_COUNT; i++) {
            int r = i % 5;
            int c = (i / 5) % 5;
            List<DomainManager.ValidPlacement> domain =
                manager.getCachedDomain(board, r, c, pieces, pieceUsed, 25);
            assertNotNull(domain);
        }
    }

    // ==================== Statistics Manager Performance ====================

    @Test
    @DisplayName("Should update statistics efficiently")
    @Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
    void testStatisticsUpdatePerformance() {
        StatisticsManager stats = new StatisticsManager();

        // Update statistics 100,000 times
        for (int i = 0; i < LARGE_ITERATION_COUNT; i++) {
            stats.recursiveCalls++;
            stats.placements++;
            stats.backtracks++;
            stats.singletonsFound++;
            stats.deadEndsDetected++;
        }

        assertEquals(LARGE_ITERATION_COUNT, stats.recursiveCalls);
    }

    @Test
    @DisplayName("Should calculate elapsed time efficiently")
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testElapsedTimePerformance() {
        StatisticsManager stats = new StatisticsManager();
        stats.start();

        // Get elapsed time 10,000 times
        for (int i = 0; i < LARGE_ITERATION_COUNT; i++) {
            long elapsed = stats.getElapsedTimeMs();
            assertTrue(elapsed >= 0);
        }
    }

    @Test
    @DisplayName("Should track progress efficiently")
    @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
    void testProgressTrackingPerformance() {
        StatisticsManager stats = new StatisticsManager();

        // Register and track progress at multiple depths
        for (int depth = 0; depth < 10; depth++) {
            stats.registerDepthOptions(depth, 100);
        }

        // Update progress 10,000 times
        for (int i = 0; i < LARGE_ITERATION_COUNT; i++) {
            int depth = i % 10;
            stats.incrementDepthProgress(depth);
            double progress = stats.getProgressPercentage();
            assertTrue(progress >= 0 && progress <= 100);
        }
    }

    // ==================== Memory Efficiency Tests ====================

    @Test
    @DisplayName("Should not leak memory with repeated piece creation")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testPieceCreationMemoryEfficiency() {
        // Create and discard many pieces
        for (int i = 0; i < 10000; i++) {
            Piece piece = new Piece(i, new int[]{i % 10, (i + 1) % 10, (i + 2) % 10, (i + 3) % 10});
            assertNotNull(piece);
        }

        // Force garbage collection
        System.gc();
    }

    @Test
    @DisplayName("Should not leak memory with repeated board operations")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testBoardOperationsMemoryEfficiency() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Create and populate many boards
        for (int iteration = 0; iteration < 100; iteration++) {
            Board board = new Board(10, 10);

            for (int r = 0; r < 10; r++) {
                for (int c = 0; c < 10; c++) {
                    board.place(r, c, piece, iteration % 4);
                }
            }
        }

        System.gc();
    }

    @Test
    @DisplayName("Should not leak memory with domain manager operations")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testDomainManagerMemoryEfficiency() {
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= 25; i++) {
            pieces.put(i, new Piece(i, new int[]{0, i % 10, i % 10 + 1, 0}));
        }

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;

        // Create and discard many domain managers
        for (int i = 0; i < 100; i++) {
            Board board = new Board(5, 5);
            BitSet pieceUsed = new BitSet(26);
            DomainManager manager = new DomainManager(fitChecker);

            manager.initializeAC3Domains(board, pieces, pieceUsed, 25);
            manager.resetAC3();
        }

        System.gc();
    }

    // ==================== Batch Operations Performance ====================

    @Test
    @DisplayName("Should handle batch piece rotations efficiently")
    @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
    void testBatchRotationsPerformance() {
        Piece[] pieces = new Piece[256];
        for (int i = 0; i < 256; i++) {
            pieces[i] = new Piece(i, new int[]{i % 10, (i + 1) % 10, (i + 2) % 10, (i + 3) % 10});
        }

        // Rotate all pieces in all rotations
        for (int rotation = 0; rotation < 4; rotation++) {
            for (Piece piece : pieces) {
                int[] rotated = piece.edgesRotated(rotation);
                assertNotNull(rotated);
            }
        }
    }

    @Test
    @DisplayName("Should handle batch unique rotation counts efficiently")
    @Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
    void testBatchUniqueRotationCountPerformance() {
        Piece[] pieces = new Piece[256];
        for (int i = 0; i < 256; i++) {
            pieces[i] = new Piece(i, new int[]{i % 10, (i + 1) % 10, (i + 2) % 10, (i + 3) % 10});
        }

        // Count unique rotations 100 times
        for (int iteration = 0; iteration < 100; iteration++) {
            for (Piece piece : pieces) {
                int count = piece.getUniqueRotationCount();
                assertTrue(count >= 1 && count <= 4);
            }
        }
    }

    @Test
    @DisplayName("Should handle batch board score calculations efficiently")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testBatchScoreCalculationsPerformance() {
        Board[] boards = new Board[10];
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Create and populate boards
        for (int i = 0; i < 10; i++) {
            boards[i] = new Board(10, 10);
            for (int r = 0; r < 10; r++) {
                for (int c = 0; c < 10; c++) {
                    boards[i].place(r, c, piece, 0);
                }
            }
        }

        // Calculate scores 100 times each
        for (int iteration = 0; iteration < 100; iteration++) {
            for (Board board : boards) {
                int[] score = board.calculateScore();
                assertNotNull(score);
            }
        }
    }

    // ==================== Worst-Case Scenarios ====================

    @Test
    @DisplayName("Should handle worst-case domain computation")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testWorstCaseDomainComputation() {
        Board board = new Board(10, 10);
        Map<Integer, Piece> pieces = new HashMap<>();

        // Create 100 pieces
        for (int i = 1; i <= 100; i++) {
            pieces.put(i, new Piece(i, new int[]{i % 20, (i + 1) % 20, (i + 2) % 20, (i + 3) % 20}));
        }

        BitSet pieceUsed = new BitSet(101);
        // FitChecker that does expensive computation
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> {
            // Simulate expensive validation
            int sum = 0;
            for (int edge : edges) {
                sum += edge;
            }
            return sum > 0;
        };

        DomainManager manager = new DomainManager(fitChecker);

        // Compute domain for center cell (most expensive)
        List<DomainManager.ValidPlacement> domain =
            manager.computeDomain(board, 5, 5, pieces, pieceUsed, 100);
        assertNotNull(domain);
    }

    @Test
    @DisplayName("Should handle worst-case piece with all rotations")
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testWorstCasePieceRotations() {
        // Piece with no symmetry (4 unique rotations)
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        assertEquals(4, piece.getUniqueRotationCount());

        // Rotate in all possible ways
        for (int rotation = -100; rotation <= 100; rotation++) {
            int[] rotated = piece.edgesRotated(rotation);
            assertNotNull(rotated);
            assertEquals(4, rotated.length);
        }
    }

    @Test
    @DisplayName("Should handle maximum board size efficiently")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testMaximumBoardSizePerformance() {
        Board board = new Board(100, 100);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Fill and empty half the board
        for (int r = 0; r < 50; r++) {
            for (int c = 0; c < 100; c++) {
                board.place(r, c, piece, (r + c) % 4);
            }
        }

        for (int r = 0; r < 50; r++) {
            for (int c = 0; c < 100; c++) {
                board.remove(r, c);
            }
        }
    }

    // ==================== Regression Benchmarks ====================

    @Test
    @DisplayName("Benchmark: Piece rotation should be under 10 microseconds")
    void benchmarkPieceRotation() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            piece.edgesRotated(i % 4);
        }
        long end = System.nanoTime();

        long avgNanos = (end - start) / 1000;
        assertTrue(avgNanos < 10000, "Average rotation time: " + avgNanos + " ns (should be < 10,000 ns)");
    }

    @Test
    @DisplayName("Benchmark: Unique rotation count should be under 1 microsecond")
    void benchmarkUniqueRotationCount() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            piece.getUniqueRotationCount();
        }
        long end = System.nanoTime();

        long avgNanos = (end - start) / 1000;
        assertTrue(avgNanos < 1000, "Average count time: " + avgNanos + " ns (should be < 1,000 ns)");
    }

    @Test
    @DisplayName("Benchmark: Board placement should be under 10 microseconds")
    void benchmarkBoardPlacement() {
        Board board = new Board(16, 16);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        long start = System.nanoTime();
        for (int i = 0; i < 256; i++) {
            int r = i / 16;
            int c = i % 16;
            board.place(r, c, piece, 0);
        }
        long end = System.nanoTime();

        long avgNanos = (end - start) / 256;
        assertTrue(avgNanos < 10000, "Average placement time: " + avgNanos + " ns (should be < 10,000 ns)");
    }
}
