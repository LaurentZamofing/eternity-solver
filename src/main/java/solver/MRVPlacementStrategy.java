package solver;

import util.SolverLogger;

import solver.heuristics.LeastConstrainingValueOrderer;
import model.Board;
import model.Piece;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

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
        this.verbose = verbose;
        this.valueOrderer = valueOrderer;
        this.symmetryBreakingManager = symmetryBreakingManager;
        this.constraintPropagator = constraintPropagator;
        this.domainManager = domainManager;
    }

    /**
     * Sets the sort order for piece iteration.
     * @param sortOrder "ascending" or "descending"
     */
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder : "ascending";
    }

    /**
     * Waits for user to press Enter in verbose mode.
     * Skips waiting in test environments to prevent test hangs.
     */
    private void waitForEnter() {
        // Skip waiting if:
        // 1. System console is null (tests, background execution, redirected stdin)
        // 2. stdin is not ready (tests with System.in redirection)
        if (System.console() == null) {
            return; // Non-interactive mode
        }

        try {
            // Check if stdin is actually available
            if (System.in.available() == 0) {
                // Only wait if there's a real terminal
                SolverLogger.info("\n[Press Enter to continue...]");
                Scanner scanner = new Scanner(System.in);
                scanner.nextLine();
            }
        } catch (java.io.IOException e) {
            // If stdin check fails, skip waiting (test environment)
            SolverLogger.debug("stdin not available: " + e.getMessage());
        }
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

            SolverLogger.info("\n╔════════════════════════════════════════════════════════════════╗");
            System.out.println("║  Step " + (solver.getStepCount() + 1) + " - Choosing next cell");
            System.out.println("║  Cell selected: (" + r + ", " + c + ")");
            System.out.println("║  Reason: MRV (Minimum Remaining Values) heuristic");
            System.out.println("║  → This cell has only " + uniquePieces + " valid pieces (satisfy constraints)");
            System.out.println("║  → Total unused pieces: " + availableCount + " (will test all, most rejected)");

            // Show required edges for this cell
            SolverLogger.info("║");
            SolverLogger.info("║  Constraints for this cell:");
            // North constraint
            if (r > 0 && !context.board.isEmpty(r - 1, c)) {
                int requiredNorth = context.board.getPlacement(r - 1, c).edges[2];
                System.out.println("║  → North edge must be: " + requiredNorth + " (match with cell above)");
            } else if (r == 0) {
                System.out.println("║  → North edge must be: 0 (border)");
            }
            // East constraint
            if (c < context.board.getCols() - 1 && !context.board.isEmpty(r, c + 1)) {
                int requiredEast = context.board.getPlacement(r, c + 1).edges[3];
                System.out.println("║  → East edge must be: " + requiredEast + " (match with cell on right)");
            } else if (c == context.board.getCols() - 1) {
                System.out.println("║  → East edge must be: 0 (border)");
            }
            // South constraint
            if (r < context.board.getRows() - 1 && !context.board.isEmpty(r + 1, c)) {
                int requiredSouth = context.board.getPlacement(r + 1, c).edges[0];
                System.out.println("║  → South edge must be: " + requiredSouth + " (match with cell below)");
            } else if (r == context.board.getRows() - 1) {
                System.out.println("║  → South edge must be: 0 (border)");
            }
            // West constraint
            if (c > 0 && !context.board.isEmpty(r, c - 1)) {
                int requiredWest = context.board.getPlacement(r, c - 1).edges[1];
                System.out.println("║  → West edge must be: " + requiredWest + " (match with cell on left)");
            } else if (c == 0) {
                System.out.println("║  → West edge must be: 0 (border)");
            }
            SolverLogger.info("╚════════════════════════════════════════════════════════════════╝");

            context.stats.printCompact();
            solver.printBoardWithCounts(context.board, context.piecesById, context.pieceUsed,
                                       context.totalPieces, solver.getLastPlacedRow(), solver.getLastPlacedCol());

            // Wait for user input before continuing
            waitForEnter();
        }

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

                // Verbose output - show what we're trying
                if (verbose) {
                    System.out.println("\n  → Testing piece ID=" + pid + ", rotation=" + (rot * 90) +
                                     "° in cell (" + r + ", " + c + ") [piece " + (optionIndex + 1) + "/" + snapshot.size() + "]");
                    System.out.println("    Edges: N=" + candidate[0] + ", E=" + candidate[1] +
                                     ", S=" + candidate[2] + ", W=" + candidate[3]);
                }

                // Check basic fit
                if (!solver.fits(context.board, r, c, candidate)) {
                    if (verbose) {
                        SolverLogger.info("    ✗ Rejected: edges don't match constraints");
                    }
                    continue;
                }

                // Symmetry breaking check
                if (symmetryBreakingManager != null &&
                    !symmetryBreakingManager.isPlacementAllowed(context.board, r, c, pid, rot, context.piecesById)) {
                    if (verbose) {
                        SolverLogger.info("    ✗ Rejected: symmetry breaking constraint");
                    }
                    continue;
                }

                context.stats.placements++;

                // Verbose output
                if (verbose) {
                    SolverLogger.info("    ✓ Constraints satisfied! Placing piece...");
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
                        SolverLogger.info("\n    ✗ DEAD END detected by AC-3!");
                        SolverLogger.info("    Reason: Placing this piece would make another cell unsolvable");
                        SolverLogger.info("    → Some neighboring cell would have no valid pieces left");
                        SolverLogger.info("    → Removing piece ID=" + pid + " and trying next option");
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
                    SolverLogger.info("\n    ✓ Piece successfully placed!");
                    SolverLogger.info("    → Continuing to next cell...");
                    solver.printBoardWithCounts(context.board, context.piecesById, context.pieceUsed,
                                              context.totalPieces, r, c);
                    waitForEnter();
                }

                // Check timeout only AFTER successful placement (not during backtracking)
                // This ensures saved state always contains a stable configuration
                long currentTime = System.currentTimeMillis();
                if (currentTime - context.startTimeMs > context.maxExecutionTimeMs) {
                    if (verbose) {
                        System.out.println("\n⏱️  Timeout reached after placing piece " + pid + " at (" + r + ", " + c + ")");
                        SolverLogger.info("    → Stopping before exploring deeper");
                        SolverLogger.info("    → Current state will be saved with this piece placed");
                        System.out.println("    → Piece order preserved: pieces 1-" + (pid-1) + " already tested");
                    }
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
                if (verbose) {
                    SolverLogger.info("\n╔════════════════════════════════════════════════════════════════╗");
                    System.out.println("║  BACKTRACKING from cell (" + r + ", " + c + ")");
                    SolverLogger.info("║  Piece ID=" + pid + " was placed but led to no solution");
                    SolverLogger.info("║  ");
                    SolverLogger.info("║  Possible reasons:");
                    SolverLogger.info("║  → Dead end: All subsequent cells had no valid pieces");
                    SolverLogger.info("║  → Timeout: Time limit reached during exploration");
                    SolverLogger.info("║  → Solution found: Another thread found the solution");
                    SolverLogger.info("║  ");
                    SolverLogger.info("║  Action: Removing piece " + pid + " and trying next available piece");
                    System.out.println("║  Total backtracks so far: " + (context.stats.backtracks + 1));
                    SolverLogger.info("╚════════════════════════════════════════════════════════════════╝");
                    waitForEnter();
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
        if (verbose) {
            SolverLogger.info("\n╔════════════════════════════════════════════════════════════════╗");
            SolverLogger.info("║  EXHAUSTED ALL OPTIONS");
            System.out.println("║  Cell (" + r + ", " + c + ") cannot be filled with any available piece");
            System.out.println("║  → All " + snapshot.size() + " available pieces have been tried");
            SolverLogger.info("║  → Backtracking to previous cell");
            SolverLogger.info("╚════════════════════════════════════════════════════════════════╝");
        }
        return false;
    }
}
