package solver.experimental.dlx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates {@link DancingLinksMatrix} + {@link AlgorithmX} on the canonical
 * Knuth example (Algorithm X reference): 7 columns, 6 rows, exact cover
 * is rows {B, D, F}.
 *
 * <p>Matrix (1-indexed for readability; 0-index in code):</p>
 * <pre>
 *    c1 c2 c3 c4 c5 c6 c7
 * A:  1  0  0  1  0  0  1
 * B:  1  0  0  1  0  0  0
 * C:  0  0  0  1  1  0  1
 * D:  0  0  1  0  1  1  0
 * E:  0  1  1  0  0  1  1
 * F:  0  1  0  0  0  0  1
 * </pre>
 *
 * <p>Exact cover = B + D + F = {c1,c4} + {c3,c5,c6} + {c2,c7}.</p>
 */
@DisplayName("DancingLinksMatrix + AlgorithmX — Knuth 7×6 example")
class DancingLinksMatrixTest {

    @Test
    @DisplayName("solves Knuth's canonical example to {B, D, F}")
    void knuthCanonical() {
        DancingLinksMatrix m = new DancingLinksMatrix(7);
        m.addRow(0, new int[]{0, 3, 6});      // A = c1,c4,c7
        m.addRow(1, new int[]{0, 3});         // B = c1,c4
        m.addRow(2, new int[]{3, 4, 6});      // C = c4,c5,c7
        m.addRow(3, new int[]{2, 4, 5});      // D = c3,c5,c6
        m.addRow(4, new int[]{1, 2, 5, 6});   // E = c2,c3,c6,c7
        m.addRow(5, new int[]{1, 6});         // F = c2,c7

        List<Integer> solution = AlgorithmX.findFirstSolution(m);

        assertNotNull(solution, "exact cover must exist");
        assertEquals(Set.of(1, 3, 5), Set.copyOf(solution),
            "Knuth's example has the unique cover B+D+F (rows 1, 3, 5)");
    }

    @Test
    @DisplayName("no-solution case: column with no row returns null")
    void noSolutionIfColumnEmpty() {
        DancingLinksMatrix m = new DancingLinksMatrix(3);
        m.addRow(0, new int[]{0});
        m.addRow(1, new int[]{1});
        // Column 2 has no row — cannot be covered.

        List<Integer> solution = AlgorithmX.findFirstSolution(m);
        assertNull(solution);
    }

    @Test
    @DisplayName("single-row cover: trivial solution returns that row")
    void trivialOneRowCover() {
        DancingLinksMatrix m = new DancingLinksMatrix(2);
        m.addRow(42, new int[]{0, 1});

        List<Integer> solution = AlgorithmX.findFirstSolution(m);
        assertEquals(List.of(42), solution);
    }

    @Test
    @DisplayName("solutionAcceptor can reject an exact cover")
    void acceptorCanReject() {
        DancingLinksMatrix m = new DancingLinksMatrix(2);
        m.addRow(7, new int[]{0, 1});

        List<Integer> solution = AlgorithmX.findFirstSolution(m, rows -> false);
        assertNull(solution, "acceptor rejects every candidate → no solution returned");
    }

    @Test
    @DisplayName("addRow rejects empty row")
    void emptyRowRejected() {
        DancingLinksMatrix m = new DancingLinksMatrix(3);
        assertThrows(IllegalArgumentException.class, () -> m.addRow(0, new int[]{}));
        assertThrows(IllegalArgumentException.class, () -> m.addRow(0, null));
    }
}
