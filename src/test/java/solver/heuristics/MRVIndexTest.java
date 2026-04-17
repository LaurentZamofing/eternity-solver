package solver.heuristics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MRVIndex}: heap ordering, lazy invalidation,
 * tie-break, dead-end short circuit, and the {@code emptinessProbe}
 * filter that drops cells the board has since filled.
 *
 * MRVIndex is package-private; tests live in the same package.
 */
class MRVIndexTest {

    /** Mutable bitmap used as a fake board.isEmpty(r,c) probe. */
    private static final class FakeBoard {
        final boolean[][] empty;
        FakeBoard(int rows, int cols) {
            this.empty = new boolean[rows][cols];
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) empty[r][c] = true;
            }
        }
        void fill(int r, int c) { empty[r][c] = false; }
        void free(int r, int c) { empty[r][c] = true; }
        boolean isEmpty(int r, int c) { return empty[r][c]; }
    }

    private MRVIndex makeIndex(int rows, int cols, FakeBoard board) {
        MRVIndex idx = new MRVIndex(rows, cols);
        idx.setEmptinessProbe(board::isEmpty);
        return idx;
    }

    @Test
    void emptyHeapReturnsNull() {
        FakeBoard board = new FakeBoard(3, 3);
        MRVIndex idx = makeIndex(3, 3, board);
        assertNull(idx.peek());
        assertEquals(-1, idx.peekDomainSize());
    }

    @Test
    void singleCellIsReturned() {
        FakeBoard board = new FakeBoard(3, 3);
        MRVIndex idx = makeIndex(3, 3, board);
        idx.onDomainChanged(1, 1, 4, 0);

        assertArrayEquals(new int[]{1, 1}, idx.peek());
        assertEquals(4, idx.peekDomainSize());
    }

    @Test
    void smallerDomainComesFirst() {
        FakeBoard board = new FakeBoard(3, 3);
        MRVIndex idx = makeIndex(3, 3, board);
        idx.onDomainChanged(0, 0, 10, 0);
        idx.onDomainChanged(1, 1, 3, 0);   // smallest
        idx.onDomainChanged(2, 2, 7, 0);

        assertArrayEquals(new int[]{1, 1}, idx.peek());
    }

    @Test
    void tieBreakByConstrainingPower() {
        FakeBoard board = new FakeBoard(3, 3);
        MRVIndex idx = makeIndex(3, 3, board);
        idx.onDomainChanged(0, 0, 5, 0);   // 0 occupied neighbors
        idx.onDomainChanged(0, 1, 5, 2);   // 2 occupied neighbors → more constraining → wins
        idx.onDomainChanged(0, 2, 5, 1);

        assertArrayEquals(new int[]{0, 1}, idx.peek());
    }

    @Test
    void tieBreakByDistToCenter() {
        FakeBoard board = new FakeBoard(5, 5);
        MRVIndex idx = makeIndex(5, 5, board);
        // Same domainSize, same constraining power → centre proximity wins
        idx.onDomainChanged(0, 0, 5, 0);  // dist 4
        idx.onDomainChanged(2, 2, 5, 0);  // dist 0 → wins
        idx.onDomainChanged(4, 4, 5, 0);

        assertArrayEquals(new int[]{2, 2}, idx.peek());
    }

    @Test
    void domainChangeInvalidatesPreviousEntry() {
        FakeBoard board = new FakeBoard(3, 3);
        MRVIndex idx = makeIndex(3, 3, board);
        idx.onDomainChanged(0, 0, 10, 0);
        idx.onDomainChanged(1, 1, 5, 0);

        idx.onDomainChanged(0, 0, 2, 0);   // (0,0) shrinks below (1,1)

        assertArrayEquals(new int[]{0, 0}, idx.peek());
        assertEquals(2, idx.peekDomainSize());
    }

    @Test
    void filledCellIsSkippedViaProbe() {
        FakeBoard board = new FakeBoard(3, 3);
        MRVIndex idx = makeIndex(3, 3, board);
        idx.onDomainChanged(0, 0, 3, 0);
        idx.onDomainChanged(1, 1, 5, 0);

        board.fill(0, 0);   // board now reports (0,0) as occupied

        assertArrayEquals(new int[]{1, 1}, idx.peek());
    }

    @Test
    void freedCellNeedsNewDomainBeforeReappearing() {
        FakeBoard board = new FakeBoard(3, 3);
        MRVIndex idx = makeIndex(3, 3, board);
        idx.onDomainChanged(0, 0, 3, 0);
        idx.onDomainChanged(1, 1, 5, 0);

        board.fill(0, 0);
        assertArrayEquals(new int[]{1, 1}, idx.peek());

        // Free the cell — must be re-registered before it surfaces with new size
        board.free(0, 0);
        idx.onDomainChanged(0, 0, 2, 0);
        assertArrayEquals(new int[]{0, 0}, idx.peek());
    }

    @Test
    void clearResetsEverything() {
        FakeBoard board = new FakeBoard(3, 3);
        MRVIndex idx = makeIndex(3, 3, board);
        idx.onDomainChanged(0, 0, 3, 0);
        idx.onDomainChanged(2, 2, 4, 0);

        idx.clear();

        assertNull(idx.peek());
        assertEquals(0, idx.rawHeapSize());

        idx.onDomainChanged(1, 1, 1, 0);
        assertArrayEquals(new int[]{1, 1}, idx.peek());
    }

    @Test
    void deadEndSurfacesAsDomainSizeZero() {
        FakeBoard board = new FakeBoard(3, 3);
        MRVIndex idx = makeIndex(3, 3, board);
        idx.onDomainChanged(0, 0, 5, 0);
        idx.onDomainChanged(1, 1, 0, 0);   // dead-end

        assertArrayEquals(new int[]{1, 1}, idx.peek());
        assertEquals(0, idx.peekDomainSize());
    }

    @Test
    void filledCellDoesNotResurfaceFromStaleEntries() {
        FakeBoard board = new FakeBoard(3, 3);
        MRVIndex idx = makeIndex(3, 3, board);
        idx.onDomainChanged(0, 0, 5, 0);
        idx.onDomainChanged(0, 0, 3, 1);   // bumps gen, pushes new node — old is now stale
        board.fill(0, 0);                  // board fills the cell

        assertNull(idx.peek(),
            "filled cell must never come back regardless of stale heap entries");
    }
}
