package model;

import util.SolverLogger;

import java.util.Arrays;
import java.util.Map;

/**
 * Represents the puzzle game board.
 * Manages piece placement and removal.
 */
public class Board {
    private final int rows;
    private final int cols;
    private final Placement[][] grid;

    /**
     * Constructor
     * @param rows number of rows
     * @param cols number of columns
     */
    public Board(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("rows and cols must be positive");
        }
        this.rows = rows;
        this.cols = cols;
        this.grid = new Placement[rows][cols];
    }

    /**
     * Returns the number of rows.
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns the number of columns.
     */
    public int getCols() {
        return cols;
    }

    /**
     * Checks if a cell is empty.
     * @param r row
     * @param c column
     * @return true if the cell is empty
     */
    public boolean isEmpty(int r, int c) {
        validateCoordinates(r, c);
        return grid[r][c] == null;
    }

    /**
     * Returns the placement at position (r, c).
     * @param r row
     * @param c column
     * @return the placement or null if empty
     */
    public Placement getPlacement(int r, int c) {
        validateCoordinates(r, c);
        return grid[r][c];
    }

    /**
     * Places a piece at position (r, c) with a given rotation.
     * @param r row
     * @param c column
     * @param piece piece to place
     * @param rotation rotation to apply (0-3)
     */
    public void place(int r, int c, Piece piece, int rotation) {
        validateCoordinates(r, c);
        int[] edges = piece.edgesRotated(rotation);
        grid[r][c] = new Placement(piece.getId(), rotation, edges);
    }

    /**
     * Removes the piece at position (r, c).
     * @param r row
     * @param c column
     */
    public void remove(int r, int c) {
        validateCoordinates(r, c);
        grid[r][c] = null;
    }

    /**
     * Displays a simple representation of the board.
     * @param piecesById map of pieces by ID (optional for detailed display)
     */
    public void prettyPrint(Map<Integer, Piece> piecesById) {
        SolverLogger.info("Board " + rows + "x" + cols + ":");
        for (int r = 0; r < rows; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == null) {
                    sb.append("---- ");
                } else {
                    sb.append(String.format("%s ", grid[r][c].toString()));
                }
            }
            SolverLogger.info(sb.toString());
        }
        SolverLogger.info("");

        if (piecesById != null) {
            SolverLogger.info("Details (coord -> id,rot,edges after rotation):");
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Placement p = grid[r][c];
                    if (p != null) {
                        SolverLogger.info(String.format("(%d,%d) => id=%d, rot=%d, edges=%s",
                                r, c, p.getPieceId(), p.getRotation(), Arrays.toString(p.edges)));
                    }
                }
            }
            SolverLogger.info("");
        }
    }

    /**
     * Calculates the board score based on the number of correct internal edges.
     *
     * Score = number of matched internal edges (borders not counted as mandatory)
     *
     * For a 16x16 board:
     * - Internal horizontal edges: (cols-1) × rows = 15 × 16 = 240
     * - Internal vertical edges: cols × (rows-1) = 16 × 15 = 240
     * - Total max: 480 internal edges
     *
     * Border edges (which must be 0) are not counted in the score
     * as they are mandatory constraints, not an objective to maximize.
     *
     * @return [current score, maximum possible score]
     */
    public int[] calculateScore() {
        int correctEdges = 0;

        // Calculate the theoretical maximum score (only internal edges)
        int maxInternalEdges = (rows - 1) * cols + rows * (cols - 1);
        int maxScore = maxInternalEdges;

        // Count placed pieces
        int placedPieces = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] != null) {
                    placedPieces++;
                }
            }
        }

        // If no pieces placed, return 0
        if (placedPieces == 0) {
            return new int[]{0, maxScore};
        }

        // Check internal horizontal edges
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols - 1; c++) {
                Placement left = grid[r][c];
                Placement right = grid[r][c + 1];

                // If both pieces are placed
                if (left != null && right != null) {
                    // The East edge of the left piece must match the West edge of the right piece
                    if (left.edges[1] == right.edges[3]) {
                        correctEdges++;
                    }
                }
            }
        }

        // Check internal vertical edges
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols; c++) {
                Placement top = grid[r][c];
                Placement bottom = grid[r + 1][c];

                // If both pieces are placed
                if (top != null && bottom != null) {
                    // The South edge of the top piece must match the North edge of the bottom piece
                    if (top.edges[2] == bottom.edges[0]) {
                        correctEdges++;
                    }
                }
            }
        }

        // Border edges are not counted in the score
        // (they are mandatory constraints, not an objective)

        return new int[]{correctEdges, maxScore};
    }

    /**
     * Calculates and displays the board score.
     */
    public void printScore() {
        int[] score = calculateScore();
        int current = score[0];
        int max = score[1];
        double percentage = max > 0 ? (current * 100.0 / max) : 0.0;

        SolverLogger.info("╔════════════════════════════════════════════════════════╗");
        SolverLogger.info("║                    BOARD SCORE                         ║");
        SolverLogger.info("╚════════════════════════════════════════════════════════╝");
        SolverLogger.info(String.format("Correct internal edges: %d / %d (%.1f%%)", current, max, percentage));

        // Score breakdown
        int internalH = (rows - 1) * cols;
        int internalV = rows * (cols - 1);

        SolverLogger.info(String.format("  - Horizontal edges: %d max", internalH));
        SolverLogger.info(String.format("  - Vertical edges: %d max", internalV));
        SolverLogger.info("  (Borders don't count in the score)");
        SolverLogger.info("");
    }

    /**
     * Checks if a cell is on any border of the board.
     *
     * @param r row
     * @param c column
     * @return true if cell is on top, bottom, left, or right border
     */
    public boolean isBorderCell(int r, int c) {
        validateCoordinates(r, c);
        return r == 0 || r == rows - 1 || c == 0 || c == cols - 1;
    }

    /**
     * Checks if a row is the top border (row 0).
     *
     * @param r row
     * @return true if this is the top border
     */
    public boolean isTopBorder(int r) {
        return r == 0;
    }

    /**
     * Checks if a row is the bottom border (last row).
     *
     * @param r row
     * @return true if this is the bottom border
     */
    public boolean isBottomBorder(int r) {
        return r == rows - 1;
    }

    /**
     * Checks if a column is the left border (col 0).
     *
     * @param c column
     * @return true if this is the left border
     */
    public boolean isLeftBorder(int c) {
        return c == 0;
    }

    /**
     * Checks if a column is the right border (last column).
     *
     * @param c column
     * @return true if this is the right border
     */
    public boolean isRightBorder(int c) {
        return c == cols - 1;
    }

    /**
     * Checks if a cell is a corner (both row and column are borders).
     *
     * @param r row
     * @param c column
     * @return true if cell is in a corner
     */
    public boolean isCorner(int r, int c) {
        validateCoordinates(r, c);
        return (r == 0 || r == rows - 1) && (c == 0 || c == cols - 1);
    }

    /**
     * Checks if a cell has any constraints (border or occupied neighbor).
     * A cell has constraints if:
     * - It is on a border (row 0, row max, col 0, or col max)
     * - OR it has at least one occupied neighbor (north, east, south, west)
     *
     * @param r row
     * @param c column
     * @return true if the cell has constraints, false if completely unconstrained
     */
    public boolean hasConstraints(int r, int c) {
        validateCoordinates(r, c);

        // Check if on border (uses utility method)
        if (isBorderCell(r, c)) {
            return true;
        }

        // Check if any neighbor is occupied
        // North
        if (r > 0 && !isEmpty(r - 1, c)) {
            return true;
        }
        // East
        if (c < cols - 1 && !isEmpty(r, c + 1)) {
            return true;
        }
        // South
        if (r < rows - 1 && !isEmpty(r + 1, c)) {
            return true;
        }
        // West
        if (c > 0 && !isEmpty(r, c - 1)) {
            return true;
        }

        // No constraints
        return false;
    }

    /**
     * Validates that coordinates are within board bounds.
     */
    private void validateCoordinates(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) {
            throw new IndexOutOfBoundsException(
                String.format("Invalid coordinates (%d, %d) for board %dx%d", r, c, rows, cols)
            );
        }
    }
}
