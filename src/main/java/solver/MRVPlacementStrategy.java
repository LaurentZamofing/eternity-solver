package solver;

import solver.debug.DebugPlacementLogger;
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
import util.CellLabelFormatter;
import util.DebugHelper;
import util.SolverLogger;

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

    // Precomputed piece ordering by LCV difficulty score — built once on
    // first call when sortOrder="lcv", reused for every recursion level.
    // Avoids per-call sort O(n log n) + boxing. lcvOrderedPieceIds is
    // ascending (most constrained first); mcvOrderedPieceIds is the
    // reverse (least constrained first — A/B test for whether the
    // original LCV direction is the right one for this puzzle type).
    private int[] lcvOrderedPieceIds;
    private int[] mcvOrderedPieceIds;
    private boolean debugBacktracking = false;
    private boolean debugShowBoard = false;
    private DebugPlacementLogger debugLogger;

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

    /** Exposes the internal validator so the owning solver can toggle
     *  experimental flags (e.g. pre-commit lookahead) on it. */
    public PlacementValidator getValidator() {
        return validator;
    }

    /** Sorts all piece IDs by ascending LCV difficulty (most constrained first)
     *  once — caller caches the result. */
    private static int[] buildLcvOrder(java.util.Set<Integer> pieceIds, LeastConstrainingValueOrderer orderer) {
        Integer[] boxed = pieceIds.toArray(new Integer[0]);
        java.util.Arrays.sort(boxed, Comparator.comparingInt(orderer::getDifficultyScore)
                                              .thenComparingInt(Integer::intValue));
        int[] out = new int[boxed.length];
        for (int i = 0; i < boxed.length; i++) out[i] = boxed[i];
        return out;
    }

    /** Filters {@code snapshot} to contain only the subset of piece IDs in
     *  the pre-sorted {@code orderedIds} that are still in the original
     *  snapshot (i.e. not yet placed). Keeps the LCV order. O(n). */
    private static List<Integer> filterByOrder(List<Integer> snapshot, int[] orderedIds) {
        if (snapshot.isEmpty()) return snapshot;
        // Use a bitset-like lookup keyed on pieceId.
        int maxId = 0;
        for (int pid : snapshot) if (pid > maxId) maxId = pid;
        boolean[] inSnapshot = new boolean[maxId + 1];
        for (int pid : snapshot) inSnapshot[pid] = true;
        List<Integer> out = new ArrayList<>(snapshot.size());
        for (int pid : orderedIds) {
            if (pid <= maxId && inSnapshot[pid]) out.add(pid);
        }
        return out;
    }

    /**
     * Set debug backtracking mode.
     *
     * @param enabled true to enable debug logs
     */
    public void setDebugBacktracking(boolean enabled) {
        this.debugBacktracking = enabled;
    }

    /**
     * Set debug show board mode.
     *
     * @param enabled true to show board after each placement
     */
    public void setDebugShowBoard(boolean enabled) {
        this.debugShowBoard = enabled;
        // Create debug logger when debug is enabled
        if (enabled) {
            this.debugLogger = new DebugPlacementLogger(true);
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

        // Log cell selection with constraints
        int uniquePieces = solver.countUniquePieces(context.board, r, c, context.piecesById,
                                                   context.pieceUsed, context.totalPieces);
        int availableCount = context.countAvailablePieces();
        outputStrategy.logCellSelection(context, solver, r, c, uniquePieces, availableCount);

        // Build list of available pieces
        List<Integer> snapshot = context.getUnusedPieces();

        // Sort pieces by ID (default ascending, optional descending, optional
        // LCV-difficulty via sortOrder="lcv"). LCV builds a precomputed
        // int[] ordering on first call so subsequent calls just iterate it
        // — no per-call boxing/sort overhead.
        if ("descending".equals(sortOrder)) {
            snapshot.sort(Collections.reverseOrder());
        } else if ("lcv".equalsIgnoreCase(sortOrder)
                   && valueOrderer != null && valueOrderer.isInitialized()) {
            if (lcvOrderedPieceIds == null) {
                lcvOrderedPieceIds = buildLcvOrder(context.piecesById.keySet(), valueOrderer);
            }
            snapshot = filterByOrder(snapshot, lcvOrderedPieceIds);
        } else if ("mcv".equalsIgnoreCase(sortOrder)
                   && valueOrderer != null && valueOrderer.isInitialized()) {
            if (mcvOrderedPieceIds == null) {
                int[] reversed = buildLcvOrder(context.piecesById.keySet(), valueOrderer).clone();
                // Reverse in place — most-constraining-value last, least-constraining first.
                for (int i = 0, j = reversed.length - 1; i < j; i++, j--) {
                    int tmp = reversed[i];
                    reversed[i] = reversed[j];
                    reversed[j] = tmp;
                }
                mcvOrderedPieceIds = reversed;
            }
            snapshot = filterByOrder(snapshot, mcvOrderedPieceIds);
        } else {
            Collections.sort(snapshot);
        }

        // Register depth options for progress tracking (first 5 depths only)
        int currentDepth = context.getCurrentDepth();
        if (currentDepth < 5) {
            context.stats.registerDepthOptions(currentDepth, snapshot.size());
        }

        // Debug: Log start of placement attempts
        if (debugBacktracking && debugLogger != null) {
            debugLogger.logCellSelection(r, c, currentDepth, snapshot.size(), solver, context);
        }

        // Try each piece
        int optionIndex = 0;
        int attemptCount = 0;
        for (int pid : snapshot) {
            // Increment progress for this depth
            if (currentDepth < 5) {
                context.stats.incrementDepthProgress(currentDepth);
            }

            Piece piece = context.piecesById.get(pid);

            // Try each rotation
            for (int rot = 0; rot < 4; rot++) {
                attemptCount++;
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

                // Pre-commit nogood lookup: XOR the tentative key into the hash
                // and, if this exact partial assignment has been proven dead
                // before, skip without ever touching the board.
                long tentativeKey = 0L;
                if (context.zobrist != null && context.nogoods != null) {
                    tentativeKey = context.zobrist.keyOf(r, c, pid, rot);
                    long tentativeHash = context.stateHash ^ tentativeKey;
                    if (context.nogoods.contains(tentativeHash)) {
                        continue; // proven dead in a previous branch
                    }
                }

                // Place piece
                context.board.place(r, c, piece, rot);
                context.pieceUsed.set(pid);
                if (tentativeKey != 0L) context.stateHash ^= tentativeKey;
                solver.setLastPlaced(r, c);
                solver.incrementStepCount();
                solver.recordPlacement(r, c, pid, rot);

                // AC-3 constraint propagation (after placement)
                if (!validator.propagateConstraints(context, r, c, pid, rot)) {
                    // Dead end detected by AC-3
                    if (debugBacktracking) {
                        String cellLabel = String.valueOf((char) ('A' + r)) + (c + 1);
                        SolverLogger.info("       ✗ Failed: AC-3 dead-end detected after placing piece " + pid + " at " + cellLabel);

                        // Show which cells have critical domains
                        validator.getDomainManager().logCriticalDomains(context.board, 5);

                        // Show board state when dead-end detected (if enabled)
                        if (debugShowBoard) {
                            List<Integer> unusedPieces = context.getUnusedPieces();

                            SolverLogger.info("");
                            SolverLogger.info("       📊 Board state when dead-end detected:");
                            SolverLogger.info("");
                            solver.printBoardWithLabels(context.board, context.piecesById, unusedPieces);
                            SolverLogger.info("");
                        }
                    }
                    outputStrategy.logAC3DeadEnd(pid);
                    context.stats.deadEndsDetected++;

                    // Backtrack
                    if (tentativeKey != 0L) context.stateHash ^= tentativeKey;
                    context.pieceUsed.clear(pid);
                    context.board.remove(r, c);
                    solver.removePlacement(r, c);
                    validator.restoreDomains(context, r, c);
                    continue;
                }

                // Log successful placement
                if (debugBacktracking && debugLogger != null) {
                    debugLogger.logPlacementSuccess(r, c, pid, rot, currentDepth - 1, currentDepth,
                                                   context.stats.backtracks, solver, context);
                }
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

                // Backtrack (recursion returned false = dead-end deeper in tree).
                // Record the current (committed) partial assignment as a nogood
                // before undoing — any future search path that reaches the
                // same placement set can now prove itself dead via the cache.
                context.stats.backtracks++;
                outputStrategy.logBacktrack(pid, r, c, (int) context.stats.backtracks);

                if (context.nogoods != null) {
                    context.nogoods.add(context.stateHash);
                }
                if (tentativeKey != 0L) context.stateHash ^= tentativeKey;
                context.pieceUsed.clear(pid);
                context.board.remove(r, c);
                solver.removePlacement(r, c);
                validator.restoreDomains(context, r, c);

                // Update last placed tracking
                solver.findAndSetLastPlaced(context.board);

                // Show board after backtrack if debug mode is enabled
                if (debugBacktracking && debugLogger != null) {
                    debugLogger.logBacktrack(r, c, pid, context.getCurrentDepth(), solver, context);
                }
            }

            optionIndex++;
        }

        // No solution found with any piece at this cell - this is the DEAD-END!
        if (debugBacktracking && debugLogger != null) {
            debugLogger.logDeadEnd(r, c, attemptCount, solver, context);
        }
        outputStrategy.logExhaustedOptions(r, c, snapshot.size());
        return false;
    }
}
