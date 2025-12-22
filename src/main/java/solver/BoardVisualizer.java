package solver;

import model.Board;
import model.Piece;
import solver.visualization.BoardFormatter;
import solver.visualization.CompactBoardFormatter;
import solver.visualization.ComparisonBoardFormatter;
import solver.visualization.DetailedBoardFormatter;
import solver.visualization.FormatterContext;
import solver.visualization.LabeledBoardFormatter;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for visualizing puzzle boards in various ASCII art formats.
 *
 * REFACTORING STATUS: ✓ COMPLETE (169 lines, was: 609 lines, reduction: 72%)
 *
 * This class now serves as a facade/adapter, delegating to specialized formatters:
 * - {@link solver.visualization.CompactBoardFormatter} - Minimal ASCII representation
 * - {@link solver.visualization.DetailedBoardFormatter} - Enhanced view with highlighting
 * - {@link solver.visualization.LabeledBoardFormatter} - Coordinate labels with validation
 * - {@link solver.visualization.ComparisonBoardFormatter} - Side-by-side comparison
 *
 * Architecture:
 * - All visualization logic extracted to solver/visualization package
 * - Common utilities: {@link AnsiColorHelper}, {@link GridDrawingHelper}
 * - Context pattern: {@link FormatterContext} encapsulates formatting parameters
 * - Strategy pattern: {@link BoardFormatter} interface for different formats
 *
 * All methods remain static and solver-independent for backward compatibility.
 *
 * @see solver.visualization for detailed formatter documentation
 */
public class BoardVisualizer {

    // ===== Formatter Instances (reusable, stateless) =====

    private static final BoardFormatter compactFormatter = new CompactBoardFormatter();
    private static final BoardFormatter detailedFormatter = new DetailedBoardFormatter();
    private static final BoardFormatter labeledFormatter = new LabeledBoardFormatter();
    private static final BoardFormatter comparisonFormatter = new ComparisonBoardFormatter();

    // ===== Public API Methods (delegate to formatters) =====

    /**
     * Displays compact board representation showing edges and piece counts.
     * Empty cells show number of possible pieces that fit.
     *
     * @param board The board to visualize
     * @param piecesById Map of piece ID to Piece object
     * @param pieceUsed BitSet marking which pieces have been used
     * @param totalPieces Total number of pieces in the puzzle
     * @param fitsChecker Functional interface for checking if a piece fits
     */
    public static void printBoardCompact(Board board, Map<Integer, Piece> piecesById,
                                        BitSet pieceUsed, int totalPieces,
                                        FitsChecker fitsChecker) {
        FormatterContext context = FormatterContext.builder()
                .board(board)
                .pieces(piecesById)
                .fitsChecker(fitsChecker)
                .pieceUsed(pieceUsed)
                .totalPieces(totalPieces)
                .build();

        compactFormatter.format(context);
    }

    /**
     * Displays board with placed pieces and possible piece counts on empty cells.
     * Highlights last placed cell (green) and minimum remaining values/MRV (cyan).
     *
     * @param board The board to visualize
     * @param piecesById Map of piece ID to Piece object
     * @param pieceUsed BitSet marking which pieces have been used
     * @param totalPieces Total number of pieces in the puzzle
     * @param lastPlacedRow Row of last placed piece (-1 if none)
     * @param lastPlacedCol Column of last placed piece (-1 if none)
     * @param fitsChecker Functional interface for checking if a piece fits
     */
    public static void printBoardWithCounts(Board board, Map<Integer, Piece> piecesById,
                                           BitSet pieceUsed, int totalPieces,
                                           int lastPlacedRow, int lastPlacedCol,
                                           FitsChecker fitsChecker) {
        FormatterContext context = FormatterContext.builder()
                .board(board)
                .pieces(piecesById)
                .fitsChecker(fitsChecker)
                .pieceUsed(pieceUsed)
                .totalPieces(totalPieces)
                .lastPlaced(lastPlacedRow, lastPlacedCol)
                .build();

        detailedFormatter.format(context);
    }

    /**
     * Displays labeled board with coordinates (A-F rows, 1-N columns) and edge matching validation.
     * Color-codes edge compatibility and highlights fixed positions.
     *
     * @param board The board to visualize
     * @param piecesById Map of piece ID to Piece object
     * @param unusedIds List of unused piece IDs
     * @param fixedPositions Set of fixed positions in "row,col" format
     * @param fitsChecker Functional interface for checking if a piece fits
     */
    public static void printBoardWithLabels(Board board, Map<Integer, Piece> piecesById,
                                           List<Integer> unusedIds, Set<String> fixedPositions,
                                           FitsChecker fitsChecker) {
        FormatterContext context = FormatterContext.builder()
                .board(board)
                .pieces(piecesById)
                .fitsChecker(fitsChecker)
                .unusedIds(unusedIds)
                .fixedPositions(fixedPositions)
                .build();

        labeledFormatter.format(context);
    }

    /**
     * Displays board comparison showing differences with color coding.
     * - Magenta: regression (was occupied, now empty)
     * - Orange: change (different piece/rotation)
     * - Yellow: progress (was empty, now occupied)
     * - Cyan: stable (identical piece/rotation)
     *
     * @param currentBoard Current board state
     * @param referenceBoard Reference board to compare against
     * @param piecesById Map of piece ID to Piece object
     * @param unusedIds List of unused piece IDs
     * @param fitsChecker Functional interface for checking if a piece fits
     */
    public static void printBoardWithComparison(Board currentBoard, Board referenceBoard,
                                               Map<Integer, Piece> piecesById, List<Integer> unusedIds,
                                               FitsChecker fitsChecker) {
        FormatterContext context = FormatterContext.builder()
                .board(currentBoard)
                .pieces(piecesById)
                .fitsChecker(fitsChecker)
                .unusedIds(unusedIds)
                .referenceBoard(referenceBoard)
                .build();

        comparisonFormatter.format(context);
    }

    // ===== Functional Interface =====

    /**
     * Functional interface for checking if a piece fits at a position.
     * Keeps BoardVisualizer independent from EternitySolver.
     */
    @FunctionalInterface
    public interface FitsChecker {
        /**
         * Checks if a piece with given edges can be placed at position.
         *
         * @param board The board
         * @param r Row index
         * @param c Column index
         * @param candidateEdges Edge values [N, E, S, W] to test
         * @return true if piece fits, false otherwise
         */
        boolean fits(Board board, int r, int c, int[] candidateEdges);
    }

    // Private constructor to prevent instantiation
    private BoardVisualizer() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
