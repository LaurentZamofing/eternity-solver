package benchmark;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import solver.EternitySolverBuilder;
import util.PuzzleFactory;
import util.PuzzleGenerator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Per-algorithm isolation bench — for each algo in the audit, runs a
 * puzzle + config that forces that algo to dominate the hot path, then
 * compares ON vs OFF to quantify its contribution.
 *
 * <p>Unlike the ablation bench (which tests 4 combined configs on every
 * puzzle size), this bench couples each config with the puzzle where
 * the tested algo has its strongest effect. That avoids diluting a real
 * contribution in puzzles where the algo barely fires.</p>
 *
 * <p>Protocol per case: 5 JIT warmup solves + 3 seeds × 3 repeats.
 * Reports ON median vs OFF median — ratio &gt; 1 means the algo helps.</p>
 */
public final class BenchAlgoIsolation {

    private static final long[] SEEDS = {1L, 17L, 42L};
    private static final int REPEATS = 3;
    private static final int WARMUP_SOLVES = 5;

    public static void main(String[] args) {
        warmup();
        System.out.printf("%-46s %-8s %-10s %-10s %-9s %-4s%n",
            "algo / case", "budget", "OFF_med", "ON_med", "speedup", "solv");

        // #1 AC-3 — turn OFF AC-3 entirely, keep piece unicity. Should be massively worse.
        runCase("AC-3 propagation (6×6/p5)", 6, 5, 120_000,
            off -> off.useAC3(false).useNogoods(false).useColorBudget(false).usePreCheckLookahead(false),
            on  -> on.useNogoods(false).useColorBudget(false).usePreCheckLookahead(false));

        // #4 Color-budget alone — AC-3 off (so color budget is the only non-trivial prune)
        runCase("Color-budget only (6×6/p5)", 6, 5, 120_000,
            off -> off.useAC3(false).useColorBudget(false).useNogoods(false).usePreCheckLookahead(false),
            on  -> on.useAC3(false).useColorBudget(true).useNogoods(false).usePreCheckLookahead(false));

        // #5 Pre-commit lookahead alone — AC-3 off so lookahead actually does work (vs being duplicate of AC-3)
        runCase("Lookahead only (5×5/p4)", 5, 4, 30_000,
            off -> off.useAC3(false).usePreCheckLookahead(false).useNogoods(false).useColorBudget(false),
            on  -> on.useAC3(false).usePreCheckLookahead(true).useNogoods(false).useColorBudget(false));

        // #6 Zobrist nogoods alone — with AC-3 on (nogoods are a fast-path shortcut)
        runCase("Nogoods only (6×6/p5)", 6, 5, 120_000,
            off -> off.useNogoods(false).useColorBudget(false).usePreCheckLookahead(false),
            on  -> on.useNogoods(true).useColorBudget(false).usePreCheckLookahead(false));

        // #3 LCV on a small puzzle where it helped in single-run bench
        runCase("LCV ordering (5×5/p4)", 5, 4, 30_000,
            off -> off,
            on  -> on.sortOrder("lcv"));

        // All features stacked vs pure AC-3 — headline net gain
        runCase("All features stacked (6×6/p5)", 6, 5, 180_000,
            off -> off.useNogoods(false).useColorBudget(false).usePreCheckLookahead(false),
            on  -> on);
    }

    private static void warmup() {
        Map<Integer, Piece> easy = PuzzleFactory.createExample4x4Easy();
        for (int i = 0; i < WARMUP_SOLVES; i++) {
            EternitySolver s = EternitySolver.builder()
                .verbose(false).maxExecutionTime(10_000)
                .symmetryBreakingFlags(false, false).build();
            s.solve(new Board(4, 4), new HashMap<>(easy));
        }
        System.gc();
    }

    private static void runCase(String label, int rows, int palette, long timeoutMs,
                                Consumer<EternitySolverBuilder> off,
                                Consumer<EternitySolverBuilder> on) {
        long[] offSamples = sampleFor(rows, rows, palette, timeoutMs, off);
        long[] onSamples  = sampleFor(rows, rows, palette, timeoutMs, on);
        long offMed = median(offSamples);
        long onMed  = median(onSamples);
        double speedup = (onMed > 0) ? (double) offMed / onMed : 0.0;
        int offSolv = countSolved(offSamples, timeoutMs);
        int onSolv  = countSolved(onSamples,  timeoutMs);
        int total = offSamples.length;
        System.out.printf("%-46s %-8d %-10d %-10d %-9s %d|%d/%d%n",
            label, timeoutMs / 1000, offMed, onMed,
            String.format("%.2fx", speedup), offSolv, onSolv, total);
    }

    private static long[] sampleFor(int rows, int cols, int palette, long timeoutMs,
                                    Consumer<EternitySolverBuilder> tuner) {
        long[] samples = new long[SEEDS.length * REPEATS];
        int idx = 0;
        for (long seed : SEEDS) {
            Map<Integer, Piece> pieces = PuzzleGenerator.generate(rows, cols, palette, seed);
            for (int r = 0; r < REPEATS; r++) {
                System.gc();
                Board b = new Board(rows, cols);
                EternitySolverBuilder builder = EternitySolver.builder();
                builder.verbose(false)
                       .maxExecutionTime(timeoutMs)
                       .symmetryBreakingFlags(false, false);
                tuner.accept(builder);
                EternitySolver solver = builder.build();
                long t = System.currentTimeMillis();
                boolean ok;
                try {
                    ok = solver.solve(b, new HashMap<>(pieces));
                } catch (Throwable e) {
                    ok = false;
                }
                long dt = System.currentTimeMillis() - t;
                samples[idx++] = ok ? dt : timeoutMs;
            }
        }
        return samples;
    }

    private static long median(long[] samples) {
        long[] copy = samples.clone();
        Arrays.sort(copy);
        return copy[copy.length / 2];
    }

    private static int countSolved(long[] samples, long timeoutMs) {
        int n = 0;
        for (long v : samples) if (v < timeoutMs) n++;
        return n;
    }
}
