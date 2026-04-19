package benchmark;

import model.Board;
import model.Piece;
import solver.experimental.bitmap.ParallelBitmapSolver;
import util.PuzzleGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * A/B bench for the fail-first MRV tiebreaker
 * ({@link solver.experimental.bitmap.BitmapSolver#setUseFailFirst}).
 *
 * <p>Runs the 3 hardest TIMEOUT cases with the heuristic off vs on.
 * Either ON breaks a wall that OFF can't — real algorithmic win — or
 * the gain is marginal and we move on to the next candidate (AC-3
 * worklist, LCV, corner-anchor sym breaking, CP-SAT).</p>
 */
public final class BenchFailFirst {

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

    private static Result run(int rows, int cols, Map<Integer, Piece> pieces, boolean failFirst) {
        Board b = new Board(rows, cols);
        ParallelBitmapSolver solver = new ParallelBitmapSolver();
        solver.setMaxExecutionTime(PER_RUN_TIMEOUT_MS);
        solver.setUseFailFirst(failFirst);
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
