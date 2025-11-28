package solver;

import model.Board;
import model.Piece;
import model.Placement;
import util.SaveManager;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Manages parallel search execution with work-stealing, thread coordination, and diversification strategies. */
public class ParallelSearchManager {

    // Global state for parallel solving
    private static AtomicBoolean solutionFound = new AtomicBoolean(false);
    private static AtomicInteger globalMaxDepth = new AtomicInteger(0);
    private static AtomicInteger globalBestScore = new AtomicInteger(0);
    private static AtomicInteger globalBestThreadId = new AtomicInteger(-1);
    private static AtomicReference<Board> globalBestBoard = new AtomicReference<>(null);
    private static AtomicReference<Map<Integer, Piece>> globalBestPieces = new AtomicReference<>(null);
    private static final Object lockObject = new Object();

    // Work-stealing parallelism
    private static ForkJoinPool workStealingPool = null;
    public static final int WORK_STEALING_DEPTH_THRESHOLD = 5;
    private static final long THREAD_SAVE_INTERVAL = 60000; // 1 minute

    // Dependencies
    private final DomainManager domainManager;

    /** Creates ParallelSearchManager with required domain manager. */
    public ParallelSearchManager(DomainManager domainManager) {
        this.domainManager = domainManager;
    }

    /** Enables work-stealing parallelism with specified thread count. */
    public void enableWorkStealing(int numThreads) {
        if (workStealingPool == null || workStealingPool.isShutdown()) {
            workStealingPool = new ForkJoinPool(numThreads);
        }
    }

    /** Resets all static global state variables; call between puzzles in sequential runs. */
    public static void resetGlobalState() {
        solutionFound.set(false);
        globalMaxDepth.set(0);
        globalBestScore.set(0);
        globalBestThreadId.set(-1);
        globalBestBoard.set(null);
        globalBestPieces.set(null);
        if (workStealingPool != null && !workStealingPool.isShutdown()) {
            workStealingPool.shutdown();
            workStealingPool = null;
        }
    }

    /** Returns global lock object for synchronized operations. */
    public static Object getLockObject() {
        return lockObject;
    }

    /** Returns atomic boolean indicating solution found (backward compatibility). */
    public static AtomicBoolean getSolutionFound() {
        return solutionFound;
    }

    /** Returns work-stealing pool (backward compatibility). */
    public static ForkJoinPool getWorkStealingPool() {
        return workStealingPool;
    }

    /** Checks if solution has been found by any thread. */
    public static boolean isSolutionFound() {
        return solutionFound.get();
    }

    /** Marks that a solution has been found. */
    public static void markSolutionFound() {
        solutionFound.set(true);
    }

    /** Returns global max depth atomic integer (backward compatibility). */
    public static AtomicInteger getGlobalMaxDepth() {
        return globalMaxDepth;
    }

    /** Updates global max depth if new depth is greater; returns true if updated. */
    public static boolean updateGlobalMaxDepth(int depth) {
        int current;
        do {
            current = globalMaxDepth.get();
            if (depth <= current) return false;
        } while (!globalMaxDepth.compareAndSet(current, depth));
        return true;
    }

    /** Returns global best score atomic integer (backward compatibility). */
    public static AtomicInteger getGlobalBestScore() {
        return globalBestScore;
    }

    /** Updates global best score if new score is better; returns true if updated. */
    public static boolean updateGlobalBestScore(int score) {
        int current;
        do {
            current = globalBestScore.get();
            if (score <= current) return false;
        } while (!globalBestScore.compareAndSet(current, score));
        return true;
    }

    /** Returns global best thread ID atomic integer (backward compatibility). */
    public static AtomicInteger getGlobalBestThreadId() {
        return globalBestThreadId;
    }

    /** Sets the thread ID that found the best solution. */
    public static void setGlobalBestThreadId(int threadId) {
        globalBestThreadId.set(threadId);
    }

    /** Returns global best board atomic reference (backward compatibility). */
    public static AtomicReference<Board> getGlobalBestBoard() {
        return globalBestBoard;
    }

    /** Sets the global best board. */
    public static void setGlobalBestBoard(Board board) {
        globalBestBoard.set(board);
    }

    /** Returns global best pieces atomic reference (backward compatibility). */
    public static AtomicReference<Map<Integer, Piece>> getGlobalBestPieces() {
        return globalBestPieces;
    }

    /** Sets the global best pieces map. */
    public static void setGlobalBestPieces(Map<Integer, Piece> pieces) {
        globalBestPieces.set(pieces);
    }

    /** Solves puzzle using work-stealing parallelism (Fork/Join framework); creates parallel task and submits to pool. */
    public boolean solveWithWorkStealing(Board board, Map<Integer, Piece> pieces,
                                         BitSet pieceUsed, int totalPieces,
                                         SequentialSolver sequentialSolver) {
        if (workStealingPool == null) {
            throw new IllegalStateException("Pool de vol de travail non activé. Appelez enableWorkStealing() d'abord.");
        }

        ParallelSearchTask task = new ParallelSearchTask(
            board, pieces, pieceUsed, totalPieces, 0, domainManager, sequentialSolver
        );
        return workStealingPool.invoke(task);
    }

    /** Callback interface for sequential solver. */
    public interface SequentialSolver {
        boolean solve(Board board, Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces);
    }

    /** Recursive task for Fork/Join parallelism with work-stealing. */
    private static class ParallelSearchTask extends RecursiveTask<Boolean> {
        private final Board board;
        private final Map<Integer, Piece> piecesById;
        private final BitSet pieceUsed;
        private final int totalPieces;
        private final int currentDepth;
        private final DomainManager domainManager;
        private final SequentialSolver sequentialSolver;

        ParallelSearchTask(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed,
                           int totalPieces, int currentDepth, DomainManager domainManager,
                           SequentialSolver sequentialSolver) {
            this.board = board;
            this.piecesById = piecesById;
            this.pieceUsed = (BitSet) pieceUsed.clone();
            this.totalPieces = totalPieces;
            this.currentDepth = currentDepth;
            this.domainManager = domainManager;
            this.sequentialSolver = sequentialSolver;
        }

        @Override
        protected Boolean compute() {
            // Check if solution already found
            if (solutionFound.get()) {
                return false;
            }

            // If depth is low enough, fork tasks
            if (currentDepth < WORK_STEALING_DEPTH_THRESHOLD && workStealingPool != null) {
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
                        Board boardCopy = copyBoard(board, piecesById);

                        Piece piece = piecesById.get(vp.pieceId);
                        if (piece != null) {
                            boardCopy.place(r, c, piece, vp.rotation);
                            BitSet usedCopy = (BitSet) pieceUsed.clone();
                            usedCopy.set(vp.pieceId);

                            ParallelSearchTask task = new ParallelSearchTask(
                                boardCopy, piecesById, usedCopy, totalPieces,
                                currentDepth + 1, domainManager, sequentialSolver
                            );
                            tasks.add(task);
                            task.fork(); // Submit to work-stealing pool
                        }
                    } catch (Exception e) {
                        // Continue with next placement if copy fails
                    }
                }

                // Wait for task to find solution
                for (ParallelSearchTask task : tasks) {
                    try {
                        if (task.join() && !solutionFound.get()) {
                            solutionFound.set(true);
                            return true;
                        }
                    } catch (Exception e) {
                        // Continue with next task
                    }
                }
                return false;
            } else {
                // Deep enough - use sequential search
                return sequentialSolver.solve(board, piecesById, pieceUsed, totalPieces);
            }
        }

        /** Finds next empty cell in row-major order. */
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

        /** Creates deep copy of board. */
        private Board copyBoard(Board original, Map<Integer, Piece> pieces) {
            Board copy = new Board(original.getRows(), original.getCols());
            for (int r = 0; r < original.getRows(); r++) {
                for (int c = 0; c < original.getCols(); c++) {
                    if (!original.isEmpty(r, c)) {
                        Placement p = original.getPlacement(r, c);
                        Piece piece = pieces.get(p.getPieceId());
                        if (piece != null) {
                            copy.place(r, c, piece, p.getRotation());
                        }
                    }
                }
            }
            return copy;
        }
    }

    /** Solves puzzle with multiple parallel threads (thread pool); each thread explores independently with own board copy. */
    public boolean solveParallel(Board board, Map<Integer, Piece> allPieces,
                                Map<Integer, Piece> availablePieces, int numThreads,
                                String puzzleName, SequentialSolver sequentialSolver) {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           PARALLEL SEARCH WITH " + numThreads + " THREADS            ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        // Reset global flags
        solutionFound.set(false);
        globalMaxDepth.set(0);

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
                    long seed = System.currentTimeMillis() + threadId * 1000;
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
                            localBoard = copyBoard(board, allPieces);
                            localPieces = new HashMap<>(allPieces);
                            unusedIds = new ArrayList<>(availablePieces.keySet());
                        }
                    } else {
                        // No save - create board copy for this thread
                        localBoard = copyBoard(board, allPieces);
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
                        diversifySearchStrategy(threadId, localBoard, localPieces, unusedIds, pieceUsed);
                    }

                    // Solve with this thread's configuration
                    return sequentialSolver.solve(localBoard, localPieces, pieceUsed, totalPieces);

                } catch (Exception e) {
                    e.printStackTrace();
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
                e.printStackTrace();
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

    /** Applies diversification strategy by pre-placing different corner pieces so threads explore different search branches. */
    private void diversifySearchStrategy(int threadId, Board board, Map<Integer, Piece> pieces,
                                        List<Integer> unusedIds, BitSet pieceUsed) {
        if (threadId >= 4 || unusedIds.size() <= 10) {
            return; // Diversify only first 4 threads and if enough pieces
        }

        // Identify corner pieces (with 2 edges at zero)
        List<Integer> cornerPieces = new ArrayList<>();
        for (int pid : unusedIds) {
            Piece p = pieces.get(pid);
            int[] edges = p.getEdges();
            int zeroCount = 0;
            for (int e : edges) {
                if (e == 0) zeroCount++;
            }
            if (zeroCount == 2) {
                cornerPieces.add(pid);
            }
        }

        if (threadId >= cornerPieces.size()) {
            return;
        }

        int cornerPieceId = cornerPieces.get(threadId);
        int cornerRow = -1, cornerCol = -1;

        // Position based on thread ID
        switch (threadId) {
            case 0: cornerRow = 0; cornerCol = 0; break;      // Top-left
            case 1: cornerRow = 0; cornerCol = 15; break;     // Top-right
            case 2: cornerRow = 15; cornerCol = 0; break;     // Bottom-left
            case 3: cornerRow = 15; cornerCol = 15; break;    // Bottom-right
        }

        Piece cornerPiece = pieces.get(cornerPieceId);
        // Find rotation that places zeros on correct edges
        for (int rot = 0; rot < 4; rot++) {
            int[] rotEdges = cornerPiece.edgesRotated(rot);
            boolean valid = false;

            if (cornerRow == 0 && cornerCol == 0 && rotEdges[0] == 0 && rotEdges[3] == 0) valid = true;
            if (cornerRow == 0 && cornerCol == 15 && rotEdges[0] == 0 && rotEdges[1] == 0) valid = true;
            if (cornerRow == 15 && cornerCol == 0 && rotEdges[2] == 0 && rotEdges[3] == 0) valid = true;
            if (cornerRow == 15 && cornerCol == 15 && rotEdges[2] == 0 && rotEdges[1] == 0) valid = true;

            if (valid) {
                board.place(cornerRow, cornerCol, cornerPiece, rot);
                pieceUsed.set(cornerPieceId);
                break;
            }
        }
    }

    /** Helper method to copy a board. */
    private Board copyBoard(Board original, Map<Integer, Piece> pieces) {
        Board copy = new Board(original.getRows(), original.getCols());
        for (int r = 0; r < original.getRows(); r++) {
            for (int c = 0; c < original.getCols(); c++) {
                if (!original.isEmpty(r, c)) {
                    Placement p = original.getPlacement(r, c);
                    Piece piece = pieces.get(p.getPieceId());
                    if (piece != null) {
                        copy.place(r, c, piece, p.getRotation());
                    }
                }
            }
        }
        return copy;
    }
}
