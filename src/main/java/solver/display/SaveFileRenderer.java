package solver.display;

import model.Board;
import model.Piece;
import model.Placement;
import util.PositionKey;
import util.SolverLogger;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Renders a board to a save file as ASCII art with edge information.
 *
 * <p>Extracted from {@link BoardDisplayService} (phase 2 refactor) to
 * isolate file-output rendering from the simpler console display
 * responsibilities. Also owns the candidate-counting logic used by
 * the detailed format.</p>
 *
 * <p>Thread-safety: all public methods are static and read-only over
 * their arguments.</p>
 */
public final class SaveFileRenderer {

    private SaveFileRenderer() {
        throw new AssertionError("utility class");
    }

    /**
     * Writes a simple framed board to the save file (one row per board
     * row, piece id and rotation inside each cell).
     */
    public static void writeToSaveFile(PrintWriter writer, Board board) {
        int rows = board.getRows();
        int cols = board.getCols();

        writer.print("# ┌");
        for (int c = 0; c < cols; c++) {
            writer.print("──────");
            if (c < cols - 1) writer.print("┬");
        }
        writer.println("┐");

        for (int r = 0; r < rows; r++) {
            writer.print("# │");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    writer.print("  --  ");
                } else {
                    Placement p = board.getPlacement(r, c);
                    writer.printf(" %3d:%d ", p.getPieceId(), p.getRotation());
                }
                writer.print("│");
            }
            writer.println();

            if (r < rows - 1) {
                writer.print("# ├");
                for (int c = 0; c < cols; c++) {
                    writer.print("──────");
                    if (c < cols - 1) writer.print("┼");
                }
                writer.println("┤");
            }
        }

        writer.print("# └");
        for (int c = 0; c < cols; c++) {
            writer.print("──────");
            if (c < cols - 1) writer.print("┴");
        }
        writer.println("┘");
    }

    /**
     * Writes a detailed visualization with north/middle/south lines per
     * row showing edges, piece ids, and candidate counts for empty
     * cells.
     *
     * @param domainManager if non-null and initialized, uses AC-3 domain
     *                      sizes; otherwise falls back to brute-force
     *                      candidate counting.
     * @param placementOrderMap optional position-to-step number overlay.
     */
    public static void writeToSaveFileDetailed(PrintWriter writer, Board board, Map<Integer, Piece> allPieces,
                                               solver.DomainManager domainManager,
                                               Map<PositionKey, Integer> placementOrderMap) {
        int rows = board.getRows();
        int cols = board.getCols();

        SolverLogger.info("Generating detailed save file visualization:");
        SolverLogger.info("  Board size: {}x{}", rows, cols);

        int placedCount = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!board.isEmpty(r, c)) placedCount++;
            }
        }
        SolverLogger.info("  Placed pieces: {}/{}", placedCount, rows * cols);
        SolverLogger.info("  Pieces map size: {}", allPieces != null ? allPieces.size() : 0);
        SolverLogger.info("  Using AC-3 domains: {}", domainManager != null && domainManager.isAC3Initialized());

        StringBuilder separator = new StringBuilder("# ├");
        for (int c = 0; c < cols; c++) {
            separator.append("─────────");
            if (c < cols - 1) separator.append("┬");
        }
        separator.append("┤");
        writer.println(separator.toString());

        for (int r = 0; r < rows; r++) {
            CandidateCount[] rowCandidates = new CandidateCount[cols];
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    if (domainManager != null && domainManager.isAC3Initialized()) {
                        Map<Integer, List<solver.DomainManager.ValidPlacement>> domain =
                            domainManager.getDomain(r, c);
                        int numPieces = (domain != null) ? domain.size() : 0;
                        rowCandidates[c] = new CandidateCount(numPieces, numPieces);
                    } else {
                        rowCandidates[c] = countCandidatesWithRotations(board, r, c, allPieces);
                    }
                }
            }

            // Line 1: north edges + placement order
            StringBuilder line1 = new StringBuilder("# │");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    line1.append("    .    ");
                } else {
                    Placement p = board.getPlacement(r, c);
                    int northEdge = p.edges[0];

                    PositionKey posKey = new PositionKey(r, c);
                    String orderSuffix = "";
                    if (placementOrderMap != null && placementOrderMap.containsKey(posKey)) {
                        int order = placementOrderMap.get(posKey);
                        orderSuffix = "#" + order;
                    }

                    int totalLength = 2 + orderSuffix.length();
                    int leftPadding = Math.max(3, (9 - totalLength) / 2 + (9 - totalLength) % 2);
                    String formatted = String.format("%" + leftPadding + "s%2d%s", "", northEdge, orderSuffix);
                    if (formatted.length() < 9) {
                        formatted = String.format("%-9s", formatted);
                    }
                    line1.append(formatted);
                }
                line1.append("│");
            }
            writer.println(line1.toString());

            // Line 2: piece id + edges, or (pieces/rotations) for empty cells
            StringBuilder line2 = new StringBuilder("# │");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    if (!board.hasConstraints(r, c)) {
                        line2.append("         ");
                    } else {
                        CandidateCount count = rowCandidates[c];
                        if (count.numPieces > 0 || count.numRotations > 0) {
                            line2.append(String.format("(%3d/%3d)", count.numPieces, count.numRotations));
                        } else {
                            line2.append("   ∅     ");
                        }
                    }
                } else {
                    Placement p = board.getPlacement(r, c);
                    int westEdge = p.edges[3];
                    int eastEdge = p.edges[1];
                    line2.append(String.format("%2d %3d %2d", westEdge, p.getPieceId(), eastEdge));
                }
                line2.append("│");
            }
            writer.println(line2.toString());

            // Line 3: south edges
            StringBuilder line3 = new StringBuilder("# │");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    line3.append("    .    ");
                } else {
                    Placement p = board.getPlacement(r, c);
                    int southEdge = p.edges[2];
                    line3.append(String.format("   %2d    ", southEdge));
                }
                line3.append("│");
            }
            writer.println(line3.toString());

            if (r < rows - 1) {
                StringBuilder sep = new StringBuilder("# ├");
                for (int c = 0; c < cols; c++) {
                    sep.append("─────────");
                    if (c < cols - 1) sep.append("┼");
                }
                sep.append("┤");
                writer.println(sep.toString());
            }
        }

        StringBuilder bottomSep = new StringBuilder("# └");
        for (int c = 0; c < cols; c++) {
            bottomSep.append("─────────");
            if (c < cols - 1) bottomSep.append("┴");
        }
        bottomSep.append("┘");
        writer.println(bottomSep.toString());

        SolverLogger.info("Save file visualization generated successfully");
        SolverLogger.info("  Total rows rendered: {}", rows);
    }

    // ════════════════════════════════════════════════════════════════
    // Candidate counting (brute-force fallback when AC-3 not available)
    // ════════════════════════════════════════════════════════════════

    /**
     * Counts candidate pieces + total rotations that can fit at (row,col).
     * Exposed for tests and for callers that want the same heuristic
     * without invoking the full detailed renderer.
     */
    public static CandidateCount countCandidatesWithRotations(Board board, int row, int col,
                                                              Map<Integer, Piece> allPieces) {
        if (allPieces == null) return new CandidateCount(0, 0);

        int numPieces = 0;
        int numRotations = 0;

        for (Piece piece : allPieces.values()) {
            if (isPiecePlacedOnBoard(board, piece.getId())) continue;

            int validRotationsForThisPiece = 0;
            for (int rotation = 0; rotation < 4; rotation++) {
                if (canPlacePiece(board, row, col, piece, rotation)) {
                    validRotationsForThisPiece++;
                    numRotations++;
                }
            }
            if (validRotationsForThisPiece > 0) numPieces++;
        }

        return new CandidateCount(numPieces, numRotations);
    }

    private static boolean isPiecePlacedOnBoard(Board board, int pieceId) {
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.isEmpty(r, c) && board.getPlacement(r, c).getPieceId() == pieceId) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean canPlacePiece(Board board, int row, int col, Piece piece, int rotation) {
        int[] edges = piece.edgesRotated(rotation);
        int north = edges[0], east = edges[1], south = edges[2], west = edges[3];

        boolean topBorder = row == 0;
        boolean bottomBorder = row == board.getRows() - 1;
        boolean leftBorder = col == 0;
        boolean rightBorder = col == board.getCols() - 1;

        // Interior cells cannot have any 0 edge
        if (!topBorder && north == 0) return false;
        if (!bottomBorder && south == 0) return false;
        if (!leftBorder && west == 0) return false;
        if (!rightBorder && east == 0) return false;

        if (topBorder) {
            if (north != 0) return false;
        } else if (!board.isEmpty(row - 1, col)) {
            if (board.getPlacement(row - 1, col).edges[2] != north) return false;
        }

        if (rightBorder) {
            if (east != 0) return false;
        } else if (!board.isEmpty(row, col + 1)) {
            if (board.getPlacement(row, col + 1).edges[3] != east) return false;
        }

        if (bottomBorder) {
            if (south != 0) return false;
        } else if (!board.isEmpty(row + 1, col)) {
            if (board.getPlacement(row + 1, col).edges[0] != south) return false;
        }

        if (leftBorder) {
            if (west != 0) return false;
        } else if (!board.isEmpty(row, col - 1)) {
            if (board.getPlacement(row, col - 1).edges[1] != west) return false;
        }

        return true;
    }

    /** Result of {@link #countCandidatesWithRotations(Board, int, int, Map)}. */
    public static final class CandidateCount {
        public final int numPieces;
        public final int numRotations;

        public CandidateCount(int numPieces, int numRotations) {
            this.numPieces = numPieces;
            this.numRotations = numRotations;
        }
    }
}
