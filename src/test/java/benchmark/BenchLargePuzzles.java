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
 * Reference corpus for future optimisations — large puzzles with a
 * generous 10-min timeout. 3 seeds per dimension.
 *
 * <p>Each successful run is <b>validated</b> (all cells filled, borders
 * have colour 0 on the outside, neighbour edges match, every piece used
 * exactly once) and a <b>solution fingerprint</b> is printed so repeated
 * runs on the same seed can be compared for determinism.</p>
 */
public final class BenchLargePuzzles {

    private static final int[][] DIMS = {
        {7, 7}, {7, 8}, {8, 8}, {8, 9}, {9, 9}
    };
    private static final long[] SEEDS = {1L, 17L, 42L};
    private static final long PER_RUN_TIMEOUT_MS = 600_000; // 10 min

    public static void main(String[] args) {
        System.out.printf("%-6s %-5s %-13s %-8s %-20s%n",
            "dim", "seed", "time_ms", "status", "solution_fp");
        for (int[] dim : DIMS) {
            int rows = dim[0];
            int cols = dim[1];
            int palette = Math.max(4, Math.max(rows, cols) - 1);
            for (long seed : SEEDS) {
                Map<Integer, Piece> pieces = PuzzleGenerator.generate(rows, cols, palette, seed);
                Board b = new Board(rows, cols);
                long t = System.nanoTime();
                boolean ok;
                try {
                    ParallelBitmapSolver solver = new ParallelBitmapSolver();
                    solver.setMaxExecutionTime(PER_RUN_TIMEOUT_MS);
                    ok = solver.solve(b, new HashMap<>(pieces));
                } catch (Throwable e) {
                    ok = false;
                }
                long ms = (System.nanoTime() - t) / 1_000_000L;

                String status;
                String fp = "-";
                if (ok) {
                    String validation = validate(b, pieces);
                    if (validation != null) {
                        status = "INVALID";
                        fp = validation;
                    } else {
                        status = "SOLVED";
                        fp = String.format("%016x", fingerprint(b));
                    }
                } else {
                    status = "TIMEOUT";
                }

                System.out.printf("%-6s %-5d %-13d %-8s %-20s%n",
                    rows + "x" + cols, seed, ms, status, fp);
            }
        }
    }

    /** Returns {@code null} if the board is a valid solution, else an
     *  error description. */
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
                if (r > 0) {
                    Placement n = b.getPlacement(r - 1, c);
                    if (n.edges[2] != p.edges[0])
                        return "N/S mismatch at " + r + "," + c;
                }
                if (c > 0) {
                    Placement w = b.getPlacement(r, c - 1);
                    if (w.edges[1] != p.edges[3])
                        return "E/W mismatch at " + r + "," + c;
                }
            }
        }
        for (int i = 1; i < used.length; i++) {
            if (!used[i]) return "piece " + i + " never placed";
        }
        return null;
    }

    /** 64-bit hash of the (cellIndex, pieceId, rotation) triples. Two runs
     *  returning the same solution produce the same fingerprint; otherwise
     *  they differ (useful to detect non-determinism in the portfolio). */
    private static long fingerprint(Board b) {
        long h = 0xCBF29CE484222325L; // FNV-1a basis
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
