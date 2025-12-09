package solver.visualization;

import util.SolverLogger;

/**
 * Utility class providing ANSI color codes and text formatting for terminal output.
 *
 * This class centralizes all ANSI escape sequences used for:
 * - Text formatting (bold, underline, reset)
 * - Standard colors (red, green, yellow, blue, cyan, magenta)
 * - Bright colors (high-intensity variants)
 * - 256-color palette (extended colors like orange)
 *
 * Usage:
 * <pre>
 * SolverLogger.info(AnsiColorHelper.RED + "Error!" + AnsiColorHelper.RESET);
 * System.out.println(AnsiColorHelper.colorize("Success", AnsiColorHelper.GREEN));
 * </pre>
 *
 * All methods are static and thread-safe.
 */
public class AnsiColorHelper {

    // ===== Text Formatting =====

    /** Resets all text formatting and colors to default. */
    public static final String RESET = "\033[0m";

    /** Makes text bold/bright. */
    public static final String BOLD = "\033[1m";

    /** Underlines text. */
    public static final String UNDERLINE = "\033[4m";

    // ===== Standard Colors (Normal Intensity) =====

    /** Black text color. */
    public static final String BLACK = "\033[30m";

    /** Red text color - typically used for errors or dead ends. */
    public static final String RED = "\033[31m";

    /** Green text color - typically used for success or valid states. */
    public static final String GREEN = "\033[32m";

    /** Yellow text color - typically used for warnings. */
    public static final String YELLOW = "\033[33m";

    /** Blue text color. */
    public static final String BLUE = "\033[34m";

    /** Magenta text color - typically used for regressions. */
    public static final String MAGENTA = "\033[35m";

    /** Cyan text color - typically used for highlighting or stability. */
    public static final String CYAN = "\033[36m";

    /** White text color. */
    public static final String WHITE = "\033[37m";

    // ===== Bright Colors (High Intensity) =====

    /** Bright red text - high contrast error or dead end indicator. */
    public static final String BRIGHT_RED = "\033[91m";

    /** Bright green text - high contrast success indicator. */
    public static final String BRIGHT_GREEN = "\033[92m";

    /** Bright yellow text - high contrast warning. */
    public static final String BRIGHT_YELLOW = "\033[93m";

    /** Bright blue text. */
    public static final String BRIGHT_BLUE = "\033[94m";

    /** Bright magenta text - high contrast regression indicator. */
    public static final String BRIGHT_MAGENTA = "\033[95m";

    /** Bright cyan text - high contrast highlighting. */
    public static final String BRIGHT_CYAN = "\033[96m";

    /** Bright white text. */
    public static final String BRIGHT_WHITE = "\033[97m";

    // ===== Combination Codes (Bold + Color) =====

    /** Bold bright red - critical errors. */
    public static final String BOLD_BRIGHT_RED = "\033[1;91m";

    /** Bold bright yellow - critical warnings. */
    public static final String BOLD_BRIGHT_YELLOW = "\033[1;93m";

    /** Bold bright cyan - highlighted important elements. */
    public static final String BOLD_BRIGHT_CYAN = "\033[1;96m";

    /** Bold bright magenta - highlighted regressions. */
    public static final String BOLD_BRIGHT_MAGENTA = "\033[1;35m";

    // ===== 256-Color Palette =====

    /** Orange color (256-color palette: color 208). */
    public static final String ORANGE = "\033[1;38;5;208m";

    // ===== Utility Methods =====

    /**
     * Wraps text with specified color and automatically resets.
     *
     * @param text The text to colorize
     * @param color The ANSI color code (e.g., AnsiColorHelper.RED)
     * @return Colored text with RESET appended
     */
    public static String colorize(String text, String color) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (color == null || color.isEmpty()) {
            return text;
        }
        return color + text + RESET;
    }

    /**
     * Formats text with bold style.
     *
     * @param text The text to make bold
     * @return Bold text with RESET appended
     */
    public static String bold(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return BOLD + text + RESET;
    }

    /**
     * Gets color code based on count thresholds for piece availability.
     * Used to visually indicate constraint propagation status.
     *
     * @param count Number of valid pieces for a position
     * @return ANSI color code:
     *         - BOLD_BRIGHT_RED if count == 0 (dead end!)
     *         - BRIGHT_YELLOW if count <= 5 (critical)
     *         - YELLOW if count <= 20 (warning)
     *         - Empty string otherwise (normal)
     */
    public static String getCountColor(int count) {
        if (count == 0) {
            return BOLD_BRIGHT_RED;     // Dead end - critical!
        } else if (count <= 5) {
            return BRIGHT_YELLOW;        // Very constrained - critical
        } else if (count <= 20) {
            return YELLOW;               // Constrained - warning
        }
        return "";                       // Normal - no special color
    }

    /**
     * Gets color for edge matching visualization.
     *
     * @param matches True if edge matches neighbor, false otherwise
     * @return GREEN if matches, BRIGHT_RED if doesn't match
     */
    public static String getEdgeMatchColor(boolean matches) {
        return matches ? GREEN : BRIGHT_RED;
    }

    /**
     * Gets color for board comparison visualization.
     *
     * @param currentEmpty True if current cell is empty
     * @param refEmpty True if reference cell is empty
     * @param sameContent True if both cells have same content (when both occupied)
     * @return ANSI color code:
     *         - Empty string if both empty (no change)
     *         - BOLD_BRIGHT_MAGENTA if regression (was occupied, now empty)
     *         - BOLD + YELLOW if progress (was empty, now occupied)
     *         - BOLD_BRIGHT_CYAN if stable (same content)
     *         - ORANGE if changed (different content)
     */
    public static String getComparisonColor(boolean currentEmpty, boolean refEmpty, boolean sameContent) {
        if (refEmpty && currentEmpty) {
            return "";  // Both empty - no special color
        } else if (!refEmpty && currentEmpty) {
            return BOLD_BRIGHT_MAGENTA;  // Regression
        } else if (refEmpty && !currentEmpty) {
            return BOLD + YELLOW;  // Progress
        } else {
            return sameContent ? BOLD_BRIGHT_CYAN : ORANGE;  // Stable or Changed
        }
    }

    // Private constructor to prevent instantiation
    private AnsiColorHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
