package solver;

import model.Board;
import model.Placement;

/**
 * Manages internal state for the Eternity solver during execution.
 * Tracks step count, last placed position, and provides state queries.
 *
 * Extracted from EternitySolver (Refactoring #11) to separate concerns
 * and reduce the god class anti-pattern.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class SolverStateManager {

    private int stepCount = 0;
    private int lastPlacedRow = -1;
    private int lastPlacedCol = -1;

    /**
     * Gets the current step count.
     * @return number of steps taken
     */
    public int getStepCount() {
        return stepCount;
    }

    /**
     * Increments the step count by 1.
     */
    public void incrementStepCount() {
        stepCount++;
    }

    /**
     * Resets the step count to 0.
     */
    public void resetStepCount() {
        stepCount = 0;
    }

    /**
     * Sets the last placed position.
     * @param row row index
     * @param col column index
     */
    public void setLastPlaced(int row, int col) {
        this.lastPlacedRow = row;
        this.lastPlacedCol = col;
    }

    /**
     * Gets the row of the last placed piece.
     * @return row index, or -1 if none placed
     */
    public int getLastPlacedRow() {
        return lastPlacedRow;
    }

    /**
     * Gets the column of the last placed piece.
     * @return column index, or -1 if none placed
     */
    public int getLastPlacedCol() {
        return lastPlacedCol;
    }

    /**
     * Scans the board to find and set the last placed position.
     * Useful after loading from a saved state.
     *
     * @param board Board to scan
     */
    public void findAndSetLastPlaced(Board board) {
        int rows = board.getRows();
        int cols = board.getCols();

        // Scan from bottom-right to top-left to find last placed piece
        for (int r = rows - 1; r >= 0; r--) {
            for (int c = cols - 1; c >= 0; c--) {
                if (!board.isEmpty(r, c)) {
                    setLastPlaced(r, c);
                    return;
                }
            }
        }

        // No pieces found - reset to -1
        setLastPlaced(-1, -1);
    }

    /**
     * Clears all state (resets to initial values).
     */
    public void reset() {
        stepCount = 0;
        lastPlacedRow = -1;
        lastPlacedCol = -1;
    }
}
