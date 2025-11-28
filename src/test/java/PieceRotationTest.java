import model.Piece;

/**
 * Tests pour valider la logique de rotation des pièces.
 */
public class PieceRotationTest {

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║        TESTS DE ROTATION DES PIÈCES                   ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        int passed = 0;
        int failed = 0;

        // Test 1: Rotation 0° (identité)
        if (testRotation0()) {
            System.out.println("✓ Test 1: Rotation 0° - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 1: Rotation 0° - ÉCHOUÉ\n");
            failed++;
        }

        // Test 2: Rotation 90° clockwise
        if (testRotation90()) {
            System.out.println("✓ Test 2: Rotation 90° - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 2: Rotation 90° - ÉCHOUÉ\n");
            failed++;
        }

        // Test 3: Rotation 180°
        if (testRotation180()) {
            System.out.println("✓ Test 3: Rotation 180° - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 3: Rotation 180° - ÉCHOUÉ\n");
            failed++;
        }

        // Test 4: Rotation 270°
        if (testRotation270()) {
            System.out.println("✓ Test 4: Rotation 270° - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 4: Rotation 270° - ÉCHOUÉ\n");
            failed++;
        }

        // Test 5: Rotation modulo 4
        if (testRotationModulo()) {
            System.out.println("✓ Test 5: Rotation modulo 4 - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 5: Rotation modulo 4 - ÉCHOUÉ\n");
            failed++;
        }

        // Test 6: Détection de symétrie 4-fold
        if (testSymmetry4Fold()) {
            System.out.println("✓ Test 6: Symétrie 4-fold - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 6: Symétrie 4-fold - ÉCHOUÉ\n");
            failed++;
        }

        // Test 7: Détection de symétrie 2-fold
        if (testSymmetry2Fold()) {
            System.out.println("✓ Test 7: Symétrie 2-fold - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 7: Symétrie 2-fold - ÉCHOUÉ\n");
            failed++;
        }

        // Test 8: Aucune symétrie (4 rotations uniques)
        if (testNoSymmetry()) {
            System.out.println("✓ Test 8: Aucune symétrie - PASSÉ\n");
            passed++;
        } else {
            System.out.println("✗ Test 8: Aucune symétrie - ÉCHOUÉ\n");
            failed++;
        }

        // Résumé
        System.out.println("════════════════════════════════════════════════════════");
        System.out.println("RÉSULTATS: " + passed + " passés, " + failed + " échoués");
        System.out.println("════════════════════════════════════════════════════════");

        System.exit(failed > 0 ? 1 : 0);
    }

    private static boolean testRotation0() {
        try {
            // Pièce avec edges [N=1, E=2, S=3, W=4]
            Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
            int[] rotated = piece.edgesRotated(0);

            if (!arrayEquals(rotated, new int[]{1, 2, 3, 4})) {
                System.out.println("  Erreur: Rotation 0° devrait être identique");
                System.out.println("  Attendu: [1, 2, 3, 4]");
                System.out.println("  Reçu: " + arrayToString(rotated));
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testRotation90() {
        try {
            // Pièce avec edges [N=1, E=2, S=3, W=4]
            // Après rotation 90° CW: [N=W, E=N, S=E, W=S] = [4, 1, 2, 3]
            Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
            int[] rotated = piece.edgesRotated(1);

            if (!arrayEquals(rotated, new int[]{4, 1, 2, 3})) {
                System.out.println("  Erreur: Rotation 90° incorrecte");
                System.out.println("  Attendu: [4, 1, 2, 3]");
                System.out.println("  Reçu: " + arrayToString(rotated));
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testRotation180() {
        try {
            // Pièce avec edges [N=1, E=2, S=3, W=4]
            // Après rotation 180°: [N=S, E=W, S=N, W=E] = [3, 4, 1, 2]
            Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
            int[] rotated = piece.edgesRotated(2);

            if (!arrayEquals(rotated, new int[]{3, 4, 1, 2})) {
                System.out.println("  Erreur: Rotation 180° incorrecte");
                System.out.println("  Attendu: [3, 4, 1, 2]");
                System.out.println("  Reçu: " + arrayToString(rotated));
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testRotation270() {
        try {
            // Pièce avec edges [N=1, E=2, S=3, W=4]
            // Après rotation 270°: [N=E, E=S, S=W, W=N] = [2, 3, 4, 1]
            Piece piece = new Piece(1, new int[]{1, 2, 3, 4});
            int[] rotated = piece.edgesRotated(3);

            if (!arrayEquals(rotated, new int[]{2, 3, 4, 1})) {
                System.out.println("  Erreur: Rotation 270° incorrecte");
                System.out.println("  Attendu: [2, 3, 4, 1]");
                System.out.println("  Reçu: " + arrayToString(rotated));
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testRotationModulo() {
        try {
            // Vérifier que rotation 4 = rotation 0, rotation 5 = rotation 1, etc.
            Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

            int[] rot4 = piece.edgesRotated(4);
            int[] rot0 = piece.edgesRotated(0);
            if (!arrayEquals(rot4, rot0)) {
                System.out.println("  Erreur: Rotation 4 devrait égaler rotation 0");
                return false;
            }

            int[] rot5 = piece.edgesRotated(5);
            int[] rot1 = piece.edgesRotated(1);
            if (!arrayEquals(rot5, rot1)) {
                System.out.println("  Erreur: Rotation 5 devrait égaler rotation 1");
                return false;
            }

            // Tester rotation négative
            int[] rotNeg1 = piece.edgesRotated(-1);
            int[] rot3 = piece.edgesRotated(3);
            if (!arrayEquals(rotNeg1, rot3)) {
                System.out.println("  Erreur: Rotation -1 devrait égaler rotation 3");
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testSymmetry4Fold() {
        try {
            // Pièce avec toutes les arêtes identiques [5, 5, 5, 5]
            // Devrait avoir 1 rotation unique
            Piece piece = new Piece(1, new int[]{5, 5, 5, 5});

            int uniqueRotations = piece.getUniqueRotationCount();
            if (uniqueRotations != 1) {
                System.out.println("  Erreur: Symétrie 4-fold devrait avoir 1 rotation unique");
                System.out.println("  Reçu: " + uniqueRotations);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testSymmetry2Fold() {
        try {
            // Pièce avec opposés identiques [N=1, E=2, S=1, W=2]
            // Devrait avoir 2 rotations uniques
            Piece piece = new Piece(1, new int[]{1, 2, 1, 2});

            int uniqueRotations = piece.getUniqueRotationCount();
            if (uniqueRotations != 2) {
                System.out.println("  Erreur: Symétrie 2-fold devrait avoir 2 rotations uniques");
                System.out.println("  Reçu: " + uniqueRotations);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean testNoSymmetry() {
        try {
            // Pièce sans symétrie [1, 2, 3, 4]
            // Devrait avoir 4 rotations uniques
            Piece piece = new Piece(1, new int[]{1, 2, 3, 4});

            int uniqueRotations = piece.getUniqueRotationCount();
            if (uniqueRotations != 4) {
                System.out.println("  Erreur: Pièce sans symétrie devrait avoir 4 rotations uniques");
                System.out.println("  Reçu: " + uniqueRotations);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("  Exception: " + e.getMessage());
            return false;
        }
    }

    private static boolean arrayEquals(int[] a, int[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
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
