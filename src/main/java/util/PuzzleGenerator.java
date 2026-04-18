package util;

import model.Piece;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Generates valid, solvable Eternity-style puzzles of arbitrary size.
 *
 * <p>Procedure:</p>
 * <ol>
 *   <li>Pick an edge colour for every grid line (horizontal &amp; vertical
 *       inner edges) and set the outer border to colour 0.</li>
 *   <li>For each cell (r,c), emit a piece whose edges N/E/S/W match the
 *       surrounding grid lines.</li>
 * </ol>
 *
 * <p>Because the pieces are extracted from a concrete grid, a valid
 * solution always exists (the original grid itself). This makes the
 * generator suitable for benchmarks that need to exercise the solver's
 * real search behaviour: the pieces returned are given in scrambled ID
 * order, so the backtracker still has to find the layout.</p>
 *
 * <p><b>Deterministic</b> — pass a seed to reproduce the same puzzle.</p>
 */
public final class PuzzleGenerator {

    private PuzzleGenerator() { /* static factory only */ }

    /** Default colour palette size for generated puzzles. Matches the
     *  ~22 distinct inner colours of the real Eternity II puzzle. */
    public static final int DEFAULT_COLOURS = 6;

    /**
     * Creates a solvable {@code size × size} puzzle with {@code numColours}
     * distinct inner edge colours (plus colour 0 for every outer border).
     *
     * @param size       board dimension (rows = cols = size)
     * @param numColours number of distinct inner colours (>= 1)
     * @param seed       PRNG seed for reproducibility
     * @return pieces indexed 1..(size*size) — IDs shuffled so the solver
     *         has to backtrack
     */
    public static Map<Integer, Piece> generate(int size, int numColours, long seed) {
        return generate(size, size, numColours, seed);
    }

    /**
     * Creates a solvable {@code rows × cols} puzzle — allows non-square
     * grids (e.g. 7×8, 8×9) for stress benches beyond the square sizes.
     */
    public static Map<Integer, Piece> generate(int rows, int cols, int numColours, long seed) {
        if (rows < 2 || cols < 2) throw new IllegalArgumentException("dimensions must be >= 2, got " + rows + "×" + cols);
        if (numColours < 1) throw new IllegalArgumentException("numColours must be >= 1, got " + numColours);

        Random rng = new Random(seed);

        int[][] innerH = new int[rows - 1][cols];
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols; c++) {
                innerH[r][c] = 1 + rng.nextInt(numColours);
            }
        }
        int[][] innerV = new int[rows][cols - 1];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols - 1; c++) {
                innerV[r][c] = 1 + rng.nextInt(numColours);
            }
        }

        int[][] northEdge = new int[rows][cols];
        int[][] eastEdge  = new int[rows][cols];
        int[][] southEdge = new int[rows][cols];
        int[][] westEdge  = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                northEdge[r][c] = (r == 0)         ? 0 : innerH[r - 1][c];
                southEdge[r][c] = (r == rows - 1)  ? 0 : innerH[r][c];
                westEdge[r][c]  = (c == 0)         ? 0 : innerV[r][c - 1];
                eastEdge[r][c]  = (c == cols - 1)  ? 0 : innerV[r][c];
            }
        }

        int n = rows * cols;
        Integer[] ids = new Integer[n];
        for (int i = 0; i < n; i++) ids[i] = i + 1;
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Integer tmp = ids[i]; ids[i] = ids[j]; ids[j] = tmp;
        }

        Map<Integer, Piece> out = new HashMap<>(n);
        int k = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int[] edges = { northEdge[r][c], eastEdge[r][c], southEdge[r][c], westEdge[r][c] };
                int pieceId = ids[k++];
                out.put(pieceId, new Piece(pieceId, edges));
            }
        }
        return out;
    }

    /** Convenience: default colour palette, default seed (42). */
    public static Map<Integer, Piece> generate(int size) {
        return generate(size, DEFAULT_COLOURS, 42L);
    }
}
