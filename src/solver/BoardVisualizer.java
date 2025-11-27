package solver;

import model.Board;
import model.Piece;
import model.Placement;

import java.util.*;

/**
 * Classe utilitaire pour visualiser les grilles de puzzle dans différents formats.
 * Fournit des représentations en ASCII art pour le débogage et le suivi de progression.
 *
 * Toutes les méthodes sont statiques et nécessitent des paramètres explicites pour maintenir
 * l'indépendance vis-à-vis de l'état d'EternitySolver.
 */
public class BoardVisualizer {

    /**
     * Classe auxiliaire pour stocker les informations de placement valide.
     */
    private static class ValidPlacement {
        int pieceId;
        int rotation;

        ValidPlacement(int pieceId, int rotation) {
            this.pieceId = pieceId;
            this.rotation = rotation;
        }
    }

    /**
     * Affiche une représentation compacte de la grille montrant les arêtes et le nombre de pièces.
     * Les cases vides affichent le nombre de pièces possibles.
     *
     * @param board état actuel de la grille
     * @param piecesById map de toutes les pièces par ID
     * @param pieceUsed BitSet marquant les pièces utilisées
     * @param totalPieces nombre total de pièces
     * @param fitsChecker interface fonctionnelle pour vérifier si une pièce correspond
     */
    public static void printBoardCompact(Board board, Map<Integer, Piece> piecesById,
                                        BitSet pieceUsed, int totalPieces,
                                        FitsChecker fitsChecker) {
        for (int r = 0; r < board.getRows(); r++) {
            // Ligne de bordure supérieure
            for (int c = 0; c < board.getCols(); c++) {
                System.out.print("-------");
            }
            System.out.println();

            // Ligne avec arêtes Nord
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

            // Ligne avec arêtes Ouest/Est (ou nombre de pièces possibles pour cases vides)
            for (int c = 0; c < board.getCols(); c++) {
                Placement p = board.getPlacement(r, c);
                if (p == null) {
                    // Case vide : afficher le nombre de pièces possibles (uniques, pas de rotations)
                    int count = countUniquePieces(board, r, c, piecesById, pieceUsed, totalPieces, fitsChecker);
                    if (count > 999) {
                        System.out.print("| 999+|");
                    } else if (count > 0) {
                        System.out.printf("| %3d |", count);
                    } else {
                        System.out.print("|  X  |");  // Impasse
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

            // Ligne avec arêtes Sud
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

        // Ligne de bordure finale
        for (int c = 0; c < board.getCols(); c++) {
            System.out.print("-------");
        }
        System.out.println();
    }

    /**
     * Affiche la grille avec les pièces posées et le nombre de pièces possibles sur les cases vides.
     *
     * @param board grille actuelle
     * @param piecesById map des pièces par ID
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @param lastPlacedRow ligne de la dernière pièce posée (-1 si aucune)
     * @param lastPlacedCol colonne de la dernière pièce posée (-1 si aucune)
     * @param fitsChecker interface fonctionnelle pour vérifier si une pièce correspond
     */
    public static void printBoardWithCounts(Board board, Map<Integer, Piece> piecesById,
                                           BitSet pieceUsed, int totalPieces,
                                           int lastPlacedRow, int lastPlacedCol,
                                           FitsChecker fitsChecker) {
        System.out.println("\nÉtat du puzzle:");
        System.out.println("===============");

        // Calculer les comptes pour toutes les cases vides
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

        // Afficher la grille de manière graphique
        for (int r = 0; r < board.getRows(); r++) {
            // Ligne de bordure supérieure
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

            // Ligne 1: arêtes Nord
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

            // Ligne 2: arêtes Ouest, ID/count, arêtes Est
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

            // Ligne 3: arêtes Sud
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

        // Ligne de bordure finale
        for (int c = 0; c < board.getCols(); c++) {
            System.out.print("----------");
        }
        System.out.println();
        System.out.println();
    }

    /**
     * Affiche la grille avec labels (coordonnées A-F pour les lignes, 1-N pour les colonnes).
     * Format détaillé avec arêtes et numéros de pièces, avec validation des correspondances.
     *
     * @param board grille actuelle
     * @param piecesById map des pièces par ID
     * @param unusedIds liste des IDs de pièces non utilisées
     * @param fixedPositions ensemble des positions fixes (format: "row,col")
     * @param fitsChecker interface fonctionnelle pour vérifier si une pièce correspond
     */
    public static void printBoardWithLabels(Board board, Map<Integer, Piece> piecesById,
                                           List<Integer> unusedIds, Set<String> fixedPositions,
                                           FitsChecker fitsChecker) {
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

            // Ligne 2: Ouest + ID pièce + Est
            System.out.print(" " + rowLabel + " │");
            for (int c = 0; c < cols; c++) {
                boolean isFixed = fixedPositions.contains(r + "," + c);
                if (board.isEmpty(r, c)) {
                    // Compter le nombre de pièces valides pour cette position
                    int validCount = countValidPiecesForPosition(board, r, c, piecesById, unusedIds, fitsChecker);

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

                    // Afficher ID (toujours en cyan si fixe)
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

    /**
     * Affiche la grille en comparant avec une autre grille (pour voir les différences)
     * Code couleur :
     * - Magenta : Case occupée dans referenceBoard mais vide dans currentBoard (régression)
     * - Orange : Case occupée dans les deux mais pièce différente (changement)
     * - Jaune : Case vide dans referenceBoard mais occupée dans currentBoard (progression)
     * - Cyan : Case identique dans les deux grilles (stabilité)
     *
     * @param currentBoard grille actuelle
     * @param referenceBoard grille de référence
     * @param piecesById map des pièces par ID
     * @param unusedIds liste des IDs de pièces non utilisées
     * @param fitsChecker interface fonctionnelle pour vérifier si une pièce correspond
     */
    public static void printBoardWithComparison(Board currentBoard, Board referenceBoard,
                                               Map<Integer, Piece> piecesById, List<Integer> unusedIds,
                                               FitsChecker fitsChecker) {
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

            // Ligne 2: Ouest + ID pièce + Est
            System.out.print(" " + rowLabel + " │");
            for (int c = 0; c < cols; c++) {
                String cellColor = getCellComparisonColor(currentBoard, referenceBoard, r, c);

                if (currentBoard.isEmpty(r, c)) {
                    // Compter le nombre de pièces valides pour cette position
                    int validCount = countValidPiecesForPosition(currentBoard, r, c, piecesById, unusedIds, fitsChecker);

                    // Utiliser la couleur de comparaison si c'était occupé dans la référence
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

    /**
     * Détermine la couleur d'une case en fonction de la comparaison entre deux grilles
     * - Magenta : occupée dans référence, vide dans current (régression)
     * - Orange : occupée dans les deux mais pièce différente (changement)
     * - Jaune : vide dans référence, occupée dans current (progression)
     * - Cyan : identique (stabilité)
     */
    public static String getCellComparisonColor(Board current, Board reference, int row, int col) {
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

    // ========== Méthodes auxiliaires ==========

    /**
     * Compte les pièces uniques qui peuvent être placées à une position (en ignorant les rotations).
     */
    private static int countUniquePieces(Board board, int r, int c, Map<Integer, Piece> piecesById,
                                        BitSet pieceUsed, int totalPieces, FitsChecker fitsChecker) {
        Set<Integer> validPieceIds = new HashSet<>();
        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed.get(pid)) continue; // Pièce déjà utilisée
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

    /**
     * Compte les pièces valides pour une position à partir d'une liste d'IDs non utilisés.
     */
    private static int countValidPiecesForPosition(Board board, int r, int c,
                                                   Map<Integer, Piece> piecesById,
                                                   List<Integer> unusedIds,
                                                   FitsChecker fitsChecker) {
        int count = 0;
        for (int pieceId : unusedIds) {
            Piece piece = piecesById.get(pieceId);
            if (piece != null) {
                // Tester les 4 rotations
                for (int rotation = 0; rotation < 4; rotation++) {
                    int[] rotatedEdges = piece.edgesRotated(rotation);
                    if (fitsChecker.fits(board, r, c, rotatedEdges)) {
                        count++;
                        break; // Une seule rotation suffit pour compter cette pièce
                    }
                }
            }
        }
        return count;
    }

    /**
     * Interface fonctionnelle pour vérifier si une pièce correspond à une position.
     * Ceci permet à BoardVisualizer de rester indépendant d'EternitySolver.
     */
    @FunctionalInterface
    public interface FitsChecker {
        boolean fits(Board board, int r, int c, int[] candidateEdges);
    }
}
