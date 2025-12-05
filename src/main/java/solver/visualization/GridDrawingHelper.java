package solver.visualization;

/**
 * Utility class for grid and box drawing operations used in board visualization.
 *
 * Provides:
 * - Unicode box-drawing characters
 * - Grid line generation
 * - Cell content formatting
 * - Header and separator generation
 *
 * All methods are static and thread-safe.
 */
public class GridDrawingHelper {

    // ===== Box Drawing Characters =====

    /** Horizontal line character: ─ */
    public static final String HORIZONTAL = "─";

    /** Vertical line character: │ */
    public static final String VERTICAL = "│";

    /** Cross/intersection character: ┼ */
    public static final String CROSS = "┼";

    /** Top-left corner: ┌ */
    public static final String TOP_LEFT = "┌";

    /** Top-right corner: ┐ */
    public static final String TOP_RIGHT = "┐";

    /** Bottom-left corner: └ */
    public static final String BOTTOM_LEFT = "└";

    /** Bottom-right corner: ┘ */
    public static final String BOTTOM_RIGHT = "┘";

    /** T-junction pointing down: ┬ */
    public static final String T_DOWN = "┬";

    /** T-junction pointing up: ┴ */
    public static final String T_UP = "┴";

    /** T-junction pointing right: ├ */
    public static final String T_RIGHT = "├";

    /** T-junction pointing left: ┤ */
    public static final String T_LEFT = "┤";

    // ===== ASCII Alternative Characters (for compatibility) =====

    /** Simple horizontal line: - */
    public static final String ASCII_HORIZONTAL = "-";

    /** Simple vertical line: | */
    public static final String ASCII_VERTICAL = "|";

    /** Simple cross: + */
    public static final String ASCII_CROSS = "+";

    // ===== Grid Generation Methods =====

    /**
     * Generates a horizontal line with specified width.
     *
     * @param width Number of columns
     * @param cellWidth Width of each cell in characters
     * @param useCrosses True to add crosses between cells, false for solid line
     * @return Formatted horizontal line string
     */
    public static String horizontalLine(int width, int cellWidth, boolean useCrosses) {
        StringBuilder sb = new StringBuilder();
        sb.append("   ").append(HORIZONTAL);  // Left margin + start

        for (int i = 0; i < width; i++) {
            // Cell separator
            for (int j = 0; j < cellWidth; j++) {
                sb.append(HORIZONTAL);
            }
            // Add cross or horizontal between cells
            if (i < width - 1) {
                sb.append(useCrosses ? CROSS : HORIZONTAL);
            }
        }

        return sb.toString();
    }

    /**
     * Generates a simple horizontal line (no crosses).
     *
     * @param width Number of columns
     * @param cellWidth Width of each cell in characters
     * @return Formatted horizontal line string
     */
    public static String horizontalLine(int width, int cellWidth) {
        return horizontalLine(width, cellWidth, false);
    }

    /**
     * Generates an ASCII horizontal separator (dashes).
     *
     * @param width Number of columns
     * @param cellWidth Width of each cell in characters
     * @return Formatted ASCII line string
     */
    public static String asciiHorizontalLine(int width, int cellWidth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < cellWidth; j++) {
                sb.append(ASCII_HORIZONTAL);
            }
        }
        return sb.toString();
    }

    /**
     * Generates column header with numbers.
     * Example: "     1     2     3   "
     *
     * @param cols Number of columns
     * @param cellWidth Width of each cell
     * @return Formatted header string
     */
    public static String columnHeader(int cols, int cellWidth) {
        StringBuilder sb = new StringBuilder();
        sb.append("     ");  // Left margin for row labels

        for (int c = 0; c < cols; c++) {
            // Right-align column number within cell
            String colNum = String.format("%2d", (c + 1));
            int padding = (cellWidth - colNum.length()) / 2;

            for (int i = 0; i < padding; i++) {
                sb.append(" ");
            }
            sb.append(colNum);
            for (int i = 0; i < cellWidth - padding - colNum.length(); i++) {
                sb.append(" ");
            }

            if (c < cols - 1) {
                sb.append(" ");  // Space between columns
            }
        }

        return sb.toString();
    }

    /**
     * Generates row label (A, B, C, etc.).
     *
     * @param row Row index (0-based)
     * @return Row label character (A-Z)
     */
    public static char rowLabel(int row) {
        return (char) ('A' + row);
    }

    /**
     * Formats a cell with borders and content.
     *
     * @param content Cell content string
     * @param cellWidth Total width including borders
     * @param color Optional ANSI color code (can be null or empty)
     * @return Formatted cell string with borders
     */
    public static String formatCell(String content, int cellWidth, String color) {
        StringBuilder sb = new StringBuilder();

        sb.append(VERTICAL);

        // Apply color if provided
        if (color != null && !color.isEmpty()) {
            sb.append(color);
        }

        // Center content within cell
        int contentLen = content.length();
        int padding = (cellWidth - contentLen - 2) / 2;  // -2 for borders

        for (int i = 0; i < padding; i++) {
            sb.append(" ");
        }
        sb.append(content);
        for (int i = 0; i < cellWidth - contentLen - padding - 2; i++) {
            sb.append(" ");
        }

        // Reset color if was applied
        if (color != null && !color.isEmpty()) {
            sb.append(AnsiColorHelper.RESET);
        }

        return sb.toString();
    }

    /**
     * Formats a cell without color.
     *
     * @param content Cell content string
     * @param cellWidth Total width including borders
     * @return Formatted cell string with borders
     */
    public static String formatCell(String content, int cellWidth) {
        return formatCell(content, cellWidth, null);
    }

    /**
     * Formats edge value for display.
     * Shows empty string for 0, otherwise formats as 2-digit number.
     *
     * @param edge Edge value (0-99)
     * @return Formatted string ("  " for 0, "%2d" otherwise)
     */
    public static String formatEdge(int edge) {
        if (edge == 0) {
            return "  ";
        }
        return String.format("%2d", edge);
    }

    /**
     * Formats piece count for display in empty cells.
     *
     * @param count Number of valid pieces
     * @param maxDigits Maximum digits to display (e.g., 3 for "999+")
     * @return Formatted string ("X" for 0, "999+" for overflow, "%3d" otherwise)
     */
    public static String formatPieceCount(int count, int maxDigits) {
        if (count == 0) {
            return "X";  // Dead end
        }

        int maxValue = (int) Math.pow(10, maxDigits) - 1;
        if (count > maxValue) {
            return maxValue + "+";  // Overflow indicator
        }

        return String.format("%" + maxDigits + "d", count);
    }

    /**
     * Creates padding string with specified number of spaces.
     *
     * @param length Number of spaces
     * @return String containing specified number of spaces
     */
    public static String spaces(int length) {
        if (length <= 0) {
            return "";
        }
        return " ".repeat(length);
    }

    /**
     * Creates a horizontal line of specified character and length.
     *
     * @param ch Character to repeat
     * @param length Number of repetitions
     * @return String containing repeated character
     */
    public static String repeat(String ch, int length) {
        if (length <= 0 || ch == null || ch.isEmpty()) {
            return "";
        }
        return ch.repeat(length);
    }

    // Private constructor to prevent instantiation
    private GridDrawingHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
