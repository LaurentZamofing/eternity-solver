package solver.display;

import model.Board;
import model.Piece;
import solver.PlacementValidator;

import java.util.List;
import java.util.Map;

/**
 * Counts valid piece placements for a board position.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Count unused pieces that fit at a given position</li>
 *   <li>Test all rotations for each piece</li>
 *   <li>Use PlacementValidator for edge matching</li>
 * </ul>
 *
 * <h2>Design</h2>
 * Extracted from BoardDisplayManager for better testability and reuse.
 * This logic is used for display (to show deadends) but could also
 * be reused for solver heuristics.
 *
 * <h2>Usage</h2>
 * <pre>
 * ValidPieceCounter counter = new ValidPieceCounter(validator);
 *
 * int validCount = counter.countValidPieces(
 *     board, row, col, piecesById, unusedIds
 * );
 *
 * if (validCount == 0) {
 *     // Deadend detected!
 * }
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class ValidPieceCounter {

    private final PlacementValidator validator;

    /**
     * Creates valid piece counter with placement validator.
     *
     * @param validator Validator for checking piece fits
     */
    public ValidPieceCounter(PlacementValidator validator) {
        this.validator = validator;
    }

    /**
     * Counts number of unused pieces that can validly fit at position.
     *
     * <p>For each unused piece, tests all 4 rotations. If any rotation
     * fits (passes edge matching validation), the piece is counted.</p>
     *
     * @param board Current board state
     * @param row Row position to test
     * @param col Column position to test
     * @param piecesById Map of all pieces by ID
     * @param unusedIds List of unused piece IDs
     * @return Count of pieces with at least one valid rotation
     */
    public int countValidPieces(Board board, int row, int col,
                                Map<Integer, Piece> piecesById,
                                List<Integer> unusedIds) {
        int count = 0;

        for (int pieceId : unusedIds) {
            Piece piece = piecesById.get(pieceId);

            if (piece != null) {
                // Test all 4 rotations
                for (int rotation = 0; rotation < 4; rotation++) {
                    int[] rotatedEdges = piece.edgesRotated(rotation);

                    if (validator.fits(board, row, col, rotatedEdges)) {
                        count++;
                        break; // At least one rotation works, count this piece
                    }
                }
            }
        }

        return count;
    }

    /**
     * Result containing both piece count and rotation count.
     */
    public static class ValidCountResult {
        public final int numPieces;      // Number of unique pieces that fit
        public final int numRotations;   // Total number of valid rotations

        public ValidCountResult(int numPieces, int numRotations) {
            this.numPieces = numPieces;
            this.numRotations = numRotations;
        }
    }

    /**
     * Counts both unique pieces AND total valid rotations for a position.
     *
     * <p>More detailed than countValidPieces() - returns both the number of
     * unique pieces that fit AND the total number of valid rotations across
     * all those pieces.</p>
     *
     * <p>Example: If piece A fits with rotations 0,1,2 and piece B fits with
     * rotation 3, returns numPieces=2, numRotations=4.</p>
     *
     * @param board Current board state
     * @param row Row position to test
     * @param col Column position to test
     * @param piecesById Map of all pieces by ID
     * @param unusedIds List of unused piece IDs
     * @return Result with piece count and rotation count
     */
    public ValidCountResult countValidPiecesAndRotations(Board board, int row, int col,
                                                          Map<Integer, Piece> piecesById,
                                                          List<Integer> unusedIds) {
        int numPieces = 0;
        int numRotations = 0;

        for (int pieceId : unusedIds) {
            Piece piece = piecesById.get(pieceId);

            if (piece != null) {
                int validRotationsForThisPiece = 0;

                // Test all 4 rotations
                for (int rotation = 0; rotation < 4; rotation++) {
                    int[] rotatedEdges = piece.edgesRotated(rotation);

                    if (validator.fits(board, row, col, rotatedEdges)) {
                        validRotationsForThisPiece++;
                        numRotations++;
                    }
                }

                // Count this piece if at least one rotation works
                if (validRotationsForThisPiece > 0) {
                    numPieces++;
                }
            }
        }

        return new ValidCountResult(numPieces, numRotations);
    }
}
