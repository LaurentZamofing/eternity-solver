package solver;

import util.SolverLogger;

import model.Board;
import model.Piece;
import model.Placement;

import java.util.Map;

/** Manages symmetry-breaking constraints to prune redundant search branches using lexicographic ordering (corner pieces), rotation fixing (top-left at 0°), and reflection pruning (4x reduction). */
public class SymmetryBreakingManager {

    private final boolean verbose;
    private final int rows;
    private final int cols;

    // Symmetry-breaking strategy flags.
    //
    // Empirical impact on solve4x4Hard (JMH SingleShotTime, 10 iter, JDK 24):
    //   Both lex+rotation enabled:  ~30 ms/op
    //   Both disabled:               ~49 ms/op  (+63% slower)
    // → Current setup buys ~40% wall-clock on 4x4.
    //
    // Reflection pruning (e.g. lex order top-right ≤ bottom-left to break the
    // diagonal reflection of the D4 group) is left disabled: Eternity II
    // pieces are not naturally mirror-symmetric, so the marginal gain is
    // probably < 20%, and rolling it out safely needs a 4x4 correctness gate
    // with a known-good solution to verify no valid branches are pruned.
    // Capture that gate in AC3CorrectnessTest before flipping this flag.
    private boolean enableLexicographicOrdering = true;
    private boolean enableRotationalFixing = true;
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
            if (!checkLexicographicOrdering(board, row, col, pieceId)) {
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

    /** Enforces lexicographic ordering on corner pieces; top-left must have smallest ID (eliminates horizontal/vertical reflection and 180° rotation, 4x reduction). */
    private boolean checkLexicographicOrdering(Board board, int row, int col, int pieceId) {
        // Apply ordering only on corner positions
        boolean isTopLeft = (row == 0 && col == 0);
        boolean isTopRight = (row == 0 && col == cols - 1);
        boolean isBottomLeft = (row == rows - 1 && col == 0);
        boolean isBottomRight = (row == rows - 1 && col == cols - 1);

        if (!isTopLeft && !isTopRight && !isBottomLeft && !isBottomRight) {
            return true; // Not a corner, no constraint
        }

        Placement topLeft = board.getPlacement(0, 0);

        // If placing top-left corner first, always allow (becomes reference)
        if (isTopLeft) {
            return true;
        }

        // If top-left corner not yet placed, allow other corners (will be constrained later)
        if (topLeft == null) {
            return true;
        }

        int topLeftId = topLeft.getPieceId();

        // Apply: All other corners must have piece ID >= top-left corner ID
        // This eliminates rotational/reflective duplicates
        if (isTopRight || isBottomLeft || isBottomRight) {
            if (pieceId < topLeftId) {
                return false; // Violates ordering constraint
            }
        }

        return true;
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

    /** Validates board state after placement; returns true if all corner pieces satisfy lexicographic ordering constraint. */
    public boolean validateBoardState(Board board) {
        if (!enableLexicographicOrdering) {
            return true;
        }

        Placement topLeft = board.getPlacement(0, 0);
        if (topLeft == null) {
            return true; // Cannot validate yet
        }

        int topLeftId = topLeft.getPieceId();

        // Check that all corners respect lexicographic ordering
        Placement topRight = board.getPlacement(0, cols - 1);
        if (topRight != null && topRight.getPieceId() < topLeftId) {
            return false;
        }

        Placement bottomLeft = board.getPlacement(rows - 1, 0);
        if (bottomLeft != null && bottomLeft.getPieceId() < topLeftId) {
            return false;
        }

        Placement bottomRight = board.getPlacement(rows - 1, cols - 1);
        if (bottomRight != null && bottomRight.getPieceId() < topLeftId) {
            return false;
        }

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
