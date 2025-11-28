package solver;

/**
 * Pre-calculated constraints for a cell position in the puzzle board.
 * Stores border information and required border values to avoid repeated calculations.
 */
public class CellConstraints {
    // Border flags (packed in a single byte for cache efficiency)
    public final byte borderMask;  // bits: 0=nord, 1=est, 2=sud, 3=ouest

    // Neighbor positions (row, column) - pre-calculated to avoid array lookups
    public final int northRow, northCol;
    public final int eastRow, eastCol;
    public final int southRow, southCol;
    public final int westRow, westCol;

    // Border requirements mask: if bit is set, this edge MUST be 0
    // bits: 0=nord, 1=est, 2=sud, 3=ouest
    public final byte requiredZeroMask;

    private CellConstraints(int row, int col, int rows, int cols) {
        // Calculate border flags
        boolean isNorthBorder = (row == 0);
        boolean isEastBorder = (col == cols - 1);
        boolean isSouthBorder = (row == rows - 1);
        boolean isWestBorder = (col == 0);

        this.borderMask = (byte) (
            (isNorthBorder ? 1 : 0) |
            (isEastBorder ? 2 : 0) |
            (isSouthBorder ? 4 : 0) |
            (isWestBorder ? 8 : 0)
        );

        this.requiredZeroMask = borderMask;

        // Pre-calculate neighbor positions
        this.northRow = row - 1;
        this.northCol = col;
        this.eastRow = row;
        this.eastCol = col + 1;
        this.southRow = row + 1;
        this.southCol = col;
        this.westRow = row;
        this.westCol = col - 1;
    }

    /**
     * Check if this cell is on the north border
     */
    public boolean isNorthBorder() {
        return (borderMask & 1) != 0;
    }

    /**
     * Check if this cell is on the east border
     */
    public boolean isEastBorder() {
        return (borderMask & 2) != 0;
    }

    /**
     * Check if this cell is on the south border
     */
    public boolean isSouthBorder() {
        return (borderMask & 4) != 0;
    }

    /**
     * Check if this cell is on the west border
     */
    public boolean isWestBorder() {
        return (borderMask & 8) != 0;
    }

    /**
     * Factory method to create a constraints matrix for a board
     */
    public static CellConstraints[][] createConstraintsMatrix(int rows, int cols) {
        CellConstraints[][] constraints = new CellConstraints[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                constraints[r][c] = new CellConstraints(r, c, rows, cols);
            }
        }
        return constraints;
    }
}
