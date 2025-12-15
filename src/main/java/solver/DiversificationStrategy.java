package solver;

import model.Board;
import model.Piece;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Strategy for diversifying parallel search threads.
 *
 * <h2>Purpose</h2>
 * When running multiple parallel threads, diversification ensures threads
 * explore different regions of the search space by pre-placing different
 * pieces (typically corner pieces) in different positions.
 *
 * <h2>Strategy</h2>
 * For the first 4 threads:
 * <ul>
 *   <li>Thread 0: Pre-place corner piece at top-left</li>
 *   <li>Thread 1: Pre-place corner piece at top-right</li>
 *   <li>Thread 2: Pre-place corner piece at bottom-left</li>
 *   <li>Thread 3: Pre-place corner piece at bottom-right</li>
 * </ul>
 *
 * <h2>Corner Pieces</h2>
 * Corner pieces are identified by having exactly 2 edges with value 0.
 * The rotation is adjusted so zeros align with the board borders.
 *
 * <h2>Usage</h2>
 * <pre>
 * DiversificationStrategy strategy = new DiversificationStrategy();
 *
 * // Apply diversification for thread 0
 * strategy.diversify(0, board, pieces, unusedIds, pieceUsed);
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class DiversificationStrategy {

    /**
     * Applies diversification strategy for a specific thread.
     *
     * <p>Pre-places a corner piece at a position determined by the thread ID.
     * Only applies diversification to the first 4 threads and if enough pieces available.</p>
     *
     * @param threadId Thread identifier (0-based)
     * @param board Board to modify
     * @param pieces Map of all pieces
     * @param unusedIds List of unused piece IDs (will be modified)
     * @param pieceUsed BitSet of used pieces (will be modified)
     */
    public void diversify(int threadId, Board board, Map<Integer, Piece> pieces,
                         List<Integer> unusedIds, BitSet pieceUsed) {
        if (threadId >= 4 || unusedIds.size() <= 10) {
            return; // Diversify only first 4 threads and if enough pieces
        }

        // Identify corner pieces (with 2 edges at zero)
        List<Integer> cornerPieces = findCornerPieces(pieces, unusedIds);

        if (threadId >= cornerPieces.size()) {
            return;
        }

        int cornerPieceId = cornerPieces.get(threadId);
        int[] cornerPosition = getCornerPosition(threadId, board);

        if (cornerPosition == null) {
            return; // Invalid thread ID
        }

        int cornerRow = cornerPosition[0];
        int cornerCol = cornerPosition[1];

        Piece cornerPiece = pieces.get(cornerPieceId);

        // Find rotation that places zeros on correct edges
        int rotation = findValidRotation(cornerPiece, cornerRow, cornerCol, board);

        if (rotation != -1) {
            board.place(cornerRow, cornerCol, cornerPiece, rotation);
            pieceUsed.set(cornerPieceId);
        }
    }

    /**
     * Finds all corner pieces in the unused pieces list.
     *
     * @param pieces Map of all pieces
     * @param unusedIds List of unused piece IDs
     * @return List of corner piece IDs
     */
    private List<Integer> findCornerPieces(Map<Integer, Piece> pieces, List<Integer> unusedIds) {
        List<Integer> cornerPieces = new ArrayList<>();

        for (int pid : unusedIds) {
            Piece p = pieces.get(pid);
            int[] edges = p.getEdges();

            int zeroCount = 0;
            for (int e : edges) {
                if (e == 0) zeroCount++;
            }

            if (zeroCount == 2) {
                cornerPieces.add(pid);
            }
        }

        return cornerPieces;
    }

    /**
     * Gets the corner position for a given thread ID.
     *
     * @param threadId Thread identifier (0-3)
     * @param board Board to get dimensions
     * @return Array of [row, col] or null if invalid thread ID
     */
    private int[] getCornerPosition(int threadId, Board board) {
        int lastRow = board.getRows() - 1;
        int lastCol = board.getCols() - 1;

        switch (threadId) {
            case 0: return new int[]{0, 0};          // Top-left
            case 1: return new int[]{0, lastCol};    // Top-right
            case 2: return new int[]{lastRow, 0};    // Bottom-left
            case 3: return new int[]{lastRow, lastCol}; // Bottom-right
            default: return null;
        }
    }

    /**
     * Finds a valid rotation for placing a corner piece at the specified position.
     *
     * @param piece Corner piece to place
     * @param row Row position
     * @param col Column position
     * @param board Board to determine corner type
     * @return Rotation (0-3) or -1 if no valid rotation found
     */
    private int findValidRotation(Piece piece, int row, int col, Board board) {
        int lastRow = board.getRows() - 1;
        int lastCol = board.getCols() - 1;

        for (int rot = 0; rot < 4; rot++) {
            int[] rotEdges = piece.edgesRotated(rot);
            boolean valid = false;

            // Top-left corner: North=0, West=0
            if (row == 0 && col == 0 && rotEdges[0] == 0 && rotEdges[3] == 0) valid = true;

            // Top-right corner: North=0, East=0
            if (row == 0 && col == lastCol && rotEdges[0] == 0 && rotEdges[1] == 0) valid = true;

            // Bottom-left corner: South=0, West=0
            if (row == lastRow && col == 0 && rotEdges[2] == 0 && rotEdges[3] == 0) valid = true;

            // Bottom-right corner: South=0, East=0
            if (row == lastRow && col == lastCol && rotEdges[2] == 0 && rotEdges[1] == 0) valid = true;

            if (valid) {
                return rot;
            }
        }

        return -1; // No valid rotation found
    }
}
