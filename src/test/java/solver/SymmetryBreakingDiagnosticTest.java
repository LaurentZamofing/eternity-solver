package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import util.PuzzleFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Diagnostic tests that traced the AC-3 + sym-breaking bugs fixed in
 * commits 34f693e / b8093e3 / 5e5d855 / 3d0f947.
 *
 * <p>Split off from {@link SymmetryBreakingBugTrackingTest} (2026-04-17) to
 * keep regression tests tight; these tests are mostly informational
 * (System.err dumps), but they also serve as high-signal smoke tests that
 * will flag a regression in the AC-3 restore path or canonical TL rule if
 * it creeps back in.</p>
 *
 * <p>Each test carries its own tag in the DisplayName: [A1.x] diagnostics
 * belong to the first bug (4×4 easy), [A2.x] to the second (4×4 hard).</p>
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class SymmetryBreakingDiagnosticTest {

    @Test
    @DisplayName("[A1.2 diag] rejection counters after lex+rotation on 4x4 easy")
    void diagnosticRejectionCountersOn4x4Easy() {
        Board board = new Board(4, 4);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(20_000);
        solver.setSymmetryBreakingFlags(true, true);
        boolean solved = solver.solve(board, PuzzleFactory.createExample4x4Easy());

        SymmetryBreakingManager mgr = solver.getSymmetryBreakingManager();
        assertNotNull(mgr, "manager must be built after solve()");

        System.err.println("=== A1.2 diagnostic — solve4x4Easy with lex+rotation ON ===");
        System.err.println("  solved          : " + solved);
        System.err.println("  lexRejections   : " + mgr.getLexRejections());
        System.err.println("  rotRejections   : " + mgr.getRotationRejections());
        System.err.println("  firstLexReject  : ("
            + mgr.getFirstLexRejectionRow() + "," + mgr.getFirstLexRejectionCol()
            + ") piece " + mgr.getFirstLexRejectionPieceId());
    }

    @Test
    @DisplayName("[A1.4b diag] lex rejections broken down by call-site on 4x4 easy")
    void diagnosticCallSiteBreakdownOn4x4Easy() {
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4Easy();
        Board board = new Board(4, 4);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(15_000);
        solver.setSymmetryBreakingFlags(true, true);
        boolean solved = solver.solve(board, pieces);

        SymmetryBreakingManager mgr = solver.getSymmetryBreakingManager();
        assertNotNull(mgr, "manager must be built after solve()");

        System.err.println("=== A1.4b diagnostic — call-site breakdown ===");
        System.err.println("  solved                     : " + solved);
        System.err.println("  lexRejections (total)      : " + mgr.getLexRejections());
        System.err.println("  lexRejectionsFromSingleton : " + mgr.getLexRejectionsFromSingleton());
        System.err.println("  lexRejectionsFromValidator : " + mgr.getLexRejectionsFromValidator());
        System.err.println("  lexRejectionsFromOther     : " + mgr.getLexRejectionsFromOther());
        System.err.println("  firstLexReject             : ("
            + mgr.getFirstLexRejectionRow() + "," + mgr.getFirstLexRejectionCol()
            + ") piece " + mgr.getFirstLexRejectionPieceId()
            + " rot " + mgr.getFirstLexRejectionRotation());

        long rejPid = mgr.getFirstLexRejectionPieceId();
        if (rejPid >= 0) {
            Piece p = pieces.get((int) rejPid);
            StringBuilder rotList = new StringBuilder();
            for (int r = 0; r < 4; r++) {
                int[] edges = p.edgesRotated(r);
                if (edges[0] == 0 && edges[3] == 0) {
                    if (rotList.length() > 0) rotList.append(",");
                    rotList.append(r);
                }
            }
            System.err.println("  piece " + rejPid + " TL-fittable at rotations: ["
                + (rotList.length() == 0 ? "NONE" : rotList) + "]");
        }
    }

    @Test
    @DisplayName("[A2.1 diag] rotation 90° CW de la solution 4x4 hard est-elle valide ?")
    void diagnosticRotated90CwBoardIsValid() {
        Board solved = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(30_000);
        solver.setSymmetryBreakingFlags(false, false);
        assertTrue(solver.solve(solved, pieces), "unconstrained solution must exist");

        Board rotated = new Board(4, 4);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                model.Placement pl = solved.getPlacement(r, c);
                assertNotNull(pl, "cell not placed?");
                int newR = c, newC = 3 - r;
                int newRot = (pl.getRotation() + 1) % 4;
                rotated.place(newR, newC, pieces.get(pl.getPieceId()), newRot);
            }
        }

        model.Placement tl = rotated.getPlacement(0, 0);
        System.err.println("=== A2.1 diagnostic — rotation 90° CW du board ===");
        System.err.println("  rotated TL (0,0) : piece " + tl.getPieceId() + " rot " + tl.getRotation());

        int mismatches = 0;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                model.Placement p = rotated.getPlacement(r, c);
                if (r == 0 && p.edges[0] != 0) mismatches++;
                if (r == 3 && p.edges[2] != 0) mismatches++;
                if (c == 0 && p.edges[3] != 0) mismatches++;
                if (c == 3 && p.edges[1] != 0) mismatches++;
                if (c < 3 && p.edges[1] != rotated.getPlacement(r, c + 1).edges[3]) mismatches++;
                if (r < 3 && p.edges[2] != rotated.getPlacement(r + 1, c).edges[0]) mismatches++;
            }
        }
        System.err.println("  total mismatches in rotated board: " + mismatches);
        assertEquals(0, mismatches, "the 90° CW rotation of a valid solution must itself be valid");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("[A2.1 diag] solveur accepte-t-il la rotated solution pré-placée ?")
    void diagnosticSolverAcceptsFullyRotatedSolution() {
        Board solved = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();
        EternitySolver prep = new EternitySolver();
        prep.setVerbose(false);
        prep.setMaxExecutionTime(20_000);
        prep.setSymmetryBreakingFlags(false, false);
        assertTrue(prep.solve(solved, pieces));

        Board rotated = new Board(4, 4);
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            model.Placement pl = solved.getPlacement(r, c);
            rotated.place(c, 3 - r, pieces.get(pl.getPieceId()), (pl.getRotation() + 1) % 4);
        }

        EternitySolver acceptor = new EternitySolver();
        acceptor.setVerbose(false);
        acceptor.setMaxExecutionTime(5_000);
        acceptor.setSymmetryBreakingFlags(false, false);
        long t0 = System.currentTimeMillis();
        boolean accepted = acceptor.solve(rotated, pieces);
        long elapsed = System.currentTimeMillis() - t0;
        System.err.println("=== A2.1 diagnostic — solver accepts fully pre-placed rotated solution ===");
        System.err.println("  accepted: " + accepted + " (elapsed=" + elapsed + "ms)");
    }

    @Test
    @DisplayName("[A2.1 diag] AC-3 élimine-t-il (piece,rot) attendu ?")
    void diagnosticAC3EliminatesExpectedPlacement() {
        Board solved = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();
        EternitySolver prep = new EternitySolver();
        prep.setVerbose(false);
        prep.setMaxExecutionTime(20_000);
        prep.setSymmetryBreakingFlags(false, false);
        assertTrue(prep.solve(solved, pieces));
        int[][] expPid = new int[4][4], expRot = new int[4][4];
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            model.Placement pl = solved.getPlacement(r, c);
            expPid[c][3-r] = pl.getPieceId();
            expRot[c][3-r] = (pl.getRotation() + 1) % 4;
        }

        Board board = new Board(4, 4);
        board.place(0, 0, pieces.get(expPid[0][0]), expRot[0][0]);

        ConstraintPropagator.Statistics cpStats = new ConstraintPropagator.Statistics();
        DomainManager dm = new DomainManager(realisticFit());
        java.util.BitSet used = new java.util.BitSet();
        used.set(expPid[0][0]);
        dm.initializeAC3Domains(board, pieces, used, 16);

        ConstraintPropagator cp = new ConstraintPropagator(dm, cpStats);
        cp.propagateAC3(board, 0, 0, expPid[0][0], expRot[0][0], pieces, used, 16);

        System.err.println("=== A2.1 diagnostic — expected (piece,rot) in domain après AC-3 ===");
        int missingCount = 0;
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            if (r == 0 && c == 0) continue;
            Map<Integer, List<DomainManager.ValidPlacement>> dom = dm.getDomain(r, c);
            boolean found = false;
            if (dom != null && dom.containsKey(expPid[r][c])) {
                for (DomainManager.ValidPlacement vp : dom.get(expPid[r][c])) {
                    if (vp.rotation == expRot[r][c]) { found = true; break; }
                }
            }
            if (!found) missingCount++;
        }
        System.err.println("  total missing: " + missingCount);
    }

    @Test
    @DisplayName("[A2.1 diag] AC-3 complète-t-il depuis N pieces pré-placées ?")
    void diagnosticAC3CompleteFromNPreplaced() {
        Board solved = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();
        EternitySolver prep = new EternitySolver();
        prep.setVerbose(false);
        prep.setMaxExecutionTime(20_000);
        prep.setSymmetryBreakingFlags(false, false);
        assertTrue(prep.solve(solved, pieces));

        int[][] rotPid = new int[4][4];
        int[][] rotRot = new int[4][4];
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            model.Placement pl = solved.getPlacement(r, c);
            rotPid[c][3-r] = pl.getPieceId();
            rotRot[c][3-r] = (pl.getRotation() + 1) % 4;
        }

        System.err.println("=== A2.1 diagnostic — incremental AC-3 completion ===");
        for (int n : new int[]{1, 2, 4, 6, 8, 12}) {
            Board b = new Board(4, 4);
            int placed = 0;
            outer:
            for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
                b.place(r, c, pieces.get(rotPid[r][c]), rotRot[r][c]);
                placed++;
                if (placed >= n) break outer;
            }
            EternitySolver s = new EternitySolver();
            s.setVerbose(false);
            s.setMaxExecutionTime(20_000);
            s.setSymmetryBreakingFlags(false, false);
            long t0 = System.currentTimeMillis();
            boolean ok = s.solve(b, pieces);
            long dt = System.currentTimeMillis() - t0;
            System.err.println(String.format("  n=%d pre-placed → solved=%s in %dms", n, ok, dt));
        }
    }

    @Test
    @DisplayName("[A2.1 diag] domains AC-3 après piece 7 rot 0 à TL")
    void diagnosticAC3DomainsAfterPiece7Rot0AtTL() {
        Board board = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();
        board.place(0, 0, pieces.get(7), 0);

        DomainManager dm = new DomainManager(realisticFit());
        java.util.BitSet used = new java.util.BitSet();
        used.set(7);
        dm.initializeAC3Domains(board, pieces, used, 16);

        System.err.println("=== A2.1 diagnostic — AC-3 domain sizes après piece 7 rot 0 à TL ===");
        for (int r = 0; r < 4; r++) {
            StringBuilder line = new StringBuilder("    ");
            for (int c = 0; c < 4; c++) {
                if (r == 0 && c == 0) { line.append(" FIXED "); continue; }
                Map<Integer, List<DomainManager.ValidPlacement>> dom = dm.getDomain(r, c);
                int total = 0;
                if (dom != null) for (List<DomainManager.ValidPlacement> l : dom.values()) total += l.size();
                line.append(String.format(" %2dp/%2d ", dom == null ? 0 : dom.size(), total));
            }
            System.err.println(line);
        }
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    @DisplayName("[A2.1 diag] piece 7 rot 0 à TL, AC-3 désactivé")
    void diagnosticHardWithPiece7Rot0NoAC3() {
        Board board = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();
        board.place(0, 0, pieces.get(7), 0);

        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(60_000);
        solver.setSymmetryBreakingFlags(false, false);
        solver.setUseAC3(false);
        long t0 = System.currentTimeMillis();
        boolean solved = solver.solve(board, pieces);
        long elapsed = System.currentTimeMillis() - t0;
        System.err.println("=== A2.1 diagnostic — piece 7 rot 0 at TL, AC-3 OFF ===");
        System.err.println("  solved: " + solved + " (elapsed=" + elapsed + "ms)");
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    @DisplayName("[A2.1 diag] 4x4 hard résout-il avec piece 7 rot 0 forcée à TL ?")
    void diagnosticHardWithPiece7Rot0ForcedAtTL() {
        Board board = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();
        Piece p7 = pieces.get(7);
        board.place(0, 0, p7, 0);

        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(120_000);
        solver.setSymmetryBreakingFlags(false, false);
        long t0 = System.currentTimeMillis();
        boolean solved = solver.solve(board, pieces);
        long elapsed = System.currentTimeMillis() - t0;
        System.err.println("=== A2.1 diagnostic — piece 7 rot 0 forced at TL ===");
        System.err.println("  solved with flags OFF, piece 7 rot 0 pre-placed: " + solved
            + " (elapsed=" + elapsed + "ms)");
    }

    @Test
    @DisplayName("[A2.1 diag] 4x4 hard flag combo matrix")
    void diagnosticHardFlagMatrix() {
        System.err.println("=== A2.1 diagnostic — 4x4 hard flag matrix ===");
        for (boolean lex : new boolean[]{false, true}) {
            for (boolean rot : new boolean[]{false, true}) {
                Board b = new Board(4, 4);
                EternitySolver s = new EternitySolver();
                s.setVerbose(false);
                s.setMaxExecutionTime(10_000);
                s.setSymmetryBreakingFlags(lex, rot);
                boolean solved = s.solve(b, PuzzleFactory.createExample4x4HardV3());
                SymmetryBreakingManager m = s.getSymmetryBreakingManager();
                System.err.println(String.format("  lex=%s rot=%s → solved=%s  lexRej=%d rotRej=%d",
                    lex, rot, solved, m.getLexRejections(), m.getRotationRejections()));
            }
        }
    }

    @Test
    @DisplayName("[A2.1 diag] inspect TL piece in unconstrained 4x4 hard solution")
    void diagnosticInspectHardSolutionCorners() {
        Board board = new Board(4, 4);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(40_000);
        solver.setSymmetryBreakingFlags(false, false);
        assertTrue(solver.solve(board, PuzzleFactory.createExample4x4HardV3()));

        System.err.println("=== A2.1 diagnostic — unconstrained 4x4 hard solution ===");
        int[][] corners = {{0,0}, {0,3}, {3,0}, {3,3}};
        String[] names = {"TL", "TR", "BL", "BR"};
        for (int i = 0; i < 4; i++) {
            int r = corners[i][0], c = corners[i][1];
            model.Placement pl = board.getPlacement(r, c);
            System.err.println("  " + names[i] + " (" + r + "," + c + "): piece "
                + pl.getPieceId() + " rot " + pl.getRotation());
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    /** Mirrors PlacementValidator.fits logic for tests that build a DomainManager
     *  outside the solver. */
    private static DomainManager.FitChecker realisticFit() {
        return (b, r, c, edges) -> {
            if (r == 0 && edges[0] != 0) return false;
            if (c == 0 && edges[3] != 0) return false;
            if (r == 3 && edges[2] != 0) return false;
            if (c == 3 && edges[1] != 0) return false;
            if (r > 0 && !b.isEmpty(r-1, c) && b.getPlacement(r-1, c).edges[2] != edges[0]) return false;
            if (c > 0 && !b.isEmpty(r, c-1) && b.getPlacement(r, c-1).edges[1] != edges[3]) return false;
            if (r < 3 && !b.isEmpty(r+1, c) && b.getPlacement(r+1, c).edges[0] != edges[2]) return false;
            if (c < 3 && !b.isEmpty(r, c+1) && b.getPlacement(r, c+1).edges[3] != edges[1]) return false;
            return true;
        };
    }
}
