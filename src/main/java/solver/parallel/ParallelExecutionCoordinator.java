package solver.parallel;

import model.Board;
import model.Piece;
import solver.BoardCopyService;
import solver.DiversificationStrategy;
import solver.SharedSearchState;
import solver.SolverConstants;
import util.SaveManager;
import util.SolverLogger;

import java.util.*;
import java.util.concurrent.*;

/**
 * Coordinates parallel execution of puzzle solving across multiple threads.
 *
 * <h2>Execution Strategy</h2>
 * <ul>
 *   <li>Creates fixed thread pool with specified thread count</li>
 *   <li>Each thread gets independent board copy with diversification</li>
 *   <li>Threads coordinate via {@link SharedSearchState}</li>
 *   <li>Supports resuming from saved thread states</li>
 * </ul>
 *
 * <h2>Thread Diversification</h2>
 * Uses {@link DiversificationStrategy} to ensure threads explore different
 * parts of the search space (e.g., different corner pieces).
 *
 * @author Eternity Solver Team
 * @version 2.0.0 (Phase 4 extraction)
 */
public class ParallelExecutionCoordinator {

    private final SharedSearchState sharedState;
    private final BoardCopyService boardCopyService;
    private final DiversificationStrategy diversificationStrategy;

    /**
     * Creates coordinator with required dependencies.
     *
     * @param sharedState Shared state for thread coordination
     * @param boardCopyService Service for creating board copies
     * @param diversificationStrategy Strategy for thread diversification
     */
    public ParallelExecutionCoordinator(SharedSearchState sharedState,
                                       BoardCopyService boardCopyService,
                                       DiversificationStrategy diversificationStrategy) {
        this.sharedState = sharedState;
        this.boardCopyService = boardCopyService;
        this.diversificationStrategy = diversificationStrategy;
    }

    /**
     * Solves puzzle with multiple parallel threads.
     * Each thread explores independently with its own board copy.
     *
     * @param board Initial board state
     * @param allPieces All pieces in puzzle
     * @param availablePieces Pieces not yet placed
     * @param numThreads Number of parallel threads
     * @param puzzleName Puzzle identifier for save/load
     * @param sequentialSolver Solver each thread uses
     * @return true if any thread found solution
     */
    public boolean solveParallel(Board board,
                                 Map<Integer, Piece> allPieces,
                                 Map<Integer, Piece> availablePieces,
                                 int numThreads,
                                 String puzzleName,
                                 SequentialSolver sequentialSolver) {

        SolverLogger.info("\n╔══════════════════════════════════════════════════════════╗");
        SolverLogger.info("║           PARALLEL SEARCH WITH " + numThreads + " THREADS            ║");
        SolverLogger.info("╚══════════════════════════════════════════════════════════╝\n");

        // Reset shared state flags
        sharedState.getSolutionFound().set(false);
        sharedState.getGlobalMaxDepth().set(0);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Boolean>> futures = new ArrayList<>();

        // Launch worker threads
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            Future<Boolean> future = executor.submit(() ->
                executeThread(threadId, board, allPieces, availablePieces, sequentialSolver)
            );
            futures.add(future);
        }

        // Wait for all threads and collect results
        boolean solved = false;
        for (Future<Boolean> future : futures) {
            try {
                if (future.get()) {
                    solved = true;
                }
            } catch (ExecutionException e) {
                SolverLogger.error("Thread execution failed", e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                SolverLogger.error("Thread interrupted", e);
            }
        }

        // Shutdown executor
        shutdown(executor);

        return solved;
    }

    /**
     * Executes solving in a single thread.
     *
     * @param threadId Thread identifier
     * @param board Initial board
     * @param allPieces All pieces
     * @param availablePieces Available pieces
     * @param sequentialSolver Solver to use
     * @return true if solution found
     */
    private boolean executeThread(int threadId,
                                  Board board,
                                  Map<Integer, Piece> allPieces,
                                  Map<Integer, Piece> availablePieces,
                                  SequentialSolver sequentialSolver) {
        try {
            // Initialize thread state
            ThreadState state = initializeThreadState(
                threadId, board, allPieces, availablePieces
            );

            // Apply diversification if not loaded from save
            if (!state.loadedFromSave) {
                diversificationStrategy.diversify(
                    threadId, state.localBoard, state.localPieces,
                    state.unusedIds, state.pieceUsed
                );
            }

            // Solve with this thread's configuration
            return sequentialSolver.solve(
                state.localBoard, state.localPieces,
                state.pieceUsed, state.totalPieces
            );

        } catch (RuntimeException e) {
            SolverLogger.error("Error during parallel search execution: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Initializes state for a thread, loading from save if available.
     *
     * @param threadId Thread identifier
     * @param board Initial board
     * @param allPieces All pieces
     * @param availablePieces Available pieces
     * @return Initialized thread state
     */
    private ThreadState initializeThreadState(int threadId,
                                             Board board,
                                             Map<Integer, Piece> allPieces,
                                             Map<Integer, Piece> availablePieces) {
        Board localBoard;
        Map<Integer, Piece> localPieces;
        List<Integer> unusedIds;
        long seed = System.currentTimeMillis() + threadId * SolverConstants.THREAD_SEED_OFFSET_MS;
        boolean loadedFromSave = false;

        // Try to load from saved state
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
                // Load failed - start fresh
                localBoard = boardCopyService.copyBoard(board, allPieces);
                localPieces = new HashMap<>(allPieces);
                unusedIds = new ArrayList<>(availablePieces.keySet());
            }
        } else {
            // No save - create fresh copy
            localBoard = boardCopyService.copyBoard(board, allPieces);
            localPieces = new HashMap<>(allPieces);
            unusedIds = new ArrayList<>(availablePieces.keySet());
        }

        // Create BitSet for piece usage
        int totalPieces = localPieces.size();
        int maxPieceId = localPieces.keySet().stream()
            .max(Integer::compareTo)
            .orElse(totalPieces);
        BitSet pieceUsed = new BitSet(maxPieceId + 1);
        for (int pid : localPieces.keySet()) {
            if (!unusedIds.contains(pid)) {
                pieceUsed.set(pid);
            }
        }

        System.out.println("🚀 Thread " + threadId + " started (seed=" + seed + ")");

        return new ThreadState(localBoard, localPieces, unusedIds,
                              pieceUsed, totalPieces, seed, loadedFromSave);
    }

    /**
     * Shuts down executor gracefully with timeout.
     *
     * @param executor Executor to shutdown
     */
    private void shutdown(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    /**
     * Callback interface for sequential solver.
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
     * Internal class to hold thread state.
     */
    private static class ThreadState {
        final Board localBoard;
        final Map<Integer, Piece> localPieces;
        final List<Integer> unusedIds;
        final BitSet pieceUsed;
        final int totalPieces;
        final long seed;
        final boolean loadedFromSave;

        ThreadState(Board localBoard, Map<Integer, Piece> localPieces,
                   List<Integer> unusedIds, BitSet pieceUsed,
                   int totalPieces, long seed, boolean loadedFromSave) {
            this.localBoard = localBoard;
            this.localPieces = localPieces;
            this.unusedIds = unusedIds;
            this.pieceUsed = pieceUsed;
            this.totalPieces = totalPieces;
            this.seed = seed;
            this.loadedFromSave = loadedFromSave;
        }
    }
}
