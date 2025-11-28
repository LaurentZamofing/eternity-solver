import model.Board;
import model.Piece;

/**
 * Tests pour valider le système de scoring basé sur les arêtes correctes.
 */
public class BoardScoringTest {

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║            TESTS DU SYSTÈME DE SCORING                ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        int passed = 0;
        int failed = 0;

        // Test 1: Board vide
        if (testEmptyBoard()) {
            System.out.println("✓ Test 1: Board vide - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 1: Board vide - ÉCHOUÉ\n");
            failed++;
        }

        // Test 2: Une pièce de coin correcte
        if (testSingleCornerPiece()) {
            System.out.println("✓ Test 2: Une pièce de coin correcte - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 2: Une pièce de coin correcte - ÉCHOUÉ\n");
            failed++;
        }

        // Test 3: Deux pièces adjacentes qui matchent
        if (testTwoMatchingPieces()) {
            System.out.println("✓ Test 3: Deux pièces adjacentes qui matchent - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 3: Deux pièces adjacentes qui matchent - ÉCHOUÉ\n");
            failed++;
        }

        // Test 4: Deux pièces adjacentes qui ne matchent pas
        if (testTwoNonMatchingPieces()) {
            System.out.println("✓ Test 4: Deux pièces qui ne matchent pas - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 4: Deux pièces qui ne matchent pas - ÉCHOUÉ\n");
            failed++;
        }

        // Test 5: Calcul du score maximum pour 4x4
        if (testMaxScore4x4()) {
            System.out.println("✓ Test 5: Score maximum 4x4 - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 5: Score maximum 4x4 - ÉCHOUÉ\n");
            failed++;
        }

        // Test 6: Calcul du score maximum pour 16x16
        if (testMaxScore16x16()) {
            System.out.println("✓ Test 6: Score maximum 16x16 - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 6: Score maximum 16x16 - ÉCHOUÉ\n");
            failed++;
        }

        // Test 7: Board complètement rempli et correct
        if (testPerfectSmallBoard()) {
            System.out.println("✓ Test 7: Board 2x2 parfait - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 7: Board 2x2 parfait - ÉCHOUÉ\n");
            failed++;
        }

        // Résumé
        System.out.println("════════════════════════════════════════════════════════");
        System.out.println("RÉSULTATS: " + passed + " passés, " + failed + " échoués");
        System.out.println("════════════════════════════════════════════════════════");

        System.exit(failed > 0 ? 1 : 0);
    }

    private static boolean testEmptyBoard() {
        try {
            Board board = new Board(4, 4);
            int[] score = board.calculateScore();

            // Board vide devrait avoir score = 0
            if (score[0] != 0) {
                System.out.println("  Erreur: Board vide devrait avoir score = 0");
                System.out.println("  Reçu: " + score[0]);
                return false;
            }

            // Pour un board 4x4:
            // Arêtes internes: 3*4 + 4*3 = 24
            // (Les bordures ne comptent pas)
            int expectedMax = 24;
            if (score[1] != expectedMax) {
                System.out.println("  Erreur: Score max pour 4x4 devrait être " + expectedMax);
                System.out.println("  Reçu: " + score[1]);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean testSingleCornerPiece() {
        try {
            Board board = new Board(4, 4);

            // Pièce de coin valide: [N=0, E=5, S=3, W=0]
            Piece corner = new Piece(1, new int[]{0, 5, 3, 0});
            board.place(0, 0, corner, 0);

            int[] score = board.calculateScore();

            // Une seule pièce sans voisins = 0 arêtes internes
            // (Les bordures ne comptent pas dans le score)
            if (score[0] != 0) {
                System.out.println("  Erreur: Une pièce seule devrait avoir score = 0");
                System.out.println("  Reçu: " + score[0]);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean testTwoMatchingPieces() {
        try {
            Board board = new Board(4, 4);

            // Pièce 1 à (1, 1) avec E=7
            Piece piece1 = new Piece(1, new int[]{2, 7, 3, 4});
            board.place(1, 1, piece1, 0);

            // Pièce 2 à (1, 2) avec W=7 (compatible)
            Piece piece2 = new Piece(2, new int[]{5, 8, 6, 7});
            board.place(1, 2, piece2, 0);

            int[] score = board.calculateScore();

            // Devrait avoir 1 arête interne correcte (entre les deux pièces)
            if (score[0] != 1) {
                System.out.println("  Erreur: Deux pièces matchées devraient avoir score = 1");
                System.out.println("  Reçu: " + score[0]);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean testTwoNonMatchingPieces() {
        try {
            Board board = new Board(4, 4);

            // Pièce 1 à (1, 1) avec E=7
            Piece piece1 = new Piece(1, new int[]{2, 7, 3, 4});
            board.place(1, 1, piece1, 0);

            // Pièce 2 à (1, 2) avec W=99 (incompatible!)
            Piece piece2 = new Piece(2, new int[]{5, 8, 6, 99});
            board.place(1, 2, piece2, 0);

            int[] score = board.calculateScore();

            // Devrait avoir score = 0 (arêtes ne matchent pas)
            if (score[0] != 0) {
                System.out.println("  Erreur: Deux pièces non-matchées devraient avoir score = 0");
                System.out.println("  Reçu: " + score[0]);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean testMaxScore4x4() {
        try {
            Board board = new Board(4, 4);
            int[] score = board.calculateScore();

            // Pour un board 4x4:
            // Arêtes internes horizontales: (4-1) * 4 = 12
            // Arêtes internes verticales: 4 * (4-1) = 12
            // Total arêtes internes: 24
            int expectedMax = 24;

            if (score[1] != expectedMax) {
                System.out.println("  Erreur: Score max pour 4x4 devrait être " + expectedMax);
                System.out.println("  Reçu: " + score[1]);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testMaxScore16x16() {
        try {
            Board board = new Board(16, 16);
            int[] score = board.calculateScore();

            // Pour un board 16x16:
            // Arêtes internes horizontales: (16-1) * 16 = 240
            // Arêtes internes verticales: 16 * (16-1) = 240
            // Total arêtes internes: 480
            int expectedMax = 480;

            if (score[1] != expectedMax) {
                System.out.println("  Erreur: Score max pour 16x16 devrait être " + expectedMax);
                System.out.println("  Reçu: " + score[1]);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testPerfectSmallBoard() {
        try {
            Board board = new Board(2, 2);

            // Créer un board 2x2 parfait
            // Coin TL: [N=0, E=5, S=7, W=0]
            board.place(0, 0, new Piece(1, new int[]{0, 5, 7, 0}), 0);

            // Coin TR: [N=0, E=0, S=8, W=5]
            board.place(0, 1, new Piece(2, new int[]{0, 0, 8, 5}), 0);

            // Coin BL: [N=7, E=9, S=0, W=0]
            board.place(1, 0, new Piece(3, new int[]{7, 9, 0, 0}), 0);

            // Coin BR: [N=8, E=0, S=0, W=9]
            board.place(1, 1, new Piece(4, new int[]{8, 0, 0, 9}), 0);

            int[] score = board.calculateScore();

            // Pour un board 2x2 parfait:
            // Arêtes internes: 2 (horizontale entre TL-TR) + 2 (verticales) = 4
            // (Les bordures ne comptent pas dans le score)
            int expectedMax = 4;
            int expectedScore = 4; // Toutes les arêtes internes sont correctes

            if (score[0] != expectedScore) {
                System.out.println("  Erreur: Board parfait devrait avoir score = " + expectedScore);
                System.out.println("  Reçu: " + score[0]);
                return false;
            }

            if (score[1] != expectedMax) {
                System.out.println("  Erreur: Score max pour 2x2 devrait être " + expectedMax);
                System.out.println("  Reçu: " + score[1]);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
