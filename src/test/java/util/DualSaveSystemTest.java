package util;

import model.Board;
import model.Piece;
import util.SaveStateManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests complets pour le système de sauvegarde dual (current + best)
 */
public class DualSaveSystemTest {

    private static final String TEST_PUZZLE = "test_dual_save";
    private static final String SAVE_DIR = "saves/";
    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║         TESTS DU SYSTÈME DE SAUVEGARDE DUAL                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Nettoyer les anciens tests
        cleanupTestFiles();

        // Test 1: Vérifier que saveState crée current et best
        testSaveStateCreatesBothFiles();

        // Test 2: Vérifier que findCurrentSave trouve le fichier current
        testFindCurrentSave();

        // Test 3: Vérifier que findAllSaves trouve les fichiers best
        testFindAllSaves();

        // Test 4: Vérifier que seuls les nouveaux records créent des best saves
        testNewRecordDetection();

        // Test 5: Vérifier le cleanup des anciennes sauvegardes best
        testCleanupOldBestSaves();

        // Test 6: Vérifier que current est toujours mis à jour
        testCurrentAlwaysUpdated();

        // Test 7: Vérifier le chargement depuis current
        testLoadFromCurrent();

        // Test 8: Vérifier le chargement depuis best
        testLoadFromBest();

        // Test 9: Vérifier que best n'est pas écrasé
        testBestNotOverwritten();

        // Nettoyer après les tests
        cleanupTestFiles();

        // Rapport final
        printSummary();
    }

    /**
     * Test 1: Vérifier que saveState crée à la fois current et best
     */
    private static void testSaveStateCreatesBothFiles() {
        testsRun++;
        System.out.println("Test 1: saveState crée current et best");

        try {
            // Créer un board de test
            Board board = new Board(4, 4);
            Map<Integer, Piece> allPieces = createTestPieces(16);

            // Placer 5 pièces (multiple de 5 = intervalle de sauvegarde)
            for (int i = 0; i < 5; i++) {
                Piece p = allPieces.get(i + 1);
                board.place(i / 4, i % 4, p, 0);
            }

            // Sauvegarder
            List<Integer> unusedIds = new ArrayList<>();
            for (int i = 6; i <= 16; i++) {
                unusedIds.add(i);
            }

            List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                placementOrder.add(new SaveStateManager.PlacementInfo(i / 4, i % 4, i + 1, 0));
            }

            SaveStateManager.saveState(TEST_PUZZLE, board, allPieces, unusedIds, placementOrder);

            // Vérifier que current existe
            File currentFile = new File(SAVE_DIR + "test_dual_save_current.txt");
            if (!currentFile.exists()) {
                fail("Fichier current n'a pas été créé");
                return;
            }

            // Vérifier que best_5 existe (5 est un multiple de l'intervalle)
            File bestFile = new File(SAVE_DIR + "test_dual_save_best_5.txt");
            if (!bestFile.exists()) {
                fail("Fichier best_5 n'a pas été créé");
                return;
            }

            pass("current et best créés correctement");

        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 2: Vérifier findCurrentSave
     */
    private static void testFindCurrentSave() {
        testsRun++;
        System.out.println("\nTest 2: findCurrentSave trouve le fichier current");

        try {
            File currentFile = SaveStateManager.findCurrentSave(TEST_PUZZLE);

            if (currentFile == null) {
                fail("findCurrentSave a retourné null");
                return;
            }

            if (!currentFile.exists()) {
                fail("Le fichier retourné n'existe pas");
                return;
            }

            if (!currentFile.getName().equals("test_dual_save_current.txt")) {
                fail("Mauvais nom de fichier: " + currentFile.getName());
                return;
            }

            pass("findCurrentSave fonctionne correctement");

        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 3: Vérifier findAllSaves
     */
    private static void testFindAllSaves() {
        testsRun++;
        System.out.println("\nTest 3: findAllSaves trouve les fichiers best");

        try {
            List<File> bestFiles = SaveStateManager.findAllSaves(TEST_PUZZLE);

            if (bestFiles == null || bestFiles.isEmpty()) {
                fail("findAllSaves n'a trouvé aucun fichier");
                return;
            }

            // Devrait trouver best_5
            boolean found = false;
            for (File f : bestFiles) {
                if (f.getName().equals("test_dual_save_best_5.txt")) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                fail("best_5.txt non trouvé dans la liste");
                return;
            }

            pass("findAllSaves fonctionne correctement (" + bestFiles.size() + " fichier(s))");

        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 4: Vérifier que seuls les nouveaux records créent des best saves
     */
    private static void testNewRecordDetection() {
        testsRun++;
        System.out.println("\nTest 4: Détection des nouveaux records");

        try {
            Board board = new Board(4, 4);
            Map<Integer, Piece> allPieces = createTestPieces(16);

            // Placer 3 pièces (pas un multiple de 5, pas un record)
            for (int i = 0; i < 3; i++) {
                Piece p = allPieces.get(i + 1);
                board.place(i / 4, i % 4, p, 0);
            }

            List<Integer> unusedIds = new ArrayList<>();
            for (int i = 4; i <= 16; i++) {
                unusedIds.add(i);
            }

            List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                placementOrder.add(new SaveStateManager.PlacementInfo(i / 4, i % 4, i + 1, 0));
            }

            SaveStateManager.saveState(TEST_PUZZLE, board, allPieces, unusedIds, placementOrder);

            // best_3 ne devrait PAS exister (3 n'est pas un multiple de 5 et < 5)
            File best3File = new File(SAVE_DIR + "test_dual_save_best_3.txt");
            if (best3File.exists()) {
                fail("best_3.txt ne devrait pas exister (pas un intervalle ni un record)");
                return;
            }

            // Maintenant placer 10 pièces (nouveau record)
            board = new Board(4, 4);
            allPieces = createTestPieces(16);
            for (int i = 0; i < 10; i++) {
                Piece p = allPieces.get(i + 1);
                board.place(i / 4, i % 4, p, 0);
            }

            unusedIds.clear();
            for (int i = 11; i <= 16; i++) {
                unusedIds.add(i);
            }

            placementOrder.clear();
            for (int i = 0; i < 10; i++) {
                placementOrder.add(new SaveStateManager.PlacementInfo(i / 4, i % 4, i + 1, 0));
            }

            SaveStateManager.saveState(TEST_PUZZLE, board, allPieces, unusedIds, placementOrder);

            // best_10 devrait exister (multiple de 5 ET nouveau record)
            File best10File = new File(SAVE_DIR + "test_dual_save_best_10.txt");
            if (!best10File.exists()) {
                fail("best_10.txt devrait exister (multiple de 5 et record)");
                return;
            }

            pass("Détection des nouveaux records fonctionne");

        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 5: Vérifier le cleanup des anciennes sauvegardes
     */
    private static void testCleanupOldBestSaves() {
        testsRun++;
        System.out.println("\nTest 5: Cleanup des anciennes sauvegardes best");

        try {
            // Créer 12 sauvegardes best pour tester le cleanup (max 10)
            Board board = new Board(4, 4);
            Map<Integer, Piece> allPieces = createTestPieces(16);

            for (int depth = 5; depth <= 60; depth += 5) {
                board = new Board(4, 4);
                allPieces = createTestPieces(16);

                int piecesToPlace = Math.min(depth, 16);
                for (int i = 0; i < piecesToPlace; i++) {
                    Piece p = allPieces.get(i + 1);
                    board.place(i / 4, i % 4, p, 0);
                }

                List<Integer> unusedIds = new ArrayList<>();
                for (int i = piecesToPlace + 1; i <= 16; i++) {
                    unusedIds.add(i);
                }

                List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
                for (int i = 0; i < piecesToPlace; i++) {
                    placementOrder.add(new SaveStateManager.PlacementInfo(i / 4, i % 4, i + 1, 0));
                }

                SaveStateManager.saveState(TEST_PUZZLE, board, allPieces, unusedIds, placementOrder);
            }

            // Vérifier qu'on a au maximum 10 fichiers best
            List<File> bestFiles = SaveStateManager.findAllSaves(TEST_PUZZLE);

            if (bestFiles.size() > 10) {
                fail("Trop de fichiers best: " + bestFiles.size() + " (max 10)");
                return;
            }

            pass("Cleanup fonctionne (" + bestFiles.size() + " fichiers best conservés)");

        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 6: Vérifier que current est toujours mis à jour
     */
    private static void testCurrentAlwaysUpdated() {
        testsRun++;
        System.out.println("\nTest 6: current est toujours mis à jour");

        try {
            File currentFile = new File(SAVE_DIR + "test_dual_save_current.txt");
            long lastModified = currentFile.lastModified();

            // Attendre un peu
            Thread.sleep(100);

            // Faire une nouvelle sauvegarde avec une profondeur différente
            Board board = new Board(4, 4);
            Map<Integer, Piece> allPieces = createTestPieces(16);

            for (int i = 0; i < 7; i++) {
                Piece p = allPieces.get(i + 1);
                board.place(i / 4, i % 4, p, 0);
            }

            List<Integer> unusedIds = new ArrayList<>();
            for (int i = 8; i <= 16; i++) {
                unusedIds.add(i);
            }

            List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                placementOrder.add(new SaveStateManager.PlacementInfo(i / 4, i % 4, i + 1, 0));
            }

            SaveStateManager.saveState(TEST_PUZZLE, board, allPieces, unusedIds, placementOrder);

            // Vérifier que current a été modifié
            long newModified = currentFile.lastModified();
            if (newModified <= lastModified) {
                fail("current n'a pas été mis à jour");
                return;
            }

            // Vérifier le contenu (devrait avoir depth=7)
            BufferedReader reader = new BufferedReader(new FileReader(currentFile));
            String line;
            boolean foundDepth = false;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("# Depth: ")) {
                    int depth = Integer.parseInt(line.replace("# Depth: ", "").trim());
                    if (depth == 7) {
                        foundDepth = true;
                    }
                    break;
                }
            }
            reader.close();

            if (!foundDepth) {
                fail("Depth incorrect dans current");
                return;
            }

            pass("current toujours mis à jour correctement");

        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 7: Vérifier le chargement depuis current
     */
    private static void testLoadFromCurrent() {
        testsRun++;
        System.out.println("\nTest 7: Chargement depuis current");

        try {
            File currentFile = SaveStateManager.findCurrentSave(TEST_PUZZLE);
            if (currentFile == null) {
                fail("Pas de fichier current");
                return;
            }

            SaveStateManager.SaveState state = SaveStateManager.loadStateFromFile(currentFile, TEST_PUZZLE);
            if (state == null) {
                fail("Chargement a retourné null");
                return;
            }

            if (state.depth != 7) {
                fail("Depth incorrect: " + state.depth + " (attendu 7)");
                return;
            }

            if (state.placementOrder == null || state.placementOrder.size() != 7) {
                fail("PlacementOrder incorrect");
                return;
            }

            pass("Chargement depuis current fonctionne");

        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 8: Vérifier le chargement depuis best
     */
    private static void testLoadFromBest() {
        testsRun++;
        System.out.println("\nTest 8: Chargement depuis best");

        try {
            List<File> bestFiles = SaveStateManager.findAllSaves(TEST_PUZZLE);
            if (bestFiles.isEmpty()) {
                fail("Aucun fichier best");
                return;
            }

            File bestFile = bestFiles.get(0);
            SaveStateManager.SaveState state = SaveStateManager.loadStateFromFile(bestFile, TEST_PUZZLE);

            if (state == null) {
                fail("Chargement a retourné null");
                return;
            }

            if (state.placementOrder == null) {
                fail("PlacementOrder est null");
                return;
            }

            pass("Chargement depuis best fonctionne");

        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 9: Vérifier que best n'est pas écrasé
     */
    private static void testBestNotOverwritten() {
        testsRun++;
        System.out.println("\nTest 9: best n'est pas écrasé");

        try {
            // Trouver un fichier best existant
            File best5 = new File(SAVE_DIR + "test_dual_save_best_5.txt");
            if (!best5.exists()) {
                fail("best_5 n'existe pas");
                return;
            }

            long originalModified = best5.lastModified();

            // Attendre un peu
            Thread.sleep(100);

            // Refaire une sauvegarde à depth=5
            Board board = new Board(4, 4);
            Map<Integer, Piece> allPieces = createTestPieces(16);

            for (int i = 0; i < 5; i++) {
                Piece p = allPieces.get(i + 1);
                board.place(i / 4, i % 4, p, 0);
            }

            List<Integer> unusedIds = new ArrayList<>();
            for (int i = 6; i <= 16; i++) {
                unusedIds.add(i);
            }

            List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                placementOrder.add(new SaveStateManager.PlacementInfo(i / 4, i % 4, i + 1, 0));
            }

            SaveStateManager.saveState(TEST_PUZZLE, board, allPieces, unusedIds, placementOrder);

            // Vérifier que best_5 n'a PAS été modifié
            long newModified = best5.lastModified();
            if (newModified != originalModified) {
                fail("best_5 a été écrasé!");
                return;
            }

            pass("best n'est jamais écrasé");

        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Nettoyer les fichiers de test
     */
    private static void cleanupTestFiles() {
        File saveDir = new File(SAVE_DIR);
        if (!saveDir.exists()) {
            return;
        }

        File[] files = saveDir.listFiles((dir, name) ->
            name.startsWith("test_dual_save")
        );

        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
    }

    /**
     * Créer des pièces de test
     */
    private static Map<Integer, Piece> createTestPieces(int count) {
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= count; i++) {
            int[] edges = {i % 10, (i + 1) % 10, (i + 2) % 10, (i + 3) % 10};
            pieces.put(i, new Piece(i, edges));
        }
        return pieces;
    }

    /**
     * Marquer un test comme réussi
     */
    private static void pass(String message) {
        testsPassed++;
        System.out.println("  ✓ " + message);
    }

    /**
     * Marquer un test comme échoué
     */
    private static void fail(String message) {
        testsFailed++;
        System.out.println("  ✗ ÉCHEC: " + message);
    }

    /**
     * Afficher le résumé des tests
     */
    private static void printSummary() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                      RÉSUMÉ DES TESTS                         ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.println("║ Tests exécutés: " + String.format("%-44d", testsRun) + " ║");
        System.out.println("║ Tests réussis:  " + String.format("%-44d", testsPassed) + " ║");
        System.out.println("║ Tests échoués:  " + String.format("%-44d", testsFailed) + " ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");

        if (testsFailed == 0) {
            System.out.println("\n✓ Tous les tests sont passés!");
        } else {
            System.out.println("\n✗ " + testsFailed + " test(s) ont échoué");
            System.exit(1);
        }
    }
}
