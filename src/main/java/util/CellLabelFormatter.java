package util;

/**
 * Utility class for formatting cell labels in chess-like notation.
 * 
 * Converts (row, col) coordinates to human-readable labels like "A1", "B2", "P16".
 * - Rows are labeled A-Z (A=0, B=1, ..., P=15 for 16x16 board)
 * - Columns are labeled 1-16 (1-indexed)
 * 
 * Example: (0, 0) → "A1", (7, 8) → "H9", (15, 15) → "P16"
 */
public class CellLabelFormatter {

    /**
     * Formats a cell position as a chess-like label.
     * 
     * @param row row index (0-based)
     * @param col column index (0-based)
     * @return formatted label (e.g., "A1", "H9", "P16")
     */
    public static String format(int row, int col) {
        char rowLabel = (char) ('A' + row);
        int colLabel = col + 1;
        return String.valueOf(rowLabel) + colLabel;
    }

    /**
     * Formats a cell position with additional context.
     * 
     * @param row row index (0-based)
     * @param col column index (0-based)
     * @param includeCoordinates whether to include numeric coordinates
     * @return formatted label with optional coordinates (e.g., "A1 (0,0)")
     */
    public static String formatWithCoordinates(int row, int col, boolean includeCoordinates) {
        String label = format(row, col);
        if (includeCoordinates) {
            return label + " (" + row + "," + col + ")";
        }
        return label;
    }
}
