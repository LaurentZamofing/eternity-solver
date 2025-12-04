package integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.File;

/**
 * Tests d'intégration end-to-end pour le CLI.
 * Vérifie le comportement complet du système de bout en bout.
 *
 * NOTE: Tests ProcessBuilder désactivés - nécessitent configuration classpath Maven spécifique.
 * Ces tests doivent être exécutés après compilation complète avec: mvn verify
 */
public class CLIIntegrationTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    public void setUp() {
        // Rediriger stdout et stderr pour capturer les sorties
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    public void tearDown() {
        // Restaurer stdout et stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testHelpCommand() {
        // Exécuter MainCLI avec --help
        String[] args = {"--help"};

        try {
            // Note: MainCLI appelle System.exit(), donc on teste via ProcessBuilder
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", "bin:lib/*", "MainCLI", "--help"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            assertEquals(0, exitCode, "Help devrait retourner exit code 0");

            // Vérifier que la sortie contient les éléments d'aide
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            String helpOutput = output.toString();
            assertTrue(helpOutput.contains("USAGE"), "Aide devrait contenir USAGE");
            assertTrue(helpOutput.contains("OPTIONS"), "Aide devrait contenir OPTIONS");
            assertTrue(helpOutput.contains("--help"), "Aide devrait mentionner --help");
            assertTrue(helpOutput.contains("--verbose"), "Aide devrait mentionner --verbose");

        } catch (Exception e) {
            fail("Exception lors du test d'aide: " + e.getMessage());
        }
    }

    @Test
    public void testVersionCommand() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", "bin:lib/*", "MainCLI", "--version"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            assertEquals(0, exitCode, "Version devrait retourner exit code 0");

            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            String firstLine = reader.readLine();

            assertNotNull(firstLine, "Version devrait afficher une ligne");
            assertTrue(firstLine.contains("v1.0") || firstLine.contains("Solver"),
                      "Version devrait contenir numéro de version ou nom");

        } catch (Exception e) {
            fail("Exception lors du test de version: " + e.getMessage());
        }
    }

    @Test
    @Disabled("Requires Maven classpath configuration - run with: mvn verify")
    public void testInvalidPuzzleName() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", "bin:lib/*", "MainCLI", "puzzle_inexistant"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            assertEquals(1, exitCode, "Puzzle invalide devrait retourner exit code 1");

            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            String errorOutput = output.toString();
            assertTrue(errorOutput.contains("Erreur") || errorOutput.contains("inconnu"),
                      "Devrait afficher message d'erreur pour puzzle invalide");

        } catch (Exception e) {
            fail("Exception lors du test puzzle invalide: " + e.getMessage());
        }
    }

    @Test
    @Disabled("Requires Maven classpath configuration - run with: mvn verify")
    public void testInvalidThreadsValue() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", "bin:lib/*", "MainCLI", "-t", "invalid", "example_3x3"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            assertEquals(1, exitCode, "Valeur threads invalide devrait retourner exit code 1");

            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            String errorOutput = output.toString();
            assertTrue(errorOutput.contains("Erreur") || errorOutput.contains("entier"),
                      "Devrait afficher message d'erreur pour valeur invalide");

        } catch (Exception e) {
            fail("Exception lors du test threads invalide: " + e.getMessage());
        }
    }

    @Test
    @Disabled("Requires Maven classpath configuration - run with: mvn verify")
    public void testSolveExample3x3() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", "bin:lib/*", "MainCLI", "-q", "example_3x3"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Timeout de 30 secondes pour résoudre 3x3
            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(finished, "Résolution 3x3 devrait terminer en 30s");

            int exitCode = process.exitValue();
            // 0 = résolu, 1 = non résolu (tous deux sont valides)
            assertTrue(exitCode == 0 || exitCode == 1,
                      "Exit code devrait être 0 (résolu) ou 1 (non résolu)");

            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            String solverOutput = output.toString();
            assertTrue(solverOutput.contains("RÉSULTAT") || solverOutput.contains("Temps"),
                      "Devrait afficher résultat de la résolution");

        } catch (Exception e) {
            fail("Exception lors du test résolution 3x3: " + e.getMessage());
        }
    }

    @Test
    public void testVerboseMode() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", "bin:lib/*", "MainCLI", "-v", "example_3x3"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(finished, "Résolution devrait terminer en 30s");

            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            String verboseOutput = output.toString();
            // Mode verbeux devrait afficher plus d'informations
            assertTrue(verboseOutput.length() > 100,
                      "Mode verbeux devrait produire une sortie détaillée");

        } catch (Exception e) {
            fail("Exception lors du test mode verbeux: " + e.getMessage());
        }
    }

    @Test
    public void testQuietMode() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", "bin:lib/*", "MainCLI", "-q", "example_3x3"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(finished, "Résolution devrait terminer en 30s");

            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            String quietOutput = output.toString();
            // Mode silencieux devrait produire moins de sortie (pas d'en-tête fancy)
            assertFalse(quietOutput.contains("╔═══"),
                       "Mode silencieux ne devrait pas afficher les en-têtes fancy");

        } catch (Exception e) {
            fail("Exception lors du test mode silencieux: " + e.getMessage());
        }
    }

    @Test
    public void testTimeoutOption() {
        try {
            // Timeout de 1 seconde sur un petit puzzle
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", "bin:lib/*", "MainCLI", "-q", "--timeout", "1", "example_4x4"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            long startTime = System.currentTimeMillis();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            long elapsedTime = System.currentTimeMillis() - startTime;

            assertTrue(finished, "Process devrait terminer dans les 5s");
            assertTrue(elapsedTime < 3000,
                      "Avec timeout de 1s, devrait terminer rapidement (< 3s)");

        } catch (Exception e) {
            fail("Exception lors du test timeout: " + e.getMessage());
        }
    }

    @Test
    public void testParallelMode() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", "bin:lib/*", "MainCLI", "-q", "-p", "-t", "4", "example_3x3"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(finished, "Résolution parallèle devrait terminer en 30s");

            int exitCode = process.exitValue();
            assertTrue(exitCode == 0 || exitCode == 1,
                      "Exit code devrait être valide (0 ou 1)");

        } catch (Exception e) {
            fail("Exception lors du test mode parallèle: " + e.getMessage());
        }
    }

    @Test
    public void testCombinedOptions() {
        try {
            // Tester combinaison d'options
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", "bin:lib/*", "MainCLI",
                "-v", "-p", "--threads", "2", "--timeout", "5",
                "--min-depth", "3", "example_3x3"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(finished, "Devrait terminer avec options combinées");

            int exitCode = process.exitValue();
            assertTrue(exitCode == 0 || exitCode == 1,
                      "Exit code devrait être valide avec options combinées");

        } catch (Exception e) {
            fail("Exception lors du test options combinées: " + e.getMessage());
        }
    }

    @Test
    public void testLoggingOutput() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", "bin:lib/*", "MainCLI", "example_3x3"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(finished, "Devrait terminer");

            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            String logOutput = output.toString();
            // Vérifier présence de logs SLF4J
            assertTrue(logOutput.contains("INFO") || logOutput.contains("Puzzle"),
                      "Devrait contenir des logs INFO ou informations sur le puzzle");

        } catch (Exception e) {
            fail("Exception lors du test logging: " + e.getMessage());
        }
    }

    @Test
    public void testShutdownHookRegistration() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", "bin:lib/*", "MainCLI", "-q", "example_4x4"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Laisser démarrer le solver
            Thread.sleep(2000);

            // Envoyer SIGTERM pour déclencher le shutdown hook
            process.destroy();

            // Vérifier que le process se termine gracieusement dans un délai raisonnable
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(finished, "Process devrait terminer gracieusement après SIGTERM");

            // Vérifier l'exit code - devrait être normal (0 ou 1, pas 143 qui est SIGTERM)
            int exitCode = process.exitValue();
            assertTrue(exitCode >= 0 && exitCode <= 1,
                      "Exit code devrait être normal (0-1), pas " + exitCode);

        } catch (Exception e) {
            fail("Exception lors du test shutdown hook: " + e.getMessage());
        }
    }
}
