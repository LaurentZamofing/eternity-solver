import model.Board;
import model.Piece;
import model.Placement;

/**
 * Tests de base pour valider les fonctionnalités fondamentales
 */
public class TestBasics {

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         TESTS DE BASE - FONCTIONNALITÉS FONDAMENTALES     ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        testPieceCreation();
        testPieceRotation();
        testBoardCreation();
        testBoardPlacement();
        testBoardRemoval();
        testEdgeMatching();
        testBorderConstraints();

        printResults();
    }

    private static void testPieceCreation() {
        testsRun++;
        try {
            int[] edges = {0, 1, 2, 3};
            Piece piece = new Piece(1, edges);

            assert piece.getId() == 1 : "ID incorrect";
            assert piece.getEdges()[0] == 0 : "Arête Nord incorrecte";
            assert piece.getEdges()[1] == 1 : "Arête Est incorrecte";
            assert piece.getEdges()[2] == 2 : "Arête Sud incorrecte";
            assert piece.getEdges()[3] == 3 : "Arête Ouest incorrecte";

            testsPassed++;
            System.out.println("✓ Test création de pièce");
        } catch (AssertionError e) {
            System.out.println("✗ Test création de pièce: " + e.getMessage());
        }
    }

    private static void testPieceRotation() {
        testsRun++;
        try {
            int[] edges = {0, 1, 2, 3};
            Piece piece = new Piece(1, edges);

            // Rotation de 90° (1 fois)
            int[] rotated90 = piece.edgesRotated(1);
            assert rotated90[0] == 3 : "Rotation 90° Nord incorrect";
            assert rotated90[1] == 0 : "Rotation 90° Est incorrect";
            assert rotated90[2] == 1 : "Rotation 90° Sud incorrect";
            assert rotated90[3] == 2 : "Rotation 90° Ouest incorrect";

            // Rotation de 180° (2 fois)
            int[] rotated180 = piece.edgesRotated(2);
            assert rotated180[0] == 2 : "Rotation 180° Nord incorrect";
            assert rotated180[1] == 3 : "Rotation 180° Est incorrect";
            assert rotated180[2] == 0 : "Rotation 180° Sud incorrect";
            assert rotated180[3] == 1 : "Rotation 180° Ouest incorrect";

            // Rotation de 270° (3 fois)
            int[] rotated270 = piece.edgesRotated(3);
            assert rotated270[0] == 1 : "Rotation 270° Nord incorrect";
            assert rotated270[1] == 2 : "Rotation 270° Est incorrect";
            assert rotated270[2] == 3 : "Rotation 270° Sud incorrect";
            assert rotated270[3] == 0 : "Rotation 270° Ouest incorrect";

            testsPassed++;
            System.out.println("✓ Test rotation de pièce");
        } catch (AssertionError e) {
            System.out.println("✗ Test rotation de pièce: " + e.getMessage());
        }
    }

    private static void testBoardCreation() {
        testsRun++;
        try {
            Board board = new Board(3, 3);

            assert board.getRows() == 3 : "Nombre de lignes incorrect";
            assert board.getCols() == 3 : "Nombre de colonnes incorrect";

            // Vérifier que toutes les cases sont vides
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    assert board.isEmpty(r, c) : "Case (" + r + "," + c + ") devrait être vide";
                }
            }

            testsPassed++;
            System.out.println("✓ Test création de board");
        } catch (AssertionError e) {
            System.out.println("✗ Test création de board: " + e.getMessage());
        }
    }

    private static void testBoardPlacement() {
        testsRun++;
        try {
            Board board = new Board(3, 3);
            int[] edges = {0, 1, 2, 3};
            Piece piece = new Piece(1, edges);

            board.place(1, 1, piece, 0);

            assert !board.isEmpty(1, 1) : "Case devrait être occupée";
            Placement p = board.getPlacement(1, 1);
            assert p != null : "Placement devrait exister";
            assert p.getPieceId() == 1 : "ID de pièce incorrect";
            assert p.getRotation() == 0 : "Rotation incorrecte";

            testsPassed++;
            System.out.println("✓ Test placement sur board");
        } catch (AssertionError e) {
            System.out.println("✗ Test placement sur board: " + e.getMessage());
        }
    }

    private static void testBoardRemoval() {
        testsRun++;
        try {
            Board board = new Board(3, 3);
            int[] edges = {0, 1, 2, 3};
            Piece piece = new Piece(1, edges);

            board.place(1, 1, piece, 0);
            assert !board.isEmpty(1, 1) : "Case devrait être occupée";

            board.remove(1, 1);
            assert board.isEmpty(1, 1) : "Case devrait être vide après suppression";
            assert board.getPlacement(1, 1) == null : "Placement devrait être null";

            testsPassed++;
            System.out.println("✓ Test suppression sur board");
        } catch (AssertionError e) {
            System.out.println("✗ Test suppression sur board: " + e.getMessage());
        }
    }

    private static void testEdgeMatching() {
        testsRun++;
        try {
            Board board = new Board(2, 2);

            // Pièce 1: arête Est = 5
            int[] edges1 = {0, 5, 0, 0};
            Piece piece1 = new Piece(1, edges1);

            // Pièce 2: arête Ouest = 5 (match!)
            int[] edges2 = {0, 0, 0, 5};
            Piece piece2 = new Piece(2, edges2);

            // Pièce 3: arête Ouest = 3 (pas de match)
            int[] edges3 = {0, 0, 0, 3};
            Piece piece3 = new Piece(3, edges3);

            board.place(0, 0, piece1, 0);

            // Vérifier que piece2 match avec piece1
            Placement p1 = board.getPlacement(0, 0);
            int piece1East = p1.edges[1];
            int piece2West = piece2.getEdges()[3];
            assert piece1East == piece2West : "Les pièces devraient matcher";

            // Vérifier que piece3 ne match pas avec piece1
            int piece3West = piece3.getEdges()[3];
            assert piece1East != piece3West : "Les pièces ne devraient pas matcher";

            testsPassed++;
            System.out.println("✓ Test correspondance des arêtes");
        } catch (AssertionError e) {
            System.out.println("✗ Test correspondance des arêtes: " + e.getMessage());
        }
    }

    private static void testBorderConstraints() {
        testsRun++;
        try {
            Board board = new Board(3, 3);

            // Pièce de bordure (arête Nord = 0)
            int[] borderEdges = {0, 1, 2, 3};
            Piece borderPiece = new Piece(1, borderEdges);

            // Pièce intérieure (pas d'arête 0)
            int[] innerEdges = {1, 2, 3, 4};
            Piece innerPiece = new Piece(2, innerEdges);

            // Placer la pièce de bordure en haut
            board.place(0, 1, borderPiece, 0);
            Placement p = board.getPlacement(0, 1);
            assert p.edges[0] == 0 : "Arête Nord devrait être 0 en bordure";

            // Vérifier qu'une pièce intérieure ne devrait pas aller en bordure
            assert innerPiece.getEdges()[0] != 0 : "Pièce intérieure ne devrait pas avoir d'arête 0";

            testsPassed++;
            System.out.println("✓ Test contraintes de bordure");
        } catch (AssertionError e) {
            System.out.println("✗ Test contraintes de bordure: " + e.getMessage());
        }
    }

    private static void printResults() {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                      RÉSULTATS                             ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Tests exécutés: %-43d║%n", testsRun);
        System.out.printf("║ Tests réussis:  %-43d║%n", testsPassed);
        System.out.printf("║ Tests échoués:  %-43d║%n", testsRun - testsPassed);
        System.out.println("╚════════════════════════════════════════════════════════════╝");

        if (testsPassed == testsRun) {
            System.out.println("\n✓ TOUS LES TESTS DE BASE ONT RÉUSSI!");
        } else {
            System.out.println("\n✗ CERTAINS TESTS ONT ÉCHOUÉ!");
            throw new RuntimeException("Tests de base échoués");
        }
    }
}
