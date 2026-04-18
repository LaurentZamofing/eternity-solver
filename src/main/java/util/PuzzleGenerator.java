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
        if (size < 2) throw new IllegalArgumentException("size must be >= 2, got " + size);
        if (numColours < 1) throw new IllegalArgumentException("numColours must be >= 1, got " + numColours);

        Random rng = new Random(seed);

        // Inner horizontal lines: between row r and r+1, for cols 0..size-1.
        // innerH[r][c] = colour of the edge separating cell (r,c) and (r+1,c).
        int[][] innerH = new int[size - 1][size];
        for (int r = 0; r < size - 1; r++) {
            for (int c = 0; c < size; c++) {
                innerH[r][c] = 1 + rng.nextInt(numColours); // avoid 0 — reserved for borders
            }
        }
        // Inner vertical lines: between col c and c+1, for rows 0..size-1.
        int[][] innerV = new int[size][size - 1];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size - 1; c++) {
                innerV[r][c] = 1 + rng.nextInt(numColours);
            }
        }

        // Build cell pieces. For cell (r,c): N=above, E=right, S=below, W=left.
        // Border edges are 0.
        int[][] northEdge = new int[size][size];
        int[][] eastEdge  = new int[size][size];
        int[][] southEdge = new int[size][size];
        int[][] westEdge  = new int[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                northEdge[r][c] = (r == 0)        ? 0 : innerH[r - 1][c];
                southEdge[r][c] = (r == size - 1) ? 0 : innerH[r][c];
                westEdge[r][c]  = (c == 0)        ? 0 : innerV[r][c - 1];
                eastEdge[r][c]  = (c == size - 1) ? 0 : innerV[r][c];
            }
        }

        // Shuffle cell order into piece IDs so the solver doesn't get the
        // original grid layout trivially in-order.
        int n = size * size;
        Integer[] ids = new Integer[n];
        for (int i = 0; i < n; i++) ids[i] = i + 1;
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Integer tmp = ids[i]; ids[i] = ids[j]; ids[j] = tmp;
        }

        Map<Integer, Piece> out = new HashMap<>(n);
        int k = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
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
