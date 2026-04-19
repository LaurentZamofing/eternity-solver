package benchmark;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.PuzzleFactory;
import util.PuzzleGenerator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Robust ablation bench — reduces single-run noise by:
 *   1. JIT warmup: solves a 4×4 easy puzzle N times before timing starts.
 *   2. Multiple seeds per size (5 seeds).
 *   3. Multiple repeats per (config, size, seed) triple — reports
 *      min/median/mean over the repeats to identify outliers.
 *   4. Explicit {@code System.gc()} between repeats.
 *
 * Per config, prints one row per size with aggregated stats.
 */
public final class BenchMainSolverRobust {

    private static final int[] SIZES = {5, 6};
    private static final long[] SEEDS = {1L, 17L, 42L, 101L, 404L};
    private static final int REPEATS = 3;
    private static final int WARMUP_SOLVES = 5;
    private static final long PER_RUN_TIMEOUT_MS = 30_000;

    public static void main(String[] args) {
        warmup();
        System.out.printf("%-28s %-4s %-9s %-9s %-9s %-9s %-4s%n",
            "config", "size", "min_ms", "med_ms", "mean_ms", "p90_ms", "solv");
        runConfig("default (features ON)", b -> {});
        runConfig("LCV ON (precomputed)",  b -> b.sortOrder("lcv"));
        runConfig("features OFF",          b -> b.useColorBudget(false).usePreCheckLookahead(false).useNogoods(false));
        runConfig("LCV + features OFF",    b -> b.sortOrder("lcv").useColorBudget(false).usePreCheckLookahead(false).useNogoods(false));
    }

    /** JIT warmup: solve a small curated puzzle a few times so the hot
     *  code paths are compiled before we start measuring. */
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

    private static void runConfig(String label, Consumer<solver.EternitySolverBuilder> tuner) {
        for (int size : SIZES) {
            int palette = Math.max(4, size - 1);
            long[] samples = new long[SEEDS.length * REPEATS];
            int solvedTotal = 0;
            int sampleIdx = 0;
            for (long seed : SEEDS) {
                Map<Integer, Piece> pieces = PuzzleGenerator.generate(size, palette, seed);
                for (int rep = 0; rep < REPEATS; rep++) {
                    System.gc();
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
                    // On timeout we record the full elapsed (useful for p90)
                    samples[sampleIdx++] = ok ? dt : PER_RUN_TIMEOUT_MS;
                    if (ok) solvedTotal++;
                }
            }
            Arrays.sort(samples);
            long min = samples[0];
            long median = samples[samples.length / 2];
            long p90 = samples[(int) Math.floor(samples.length * 0.9)];
            long sum = 0; for (long v : samples) sum += v;
            long mean = sum / samples.length;
            int totalRuns = SEEDS.length * REPEATS;
            System.out.printf("%-28s %dx%d %-9d %-9d %-9d %-9d %d/%d%n",
                label, size, size, min, median, mean, p90, solvedTotal, totalRuns);
        }
    }
}
