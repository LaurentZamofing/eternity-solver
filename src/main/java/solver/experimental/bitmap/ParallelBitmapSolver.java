package solver.experimental.bitmap;

import model.Board;
import model.Piece;
import model.Placement;
import solver.Solver;

import java.util.HashMap;
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

    public ParallelBitmapSolver() { }

    public void setThreads(int n) { this.threads = Math.max(1, n); }

    public void setMaxExecutionTime(long ms) { this.maxExecutionTimeMs = ms; }

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

        for (int i = 0; i < n; i++) {
            final int idx = i;
            pool.submit(() -> {
                Board local = new Board(rows, cols);
                BitmapSolver solver = configFor(idx);
                solver.setMaxExecutionTime(maxExecutionTimeMs);
                solver.setCancellation(foundFlag);
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

        boolean signalled = false;
        try {
            signalled = done.await(maxExecutionTimeMs + 1_000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            foundFlag.set(true); // ensure all workers abort
            pool.shutdownNow();
        }

        if (!signalled || !foundFlag.get()) return false;

        Board w = winningBoard.get();
        if (w == null) return false;
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
