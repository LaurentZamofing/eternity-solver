package benchmark;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.PuzzleGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Ablation bench on the main EternitySolver — isolates which of the
 * ultraplan features is responsible for the 6×6 slowdown observed in
 * BenchmarkGrid (baseline 25 s → all-features-on 98 s).
 *
 * <p>Runs 5×5 and 6×6 with 3 seeds per size and a 60 s per-run timeout,
 * toggling each feature off in turn while the others stay on.</p>
 */
public final class BenchMainSolverAblation {

    private static final int[] SIZES = {5, 6};
    private static final long[] SEEDS = {1L, 17L, 42L};
    private static final long PER_RUN_TIMEOUT_MS = 60_000;

    public static void main(String[] args) {
        System.out.printf("%-28s %-4s %-10s %-10s %-4s%n",
            "config", "size", "avg_ms", "avg_solved", "cnt");
        runConfig("all-on (baseline)", b -> {});
        runConfig("colorBudget OFF",   b -> b.useColorBudget(false));
        runConfig("lookahead OFF",     b -> b.usePreCheckLookahead(false));
        runConfig("nogoods OFF",       b -> b.useNogoods(false));
        runConfig("all OFF",           b -> b.useColorBudget(false).usePreCheckLookahead(false).useNogoods(false));
    }

    private static void runConfig(String label, Consumer<solver.EternitySolverBuilder> tuner) {
        for (int size : SIZES) {
            int palette = Math.max(4, size - 1);
            long total = 0; int solved = 0;
            for (long seed : SEEDS) {
                Map<Integer, Piece> pieces = PuzzleGenerator.generate(size, palette, seed);
                Board b = new Board(size, size);
                solver.EternitySolverBuilder builder = EternitySolver.builder();
                builder.verbose(false)
                       .maxExecutionTime(PER_RUN_TIMEOUT_MS)
                       .symmetryBreakingFlags(false, false);
                tuner.accept(builder);
                EternitySolver solver = builder.build();
                long t = System.currentTimeMillis();
                boolean ok = solver.solve(b, new HashMap<>(pieces));
                long dt = System.currentTimeMillis() - t;
                if (ok) { solved++; total += dt; }
            }
            long avg = solved > 0 ? total / solved : -1;
            System.out.printf("%-28s %dx%d %-10d %-10d %d/3%n",
                label, size, size, avg, solved, solved);
        }
    }
}
