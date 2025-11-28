package solver;

import model.Board;
import model.Piece;

import java.util.*;

/** Optimizes piece ordering and selection using edge compatibility, LCV heuristic, difficulty ordering, and corner piece selection for efficient puzzle solving. */
public class PieceOrderingOptimizer {

    private final EdgeCompatibilityIndex edgeIndex;
    private final PlacementValidator validator;
    private final NeighborAnalyzer neighborAnalyzer;

    /** Helper class storing piece ID and score for sorting. */
    public static class PieceScore {
        public final int pieceId;
        public final int score;

        public PieceScore(int pieceId, int score) {
            this.pieceId = pieceId;
            this.score = score;
        }
    }

    /** Creates optimizer with edge compatibility index, placement validator, and neighbor analyzer for constraint calculations. */
    public PieceOrderingOptimizer(EdgeCompatibilityIndex edgeIndex,
                                  PlacementValidator validator,
                                  NeighborAnalyzer neighborAnalyzer) {
        this.edgeIndex = edgeIndex;
        this.validator = validator;
        this.neighborAnalyzer = neighborAnalyzer;
    }

    /** Finds all valid placements (piece, rotation) for cell using edge compatibility index for early rejection. */
    public List<DomainManager.ValidPlacement> getValidPlacements(
            Board board, int r, int c,
            Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {

        List<DomainManager.ValidPlacement> validPlacements = new ArrayList<>();

        // Try all available pieces
        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed.get(pid)) continue;

            Piece piece = pieces.get(pid);
            if (piece == null) continue;

            int maxRotations = piece.getUniqueRotationCount();
            for (int rot = 0; rot < maxRotations; rot++) {
                int[] edges = piece.edgesRotated(rot);
                if (validator.fits(board, r, c, edges)) {
                    validPlacements.add(new DomainManager.ValidPlacement(pid, rot));
                }
            }
        }

        return validPlacements;
    }

    /** Counts valid placements for cell; wrapper around getValidPlacements() returning only count. */
    public int countValidPlacements(Board board, int r, int c,
                                   Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {
        return getValidPlacements(board, r, c, pieces, pieceUsed, totalPieces).size();
    }

    /** Counts unique pieces that can be placed at position (ignoring rotations). */
    public int countUniquePieces(Board board, int r, int c,
                                Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {
        Set<Integer> uniquePieces = new HashSet<>();

        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed.get(pid)) continue;

            Piece piece = pieces.get(pid);
            if (piece == null) continue;

            int maxRotations = piece.getUniqueRotationCount();
            for (int rot = 0; rot < maxRotations; rot++) {
                int[] edges = piece.edgesRotated(rot);
                if (validator.fits(board, r, c, edges)) {
                    uniquePieces.add(pid);
                    break; // Count piece once
                }
            }
        }

        return uniquePieces.size();
    }

    /** Orders pieces by LCV (Least Constraining Value) heuristic; prefers pieces leaving more options for neighbors (lower constraint score first). */
    public List<Integer> orderPiecesByLeastConstraining(
            Board board, int r, int c, List<Integer> availablePieces,
            Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {

        List<PieceScore> scored = new ArrayList<>();

        for (int pid : availablePieces) {
            if (pieceUsed.get(pid)) continue;

            Piece piece = pieces.get(pid);
            if (piece == null) continue;

            // Try all rotations and find least constraining
            int bestScore = Integer.MAX_VALUE;
            int maxRotations = piece.getUniqueRotationCount();

            for (int rot = 0; rot < maxRotations; rot++) {
                int[] edges = piece.edgesRotated(rot);
                if (validator.fits(board, r, c, edges)) {
                    int constraintScore = neighborAnalyzer.calculateConstraintScore(
                        board, r, c, edges, pieces, pieceUsed, totalPieces, pid);
                    bestScore = Math.min(bestScore, constraintScore);
                }
            }

            if (bestScore != Integer.MAX_VALUE) {
                scored.add(new PieceScore(pid, bestScore));
            }
        }

        // Sort by score (lower constraint = better)
        scored.sort(Comparator.comparingInt(ps -> ps.score));

        List<Integer> ordered = new ArrayList<>();
        for (PieceScore ps : scored) {
            ordered.add(ps.pieceId);
        }

        return ordered;
    }

    /** Orders pieces by difficulty (hardest first) using pre-calculated difficulty scores; fail-fast strategy to detect dead-ends early. */
    public List<Integer> orderByDifficulty(List<Integer> pieceIds,
                                          Map<Integer, Integer> difficultyScores) {
        if (difficultyScores == null || difficultyScores.isEmpty()) {
            return new ArrayList<>(pieceIds); // Return copy in original order
        }

        List<Integer> ordered = new ArrayList<>(pieceIds);
        ordered.sort(Comparator.comparingInt(pid ->
            difficultyScores.getOrDefault(pid, Integer.MAX_VALUE)));

        return ordered;
    }

    /** Selects corner piece for thread diversification; different threads try different corners to explore search space in parallel. */
    public Integer selectCornerPieceForThread(int threadId, List<Integer> unusedIds,
                                             Map<Integer, Piece> pieces) {
        if (threadId >= 4 || unusedIds.size() <= 10) {
            return null; // Only for first 4 threads, and only if enough pieces
        }

        // Identify corner pieces (exactly 2 edges = 0)
        List<Integer> cornerPieces = new ArrayList<>();
        for (int pid : unusedIds) {
            Piece p = pieces.get(pid);
            if (p == null) continue;

            int[] edges = p.getEdges();
            int zeroCount = 0;
            for (int e : edges) {
                if (e == 0) zeroCount++;
            }
            if (zeroCount == 2) {
                cornerPieces.add(pid);
            }
        }

        if (cornerPieces.isEmpty()) {
            return null;
        }

        // Select corner based on thread ID (distribute evenly)
        int index = threadId % cornerPieces.size();
        return cornerPieces.get(index);
    }

    /** Returns corner position for thread ID; maps threads 0-3 to four board corners. */
    public int[] getCornerPositionForThread(int threadId, int rows, int cols) {
        switch (threadId) {
            case 0: return new int[]{0, 0};           // Top-left
            case 1: return new int[]{0, cols - 1};    // Top-right
            case 2: return new int[]{rows - 1, 0};    // Bottom-left
            case 3: return new int[]{rows - 1, cols - 1}; // Bottom-right
            default: return null;
        }
    }

    /** Finds correct rotation for corner piece at specific corner position; corner pieces have exactly 2 zeros that must align with board edges. */
    public int findCornerRotation(Piece piece, int cornerRow, int cornerCol, int rows, int cols) {
        boolean isTopEdge = (cornerRow == 0);
        boolean isBottomEdge = (cornerRow == rows - 1);
        boolean isLeftEdge = (cornerCol == 0);
        boolean isRightEdge = (cornerCol == cols - 1);

        for (int rot = 0; rot < 4; rot++) {
            int[] edges = piece.edgesRotated(rot);
            boolean valid = true;

            // Check that each edge matches board constraint
            if (isTopEdge && edges[0] != 0) valid = false;      // North must be 0
            if (isBottomEdge && edges[2] != 0) valid = false;   // South must be 0
            if (isLeftEdge && edges[3] != 0) valid = false;     // West must be 0
            if (isRightEdge && edges[1] != 0) valid = false;    // East must be 0

            if (valid) {
                return rot;
            }
        }

        return -1; // No valid rotation found
    }
}
