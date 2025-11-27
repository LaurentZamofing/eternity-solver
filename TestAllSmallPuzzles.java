import model.Board;
import model.Piece;
import solver.EternitySolver;
import java.io.IOException;
import java.util.*;

/**
 * Test de validation pour tous les petits puzzles avec PrioritizeBorders
 * Vérifie qu'aucun gap n'apparaît sur les bords
 */
public class TestAllSmallPuzzles {

    private static class TestResult {
        String puzzleName;
        boolean solved;
        boolean hasGaps;
        long duration;
        int pieces;

        TestResult(String name, boolean solved, boolean hasGaps, long duration, int pieces) {
            this.puzzleName = name;
            this.solved = solved;
            this.hasGaps = hasGaps;
            this.duration = duration;
            this.pieces = pieces;
        }
    }

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║    TEST DE VALIDATION: Tous les petits puzzles                  ║");
        System.out.println("║         (avec et sans priorisation des bords)                    ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        String[] puzzles = {
            // Online puzzles (4x4)
            "data/online/online_ascending.txt",
            "data/online/online_descending.txt",
            "data/online/online_ascending_border.txt",
            "data/online/online_descending_border.txt",

            // Indice1 puzzles (6x6)
            "data/indice1/indice1.txt",
            "data/indice1/indice1_border.txt",

            // Indice2 puzzles (6x12)
            "data/indice2/indice2_ascending.txt",
            "data/indice2/indice2_descending.txt",
            "data/indice2/indice2_ascending_border.txt",
            "data/indice2/indice2_descending_border.txt",

            // Indice3 puzzles (6x6)
            "data/indice3/indice3_ascending.txt",
            "data/indice3/indice3_descending.txt",
            "data/indice3/indice3_ascending_border.txt",
            "data/indice3/indice3_descending_border.txt",

            // Indice4 puzzles (6x12)
            "data/indice4/indice4_ascending.txt",
            "data/indice4/indice4_descending.txt",
            "data/indice4/indice4_ascending_border.txt",
            "data/indice4/indice4_descending_border.txt"
        };

        List<TestResult> results = new ArrayList<>();
        int totalTests = puzzles.length;
        int passed = 0;

        for (int i = 0; i < puzzles.length; i++) {
            String filepath = puzzles[i];
            System.out.println("─".repeat(70));
            System.out.println("Test " + (i + 1) + "/" + totalTests + ": " + filepath.substring(filepath.lastIndexOf('/') + 1));
            System.out.println("─".repeat(70));

            TestResult result = testPuzzle(filepath);
            results.add(result);

            if (result.solved && !result.hasGaps) {
                passed++;
                System.out.println("✓ SUCCÈS: Solution trouvée sans gaps");
            } else if (result.solved && result.hasGaps) {
                System.out.println("✗ ÉCHEC: Solution trouvée mais avec des gaps!");
            } else {
                System.out.println("⚠ Non résolu (timeout ou impossible)");
            }

            System.out.println("  Temps: " + formatDuration(result.duration));
            System.out.println();
        }

        // Afficher le résumé
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      RÉSUMÉ DES TESTS                            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        for (TestResult result : results) {
            String status = result.solved && !result.hasGaps ? "✓" :
                           result.solved && result.hasGaps ? "✗ GAPS" :
                           "⚠ NON RÉSOLU";

            System.out.println(String.format("%-45s %s (%s, %dx%d pièces)",
                result.puzzleName,
                status,
                formatDuration(result.duration),
                (int)Math.sqrt(result.pieces),
                (int)Math.sqrt(result.pieces)
            ));
        }

        System.out.println();
        System.out.println("═".repeat(70));
        System.out.println("RÉSULTAT FINAL: " + passed + "/" + totalTests + " tests réussis");

        if (passed == totalTests) {
            System.out.println("🎉 TOUS LES TESTS SONT PASSÉS!");
        } else {
            System.out.println("⚠ Certains tests ont échoué");
        }
        System.out.println("═".repeat(70));
    }

    private static TestResult testPuzzle(String filepath) {
        try {
            PuzzleConfig config = PuzzleConfig.loadFromFile(filepath);
            if (config == null) {
                return new TestResult(filepath, false, false, 0, 0);
            }

            Board board = new Board(config.getRows(), config.getCols());
            Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());
            int totalPieces = config.getRows() * config.getCols();

            EternitySolver.resetGlobalState();
            EternitySolver solver = new EternitySolver();
            solver.setDisplayConfig(false, Integer.MAX_VALUE); // Pas de verbose
            solver.setPuzzleName(config.getType());
            solver.setSortOrder(config.getSortOrder());
            solver.setPrioritizeBorders(config.isPrioritizeBorders());

            long startTime = System.currentTimeMillis();
            boolean solved = solver.solve(board, allPieces);
            long duration = System.currentTimeMillis() - startTime;

            if (solved) {
                boolean hasGaps = checkForBorderGaps(board);
                return new TestResult(filepath.substring(filepath.lastIndexOf('/') + 1),
                                     true, hasGaps, duration, totalPieces);
            } else {
                return new TestResult(filepath.substring(filepath.lastIndexOf('/') + 1),
                                     false, false, duration, totalPieces);
            }

        } catch (Exception e) {
            System.out.println("  ✗ Erreur: " + e.getMessage());
            return new TestResult(filepath, false, false, 0, 0);
        }
    }

    private static boolean checkForBorderGaps(Board board) {
        int rows = board.getRows();
        int cols = board.getCols();

        // Vérifier le bord supérieur
        for (int c = 1; c < cols - 1; c++) {
            if (board.isEmpty(0, c) && !board.isEmpty(0, c - 1) && !board.isEmpty(0, c + 1)) {
                System.out.println("  ⚠ Gap sur bord supérieur à [0," + c + "]");
                return true;
            }
        }

        // Vérifier le bord inférieur
        for (int c = 1; c < cols - 1; c++) {
            if (board.isEmpty(rows - 1, c) && !board.isEmpty(rows - 1, c - 1) && !board.isEmpty(rows - 1, c + 1)) {
                System.out.println("  ⚠ Gap sur bord inférieur à [" + (rows - 1) + "," + c + "]");
                return true;
            }
        }

        // Vérifier le bord gauche
        for (int r = 1; r < rows - 1; r++) {
            if (board.isEmpty(r, 0) && !board.isEmpty(r - 1, 0) && !board.isEmpty(r + 1, 0)) {
                System.out.println("  ⚠ Gap sur bord gauche à [" + r + ",0]");
                return true;
            }
        }

        // Vérifier le bord droit
        for (int r = 1; r < rows - 1; r++) {
            if (board.isEmpty(r, cols - 1) && !board.isEmpty(r - 1, cols - 1) && !board.isEmpty(r + 1, cols - 1)) {
                System.out.println("  ⚠ Gap sur bord droit à [" + r + "," + (cols - 1) + "]");
                return true;
            }
        }

        return false;
    }

    private static String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + "ms";
        } else if (ms < 60000) {
            return String.format("%.1fs", ms / 1000.0);
        } else {
            return String.format("%.1fmin", ms / 60000.0);
        }
    }
}
