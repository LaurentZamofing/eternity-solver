package solver.heuristics;

import model.Board;
import model.Piece;

import java.util.BitSet;
import java.util.Map;

/**
 * HeuristicStrategy defines interface for cell selection strategies in Eternity solver.
 *
 * Different heuristics can be implemented to choose which empty cell to fill next during
 * backtracking search. Cell choice can dramatically affect search performance.
 *
 * Common heuristics include:
 * - MRV (Minimum Remaining Values): Choose cell with fewest valid placements
 * - Degree heuristic: Choose cell with most neighbors
 * - Row-major order: Fill cells from top-left to bottom-right
 *
 * @author Eternity Solver Team
 */
public interface HeuristicStrategy {

    /**
     * Represents a position on the board.
     *
     * <p>Java 21 record — equals/hashCode/toString and accessors generated
     * automatically. Callers access components via {@code row()} / {@code col()}
     * (method form, not field form).</p>
     */
    record CellPosition(int row, int col) {

        /** Converts to array format [row, col] for backward compatibility. */
        public int[] toArray() {
            return new int[]{row, col};
        }
    }

    /**
     * Selects next cell to fill based on heuristic strategy.
     *
     * @param board current board state
     * @param piecesById map of all pieces by ID
     * @param pieceUsed bitset tracking which pieces are already placed
     * @param totalPieces total number of pieces in puzzle
     * @return position of next cell to fill, or null if no empty cells remain
     */
    CellPosition selectNextCell(Board board, Map<Integer, Piece> piecesById,
                                BitSet pieceUsed, int totalPieces);

    /**
     * Gets name of this heuristic strategy.
     *
     * @return descriptive name for this strategy
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
