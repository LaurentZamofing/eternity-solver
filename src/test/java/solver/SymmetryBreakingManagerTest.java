package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SymmetryBreakingManager
 */
public class SymmetryBreakingManagerTest {

    private SymmetryBreakingManager manager;
    private Board board;
    private Map<Integer, Piece> pieces;

    @BeforeEach
    void setUp() {
        // Create a 3x3 board for testing
        board = new Board(3, 3);

        // Create some test pieces
        pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 1, 1, 0})); // Corner piece
        pieces.put(2, new Piece(2, new int[]{0, 2, 2, 0})); // Corner piece
        pieces.put(3, new Piece(3, new int[]{0, 3, 3, 0})); // Corner piece
        pieces.put(4, new Piece(4, new int[]{1, 2, 3, 4})); // Regular piece

        // Create manager (3x3 board, verbose=false).
        // Symmetry-breaking flags ship OFF by default (correctness bug on
        // 4x4 puzzles, see SymmetryBreakingManager). These tests still
        // exercise the rule logic, so we enable both flags here.
        manager = new SymmetryBreakingManager(3, 3, false);
        manager.setLexicographicOrdering(true);
        manager.setRotationalFixing(true);
    }

    @Test
    void testLexicographicOrdering_OnlyCanonicalTLAccepted() {
        // Test pieces 1, 2, 3 are all TL-fittable (have a rotation with N=0 AND W=0).
        // The canonical TL piece is the one with the smallest ID = piece 1.
        // The new rule rejects any other TL-fittable piece at TL.
        assertTrue(manager.isPlacementAllowed(board, 0, 0, 1, 0, pieces),
            "canonical TL piece (smallest ID among TL-fittable) accepted");
        assertFalse(manager.isPlacementAllowed(board, 0, 0, 2, 0, pieces),
            "non-canonical TL-fittable piece rejected (breaks rotational symmetry)");
        assertFalse(manager.isPlacementAllowed(board, 0, 0, 3, 0, pieces),
            "non-canonical TL-fittable piece rejected");
    }

    @Test
    void testLexicographicOrdering_OtherCornersUnconstrained() {
        // The new rule constrains TL only — other corners are pinned by
        // edge-matching during the actual solve. The pre-redesign rule
        // ("every corner ≥ TL.id") was incorrect on puzzles where the
        // smallest-ID corner is geometrically constrained off-TL.
        board.place(0, 0, pieces.get(1), 0);

        // Anything goes at TR/BL/BR now (the previous test asserted some
        // would be rejected; that was the bug fixed in 2026-04 redesign).
        assertTrue(manager.isPlacementAllowed(board, 0, 2, 2, 0, pieces));
        assertTrue(manager.isPlacementAllowed(board, 2, 0, 3, 0, pieces));
        assertTrue(manager.isPlacementAllowed(board, 2, 2, 4, 0, pieces));
    }

    @Test
    void testRotationFixing_TopLeftCorner() {
        // Top-left corner must use rotation 0
        assertTrue(manager.isPlacementAllowed(board, 0, 0, 1, 0, pieces));
        assertFalse(manager.isPlacementAllowed(board, 0, 0, 1, 1, pieces));
        assertFalse(manager.isPlacementAllowed(board, 0, 0, 1, 2, pieces));
        assertFalse(manager.isPlacementAllowed(board, 0, 0, 1, 3, pieces));
    }

    @Test
    void testRotationFixing_OtherPositions() {
        // Other positions can use any rotation
        assertTrue(manager.isPlacementAllowed(board, 0, 1, 1, 0, pieces));
        assertTrue(manager.isPlacementAllowed(board, 0, 1, 1, 1, pieces));
        assertTrue(manager.isPlacementAllowed(board, 0, 1, 1, 2, pieces));
        assertTrue(manager.isPlacementAllowed(board, 0, 1, 1, 3, pieces));
    }

    @Test
    void testIsPlacementAllowed_NonCornerPosition() {
        // Place piece 2 at top-left
        board.place(0, 0, pieces.get(2), 0);

        // Non-corner positions have no lexicographic constraint
        // Even piece 1 (ID < 2) should be allowed
        assertTrue(manager.isPlacementAllowed(board, 0, 1, 1, 0, pieces)); // Non-corner
        assertTrue(manager.isPlacementAllowed(board, 1, 0, 1, 0, pieces)); // Non-corner
        assertTrue(manager.isPlacementAllowed(board, 1, 1, 1, 0, pieces)); // Center
    }

    @Test
    void testValidateBoardState_ValidConfiguration() {
        // Place pieces in valid order: piece 1 at top-left, larger IDs elsewhere
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 2, pieces.get(2), 0);
        board.place(2, 0, pieces.get(3), 0);

        assertTrue(manager.validateBoardState(board));
    }

    @Test
    void testValidateBoardState_InvalidConfiguration() {
        // Post-2026-04 redesign: the lex rule is enforced per-placement at
        // TL only, so post-hoc validation is a no-op. The previous "every
        // corner ≥ TL.id" rule was incorrect on edge-matching puzzles.
        board.place(0, 0, pieces.get(3), 0);
        board.place(0, 2, pieces.get(1), 0);

        assertTrue(manager.validateBoardState(board),
            "validateBoardState is now a no-op; the per-placement rule guards correctness");
    }

    @Test
    void testValidateBoardState_EmptyBoard() {
        // Empty board is valid
        assertTrue(manager.validateBoardState(board));
    }

    @Test
    void testValidateBoardState_OnlyTopLeftPlaced() {
        // Board with only top-left is valid
        board.place(0, 0, pieces.get(2), 0);
        assertTrue(manager.validateBoardState(board));
    }

    @Test
    void testGetExpectedReductionFactor_Default() {
        // Default: lexicographic ordering enabled = 4x reduction
        assertEquals(4.0, manager.getExpectedReductionFactor());
    }

    @Test
    void testGetExpectedReductionFactor_Disabled() {
        // Disable all symmetry breaking
        manager.setLexicographicOrdering(false);
        manager.setRotationalFixing(false);
        assertEquals(1.0, manager.getExpectedReductionFactor());
    }

    @Test
    void testIsEnabled_Default() {
        // Default: at least one strategy should be enabled
        assertTrue(manager.isEnabled());
    }

    @Test
    void testIsEnabled_AllDisabled() {
        // Disable all strategies
        manager.setLexicographicOrdering(false);
        manager.setRotationalFixing(false);
        assertFalse(manager.isEnabled());
    }

    @Test
    void testLogConfiguration_DoesNotCrash() {
        // Verbose manager should not crash when logging
        SymmetryBreakingManager verboseManager = new SymmetryBreakingManager(3, 3, true);
        assertDoesNotThrow(() -> verboseManager.logConfiguration());
    }

    @Test
    void testSetLexicographicOrdering() {
        manager.setLexicographicOrdering(false);

        // With lexicographic disabled, smaller IDs should be allowed
        board.place(0, 0, pieces.get(3), 0);
        assertTrue(manager.isPlacementAllowed(board, 0, 2, 1, 0, pieces));
    }

    @Test
    void testSetRotationalFixing() {
        manager.setRotationalFixing(false);

        // With rotation fixing disabled, any rotation at top-left should be allowed
        assertTrue(manager.isPlacementAllowed(board, 0, 0, 1, 0, pieces));
        assertTrue(manager.isPlacementAllowed(board, 0, 0, 1, 1, pieces));
        assertTrue(manager.isPlacementAllowed(board, 0, 0, 1, 2, pieces));
        assertTrue(manager.isPlacementAllowed(board, 0, 0, 1, 3, pieces));
    }
}
