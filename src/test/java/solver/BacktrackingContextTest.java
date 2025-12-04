package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for BacktrackingContext.
 * Tests context creation, depth calculation, and available piece counting.
 */
@DisplayName("BacktrackingContext Tests")
public class BacktrackingContextTest {

    private Board board;
    private Map<Integer, Piece> piecesById;
    private BitSet pieceUsed;
    private StatisticsManager stats;

    @BeforeEach
    void setUp() {
        board = new Board(3, 3);
        piecesById = createTestPieces(9);
        pieceUsed = new BitSet(10); // For pieces 1-9
        stats = new StatisticsManager();
    }

    private Map<Integer, Piece> createTestPieces(int count) {
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= count; i++) {
            pieces.put(i, new Piece(i, new int[]{i, i+1, i+2, i+3}));
        }
        return pieces;
    }

    @Test
    @DisplayName("Constructor sets all fields correctly")
    void testConstructor() {
        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 0
        );

        assertNotNull(context.board);
        assertNotNull(context.piecesById);
        assertNotNull(context.pieceUsed);
        assertEquals(9, context.totalPieces);
        assertNotNull(context.stats);
        assertEquals(0, context.numFixedPieces);
    }

    @Test
    @DisplayName("Constructor with fixed pieces")
    void testConstructorWithFixedPieces() {
        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 3
        );

        assertEquals(3, context.numFixedPieces);
    }

    @Test
    @DisplayName("getCurrentDepth returns 0 when no pieces placed")
    void testGetCurrentDepthEmpty() {
        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 0
        );

        assertEquals(0, context.getCurrentDepth());
    }

    @Test
    @DisplayName("getCurrentDepth counts placed pieces correctly")
    void testGetCurrentDepthWithPieces() {
        pieceUsed.set(1);
        pieceUsed.set(3);
        pieceUsed.set(5);

        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 0
        );

        assertEquals(3, context.getCurrentDepth());
    }

    @Test
    @DisplayName("getCurrentDepth subtracts fixed pieces")
    void testGetCurrentDepthWithFixedPieces() {
        pieceUsed.set(1);
        pieceUsed.set(2);
        pieceUsed.set(3);
        pieceUsed.set(4);
        pieceUsed.set(5);

        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 2
        );

        // 5 pieces placed - 2 fixed = depth 3
        assertEquals(3, context.getCurrentDepth());
    }

    @Test
    @DisplayName("getCurrentDepth with all pieces placed")
    void testGetCurrentDepthAllPlaced() {
        for (int i = 1; i <= 9; i++) {
            pieceUsed.set(i);
        }

        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 0
        );

        assertEquals(9, context.getCurrentDepth());
    }

    @Test
    @DisplayName("countAvailablePieces returns total when none placed")
    void testCountAvailablePiecesNonePlaced() {
        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 0
        );

        assertEquals(9, context.countAvailablePieces());
    }

    @Test
    @DisplayName("countAvailablePieces decreases as pieces are placed")
    void testCountAvailablePiecesWithPiecesPlaced() {
        pieceUsed.set(1);
        pieceUsed.set(2);
        pieceUsed.set(3);

        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 0
        );

        assertEquals(6, context.countAvailablePieces());
    }

    @Test
    @DisplayName("countAvailablePieces returns 0 when all placed")
    void testCountAvailablePiecesAllPlaced() {
        for (int i = 1; i <= 9; i++) {
            pieceUsed.set(i);
        }

        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 0
        );

        assertEquals(0, context.countAvailablePieces());
    }

    @Test
    @DisplayName("countAvailablePieces with exactly one piece remaining")
    void testCountAvailablePiecesOneRemaining() {
        for (int i = 1; i <= 8; i++) {
            pieceUsed.set(i);
        }

        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 0
        );

        assertEquals(1, context.countAvailablePieces());
    }

    @Test
    @DisplayName("Context with large puzzle")
    void testLargePuzzle() {
        Board largeBoard = new Board(16, 16);
        Map<Integer, Piece> largePieces = createTestPieces(256);
        BitSet largePieceUsed = new BitSet(257);

        BacktrackingContext context = new BacktrackingContext(
            largeBoard, largePieces, largePieceUsed, 256, stats, 0
        );

        assertEquals(256, context.totalPieces);
        assertEquals(0, context.getCurrentDepth());
        assertEquals(256, context.countAvailablePieces());
    }

    @Test
    @DisplayName("Context maintains independent state")
    void testIndependentContexts() {
        BitSet pieceUsed1 = new BitSet(10);
        BitSet pieceUsed2 = new BitSet(10);

        pieceUsed1.set(1);
        pieceUsed1.set(2);
        pieceUsed2.set(3);
        pieceUsed2.set(4);
        pieceUsed2.set(5);

        BacktrackingContext context1 = new BacktrackingContext(
            board, piecesById, pieceUsed1, 9, stats, 0
        );
        BacktrackingContext context2 = new BacktrackingContext(
            board, piecesById, pieceUsed2, 9, stats, 0
        );

        assertEquals(2, context1.getCurrentDepth());
        assertEquals(3, context2.getCurrentDepth());
        assertEquals(7, context1.countAvailablePieces());
        assertEquals(6, context2.countAvailablePieces());
    }

    @Test
    @DisplayName("Modifying pieceUsed after context creation affects depth")
    void testModifyingPieceUsedAfterCreation() {
        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 0
        );

        assertEquals(0, context.getCurrentDepth());
        assertEquals(9, context.countAvailablePieces());

        // Modify the BitSet (context holds reference)
        pieceUsed.set(1);
        pieceUsed.set(2);

        assertEquals(2, context.getCurrentDepth());
        assertEquals(7, context.countAvailablePieces());
    }

    @Test
    @DisplayName("getCurrentDepth and countAvailablePieces sum to totalPieces minus fixed")
    void testDepthAndAvailableSum() {
        pieceUsed.set(1);
        pieceUsed.set(2);
        pieceUsed.set(3);
        pieceUsed.set(4);

        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 2
        );

        int depth = context.getCurrentDepth();
        int available = context.countAvailablePieces();

        // depth (4-2=2) + available (5) + fixed (2) = total (9)
        assertEquals(9, depth + available + context.numFixedPieces);
    }

    @Test
    @DisplayName("Context with single piece puzzle")
    void testSinglePiecePuzzle() {
        Board smallBoard = new Board(1, 1);
        Map<Integer, Piece> singlePiece = createTestPieces(1);
        BitSet singleUsed = new BitSet(2);

        BacktrackingContext context = new BacktrackingContext(
            smallBoard, singlePiece, singleUsed, 1, stats, 0
        );

        assertEquals(1, context.totalPieces);
        assertEquals(0, context.getCurrentDepth());
        assertEquals(1, context.countAvailablePieces());

        singleUsed.set(1);
        assertEquals(1, context.getCurrentDepth());
        assertEquals(0, context.countAvailablePieces());
    }

    @Test
    @DisplayName("Context with all pieces fixed")
    void testAllPiecesFixed() {
        for (int i = 1; i <= 9; i++) {
            pieceUsed.set(i);
        }

        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 9
        );

        // All 9 pieces placed, all 9 fixed -> depth = 0
        assertEquals(0, context.getCurrentDepth());
        assertEquals(0, context.countAvailablePieces());
    }

    @Test
    @DisplayName("Context fields are public and accessible")
    void testPublicFieldAccess() {
        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 2
        );

        // Verify we can access all public fields
        assertNotNull(context.board);
        assertNotNull(context.piecesById);
        assertNotNull(context.pieceUsed);
        assertTrue(context.totalPieces > 0);
        assertNotNull(context.stats);
        assertEquals(2, context.numFixedPieces);
    }

    @Test
    @DisplayName("Context with partial placement")
    void testPartialPlacement() {
        // Place pieces 1, 3, 5, 7 (4 pieces)
        pieceUsed.set(1);
        pieceUsed.set(3);
        pieceUsed.set(5);
        pieceUsed.set(7);

        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 1
        );

        assertEquals(3, context.getCurrentDepth()); // 4 - 1 fixed
        assertEquals(5, context.countAvailablePieces()); // 9 - 4 placed
    }

    @Test
    @DisplayName("Multiple calls to getCurrentDepth return consistent results")
    void testGetCurrentDepthConsistency() {
        pieceUsed.set(1);
        pieceUsed.set(2);
        pieceUsed.set(3);

        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 0
        );

        int depth1 = context.getCurrentDepth();
        int depth2 = context.getCurrentDepth();
        int depth3 = context.getCurrentDepth();

        assertEquals(depth1, depth2);
        assertEquals(depth2, depth3);
        assertEquals(3, depth1);
    }

    @Test
    @DisplayName("Multiple calls to countAvailablePieces return consistent results")
    void testCountAvailablePiecesConsistency() {
        pieceUsed.set(1);
        pieceUsed.set(2);

        BacktrackingContext context = new BacktrackingContext(
            board, piecesById, pieceUsed, 9, stats, 0
        );

        int count1 = context.countAvailablePieces();
        int count2 = context.countAvailablePieces();
        int count3 = context.countAvailablePieces();

        assertEquals(count1, count2);
        assertEquals(count2, count3);
        assertEquals(7, count1);
    }
}
