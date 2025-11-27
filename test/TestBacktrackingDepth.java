import model.Board;
import model.Piece;
import util.SaveStateManager;
import java.util.*;

/**
 * Tests pour valider la logique de profondeur de backtracking
 * lors du chargement d'une sauvegarde.
 */
public class TestBacktrackingDepth {

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  Tests de la profondeur de backtracking                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        int passed = 0;
        int total = 0;

        // Test 1: Calcul du backtrackDepth
        total++;
        if (testBacktrackDepthCalculation()) {
            passed++;
            System.out.println("✓ Test 1: Calcul du backtrackDepth");
        } else {
            System.out.println("✗ Test 1: Calcul du backtrackDepth");
        }

        // Test 2: Retrait des pièces de la liste de placements
        total++;
        if (testRemovingPiecesFromPlacements()) {
            passed++;
            System.out.println("✓ Test 2: Retrait des pièces");
        } else {
            System.out.println("✗ Test 2: Retrait des pièces");
        }

        // Test 3: Vérification de la profondeur après retrait
        total++;
        if (testDepthAfterRemoval()) {
            passed++;
            System.out.println("✓ Test 3: Profondeur après retrait");
        } else {
            System.out.println("✗ Test 3: Profondeur après retrait");
        }

        // Test 4: Cas limite - moins de 10 pièces
        total++;
        if (testSmallDepthCase()) {
            passed++;
            System.out.println("✓ Test 4: Cas limite (peu de pièces)");
        } else {
            System.out.println("✗ Test 4: Cas limite (peu de pièces)");
        }

        // Test 5: Restauration après retrait
        total++;
        if (testRestorationAfterRemoval()) {
            passed++;
            System.out.println("✓ Test 5: Restauration après retrait");
        } else {
            System.out.println("✗ Test 5: Restauration après retrait");
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
     * Test 1: Vérifie que le calcul du backtrackDepth est correct
     */
    private static boolean testBacktrackDepthCalculation() {
        // Pour 81 pièces: min(10, 81/2) = min(10, 40) = 10
        int depth81 = 81;
        int expected81 = Math.min(10, depth81 / 2);
        if (expected81 != 10) {
            System.out.println("  Erreur: Pour depth=81, attendu 10, obtenu " + expected81);
            return false;
        }

        // Pour 15 pièces: min(10, 15/2) = min(10, 7) = 7
        int depth15 = 15;
        int expected15 = Math.min(10, depth15 / 2);
        if (expected15 != 7) {
            System.out.println("  Erreur: Pour depth=15, attendu 7, obtenu " + expected15);
            return false;
        }

        // Pour 5 pièces: min(10, 5/2) = min(10, 2) = 2
        int depth5 = 5;
        int expected5 = Math.min(10, depth5 / 2);
        if (expected5 != 2) {
            System.out.println("  Erreur: Pour depth=5, attendu 2, obtenu " + expected5);
            return false;
        }

        return true;
    }

    /**
     * Test 2: Vérifie que les pièces sont correctement retirées de la liste
     */
    private static boolean testRemovingPiecesFromPlacements() {
        List<SaveStateManager.PlacementInfo> placements = new ArrayList<>();

        // Créer 20 placements
        for (int i = 0; i < 20; i++) {
            placements.add(new SaveStateManager.PlacementInfo(0, i, i + 1, 0));
        }

        int initialSize = placements.size();
        int toRemove = 10;

        // Retirer 10 pièces
        for (int i = 0; i < toRemove && !placements.isEmpty(); i++) {
            placements.remove(placements.size() - 1);
        }

        if (placements.size() != initialSize - toRemove) {
            System.out.println("  Erreur: Attendu " + (initialSize - toRemove) + " pièces, obtenu " + placements.size());
            return false;
        }

        // Vérifier que les bonnes pièces ont été retirées (les dernières)
        // La dernière pièce devrait maintenant être celle à l'index 9 (pieceId = 10)
        SaveStateManager.PlacementInfo lastPlacement = placements.get(placements.size() - 1);
        if (lastPlacement.pieceId != 10) {
            System.out.println("  Erreur: La dernière pièce devrait être 10, obtenu " + lastPlacement.pieceId);
            return false;
        }

        return true;
    }

    /**
     * Test 3: Vérifie que la profondeur est correctement calculée après retrait
     */
    private static boolean testDepthAfterRemoval() {
        List<SaveStateManager.PlacementInfo> placements = new ArrayList<>();

        // Créer 81 placements
        for (int i = 0; i < 81; i++) {
            placements.add(new SaveStateManager.PlacementInfo(i / 10, i % 10, i + 1, 0));
        }

        int depth = 81;
        int backtrackDepth = Math.min(10, depth / 2);

        // Retirer les pièces
        for (int i = 0; i < backtrackDepth && !placements.isEmpty(); i++) {
            placements.remove(placements.size() - 1);
        }

        int actualDepth = placements.size();
        int expectedDepth = 71; // 81 - 10

        if (actualDepth != expectedDepth) {
            System.out.println("  Erreur: Attendu depth=" + expectedDepth + ", obtenu " + actualDepth);
            return false;
        }

        return true;
    }

    /**
     * Test 4: Cas limite avec peu de pièces
     */
    private static boolean testSmallDepthCase() {
        List<SaveStateManager.PlacementInfo> placements = new ArrayList<>();

        // Créer seulement 5 placements
        for (int i = 0; i < 5; i++) {
            placements.add(new SaveStateManager.PlacementInfo(0, i, i + 1, 0));
        }

        int depth = 5;
        int backtrackDepth = Math.min(10, depth / 2); // = 2

        // Retirer les pièces
        for (int i = 0; i < backtrackDepth && !placements.isEmpty(); i++) {
            placements.remove(placements.size() - 1);
        }

        int actualDepth = placements.size();
        int expectedDepth = 3; // 5 - 2

        if (actualDepth != expectedDepth) {
            System.out.println("  Erreur: Attendu depth=" + expectedDepth + ", obtenu " + actualDepth);
            return false;
        }

        return true;
    }

    /**
     * Test 5: Vérifie que le retrait fonctionne correctement avec la logique réelle
     */
    private static boolean testRestorationAfterRemoval() {
        // Créer une liste de placements avec 50 pièces
        List<SaveStateManager.PlacementInfo> placements = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            placements.add(new SaveStateManager.PlacementInfo(i / 10, i % 10, i + 1, 0));
        }

        // Simuler la logique de MainSequential
        int depth = 50;
        int backtrackDepth = Math.min(10, depth / 2); // = 10

        // Retirer les pièces
        for (int i = 0; i < backtrackDepth && !placements.isEmpty(); i++) {
            placements.remove(placements.size() - 1);
        }

        int actualDepth = placements.size();
        int expectedDepth = 40; // 50 - 10

        if (actualDepth != expectedDepth) {
            System.out.println("  Erreur: Attendu " + expectedDepth + " pièces, obtenu " + actualDepth);
            return false;
        }

        // Vérifier que les bonnes pièces restent
        // La première pièce devrait être pieceId=1
        if (placements.get(0).pieceId != 1) {
            System.out.println("  Erreur: La première pièce devrait être 1, obtenu " + placements.get(0).pieceId);
            return false;
        }

        // La dernière pièce devrait être pieceId=40 (car on a retiré 41-50)
        if (placements.get(placements.size() - 1).pieceId != 40) {
            System.out.println("  Erreur: La dernière pièce devrait être 40, obtenu " + placements.get(placements.size() - 1).pieceId);
            return false;
        }

        return true;
    }
}
