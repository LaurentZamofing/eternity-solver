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

        // Create manager (3x3 board, verbose=false)
        manager = new SymmetryBreakingManager(3, 3, false);
    }

    @Test
    void testLexicographicOrdering_TopLeftFirst() {
        // Top-left corner can always be placed first
        assertTrue(manager.isPlacementAllowed(board, 0, 0, 1, 0, pieces));
        assertTrue(manager.isPlacementAllowed(board, 0, 0, 2, 0, pieces));
        assertTrue(manager.isPlacementAllowed(board, 0, 0, 3, 0, pieces));
    }

    @Test
    void testLexicographicOrdering_RejectSmallerThanTopLeft() {
        // Place piece 2 at top-left
        board.place(0, 0, pieces.get(2), 0);

        // Piece 1 (ID < 2) should be rejected at other corners
        assertFalse(manager.isPlacementAllowed(board, 0, 2, 1, 0, pieces)); // Top-right
        assertFalse(manager.isPlacementAllowed(board, 2, 0, 1, 0, pieces)); // Bottom-left
        assertFalse(manager.isPlacementAllowed(board, 2, 2, 1, 0, pieces)); // Bottom-right
    }

    @Test
    void testLexicographicOrdering_AcceptLargerThanTopLeft() {
        // Place piece 1 at top-left
        board.place(0, 0, pieces.get(1), 0);

        // Pieces with ID > 1 should be accepted at other corners
        assertTrue(manager.isPlacementAllowed(board, 0, 2, 2, 0, pieces)); // Top-right
        assertTrue(manager.isPlacementAllowed(board, 2, 0, 3, 0, pieces)); // Bottom-left
        assertTrue(manager.isPlacementAllowed(board, 2, 2, 4, 0, pieces)); // Bottom-right
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
        // Place pieces in invalid order: piece 3 at top-left, piece 1 at corner
        board.place(0, 0, pieces.get(3), 0);
        board.place(0, 2, pieces.get(1), 0); // Violates: 1 < 3

        assertFalse(manager.validateBoardState(board));
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
