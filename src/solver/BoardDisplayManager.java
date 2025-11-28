package solver;

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

        // En-tête avec numéros de colonnes (alignés à droite sur 2 caractères)
        System.out.print("     ");
        for (int c = 0; c < cols; c++) {
            System.out.printf("  %2d     ", (c + 1));
            if (c < cols - 1) System.out.print(" ");
        }
        System.out.println();

        // Ligne supérieure
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("─");
        }
        System.out.println();

        for (int r = 0; r < rows; r++) {
            char rowLabel = (char) ('A' + r);

            // Ligne 1: Arête Nord
            System.out.print("   │");
            for (int c = 0; c < cols; c++) {
                boolean isFixed = fixedPositions.contains(r + "," + c);
                if (board.isEmpty(r, c)) {
                    System.out.print("         ");
                } else {
                    int[] edges = board.getPlacement(r, c).edges;
                    int edgeNorth = edges[0];

                    // Vérifier si l'arête correspond à la pièce voisine
                    String edgeColor = "";
                    if (r > 0 && !board.isEmpty(r - 1, c)) {
                        int neighborSouth = board.getPlacement(r - 1, c).edges[2];
                        edgeColor = (edgeNorth == neighborSouth) ? "\033[32m" : "\033[91m"; // Vert ou Rouge brillant
                    }

                    if (isFixed) System.out.print("\033[1;96m"); // Cyan brillant + gras pour pièces fixes
                    else if (!edgeColor.isEmpty()) System.out.print(edgeColor);
                    System.out.printf("   %2d    ", edgeNorth);
                    System.out.print("\033[0m");
                }
                System.out.print("│");
            }
            System.out.println();

            // Ligne 2: Ouest + identifiant pièce + Est
            System.out.print(" " + rowLabel + " │");
            for (int c = 0; c < cols; c++) {
                boolean isFixed = fixedPositions.contains(r + "," + c);
                if (board.isEmpty(r, c)) {
                    // Compter le nombre de pièces valides pour cette position
                    int validCount = countValidPiecesForPosition(board, r, c, piecesById, unusedIds);

                    // Colorier selon le nombre de pièces possibles
                    String countColor = "";
                    if (validCount == 0) countColor = "\033[1;91m";      // Rouge brillant + gras (impasse!)
                    else if (validCount <= 5) countColor = "\033[93m";   // Jaune brillant (critique)
                    else if (validCount <= 20) countColor = "\033[33m";  // Jaune (attention)

                    if (!countColor.isEmpty()) System.out.print(countColor);
                    System.out.printf("  (%3d)  ", validCount);
                    if (!countColor.isEmpty()) System.out.print("\033[0m");
                } else {
                    int pieceId = board.getPlacement(r, c).getPieceId();
                    int[] edges = board.getPlacement(r, c).edges;
                    int edgeWest = edges[3];
                    int edgeEast = edges[1];

                    // Vérifier correspondances Ouest et Est
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

                    // Afficher Ouest
                    if (isFixed) System.out.print("\033[1;96m");
                    else if (!westColor.isEmpty()) System.out.print(westColor);
                    System.out.printf("%2d", edgeWest);
                    System.out.print("\033[0m");

                    // Afficher identifiant (toujours en cyan si fixe)
                    if (isFixed) System.out.print("\033[1;96m");
                    System.out.printf(" %3d ", pieceId);
                    System.out.print("\033[0m");

                    // Afficher Est
                    if (isFixed) System.out.print("\033[1;96m");
                    else if (!eastColor.isEmpty()) System.out.print(eastColor);
                    System.out.printf("%2d", edgeEast);
                    System.out.print("\033[0m");
                }
                System.out.print("│");
            }
            System.out.println();

            // Ligne 3: Arête Sud
            System.out.print("   │");
            for (int c = 0; c < cols; c++) {
                boolean isFixed = fixedPositions.contains(r + "," + c);
                if (board.isEmpty(r, c)) {
                    System.out.print("         ");
                } else {
                    int[] edges = board.getPlacement(r, c).edges;
                    int edgeSouth = edges[2];

                    // Vérifier si l'arête correspond à la pièce voisine du dessous
                    String edgeColor = "";
                    if (r < rows - 1 && !board.isEmpty(r + 1, c)) {
                        int neighborNorth = board.getPlacement(r + 1, c).edges[0];
                        edgeColor = (edgeSouth == neighborNorth) ? "\033[32m" : "\033[91m"; // Vert ou Rouge brillant
                    }

                    if (isFixed) System.out.print("\033[1;96m"); // Cyan brillant + gras pour pièces fixes
                    else if (!edgeColor.isEmpty()) System.out.print(edgeColor);
                    System.out.printf("   %2d    ", edgeSouth);
                    System.out.print("\033[0m");
                }
                System.out.print("│");
            }
            System.out.println();

            // Séparateur entre lignes
            if (r < rows - 1) {
                System.out.print("   ─");
                for (int c = 0; c < cols; c++) {
                    System.out.print("─────────");
                    if (c < cols - 1) System.out.print("┼");
                }
                System.out.println();
            }
        }

        // Ligne inférieure
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("─");
        }
        System.out.println();
    }

    /** Displays board comparison showing differences; colors: magenta (regression), orange (change), yellow (progress), cyan (stable). */
    public void printBoardWithComparison(Board currentBoard, Board referenceBoard,
                                          Map<Integer, Piece> piecesById, List<Integer> unusedIds) {
        int rows = currentBoard.getRows();
        int cols = currentBoard.getCols();

        // En-tête avec numéros de colonnes
        System.out.print("     ");
        for (int c = 0; c < cols; c++) {
            System.out.printf("  %2d     ", (c + 1));
            if (c < cols - 1) System.out.print(" ");
        }
        System.out.println();

        // Ligne supérieure
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("─");
        }
        System.out.println();

        for (int r = 0; r < rows; r++) {
            char rowLabel = (char) ('A' + r);

            // Ligne 1: Arête Nord
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

            // Ligne 2: Ouest + identifiant pièce + Est
            System.out.print(" " + rowLabel + " │");
            for (int c = 0; c < cols; c++) {
                String cellColor = getCellComparisonColor(currentBoard, referenceBoard, r, c);

                if (currentBoard.isEmpty(r, c)) {
                    // Compter le nombre de pièces valides pour cette position
                    int validCount = countValidPiecesForPosition(currentBoard, r, c, piecesById, unusedIds);

                    // Utiliser la couleur de comparaison si c'était occupé dans le référence
                    if (!referenceBoard.isEmpty(r, c)) {
                        System.out.print(cellColor); // Magenta pour régression
                    } else {
                        // Colorier selon le nombre de pièces possibles
                        if (validCount == 0) System.out.print("\033[1;91m");      // Rouge brillant
                        else if (validCount <= 5) System.out.print("\033[93m");   // Jaune
                        else if (validCount <= 20) System.out.print("\033[33m");  // Jaune foncé
                    }

                    System.out.printf("  (%3d)  ", validCount);
                    System.out.print("\033[0m");
                } else {
                    int pieceId = currentBoard.getPlacement(r, c).getPieceId();
                    int[] edges = currentBoard.getPlacement(r, c).edges;
                    int edgeWest = edges[3];
                    int edgeEast = edges[1];

                    // Afficher avec la couleur de comparaison
                    System.out.print(cellColor);
                    System.out.printf("%2d %3d %2d", edgeWest, pieceId, edgeEast);
                    System.out.print("\033[0m");
                }
                System.out.print("│");
            }
            System.out.println();

            // Ligne 3: Arête Sud
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

            // Ligne de séparation entre les lignes
            if (r < rows - 1) {
                System.out.print("   ─");
                for (int c = 0; c < cols; c++) {
                    System.out.print("─────────");
                    if (c < cols - 1) System.out.print("┼");
                }
                System.out.println();
            }
        }

        // Ligne inférieure
        System.out.print("   ─");
        for (int c = 0; c < cols; c++) {
            System.out.print("─────────");
            if (c < cols - 1) System.out.print("─");
        }
        System.out.println();
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
            // Les deux vides - pas de couleur spéciale
            return "";
        } else if (!refEmpty && currentEmpty) {
            // Était occupée, maintenant vide - RÉGRESSION (Magenta)
            return "\033[1;35m"; // Magenta brillant + gras
        } else if (refEmpty && !currentEmpty) {
            // Était vide, maintenant occupée - PROGRESSION (Jaune)
            return "\033[1;33m"; // Jaune brillant + gras
        } else {
            // Les deux occupées - comparer les pièces
            int currentPieceId = current.getPlacement(row, col).getPieceId();
            int currentRotation = current.getPlacement(row, col).getRotation();
            int refPieceId = reference.getPlacement(row, col).getPieceId();
            int refRotation = reference.getPlacement(row, col).getRotation();

            if (currentPieceId == refPieceId && currentRotation == refRotation) {
                // Identique - STABILITÉ (Cyan)
                return "\033[1;36m"; // Cyan brillant + gras
            } else {
                // Pièce différente - CHANGEMENT (Orange)
                return "\033[1;38;5;208m"; // Orange brillant
            }
        }
    }
}
