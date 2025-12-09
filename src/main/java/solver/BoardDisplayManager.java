package solver;

import util.SolverLogger;

import model.Board;
import model.Piece;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Handles board visualization and display operations with color-coded edge matching, valid piece counts, and comparison views. */
public class BoardDisplayManager {

    private final Set<String> fixedPositions;
    private final PlacementValidator validator;

    /** Creates display manager with fixed positions set and placement validator for piece fitting checks. */
    public BoardDisplayManager(Set<String> fixedPositions, PlacementValidator validator) {
        this.fixedPositions = fixedPositions;
        this.validator = validator;
    }

    /** Displays board with labels, color-coded grid, and valid piece counts; colors: cyan (fixed), green (matching edges), red (mismatched), bright red (deadends), yellow (critical). */
    public void printBoardWithLabels(Board board, Map<Integer, Piece> piecesById, List<Integer> unusedIds) {
        int rows = board.getRows();
        int cols = board.getCols();

        // Header with column numbers (right-aligned on 2 characters)
        System.out.print("     ");
        for (int c = 0; c < cols; c++) {
            System.out.printf("  %2d     ", (c + 1));
            if (c < cols - 1) System.out.print(" ");
        }
        SolverLogger.info("");

        // Top line
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("─");
        }
        SolverLogger.info("");

        for (int r = 0; r < rows; r++) {
            char rowLabel = (char) ('A' + r);

            // Line 1: North Edge
            System.out.print("   │");
            for (int c = 0; c < cols; c++) {
                boolean isFixed = fixedPositions.contains(r + "," + c);
                if (board.isEmpty(r, c)) {
                    System.out.print("         ");
                } else {
                    int[] edges = board.getPlacement(r, c).edges;
                    int edgeNorth = edges[0];

                    // Check if edge matches neighbor piece
                    String edgeColor = "";
                    if (r > 0 && !board.isEmpty(r - 1, c)) {
                        int neighborSouth = board.getPlacement(r - 1, c).edges[2];
                        edgeColor = (edgeNorth == neighborSouth) ? "\033[32m" : "\033[91m"; // Green or bright red
                    }

                    if (isFixed) System.out.print("\033[1;96m"); // Bright cyan + bold for fixed pieces
                    else if (!edgeColor.isEmpty()) System.out.print(edgeColor);
                    System.out.printf("   %2d    ", edgeNorth);
                    System.out.print("\033[0m");
                }
                System.out.print("│");
            }
            SolverLogger.info("");

            // Line 2: West + piece identifier + East
            System.out.print(" " + rowLabel + " │");
            for (int c = 0; c < cols; c++) {
                boolean isFixed = fixedPositions.contains(r + "," + c);
                if (board.isEmpty(r, c)) {
                    // Count number of valid pieces for this position
                    int validCount = countValidPiecesForPosition(board, r, c, piecesById, unusedIds);

                    // Color according to number of possible pieces
                    String countColor = "";
                    if (validCount == 0) countColor = "\033[1;91m";      // Bright red + bold (deadend!)
                    else if (validCount <= 5) countColor = "\033[93m";   // Bright yellow (critical)
                    else if (validCount <= 20) countColor = "\033[33m";  // Yellow (warning)

                    if (!countColor.isEmpty()) System.out.print(countColor);
                    System.out.printf("  (%3d)  ", validCount);
                    if (!countColor.isEmpty()) System.out.print("\033[0m");
                } else {
                    int pieceId = board.getPlacement(r, c).getPieceId();
                    int[] edges = board.getPlacement(r, c).edges;
                    int edgeWest = edges[3];
                    int edgeEast = edges[1];

                    // Check West and East matches
                    String westColor = "";
                    String eastColor = "";
                    if (c > 0 && !board.isEmpty(r, c - 1)) {
                        int neighborEast = board.getPlacement(r, c - 1).edges[1];
                        westColor = (edgeWest == neighborEast) ? "\033[32m" : "\033[91m";
                    }
                    if (c < cols - 1 && !board.isEmpty(r, c + 1)) {
                        int neighborWest = board.getPlacement(r, c + 1).edges[3];
                        eastColor = (edgeEast == neighborWest) ? "\033[32m" : "\033[91m";
                    }

                    // Display West
                    if (isFixed) System.out.print("\033[1;96m");
                    else if (!westColor.isEmpty()) System.out.print(westColor);
                    System.out.printf("%2d", edgeWest);
                    System.out.print("\033[0m");

                    // Display identifier (always cyan if fixed)
                    if (isFixed) System.out.print("\033[1;96m");
                    System.out.printf(" %3d ", pieceId);
                    System.out.print("\033[0m");

                    // Display East
                    if (isFixed) System.out.print("\033[1;96m");
                    else if (!eastColor.isEmpty()) System.out.print(eastColor);
                    System.out.printf("%2d", edgeEast);
                    System.out.print("\033[0m");
                }
                System.out.print("│");
            }
            SolverLogger.info("");

            // Line 3: South Edge
            System.out.print("   │");
            for (int c = 0; c < cols; c++) {
                boolean isFixed = fixedPositions.contains(r + "," + c);
                if (board.isEmpty(r, c)) {
                    System.out.print("         ");
                } else {
                    int[] edges = board.getPlacement(r, c).edges;
                    int edgeSouth = edges[2];

                    // Check if edge matches neighbor piece below
                    String edgeColor = "";
                    if (r < rows - 1 && !board.isEmpty(r + 1, c)) {
                        int neighborNorth = board.getPlacement(r + 1, c).edges[0];
                        edgeColor = (edgeSouth == neighborNorth) ? "\033[32m" : "\033[91m"; // Green or bright red
                    }

                    if (isFixed) System.out.print("\033[1;96m"); // Bright cyan + bold for fixed pieces
                    else if (!edgeColor.isEmpty()) System.out.print(edgeColor);
                    System.out.printf("   %2d    ", edgeSouth);
                    System.out.print("\033[0m");
                }
                System.out.print("│");
            }
            SolverLogger.info("");

            // Separator between rows
            if (r < rows - 1) {
                System.out.print("   ─");
                for (int c = 0; c < cols; c++) {
                    System.out.print("─────────");
                    if (c < cols - 1) System.out.print("┼");
                }
                SolverLogger.info("");
            }
        }

        // Bottom line
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("─");
        }
        SolverLogger.info("");
    }

    /** Displays board comparison showing differences; colors: magenta (regression), orange (change), yellow (progress), cyan (stable). */
    public void printBoardWithComparison(Board currentBoard, Board referenceBoard,
                                          Map<Integer, Piece> piecesById, List<Integer> unusedIds) {
        int rows = currentBoard.getRows();
        int cols = currentBoard.getCols();

        // Header with column numbers
        System.out.print("     ");
        for (int c = 0; c < cols; c++) {
            System.out.printf("  %2d     ", (c + 1));
            if (c < cols - 1) System.out.print(" ");
        }
        SolverLogger.info("");

        // Top line
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("─");
        }
        SolverLogger.info("");

        for (int r = 0; r < rows; r++) {
            char rowLabel = (char) ('A' + r);

            // Line 1: North Edge
            System.out.print("   │");
            for (int c = 0; c < cols; c++) {
                String cellColor = getCellComparisonColor(currentBoard, referenceBoard, r, c);

                if (currentBoard.isEmpty(r, c)) {
                    System.out.print("         ");
                } else {
                    int[] edges = currentBoard.getPlacement(r, c).edges;
                    int edgeNorth = edges[0];

                    System.out.print(cellColor);
                    System.out.printf("   %2d    ", edgeNorth);
                    System.out.print("\033[0m");
                }
                System.out.print("│");
            }
            SolverLogger.info("");

            // Line 2: West + piece identifier + East
            System.out.print(" " + rowLabel + " │");
            for (int c = 0; c < cols; c++) {
                String cellColor = getCellComparisonColor(currentBoard, referenceBoard, r, c);

                if (currentBoard.isEmpty(r, c)) {
                    // Count number of valid pieces for this position
                    int validCount = countValidPiecesForPosition(currentBoard, r, c, piecesById, unusedIds);

                    // Use comparison color if it was occupied in reference
                    if (!referenceBoard.isEmpty(r, c)) {
                        System.out.print(cellColor); // Magenta for regression
                    } else {
                        // Color according to number of possible pieces
                        if (validCount == 0) System.out.print("\033[1;91m");      // Bright red
                        else if (validCount <= 5) System.out.print("\033[93m");   // Yellow
                        else if (validCount <= 20) System.out.print("\033[33m");  // Dark yellow
                    }

                    System.out.printf("  (%3d)  ", validCount);
                    System.out.print("\033[0m");
                } else {
                    int pieceId = currentBoard.getPlacement(r, c).getPieceId();
                    int[] edges = currentBoard.getPlacement(r, c).edges;
                    int edgeWest = edges[3];
                    int edgeEast = edges[1];

                    // Display with comparison color
                    System.out.print(cellColor);
                    System.out.printf("%2d %3d %2d", edgeWest, pieceId, edgeEast);
                    System.out.print("\033[0m");
                }
                System.out.print("│");
            }
            SolverLogger.info("");

            // Line 3: South Edge
            System.out.print("   │");
            for (int c = 0; c < cols; c++) {
                String cellColor = getCellComparisonColor(currentBoard, referenceBoard, r, c);

                if (currentBoard.isEmpty(r, c)) {
                    System.out.print("         ");
                } else {
                    int[] edges = currentBoard.getPlacement(r, c).edges;
                    int edgeSouth = edges[2];

                    System.out.print(cellColor);
                    System.out.printf("   %2d    ", edgeSouth);
                    System.out.print("\033[0m");
                }
                System.out.print("│");
            }
            SolverLogger.info("");

            // Separator line between rows
            if (r < rows - 1) {
                System.out.print("   ─");
                for (int c = 0; c < cols; c++) {
                    System.out.print("─────────");
                    if (c < cols - 1) System.out.print("┼");
                }
                SolverLogger.info("");
            }
        }

        // Bottom line
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("─");
        }
        SolverLogger.info("");
    }

    /** Counts valid pieces for position (all rotations); returns number of unused pieces that fit at (r,c). */
    private int countValidPiecesForPosition(Board board, int r, int c,
                                           Map<Integer, Piece> piecesById,
                                           List<Integer> unusedIds) {
        int count = 0;
        for (int pieceId : unusedIds) {
            Piece piece = piecesById.get(pieceId);
            if (piece != null) {
                for (int rotation = 0; rotation < 4; rotation++) {
                    int[] rotatedEdges = piece.edgesRotated(rotation);
                    if (validator.fits(board, r, c, rotatedEdges)) {
                        count++;
                        break;
                    }
                }
            }
        }
        return count;
    }

    /** Returns ANSI color code for cell comparison: magenta (regression), orange (change), yellow (progress), cyan (stable). */
    private String getCellComparisonColor(Board current, Board reference, int row, int col) {
        boolean currentEmpty = current.isEmpty(row, col);
        boolean refEmpty = reference.isEmpty(row, col);

        if (refEmpty && currentEmpty) {
            // Both empty - no special color
            return "";
        } else if (!refEmpty && currentEmpty) {
            // Was occupied, now empty - REGRESSION (Magenta)
            return "\033[1;35m"; // Bright magenta + bold
        } else if (refEmpty && !currentEmpty) {
            // Was empty, now occupied - PROGRESS (Yellow)
            return "\033[1;33m"; // Bright yellow + bold
        } else {
            // Both occupied - compare pieces
            int currentPieceId = current.getPlacement(row, col).getPieceId();
            int currentRotation = current.getPlacement(row, col).getRotation();
            int refPieceId = reference.getPlacement(row, col).getPieceId();
            int refRotation = reference.getPlacement(row, col).getRotation();

            if (currentPieceId == refPieceId && currentRotation == refRotation) {
                // Identical - STABILITY (Cyan)
                return "\033[1;36m"; // Bright cyan + bold
            } else {
                // Different piece - CHANGE (Orange)
                return "\033[1;38;5;208m"; // Bright orange
            }
        }
    }
}
