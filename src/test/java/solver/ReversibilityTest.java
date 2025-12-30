package solver;

import model.Board;
import model.Piece;
import model.Placement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reversibility Tests for Eternity Solver
 *
 * Tests that verify operations can be reversed to restore original state:
 * - Place/Remove reversibility
 * - Rotation reversibility
 * - State restoration after backtracking
 * - Undo/Redo semantics
 * - Domain restoration
 */
public class ReversibilityTest {

    private Board board;
    private Map<Integer, Piece> pieces;

    @BeforeEach
    void setUp() {
        board = new Board(5, 5);
        pieces = createTestPieces();
    }

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> p = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            p.put(i, new Piece(i, new int[]{0, i, i + 1, 0}));
        }
        return p;
    }

    // ==================== PLACE/REMOVE REVERSIBILITY ====================

    @Test
    void testSinglePlacementRemovalReversibility() {
        // Reversibility: place(p) then remove() restores original state
        assertTrue(board.isEmpty(0, 0));
        assertNull(board.getPlacement(0, 0));

        Piece piece = pieces.get(1);
        board.place(0, 0, piece, 0);
        assertFalse(board.isEmpty(0, 0));

        board.remove(0, 0);
        assertTrue(board.isEmpty(0, 0));
        assertNull(board.getPlacement(0, 0));
    }

    @Test
    void testMultiplePlacementsReversibility() {
        // Reversibility: Multiple place/remove cycles restore original state
        int[][] positions = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};

        for (int i = 0; i < positions.length; i++) {
            int r = positions[i][0];
            int c = positions[i][1];

            assertTrue(board.isEmpty(r, c), "Initially empty at (" + r + "," + c + ")");
            board.place(r, c, pieces.get(i + 1), 0);
            assertFalse(board.isEmpty(r, c), "Should be filled");
            board.remove(r, c);
            assertTrue(board.isEmpty(r, c), "Should be empty again at (" + r + "," + c + ")");
        }
    }

    @Test
    void testNestedPlacementReversibility() {
        // Reversibility: Nested operations can be reversed in LIFO order
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(2), 0);
        board.place(0, 2, pieces.get(3), 0);

        // Remove in reverse order
        board.remove(0, 2);
        assertNull(board.getPlacement(0, 2));
        assertNotNull(board.getPlacement(0, 0));
        assertNotNull(board.getPlacement(0, 1));

        board.remove(0, 1);
        assertNull(board.getPlacement(0, 1));
        assertNotNull(board.getPlacement(0, 0));

        board.remove(0, 0);
        assertTrue(board.isEmpty(0, 0));
        assertTrue(board.isEmpty(0, 1));
        assertTrue(board.isEmpty(0, 2));
    }

    @Test
    void testPlacementReversibilityWithDifferentRotations() {
        // Reversibility: Works regardless of rotation
        for (int rot = 0; rot < 4; rot++) {
            assertTrue(board.isEmpty(0, rot));
            board.place(0, rot, pieces.get(1), rot);
            assertFalse(board.isEmpty(0, rot));
            board.remove(0, rot);
            assertTrue(board.isEmpty(0, rot));
        }
    }

    @Test
    void testScoreReversibilityAfterPlaceRemove() {
        // Reversibility: Score returns to original after place/remove
        int[] initialScore = board.calculateScore();

        board.place(0, 0, pieces.get(1), 0);
        // Note: Single piece has no neighbors, so score stays at 0
        // We just verify that max score is unchanged

        board.remove(0, 0);
        int[] afterRemove = board.calculateScore();
        assertArrayEquals(initialScore, afterRemove, "Score should be restored");
    }

    // ==================== ROTATION REVERSIBILITY ====================

    @Test
    void testRotationCycleReversibility() {
        // Reversibility: 4 rotations return to original
        Piece piece = pieces.get(1);
        int[] original = piece.edgesRotated(0);

        int[] after1 = piece.edgesRotated(1);
        int[] after2 = piece.edgesRotated(2);
        int[] after3 = piece.edgesRotated(3);
        int[] after4 = piece.edgesRotated(4);

        assertArrayEquals(original, after4, "4 rotations should return to original");
    }

    @Test
    void testRotationInverseReversibility() {
        // Reversibility: rotate(k) then rotate(4-k) returns to original
        Piece piece = new Piece(99, new int[]{1, 2, 3, 4});

        for (int k = 0; k < 4; k++) {
            int[] rotated = piece.edgesRotated(k);
            Piece temp = new Piece(100, rotated);
            int[] backToOriginal = temp.edgesRotated(4 - k);

            int[] original = piece.edgesRotated(0);
            assertArrayEquals(original, backToOriginal,
                "rotate(" + k + ") then rotate(" + (4 - k) + ") should restore original");
        }
    }

    @Test
    void testRotationReversibilityOnBoard() {
        // Reversibility: Placing with rotation then replacing with inverse restores state
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        board.place(0, 0, piece, 2); // 180° rotation
        int[] rotated = board.getPlacement(0, 0).edges;

        board.place(0, 0, piece, 0); // Back to 0° (or could use rot 4-2=2 again to get back via composition)
        int[] original = board.getPlacement(0, 0).edges;

        assertArrayEquals(piece.edgesRotated(0), original, "Should restore to original orientation");
    }

    @Test
    void testNegativeRotationReversibility() {
        // Reversibility: Negative rotations work as inverse
        Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

        int[] forward = piece.edgesRotated(1);
        int[] backward = piece.edgesRotated(-1);

        // -1 should equal 3 (counterclockwise = 3 clockwise)
        int[] three = piece.edgesRotated(3);
        assertArrayEquals(three, backward, "rotate(-1) should equal rotate(3)");
    }

    // ==================== STATE RESTORATION ====================

    @Test
    void testFullBoardStateReversibility() {
        // Reversibility: Fill then clear entire board
        // Fill board
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                board.place(r, c, pieces.get((r * board.getCols() + c) % pieces.size() + 1), 0);
            }
        }

        // Verify all filled
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                assertFalse(board.isEmpty(r, c));
            }
        }

        // Clear board
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                board.remove(r, c);
            }
        }

        // Verify all empty
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                assertTrue(board.isEmpty(r, c), "Cell (" + r + "," + c + ") should be empty");
            }
        }
    }

    @Test
    void testPartialStateReversibility() {
        // Reversibility: Partial fill and restore
        List<int[]> placements = new ArrayList<>();
        placements.add(new int[]{0, 0, 1, 0});
        placements.add(new int[]{0, 1, 2, 1});
        placements.add(new int[]{1, 0, 3, 2});
        placements.add(new int[]{2, 2, 4, 3});

        // Place pieces
        for (int[] p : placements) {
            board.place(p[0], p[1], pieces.get(p[2]), p[3]);
        }

        // Remove in reverse order
        for (int i = placements.size() - 1; i >= 0; i--) {
            int[] p = placements.get(i);
            board.remove(p[0], p[1]);
            assertTrue(board.isEmpty(p[0], p[1]));
        }

        // Verify entire board is empty
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                assertTrue(board.isEmpty(r, c));
            }
        }
    }

    @Test
    void testSnapshotRestoreReversibility() {
        // Reversibility: Capture state, modify, restore
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(2), 1);

        // Capture state
        Map<String, Integer> snapshot = new HashMap<>();
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Placement p = board.getPlacement(r, c);
                if (p != null) {
                    snapshot.put(r + "," + c, p.getPieceId());
                }
            }
        }

        // Modify board
        board.place(1, 1, pieces.get(3), 0);
        board.place(2, 2, pieces.get(4), 2);

        // Restore original state
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                String key = r + "," + c;
                if (snapshot.containsKey(key)) {
                    int pieceId = snapshot.get(key);
                    // Re-place from snapshot (simplified - assumes rotation 0)
                    board.place(r, c, pieces.get(pieceId), 0);
                } else {
                    board.remove(r, c);
                }
            }
        }

        // Verify restoration
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                String key = r + "," + c;
                if (snapshot.containsKey(key)) {
                    assertEquals(snapshot.get(key), board.getPlacement(r, c).getPieceId());
                } else {
                    assertTrue(board.isEmpty(r, c));
                }
            }
        }
    }

    // ==================== OVERWRITE REVERSIBILITY ====================

    @Test
    void testOverwriteReversibility() {
        // Reversibility: Overwrite then restore original
        Piece original = pieces.get(1);
        Piece replacement = pieces.get(2);

        board.place(0, 0, original, 0);
        int originalId = board.getPlacement(0, 0).getPieceId();

        board.place(0, 0, replacement, 1);
        assertEquals(replacement.getId(), board.getPlacement(0, 0).getPieceId());

        board.place(0, 0, original, 0);
        assertEquals(originalId, board.getPlacement(0, 0).getPieceId());
    }

    @Test
    void testMultipleOverwriteReversibility() {
        // Reversibility: Multiple overwrites can be reversed
        for (int i = 1; i <= 5; i++) {
            board.place(0, 0, pieces.get(i), i % 4);
            assertEquals(i, board.getPlacement(0, 0).getPieceId());
        }

        // Place original again
        board.place(0, 0, pieces.get(1), 0);
        assertEquals(1, board.getPlacement(0, 0).getPieceId());
        assertEquals(0, board.getPlacement(0, 0).getRotation());
    }

    // ==================== COMPLEX REVERSIBILITY SCENARIOS ====================

    @Test
    void testInterleavedPlaceRemoveReversibility() {
        // Reversibility: Interleaved operations maintain reversibility
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(2), 0);
        board.remove(0, 0);
        board.place(0, 2, pieces.get(3), 0);
        board.remove(0, 1);
        board.place(0, 3, pieces.get(4), 0);

        // Now remove all
        board.remove(0, 2);
        board.remove(0, 3);

        // Should be fully empty
        for (int c = 0; c < 4; c++) {
            assertTrue(board.isEmpty(0, c));
        }
    }

    @Test
    void testChainedPlacementReversibility() {
        // Reversibility: Chain of placements can be fully reversed
        List<int[]> chain = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int r = i / board.getCols();
            int c = i % board.getCols();
            chain.add(new int[]{r, c, i % pieces.size() + 1, i % 4});
        }

        // Place all
        for (int[] p : chain) {
            board.place(p[0], p[1], pieces.get(p[2]), p[3]);
        }

        // Remove all in reverse
        for (int i = chain.size() - 1; i >= 0; i--) {
            int[] p = chain.get(i);
            board.remove(p[0], p[1]);
        }

        // Verify empty
        for (int[] p : chain) {
            assertTrue(board.isEmpty(p[0], p[1]));
        }
    }

    @Test
    void testRandomAccessReversibility() {
        // Reversibility: Random access pattern maintains reversibility
        Random rand = new Random(42); // Fixed seed for reproducibility
        List<int[]> operations = new ArrayList<>();

        // Perform random placements
        for (int i = 0; i < 20; i++) {
            int r = rand.nextInt(board.getRows());
            int c = rand.nextInt(board.getCols());
            int pieceId = rand.nextInt(pieces.size()) + 1;
            int rotation = rand.nextInt(4);

            board.place(r, c, pieces.get(pieceId), rotation);
            operations.add(new int[]{r, c});
        }

        // Remove all in reverse order
        for (int i = operations.size() - 1; i >= 0; i--) {
            int[] op = operations.get(i);
            board.remove(op[0], op[1]);
        }

        // Verify final state
        for (int[] op : operations) {
            assertTrue(board.isEmpty(op[0], op[1]));
        }
    }

    @Test
    void testBoundaryReversibility() {
        // Reversibility: Works at board boundaries
        int[][] boundaries = {
            {0, 0}, {0, board.getCols() - 1},
            {board.getRows() - 1, 0}, {board.getRows() - 1, board.getCols() - 1}
        };

        for (int i = 0; i < boundaries.length; i++) {
            int r = boundaries[i][0];
            int c = boundaries[i][1];

            assertTrue(board.isEmpty(r, c));
            board.place(r, c, pieces.get(i + 1), i % 4);
            assertFalse(board.isEmpty(r, c));
            board.remove(r, c);
            assertTrue(board.isEmpty(r, c));
        }
    }

    @Test
    void testScoreReversibilityWithMultiplePlacements() {
        // Reversibility: Score reverses correctly with multiple pieces
        int[] score0 = board.calculateScore();

        board.place(0, 0, pieces.get(1), 0);
        int[] score1 = board.calculateScore();

        board.place(0, 1, pieces.get(2), 0);
        int[] score2 = board.calculateScore();

        board.remove(0, 1);
        int[] scoreAfterRemove1 = board.calculateScore();
        assertArrayEquals(score1, scoreAfterRemove1, "Score should match after removing second piece");

        board.remove(0, 0);
        int[] scoreAfterRemove2 = board.calculateScore();
        assertArrayEquals(score0, scoreAfterRemove2, "Score should match original after removing all");
    }

    @Test
    void testReversibilityWithComplexPattern() {
        // Reversibility: Complex cross pattern can be reversed
        int midRow = board.getRows() / 2;
        int midCol = board.getCols() / 2;

        // Create cross pattern
        List<int[]> crossPattern = new ArrayList<>();
        for (int c = 0; c < board.getCols(); c++) {
            crossPattern.add(new int[]{midRow, c});
        }
        for (int r = 0; r < board.getRows(); r++) {
            if (r != midRow) {
                crossPattern.add(new int[]{r, midCol});
            }
        }

        // Place cross
        for (int i = 0; i < crossPattern.size(); i++) {
            int[] pos = crossPattern.get(i);
            board.place(pos[0], pos[1], pieces.get((i % pieces.size()) + 1), i % 4);
        }

        // Remove cross
        for (int i = crossPattern.size() - 1; i >= 0; i--) {
            int[] pos = crossPattern.get(i);
            board.remove(pos[0], pos[1]);
        }

        // Verify all empty
        for (int[] pos : crossPattern) {
            assertTrue(board.isEmpty(pos[0], pos[1]));
        }
    }

    @Test
    void testReversibilityPreservesImmutability() {
        // Reversibility: Piece immutability is preserved during place/remove
        Piece piece = pieces.get(1);
        int[] originalEdges = piece.getEdges();

        board.place(0, 0, piece, 0);
        board.remove(0, 0);
        board.place(1, 1, piece, 2);
        board.remove(1, 1);

        int[] finalEdges = piece.getEdges();
        assertArrayEquals(originalEdges, finalEdges, "Piece should remain immutable");
    }

    @Test
    void testCumulativeReversibility() {
        // Reversibility: Cumulative operations all reverse correctly
        int operationCount = 0;

        for (int i = 0; i < 5; i++) {
            board.place(0, i, pieces.get(i + 1), i % 4);
            operationCount++;
            assertEquals(operationCount, countFilledCells());
        }

        for (int i = 4; i >= 0; i--) {
            board.remove(0, i);
            operationCount--;
            assertEquals(operationCount, countFilledCells());
        }

        assertEquals(0, countFilledCells());
    }

    @Test
    void testReversibilityWithDifferentPieces() {
        // Reversibility: Works with different pieces at same location
        for (int i = 1; i <= 5; i++) {
            board.place(2, 2, pieces.get(i), i % 4);
            assertFalse(board.isEmpty(2, 2));
            board.remove(2, 2);
            assertTrue(board.isEmpty(2, 2));
        }
    }

    @Test
    void testReversibilityMaintainsStateConsistency() {
        // Reversibility: State consistency maintained through place/remove cycles
        for (int cycle = 0; cycle < 3; cycle++) {
            // Place
            board.place(0, 0, pieces.get(1), 0);
            assertFalse(board.isEmpty(0, 0));
            assertNotNull(board.getPlacement(0, 0));
            assertEquals(1, board.getPlacement(0, 0).getPieceId());

            // Remove
            board.remove(0, 0);
            assertTrue(board.isEmpty(0, 0));
            assertNull(board.getPlacement(0, 0));
        }
    }

    // ==================== HELPER METHODS ====================

    private int countFilledCells() {
        int count = 0;
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.isEmpty(r, c)) count++;
            }
        }
        return count;
    }
}
