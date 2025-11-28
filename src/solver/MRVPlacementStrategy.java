package solver;

import solver.heuristics.LeastConstrainingValueOrderer;
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

    private final boolean verbose;
    private final LeastConstrainingValueOrderer valueOrderer;
    private final SymmetryBreakingManager symmetryBreakingManager;
    private final ConstraintPropagator constraintPropagator;
    private final DomainManager domainManager;

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
        this.verbose = verbose;
        this.valueOrderer = valueOrderer;
        this.symmetryBreakingManager = symmetryBreakingManager;
        this.constraintPropagator = constraintPropagator;
        this.domainManager = domainManager;
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

        // Verbose output
        if (verbose) {
            int uniquePieces = solver.countUniquePieces(context.board, r, c, context.piecesById,
                                                       context.pieceUsed, context.totalPieces);
            int availableCount = context.countAvailablePieces();

            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║  Step " + (solver.getStepCount() + 1) + " - Cell (" + r + ", " + c + ")");
            System.out.println("║  Available pieces: " + availableCount);
            System.out.println("║  Possible pieces here: " + uniquePieces);
            System.out.println("╚════════════════════════════════════════╝");

            context.stats.printCompact();
            solver.printBoardWithCounts(context.board, context.piecesById, context.pieceUsed,
                                       context.totalPieces, solver.getLastPlacedRow(), solver.getLastPlacedCol());
        }

        // Build list of available pieces
        List<Integer> snapshot = new ArrayList<>();
        for (int i = 1; i <= context.totalPieces; i++) {
            if (!context.pieceUsed.get(i)) {
                snapshot.add(i);
            }
        }

        // Smart piece ordering: sort by difficulty (hardest first for fail-fast)
        if (valueOrderer.getAllDifficultyScores() != null) {
            snapshot.sort(Comparator.comparingInt(pid ->
                valueOrderer.getAllDifficultyScores().getOrDefault(pid, Integer.MAX_VALUE)));
        } else {
            Collections.sort(snapshot);  // Fallback to ID order for determinism
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

                // Check basic fit
                if (!solver.fits(context.board, r, c, candidate)) {
                    continue;
                }

                // Symmetry breaking check
                if (symmetryBreakingManager != null &&
                    !symmetryBreakingManager.isPlacementAllowed(context.board, r, c, pid, rot, context.piecesById)) {
                    continue;
                }

                context.stats.placements++;

                // Verbose output
                if (verbose) {
                    System.out.println("  → Trying piece ID=" + pid + ", rotation=" + (rot * 90) +
                                     "° (option " + (optionIndex + 1) + "/" + snapshot.size() + ")");
                }

                // Place piece
                context.board.place(r, c, piece, rot);
                context.pieceUsed.set(pid);
                solver.setLastPlaced(r, c);
                solver.incrementStepCount();
                solver.recordPlacement(r, c, pid, rot);

                // AC-3 constraint propagation
                if (!constraintPropagator.propagateAC3(context.board, r, c, pid, rot,
                                                      context.piecesById, context.pieceUsed, context.totalPieces)) {
                    // Dead end detected by AC-3
                    if (verbose) {
                        System.out.println("✗ AC-3 dead-end detected: ID=" + pid + " at (" + r + ", " + c + ")");
                    }
                    context.stats.deadEndsDetected++;

                    // Backtrack
                    context.pieceUsed.clear(pid);
                    context.board.remove(r, c);
                    solver.removeLastPlacement();
                    domainManager.restoreAC3Domains(context.board, r, c, context.piecesById,
                                                   context.pieceUsed, context.totalPieces);
                    continue;
                }

                // Verbose output
                if (verbose) {
                    solver.printBoardWithCounts(context.board, context.piecesById, context.pieceUsed,
                                              context.totalPieces, r, c);
                    System.out.println("✓ Piece placed: ID=" + pid + ", Rotation=" + (rot * 90) +
                                     "°, Edges=" + java.util.Arrays.toString(candidate));
                }

                // Recursive call
                boolean solved = solver.solveBacktracking(context.board, context.piecesById,
                                                         context.pieceUsed, context.totalPieces);
                if (solved) {
                    return true;
                }

                // Backtrack
                context.stats.backtracks++;
                if (verbose) {
                    System.out.println("✗ BACKTRACK: Removing piece ID=" + pid + " at (" + r + ", " + c + ")");
                }

                context.pieceUsed.clear(pid);
                context.board.remove(r, c);
                solver.removeLastPlacement();
                domainManager.restoreAC3Domains(context.board, r, c, context.piecesById,
                                               context.pieceUsed, context.totalPieces);

                // Update last placed tracking
                solver.findAndSetLastPlaced(context.board);
            }

            optionIndex++;
        }

        // No solution found with any piece at this cell
        return false;
    }
}
