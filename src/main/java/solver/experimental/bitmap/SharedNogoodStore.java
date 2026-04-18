package solver.experimental.bitmap;

/**
 * Sharded, thread-safe {@link NogoodStore} — one {@link NogoodCache} per
 * shard, synchronised independently. Designed for portfolio parallel
 * solvers where all workers share discovered dead-ends.
 *
 * <p>Shard selection uses the top bits of the 64-bit key (so workers
 * converging on similar hashes still spread across shards). Contention
 * per shard stays low because writes are rare (only on dead-end at
 * cell exhaustion) and reads are per-descend but each goes to the
 * picked shard — not a shared global structure.</p>
 */
public final class SharedNogoodStore implements NogoodStore {

    private final NogoodCache[] shards;
    private final int shardMask;

    /**
     * @param totalBits log2 of total capacity (e.g. 22 → 4 M slots).
     *                  Split across {@code 1 << shardBits} shards.
     * @param shardBits log2 of shard count (e.g. 5 → 32 shards).
     */
    public SharedNogoodStore(int totalBits, int shardBits) {
        if (shardBits < 0 || shardBits > 10) throw new IllegalArgumentException("shardBits out of range: " + shardBits);
        int nShards = 1 << shardBits;
        int perShardBits = Math.max(4, totalBits - shardBits);
        this.shards = new NogoodCache[nShards];
        for (int i = 0; i < nShards; i++) shards[i] = new NogoodCache(perShardBits);
        this.shardMask = nShards - 1;
    }

    /** Default 2²² total capacity, 2⁵ = 32 shards. */
    public SharedNogoodStore() {
        this(22, 5);
    }

    @Override
    public void add(long key) {
        int idx = shardIndex(key);
        NogoodCache shard = shards[idx];
        synchronized (shard) {
            shard.add(key);
        }
    }

    @Override
    public boolean contains(long key) {
        int idx = shardIndex(key);
        NogoodCache shard = shards[idx];
        synchronized (shard) {
            return shard.contains(key);
        }
    }

    private int shardIndex(long key) {
        // Use the high bits — key low bits are used inside each shard for
        // bucket selection, so keeping the high bits for sharding decorrelates
        // the two levels (no clustering).
        return (int) (key >>> (64 - Integer.numberOfTrailingZeros(shards.length))) & shardMask;
    }

    /** Number of shards. */
    public int shardCount() { return shards.length; }
}
