package benchmark;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import solver.experimental.bitmap.BitmapSolver;
import util.PuzzleGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * Comparative bench: {@link EternitySolver} vs {@link BitmapSolver} across
 * generated 3×3 → 7×7 puzzles with 3 seeds each.
 *
 * <p>Not a JUnit test — invoke as {@code java benchmark.BitmapBench}.</p>
 */
public final class BitmapBench {

    private static final int[] SIZES = {3, 4, 5, 6, 7};
    private static final long[] SEEDS = {1L, 17L, 42L};
    private static final long PER_RUN_TIMEOUT_MS = 60_000;

    public static void main(String[] args) {
        System.out.printf("%-8s %-8s %-14s %-14s %-8s%n", "size", "seed", "ref_ms", "bitmap_ms", "speedup");
        for (int size : SIZES) {
            int palette = Math.max(4, size - 1);
            for (long seed : SEEDS) {
                Map<Integer, Piece> pieces = PuzzleGenerator.generate(size, palette, seed);

                long refMs = timeSolve(() -> runRef(size, pieces));
                long bmMs  = timeSolve(() -> runBitmap(size, pieces));

                String speedup = refMs > 0 && bmMs > 0
                    ? String.format("%.1fx", (double) refMs / bmMs)
                    : "-";
                System.out.printf("%-8d %-8d %-14d %-14d %-8s%n",
                    size, seed, refMs, bmMs, speedup);
            }
        }
    }

    private static long timeSolve(Runnable r) {
        long t = System.nanoTime();
        try { r.run(); return (System.nanoTime() - t) / 1_000_000L; }
        catch (Throwable e) { return -1; }
    }

    private static void runRef(int size, Map<Integer, Piece> pieces) {
        Board b = new Board(size, size);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(PER_RUN_TIMEOUT_MS);
        solver.setSymmetryBreakingFlags(false, false);
        solver.solve(b, new HashMap<>(pieces));
    }

    private static void runBitmap(int size, Map<Integer, Piece> pieces) {
        Board b = new Board(size, size);
        BitmapSolver solver = new BitmapSolver();
        solver.setMaxExecutionTime(PER_RUN_TIMEOUT_MS);
        solver.solve(b, new HashMap<>(pieces));
    }
}
