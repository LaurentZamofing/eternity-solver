package solver;

import java.util.SplittableRandom;

/**
 * Deterministic Zobrist hasher for partial-assignment states.
 *
 * <p>Each (cell, pieceId × 4 + rotation) pair is associated with a
 * random {@code long}. The hash of a partial assignment is the XOR of
 * all its placed pairs — commutative, so two search paths that reach
 * the same set of placements produce the same hash regardless of
 * order.</p>
 *
 * <p>XOR is self-inverse: applying the same key twice restores the
 * original hash. This makes per-move maintenance cheap: O(1) on
 * place/remove via {@code hash ^= keyOf(cell, piece, rot)}.</p>
 *
 * <p>Collision rate on 64-bit hashes at 2²² entries is ≈ 2⁻³² —
 * acceptable for nogood caching (false positives only cause us to
 * skip a branch that might have been valid, which is unsafe; false
 * negatives just re-explore). Callers that can't tolerate unsafe
 * skipping must store the full hash alongside a cache slot.</p>
 */
public final class ZobristHasher {

    private final int rows;
    private final int cols;
    private final int maxPidRot;
    private final long[][] table; // [cellIndex][pidRot]

    /** Builds a fresh table sized for a {@code rows × cols} board and a
     *  piece set going up to {@code maxPieceId}. Seeded deterministically
     *  so the same puzzle always hashes identically. */
    public ZobristHasher(int rows, int cols, int maxPieceId, long seed) {
        this.rows = rows;
        this.cols = cols;
        this.maxPidRot = (maxPieceId + 1) * 4; // pidRot = pid * 4 + rot; pid in [0..maxPieceId]
        int cells = rows * cols;
        this.table = new long[cells][maxPidRot];
        SplittableRandom rng = new SplittableRandom(seed);
        for (int c = 0; c < cells; c++) {
            for (int pr = 0; pr < maxPidRot; pr++) {
                // Avoid 0 — it's the empty-hash value and also the sentinel
                // used by NogoodCache for empty slots.
                long v;
                do { v = rng.nextLong(); } while (v == 0L);
                table[c][pr] = v;
            }
        }
    }

    /** Returns the key for a placement; XOR into/out of the state hash. */
    public long keyOf(int row, int col, int pieceId, int rotation) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return 0L;
        int pidRot = pieceId * 4 + (rotation & 3);
        if (pidRot < 0 || pidRot >= maxPidRot) return 0L;
        return table[row * cols + col][pidRot];
    }
}
