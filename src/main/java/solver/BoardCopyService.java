package solver;

import model.Board;
import model.Piece;
import model.Placement;

import java.util.Map;

/**
 * Service for creating deep copies of boards.
 *
 * <h2>Purpose</h2>
 * Parallel search threads need independent board copies to avoid race conditions.
 * This service encapsulates the logic for creating complete deep copies.
 *
 * <h2>Deep Copy Process</h2>
 * <ol>
 *   <li>Create new board with same dimensions</li>
 *   <li>Iterate over all cells in original board</li>
 *   <li>For each placed piece, copy placement with same piece and rotation</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>
 * BoardCopyService copyService = new BoardCopyService();
 *
 * Board copy = copyService.copyBoard(original, pieces);
 * // copy is now a completely independent board instance
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class BoardCopyService {

    /**
     * Creates a deep copy of a board.
     *
     * <p>All placements are copied, but pieces are shared (not copied)
     * since pieces are immutable.</p>
     *
     * @param original Original board to copy
     * @param pieces Map of all pieces (shared, not copied)
     * @return New board instance with copied placements
     */
    public Board copyBoard(Board original, Map<Integer, Piece> pieces) {
        Board copy = new Board(original.getRows(), original.getCols());

        for (int r = 0; r < original.getRows(); r++) {
            for (int c = 0; c < original.getCols(); c++) {
                if (!original.isEmpty(r, c)) {
                    Placement p = original.getPlacement(r, c);
                    Piece piece = pieces.get(p.getPieceId());

                    if (piece != null) {
                        copy.place(r, c, piece, p.getRotation());
                    }
                }
            }
        }

        return copy;
    }
}
