package benchmark;

import model.Board;
import model.Piece;
import solver.experimental.bitmap.ParallelBitmapSolver;
import util.PuzzleGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * A/B bench for the singleton propagation enhancement (commit 5f4e97b):
 * {@link solver.experimental.bitmap.BitmapSolver#setUseSingletonProp}.
 *
 * <p>Runs the 3 hardest TIMEOUT cases with singleton-prop off vs on,
 * 60 min each side. Reports best-depth <b>and</b> time-to-best so
 * "same record reached faster" surfaces as a win.</p>
 */
public final class BenchSingletonProp {

    private static final long[][] HARD_CASES = {
        { 8, 8, 42L },
        { 8, 9, 17L },
        { 9, 9,  1L },
    };
    private static final long PER_RUN_TIMEOUT_MS = 3_600_000; // 60 min

    public static void main(String[] args) {
        System.out.printf("%-6s %-5s %-10s %-9s %-10s %-10s %-9s %-10s %-12s%n",
            "dim", "seed", "off_best", "off_tbst", "off_st",
                            "on_best",  "on_tbst",  "on_st", "note");
        for (long[] spec : HARD_CASES) {
            int rows = (int) spec[0];
            int cols = (int) spec[1];
            long seed = spec[2];
            int palette = Math.max(4, Math.max(rows, cols) - 1);
            int total = rows * cols;

            Map<Integer, Piece> pieces = PuzzleGenerator.generate(rows, cols, palette, seed);

            Result off = run(rows, cols, pieces, false);
            Result on  = run(rows, cols, pieces, true);

            String note;
            if (off.solved && on.solved) {
                note = String.format("%.1fx", (double) off.ms / on.ms);
            } else if (!off.solved && on.solved) {
                note = "on_fix";
            } else if (off.solved && !on.solved) {
                note = "on_regress";
            } else if (off.bestDepth == on.bestDepth && off.timeToBest > 0 && on.timeToBest > 0) {
                note = String.format("t_best %.1fx", (double) off.timeToBest / on.timeToBest);
            } else {
                note = String.format("best %d→%d", off.bestDepth, on.bestDepth);
            }

            System.out.printf("%-6s %-5d %-10s %-9d %-10s %-10s %-9d %-10s %-12s%n",
                rows + "x" + cols, seed,
                off.bestDepth + "/" + total, off.timeToBest, off.solved ? "SOLVED" : "TIMEOUT",
                on.bestDepth + "/" + total,  on.timeToBest,  on.solved  ? "SOLVED" : "TIMEOUT",
                note);
        }
    }

    private static Result run(int rows, int cols, Map<Integer, Piece> pieces, boolean singleton) {
        Board b = new Board(rows, cols);
        ParallelBitmapSolver solver = new ParallelBitmapSolver();
        solver.setMaxExecutionTime(PER_RUN_TIMEOUT_MS);
        solver.setUseSingletonProp(singleton);
        long t = System.nanoTime();
        boolean ok;
        try {
            ok = solver.solve(b, new HashMap<>(pieces));
        } catch (Throwable e) {
            ok = false;
        }
        long ms = (System.nanoTime() - t) / 1_000_000L;
        return new Result(ms, ok, solver.getBestDepth(), solver.getTimeToBestMs());
    }

    private record Result(long ms, boolean solved, int bestDepth, long timeToBest) { }
}
