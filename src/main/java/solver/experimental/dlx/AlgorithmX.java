package solver.experimental.dlx;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Knuth's Algorithm X — recursive search over a {@link DancingLinksMatrix}.
 *
 * <p>Picks the column with the fewest remaining rows ("S-heuristic") to
 * minimise branching, covers it, and tries every still-living row. The
 * first solution found is returned as an ordered list of the rows'
 * external ids (in the order Algorithm X committed them).</p>
 *
 * <p>Callers can pass a {@code solutionAcceptor} predicate: a solution is
 * returned only when the predicate accepts the candidate. This lets the
 * Eternity POC validate edge-matching on the leaf (each "exact cover"
 * produced satisfies cell & piece uniqueness but not necessarily the
 * edge constraints).</p>
 */
final class AlgorithmX {

    private AlgorithmX() { /* static */ }

    /**
     * Runs Algorithm X on {@code matrix} and returns the first solution
     * accepted by {@code solutionAcceptor}, or {@code null} if none.
     *
     * @param matrix          the DLX matrix to search (mutated during search, restored at exit)
     * @param solutionAcceptor filter that decides whether a candidate solution is valid
     *                         (e.g. Eternity edge-matching check)
     */
    static List<Integer> findFirstSolution(DancingLinksMatrix matrix,
                                           Predicate<List<Integer>> solutionAcceptor) {
        List<Integer> partial = new ArrayList<>();
        List<Integer> result = new ArrayList<>();
        boolean found = search(matrix, partial, result, solutionAcceptor);
        return found ? result : null;
    }

    /** Convenience: find any exact cover without extra validation. */
    static List<Integer> findFirstSolution(DancingLinksMatrix matrix) {
        return findFirstSolution(matrix, any -> true);
    }

    private static boolean search(DancingLinksMatrix matrix,
                                  List<Integer> partial,
                                  List<Integer> result,
                                  Predicate<List<Integer>> solutionAcceptor) {
        DancingLinksMatrix.Column root = matrix.root();

        // All columns covered — candidate solution.
        if (root.right == root) {
            if (solutionAcceptor.test(partial)) {
                result.addAll(partial);
                return true;
            }
            return false;
        }

        // S-heuristic: pick the column with the smallest size.
        DancingLinksMatrix.Column chosen = null;
        int minSize = Integer.MAX_VALUE;
        for (DancingLinksMatrix.Node c = root.right; c != root; c = c.right) {
            DancingLinksMatrix.Column col = (DancingLinksMatrix.Column) c;
            if (col.size < minSize) {
                minSize = col.size;
                chosen = col;
                if (minSize == 0) break; // dead column — no row covers it
            }
        }
        if (chosen == null || chosen.size == 0) {
            return false;
        }

        DancingLinksMatrix.cover(chosen);
        try {
            for (DancingLinksMatrix.Node row = chosen.down; row != chosen; row = row.down) {
                partial.add(row.rowId);
                // Cover every other column this row touches.
                for (DancingLinksMatrix.Node n = row.right; n != row; n = n.right) {
                    DancingLinksMatrix.cover(n.column);
                }

                if (search(matrix, partial, result, solutionAcceptor)) {
                    return true;
                }

                // Backtrack.
                partial.remove(partial.size() - 1);
                for (DancingLinksMatrix.Node n = row.left; n != row; n = n.left) {
                    DancingLinksMatrix.uncover(n.column);
                }
            }
            return false;
        } finally {
            DancingLinksMatrix.uncover(chosen);
        }
    }
}
