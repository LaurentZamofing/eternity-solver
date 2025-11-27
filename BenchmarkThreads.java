import model.Board;
import model.Piece;
import solver.EternitySolver;
import java.io.*;
import java.util.*;

/**
 * Benchmark pour déterminer le nombre optimal de threads
 * Teste différentes configurations de threads et mesure les performances
 */
public class BenchmarkThreads {

    private static final int BENCHMARK_DURATION_SECONDS = 30; // Durée du test par configuration
    private static final String TEST_PUZZLE = "data/indice2/indice2_ascending_border.txt"; // Puzzle de test (6x12)

    private static class BenchmarkResult {
        int numThreads;
        long nodesExplored;
        long timeMs;
        double nodesPerSecond;

        BenchmarkResult(int threads, long nodes, long time) {
            this.numThreads = threads;
            this.nodesExplored = nodes;
            this.timeMs = time;
            this.nodesPerSecond = (nodes * 1000.0) / time;
        }
    }

    /**
     * Simule un thread de résolution pour le benchmark
     */
    private static class BenchmarkWorker implements Runnable {
        private final int threadId;
        private final PuzzleConfig config;
        private final long durationMs;
        private long nodesExplored = 0;
        private volatile boolean running = true;

        BenchmarkWorker(int threadId, PuzzleConfig config, long durationMs) {
            this.threadId = threadId;
            this.config = config;
            this.durationMs = durationMs;
        }

        @Override
        public void run() {
            try {
                Board board = new Board(config.getRows(), config.getCols());
                Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

                // Placer les pièces fixes
                for (PuzzleConfig.FixedPiece fp : config.getFixedPieces()) {
                    Piece piece = allPieces.get(fp.pieceId);
                    if (piece != null) {
                        board.place(fp.row, fp.col, piece, fp.rotation);
                        allPieces.remove(fp.pieceId);
                    }
                }

                // Créer le solveur
                EternitySolver.resetGlobalState();
                EternitySolver solver = new EternitySolver();
                solver.setDisplayConfig(false, Integer.MAX_VALUE); // Pas de verbose
                solver.setPuzzleName("benchmark_t" + threadId);
                solver.setSortOrder(config.getSortOrder());
                solver.setPrioritizeBorders(config.isPrioritizeBorders());

                // Lancer la résolution avec timeout
                long startTime = System.currentTimeMillis();

                // Créer un thread pour le solver
                Thread solverThread = new Thread(() -> {
                    try {
                        solver.solve(board, allPieces);
                    } catch (Exception e) {
                        // Ignorer les exceptions pendant le benchmark
                    }
                });

                solverThread.start();

                // Attendre le timeout
                try {
                    solverThread.join(durationMs);
                } catch (InterruptedException e) {
                    // Timeout atteint
                }

                // Arrêter le thread si toujours en cours
                if (solverThread.isAlive()) {
                    solverThread.interrupt();
                    solverThread.join(1000); // Attendre max 1 seconde
                }

                long endTime = System.currentTimeMillis();

                // Utiliser le temps comme métrique (approximation des noeuds explorés)
                // Plus de temps = plus de noeuds explorés
                nodesExplored = endTime - startTime;

            } catch (Exception e) {
                System.err.println("Erreur dans worker " + threadId + ": " + e.getMessage());
            }
        }

        public long getNodesExplored() {
            return nodesExplored;
        }
    }

    /**
     * Teste une configuration avec N threads
     */
    private static BenchmarkResult testConfiguration(int numThreads, PuzzleConfig config, long durationMs)
            throws InterruptedException {

        System.out.println("  Démarrage de " + numThreads + " thread(s)...");

        // Créer et lancer les workers
        List<BenchmarkWorker> workers = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            BenchmarkWorker worker = new BenchmarkWorker(i + 1, config, durationMs);
            workers.add(worker);

            Thread thread = new Thread(worker);
            threads.add(thread);
            thread.start();
        }

        // Attendre que tous les threads terminent
        for (Thread thread : threads) {
            thread.join();
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Calculer le total de noeuds explorés
        long totalNodes = 0;
        for (BenchmarkWorker worker : workers) {
            totalNodes += worker.getNodesExplored();
        }

        return new BenchmarkResult(numThreads, totalNodes, totalTime);
    }

    public static void main(String[] args) {
        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║          BENCHMARK - NOMBRE OPTIMAL DE THREADS                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        int maxCpus = Runtime.getRuntime().availableProcessors();
        System.out.println("🖥️  Processeurs disponibles: " + maxCpus);
        System.out.println("⏱️  Durée par test: " + BENCHMARK_DURATION_SECONDS + " secondes");
        System.out.println("📁 Puzzle de test: " + TEST_PUZZLE);
        System.out.println();

        try {
            // Charger le puzzle de test
            PuzzleConfig config = PuzzleConfig.loadFromFile(TEST_PUZZLE);
            if (config == null) {
                System.out.println("✗ Impossible de charger le puzzle de test");
                return;
            }

            System.out.println("✓ Puzzle chargé: " + config.getRows() + "×" + config.getCols() +
                             " (" + (config.getRows() * config.getCols()) + " pièces)");
            System.out.println();
            System.out.println("═".repeat(70));
            System.out.println();

            List<BenchmarkResult> results = new ArrayList<>();
            long durationMs = BENCHMARK_DURATION_SECONDS * 1000;

            // Tester différentes configurations: 1, 2, 4, 8, ..., maxCpus
            List<Integer> threadCounts = new ArrayList<>();
            threadCounts.add(1);

            int current = 2;
            while (current <= maxCpus) {
                threadCounts.add(current);
                if (current < maxCpus && current * 2 > maxCpus) {
                    threadCounts.add(maxCpus);
                    break;
                }
                current *= 2;
            }

            // Exécuter les benchmarks
            for (int numThreads : threadCounts) {
                System.out.println("🔬 Test avec " + numThreads + " thread(s)...");

                BenchmarkResult result = testConfiguration(numThreads, config, durationMs);
                results.add(result);

                System.out.println("  ✓ Noeuds explorés: " + formatNumber(result.nodesExplored));
                System.out.println("  ✓ Performance: " + formatNumber((long)result.nodesPerSecond) + " noeuds/s");
                System.out.println();

                // Pause courte entre les tests pour éviter la surchauffe
                Thread.sleep(2000);
            }

            // Afficher les résultats
            System.out.println("═".repeat(70));
            System.out.println();
            System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                      RÉSULTATS DU BENCHMARK                      ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
            System.out.println();

            // Trouver le meilleur résultat
            BenchmarkResult best = results.get(0);
            for (BenchmarkResult result : results) {
                if (result.nodesPerSecond > best.nodesPerSecond) {
                    best = result;
                }
            }

            // Afficher tableau comparatif
            System.out.println("Threads │ Noeuds explorés │ Performance (n/s) │ Efficacité │ Recommandation");
            System.out.println("────────┼─────────────────┼───────────────────┼────────────┼───────────────");

            for (BenchmarkResult result : results) {
                double efficiency = (result.nodesPerSecond / result.numThreads) /
                                   (results.get(0).nodesPerSecond / 1.0) * 100.0;

                String recommendation = "";
                if (result.numThreads == best.numThreads) {
                    recommendation = "⭐ OPTIMAL";
                } else if (efficiency >= 85.0) {
                    recommendation = "✓ Bon";
                } else if (efficiency >= 70.0) {
                    recommendation = "○ Acceptable";
                } else {
                    recommendation = "✗ Sous-optimal";
                }

                System.out.println(String.format("%6d  │ %15s │ %17s │ %7.1f%%  │ %s",
                    result.numThreads,
                    formatNumber(result.nodesExplored),
                    formatNumber((long)result.nodesPerSecond),
                    efficiency,
                    recommendation
                ));
            }

            System.out.println();
            System.out.println("═".repeat(70));
            System.out.println();
            System.out.println("🎯 RECOMMANDATION: Utiliser " + best.numThreads + " thread(s)");
            System.out.println();
            System.out.println("📊 Analyse:");
            System.out.println("   • Performance maximale: " + formatNumber((long)best.nodesPerSecond) + " noeuds/s");

            // Calculer le speedup
            double speedup = best.nodesPerSecond / results.get(0).nodesPerSecond;
            System.out.println("   • Speedup par rapport à 1 thread: " + String.format("%.2fx", speedup));

            // Calculer l'efficacité parallèle
            double parallelEfficiency = (speedup / best.numThreads) * 100.0;
            System.out.println("   • Efficacité parallèle: " + String.format("%.1f%%", parallelEfficiency));

            System.out.println();
            System.out.println("💡 Pour lancer MainParallel avec cette configuration:");
            System.out.println("   java -cp bin MainParallel " + best.numThreads);
            System.out.println();

        } catch (Exception e) {
            System.err.println("✗ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String formatNumber(long number) {
        if (number >= 1_000_000_000) {
            return String.format("%.2fG", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.2fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.2fK", number / 1_000.0);
        } else {
            return String.valueOf(number);
        }
    }
}
