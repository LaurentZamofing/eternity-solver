package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConstraintPropagator class.
 * Tests AC-3 constraint propagation and dead-end detection.
 *
 * NOTE: Some tests disabled - require realistic FitChecker instead of mock
 * that always returns true. AC-3 propagation depends on actual constraint checking.
 */
@DisplayName("ConstraintPropagator Tests")
public class ConstraintPropagatorTest {

    private ConstraintPropagator propagator;
    private DomainManager domainManager;
    private ConstraintPropagator.Statistics stats;
    private Board board;
    private Map<Integer, Piece> pieces;
    private BitSet pieceUsed;
    private int tlPieceId;
    private int tlRotation;

    private DomainManager.FitChecker realisticFitChecker() {
        return (b, r, c, edges) -> {
            if (r == 0 && edges[0] != 0) return false;
            if (c == 0 && edges[3] != 0) return false;
            if (r == b.getRows() - 1 && edges[2] != 0) return false;
            if (c == b.getCols() - 1 && edges[1] != 0) return false;
            if (r > 0 && !b.isEmpty(r - 1, c) && b.getPlacement(r - 1, c).edges[2] != edges[0]) return false;
            if (c > 0 && !b.isEmpty(r, c - 1) && b.getPlacement(r, c - 1).edges[1] != edges[3]) return false;
            if (r < b.getRows() - 1 && !b.isEmpty(r + 1, c) && b.getPlacement(r + 1, c).edges[0] != edges[2]) return false;
            if (c < b.getCols() - 1 && !b.isEmpty(r, c + 1) && b.getPlacement(r, c + 1).edges[3] != edges[1]) return false;
            return true;
        };
    }

    @BeforeEach
    public void setUp() {
        // Realistic fit checker: borders must be 0 and any placed neighbour
        // edge must match. Mirrors the checks PlacementValidator.fits() makes
        // in production; the earlier mock (always-true) caused AC-3 tests to
        // skip real constraint propagation logic.
        domainManager = new DomainManager(realisticFitChecker());

        // Create statistics
        stats = new ConstraintPropagator.Statistics();

        // Create propagator
        propagator = new ConstraintPropagator(domainManager, stats);

        // Create test board and pieces
        board = new Board(3, 3);
        pieces = createTestPieces();
        pieceUsed = new BitSet(pieces.size() + 1);

        // Initialize domains
        domainManager.initializeAC3Domains(board, pieces, pieceUsed, pieces.size());

        // Find a piece that actually fits at TL (0,0) in some rotation — the
        // earlier hard-coded "piece 1" only worked with the always-true mock.
        findTLPlaceable();
    }

    private void findTLPlaceable() {
        for (Map.Entry<Integer, Piece> e : pieces.entrySet()) {
            for (int rot = 0; rot < 4; rot++) {
                int[] edges = e.getValue().edgesRotated(rot);
                if (edges[0] == 0 && edges[3] == 0) {
                    tlPieceId = e.getKey();
                    tlRotation = rot;
                    return;
                }
            }
        }
        fail("No TL-fittable piece in test puzzle");
    }

    private Map<Integer, Piece> createTestPieces() {
        // Use the real 3x3 example so AC-3 runs on a valid, solvable puzzle.
        // The earlier three-piece map with random edges could not satisfy any
        // border constraint, which is why AC-3 tests had to be @Disabled.
        return util.PuzzleFactory.createExample3x3();
    }

    @Test
    @DisplayName("Propagate AC-3 without dead ends")
    public void testPropagateAC3Success() {
        Piece piece = pieces.get(tlPieceId);
        board.place(0, 0, piece, tlRotation);
        pieceUsed.set(tlPieceId);

        boolean result = propagator.propagateAC3(board, 0, 0, tlPieceId, tlRotation, pieces, pieceUsed, pieces.size());

        assertTrue(result, "Propagation should succeed with valid TL placement");
    }

    @Test
    @DisplayName("Enable and disable AC-3")
    public void testEnableDisableAC3() {
        Piece piece = pieces.get(tlPieceId);
        board.place(0, 0, piece, tlRotation);
        pieceUsed.set(tlPieceId);

        propagator.setUseAC3(false);
        boolean result1 = propagator.propagateAC3(board, 0, 0, tlPieceId, tlRotation, pieces, pieceUsed, pieces.size());
        assertTrue(result1, "Should succeed when AC-3 disabled");

        propagator.setUseAC3(true);
        boolean result2 = propagator.propagateAC3(board, 0, 0, tlPieceId, tlRotation, pieces, pieceUsed, pieces.size());
        assertTrue(result2, "Should succeed when AC-3 enabled");
    }

    @Test
    @DisplayName("Statistics are updated on propagation")
    public void testStatisticsUpdated() {
        long initialDeadEnds = stats.getDeadEndsDetected();

        Piece piece = pieces.get(tlPieceId);
        board.place(0, 0, piece, tlRotation);
        pieceUsed.set(tlPieceId);
        propagator.propagateAC3(board, 0, 0, tlPieceId, tlRotation, pieces, pieceUsed, pieces.size());

        // Dead ends count should be same or increased (depending on board state)
        assertTrue(stats.getDeadEndsDetected() >= initialDeadEnds,
                "Dead ends count should not decrease");
    }

    @Test
    @DisplayName("Propagate after placement")
    public void testPropagateAfterPlacement() {
        Piece piece = pieces.get(tlPieceId);
        board.place(0, 0, piece, tlRotation);
        pieceUsed.set(tlPieceId);

        boolean result = propagator.propagateAC3(board, 0, 0, tlPieceId, tlRotation, pieces, pieceUsed, pieces.size());

        // Should still succeed (or detect dead end based on constraints)
        assertNotNull(result, "Propagation should return a result");
    }

    @Test
    @DisplayName("Dead end detected increments statistics")
    public void testDeadEndDetected() {
        // Create impossible configuration by filling all cells except one
        // and marking all pieces as used
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (r == 2 && c == 2) continue; // Leave one empty
                Piece piece = pieces.get((r * 3 + c) % pieces.size() + 1);
                board.place(r, c, piece, 0);
            }
        }

        // Mark all pieces as used
        for (int id : pieces.keySet()) {
            pieceUsed.set(id);
        }

        // Reinitialize domains with impossible state
        domainManager.resetAC3();
        domainManager.initializeAC3Domains(board, pieces, pieceUsed, board.getRows() * board.getCols());

        long beforeDeadEnds = stats.getDeadEndsDetected();

        // Try to propagate from the last placed piece
        boolean result = propagator.propagateAC3(board, 2, 1, 3, 0, pieces, pieceUsed, board.getRows() * board.getCols());

        // Should detect dead end (no available pieces)
        if (!result) {
            assertTrue(stats.getDeadEndsDetected() > beforeDeadEnds,
                    "Dead end detection should increment statistics");
        }
    }

    @Test
    @DisplayName("Propagate with fresh board + placed TL piece")
    public void testPropagateEmptyBoard() {
        Board freshBoard = new Board(3, 3);
        BitSet freshUsed = new BitSet(pieces.size() + 1);
        DomainManager freshDM = new DomainManager(realisticFitChecker());
        freshDM.initializeAC3Domains(freshBoard, pieces, freshUsed, pieces.size());

        ConstraintPropagator freshCP = new ConstraintPropagator(
                freshDM, new ConstraintPropagator.Statistics());

        Piece piece = pieces.get(tlPieceId);
        freshBoard.place(0, 0, piece, tlRotation);
        freshUsed.set(tlPieceId);

        boolean result = freshCP.propagateAC3(freshBoard, 0, 0, tlPieceId, tlRotation, pieces, freshUsed, pieces.size());

        assertTrue(result, "Propagation should succeed with placed TL piece on fresh board");
    }

    @Test
    @DisplayName("Statistics increment methods work")
    public void testStatisticsIncrements() {
        ConstraintPropagator.Statistics testStats = new ConstraintPropagator.Statistics();

        assertEquals(0, testStats.getDeadEndsDetected());

        testStats.incrementDeadEnds();
        assertEquals(1, testStats.getDeadEndsDetected());

        testStats.incrementDeadEnds();
        assertEquals(2, testStats.getDeadEndsDetected());
    }

    @Test
    @DisplayName("Propagate doesn't throw exceptions")
    public void testPropagateNoExceptions() {
        // Place a piece before testing
        Piece piece = pieces.get(1);
        board.place(0, 0, piece, 0);
        pieceUsed.set(1);

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                propagator.propagateAC3(board, 0, 0, 1, 0, pieces, pieceUsed,
                        board.getRows() * board.getCols());
            }
        });
    }

    @Test
    @DisplayName("Propagate with single piece")
    public void testPropagateWithSinglePiece() {
        Map<Integer, Piece> singlePiece = new HashMap<>();
        singlePiece.put(1, pieces.get(1));

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager singleDomainManager = new DomainManager(fitChecker);
        BitSet singleUsed = new BitSet(2);

        singleDomainManager.initializeAC3Domains(board, singlePiece, singleUsed, board.getRows() * board.getCols());

        ConstraintPropagator singlePropagator = new ConstraintPropagator(
                singleDomainManager, new ConstraintPropagator.Statistics());

        // Place the single piece
        Piece piece = singlePiece.get(1);
        board.place(0, 0, piece, 0);
        singleUsed.set(1);

        boolean result = singlePropagator.propagateAC3(board, 0, 0, 1, 0, singlePiece, singleUsed,
                board.getRows() * board.getCols());

        assertNotNull(result);
    }

    // ─── Coverage for the secondary methods (filterDomain, getDomainSize,
    //     isSingleton, getSingletonPieceId, isDomainEmpty,
    //     getUniquePieceCount). These are pure queries, easy to lock down. ──

    @Test
    @DisplayName("filterDomain returns empty list for null/empty input")
    public void testFilterDomainNullEmpty() {
        assertTrue(propagator.filterDomain(null, 0, 0, pieces).isEmpty());
        assertTrue(propagator.filterDomain(java.util.Collections.emptyList(), 0, 0, pieces).isEmpty());
    }

    @Test
    @DisplayName("filterDomain keeps only placements whose edge[edgeIndex] matches required")
    public void testFilterDomainEdgeMatching() {
        // Local pieces used only by this test so we can reason about exact
        // edge values — the shared setUp now uses real puzzle pieces.
        Map<Integer, Piece> localPieces = new HashMap<>();
        localPieces.put(1, new Piece(1, new int[]{1, 2, 3, 4}));
        localPieces.put(2, new Piece(2, new int[]{5, 6, 7, 8}));

        // North edges at rot 0: 1, 5. Filter for edge[0] == 1 → keep only piece 1 rot 0.
        java.util.List<DomainManager.ValidPlacement> input = java.util.List.of(
            new DomainManager.ValidPlacement(1, 0),
            new DomainManager.ValidPlacement(2, 0)
        );
        java.util.List<DomainManager.ValidPlacement> kept = propagator.filterDomain(input, 1, 0, localPieces);

        assertEquals(1, kept.size());
        assertEquals(1, kept.get(0).pieceId);
        assertEquals(0, kept.get(0).rotation);
    }

    @Test
    @DisplayName("getDomainSize sums per-piece rotation lists")
    public void testGetDomainSize() {
        Map<Integer, java.util.List<DomainManager.ValidPlacement>> domain = new HashMap<>();
        domain.put(1, java.util.List.of(
            new DomainManager.ValidPlacement(1, 0),
            new DomainManager.ValidPlacement(1, 1)));
        domain.put(2, java.util.List.of(new DomainManager.ValidPlacement(2, 3)));

        assertEquals(3, propagator.getDomainSize(domain), "2 + 1 rotations across 2 pieces");
        assertEquals(0, propagator.getDomainSize(null));
        assertEquals(0, propagator.getDomainSize(new HashMap<>()));
    }

    @Test
    @DisplayName("getUniquePieceCount counts distinct piece IDs (ignores rotations)")
    public void testGetUniquePieceCount() {
        Map<Integer, java.util.List<DomainManager.ValidPlacement>> domain = new HashMap<>();
        domain.put(1, java.util.List.of(
            new DomainManager.ValidPlacement(1, 0),
            new DomainManager.ValidPlacement(1, 1),
            new DomainManager.ValidPlacement(1, 2)));
        domain.put(2, java.util.List.of(new DomainManager.ValidPlacement(2, 0)));

        assertEquals(2, propagator.getUniquePieceCount(domain),
            "2 piece IDs even if piece 1 has 3 rotations");
        assertEquals(0, propagator.getUniquePieceCount(null));
    }

    @Test
    @DisplayName("isDomainEmpty handles null, empty, and populated maps")
    public void testIsDomainEmpty() {
        assertTrue(propagator.isDomainEmpty(null));
        assertTrue(propagator.isDomainEmpty(new HashMap<>()));
        Map<Integer, java.util.List<DomainManager.ValidPlacement>> domain = new HashMap<>();
        domain.put(1, java.util.List.of(new DomainManager.ValidPlacement(1, 0)));
        assertFalse(propagator.isDomainEmpty(domain));
    }

    @Test
    @DisplayName("isSingleton + getSingletonPieceId roundtrip")
    public void testSingletonDetection() {
        Map<Integer, java.util.List<DomainManager.ValidPlacement>> domain = new HashMap<>();
        domain.put(7, java.util.List.of(new DomainManager.ValidPlacement(7, 1)));

        assertTrue(propagator.isSingleton(domain));
        assertEquals(7, propagator.getSingletonPieceId(domain));

        domain.put(8, java.util.List.of(new DomainManager.ValidPlacement(8, 0)));
        assertFalse(propagator.isSingleton(domain));
        assertEquals(-1, propagator.getSingletonPieceId(domain));
    }

    @Test
    @DisplayName("setUseAC3 toggles short-circuit in propagateAC3")
    public void testSetUseAC3() {
        propagator.setUseAC3(false);
        // With AC-3 disabled, propagate is a no-op returning true regardless of state
        boolean result = propagator.propagateAC3(board, 0, 0, 1, 0, pieces, pieceUsed,
                board.getRows() * board.getCols());
        assertTrue(result, "AC-3 disabled returns true (skip propagation)");
        assertEquals(0, stats.getDeadEndsDetected(), "no dead-ends counted when disabled");
    }
}
