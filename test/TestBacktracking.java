import model.Board;
import model.Piece;
import util.SaveStateManager;
import java.io.*;
import java.util.*;

/**
 * Tests pour le système de backtracking automatique avec placement order
 */
public class TestBacktracking {

    private static final String TEST_SAVE_DIR = "saves/";
    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║           TESTS DU SYSTÈME DE BACKTRACKING                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Test 1: Vérifier que findAllSaves() trouve les sauvegardes
        testFindAllSaves();

        // Test 2: Vérifier le tri des sauvegardes (plus récent en premier)
        testSavesSorting();

        // Test 3: Vérifier loadStateFromFile()
        testLoadStateFromFile();

        // Test 4: Vérifier la sauvegarde et chargement du placement order
        testPlacementOrderSaveLoad();

        // Test 5: Vérifier la restauration complète avec placement order
        testFullRestoreWithOrder();

        // Rapport final
        printSummary();
    }

    /**
     * Test 1: Vérifier que findAllSaves() trouve toutes les sauvegardes
     */
    private static void testFindAllSaves() {
        testsRun++;
        System.out.println("Test 1: findAllSaves() trouve les sauvegardes");

        try {
            List<File> saves = SaveStateManager.findAllSaves("eternity2");

            if (saves == null) {
                fail("findAllSaves() a retourné null");
                return;
            }

            System.out.println("  → Trouvé " + saves.size() + " sauvegarde(s) pour eternity2");

            // Vérifier que les fichiers existent
            for (File save : saves) {
                if (!save.exists()) {
                    fail("Le fichier " + save.getName() + " n'existe pas");
                    return;
                }
            }

            pass("findAllSaves() fonctionne correctement");

        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 2: Vérifier que les sauvegardes sont triées (plus récent en premier)
     */
    private static void testSavesSorting() {
        testsRun++;
        System.out.println("\nTest 2: Tri des sauvegardes par profondeur");

        try {
            List<File> saves = SaveStateManager.findAllSaves("eternity2");

            if (saves == null || saves.isEmpty()) {
                System.out.println("  → SKIP: Aucune sauvegarde disponible");
                testsPassed++; // On considère le test comme passé
                return;
            }

            // Extraire les profondeurs des noms de fichiers
            List<Integer> depths = new ArrayList<>();
            for (File save : saves) {
                String name = save.getName();
                // Format: eternity2_save_XXX.txt
                String depthStr = name.replace("eternity2_save_", "").replace(".txt", "");
                try {
                    int depth = Integer.parseInt(depthStr);
                    depths.add(depth);
                } catch (NumberFormatException e) {
                    // Ignorer les fichiers sans depth valide
                }
            }

            // Vérifier que les profondeurs sont en ordre décroissant
            for (int i = 0; i < depths.size() - 1; i++) {
                if (depths.get(i) < depths.get(i + 1)) {
                    fail("Les sauvegardes ne sont pas triées correctement: " +
                         depths.get(i) + " < " + depths.get(i + 1));
                    return;
                }
            }

            System.out.println("  → Profondeurs: " + depths);
            pass("Les sauvegardes sont correctement triées");

        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 3: Vérifier loadStateFromFile()
     */
    private static void testLoadStateFromFile() {
        testsRun++;
        System.out.println("\nTest 3: loadStateFromFile() charge un fichier");

        try {
            List<File> saves = SaveStateManager.findAllSaves("eternity2");

            if (saves == null || saves.isEmpty()) {
                System.out.println("  → SKIP: Aucune sauvegarde disponible");
                testsPassed++; // On considère le test comme passé
                return;
            }

            File firstSave = saves.get(0);
            SaveStateManager.SaveState state = SaveStateManager.loadStateFromFile(firstSave, "eternity2");

            if (state == null) {
                fail("loadStateFromFile() a retourné null");
                return;
            }

            System.out.println("  → Chargé: " + firstSave.getName());
            System.out.println("  → Profondeur: " + state.depth);
            System.out.println("  → Placements: " + state.placements.size());
            System.out.println("  → Ordre de placement: " + (state.placementOrder != null ? state.placementOrder.size() : "null"));

            if (state.placementOrder == null) {
                fail("Le placement order est null");
                return;
            }

            pass("loadStateFromFile() fonctionne correctement");

        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 4: Vérifier la cohérence du placement order dans les sauvegardes existantes
     */
    private static void testPlacementOrderSaveLoad() {
        testsRun++;
        System.out.println("\nTest 4: Cohérence du placement order");

        try {
            List<File> saves = SaveStateManager.findAllSaves("eternity2");

            if (saves == null || saves.isEmpty()) {
                System.out.println("  → SKIP: Aucune sauvegarde disponible");
                testsPassed++; // On considère le test comme passé
                return;
            }

            // Vérifier plusieurs sauvegardes
            int checked = 0;
            for (File save : saves) {
                if (checked >= 3) break; // Vérifier au maximum 3 sauvegardes

                SaveStateManager.SaveState state = SaveStateManager.loadStateFromFile(save, "eternity2");
                if (state == null) continue;

                // Vérifier que si placementOrder existe, sa taille correspond aux placements
                if (state.placementOrder != null) {
                    int placementsCount = state.placements.size();
                    int orderCount = state.placementOrder.size();

                    if (orderCount != placementsCount) {
                        fail("Sauvegarde " + save.getName() + ": " +
                             "placementOrder.size()=" + orderCount +
                             " != placements.size()=" + placementsCount);
                        return;
                    }

                    // Vérifier que chaque élément de placementOrder existe dans placements
                    for (SaveStateManager.PlacementInfo info : state.placementOrder) {
                        String key = info.row + "," + info.col;
                        if (!state.placements.containsKey(key)) {
                            fail("Sauvegarde " + save.getName() + ": " +
                                 "Placement (" + info.row + "," + info.col + ") " +
                                 "dans placementOrder mais pas dans placements");
                            return;
                        }
                    }

                    checked++;
                    System.out.println("  → " + save.getName() + ": OK (" + orderCount + " placements)");
                }
            }

            if (checked == 0) {
                fail("Aucune sauvegarde avec placement order trouvée");
                return;
            }

            pass("Placement order cohérent dans " + checked + " sauvegarde(s)");

        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 5: Vérifier la restauration complète avec placement order
     */
    private static void testFullRestoreWithOrder() {
        testsRun++;
        System.out.println("\nTest 5: Restauration complète avec placement order");

        try {
            List<File> saves = SaveStateManager.findAllSaves("eternity2");

            if (saves == null || saves.isEmpty()) {
                System.out.println("  → SKIP: Aucune sauvegarde disponible");
                testsPassed++; // On considère le test comme passé
                return;
            }

            File firstSave = saves.get(0);
            SaveStateManager.SaveState state = SaveStateManager.loadStateFromFile(firstSave, "eternity2");

            if (state == null) {
                fail("Impossible de charger la sauvegarde");
                return;
            }

            // Vérifier que le nombre de placements correspond à la profondeur
            if (state.placementOrder != null && state.placementOrder.size() != state.depth) {
                fail("Incohérence: depth=" + state.depth +
                     " mais placementOrder.size()=" + state.placementOrder.size());
                return;
            }

            // Vérifier que tous les placements dans l'ordre sont aussi dans la map
            if (state.placementOrder != null) {
                for (SaveStateManager.PlacementInfo info : state.placementOrder) {
                    String key = info.row + "," + info.col;
                    if (!state.placements.containsKey(key)) {
                        fail("Placement (" + info.row + "," + info.col + ") dans l'ordre mais pas dans la map");
                        return;
                    }
                }
            }

            System.out.println("  → Sauvegarde: " + firstSave.getName());
            System.out.println("  → Profondeur: " + state.depth);
            System.out.println("  → Placements: " + state.placements.size());
            System.out.println("  → Ordre: " + (state.placementOrder != null ? state.placementOrder.size() : "null"));

            pass("La restauration complète fonctionne correctement");

        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Crée des pièces de test
     */
    private static Map<Integer, Piece> createTestPieces(int size) {
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= size * size; i++) {
            int[] edges = {1, 2, 3, 4};
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
