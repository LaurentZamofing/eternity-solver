package solver;

import model.Board;
import model.Piece;
import model.Placement;

import java.util.BitSet;
import java.util.Map;

/**
 * Global colour-budget sanity check — dead-branch detection via an
 * arithmetic necessary condition: for every interior colour {@code c},
 * the number of frontier half-edges requiring {@code c} must not exceed
 * the number of {@code c}-coloured edges remaining on unused pieces.
 *
 * <p>If demand > supply for any colour, the current partial assignment
 * is unsatisfiable regardless of how we try to fill the remaining
 * cells. Catches dead branches much earlier than AC-3 can.</p>
 *
 * <p>Conservative: this is a <b>necessary</b> condition, not sufficient
 * — passing the check does not prove satisfiability. Failing the check
 * does prove infeasibility, so it's always safe to prune on failure.</p>
 */
public final class ColorBudgetTracker {

    private final int maxColour;

    /** Builds a tracker sized for the given piece set. Scans all edges
     *  once to find {@code maxColour}. */
    public ColorBudgetTracker(Map<Integer, Piece> pieces) {
        int m = 0;
        for (Piece p : pieces.values()) {
            for (int e : p.getEdges()) if (e > m) m = e;
        }
        this.maxColour = m;
    }

    /**
     * Returns {@code true} if the current partial assignment can still
     * satisfy the colour-budget constraint on every interior colour.
     * {@code false} → prune this branch.
     */
    public boolean check(Board board, Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {
        int[] supply = new int[maxColour + 1];
        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed.get(pid)) continue;
            Piece p = pieces.get(pid);
            if (p == null) continue;
            for (int e : p.getEdges()) {
                if (e >= 0 && e <= maxColour) supply[e]++;
            }
        }

        int[] demand = new int[maxColour + 1];
        int rows = board.getRows();
        int cols = board.getCols();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!board.isEmpty(r, c)) continue;
                // Empty cell — check its placed neighbours; each contributes a
                // required colour on our side of the shared edge.
                if (r > 0 && !board.isEmpty(r - 1, c)) {
                    Placement above = board.getPlacement(r - 1, c);
                    int req = above.edges[2]; // south edge of above = required north on our side
                    if (req >= 0 && req <= maxColour) demand[req]++;
                }
                if (r < rows - 1 && !board.isEmpty(r + 1, c)) {
                    Placement below = board.getPlacement(r + 1, c);
                    int req = below.edges[0];
                    if (req >= 0 && req <= maxColour) demand[req]++;
                }
                if (c > 0 && !board.isEmpty(r, c - 1)) {
                    Placement left = board.getPlacement(r, c - 1);
                    int req = left.edges[1];
                    if (req >= 0 && req <= maxColour) demand[req]++;
                }
                if (c < cols - 1 && !board.isEmpty(r, c + 1)) {
                    Placement right = board.getPlacement(r, c + 1);
                    int req = right.edges[3];
                    if (req >= 0 && req <= maxColour) demand[req]++;
                }
            }
        }

        for (int col = 1; col <= maxColour; col++) {
            if (demand[col] > supply[col]) return false;
        }
        return true;
    }

    public int getMaxColour() { return maxColour; }
}
