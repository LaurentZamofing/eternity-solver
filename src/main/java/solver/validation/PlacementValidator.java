package solver.validation;

import model.Board;
import model.Piece;
import solver.BacktrackingContext;
import solver.ConstraintPropagator;
import solver.DomainManager;
import solver.EternitySolver;
import solver.SymmetryBreakingManager;

import java.util.Map;

/**
 * Validates piece placements against all constraints.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li><b>Basic fit checking</b> - Edge compatibility with neighbors and borders</li>
 *   <li><b>Symmetry breaking</b> - Prevent symmetric duplicate solutions</li>
 *   <li><b>AC-3 propagation</b> - Forward checking and constraint propagation</li>
 * </ul>
 *
 * <h2>Validation Steps</h2>
 * <pre>
 * 1. Check basic fit (edges match neighbors/borders)
 * 2. Check symmetry breaking constraints
 * 3. Propagate with AC-3 (lookahead)
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>
 * PlacementValidator validator = new PlacementValidator(
 *     symmetryManager,
 *     constraintPropagator,
 *     domainManager
 * );
 *
 * ValidationResult result = validator.validate(
 *     context, solver, row, col, piece, rotation
 * );
 *
 * if (result.isValid()) {
 *     // Proceed with placement
 * } else {
 *     // Handle rejection: result.getReason()
 * }
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class PlacementValidator {

    private final SymmetryBreakingManager symmetryBreakingManager;
    private final ConstraintPropagator constraintPropagator;
    private final DomainManager domainManager;

    /**
     * Creates a placement validator.
     *
     * @param symmetryBreakingManager Symmetry breaking manager (can be null)
     * @param constraintPropagator AC-3 constraint propagator
     * @param domainManager Domain manager for AC-3
     */
    public PlacementValidator(SymmetryBreakingManager symmetryBreakingManager,
                             ConstraintPropagator constraintPropagator,
                             DomainManager domainManager) {
        this.symmetryBreakingManager = symmetryBreakingManager;
        this.constraintPropagator = constraintPropagator;
        this.domainManager = domainManager;
    }

    /**
     * Validates a piece placement against pre-placement constraints.
     *
     * <p><b>Important:</b> This validates constraints that can be checked BEFORE
     * placing the piece. AC-3 propagation happens AFTER placement in the main flow.</p>
     *
     * @param context Backtracking context
     * @param solver Solver instance
     * @param row Target row
     * @param col Target column
     * @param pieceId Piece ID
     * @param rotation Rotation (0-3)
     * @param edges Rotated edges [N, E, S, W]
     * @return Validation result with status and reason
     */
    public ValidationResult validate(BacktrackingContext context, EternitySolver solver,
                                     int row, int col, int pieceId, int rotation, int[] edges) {
        // Step 1: Check basic fit (edges match neighbors/borders)
        if (!solver.fits(context.board, row, col, edges)) {
            return ValidationResult.rejected(RejectionReason.EDGE_MISMATCH);
        }

        // Step 2: Check symmetry breaking constraints
        if (symmetryBreakingManager != null &&
            !symmetryBreakingManager.isPlacementAllowed(context.board, row, col, pieceId,
                                                       rotation, context.piecesById)) {
            return ValidationResult.rejected(RejectionReason.SYMMETRY_BREAKING);
        }

        // All pre-placement constraints satisfied
        return ValidationResult.valid();
    }

    /**
     * Propagates AC-3 constraints after piece placement.
     *
     * <p><b>Important:</b> Call this AFTER placing the piece on the board.</p>
     *
     * @param context Backtracking context
     * @param row Row position
     * @param col Column position
     * @param pieceId Piece ID
     * @param rotation Rotation (0-3)
     * @return true if constraints are consistent, false if dead end detected
     */
    public boolean propagateConstraints(BacktrackingContext context,
                                       int row, int col, int pieceId, int rotation) {
        return constraintPropagator.propagateAC3(context.board, row, col, pieceId, rotation,
                                                context.piecesById, context.pieceUsed, context.totalPieces);
    }

    /**
     * Restores AC-3 domains after a failed placement or backtrack.
     *
     * @param context Backtracking context
     * @param row Row position
     * @param col Column position
     */
    public void restoreDomains(BacktrackingContext context, int row, int col) {
        domainManager.restoreAC3Domains(context.board, row, col, context.piecesById,
                                       context.pieceUsed, context.totalPieces);
    }

    /**
     * Result of a placement validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final RejectionReason reason;

        private ValidationResult(boolean valid, RejectionReason reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult rejected(RejectionReason reason) {
            return new ValidationResult(false, reason);
        }

        public boolean isValid() {
            return valid;
        }

        public RejectionReason getReason() {
            return reason;
        }
    }

    /**
     * Reasons for rejecting a pre-placement validation.
     */
    public enum RejectionReason {
        /** Edges don't match neighbor constraints or borders */
        EDGE_MISMATCH,

        /** Placement violates symmetry breaking rules */
        SYMMETRY_BREAKING
    }
}
