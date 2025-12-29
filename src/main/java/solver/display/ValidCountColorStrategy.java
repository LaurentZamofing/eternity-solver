package solver.display;

import model.Board;
import model.Piece;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Color strategy for valid piece count visualization.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Highlight deadends (0 valid pieces) in bright red</li>
 *   <li>Highlight critical positions (1-5 valid pieces) in bright yellow</li>
 *   <li>Highlight warning positions (6-20 valid pieces) in yellow</li>
 * </ul>
 *
 * <h2>Design</h2>
 * Uses ValidPieceCounter to count valid pieces for empty positions.
 * Only applies colors to empty cells based on how many pieces can fit.
 *
 * <h2>Color Scheme</h2>
 * <ul>
 *   <li>Bright Red: Deadend (0 valid pieces - unsolvable)</li>
 *   <li>Bright Yellow: Critical (1-5 valid pieces - needs attention)</li>
 *   <li>Yellow: Warning (6-20 valid pieces - constrained)</li>
 *   <li>No color: Normal (21+ valid pieces)</li>
 * </ul>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class ValidCountColorStrategy implements ColorStrategy {

    private final ValidPieceCounter counter;
    private final Map<Integer, Piece> piecesById;
    private final List<Integer> unusedIds;
    private final Set<String> highlightedPositions;

    /**
     * Creates valid count color strategy.
     *
     * @param counter Valid piece counter
     * @param piecesById Map of all pieces by ID
     * @param unusedIds List of unused piece IDs
     */
    public ValidCountColorStrategy(ValidPieceCounter counter,
                                   Map<Integer, Piece> piecesById,
                                   List<Integer> unusedIds) {
        this(counter, piecesById, unusedIds, null);
    }

    /**
     * Creates valid count color strategy with highlighted positions.
     *
     * @param counter Valid piece counter
     * @param piecesById Map of all pieces by ID
     * @param unusedIds List of unused piece IDs
     * @param highlightedPositions Set of positions to highlight ("row,col")
     */
    public ValidCountColorStrategy(ValidPieceCounter counter,
                                   Map<Integer, Piece> piecesById,
                                   List<Integer> unusedIds,
                                   Set<String> highlightedPositions) {
        this.counter = counter;
        this.piecesById = piecesById;
        this.unusedIds = unusedIds;
        this.highlightedPositions = highlightedPositions;
    }

    /**
     * Returns color based on number of valid pieces for empty position.
     * Highlighted empty cells (next target) get bold blue.
     * Only colors empty cells - occupied cells return empty string.
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @return Color code based on valid piece count or highlight status
     */
    @Override
    public String getCellColor(Board board, int row, int col) {
        // Only color empty cells
        if (!board.isEmpty(row, col)) {
            return "";
        }

        String positionKey = row + "," + col;

        // Highlighted empty positions (next target) get bold blue
        if (highlightedPositions != null && highlightedPositions.contains(positionKey)) {
            return BOLD + BLUE;
        }

        // Count valid pieces for this position
        int validCount = counter.countValidPieces(board, row, col, piecesById, unusedIds);

        // Color according to number of possible pieces
        if (validCount == 0) {
            return BRIGHT_RED;  // Deadend! (bright red + bold)
        } else if (validCount <= 5) {
            return BRIGHT_YELLOW;  // Critical (bright yellow)
        } else if (validCount <= 20) {
            return YELLOW;  // Warning (dark yellow)
        }

        // Normal - plenty of options
        return "";
    }

    /**
     * Returns empty string - valid count mode uses cell colors only.
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @param direction Edge direction
     * @return Empty string (no edge-specific coloring)
     */
    @Override
    public String getEdgeColor(Board board, int row, int col, int direction) {
        // Valid count mode doesn't use edge-specific colors
        // All coloring is done at cell level based on valid piece count
        return "";
    }
}
