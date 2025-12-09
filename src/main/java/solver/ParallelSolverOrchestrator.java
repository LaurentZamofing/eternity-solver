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

/** Orchestrates parallel puzzle solving with thread pool, worker coordination, state management, and diversification. */
public class ParallelSolverOrchestrator {

    // Core dependencies
    private final EternitySolver solverTemplate;
    private final Map<Integer, Piece> allPieces;
    private final String puzzleName;
    private final boolean useDomainCache;

    // Global state (shared across threads)
    private final AtomicBoolean solutionFound;
    private final AtomicInteger globalMaxDepth;
    private final AtomicInteger globalBestScore;
    private final AtomicInteger globalBestThreadId;
    private final AtomicReference<Board> globalBestBoard;
    private final AtomicReference<Map<Integer, Piece>> globalBestPieces;
    private final Object lockObject;

    /** Creates orchestrator with solver template and all shared state dependencies. */
    public ParallelSolverOrchestrator(
            EternitySolver solverTemplate,
            Map<Integer, Piece> allPieces,
            String puzzleName,
            boolean useDomainCache,
            AtomicBoolean solutionFound,
            AtomicInteger globalMaxDepth,
            AtomicInteger globalBestScore,
            AtomicInteger globalBestThreadId,
            AtomicReference<Board> globalBestBoard,
            AtomicReference<Map<Integer, Piece>> globalBestPieces,
            Object lockObject) {
        this.solverTemplate = solverTemplate;
        this.allPieces = allPieces;
        this.puzzleName = puzzleName;
        this.useDomainCache = useDomainCache;
        this.solutionFound = solutionFound;
        this.globalMaxDepth = globalMaxDepth;
        this.globalBestScore = globalBestScore;
        this.globalBestThreadId = globalBestThreadId;
        this.globalBestBoard = globalBestBoard;
        this.globalBestPieces = globalBestPieces;
        this.lockObject = lockObject;
    }

    /** Solves puzzle using multiple parallel threads; returns true if solution found. */
    public boolean solve(Board board, Map<Integer, Piece> availablePieces, int numThreads) {
        printHeader(numThreads);

        // Reset global state
        solutionFound.set(false);
        globalMaxDepth.set(0);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Boolean>> futures = new ArrayList<>();

        // Launch worker threads
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            Future<Boolean> future = executor.submit(() -> runWorkerThread(threadId, board, availablePieces));
            futures.add(future);
        }

        // Start progress monitor
        Thread monitor = startProgressMonitor();

        // Wait for completion and collect results
        boolean solved = waitForCompletion(executor, monitor, futures);

        // Copy solution to original board if found
        if (solved) {
            copySolutionToBoard(board);
        }

        return solved;
    }

    /** Runs a single worker thread with thread-local state and diversification. */
    private boolean runWorkerThread(int threadId, Board originalBoard, Map<Integer, Piece> availablePieces) {
        try {
            // Prepare thread-local state
            WorkerState state = prepareWorkerState(threadId, originalBoard, availablePieces);

            // Apply corner piece diversification
            applyCornerDiversification(threadId, state);

            // Create and configure local solver
            EternitySolver localSolver = createLocalSolver(threadId, state.seed);

            // Print thread start message
            printThreadStart(threadId, state);

            // Domain cache removed (was unused/empty implementation)

            // Run solver
            localSolver.stats.start();
            boolean solved = localSolver.solveBacktracking(state.localBoard, state.localPieces,
                                                          state.pieceUsed, state.totalPieces);

            // Handle result
            if (solved) {
                handleSolutionFound(threadId, state.localBoard, state.localPieces);
                return true;
            }

            printThreadComplete(threadId);
            return false;

        } catch (Exception e) {
            printThreadError(threadId, e);
            return false;
        }
    }

    /** Prepares worker thread state, loading from save if available. */
    private WorkerState prepareWorkerState(int threadId, Board originalBoard,
                                          Map<Integer, Piece> availablePieces) {
        Board localBoard;
        Map<Integer, Piece> localPieces;
        List<Integer> unusedIds;
        long seed = System.currentTimeMillis() + threadId * SolverConstants.THREAD_SEED_OFFSET_MS;
        boolean loadedFromSave = false;

        // Try to load from save
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
                printThreadRestored(threadId, savedDepth);
            } else {
                // Load failed, create fresh
                localBoard = copyBoard(originalBoard);
                localPieces = new HashMap<>(allPieces);
                unusedIds = new ArrayList<>(availablePieces.keySet());
            }
        } else {
            // No save - create fresh
            localBoard = copyBoard(originalBoard);
            localPieces = new HashMap<>(allPieces);
            unusedIds = new ArrayList<>(availablePieces.keySet());
        }

        // Create BitSet
        int totalPieces = localPieces.size();
        int maxPieceId = localPieces.keySet().stream().max(Integer::compareTo).orElse(totalPieces);
        BitSet pieceUsed = new BitSet(maxPieceId + 1);
        for (int pid : localPieces.keySet()) {
            if (!unusedIds.contains(pid)) {
                pieceUsed.set(pid);
            }
        }

        return new WorkerState(localBoard, localPieces, unusedIds, pieceUsed,
                              totalPieces, seed, loadedFromSave);
    }

    /** Applies corner piece diversification strategy for first 4 threads. */
    private void applyCornerDiversification(int threadId, WorkerState state) {
        if (state.loadedFromSave || threadId >= 4 || state.unusedIds.size() <= 10) {
            return; // Skip diversification
        }

        // Find corner pieces (2 edges = 0)
        List<Integer> cornerPieces = new ArrayList<>();
        for (int pid : state.unusedIds) {
            Piece p = state.localPieces.get(pid);
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
            return; // Not enough corner pieces
        }

        // Place corner piece for this thread
        Integer cornerPieceId = cornerPieces.get(threadId);
        int cornerRow = -1, cornerCol = -1;

        switch (threadId) {
            case 0: cornerRow = 0; cornerCol = 0; break;
            case 1: cornerRow = 0; cornerCol = 15; break;
            case 2: cornerRow = 15; cornerCol = 0; break;
            case 3: cornerRow = 15; cornerCol = 15; break;
        }

        Piece cornerPiece = state.localPieces.get(cornerPieceId);
        for (int rot = 0; rot < 4; rot++) {
            int[] rotEdges = cornerPiece.edgesRotated(rot);
            boolean valid = false;

            if (cornerRow == 0 && cornerCol == 0 && rotEdges[0] == 0 && rotEdges[3] == 0) valid = true;
            if (cornerRow == 0 && cornerCol == 15 && rotEdges[0] == 0 && rotEdges[1] == 0) valid = true;
            if (cornerRow == 15 && cornerCol == 0 && rotEdges[2] == 0 && rotEdges[3] == 0) valid = true;
            if (cornerRow == 15 && cornerCol == 15 && rotEdges[2] == 0 && rotEdges[1] == 0) valid = true;

            if (valid) {
                state.localBoard.place(cornerRow, cornerCol, cornerPiece, rot);
                state.pieceUsed.set(cornerPieceId);
                state.cornerPieceId = cornerPieceId;
                state.cornerRow = cornerRow;
                state.cornerCol = cornerCol;
                break;
            }
        }
    }

    /** Creates local solver instance for thread with specific seed. */
    private EternitySolver createLocalSolver(int threadId, long seed) {
        EternitySolver localSolver = new EternitySolver();
        localSolver.random = new Random(seed);
        localSolver.randomSeed = seed;
        localSolver.threadId = threadId;
        localSolver.setPuzzleName(puzzleName); // Use setter instead of direct field access (Refactoring #15)
        localSolver.setVerbose(false);
        localSolver.setUseSingletons(true);
        return localSolver;
    }

    /** Starts daemon progress monitoring thread. */
    private Thread startProgressMonitor() {
        Thread monitor = new Thread(() -> {
            try {
                while (!solutionFound.get() && !Thread.interrupted()) {
                    Thread.sleep(1800000); // 30 minutes
                    printProgress();
                }
            } catch (InterruptedException e) {
                // Normal on shutdown
            }
        });
        monitor.setDaemon(true);
        monitor.start();
        return monitor;
    }

    /** Waits for threads to complete or solution to be found; shuts down executor. */
    private boolean waitForCompletion(ExecutorService executor, Thread monitor,
                                     List<Future<Boolean>> futures) {
        boolean solved = false;
        try {
            for (Future<Boolean> future : futures) {
                try {
                    Boolean result = future.get();
                    if (result) {
                        solved = true;
                        executor.shutdownNow();
                        monitor.interrupt();
                        break;
                    }
                } catch (Exception e) {
                    synchronized (System.err) {
                        System.err.println("Error in thread: " + e.getMessage());
                    }
                }
            }

            if (!solved) {
                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.HOURS);
            }

        } catch (InterruptedException e) {
            System.err.println("Interrupted: " + e.getMessage());
            executor.shutdownNow();
        }

        return solved;
    }

    /** Handles solution found by thread, updating global state. */
    private void handleSolutionFound(int threadId, Board board, Map<Integer, Piece> pieces) {
        synchronized (System.out) {
            System.out.println("\n" + "=".repeat(60));
            SolverLogger.info("🎉 Thread " + threadId + " found SOLUTION! 🎉");
            System.out.println("=".repeat(60));
        }

        synchronized (lockObject) {
            globalBestBoard.set(board);
            globalBestPieces.set(pieces);
        }
    }

    /** Copies solution from global state to target board. */
    private void copySolutionToBoard(Board targetBoard) {
        Board bestBoard = globalBestBoard.get();
        Map<Integer, Piece> bestPieces = globalBestPieces.get();

        if (bestBoard != null) {
            synchronized (lockObject) {
                for (int r = 0; r < bestBoard.getRows(); r++) {
                    for (int c = 0; c < bestBoard.getCols(); c++) {
                        if (!bestBoard.isEmpty(r, c)) {
                            Placement p = bestBoard.getPlacement(r, c);
                            Piece piece = bestPieces.get(p.getPieceId());
                            targetBoard.place(r, c, piece, p.getRotation());
                        }
                    }
                }
            }
        }
    }

    /** Creates deep copy of board. */
    private Board copyBoard(Board original) {
        Board copy = new Board(original.getRows(), original.getCols());
        for (int r = 0; r < original.getRows(); r++) {
            for (int c = 0; c < original.getCols(); c++) {
                if (!original.isEmpty(r, c)) {
                    Placement p = original.getPlacement(r, c);
                    Piece piece = allPieces.get(p.getPieceId());
                    if (piece != null) {
                        copy.place(r, c, piece, p.getRotation());
                    }
                }
            }
        }
        return copy;
    }

    // ==================== Printing Methods ====================

    private void printHeader(int numThreads) {
        SolverLogger.info("\n╔══════════════════════════════════════════════════════════╗");
        SolverLogger.info("║           PARALLEL SEARCH WITH " + numThreads + " THREADS            ║");
        SolverLogger.info("╚══════════════════════════════════════════════════════════╝\n");
    }

    private void printThreadRestored(int threadId, int depth) {
        synchronized (System.out) {
            System.out.println("📂 Thread " + threadId + " restored from save: " +
                             depth + " pieces placed");
        }
    }

    private void printThreadStart(int threadId, WorkerState state) {
        synchronized (System.out) {
            if (state.cornerPieceId != null) {
                System.out.println("🚀 Thread " + threadId + " started (seed=" + state.seed +
                                 ") - Fixed corner: piece " + state.cornerPieceId +
                                 " at (" + state.cornerRow + "," + state.cornerCol + ")");
            } else {
                System.out.println("🚀 Thread " + threadId + " started (seed=" + state.seed + ")");
            }
        }
    }

    private void printThreadComplete(int threadId) {
        synchronized (System.out) {
            SolverLogger.info("✗ Thread " + threadId + " completed without solution");
        }
    }

    private void printThreadError(int threadId, Exception e) {
        synchronized (System.err) {
            System.err.println("✗ Thread " + threadId + " error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printProgress() {
        int depth = globalMaxDepth.get();
        int score = globalBestScore.get();

        if (depth > 0) {
            int maxScore = 480;
            double percentage = maxScore > 0 ? (score * 100.0 / maxScore) : 0.0;

            SolverLogger.info("\n╔════════════════════════════════════════════════════════╗");
            SolverLogger.info("║                  PROGRESS - 30 minutes                 ║");
            SolverLogger.info("╚════════════════════════════════════════════════════════╝");
            SolverLogger.info("📊 Max depth:  " + depth + " pieces placed");
            System.out.println("⭐ Best score:  " + score + "/" + maxScore +
                             " internal edges (" + String.format("%.1f%%", percentage) + ")");
            SolverLogger.info("");
        }
    }

    // ==================== Inner Classes ====================

    /** Encapsulates worker thread state with board, pieces, and diversification info. */
    private static class WorkerState {
        Board localBoard;
        Map<Integer, Piece> localPieces;
        List<Integer> unusedIds;
        BitSet pieceUsed;
        int totalPieces;
        long seed;
        boolean loadedFromSave;
        Integer cornerPieceId;
        int cornerRow = -1;
        int cornerCol = -1;

        WorkerState(Board localBoard, Map<Integer, Piece> localPieces, List<Integer> unusedIds,
                   BitSet pieceUsed, int totalPieces, long seed, boolean loadedFromSave) {
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
