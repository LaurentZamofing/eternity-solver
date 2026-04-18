package solver.experimental.bitmap;

/**
 * Generates the Luby restart sequence: 1, 1, 2, 1, 1, 2, 4, 1, 1, 2, 1, 1, 2, 4, 8, …
 *
 * <p>This is the restart schedule used by modern SAT/CP solvers. It
 * guarantees the expected running time is within a log factor of the
 * optimal restart strategy without knowing the problem distribution.</p>
 *
 * <p>The sequence is scaled by a multiplier (commonly called the "unit")
 * and counted in decisions or dead-ends. Once the count exceeds the
 * current Luby term × unit, the solver restarts from depth 0 with a new
 * random seed for the MRV tiebreaker.</p>
 */
public final class LubyRestart {

    private int k = 1;  // current power-of-two bound
    private int u = 1;  // current term index (1-based)

    /**
     * Returns the next Luby number in the sequence.
     *
     * <p>Reference: Luby, Sinclair, Zuckerman, 1993,
     * "Optimal speedup of Las Vegas algorithms".</p>
     */
    public int next() {
        int v;
        if ((u & -u) == u) {
            // u is a power of 2 → v = u
            v = u;
        } else {
            // v = Luby(u - 2^(k-1)) where k was such that 2^(k-1) ≤ u < 2^k
            // Simple computation via a method call below; for the common case
            // we inline.
            v = lubyValue(u);
        }
        u++;
        return v;
    }

    /** Classical recursive Luby definition; O(log n) per call. */
    private static int lubyValue(int i) {
        int k = 1;
        while ((1 << k) - 1 < i) k++;
        if (i == (1 << k) - 1) return 1 << (k - 1);
        return lubyValue(i - (1 << (k - 1)) + 1);
    }

    /** Resets the sequence to the beginning. */
    public void reset() {
        k = 1;
        u = 1;
    }
}
