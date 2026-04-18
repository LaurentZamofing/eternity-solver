package solver.experimental.mcv;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.PuzzleFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for {@link MCVCellSelector}. */
@DisplayName("MCVCellSelector")
class MCVCellSelectorTest {

    @Test
    @DisplayName("empty 3×3 board: corners score 2 (two borders)")
    void cornersHaveTwoBorderPressure() {
        Board board = new Board(3, 3);
        assertEquals(2, MCVCellSelector.score(board, 0, 0));
        assertEquals(2, MCVCellSelector.score(board, 0, 2));
        assertEquals(2, MCVCellSelector.score(board, 2, 0));
        assertEquals(2, MCVCellSelector.score(board, 2, 2));
    }

    @Test
    @DisplayName("empty 3×3 board: edges score 1 (one border)")
    void edgesHaveOneBorderPressure() {
        Board board = new Board(3, 3);
        assertEquals(1, MCVCellSelector.score(board, 0, 1));
        assertEquals(1, MCVCellSelector.score(board, 1, 0));
        assertEquals(1, MCVCellSelector.score(board, 1, 2));
        assertEquals(1, MCVCellSelector.score(board, 2, 1));
    }

    @Test
    @DisplayName("empty 3×3 board: center has pressure 0 — no borders, no placed neighbour")
    void centerHasZeroPressure() {
        Board board = new Board(3, 3);
        assertEquals(0, MCVCellSelector.score(board, 1, 1));
    }

    @Test
    @DisplayName("non-empty cell returns -1")
    void placedCellReturnsMinusOne() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample3x3();
        // Place any piece at a corner
        Piece p = pieces.values().iterator().next();
        board.place(0, 0, p, 0);
        assertEquals(-1, MCVCellSelector.score(board, 0, 0));
    }

    @Test
    @DisplayName("placed neighbour increases pressure of adjacent cells")
    void placedNeighbourRaisesPressure() {
        Board board = new Board(3, 3);
        Piece p = PuzzleFactory.createExample3x3().values().iterator().next();
        // Before: center (1,1) has pressure 0.
        assertEquals(0, MCVCellSelector.score(board, 1, 1));
        // Place a piece next to center.
        board.place(0, 1, p, 0);
        // Now (1,1) has one placed neighbour → pressure 1.
        assertEquals(1, MCVCellSelector.score(board, 1, 1));
    }

    @Test
    @DisplayName("findMostConstraining on empty 3×3 returns a corner")
    void findPicksCorner() {
        Board board = new Board(3, 3);
        int[] best = MCVCellSelector.findMostConstraining(board);
        assertNotNull(best);
        int r = best[0], c = best[1];
        boolean isCorner = (r == 0 || r == 2) && (c == 0 || c == 2);
        assertTrue(isCorner, "highest-pressure cell on an empty 3×3 is a corner, got (" + r + "," + c + ")");
    }

    @Test
    @DisplayName("findMostConstraining on a full board returns null")
    void fullBoardReturnsNull() {
        Board board = new Board(1, 1);
        Piece p = PuzzleFactory.createExample3x3().values().iterator().next();
        board.place(0, 0, p, 0);
        assertNull(MCVCellSelector.findMostConstraining(board));
    }

    @Test
    @DisplayName("cell with 3 placed neighbours + 1 border has pressure 4")
    void maxPressureCase() {
        Board board = new Board(3, 3);
        Piece p = PuzzleFactory.createExample3x3().values().iterator().next();
        // Fill 3 neighbours of the bottom-center cell (2,1): (1,1), (2,0), (2,2).
        board.place(1, 1, p, 0);
        board.place(2, 0, p, 0);
        board.place(2, 2, p, 0);
        // (2,1) has 1 bottom-border + 3 placed neighbours = pressure 4.
        assertEquals(4, MCVCellSelector.score(board, 2, 1));
    }
}
