package solver;

import model.Board;
import model.Piece;
import model.Placement;

import java.util.*;

/** Utility class for visualizing puzzle boards in ASCII art formats; all methods static and solver-independent. */
public class BoardVisualizer {

    /** Helper class storing valid placement information (piece ID and rotation). */
    private static class ValidPlacement {
        int pieceId;
        int rotation;

        ValidPlacement(int pieceId, int rotation) {
            this.pieceId = pieceId;
            this.rotation = rotation;
        }
    }

    /** Displays compact board representation showing edges and piece counts; empty cells show number of possible pieces. */
    public static void printBoardCompact(Board board, Map<Integer, Piece> piecesById,
                                        BitSet pieceUsed, int totalPieces,
                                        FitsChecker fitsChecker) {
        for (int r = 0; r < board.getRows(); r++) {
            // Top border line
            for (int c = 0; c < board.getCols(); c++) {
                System.out.print("-------");
            }
            System.out.println();

            // Line with North edges
            for (int c = 0; c < board.getCols(); c++) {
                Placement p = board.getPlacement(r, c);
                if (p == null) {
                    System.out.print("|     |");
                } else {
                    int n = p.edges[0];
                    if (n == 0) {
                        System.out.print("|     |");
                    } else {
                        System.out.printf("|  %2d |", n);
                    }
                }
            }
            System.out.println();

            // Line with West/East edges (or number of possible pieces for empty cells)
            for (int c = 0; c < board.getCols(); c++) {
                Placement p = board.getPlacement(r, c);
                if (p == null) {
                    // Empty cell: display number of possible pieces (unique, not rotations)
                    int count = countUniquePieces(board, r, c, piecesById, pieceUsed, totalPieces, fitsChecker);
                    if (count > 999) {
                        System.out.print("| 999+|");
                    } else if (count > 0) {
                        System.out.printf("| %3d |", count);
                    } else {
                        System.out.print("|  X  |");  // Deadend
                    }
                } else {
                    int w = p.edges[3];
                    int e = p.edges[1];
                    String wStr = (w == 0) ? "  " : String.format("%2d", w);
                    String eStr = (e == 0) ? "  " : String.format("%2d", e);
                    System.out.printf("|%s %s|", wStr, eStr);
                }
            }
            System.out.println();

            // Line with South edges
            for (int c = 0; c < board.getCols(); c++) {
                Placement p = board.getPlacement(r, c);
                if (p == null) {
                    System.out.print("|     |");
                } else {
                    int s = p.edges[2];
                    if (s == 0) {
                        System.out.print("|     |");
                    } else {
                        System.out.printf("|  %2d |", s);
                    }
                }
            }
            System.out.println();
        }

        // Final border line
        for (int c = 0; c < board.getCols(); c++) {
            System.out.print("-------");
        }
        System.out.println();
    }

    /** Displays board with placed pieces and possible piece counts on empty cells; highlights last placed cell and minimum remaining values. */
    public static void printBoardWithCounts(Board board, Map<Integer, Piece> piecesById,
                                           BitSet pieceUsed, int totalPieces,
                                           int lastPlacedRow, int lastPlacedCol,
                                           FitsChecker fitsChecker) {
        System.out.println("\nPuzzle state:");
        System.out.println("===============");

        // Calculate counts for all empty cells
        int[][] counts = new int[board.getRows()][board.getCols()];
        int minCount = Integer.MAX_VALUE;
        int minRow = -1, minCol = -1;

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.isEmpty(r, c)) {
                    counts[r][c] = countUniquePieces(board, r, c, piecesById, pieceUsed, totalPieces, fitsChecker);
                    if (counts[r][c] > 0 && counts[r][c] < minCount) {
                        minCount = counts[r][c];
                        minRow = r;
                        minCol = c;
                    }
                }
            }
        }

        // Codes ANSI pour le formatage
        final String BOLD = "\033[1m";
        final String RESET = "\033[0m";

        // Display grid graphically
        for (int r = 0; r < board.getRows(); r++) {
            // Top border line
            for (int c = 0; c < board.getCols(); c++) {
                boolean isLastPlaced = (r == lastPlacedRow && c == lastPlacedCol);
                boolean isMinCount = (r == minRow && c == minCol);
                if (isLastPlaced || isMinCount) {
                    System.out.print(BOLD + "----------" + RESET);
                } else {
                    System.out.print("----------");
                }
            }
            System.out.println();

            // Line 1: North edges
            for (int c = 0; c < board.getCols(); c++) {
                boolean isLastPlaced = (r == lastPlacedRow && c == lastPlacedCol);
                boolean isMinCount = (r == minRow && c == minCol);
                Placement p = board.getPlacement(r, c);

                if (isLastPlaced || isMinCount) {
                    System.out.print(BOLD);
                }

                if (p == null) {
                    System.out.print("|        |");
                } else {
                    System.out.printf("|   %d    |", p.edges[0]);
                }

                if (isLastPlaced || isMinCount) {
                    System.out.print(RESET);
                }
            }
            System.out.println();

            // Line 2: West edges, ID/count, East edges
            for (int c = 0; c < board.getCols(); c++) {
                boolean isLastPlaced = (r == lastPlacedRow && c == lastPlacedCol);
                boolean isMinCount = (r == minRow && c == minCol);
                Placement p = board.getPlacement(r, c);

                if (isLastPlaced || isMinCount) {
                    System.out.print(BOLD);
                }

                if (p == null) {
                    System.out.printf("|   %2d   |", counts[r][c]);
                } else {
                    System.out.printf("|%d  %2d  %d|", p.edges[3], p.getPieceId(), p.edges[1]);
                }

                if (isLastPlaced || isMinCount) {
                    System.out.print(RESET);
                }
            }
            System.out.println();

            // Line 3: South edges
            for (int c = 0; c < board.getCols(); c++) {
                boolean isLastPlaced = (r == lastPlacedRow && c == lastPlacedCol);
                boolean isMinCount = (r == minRow && c == minCol);
                Placement p = board.getPlacement(r, c);

                if (isLastPlaced || isMinCount) {
                    System.out.print(BOLD);
                }

                if (p == null) {
                    System.out.print("|        |");
                } else {
                    System.out.printf("|   %d    |", p.edges[2]);
                }

                if (isLastPlaced || isMinCount) {
                    System.out.print(RESET);
                }
            }
            System.out.println();
        }

        // Final border line
        for (int c = 0; c < board.getCols(); c++) {
            System.out.print("----------");
        }
        System.out.println();
        System.out.println();
    }

    /** Displays labeled board with coordinates (A-F rows, 1-N columns) and edge matching validation; color-codes edge compatibility. */
    public static void printBoardWithLabels(Board board, Map<Integer, Piece> piecesById,
                                           List<Integer> unusedIds, Set<String> fixedPositions,
                                           FitsChecker fitsChecker) {
        int rows = board.getRows();
        int cols = board.getCols();

        // Header with column numbers (right-aligned on 2 characters)
        System.out.print("     ");
        for (int c = 0; c < cols; c++) {
            System.out.printf("  %2d     ", (c + 1));
            if (c < cols - 1) System.out.print(" ");
        }
        System.out.println();

        // Top line
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("─");
        }
        System.out.println();

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
            System.out.println();

            // Line 2: West + piece ID + East
            System.out.print(" " + rowLabel + " │");
            for (int c = 0; c < cols; c++) {
                boolean isFixed = fixedPositions.contains(r + "," + c);
                if (board.isEmpty(r, c)) {
                    // Count number of valid pieces for this position
                    int validCount = countValidPiecesForPosition(board, r, c, piecesById, unusedIds, fitsChecker);

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

                    // Display ID (always cyan if fixed)
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
            System.out.println();

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
            System.out.println();

            // Separator between rows
            if (r < rows - 1) {
                System.out.print("   ─");
                for (int c = 0; c < cols; c++) {
                    System.out.print("─────────");
                    if (c < cols - 1) System.out.print("┼");
                }
                System.out.println();
            }
        }

        // Bottom line
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("─");
        }
        System.out.println();
    }

    /** Displays board comparison showing differences with color coding: magenta (regression), orange (change), yellow (progress), cyan (stable). */
    public static void printBoardWithComparison(Board currentBoard, Board referenceBoard,
                                               Map<Integer, Piece> piecesById, List<Integer> unusedIds,
                                               FitsChecker fitsChecker) {
        int rows = currentBoard.getRows();
        int cols = currentBoard.getCols();

        // Header with column numbers
        System.out.print("     ");
        for (int c = 0; c < cols; c++) {
            System.out.printf("  %2d     ", (c + 1));
            if (c < cols - 1) System.out.print(" ");
        }
        System.out.println();

        // Top line
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("─");
        }
        System.out.println();

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
            System.out.println();

            // Line 2: West + piece ID + East
            System.out.print(" " + rowLabel + " │");
            for (int c = 0; c < cols; c++) {
                String cellColor = getCellComparisonColor(currentBoard, referenceBoard, r, c);

                if (currentBoard.isEmpty(r, c)) {
                    // Count number of valid pieces for this position
                    int validCount = countValidPiecesForPosition(currentBoard, r, c, piecesById, unusedIds, fitsChecker);

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
            System.out.println();

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
            System.out.println();

            // Separator line between rows
            if (r < rows - 1) {
                System.out.print("   ─");
                for (int c = 0; c < cols; c++) {
                    System.out.print("─────────");
                    if (c < cols - 1) System.out.print("┼");
                }
                System.out.println();
            }
        }

        // Bottom line
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("─");
        }
        System.out.println();
    }

    /** Returns cell comparison color: magenta (regression), orange (change), yellow (progress), cyan (stable). */
    public static String getCellComparisonColor(Board current, Board reference, int row, int col) {
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

    // ========== Helper methods ==========

    /** Counts unique pieces that can be placed at position (ignoring rotations). */
    private static int countUniquePieces(Board board, int r, int c, Map<Integer, Piece> piecesById,
                                        BitSet pieceUsed, int totalPieces, FitsChecker fitsChecker) {
        Set<Integer> validPieceIds = new HashSet<>();
        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed.get(pid)) continue; // Piece already used
            Piece piece = piecesById.get(pid);
            boolean foundValidRotation = false;
            for (int rot = 0; rot < 4 && !foundValidRotation; rot++) {
                int[] candidate = piece.edgesRotated(rot);
                if (fitsChecker.fits(board, r, c, candidate)) {
                    validPieceIds.add(pid);
                    foundValidRotation = true;
                }
            }
        }
        return validPieceIds.size();
    }

    /** Counts valid pieces for position from list of unused IDs. */
    private static int countValidPiecesForPosition(Board board, int r, int c,
                                                   Map<Integer, Piece> piecesById,
                                                   List<Integer> unusedIds,
                                                   FitsChecker fitsChecker) {
        int count = 0;
        for (int pieceId : unusedIds) {
            Piece piece = piecesById.get(pieceId);
            if (piece != null) {
                // Test all 4 rotations
                for (int rotation = 0; rotation < 4; rotation++) {
                    int[] rotatedEdges = piece.edgesRotated(rotation);
                    if (fitsChecker.fits(board, r, c, rotatedEdges)) {
                        count++;
                        break; // One rotation is enough to count this piece
                    }
                }
            }
        }
        return count;
    }

    /** Functional interface for checking if piece fits at position; keeps BoardVisualizer independent from EternitySolver. */
    @FunctionalInterface
    public interface FitsChecker {
        boolean fits(Board board, int r, int c, int[] candidateEdges);
    }
}
