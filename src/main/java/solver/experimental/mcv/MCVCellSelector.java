package solver.experimental.mcv;

import model.Board;

/**
 * Most-Constraining Variable (MCV) cell selection — experimental.
 *
 * <p>Picks the empty cell whose placement would eliminate the most
 * options for its neighbours. For Eternity, "constraining power" is
 * proxied by the number of non-empty orthogonal neighbours + the number
 * of border sides: each such side pins an edge the future neighbour
 * must match. Secondary tiebreaker: cells nearer the board edge tend to
 * be more constrained themselves.</p>
 *
 * <p>This class is standalone (no dependency on the production solver
 * state) so benchmarks / tests can compare its scoring to MRV without
 * wiring a full alternative search path. Wiring MCV into
 * {@link solver.EternitySolver} is a follow-up tracked in the {@code
 * README.md} of this package.</p>
 */
public final class MCVCellSelector {

    private MCVCellSelector() { /* static */ }

    /**
     * Returns the MCV score of cell {@code (r,c)} on {@code board}.
     *
     * <p>Higher score = more constraining. Out-of-bounds cells or
     * non-empty cells return {@code -1} so callers can skip them.</p>
     */
    public static int score(Board board, int r, int c) {
        if (!board.isEmpty(r, c)) return -1;
        int rows = board.getRows();
        int cols = board.getCols();
        int pressure = 0;
        // Each border side is a hard constraint (edge must be 0).
        if (r == 0) pressure++;
        if (r == rows - 1) pressure++;
        if (c == 0) pressure++;
        if (c == cols - 1) pressure++;
        // Each placed neighbour pins an inner edge colour.
        if (r > 0 && !board.isEmpty(r - 1, c)) pressure++;
        if (r < rows - 1 && !board.isEmpty(r + 1, c)) pressure++;
        if (c > 0 && !board.isEmpty(r, c - 1)) pressure++;
        if (c < cols - 1 && !board.isEmpty(r, c + 1)) pressure++;
        return pressure;
    }

    /**
     * Finds the cell with the highest MCV score on {@code board}. Ties are
     * broken by preferring cells nearer the top-left (lexicographic order
     * on {@code r * cols + c}). Returns {@code null} if every cell is
     * placed already.
     */
    public static int[] findMostConstraining(Board board) {
        int rows = board.getRows();
        int cols = board.getCols();
        int bestScore = -1;
        int[] best = null;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int s = score(board, r, c);
                if (s > bestScore) {
                    bestScore = s;
                    best = new int[] {r, c};
                }
            }
        }
        return best;
    }
}
