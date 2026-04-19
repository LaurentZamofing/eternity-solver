package benchmark;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.PuzzleFactory;
import util.PuzzleGenerator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Robust ablation bench — larger samples + per-size timeouts + puzzle
 * variety (curated hard, generated with 2 palette levels) to produce
 * measurements that don't drown small optimisations in noise.
 *
 * Protocol per config:
 *   - 5 JIT warmup solves on a 4×4 easy puzzle (not timed)
 *   - For each (size, palette, seed) — REPEATS runs with System.gc() between
 *   - Report min / median / mean / p90 over the samples
 *   - Timeouts counted as the full budget (so p90 surfaces "near-timeout" runs)
 *
 * Per-size timeouts let small boards finish fast while big boards
 * get the budget they actually need — 6×6 timeouts at 30 s were
 * clipping real solves.
 */
public final class BenchMainSolverRobust {

    /** {@code {rows, cols, paletteOffset, seedCount, repeats, timeout_ms}}.
     *  Palette is {@code max(4, max(rows,cols) - 1) + paletteOffset}. */
    private static final int[][] CASES = {
        // 5×5 — two difficulties to catch easy/hard regressions distinctly
        { 5, 5, 0, 8, 2, 30_000 },  // standard palette (=4)
        { 5, 5, 2, 5, 2, 60_000 },  // harder palette (=6)
        // 6×6 — bump timeout to 120 s so near-timeout seeds complete
        { 6, 6, 0, 8, 2, 120_000 }, // standard palette (=5)
        { 6, 6, 2, 5, 2, 180_000 }, // harder palette (=7)
        // 7×7 at long budget — only 3 seeds to keep runtime sane
        { 7, 7, 0, 3, 1, 300_000 },
    };
    private static final long[] SEEDS = {1L, 17L, 42L, 101L, 404L, 777L, 1337L, 9999L};
    private static final int WARMUP_SOLVES = 5;

    public static void main(String[] args) {
        warmup();
        System.out.printf("%-28s %-8s %-9s %-9s %-9s %-9s %-8s %-6s%n",
            "config", "dim×pal", "min_ms", "med_ms", "mean_ms", "p90_ms", "budget", "solv");
        runConfig("default (features ON)", b -> {});
        runConfig("LCV ON (precomputed)",  b -> b.sortOrder("lcv"));
        runConfig("features OFF",          b -> b.useColorBudget(false).usePreCheckLookahead(false).useNogoods(false));
        runConfig("LCV + features OFF",    b -> b.sortOrder("lcv").useColorBudget(false).usePreCheckLookahead(false).useNogoods(false));
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

    private static void runConfig(String label, Consumer<solver.EternitySolverBuilder> tuner) {
        for (int[] caseSpec : CASES) {
            int rows = caseSpec[0], cols = caseSpec[1];
            int paletteOffset = caseSpec[2];
            int seedCount = Math.min(caseSpec[3], SEEDS.length);
            int repeats = caseSpec[4];
            long timeoutMs = caseSpec[5];
            int palette = Math.max(4, Math.max(rows, cols) - 1) + paletteOffset;

            int sampleCount = seedCount * repeats;
            long[] samples = new long[sampleCount];
            int solvedTotal = 0;
            int idx = 0;
            for (int si = 0; si < seedCount; si++) {
                long seed = SEEDS[si];
                Map<Integer, Piece> pieces = PuzzleGenerator.generate(rows, cols, palette, seed);
                for (int rep = 0; rep < repeats; rep++) {
                    System.gc();
                    Board b = new Board(rows, cols);
                    solver.EternitySolverBuilder builder = EternitySolver.builder();
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
                    if (ok) solvedTotal++;
                }
            }
            Arrays.sort(samples);
            long min = samples[0];
            long median = samples[samples.length / 2];
            long p90 = samples[(int) Math.floor(samples.length * 0.9)];
            long sum = 0; for (long v : samples) sum += v;
            long mean = sum / samples.length;
            String dim = rows + "x" + cols + "/p" + palette;
            System.out.printf("%-28s %-8s %-9d %-9d %-9d %-9d %-8d %d/%d%n",
                label, dim, min, median, mean, p90, timeoutMs / 1000, solvedTotal, sampleCount);
        }
    }
}
