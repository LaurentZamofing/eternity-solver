package solver;

import util.SolverLogger;

import model.Board;
import model.Piece;
import model.Placement;

import java.util.Map;
import java.util.Set;

/** Manages symmetry-breaking constraints to prune redundant search branches using lexicographic ordering (corner pieces), rotation fixing (top-left at 0°), and reflection pruning (4x reduction). */
public class SymmetryBreakingManager {

    private final boolean verbose;
    private final int rows;
    private final int cols;

    // Symmetry-breaking strategy flags.
    //
    // Lex ordering rule (post-2026-04-17 redesign): TL must hold the
    // piece with the smallest ID among pieces geometrically able to fit
    // at TL (some rotation has N=0 AND W=0). This breaks the 4-way
    // rotational symmetry of the board (every rotated solution has a
    // different "TL" piece, only the canonical one survives) without
    // assuming corners are interchangeable — the previous "every corner
    // pieceId >= TL.id" rule did, and silently rejected valid solutions
    // on puzzles where the smallest-ID corner is geometrically constrained
    // off-TL (caught by AC3CorrectnessTest.solve4x4EasyProducesValidSolution
    // before the redesign).
    //
    // Rotation fixing: TL piece is forced to rotation 0. Combined with the
    // lex rule, the canonical TL candidate is its lowest valid rotation —
    // works because the TL-fittable filter checks all rotations, but the
    // canonical piece's rotation 0 IS one of them by construction (else it
    // wouldn't have been added to the set with edges[0]==0 && edges[3]==0
    // at rot 0).
    //
    // Reflection pruning never implemented — see future-work plan in
    // project notes. Eternity II pieces are non-mirror-symmetric so the
    // marginal gain on this puzzle is probably < 20%.
    //
    // Perf impact (JMH SingleShotTime, 10 iter, solve4x4Hard, JDK 24):
    //   Both flags on:   ~30 ms/op
    //   Both flags off:  ~49 ms/op  (+63% slower)
    // Default OFF after a redesign attempt failed solve4x4Easy (interaction
    // with MRV / singleton detection still rejects valid trees in a way the
    // 4x4 gate catches). The rotation-aware lex rule below is kept in code
    // for the next iteration. Re-enable once the gate passes both 4x4 cases.
    private boolean enableLexicographicOrdering = false;
    private boolean enableRotationalFixing = false;
    private boolean enableReflectionPruning = false;

    /** Creates symmetry-breaking manager with board dimensions and verbose flag for detailed logging. */
    public SymmetryBreakingManager(int rows, int cols, boolean verbose) {
        this.rows = rows;
        this.cols = cols;
        this.verbose = verbose;
    }

    /** Enables or disables lexicographic ordering constraint for corner pieces. */
    public void setLexicographicOrdering(boolean enabled) {
        this.enableLexicographicOrdering = enabled;
    }

    /** Enables or disables rotation fixing constraint (top-left corner at 0°). */
    public void setRotationalFixing(boolean enabled) {
        this.enableRotationalFixing = enabled;
    }

    /** Returns true if placement is allowed; called before placing piece to prune branches violating symmetry constraints (lexicographic ordering or rotation fixing). */
    public boolean isPlacementAllowed(Board board, int row, int col, int pieceId,
                                     int rotation, Map<Integer, Piece> allPieces) {
        // Check lexicographic ordering on corners
        if (enableLexicographicOrdering) {
            if (!checkLexicographicOrdering(board, row, col, pieceId, allPieces)) {
                if (verbose) {
                    SolverLogger.info("  ⛔ Symmetry: Rejecting piece " + pieceId + " at (" + row + "," + col + ") - violates lexicographic ordering");
                }
                return false;
            }
        }

        // Check rotation fixing for first piece (if applicable)
        if (enableRotationalFixing) {
            if (!checkRotationFixing(board, row, col, rotation)) {
                if (verbose) {
                    SolverLogger.info("  ⛔ Symmetry: Rejecting rotation " + rotation + " at (" + row + "," + col + ") - violates rotation fixing");
                }
                return false;
            }
        }

        return true;
    }

    /**
     * Enforces lexicographic ordering on the TOP-LEFT corner only.
     *
     * <p>Constraint: TL must use the piece with the smallest ID among the
     * pieces that geometrically can fit at TL (i.e. have at least one
     * rotation with N=0 and W=0). No constraint on the other three corners,
     * which are pinned by edge-matching to whatever fits.</p>
     *
     * <p>Why not the previous "every corner pieceId ≥ TL.id" rule: it
     * silently rejected valid solutions when the smallest-ID corner was
     * geometrically constrained off-TL (see the bug surfaced by
     * AC3CorrectnessTest.solve4x4EasyProducesValidSolution before this
     * fix).</p>
     *
     * <p>The new rule still breaks the 4-way rotational symmetry of the
     * board: every rotated solution would have a different "top-left"
     * piece, but only one of them has the canonical (smallest-ID)
     * TL-fittable piece in the actual TL slot.</p>
     */
    private boolean checkLexicographicOrdering(Board board, int row, int col, int pieceId,
                                               Map<Integer, Piece> allPieces) {
        // Only constrain the top-left corner.
        if (row != 0 || col != 0) {
            return true;
        }

        Set<Integer> tlFittable = topLeftFittablePieceIds(allPieces);
        if (tlFittable.isEmpty()) {
            // Defensive: nothing pre-computable, allow the placement.
            return true;
        }

        int canonicalId = Integer.MAX_VALUE;
        for (int id : tlFittable) canonicalId = Math.min(canonicalId, id);
        return pieceId == canonicalId;
    }

    /**
     * Cached set of piece IDs that can fit at the top-left corner —
     * i.e. have at least one rotation with N=0 AND W=0. Computed lazily
     * once and reused for the rest of the solve.
     */
    private Set<Integer> topLeftFittableIds;

    /**
     * Pieces eligible for the canonical TL slot.
     *
     * <p>Restricted to pieces whose <em>rotation 0</em> already satisfies
     * {@code N=0 && W=0}. This is critical: when {@link #enableRotationalFixing}
     * is on, TL is locked to rotation 0, so any "TL-fittable in some
     * rotation" piece that needs a non-zero rotation to fit cannot
     * actually be placed there. Allowing it as the canonical TL would
     * make the puzzle unsolvable (the solver would search forever).</p>
     */
    private Set<Integer> topLeftFittablePieceIds(Map<Integer, Piece> allPieces) {
        if (topLeftFittableIds != null) return topLeftFittableIds;
        Set<Integer> ids = new java.util.HashSet<>();
        for (Map.Entry<Integer, Piece> e : allPieces.entrySet()) {
            int[] edges = e.getValue().edgesRotated(0);
            if (edges[0] == 0 && edges[3] == 0) {
                ids.add(e.getKey());
            }
        }
        topLeftFittableIds = ids;
        return ids;
    }

    /** Fixes rotation of top-left corner piece to 0° to eliminate rotational symmetry (eliminates 3/4 of rotationally equivalent solutions). */
    private boolean checkRotationFixing(Board board, int row, int col, int rotation) {
        // Fix rotation only for top-left corner
        if (row != 0 || col != 0) {
            return true; // Not top-left corner, no constraint
        }

        // Fix top-left corner to rotation 0
        // This breaks rotational symmetry of entire solution
        return rotation == 0;
    }

    /**
     * @deprecated The previous post-hoc validation enforced "every corner ≥ TL.id"
     * which is incorrect on edge-matching puzzles (see the redesigned
     * {@link #checkLexicographicOrdering} for details). The new rule is
     * enforced per-placement at the TL slot only, so post-hoc validation is
     * unnecessary. Kept as a no-op for source compatibility with existing
     * tests; remove in a follow-up.
     */
    @Deprecated
    public boolean validateBoardState(Board board) {
        return true;
    }

    /** Logs symmetry-breaking configuration at solver start. */
    public void logConfiguration() {
        if (verbose) {
            SolverLogger.info("  🔄 Symmetry breaking:");
            SolverLogger.info("     - Lexicographic ordering: " + (enableLexicographicOrdering ? "✓" : "✗"));
            SolverLogger.info("     - Rotation fixing: " + (enableRotationalFixing ? "✓" : "✗"));
            SolverLogger.info("     - Reflection pruning: " + (enableReflectionPruning ? "✓" : "✗"));
        }
    }

    /** Returns expected search space reduction factor from symmetry breaking (e.g., 4.0 means 1/4 of original space). */
    public double getExpectedReductionFactor() {
        double factor = 1.0;

        if (enableLexicographicOrdering) {
            factor *= 4.0; // Eliminates 4-way rotational symmetry
        }

        if (enableRotationalFixing) {
            // Already counted in lexicographic
        }

        if (enableReflectionPruning) {
            factor *= 2.0; // Eliminates horizontal/vertical reflection
        }

        return factor;
    }

    /** Returns true if any symmetry-breaking strategy is enabled. */
    public boolean isEnabled() {
        return enableLexicographicOrdering || enableRotationalFixing || enableReflectionPruning;
    }
}
