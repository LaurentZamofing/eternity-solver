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
 * Focused bench for piece-ordering sort variants — tests whether the
 * two new opt-in options from #3 of the audit earn their keep.
 *
 * Sort orders tested (all with features ON):
 *   - (default)  — pieceId ascending
 *   - "lcv"      — static LCV (ascending difficulty, commit 86661f5)
 *   - "lcv-border" — LCV only on border/corner cells (commit f583c9d)
 *   - "mcv"      — inverse of LCV (least constraining first, commit 46c5175)
 *
 * 5 seeds × 2 repeats × 2 sizes (5×5, 6×6) with JIT warmup.
 */
public final class BenchSortOrderVariants {

    private static final int[][] CASES = {
        { 5, 5, 0, 5, 2, 60_000 },
        { 6, 6, 0, 5, 2, 120_000 },
    };
    private static final long[] SEEDS = {1L, 17L, 42L, 101L, 404L};

    public static void main(String[] args) {
        warmup();
        System.out.printf("%-28s %-8s %-9s %-9s %-9s %-9s %-4s%n",
            "sortOrder", "dim×pal", "min_ms", "med_ms", "mean_ms", "p90_ms", "solv");
        runConfig("default (pieceId asc)", b -> {});
        runConfig("lcv",                   b -> b.sortOrder("lcv"));
        runConfig("lcv-border",            b -> b.sortOrder("lcv-border"));
        runConfig("mcv",                   b -> b.sortOrder("mcv"));
    }

    private static void warmup() {
        Map<Integer, Piece> easy = PuzzleFactory.createExample4x4Easy();
        for (int i = 0; i < 5; i++) {
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

            long[] samples = new long[seedCount * repeats];
            int idx = 0, solvedTotal = 0;
            for (int si = 0; si < seedCount; si++) {
                long seed = SEEDS[si];
                Map<Integer, Piece> pieces = PuzzleGenerator.generate(rows, cols, palette, seed);
                for (int r = 0; r < repeats; r++) {
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
                    try { ok = solver.solve(b, new HashMap<>(pieces)); }
                    catch (Throwable e) { ok = false; }
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
            System.out.printf("%-28s %s/p%d    %-9d %-9d %-9d %-9d %d/%d%n",
                label, rows + "x" + cols, palette,
                min, median, mean, p90, solvedTotal, samples.length);
        }
    }
}
