package solver;

import util.SolverLogger;

import model.Board;
import model.Piece;
import model.Placement;
import util.SaveManager;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages parallel search execution with work-stealing, thread coordination, and diversification strategies.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Coordinate work-stealing parallelism (Fork/Join framework)</li>
 *   <li>Manage thread pool execution</li>
 *   <li>Apply diversification strategies for parallel search</li>
 *   <li>Coordinate shared state across threads</li>
 * </ul>
 *
 * <h2>Design</h2>
 * Uses instance-based SharedSearchState (not static state) for thread-safe coordination.
 * Delegates board copying and diversification to extracted services.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class ParallelSearchManager {

    public static final int WORK_STEALING_DEPTH_THRESHOLD = SolverConstants.WORK_STEALING_DEPTH_THRESHOLD;

    // Default static shared state for backward compatibility
    // TODO: Remove this and require callers to pass SharedSearchState explicitly
    private static final SharedSearchState defaultSharedState = new SharedSearchState();

    // Instance state
    private final SharedSearchState sharedState;
    private final DomainManager domainManager;

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
    // TODO: Remove these and migrate callers to use SharedSearchState directly

    public static void resetGlobalState() {
        defaultSharedState.reset();
    }

    public static Object getLockObject() {
        return defaultSharedState.getLockObject();
    }

    public static AtomicBoolean getSolutionFound() {
        return defaultSharedState.getSolutionFound();
    }

    public static AtomicInteger getGlobalMaxDepth() {
        return defaultSharedState.getGlobalMaxDepth();
    }

    public static AtomicInteger getGlobalBestScore() {
        return defaultSharedState.getGlobalBestScore();
    }

    public static AtomicInteger getGlobalBestThreadId() {
        return defaultSharedState.getGlobalBestThreadId();
    }

    public static AtomicReference<Board> getGlobalBestBoard() {
        return defaultSharedState.getGlobalBestBoard();
    }

    public static AtomicReference<Map<Integer, Piece>> getGlobalBestPieces() {
        return defaultSharedState.getGlobalBestPieces();
    }

    /**
     * Solves puzzle using work-stealing parallelism (Fork/Join framework).
     * Creates parallel task and submits to pool.
     *
     * @param board Initial board state
     * @param pieces Map of all pieces
     * @param pieceUsed BitSet of used pieces
     * @param totalPieces Total number of pieces
     * @param sequentialSolver Sequential solver callback
     * @return true if solution found
     */
    public boolean solveWithWorkStealing(Board board, Map<Integer, Piece> pieces,
                                         BitSet pieceUsed, int totalPieces,
                                         SequentialSolver sequentialSolver) {
        ForkJoinPool pool = sharedState.getWorkStealingPool();

        if (pool == null) {
            throw new IllegalStateException("Work-stealing pool not enabled. Call enableWorkStealing() first.");
        }

        ParallelSearchTask task = new ParallelSearchTask(
            board, pieces, pieceUsed, totalPieces, 0, domainManager, sequentialSolver, sharedState, boardCopyService
        );
        return pool.invoke(task);
    }

    /** Callback interface for sequential solver. */
    public interface SequentialSolver {
        boolean solve(Board board, Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces);
    }

    /**
     * Recursive task for Fork/Join parallelism with work-stealing.
     */
    private static class ParallelSearchTask extends RecursiveTask<Boolean> {
        private final Board board;
        private final Map<Integer, Piece> piecesById;
        private final BitSet pieceUsed;
        private final int totalPieces;
        private final int currentDepth;
        private final DomainManager domainManager;
        private final SequentialSolver sequentialSolver;
        private final SharedSearchState sharedState;
        private final BoardCopyService boardCopyService;

        ParallelSearchTask(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed,
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
            // Check if solution already found
            if (sharedState.isSolutionFound()) {
                return false;
            }

            // Get work-stealing pool reference
            ForkJoinPool pool = sharedState.getWorkStealingPool();

            // If depth is low enough, fork tasks
            if (currentDepth < WORK_STEALING_DEPTH_THRESHOLD && pool != null) {
                // Find next empty cell with MRV heuristic (simplified version)
                int[] cell = findNextEmptyCell(board);

                // If no cell found, check if board is complete
                if (cell == null) {
                    int usedCount = pieceUsed.cardinality();
                    return usedCount == totalPieces;
                }

                int r = cell[0], c = cell[1];
                List<DomainManager.ValidPlacement> validPlacements =
                    domainManager.computeDomain(board, r, c, piecesById, pieceUsed, totalPieces);

                // Create parallel tasks for different piece placements
                List<ParallelSearchTask> tasks = new ArrayList<>();
                for (DomainManager.ValidPlacement vp : validPlacements) {
                    try {
                        // Create deep copy of board
                        Board boardCopy = boardCopyService.copyBoard(board, piecesById);

                        Piece piece = piecesById.get(vp.pieceId);
                        if (piece != null) {
                            boardCopy.place(r, c, piece, vp.rotation);
                            BitSet usedCopy = (BitSet) pieceUsed.clone();
                            usedCopy.set(vp.pieceId);

                            ParallelSearchTask task = new ParallelSearchTask(
                                boardCopy, piecesById, usedCopy, totalPieces,
                                currentDepth + 1, domainManager, sequentialSolver, sharedState, boardCopyService
                            );
                            tasks.add(task);
                            task.fork(); // Submit to work-stealing pool
                        }
                    } catch (RuntimeException e) {
                        // Continue with next placement if copy fails
                        SolverLogger.debug("Failed to create parallel task: " + e.getMessage());
                    }
                }

                // Wait for task to find solution
                for (ParallelSearchTask task : tasks) {
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
            } else {
                // Deep enough - use sequential search
                return sequentialSolver.solve(board, piecesById, pieceUsed, totalPieces);
            }
        }

        /**
         * Finds next empty cell in row-major order.
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

    /** Solves puzzle with multiple parallel threads (thread pool); each thread explores independently with own board copy. */
    public boolean solveParallel(Board board, Map<Integer, Piece> allPieces,
                                Map<Integer, Piece> availablePieces, int numThreads,
                                String puzzleName, SequentialSolver sequentialSolver) {
        SolverLogger.info("\n╔══════════════════════════════════════════════════════════╗");
        SolverLogger.info("║           PARALLEL SEARCH WITH " + numThreads + " THREADS            ║");
        SolverLogger.info("╚══════════════════════════════════════════════════════════╝\n");

        // Reset flags
        sharedState.getSolutionFound().set(false);
        sharedState.getGlobalMaxDepth().set(0);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Boolean>> futures = new ArrayList<>();

        // Launch numThreads workers with different seeds
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;

            Future<Boolean> future = executor.submit(() -> {
                try {
                    Board localBoard;
                    Map<Integer, Piece> localPieces;
                    List<Integer> unusedIds;
                    long seed = System.currentTimeMillis() + threadId * SolverConstants.THREAD_SEED_OFFSET_MS;
                    boolean loadedFromSave = false;

                    // Check if this thread has saved state
                    if (SaveManager.hasThreadState(threadId)) {
                        Object[] savedState = SaveManager.loadThreadState(threadId, allPieces);
                        if (savedState != null) {
                            localBoard = (Board) savedState[0];
                            @SuppressWarnings("unchecked")
                            Set<Integer> usedPieceIds = (Set<Integer>) savedState[1];
                            int savedDepth = (int) savedState[2];
                            seed = (long) savedState[3];

                            localPieces = new HashMap<>(allPieces);
                            unusedIds = new ArrayList<>();
                            for (int pid : allPieces.keySet()) {
                                if (!usedPieceIds.contains(pid)) {
                                    unusedIds.add(pid);
                                }
                            }

                            loadedFromSave = true;
                            synchronized (System.out) {
                                System.out.println("📂 Thread " + threadId + " restored from save: "
                                    + savedDepth + " pieces placed");
                            }
                        } else {
                            // Load error, start normally
                            localBoard = boardCopyService.copyBoard(board, allPieces);
                            localPieces = new HashMap<>(allPieces);
                            unusedIds = new ArrayList<>(availablePieces.keySet());
                        }
                    } else {
                        // No save - create board copy for this thread
                        localBoard = boardCopyService.copyBoard(board, allPieces);
                        localPieces = new HashMap<>(allPieces);
                        unusedIds = new ArrayList<>(availablePieces.keySet());
                    }

                    // Create BitSet pieceUsed
                    int totalPieces = localPieces.size();
                    int maxPieceId = localPieces.keySet().stream().max(Integer::compareTo).orElse(totalPieces);
                    BitSet pieceUsed = new BitSet(maxPieceId + 1);
                    for (int pid : localPieces.keySet()) {
                        if (!unusedIds.contains(pid)) {
                            pieceUsed.set(pid);
                        }
                    }

                    // Diversification strategy: pre-place different corner for each thread
                    if (!loadedFromSave) {
                        diversificationStrategy.diversify(threadId, localBoard, localPieces, unusedIds, pieceUsed);
                    }

                    // Solve with this thread's configuration
                    return sequentialSolver.solve(localBoard, localPieces, pieceUsed, totalPieces);

                } catch (RuntimeException e) {
                    SolverLogger.error("Error during parallel search execution: " + e.getMessage(), e);
                    return false;
                }
            });

            futures.add(future);
        }

        // Wait for all threads and check if one found solution
        boolean solved = false;
        for (Future<Boolean> future : futures) {
            try {
                if (future.get()) {
                    solved = true;
                }
            } catch (Exception e) {
                SolverLogger.error("Error occurred", e);
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return solved;
    }
}
