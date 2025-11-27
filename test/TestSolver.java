import model.Board;
import model.Piece;
import solver.EternitySolver;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests du solveur avec des puzzles simples dont on connaît la solution
 */
public class TestSolver {

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           TESTS DU SOLVEUR - PUZZLES SIMPLES              ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        testTrivial2x2();
        testSimple2x2();
        testSimple3x3();
        testInvalidPuzzle();

        printResults();
    }

    /**
     * Test trivial: 2x2 avec une seule pièce (3 pièces de bordure pré-placées)
     */
    private static void testTrivial2x2() {
        testsRun++;
        try {
            System.out.println("Test: Puzzle 2x2 trivial (1 pièce à placer)...");

            Board board = new Board(2, 2);
            Map<Integer, Piece> pieces = new HashMap<>();

            // Créer 4 pièces de coin
            // Pièce 1: coin haut-gauche (0, ?, ?, 0)
            pieces.put(1, new Piece(1, new int[]{0, 1, 1, 0}));
            // Pièce 2: coin haut-droit (0, 0, ?, ?)
            pieces.put(2, new Piece(2, new int[]{0, 0, 1, 1}));
            // Pièce 3: coin bas-gauche (?, ?, 0, 0)
            pieces.put(3, new Piece(3, new int[]{1, 1, 0, 0}));
            // Pièce 4: coin bas-droit (?, 0, 0, ?)
            pieces.put(4, new Piece(4, new int[]{1, 0, 0, 1}));

            // Pré-placer 3 pièces
            board.place(0, 0, pieces.get(1), 0);
            board.place(0, 1, pieces.get(2), 0);
            board.place(1, 0, pieces.get(3), 0);

            // Retirer ces pièces de la liste
            pieces.remove(1);
            pieces.remove(2);
            pieces.remove(3);

            // Résoudre
            EternitySolver.resetGlobalState();
            EternitySolver solver = new EternitySolver();
            solver.setDisplayConfig(false, 999);

            boolean solved = solver.solve(board, pieces);

            assert solved : "Le puzzle devrait être résolu";
            assert !board.isEmpty(1, 1) : "La dernière case devrait être remplie";

            testsPassed++;
            System.out.println("  ✓ Puzzle 2x2 trivial résolu");
        } catch (AssertionError e) {
            System.out.println("  ✗ Test puzzle 2x2 trivial: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ✗ Test puzzle 2x2 trivial: Erreur - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test simple: 2x2 complet à résoudre
     */
    private static void testSimple2x2() {
        testsRun++;
        try {
            System.out.println("Test: Puzzle 2x2 simple (4 pièces à placer)...");

            Board board = new Board(2, 2);
            Map<Integer, Piece> pieces = new HashMap<>();

            // Créer 4 pièces de coin qui s'emboîtent
            pieces.put(1, new Piece(1, new int[]{0, 1, 1, 0}));  // Haut-gauche
            pieces.put(2, new Piece(2, new int[]{0, 0, 1, 1}));  // Haut-droit
            pieces.put(3, new Piece(3, new int[]{1, 1, 0, 0}));  // Bas-gauche
            pieces.put(4, new Piece(4, new int[]{1, 0, 0, 1}));  // Bas-droit

            // Résoudre
            EternitySolver.resetGlobalState();
            EternitySolver solver = new EternitySolver();
            solver.setDisplayConfig(false, 999);

            long startTime = System.currentTimeMillis();
            boolean solved = solver.solve(board, pieces);
            long duration = System.currentTimeMillis() - startTime;

            assert solved : "Le puzzle devrait être résolu";

            // Vérifier que toutes les cases sont remplies
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 2; c++) {
                    assert !board.isEmpty(r, c) : "Case (" + r + "," + c + ") devrait être remplie";
                }
            }

            testsPassed++;
            System.out.println("  ✓ Puzzle 2x2 simple résolu en " + duration + " ms");
        } catch (AssertionError e) {
            System.out.println("  ✗ Test puzzle 2x2 simple: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ✗ Test puzzle 2x2 simple: Erreur - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test: 3x3 simple
     */
    private static void testSimple3x3() {
        testsRun++;
        try {
            System.out.println("Test: Puzzle 3x3 simple (9 pièces à placer)...");

            Board board = new Board(3, 3);
            Map<Integer, Piece> pieces = new HashMap<>();

            // Créer 9 pièces qui s'emboîtent
            // Ligne 1
            pieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));  // Coin haut-gauche
            pieces.put(2, new Piece(2, new int[]{0, 1, 2, 1}));  // Bordure haut
            pieces.put(3, new Piece(3, new int[]{0, 0, 2, 1}));  // Coin haut-droit

            // Ligne 2
            pieces.put(4, new Piece(4, new int[]{2, 1, 2, 0}));  // Bordure gauche
            pieces.put(5, new Piece(5, new int[]{2, 1, 2, 1}));  // Centre
            pieces.put(6, new Piece(6, new int[]{2, 0, 2, 1}));  // Bordure droite

            // Ligne 3
            pieces.put(7, new Piece(7, new int[]{2, 1, 0, 0}));  // Coin bas-gauche
            pieces.put(8, new Piece(8, new int[]{2, 1, 0, 1}));  // Bordure bas
            pieces.put(9, new Piece(9, new int[]{2, 0, 0, 1}));  // Coin bas-droit

            // Résoudre
            EternitySolver.resetGlobalState();
            EternitySolver solver = new EternitySolver();
            solver.setDisplayConfig(false, 999);

            long startTime = System.currentTimeMillis();
            boolean solved = solver.solve(board, pieces);
            long duration = System.currentTimeMillis() - startTime;

            assert solved : "Le puzzle devrait être résolu";

            // Vérifier que toutes les cases sont remplies
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    assert !board.isEmpty(r, c) : "Case (" + r + "," + c + ") devrait être remplie";
                }
            }

            testsPassed++;
            System.out.println("  ✓ Puzzle 3x3 simple résolu en " + duration + " ms");
        } catch (AssertionError e) {
            System.out.println("  ✗ Test puzzle 3x3 simple: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ✗ Test puzzle 3x3 simple: Erreur - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test: Puzzle invalide (pas de solution)
     */
    private static void testInvalidPuzzle() {
        testsRun++;
        try {
            System.out.println("Test: Puzzle 2x2 invalide (pas de solution)...");

            Board board = new Board(2, 2);
            Map<Integer, Piece> pieces = new HashMap<>();

            // Créer 4 pièces qui NE s'emboîtent PAS
            pieces.put(1, new Piece(1, new int[]{0, 1, 1, 0}));  // Coin haut-gauche
            pieces.put(2, new Piece(2, new int[]{0, 0, 1, 2}));  // Haut-droit (arête Ouest = 2, ne match pas)
            pieces.put(3, new Piece(3, new int[]{1, 1, 0, 0}));  // Bas-gauche
            pieces.put(4, new Piece(4, new int[]{2, 0, 0, 1}));  // Bas-droit (arête Nord = 2, ne match pas)

            // Résoudre
            EternitySolver.resetGlobalState();
            EternitySolver solver = new EternitySolver();
            solver.setDisplayConfig(false, 999);

            long startTime = System.currentTimeMillis();
            boolean solved = solver.solve(board, pieces);
            long duration = System.currentTimeMillis() - startTime;

            assert !solved : "Le puzzle ne devrait PAS être résolu";

            testsPassed++;
            System.out.println("  ✓ Puzzle invalide correctement détecté en " + duration + " ms");
        } catch (AssertionError e) {
            System.out.println("  ✗ Test puzzle invalide: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ✗ Test puzzle invalide: Erreur - " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("\n✓ TOUS LES TESTS DU SOLVEUR ONT RÉUSSI!");
        } else {
            System.out.println("\n✗ CERTAINS TESTS ONT ÉCHOUÉ!");
            throw new RuntimeException("Tests du solveur échoués");
        }
    }
}
