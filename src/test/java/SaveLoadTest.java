import model.Board;
import model.Piece;
import model.Placement;
import util.PuzzleFactory;
import util.SaveManager;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tests pour valider la logique de sauvegarde et chargement.
 */
public class SaveLoadTest {

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║        TESTS DE SAUVEGARDE/CHARGEMENT                  ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        int passed = 0;
        int failed = 0;

        // Test 1: Sauvegarde et chargement d'un état simple
        if (testSimpleSaveLoad()) {
            System.out.println("✓ Test 1: Sauvegarde/chargement simple - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 1: Sauvegarde/chargement simple - ÉCHOUÉ\n");
            failed++;
        }

        // Test 2: Sauvegarde avec hint piece
        if (testSaveLoadWithHintPiece()) {
            System.out.println("✓ Test 2: Sauvegarde avec hint piece - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 2: Sauvegarde avec hint piece - ÉCHOUÉ\n");
            failed++;
        }

        // Test 3: Sauvegarde avec plusieurs pièces
        if (testSaveLoadMultiplePieces()) {
            System.out.println("✓ Test 3: Sauvegarde avec plusieurs pièces - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 3: Sauvegarde avec plusieurs pièces - ÉCHOUÉ\n");
            failed++;
        }

        // Test 4: Vérification des pièces utilisées
        if (testUsedPiecesIdentification()) {
            System.out.println("✓ Test 4: Identification des pièces utilisées - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 4: Identification des pièces utilisées - ÉCHOUÉ\n");
            failed++;
        }

        // Résumé
        System.out.println("════════════════════════════════════════════════════════");
        System.out.println("RÉSULTATS: " + passed + " passés, " + failed + " échoués");
        System.out.println("════════════════════════════════════════════════════════");

        System.exit(failed > 0 ? 1 : 0);
    }

    private static boolean testSimpleSaveLoad() {
        try {
            // Créer un board simple avec une pièce
            Board board = new Board(4, 4);
            Map<Integer, Piece> allPieces = PuzzleFactory.createExample4x4Easy();

            // Placer une pièce
            Piece piece1 = allPieces.get(1);
            board.place(0, 0, piece1, 0);

            // Sauvegarder
            SaveManager.saveThreadState(board, allPieces, 1, 999, 123456L);

            // Charger
            Object[] loaded = SaveManager.loadThreadState(999, allPieces);
            if (loaded == null) {
                System.out.println("  Erreur: Impossible de charger la sauvegarde");
                return false;
            }

            Board loadedBoard = (Board) loaded[0];
            @SuppressWarnings("unchecked")
            Set<Integer> usedPieceIds = (Set<Integer>) loaded[1];
            int depth = (int) loaded[2];
            long seed = (long) loaded[3];

            // Vérifications
            if (depth != 1) {
                System.out.println("  Erreur: Depth incorrect (attendu 1, reçu " + depth + ")");
                return false;
            }

            if (seed != 123456L) {
                System.out.println("  Erreur: Seed incorrect (attendu 123456, reçu " + seed + ")");
                return false;
            }

            if (!usedPieceIds.contains(1)) {
                System.out.println("  Erreur: Pièce 1 devrait être dans usedPieceIds");
                return false;
            }

            if (loadedBoard.isEmpty(0, 0)) {
                System.out.println("  Erreur: Board (0,0) devrait contenir une pièce");
                return false;
            }

            Placement p = loadedBoard.getPlacement(0, 0);
            if (p.getPieceId() != 1 || p.getRotation() != 0) {
                System.out.println("  Erreur: Placement incorrect à (0,0)");
                return false;
            }

            // Nettoyer
            new File("saves/thread_999.txt").delete();

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean testSaveLoadWithHintPiece() {
        try {
            // Créer un board avec la hint piece (139)
            Board board = new Board(16, 16);
            Map<Integer, Piece> allPieces = PuzzleFactory.createEternityII();

            // Placer la hint piece
            Piece hint = allPieces.get(139);
            board.place(8, 7, hint, 3);

            // Placer une autre pièce
            Piece corner = allPieces.get(1);
            board.place(0, 0, corner, 1);

            // Sauvegarder
            SaveManager.saveThreadState(board, allPieces, 2, 998, 789012L);

            // Charger
            Object[] loaded = SaveManager.loadThreadState(998, allPieces);
            if (loaded == null) {
                System.out.println("  Erreur: Impossible de charger la sauvegarde");
                return false;
            }

            Board loadedBoard = (Board) loaded[0];
            @SuppressWarnings("unchecked")
            Set<Integer> usedPieceIds = (Set<Integer>) loaded[1];
            int depth = (int) loaded[2];

            // Vérifications
            if (depth != 2) {
                System.out.println("  Erreur: Depth incorrect (attendu 2, reçu " + depth + ")");
                return false;
            }

            if (!usedPieceIds.contains(139)) {
                System.out.println("  Erreur: Pièce 139 (hint) devrait être dans usedPieceIds");
                return false;
            }

            if (!usedPieceIds.contains(1)) {
                System.out.println("  Erreur: Pièce 1 devrait être dans usedPieceIds");
                return false;
            }

            if (loadedBoard.isEmpty(8, 7)) {
                System.out.println("  Erreur: Hint piece manquante à (8,7)");
                return false;
            }

            Placement hintPlacement = loadedBoard.getPlacement(8, 7);
            if (hintPlacement.getPieceId() != 139 || hintPlacement.getRotation() != 3) {
                System.out.println("  Erreur: Hint piece incorrecte à (8,7)");
                return false;
            }

            if (loadedBoard.isEmpty(0, 0)) {
                System.out.println("  Erreur: Corner piece manquante à (0,0)");
                return false;
            }

            // Nettoyer
            new File("saves/thread_998.txt").delete();

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean testSaveLoadMultiplePieces() {
        try {
            // Créer un board avec plusieurs pièces
            Board board = new Board(4, 4);
            Map<Integer, Piece> allPieces = PuzzleFactory.createExample4x4Easy();

            // Placer 4 pièces
            board.place(0, 0, allPieces.get(1), 0);
            board.place(0, 1, allPieces.get(2), 1);
            board.place(1, 0, allPieces.get(3), 2);
            board.place(1, 1, allPieces.get(4), 3);

            // Sauvegarder
            SaveManager.saveThreadState(board, allPieces, 4, 997, 111222L);

            // Charger
            Object[] loaded = SaveManager.loadThreadState(997, allPieces);
            if (loaded == null) {
                System.out.println("  Erreur: Impossible de charger la sauvegarde");
                return false;
            }

            Board loadedBoard = (Board) loaded[0];
            @SuppressWarnings("unchecked")
            Set<Integer> usedPieceIds = (Set<Integer>) loaded[1];
            int depth = (int) loaded[2];

            // Vérifications
            if (depth != 4) {
                System.out.println("  Erreur: Depth incorrect (attendu 4, reçu " + depth + ")");
                return false;
            }

            if (usedPieceIds.size() != 4) {
                System.out.println("  Erreur: Devrait avoir 4 pièces utilisées, reçu " + usedPieceIds.size());
                return false;
            }

            // Vérifier chaque placement
            int[][] positions = {{0,0}, {0,1}, {1,0}, {1,1}};
            int[] pieceIds = {1, 2, 3, 4};
            int[] rotations = {0, 1, 2, 3};

            for (int i = 0; i < positions.length; i++) {
                int r = positions[i][0];
                int c = positions[i][1];

                if (loadedBoard.isEmpty(r, c)) {
                    System.out.println("  Erreur: Position (" + r + "," + c + ") devrait contenir une pièce");
                    return false;
                }

                Placement p = loadedBoard.getPlacement(r, c);
                if (p.getPieceId() != pieceIds[i]) {
                    System.out.println("  Erreur: Piece ID incorrect à (" + r + "," + c + ")");
                    return false;
                }

                if (p.getRotation() != rotations[i]) {
                    System.out.println("  Erreur: Rotation incorrecte à (" + r + "," + c + ")");
                    return false;
                }
            }

            // Nettoyer
            new File("saves/thread_997.txt").delete();

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean testUsedPiecesIdentification() {
        try {
            // Créer un board et vérifier que unusedIds est correct après chargement
            Board board = new Board(4, 4);
            Map<Integer, Piece> allPieces = PuzzleFactory.createExample4x4Easy();

            // Placer quelques pièces
            board.place(0, 0, allPieces.get(1), 0);
            board.place(0, 1, allPieces.get(5), 1);
            board.place(2, 2, allPieces.get(10), 2);

            // Sauvegarder
            SaveManager.saveThreadState(board, allPieces, 3, 996, 333444L);

            // Charger
            Object[] loaded = SaveManager.loadThreadState(996, allPieces);
            if (loaded == null) {
                System.out.println("  Erreur: Impossible de charger la sauvegarde");
                return false;
            }

            @SuppressWarnings("unchecked")
            Set<Integer> usedPieceIds = (Set<Integer>) loaded[1];

            // Vérifier que les bonnes pièces sont marquées comme utilisées
            if (!usedPieceIds.contains(1) || !usedPieceIds.contains(5) || !usedPieceIds.contains(10)) {
                System.out.println("  Erreur: Pièces utilisées incorrectes");
                System.out.println("  UsedPieceIds: " + usedPieceIds);
                return false;
            }

            if (usedPieceIds.size() != 3) {
                System.out.println("  Erreur: Devrait avoir exactement 3 pièces utilisées, reçu " + usedPieceIds.size());
                return false;
            }

            // Simuler la reconstruction de unusedIds (comme dans le solver)
            Set<Integer> unusedIds = new HashSet<>(allPieces.keySet());
            unusedIds.removeAll(usedPieceIds);

            // Vérifier que les pièces utilisées ne sont pas dans unusedIds
            if (unusedIds.contains(1) || unusedIds.contains(5) || unusedIds.contains(10)) {
                System.out.println("  Erreur: Pièces utilisées devraient être absentes de unusedIds");
                return false;
            }

            // Vérifier que le nombre total est correct
            if (usedPieceIds.size() + unusedIds.size() != allPieces.size()) {
                System.out.println("  Erreur: Nombre total de pièces incorrect");
                return false;
            }

            // Nettoyer
            new File("saves/thread_996.txt").delete();

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
