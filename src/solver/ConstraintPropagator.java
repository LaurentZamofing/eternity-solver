package solver;

import model.Board;
import model.Piece;
import solver.DomainManager.ValidPlacement;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ConstraintPropagator handles AC-3 constraint propagation for the Eternity solver.
 *
 * This class implements the AC-3 (Arc Consistency 3) algorithm to maintain arc consistency
 * in the constraint satisfaction problem. When a piece is placed, it propagates constraints
 * to neighboring cells, removing incompatible placements from their domains.
 *
 * Key features:
 * - Incremental constraint propagation after each placement
 * - Early detection of dead-ends (empty domains)
 * - Efficient filtering of domains based on edge compatibility
 *
 * @author Eternity Solver Team
 */
public class ConstraintPropagator {

    // Statistics tracking
    public static class Statistics {
        public long deadEndsDetected = 0;

        public void incrementDeadEnds() {
            deadEndsDetected++;
        }

        public long getDeadEndsDetected() {
            return deadEndsDetected;
        }
    }

    private final DomainManager domainManager;
    private final Statistics stats;
    private boolean useAC3 = true;

    /**
     * Constructor for ConstraintPropagator.
     *
     * @param domainManager the domain manager to work with
     * @param stats statistics tracker
     */
    public ConstraintPropagator(DomainManager domainManager, Statistics stats) {
        this.domainManager = domainManager;
        this.stats = stats;
    }

    /**
     * Enable or disable AC-3 constraint propagation.
     *
     * @param enabled true to enable AC-3, false to disable
     */
    public void setUseAC3(boolean enabled) {
        this.useAC3 = enabled;
    }

    /**
     * Propagate constraints using AC-3 after placing a piece at (r,c).
     * This incrementally updates neighbor domains by removing incompatible placements.
     * Returns false if any domain becomes empty (dead end detected).
     *
     * @param board current board state
     * @param r row of placed piece
     * @param c column of placed piece
     * @param placedPieceId ID of the piece just placed
     * @param rotation rotation of the placed piece
     * @param piecesById map of all pieces
     * @param pieceUsed array tracking used pieces
     * @param totalPieces total number of pieces
     * @return false if a dead end is detected (empty domain), true otherwise
     */
    public boolean propagateAC3(Board board, int r, int c, int placedPieceId, int rotation,
                                Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        if (!useAC3 || !domainManager.isAC3Initialized()) return true;

        Piece placedPiece = piecesById.get(placedPieceId);
        int[] placedEdges = placedPiece.edgesRotated(rotation);

        // Check and update domain of each neighbor
        // Format: {neighborRow, neighborCol, placedSide, neighborSide}
        int[][] neighbors = {{r-1, c, 0, 2}, {r+1, c, 2, 0}, {r, c-1, 3, 1}, {r, c+1, 1, 3}};

        for (int[] nbr : neighbors) {
            int nr = nbr[0], nc = nbr[1];
            int placedSide = nbr[2];  // Which side of placed piece faces neighbor
            int nbrSide = nbr[3];      // Which side of neighbor faces placed piece

            if (nr < 0 || nr >= board.getRows() || nc < 0 || nc >= board.getCols()) continue;
            if (!board.isEmpty(nr, nc)) continue;

            Map<Integer, List<ValidPlacement>> currentDomain = domainManager.getDomain(nr, nc);
            if (currentDomain == null) continue;

            int requiredEdge = placedEdges[placedSide];

            // Remove incompatible placements from neighbor's domain
            Map<Integer, List<ValidPlacement>> newDomain = new HashMap<>();
            for (Map.Entry<Integer, List<ValidPlacement>> entry : currentDomain.entrySet()) {
                List<ValidPlacement> validRotations = new ArrayList<>();
                for (ValidPlacement vp : entry.getValue()) {
                    int[] edges = piecesById.get(vp.pieceId).edgesRotated(vp.rotation);
                    if (edges[nbrSide] == requiredEdge) {
                        validRotations.add(vp);
                    }
                }
                if (!validRotations.isEmpty()) {
                    newDomain.put(entry.getKey(), validRotations);
                }
            }

            // Update domain
            domainManager.setDomain(nr, nc, newDomain);

            // Dead end if domain is empty
            if (newDomain.isEmpty()) {
                stats.incrementDeadEnds();
                return false;
            }
        }

        return true;
    }

    /**
     * Filter an existing domain to keep only placements compatible with a neighbor constraint.
     * This is more efficient than recomputing the entire domain from scratch.
     *
     * @param currentDomain existing domain to filter
     * @param requiredEdge the edge value that must match
     * @param edgeIndex which edge of the piece must match (0=N, 1=E, 2=S, 3=W)
     * @param piecesById map of all pieces
     * @return filtered list of valid placements
     */
    public List<ValidPlacement> filterDomain(List<ValidPlacement> currentDomain, int requiredEdge,
                                             int edgeIndex, Map<Integer, Piece> piecesById) {
        if (currentDomain == null || currentDomain.isEmpty()) return new ArrayList<>();

        List<ValidPlacement> filtered = new ArrayList<>();
        for (ValidPlacement vp : currentDomain) {
            int[] edges = piecesById.get(vp.pieceId).edgesRotated(vp.rotation);
            if (edges[edgeIndex] == requiredEdge) {
                filtered.add(vp);
            }
        }
        return filtered;
    }

    /**
     * Check if propagation would lead to a dead-end without actually modifying domains.
     * This is useful for forward checking before committing to a placement.
     *
     * @param board current board state
     * @param r row of potential placement
     * @param c column of potential placement
     * @param candidateEdges edges of the candidate piece
     * @param piecesById map of all pieces
     * @return true if placement would cause a dead-end, false otherwise
     */
    public boolean wouldCauseDeadEnd(Board board, int r, int c, int[] candidateEdges,
                                     Map<Integer, Piece> piecesById) {
        if (!useAC3 || !domainManager.isAC3Initialized()) return false;

        // Check each neighbor
        int[][] neighbors = {{r-1, c, 0, 2}, {r+1, c, 2, 0}, {r, c-1, 3, 1}, {r, c+1, 1, 3}};

        for (int[] nbr : neighbors) {
            int nr = nbr[0], nc = nbr[1];
            int candidateSide = nbr[2];  // Which side of candidate faces neighbor
            int nbrSide = nbr[3];         // Which side of neighbor faces candidate

            if (nr < 0 || nr >= board.getRows() || nc < 0 || nc >= board.getCols()) continue;
            if (!board.isEmpty(nr, nc)) continue;

            Map<Integer, List<ValidPlacement>> currentDomain = domainManager.getDomain(nr, nc);
            if (currentDomain == null) continue;

            int requiredEdge = candidateEdges[candidateSide];

            // Check if any placements would remain valid
            boolean hasValidPlacement = false;
            for (List<ValidPlacement> placements : currentDomain.values()) {
                for (ValidPlacement vp : placements) {
                    int[] edges = piecesById.get(vp.pieceId).edgesRotated(vp.rotation);
                    if (edges[nbrSide] == requiredEdge) {
                        hasValidPlacement = true;
                        break;
                    }
                }
                if (hasValidPlacement) break;
            }

            // If no valid placements remain for this neighbor, it's a dead-end
            if (!hasValidPlacement) {
                return true;
            }
        }

        return false;
    }

    /**
     * Count how many valid placements would remain for a neighbor after a placement.
     * Useful for heuristics to evaluate the "constraining" effect of a placement.
     *
     * @param board current board state
     * @param neighborRow row of the neighbor cell
     * @param neighborCol column of the neighbor cell
     * @param requiredEdge the edge value the neighbor must match
     * @param neighborSide which side of the neighbor must match (0=N, 1=E, 2=S, 3=W)
     * @param piecesById map of all pieces
     * @return number of valid placements that would remain
     */
    public int countRemainingPlacements(Board board, int neighborRow, int neighborCol,
                                        int requiredEdge, int neighborSide,
                                        Map<Integer, Piece> piecesById) {
        if (!useAC3 || !domainManager.isAC3Initialized()) return 0;

        Map<Integer, List<ValidPlacement>> domain = domainManager.getDomain(neighborRow, neighborCol);
        if (domain == null) return 0;

        int count = 0;
        for (List<ValidPlacement> placements : domain.values()) {
            for (ValidPlacement vp : placements) {
                int[] edges = piecesById.get(vp.pieceId).edgesRotated(vp.rotation);
                if (edges[neighborSide] == requiredEdge) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Get the total number of valid placements in a domain.
     *
     * @param domain the domain to count
     * @return total number of valid placements
     */
    public int getDomainSize(Map<Integer, List<ValidPlacement>> domain) {
        if (domain == null) return 0;

        int size = 0;
        for (List<ValidPlacement> placements : domain.values()) {
            size += placements.size();
        }
        return size;
    }

    /**
     * Get the number of unique pieces in a domain (ignoring different rotations).
     *
     * @param domain the domain to count
     * @return number of unique pieces
     */
    public int getUniquePieceCount(Map<Integer, List<ValidPlacement>> domain) {
        if (domain == null) return 0;
        return domain.size();
    }

    /**
     * Check if a domain is empty (no valid placements).
     *
     * @param domain the domain to check
     * @return true if domain is empty or null
     */
    public boolean isDomainEmpty(Map<Integer, List<ValidPlacement>> domain) {
        return domain == null || domain.isEmpty();
    }

    /**
     * Check if a domain has only one valid piece (singleton).
     * Note: The piece may have multiple valid rotations.
     *
     * @param domain the domain to check
     * @return true if domain contains only one piece ID
     */
    public boolean isSingleton(Map<Integer, List<ValidPlacement>> domain) {
        return domain != null && domain.size() == 1;
    }

    /**
     * Get the singleton piece ID if the domain has only one piece.
     *
     * @param domain the domain to check
     * @return the piece ID if singleton, -1 otherwise
     */
    public int getSingletonPieceId(Map<Integer, List<ValidPlacement>> domain) {
        if (!isSingleton(domain)) return -1;
        return domain.keySet().iterator().next();
    }
}
