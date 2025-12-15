package solver;

import util.SolverLogger;

import model.Board;
import model.Piece;

import java.util.Scanner;

/**
 * Placement strategy that detects and places singleton pieces.
 *
 * A singleton is a piece that can only fit in one specific location on the board.
 * Placing singletons first is a powerful pruning technique that dramatically
 * reduces the search space.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class SingletonPlacementStrategy implements PlacementStrategy {

    private final SingletonDetector singletonDetector;
    private final boolean useSingletons;
    private final boolean verbose;
    private final SymmetryBreakingManager symmetryBreakingManager;
    private final ConstraintPropagator constraintPropagator;
    private final DomainManager domainManager;

    /**
     * Creates a singleton placement strategy.
     *
     * @param singletonDetector Detector for finding singleton pieces
     * @param useSingletons Whether singleton optimization is enabled
     * @param verbose Whether to output verbose logging
     * @param symmetryBreakingManager Manager for symmetry breaking constraints (can be null)
     * @param constraintPropagator AC-3 constraint propagator
     * @param domainManager Domain manager for AC-3
     */
    public SingletonPlacementStrategy(SingletonDetector singletonDetector, boolean useSingletons,
                                     boolean verbose, SymmetryBreakingManager symmetryBreakingManager,
                                     ConstraintPropagator constraintPropagator, DomainManager domainManager) {
        this.singletonDetector = singletonDetector;
        this.useSingletons = useSingletons;
        this.verbose = verbose;
        this.symmetryBreakingManager = symmetryBreakingManager;
        this.constraintPropagator = constraintPropagator;
        this.domainManager = domainManager;
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
        } catch (java.io.IOException | RuntimeException e) {
            // If stdin check fails, skip waiting (test environment)
        }
    }

    @Override
    public boolean tryPlacement(BacktrackingContext context, EternitySolver solver) {
        // Check if singleton optimization is disabled
        if (!useSingletons) {
            return false; // Indicate this strategy doesn't apply
        }

        // Find a singleton piece
        SingletonDetector.SingletonInfo singleton = singletonDetector.findSingletonPiece(
            context.board, context.piecesById, context.pieceUsed, context.totalPieces);

        if (singleton == null) {
            return false; // No singleton found, this strategy doesn't apply
        }

        // Extract singleton info
        int r = singleton.row;
        int c = singleton.col;
        int pid = singleton.pieceId;
        int rot = singleton.rotation;
        Piece piece = context.piecesById.get(pid);
        int[] candidate = piece.edgesRotated(rot);

        // Verbose output
        if (verbose) {
            int availableCount = context.countAvailablePieces();
            SolverLogger.info("\n╔════════════════════════════════════════╗");
            System.out.println("║  Step " + (solver.getStepCount() + 1) + " - FORCED SINGLETON");
            System.out.println("║  Piece " + pid + " → Cell (" + r + ", " + c + ")");
            SolverLogger.info("║  Available pieces: " + availableCount);
            SolverLogger.info("╚════════════════════════════════════════╝");
            context.stats.printCompact();
            waitForEnter();
        }

        // Symmetry breaking check
        if (symmetryBreakingManager != null &&
            !symmetryBreakingManager.isPlacementAllowed(context.board, r, c, pid, rot, context.piecesById)) {
            // Singleton violates symmetry constraints - no solution possible
            return false;
        }

        // Place the singleton
        context.board.place(r, c, piece, rot);
        context.pieceUsed.set(pid);
        solver.setLastPlaced(r, c);
        solver.incrementStepCount();
        context.stats.placements++;
        context.stats.singletonsPlaced++;

        // Track placement order
        solver.recordPlacement(r, c, pid, rot);

        // AC-3 constraint propagation
        if (!constraintPropagator.propagateAC3(context.board, r, c, pid, rot,
                                               context.piecesById, context.pieceUsed, context.totalPieces)) {
            // Dead end detected by AC-3 for singleton
            if (verbose) {
                System.out.println("✗ AC-3 dead-end detected for singleton: ID=" + pid + " at (" + r + ", " + c + ")");
            }
            // Backtrack immediately
            context.pieceUsed.clear(pid);
            context.board.remove(r, c);
            solver.removeLastPlacement();
            domainManager.restoreAC3Domains(context.board, r, c, context.piecesById,
                                           context.pieceUsed, context.totalPieces);
            return false;
        }

        // Verbose board display
        if (verbose) {
            solver.printBoardWithCounts(context.board, context.piecesById, context.pieceUsed,
                                       context.totalPieces, r, c);
            System.out.println("✓ Singleton placed: ID=" + pid + ", Rotation=" + (rot * 90) +
                             "°, Edges=" + java.util.Arrays.toString(candidate));
            waitForEnter();
        }

        // Recursive call
        boolean solved = solver.solveBacktracking(context.board, context.piecesById,
                                                  context.pieceUsed, context.totalPieces);
        if (solved) {
            return true;
        }

        // Backtrack singleton
        context.stats.backtracks++;
        if (verbose) {
            System.out.println("✗ Singleton BACKTRACK: Removing piece ID=" + pid + " at (" + r + ", " + c + ")");
            waitForEnter();
        }

        context.pieceUsed.clear(pid);
        context.board.remove(r, c);
        solver.removeLastPlacement();
        domainManager.restoreAC3Domains(context.board, r, c, context.piecesById,
                                       context.pieceUsed, context.totalPieces);

        // Update lastPlaced tracking
        solver.findAndSetLastPlaced(context.board);

        return false; // Singleton didn't lead to solution
    }
}
