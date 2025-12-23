package solver;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.PuzzleFactory;

import java.util.Map;

/**
 * Programme de test pour valider le code avec un puzzle 6x6.
 * Lignes: A-F (0-5)
 * Colonnes: 1-6 (0-5)
 */
public class TestValidation6x6 {
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║        TEST DE VALIDATION - PUZZLE 6×6                 ║");
        System.out.println("║        Lignes: A-F / Colonnes: 1-6                     ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        // Charger les pièces du puzzle 6x6
        Map<Integer, Piece> pieces = PuzzleFactory.createValidation6x6();
        System.out.println("✓ " + pieces.size() + " pièces chargées\n");

        // Afficher les pièces
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("PIÈCES DU PUZZLE");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("ID  | N  E  S  W");
        System.out.println("────┼────────────");
        for (Map.Entry<Integer, Piece> entry : pieces.entrySet()) {
            int id = entry.getKey();
            int[] edges = entry.getValue().getEdges();
            System.out.printf("%2d  | %d  %d  %d  %d%n",
                id, edges[0], edges[1], edges[2], edges[3]);
        }
        System.out.println();

        // Créer le board
        Board board = new Board(6, 6);

        // Afficher le board vide avec coordonnées A-F et 1-6
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("BOARD VIDE (6×6)");
        System.out.println("═══════════════════════════════════════════════════════");
        printBoardWithCoordinates(board, pieces);

        // Afficher le score maximum
        int[] score = board.calculateScore();
        System.out.println("Score maximum théorique:");
        System.out.println("  - Arêtes internes horizontales: " + ((6-1) * 6) + " (5 × 6)");
        System.out.println("  - Arêtes internes verticales: " + (6 * (6-1)) + " (6 × 5)");
        System.out.println("  - Total: " + score[1] + " arêtes internes");
        System.out.println();

        // Lancer le solver
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("LANCEMENT DU SOLVER");
        System.out.println("═══════════════════════════════════════════════════════\n");

        EternitySolver solver = new EternitySolver();
        long startTime = System.currentTimeMillis();

        boolean solved = solver.solve(board, pieces);

        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;

        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("RÉSULTAT");
        System.out.println("═══════════════════════════════════════════════════════");

        if (solved) {
            System.out.println("✓ PUZZLE RÉSOLU!");
            System.out.println("Temps: " + String.format("%.2f", duration) + " secondes\n");

            printBoardWithCoordinates(board, pieces);
            board.printScore();
        } else {
            System.out.println("✗ Aucune solution trouvée");
            System.out.println("Temps: " + String.format("%.2f", duration) + " secondes\n");
        }
    }

    /**
     * Affiche le board avec les coordonnées A-F (lignes) et 1-6 (colonnes).
     */
    private static void printBoardWithCoordinates(Board board, Map<Integer, Piece> pieces) {
        System.out.print("     ");
        for (int c = 0; c < 6; c++) {
            System.out.print(" " + (c + 1) + "    ");
        }
        System.out.println();
        System.out.print("   ");
        for (int c = 0; c < 6; c++) {
            System.out.print("─────");
            if (c < 5) System.out.print("┼");
        }
        System.out.println();

        for (int r = 0; r < 6; r++) {
            char rowLabel = (char) ('A' + r);
            System.out.print(" " + rowLabel + " │");

            for (int c = 0; c < 6; c++) {
                if (board.isEmpty(r, c)) {
                    System.out.print(" --  ");
                } else {
                    int pieceId = board.getPlacement(r, c).getPieceId();
                    int rotation = board.getPlacement(r, c).getRotation();
                    System.out.printf(" %2d", pieceId);

                    // Afficher la rotation avec un symbole
                    char rotSymbol = rotation == 0 ? ' ' :
                                   rotation == 1 ? '→' :
                                   rotation == 2 ? '↓' : '←';
                    System.out.print(rotSymbol + " ");
                }
                if (c < 5) {
                    System.out.print("│");
                }
            }
            System.out.println("│");

            if (r < 5) {
                System.out.print("   ");
                for (int c = 0; c < 6; c++) {
                    System.out.print("─────");
                    if (c < 5) System.out.print("┼");
                }
                System.out.println();
            }
        }

        System.out.print("   ");
        for (int c = 0; c < 6; c++) {
            System.out.print("─────");
            if (c < 5) System.out.print("┼");
        }
        System.out.println("\n");
    }
}
