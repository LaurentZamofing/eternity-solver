package benchmark;

import model.Board;
import model.Piece;
import solver.experimental.bitmap.BitmapSolver;
import solver.experimental.bitmap.ParallelBitmapSolver;
import util.PuzzleGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * Comparative bench: single-threaded {@link BitmapSolver} (P2 defaults) vs
 * {@link ParallelBitmapSolver} (4-worker portfolio) on generated 5×5 → 7×7
 * puzzles, 3 seeds each. Goal: confirm the portfolio resolves the P2 seed=1
 * regression observed on 7×7.
 */
public final class ParallelBitmapBench {

    private static final int[] SIZES = {5, 6, 7};
    private static final long[] SEEDS = {1L, 17L, 42L};
    private static final long PER_RUN_TIMEOUT_MS = 60_000;

    public static void main(String[] args) {
        System.out.printf("%-5s %-5s %-12s %-12s %-8s%n",
            "size", "seed", "single_ms", "parallel_ms", "speedup");
        for (int size : SIZES) {
            int palette = Math.max(4, size - 1);
            for (long seed : SEEDS) {
                Map<Integer, Piece> pieces = PuzzleGenerator.generate(size, palette, seed);

                long singleMs = timeSolve(() -> runSingle(size, pieces));
                long parMs    = timeSolve(() -> runParallel(size, pieces));

                String speedup = singleMs > 0 && parMs > 0
                    ? String.format("%.1fx", (double) singleMs / parMs)
                    : "-";
                System.out.printf("%-5d %-5d %-12d %-12d %-8s%n",
                    size, seed, singleMs, parMs, speedup);
            }
        }
    }

    private static long timeSolve(Runnable r) {
        long t = System.nanoTime();
        try { r.run(); return (System.nanoTime() - t) / 1_000_000L; }
        catch (Throwable e) { return -1; }
    }

    private static void runSingle(int size, Map<Integer, Piece> pieces) {
        Board b = new Board(size, size);
        BitmapSolver solver = new BitmapSolver();
        solver.setMaxExecutionTime(PER_RUN_TIMEOUT_MS);
        solver.solve(b, new HashMap<>(pieces));
    }

    private static void runParallel(int size, Map<Integer, Piece> pieces) {
        Board b = new Board(size, size);
        ParallelBitmapSolver solver = new ParallelBitmapSolver();
        solver.setMaxExecutionTime(PER_RUN_TIMEOUT_MS);
        solver.solve(b, new HashMap<>(pieces));
    }
}
