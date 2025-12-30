package solver;

import model.Board;
import model.Piece;
import model.Placement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.PuzzleFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-Based Tests for Eternity Solver
 *
 * Tests fundamental properties that should hold true for all puzzle operations:
 * - Idempotence: Operations that should produce same results when repeated
 * - Determinism: Same inputs produce same outputs
 * - Boundary conditions: Operations at limits behave correctly
 * - Symmetry: Operations that should be symmetric
 */
public class PropertyBasedTest {

    private Board board;
    private Map<Integer, Piece> pieces;
    private EternitySolver solver;
    private PlacementValidator validator;
    private CellConstraints[][] cellConstraints;
    private EternitySolver.Statistics stats;

    @BeforeEach
    void setUp() {
        EternitySolver.resetGlobalState();
        board = new Board(4, 4);
        pieces = PuzzleFactory.createExample3x3(); // Use as test pieces
        solver = new EternitySolver();
        stats = new EternitySolver.Statistics();
        cellConstraints = CellConstraints.createConstraintsMatrix(4, 4);
        validator = new PlacementValidator(cellConstraints, stats, "ascending");
    }

    // ==================== IDEMPOTENCE PROPERTIES ====================

    @Test
    void testScoreCalculationIsIdempotent() {
        // Property: Calculating score multiple times yields same result
        Piece p1 = new Piece(1, new int[]{0, 1, 2, 0});
        board.place(0, 0, p1, 0);

        int[] score1 = board.calculateScore();
        int[] score2 = board.calculateScore();
        int[] score3 = board.calculateScore();

        assertArrayEquals(score1, score2, "First and second score calculations should match");
        assertArrayEquals(score2, score3, "Second and third score calculations should match");
    }

    @Test
    void testPieceRotationIsIdempotent() {
        // Property: Rotating a piece multiple times with same parameter gives same result
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        int[] rotation1 = piece.edgesRotated(2);
        int[] rotation2 = piece.edgesRotated(2);
        int[] rotation3 = piece.edgesRotated(2);

        assertArrayEquals(rotation1, rotation2);
        assertArrayEquals(rotation2, rotation3);
    }

    @Test
    void testIsEmptyCheckIsIdempotent() {
        // Property: Checking if cell is empty multiple times yields same result
        assertTrue(board.isEmpty(0, 0));
        assertTrue(board.isEmpty(0, 0));
        assertTrue(board.isEmpty(0, 0));

        Piece piece = new Piece(1, new int[]{0, 1, 2, 0});
        board.place(0, 0, piece, 0);

        assertFalse(board.isEmpty(0, 0));
        assertFalse(board.isEmpty(0, 0));
        assertFalse(board.isEmpty(0, 0));
    }

    @Test
    void testFitsCheckIsIdempotent() {
        // Property: Checking if piece fits multiple times yields same result
        int[] edges = {0, 1, 2, 0};

        boolean fits1 = validator.fits(board, 0, 0, edges);
        boolean fits2 = validator.fits(board, 0, 0, edges);
        boolean fits3 = validator.fits(board, 0, 0, edges);

        assertEquals(fits1, fits2);
        assertEquals(fits2, fits3);
    }

    @Test
    void testGetPlacementIsIdempotent() {
        // Property: Getting placement multiple times returns same result
        Piece piece = new Piece(1, new int[]{0, 1, 2, 0});
        board.place(0, 0, piece, 0);

        Placement p1 = board.getPlacement(0, 0);
        Placement p2 = board.getPlacement(0, 0);
        Placement p3 = board.getPlacement(0, 0);

        assertSame(p1, p2, "Should return same placement object");
        assertSame(p2, p3, "Should return same placement object");
    }

    // ==================== DETERMINISM PROPERTIES ====================

    @Test
    void testPlacementWithSameInputsProducesSameState() {
        // Property: Placing same piece with same rotation always produces same board state
        Board board1 = new Board(3, 3);
        Board board2 = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{0, 1, 2, 3});

        board1.place(0, 0, piece, 0);
        board2.place(0, 0, piece, 0);

        Placement p1 = board1.getPlacement(0, 0);
        Placement p2 = board2.getPlacement(0, 0);

        assertEquals(p1.getPieceId(), p2.getPieceId());
        assertEquals(p1.getRotation(), p2.getRotation());
        assertArrayEquals(p1.edges, p2.edges);
    }

    @Test
    void testRotationDeterminism() {
        // Property: Same rotation value always produces same edges
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        int[] r1 = piece.edgesRotated(2);
        int[] r2 = piece.edgesRotated(2);
        int[] r3 = piece.edgesRotated(2);

        assertArrayEquals(r1, r2);
        assertArrayEquals(r2, r3);
    }

    @Test
    void testScoreDeterminism() {
        // Property: Same board configuration always produces same score
        Board b1 = new Board(2, 2);
        Board b2 = new Board(2, 2);

        Piece p = new Piece(1, new int[]{0, 5, 3, 0});
        b1.place(0, 0, p, 0);
        b2.place(0, 0, p, 0);

        assertArrayEquals(b1.calculateScore(), b2.calculateScore());
    }

    @Test
    void testEdgeComputationDeterminism() {
        // Property: Computing edges after rotation is deterministic
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        Map<Integer, int[]> rotationResults = new HashMap<>();
        for (int i = 0; i < 4; i++) {
            rotationResults.put(i, piece.edgesRotated(i));
        }

        // Re-compute and verify determinism
        for (int i = 0; i < 4; i++) {
            assertArrayEquals(rotationResults.get(i), piece.edgesRotated(i),
                "Rotation " + i + " should be deterministic");
        }
    }

    @Test
    void testGetEdgesReturnsCopyNotReference() {
        // Property: getEdges returns a defensive copy, ensuring immutability
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        int[] edges1 = piece.getEdges();
        int[] edges2 = piece.getEdges();

        assertNotSame(edges1, edges2, "Should return different array instances");
        assertArrayEquals(edges1, edges2, "But arrays should have same values");

        // Modify one copy
        edges1[0] = 999;

        // Original should be unchanged
        int[] edges3 = piece.getEdges();
        assertEquals(1, edges3[0], "Original piece should be immutable");
    }

    // ==================== BOUNDARY CONDITIONS ====================

    @Test
    void testScoreOnEmptyBoard() {
        // Property: Empty board has score 0
        Board emptyBoard = new Board(5, 5);
        int[] score = emptyBoard.calculateScore();
        assertEquals(0, score[0], "Empty board should have score 0");
        assertTrue(score[1] > 0, "Max score should be positive");
    }

    @Test
    void testScoreOnFullyFilledBoard() {
        // Property: Fully filled board produces consistent score
        Board fullBoard = new Board(2, 2);
        Piece p = new Piece(1, new int[]{0, 1, 1, 0});

        fullBoard.place(0, 0, p, 0);
        fullBoard.place(0, 1, p, 0);
        fullBoard.place(1, 0, p, 0);
        fullBoard.place(1, 1, p, 0);

        int[] score = fullBoard.calculateScore();
        assertTrue(score[0] >= 0, "Score should be non-negative");
        assertTrue(score[0] <= score[1], "Score should not exceed maximum");
    }

    @Test
    void testSinglePieceBoard() {
        // Property: 1x1 board with valid piece has perfect score
        Board single = new Board(1, 1);
        Piece p = new Piece(1, new int[]{0, 0, 0, 0});
        single.place(0, 0, p, 0);

        int[] score = single.calculateScore();
        assertEquals(0, score[0], "1x1 board has no internal edges");
        assertEquals(0, score[1], "1x1 board has max score of 0");
    }

    @Test
    void testLargeBoardInitialization() {
        // Property: Large board can be initialized without errors
        Board large = new Board(16, 16);
        assertEquals(16, large.getRows());
        assertEquals(16, large.getCols());

        for (int r = 0; r < 16; r++) {
            for (int c = 0; c < 16; c++) {
                assertTrue(large.isEmpty(r, c), "All cells should start empty");
            }
        }
    }

    @Test
    void testRotationBoundaries() {
        // Property: Rotations wrap around correctly at boundaries
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        assertArrayEquals(piece.edgesRotated(0), piece.edgesRotated(4));
        assertArrayEquals(piece.edgesRotated(1), piece.edgesRotated(5));
        assertArrayEquals(piece.edgesRotated(0), piece.edgesRotated(8));
        assertArrayEquals(piece.edgesRotated(-1), piece.edgesRotated(3));
    }

    // ==================== SYMMETRY PROPERTIES ====================

    @Test
    void testRotationSymmetry360Degrees() {
        // Property: 4 consecutive 90° rotations return to original state
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        int[] original = piece.edgesRotated(0);
        int[] after360 = piece.edgesRotated(4);

        assertArrayEquals(original, after360, "360° rotation should return to original");
    }

    @Test
    void testPlacementRemovalSymmetry() {
        // Property: Placing then removing returns to original state
        assertTrue(board.isEmpty(0, 0));

        Piece piece = new Piece(1, new int[]{0, 1, 2, 0});
        board.place(0, 0, piece, 0);
        assertFalse(board.isEmpty(0, 0));

        board.remove(0, 0);
        assertTrue(board.isEmpty(0, 0), "Should return to empty state");
        assertNull(board.getPlacement(0, 0), "Placement should be null");
    }

    @Test
    void testBoardDimensionSymmetry() {
        // Property: Creating boards with swapped dimensions works symmetrically
        Board b1 = new Board(3, 5);
        Board b2 = new Board(5, 3);

        assertEquals(3, b1.getRows());
        assertEquals(5, b1.getCols());
        assertEquals(5, b2.getRows());
        assertEquals(3, b2.getCols());
    }

    @Test
    void testPieceIdSymmetry() {
        // Property: Pieces with same edges but different IDs behave identically for placement
        Piece p1 = new Piece(1, new int[]{0, 1, 2, 0});
        Piece p2 = new Piece(2, new int[]{0, 1, 2, 0});

        Board b1 = new Board(3, 3);
        Board b2 = new Board(3, 3);

        b1.place(0, 0, p1, 0);
        b2.place(0, 0, p2, 0);

        assertArrayEquals(b1.getPlacement(0, 0).edges, b2.getPlacement(0, 0).edges,
            "Pieces with same edges should have same placement edges");
    }

    @Test
    void testRotationInverseSymmetry() {
        // Property: Rotating by k then by -k returns to original
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        int[] original = piece.edgesRotated(0);

        // Rotate by 2, then by -2 (which is same as +2)
        int[] rotated = piece.edgesRotated(2);
        int[] backToOriginal = piece.edgesRotated(-2);

        assertArrayEquals(rotated, backToOriginal, "k and -k rotations should produce same result");
    }
}
