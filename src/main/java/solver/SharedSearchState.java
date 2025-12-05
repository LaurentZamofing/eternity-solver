package solver;

import model.Board;
import model.Piece;

import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Encapsulates shared state for parallel puzzle solving.
 *
 * Replaces static state in ParallelSearchManager to enable:
 * - Multiple concurrent puzzle solving sessions
 * - Better testability (no static state to reset)
 * - Clear ownership of shared state
 * - Dependency injection instead of static coupling
 */
public class SharedSearchState {

    // Solution tracking
    private final AtomicBoolean solutionFound = new AtomicBoolean(false);
    private final AtomicInteger globalMaxDepth = new AtomicInteger(0);
    private final AtomicInteger globalBestScore = new AtomicInteger(0);
    private final AtomicInteger globalBestThreadId = new AtomicInteger(-1);
    private final AtomicReference<Board> globalBestBoard = new AtomicReference<>(null);
    private final AtomicReference<Map<Integer, Piece>> globalBestPieces = new AtomicReference<>(null);

    // Synchronization
    private final Object lockObject = new Object();

    // Work-stealing parallelism
    private ForkJoinPool workStealingPool = null;

    /** Creates new shared search state with all fields initialized to default values. */
    public SharedSearchState() {
        // All fields initialized with their declarations
    }

    /** Resets all state to initial values; call between puzzle solving sessions. */
    public void reset() {
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

    // ==================== Solution Found ====================

    public AtomicBoolean getSolutionFound() {
        return solutionFound;
    }

    public boolean isSolutionFound() {
        return solutionFound.get();
    }

    public void markSolutionFound() {
        solutionFound.set(true);
    }

    // ==================== Global Max Depth ====================

    public AtomicInteger getGlobalMaxDepth() {
        return globalMaxDepth;
    }

    public boolean updateGlobalMaxDepth(int depth) {
        int current;
        do {
            current = globalMaxDepth.get();
            if (depth <= current) return false;
        } while (!globalMaxDepth.compareAndSet(current, depth));
        return true;
    }

    // ==================== Global Best Score ====================

    public AtomicInteger getGlobalBestScore() {
        return globalBestScore;
    }

    public boolean updateGlobalBestScore(int score) {
        int current;
        do {
            current = globalBestScore.get();
            if (score <= current) return false;
        } while (!globalBestScore.compareAndSet(current, score));
        return true;
    }

    // ==================== Global Best Thread ID ====================

    public AtomicInteger getGlobalBestThreadId() {
        return globalBestThreadId;
    }

    public void setGlobalBestThreadId(int threadId) {
        globalBestThreadId.set(threadId);
    }

    // ==================== Global Best Board ====================

    public AtomicReference<Board> getGlobalBestBoard() {
        return globalBestBoard;
    }

    public void setGlobalBestBoard(Board board) {
        globalBestBoard.set(board);
    }

    // ==================== Global Best Pieces ====================

    public AtomicReference<Map<Integer, Piece>> getGlobalBestPieces() {
        return globalBestPieces;
    }

    public void setGlobalBestPieces(Map<Integer, Piece> pieces) {
        globalBestPieces.set(pieces);
    }

    // ==================== Lock Object ====================

    public Object getLockObject() {
        return lockObject;
    }

    // ==================== Work-Stealing Pool ====================

    public ForkJoinPool getWorkStealingPool() {
        return workStealingPool;
    }

    public void setWorkStealingPool(ForkJoinPool pool) {
        this.workStealingPool = pool;
    }

    public void enableWorkStealing(int numThreads) {
        if (workStealingPool == null || workStealingPool.isShutdown()) {
            workStealingPool = new ForkJoinPool(numThreads);
        }
    }
}
