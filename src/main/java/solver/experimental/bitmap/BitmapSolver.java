package solver.experimental.bitmap;

import model.Board;
import model.Piece;
import solver.Solver;

import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bitmap-backed iterative DFS solver — P1 skeleton from ULTRA_PLAN.md.
 *
 * <p>Does <b>not</b> use HashMap / List / Placement objects in the hot loop.
 * Domain per cell is {@code long[WORDS]}; placements are packed {@code int}s;
 * undo is trail-based; forward checking is {@code AND}-masks on longs.</p>
 *
 * <p>P1 scope:</p>
 * <ul>
 *   <li>Precomputed catalog of piece edges + edge-compat masks + border masks.</li>
 *   <li>Iterative DFS with MRV cell selection (smallest non-empty domain).</li>
 *   <li>Forward checking on the 4 orthogonal neighbours after placement.</li>
 *   <li>Trail-based backtrack.</li>
 * </ul>
 *
 * <p>Not yet: nogood cache (P2), Luby restart (P2), parallel (P3), LCV (P5).</p>
 */
public final class BitmapSolver implements Solver {

    private long maxExecutionTimeMs = 120_000;
    private boolean useNogoods = true;
    private int nogoodBits = 22; // 4 M entries → 32 MB
    private boolean useRestart = true;
    private int restartUnit = 128; // Luby multiplier in dead-ends
    private long randomSeed = 0xEA7E811E2L;
    private boolean useFailFirst = false; // disabled: A/B on hard cases showed 0 gain, -1 piece on 8×8 seed=42
    private AtomicBoolean cancellation; // optional: set by parent portfolio to abort early
    private NogoodStore externalNogoods; // optional: shared across portfolio workers

    // Fail-first heuristic: per-cell dead-end counter. Used as MRV tiebreaker
    // so "hot" cells (repeatedly failing) are picked over quiet ones — shifts
    // the search toward the combinatorially hard regions earlier.
    private int[] cellFailWeight;

    // Best partial — max depth reached at any point during the search.
    // Kept as two parallel arrays (cell → pidRot) sized numCells; snapshot on
    // every new-max placement (rare, so the arraycopy cost is negligible).
    private volatile int bestDepthSeen = 0;
    private volatile long timeToBestDepthMs = 0L; // wall-time from solve() start when bestDepthSeen last increased
    private int[] bestCellsAtDepth;
    private int[] bestPidRotsAtDepth;
    private int bestCols; // board width, cached so writeBestTo doesn't need the catalog

    public BitmapSolver() { }

    public void setMaxExecutionTime(long ms) { this.maxExecutionTimeMs = ms; }

    /** Disable nogood caching (mostly for A/B testing). Default: true. */
    public void setUseNogoods(boolean on) { this.useNogoods = on; }

    /** Override the log2 size of the nogood cache. Default 22 → 32 MB. */
    public void setNogoodBits(int bits) { this.nogoodBits = bits; }

    /** Disable Luby restart. Default: true. */
    public void setUseRestart(boolean on) { this.useRestart = on; }

    /** Luby unit — conflicts before first restart (term × unit). Default 128. */
    public void setRestartUnit(int u) { this.restartUnit = u; }

    /** Seed for the randomized MRV tiebreaker (only matters if restart is on). */
    public void setRandomSeed(long seed) { this.randomSeed = seed; }

    /** Shared cancellation flag — the solver polls this in the deadline check
     *  and aborts (returns {@code false}) when another portfolio worker wins. */
    public void setCancellation(AtomicBoolean flag) { this.cancellation = flag; }

    /** Inject a shared {@link NogoodStore} (typically a {@link SharedNogoodStore})
     *  so that dead-ends discovered by one worker are visible to all others.
     *  If {@code null}, the solver falls back to a thread-local {@link NogoodCache}. */
    public void setExternalNogoods(NogoodStore store) { this.externalNogoods = store; }

    /** Toggle the fail-first (constraint-weighting) MRV tiebreaker. Default: true. */
    public void setUseFailFirst(boolean on) { this.useFailFirst = on; }

    /** Max depth ever reached during the most recent {@link #solve} call —
     *  equal to the number of pieces in the best partial assignment seen. */
    public int getBestDepth() { return bestDepthSeen; }

    /** Wall-time (ms since solve() start) at which {@link #getBestDepth}
     *  was last set. Lets callers distinguish "reached max depth quickly
     *  then stalled" from "reached max depth just before timeout". */
    public long getTimeToBestDepthMs() { return timeToBestDepthMs; }

    /** Writes the best partial assignment ever reached during the most recent
     *  {@link #solve} call to {@code b}. Useful when {@link #solve} returned
     *  {@code false} (timeout / cancellation) but you still want to inspect
     *  how far the search got. No-op if no placement was ever committed. */
    public void writeBestTo(Board b, Map<Integer, Piece> pieces) {
        int depth = bestDepthSeen;
        if (depth <= 0 || bestCellsAtDepth == null) return;
        for (int d = 0; d < depth; d++) {
            int cell = bestCellsAtDepth[d];
            int pr = bestPidRotsAtDepth[d];
            int pid = PiecesCatalog.pieceIdOf(pr);
            int rot = PiecesCatalog.rotationOf(pr);
            int cr = cell / bestCols;
            int cc = cell % bestCols;
            if (b.isEmpty(cr, cc)) b.place(cr, cc, pieces.get(pid), rot);
        }
    }

    @Override
    public boolean solve(Board board, Map<Integer, Piece> pieces) {
        // POC: requires empty board. Pre-filled boards would need the
        // catalog initial domains to be narrowed by the pre-placement —
        // left for a follow-up.
        int rows = board.getRows();
        int cols = board.getCols();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!board.isEmpty(r, c)) {
                    throw new IllegalArgumentException(
                        "BitmapSolver P1 requires an empty board; cell ("
                        + r + "," + c + ") is pre-filled.");
                }
            }
        }

        bestDepthSeen = 0;
        timeToBestDepthMs = 0L;
        long solveStartMs = System.currentTimeMillis();
        bestCols = cols;
        bestCellsAtDepth = new int[rows * cols];
        bestPidRotsAtDepth = new int[rows * cols];
        cellFailWeight = useFailFirst ? new int[rows * cols] : null;

        PiecesCatalog catalog = new PiecesCatalog(rows, cols, pieces, 0xC0FFEEL);
        // Trail capacity: worst-case every placement touches every word of
        // every cell → numCells² × words. Over-dimensioned on purpose.
        int trailCapacity = catalog.numCells * catalog.numCells * catalog.words * 2 + 1024;
        SearchState state = new SearchState(catalog, trailCapacity);
        ForwardChecker fc = new ForwardChecker(catalog);
        NogoodStore nogoods = useNogoods
            ? (externalNogoods != null ? externalNogoods : new NogoodCache(nogoodBits))
            : null;
        LubyRestart luby = useRestart ? new LubyRestart() : null;
        SplittableRandom random = useRestart ? new SplittableRandom(randomSeed) : null;

        long deadline = System.currentTimeMillis() + maxExecutionTimeMs;

        int[] depthCell = state.cellAtDepth;       // alias
        int[] cursorAtDepth = new int[catalog.numCells];

        long deadEndCount = 0L;
        long restartThreshold = useRestart ? (long) luby.next() * restartUnit : Long.MAX_VALUE;

        int depth = 0;
        // Pick first cell then enter loop.
        int currentCell = random != null ? pickCellRandomized(state, random) : pickCell(state);
        if (currentCell < 0) return true;          // empty board → already solved
        state.beginDepth(depth);
        depthCell[depth] = currentCell;
        cursorAtDepth[depth] = 0;

        AtomicBoolean cancelFlag = this.cancellation;
        // Iteration counter — we poll the deadline / cancellation every
        // DEADLINE_POLL_INTERVAL iterations, independent of `depth`. The
        // earlier depth-only poll (`depth & 0x3F == 0`) could starve for
        // minutes when the search oscillates between depths that never
        // reach a 64-multiple — e.g. thrashing between 70 and 78 on a
        // 9×9 puzzle would miss the 64 / 128 boundaries entirely.
        long iter = 0;
        while (true) {
            if ((iter++ & 0xFFFFL) == 0L) {
                if (System.currentTimeMillis() > deadline) return false;
                if (cancelFlag != null && cancelFlag.get()) return false;
            }

            // Luby restart — wipe the current search tree, keep the nogood cache.
            if (useRestart && deadEndCount >= restartThreshold) {
                while (depth > 0) {
                    state.rollbackDepth(depth);
                    depth--;
                    state.rollbackPlacement(depth);
                }
                state.rollbackDepth(0);
                int rc = pickCellRandomized(state, random);
                if (rc < 0) return true;
                state.beginDepth(0);
                depthCell[0] = rc;
                cursorAtDepth[0] = 0;
                deadEndCount = 0;
                restartThreshold = (long) luby.next() * restartUnit;
                continue;
            }

            int cell = depthCell[depth];
            long[] dom = state.domain[cell];
            int nextPidRot = PiecesCatalog.nextSetBit(dom, cursorAtDepth[depth]);

            if (nextPidRot < 0) {
                // Cell exhausted → partial assignment 0..depth-1 is proven dead.
                if (nogoods != null && depth > 0) nogoods.add(state.stateHash);
                if (cellFailWeight != null) cellFailWeight[depthCell[depth]]++;
                state.rollbackDepth(depth);
                if (depth == 0) return false;
                depth--;
                state.rollbackPlacement(depth);
                cursorAtDepth[depth] = state.pidRotAtDepth[depth] + 1;
                deadEndCount++;
                continue;
            }

            cursorAtDepth[depth] = nextPidRot + 1;

            // Apply placement: collapse cell to this pidRot + mark piece used.
            // Phase 1 picks one pidRot but pidRots with the same pieceId are
            // siblings — prune all pieceId rotations from all other cells.
            int pieceId = PiecesCatalog.pieceIdOf(nextPidRot);
            state.commitPlacement(depth, cell, nextPidRot);

            // Remove all pidRots of this pieceId from every other cell; also
            // detects dead-end if any other cell's domain becomes empty.
            boolean pieceRemoveOk = removePieceFromOthers(state, catalog, cell, pieceId);

            // Forward checking on neighbours.
            if (!pieceRemoveOk || !fc.apply(state, cell, nextPidRot)) {
                // Dead-end — rollback placement, try next value at same depth.
                if (cellFailWeight != null) cellFailWeight[cell]++;
                state.rollbackPlacement(depth);
                deadEndCount++;
                continue;
            }

            // Nogood lookup — if this partial assignment has been proven dead
            // in an earlier branch (or restart), skip it.
            if (nogoods != null && nogoods.contains(state.stateHash)) {
                if (cellFailWeight != null) cellFailWeight[cell]++;
                state.rollbackPlacement(depth);
                deadEndCount++;
                continue;
            }

            // Snapshot best partial if this placement extends the max depth
            // we've ever reached. Rare update → the arraycopy is negligible.
            int committed = depth + 1;
            if (committed > bestDepthSeen) {
                bestDepthSeen = committed;
                timeToBestDepthMs = System.currentTimeMillis() - solveStartMs;
                System.arraycopy(state.cellAtDepth, 0, bestCellsAtDepth, 0, committed);
                System.arraycopy(state.pidRotAtDepth, 0, bestPidRotsAtDepth, 0, committed);
            }

            if (state.emptyCount == 0) {
                // Solution found — write it back to the board.
                for (int d = 0; d <= depth; d++) {
                    int cd = state.cellAtDepth[d];
                    int pr = state.pidRotAtDepth[d];
                    int pid = PiecesCatalog.pieceIdOf(pr);
                    int rot = PiecesCatalog.rotationOf(pr);
                    int cr = cd / catalog.cols;
                    int cc = cd % catalog.cols;
                    board.place(cr, cc, pieces.get(pid), rot);
                }
                return true;
            }

            // Descend.
            depth++;
            int nextCell = random != null ? pickCellRandomized(state, random) : pickCell(state);
            if (nextCell < 0) {
                // Shouldn't happen (emptyCount > 0) but be defensive.
                state.rollbackPlacement(depth - 1);
                depth--;
                continue;
            }
            state.beginDepth(depth);
            depthCell[depth] = nextCell;
            cursorAtDepth[depth] = 0;
        }
    }

    /** MRV + fail-first: pick the non-placed cell with the smallest non-empty
     *  domain, breaking ties by highest {@link #cellFailWeight}. */
    private int pickCell(SearchState state) {
        int best = -1;
        int bestSize = Integer.MAX_VALUE;
        int bestWeight = -1;
        int n = state.catalog.numCells;
        boolean[] placed = state.isCellPlaced;
        int[] size = state.domainSize;
        int[] weight = cellFailWeight;
        for (int c = 0; c < n; c++) {
            if (placed[c]) continue;
            int card = size[c];
            if (card == 0) return c; // dead-end surfaced via MRV
            if (card < bestSize) {
                bestSize = card;
                best = c;
                bestWeight = weight != null ? weight[c] : 0;
                if (card == 1) return c; // can't do better than a singleton
            } else if (card == bestSize && weight != null && weight[c] > bestWeight) {
                best = c;
                bestWeight = weight[c];
            }
        }
        return best;
    }

    /** MRV + fail-first, with randomised tiebreak among cells that share the
     *  same (domain-size, weight) — preserves portfolio diversification. */
    private int pickCellRandomized(SearchState state, SplittableRandom rng) {
        int best = -1;
        int bestSize = Integer.MAX_VALUE;
        int bestWeight = -1;
        int tieCount = 0;
        int n = state.catalog.numCells;
        boolean[] placed = state.isCellPlaced;
        int[] size = state.domainSize;
        int[] weight = cellFailWeight;
        for (int c = 0; c < n; c++) {
            if (placed[c]) continue;
            int card = size[c];
            if (card == 0) return c;
            int w = weight != null ? weight[c] : 0;
            if (card < bestSize || (card == bestSize && w > bestWeight)) {
                bestSize = card;
                bestWeight = w;
                best = c;
                tieCount = 1;
                if (card == 1) return c;
            } else if (card == bestSize && w == bestWeight) {
                tieCount++;
                if (rng.nextInt(tieCount) == 0) best = c;
            }
        }
        return best;
    }

    /** Removes the 4 bits representing pieceId × 4 rotations from every cell
     *  except {@code keepCell}. Returns {@code false} (dead-end) if any
     *  non-placed cell's domain becomes empty. Uses the cached
     *  {@link SearchState#domainSize} instead of summing words. */
    private boolean removePieceFromOthers(SearchState state, PiecesCatalog cat, int keepCell, int pieceId) {
        int base = pieceId * 4;
        int word = base >>> 6;
        long pieceMask = ~(0xFL << (base & 63));
        int n = cat.numCells;
        boolean[] placed = state.isCellPlaced;
        int[] size = state.domainSize;
        for (int c = 0; c < n; c++) {
            if (c == keepCell) continue;
            long[] dom = state.domain[c];
            long old = dom[word];
            long nw = old & pieceMask;
            if (nw != old) {
                state.writeWord(c, word, nw);
                if (!placed[c] && size[c] == 0) return false;
            }
        }
        return true;
    }
}
