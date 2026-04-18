package solver.experimental.bitmap;

/**
 * Open-addressing hash set of {@code long} keys, used to remember
 * state hashes of dead partial assignments.
 *
 * <p>The table has fixed capacity {@code 1 << bits} (must be a power of
 * two). The key 0 is reserved as "empty slot" — if a real Zobrist hash
 * happens to be 0, we store it as 1 (both collapse to the same bucket
 * since the probability of a genuine collision on 64-bit is ~2⁻³² at
 * 2²² entries, which dominates).</p>
 *
 * <p>Linear probing on collision, eviction on full (fall back to
 * overwriting an existing slot — false negatives are acceptable: they
 * just lead to re-exploring a dead branch, slower but correct).</p>
 *
 * <p>Not thread-safe. For the parallel solver (P3), either shard (one
 * table per bucket range with a short lock) or use a concurrent variant.</p>
 */
public final class NogoodCache {

    private final long[] table;
    private final int mask;

    /**
     * @param bits log2 of capacity. 22 → 4 M entries → 32 MB (fits L3).
     */
    public NogoodCache(int bits) {
        if (bits < 4 || bits > 30) throw new IllegalArgumentException("bits out of range: " + bits);
        this.table = new long[1 << bits];
        this.mask = (1 << bits) - 1;
    }

    /** Adds {@code key} to the set. No-op if already present. Evicts via linear
     *  probing when the chain gets long (see class Javadoc). */
    public void add(long key) {
        if (key == 0L) key = 1L; // reserve 0 for empty slots
        int idx = (int) (key & mask);
        // Try up to 8 linear probes; fall back to overwriting idx if all occupied.
        for (int probe = 0; probe < 8; probe++) {
            int i = (idx + probe) & mask;
            long k = table[i];
            if (k == 0L || k == key) {
                table[i] = key;
                return;
            }
        }
        table[idx] = key; // overwrite — losing an older nogood is acceptable
    }

    /** Returns {@code true} iff {@code key} is present. */
    public boolean contains(long key) {
        if (key == 0L) key = 1L;
        int idx = (int) (key & mask);
        for (int probe = 0; probe < 8; probe++) {
            int i = (idx + probe) & mask;
            long k = table[i];
            if (k == 0L) return false;
            if (k == key) return true;
        }
        return false;
    }

    /** Capacity. */
    public int capacity() { return table.length; }
}
