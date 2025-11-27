import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Benchmark simple pour déterminer le nombre optimal de threads
 * Mesure le temps nécessaire pour effectuer un calcul parallélisable
 */
public class BenchmarkThreadsSimple {

    private static final int WORK_SIZE = 10_000_000; // Taille du travail à paralléliser
    private static final int WARMUP_ITERATIONS = 2;
    private static final int BENCHMARK_ITERATIONS = 3;

    /**
     * Tâche de calcul intensif simulant la résolution de puzzle
     */
    private static class ComputeTask implements Callable<Long> {
        private final int start;
        private final int end;

        ComputeTask(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public Long call() {
            long sum = 0;
            // Calcul intensif : simulation de backtracking
            for (int i = start; i < end; i++) {
                // Simule des opérations de validation de pièces
                int value = i;
                for (int j = 0; j < 100; j++) {
                    value = (value * 31 + j) % 1000000007;
                }
                sum += value;
            }
            return sum;
        }
    }

    /**
     * Teste une configuration avec N threads
     */
    private static long testConfiguration(int numThreads) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Long>> futures = new ArrayList<>();

        long startTime = System.nanoTime();

        // Diviser le travail entre les threads
        int workPerThread = WORK_SIZE / numThreads;
        for (int i = 0; i < numThreads; i++) {
            int start = i * workPerThread;
            int end = (i == numThreads - 1) ? WORK_SIZE : (i + 1) * workPerThread;
            futures.add(executor.submit(new ComputeTask(start, end)));
        }

        // Attendre que tout soit terminé
        long totalSum = 0;
        for (Future<Long> future : futures) {
            totalSum += future.get();
        }

        long endTime = System.nanoTime();
        executor.shutdown();

        return (endTime - startTime) / 1_000_000; // Convertir en millisecondes
    }

    public static void main(String[] args) {
        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║          BENCHMARK - NOMBRE OPTIMAL DE THREADS                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        int maxCpus = Runtime.getRuntime().availableProcessors();
        System.out.println("🖥️  Processeurs disponibles: " + maxCpus);
        System.out.println("⚙️  Taille du travail: " + String.format("%,d", WORK_SIZE) + " opérations");
        System.out.println("🔄 Itérations par test: " + BENCHMARK_ITERATIONS);
        System.out.println();

        try {
            // Liste des configurations à tester
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

            // Warmup
            System.out.println("🔥 Phase de chauffe (warmup)...");
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                testConfiguration(maxCpus);
            }
            System.out.println("✓ Warmup terminé");
            System.out.println();
            System.out.println("═".repeat(70));
            System.out.println();

            // Résultats du benchmark
            Map<Integer, List<Long>> results = new LinkedHashMap<>();

            for (int numThreads : threadCounts) {
                System.out.print("🔬 Test avec " + numThreads + " thread(s)... ");
                System.out.flush();

                List<Long> times = new ArrayList<>();
                for (int iter = 0; iter < BENCHMARK_ITERATIONS; iter++) {
                    long time = testConfiguration(numThreads);
                    times.add(time);
                    Thread.sleep(100); // Petite pause entre itérations
                }

                results.put(numThreads, times);

                // Calculer la moyenne
                long avgTime = times.stream().mapToLong(Long::longValue).sum() / times.size();
                System.out.println(avgTime + " ms (moy.)");
            }

            // Afficher les résultats détaillés
            System.out.println();
            System.out.println("═".repeat(70));
            System.out.println();
            System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                      RÉSULTATS DU BENCHMARK                      ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
            System.out.println();

            // Calculer les métriques
            long baselineTime = results.get(1).stream().mapToLong(Long::longValue).sum() / results.get(1).size();

            System.out.println("Threads │ Temps moyen │ Speedup │ Efficacité │ Recommandation");
            System.out.println("────────┼─────────────┼─────────┼────────────┼───────────────");

            int bestThreads = 1;
            double bestSpeedup = 1.0;

            for (Map.Entry<Integer, List<Long>> entry : results.entrySet()) {
                int numThreads = entry.getKey();
                long avgTime = entry.getValue().stream().mapToLong(Long::longValue).sum() / entry.getValue().size();

                double speedup = (double) baselineTime / avgTime;
                double efficiency = (speedup / numThreads) * 100.0;

                if (speedup > bestSpeedup) {
                    bestSpeedup = speedup;
                    bestThreads = numThreads;
                }

                String recommendation;
                if (numThreads == bestThreads && speedup == bestSpeedup) {
                    recommendation = "⭐ OPTIMAL";
                } else if (efficiency >= 80.0) {
                    recommendation = "✓ Excellent";
                } else if (efficiency >= 60.0) {
                    recommendation = "○ Bon";
                } else if (efficiency >= 40.0) {
                    recommendation = "△ Acceptable";
                } else {
                    recommendation = "✗ Sous-optimal";
                }

                System.out.println(String.format("%6d  │ %8d ms │ %6.2fx │ %7.1f%%  │ %s",
                    numThreads,
                    avgTime,
                    speedup,
                    efficiency,
                    recommendation
                ));
            }

            System.out.println();
            System.out.println("═".repeat(70));
            System.out.println();
            System.out.println("🎯 RECOMMANDATION: Utiliser " + bestThreads + " thread(s)");
            System.out.println();
            System.out.println("📊 Analyse:");

            long bestTime = results.get(bestThreads).stream().mapToLong(Long::longValue).sum() / results.get(bestThreads).size();
            System.out.println("   • Temps optimal: " + bestTime + " ms");
            System.out.println("   • Speedup: " + String.format("%.2fx", bestSpeedup) + " par rapport à 1 thread");

            double parallelEfficiency = (bestSpeedup / bestThreads) * 100.0;
            System.out.println("   • Efficacité parallèle: " + String.format("%.1f%%", parallelEfficiency));

            // Interpréter l'efficacité
            System.out.println();
            System.out.println("💡 Interprétation:");
            if (parallelEfficiency >= 90.0) {
                System.out.println("   Excellente scalabilité - le problème se parallélise très bien");
            } else if (parallelEfficiency >= 70.0) {
                System.out.println("   Bonne scalabilité - gain significatif avec plusieurs threads");
            } else if (parallelEfficiency >= 50.0) {
                System.out.println("   Scalabilité modérée - gain présent mais limité");
            } else {
                System.out.println("   Scalabilité faible - overhead de parallélisation important");
            }

            System.out.println();
            System.out.println("🚀 Pour lancer MainParallel avec cette configuration:");
            System.out.println("   java -cp bin MainParallel " + bestThreads);
            System.out.println();

        } catch (Exception e) {
            System.err.println("✗ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
