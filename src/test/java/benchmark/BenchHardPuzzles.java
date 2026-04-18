package benchmark;

import model.Board;
import model.Piece;
import model.Placement;
import solver.experimental.bitmap.ParallelBitmapSolver;
import util.PuzzleGenerator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Hard stress corpus beyond {@link BenchLargePuzzles}: puzzles where we
 * expect many or most runs to hit the timeout. Useful as a ceiling
 * benchmark — the goal over time is to convert TIMEOUT rows to SOLVED
 * rows, or at least grow the {@code best/total} ratio (pieces placed
 * in the deepest partial).
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>9×10, 10×10, 10×11, 11×11 — generated with palette size
 *       {@code max(rows, cols)} (harder than the {@code -1} used for
 *       the regular corpus).</li>
 *   <li>16×16 Eternity-II-sized puzzle — included for reference even if
 *       we don't expect to solve it; tracks how far the portfolio can
 *       push the best partial.</li>
 * </ul>
 *
 * <p>All solved boards are validated and fingerprinted like
 * {@link BenchLargePuzzles}. On timeout the best partial is measured
 * via {@link Board#calculateScore()} so future optimisations can be
 * compared even when no full solution is found.</p>
 */
public final class BenchHardPuzzles {

    /**
     * Each row: {@code {rows, cols, palette, seedCount}}. Two sections:
     * (1) size ramp — grid grows with a comparable palette size, stresses
     *     the search-tree exponential; (2) palette ramp — grid is fixed
     *     but colour palette grows, stresses piece complexity (fewer
     *     matches per edge → narrower domains, higher branching factor).
     */
    private static final int[][] CASES = {
        // --- Size ramp (palette ≈ max dim) -----------------------------
        {  9,  9,  8, 3 },
        {  9, 10,  9, 2 },
        { 10, 10,  9, 2 },
        { 10, 11, 10, 2 },
        { 11, 11, 10, 2 },
        { 16, 16, 16, 1 },  // real Eternity-II scale, reference only
        // --- Palette ramp at fixed 7×7 (49 cells) ----------------------
        {  7,  7,  4, 2 },
        {  7,  7,  6, 2 },
        {  7,  7,  8, 2 },
        {  7,  7, 10, 2 },
        // --- Palette ramp at fixed 8×8 (64 cells) ----------------------
        {  8,  8,  5, 2 },
        {  8,  8,  7, 2 },
        {  8,  8,  9, 2 },
        {  8,  8, 12, 1 },
    };
    private static final long[] SEEDS = {1L, 17L, 42L, 101L, 404L};
    private static final long PER_RUN_TIMEOUT_MS = 1_800_000; // 30 min

    public static void main(String[] args) {
        System.out.printf("%-10s %-7s %-5s %-13s %-10s %-12s %-20s%n",
            "dim", "palette", "seed", "time_ms", "status", "best", "info");
        for (int[] spec : CASES) {
            int rows = spec[0];
            int cols = spec[1];
            int palette = spec[2];
            int seedCount = Math.min(spec[3], SEEDS.length);
            int total = rows * cols;
            int maxEdges = (rows - 1) * cols + rows * (cols - 1);
            for (int si = 0; si < seedCount; si++) {
                long seed = SEEDS[si];
                Map<Integer, Piece> pieces = PuzzleGenerator.generate(rows, cols, palette, seed);
                Board b = new Board(rows, cols);
                ParallelBitmapSolver solver = new ParallelBitmapSolver();
                solver.setMaxExecutionTime(PER_RUN_TIMEOUT_MS);
                long t = System.nanoTime();
                boolean ok;
                try {
                    ok = solver.solve(b, new HashMap<>(pieces));
                } catch (Throwable e) {
                    ok = false;
                }
                long ms = (System.nanoTime() - t) / 1_000_000L;

                String status;
                String bestStr;
                String info;
                if (ok) {
                    String validation = validate(b, pieces);
                    if (validation != null) {
                        status = "INVALID";
                        bestStr = total + "/" + total;
                        info = validation;
                    } else {
                        status = "SOLVED";
                        bestStr = total + "/" + total;
                        info = "fp=" + String.format("%016x", fingerprint(b));
                    }
                } else {
                    status = "TIMEOUT";
                    int best = solver.getBestDepth();
                    bestStr = best + "/" + total;
                    int[] score = b.calculateScore();
                    info = "partial_edges=" + score[0] + "/" + maxEdges;
                }

                System.out.printf("%-10s %-7d %-5d %-13d %-10s %-12s %-20s%n",
                    rows + "x" + cols, palette, seed, ms, status, bestStr, info);
            }
        }
    }

    private static String validate(Board b, Map<Integer, Piece> pieces) {
        int rows = b.getRows();
        int cols = b.getCols();
        boolean[] used = new boolean[pieces.size() + 1];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (b.isEmpty(r, c)) return "empty cell " + r + "," + c;
                Placement p = b.getPlacement(r, c);
                int id = p.getPieceId();
                if (id < 1 || id >= used.length) return "bad pieceId " + id;
                if (used[id]) return "piece " + id + " used twice";
                used[id] = true;
                if (r == 0 && p.edges[0] != 0) return "north border not 0 at " + r + "," + c;
                if (r == rows - 1 && p.edges[2] != 0) return "south border not 0 at " + r + "," + c;
                if (c == 0 && p.edges[3] != 0) return "west border not 0 at " + r + "," + c;
                if (c == cols - 1 && p.edges[1] != 0) return "east border not 0 at " + r + "," + c;
                if (r > 0 && b.getPlacement(r - 1, c).edges[2] != p.edges[0])
                    return "N/S mismatch at " + r + "," + c;
                if (c > 0 && b.getPlacement(r, c - 1).edges[1] != p.edges[3])
                    return "E/W mismatch at " + r + "," + c;
            }
        }
        for (int i = 1; i < used.length; i++) {
            if (!used[i]) return "piece " + i + " never placed";
        }
        return null;
    }

    private static long fingerprint(Board b) {
        long h = 0xCBF29CE484222325L;
        int rows = b.getRows();
        int cols = b.getCols();
        int[] buf = new int[rows * cols * 3];
        int k = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Placement p = b.getPlacement(r, c);
                buf[k++] = r * cols + c;
                buf[k++] = p.getPieceId();
                buf[k++] = p.getRotation();
            }
        }
        for (int v : buf) {
            h ^= v;
            h *= 0x100000001B3L;
        }
        return h ^ Arrays.hashCode(buf);
    }
}
