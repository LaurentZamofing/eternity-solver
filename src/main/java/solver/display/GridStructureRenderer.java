package solver.display;

/**
 * Renders ASCII grid structure for board visualization.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Column header rendering (numbered columns)</li>
 *   <li>Row labels (A, B, C, ...)</li>
 *   <li>Border lines (top, bottom, separators)</li>
 *   <li>Cell boundaries (vertical bars)</li>
 * </ul>
 *
 * <h2>Design</h2>
 * Extracted from BoardDisplayManager to eliminate duplication.
 * Grid structure was repeated 5+ times across display methods.
 *
 * <h2>Usage</h2>
 * <pre>
 * GridStructureRenderer grid = new GridStructureRenderer(16, 16);
 * grid.renderHeader();
 * grid.renderTopBorder();
 * // ... render rows ...
 * grid.renderRowSeparator();
 * // ... more rows ...
 * grid.renderBottomBorder();
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class GridStructureRenderer {

    private final int rows;
    private final int cols;

    /**
     * Creates grid structure renderer for specified dimensions.
     *
     * @param rows Number of rows
     * @param cols Number of columns
     */
    public GridStructureRenderer(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    /**
     * Renders column header with numbers (1, 2, 3, ...).
     */
    public void renderHeader() {
        System.out.print("     ");
        for (int c = 0; c < cols; c++) {
            System.out.printf("  %2d     ", (c + 1));
            if (c < cols - 1) System.out.print(" ");
        }
        System.out.println();
    }

    /**
     * Renders top border line.
     */
    public void renderTopBorder() {
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("─");
        }
        System.out.println();
    }

    /**
     * Renders row separator line (between rows).
     */
    public void renderRowSeparator() {
        System.out.print("   ├");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("┼");
        }
        System.out.println("┤");
    }

    /**
     * Renders bottom border line.
     */
    public void renderBottomBorder() {
        System.out.print("   └");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("┴");
        }
        System.out.println("┘");
    }

    /**
     * Returns row label for given row index (A, B, C, ...).
     *
     * @param row Row index (0-based)
     * @return Row label character
     */
    public char getRowLabel(int row) {
        return (char) ('A' + row);
    }

    /**
     * Renders cell border (vertical bar).
     */
    public void renderCellBorder() {
        System.out.print("│");
    }
}
