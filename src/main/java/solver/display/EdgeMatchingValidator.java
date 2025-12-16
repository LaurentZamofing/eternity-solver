package solver.display;

import model.Board;

/**
 * Validates edge matching between neighboring pieces.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Check if cell has neighbors in each direction</li>
 *   <li>Verify edge values match between neighbors</li>
 *   <li>Extract neighbor edge values</li>
 * </ul>
 *
 * <h2>Design</h2>
 * Extracted from BoardDisplayManager to eliminate 90% duplication.
 * Edge matching logic was repeated 4 times (once per direction).
 *
 * <h2>Edge Indices</h2>
 * Edge array format: [North=0, East=1, South=2, West=3]
 *
 * <h2>Usage</h2>
 * <pre>
 * EdgeMatchingValidator validator = new EdgeMatchingValidator(board);
 *
 * if (validator.hasNorthNeighbor(row, col)) {
 *     boolean matches = validator.northEdgeMatches(row, col);
 *     // Apply color based on match
 * }
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class EdgeMatchingValidator {

    private final Board board;

    /**
     * Edge direction constants for array indexing.
     */
    public static final int NORTH = 0;
    public static final int EAST = 1;
    public static final int SOUTH = 2;
    public static final int WEST = 3;

    /**
     * Creates edge matching validator for specified board.
     *
     * @param board Board to validate
     */
    public EdgeMatchingValidator(Board board) {
        this.board = board;
    }

    /**
     * Checks if cell has a placed neighbor to the north.
     *
     * @param row Row index
     * @param col Column index
     * @return true if north neighbor exists and is not empty
     */
    public boolean hasNorthNeighbor(int row, int col) {
        return row > 0 && !board.isEmpty(row - 1, col);
    }

    /**
     * Checks if cell has a placed neighbor to the south.
     *
     * @param row Row index
     * @param col Column index
     * @return true if south neighbor exists and is not empty
     */
    public boolean hasSouthNeighbor(int row, int col) {
        return row < board.getRows() - 1 && !board.isEmpty(row + 1, col);
    }

    /**
     * Checks if cell has a placed neighbor to the east.
     *
     * @param row Row index
     * @param col Column index
     * @return true if east neighbor exists and is not empty
     */
    public boolean hasEastNeighbor(int row, int col) {
        return col < board.getCols() - 1 && !board.isEmpty(row, col + 1);
    }

    /**
     * Checks if cell has a placed neighbor to the west.
     *
     * @param row Row index
     * @param col Column index
     * @return true if west neighbor exists and is not empty
     */
    public boolean hasWestNeighbor(int row, int col) {
        return col > 0 && !board.isEmpty(row, col - 1);
    }

    /**
     * Checks if north edge matches neighbor's south edge.
     *
     * @param row Row index
     * @param col Column index
     * @return true if edges match
     */
    public boolean northEdgeMatches(int row, int col) {
        if (!hasNorthNeighbor(row, col)) {
            return false; // No neighbor to match
        }

        int currentNorth = board.getPlacement(row, col).edges[NORTH];
        int neighborSouth = board.getPlacement(row - 1, col).edges[SOUTH];

        return currentNorth == neighborSouth;
    }

    /**
     * Checks if south edge matches neighbor's north edge.
     *
     * @param row Row index
     * @param col Column index
     * @return true if edges match
     */
    public boolean southEdgeMatches(int row, int col) {
        if (!hasSouthNeighbor(row, col)) {
            return false; // No neighbor to match
        }

        int currentSouth = board.getPlacement(row, col).edges[SOUTH];
        int neighborNorth = board.getPlacement(row + 1, col).edges[NORTH];

        return currentSouth == neighborNorth;
    }

    /**
     * Checks if east edge matches neighbor's west edge.
     *
     * @param row Row index
     * @param col Column index
     * @return true if edges match
     */
    public boolean eastEdgeMatches(int row, int col) {
        if (!hasEastNeighbor(row, col)) {
            return false; // No neighbor to match
        }

        int currentEast = board.getPlacement(row, col).edges[EAST];
        int neighborWest = board.getPlacement(row, col + 1).edges[WEST];

        return currentEast == neighborWest;
    }

    /**
     * Checks if west edge matches neighbor's east edge.
     *
     * @param row Row index
     * @param col Column index
     * @return true if edges match
     */
    public boolean westEdgeMatches(int row, int col) {
        if (!hasWestNeighbor(row, col)) {
            return false; // No neighbor to match
        }

        int currentWest = board.getPlacement(row, col).edges[WEST];
        int neighborEast = board.getPlacement(row, col - 1).edges[EAST];

        return currentWest == neighborEast;
    }
}
