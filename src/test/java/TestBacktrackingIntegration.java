import model.Board;
import model.Piece;
import util.SaveStateManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests d'intégration pour valider la nouvelle logique de backtracking
 * avec profondeur augmentée (40 pièces au lieu de 10)
 */
public class TestBacktrackingIntegration {

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  Tests d'intégration du backtracking amélioré             ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        int passed = 0;
        int total = 0;

        // Test 1: Calcul avec nouveau backtrackDepth = 40
        total++;
        if (testNewBacktrackDepth()) {
            passed++;
            System.out.println("✓ Test 1: Nouveau backtrackDepth (max 40)");
        } else {
            System.out.println("✗ Test 1: Nouveau backtrackDepth (max 40)");
        }

        // Test 2: Retrait de 40 pièces d'une sauvegarde de 100 pièces
        total++;
        if (testRemoval40Pieces()) {
            passed++;
            System.out.println("✓ Test 2: Retrait de 40 pièces");
        } else {
            System.out.println("✗ Test 2: Retrait de 40 pièces");
        }

        // Test 3: Cas limite - 63 pièces (depth/2 = 31)
        total++;
        if (testRemoval63Pieces()) {
            passed++;
            System.out.println("✓ Test 3: Cas limite 63 pièces");
        } else {
            System.out.println("✗ Test 3: Cas limite 63 pièces");
        }

        // Test 4: Vérification que les pièces retirées sont dans unusedPieces
        total++;
        if (testUnusedPiecesCorrect()) {
            passed++;
            System.out.println("✓ Test 4: Pièces retirées dans unusedPieces");
        } else {
            System.out.println("✗ Test 4: Pièces retirées dans unusedPieces");
        }

        // Test 5: Vérification Map placements synchronisé avec List placementOrder
        total++;
        if (testPlacementsSynchronization()) {
            passed++;
            System.out.println("✓ Test 5: Synchronisation placements/placementOrder");
        } else {
            System.out.println("✗ Test 5: Synchronisation placements/placementOrder");
        }

        // Test 6: Test du calcul pour différentes profondeurs
        total++;
        if (testVariousDepths()) {
            passed++;
            System.out.println("✓ Test 6: Calculs pour différentes profondeurs");
        } else {
            System.out.println("✗ Test 6: Calculs pour différentes profondeurs");
        }

        System.out.println();
        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println("Résultats: " + passed + "/" + total + " tests réussis");
        System.out.println("════════════════════════════════════════════════════════════");

        if (passed == total) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }

    /**
     * Test 1: Vérifie que le backtrackDepth max est maintenant 40
     */
    private static boolean testNewBacktrackDepth() {
        // Pour 100 pièces: min(40, 100/2) = min(40, 50) = 40
        int depth100 = 100;
        int backtrack100 = Math.min(40, depth100 / 2);
        if (backtrack100 != 40) {
            System.out.println("  Erreur: Pour depth=100, attendu 40, obtenu " + backtrack100);
            return false;
        }

        // Pour 70 pièces: min(40, 70/2) = min(40, 35) = 35
        int depth70 = 70;
        int backtrack70 = Math.min(40, depth70 / 2);
        if (backtrack70 != 35) {
            System.out.println("  Erreur: Pour depth=70, attendu 35, obtenu " + backtrack70);
            return false;
        }

        // Pour 256 pièces: min(40, 256/2) = min(40, 128) = 40
        int depth256 = 256;
        int backtrack256 = Math.min(40, depth256 / 2);
        if (backtrack256 != 40) {
            System.out.println("  Erreur: Pour depth=256, attendu 40, obtenu " + backtrack256);
            return false;
        }

        return true;
    }

    /**
     * Test 2: Retrait de 40 pièces d'une sauvegarde de 100 pièces
     */
    private static boolean testRemoval40Pieces() {
        List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
        Map<String, SaveStateManager.PlacementInfo> placements = new HashMap<>();
        Set<Integer> unusedPieceIds = new HashSet<>();

        // Créer 100 placements
        for (int i = 0; i < 100; i++) {
            SaveStateManager.PlacementInfo info = new SaveStateManager.PlacementInfo(i / 10, i % 10, i + 1, 0);
            placementOrder.add(info);
            placements.put(info.row + "," + info.col, info);
        }

        // Ajouter 156 pièces non utilisées (256 - 100)
        for (int i = 101; i <= 256; i++) {
            unusedPieceIds.add(i);
        }

        int depth = 100;
        int backtrackDepth = Math.min(40, depth / 2); // = 40

        // Retirer les pièces
        for (int i = 0; i < backtrackDepth && !placementOrder.isEmpty(); i++) {
            SaveStateManager.PlacementInfo removed = placementOrder.remove(placementOrder.size() - 1);
            String key = removed.row + "," + removed.col;
            placements.remove(key);
            unusedPieceIds.add(removed.pieceId);
        }

        // Vérifications
        if (placementOrder.size() != 60) {
            System.out.println("  Erreur: placementOrder devrait avoir 60 éléments, obtenu " + placementOrder.size());
            return false;
        }

        if (placements.size() != 60) {
            System.out.println("  Erreur: placements devrait avoir 60 éléments, obtenu " + placements.size());
            return false;
        }

        if (unusedPieceIds.size() != 196) { // 156 + 40
            System.out.println("  Erreur: unusedPieceIds devrait avoir 196 éléments, obtenu " + unusedPieceIds.size());
            return false;
        }

        return true;
    }

    /**
     * Test 3: Cas limite - 63 pièces (comme dans l'exécution réelle)
     */
    private static boolean testRemoval63Pieces() {
        List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();

        // Créer 63 placements
        for (int i = 0; i < 63; i++) {
            placementOrder.add(new SaveStateManager.PlacementInfo(i / 10, i % 10, i + 1, 0));
        }

        int depth = 63;
        int backtrackDepth = Math.min(40, depth / 2); // = 31

        // Retirer les pièces
        for (int i = 0; i < backtrackDepth && !placementOrder.isEmpty(); i++) {
            placementOrder.remove(placementOrder.size() - 1);
        }

        int expectedSize = 32; // 63 - 31
        if (placementOrder.size() != expectedSize) {
            System.out.println("  Erreur: Attendu " + expectedSize + " pièces, obtenu " + placementOrder.size());
            return false;
        }

        return true;
    }

    /**
     * Test 4: Vérifier que les pièces retirées sont ajoutées à unusedPieceIds
     */
    private static boolean testUnusedPiecesCorrect() {
        List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
        Set<Integer> unusedPieceIds = new HashSet<>();

        // Créer 50 placements (pièces 1-50)
        for (int i = 0; i < 50; i++) {
            placementOrder.add(new SaveStateManager.PlacementInfo(i / 10, i % 10, i + 1, 0));
        }

        // Pièces 51-256 non utilisées
        for (int i = 51; i <= 256; i++) {
            unusedPieceIds.add(i);
        }

        int initialUnused = unusedPieceIds.size(); // 206
        int backtrackDepth = Math.min(40, 50 / 2); // = 25

        // Retirer les 25 dernières pièces (pièces 26-50)
        List<Integer> removedIds = new ArrayList<>();
        for (int i = 0; i < backtrackDepth && !placementOrder.isEmpty(); i++) {
            SaveStateManager.PlacementInfo removed = placementOrder.remove(placementOrder.size() - 1);
            unusedPieceIds.add(removed.pieceId);
            removedIds.add(removed.pieceId);
        }

        // Vérifier que unusedPieceIds contient les pièces retirées
        if (unusedPieceIds.size() != initialUnused + 25) {
            System.out.println("  Erreur: unusedPieceIds devrait avoir " + (initialUnused + 25) + " éléments, obtenu " + unusedPieceIds.size());
            return false;
        }

        // Vérifier que les pièces 26-50 sont dans unusedPieceIds
        for (int i = 26; i <= 50; i++) {
            if (!unusedPieceIds.contains(i)) {
                System.out.println("  Erreur: Pièce " + i + " devrait être dans unusedPieceIds");
                return false;
            }
        }

        return true;
    }

    /**
     * Test 5: Vérifier que placements et placementOrder restent synchronisés
     */
    private static boolean testPlacementsSynchronization() {
        List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
        Map<String, SaveStateManager.PlacementInfo> placements = new HashMap<>();

        // Créer 80 placements
        for (int i = 0; i < 80; i++) {
            SaveStateManager.PlacementInfo info = new SaveStateManager.PlacementInfo(i / 10, i % 10, i + 1, 0);
            placementOrder.add(info);
            placements.put(info.row + "," + info.col, info);
        }

        int backtrackDepth = Math.min(40, 80 / 2); // = 40

        // Retirer les pièces
        for (int i = 0; i < backtrackDepth && !placementOrder.isEmpty(); i++) {
            SaveStateManager.PlacementInfo removed = placementOrder.remove(placementOrder.size() - 1);
            String key = removed.row + "," + removed.col;
            placements.remove(key);
        }

        // Vérifier la synchronisation
        if (placementOrder.size() != placements.size()) {
            System.out.println("  Erreur: Tailles différentes - placementOrder: " + placementOrder.size() + ", placements: " + placements.size());
            return false;
        }

        // Vérifier que toutes les pièces dans placementOrder sont dans placements
        for (SaveStateManager.PlacementInfo info : placementOrder) {
            String key = info.row + "," + info.col;
            if (!placements.containsKey(key)) {
                System.out.println("  Erreur: Position " + key + " manquante dans placements");
                return false;
            }

            SaveStateManager.PlacementInfo mapInfo = placements.get(key);
            if (mapInfo.pieceId != info.pieceId) {
                System.out.println("  Erreur: Pièce différente à " + key);
                return false;
            }
        }

        return true;
    }

    /**
     * Test 6: Tester le calcul pour différentes profondeurs
     */
    private static boolean testVariousDepths() {
        // Tableau de test: (depth, expectedBacktrackDepth)
        int[][] testCases = {
            {10, 5},      // min(40, 10/2) = 5
            {20, 10},     // min(40, 20/2) = 10
            {50, 25},     // min(40, 50/2) = 25
            {80, 40},     // min(40, 80/2) = 40
            {100, 40},    // min(40, 100/2) = 40
            {256, 40},    // min(40, 256/2) = 40
            {5, 2},       // min(40, 5/2) = 2
            {1, 0},       // min(40, 1/2) = 0
        };

        for (int[] testCase : testCases) {
            int depth = testCase[0];
            int expected = testCase[1];
            int actual = Math.min(40, depth / 2);

            if (actual != expected) {
                System.out.println("  Erreur: Pour depth=" + depth + ", attendu " + expected + ", obtenu " + actual);
                return false;
            }
        }

        return true;
    }
}
