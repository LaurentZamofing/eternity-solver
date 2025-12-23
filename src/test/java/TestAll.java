import solver.BasicsTest;
import solver.SolverTest;
import solver.MRVTieBreakingTest;
import util.SaveLoadTest;

/**
 * Suite de tests complète - Lance tous les tests dans l'ordre
 */
public class TestAll {

    public static void main(String[] args) {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                   SUITE DE TESTS COMPLÈTE                        ║");
        System.out.println("║                  VALIDATION PRÉ-LANCEMENT                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        int totalTests = 0;
        int totalPassed = 0;
        int suitesFailed = 0;

        // Test 1: Tests de base
        System.out.println("┌──────────────────────────────────────────────────────────────────┐");
        System.out.println("│ SUITE 1/3: Tests de base                                        │");
        System.out.println("└──────────────────────────────────────────────────────────────────┘");
        try {
            BasicsTest.main(new String[]{});
            totalPassed++;
        } catch (Exception e) {
            suitesFailed++;
            System.err.println("⚠️  Tests de base échoués!");
        }
        System.out.println();

        // Test 2: Tests du solveur
        System.out.println("┌──────────────────────────────────────────────────────────────────┐");
        System.out.println("│ SUITE 2/3: Tests du solveur                                     │");
        System.out.println("└──────────────────────────────────────────────────────────────────┘");
        try {
            SolverTest.main(new String[]{});
            totalPassed++;
        } catch (Exception e) {
            suitesFailed++;
            System.err.println("⚠️  Tests du solveur échoués!");
        }
        System.out.println();

        // Test 3: Tests de sauvegarde/chargement
        System.out.println("┌──────────────────────────────────────────────────────────────────┐");
        System.out.println("│ SUITE 3/4: Tests sauvegarde/chargement                          │");
        System.out.println("└──────────────────────────────────────────────────────────────────┘");
        try {
            SaveLoadTest.main(new String[]{});
            totalPassed++;
        } catch (Exception e) {
            suitesFailed++;
            System.err.println("⚠️  Tests save/load échoués!");
        }
        System.out.println();

        // Test 4: Tests du tie-breaking MRV
        System.out.println("┌──────────────────────────────────────────────────────────────────┐");
        System.out.println("│ SUITE 4/4: Tests MRV tie-breaking (contraintes)                │");
        System.out.println("└──────────────────────────────────────────────────────────────────┘");
        try {
            MRVTieBreakingTest.main(new String[]{});
            totalPassed++;
        } catch (Exception e) {
            suitesFailed++;
            System.err.println("⚠️  Tests MRV tie-breaking échoués!");
        }
        System.out.println();

        // Résumé final
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      RÉSUMÉ FINAL                                ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Suites de tests exécutées: %-37d║%n", 4);
        System.out.printf("║ Suites réussies:           %-37d║%n", totalPassed);
        System.out.printf("║ Suites échouées:           %-37d║%n", suitesFailed);
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        if (suitesFailed == 0) {
            System.out.println("✓✓✓ TOUS LES TESTS ONT RÉUSSI ✓✓✓");
            System.out.println();
            System.out.println("Le système est validé et prêt pour une utilisation longue durée.");
            System.out.println("Vous pouvez lancer en toute confiance des recherches de plusieurs jours.");
            System.out.println();
            System.exit(0);
        } else {
            System.out.println("✗✗✗ CERTAINS TESTS ONT ÉCHOUÉ ✗✗✗");
            System.out.println();
            System.out.println("⚠️  NE PAS LANCER DE RECHERCHE LONGUE DURÉE!");
            System.out.println("Corrigez d'abord les erreurs détectées.");
            System.out.println();
            System.exit(1);
        }
    }
}
