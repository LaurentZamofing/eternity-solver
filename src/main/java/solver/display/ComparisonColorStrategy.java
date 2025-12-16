package solver.display;

import model.Board;

/**
 * Color strategy for board comparison visualization.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Highlight regressions (pieces removed)</li>
 *   <li>Highlight progress (new pieces placed)</li>
 *   <li>Highlight stability (unchanged pieces)</li>
 *   <li>Highlight changes (different pieces)</li>
 * </ul>
 *
 * <h2>Design</h2>
 * Compares current board with reference board to show differences.
 * Uses bold colors to make changes stand out clearly.
 *
 * <h2>Color Scheme</h2>
 * <ul>
 *   <li>Bold Magenta: Regression (was occupied, now empty)</li>
 *   <li>Bold Yellow: Progress (was empty, now occupied)</li>
 *   <li>Bold Cyan: Stability (same piece, same rotation)</li>
 *   <li>Bold Orange: Change (different piece or rotation)</li>
 * </ul>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class ComparisonColorStrategy implements ColorStrategy {

    private final Board referenceBoard;

    /**
     * Creates comparison color strategy.
     *
     * @param referenceBoard Reference board to compare against
     */
    public ComparisonColorStrategy(Board referenceBoard) {
        this.referenceBoard = referenceBoard;
    }

    /**
     * Returns color based on comparison with reference board.
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @return Color code indicating change type
     */
    @Override
    public String getCellColor(Board board, int row, int col) {
        boolean currentEmpty = board.isEmpty(row, col);
        boolean refEmpty = referenceBoard.isEmpty(row, col);

        if (refEmpty && currentEmpty) {
            // Both empty - no special color
            return "";
        } else if (!refEmpty && currentEmpty) {
            // Was occupied, now empty - REGRESSION (Magenta)
            return BOLD_MAGENTA;
        } else if (refEmpty && !currentEmpty) {
            // Was empty, now occupied - PROGRESS (Yellow)
            return BOLD_YELLOW;
        } else {
            // Both occupied - compare pieces
            int currentPieceId = board.getPlacement(row, col).getPieceId();
            int currentRotation = board.getPlacement(row, col).getRotation();
            int refPieceId = referenceBoard.getPlacement(row, col).getPieceId();
            int refRotation = referenceBoard.getPlacement(row, col).getRotation();

            if (currentPieceId == refPieceId && currentRotation == refRotation) {
                // Identical - STABILITY (Cyan)
                return BOLD_CYAN;
            } else {
                // Different piece - CHANGE (Orange)
                return BOLD_ORANGE;
            }
        }
    }

    /**
     * Returns empty string - comparison mode uses cell colors only.
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @param direction Edge direction
     * @return Empty string (no edge-specific coloring in comparison mode)
     */
    @Override
    public String getEdgeColor(Board board, int row, int col, int direction) {
        // Comparison mode doesn't use edge-specific colors
        // All coloring is done at cell level
        return "";
    }
}
