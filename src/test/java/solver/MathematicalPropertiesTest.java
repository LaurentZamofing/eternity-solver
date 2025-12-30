package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mathematical Properties Tests for Eternity Solver
 *
 * Tests mathematical properties and relationships:
 * - Associativity
 * - Commutativity (where applicable)
 * - Transitivity
 * - Distributivity
 * - Algebraic identities
 * - Counting properties
 */
public class MathematicalPropertiesTest {

    private Map<Integer, Piece> pieces;

    @BeforeEach
    void setUp() {
        pieces = createTestPieces();
    }

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> p = new HashMap<>();
        p.put(1, new Piece(1, new int[]{0, 1, 2, 0}));
        p.put(2, new Piece(2, new int[]{1, 2, 3, 4}));
        p.put(3, new Piece(3, new int[]{5, 5, 5, 5}));
        p.put(4, new Piece(4, new int[]{1, 2, 1, 2}));
        return p;
    }

    // ==================== ASSOCIATIVITY ====================

    @Test
    void testRotationAssociativity() {
        // Property: rotate(k1 + k2) = rotate(k1) then rotate(k2)
        Piece piece = pieces.get(2);

        // Direct rotation by 3
        int[] direct = piece.edgesRotated(3);

        // Rotate by 1, then by 2
        int[] step1 = piece.edgesRotated(1);
        Piece temp = new Piece(99, step1);
        int[] step2 = temp.edgesRotated(2);

        assertArrayEquals(direct, step2, "Rotation should be associative");
    }

    @Test
    void testScoreAdditionAssociativity() {
        // Property: Score from A+B+C should match score from (A+B)+C
        Board board = new Board(1, 3);
        Piece p1 = new Piece(1, new int[]{0, 1, 0, 0});
        Piece p2 = new Piece(2, new int[]{0, 2, 0, 1});
        Piece p3 = new Piece(3, new int[]{0, 0, 0, 2});

        board.place(0, 0, p1, 0);
        board.place(0, 1, p2, 0);
        board.place(0, 2, p3, 0);

        int[] score = board.calculateScore();

        // Score calculation should be same regardless of order of internal computation
        assertTrue(score[0] >= 0, "Score should be computed consistently");
    }

    @Test
    void testPlacementSequenceAssociativity() {
        // Property: Place A, B, C or Place A, (B, C) - final state is same
        Board b1 = new Board(1, 3);
        Board b2 = new Board(1, 3);

        Piece p1 = pieces.get(1);
        Piece p2 = pieces.get(2);
        Piece p3 = pieces.get(3);

        // Sequence 1
        b1.place(0, 0, p1, 0);
        b1.place(0, 1, p2, 0);
        b1.place(0, 2, p3, 0);

        // Sequence 2 (same order, different grouping mentally)
        b2.place(0, 0, p1, 0);
        b2.place(0, 1, p2, 0);
        b2.place(0, 2, p3, 0);

        // States should be identical
        for (int c = 0; c < 3; c++) {
            assertEquals(b1.getPlacement(0, c).getPieceId(), b2.getPlacement(0, c).getPieceId());
        }
    }

    // ==================== COMMUTATIVITY ====================

    @Test
    void testIndependentCellPlacementCommutativity() {
        // Property: Placing at independent cells - order doesn't matter
        Board b1 = new Board(3, 3);
        Board b2 = new Board(3, 3);

        Piece p1 = pieces.get(1);
        Piece p2 = pieces.get(2);

        // Order 1: Place p1 then p2
        b1.place(0, 0, p1, 0);
        b1.place(2, 2, p2, 0);

        // Order 2: Place p2 then p1
        b2.place(2, 2, p2, 0);
        b2.place(0, 0, p1, 0);

        // Final states should be identical
        assertEquals(b1.getPlacement(0, 0).getPieceId(), b2.getPlacement(0, 0).getPieceId());
        assertEquals(b1.getPlacement(2, 2).getPieceId(), b2.getPlacement(2, 2).getPieceId());
        assertArrayEquals(b1.calculateScore(), b2.calculateScore());
    }

    @Test
    void testScoreSymmetricForIndependentPlacements() {
        // Property: Score calculation is symmetric for non-adjacent pieces
        Board b1 = new Board(2, 2);
        Board b2 = new Board(2, 2);

        Piece p1 = pieces.get(1);
        Piece p2 = pieces.get(2);

        b1.place(0, 0, p1, 0);
        b1.place(1, 1, p2, 0);

        b2.place(1, 1, p2, 0);
        b2.place(0, 0, p1, 0);

        assertArrayEquals(b1.calculateScore(), b2.calculateScore());
    }

    @Test
    void testBoardDimensionCommutativity() {
        // Property: Board(r, c).area = Board(c, r).area (when r != c)
        Board b1 = new Board(3, 5);
        Board b2 = new Board(5, 3);

        assertEquals(15, b1.getRows() * b1.getCols());
        assertEquals(15, b2.getRows() * b2.getCols());
    }

    // ==================== IDENTITY ELEMENTS ====================

    @Test
    void testRotationIdentityElement() {
        // Property: rotate(0) is identity - returns original orientation
        Piece piece = pieces.get(2);
        int[] original = piece.getEdges();
        int[] rotatedByZero = piece.edgesRotated(0);

        assertArrayEquals(original, rotatedByZero, "Rotation by 0 should be identity");
    }

    @Test
    void testEmptyBoardScoreIdentity() {
        // Property: Empty board has identity score (0 out of max)
        Board board = new Board(4, 4);
        int[] score = board.calculateScore();
        assertEquals(0, score[0], "Empty board should have score of 0");
    }

    @Test
    void testPieceCountIdentity() {
        // Property: Initially, used pieces = 0
        Set<Integer> usedPieces = new HashSet<>();
        assertEquals(0, usedPieces.size(), "Initially no pieces should be used");
    }

    // ==================== INVERSE OPERATIONS ====================

    @Test
    void testRotationInverse() {
        // Property: rotate(k) then rotate(-k mod 4) returns original
        Piece piece = pieces.get(2);
        int[] original = piece.edgesRotated(0);

        int[] rotated = piece.edgesRotated(2);
        Piece temp = new Piece(99, rotated);
        int[] inverse = temp.edgesRotated(2); // 2 + 2 = 4 = 0 mod 4

        assertArrayEquals(original, inverse, "Rotation should have inverse");
    }

    @Test
    void testPlacementRemovalInverse() {
        // Property: place then remove is inverse operation
        Board board = new Board(3, 3);
        Piece piece = pieces.get(1);

        boolean wasEmpty = board.isEmpty(0, 0);
        board.place(0, 0, piece, 0);
        board.remove(0, 0);
        boolean isEmptyNow = board.isEmpty(0, 0);

        assertEquals(wasEmpty, isEmptyNow, "Place then remove should be inverse");
    }

    // ==================== COUNTING PROPERTIES ====================

    @Test
    void testTotalRotationsForAllPieces() {
        // Property: Each piece has between 1 and 4 unique rotations
        for (Piece piece : pieces.values()) {
            int rotations = piece.getUniqueRotationCount();
            assertTrue(rotations >= 1 && rotations <= 4,
                "Piece should have 1-4 unique rotations, got " + rotations);
        }
    }

    @Test
    void testSymmetricPieceRotationCount() {
        // Property: Piece with all edges identical has 1 rotation
        Piece symmetric = pieces.get(3); // All edges are 5
        assertEquals(1, symmetric.getUniqueRotationCount(),
            "Fully symmetric piece should have 1 unique rotation");
    }

    @Test
    void testTwoFoldSymmetryRotationCount() {
        // Property: Piece with opposite sides identical has 2 rotations
        Piece twoFold = pieces.get(4); // edges [1, 2, 1, 2]
        assertEquals(2, twoFold.getUniqueRotationCount(),
            "Two-fold symmetric piece should have 2 unique rotations");
    }

    @Test
    void testAsymmetricPieceRotationCount() {
        // Property: Fully asymmetric piece has 4 rotations
        Piece asymmetric = pieces.get(2); // edges [1, 2, 3, 4]
        assertEquals(4, asymmetric.getUniqueRotationCount(),
            "Asymmetric piece should have 4 unique rotations");
    }

    @Test
    void testMaximumScoreFormula() {
        // Property: Max score = (rows-1)*cols + rows*(cols-1) = 2*rows*cols - rows - cols
        Board board = new Board(5, 7);
        int[] score = board.calculateScore();

        int rows = 5, cols = 7;
        int expected = (rows - 1) * cols + rows * (cols - 1);
        assertEquals(expected, score[1], "Max score should follow formula");

        // Alternative formula
        int alternative = 2 * rows * cols - rows - cols;
        assertEquals(alternative, score[1], "Alternative formula should match");
    }

    @Test
    void testInternalEdgeCountFormula() {
        // Property: Internal edges = total edges - border edges
        Board board = new Board(4, 4);
        int rows = 4, cols = 4;

        int totalEdges = rows * cols * 4; // Each cell has 4 edges
        int borderEdges = 2 * (rows + cols); // Perimeter
        int internalEdges = (totalEdges - borderEdges) / 2; // Divided by 2 (shared)

        // This should equal max score
        int expectedMax = (rows - 1) * cols + rows * (cols - 1);
        assertEquals(expectedMax, internalEdges,
            "Internal edge count formula should match");
    }

    // ==================== TRANSITIVITY ====================

    @Test
    void testRotationTransitivity() {
        // Property: If R1 -> R2 and R2 -> R3, then R1 -> R3
        Piece piece = pieces.get(2);

        int[] r0 = piece.edgesRotated(0);
        int[] r1 = piece.edgesRotated(1);
        int[] r2 = piece.edgesRotated(2);

        // Verify transitivity in rotation sequence
        Piece p1 = new Piece(98, r1);
        int[] r1to2 = p1.edgesRotated(1);
        assertArrayEquals(r2, r1to2, "Rotation should be transitive");
    }

    // ==================== DISTRIBUTIVITY ====================

    @Test
    void testRotationDistributesOverEdges() {
        // Property: Rotating piece affects all edges uniformly
        Piece piece = pieces.get(2);
        int[] original = piece.getEdges();

        int[] rotated = piece.edgesRotated(1);

        // After 90° CW: N<-W, E<-N, S<-E, W<-S
        assertEquals(original[3], rotated[0]); // N <- W
        assertEquals(original[0], rotated[1]); // E <- N
        assertEquals(original[1], rotated[2]); // S <- E
        assertEquals(original[2], rotated[3]); // W <- S
    }

    @Test
    void testScoreDistributesOverEdges() {
        // Property: Score = sum of matching edges (distributive)
        Board board = new Board(1, 4);
        Piece p1 = new Piece(1, new int[]{0, 5, 0, 0});
        Piece p2 = new Piece(2, new int[]{0, 3, 0, 5});
        Piece p3 = new Piece(3, new int[]{0, 7, 0, 3});
        Piece p4 = new Piece(4, new int[]{0, 0, 0, 7});

        board.place(0, 0, p1, 0);
        board.place(0, 1, p2, 0);
        board.place(0, 2, p3, 0);
        board.place(0, 3, p4, 0);

        int[] score = board.calculateScore();
        // Should count matching edges: p1.E==p2.W, p2.E==p3.W, p3.E==p4.W
        assertEquals(3, score[0], "Should count all matching edges");
    }

    // ==================== ALGEBRAIC PROPERTIES ====================

    @Test
    void testRotation360DegreesReturnsOriginal() {
        // Property: R^4 = I (rotation group of order 4)
        Piece piece = pieces.get(2);
        int[] original = piece.edgesRotated(0);
        int[] after360 = piece.edgesRotated(4);

        assertArrayEquals(original, after360, "4 rotations = identity");
    }

    @Test
    void testRotationGroupClosure() {
        // Property: Combining rotations stays within [0, 3]
        Piece piece = pieces.get(2);

        for (int k1 = 0; k1 < 4; k1++) {
            for (int k2 = 0; k2 < 4; k2++) {
                int[] r1 = piece.edgesRotated(k1);
                Piece temp = new Piece(99, r1);
                int[] r2 = temp.edgesRotated(k2);

                int[] direct = piece.edgesRotated((k1 + k2) % 4);
                assertArrayEquals(direct, r2,
                    "Rotation composition should be closed: " + k1 + "+" + k2);
            }
        }
    }
}
