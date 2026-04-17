package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import util.PuzzleFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests pinning the AC-3 + sym-breaking fixes landed in
 * commits 34f693e / b8093e3 / 5e5d855 / 3d0f947.
 *
 * <p>Scope:</p>
 * <ol>
 *   <li>Check the TL-fittable computation against real puzzle pieces.</li>
 *   <li>Assert the per-placement rule decisions on the TL corner with the
 *       actual 4×4 easy/hard piece sets.</li>
 *   <li>Solve 4×4 easy under every lex × rotation flag combination so any
 *       reintroduction of the old deadlock is caught immediately.</li>
 *   <li>Check AC-3 does not emit singleton-sourced lex rejections on
 *       4×4 easy once the pre-filter is in place.</li>
 * </ol>
 *
 * <p>Diagnostic / investigation tests (those with a {@code [A1.x diag]} or
 * {@code [A2.x diag]} DisplayName) live in {@link SymmetryBreakingDiagnosticTest}.</p>
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class SymmetryBreakingBugTrackingTest {

    /**
     * The 4x4 easy puzzle has exactly one piece (id=11) with N=0 AND W=0 at
     * rotation 0. The redesigned canonical rule must therefore identify
     * piece 11 as the only TL-fittable canonical piece.
     */
    @Test
    @DisplayName("4x4 easy: only piece 11 is TL-fittable at rotation 0")
    void easyPuzzleHasSingleTLCandidateAtRot0() {
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4Easy();

        Set<Integer> tlFittable = computeTLFittableAtRot0(pieces);

        assertEquals(Set.of(11), tlFittable,
            "easy puzzle: only piece 11 has N=0 AND W=0 at rotation 0; got " + tlFittable);
    }

    @Test
    @DisplayName("4x4 hard: only piece 7 is TL-fittable at rotation 0")
    void hardPuzzleHasSingleTLCandidateAtRot0() {
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();

        Set<Integer> tlFittable = computeTLFittableAtRot0(pieces);

        assertEquals(Set.of(7), tlFittable,
            "hard puzzle: only piece 7 has N=0 AND W=0 at rotation 0; got " + tlFittable);
    }

    @Test
    @DisplayName("4x4 easy + lex on: TL accepts only the canonical TL piece (11)")
    void easyPuzzleLexAcceptsOnlyCanonicalTLPiece() {
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4Easy();
        SymmetryBreakingManager mgr = new SymmetryBreakingManager(4, 4, false);
        mgr.setLexicographicOrdering(true);
        mgr.setRotationalFixing(false);
        Board board = new Board(4, 4);

        assertTrue(mgr.isPlacementAllowed(board, 0, 0, 11, 0, pieces),
            "canonical TL piece must be allowed at (0,0)");

        for (int otherTLFittable : Set.of(1, 10)) {
            assertFalse(mgr.isPlacementAllowed(board, 0, 0, otherTLFittable, 0, pieces),
                "non-canonical TL-fittable piece " + otherTLFittable + " must be rejected at (0,0)");
        }

        for (int notTL : Set.of(2, 3, 4, 5, 6, 8, 9, 12, 13, 14, 15, 16)) {
            assertFalse(mgr.isPlacementAllowed(board, 0, 0, notTL, 0, pieces),
                "non-TL-fittable piece " + notTL + " must be rejected at (0,0)");
        }
    }

    @Test
    @DisplayName("4x4 easy + lex on: non-TL corners are unconstrained by lex")
    void easyPuzzleLexDoesNotConstrainOtherCorners() {
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4Easy();
        SymmetryBreakingManager mgr = new SymmetryBreakingManager(4, 4, false);
        mgr.setLexicographicOrdering(true);
        mgr.setRotationalFixing(false);
        Board board = new Board(4, 4);
        board.place(0, 0, pieces.get(11), 0);

        for (int pid : pieces.keySet()) {
            assertTrue(mgr.isPlacementAllowed(board, 0, 3, pid, 0, pieces),
                "TR must accept piece " + pid + " (lex doesn't constrain non-TL corners)");
            assertTrue(mgr.isPlacementAllowed(board, 3, 0, pid, 0, pieces),
                "BL must accept piece " + pid);
            assertTrue(mgr.isPlacementAllowed(board, 3, 3, pid, 0, pieces),
                "BR must accept piece " + pid);
        }
    }

    @Nested
    @DisplayName("Regression: sym-breaking flag combinations on 4x4 easy")
    class RegressionCombos {

        @Test
        @DisplayName("lex on + rotation off: solver succeeds (post-fix)")
        void lexOnly_succeedsOn4x4Easy() {
            assertTrue(solveWith(true, false, PuzzleFactory.createExample4x4Easy(), 4),
                "lex on + rotation off must solve after TL domain pre-filter fix");
        }

        @Test
        @DisplayName("lex on + rotation on: solver succeeds (post-fix)")
        void lexAndRotation_succeedOn4x4Easy() {
            assertTrue(solveWith(true, true, PuzzleFactory.createExample4x4Easy(), 4),
                "both flags on must solve after TL domain pre-filter fix");
        }

        @Test
        @DisplayName("Both flags off: solver still succeeds")
        void bothOff_succeedsOn4x4Easy() {
            assertTrue(solveWith(false, false, PuzzleFactory.createExample4x4Easy(), 4),
                "with all sym-breaking off the easy puzzle must solve");
        }

        /**
         * After the fix, no lex rejections should originate from
         * SingletonPlacementStrategy on 4x4 easy — AC-3 cannot propose a
         * singleton at (0,0) incompatible with the sym-breaking rule
         * because the domain has been pre-filtered.
         */
        @Test
        @DisplayName("No singleton-sourced lex rejections after fix (4x4 easy)")
        void noSingletonLexRejectionsAfterFix() {
            Board board = new Board(4, 4);
            EternitySolver solver = new EternitySolver();
            solver.setVerbose(false);
            solver.setMaxExecutionTime(15_000);
            solver.setSymmetryBreakingFlags(true, true);
            solver.solve(board, PuzzleFactory.createExample4x4Easy());

            SymmetryBreakingManager mgr = solver.getSymmetryBreakingManager();
            assertEquals(0, mgr.getLexRejectionsFromSingleton(),
                "TL domain pre-filter should eliminate singleton-sourced lex rejections");
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private static Set<Integer> computeTLFittableAtRot0(Map<Integer, Piece> pieces) {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (Map.Entry<Integer, Piece> e : pieces.entrySet()) {
            int[] edges = e.getValue().edgesRotated(0);
            if (edges[0] == 0 && edges[3] == 0) ids.add(e.getKey());
        }
        return ids;
    }

    private static boolean solveWith(boolean lex, boolean rotation,
                                     Map<Integer, Piece> pieces, int size) {
        Board board = new Board(size, size);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(20_000);
        solver.setSymmetryBreakingFlags(lex, rotation);
        return solver.solve(board, pieces);
    }
}
