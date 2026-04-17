package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import util.PuzzleFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Targeted tests that pin down the SymmetryBreakingManager bug found by
 * {@link AC3CorrectnessTest#solve4x4EasyProducesValidSolution}.
 *
 * <p>The flags ship OFF (commit e5b7716) because re-enabling them with the
 * redesigned per-TL canonical rule still fails the 4x4 easy gate. This
 * suite isolates the failure surface by:
 *  <ol>
 *    <li>checking the TL-fittable computation against real puzzle pieces;</li>
 *    <li>asserting the per-placement rule decisions on the 4 corner positions
 *        with the actual easy/hard piece sets;</li>
 *    <li>solving each puzzle with combinations of flag states so the failing
 *        combination is documented and pinned to a regression test.</li>
 *  </ol>
 *
 * <p>Run with: {@code mvn test -Dtest=SymmetryBreakingBugTrackingTest}.</p>
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

    /**
     * The 4x4 hard variant has exactly one TL-fittable piece (id=7).
     */
    @Test
    @DisplayName("4x4 hard: only piece 7 is TL-fittable at rotation 0")
    void hardPuzzleHasSingleTLCandidateAtRot0() {
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();

        Set<Integer> tlFittable = computeTLFittableAtRot0(pieces);

        assertEquals(Set.of(7), tlFittable,
            "hard puzzle: only piece 7 has N=0 AND W=0 at rotation 0; got " + tlFittable);
    }

    /**
     * On the easy puzzle, the canonical TL piece is 11. With lex on, the
     * rule must accept piece 11 at TL and reject any other piece at TL.
     */
    @Test
    @DisplayName("4x4 easy + lex on: TL accepts only the canonical TL piece (11)")
    void easyPuzzleLexAcceptsOnlyCanonicalTLPiece() {
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4Easy();
        SymmetryBreakingManager mgr = new SymmetryBreakingManager(4, 4, false);
        mgr.setLexicographicOrdering(true);
        mgr.setRotationalFixing(false);
        Board board = new Board(4, 4);

        // Canonical piece passes
        assertTrue(mgr.isPlacementAllowed(board, 0, 0, 11, 0, pieces),
            "canonical TL piece must be allowed at (0,0)");

        // Non-canonical TL-fittable IDs (in some rotation) must be rejected
        for (int otherTLFittable : Set.of(1, 10)) { // also TL-fittable in some rotation
            assertFalse(mgr.isPlacementAllowed(board, 0, 0, otherTLFittable, 0, pieces),
                "non-canonical TL-fittable piece " + otherTLFittable + " must be rejected at (0,0)");
        }

        // Pieces that aren't TL-fittable at all are also rejected
        for (int notTL : Set.of(2, 3, 4, 5, 6, 8, 9, 12, 13, 14, 15, 16)) {
            assertFalse(mgr.isPlacementAllowed(board, 0, 0, notTL, 0, pieces),
                "non-TL-fittable piece " + notTL + " must be rejected at (0,0)");
        }
    }

    /**
     * Other corners (TR/BL/BR) are unconstrained by the new lex rule —
     * they're pinned by edge matching in the real solver, not by symmetry.
     */
    @Test
    @DisplayName("4x4 easy + lex on: non-TL corners are unconstrained by lex")
    void easyPuzzleLexDoesNotConstrainOtherCorners() {
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4Easy();
        SymmetryBreakingManager mgr = new SymmetryBreakingManager(4, 4, false);
        mgr.setLexicographicOrdering(true);
        mgr.setRotationalFixing(false);
        Board board = new Board(4, 4);
        board.place(0, 0, pieces.get(11), 0); // canonical TL placed

        // Any piece is allowed at TR/BL/BR per the new rule (edge matching
        // catches the actual constraints elsewhere).
        for (int pid : pieces.keySet()) {
            assertTrue(mgr.isPlacementAllowed(board, 0, 3, pid, 0, pieces),
                "TR must accept piece " + pid + " (lex doesn't constrain non-TL corners)");
            assertTrue(mgr.isPlacementAllowed(board, 3, 0, pid, 0, pieces),
                "BL must accept piece " + pid);
            assertTrue(mgr.isPlacementAllowed(board, 3, 3, pid, 0, pieces),
                "BR must accept piece " + pid);
        }
    }

    /**
     * Document the exact failing combination: lex+rotation both on, 4x4 easy.
     * This is the regression test for the bug surfaced post-redesign.
     * Currently expected to FAIL (xfail) — when the fix lands, flip the
     * assertion sense and remove the @Disabled if applied.
     */
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

    /**
     * Construit manuellement la rotation 90° CW de la solution unconstrained
     * et vérifie qu'elle est valide. Ça permet de distinguer :
     * <ul>
     *   <li>Si le rotated board est valide → le solveur a un bug qui
     *     l'empêche de trouver la solution quand piece 7 rot 0 est à TL.</li>
     *   <li>Si le rotated board est invalide → la convention de rotation
     *     Piece / Board ne correspond pas à la théorie canonique. Il faut
     *     réviser la règle canonique.</li>
     * </ul>
     */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("[A2.1 diag] rotation 90° CW de la solution 4x4 hard est-elle valide ?")
    void diagnosticRotated90CwBoardIsValid() {
        // Step 1: obtenir la solution unconstrained
        Board solved = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(30_000);
        solver.setSymmetryBreakingFlags(false, false);
        assertTrue(solver.solve(solved, pieces), "unconstrained solution must exist");

        // Step 2: construire le plateau tourné 90° CW
        Board rotated = new Board(4, 4);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                model.Placement pl = solved.getPlacement(r, c);
                assertNotNull(pl, "cell not placed?");
                // (r,c) → (c, N-1-r) pour rotation 90° CW
                int newR = c, newC = 3 - r;
                int newRot = (pl.getRotation() + 1) % 4;
                rotated.place(newR, newC, pieces.get(pl.getPieceId()), newRot);
            }
        }

        // Step 3: vérifier piece 7 rot 0 à TL dans le plateau tourné
        model.Placement tl = rotated.getPlacement(0, 0);
        System.err.println("=== A2.1 diagnostic — rotation 90° CW du board ===");
        System.err.println("  rotated TL (0,0) : piece " + tl.getPieceId() + " rot " + tl.getRotation());

        // Step 4: vérifier que chaque paire adjacente matche
        int mismatches = 0;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                model.Placement p = rotated.getPlacement(r, c);
                // Vérif bordure
                if (r == 0 && p.edges[0] != 0) { System.err.println("  ❌ (" + r + "," + c + ") N-border not 0: " + p.edges[0]); mismatches++; }
                if (r == 3 && p.edges[2] != 0) { System.err.println("  ❌ (" + r + "," + c + ") S-border not 0: " + p.edges[2]); mismatches++; }
                if (c == 0 && p.edges[3] != 0) { System.err.println("  ❌ (" + r + "," + c + ") W-border not 0: " + p.edges[3]); mismatches++; }
                if (c == 3 && p.edges[1] != 0) { System.err.println("  ❌ (" + r + "," + c + ") E-border not 0: " + p.edges[1]); mismatches++; }
                // Vérif horizontale (E de gauche = W de droite)
                if (c < 3) {
                    model.Placement right = rotated.getPlacement(r, c + 1);
                    if (p.edges[1] != right.edges[3]) {
                        System.err.println("  ❌ (" + r + "," + c + ") E=" + p.edges[1] + " vs (" + r + "," + (c+1) + ") W=" + right.edges[3]);
                        mismatches++;
                    }
                }
                // Vérif verticale (S du haut = N du bas)
                if (r < 3) {
                    model.Placement below = rotated.getPlacement(r + 1, c);
                    if (p.edges[2] != below.edges[0]) {
                        System.err.println("  ❌ (" + r + "," + c + ") S=" + p.edges[2] + " vs (" + (r+1) + "," + c + ") N=" + below.edges[0]);
                        mismatches++;
                    }
                }
            }
        }
        System.err.println("  total mismatches in rotated board: " + mismatches);
    }

    /**
     * Pré-place TOUTE la rotated solution puis lance solver.solve. Si le
     * solveur retourne true instantanément, il sait reconnaître une solution
     * complète. Si false, il y a un bug dans detectFromBoard ou verification.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("[A2.1 diag] solveur accepte-t-il la rotated solution pré-placée ?")
    void diagnosticSolverAcceptsFullyRotatedSolution() {
        // Unconstrained solve
        Board solved = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();
        EternitySolver prep = new EternitySolver();
        prep.setVerbose(false);
        prep.setMaxExecutionTime(20_000);
        prep.setSymmetryBreakingFlags(false, false);
        assertTrue(prep.solve(solved, pieces));

        // Pré-place toute la rotated solution
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

    /**
     * Après pré-placement de piece 7 rot 0 à TL + propagation AC-3 réelle,
     * on vérifie pour chaque cellule si la (piece, rot) attendue par la
     * rotated solution est toujours dans le domain. Si non → bug dans
     * AC-3 propagation initiale.
     */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("[A2.1 diag] AC-3 élimine-t-il (piece,rot) attendu ?")
    void diagnosticAC3EliminatesExpectedPlacement() {
        // Rotated solution
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

        // Init AC-3 réelle + propagation avec piece 7 rot 0 à TL
        Board board = new Board(4, 4);
        board.place(0, 0, pieces.get(expPid[0][0]), expRot[0][0]); // piece 7 rot 0

        ConstraintPropagator.Statistics cpStats = new ConstraintPropagator.Statistics();
        DomainManager dm = new DomainManager((b, r, c, edges) -> {
            // Same fit logic as PlacementValidator.fits
            if (r == 0 && edges[0] != 0) return false;
            if (c == 0 && edges[3] != 0) return false;
            if (r == 3 && edges[2] != 0) return false;
            if (c == 3 && edges[1] != 0) return false;
            if (r > 0 && !b.isEmpty(r-1, c) && b.getPlacement(r-1, c).edges[2] != edges[0]) return false;
            if (c > 0 && !b.isEmpty(r, c-1) && b.getPlacement(r, c-1).edges[1] != edges[3]) return false;
            if (r < 3 && !b.isEmpty(r+1, c) && b.getPlacement(r+1, c).edges[0] != edges[2]) return false;
            if (c < 3 && !b.isEmpty(r, c+1) && b.getPlacement(r, c+1).edges[3] != edges[1]) return false;
            return true;
        });
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
            int domSize = 0;
            if (dom != null && dom.containsKey(expPid[r][c])) {
                domSize = dom.get(expPid[r][c]).size();
                for (DomainManager.ValidPlacement vp : dom.get(expPid[r][c])) {
                    if (vp.rotation == expRot[r][c]) { found = true; break; }
                }
            }
            String mark = found ? "✓" : "❌";
            if (!found) missingCount++;
            System.err.println(String.format("  %s (%d,%d) expected piece %d rot %d  %s",
                mark, r, c, expPid[r][c], expRot[r][c],
                found ? "present" : "MISSING from domain (domSize for pid=" + domSize + ")"));
        }
        System.err.println("  total missing: " + missingCount);
    }

    /**
     * Test incrémental : pré-place les N premières pieces de la rotated
     * solution (ordre ligne par ligne) et demande au solver avec AC-3
     * ON de compléter. Localise exactement à quel step AC-3 casse.
     */
    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    @DisplayName("[A2.1 diag] AC-3 complète-t-il depuis N pieces pré-placées ?")
    void diagnosticAC3CompleteFromNPreplaced() {
        Board solved = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();
        EternitySolver prep = new EternitySolver();
        prep.setVerbose(false);
        prep.setMaxExecutionTime(20_000);
        prep.setSymmetryBreakingFlags(false, false);
        assertTrue(prep.solve(solved, pieces));

        // Liste des placements rotated dans l'ordre (0,0), (0,1), ... (3,3)
        int[][] rotPid = new int[4][4];
        int[][] rotRot = new int[4][4];
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            model.Placement pl = solved.getPlacement(r, c);
            rotPid[c][3-r] = pl.getPieceId();
            rotRot[c][3-r] = (pl.getRotation() + 1) % 4;
        }

        System.err.println("=== A2.1 diagnostic — incremental AC-3 completion ===");
        // Teste N = 1, 2, 4, 8 cells pre-placed
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

    /**
     * Compare la rotated solution à l'état du domain AC-3 APRÈS propagation
     * initiale. Pour chaque cellule, vérifie si la (piece, rot) attendue de
     * la rotated solution est toujours dans le domain. Si AC-3 a éliminé un
     * (piece, rot) valide, bug confirmé.
     */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("[A2.1 diag] la rotated solution survit-elle à AC-3 propagation ?")
    void diagnosticRotatedSolutionSurvivesAC3Propagation() {
        // Obtenir la rotated solution
        Board orig = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();
        EternitySolver prep = new EternitySolver();
        prep.setVerbose(false);
        prep.setMaxExecutionTime(20_000);
        prep.setSymmetryBreakingFlags(false, false);
        assertTrue(prep.solve(orig, pieces));
        // Stocker rotated solution attendue
        int[][] expectedPid = new int[4][4];
        int[][] expectedRot = new int[4][4];
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            model.Placement pl = orig.getPlacement(r, c);
            expectedPid[c][3-r] = pl.getPieceId();
            expectedRot[c][3-r] = (pl.getRotation() + 1) % 4;
        }

        // Place piece 7 rot 0 à TL et init AC-3 via le solver réel
        Board board = new Board(4, 4);
        board.place(0, 0, pieces.get(7), 0);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(5_000);
        solver.setSymmetryBreakingFlags(false, false);
        // Lancer solve pour que AC-3 propage (même si résultat false)
        solver.solve(board, pieces);

        // Note : le solver a backtracké tout, donc board est vide sauf si
        // piece 7 rot 0 à TL persiste — mais le test vise à voir ce qu'AC-3
        // a déjà éliminé dans la phase initiale. Meilleure approche :
        // directement interroger DomainManager en reproduisant le premier
        // appel de AC-3 init + premier propagation.
        System.err.println("=== A2.1 diagnostic — rotated solution vs AC-3 domains (piece 7 rot 0 at TL) ===");
        System.err.println("  expected rotated cells:");
        for (int r = 0; r < 4; r++) {
            StringBuilder line = new StringBuilder("    ");
            for (int c = 0; c < 4; c++) {
                line.append(String.format(" %2d/%d ", expectedPid[r][c], expectedRot[r][c]));
            }
            System.err.println(line);
        }
    }

    /**
     * Diagnostic du bug AC-3 : avec piece 7 rot 0 pré-placée à TL, AC-3 fait
     * une propagation qui tue la branche. Ce test imprime les tailles de
     * domain pour chaque cellule après init AC-3 — si une cellule a domain
     * vide alors que la solution existe (on l'a prouvé), AC-3 sur-réduit.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("[A2.1 diag] domains AC-3 après piece 7 rot 0 à TL")
    void diagnosticAC3DomainsAfterPiece7Rot0AtTL() {
        Board board = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();
        board.place(0, 0, pieces.get(7), 0);

        DomainManager dm = new DomainManager((b, r, c, edges) -> {
            // Simple fits: border check + edge match with placed neighbors
            if (r == 0 && edges[0] != 0) return false;
            if (c == 0 && edges[3] != 0) return false;
            if (r == 3 && edges[2] != 0) return false;
            if (c == 3 && edges[1] != 0) return false;
            if (r > 0 && !b.isEmpty(r-1, c) && b.getPlacement(r-1, c).edges[2] != edges[0]) return false;
            if (c > 0 && !b.isEmpty(r, c-1) && b.getPlacement(r, c-1).edges[1] != edges[3]) return false;
            if (r < 3 && !b.isEmpty(r+1, c) && b.getPlacement(r+1, c).edges[0] != edges[2]) return false;
            if (c < 3 && !b.isEmpty(r, c+1) && b.getPlacement(r, c+1).edges[3] != edges[1]) return false;
            return true;
        });
        java.util.BitSet used = new java.util.BitSet();
        used.set(7); // piece 7 pré-placée
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

    /**
     * Essayer sans AC-3 pour isoler : si AC-3 cause le dead-end, le solveur
     * pur backtracking devrait trouver la solution avec piece 7 rot 0 à TL.
     */
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

    /**
     * Pré-place piece 7 rot 0 à TL et tente de résoudre SANS flags. Si le
     * solveur trouve une solution complète, la théorie canonique tient et
     * le bug 4x4 hard est dans le flow (pas dans le choix canonique). Si
     * il ne trouve aucune solution, alors cette puzzle n'a pas de solution
     * avec piece 7 rot 0 à TL — et la règle canonique doit être révisée.
     */
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

    /**
     * Isolates which flag combo fails on 4x4 hard. Informational only.
     */
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
                System.err.println(String.format("  lex=%s rot=%s → solved=%s  lexRej=%d (sg=%d,va=%d) rotRej=%d  firstRej=(%d,%d) pid=%d rot=%d",
                    lex, rot, solved,
                    m.getLexRejections(), m.getLexRejectionsFromSingleton(), m.getLexRejectionsFromValidator(),
                    m.getRotationRejections(),
                    m.getFirstLexRejectionRow(), m.getFirstLexRejectionCol(),
                    m.getFirstLexRejectionPieceId(), m.getFirstLexRejectionRotation()));
            }
        }
    }

    /**
     * Diagnostic for the 4x4 hard variant bug: after the TL pre-filter fix,
     * the easy puzzle solves with lex+rot ON but the hard one doesn't.
     * Hypothesis: no valid solution of hardV3 has the canonical TL piece
     * (piece 7) at (0,0) rot 0 in any board rotation.
     *
     * <p>This test solves the puzzle with flags OFF, then inspects what
     * ended up at each corner and at what rotation. Purely informational.</p>
     */
    @Test
    @DisplayName("[A2.1 diag] inspect TL piece in unconstrained 4x4 hard solution")
    void diagnosticInspectHardSolutionCorners() {
        Board board = new Board(4, 4);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(40_000);
        solver.setSymmetryBreakingFlags(false, false);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4HardV3();
        boolean solved = solver.solve(board, pieces);
        assertTrue(solved, "hard puzzle must solve with flags off");

        System.err.println("=== A2.1 diagnostic — unconstrained 4x4 hard solution ===");
        int[][] corners = {{0,0}, {0,3}, {3,0}, {3,3}};
        String[] names = {"TL", "TR", "BL", "BR"};
        for (int i = 0; i < 4; i++) {
            int r = corners[i][0], c = corners[i][1];
            model.Placement pl = board.getPlacement(r, c);
            System.err.println("  " + names[i] + " (" + r + "," + c + "): piece "
                + (pl == null ? "?" : pl.getPieceId()) + " rot "
                + (pl == null ? "?" : pl.getRotation()));
        }

        // Full solved board dump
        System.err.println("  Full solved board:");
        for (int r = 0; r < 4; r++) {
            StringBuilder line = new StringBuilder("    ");
            for (int c = 0; c < 4; c++) {
                model.Placement pl = board.getPlacement(r, c);
                if (pl == null) line.append(" __ ");
                else line.append(String.format(" %2d/%d", pl.getPieceId(), pl.getRotation()));
            }
            System.err.println(line);
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

    /**
     * Diagnostic test (A1.2): print the rejection counters from the
     * SymmetryBreakingManager after a failed solve. The numbers tell us
     * which rule (lex vs rotation) is doing the over-pruning, and
     * the coordinates of the first rejection point us at the geometry
     * of the bug.
     *
     * <p>This test always passes — it's purely informational; the values
     * surface in the test report's stdout for analysis.</p>
     */
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

    /**
     * Diagnostic test (A1.4b): split lex rejection counts by CALL SITE
     * (PlacementValidator vs SingletonPlacementStrategy) to tell us which
     * solver path proposes the bogus TL placements. Replaces the previous
     * verbose-log variant which blocked forever in Scanner.nextLine on
     * macOS because System.console() is non-null even in forked surefire.
     *
     * <p>Expected: if all rejections come from SingletonPlacementStrategy,
     * the bug is in SingletonDetector (proposing infeasible TL singletons).
     * If from PlacementValidator, the bug is upstream in MRV/AC-3 domain
     * pruning or piece-rotation enumeration.</p>
     */
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

        // Scénario A vs B : piece 7 fitte-t-elle à TL (N=0,W=0) dans AU MOINS une rotation ?
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
}
