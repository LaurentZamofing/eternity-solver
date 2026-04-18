package solver.experimental.bitmap;

import model.Board;
import model.Placement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import util.PuzzleFactory;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P1 gate tests for {@link BitmapSolver}: curated 3×3 and 4×4 cases from
 * {@link util.PuzzleFactory}. Differential oracle lives in
 * {@link BitmapDifferentialTest}.
 */
@DisplayName("BitmapSolver — P1 skeleton")
class BitmapSolverTest {

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("solves curated 3×3")
    void solve3x3() {
        Board board = new Board(3, 3);
        BitmapSolver solver = new BitmapSolver();
        solver.setMaxExecutionTime(20_000);
        assertTrue(solver.solve(board, PuzzleFactory.createExample3x3()),
            "3×3 curated must solve");
        assertValidBoard(board);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("solves curated 4×4 easy")
    void solve4x4Easy() {
        Board board = new Board(4, 4);
        BitmapSolver solver = new BitmapSolver();
        solver.setMaxExecutionTime(40_000);
        assertTrue(solver.solve(board, PuzzleFactory.createExample4x4Easy()),
            "4×4 easy curated must solve");
        assertValidBoard(board);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("solves curated 4×4 hardV3")
    void solve4x4Hard() {
        Board board = new Board(4, 4);
        BitmapSolver solver = new BitmapSolver();
        solver.setMaxExecutionTime(40_000);
        assertTrue(solver.solve(board, PuzzleFactory.createExample4x4HardV3()),
            "4×4 hardV3 curated must solve");
        assertValidBoard(board);
    }

    @Test
    @DisplayName("rejects pre-filled board (POC contract)")
    void rejectsPreFilledBoard() {
        Board board = new Board(3, 3);
        var pieces = PuzzleFactory.createExample3x3();
        board.place(0, 0, pieces.values().iterator().next(), 0);
        BitmapSolver solver = new BitmapSolver();
        assertThrows(IllegalArgumentException.class, () -> solver.solve(board, pieces));
    }

    private static void assertValidBoard(Board board) {
        int rows = board.getRows();
        int cols = board.getCols();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                assertFalse(board.isEmpty(r, c), "cell (" + r + "," + c + ") filled");
                Placement p = board.getPlacement(r, c);
                if (r == 0) assertEquals(0, p.edges[0], "N-border 0");
                if (r == rows - 1) assertEquals(0, p.edges[2], "S-border 0");
                if (c == 0) assertEquals(0, p.edges[3], "W-border 0");
                if (c == cols - 1) assertEquals(0, p.edges[1], "E-border 0");
                if (r > 0) {
                    Placement n = board.getPlacement(r - 1, c);
                    assertEquals(n.edges[2], p.edges[0], "N/S match (" + r + "," + c + ")");
                }
                if (c > 0) {
                    Placement w = board.getPlacement(r, c - 1);
                    assertEquals(w.edges[1], p.edges[3], "E/W match (" + r + "," + c + ")");
                }
            }
        }
    }
}
