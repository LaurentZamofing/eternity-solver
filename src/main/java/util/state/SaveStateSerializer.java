package util.state;

import model.Board;
import model.Piece;
import model.Placement;
import util.SaveStateManager.PlacementInfo;
import util.SaveStateManager.SaveState;
import util.SolverLogger;

import java.util.*;

/**
 * Handles serialization and deserialization of SaveState objects.
 * Converts between Board/Pieces and SaveState data structures.
 *
 * Extracted from SaveStateManager for better separation of concerns.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class SaveStateSerializer {

    /**
     * Creates a SaveState from a Board and piece information.
     *
     * @param puzzleName Name of the puzzle
     * @param board Current board state
     * @param unusedIds List of unused piece IDs
     * @param placementOrder Order in which pieces were placed
     * @param numFixedPieces Number of fixed pieces (pre-placed)
     * @return SaveState object containing all the state information
     */
    public static SaveState createSaveState(String puzzleName, Board board,
                                           List<Integer> unusedIds,
                                           List<PlacementInfo> placementOrder,
                                           int numFixedPieces,
                                           long totalComputeTimeMs) {
        // Collect placements from board
        Map<String, PlacementInfo> placements = new HashMap<>();
        int totalPieces = 0;

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.isEmpty(r, c)) {
                    Placement p = board.getPlacement(r, c);
                    placements.put(r + "," + c,
                        new PlacementInfo(r, c, p.getPieceId(), p.getRotation()));
                    totalPieces++;
                }
            }
        }

        // Calculate depth (pieces placed by backtracking, excluding fixed)
        int depth = totalPieces - numFixedPieces;

        // Convert unusedIds to Set
        Set<Integer> unusedPieceIds = new HashSet<>(unusedIds);

        long timestamp = System.currentTimeMillis();

        return new SaveState(puzzleName, board.getRows(), board.getCols(),
                           placements, placementOrder, unusedPieceIds,
                           timestamp, depth, totalComputeTimeMs);
    }

    /**
     * Restores a Board from a SaveState.
     *
     * @param state SaveState to restore from
     * @param board Board to restore into (must have matching dimensions)
     * @param allPieces Map of all available pieces
     * @return true if restoration was successful, false otherwise
     */
    public static boolean restoreStateToBoard(SaveState state, Board board,
                                             Map<Integer, Piece> allPieces) {
        // Verify dimensions
        if (board.getRows() != state.rows || board.getCols() != state.cols) {
            SolverLogger.error("  ⚠️  Incompatible dimensions!");
            return false;
        }

        // Place pieces on the board
        for (Map.Entry<String, PlacementInfo> entry : state.placements.entrySet()) {
            String[] coords = entry.getKey().split(",");
            int r = Integer.parseInt(coords[0]);
            int c = Integer.parseInt(coords[1]);
            PlacementInfo info = entry.getValue();

            Piece piece = allPieces.get(info.pieceId);
            if (piece == null) {
                SolverLogger.error("  ⚠️  Piece " + info.pieceId + " not found!");
                return false;
            }

            board.place(r, c, piece, info.rotation);
        }

        return true;
    }

    /**
     * Extracts the list of unused piece IDs from a SaveState.
     *
     * @param state SaveState to extract from
     * @return List of unused piece IDs
     */
    public static List<Integer> getUnusedPieceIds(SaveState state) {
        return new ArrayList<>(state.unusedPieceIds);
    }

    /**
     * Gets the placement order (excluding fixed pieces if specified).
     *
     * @param state SaveState to extract from
     * @param excludeFixedCount Number of fixed pieces to exclude from the beginning
     * @return List of placement info for backtracking
     */
    public static List<PlacementInfo> getBacktrackingOrder(SaveState state,
                                                           int excludeFixedCount) {
        if (state.placementOrder == null || state.placementOrder.isEmpty()) {
            return new ArrayList<>();
        }

        if (excludeFixedCount >= state.placementOrder.size()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(
            state.placementOrder.subList(excludeFixedCount, state.placementOrder.size())
        );
    }
}
