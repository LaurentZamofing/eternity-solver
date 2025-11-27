import model.PieceTest;
import model.BoardTest;
import solver.EternitySolverTest;

/**
 * Simple manual test runner.
 * Exécute tous les tests et affiche les résultats.
 */
public class SimpleTestRunner {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        System.out.println("=== Exécution des tests ===\n");

        runTests("PieceTest", PieceTest.class);
        runTests("BoardTest", BoardTest.class);
        runTests("EternitySolverTest", EternitySolverTest.class);

        System.out.println("\n=== Résumé des tests ===");
        System.out.println("Total: " + totalTests);
        System.out.println("Réussis: " + passedTests);
        System.out.println("Échoués: " + failedTests);

        if (failedTests == 0) {
            System.out.println("\n✓ Tous les tests sont passés!");
            System.exit(0);
        } else {
            System.out.println("\n✗ Certains tests ont échoué");
            System.exit(1);
        }
    }

    private static void runTests(String className, Class<?> testClass) {
        System.out.println("--- " + className + " ---");

        // Pour simplifier, on va juste instancier la classe et appeler les méthodes
        // Une vraie implémentation utiliserait la reflection pour trouver les méthodes @Test

        try {
            Object instance = testClass.getDeclaredConstructor().newInstance();

            // Exécuter setUp si existe
            try {
                var setUp = testClass.getDeclaredMethod("setUp");
                setUp.setAccessible(true);
            } catch (NoSuchMethodException e) {
                // Pas de setUp, c'est ok
            }

            // Trouver toutes les méthodes de test (celles qui commencent par "test")
            var methods = testClass.getDeclaredMethods();
            for (var method : methods) {
                if (method.getName().startsWith("test")) {
                    totalTests++;
                    try {
                        // Exécuter setUp avant chaque test si existe
                        try {
                            var setUp = testClass.getDeclaredMethod("setUp");
                            setUp.setAccessible(true);
                            setUp.invoke(instance);
                        } catch (NoSuchMethodException e) {
                            // Pas de setUp
                        }

                        method.setAccessible(true);
                        method.invoke(instance);
                        passedTests++;
                        System.out.println("  ✓ " + method.getName());
                    } catch (Exception e) {
                        failedTests++;
                        System.out.println("  ✗ " + method.getName());
                        System.out.println("    Erreur: " + e.getCause().getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("  Erreur lors de l'initialisation: " + e.getMessage());
        }

        System.out.println();
    }
}
