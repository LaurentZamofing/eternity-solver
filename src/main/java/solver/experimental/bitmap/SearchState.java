package solver.experimental.bitmap;

/**
 * Mutable state for one in-flight bitmap search.
 *
 * <p>The critical design choice is <b>trail-based undo</b>: when a placement
 * or propagation masks a cell's domain from {@code old} to {@code new}, we
 * push {@code (cellIndex, wordIndex, oldValue)} onto the trail. On backtrack
 * we pop until {@code trailCheckpoint[depth]} and write the old values back.
 * This costs O(Δ) per undo — one entry per word actually changed — instead of
 * O(numCells × words) for a naïve per-depth snapshot.</p>
 *
 * <h3>Thread-safety</h3>
 * Not thread-safe. Each solver / worker owns a fresh {@code SearchState}.
 */
public final class SearchState {

    final PiecesCatalog catalog;

    /** {@code [cell][word]} — current domain bitmaps. Mutated in-place; trail stores reversals. */
    public final long[][] domain;

    /**
     * Trail of (packedCellWord, oldValue) pairs. Even indices hold the packed
     * {@code cell * words + wordIndex}, odd indices hold the old {@code long}.
     * Grows as the search descends; sized generously at construction.
     */
    public final long[] trail;

    /** Index of the next slot to write in {@link #trail}. Two slots per entry. */
    public int trailPos;

    /** {@code [depth]} — value of {@code trailPos} at the start of that depth. */
    public final int[] trailCheckpoint;

    /** {@code [depth]} — cell chosen at that depth. */
    public final int[] cellAtDepth;

    /** {@code [depth]} — pidRot placed at that depth. */
    public final int[] pidRotAtDepth;

    /** Number of cells still empty. Decrements on {@link #commitPlacement}. */
    public int emptyCount;

    /** Incremental Zobrist hash of the current partial assignment. */
    public long stateHash;

    /**
     * Builds a fresh state seeded with the catalog's initial domain for each
     * cell (which already bakes in border-piece segregation).
     *
     * @param catalog        shared precomputed tables
     * @param trailCapacity  max number of word-writes during the whole search;
     *                       safely overestimated by caller
     */
    public SearchState(PiecesCatalog catalog, int trailCapacity) {
        this.catalog = catalog;
        this.domain = new long[catalog.numCells][];
        for (int c = 0; c < catalog.numCells; c++) {
            domain[c] = catalog.initialDomainForCell(c);
        }
        this.trail = new long[2 * trailCapacity];
        this.trailPos = 0;
        this.trailCheckpoint = new int[catalog.numCells + 1];
        this.cellAtDepth = new int[catalog.numCells];
        this.pidRotAtDepth = new int[catalog.numCells];
        this.emptyCount = catalog.numCells;
        this.stateHash = 0L;
    }

    /**
     * Records the current value of {@code domain[cell][word]} on the trail and
     * replaces the word with {@code newValue}. Callers use this before any
     * {@code AND}-mask update. If {@code newValue == domain[cell][word]} this
     * is a no-op (no trail entry).
     */
    public void writeWord(int cell, int word, long newValue) {
        long cur = domain[cell][word];
        if (cur == newValue) return;
        // Pack cell and word index into a single long (cell << 32 | word)
        long packed = ((long) cell << 32) | (word & 0xFFFFFFFFL);
        trail[trailPos]     = packed;
        trail[trailPos + 1] = cur;
        trailPos += 2;
        domain[cell][word] = newValue;
    }

    /** Opens a new depth level — records the current {@code trailPos}. */
    public void beginDepth(int depth) {
        trailCheckpoint[depth] = trailPos;
    }

    /** Rolls back all trail entries recorded since {@code beginDepth(depth)}. */
    public void rollbackDepth(int depth) {
        int checkpoint = trailCheckpoint[depth];
        while (trailPos > checkpoint) {
            trailPos -= 2;
            long packed = trail[trailPos];
            long old = trail[trailPos + 1];
            int cell = (int) (packed >>> 32);
            int word = (int) packed;
            domain[cell][word] = old;
        }
    }

    /**
     * Commits a placement — narrows the cell's domain to only the single
     * {@code pidRot} and updates the incremental hash. Caller is responsible
     * for calling {@link #beginDepth} first.
     */
    public void commitPlacement(int depth, int cell, int pidRot) {
        cellAtDepth[depth] = cell;
        pidRotAtDepth[depth] = pidRot;
        stateHash ^= catalog.zobristTable[cell][pidRot];
        emptyCount--;

        // Collapse the cell domain to the one bit (pidRot).
        int targetWord = pidRot >>> 6;
        long targetBit = 1L << (pidRot & 63);
        for (int w = 0; w < catalog.words; w++) {
            long newValue = (w == targetWord) ? targetBit : 0L;
            writeWord(cell, w, newValue);
        }
    }

    /** Inverts {@link #commitPlacement} for depth {@code depth}. */
    public void rollbackPlacement(int depth) {
        int cell = cellAtDepth[depth];
        int pidRot = pidRotAtDepth[depth];
        stateHash ^= catalog.zobristTable[cell][pidRot];
        emptyCount++;
        rollbackDepth(depth);
    }

    /** Cardinality of the domain for one cell. */
    public int cellDomainSize(int cell) {
        return PiecesCatalog.cardinality(domain[cell]);
    }

    /** True iff at least one cell's domain is empty. */
    public boolean anyDomainEmpty() {
        for (int c = 0; c < catalog.numCells; c++) {
            if (PiecesCatalog.cardinality(domain[c]) == 0) return true;
        }
        return false;
    }

    /** Fast check: is the given cell's domain empty? */
    public boolean cellEmpty(int cell) {
        for (long w : domain[cell]) {
            if (w != 0) return false;
        }
        return true;
    }

    /** True iff any cell's domain other than {@code exceptCell} is empty. */
    public boolean anyDomainEmptyExcept(int exceptCell) {
        for (int c = 0; c < catalog.numCells; c++) {
            if (c == exceptCell) continue;
            if (cellEmpty(c)) return true;
        }
        return false;
    }
}
