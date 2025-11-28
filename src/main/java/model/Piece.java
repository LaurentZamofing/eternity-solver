package model;

import java.util.Arrays;

/**
 * Represents a puzzle piece with its edges.
 * Pieces are immutable - rotations generate new arrays.
 */
public class Piece {
    private final int id;            // unique piece identifier
    private final int[] edges;       // edges in order [N, E, S, W]

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
     * Returns a new array representing the edges after clockwise k*90° rotation.
     * k is reduced modulo 4.
     * Mapping (90° cw): newN = W, newE = N, newS = E, newW = S
     *
     * @param k number of 90° clockwise rotations
     * @return new edge array after rotation (or internal array if k=0)
     */
    public int[] edgesRotated(int k) {
        k = ((k % 4) + 4) % 4;
        if (k == 0) return Arrays.copyOf(edges, 4);  // Return defensive copy for immutability

        // Optimization: direct rotation without loop
        int n = edges[0], e = edges[1], s = edges[2], w = edges[3];
        switch (k) {
            case 1:  // 90° clockwise: N<-W, E<-N, S<-E, W<-S
                return new int[]{w, n, e, s};
            case 2:  // 180°: N<-S, E<-W, S<-N, W<-E
                return new int[]{s, w, n, e};
            case 3:  // 270° clockwise: N<-E, E<-S, S<-W, W<-N
                return new int[]{e, s, w, n};
            default:
                return Arrays.copyOf(edges, 4);  // Should never happen, but return copy
        }
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
