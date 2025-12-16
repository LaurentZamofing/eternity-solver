package solver.display;

import model.Board;

import java.util.Set;

/**
 * Color strategy for edge matching visualization.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Color fixed positions in bright cyan</li>
 *   <li>Color matching edges in green</li>
 *   <li>Color non-matching edges in red</li>
 * </ul>
 *
 * <h2>Design</h2>
 * Uses EdgeMatchingValidator to check if edges match neighbors.
 * Fixed positions (hint pieces) are always highlighted in bright cyan.
 *
 * <h2>Color Scheme</h2>
 * <ul>
 *   <li>Bright Cyan: Fixed positions (hints)</li>
 *   <li>Green: Edges that match neighbors</li>
 *   <li>Red: Edges that don't match neighbors</li>
 * </ul>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class EdgeMatchingColorStrategy implements ColorStrategy {

    private final EdgeMatchingValidator validator;
    private final Set<String> fixedPositions;

    /**
     * Creates edge matching color strategy.
     *
     * @param board Board to validate
     * @param fixedPositions Set of fixed position keys ("row,col")
     */
    public EdgeMatchingColorStrategy(Board board, Set<String> fixedPositions) {
        this.validator = new EdgeMatchingValidator(board);
        this.fixedPositions = fixedPositions;
    }

    /**
     * Returns color for a cell.
     * Fixed positions get bright cyan, others get no cell color.
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @return BRIGHT_CYAN if fixed, empty string otherwise
     */
    @Override
    public String getCellColor(Board board, int row, int col) {
        String positionKey = row + "," + col;
        return fixedPositions.contains(positionKey) ? BRIGHT_CYAN : "";
    }

    /**
     * Returns color for an edge based on matching with neighbor.
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @param direction Edge direction (NORTH=0, EAST=1, SOUTH=2, WEST=3)
     * @return GREEN if matches, RED if doesn't match, empty if no neighbor
     */
    @Override
    public String getEdgeColor(Board board, int row, int col, int direction) {
        // Check if this is a fixed position - fixed positions always use cell color
        String positionKey = row + "," + col;
        if (fixedPositions.contains(positionKey)) {
            return ""; // Cell color (bright cyan) will be used instead
        }

        // Check edge matching based on direction
        switch (direction) {
            case EdgeMatchingValidator.NORTH:
                if (validator.hasNorthNeighbor(row, col)) {
                    return validator.northEdgeMatches(row, col) ? GREEN : RED;
                }
                break;

            case EdgeMatchingValidator.EAST:
                if (validator.hasEastNeighbor(row, col)) {
                    return validator.eastEdgeMatches(row, col) ? GREEN : RED;
                }
                break;

            case EdgeMatchingValidator.SOUTH:
                if (validator.hasSouthNeighbor(row, col)) {
                    return validator.southEdgeMatches(row, col) ? GREEN : RED;
                }
                break;

            case EdgeMatchingValidator.WEST:
                if (validator.hasWestNeighbor(row, col)) {
                    return validator.westEdgeMatches(row, col) ? GREEN : RED;
                }
                break;
        }

        // No neighbor in this direction - no color
        return "";
    }
}
