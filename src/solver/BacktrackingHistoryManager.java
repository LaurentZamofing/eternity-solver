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
            return 9; // 4 coins + 5 indices pour Eternity II
        } else if (puzzleName.startsWith("indice")) {
            return 0; // Pas de pièces fixes pour les puzzles d'indices
        } else {
            return 0; // Par défaut : pas de pièces fixes
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
            // Retirer la dernière pièce de l'historique
            SaveStateManager.PlacementInfo lastPlacement =
                placementOrder.remove(placementOrder.size() - 1);

            int row = lastPlacement.row;
            int col = lastPlacement.col;
            int pieceId = lastPlacement.pieceId;
            int oldRotation = lastPlacement.rotation;

            // Retirer la pièce du plateau
            board.remove(row, col);

            // Remettre la pièce dans les pièces disponibles
            pieceUsed.clear(pieceId);

            stats.backtracks++;
            backtrackCount++;

            System.out.println(threadLabel + "  → Backtrack pré-chargé #" + backtrackCount +
                             ": retrait pièce " + pieceId + " (rot=" + oldRotation + ") à [" +
                             row + "," + col + "]");
            System.out.println(threadLabel + "  → Profondeur réduite à: " +
                             placementOrder.size() + " pièces");

            // Essayer des rotations alternatives avant de reculer davantage
            result = tryAlternativeRotations(board, allPieces, pieceUsed, placementOrder,
                                           row, col, pieceId, oldRotation, sequentialSolver);

            if (result) {
                return true;
            }

            // Aucune rotation alternative n'a fonctionné, essayer de résoudre avec moins de pièces
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

        // Essayer les rotations après celle qui a échoué
        for (int rot = (oldRotation + 1) % 4; rot < maxRotations; rot++) {
            int[] candidate = piece.edgesRotated(rot);

            if (validator.fits(board, row, col, candidate)) {
                System.out.println(threadLabel + "  → Tentative rotation alternative " + rot +
                                 " pour pièce " + pieceId + " à [" + row + "," + col + "]");

                // Placer avec la nouvelle rotation
                board.place(row, col, piece, rot);
                pieceUsed.set(pieceId);
                placementOrder.add(new SaveStateManager.PlacementInfo(row, col, pieceId, rot));

                // Essayer de résoudre avec cette rotation
                int totalPieces = allPieces.size();
                boolean result = sequentialSolver.solve(board, allPieces, pieceUsed, totalPieces);

                if (result) {
                    return true;
                }

                // Cette rotation n'a pas fonctionné, la retirer
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

        // Retirer la dernière pièce de l'historique
        SaveStateManager.PlacementInfo lastPlacement =
            placementOrder.remove(placementOrder.size() - 1);

        int row = lastPlacement.row;
        int col = lastPlacement.col;
        int pieceId = lastPlacement.pieceId;

        // Retirer la pièce du plateau
        board.remove(row, col);

        // Remettre la pièce dans les pièces disponibles
        pieceUsed.clear(pieceId);

        stats.backtracks++;

        return lastPlacement;
    }

    /** Returns fixed positions that should not be backtracked (currently empty set). */
    public Set<String> getFixedPositions() {
        return new HashSet<>(); // Pas de positions fixes pour l'instant
    }

    /** Returns true if position is fixed and should not be backtracked. */
    public boolean isFixedPosition(int row, int col, Set<String> fixedPositions) {
        return fixedPositions.contains(row + "," + col);
    }

    /** Logs backtrack progress showing count and current depth. */
    public void logBacktrackProgress(int backtrackCount, int currentDepth) {
        System.out.println(threadLabel + "  → Total backtracks dans historique: " + backtrackCount);
        System.out.println(threadLabel + "  → Profondeur actuelle: " + currentDepth + " pièces");
    }
}
