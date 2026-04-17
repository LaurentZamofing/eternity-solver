package model;

import java.util.Arrays;

/**
 * Represents a puzzle piece with its edges.
 * Pieces are immutable - rotations generate new arrays.
 */
public class Piece {
    private final int id;            // unique piece identifier
    private final int[] edges;       // edges in order [N, E, S, W]
    // Cached rotations [0=0°, 1=90°, 2=180°, 3=270°]. Returned directly; callers must not mutate.
    private final int[][] rotations;

    /**
     * Constructor
     * @param id unique piece identifier
     * @param edges integer array of length 4 representing [N, E, S, W]
     * @throws IllegalArgumentException if edges doesn't have exactly 4 elements
     */
    public Piece(int id, int[] edges) {
        if (edges.length != 4) {
            throw new IllegalArgumentException("edges must have length 4");
        }
        this.id = id;
        this.edges = Arrays.copyOf(edges, 4);
        int n = this.edges[0], e = this.edges[1], s = this.edges[2], w = this.edges[3];
        this.rotations = new int[][] {
            this.edges,                  // 0°   [N, E, S, W]
            new int[] {w, n, e, s},      // 90°  cw: N<-W, E<-N, S<-E, W<-S
            new int[] {s, w, n, e},      // 180°     N<-S, E<-W, S<-N, W<-E
            new int[] {e, s, w, n},      // 270° cw: N<-E, E<-S, S<-W, W<-N
        };
    }

    /**
     * Returns the piece identifier.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns a defensive copy of the piece's edges.
     * The copy guarantees piece immutability.
     */
    public int[] getEdges() {
        return Arrays.copyOf(edges, 4);
    }

    /**
     * Returns the cached edge array for clockwise k*90° rotation.
     * k is reduced modulo 4. The returned array is the internal cached
     * rotation — callers MUST treat it as read-only; mutation corrupts
     * all subsequent fit/AC-3 checks.
     *
     * Mapping (90° cw): newN = W, newE = N, newS = E, newW = S
     *
     * @param k number of 90° clockwise rotations
     * @return cached edge array after rotation (do not mutate)
     */
    public int[] edgesRotated(int k) {
        return rotations[((k % 4) + 4) % 4];
    }

    /**
     * Returns true if this piece has at least one rotation geometrically
     * able to fit at the top-left corner (N=0 AND W=0 on that rotation).
     *
     * <p>Shortcut for the common "TL fittable" predicate repeated across
     * SymmetryBreakingManager, DomainManager and tests.</p>
     */
    public boolean isTopLeftFittableInAnyRotation() {
        int maxRot = getUniqueRotationCount();
        for (int k = 0; k < maxRot; k++) {
            int[] e = rotations[k];
            if (e[0] == 0 && e[3] == 0) return true;
        }
        return false;
    }

    /**
     * Returns true if rotating this piece by {@code k} produces a North/West
     * pair of zero edges (i.e. fits at (0,0) without violating the borders).
     */
    public boolean isTopLeftFittableAt(int k) {
        int[] e = rotations[((k % 4) + 4) % 4];
        return e[0] == 0 && e[3] == 0;
    }

    /**
     * Returns the number of unique rotations for this piece.
     * A piece can have 1, 2, or 4 unique rotations depending on its symmetry.
     *
     * @return number of distinct rotations (1, 2, or 4)
     */
    public int getUniqueRotationCount() {
        int n = edges[0], e = edges[1], s = edges[2], w = edges[3];

        // Case 1: All edges identical (4-fold symmetry) -> 1 unique rotation
        if (n == e && e == s && s == w) {
            return 1;
        }

        // Case 2: 2-fold symmetry (opposite sides identical) -> 2 unique rotations
        if (n == s && e == w) {
            return 2;
        }

        // Case 3: No symmetry -> 4 unique rotations
        return 4;
    }

    @Override
    public String toString() {
        return "Piece(id=" + id + ", edges=" + Arrays.toString(edges) + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Piece other = (Piece) obj;
        return id == other.id && Arrays.equals(edges, other.edges);
    }

    @Override
    public int hashCode() {
        return 31 * id + Arrays.hashCode(edges);
    }
}
