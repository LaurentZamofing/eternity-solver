package solver;

import model.Board;
import model.Piece;
import model.Placement;

import java.util.BitSet;
import java.util.Map;

/**
 * Responsible for validating piece placements on the grid.
 * Extracted from EternitySolver to improve code organization.
 */
public class PlacementValidator {
    private final CellConstraints[][] cellConstraints;
    private final EternitySolver.Statistics stats;
    private final String sortOrder;

    public PlacementValidator(CellConstraints[][] cellConstraints, EternitySolver.Statistics stats, String sortOrder) {
        this.cellConstraints = cellConstraints;
        this.stats = stats;
        this.sortOrder = sortOrder;
    }

    /**
     * Checks if a piece with given edges can fit at position (r, c) on the grid.
     *
     * @param board current grid
     * @param r row
     * @param c column
     * @param candidateEdges edges of the candidate piece
     * @return true if the piece can be placed at this position
     */
    public boolean fits(Board board, int r, int c, int[] candidateEdges) {
        stats.fitChecks++;

        // Get pre-calculated constraints for this cell
        CellConstraints constraints = cellConstraints[r][c];

        // Quick check: if a required border edge is non-zero, reject immediately
        // Check north border
        if (constraints.isNorthBorder()) {
            if (candidateEdges[0] != 0) return false;
        } else {
            Placement north = board.getPlacement(constraints.northRow, constraints.northCol);
            if (north != null && north.edges[2] != candidateEdges[0]) return false;
            // Inverse constraint: if edge is 0, it must be on border
            if (candidateEdges[0] == 0) return false;
        }

        // Check west border
        if (constraints.isWestBorder()) {
            if (candidateEdges[3] != 0) return false;
        } else {
            Placement west = board.getPlacement(constraints.westRow, constraints.westCol);
            if (west != null && west.edges[1] != candidateEdges[3]) return false;
            // Inverse constraint
            if (candidateEdges[3] == 0) return false;
        }

        // Check south border
        if (constraints.isSouthBorder()) {
            if (candidateEdges[2] != 0) return false;
        } else {
            Placement south = board.getPlacement(constraints.southRow, constraints.southCol);
            if (south != null && south.edges[0] != candidateEdges[2]) return false;
            // Inverse constraint
            if (candidateEdges[2] == 0) return false;
        }

        // Check east border
        if (constraints.isEastBorder()) {
            if (candidateEdges[1] != 0) return false;
        } else {
            Placement east = board.getPlacement(constraints.eastRow, constraints.eastCol);
            if (east != null && east.edges[3] != candidateEdges[1]) return false;
            // Inverse constraint
            if (candidateEdges[1] == 0) return false;
        }

        return true;
    }

    /**
     * Forward checking: verifies that a placement will not create a dead-end for empty neighbors.
     *
     * @param board current grid
     * @param r row where we want to place
     * @param c column where we want to place
     * @param candidateEdges edges of the piece we want to place
     * @param piecesById all available pieces
     * @param pieceUsed array of used pieces
     * @param totalPieces total number of pieces
     * @param excludePieceId ID of the piece being tested (to exclude)
     * @return true if the placement is safe, false if it would create a dead-end
     */
    public boolean forwardCheck(Board board, int r, int c, int[] candidateEdges,
                                 Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces, int excludePieceId) {
        int rows = board.getRows();
        int cols = board.getCols();

        // Check each empty neighbor
        // Top neighbor
        if (r > 0 && board.isEmpty(r - 1, c)) {
            if (!hasValidPiece(board, r - 1, c, candidateEdges[0], 2, piecesById, pieceUsed, totalPieces, excludePieceId)) {
                return false; // Top neighbor would have no valid piece
            }
        }

        // Bottom neighbor
        if (r < rows - 1 && board.isEmpty(r + 1, c)) {
            if (!hasValidPiece(board, r + 1, c, candidateEdges[2], 0, piecesById, pieceUsed, totalPieces, excludePieceId)) {
                return false; // Bottom neighbor would have no valid piece
            }
        }

        // Left neighbor
        if (c > 0 && board.isEmpty(r, c - 1)) {
            if (!hasValidPiece(board, r, c - 1, candidateEdges[3], 1, piecesById, pieceUsed, totalPieces, excludePieceId)) {
                return false; // Left neighbor would have no valid piece
            }
        }

        // Right neighbor
        if (c < cols - 1 && board.isEmpty(r, c + 1)) {
            if (!hasValidPiece(board, r, c + 1, candidateEdges[1], 3, piecesById, pieceUsed, totalPieces, excludePieceId)) {
                return false; // Right neighbor would have no valid piece
            }
        }

        return true; // All neighbors have at least one valid piece
    }

    /**
     * Checks if a neighbor cell will have at least one valid piece after a placement.
     *
     * @param board current grid
     * @param r row of the neighbor cell
     * @param c column of the neighbor cell
     * @param requiredEdge edge value that the neighbor piece must match
     * @param edgeIndex edge index (0=N, 1=E, 2=S, 3=W)
     * @param piecesById all pieces
     * @param pieceUsed array of used pieces
     * @param totalPieces total number of pieces
     * @param excludePieceId piece to exclude (the one being tested)
     * @return true if at least one valid piece exists for this cell
     */
    private boolean hasValidPiece(Board board, int r, int c, int requiredEdge, int edgeIndex,
                                   Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces, int excludePieceId) {
        // Iterate over pieces in the order specified by sortOrder
        if ("descending".equals(sortOrder)) {
            // Descending: from totalPieces to 1
            for (int pid = totalPieces; pid >= 1; pid--) {
                if (pieceUsed.get(pid)) continue; // Piece already used
                if (pid == excludePieceId) continue; // Ignore the piece being placed

                Piece piece = piecesById.get(pid);
                if (piece == null) continue; // Skip null pieces (sparse IDs or corrupted data)
                // Try all 4 rotations
                for (int rot = 0; rot < 4; rot++) {
                    int[] edges = piece.edgesRotated(rot);

                    // Check that required edge matches
                    if (edges[edgeIndex] != requiredEdge) continue;

                    // Check if this piece/rotation would be valid at this position
                    if (fits(board, r, c, edges)) {
                        return true; // Found at least one valid piece
                    }
                }
            }
        } else {
            // Ascending (default): from 1 to totalPieces
            for (int pid = 1; pid <= totalPieces; pid++) {
                if (pieceUsed.get(pid)) continue; // Piece already used
                if (pid == excludePieceId) continue; // Ignore the piece being placed

                Piece piece = piecesById.get(pid);
                if (piece == null) continue; // Skip null pieces (sparse IDs or corrupted data)
                // Try all 4 rotations
                for (int rot = 0; rot < 4; rot++) {
                    int[] edges = piece.edgesRotated(rot);

                    // Check that required edge matches
                    if (edges[edgeIndex] != requiredEdge) continue;

                    // Check if this piece/rotation would be valid at this position
                    if (fits(board, r, c, edges)) {
                        return true; // Found at least one valid piece
                    }
                }
            }
        }

        return false; // No valid piece found
    }
}
