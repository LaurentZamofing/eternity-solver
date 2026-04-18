package solver.experimental.bitmap;

import model.Board;
import model.Piece;
import solver.Solver;

import java.util.Map;

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

    public BitmapSolver() { }

    public void setMaxExecutionTime(long ms) { this.maxExecutionTimeMs = ms; }

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

        PiecesCatalog catalog = new PiecesCatalog(rows, cols, pieces, 0xC0FFEEL);
        // Trail capacity: worst-case every placement touches every word of
        // every cell → numCells² × words. Over-dimensioned on purpose.
        int trailCapacity = catalog.numCells * catalog.numCells * catalog.words * 2 + 1024;
        SearchState state = new SearchState(catalog, trailCapacity);
        ForwardChecker fc = new ForwardChecker(catalog);

        long deadline = System.currentTimeMillis() + maxExecutionTimeMs;

        int[] depthCell = state.cellAtDepth;       // alias
        int[] cursorAtDepth = new int[catalog.numCells];

        int depth = 0;
        // Pick first cell then enter loop.
        int currentCell = pickCell(state);
        if (currentCell < 0) return true;          // empty board → already solved
        state.beginDepth(depth);
        depthCell[depth] = currentCell;
        cursorAtDepth[depth] = 0;

        while (true) {
            if ((depth & 0x3F) == 0 && System.currentTimeMillis() > deadline) return false;

            int cell = depthCell[depth];
            long[] dom = state.domain[cell];
            int nextPidRot = PiecesCatalog.nextSetBit(dom, cursorAtDepth[depth]);

            if (nextPidRot < 0) {
                // Cell exhausted → backtrack.
                state.rollbackDepth(depth);
                if (depth == 0) return false;
                depth--;
                state.rollbackPlacement(depth);
                cursorAtDepth[depth] = PiecesCatalog.nextSetBit(
                    state.domain[depthCell[depth]], 0) + 1;
                // That was before the placement collapsed the cell domain;
                // use the pidRot that was placed + 1 to avoid re-trying it.
                cursorAtDepth[depth] = state.pidRotAtDepth[depth] + 1;
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
                state.rollbackPlacement(depth);
                continue;
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
            int nextCell = pickCell(state);
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

    /** MRV: pick the non-placed cell with the smallest non-empty domain.
     *  Uses the incremental {@link SearchState#domainSize} cache — O(cells). */
    private int pickCell(SearchState state) {
        int best = -1;
        int bestSize = Integer.MAX_VALUE;
        int n = state.catalog.numCells;
        boolean[] placed = state.isCellPlaced;
        int[] size = state.domainSize;
        for (int c = 0; c < n; c++) {
            if (placed[c]) continue;
            int card = size[c];
            if (card == 0) return c; // dead-end surfaced via MRV
            if (card < bestSize) {
                bestSize = card;
                best = c;
                if (card == 1) return c; // can't do better than a singleton
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
