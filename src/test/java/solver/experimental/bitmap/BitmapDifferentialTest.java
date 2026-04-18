package solver.experimental.bitmap;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import solver.EternitySolver;
import util.PuzzleGenerator;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Differential oracle: for a handful of generated puzzles at each size, run
 * both {@link solver.EternitySolver} and {@link BitmapSolver} on the same
 * inputs and assert they agree on whether a solution exists (both "solved"
 * or both "not solved" — each may find a different valid layout, that's OK).
 *
 * <p>This pins the bitmap solver's correctness against the trusted
 * reference engine. P1 gate is "differential green on 3×3/4×4/5×5".</p>
 */
@DisplayName("BitmapSolver — differential oracle vs EternitySolver")
class BitmapDifferentialTest {

    private static final long[] SEEDS = {1L, 7L, 42L, 100L, 777L};

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("3×3 generated: agree on all 5 seeds")
    void differential3x3() {
        for (long seed : SEEDS) {
            checkAgrees(3, 4, seed);
        }
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    @DisplayName("4×4 generated: agree on all 5 seeds")
    void differential4x4() {
        for (long seed : SEEDS) {
            checkAgrees(4, 5, seed);
        }
    }

    @Test
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    @DisplayName("5×5 generated: agree on all 5 seeds")
    void differential5x5() {
        for (long seed : SEEDS) {
            checkAgrees(5, 5, seed);
        }
    }

    private void checkAgrees(int size, int colours, long seed) {
        Map<Integer, Piece> pieces = PuzzleGenerator.generate(size, colours, seed);

        Board b1 = new Board(size, size);
        EternitySolver ref = new EternitySolver();
        ref.setVerbose(false);
        ref.setMaxExecutionTime(30_000);
        ref.setSymmetryBreakingFlags(false, false);
        boolean refSolved = ref.solve(b1, new java.util.HashMap<>(pieces));

        Board b2 = new Board(size, size);
        BitmapSolver bm = new BitmapSolver();
        bm.setMaxExecutionTime(30_000);
        boolean bmSolved = bm.solve(b2, new java.util.HashMap<>(pieces));

        assertEquals(refSolved, bmSolved,
            "disagreement on " + size + "×" + size + " seed=" + seed
            + " (ref=" + refSolved + ", bitmap=" + bmSolved + ")");

        if (bmSolved) {
            assertValidBoard(b2, size);
        }
    }

    private static void assertValidBoard(Board board, int size) {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                assertFalse(board.isEmpty(r, c),
                    "(" + r + "," + c + ") filled");
                model.Placement p = board.getPlacement(r, c);
                if (r == 0) assertEquals(0, p.edges[0]);
                if (r == size - 1) assertEquals(0, p.edges[2]);
                if (c == 0) assertEquals(0, p.edges[3]);
                if (c == size - 1) assertEquals(0, p.edges[1]);
                if (r > 0)
                    assertEquals(board.getPlacement(r - 1, c).edges[2], p.edges[0]);
                if (c > 0)
                    assertEquals(board.getPlacement(r, c - 1).edges[1], p.edges[3]);
            }
        }
    }
}
