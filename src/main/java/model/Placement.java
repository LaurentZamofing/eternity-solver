package model;

import java.util.Arrays;

/**
 * Represents the placement of a piece on the grid with its rotation.
 * Stores pre-calculated edges after rotation for performance reasons.
 */
public class Placement {
    private final int pieceId;
    private final int rotation; // 0..3
    public final int[] edges;  // edges after rotation (direct access for performance)

    /**
     * Constructor
     * @param pieceId identifier of the placed piece
     * @param rotation rotation applied (0-3, reduced modulo 4)
     * @param edges edges after rotation [N, E, S, W]
     */
    public Placement(int pieceId, int rotation, int[] edges) {
        this.pieceId = pieceId;
        this.rotation = rotation % 4;
        this.edges = Arrays.copyOf(edges, 4);
    }

    /**
     * Returns the piece identifier.
     */
    public int getPieceId() {
        return pieceId;
    }

    /**
     * Returns the rotation applied (0-3).
     */
    public int getRotation() {
        return rotation;
    }

    @Override
    public String toString() {
        return String.format("%02d r%d", pieceId, rotation);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Placement other = (Placement) obj;
        return pieceId == other.pieceId
            && rotation == other.rotation
            && Arrays.equals(edges, other.edges);
    }

    @Override
    public int hashCode() {
        int result = pieceId;
        result = 31 * result + rotation;
        result = 31 * result + Arrays.hashCode(edges);
        return result;
    }
}
