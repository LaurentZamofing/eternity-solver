package solver.heuristics;

import model.Piece;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Implements LCV (Least Constraining Value) heuristic by computing piece difficulty scores based on edge compatibility; lower score = more constrained = try first (fail-fast). */
public class LeastConstrainingValueOrderer {

    // Edge compatibility tables for fast piece ordering
    private Map<Integer, Set<Integer>> northEdgeCompatible;  // edgeValue -> piece IDs
    private Map<Integer, Set<Integer>> eastEdgeCompatible;
    private Map<Integer, Set<Integer>> southEdgeCompatible;
    private Map<Integer, Set<Integer>> westEdgeCompatible;

    // Piece difficulty scores: pieceId -> difficulty
    // Lower score = more constrained = harder to place
    private Map<Integer, Integer> pieceDifficultyScores;

    private final boolean verbose;

    /** Creates LCV orderer with verbose flag for detailed output. */
    public LeastConstrainingValueOrderer(boolean verbose) {
        this.verbose = verbose;
        this.pieceDifficultyScores = new HashMap<>();
    }

    /** Builds edge compatibility tables mapping edge values to piece IDs that can provide that edge (in any rotation); enables fast piece filtering. */
    public void buildEdgeCompatibilityTables(Map<Integer, Piece> pieces) {
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

    /** Computes difficulty scores for all pieces based on edge pattern constraints; lower score = more constrained = harder to place (lightweight approximation). */
    public void computePieceDifficulty(Map<Integer, Piece> pieces) {
        pieceDifficultyScores = new HashMap<>();

        for (Map.Entry<Integer, Piece> entry : pieces.entrySet()) {
            int pieceId = entry.getKey();
            Piece piece = entry.getValue();
            int[] edges = piece.getEdges();

            // For each edge, count how many pieces could potentially match
            int totalCompatibility = 0;
            for (int i = 0; i < 4; i++) {
                int edgeValue = edges[i];
                // Count pieces that have this edge value in any rotation
                Set<Integer> compatiblePieces = new HashSet<>();
                for (int rot = 0; rot < 4; rot++) {
                    int[] rotatedEdges = piece.edgesRotated(rot);
                    // Check all compatibility tables
                    if (northEdgeCompatible.containsKey(edgeValue)) {
                        compatiblePieces.addAll(northEdgeCompatible.get(edgeValue));
                    }
                    if (eastEdgeCompatible.containsKey(edgeValue)) {
                        compatiblePieces.addAll(eastEdgeCompatible.get(edgeValue));
                    }
                    if (southEdgeCompatible.containsKey(edgeValue)) {
                        compatiblePieces.addAll(southEdgeCompatible.get(edgeValue));
                    }
                    if (westEdgeCompatible.containsKey(edgeValue)) {
                        compatiblePieces.addAll(westEdgeCompatible.get(edgeValue));
                    }
                }
                totalCompatibility += compatiblePieces.size();
            }

            // Lower score = fewer compatible pieces = harder to place = should try first
            pieceDifficultyScores.put(pieceId, totalCompatibility);
        }

        if (verbose) {
            System.out.println("✓ Piece difficulty scores computed");
        }
    }

    /** Returns difficulty score for piece (lower = more constrained); returns Integer.MAX_VALUE if not found. */
    public int getDifficultyScore(int pieceId) {
        return pieceDifficultyScores.getOrDefault(pieceId, Integer.MAX_VALUE);
    }

    /** Returns copy of all difficulty scores (piece ID → difficulty score). */
    public Map<Integer, Integer> getAllDifficultyScores() {
        return new HashMap<>(pieceDifficultyScores);
    }

    /** Returns piece IDs that can provide specified edge value on north side (in any rotation). */
    public Set<Integer> getNorthCompatiblePieces(int edgeValue) {
        return northEdgeCompatible.getOrDefault(edgeValue, new HashSet<>());
    }

    /** Returns piece IDs that can provide specified edge value on east side (in any rotation). */
    public Set<Integer> getEastCompatiblePieces(int edgeValue) {
        return eastEdgeCompatible.getOrDefault(edgeValue, new HashSet<>());
    }

    /** Returns piece IDs that can provide specified edge value on south side (in any rotation). */
    public Set<Integer> getSouthCompatiblePieces(int edgeValue) {
        return southEdgeCompatible.getOrDefault(edgeValue, new HashSet<>());
    }

    /** Returns piece IDs that can provide specified edge value on west side (in any rotation). */
    public Set<Integer> getWestCompatiblePieces(int edgeValue) {
        return westEdgeCompatible.getOrDefault(edgeValue, new HashSet<>());
    }

    /** Returns candidate pieces matching all specified edge constraints (use -1 for no constraint); intersects compatible pieces from edge tables for fast filtering. */
    public Set<Integer> getCandidatePieces(int northEdge, int eastEdge, int southEdge, int westEdge) {
        Set<Integer> candidates = null;

        // Build constraint set by intersecting compatible pieces for each edge
        if (northEdge >= 0) {
            candidates = new HashSet<>(getNorthCompatiblePieces(northEdge));
        }
        if (eastEdge >= 0) {
            Set<Integer> eastCandidates = getEastCompatiblePieces(eastEdge);
            if (candidates == null) {
                candidates = new HashSet<>(eastCandidates);
            } else {
                candidates.retainAll(eastCandidates);
            }
        }
        if (southEdge >= 0) {
            Set<Integer> southCandidates = getSouthCompatiblePieces(southEdge);
            if (candidates == null) {
                candidates = new HashSet<>(southCandidates);
            } else {
                candidates.retainAll(southCandidates);
            }
        }
        if (westEdge >= 0) {
            Set<Integer> westCandidates = getWestCompatiblePieces(westEdge);
            if (candidates == null) {
                candidates = new HashSet<>(westCandidates);
            } else {
                candidates.retainAll(westCandidates);
            }
        }

        return (candidates != null) ? candidates : new HashSet<>();
    }

    /** Returns true if edge compatibility tables have been built. */
    public boolean isInitialized() {
        return northEdgeCompatible != null && pieceDifficultyScores != null;
    }

    /** Clears all internal data structures. */
    public void clear() {
        if (northEdgeCompatible != null) northEdgeCompatible.clear();
        if (eastEdgeCompatible != null) eastEdgeCompatible.clear();
        if (southEdgeCompatible != null) southEdgeCompatible.clear();
        if (westEdgeCompatible != null) westEdgeCompatible.clear();
        if (pieceDifficultyScores != null) pieceDifficultyScores.clear();
    }

    /** Returns statistics string describing edge compatibility tables (unique edge values, average pieces per edge). */
    public String getStatistics() {
        if (!isInitialized()) {
            return "Edge compatibility tables not initialized";
        }

        int uniqueEdgeValues = northEdgeCompatible.size();
        int avgPiecesPerEdge = 0;
        if (uniqueEdgeValues > 0) {
            int totalPieces = 0;
            for (Set<Integer> pieces : northEdgeCompatible.values()) {
                totalPieces += pieces.size();
            }
            avgPiecesPerEdge = totalPieces / uniqueEdgeValues;
        }

        return String.format("Edge compatibility: %d unique edge values, ~%d pieces/edge average",
                           uniqueEdgeValues, avgPiecesPerEdge);
    }
}
