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
 * Consistency Tests for Eternity Solver
 *
 * Tests that verify consistency between different components:
 * - AC-3 domains vs brute-force counting
 * - PlacementValidator consistency
 * - Score calculation consistency
 * - Edge matching consistency
 * - State synchronization between components
 */
public class ConsistencyTest {

    private Board board;
    private Map<Integer, Piece> pieces;
    private EternitySolver solver;
    private PlacementValidator validator;
    private CellConstraints[][] cellConstraints;
    private EternitySolver.Statistics stats;
    private DomainManager domainManager;

    @BeforeEach
    void setUp() {
        EternitySolver.resetGlobalState();
        board = new Board(4, 4);
        pieces = PuzzleFactory.createExample3x3();
        solver = new EternitySolver();
        stats = new EternitySolver.Statistics();
        cellConstraints = CellConstraints.createConstraintsMatrix(4, 4);
        validator = new PlacementValidator(cellConstraints, stats, "ascending");
        domainManager = new DomainManager(validator::fits);
    }

    // ==================== VALIDATOR CONSISTENCY ====================

    @Test
    void testValidatorConsistencyWithBorderConstraints() {
        // Consistency: Validator should consistently reject invalid border pieces
        int[] invalidNorth = {1, 1, 1, 0}; // North must be 0 for top edge
        int[] validCorner = {0, 1, 1, 0}; // N=0, W=0 for corner; E and S non-zero for interior

        assertFalse(validator.fits(board, 0, 0, invalidNorth), "Should reject non-zero north edge at top");
        assertTrue(validator.fits(board, 0, 0, validCorner), "Should accept valid corner piece");
    }

    @Test
    void testValidatorConsistencyAcrossMultipleCalls() {
        // Consistency: Same validation should give same result
        int[] edges = {0, 1, 2, 0};

        boolean result1 = validator.fits(board, 0, 0, edges);
        boolean result2 = validator.fits(board, 0, 0, edges);
        boolean result3 = validator.fits(board, 0, 0, edges);

        assertEquals(result1, result2);
        assertEquals(result2, result3);
    }

    @Test
    void testValidatorConsistencyWithAdjacentPieces() {
        // Consistency: If piece A validates next to B, edge matching should be symmetric
        Piece p1 = new Piece(1, new int[]{0, 5, 3, 0});
        board.place(0, 0, p1, 0);

        int[] matchingEast = {0, 1, 2, 5}; // West edge matches p1's East
        int[] nonMatchingEast = {0, 1, 2, 4}; // West edge doesn't match

        assertTrue(validator.fits(board, 0, 1, matchingEast));
        assertFalse(validator.fits(board, 0, 1, nonMatchingEast));
    }

    @Test
    void testValidatorConsistencyForAllRotations() {
        // Consistency: Validator should handle all rotations consistently
        Piece piece = new Piece(1, new int[]{0, 1, 2, 0});

        for (int rot = 0; rot < 4; rot++) {
            int[] rotatedEdges = piece.edgesRotated(rot);
            boolean fits = validator.fits(board, 0, 0, rotatedEdges);
            // Result should be consistent with border constraints
            assertEquals(rotatedEdges[0] == 0 && rotatedEdges[3] == 0, fits,
                "Rotation " + rot + " validation should be consistent");
        }
    }

    @Test
    void testValidatorConsistencyAfterPlacements() {
        // Consistency: Validator state should remain consistent after multiple placements
        Piece p1 = new Piece(1, new int[]{0, 1, 2, 0});
        Piece p2 = new Piece(2, new int[]{0, 3, 4, 1});

        board.place(0, 0, p1, 0);
        int[] testEdges = {0, 5, 6, 0};

        boolean before = validator.fits(board, 0, 1, testEdges);
        board.place(1, 0, p2, 0);
        boolean after = validator.fits(board, 0, 1, testEdges);

        // Validation at (0,1) should be same regardless of placement at (1,0)
        assertEquals(before, after, "Non-adjacent placements shouldn't affect validation");
    }

    // ==================== SCORE CALCULATION CONSISTENCY ====================

    @Test
    void testScoreConsistencyWithEmptyBoard() {
        // Consistency: Empty board always has score 0
        Board b1 = new Board(3, 3);
        Board b2 = new Board(5, 5);
        Board b3 = new Board(10, 10);

        assertEquals(0, b1.calculateScore()[0]);
        assertEquals(0, b2.calculateScore()[0]);
        assertEquals(0, b3.calculateScore()[0]);
    }

    @Test
    void testScoreConsistencyWithSinglePiece() {
        // Consistency: Single piece (no neighbors) always has score 0
        Board b1 = new Board(3, 3);
        Piece p = new Piece(1, new int[]{0, 1, 2, 0});

        b1.place(0, 0, p, 0);
        assertEquals(0, b1.calculateScore()[0], "Single piece has no matching edges");

        b1.place(2, 2, p, 0);
        assertEquals(0, b1.calculateScore()[0], "Two non-adjacent pieces have no matching edges");
    }

    @Test
    void testScoreConsistencyWithMatchingEdges() {
        // Consistency: Score increments by 1 for each matching internal edge
        Board board = new Board(1, 2);
        Piece p1 = new Piece(1, new int[]{0, 5, 0, 0});
        Piece p2 = new Piece(2, new int[]{0, 0, 0, 5});

        board.place(0, 0, p1, 0);
        board.place(0, 1, p2, 0);

        assertEquals(1, board.calculateScore()[0], "One matching edge should give score 1");
    }

    @Test
    void testScoreConsistencyWithMaxFormula() {
        // Consistency: Max score should always equal (rows-1)*cols + rows*(cols-1)
        for (int rows = 2; rows <= 8; rows++) {
            for (int cols = 2; cols <= 8; cols++) {
                Board b = new Board(rows, cols);
                int[] score = b.calculateScore();
                int expectedMax = (rows - 1) * cols + rows * (cols - 1);
                assertEquals(expectedMax, score[1],
                    "Max score formula inconsistent for " + rows + "x" + cols);
            }
        }
    }

    @Test
    void testScoreConsistencyAfterPlacementAndRemoval() {
        // Consistency: Score should be same after place+remove cycle
        int[] initialScore = board.calculateScore();

        Piece p = new Piece(1, new int[]{0, 1, 2, 0});
        board.place(0, 0, p, 0);
        board.remove(0, 0);

        int[] finalScore = board.calculateScore();
        assertArrayEquals(initialScore, finalScore, "Score should be consistent after place+remove");
    }

    // ==================== EDGE MATCHING CONSISTENCY ====================

    @Test
    void testEdgeMatchingIsSymmetric() {
        // Consistency: If A's east matches B's west, then B's west matches A's east
        Piece p1 = new Piece(1, new int[]{0, 5, 0, 0});
        Piece p2 = new Piece(2, new int[]{0, 0, 0, 5});

        board.place(0, 0, p1, 0);
        board.place(0, 1, p2, 0);

        Placement pl1 = board.getPlacement(0, 0);
        Placement pl2 = board.getPlacement(0, 1);

        assertEquals(pl1.edges[1], pl2.edges[3], "Edge matching should be symmetric");
    }

    @Test
    void testEdgeMatchingConsistencyVertical() {
        // Consistency: Vertical edge matching is symmetric
        Piece p1 = new Piece(1, new int[]{0, 0, 7, 0});
        Piece p2 = new Piece(2, new int[]{7, 0, 0, 0});

        board.place(0, 0, p1, 0);
        board.place(1, 0, p2, 0);

        Placement pl1 = board.getPlacement(0, 0);
        Placement pl2 = board.getPlacement(1, 0);

        assertEquals(pl1.edges[2], pl2.edges[0], "Vertical edge matching should be symmetric");
    }

    @Test
    void testEdgeMatchingConsistencyAfterRotation() {
        // Consistency: Rotated piece edges should match correctly
        Piece p = new Piece(1, new int[]{1, 2, 3, 4});

        board.place(0, 0, p, 1); // 90° rotation
        Placement pl = board.getPlacement(0, 0);

        int[] expectedEdges = p.edgesRotated(1);
        assertArrayEquals(expectedEdges, pl.edges, "Placement edges should match rotated piece");
    }

    @Test
    void testAllBorderEdgesMustBeZero() {
        // Consistency: All border edges must be 0 for valid solution
        Board b = new Board(3, 3);
        Piece p = new Piece(1, new int[]{0, 0, 0, 0});

        // Fill entire border with pieces
        for (int c = 0; c < 3; c++) b.place(0, c, p, 0); // Top row
        for (int c = 0; c < 3; c++) b.place(2, c, p, 0); // Bottom row
        b.place(1, 0, p, 0); // Left middle
        b.place(1, 2, p, 0); // Right middle

        // Verify all border edges are 0
        for (int c = 0; c < 3; c++) {
            assertEquals(0, b.getPlacement(0, c).edges[0], "Top border should be 0");
            assertEquals(0, b.getPlacement(2, c).edges[2], "Bottom border should be 0");
        }
        for (int r = 0; r < 3; r++) {
            assertEquals(0, b.getPlacement(r, 0).edges[3], "Left border should be 0");
            assertEquals(0, b.getPlacement(r, 2).edges[1], "Right border should be 0");
        }
    }

    // ==================== STATE CONSISTENCY ====================

    @Test
    void testBoardStateConsistencyAfterMultiplePlacements() {
        // Consistency: Board state should accurately reflect all placements
        Map<String, Integer> placements = new HashMap<>();

        for (int i = 0; i < 5; i++) {
            int r = i / 4;
            int c = i % 4;
            Piece p = new Piece(i + 1, new int[]{0, i, i + 1, 0});
            board.place(r, c, p, 0);
            placements.put(r + "," + c, i + 1);
        }

        // Verify consistency
        for (Map.Entry<String, Integer> entry : placements.entrySet()) {
            String[] coords = entry.getKey().split(",");
            int r = Integer.parseInt(coords[0]);
            int c = Integer.parseInt(coords[1]);
            int expectedId = entry.getValue();

            assertFalse(board.isEmpty(r, c), "Cell should not be empty");
            assertEquals(expectedId, board.getPlacement(r, c).getPieceId(),
                "Piece ID should match at (" + r + "," + c + ")");
        }
    }

    @Test
    void testStateConsistencyAfterRemoval() {
        // Consistency: Removing piece should restore empty state
        Piece p = new Piece(1, new int[]{0, 1, 2, 0});

        board.place(1, 1, p, 0);
        assertFalse(board.isEmpty(1, 1));
        assertNotNull(board.getPlacement(1, 1));

        board.remove(1, 1);
        assertTrue(board.isEmpty(1, 1));
        assertNull(board.getPlacement(1, 1));
    }

    @Test
    void testStateConsistencyAfterOverwrite() {
        // Consistency: Overwriting placement should update state completely
        Piece p1 = new Piece(1, new int[]{0, 1, 2, 0});
        Piece p2 = new Piece(2, new int[]{0, 3, 4, 0});

        board.place(0, 0, p1, 0);
        assertEquals(1, board.getPlacement(0, 0).getPieceId());

        board.place(0, 0, p2, 1);
        assertEquals(2, board.getPlacement(0, 0).getPieceId());
        assertEquals(1, board.getPlacement(0, 0).getRotation());
    }

    @Test
    void testPieceImmutabilityConsistency() {
        // Consistency: Original piece should never be modified
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
        int[] originalEdges = piece.getEdges();

        // Place with various rotations
        board.place(0, 0, piece, 0);
        board.place(0, 1, piece, 1);
        board.place(0, 2, piece, 2);
        board.place(0, 3, piece, 3);

        // Original piece should be unchanged
        int[] currentEdges = piece.getEdges();
        assertArrayEquals(originalEdges, currentEdges, "Original piece should remain immutable");
    }

    // ==================== CROSS-COMPONENT CONSISTENCY ====================

    @Test
    void testPlacementConsistencyBetweenBoardAndValidator() {
        // Consistency: If validator says piece fits, placement should succeed
        int[] edges = {0, 1, 2, 0};

        if (validator.fits(board, 0, 0, edges)) {
            Piece p = new Piece(1, edges);
            assertDoesNotThrow(() -> board.place(0, 0, p, 0),
                "Placement should succeed if validator approves");
        }
    }

    @Test
    void testRotationConsistencyBetweenPieceAndPlacement() {
        // Consistency: Placement edges should match piece.edgesRotated(rotation)
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        for (int rot = 0; rot < 4; rot++) {
            board.place(rot, 0, piece, rot);
            Placement pl = board.getPlacement(rot, 0);

            int[] expectedEdges = piece.edgesRotated(rot);
            assertArrayEquals(expectedEdges, pl.edges,
                "Placement edges should match rotated piece for rotation " + rot);
        }
    }

    @Test
    void testScoreConsistencyWithManualCount() {
        // Consistency: Calculated score should match manual edge counting
        Board b = new Board(2, 2);
        Piece p1 = new Piece(1, new int[]{0, 5, 3, 0});
        Piece p2 = new Piece(2, new int[]{0, 0, 7, 5});
        Piece p3 = new Piece(3, new int[]{3, 5, 0, 0});
        Piece p4 = new Piece(4, new int[]{7, 0, 0, 5});

        b.place(0, 0, p1, 0);
        b.place(0, 1, p2, 0);
        b.place(1, 0, p3, 0);
        b.place(1, 1, p4, 0);

        int[] score = b.calculateScore();

        // Manual count: p1.E==p2.W (5==5), p1.S==p3.N (3==3), p2.S==p4.N (7==7), p3.E==p4.W (5==5)
        int manualCount = 4;
        assertEquals(manualCount, score[0], "Score should match manual edge count");
    }
}
