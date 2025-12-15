package solver.output;

import model.Board;
import model.Piece;
import solver.BacktrackingContext;
import solver.EternitySolver;
import util.SolverLogger;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

/**
 * Verbose implementation of PlacementOutputStrategy.
 * Provides detailed step-by-step logging for debugging and visualization.
 *
 * Features:
 * - Cell selection explanation with MRV heuristic details
 * - Constraint display (border requirements, neighbor edges)
 * - Piece attempt tracking
 * - AC-3 dead-end detection logs
 * - Backtracking visualization
 * - Optional user interaction (press Enter to continue)
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class VerbosePlacementOutput implements PlacementOutputStrategy {

    @Override
    public void logCellSelection(BacktrackingContext context, EternitySolver solver,
                                int row, int col, int uniquePieces, int availableCount) {
        SolverLogger.info("\n╔════════════════════════════════════════════════════════════════╗");
        SolverLogger.info("║  Step " + (solver.getStepCount() + 1) + " - Choosing next cell");
        SolverLogger.info("║  Cell selected: (" + row + ", " + col + ")");
        SolverLogger.info("║  Reason: MRV (Minimum Remaining Values) heuristic");
        SolverLogger.info("║  → This cell has only " + uniquePieces + " valid pieces (satisfy constraints)");
        SolverLogger.info("║  → Total unused pieces: " + availableCount + " (will test all, most rejected)");

        // Show required edges for this cell
        SolverLogger.info("║");
        SolverLogger.info("║  Constraints for this cell:");

        Board board = context.board;

        // North constraint
        if (row > 0 && !board.isEmpty(row - 1, col)) {
            int requiredNorth = board.getPlacement(row - 1, col).edges[2];
            SolverLogger.info("║  → North edge must be: " + requiredNorth + " (match with cell above)");
        } else if (row == 0) {
            SolverLogger.info("║  → North edge must be: 0 (border)");
        }

        // East constraint
        if (col < board.getCols() - 1 && !board.isEmpty(row, col + 1)) {
            int requiredEast = board.getPlacement(row, col + 1).edges[3];
            SolverLogger.info("║  → East edge must be: " + requiredEast + " (match with cell on right)");
        } else if (col == board.getCols() - 1) {
            SolverLogger.info("║  → East edge must be: 0 (border)");
        }

        // South constraint
        if (row < board.getRows() - 1 && !board.isEmpty(row + 1, col)) {
            int requiredSouth = board.getPlacement(row + 1, col).edges[0];
            SolverLogger.info("║  → South edge must be: " + requiredSouth + " (match with cell below)");
        } else if (row == board.getRows() - 1) {
            SolverLogger.info("║  → South edge must be: 0 (border)");
        }

        // West constraint
        if (col > 0 && !board.isEmpty(row, col - 1)) {
            int requiredWest = board.getPlacement(row, col - 1).edges[1];
            SolverLogger.info("║  → West edge must be: " + requiredWest + " (match with cell on left)");
        } else if (col == 0) {
            SolverLogger.info("║  → West edge must be: 0 (border)");
        }

        SolverLogger.info("╚════════════════════════════════════════════════════════════════╝");

        // Print compact statistics
        context.stats.printCompact();

        // Print board with counts
        solver.printBoardWithCounts(context.board, context.piecesById, context.pieceUsed,
                                   context.totalPieces, solver.getLastPlacedRow(), solver.getLastPlacedCol());

        // Wait for user input
        waitForUser();
    }

    @Override
    public void logPlacementAttempt(int pieceId, int rotation, int row, int col,
                                   int optionIndex, int totalOptions, int[] edges) {
        SolverLogger.info("\n  → Testing piece ID=" + pieceId + ", rotation=" + (rotation * 90) +
                         "° in cell (" + row + ", " + col + ") [piece " + (optionIndex + 1) + "/" + totalOptions + "]");
        SolverLogger.info("    Edges: N=" + edges[0] + ", E=" + edges[1] +
                         ", S=" + edges[2] + ", W=" + edges[3]);
    }

    @Override
    public void logEdgeRejection() {
        SolverLogger.info("    ✗ Rejected: edges don't match constraints");
    }

    @Override
    public void logSymmetryRejection() {
        SolverLogger.info("    ✗ Rejected: symmetry breaking constraint");
    }

    @Override
    public void logConstraintsSatisfied() {
        SolverLogger.info("    ✓ Constraints satisfied! Placing piece...");
    }

    @Override
    public void logSuccessfulPlacement(BacktrackingContext context, EternitySolver solver,
                                      int row, int col) {
        SolverLogger.info("\n    ✓ Piece successfully placed!");
        SolverLogger.info("    → Continuing to next cell...");
        solver.printBoardWithCounts(context.board, context.piecesById, context.pieceUsed,
                                  context.totalPieces, row, col);
        waitForUser();
    }

    @Override
    public void logAC3DeadEnd(int pieceId) {
        SolverLogger.info("\n    ✗ DEAD END detected by AC-3!");
        SolverLogger.info("    Reason: Placing this piece would make another cell unsolvable");
        SolverLogger.info("    → Some neighboring cell would have no valid pieces left");
        SolverLogger.info("    → Removing piece ID=" + pieceId + " and trying next option");
    }

    @Override
    public void logTimeout(int pieceId, int row, int col) {
        SolverLogger.info("\n⏱️  Timeout reached after placing piece " + pieceId + " at (" + row + ", " + col + ")");
        SolverLogger.info("    → Stopping before exploring deeper");
        SolverLogger.info("    → Current state will be saved with this piece placed");
        SolverLogger.info("    → Piece order preserved: pieces 1-" + (pieceId-1) + " already tested");
    }

    @Override
    public void logBacktrack(int pieceId, int row, int col, int totalBacktracks) {
        SolverLogger.info("\n╔════════════════════════════════════════════════════════════════╗");
        SolverLogger.info("║  BACKTRACKING from cell (" + row + ", " + col + ")");
        SolverLogger.info("║  Piece ID=" + pieceId + " was placed but led to no solution");
        SolverLogger.info("║  ");
        SolverLogger.info("║  Possible reasons:");
        SolverLogger.info("║  → Dead end: All subsequent cells had no valid pieces");
        SolverLogger.info("║  → Timeout: Time limit reached during exploration");
        SolverLogger.info("║  → Solution found: Another thread found the solution");
        SolverLogger.info("║  ");
        SolverLogger.info("║  Action: Removing piece " + pieceId + " and trying next available piece");
        SolverLogger.info("║  Total backtracks so far: " + (totalBacktracks + 1));
        SolverLogger.info("╚════════════════════════════════════════════════════════════════╝");
        waitForUser();
    }

    @Override
    public void logExhaustedOptions(int row, int col, int attemptedPieces) {
        SolverLogger.info("\n╔════════════════════════════════════════════════════════════════╗");
        SolverLogger.info("║  EXHAUSTED ALL OPTIONS");
        SolverLogger.info("║  Cell (" + row + ", " + col + ") cannot be filled with any available piece");
        SolverLogger.info("║  → All " + attemptedPieces + " available pieces have been tried");
        SolverLogger.info("║  → Backtracking to previous cell");
        SolverLogger.info("╚════════════════════════════════════════════════════════════════╝");
    }

    @Override
    public void waitForUser() {
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
        } catch (IOException e) {
            // If stdin check fails, skip waiting (test environment)
            SolverLogger.debug("stdin not available: " + e.getMessage());
        }
    }
}
