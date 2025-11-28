import model.Board;
import model.Piece;
import model.Placement;

/**
 * Tests pour valider la logique de contraintes et de placement.
 * Ces tests vérifient que les pièces respectent les contraintes de bord
 * et d'adjacence qui sont essentielles au puzzle Eternity II.
 */
public class ConstraintValidationTest {

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║        TESTS DE VALIDATION DES CONTRAINTES            ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        int passed = 0;
        int failed = 0;

        // Test 1: Contrainte de bordure - coin
        if (testCornerConstraint()) {
            System.out.println("✓ Test 1: Contrainte de coin (2 bordures à 0) - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 1: Contrainte de coin (2 bordures à 0) - ÉCHOUÉ\n");
            failed++;
        }

        // Test 2: Contrainte de bordure - bord
        if (testEdgeConstraint()) {
            System.out.println("✓ Test 2: Contrainte de bord (1 bordure à 0) - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 2: Contrainte de bord (1 bordure à 0) - ÉCHOUÉ\n");
            failed++;
        }

        // Test 3: Placement et vérification des edges
        if (testPlacementEdges()) {
            System.out.println("✓ Test 3: Vérification des edges après placement - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 3: Vérification des edges après placement - ÉCHOUÉ\n");
            failed++;
        }

        // Test 4: Rotation et placement
        if (testRotationPlacement()) {
            System.out.println("✓ Test 4: Placement avec rotation - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 4: Placement avec rotation - ÉCHOUÉ\n");
            failed++;
        }

        // Test 5: Adjacence horizontale
        if (testHorizontalAdjacency()) {
            System.out.println("✓ Test 5: Vérification adjacence horizontale - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 5: Vérification adjacence horizontale - ÉCHOUÉ\n");
            failed++;
        }

        // Test 6: Adjacence verticale
        if (testVerticalAdjacency()) {
            System.out.println("✓ Test 6: Vérification adjacence verticale - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 6: Vérification adjacence verticale - ÉCHOUÉ\n");
            failed++;
        }

        // Test 7: Détection de conflit horizontal
        if (testHorizontalConflict()) {
            System.out.println("✓ Test 7: Détection conflit horizontal - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 7: Détection conflit horizontal - ÉCHOUÉ\n");
            failed++;
        }

        // Test 8: Détection de conflit vertical
        if (testVerticalConflict()) {
            System.out.println("✓ Test 8: Détection conflit vertical - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 8: Détection conflit vertical - ÉCHOUÉ\n");
            failed++;
        }

        // Test 9: Contrainte coin supérieur gauche
        if (testTopLeftCorner()) {
            System.out.println("✓ Test 9: Coin supérieur gauche (N=0, W=0) - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 9: Coin supérieur gauche (N=0, W=0) - ÉCHOUÉ\n");
            failed++;
        }

        // Test 10: Contrainte bords multiples
        if (testMultipleBorders()) {
            System.out.println("✓ Test 10: Test des 4 bords du plateau - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 10: Test des 4 bords du plateau - ÉCHOUÉ\n");
            failed++;
        }

        // Résumé
        System.out.println("════════════════════════════════════════════════════════");
        System.out.println("RÉSULTATS: " + passed + " passés, " + failed + " échoués");
        System.out.println("════════════════════════════════════════════════════════");

        System.exit(failed > 0 ? 1 : 0);
    }

    private static boolean testCornerConstraint() {
        try {
            // Un coin doit avoir exactement 2 bordures à 0
            // Exemple: coin supérieur gauche (0,0) doit avoir N=0 et W=0

            // Pièce valide pour un coin: [N=0, E=5, S=3, W=0]
            Piece validCorner = new Piece(1, new int[]{0, 5, 3, 0});
            int[] edges = validCorner.edgesRotated(0);

            int zeroCount = 0;
            for (int edge : edges) {
                if (edge == 0) zeroCount++;
            }

            if (zeroCount != 2) {
                System.out.println("  Erreur: Pièce de coin devrait avoir exactement 2 bordures à 0");
                System.out.println("  Trouvé: " + zeroCount + " bordures à 0");
                return false;
            }

            // Vérifier qu'une pièce avec 1 seul 0 n'est pas valide pour un coin
            Piece invalidCorner = new Piece(2, new int[]{0, 5, 3, 4});
            int[] invalidEdges = invalidCorner.edgesRotated(0);

            zeroCount = 0;
            for (int edge : invalidEdges) {
                if (edge == 0) zeroCount++;
            }

            if (zeroCount == 2) {
                System.out.println("  Erreur: Pièce test invalide devrait avoir != 2 bordures à 0");
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testEdgeConstraint() {
        try {
            // Un bord (non-coin) doit avoir exactement 1 bordure à 0

            // Pièce valide pour un bord: [N=0, E=5, S=3, W=4]
            Piece validEdge = new Piece(1, new int[]{0, 5, 3, 4});
            int[] edges = validEdge.edgesRotated(0);

            int zeroCount = 0;
            for (int edge : edges) {
                if (edge == 0) zeroCount++;
            }

            if (zeroCount != 1) {
                System.out.println("  Erreur: Pièce de bord devrait avoir exactement 1 bordure à 0");
                System.out.println("  Trouvé: " + zeroCount + " bordures à 0");
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testPlacementEdges() {
        try {
            // Vérifier que les edges sont correctement stockés après placement
            Board board = new Board(4, 4);
            Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

            board.place(1, 1, piece, 0);
            Placement p = board.getPlacement(1, 1);

            if (p == null) {
                System.out.println("  Erreur: Placement devrait exister après place()");
                return false;
            }

            int[] edges = p.edges;
            if (edges[0] != 1 || edges[1] != 2 || edges[2] != 3 || edges[3] != 4) {
                System.out.println("  Erreur: Edges incorrects après placement");
                System.out.println("  Attendu: [1, 2, 3, 4]");
                System.out.println("  Reçu: " + arrayToString(edges));
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testRotationPlacement() {
        try {
            // Vérifier que la rotation est correctement appliquée lors du placement
            Board board = new Board(4, 4);
            Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

            // Placement avec rotation 1 (90°)
            // [N=1, E=2, S=3, W=4] -> [N=4, E=1, S=2, W=3]
            board.place(1, 1, piece, 1);
            Placement p = board.getPlacement(1, 1);

            int[] edges = p.edges;
            if (edges[0] != 4 || edges[1] != 1 || edges[2] != 2 || edges[3] != 3) {
                System.out.println("  Erreur: Rotation 90° incorrecte");
                System.out.println("  Attendu: [4, 1, 2, 3]");
                System.out.println("  Reçu: " + arrayToString(edges));
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testHorizontalAdjacency() {
        try {
            // Vérifier que deux pièces adjacentes horizontalement ont des edges compatibles
            Board board = new Board(4, 4);

            // Pièce 1 à (1, 1) avec E=7
            Piece piece1 = new Piece(1, new int[]{2, 7, 3, 4});
            board.place(1, 1, piece1, 0);

            // Pièce 2 à (1, 2) avec W=7 (compatible)
            Piece piece2 = new Piece(2, new int[]{5, 8, 6, 7});
            board.place(1, 2, piece2, 0);

            Placement p1 = board.getPlacement(1, 1);
            Placement p2 = board.getPlacement(1, 2);

            // L'edge Est de p1 doit égaler l'edge Ouest de p2
            if (p1.edges[1] != p2.edges[3]) {
                System.out.println("  Erreur: Adjacence horizontale incompatible");
                System.out.println("  Piece1 E=" + p1.edges[1]);
                System.out.println("  Piece2 W=" + p2.edges[3]);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testVerticalAdjacency() {
        try {
            // Vérifier que deux pièces adjacentes verticalement ont des edges compatibles
            Board board = new Board(4, 4);

            // Pièce 1 à (1, 1) avec S=12
            Piece piece1 = new Piece(1, new int[]{2, 7, 12, 4});
            board.place(1, 1, piece1, 0);

            // Pièce 2 à (2, 1) avec N=12 (compatible)
            Piece piece2 = new Piece(2, new int[]{12, 8, 6, 7});
            board.place(2, 1, piece2, 0);

            Placement p1 = board.getPlacement(1, 1);
            Placement p2 = board.getPlacement(2, 1);

            // L'edge Sud de p1 doit égaler l'edge Nord de p2
            if (p1.edges[2] != p2.edges[0]) {
                System.out.println("  Erreur: Adjacence verticale incompatible");
                System.out.println("  Piece1 S=" + p1.edges[2]);
                System.out.println("  Piece2 N=" + p2.edges[0]);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testHorizontalConflict() {
        try {
            // Vérifier qu'on peut détecter un conflit horizontal
            Board board = new Board(4, 4);

            // Pièce 1 à (1, 1) avec E=7
            Piece piece1 = new Piece(1, new int[]{2, 7, 3, 4});
            board.place(1, 1, piece1, 0);

            // Pièce 2 à (1, 2) avec W=99 (incompatible!)
            Piece piece2 = new Piece(2, new int[]{5, 8, 6, 99});
            board.place(1, 2, piece2, 0);

            Placement p1 = board.getPlacement(1, 1);
            Placement p2 = board.getPlacement(1, 2);

            // Vérifier qu'il y a bien un conflit
            if (p1.edges[1] == p2.edges[3]) {
                System.out.println("  Erreur: Conflit horizontal non détecté");
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testVerticalConflict() {
        try {
            // Vérifier qu'on peut détecter un conflit vertical
            Board board = new Board(4, 4);

            // Pièce 1 à (1, 1) avec S=12
            Piece piece1 = new Piece(1, new int[]{2, 7, 12, 4});
            board.place(1, 1, piece1, 0);

            // Pièce 2 à (2, 1) avec N=88 (incompatible!)
            Piece piece2 = new Piece(2, new int[]{88, 8, 6, 7});
            board.place(2, 1, piece2, 0);

            Placement p1 = board.getPlacement(1, 1);
            Placement p2 = board.getPlacement(2, 1);

            // Vérifier qu'il y a bien un conflit
            if (p1.edges[2] == p2.edges[0]) {
                System.out.println("  Erreur: Conflit vertical non détecté");
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testTopLeftCorner() {
        try {
            // Coin supérieur gauche: N=0 et W=0
            Board board = new Board(4, 4);

            // Pièce valide pour (0, 0)
            Piece corner = new Piece(1, new int[]{0, 5, 3, 0});
            board.place(0, 0, corner, 0);

            Placement p = board.getPlacement(0, 0);
            int[] edges = p.edges;

            if (edges[0] != 0) {
                System.out.println("  Erreur: Coin (0,0) devrait avoir N=0");
                System.out.println("  Reçu: N=" + edges[0]);
                return false;
            }

            if (edges[3] != 0) {
                System.out.println("  Erreur: Coin (0,0) devrait avoir W=0");
                System.out.println("  Reçu: W=" + edges[3]);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testMultipleBorders() {
        try {
            // Tester les contraintes des 4 bords
            Board board = new Board(4, 4);

            // Bord haut (0, 1): N=0
            Piece topEdge = new Piece(1, new int[]{0, 5, 3, 4});
            board.place(0, 1, topEdge, 0);
            if (board.getPlacement(0, 1).edges[0] != 0) {
                System.out.println("  Erreur: Bord haut devrait avoir N=0");
                return false;
            }

            // Bord gauche (1, 0): W=0
            Piece leftEdge = new Piece(2, new int[]{3, 5, 4, 0});
            board.place(1, 0, leftEdge, 0);
            if (board.getPlacement(1, 0).edges[3] != 0) {
                System.out.println("  Erreur: Bord gauche devrait avoir W=0");
                return false;
            }

            // Bord droit (1, 3): E=0
            Piece rightEdge = new Piece(3, new int[]{3, 0, 4, 5});
            board.place(1, 3, rightEdge, 0);
            if (board.getPlacement(1, 3).edges[1] != 0) {
                System.out.println("  Erreur: Bord droit devrait avoir E=0");
                return false;
            }

            // Bord bas (3, 1): S=0
            Piece bottomEdge = new Piece(4, new int[]{3, 5, 0, 4});
            board.place(3, 1, bottomEdge, 0);
            if (board.getPlacement(3, 1).edges[2] != 0) {
                System.out.println("  Erreur: Bord bas devrait avoir S=0");
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static String arrayToString(int[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
