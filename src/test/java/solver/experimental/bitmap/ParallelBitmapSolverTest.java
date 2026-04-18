package solver.experimental.bitmap;

import model.Board;
import model.Piece;
import model.Placement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import util.PuzzleFactory;
import util.PuzzleGenerator;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ParallelBitmapSolver — portfolio P3")
class ParallelBitmapSolverTest {

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("solves curated 3×3")
    void solve3x3() {
        Board board = new Board(3, 3);
        ParallelBitmapSolver solver = new ParallelBitmapSolver();
        solver.setMaxExecutionTime(20_000);
        assertTrue(solver.solve(board, PuzzleFactory.createExample3x3()));
        assertValidBoard(board);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("solves curated 4×4 hardV3")
    void solve4x4Hard() {
        Board board = new Board(4, 4);
        ParallelBitmapSolver solver = new ParallelBitmapSolver();
        solver.setMaxExecutionTime(40_000);
        assertTrue(solver.solve(board, PuzzleFactory.createExample4x4HardV3()));
        assertValidBoard(board);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("solves generated 6×6 seed=42 (P1 single-thread 493 ms)")
    void solveGenerated6x6() {
        Map<Integer, Piece> pieces = PuzzleGenerator.generate(6, 5, 42L);
        Board board = new Board(6, 6);
        ParallelBitmapSolver solver = new ParallelBitmapSolver();
        solver.setMaxExecutionTime(30_000);
        assertTrue(solver.solve(board, pieces), "6×6 generated must solve");
        assertValidBoard(board);
    }

    @Test
    @DisplayName("single-thread mode still works (threads=1)")
    void singleThread() {
        Board board = new Board(4, 4);
        ParallelBitmapSolver solver = new ParallelBitmapSolver();
        solver.setThreads(1);
        solver.setMaxExecutionTime(30_000);
        assertTrue(solver.solve(board, PuzzleFactory.createExample4x4Easy()));
        assertValidBoard(board);
    }

    private static void assertValidBoard(Board board) {
        int rows = board.getRows();
        int cols = board.getCols();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                assertFalse(board.isEmpty(r, c), "cell (" + r + "," + c + ") filled");
                Placement p = board.getPlacement(r, c);
                if (r == 0) assertEquals(0, p.edges[0]);
                if (r == rows - 1) assertEquals(0, p.edges[2]);
                if (c == 0) assertEquals(0, p.edges[3]);
                if (c == cols - 1) assertEquals(0, p.edges[1]);
                if (r > 0) {
                    Placement n = board.getPlacement(r - 1, c);
                    assertEquals(n.edges[2], p.edges[0]);
                }
                if (c > 0) {
                    Placement w = board.getPlacement(r, c - 1);
                    assertEquals(w.edges[1], p.edges[3]);
                }
            }
        }
    }
}
