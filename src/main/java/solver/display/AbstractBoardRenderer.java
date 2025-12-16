package solver.display;

import model.Board;
import model.Piece;
import util.SolverLogger;

import java.util.List;
import java.util.Map;

/**
 * Abstract base class for board rendering using Template Method pattern.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Define rendering algorithm structure (template method)</li>
 *   <li>Implement common rendering logic (grid structure)</li>
 *   <li>Delegate cell-specific coloring to subclasses</li>
 * </ul>
 *
 * <h2>Design Pattern: Template Method</h2>
 * The render() method defines the skeleton of the rendering algorithm:
 * <ol>
 *   <li>Render header with column numbers</li>
 *   <li>Render top border</li>
 *   <li>For each row:
 *     <ul>
 *       <li>Render north edge line</li>
 *       <li>Render middle line (west + ID + east)</li>
 *       <li>Render south edge line</li>
 *       <li>Render row separator (if not last row)</li>
 *     </ul>
 *   </li>
 *   <li>Render bottom border</li>
 * </ol>
 *
 * Subclasses customize coloring by implementing abstract methods.
 *
 * <h2>Subclasses</h2>
 * <ul>
 *   <li>LabeledBoardRenderer: Edge matching + valid count colors</li>
 *   <li>ComparisonBoardRenderer: Comparison colors (regression/progress/stability/change)</li>
 * </ul>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public abstract class AbstractBoardRenderer {

    protected final Board board;
    protected final Map<Integer, Piece> piecesById;
    protected final List<Integer> unusedIds;
    protected final GridStructureRenderer gridRenderer;
    protected final ValidPieceCounter validPieceCounter;

    /**
     * Creates abstract board renderer.
     *
     * @param board Board to render
     * @param piecesById Map of all pieces by ID
     * @param unusedIds List of unused piece IDs
     * @param validPieceCounter Counter for valid pieces at empty positions
     */
    public AbstractBoardRenderer(Board board, Map<Integer, Piece> piecesById,
                                  List<Integer> unusedIds, ValidPieceCounter validPieceCounter) {
        this.board = board;
        this.piecesById = piecesById;
        this.unusedIds = unusedIds;
        this.gridRenderer = new GridStructureRenderer(board.getRows(), board.getCols());
        this.validPieceCounter = validPieceCounter;
    }

    /**
     * Template method: renders entire board using defined algorithm.
     * This method defines the structure; subclasses customize colors.
     */
    public final void render() {
        gridRenderer.renderHeader();
        gridRenderer.renderTopBorder();

        int rows = board.getRows();
        for (int r = 0; r < rows; r++) {
            renderRow(r);

            if (r < rows - 1) {
                gridRenderer.renderRowSeparator();
            }
        }

        gridRenderer.renderBottomBorder();
    }

    /**
     * Renders a single row (3 lines: north edge, middle, south edge).
     *
     * @param row Row index
     */
    protected void renderRow(int row) {
        renderNorthEdgeLine(row);
        renderMiddleLine(row);
        renderSouthEdgeLine(row);
    }

    /**
     * Renders north edge line for all cells in row.
     *
     * @param row Row index
     */
    protected void renderNorthEdgeLine(int row) {
        System.out.print("   │");
        int cols = board.getCols();

        for (int col = 0; col < cols; col++) {
            String cellContent = formatNorthEdge(row, col);
            System.out.print(cellContent);
            gridRenderer.renderCellBorder();
        }
        SolverLogger.info("");
    }

    /**
     * Renders middle line for all cells in row (includes row label).
     *
     * @param row Row index
     */
    protected void renderMiddleLine(int row) {
        char rowLabel = gridRenderer.getRowLabel(row);
        System.out.print(" " + rowLabel + " │");
        int cols = board.getCols();

        for (int col = 0; col < cols; col++) {
            String cellContent = formatMiddleContent(row, col);
            System.out.print(cellContent);
            gridRenderer.renderCellBorder();
        }
        SolverLogger.info("");
    }

    /**
     * Renders south edge line for all cells in row.
     *
     * @param row Row index
     */
    protected void renderSouthEdgeLine(int row) {
        System.out.print("   │");
        int cols = board.getCols();

        for (int col = 0; col < cols; col++) {
            String cellContent = formatSouthEdge(row, col);
            System.out.print(cellContent);
            gridRenderer.renderCellBorder();
        }
        SolverLogger.info("");
    }

    /**
     * Formats north edge for a cell.
     * Subclasses provide cell formatter with appropriate color strategy.
     *
     * @param row Row index
     * @param col Column index
     * @return Formatted string (9 characters)
     */
    protected abstract String formatNorthEdge(int row, int col);

    /**
     * Formats middle content for a cell (west + ID + east or valid count).
     * Subclasses provide cell formatter with appropriate color strategy.
     *
     * @param row Row index
     * @param col Column index
     * @return Formatted string (9 characters)
     */
    protected abstract String formatMiddleContent(int row, int col);

    /**
     * Formats south edge for a cell.
     * Subclasses provide cell formatter with appropriate color strategy.
     *
     * @param row Row index
     * @param col Column index
     * @return Formatted string (9 characters)
     */
    protected abstract String formatSouthEdge(int row, int col);
}
