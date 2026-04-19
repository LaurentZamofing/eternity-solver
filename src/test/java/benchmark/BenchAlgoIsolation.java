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

    /** Minimal config used as the OFF baseline for every isolation test:
     *  AC-3 ON (can't disable without solver exploding on 5×5+), all
     *  ultraplan features OFF, LCV OFF. Each test ON flips exactly one
     *  flag on top of this. */
    private static final Consumer<EternitySolverBuilder> MINIMAL = b ->
        b.useNogoods(false).useColorBudget(false).usePreCheckLookahead(false);

    public static void main(String[] args) {
        warmup();
        System.out.printf("%-46s %-8s %-10s %-10s %-9s %-4s%n",
            "algo / case", "budget", "OFF_med", "ON_med", "speedup", "solv");

        // Isolation tests on 5×5/p4 where MINIMAL still solves in budget.
        // Each flips exactly one flag vs MINIMAL (AC-3 + nothing else).
        runCase("Color-budget (5×5/p4)", 5, 4, 60_000,
            MINIMAL,
            MINIMAL.andThen(b -> b.useColorBudget(true)));

        runCase("Pre-commit lookahead (5×5/p4)", 5, 4, 60_000,
            MINIMAL,
            MINIMAL.andThen(b -> b.usePreCheckLookahead(true)));

        runCase("Zobrist nogoods (5×5/p4)", 5, 4, 60_000,
            MINIMAL,
            MINIMAL.andThen(b -> b.useNogoods(true)));

        runCase("LCV ordering (5×5/p4)", 5, 4, 60_000,
            MINIMAL,
            MINIMAL.andThen(b -> b.sortOrder("lcv")));

        // Stacked headline tests on 6×6 — where MINIMAL times out but the
        // features ON finish. Speedup here is "unblock" rather than "faster".
        runCase("Features ON vs MINIMAL (6×6/p5)", 6, 5, 120_000,
            MINIMAL,
            b -> {}); // defaults = all features ON

        // LCV on 6×6 with full features — known regression (from ablation2)
        runCase("LCV vs pieceId (6×6/p5, features ON)", 6, 5, 120_000,
            b -> {},
            b -> { b.sortOrder("lcv"); });
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
