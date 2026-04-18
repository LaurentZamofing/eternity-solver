package solver.experimental.bitmap;

/**
 * Minimal API for a nogood store — decouples the search engine from whether
 * it's using a thread-local {@link NogoodCache} or a shared, sharded
 * {@link SharedNogoodStore} instance.
 */
public interface NogoodStore {

    /** Inserts the key; idempotent. Implementations are free to evict older
     *  entries on capacity pressure (false negatives are acceptable — they
     *  only cause a dead branch to be re-explored). */
    void add(long key);

    /** Returns {@code true} iff the key has been inserted. False negatives
     *  acceptable; false positives must not occur (keys are 64-bit hashes so
     *  the intrinsic collision rate is ~2⁻³² at 2²² entries). */
    boolean contains(long key);
}
