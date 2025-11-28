package model;

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
        System.out.println("Board " + rows + "x" + cols + ":");
        for (int r = 0; r < rows; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == null) {
                    sb.append("---- ");
                } else {
                    sb.append(String.format("%s ", grid[r][c].toString()));
                }
            }
            System.out.println(sb.toString());
        }
        System.out.println();

        if (piecesById != null) {
            System.out.println("Details (coord -> id,rot,edges after rotation):");
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Placement p = grid[r][c];
                    if (p != null) {
                        System.out.printf("(%d,%d) => id=%d, rot=%d, edges=%s%n",
                                r, c, p.getPieceId(), p.getRotation(), Arrays.toString(p.edges));
                    }
                }
            }
            System.out.println();
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

        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║                    BOARD SCORE                         ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.printf("Correct internal edges: %d / %d (%.1f%%)%n", current, max, percentage);

        // Score breakdown
        int internalH = (rows - 1) * cols;
        int internalV = rows * (cols - 1);

        System.out.printf("  - Horizontal edges: %d max%n", internalH);
        System.out.printf("  - Vertical edges: %d max%n", internalV);
        System.out.println("  (Borders don't count in the score)");
        System.out.println();
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
