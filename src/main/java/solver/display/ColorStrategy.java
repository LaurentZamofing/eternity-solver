package solver.display;

import model.Board;

/**
 * Strategy interface for cell and edge coloring.
 *
 * <h2>Strategy Pattern</h2>
 * Allows different color schemes for different display modes:
 * - Edge matching colors (green/red)
 * - Comparison colors (magenta/yellow/cyan/orange)
 * - Valid count colors (red for deadends, yellow for critical)
 *
 * <h2>ANSI Color Codes</h2>
 * Colors use ANSI escape codes for terminal display.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public interface ColorStrategy {

    /**
     * Gets color code for a cell.
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @return ANSI color code or empty string for no color
     */
    String getCellColor(Board board, int row, int col);

    /**
     * Gets color code for an edge.
     *
     * @param board Current board
     * @param row Row index
     * @param col Column index
     * @param direction Edge direction (NORTH=0, EAST=1, SOUTH=2, WEST=3)
     * @return ANSI color code or empty string for no color
     */
    String getEdgeColor(Board board, int row, int col, int direction);

    /** ANSI color codes */
    String GREEN = "\033[32m";
    String RED = "\033[91m";
    String BRIGHT_RED = "\033[1;91m";
    String YELLOW = "\033[33m";
    String BRIGHT_YELLOW = "\033[93m";
    String BOLD_YELLOW = "\033[1;33m";
    String CYAN = "\033[96m";
    String BRIGHT_CYAN = "\033[1;96m";
    String BOLD_CYAN = "\033[1;36m";
    String BLUE = "\033[34m";
    String BRIGHT_BLUE = "\033[94m";
    String BOLD_BLUE = "\033[1;34m";
    String MAGENTA = "\033[95m";
    String BOLD_MAGENTA = "\033[1;35m";
    String BRIGHT_MAGENTA = "\033[1;95m";
    String ORANGE = "\033[38;5;208m";
    String BOLD_ORANGE = "\033[1;38;5;208m";
    String BOLD = "\033[1m";
    String RESET = "\033[0m";
}
