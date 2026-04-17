package util;

import java.util.Objects;

/**
 * Immutable value object representing a position (row, col) on the board.
 *
 * <h2>Purpose</h2>
 * Eliminates string concatenation duplication ({@code row + "," + col}) across codebase.
 * Provides type-safe, efficient Map keys with proper {@code hashCode()} and {@code equals()}.
 *
 * <h2>Usage</h2>
 * <pre>
 * // As Map key
 * Map&lt;PositionKey, Integer&gt; placementOrder = new HashMap&lt;&gt;();
 * PositionKey pos = new PositionKey(2, 3);
 * placementOrder.put(pos, 15);
 *
 * // String representation
 * String key = pos.toString(); // "2,3"
 * </pre>
 *
 * <h2>Replaces</h2>
 * String-based keys like:
 * <ul>
 *   <li>{@code String posKey = row + "," + col;}</li>
 *   <li>{@code Map<String, Integer> map;}</li>
 * </ul>
 *
 * With type-safe alternative:
 * <ul>
 *   <li>{@code PositionKey posKey = new PositionKey(row, col);}</li>
 *   <li>{@code Map<PositionKey, Integer> map;}</li>
 * </ul>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public final class PositionKey {

    private final int row;
    private final int col;

    /**
     * Creates a position key for the given board coordinates.
     *
     * @param row Row index (0-based)
     * @param col Column index (0-based)
     */
    public PositionKey(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * Returns the row index.
     *
     * @return Row index
     */
    public int getRow() {
        return row;
    }

    /**
     * Returns the column index.
     *
     * @return Column index
     */
    public int getCol() {
        return col;
    }

    /**
     * Returns string representation in format "row,col".
     * Compatible with existing string-based keys.
     *
     * @return String representation
     */
    @Override
    public String toString() {
        return row + "," + col;
    }

    /**
     * Computes hash code based on row and col.
     * Optimized for use as Map key.
     *
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    /**
     * Checks equality based on row and col values.
     *
     * @param obj Object to compare
     * @return true if same row and col
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        PositionKey other = (PositionKey) obj;
        return row == other.row && col == other.col;
    }

    /**
     * Parses a position key from string format "row,col".
     *
     * @param posKeyString String in format "row,col"
     * @return PositionKey instance
     * @throws IllegalArgumentException if format is invalid
     */
    public static PositionKey parse(String posKeyString) {
        if (posKeyString == null || posKeyString.isEmpty()) {
            throw new IllegalArgumentException("Position key string cannot be null or empty");
        }

        String[] parts = posKeyString.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Invalid position key format: '" + posKeyString + "'. Expected format: 'row,col'"
            );
        }

        try {
            int row = Integer.parseInt(parts[0].trim());
            int col = Integer.parseInt(parts[1].trim());
            return new PositionKey(row, col);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Invalid position key format: '" + posKeyString + "'. Row and col must be integers.",
                e
            );
        }
    }
}
