package benchmark;

import model.Board;
import model.Piece;
import solver.experimental.bitmap.ParallelBitmapSolver;
import util.PuzzleGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * A/B bench for the shared-nogood-store feature
 * ({@code setShareNogoods(true/false)}).
 *
 * <p>Runs the same puzzle twice, once with per-worker nogood caches and
 * once with a single shared, sharded store. Pathological 8×8 seeds that
 * normally time out are the headline test — the gain should show as
 * (a) more seeds solving and/or (b) higher best-partial depth at
 * timeout.</p>
 */
public final class BenchSharedNogoods {

    /** {@code {rows, cols, seed}} — three hardest cases from the reference
     *  corpus where the 60-min retry bench showed a real wall (8×8 seed=42,
     *  8×9 seed=17, 9×9 seed=1). A/B-comparing shared vs per-worker nogood
     *  caches here is the direct test of whether shared nogoods help on
     *  pathological puzzles. */
    private static final long[][] HARD_CASES = {
        { 8, 8, 42L },
        { 8, 9, 17L },
        { 9, 9,  1L },
    };
    private static final long PER_RUN_TIMEOUT_MS = 1_800_000; // 30 min

    public static void main(String[] args) {
        System.out.printf("%-6s %-5s %-12s %-10s %-10s %-12s %-10s %-10s %-10s%n",
            "dim", "seed", "off_ms", "off_st", "off_best", "on_ms", "on_st", "on_best", "note");
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
                double ratio = (double) off.ms / on.ms;
                note = String.format("%.1fx", ratio);
            } else if (!off.solved && on.solved) {
                note = "on_fix";
            } else if (off.solved && !on.solved) {
                note = "on_regress";
            } else {
                note = String.format("best %d→%d", off.bestDepth, on.bestDepth);
            }

            System.out.printf("%-6s %-5d %-12d %-10s %-10s %-12d %-10s %-10s %-10s%n",
                rows + "x" + cols, seed,
                off.ms, off.solved ? "SOLVED" : "TIMEOUT", off.bestDepth + "/" + total,
                on.ms, on.solved ? "SOLVED" : "TIMEOUT", on.bestDepth + "/" + total,
                note);
        }
    }

    private static Result run(int rows, int cols, Map<Integer, Piece> pieces, boolean shared) {
        Board b = new Board(rows, cols);
        ParallelBitmapSolver solver = new ParallelBitmapSolver();
        solver.setMaxExecutionTime(PER_RUN_TIMEOUT_MS);
        solver.setShareNogoods(shared);
        long t = System.nanoTime();
        boolean ok;
        try {
            ok = solver.solve(b, new HashMap<>(pieces));
        } catch (Throwable e) {
            ok = false;
        }
        long ms = (System.nanoTime() - t) / 1_000_000L;
        return new Result(ms, ok, solver.getBestDepth());
    }

    private record Result(long ms, boolean solved, int bestDepth) { }
}
