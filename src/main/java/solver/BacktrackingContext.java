package solver;

import model.Board;
import model.Piece;

import java.util.BitSet;
import java.util.Map;

/**
 * Context object for backtracking algorithm execution.
 * Encapsulates all state needed during recursive backtracking to reduce parameter passing.
 *
 * This is an immutable value object - modifications should be done on the contained
 * mutable objects (Board, BitSet, etc.) not the context itself.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class BacktrackingContext {

    /** The puzzle board being solved */
    public final Board board;

    /** All available pieces indexed by ID */
    public final Map<Integer, Piece> piecesById;

    /** BitSet tracking which pieces have been placed (true = used) */
    public final BitSet pieceUsed;

    /** Total number of pieces in the puzzle */
    public final int totalPieces;

    /** Statistics collector for the solving process */
    public final StatisticsManager stats;

    /** Number of pieces that were fixed/pre-placed before backtracking started */
    public final int numFixedPieces;

    /**
     * Creates a new backtracking context.
     *
     * @param board Current state of the puzzle board
     * @param piecesById All pieces indexed by their ID
     * @param pieceUsed BitSet marking which pieces are already placed
     * @param totalPieces Total count of pieces in the puzzle
     * @param stats Statistics collector
     * @param numFixedPieces Number of pre-placed/fixed pieces
     */
    public BacktrackingContext(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed,
                              int totalPieces, StatisticsManager stats, int numFixedPieces) {
        this.board = board;
        this.piecesById = piecesById;
        this.pieceUsed = pieceUsed;
        this.totalPieces = totalPieces;
        this.stats = stats;
        this.numFixedPieces = numFixedPieces;
    }

    /**
     * Calculates the current depth (number of pieces placed, excluding fixed pieces).
     *
     * @return Current search depth
     */
    public int getCurrentDepth() {
        int usedCount = 0;
        for (int i = 1; i <= totalPieces; i++) {
            if (pieceUsed.get(i)) usedCount++;
        }
        return usedCount - numFixedPieces;
    }

    /**
     * Counts how many pieces are currently available (not yet used).
     *
     * @return Number of available pieces
     */
    public int countAvailablePieces() {
        int count = 0;
        for (int i = 1; i <= totalPieces; i++) {
            if (!pieceUsed.get(i)) count++;
        }
        return count;
    }
}
