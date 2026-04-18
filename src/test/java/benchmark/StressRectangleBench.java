package benchmark;

import model.Board;
import model.Piece;
import solver.experimental.bitmap.ParallelBitmapSolver;
import util.PuzzleGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * Stress bench on rectangular / large boards: 7×8, 8×8 (re-run with longer
 * timeout), 8×9. ParallelBitmapSolver with 4 workers. 3 seeds per size.
 */
public final class StressRectangleBench {

    private static final int[][] DIMS = { {7, 8}, {8, 8}, {8, 9} };
    private static final long[] SEEDS = {1L, 17L, 42L};
    private static final long PER_RUN_TIMEOUT_MS = 300_000; // 5 min

    public static void main(String[] args) {
        System.out.printf("%-8s %-5s %-14s %-10s%n", "dim", "seed", "parallel_ms", "status");
        for (int[] dim : DIMS) {
            int rows = dim[0];
            int cols = dim[1];
            int palette = Math.max(4, Math.max(rows, cols) - 1);
            for (long seed : SEEDS) {
                Map<Integer, Piece> pieces = PuzzleGenerator.generate(rows, cols, palette, seed);
                long t = System.nanoTime();
                boolean ok;
                try {
                    Board b = new Board(rows, cols);
                    ParallelBitmapSolver solver = new ParallelBitmapSolver();
                    solver.setMaxExecutionTime(PER_RUN_TIMEOUT_MS);
                    ok = solver.solve(b, new HashMap<>(pieces));
                } catch (Throwable e) {
                    ok = false;
                }
                long ms = (System.nanoTime() - t) / 1_000_000L;
                System.out.printf("%-8s %-5d %-14d %-10s%n",
                    rows + "x" + cols, seed, ms, ok ? "SOLVED" : "TIMEOUT");
            }
        }
    }
}
