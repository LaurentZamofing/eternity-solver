package solver.experimental.bitmap;

import model.Board;
import model.Piece;
import model.Placement;
import solver.Solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Portfolio-parallel bitmap solver (ULTRA_PLAN P3).
 *
 * <p>Spawns {@code N} worker threads, each running a {@link BitmapSolver}
 * with a different configuration (deterministic MRV, randomized MRV with
 * various seeds, different Luby restart units). First worker to find a
 * solution wins; the others are signalled via a shared {@code AtomicBoolean}
 * and exit at their next deadline check (~every 64 descends).</p>
 *
 * <p>Each worker has its own {@link NogoodCache} for now — sharing the
 * cache requires sharded locks and is deferred until measurements justify
 * the complexity.</p>
 *
 * <p>The winning worker writes to its private {@link Board}; after the
 * CAS wins, the outer thread copies placements into the user's board.</p>
 */
public final class ParallelBitmapSolver implements Solver {

    private int threads = Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors()));
    private long maxExecutionTimeMs = 60_000;
    private boolean shareNogoods = true;
    private Boolean useFailFirstOverride; // null = inherit BitmapSolver default
    private final List<BitmapSolver> lastWorkers = new ArrayList<>();
    private volatile int lastBestDepth = 0;

    public ParallelBitmapSolver() { }

    public void setThreads(int n) { this.threads = Math.max(1, n); }

    public void setMaxExecutionTime(long ms) { this.maxExecutionTimeMs = ms; }

    /** Share a single {@link SharedNogoodStore} across all workers so
     *  dead-ends discovered by one are visible to all. Default: true. */
    public void setShareNogoods(boolean on) { this.shareNogoods = on; }

    /** Override {@link BitmapSolver#setUseFailFirst} for every portfolio
     *  worker. {@code null} leaves the BitmapSolver default in place. */
    public void setUseFailFirst(boolean on) { this.useFailFirstOverride = on; }

    @Override
    public boolean solve(Board board, Map<Integer, Piece> pieces) {
        int rows = board.getRows();
        int cols = board.getCols();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!board.isEmpty(r, c)) {
                    throw new IllegalArgumentException(
                        "ParallelBitmapSolver requires an empty board; cell ("
                        + r + "," + c + ") is pre-filled.");
                }
            }
        }

        int n = threads;
        AtomicBoolean foundFlag = new AtomicBoolean(false);
        AtomicReference<Board> winningBoard = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(n, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("bitmap-portfolio-" + t.getId());
            return t;
        });

        lastWorkers.clear();
        lastBestDepth = 0;
        SharedNogoodStore shared = shareNogoods ? new SharedNogoodStore() : null;
        for (int i = 0; i < n; i++) {
            final int idx = i;
            BitmapSolver solver = configFor(idx);
            solver.setMaxExecutionTime(maxExecutionTimeMs);
            solver.setCancellation(foundFlag);
            if (shared != null) solver.setExternalNogoods(shared);
            if (useFailFirstOverride != null) solver.setUseFailFirst(useFailFirstOverride);
            lastWorkers.add(solver);
            pool.submit(() -> {
                Board local = new Board(rows, cols);
                boolean ok;
                try {
                    ok = solver.solve(local, new HashMap<>(pieces));
                } catch (Throwable t) {
                    return; // swallow — other workers may still succeed
                }
                if (ok && foundFlag.compareAndSet(false, true)) {
                    winningBoard.set(local);
                    done.countDown();
                }
            });
        }

        boolean solved = false;
        try {
            solved = done.await(maxExecutionTimeMs + 1_000, TimeUnit.MILLISECONDS)
                  && winningBoard.get() != null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            foundFlag.set(true); // ensure all workers abort
            pool.shutdownNow();
            try {
                pool.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }

        // Track the best partial across workers — useful for the caller when
        // we time out: they can still inspect how close the portfolio got.
        int maxDepth = 0;
        BitmapSolver bestWorker = null;
        for (BitmapSolver w : lastWorkers) {
            int d = w.getBestDepth();
            if (d > maxDepth) { maxDepth = d; bestWorker = w; }
        }
        lastBestDepth = maxDepth;

        if (solved) {
            Board w = winningBoard.get();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (!w.isEmpty(r, c)) {
                        Placement p = w.getPlacement(r, c);
                        board.place(r, c, pieces.get(p.getPieceId()), p.getRotation());
                    }
                }
            }
            return true;
        }

        // Timeout path — write best partial from the deepest worker so the
        // caller can inspect (e.g. print its partial-edge score).
        if (bestWorker != null) bestWorker.writeBestTo(board, pieces);
        return false;
    }

    /** Max depth any worker reached during the last {@link #solve} call —
     *  equal to the number of pieces in the best partial ever observed. */
    public int getBestDepth() { return lastBestDepth; }

    /** Portfolio configurations — designed to cover complementary strategies.
     *  Worker 0 is the safety net (deterministic P1-style), workers 1+
     *  diversify via seed × restart unit. */
    private BitmapSolver configFor(int idx) {
        BitmapSolver s = new BitmapSolver();
        switch (idx % 4) {
            case 0 -> {
                // Deterministic: the strategy that had the fastest 7×7 seed=1 in P1.
                s.setUseRestart(false);
                s.setUseNogoods(true);
            }
            case 1 -> {
                // Randomized, aggressive restart.
                s.setUseRestart(true);
                s.setRestartUnit(128);
                s.setRandomSeed(0xC0FFEE00L + idx);
            }
            case 2 -> {
                // Randomized, lazy restart — gives runs time to amortize.
                s.setUseRestart(true);
                s.setRestartUnit(1024);
                s.setRandomSeed(0xDEADBEEFL + idx);
            }
            case 3 -> {
                // Randomized, mid restart unit.
                s.setUseRestart(true);
                s.setRestartUnit(256);
                s.setRandomSeed(0xFEEDFACEL + idx);
            }
        }
        return s;
    }
}
