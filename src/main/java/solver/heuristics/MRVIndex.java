package solver.heuristics;

import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Indexed priority queue for MRV (Minimum Remaining Values) cell selection.
 *
 * <p>The legacy {@code MRVCellSelector#findNextCellMRV} scans the entire board
 * on every decision — O(rows × cols) per call. On a 16×16 with depth 200 that
 * is 51 200 cell checks per move just to pick the next cell. This index reduces
 * the steady-state lookup to O(log N) by maintaining a heap keyed on
 * {@code (domainSize, tieBreaker)}.</p>
 *
 * <h2>Lazy invalidation via generation counter</h2>
 *
 * <p>Backtracking changes domain sizes thousands of times per second. Removing
 * old heap entries on every change would be O(log N) per removal, multiplied
 * by every cell whose domain shifts during AC-3 propagation. Instead, each
 * cell carries a monotonic {@code generation} counter; when a cell changes,
 * a fresh node with the new {@code generation} is pushed and the array slot
 * {@code current[r][c]} is updated to point to it. Stale heap entries are
 * silently dropped at {@link #peek()} time — the caller pops until either the
 * heap is empty or the top entry's {@code generation} matches what
 * {@code current[r][c]} expects. Amortised O(log N).</p>
 *
 * <h2>Single-threaded contract</h2>
 *
 * <p>Each solver thread owns its own {@code DomainManager} and therefore its
 * own {@code MRVIndex}. No internal synchronization.</p>
 *
 * <h2>Tie-break</h2>
 *
 * <p>When two cells have the same domain size, the index orders by
 * {@code (-constrainingPower, distToCenter)}: more occupied neighbors and
 * cells closer to the centre come first. {@code constrainingPower} is the
 * count of occupied neighbors at the time the node was pushed — slightly
 * stale during propagation but corrected when the cell is next touched.
 * The {@link MRVCellSelector} fallback is used when {@code prioritizeBorders}
 * is enabled (its border-first / gap-avoidance / trap-detection logic is
 * non-monotonic in {@code domainSize} and not yet captured here).</p>
 */
public final class MRVIndex {

    /** Snapshot of a cell's MRV state at a point in time. */
    private static final class Node implements Comparable<Node> {
        final int row;
        final int col;
        final int domainSize;
        final int constrainingPower;   // -occupied-neighbor count (negated for natural ordering)
        final int distToCenter;
        final long generation;          // monotonic; matched against `currentGen[row][col]` to detect staleness

        Node(int row, int col, int domainSize, int constrainingPower, int distToCenter, long generation) {
            this.row = row;
            this.col = col;
            this.domainSize = domainSize;
            this.constrainingPower = constrainingPower;
            this.distToCenter = distToCenter;
            this.generation = generation;
        }

        @Override
        public int compareTo(Node o) {
            int c = Integer.compare(domainSize, o.domainSize);
            if (c != 0) return c;
            c = Integer.compare(constrainingPower, o.constrainingPower);
            if (c != 0) return c;
            return Integer.compare(distToCenter, o.distToCenter);
        }
    }

    private final PriorityQueue<Node> heap = new PriorityQueue<>();

    /** Current authoritative generation per cell. A heap entry whose generation
     *  doesn't match this is stale. */
    private final long[][] currentGen;

    private final int rows;
    private final int cols;
    private final int centerRow;
    private final int centerCol;
    private final AtomicLong generationCounter = new AtomicLong(0);

    /**
     * Predicate plugged in by the owner (typically a {@code Board::isEmpty}
     * reference) so the index can drop heap entries pointing at cells that
     * have since been filled, without needing every place/remove call site
     * to notify the index.
     */
    @FunctionalInterface
    public interface CellEmptinessProbe {
        boolean isEmpty(int row, int col);
    }

    private CellEmptinessProbe emptinessProbe = (r, c) -> true;

    public MRVIndex(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.centerRow = rows / 2;
        this.centerCol = cols / 2;
        this.currentGen = new long[rows][cols];
    }

    /** Wires in the {@code isEmpty(r,c)} probe used at peek time. */
    public void setEmptinessProbe(CellEmptinessProbe probe) {
        this.emptinessProbe = (probe == null) ? (r, c) -> true : probe;
    }

    /** Resets the index. All cells must be re-registered via {@link #onDomainChanged}. */
    public void clear() {
        heap.clear();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                currentGen[r][c] = 0;
            }
        }
        generationCounter.set(0);
    }

    /** Records a domain change for an empty cell. Pushes a new node and
     *  invalidates any previous heap entry for {@code (row, col)} via
     *  generation counter. */
    public void onDomainChanged(int row, int col, int domainSize, int occupiedNeighbors) {
        long gen = generationCounter.incrementAndGet();
        currentGen[row][col] = gen;
        int dist = Math.abs(row - centerRow) + Math.abs(col - centerCol);
        heap.offer(new Node(row, col, domainSize, -occupiedNeighbors, dist, gen));
    }

    /** Returns the empty cell with the smallest domain (with tie-break), or
     *  {@code null} when no empty cell is registered. Stale entries (and
     *  cells that the {@link CellEmptinessProbe} reports as filled) are
     *  popped off the heap; this is the only place invalidation work happens. */
    public int[] peek() {
        while (!heap.isEmpty()) {
            Node top = heap.peek();
            if (currentGen[top.row][top.col] != top.generation
                || !emptinessProbe.isEmpty(top.row, top.col)) {
                heap.poll();
                continue;
            }
            return new int[] { top.row, top.col };
        }
        return null;
    }

    /** Returns the cached domain size of the cell that {@link #peek()} would
     *  return, or {@code -1} if the heap is empty. Useful for dead-end short
     *  circuit. */
    public int peekDomainSize() {
        while (!heap.isEmpty()) {
            Node top = heap.peek();
            if (currentGen[top.row][top.col] != top.generation
                || !emptinessProbe.isEmpty(top.row, top.col)) {
                heap.poll();
                continue;
            }
            return top.domainSize;
        }
        return -1;
    }

    /** Heap size (stale entries included). For diagnostics / tests only. */
    public int rawHeapSize() {
        return heap.size();
    }
}
