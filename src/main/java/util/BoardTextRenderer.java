package util;

import model.Board;
import model.Piece;
import model.Placement;

import java.io.PrintWriter;
import java.util.Map;

/**
 * Responsible for rendering board visualizations as ASCII art.
 * Extracted from SaveStateManager for better code organization.
 */
public class BoardTextRenderer {

    /**
     * Generates a visual ASCII display of the board WITH detailed edges.
     * Format: displays each piece with its 4 edges (N, E, S, W) and its ID.
     *
     * @param writer destination for output
     * @param board current board state
     * @param allPieces map of all available pieces
     */
    public static void generateBoardVisualDetailed(PrintWriter writer, Board board, Map<Integer, Piece> allPieces) {
        int rows = board.getRows();
        int cols = board.getCols();

        // Generate a horizontal separator
        StringBuilder separator = new StringBuilder("# ");
        for (int c = 0; c < cols; c++) {
            separator.append("─────────");
            if (c < cols - 1) separator.append("┬");
        }
        writer.println(separator.toString());

        for (int r = 0; r < rows; r++) {
            // Line 1: north edges
            StringBuilder line1 = new StringBuilder("# ");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    line1.append("    .    ");
                } else {
                    Placement p = board.getPlacement(r, c);
                    Piece piece = allPieces.get(p.getPieceId());
                    if (piece != null) {
                        int[] edges = piece.getEdges();
                        // Apply rotation
                        int northEdge = edges[p.getRotation()];
                        line1.append(String.format("   %2d    ", northEdge));
                    } else {
                        line1.append("   ??    ");
                    }
                }
                if (c < cols - 1) line1.append("│");
            }
            writer.println(line1.toString());

            // Line 2: piece ID with west and east edges
            StringBuilder line2 = new StringBuilder("# ");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    line2.append("    .    ");
                } else {
                    Placement p = board.getPlacement(r, c);
                    Piece piece = allPieces.get(p.getPieceId());
                    if (piece != null) {
                        int[] edges = piece.getEdges();
                        int westEdge = edges[(3 + p.getRotation()) % 4];
                        int eastEdge = edges[(1 + p.getRotation()) % 4];
                        line2.append(String.format("%2d %3d %2d", westEdge, p.getPieceId(), eastEdge));
                    } else {
                        line2.append(" ? ??? ?");
                    }
                }
                if (c < cols - 1) line2.append("│");
            }
            writer.println(line2.toString());

            // Line 3: south edges
            StringBuilder line3 = new StringBuilder("# ");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    line3.append("    .    ");
                } else {
                    Placement p = board.getPlacement(r, c);
                    Piece piece = allPieces.get(p.getPieceId());
                    if (piece != null) {
                        int[] edges = piece.getEdges();
                        int southEdge = edges[(2 + p.getRotation()) % 4];
                        line3.append(String.format("   %2d    ", southEdge));
                    } else {
                        line3.append("   ??    ");
                    }
                }
                if (c < cols - 1) line3.append("│");
            }
            writer.println(line3.toString());

            // Separator between lines
            if (r < rows - 1) {
                StringBuilder sep = new StringBuilder("# ");
                for (int c = 0; c < cols; c++) {
                    sep.append("─────────");
                    if (c < cols - 1) sep.append("┼");
                }
                writer.println(sep.toString());
            }
        }

        // Final separator
        StringBuilder finalSep = new StringBuilder("# ");
        for (int c = 0; c < cols; c++) {
            finalSep.append("─────────");
            if (c < cols - 1) finalSep.append("┴");
        }
        writer.println(finalSep.toString());
    }

    /**
     * Generates a visual ASCII display of the board (simple, without edges).
     * Compact format: displays the piece ID and its rotation.
     *
     * @param writer destination for output
     * @param board current board state
     */
    public static void generateBoardVisual(PrintWriter writer, Board board) {
        int rows = board.getRows();
        int cols = board.getCols();

        for (int r = 0; r < rows; r++) {
            StringBuilder line = new StringBuilder("# ");
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    line.append("  .   ");
                } else {
                    Placement p = board.getPlacement(r, c);
                    // Format: "ID_r " where r is the rotation (e.g.: "25_0 ", "210_3 ")
                    String pieceStr = String.format("%3d_%d", p.getPieceId(), p.getRotation());
                    line.append(pieceStr).append(" ");
                }
            }
            writer.println(line.toString());
        }
    }
}
