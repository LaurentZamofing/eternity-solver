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
 * Re-runs the subset of {@link BenchLargePuzzles} cases that timed out
 * at the 10-min limit with the new 30-min budget — the cheapest way to
 * learn how much of the timeout wall was "really hard" vs "just needed
 * more budget".
 *
 * <p>The {@code TIMEOUT_CASES} array is hand-populated from the last
 * BenchLargePuzzles output. Update it after each main bench run.</p>
 */
public final class BenchRetryTimeouts {

    // {rows, cols, seed} — populated from the BenchLargePuzzles 2026-04-19
    // 00:10 run (all TIMEOUTs observed at 10-min budget).
    private static final long[][] TIMEOUT_CASES = {
        { 8, 8, 42L },
        { 8, 9, 17L },
        { 8, 9, 42L },
        { 9, 9,  1L },
        { 9, 9, 17L },
        { 9, 9, 42L },
    };
    private static final long PER_RUN_TIMEOUT_MS = 3_600_000; // 60 min

    public static void main(String[] args) {
        System.out.printf("%-6s %-5s %-13s %-10s %-10s %-20s%n",
            "dim", "seed", "time_ms", "status", "best", "info");
        for (long[] spec : TIMEOUT_CASES) {
            int rows = (int) spec[0];
            int cols = (int) spec[1];
            long seed = spec[2];
            int palette = Math.max(4, Math.max(rows, cols) - 1);
            int total = rows * cols;
            int maxEdges = (rows - 1) * cols + rows * (cols - 1);

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
            System.out.printf("%-6s %-5d %-13d %-10s %-10s %-20s%n",
                rows + "x" + cols, seed, ms, status, bestStr, info);
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
                if (r == 0 && p.edges[0] != 0) return "north border not 0";
                if (r == rows - 1 && p.edges[2] != 0) return "south border not 0";
                if (c == 0 && p.edges[3] != 0) return "west border not 0";
                if (c == cols - 1 && p.edges[1] != 0) return "east border not 0";
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
        for (int v : buf) { h ^= v; h *= 0x100000001B3L; }
        return h ^ Arrays.hashCode(buf);
    }
}
