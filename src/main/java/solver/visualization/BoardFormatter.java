package solver.visualization;

/**
 * Interface for board visualization formatters.
 *
 * Implementations provide different visualization styles for puzzle boards:
 * - CompactBoardFormatter: Minimal ASCII representation
 * - DetailedBoardFormatter: Enhanced view with piece counts and highlighting
 * - LabeledBoardFormatter: Coordinate labels with edge validation
 * - ComparisonBoardFormatter: Side-by-side comparison with color coding
 *
 * All formatters output directly to System.out using ANSI color codes.
 *
 * Usage:
 * <pre>
 * BoardFormatter formatter = new DetailedBoardFormatter();
 * FormatterContext ctx = FormatterContext.builder()
 *     .board(board)
 *     .pieces(pieces)
 *     .fitsChecker(fitsChecker)
 *     .build();
 * formatter.format(ctx);
 * </pre>
 */
@FunctionalInterface
public interface BoardFormatter {

    /**
     * Formats and displays the board to System.out.
     *
     * @param context Context object containing board, pieces, and formatting options
     * @throws IllegalArgumentException if required context fields are missing
     */
    void format(FormatterContext context);
}
