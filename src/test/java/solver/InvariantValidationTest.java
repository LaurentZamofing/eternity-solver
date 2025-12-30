package solver;

import model.Board;
import model.Piece;
import model.Placement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Invariant Validation Tests for Eternity Solver
 *
 * Tests that verify critical invariants that must always hold:
 * - Conservation laws (pieces, rotations, cells)
 * - Monotonicity constraints
 * - State consistency
 * - Structural integrity
 */
public class InvariantValidationTest {

    private Board board;
    private Map<Integer, Piece> pieces;

    @BeforeEach
    void setUp() {
        board = new Board(4, 4);
        pieces = createTestPieces();
    }

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> p = new HashMap<>();
        p.put(1, new Piece(1, new int[]{0, 1, 2, 0}));
        p.put(2, new Piece(2, new int[]{0, 2, 3, 0}));
        p.put(3, new Piece(3, new int[]{0, 3, 4, 0}));
        p.put(4, new Piece(4, new int[]{1, 2, 3, 4}));
        p.put(5, new Piece(5, new int[]{5, 5, 5, 5}));
        return p;
    }

    // ==================== CONSERVATION INVARIANTS ====================

    @Test
    void testPieceCountConservation() {
        // Invariant: Total pieces = pieces in map at all times
        int initialCount = pieces.size();
        assertEquals(5, initialCount);

        // After placing, count should remain the same
        board.place(0, 0, pieces.get(1), 0);
        assertEquals(initialCount, pieces.size(), "Piece count should be conserved");

        board.place(0, 1, pieces.get(2), 0);
        assertEquals(initialCount, pieces.size(), "Piece count should be conserved");

        board.remove(0, 0);
        assertEquals(initialCount, pieces.size(), "Piece count should be conserved");
    }

    @Test
    void testCellCountConservation() {
        // Invariant: rows * cols = constant for a board
        assertEquals(16, board.getRows() * board.getCols());

        // Place pieces
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                board.place(r, c, pieces.get(1), 0);
            }
        }

        assertEquals(16, board.getRows() * board.getCols(), "Cell count should be conserved");
    }

    @Test
    void testEmptyPlusFilledCellsEqualsTotal() {
        // Invariant: empty cells + filled cells = total cells
        int totalCells = board.getRows() * board.getCols();

        int emptyCells = countEmptyCells(board);
        int filledCells = countFilledCells(board);

        assertEquals(totalCells, emptyCells + filledCells, "Empty + filled should equal total");

        // After placing
        board.place(0, 0, pieces.get(1), 0);
        emptyCells = countEmptyCells(board);
        filledCells = countFilledCells(board);
        assertEquals(totalCells, emptyCells + filledCells);

        // After removing
        board.remove(0, 0);
        emptyCells = countEmptyCells(board);
        filledCells = countFilledCells(board);
        assertEquals(totalCells, emptyCells + filledCells);
    }

    @Test
    void testPlacedPiecesConservation() {
        // Invariant: Count of placed pieces equals non-empty cells
        assertEquals(0, countFilledCells(board));

        board.place(0, 0, pieces.get(1), 0);
        assertEquals(1, countFilledCells(board));

        board.place(1, 1, pieces.get(2), 0);
        assertEquals(2, countFilledCells(board));

        board.remove(0, 0);
        assertEquals(1, countFilledCells(board));

        board.remove(1, 1);
        assertEquals(0, countFilledCells(board));
    }

    @Test
    void testEdgeCountConservation() {
        // Invariant: Each piece always has exactly 4 edges
        for (Piece piece : pieces.values()) {
            assertEquals(4, piece.getEdges().length);

            for (int rot = 0; rot < 4; rot++) {
                int[] rotated = piece.edgesRotated(rot);
                assertEquals(4, rotated.length, "Rotated piece should still have 4 edges");
            }
        }
    }

    // ==================== MONOTONICITY INVARIANTS ====================

    @Test
    void testRotationCountMonotonicity() {
        // Invariant: Total possible rotations >= number of pieces
        Piece piece = pieces.get(4); // Non-symmetric piece
        int uniqueRotations = piece.getUniqueRotationCount();
        assertTrue(uniqueRotations >= 1 && uniqueRotations <= 4,
            "Rotation count should be between 1 and 4");
    }

    @Test
    void testSymmetricPieceHasFewerRotations() {
        // Invariant: Symmetric pieces have <= 4 unique rotations
        Piece symmetric = pieces.get(5); // All edges same
        int rotations = symmetric.getUniqueRotationCount();
        assertTrue(rotations <= 4, "Symmetric piece should have at most 4 rotations");
        assertEquals(1, rotations, "Piece with all edges same should have 1 rotation");
    }

    @Test
    void testScoreNeverExceedsMaximum() {
        // Invariant: Score <= maximum possible score
        for (int i = 0; i < 10; i++) {
            board.place(i / 4, i % 4, pieces.get((i % pieces.size()) + 1), 0);
            int[] score = board.calculateScore();
            assertTrue(score[0] <= score[1], "Score should never exceed maximum");
        }
    }

    @Test
    void testScoreNonNegative() {
        // Invariant: Score is always >= 0
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(2), 0);

        int[] score = board.calculateScore();
        assertTrue(score[0] >= 0, "Score should be non-negative");
        assertTrue(score[1] >= 0, "Max score should be non-negative");
    }

    @Test
    void testFilledCellsNeverDecreaseWithoutRemoval() {
        // Invariant: Placing pieces monotonically increases filled cells
        int count = 0;
        for (int i = 0; i < 5; i++) {
            board.place(i / 4, i % 4, pieces.get((i % pieces.size()) + 1), 0);
            count++;
            assertEquals(count, countFilledCells(board),
                "Filled cells should increase with each placement");
        }
    }

    // ==================== STATE CONSISTENCY INVARIANTS ====================

    @Test
    void testEmptyCellHasNoPlacement() {
        // Invariant: isEmpty(r,c) == true <=> getPlacement(r,c) == null
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.isEmpty(r, c)) {
                    assertNull(board.getPlacement(r, c),
                        "Empty cell should have null placement at (" + r + "," + c + ")");
                } else {
                    assertNotNull(board.getPlacement(r, c),
                        "Non-empty cell should have non-null placement at (" + r + "," + c + ")");
                }
            }
        }

        // After placing
        board.place(0, 0, pieces.get(1), 0);
        assertFalse(board.isEmpty(0, 0));
        assertNotNull(board.getPlacement(0, 0));
    }

    @Test
    void testPlacementHasValidPieceId() {
        // Invariant: All placements reference valid piece IDs
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(2), 0);
        board.place(1, 0, pieces.get(3), 0);

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Placement p = board.getPlacement(r, c);
                if (p != null) {
                    assertTrue(p.getPieceId() > 0, "Piece ID should be positive");
                }
            }
        }
    }

    @Test
    void testPlacementRotationInBounds() {
        // Invariant: Rotation value is always in [0, 3]
        for (int rot = 0; rot < 4; rot++) {
            board.place(0, rot, pieces.get(1), rot);
            Placement p = board.getPlacement(0, rot);
            assertTrue(p.getRotation() >= 0 && p.getRotation() <= 3,
                "Rotation should be in range [0,3]");
        }
    }

    @Test
    void testBoardDimensionsPositive() {
        // Invariant: Rows and columns are always positive
        assertTrue(board.getRows() > 0, "Rows should be positive");
        assertTrue(board.getCols() > 0, "Columns should be positive");

        Board b2 = new Board(10, 15);
        assertTrue(b2.getRows() > 0, "Rows should be positive");
        assertTrue(b2.getCols() > 0, "Columns should be positive");
    }

    @Test
    void testPieceImmutability() {
        // Invariant: Pieces are immutable - getEdges() returns copy
        Piece piece = pieces.get(1);
        int[] edges1 = piece.getEdges();
        int[] edges2 = piece.getEdges();

        assertNotSame(edges1, edges2, "Should return different array instances");

        edges1[0] = 999;
        int[] edges3 = piece.getEdges();
        assertNotEquals(999, edges3[0], "Piece should be immutable");
    }

    // ==================== STRUCTURAL INTEGRITY INVARIANTS ====================

    @Test
    void testEveryPieceHasUniqueId() {
        // Invariant: All piece IDs in the map are unique
        Set<Integer> ids = new HashSet<>();
        for (Piece piece : pieces.values()) {
            assertTrue(ids.add(piece.getId()), "Piece ID should be unique");
        }
    }

    @Test
    void testBoardGridCompletelyCoversSpace() {
        // Invariant: Every cell in [0, rows) x [0, cols) is accessible
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                final int row = r;
                final int col = c;
                assertDoesNotThrow(() -> board.isEmpty(row, col),
                    "Cell (" + row + "," + col + ") should be accessible");
            }
        }
    }

    @Test
    void testPlacementEdgesMatchPieceWithRotation() {
        // Invariant: Placement edges equal piece edges after rotation
        Piece piece = pieces.get(1);
        int rotation = 2;

        board.place(0, 0, piece, rotation);
        Placement placement = board.getPlacement(0, 0);

        int[] expectedEdges = piece.edgesRotated(rotation);
        assertArrayEquals(expectedEdges, placement.edges,
            "Placement edges should match rotated piece edges");
    }

    @Test
    void testMaxScoreMatchesExpectedFormula() {
        // Invariant: Max score = (rows-1)*cols + rows*(cols-1)
        int rows = board.getRows();
        int cols = board.getCols();
        int expectedMax = (rows - 1) * cols + rows * (cols - 1);

        int[] score = board.calculateScore();
        assertEquals(expectedMax, score[1], "Max score should match formula");
    }

    @Test
    void testMultiplePlacementsAtSameCellOverwrite() {
        // Invariant: Placing multiple pieces at same position keeps only the last one
        board.place(0, 0, pieces.get(1), 0);
        assertEquals(1, board.getPlacement(0, 0).getPieceId());

        board.place(0, 0, pieces.get(2), 1);
        assertEquals(2, board.getPlacement(0, 0).getPieceId());
        assertEquals(1, board.getPlacement(0, 0).getRotation());

        assertEquals(1, countFilledCells(board), "Should still have only 1 filled cell");
    }

    // ==================== HELPER METHODS ====================

    private int countEmptyCells(Board board) {
        int count = 0;
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.isEmpty(r, c)) count++;
            }
        }
        return count;
    }

    private int countFilledCells(Board board) {
        int count = 0;
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.isEmpty(r, c)) count++;
            }
        }
        return count;
    }
}
