package solver.display;

import model.Board;
import util.PositionKey;

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
    private final Set<PositionKey> fixedPositions;
    private final Set<PositionKey> highlightedPositions;
    private final Set<PositionKey> nextCellPositions;
    private final Set<PositionKey> removedCellPositions;

    /**
     * Creates edge matching color strategy.
     *
     * @param board Board to validate
     * @param fixedPositions Set of fixed position keys (PositionKey)
     */
    public EdgeMatchingColorStrategy(Board board, Set<PositionKey> fixedPositions) {
        this(board, fixedPositions, null, null);
    }

    /**
     * Creates edge matching color strategy with highlighted positions.
     *
     * @param board Board to validate
     * @param fixedPositions Set of fixed position keys (PositionKey)
     * @param highlightedPositions Set of positions to highlight (PositionKey) - shown in magenta
     */
    public EdgeMatchingColorStrategy(Board board, Set<PositionKey> fixedPositions, Set<PositionKey> highlightedPositions) {
        this(board, fixedPositions, highlightedPositions, null);
    }

    /**
     * Creates edge matching color strategy with highlighted and next cell positions.
     *
     * @param board Board to validate
     * @param fixedPositions Set of fixed position keys (PositionKey)
     * @param highlightedPositions Set of positions to highlight (PositionKey) - shown in magenta
     * @param nextCellPositions Set of next cells to process (PositionKey) - shown in blue
     */
    public EdgeMatchingColorStrategy(Board board, Set<PositionKey> fixedPositions,
                                     Set<PositionKey> highlightedPositions, Set<PositionKey> nextCellPositions) {
        this(board, fixedPositions, highlightedPositions, nextCellPositions, null);
    }

    /**
     * Creates edge matching color strategy with highlighted, next cell, and removed cell positions.
     *
     * @param board Board to validate
     * @param fixedPositions Set of fixed position keys (PositionKey)
     * @param highlightedPositions Set of positions to highlight (PositionKey) - shown in magenta
     * @param nextCellPositions Set of next cells to process (PositionKey) - shown in blue
     * @param removedCellPositions Set of removed cells during backtrack (PositionKey) - shown in red with ⬅
     */
    public EdgeMatchingColorStrategy(Board board, Set<PositionKey> fixedPositions,
                                     Set<PositionKey> highlightedPositions, Set<PositionKey> nextCellPositions,
                                     Set<PositionKey> removedCellPositions) {
        this.validator = new EdgeMatchingValidator(board);
        this.fixedPositions = fixedPositions;
        this.highlightedPositions = highlightedPositions;
        this.nextCellPositions = nextCellPositions;
        this.removedCellPositions = removedCellPositions;
    }

    /**
     * Returns color for a cell.
     * Priority: Removed (red) > Next cell (blue) > Highlighted (magenta) > Fixed (cyan).
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @return Color code based on cell type
     */
    @Override
    public String getCellColor(Board board, int row, int col) {
        PositionKey positionKey = new PositionKey(row, col);

        // Removed cell during backtrack gets bright red (highest priority)
        if (removedCellPositions != null && removedCellPositions.contains(positionKey)) {
            return BRIGHT_RED;
        }

        // Next cell to process gets bright blue
        if (nextCellPositions != null && nextCellPositions.contains(positionKey)) {
            return BRIGHT_BLUE;
        }

        // Highlighted positions (last placed piece) get bright magenta
        if (highlightedPositions != null && highlightedPositions.contains(positionKey)) {
            return BRIGHT_MAGENTA;
        }

        // Fixed positions get bright cyan
        return (fixedPositions != null && fixedPositions.contains(positionKey)) ? BRIGHT_CYAN : "";
    }

    /**
     * Returns color for an edge based on matching with neighbor.
     * Priority: Removed (red) > Next cell (blue) > Highlighted (magenta) > Edge validation (green/red).
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @param direction Edge direction (NORTH=0, EAST=1, SOUTH=2, WEST=3)
     * @return Color code for the edge
     */
    @Override
    public String getEdgeColor(Board board, int row, int col, int direction) {
        PositionKey positionKey = new PositionKey(row, col);

        // Removed cell during backtrack gets bright red edges (highest priority)
        if (removedCellPositions != null && removedCellPositions.contains(positionKey)) {
            return BRIGHT_RED;
        }

        // Next cell to process gets bright blue edges
        if (nextCellPositions != null && nextCellPositions.contains(positionKey)) {
            return BRIGHT_BLUE;
        }

        // Highlighted positions (last placed piece) get bright magenta edges
        if (highlightedPositions != null && highlightedPositions.contains(positionKey)) {
            return BRIGHT_MAGENTA;
        }

        // Fixed positions always use cell color (bright cyan)
        if (fixedPositions != null && fixedPositions.contains(positionKey)) {
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
