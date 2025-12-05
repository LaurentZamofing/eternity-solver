package solver;

import model.Board;
import model.Piece;
import model.Placement;
import solver.DomainManager.ValidPlacement;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/** Implements AC-3 constraint propagation for puzzle solving with incremental neighbor domain filtering, dead-end detection, and forward checking. */
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

    /** Creates propagator with domain manager and statistics tracker for AC-3 constraint propagation. */
    public ConstraintPropagator(DomainManager domainManager, Statistics stats) {
        this.domainManager = domainManager;
        this.stats = stats;
    }

    /** Enables or disables AC-3 constraint propagation. */
    public void setUseAC3(boolean enabled) {
        this.useAC3 = enabled;
    }

    /** Propagates AC-3 constraints after placement at (r,c) by filtering neighbor domains; returns false if dead-end detected (empty domain). */
    public boolean propagateAC3(Board board, int r, int c, int placedPieceId, int rotation,
                                Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        if (!useAC3 || !domainManager.isAC3Initialized()) return true;

        // AC-3 with cascading propagation
        // Queue of cells whose domains need to be updated
        Queue<int[]> queue = new java.util.LinkedList<>();
        Set<String> inQueue = new java.util.HashSet<>();

        // Format: {rowOffset, colOffset, placedSide, neighborSide}
        int[][] neighborOffsets = {{-1, 0, 0, 2}, {1, 0, 2, 0}, {0, -1, 3, 1}, {0, 1, 1, 3}};

        // Add ALL empty cells to the queue to propagate both:
        // 1. Edge constraints from adjacent placed pieces
        // 2. Piece availability (the placed piece is no longer available)
        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                if (board.isEmpty(row, col)) {
                    String key = row + "," + col;
                    if (!inQueue.contains(key)) {
                        queue.offer(new int[]{row, col});
                        inQueue.add(key);
                    }
                }
            }
        }

        // Propagate constraints in cascade
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int cellRow = cell[0];
            int cellCol = cell[1];
            inQueue.remove(cellRow + "," + cellCol);

            // Get current domain
            Map<Integer, List<ValidPlacement>> currentDomain = domainManager.getDomain(cellRow, cellCol);
            if (currentDomain == null || currentDomain.isEmpty()) {
                stats.incrementDeadEnds();
                return false;
            }

            // Compute new domain by checking all placed neighbors
            Map<Integer, List<ValidPlacement>> newDomain = new HashMap<>(currentDomain);
            boolean domainChanged = false;

            // Check each neighbor of this cell
            for (int[] offset : neighborOffsets) {
                int nbrRow = cellRow + offset[0];
                int nbrCol = cellCol + offset[1];
                int cellSide = offset[2];  // Which side of cell faces neighbor
                int nbrSide = offset[3];   // Which side of neighbor faces cell

                if (nbrRow < 0 || nbrRow >= board.getRows() || nbrCol < 0 || nbrCol >= board.getCols()) continue;
                if (board.isEmpty(nbrRow, nbrCol)) continue; // Empty neighbor, no constraint

                // Neighbor is occupied, get its edges
                Placement nbrPlacement = board.getPlacement(nbrRow, nbrCol);
                if (nbrPlacement == null) continue;

                int requiredEdge = nbrPlacement.edges[nbrSide];

                // Filter domain: keep only compatible placements
                Map<Integer, List<ValidPlacement>> filteredDomain = new HashMap<>();
                for (Map.Entry<Integer, List<ValidPlacement>> entry : newDomain.entrySet()) {
                    List<ValidPlacement> validRotations = new ArrayList<>();
                    for (ValidPlacement vp : entry.getValue()) {
                        int[] edges = piecesById.get(vp.pieceId).edgesRotated(vp.rotation);
                        if (edges[cellSide] == requiredEdge) {
                            validRotations.add(vp);
                        }
                    }
                    if (!validRotations.isEmpty()) {
                        filteredDomain.put(entry.getKey(), validRotations);
                    }
                }

                if (filteredDomain.size() < newDomain.size() ||
                    !filteredDomain.keySet().equals(newDomain.keySet())) {
                    domainChanged = true;
                }
                newDomain = filteredDomain;

                if (newDomain.isEmpty()) {
                    stats.incrementDeadEnds();
                    return false;
                }
            }

            // Check if pieces are still available
            Map<Integer, List<ValidPlacement>> availableDomain = new HashMap<>();
            for (Map.Entry<Integer, List<ValidPlacement>> entry : newDomain.entrySet()) {
                if (!pieceUsed.get(entry.getKey())) {
                    availableDomain.put(entry.getKey(), entry.getValue());
                }
            }

            if (availableDomain.isEmpty()) {
                stats.incrementDeadEnds();
                return false;
            }

            // If domain changed, update and add neighbors to queue for propagation
            if (domainChanged || availableDomain.size() < newDomain.size()) {
                domainManager.setDomain(cellRow, cellCol, availableDomain);

                // Add all empty neighbors to queue to propagate changes
                for (int[] offset : neighborOffsets) {
                    int nbrRow = cellRow + offset[0];
                    int nbrCol = cellCol + offset[1];

                    if (nbrRow >= 0 && nbrRow < board.getRows() &&
                        nbrCol >= 0 && nbrCol < board.getCols() &&
                        board.isEmpty(nbrRow, nbrCol)) {
                        String key = nbrRow + "," + nbrCol;
                        if (!inQueue.contains(key)) {
                            queue.offer(new int[]{nbrRow, nbrCol});
                            inQueue.add(key);
                        }
                    }
                }
            }
        }

        return true;
    }

    /** Filters domain to keep only placements matching edge constraint at edgeIndex (0=N, 1=E, 2=S, 3=W); more efficient than recomputing domain. */
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

    /** Checks if placement would cause dead-end without modifying domains (forward checking); returns true if any neighbor would have no valid placements. */
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

    /** Counts valid placements remaining for neighbor with edge constraint at neighborSide (0=N, 1=E, 2=S, 3=W); used by LCV heuristic to evaluate constraining effect. */
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

    /** Returns total number of valid placements in domain (including all rotations). */
    public int getDomainSize(Map<Integer, List<ValidPlacement>> domain) {
        if (domain == null) return 0;

        int size = 0;
        for (List<ValidPlacement> placements : domain.values()) {
            size += placements.size();
        }
        return size;
    }

    /** Returns number of unique pieces in domain (ignoring different rotations). */
    public int getUniquePieceCount(Map<Integer, List<ValidPlacement>> domain) {
        if (domain == null) return 0;
        return domain.size();
    }

    /** Returns true if domain is empty or null (no valid placements). */
    public boolean isDomainEmpty(Map<Integer, List<ValidPlacement>> domain) {
        return domain == null || domain.isEmpty();
    }

    /** Returns true if domain contains only one piece ID (singleton); piece may have multiple valid rotations. */
    public boolean isSingleton(Map<Integer, List<ValidPlacement>> domain) {
        return domain != null && domain.size() == 1;
    }

    /** Returns singleton piece ID if domain contains only one piece, -1 otherwise. */
    public int getSingletonPieceId(Map<Integer, List<ValidPlacement>> domain) {
        if (!isSingleton(domain)) return -1;
        return domain.keySet().iterator().next();
    }
}
