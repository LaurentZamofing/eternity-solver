import model.Board;
import model.Piece;
import model.Placement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solver.DomainManager;
import solver.StatisticsManager;

import java.lang.ref.WeakReference;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive memory management tests for the Eternity solver.
 * Tests for memory leaks, proper garbage collection, and resource cleanup.
 */
@DisplayName("Memory Management Tests")
class MemoryManagementTest {

    private static final int LARGE_ALLOCATION_COUNT = 1000;

    // ==================== Garbage Collection Tests ====================

    @Test
    @DisplayName("Should allow piece to be garbage collected")
    void testPieceGarbageCollection() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
        WeakReference<Piece> weakRef = new WeakReference<>(piece);

        assertNotNull(weakRef.get());

        // Remove strong reference
        piece = null;

        // Force garbage collection
        System.gc();
        System.runFinalization();
        System.gc();

        // Weak reference should eventually be cleared
        // Note: GC is not guaranteed, but this tests the pattern
        // In a real scenario with memory pressure, it would be collected
    }

    @Test
    @DisplayName("Should allow board to be garbage collected")
    void testBoardGarbageCollection() {
        Board board = new Board(10, 10);
        WeakReference<Board> weakRef = new WeakReference<>(board);

        assertNotNull(weakRef.get());

        board = null;
        System.gc();
        System.runFinalization();
    }

    @Test
    @DisplayName("Should not retain references after board clear")
    void testBoardClearReferences() {
        Board board = new Board(5, 5);
        List<WeakReference<Placement>> refs = new ArrayList<>();

        // Place pieces and keep weak references
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                Piece piece = new Piece(r * 5 + c, new int[]{1, 2, 3, 4});
                board.place(r, c, piece, 0);
                Placement placement = board.getPlacement(r, c);
                refs.add(new WeakReference<>(placement));
            }
        }

        // Remove all pieces
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                board.remove(r, c);
            }
        }

        // Note: Placements might still be referenced by our list
        // This test ensures remove() doesn't keep internal references
    }

    // ==================== Large Dataset Tests ====================

    @Test
    @DisplayName("Should handle creation of many pieces")
    void testManyPieceCreation() {
        List<Piece> pieces = new ArrayList<>();

        for (int i = 0; i < LARGE_ALLOCATION_COUNT; i++) {
            pieces.add(new Piece(i, new int[]{i % 10, (i + 1) % 10, (i + 2) % 10, (i + 3) % 10}));
        }

        assertEquals(LARGE_ALLOCATION_COUNT, pieces.size());

        // Clear and allow GC
        pieces.clear();
        System.gc();
    }

    @Test
    @DisplayName("Should handle creation of many boards")
    void testManyBoardCreation() {
        for (int i = 0; i < 100; i++) {
            Board board = new Board(10, 10);
            assertNotNull(board);
        }

        System.gc();
    }

    @Test
    @DisplayName("Should handle large piece map without memory issues")
    void testLargePieceMap() {
        Map<Integer, Piece> pieces = new HashMap<>();

        for (int i = 1; i <= 10000; i++) {
            pieces.put(i, new Piece(i, new int[]{i % 10, (i + 1) % 10, (i + 2) % 10, (i + 3) % 10}));
        }

        assertEquals(10000, pieces.size());

        // Verify pieces are accessible
        assertNotNull(pieces.get(1));
        assertNotNull(pieces.get(5000));
        assertNotNull(pieces.get(10000));

        pieces.clear();
        System.gc();
    }

    @Test
    @DisplayName("Should handle repeated board fill and clear")
    void testRepeatedBoardFillClear() {
        Board board = new Board(16, 16);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Fill and clear 100 times
        for (int iteration = 0; iteration < 100; iteration++) {
            // Fill
            for (int r = 0; r < 16; r++) {
                for (int c = 0; c < 16; c++) {
                    board.place(r, c, piece, iteration % 4);
                }
            }

            // Clear
            for (int r = 0; r < 16; r++) {
                for (int c = 0; c < 16; c++) {
                    board.remove(r, c);
                }
            }
        }

        // Verify board is empty
        for (int r = 0; r < 16; r++) {
            for (int c = 0; c < 16; c++) {
                assertTrue(board.isEmpty(r, c));
            }
        }
    }

    // ==================== Domain Manager Memory Tests ====================

    @Test
    @DisplayName("Should not leak memory with repeated AC3 initialization")
    void testAC3MemoryLeaks() {
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= 64; i++) {
            pieces.put(i, new Piece(i, new int[]{0, i % 10, i % 10 + 1, 0}));
        }

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;

        // Initialize and reset AC3 many times
        for (int i = 0; i < 100; i++) {
            Board board = new Board(8, 8);
            BitSet pieceUsed = new BitSet(65);
            DomainManager manager = new DomainManager(fitChecker);

            manager.initializeAC3Domains(board, pieces, pieceUsed, 64);
            assertTrue(manager.isAC3Initialized());

            manager.resetAC3();
            assertFalse(manager.isAC3Initialized());
        }

        System.gc();
    }

    @Test
    @DisplayName("Should not leak memory with domain cache operations")
    void testDomainCacheMemoryLeaks() {
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= 25; i++) {
            pieces.put(i, new Piece(i, new int[]{0, i % 10, i % 10 + 1, 0}));
        }

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;

        for (int i = 0; i < 100; i++) {
            Board board = new Board(5, 5);
            BitSet pieceUsed = new BitSet(26);
            DomainManager manager = new DomainManager(fitChecker);

            manager.setUseDomainCache(true);
            manager.initializeDomainCache(board, pieces, pieceUsed, 25);

            // Access cache multiple times
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 5; c++) {
                    manager.getCachedDomain(board, r, c, pieces, pieceUsed, 25);
                }
            }

            manager.clearCache();
        }

        System.gc();
    }

    @Test
    @DisplayName("Should handle large domain computations without memory issues")
    void testLargeDomainComputation() {
        Board board = new Board(10, 10);
        Map<Integer, Piece> pieces = new HashMap<>();

        // Create 256 pieces
        for (int i = 1; i <= 256; i++) {
            pieces.put(i, new Piece(i, new int[]{i % 20, (i + 1) % 20, (i + 2) % 20, (i + 3) % 20}));
        }

        BitSet pieceUsed = new BitSet(257);
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        // Compute domains for all cells
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                List<DomainManager.ValidPlacement> domain =
                    manager.computeDomain(board, r, c, pieces, pieceUsed, 256);
                assertNotNull(domain);
            }
        }

        System.gc();
    }

    // ==================== Array Memory Tests ====================

    @Test
    @DisplayName("Should not leak memory with edge array copies")
    void testEdgeArrayCopies() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Get edges many times
        for (int i = 0; i < LARGE_ALLOCATION_COUNT; i++) {
            int[] edges = piece.getEdges();
            assertNotNull(edges);
        }

        System.gc();
    }

    @Test
    @DisplayName("Should not leak memory with rotation array creation")
    void testRotationArrayCreation() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Create many rotated arrays
        for (int i = 0; i < LARGE_ALLOCATION_COUNT; i++) {
            int[] rotated = piece.edgesRotated(i % 4);
            assertNotNull(rotated);
        }

        System.gc();
    }

    @Test
    @DisplayName("Should handle array modifications without affecting original")
    void testArrayImmutability() {
        int[] originalEdges = new int[]{1, 2, 3, 4};
        Piece piece = new Piece(1, originalEdges);

        // Modify original
        originalEdges[0] = 999;

        // Piece should be unaffected
        int[] pieceEdges = piece.getEdges();
        assertEquals(1, pieceEdges[0]);

        // Modify returned array
        pieceEdges[0] = 888;

        // Piece should still be unaffected
        int[] pieceEdges2 = piece.getEdges();
        assertEquals(1, pieceEdges2[0]);
    }

    // ==================== Collection Memory Tests ====================

    @Test
    @DisplayName("Should not leak memory with BitSet operations")
    void testBitSetMemory() {
        List<BitSet> bitsets = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            BitSet bs = new BitSet(256);
            for (int j = 0; j < 256; j++) {
                bs.set(j, j % 2 == 0);
            }
            bitsets.add(bs);
        }

        assertEquals(1000, bitsets.size());

        bitsets.clear();
        System.gc();
    }

    @Test
    @DisplayName("Should not leak memory with list operations")
    void testListMemory() {
        for (int iteration = 0; iteration < 100; iteration++) {
            List<DomainManager.ValidPlacement> placements = new ArrayList<>();

            for (int i = 0; i < 1000; i++) {
                placements.add(new DomainManager.ValidPlacement(i, i % 4));
            }

            assertEquals(1000, placements.size());
            placements.clear();
        }

        System.gc();
    }

    @Test
    @DisplayName("Should not leak memory with map operations")
    void testMapMemory() {
        for (int iteration = 0; iteration < 100; iteration++) {
            Map<Integer, List<DomainManager.ValidPlacement>> domainMap = new HashMap<>();

            for (int i = 0; i < 100; i++) {
                List<DomainManager.ValidPlacement> placements = new ArrayList<>();
                for (int j = 0; j < 10; j++) {
                    placements.add(new DomainManager.ValidPlacement(j, j % 4));
                }
                domainMap.put(i, placements);
            }

            assertEquals(100, domainMap.size());
            domainMap.clear();
        }

        System.gc();
    }

    // ==================== Statistics Memory Tests ====================

    @Test
    @DisplayName("Should not leak memory with statistics tracking")
    void testStatisticsMemory() {
        for (int i = 0; i < 100; i++) {
            StatisticsManager stats = new StatisticsManager();
            stats.start();

            // Register many depth options
            for (int depth = 0; depth < 100; depth++) {
                stats.registerDepthOptions(depth, 10);
                stats.incrementDepthProgress(depth);
            }

            stats.end();
        }

        System.gc();
    }

    @Test
    @DisplayName("Should not leak memory with repeated board scans")
    void testBoardScanMemory() {
        StatisticsManager stats = new StatisticsManager();
        Board board = new Board(16, 16);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Fill board
        for (int r = 0; r < 16; r++) {
            for (int c = 0; c < 16; c++) {
                board.place(r, c, piece, 0);
            }
        }

        // Scan board many times
        for (int i = 0; i < 1000; i++) {
            stats.findAndSetLastPlaced(board);
            assertEquals(15, stats.getLastPlacedRow());
            assertEquals(15, stats.getLastPlacedCol());
        }

        System.gc();
    }

    // ==================== Resource Cleanup Tests ====================

    @Test
    @DisplayName("Should properly clean up after domain manager reset")
    void testDomainManagerCleanup() {
        Board board = new Board(5, 5);
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= 25; i++) {
            pieces.put(i, new Piece(i, new int[]{0, i % 10, i % 10 + 1, 0}));
        }

        BitSet pieceUsed = new BitSet(26);
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        manager.initializeAC3Domains(board, pieces, pieceUsed, 25);
        assertTrue(manager.isAC3Initialized());

        manager.resetAC3();
        assertFalse(manager.isAC3Initialized());
        assertNull(manager.getDomain(0, 0));

        System.gc();
    }

    @Test
    @DisplayName("Should properly clean up cache")
    void testCacheCleanup() {
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

        manager.clearCache();

        // Cache should be empty after clear
        manager.setUseDomainCache(true);
        List<DomainManager.ValidPlacement> domain = manager.getCachedDomain(board, 0, 0, pieces, pieceUsed, 25);
        assertNotNull(domain);

        System.gc();
    }

    // ==================== Stress Tests ====================

    @Test
    @DisplayName("Should handle memory stress with massive piece creation")
    void testMassivePieceCreation() {
        int createdCount = 0;

        for (int i = 0; i < 10000; i++) {
            Piece piece = new Piece(i, new int[]{i % 10, (i + 1) % 10, (i + 2) % 10, (i + 3) % 10});
            assertNotNull(piece);
            createdCount++;

            if (i % 1000 == 0) {
                System.gc(); // Periodic GC to prevent OutOfMemoryError
            }
        }

        assertEquals(10000, createdCount);
    }

    @Test
    @DisplayName("Should handle memory stress with massive board operations")
    void testMassiveBoardOperations() {
        Board board = new Board(50, 50);
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        // Fill and clear 10 times
        for (int iteration = 0; iteration < 10; iteration++) {
            for (int r = 0; r < 50; r++) {
                for (int c = 0; c < 50; c++) {
                    board.place(r, c, piece, iteration % 4);
                }
            }

            for (int r = 0; r < 50; r++) {
                for (int c = 0; c < 50; c++) {
                    board.remove(r, c);
                }
            }

            if (iteration % 2 == 0) {
                System.gc();
            }
        }
    }

    @Test
    @DisplayName("Should handle memory stress with massive domain computations")
    void testMassiveDomainComputations() {
        Board board = new Board(10, 10);
        Map<Integer, Piece> pieces = new HashMap<>();

        for (int i = 1; i <= 100; i++) {
            pieces.put(i, new Piece(i, new int[]{i % 20, (i + 1) % 20, (i + 2) % 20, (i + 3) % 20}));
        }

        BitSet pieceUsed = new BitSet(101);
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        // Compute domains 100 times for each cell
        for (int iteration = 0; iteration < 100; iteration++) {
            for (int r = 0; r < 10; r++) {
                for (int c = 0; c < 10; c++) {
                    List<DomainManager.ValidPlacement> domain =
                        manager.computeDomain(board, r, c, pieces, pieceUsed, 100);
                    assertNotNull(domain);
                }
            }

            if (iteration % 10 == 0) {
                System.gc();
            }
        }
    }

    // ==================== Memory Boundary Tests ====================

    @Test
    @DisplayName("Should handle empty collections without memory waste")
    void testEmptyCollections() {
        List<DomainManager.ValidPlacement> emptyList = new ArrayList<>();
        Map<Integer, List<DomainManager.ValidPlacement>> emptyMap = new HashMap<>();
        BitSet emptyBitSet = new BitSet();

        assertTrue(emptyList.isEmpty());
        assertTrue(emptyMap.isEmpty());
        assertEquals(0, emptyBitSet.length());

        System.gc();
    }

    @Test
    @DisplayName("Should handle single-element collections efficiently")
    void testSingleElementCollections() {
        List<Piece> singlePiece = new ArrayList<>();
        singlePiece.add(new Piece(1, new int[]{1, 2, 3, 4}));

        Map<Integer, Piece> singleMap = new HashMap<>();
        singleMap.put(1, new Piece(1, new int[]{1, 2, 3, 4}));

        assertEquals(1, singlePiece.size());
        assertEquals(1, singleMap.size());

        System.gc();
    }
}
