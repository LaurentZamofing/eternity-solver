package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SolverStateManager class.
 * Tests state management including step count and last placed position tracking.
 */
@DisplayName("SolverStateManager Tests")
public class SolverStateManagerTest {

    private SolverStateManager stateManager;

    @BeforeEach
    public void setUp() {
        stateManager = new SolverStateManager();
    }

    @Test
    @DisplayName("Initialize with zero/default values")
    public void testInitialValues() {
        assertEquals(0, stateManager.getStepCount(), "Initial step count should be 0");
        assertEquals(-1, stateManager.getLastPlacedRow(), "Initial last placed row should be -1");
        assertEquals(-1, stateManager.getLastPlacedCol(), "Initial last placed col should be -1");
    }

    @Test
    @DisplayName("Increment step count")
    public void testIncrementStepCount() {
        assertEquals(0, stateManager.getStepCount());

        stateManager.incrementStepCount();
        assertEquals(1, stateManager.getStepCount());

        stateManager.incrementStepCount();
        assertEquals(2, stateManager.getStepCount());

        stateManager.incrementStepCount();
        assertEquals(3, stateManager.getStepCount());
    }

    @Test
    @DisplayName("Set and get last placed position")
    public void testSetLastPlaced() {
        stateManager.setLastPlaced(2, 3);
        assertEquals(2, stateManager.getLastPlacedRow());
        assertEquals(3, stateManager.getLastPlacedCol());

        stateManager.setLastPlaced(5, 7);
        assertEquals(5, stateManager.getLastPlacedRow());
        assertEquals(7, stateManager.getLastPlacedCol());
    }

    @Test
    @DisplayName("Set last placed to -1, -1")
    public void testSetLastPlacedToNone() {
        stateManager.setLastPlaced(2, 3);
        assertEquals(2, stateManager.getLastPlacedRow());

        stateManager.setLastPlaced(-1, -1);
        assertEquals(-1, stateManager.getLastPlacedRow());
        assertEquals(-1, stateManager.getLastPlacedCol());
    }

    @Test
    @DisplayName("Reset clears all state")
    public void testReset() {
        // Set some state
        stateManager.incrementStepCount();
        stateManager.incrementStepCount();
        stateManager.setLastPlaced(4, 5);

        assertEquals(2, stateManager.getStepCount());
        assertEquals(4, stateManager.getLastPlacedRow());
        assertEquals(5, stateManager.getLastPlacedCol());

        // Reset
        stateManager.reset();

        // Verify all cleared
        assertEquals(0, stateManager.getStepCount());
        assertEquals(-1, stateManager.getLastPlacedRow());
        assertEquals(-1, stateManager.getLastPlacedCol());
    }

    @Test
    @DisplayName("Find and set last placed on empty board")
    public void testFindAndSetLastPlacedEmptyBoard() {
        Board board = new Board(3, 3);

        stateManager.findAndSetLastPlaced(board);

        assertEquals(-1, stateManager.getLastPlacedRow(), "Empty board should result in -1");
        assertEquals(-1, stateManager.getLastPlacedCol(), "Empty board should result in -1");
    }

    @Test
    @DisplayName("Find and set last placed with single piece")
    public void testFindAndSetLastPlacedSinglePiece() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{0, 0, 0, 0});

        board.place(1, 1, piece, 0);

        stateManager.findAndSetLastPlaced(board);

        assertEquals(1, stateManager.getLastPlacedRow());
        assertEquals(1, stateManager.getLastPlacedCol());
    }

    @Test
    @DisplayName("Find and set last placed - bottom-right wins")
    public void testFindAndSetLastPlacedBottomRight() {
        Board board = new Board(4, 4);
        Piece piece = new Piece(1, new int[]{0, 0, 0, 0});

        // Place pieces at multiple positions
        board.place(0, 0, piece, 0);
        board.place(1, 2, piece, 0);
        board.place(3, 3, piece, 0);  // Bottom-right should be found

        stateManager.findAndSetLastPlaced(board);

        assertEquals(3, stateManager.getLastPlacedRow(), "Should find bottom-right piece");
        assertEquals(3, stateManager.getLastPlacedCol(), "Should find bottom-right piece");
    }

    @Test
    @DisplayName("Find and set last placed - scans from bottom-right")
    public void testFindAndSetLastPlacedScanOrder() {
        Board board = new Board(3, 3);
        Piece piece = new Piece(1, new int[]{0, 0, 0, 0});

        // Place piece at (1, 2) - should be found as last
        board.place(0, 0, piece, 0);
        board.place(0, 1, piece, 0);
        board.place(1, 2, piece, 0);

        stateManager.findAndSetLastPlaced(board);

        assertEquals(1, stateManager.getLastPlacedRow());
        assertEquals(2, stateManager.getLastPlacedCol());
    }

    @Test
    @DisplayName("Step count persists across multiple increments")
    public void testStepCountAccumulation() {
        for (int i = 0; i < 10; i++) {
            stateManager.incrementStepCount();
        }
        assertEquals(10, stateManager.getStepCount());

        for (int i = 0; i < 5; i++) {
            stateManager.incrementStepCount();
        }
        assertEquals(15, stateManager.getStepCount());
    }

    @Test
    @DisplayName("Last placed position persists until changed")
    public void testLastPlacedPersistence() {
        stateManager.setLastPlaced(2, 3);

        // Increment step count shouldn't affect last placed
        stateManager.incrementStepCount();
        stateManager.incrementStepCount();

        assertEquals(2, stateManager.getLastPlacedRow());
        assertEquals(3, stateManager.getLastPlacedCol());
    }

    @Test
    @DisplayName("Reset can be called multiple times")
    public void testMultipleResets() {
        stateManager.incrementStepCount();
        stateManager.setLastPlaced(1, 1);

        stateManager.reset();
        assertEquals(0, stateManager.getStepCount());

        stateManager.incrementStepCount();
        stateManager.setLastPlaced(2, 2);
        assertEquals(1, stateManager.getStepCount());

        stateManager.reset();
        assertEquals(0, stateManager.getStepCount());
        assertEquals(-1, stateManager.getLastPlacedRow());
    }

    @Test
    @DisplayName("Find and set last placed after reset")
    public void testFindAfterReset() {
        Board board = new Board(2, 2);
        Piece piece = new Piece(1, new int[]{0, 0, 0, 0});

        board.place(1, 1, piece, 0);
        stateManager.findAndSetLastPlaced(board);
        assertEquals(1, stateManager.getLastPlacedRow());

        stateManager.reset();
        assertEquals(-1, stateManager.getLastPlacedRow());

        stateManager.findAndSetLastPlaced(board);
        assertEquals(1, stateManager.getLastPlacedRow(), "Should refind after reset");
    }
}
