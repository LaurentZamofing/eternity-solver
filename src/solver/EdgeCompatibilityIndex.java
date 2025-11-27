package solver;

import model.Piece;

import java.util.*;

/**
 * Precomputed index for fast edge compatibility lookups.
 * Maps edge values to sets of piece IDs that can provide that edge.
 *
 * This immutable class builds lookup tables that allow O(1) checking
 * of which pieces can potentially match a given edge value, dramatically
 * speeding up constraint satisfaction checks during puzzle solving.
 */
public class EdgeCompatibilityIndex {

    private final Map<Integer, Set<Integer>> northEdgeCompatible;
    private final Map<Integer, Set<Integer>> eastEdgeCompatible;
    private final Map<Integer, Set<Integer>> southEdgeCompatible;
    private final Map<Integer, Set<Integer>> westEdgeCompatible;

    /**
     * Build compatibility tables for the given pieces.
     * For each edge value, records which piece IDs can provide that edge
     * (considering all 4 possible rotations).
     *
     * @param pieces map of piece ID to Piece objects
     * @param verbose if true, print debug information about table construction
     */
    public EdgeCompatibilityIndex(Map<Integer, Piece> pieces, boolean verbose) {
        northEdgeCompatible = new HashMap<>();
        eastEdgeCompatible = new HashMap<>();
        southEdgeCompatible = new HashMap<>();
        westEdgeCompatible = new HashMap<>();

        // For each piece, for each rotation, record which edges it can provide
        for (Map.Entry<Integer, Piece> entry : pieces.entrySet()) {
            int pieceId = entry.getKey();
            Piece piece = entry.getValue();

            // Try all 4 rotations
            for (int rot = 0; rot < 4; rot++) {
                int[] edges = piece.edgesRotated(rot);

                // This piece (at this rotation) provides these edge values:
                northEdgeCompatible.computeIfAbsent(edges[0], k -> new HashSet<>()).add(pieceId);
                eastEdgeCompatible.computeIfAbsent(edges[1], k -> new HashSet<>()).add(pieceId);
                southEdgeCompatible.computeIfAbsent(edges[2], k -> new HashSet<>()).add(pieceId);
                westEdgeCompatible.computeIfAbsent(edges[3], k -> new HashSet<>()).add(pieceId);
            }
        }

        if (verbose) {
            System.out.println("✓ Edge compatibility tables built:");
            System.out.println("  - Unique edge values: ~" + northEdgeCompatible.size());
        }
    }

    /**
     * Convenience constructor with verbose=false.
     */
    public EdgeCompatibilityIndex(Map<Integer, Piece> pieces) {
        this(pieces, false);
    }

    /**
     * Get piece IDs that can provide a given north edge value.
     * @param edgeValue the edge value to look up
     * @return set of piece IDs, or empty set if none match
     */
    public Set<Integer> getNorthCompatible(int edgeValue) {
        return northEdgeCompatible.getOrDefault(edgeValue, Collections.emptySet());
    }

    /**
     * Get piece IDs that can provide a given east edge value.
     * @param edgeValue the edge value to look up
     * @return set of piece IDs, or empty set if none match
     */
    public Set<Integer> getEastCompatible(int edgeValue) {
        return eastEdgeCompatible.getOrDefault(edgeValue, Collections.emptySet());
    }

    /**
     * Get piece IDs that can provide a given south edge value.
     * @param edgeValue the edge value to look up
     * @return set of piece IDs, or empty set if none match
     */
    public Set<Integer> getSouthCompatible(int edgeValue) {
        return southEdgeCompatible.getOrDefault(edgeValue, Collections.emptySet());
    }

    /**
     * Get piece IDs that can provide a given west edge value.
     * @param edgeValue the edge value to look up
     * @return set of piece IDs, or empty set if none match
     */
    public Set<Integer> getWestCompatible(int edgeValue) {
        return westEdgeCompatible.getOrDefault(edgeValue, Collections.emptySet());
    }

    /**
     * Get the number of unique edge values across all pieces.
     * @return count of distinct edge values
     */
    public int getUniqueEdgeValueCount() {
        return northEdgeCompatible.size();
    }
}
