import model.Board;
import model.Piece;
import model.Placement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solver.DomainManager;
import solver.StatisticsManager;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive null safety tests for the Eternity solver.
 * Tests every parameter and method that could receive null values.
 */
@DisplayName("Null Safety Tests")
class NullSafetyTest {

    // ==================== Board Null Safety ====================

    @Test
    @DisplayName("Should throw when placing null piece")
    void testPlaceNullPiece() {
        Board board = new Board(3, 3);
        assertThrows(NullPointerException.class, () -> board.place(0, 0, null, 0));
    }

    @Test
    @DisplayName("Should return null for empty cell")
    void testGetPlacementOnEmptyCell() {
        Board board = new Board(3, 3);
        assertNull(board.getPlacement(0, 0));
    }

    @Test
    @DisplayName("Should handle null pieces map in prettyPrint")
    void testPrettyPrintWithNullMap() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{0, 1, 2, 3});
        board.place(0, 0, piece, 0);

        // Should not throw with null map
        assertDoesNotThrow(() -> board.prettyPrint(null));
    }

    @Test
    @DisplayName("Should handle empty map in prettyPrint")
    void testPrettyPrintWithEmptyMap() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{0, 1, 2, 3});
        board.place(0, 0, piece, 0);

        Map<Integer, Piece> emptyMap = new HashMap<>();
        assertDoesNotThrow(() -> board.prettyPrint(emptyMap));
    }

    // ==================== Piece Null Safety ====================

    @Test
    @DisplayName("Should throw when creating piece with null edges")
    void testPieceWithNullEdges() {
        assertThrows(NullPointerException.class, () -> new Piece(1, null));
    }

    @Test
    @DisplayName("Should handle piece edge rotation safely")
    void testPieceEdgesRotatedNullSafety() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
        assertNotNull(piece.edgesRotated(0));
        assertNotNull(piece.edgesRotated(1));
        assertNotNull(piece.edgesRotated(2));
        assertNotNull(piece.edgesRotated(3));
    }

    @Test
    @DisplayName("Should handle piece getEdges null safety")
    void testPieceGetEdgesNullSafety() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
        int[] edges = piece.getEdges();
        assertNotNull(edges);
        assertEquals(4, edges.length);
    }

    @Test
    @DisplayName("Should handle piece equals with null")
    void testPieceEqualsNull() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
        assertNotEquals(piece, null);
        assertFalse(piece.equals(null));
    }

    @Test
    @DisplayName("Should handle piece toString null safety")
    void testPieceToStringNullSafety() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
        String str = piece.toString();
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }

    // ==================== Placement Null Safety ====================

    @Test
    @DisplayName("Should create placement with valid edges")
    void testPlacementCreation() {
        int[] edges = new int[]{1, 2, 3, 4};
        Placement placement = new Placement(1, 0, edges);
        assertNotNull(placement);
        assertEquals(1, placement.getPieceId());
        assertEquals(0, placement.getRotation());
    }

    @Test
    @DisplayName("Should handle placement with null edges array")
    void testPlacementWithNullEdges() {
        assertThrows(NullPointerException.class, () -> new Placement(1, 0, null));
    }

    @Test
    @DisplayName("Should handle placement toString null safety")
    void testPlacementToStringNullSafety() {
        Placement placement = new Placement(1, 0, new int[]{1, 2, 3, 4});
        String str = placement.toString();
        assertNotNull(str);
    }

    // ==================== DomainManager Null Safety ====================

    @Test
    @DisplayName("Should handle creating DomainManager with null fit checker")
    void testDomainManagerNullFitChecker() {
        // DomainManager accepts null fit checker, but will fail on first use
        DomainManager manager = new DomainManager(null);
        assertNotNull(manager);
    }

    @Test
    @DisplayName("Should handle computeDomain with null board")
    void testComputeDomainNullBoard() {
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        Map<Integer, Piece> pieces = new HashMap<>();
        BitSet pieceUsed = new BitSet();

        // Will throw NullPointerException when trying to access board methods
        assertThrows(Exception.class,
            () -> manager.computeDomain(null, 0, 0, pieces, pieceUsed, 1));
    }

    @Test
    @DisplayName("Should handle computeDomain with null pieces map")
    void testComputeDomainNullPiecesMap() {
        Board board = new Board(3, 3);
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        BitSet pieceUsed = new BitSet();

        assertThrows(NullPointerException.class,
            () -> manager.computeDomain(board, 0, 0, null, pieceUsed, 1));
    }

    @Test
    @DisplayName("Should handle computeDomain with null BitSet")
    void testComputeDomainNullBitSet() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>();
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        assertThrows(NullPointerException.class,
            () -> manager.computeDomain(board, 0, 0, pieces, null, 1));
    }

    @Test
    @DisplayName("Should handle initializeAC3Domains with null board")
    void testInitializeAC3DomainsNullBoard() {
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        Map<Integer, Piece> pieces = new HashMap<>();
        BitSet pieceUsed = new BitSet();

        assertThrows(NullPointerException.class,
            () -> manager.initializeAC3Domains(null, pieces, pieceUsed, 1));
    }

    @Test
    @DisplayName("Should handle initializeAC3Domains with null pieces")
    void testInitializeAC3DomainsNullPieces() {
        Board board = new Board(3, 3);
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        BitSet pieceUsed = new BitSet();

        assertThrows(NullPointerException.class,
            () -> manager.initializeAC3Domains(board, null, pieceUsed, 1));
    }

    @Test
    @DisplayName("Should handle getDomain for uninitialized manager")
    void testGetDomainUninitialized() {
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        assertNull(manager.getDomain(0, 0));
    }

    @Test
    @DisplayName("Should handle setDomain with null domain")
    void testSetDomainNull() {
        Board board = new Board(3, 3);
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        Map<Integer, Piece> pieces = new HashMap<>();
        BitSet pieceUsed = new BitSet();

        manager.initializeAC3Domains(board, pieces, pieceUsed, 0);

        // Should not throw
        assertDoesNotThrow(() -> manager.setDomain(0, 0, null));
    }

    @Test
    @DisplayName("Should handle setSortOrder with null")
    void testSetSortOrderNull() {
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        // Should default to ascending
        assertDoesNotThrow(() -> manager.setSortOrder(null));
    }

    @Test
    @DisplayName("Should handle getCachedDomain with null board")
    void testGetCachedDomainNullBoard() {
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        Map<Integer, Piece> pieces = new HashMap<>();
        BitSet pieceUsed = new BitSet();

        assertThrows(NullPointerException.class,
            () -> manager.getCachedDomain(null, 0, 0, pieces, pieceUsed, 1));
    }

    @Test
    @DisplayName("Should handle restoreAC3Domains with null board")
    void testRestoreAC3DomainsNullBoard() {
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        Map<Integer, Piece> pieces = new HashMap<>();
        BitSet pieceUsed = new BitSet();

        // Restoring without initialization should be a no-op
        assertDoesNotThrow(() -> manager.restoreAC3Domains(null, 0, 0, pieces, pieceUsed, 1));
    }

    // ==================== StatisticsManager Null Safety ====================

    @Test
    @DisplayName("Should create StatisticsManager without null issues")
    void testStatisticsManagerCreation() {
        StatisticsManager stats = new StatisticsManager();
        assertNotNull(stats);
    }

    @Test
    @DisplayName("Should handle print methods without null issues")
    void testStatisticsManagerPrintMethods() {
        StatisticsManager stats = new StatisticsManager();
        stats.start();

        assertDoesNotThrow(() -> stats.print());
        assertDoesNotThrow(() -> stats.printCompact());
    }

    @Test
    @DisplayName("Should handle getElapsedTimeMs without starting")
    void testGetElapsedTimeWithoutStart() {
        StatisticsManager stats = new StatisticsManager();

        // Should handle case where startTime is 0
        assertDoesNotThrow(() -> stats.getElapsedTimeMs());
    }

    @Test
    @DisplayName("Should handle findAndSetLastPlaced with null board")
    void testFindAndSetLastPlacedNullBoard() {
        StatisticsManager stats = new StatisticsManager();

        assertThrows(NullPointerException.class, () -> stats.findAndSetLastPlaced(null));
    }

    @Test
    @DisplayName("Should handle findAndSetLastPlaced with empty board")
    void testFindAndSetLastPlacedEmptyBoard() {
        StatisticsManager stats = new StatisticsManager();
        Board board = new Board(3, 3);

        assertDoesNotThrow(() -> stats.findAndSetLastPlaced(board));
        assertEquals(-1, stats.getLastPlacedRow());
        assertEquals(-1, stats.getLastPlacedCol());
    }

    // ==================== Map Operations Null Safety ====================

    @Test
    @DisplayName("Should handle map with null values")
    void testMapWithNullPieceValues() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 1, 2, 3}));
        pieces.put(2, null);
        pieces.put(3, new Piece(3, new int[]{4, 5, 6, 7}));

        Board board = new Board(3, 3);
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        BitSet pieceUsed = new BitSet();

        // Should handle null piece in map
        List<DomainManager.ValidPlacement> domain = manager.computeDomain(board, 0, 0, pieces, pieceUsed, 3);
        assertNotNull(domain);
    }

    @Test
    @DisplayName("Should handle map get returning null")
    void testMapGetReturningNull() {
        Map<Integer, Piece> pieces = new HashMap<>();
        assertNull(pieces.get(999));
    }

    @Test
    @DisplayName("Should handle empty pieces map in domain computation")
    void testEmptyPiecesMapInDomain() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>();
        BitSet pieceUsed = new BitSet();

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        List<DomainManager.ValidPlacement> domain = manager.computeDomain(board, 0, 0, pieces, pieceUsed, 10);
        assertNotNull(domain);
        assertTrue(domain.isEmpty());
    }

    // ==================== Array Null Safety ====================

    @Test
    @DisplayName("Should handle board operations without null array issues")
    void testBoardArrayNullSafety() {
        Board board = new Board(3, 3);

        // Grid should be initialized, not null
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                final int row = r;
                final int col = c;
                // Should not throw NullPointerException
                assertDoesNotThrow(() -> board.isEmpty(row, col));
            }
        }
    }

    @Test
    @DisplayName("Should protect piece edges array immutability")
    void testPieceEdgesImmutability() {
        int[] originalEdges = new int[]{1, 2, 3, 4};
        Piece piece = new Piece(1, originalEdges);

        // Modify original array
        originalEdges[0] = 999;

        // Piece should not be affected
        int[] pieceEdges = piece.getEdges();
        assertEquals(1, pieceEdges[0]);
    }

    @Test
    @DisplayName("Should return defensive copy from getEdges")
    void testGetEdgesDefensiveCopy() {
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        int[] edges1 = piece.getEdges();
        int[] edges2 = piece.getEdges();

        // Should be different array instances
        assertNotSame(edges1, edges2);

        // But same values
        assertArrayEquals(edges1, edges2);

        // Modifying one should not affect the other
        edges1[0] = 999;
        assertEquals(1, edges2[0]);
    }

    // ==================== ValidPlacement Null Safety ====================

    @Test
    @DisplayName("Should create ValidPlacement without null issues")
    void testValidPlacementCreation() {
        DomainManager.ValidPlacement vp = new DomainManager.ValidPlacement(1, 0);
        assertNotNull(vp);
        assertEquals(1, vp.pieceId);
        assertEquals(0, vp.rotation);
    }

    @Test
    @DisplayName("Should handle ValidPlacement with boundary values")
    void testValidPlacementBoundaryValues() {
        DomainManager.ValidPlacement vp1 = new DomainManager.ValidPlacement(0, 0);
        assertEquals(0, vp1.pieceId);

        DomainManager.ValidPlacement vp2 = new DomainManager.ValidPlacement(-1, -1);
        assertEquals(-1, vp2.pieceId);
        assertEquals(-1, vp2.rotation);

        DomainManager.ValidPlacement vp3 = new DomainManager.ValidPlacement(Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, vp3.pieceId);
        assertEquals(Integer.MAX_VALUE, vp3.rotation);
    }

    // ==================== Domain Cache Null Safety ====================

    @Test
    @DisplayName("Should handle disabled cache")
    void testDisabledCache() {
        Board board = new Board(3, 3);
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        manager.setUseDomainCache(false);

        Map<Integer, Piece> pieces = new HashMap<>();
        BitSet pieceUsed = new BitSet();

        // Should still work without cache
        List<DomainManager.ValidPlacement> domain = manager.getCachedDomain(board, 0, 0, pieces, pieceUsed, 0);
        assertNotNull(domain);
    }

    @Test
    @DisplayName("Should handle clearCache on uninitialized cache")
    void testClearCacheUninitialized() {
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        manager.setUseDomainCache(false);

        // Should not throw
        assertDoesNotThrow(() -> manager.clearCache());
    }

    @Test
    @DisplayName("Should handle updateCacheAfterPlacement with disabled cache")
    void testUpdateCacheDisabled() {
        Board board = new Board(3, 3);
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager manager = new DomainManager(fitChecker);

        manager.setUseDomainCache(false);

        Map<Integer, Piece> pieces = new HashMap<>();
        BitSet pieceUsed = new BitSet();

        // Should not throw
        assertDoesNotThrow(() -> manager.updateCacheAfterPlacement(board, 0, 0, pieces, pieceUsed, 0));
    }

    // ==================== Collection Null Safety ====================

    @Test
    @DisplayName("Should handle empty list from computeDomain")
    void testEmptyListFromComputeDomain() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>();
        BitSet pieceUsed = new BitSet();

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> false; // Nothing fits
        DomainManager manager = new DomainManager(fitChecker);

        List<DomainManager.ValidPlacement> domain = manager.computeDomain(board, 0, 0, pieces, pieceUsed, 0);

        assertNotNull(domain);
        assertTrue(domain.isEmpty());
    }

    @Test
    @DisplayName("Should handle domain map with empty lists")
    void testDomainMapWithEmptyLists() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{1, 1, 1, 1})); // Won't fit on borders

        BitSet pieceUsed = new BitSet();

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> {
            // Strict border checking
            int rows = b.getRows();
            int cols = b.getCols();
            if (r == 0 && edges[0] != 0) return false;
            if (r == rows - 1 && edges[2] != 0) return false;
            if (c == 0 && edges[3] != 0) return false;
            if (c == cols - 1 && edges[1] != 0) return false;
            return true;
        };

        DomainManager manager = new DomainManager(fitChecker);
        manager.initializeAC3Domains(board, pieces, pieceUsed, 1);

        Map<Integer, List<DomainManager.ValidPlacement>> domain = manager.getDomain(0, 0);
        assertNotNull(domain);
    }
}
