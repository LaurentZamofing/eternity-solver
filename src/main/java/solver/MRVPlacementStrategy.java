package solver;

import solver.heuristics.LeastConstrainingValueOrderer;
import solver.output.PlacementOutputStrategy;
import solver.output.VerbosePlacementOutput;
import solver.output.QuietPlacementOutput;
import solver.validation.PlacementValidator;
import solver.validation.PlacementValidator.ValidationResult;
import solver.validation.PlacementValidator.RejectionReason;
import solver.timeout.TimeoutChecker;
import model.Board;
import model.Piece;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Placement strategy using Minimum Remaining Values (MRV) heuristic.
 *
 * This is the standard backtracking approach that:
 * 1. Selects the cell with fewest valid placements (MRV)
 * 2. Tries pieces in smart order (hardest first for fail-fast)
 * 3. Uses forward checking and constraint propagation
 * 4. Backtracks when dead ends are found
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class MRVPlacementStrategy implements PlacementStrategy {

    private final PlacementOutputStrategy outputStrategy;
    private final PlacementValidator validator;
    private final TimeoutChecker timeoutChecker;
    private final LeastConstrainingValueOrderer valueOrderer;
    private String sortOrder = "ascending"; // Default sort order

    /**
     * Creates an MRV placement strategy.
     *
     * @param verbose Whether to output verbose logging
     * @param valueOrderer Orderer for smart piece selection
     * @param symmetryBreakingManager Manager for symmetry breaking (can be null)
     * @param constraintPropagator AC-3 constraint propagator
     * @param domainManager Domain manager for AC-3
     */
    public MRVPlacementStrategy(boolean verbose, LeastConstrainingValueOrderer valueOrderer,
                               SymmetryBreakingManager symmetryBreakingManager,
                               ConstraintPropagator constraintPropagator, DomainManager domainManager) {
        this.outputStrategy = verbose ? new VerbosePlacementOutput() : new QuietPlacementOutput();
        this.validator = new PlacementValidator(symmetryBreakingManager, constraintPropagator, domainManager);
        this.timeoutChecker = new TimeoutChecker();
        this.valueOrderer = valueOrderer;
    }

    /**
     * Sets the sort order for piece iteration.
     * @param sortOrder "ascending" or "descending"
     */
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder : "ascending";
    }

    @Override
    public boolean tryPlacement(BacktrackingContext context, EternitySolver solver) {
        // Find next cell using MRV heuristic
        int[] cell = solver.findNextCellMRV(context.board, context.piecesById,
                                           context.pieceUsed, context.totalPieces);

        if (cell == null) {
            // No empty cell found - should not happen as this is checked before calling strategies
            return false;
        }

        int r = cell[0];
        int c = cell[1];

        // Log cell selection with constraints
        int uniquePieces = solver.countUniquePieces(context.board, r, c, context.piecesById,
                                                   context.pieceUsed, context.totalPieces);
        int availableCount = context.countAvailablePieces();
        outputStrategy.logCellSelection(context, solver, r, c, uniquePieces, availableCount);

        // Build list of available pieces
        List<Integer> snapshot = new ArrayList<>();
        for (int i = 1; i <= context.totalPieces; i++) {
            if (!context.pieceUsed.get(i)) {
                snapshot.add(i);
            }
        }

        // Sort pieces according to sortOrder configuration
        // Note: We use piece ID order (not difficulty) to ensure ascending/descending works correctly
        if ("descending".equals(sortOrder)) {
            snapshot.sort(Collections.reverseOrder());  // Descending: highest ID first
        } else {
            Collections.sort(snapshot);  // Ascending: lowest ID first (default)
        }

        // Register depth options for progress tracking (first 5 depths only)
        int currentDepth = context.getCurrentDepth();
        if (currentDepth < 5) {
            context.stats.registerDepthOptions(currentDepth, snapshot.size());
        }

        // Try each piece
        int optionIndex = 0;
        for (int pid : snapshot) {
            // Increment progress for this depth
            if (currentDepth < 5) {
                context.stats.incrementDepthProgress(currentDepth);
            }

            Piece piece = context.piecesById.get(pid);

            // Try each rotation
            for (int rot = 0; rot < 4; rot++) {
                int[] candidate = piece.edgesRotated(rot);

                // Log placement attempt
                outputStrategy.logPlacementAttempt(pid, rot, r, c, optionIndex, snapshot.size(), candidate);

                // Validate pre-placement constraints (basic fit + symmetry)
                ValidationResult result = validator.validate(context, solver, r, c, pid, rot, candidate);
                if (!result.isValid()) {
                    if (result.getReason() == RejectionReason.EDGE_MISMATCH) {
                        outputStrategy.logEdgeRejection();
                    } else if (result.getReason() == RejectionReason.SYMMETRY_BREAKING) {
                        outputStrategy.logSymmetryRejection();
                    }
                    continue;
                }

                context.stats.placements++;
                outputStrategy.logConstraintsSatisfied();

                // Place piece
                context.board.place(r, c, piece, rot);
                context.pieceUsed.set(pid);
                solver.setLastPlaced(r, c);
                solver.incrementStepCount();
                solver.recordPlacement(r, c, pid, rot);

                // AC-3 constraint propagation (after placement)
                if (!validator.propagateConstraints(context, r, c, pid, rot)) {
                    // Dead end detected by AC-3
                    outputStrategy.logAC3DeadEnd(pid);
                    context.stats.deadEndsDetected++;

                    // Backtrack
                    context.pieceUsed.clear(pid);
                    context.board.remove(r, c);
                    solver.removeLastPlacement();
                    validator.restoreDomains(context, r, c);
                    continue;
                }

                // Log successful placement
                outputStrategy.logSuccessfulPlacement(context, solver, r, c);

                // Check timeout only AFTER successful placement (not during backtracking)
                // This ensures saved state always contains a stable configuration
                if (timeoutChecker.isTimedOut(context)) {
                    outputStrategy.logTimeout(pid, r, c);
                    // Don't explore deeper, but keep this piece placed for save
                    // This prevents duplicate work on resume because:
                    // - Saved state has piece N placed → pieces 1..N-1 implicitly tested
                    // - Resume will continue from piece N+1 (not retry 1..N)
                    return false;
                }

                // Recursive call
                boolean solved = solver.solveBacktracking(context.board, context.piecesById,
                                                         context.pieceUsed, context.totalPieces);
                if (solved) {
                    return true;
                }

                // Backtrack
                context.stats.backtracks++;
                outputStrategy.logBacktrack(pid, r, c, (int) context.stats.backtracks);

                context.pieceUsed.clear(pid);
                context.board.remove(r, c);
                solver.removeLastPlacement();
                validator.restoreDomains(context, r, c);

                // Update last placed tracking
                solver.findAndSetLastPlaced(context.board);
            }

            optionIndex++;
        }

        // No solution found with any piece at this cell
        outputStrategy.logExhaustedOptions(r, c, snapshot.size());
        return false;
    }
}
