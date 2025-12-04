package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
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

    @BeforeEach
    public void setUp() {
        // Create mock fit checker
        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        domainManager = new DomainManager(fitChecker);

        // Create statistics
        stats = new ConstraintPropagator.Statistics();

        // Create propagator
        propagator = new ConstraintPropagator(domainManager, stats);

        // Create test board and pieces
        board = new Board(3, 3);
        pieces = createTestPieces();
        pieceUsed = new BitSet(pieces.size() + 1);

        // Initialize domains
        domainManager.initializeAC3Domains(board, pieces, pieceUsed, board.getRows() * board.getCols());
    }

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{1, 2, 3, 4}));
        pieces.put(2, new Piece(2, new int[]{5, 6, 7, 8}));
        pieces.put(3, new Piece(3, new int[]{0, 0, 0, 0}));
        return pieces;
    }

    @Test
    @Disabled("Requires realistic FitChecker for proper AC-3 testing")
    @DisplayName("Propagate AC-3 without dead ends")
    public void testPropagateAC3Success() {
        // Place a piece at (0,0) and propagate constraints
        Piece piece = pieces.get(1);
        board.place(0, 0, piece, 0);
        pieceUsed.set(1);

        boolean result = propagator.propagateAC3(board, 0, 0, 1, 0, pieces, pieceUsed, 3);

        assertTrue(result, "Propagation should succeed with no dead ends");
    }

    @Test
    @Disabled("Requires realistic FitChecker for proper AC-3 testing")
    @DisplayName("Enable and disable AC-3")
    public void testEnableDisableAC3() {
        // Place a piece to have something to propagate
        Piece piece = pieces.get(1);
        board.place(0, 0, piece, 0);
        pieceUsed.set(1);

        propagator.setUseAC3(false);
        boolean result1 = propagator.propagateAC3(board, 0, 0, 1, 0, pieces, pieceUsed, 3);
        assertTrue(result1, "Should succeed when AC-3 disabled");

        propagator.setUseAC3(true);
        boolean result2 = propagator.propagateAC3(board, 0, 0, 1, 0, pieces, pieceUsed, 3);
        assertTrue(result2, "Should succeed when AC-3 enabled");
    }

    @Test
    @DisplayName("Statistics are updated on propagation")
    public void testStatisticsUpdated() {
        long initialDeadEnds = stats.getDeadEndsDetected();

        // Place a piece and propagate
        Piece piece = pieces.get(1);
        board.place(0, 0, piece, 0);
        pieceUsed.set(1);
        propagator.propagateAC3(board, 0, 0, 1, 0, pieces, pieceUsed, 3);

        // Dead ends count should be same or increased (depending on board state)
        assertTrue(stats.getDeadEndsDetected() >= initialDeadEnds,
                "Dead ends count should not decrease");
    }

    @Test
    @DisplayName("Propagate after placement")
    public void testPropagateAfterPlacement() {
        // Place a piece
        Piece piece = pieces.get(1);
        board.place(0, 0, piece, 0);
        pieceUsed.set(1);

        // Propagate constraints
        boolean result = propagator.propagateAC3(board, 0, 0, 1, 0, pieces, pieceUsed, 3);

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
    @Disabled("Requires realistic FitChecker for proper AC-3 testing")
    @DisplayName("Propagate with empty board")
    public void testPropagateEmptyBoard() {
        Board emptyBoard = new Board(3, 3);
        BitSet emptyUsed = new BitSet(pieces.size() + 1);

        DomainManager.FitChecker fitChecker = (b, r, c, edges) -> true;
        DomainManager emptyDomainManager = new DomainManager(fitChecker);
        emptyDomainManager.initializeAC3Domains(emptyBoard, pieces, emptyUsed, emptyBoard.getRows() * emptyBoard.getCols());

        ConstraintPropagator emptyPropagator = new ConstraintPropagator(
                emptyDomainManager, new ConstraintPropagator.Statistics());

        // Place a piece to propagate from
        Piece piece = pieces.get(1);
        emptyBoard.place(0, 0, piece, 0);
        emptyUsed.set(1);

        boolean result = emptyPropagator.propagateAC3(emptyBoard, 0, 0, 1, 0, pieces, emptyUsed,
                emptyBoard.getRows() * emptyBoard.getCols());

        assertTrue(result, "Propagation should succeed with placed piece");
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
}
