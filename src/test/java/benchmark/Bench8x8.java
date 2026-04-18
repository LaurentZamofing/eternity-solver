package benchmark;

import model.Board;
import model.Piece;
import solver.experimental.bitmap.ParallelBitmapSolver;
import util.PuzzleGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * 8×8 stress bench — the target size in ULTRA_PLAN. 3 seeds, 120 s timeout
 * per run, ParallelBitmapSolver (4 workers).
 */
public final class Bench8x8 {

    private static final long[] SEEDS = {1L, 17L, 42L};
    private static final long PER_RUN_TIMEOUT_MS = 120_000;

    public static void main(String[] args) {
        System.out.printf("%-5s %-5s %-14s %-10s%n", "size", "seed", "parallel_ms", "status");
        for (long seed : SEEDS) {
            Map<Integer, Piece> pieces = PuzzleGenerator.generate(8, 7, seed);
            long t = System.nanoTime();
            boolean ok;
            try {
                Board b = new Board(8, 8);
                ParallelBitmapSolver solver = new ParallelBitmapSolver();
                solver.setMaxExecutionTime(PER_RUN_TIMEOUT_MS);
                ok = solver.solve(b, new HashMap<>(pieces));
            } catch (Throwable e) {
                ok = false;
            }
            long ms = (System.nanoTime() - t) / 1_000_000L;
            System.out.printf("%-5d %-5d %-14d %-10s%n",
                8, seed, ms, ok ? "SOLVED" : "TIMEOUT");
        }
    }
}
