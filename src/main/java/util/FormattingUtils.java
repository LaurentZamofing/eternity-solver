package util;

/**
 * Utility methods for formatting output across Main classes.
 * Provides consistent formatting for durations, headers, and visual elements.
 *
 * Extracted from Main*, MainSequential, and MainParallel to eliminate duplication.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class FormattingUtils {

    // Box drawing characters
    private static final String HORIZONTAL_LINE = "═";
    private static final String VERTICAL_LINE = "║";
    private static final String TOP_LEFT = "╔";
    private static final String TOP_RIGHT = "╗";
    private static final String BOTTOM_LEFT = "╚";
    private static final String BOTTOM_RIGHT = "╝";

    /**
     * Formats a duration in milliseconds to human-readable format.
     * Automatically chooses appropriate unit (ms, s, min, h).
     *
     * @param ms Duration in milliseconds
     * @return Formatted string (e.g., "1.50 s", "3.25 min", "2.00 h")
     */
    public static String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + " ms";
        } else if (ms < 60000) {
            return String.format("%.2f s", ms / 1000.0);
        } else if (ms < 3600000) {
            return String.format("%.2f min", ms / 60000.0);
        } else {
            return String.format("%.2f h", ms / 3600000.0);
        }
    }

    /**
     * Formats a duration in milliseconds to hours:minutes:seconds format.
     *
     * @param ms Duration in milliseconds
     * @return Formatted string (e.g., "2h 15m 30s")
     */
    public static String formatDurationHMS(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Prints a centered header with a box border.
     *
     * @param title Text to display in the header
     * @param width Total width of the box (default: 60 if width <= 0)
     */
    public static void printHeader(String title, int width) {
        if (width <= 0) {
            width = 60;
        }

        String top = TOP_LEFT + HORIZONTAL_LINE.repeat(width - 2) + TOP_RIGHT;
        String bottom = BOTTOM_LEFT + HORIZONTAL_LINE.repeat(width - 2) + BOTTOM_RIGHT;

        int padding = (width - 2 - title.length()) / 2;
        int rightPadding = width - 2 - title.length() - padding;

        String middle = VERTICAL_LINE + " ".repeat(padding) + title + " ".repeat(rightPadding) + VERTICAL_LINE;

        System.out.println(top);
        System.out.println(middle);
        System.out.println(bottom);
    }

    /**
     * Prints a centered header with default width (60 characters).
     *
     * @param title Text to display in the header
     */
    public static void printHeader(String title) {
        printHeader(title, 60);
    }

    /**
     * Prints a separator line.
     *
     * @param width Width of the separator
     */
    public static void printSeparator(int width) {
        System.out.println(HORIZONTAL_LINE.repeat(width));
    }

    /**
     * Prints a separator line with default width (60 characters).
     */
    public static void printSeparator() {
        printSeparator(60);
    }

    /**
     * Prints a simple box with text lines inside.
     *
     * @param lines Text lines to display in the box
     */
    public static void printBox(String... lines) {
        if (lines.length == 0) {
            return;
        }

        // Find max line length
        int maxLength = 0;
        for (String line : lines) {
            if (line.length() > maxLength) {
                maxLength = line.length();
            }
        }

        int width = maxLength + 4; // 2 spaces padding on each side
        String top = TOP_LEFT + HORIZONTAL_LINE.repeat(width - 2) + TOP_RIGHT;
        String bottom = BOTTOM_LEFT + HORIZONTAL_LINE.repeat(width - 2) + BOTTOM_RIGHT;

        System.out.println(top);
        for (String line : lines) {
            int rightPadding = maxLength - line.length();
            System.out.println(VERTICAL_LINE + "  " + line + " ".repeat(rightPadding) + "  " + VERTICAL_LINE);
        }
        System.out.println(bottom);
    }

    /**
     * Formats a percentage value.
     *
     * @param value Percentage value (0-100)
     * @param decimals Number of decimal places
     * @return Formatted string with % symbol
     */
    public static String formatPercentage(double value, int decimals) {
        String format = "%." + decimals + "f%%";
        return String.format(format, value);
    }

    /**
     * Formats a large number with thousands separators.
     *
     * @param number Number to format
     * @return Formatted string (e.g., "1,234,567")
     */
    public static String formatNumber(long number) {
        return String.format("%,d", number);
    }

    /**
     * Prints a progress bar.
     *
     * @param current Current progress value
     * @param total Total value for 100%
     * @param width Width of the progress bar in characters
     */
    public static void printProgressBar(int current, int total, int width) {
        double percentage = (double) current / total;
        int filled = (int) (width * percentage);
        int empty = width - filled;

        String bar = "█".repeat(filled) + "░".repeat(empty);
        System.out.printf("\r[%s] %d/%d (%.1f%%)    ", bar, current, total, percentage * 100);
        System.out.flush();
    }
}
