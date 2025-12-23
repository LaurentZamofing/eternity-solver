package util;

import model.Board;
import model.Piece;
import model.Placement;
import util.SaveStateManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests critiques pour la sauvegarde et le chargement
 */
public class SaveLoadTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static final String TEST_PUZZLE_NAME = "test_puzzle";

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║       TESTS SAUVEGARDE/CHARGEMENT - CRITIQUE               ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        // Nettoyer les anciennes sauvegardes de test
        cleanupTestSaves();

        testBasicSaveAndLoad();
        testSaveWithMultiplePieces();
        testLoadNonExistent();
        testSaveRestore();
        testSaveOverwrite();
        testSaveFileFormat();
        testRestorePreservesDepth();
        testAllPiecesMapIntegrity();
        testPuzzleNameInSave();

        // Nettoyer après les tests
        cleanupTestSaves();

        printResults();
    }

    /**
     * Test basique: sauvegarder et charger un état simple
     */
    private static void testBasicSaveAndLoad() {
        testsRun++;
        try {
            System.out.println("Test: Sauvegarde et chargement basique...");

            // Créer un board avec quelques pièces
            Board board = new Board(3, 3);
            Map<Integer, Piece> allPieces = new HashMap<>();
            List<Integer> unusedIds = new ArrayList<>();

            // Créer 5 pièces
            for (int i = 1; i <= 5; i++) {
                allPieces.put(i, new Piece(i, new int[]{0, i, i, 0}));
                unusedIds.add(i);
            }

            // Placer 2 pièces
            board.place(0, 0, allPieces.get(1), 0);
            board.place(0, 1, allPieces.get(2), 1);
            unusedIds.remove(Integer.valueOf(1));
            unusedIds.remove(Integer.valueOf(2));

            // Sauvegarder
            SaveStateManager.saveState(TEST_PUZZLE_NAME, board, allPieces, unusedIds);

            // Charger
            SaveStateManager.SaveState loaded = SaveStateManager.loadState(TEST_PUZZLE_NAME);

            assert loaded != null : "La sauvegarde devrait exister";
            assert loaded.rows == 3 : "Nombre de lignes incorrect";
            assert loaded.cols == 3 : "Nombre de colonnes incorrect";
            assert loaded.depth == 2 : "Profondeur incorrecte";
            assert loaded.unusedPieceIds.size() == 3 : "Nombre de pièces non utilisées incorrect";
            assert loaded.placements.size() == 2 : "Nombre de placements incorrect";

            testsPassed++;
            System.out.println("  ✓ Sauvegarde et chargement basique OK");
        } catch (AssertionError e) {
            System.out.println("  ✗ Test sauvegarde/chargement basique: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ✗ Test sauvegarde/chargement basique: Erreur - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test: Sauvegarder un board complet avec plusieurs pièces
     */
    private static void testSaveWithMultiplePieces() {
        testsRun++;
        try {
            System.out.println("Test: Sauvegarde avec plusieurs pièces...");

            Board board = new Board(3, 3);
            Map<Integer, Piece> allPieces = new HashMap<>();
            List<Integer> unusedIds = new ArrayList<>();

            // Créer 9 pièces
            for (int i = 1; i <= 9; i++) {
                allPieces.put(i, new Piece(i, new int[]{i, i+1, i+2, i+3}));
                unusedIds.add(i);
            }

            // Placer 6 pièces
            int pieceId = 1;
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 3; c++) {
                    board.place(r, c, allPieces.get(pieceId), pieceId % 4);
                    unusedIds.remove(Integer.valueOf(pieceId));
                    pieceId++;
                }
            }

            // Sauvegarder
            SaveStateManager.saveState(TEST_PUZZLE_NAME, board, allPieces, unusedIds);

            // Charger
            SaveStateManager.SaveState loaded = SaveStateManager.loadState(TEST_PUZZLE_NAME);

            assert loaded != null : "La sauvegarde devrait exister";
            assert loaded.depth == 6 : "Profondeur incorrecte: attendu 6, reçu " + loaded.depth;
            assert loaded.unusedPieceIds.size() == 3 : "Nombre de pièces non utilisées incorrect";
            assert loaded.placements.size() == 6 : "Nombre de placements incorrect";

            // Vérifier que les placements sont corrects
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 3; c++) {
                    String key = r + "," + c;
                    assert loaded.placements.containsKey(key) : "Placement " + key + " manquant";
                }
            }

            testsPassed++;
            System.out.println("  ✓ Sauvegarde avec plusieurs pièces OK");
        } catch (AssertionError e) {
            System.out.println("  ✗ Test sauvegarde multiple: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ✗ Test sauvegarde multiple: Erreur - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test: Charger une sauvegarde inexistante
     */
    private static void testLoadNonExistent() {
        testsRun++;
        try {
            System.out.println("Test: Chargement d'une sauvegarde inexistante...");

            SaveStateManager.SaveState loaded = SaveStateManager.loadState("puzzle_inexistant_xyz");

            assert loaded == null : "La sauvegarde ne devrait pas exister";

            testsPassed++;
            System.out.println("  ✓ Sauvegarde inexistante correctement gérée");
        } catch (AssertionError e) {
            System.out.println("  ✗ Test sauvegarde inexistante: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ✗ Test sauvegarde inexistante: Erreur - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test critique: Sauvegarder, charger et restaurer sur un nouveau board
     */
    private static void testSaveRestore() {
        testsRun++;
        try {
            System.out.println("Test: Sauvegarde et restauration complète...");

            // Créer un board original
            Board originalBoard = new Board(3, 3);
            Map<Integer, Piece> allPieces = new HashMap<>();
            List<Integer> unusedIds = new ArrayList<>();

            // Créer des pièces
            for (int i = 1; i <= 9; i++) {
                allPieces.put(i, new Piece(i, new int[]{0, i, i, 0}));
                unusedIds.add(i);
            }

            // Placer quelques pièces avec des rotations
            originalBoard.place(0, 0, allPieces.get(1), 0);
            originalBoard.place(0, 1, allPieces.get(2), 1);
            originalBoard.place(1, 1, allPieces.get(3), 2);
            originalBoard.place(2, 2, allPieces.get(4), 3);
            unusedIds.remove(Integer.valueOf(1));
            unusedIds.remove(Integer.valueOf(2));
            unusedIds.remove(Integer.valueOf(3));
            unusedIds.remove(Integer.valueOf(4));

            // Sauvegarder
            SaveStateManager.saveState(TEST_PUZZLE_NAME, originalBoard, allPieces, unusedIds);

            // Charger
            SaveStateManager.SaveState loaded = SaveStateManager.loadState(TEST_PUZZLE_NAME);
            assert loaded != null : "La sauvegarde devrait exister";

            // Créer un nouveau board et restaurer
            Board newBoard = new Board(3, 3);
            boolean restored = SaveStateManager.restoreState(loaded, newBoard, allPieces);

            assert restored : "La restauration devrait réussir";

            // Vérifier que le board est identique
            assert !newBoard.isEmpty(0, 0) : "Case (0,0) devrait être occupée";
            assert !newBoard.isEmpty(0, 1) : "Case (0,1) devrait être occupée";
            assert !newBoard.isEmpty(1, 1) : "Case (1,1) devrait être occupée";
            assert !newBoard.isEmpty(2, 2) : "Case (2,2) devrait être occupée";

            // Vérifier les IDs et rotations
            assert newBoard.getPlacement(0, 0).getPieceId() == 1 : "ID pièce (0,0) incorrect";
            assert newBoard.getPlacement(0, 0).getRotation() == 0 : "Rotation (0,0) incorrecte";
            assert newBoard.getPlacement(0, 1).getPieceId() == 2 : "ID pièce (0,1) incorrect";
            assert newBoard.getPlacement(0, 1).getRotation() == 1 : "Rotation (0,1) incorrecte";
            assert newBoard.getPlacement(1, 1).getPieceId() == 3 : "ID pièce (1,1) incorrect";
            assert newBoard.getPlacement(1, 1).getRotation() == 2 : "Rotation (1,1) incorrecte";
            assert newBoard.getPlacement(2, 2).getPieceId() == 4 : "ID pièce (2,2) incorrect";
            assert newBoard.getPlacement(2, 2).getRotation() == 3 : "Rotation (2,2) incorrecte";

            testsPassed++;
            System.out.println("  ✓ Restauration complète OK");
        } catch (AssertionError e) {
            System.out.println("  ✗ Test restauration: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ✗ Test restauration: Erreur - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test: Sauvegarder plusieurs fois (écrasement)
     */
    private static void testSaveOverwrite() {
        testsRun++;
        try {
            System.out.println("Test: Écrasement de sauvegarde...");

            Board board = new Board(2, 2);
            Map<Integer, Piece> allPieces = new HashMap<>();
            for (int i = 1; i <= 4; i++) {
                allPieces.put(i, new Piece(i, new int[]{0, i, i, 0}));
            }

            // Première sauvegarde avec 1 pièce
            List<Integer> unusedIds1 = new ArrayList<>(Arrays.asList(2, 3, 4));
            board.place(0, 0, allPieces.get(1), 0);
            SaveStateManager.saveState(TEST_PUZZLE_NAME, board, allPieces, unusedIds1);

            SaveStateManager.SaveState loaded1 = SaveStateManager.loadState(TEST_PUZZLE_NAME);
            assert loaded1.depth == 1 : "Première sauvegarde: profondeur devrait être 1";

            // Deuxième sauvegarde avec 2 pièces (écrase la première)
            List<Integer> unusedIds2 = new ArrayList<>(Arrays.asList(3, 4));
            board.place(0, 1, allPieces.get(2), 1);
            SaveStateManager.saveState(TEST_PUZZLE_NAME, board, allPieces, unusedIds2);

            SaveStateManager.SaveState loaded2 = SaveStateManager.loadState(TEST_PUZZLE_NAME);
            assert loaded2.depth == 2 : "Deuxième sauvegarde: profondeur devrait être 2";
            assert loaded2.placements.size() == 2 : "Deuxième sauvegarde: devrait avoir 2 placements";

            testsPassed++;
            System.out.println("  ✓ Écrasement de sauvegarde OK");
        } catch (AssertionError e) {
            System.out.println("  ✗ Test écrasement: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ✗ Test écrasement: Erreur - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test CRITIQUE: Vérifier le format exact du fichier de sauvegarde
     * Ce test aurait détecté le bug row col vs row,col
     */
    private static void testSaveFileFormat() {
        testsRun++;
        try {
            System.out.println("Test: Vérification du format de sauvegarde...");

            Board board = new Board(3, 3);
            Map<Integer, Piece> allPieces = new HashMap<>();
            List<Integer> unusedIds = new ArrayList<>();

            for (int i = 1; i <= 5; i++) {
                allPieces.put(i, new Piece(i, new int[]{0, i, i, 0}));
                unusedIds.add(i);
            }

            board.place(1, 2, allPieces.get(1), 0);
            unusedIds.remove(Integer.valueOf(1));

            SaveStateManager.saveState(TEST_PUZZLE_NAME, board, allPieces, unusedIds);

            // Lire le fichier et vérifier le format
            String filename = "saves/" + TEST_PUZZLE_NAME.replaceAll("[^a-zA-Z0-9]", "_") + "_save.txt";
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            boolean foundPlacementSection = false;
            boolean formatCorrect = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("# Placements")) {
                    foundPlacementSection = true;
                } else if (foundPlacementSection && !line.startsWith("#") && !line.trim().isEmpty()) {
                    // Vérifier le format: doit être "row,col pieceId rotation"
                    // PAS "row col pieceId rotation"
                    assert line.matches("\\d+,\\d+\\s+\\d+\\s+\\d+") :
                        "Format incorrect: '" + line + "' (devrait être row,col avec virgule!)";
                    formatCorrect = true;
                    break;
                }
            }
            reader.close();

            assert foundPlacementSection : "Section placements introuvable";
            assert formatCorrect : "Format de placement non vérifié";

            testsPassed++;
            System.out.println("  ✓ Format de sauvegarde correct (row,col avec virgule)");
        } catch (AssertionError e) {
            System.out.println("  ✗ Test format sauvegarde: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ✗ Test format sauvegarde: Erreur - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test CRITIQUE: Vérifier que la profondeur est préservée après restauration
     * Ce test aurait détecté le problème de reprised depuis le début
     */
    private static void testRestorePreservesDepth() {
        testsRun++;
        try {
            System.out.println("Test: Préservation de la profondeur après restauration...");

            // Créer un board avec plusieurs pièces
            Board originalBoard = new Board(3, 3);
            Map<Integer, Piece> allPieces = new HashMap<>();
            List<Integer> unusedIds = new ArrayList<>();

            for (int i = 1; i <= 9; i++) {
                allPieces.put(i, new Piece(i, new int[]{0, i, i, 0}));
                unusedIds.add(i);
            }

            // Placer 5 pièces
            originalBoard.place(0, 0, allPieces.get(1), 0);
            originalBoard.place(0, 1, allPieces.get(2), 1);
            originalBoard.place(1, 0, allPieces.get(3), 0);
            originalBoard.place(1, 1, allPieces.get(4), 2);
            originalBoard.place(2, 2, allPieces.get(5), 3);
            for (int id = 1; id <= 5; id++) {
                unusedIds.remove(Integer.valueOf(id));
            }

            int originalDepth = 5;

            // Sauvegarder
            SaveStateManager.saveState(TEST_PUZZLE_NAME, originalBoard, allPieces, unusedIds);

            // Charger
            SaveStateManager.SaveState loaded = SaveStateManager.loadState(TEST_PUZZLE_NAME);
            assert loaded != null : "La sauvegarde devrait exister";
            assert loaded.depth == originalDepth :
                "Profondeur incorrecte après chargement: " + loaded.depth + " != " + originalDepth;

            // Restaurer sur un nouveau board
            Board restoredBoard = new Board(3, 3);
            boolean restored = SaveStateManager.restoreState(loaded, restoredBoard, allPieces);

            assert restored : "La restauration devrait réussir";

            // Compter les pièces placées sur le board restauré
            int restoredCount = 0;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (!restoredBoard.isEmpty(r, c)) {
                        restoredCount++;
                    }
                }
            }

            assert restoredCount == originalDepth :
                "Nombre de pièces après restauration incorrect: " + restoredCount + " != " + originalDepth;

            testsPassed++;
            System.out.println("  ✓ Profondeur préservée: " + originalDepth + " pièces");
        } catch (AssertionError e) {
            System.out.println("  ✗ Test préservation profondeur: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ✗ Test préservation profondeur: Erreur - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test CRITIQUE: Vérifier que toutes les pièces utilisées sont bien marquées comme utilisées
     * Ce test détecte si allPiecesMap est correctement initialisé
     */
    private static void testAllPiecesMapIntegrity() {
        testsRun++;
        try {
            System.out.println("Test: Intégrité de allPiecesMap après restauration...");

            Board board = new Board(3, 3);
            Map<Integer, Piece> allPieces = new HashMap<>();
            List<Integer> unusedIds = new ArrayList<>();

            // Créer 9 pièces
            for (int i = 1; i <= 9; i++) {
                allPieces.put(i, new Piece(i, new int[]{0, i, i, 0}));
                unusedIds.add(i);
            }

            // Placer 3 pièces
            board.place(0, 0, allPieces.get(1), 0);
            board.place(0, 1, allPieces.get(2), 1);
            board.place(1, 0, allPieces.get(3), 0);
            unusedIds.remove(Integer.valueOf(1));
            unusedIds.remove(Integer.valueOf(2));
            unusedIds.remove(Integer.valueOf(3));

            // Sauvegarder
            SaveStateManager.saveState(TEST_PUZZLE_NAME, board, allPieces, unusedIds);

            // Charger et vérifier
            SaveStateManager.SaveState loaded = SaveStateManager.loadState(TEST_PUZZLE_NAME);
            assert loaded != null : "La sauvegarde devrait exister";

            // Vérifier que toutes les pièces (utilisées + non utilisées) = total
            int totalInSave = loaded.placements.size() + loaded.unusedPieceIds.size();
            assert totalInSave == 9 :
                "Total des pièces incorrect: " + totalInSave + " != 9";

            // Vérifier qu'aucune pièce n'est à la fois placée et non utilisée
            for (Map.Entry<String, SaveStateManager.PlacementInfo> entry : loaded.placements.entrySet()) {
                int pieceId = entry.getValue().pieceId;
                assert !loaded.unusedPieceIds.contains(pieceId) :
                    "Pièce " + pieceId + " est à la fois placée et dans unusedPieceIds!";
            }

            testsPassed++;
            System.out.println("  ✓ AllPiecesMap intègre: " + loaded.placements.size() + " placées, " +
                             loaded.unusedPieceIds.size() + " restantes");
        } catch (AssertionError e) {
            System.out.println("  ✗ Test intégrité allPiecesMap: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ✗ Test intégrité allPiecesMap: Erreur - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test: Vérifier que le puzzle name est bien sauvegardé
     */
    private static void testPuzzleNameInSave() {
        testsRun++;
        try {
            System.out.println("Test: Nom du puzzle dans la sauvegarde...");

            Board board = new Board(2, 2);
            Map<Integer, Piece> allPieces = new HashMap<>();
            List<Integer> unusedIds = new ArrayList<>();

            for (int i = 1; i <= 4; i++) {
                allPieces.put(i, new Piece(i, new int[]{0, i, i, 0}));
                unusedIds.add(i);
            }

            board.place(0, 0, allPieces.get(1), 0);
            unusedIds.remove(Integer.valueOf(1));

            SaveStateManager.saveState("test_puzzle_xyz", board, allPieces, unusedIds);

            // Lire le fichier et vérifier le nom du puzzle
            String filename = "saves/test_puzzle_xyz_save.txt";
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            boolean foundPuzzleName = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("# Puzzle:")) {
                    String puzzleName = line.substring(9).trim();
                    assert puzzleName.equals("test_puzzle_xyz") :
                        "Nom du puzzle incorrect: '" + puzzleName + "'";
                    foundPuzzleName = true;
                    break;
                }
            }
            reader.close();

            assert foundPuzzleName : "Nom du puzzle non trouvé dans la sauvegarde";

            // Nettoyer
            new File(filename).delete();

            testsPassed++;
            System.out.println("  ✓ Nom du puzzle correctement sauvegardé");
        } catch (AssertionError e) {
            System.out.println("  ✗ Test nom du puzzle: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ✗ Test nom du puzzle: Erreur - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void cleanupTestSaves() {
        String filename = "saves/" + TEST_PUZZLE_NAME.replaceAll("[^a-zA-Z0-9]", "_") + "_save.txt";
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
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
            System.out.println("\n✓ TOUS LES TESTS SAVE/LOAD ONT RÉUSSI!");
        } else {
            System.out.println("\n✗ CERTAINS TESTS ONT ÉCHOUÉ!");
            throw new RuntimeException("Tests save/load échoués");
        }
    }
}
