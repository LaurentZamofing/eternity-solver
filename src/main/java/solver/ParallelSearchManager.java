package solver;

import model.Board;
import model.Piece;
import solver.parallel.ParallelExecutionCoordinator;
import solver.parallel.WorkStealingExecutor;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Facade for parallel search execution with work-stealing and thread coordination.
 *
 * <h2>Responsibilities (Phase 4 Refactoring)</h2>
 * <ul>
 *   <li>Provides unified interface for parallel solving strategies</li>
 *   <li>Delegates to {@link WorkStealingExecutor} for Fork/Join parallelism</li>
 *   <li>Delegates to {@link ParallelExecutionCoordinator} for thread pool execution</li>
 *   <li>Maintains backward compatibility via deprecated static methods</li>
 * </ul>
 *
 * <h2>Design</h2>
 * <ul>
 *   <li>Uses instance-based {@link SharedSearchState} for thread coordination</li>
 *   <li>Delegates to extracted parallel coordination components</li>
 *   <li>Provides clean separation between parallel strategies</li>
 * </ul>
 *
 * @author Eternity Solver Team
 * @version 2.0.0 (Phase 4 extraction)
 */
public class ParallelSearchManager {

    public static final int WORK_STEALING_DEPTH_THRESHOLD = SolverConstants.WORK_STEALING_DEPTH_THRESHOLD;

    // Default static shared state for backward compatibility
    // TODO: Remove this and require callers to pass SharedSearchState explicitly
    private static final SharedSearchState defaultSharedState = new SharedSearchState();

    // Instance state
    private final SharedSearchState sharedState;
    private final DomainManager domainManager;

    // Extracted parallel execution components
    private final WorkStealingExecutor workStealingExecutor;
    private final ParallelExecutionCoordinator parallelCoordinator;

    // Extracted services
    private final DiversificationStrategy diversificationStrategy;
    private final BoardCopyService boardCopyService;

    /**
     * Creates ParallelSearchManager with required domain manager and shared state.
     *
     * @param domainManager Domain manager for constraint propagation
     * @param sharedState Shared state for thread coordination
     */
    public ParallelSearchManager(DomainManager domainManager, SharedSearchState sharedState) {
        this.domainManager = domainManager;
        this.sharedState = sharedState;
        this.diversificationStrategy = new DiversificationStrategy();
        this.boardCopyService = new BoardCopyService();

        // Initialize extracted parallel execution components
        this.workStealingExecutor = new WorkStealingExecutor(
            sharedState, domainManager, boardCopyService
        );
        this.parallelCoordinator = new ParallelExecutionCoordinator(
            sharedState, boardCopyService, diversificationStrategy
        );
    }

    /**
     * Creates ParallelSearchManager with domain manager, using default shared state.
     * Convenience constructor for backward compatibility.
     *
     * @param domainManager Domain manager for constraint propagation
     */
    public ParallelSearchManager(DomainManager domainManager) {
        this(domainManager, defaultSharedState);
    }

    /**
     * Enables work-stealing parallelism with specified thread count.
     *
     * @param numThreads Number of threads for work-stealing pool
     */
    public void enableWorkStealing(int numThreads) {
        sharedState.enableWorkStealing(numThreads);
    }

    /**
     * Resets shared state.
     * Call this between puzzles in sequential runs.
     */
    public void resetState() {
        sharedState.reset();
    }

    // ==================== Static Backward Compatibility Methods ====================
    // DEPRECATED: These methods use static state and should be migrated to instance-based SharedSearchState
    // Use EternitySolver.getSharedState() or create your own SharedSearchState instance

    /**
     * @deprecated Use {@link SharedSearchState#reset()} on an instance instead.
     * Access via {@link EternitySolver#getSharedState()}.
     */
    @Deprecated
    public static void resetGlobalState() {
        defaultSharedState.reset();
    }

    /**
     * @deprecated Use {@link SharedSearchState#getLockObject()} on an instance instead.
     */
    @Deprecated
    public static Object getLockObject() {
        return defaultSharedState.getLockObject();
    }

    /**
     * @deprecated Use {@link SharedSearchState#getSolutionFound()} on an instance instead.
     */
    @Deprecated
    public static AtomicBoolean getSolutionFound() {
        return defaultSharedState.getSolutionFound();
    }

    /**
     * @deprecated Use {@link SharedSearchState#getGlobalMaxDepth()} on an instance instead.
     */
    @Deprecated
    public static AtomicInteger getGlobalMaxDepth() {
        return defaultSharedState.getGlobalMaxDepth();
    }

    /**
     * @deprecated Use {@link SharedSearchState#getGlobalBestScore()} on an instance instead.
     */
    @Deprecated
    public static AtomicInteger getGlobalBestScore() {
        return defaultSharedState.getGlobalBestScore();
    }

    /**
     * @deprecated Use {@link SharedSearchState#getGlobalBestThreadId()} on an instance instead.
     */
    @Deprecated
    public static AtomicInteger getGlobalBestThreadId() {
        return defaultSharedState.getGlobalBestThreadId();
    }

    /**
     * @deprecated Use {@link SharedSearchState#getGlobalBestBoard()} on an instance instead.
     */
    @Deprecated
    public static AtomicReference<Board> getGlobalBestBoard() {
        return defaultSharedState.getGlobalBestBoard();
    }

    /**
     * @deprecated Use {@link SharedSearchState#getGlobalBestPieces()} on an instance instead.
     */
    @Deprecated
    public static AtomicReference<Map<Integer, Piece>> getGlobalBestPieces() {
        return defaultSharedState.getGlobalBestPieces();
    }

    /**
     * @deprecated Use {@link SharedSearchState#getWorkStealingPool()} on an instance instead.
     */
    @Deprecated
    public static ForkJoinPool getWorkStealingPool() {
        return defaultSharedState.getWorkStealingPool();
    }

    /**
     * @deprecated Use {@link SharedSearchState#isSolutionFound()} on an instance instead.
     */
    @Deprecated
    public static boolean isSolutionFound() {
        return defaultSharedState.isSolutionFound();
    }

    /**
     * @deprecated Use {@link SharedSearchState#markSolutionFound()} on an instance instead.
     */
    @Deprecated
    public static void markSolutionFound() {
        defaultSharedState.markSolutionFound();
    }

    /**
     * @deprecated Use {@link SharedSearchState#updateGlobalMaxDepth(int)} on an instance instead.
     */
    @Deprecated
    public static boolean updateGlobalMaxDepth(int depth) {
        return defaultSharedState.updateGlobalMaxDepth(depth);
    }

    /**
     * @deprecated Use {@link SharedSearchState#updateGlobalBestScore(int)} on an instance instead.
     */
    @Deprecated
    public static boolean updateGlobalBestScore(int score) {
        return defaultSharedState.updateGlobalBestScore(score);
    }

    /**
     * @deprecated Use {@link SharedSearchState#setGlobalBestThreadId(int)} on an instance instead.
     */
    @Deprecated
    public static void setGlobalBestThreadId(int threadId) {
        defaultSharedState.setGlobalBestThreadId(threadId);
    }

    /**
     * @deprecated Use {@link SharedSearchState#setGlobalBestBoard(Board)} on an instance instead.
     */
    @Deprecated
    public static void setGlobalBestBoard(Board board) {
        defaultSharedState.setGlobalBestBoard(board);
    }

    /**
     * @deprecated Use {@link SharedSearchState#setGlobalBestPieces(Map)} on an instance instead.
     */
    @Deprecated
    public static void setGlobalBestPieces(Map<Integer, Piece> pieces) {
        defaultSharedState.setGlobalBestPieces(pieces);
    }

    /**
     * Solves puzzle using work-stealing parallelism (Fork/Join framework).
     * Delegates to {@link WorkStealingExecutor}.
     *
     * @param board Initial board state
     * @param pieces Map of all pieces
     * @param pieceUsed BitSet of used pieces
     * @param totalPieces Total number of pieces
     * @param sequentialSolver Sequential solver callback
     * @return true if solution found
     * @throws IllegalStateException if work-stealing pool not enabled
     */
    public boolean solveWithWorkStealing(Board board, Map<Integer, Piece> pieces,
                                         BitSet pieceUsed, int totalPieces,
                                         SequentialSolver sequentialSolver) {
        // Convert to WorkStealingExecutor.SequentialSolver
        WorkStealingExecutor.SequentialSolver executor = sequentialSolver::solve;
        return workStealingExecutor.solve(board, pieces, pieceUsed, totalPieces, executor);
    }

    /**
     * Callback interface for sequential solver.
     * Maintained for backward compatibility.
     */
    public interface SequentialSolver {
        boolean solve(Board board, Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces);
    }

    /**
     * Solves puzzle with multiple parallel threads.
     * Delegates to {@link ParallelExecutionCoordinator}.
     * Each thread explores independently with own board copy.
     *
     * @param board Initial board state
     * @param allPieces All pieces in puzzle
     * @param availablePieces Pieces not yet placed
     * @param numThreads Number of parallel threads
     * @param puzzleName Puzzle identifier for save/load
     * @param sequentialSolver Solver each thread uses
     * @return true if any thread found solution
     */
    public boolean solveParallel(Board board, Map<Integer, Piece> allPieces,
                                Map<Integer, Piece> availablePieces, int numThreads,
                                String puzzleName, SequentialSolver sequentialSolver) {
        // Convert to ParallelExecutionCoordinator.SequentialSolver
        ParallelExecutionCoordinator.SequentialSolver executor = sequentialSolver::solve;
        return parallelCoordinator.solveParallel(
            board, allPieces, availablePieces, numThreads, puzzleName, executor
        );
    }
}
