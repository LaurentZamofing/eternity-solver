package model;

/**
 * Edge directions of a puzzle cell.
 *
 * <p>The ordinal values are load-bearing: they must match the legacy
 * index convention used by {@link Piece#edgesRotated(int)} and
 * {@link Placement#edges}, where index 0=N, 1=E, 2=S, 3=W. Do not
 * reorder the constants.</p>
 *
 * <p>Example:</p>
 * <pre>
 *   int[] edges = placement.edges;
 *   int north = edges[EdgeDirection.NORTH.index];
 *   int matchingSide = edges[EdgeDirection.NORTH.opposite().index];
 * </pre>
 */
public enum EdgeDirection {
    NORTH(0),
    EAST(1),
    SOUTH(2),
    WEST(3);

    /** Array index corresponding to this direction — equal to {@link #ordinal()} but self-documenting. */
    public final int index;

    EdgeDirection(int index) {
        this.index = index;
    }

    /** The opposite direction (N↔S, E↔W). */
    public EdgeDirection opposite() {
        return VALUES[(index + 2) & 3];
    }

    /** Rotated 90° clockwise (N→E→S→W→N). */
    public EdgeDirection rotateClockwise() {
        return VALUES[(index + 1) & 3];
    }

    /** Returns the direction for a given index 0..3; throws if out of range. */
    public static EdgeDirection of(int index) {
        if (index < 0 || index >= 4) {
            throw new IllegalArgumentException("EdgeDirection index must be in [0,3]: " + index);
        }
        return VALUES[index];
    }

    // Cached values() to avoid per-call array clone on the hot path.
    private static final EdgeDirection[] VALUES = values();
}
