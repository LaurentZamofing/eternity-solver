package benchmark;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.PuzzleFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Benchmarks de performance pour mesurer les optimisations du solver.
 * Compare différentes configurations et mesure les temps d'exécution.
 */
public class PerformanceBenchmark {

    private static class BenchmarkResult {
        String testName;
        boolean solved;
        long durationMs;
        long recursiveCalls;
        long backtracks;
        long singletons;

        public BenchmarkResult(String testName, boolean solved, long durationMs,
                              long recursiveCalls, long backtracks, long singletons) {
            this.testName = testName;
            this.solved = solved;
            this.durationMs = durationMs;
            this.recursiveCalls = recursiveCalls;
            this.backtracks = backtracks;
            this.singletons = singletons;
        }

        @Override
        public String toString() {
            return String.format("%-40s | %7s | %8d ms | %12d | %10d | %9d",
                testName, solved ? "✓" : "✗", durationMs, recursiveCalls, backtracks, singletons);
        }
    }

    /**
     * Exécute un benchmark avec une configuration donnée
     */
    private static BenchmarkResult runBenchmark(String testName, Board board,
                                                Map<Integer, Piece> pieces,
                                                boolean useSingletons,
                                                int timeoutMs) {
        EternitySolver solver = new EternitySolver();
        solver.setDisplayConfig(false, 0); // Mode silencieux
        solver.setUseSingletons(useSingletons);

        long startTime = System.currentTimeMillis();
        boolean solved;

        // Timeout simple
        final boolean[] timedOut = {false};
        Thread solverThread = new Thread(() -> {
            try {
                Thread.sleep(timeoutMs);
                timedOut[0] = true;
            } catch (InterruptedException e) {
                // Terminé avant timeout
            }
        });
        solverThread.start();

        try {
            solved = solver.solve(board, pieces);
        } catch (Exception e) {
            solved = false;
        }

        solverThread.interrupt();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        EternitySolver.Statistics stats = solver.getStatistics();

        return new BenchmarkResult(
            testName,
            solved,
            duration,
            stats.recursiveCalls,
            stats.backtracks,
            stats.singletonsPlaced
        );
    }

    /**
     * Benchmark: Exemple 3x3 (rapide)
     */
    private static BenchmarkResult benchmark3x3() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample3x3();
        return runBenchmark("3x3 Example (avec singletons)", board, pieces, true, 10000);
    }

    /**
     * Benchmark: Exemple 3x3 sans singletons
     */
    private static BenchmarkResult benchmark3x3NoSingletons() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample3x3();
        return runBenchmark("3x3 Example (sans singletons)", board, pieces, false, 10000);
    }

    /**
     * Benchmark: Exemple 4x4
     */
    private static BenchmarkResult benchmark4x4() {
        Board board = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4();
        return runBenchmark("4x4 Example (avec singletons)", board, pieces, true, 30000);
    }

    /**
     * Benchmark: Exemple 4x4 sans singletons
     */
    private static BenchmarkResult benchmark4x4NoSingletons() {
        Board board = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4();
        return runBenchmark("4x4 Example (sans singletons)", board, pieces, false, 30000);
    }

    /**
     * Benchmark: Exemple 4x4 Easy (résolution rapide garantie)
     */
    private static BenchmarkResult benchmark4x4Easy() {
        Board board = new Board(4, 4);
        Map<Integer, Piece> pieces = PuzzleFactory.createExample4x4Easy();
        return runBenchmark("4x4 Example Easy (avec singletons)", board, pieces, true, 10000);
    }

    /**
     * Benchmark: Validation 6x6 (timeout court)
     */
    private static BenchmarkResult benchmark6x6() {
        Board board = new Board(6, 6);
        Map<Integer, Piece> pieces = PuzzleFactory.createValidation6x6();
        return runBenchmark("6x6 Validation (10s timeout)", board, pieces, true, 10000);
    }

    /**
     * Benchmark: Puzzle 6x12 (échantillon)
     */
    private static BenchmarkResult benchmark6x12() {
        Board board = new Board(6, 12);
        Map<Integer, Piece> pieces = PuzzleFactory.createPuzzle6x12();
        return runBenchmark("6x12 Puzzle (10s timeout)", board, pieces, true, 10000);
    }

    /**
     * Calcule le speedup entre deux résultats
     */
    private static double calculateSpeedup(BenchmarkResult baseline, BenchmarkResult optimized) {
        if (optimized.durationMs == 0) return 0.0;
        return (double) baseline.durationMs / optimized.durationMs;
    }

    /**
     * Calcule la réduction des appels récursifs
     */
    private static double calculateReduction(BenchmarkResult baseline, BenchmarkResult optimized) {
        if (baseline.recursiveCalls == 0) return 0.0;
        return 100.0 * (1.0 - (double) optimized.recursiveCalls / baseline.recursiveCalls);
    }

    /**
     * Affiche un rapport de benchmark
     */
    public static void printBenchmarkReport(List<BenchmarkResult> results) {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════════════════════════════");
        System.out.println("RAPPORT DE BENCHMARKS - ETERNITY SOLVER");
        System.out.println("═══════════════════════════════════════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println(String.format("%-40s | %7s | %10s | %12s | %10s | %9s",
            "Test", "Résolu", "Temps", "Appels Réc.", "Backtracks", "Singletons"));
        System.out.println("─────────────────────────────────────────────────────────────────────────────────────────────");

        for (BenchmarkResult result : results) {
            System.out.println(result);
        }

        System.out.println("═══════════════════════════════════════════════════════════════════════════════════════════");
        System.out.println();
    }

    /**
     * Affiche une analyse comparative
     */
    public static void printComparativeAnalysis(List<BenchmarkResult> results) {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════════════════════════════");
        System.out.println("ANALYSE COMPARATIVE - IMPACT DES SINGLETONS");
        System.out.println("═══════════════════════════════════════════════════════════════════════════════════════════");
        System.out.println();

        // Comparer 3x3 avec/sans singletons
        if (results.size() >= 2) {
            BenchmarkResult r3x3Single = results.get(0);
            BenchmarkResult r3x3NoSingle = results.get(1);

            if (r3x3Single.testName.contains("3x3") && r3x3NoSingle.testName.contains("3x3")) {
                double speedup = calculateSpeedup(r3x3NoSingle, r3x3Single);
                double reduction = calculateReduction(r3x3NoSingle, r3x3Single);

                System.out.println("3x3 Benchmark:");
                System.out.println(String.format("  Speedup avec singletons:          %.2fx plus rapide", speedup));
                System.out.println(String.format("  Réduction appels récursifs:       %.1f%%", reduction));
                System.out.println(String.format("  Singletons utilisés:              %d", r3x3Single.singletons));
                System.out.println();
            }
        }

        // Comparer 4x4 avec/sans singletons
        if (results.size() >= 4) {
            BenchmarkResult r4x4Single = results.get(2);
            BenchmarkResult r4x4NoSingle = results.get(3);

            if (r4x4Single.testName.contains("4x4") && r4x4NoSingle.testName.contains("4x4")) {
                double speedup = calculateSpeedup(r4x4NoSingle, r4x4Single);
                double reduction = calculateReduction(r4x4NoSingle, r4x4Single);

                System.out.println("4x4 Benchmark:");
                System.out.println(String.format("  Speedup avec singletons:          %.2fx plus rapide", speedup));
                System.out.println(String.format("  Réduction appels récursifs:       %.1f%%", reduction));
                System.out.println(String.format("  Singletons utilisés:              %d", r4x4Single.singletons));
                System.out.println();
            }
        }

        System.out.println("═══════════════════════════════════════════════════════════════════════════════════════════");
        System.out.println();
    }

    /**
     * Affiche des métriques agrégées
     */
    public static void printAggregateMetrics(List<BenchmarkResult> results) {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════════════════════════════");
        System.out.println("MÉTRIQUES AGRÉGÉES");
        System.out.println("═══════════════════════════════════════════════════════════════════════════════════════════");
        System.out.println();

        long totalTime = 0;
        long totalRecursiveCalls = 0;
        long totalBacktracks = 0;
        long totalSingletons = 0;
        int solvedCount = 0;

        for (BenchmarkResult result : results) {
            totalTime += result.durationMs;
            totalRecursiveCalls += result.recursiveCalls;
            totalBacktracks += result.backtracks;
            totalSingletons += result.singletons;
            if (result.solved) solvedCount++;
        }

        System.out.println(String.format("Total des tests:                  %d", results.size()));
        System.out.println(String.format("Puzzles résolus:                  %d / %d (%.1f%%)",
            solvedCount, results.size(), 100.0 * solvedCount / results.size()));
        System.out.println(String.format("Temps total:                      %d ms (%.2f s)",
            totalTime, totalTime / 1000.0));
        System.out.println(String.format("Appels récursifs totaux:          %,d", totalRecursiveCalls));
        System.out.println(String.format("Backtracks totaux:                %,d", totalBacktracks));
        System.out.println(String.format("Singletons totaux:                %,d", totalSingletons));
        System.out.println();

        if (results.size() > 0) {
            System.out.println(String.format("Temps moyen par test:             %.0f ms", (double) totalTime / results.size()));
            System.out.println(String.format("Appels récursifs moyens:          %,d", totalRecursiveCalls / results.size()));
            System.out.println(String.format("Backtracks moyens:                %,d", totalBacktracks / results.size()));
        }

        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════════════════════════════");
        System.out.println();
    }

    /**
     * Point d'entrée principal
     */
    public static void main(String[] args) {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║           ETERNITY SOLVER - BENCHMARKS DE PERFORMANCE                     ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Lancement des benchmarks...");
        System.out.println();

        List<BenchmarkResult> results = new ArrayList<>();

        // Exécuter les benchmarks
        System.out.print("Running benchmark: 3x3 avec singletons...        ");
        results.add(benchmark3x3());
        System.out.println("✓");

        System.out.print("Running benchmark: 3x3 sans singletons...        ");
        results.add(benchmark3x3NoSingletons());
        System.out.println("✓");

        System.out.print("Running benchmark: 4x4 avec singletons...        ");
        results.add(benchmark4x4());
        System.out.println("✓");

        System.out.print("Running benchmark: 4x4 sans singletons...        ");
        results.add(benchmark4x4NoSingletons());
        System.out.println("✓");

        System.out.print("Running benchmark: 4x4 Easy...                   ");
        results.add(benchmark4x4Easy());
        System.out.println("✓");

        System.out.print("Running benchmark: 6x6 Validation...             ");
        results.add(benchmark6x6());
        System.out.println("✓");

        System.out.print("Running benchmark: 6x12 Puzzle (échantillon)...  ");
        results.add(benchmark6x12());
        System.out.println("✓");

        // Afficher les rapports
        printBenchmarkReport(results);
        printComparativeAnalysis(results);
        printAggregateMetrics(results);

        System.out.println("✓ Benchmarks terminés!");
        System.out.println();
    }
}
