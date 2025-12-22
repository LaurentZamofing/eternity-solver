package solver.parallel;

import model.Board;
import model.Piece;
import solver.BoardCopyService;
import solver.DomainManager;
import solver.SharedSearchState;
import solver.SolverConstants;
import util.SolverLogger;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Executes puzzle solving using work-stealing parallelism (Fork/Join framework).
 *
 * <h2>Work-Stealing Strategy</h2>
 * <ul>
 *   <li>Shallow depths: Fork parallel tasks for different piece placements</li>
 *   <li>Deep depths: Switch to sequential solving to avoid overhead</li>
 *   <li>Threshold controlled by {@link SolverConstants#WORK_STEALING_DEPTH_THRESHOLD}</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * Uses {@link SharedSearchState} for coordination across parallel tasks.
 * Each task works on independent board copies to avoid contention.
 *
 * @author Eternity Solver Team
 * @version 2.0.0 (Phase 4 extraction)
 */
public class WorkStealingExecutor {

    private final SharedSearchState sharedState;
    private final DomainManager domainManager;
    private final BoardCopyService boardCopyService;

    /**
     * Creates work-stealing executor with required dependencies.
     *
     * @param sharedState Shared state for thread coordination
     * @param domainManager Domain manager for constraint propagation
     * @param boardCopyService Service for creating board copies
     */
    public WorkStealingExecutor(SharedSearchState sharedState,
                               DomainManager domainManager,
                               BoardCopyService boardCopyService) {
        this.sharedState = sharedState;
        this.domainManager = domainManager;
        this.boardCopyService = boardCopyService;
    }

    /**
     * Solves puzzle using work-stealing parallelism.
     *
     * @param board Initial board state
     * @param pieces Map of all pieces
     * @param pieceUsed BitSet of used pieces
     * @param totalPieces Total number of pieces
     * @param sequentialSolver Solver to use at deep depths
     * @return true if solution found
     * @throws IllegalStateException if work-stealing pool not enabled
     */
    public boolean solve(Board board, Map<Integer, Piece> pieces,
                        BitSet pieceUsed, int totalPieces,
                        SequentialSolver sequentialSolver) {
        ForkJoinPool pool = sharedState.getWorkStealingPool();

        if (pool == null) {
            throw new IllegalStateException(
                "Work-stealing pool not enabled. Call enableWorkStealing() on SharedSearchState first.");
        }

        WorkStealingTask task = new WorkStealingTask(
            board, pieces, pieceUsed, totalPieces, 0,
            domainManager, sequentialSolver, sharedState, boardCopyService
        );
        return pool.invoke(task);
    }

    /**
     * Callback interface for sequential solver at deep depths.
     */
    public interface SequentialSolver {
        /**
         * Solves puzzle sequentially from given state.
         *
         * @param board Current board state
         * @param pieces All pieces
         * @param pieceUsed BitSet of used pieces
         * @param totalPieces Total piece count
         * @return true if solution found
         */
        boolean solve(Board board, Map<Integer, Piece> pieces,
                     BitSet pieceUsed, int totalPieces);
    }

    /**
     * Recursive task for Fork/Join parallelism with work-stealing.
     *
     * <h2>Algorithm</h2>
     * <ol>
     *   <li>If depth &lt; threshold: Fork tasks for different piece placements</li>
     *   <li>If depth ≥ threshold: Use sequential solver</li>
     *   <li>Check shared state for early termination if solution found</li>
     * </ol>
     */
    private static class WorkStealingTask extends RecursiveTask<Boolean> {
        private final Board board;
        private final Map<Integer, Piece> piecesById;
        private final BitSet pieceUsed;
        private final int totalPieces;
        private final int currentDepth;
        private final DomainManager domainManager;
        private final SequentialSolver sequentialSolver;
        private final SharedSearchState sharedState;
        private final BoardCopyService boardCopyService;

        WorkStealingTask(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed,
                        int totalPieces, int currentDepth, DomainManager domainManager,
                        SequentialSolver sequentialSolver, SharedSearchState sharedState,
                        BoardCopyService boardCopyService) {
            this.board = board;
            this.piecesById = piecesById;
            this.pieceUsed = (BitSet) pieceUsed.clone();
            this.totalPieces = totalPieces;
            this.currentDepth = currentDepth;
            this.domainManager = domainManager;
            this.sequentialSolver = sequentialSolver;
            this.sharedState = sharedState;
            this.boardCopyService = boardCopyService;
        }

        @Override
        protected Boolean compute() {
            // Early termination if solution already found
            if (sharedState.isSolutionFound()) {
                return false;
            }

            // If depth is low enough, fork parallel tasks
            if (currentDepth < SolverConstants.WORK_STEALING_DEPTH_THRESHOLD) {
                return computeParallel();
            } else {
                // Deep enough - use sequential search
                return sequentialSolver.solve(board, piecesById, pieceUsed, totalPieces);
            }
        }

        /**
         * Computes solution in parallel by forking tasks for each valid placement.
         */
        private Boolean computeParallel() {
            // Find next empty cell
            int[] cell = findNextEmptyCell(board);

            // If no cell found, check if board is complete
            if (cell == null) {
                int usedCount = pieceUsed.cardinality();
                return usedCount == totalPieces;
            }

            int r = cell[0], c = cell[1];
            List<DomainManager.ValidPlacement> validPlacements =
                domainManager.computeDomain(board, r, c, piecesById, pieceUsed, totalPieces);

            // Create and fork parallel tasks for different piece placements
            List<WorkStealingTask> tasks = new ArrayList<>();
            for (DomainManager.ValidPlacement vp : validPlacements) {
                try {
                    // Create deep copy of board
                    Board boardCopy = boardCopyService.copyBoard(board, piecesById);

                    Piece piece = piecesById.get(vp.pieceId);
                    if (piece != null) {
                        boardCopy.place(r, c, piece, vp.rotation);
                        BitSet usedCopy = (BitSet) pieceUsed.clone();
                        usedCopy.set(vp.pieceId);

                        WorkStealingTask task = new WorkStealingTask(
                            boardCopy, piecesById, usedCopy, totalPieces,
                            currentDepth + 1, domainManager, sequentialSolver,
                            sharedState, boardCopyService
                        );
                        tasks.add(task);
                        task.fork(); // Submit to work-stealing pool
                    }
                } catch (RuntimeException e) {
                    // Continue with next placement if copy fails
                    SolverLogger.debug("Failed to create parallel task: " + e.getMessage());
                }
            }

            // Join tasks and check for solutions
            for (WorkStealingTask task : tasks) {
                try {
                    boolean taskFoundSolution = task.join();

                    if (taskFoundSolution && !sharedState.isSolutionFound()) {
                        sharedState.markSolutionFound();
                        return true;
                    }
                } catch (RuntimeException e) {
                    // Continue with next task
                    SolverLogger.debug("Task join failed: " + e.getMessage());
                }
            }

            return false;
        }

        /**
         * Finds next empty cell in row-major order.
         * Simple strategy - could be enhanced with MRV heuristic.
         *
         * @param board Board to search
         * @return Array of [row, col] or null if no empty cell
         */
        private int[] findNextEmptyCell(Board board) {
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (board.isEmpty(r, c)) {
                        return new int[]{r, c};
                    }
                }
            }
            return null;
        }
    }
}
