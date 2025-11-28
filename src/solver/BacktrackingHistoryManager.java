package solver;

import model.Board;
import model.Piece;
import util.SaveStateManager;

import java.util.*;

/** Manages historical backtracking through pre-loaded states with full backtracking capability; tries alternative rotations before deeper backtracking when resuming from saved games. */
public class BacktrackingHistoryManager {

    private final PlacementValidator validator;
    private final String threadLabel;
    private final EternitySolver.Statistics stats;

    /** Creates history manager with placement validator, thread label for logging, and statistics tracker. */
    public BacktrackingHistoryManager(PlacementValidator validator,
                                     String threadLabel,
                                     EternitySolver.Statistics stats) {
        this.validator = validator;
        this.threadLabel = threadLabel;
        this.stats = stats;
    }

    /** Calculates number of fixed pieces (corners, hints) that should not be backtracked based on puzzle name. */
    public int calculateFixedPieces(String puzzleName) {
        if (puzzleName.startsWith("eternity2")) {
            return 9; // 4 corners + 5 hints for Eternity II
        } else if (puzzleName.startsWith("indice")) {
            return 0; // No fixed pieces for hint puzzles
        } else {
            return 0; // Default: no fixed pieces
        }
    }

    /** Builds initial fixed pieces list from first N entries of placement history. */
    public List<SaveStateManager.PlacementInfo> buildInitialFixedPieces(
            List<SaveStateManager.PlacementInfo> preloadedOrder,
            int numFixedPieces) {
        List<SaveStateManager.PlacementInfo> fixedPieces = new ArrayList<>();
        for (int i = 0; i < Math.min(numFixedPieces, preloadedOrder.size()); i++) {
            fixedPieces.add(preloadedOrder.get(i));
        }
        return fixedPieces;
    }

    /** Backtracks through pre-loaded placements, trying alternative rotations before deeper backtracking; returns true if solution found. */
    public boolean backtrackThroughHistory(Board board,
                                          Map<Integer, Piece> allPieces,
                                          BitSet pieceUsed,
                                          List<SaveStateManager.PlacementInfo> placementOrder,
                                          SequentialSolver sequentialSolver) {
        int backtrackCount = 0;
        boolean result = false;

        while (!result && !placementOrder.isEmpty()) {
            // Remove last piece from history
            SaveStateManager.PlacementInfo lastPlacement =
                placementOrder.remove(placementOrder.size() - 1);

            int row = lastPlacement.row;
            int col = lastPlacement.col;
            int pieceId = lastPlacement.pieceId;
            int oldRotation = lastPlacement.rotation;

            // Remove piece from board
            board.remove(row, col);

            // Return piece to available pieces
            pieceUsed.clear(pieceId);

            stats.backtracks++;
            backtrackCount++;

            System.out.println(threadLabel + "  → Pre-loaded backtrack #" + backtrackCount +
                             ": removing piece " + pieceId + " (rot=" + oldRotation + ") at [" +
                             row + "," + col + "]");
            System.out.println(threadLabel + "  → Depth reduced to: " +
                             placementOrder.size() + " pieces");

            // Try alternative rotations before backtracking further
            result = tryAlternativeRotations(board, allPieces, pieceUsed, placementOrder,
                                           row, col, pieceId, oldRotation, sequentialSolver);

            if (result) {
                return true;
            }

            // No alternative rotation worked, try solving with fewer pieces
            int totalPieces = allPieces.size();
            result = sequentialSolver.solve(board, allPieces, pieceUsed, totalPieces);
        }

        return result;
    }

    /** Tries alternative rotations of same piece at same position before continuing backtrack; returns true if solution found with alternative rotation. */
    public boolean tryAlternativeRotations(Board board,
                                          Map<Integer, Piece> allPieces,
                                          BitSet pieceUsed,
                                          List<SaveStateManager.PlacementInfo> placementOrder,
                                          int row, int col, int pieceId, int oldRotation,
                                          SequentialSolver sequentialSolver) {
        Piece piece = allPieces.get(pieceId);
        if (piece == null) {
            return false;
        }

        int maxRotations = piece.getUniqueRotationCount();

        // Try rotations after the one that failed
        for (int rot = (oldRotation + 1) % 4; rot < maxRotations; rot++) {
            int[] candidate = piece.edgesRotated(rot);

            if (validator.fits(board, row, col, candidate)) {
                System.out.println(threadLabel + "  → Trying alternative rotation " + rot +
                                 " for piece " + pieceId + " at [" + row + "," + col + "]");

                // Place with new rotation
                board.place(row, col, piece, rot);
                pieceUsed.set(pieceId);
                placementOrder.add(new SaveStateManager.PlacementInfo(row, col, pieceId, rot));

                // Try solving with this rotation
                int totalPieces = allPieces.size();
                boolean result = sequentialSolver.solve(board, allPieces, pieceUsed, totalPieces);

                if (result) {
                    return true;
                }

                // This rotation didn't work, remove it
                board.remove(row, col);
                pieceUsed.clear(pieceId);
                placementOrder.remove(placementOrder.size() - 1);
                stats.backtracks++;
            }
        }

        return false;
    }

    /** Callback interface for sequential solver. */
    public interface SequentialSolver {
        boolean solve(Board board, Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces);
    }

    /** Backtracks single pre-loaded piece and returns removed placement info. */
    public SaveStateManager.PlacementInfo backtrackPreloadedPiece(
            Board board,
            Map<Integer, Piece> allPieces,
            BitSet pieceUsed,
            List<SaveStateManager.PlacementInfo> placementOrder) {

        if (placementOrder.isEmpty()) {
            return null;
        }

        // Remove last piece from history
        SaveStateManager.PlacementInfo lastPlacement =
            placementOrder.remove(placementOrder.size() - 1);

        int row = lastPlacement.row;
        int col = lastPlacement.col;
        int pieceId = lastPlacement.pieceId;

        // Remove piece from board
        board.remove(row, col);

        // Return piece to available pieces
        pieceUsed.clear(pieceId);

        stats.backtracks++;

        return lastPlacement;
    }

    /** Returns fixed positions that should not be backtracked (currently empty set). */
    public Set<String> getFixedPositions() {
        return new HashSet<>(); // No fixed positions for now
    }

    /** Returns true if position is fixed and should not be backtracked. */
    public boolean isFixedPosition(int row, int col, Set<String> fixedPositions) {
        return fixedPositions.contains(row + "," + col);
    }

    /** Logs backtrack progress showing count and current depth. */
    public void logBacktrackProgress(int backtrackCount, int currentDepth) {
        System.out.println(threadLabel + "  → Total backtracks in history: " + backtrackCount);
        System.out.println(threadLabel + "  → Current depth: " + currentDepth + " pieces");
    }
}
