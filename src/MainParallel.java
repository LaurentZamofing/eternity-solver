import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.SaveStateManager;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Lanceur parallèle pour Eternity II
 * Lance plusieurs threads sur différentes configurations de manière intelligente :
 * 1. Priorité aux configurations jamais commencées
 * 2. Puis reprise des sauvegardes les plus anciennes
 */
public class MainParallel {

    private static final String DATA_DIR = "data/";

    /**
     * Information sur une configuration disponible
     */
    private static class ConfigInfo implements Comparable<ConfigInfo> {
        final String filepath;
        final PuzzleConfig config;
        final File currentSave;
        final long totalComputeTimeMs;
        final boolean hasBeenStarted;

        ConfigInfo(String filepath, PuzzleConfig config, File currentSave, long totalComputeTimeMs) {
            this.filepath = filepath;
            this.config = config;
            this.currentSave = currentSave;
            this.totalComputeTimeMs = totalComputeTimeMs;
            this.hasBeenStarted = (currentSave != null);
        }

        @Override
        public int compareTo(ConfigInfo other) {
            // 1. Priorité aux configs jamais commencées
            if (!this.hasBeenStarted && other.hasBeenStarted) return -1;
            if (this.hasBeenStarted && !other.hasBeenStarted) return 1;

            // 2. Parmi les commencées, trier par temps cumulé (moins de temps = prioritaire)
            if (this.hasBeenStarted && other.hasBeenStarted) {
                return Long.compare(this.totalComputeTimeMs, other.totalComputeTimeMs);
            }

            // 3. Parmi les non commencées, ordre alphabétique
            return this.filepath.compareTo(other.filepath);
        }
    }

    /**
     * Trouve toutes les configurations Eternity II disponibles
     */
    private static List<ConfigInfo> findAllConfigurations() throws IOException {
        List<ConfigInfo> configs = new ArrayList<>();

        File dataDir = new File(DATA_DIR + "eternity2/");
        File[] configFiles = dataDir.listFiles((dir, name) ->
            name.startsWith("eternity2_p") && name.endsWith(".txt")
        );

        if (configFiles == null || configFiles.length == 0) {
            System.out.println("✗ Aucune configuration trouvée dans " + DATA_DIR);
            return configs;
        }

        System.out.println("📁 Analyse de " + configFiles.length + " configurations disponibles...");
        System.out.println();

        for (File file : configFiles) {
            try {
                // Charger la config
                PuzzleConfig config = PuzzleConfig.loadFromFile(file.getAbsolutePath());
                if (config == null) continue;

                // Extraire le configId depuis le nom du fichier
                String configId = extractConfigId(file.getAbsolutePath());

                // Chercher une sauvegarde current pour cette config
                File currentSave = SaveStateManager.findCurrentSave(configId);

                // Lire le temps total de calcul cumulé
                long totalComputeTimeMs = 0;
                if (currentSave != null) {
                    totalComputeTimeMs = SaveStateManager.readTotalComputeTime(configId);
                }

                configs.add(new ConfigInfo(file.getAbsolutePath(), config, currentSave, totalComputeTimeMs));

            } catch (Exception e) {
                System.err.println("⚠️  Erreur lors du chargement de " + file.getName() + ": " + e.getMessage());
            }
        }

        // Trier selon la priorité
        Collections.sort(configs);

        return configs;
    }

    /**
     * Extrait un ID de configuration depuis le chemin du fichier
     * Ex: "data/puzzle_eternity2_p01_1_2_3_4_ascending.txt" -> "eternity2_p01_ascending"
     */
    private static String extractConfigId(String filepath) {
        String filename = new File(filepath).getName();
        // Extraire: eternity2_p01_ascending.txt -> eternity2_p01_ascending
        // Ou: eternity2_p01_ascending_border.txt -> eternity2_p01_ascending_border

        // Enlever ".txt" à la fin
        if (filename.endsWith(".txt")) {
            return filename.substring(0, filename.length() - 4);
        }

        // Fallback: retourner le nom tel quel
        return filename;
    }

    /**
     * Crée un label pour identifier le thread dans les logs
     * Ex: "[T1-p01_asc] "
     */
    private static String createThreadLabel(int threadId, String configId) {
        // Extraire pXX et order depuis configId
        // Ex: "eternity2_p01_ascending" -> "p01_asc"
        String shortLabel = configId.replace("eternity2_", "").replace("ascending", "asc").replace("descending", "desc");
        return "[T" + threadId + "-" + shortLabel + "] ";
    }

    /**
     * Affiche les statistiques des configurations
     */
    private static void displayConfigStats(List<ConfigInfo> configs) {
        int notStarted = 0;
        int inProgress = 0;

        for (ConfigInfo info : configs) {
            if (!info.hasBeenStarted) {
                notStarted++;
            } else {
                inProgress++;
            }
        }

        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║              STATISTIQUES DES CONFIGURATIONS                     ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  📊 Total configurations : " + configs.size());
        System.out.println("  🆕 Jamais commencées    : " + notStarted);
        System.out.println("  🔄 En cours             : " + inProgress);
        System.out.println();
    }

    /**
     * Lance la résolution d'une configuration dans un thread avec timeout
     */
    private static class SolverTask implements Callable<Boolean> {
        private final ConfigInfo configInfo;
        private final int threadId;
        private final long timeoutMs;

        SolverTask(ConfigInfo configInfo, int threadId, long timeoutMs) {
            this.configInfo = configInfo;
            this.threadId = threadId;
            this.timeoutMs = timeoutMs;
        }

        @Override
        public Boolean call() {
            try {
                System.out.println("🚀 [Thread " + threadId + "] Démarrage: " + configInfo.config.getName());
                System.out.println("   Fichier: " + new File(configInfo.filepath).getName());
                if (configInfo.hasBeenStarted) {
                    long totalSeconds = configInfo.totalComputeTimeMs / 1000;
                    long hours = totalSeconds / 3600;
                    long minutes = (totalSeconds % 3600) / 60;
                    long seconds = totalSeconds % 60;
                    System.out.println("   Statut: REPRISE (temps cumulé: " +
                        String.format("%dh %02dm %02ds", hours, minutes, seconds) + ")");
                } else {
                    System.out.println("   Statut: NOUVEAU");
                }
                System.out.println();

                // Charger le puzzle
                PuzzleConfig config = configInfo.config;

                // Créer un ID unique basé sur le nom du fichier (ex: eternity2_p01_ascending)
                String configId = extractConfigId(configInfo.filepath);

                // Chercher une sauvegarde current pour cette config spécifique
                File currentSave = SaveStateManager.findCurrentSave(configId);

                if (currentSave != null && currentSave.exists()) {
                    // Reprise depuis sauvegarde
                    SaveStateManager.SaveState saveState = SaveStateManager.loadStateFromFile(currentSave, config.getType());

                    if (saveState != null) {
                        Board board = new Board(config.getRows(), config.getCols());
                        Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

                        boolean restored = SaveStateManager.restoreState(saveState, board, allPieces);
                        if (restored) {
                            List<Integer> unusedIds = new ArrayList<>(saveState.unusedPieceIds);

                            // Trier selon l'ordre configuré
                            if ("descending".equalsIgnoreCase(config.getSortOrder())) {
                                Collections.sort(unusedIds, Collections.reverseOrder());
                            } else {
                                Collections.sort(unusedIds);
                            }

                            // Créer et configurer le solveur
                            EternitySolver.resetGlobalState();
                            EternitySolver solver = new EternitySolver();
                            solver.setDisplayConfig(config.isVerbose(), config.getMinDepthToShowRecords());

                            // Utiliser le configId déjà extrait
                            solver.setPuzzleName(configId);
                            solver.setSortOrder(config.getSortOrder());
                            solver.setPrioritizeBorders(config.isPrioritizeBorders());
                            solver.setNumFixedPieces(config.getFixedPieces().size());
                            solver.setThreadLabel(createThreadLabel(threadId, configId));

                            System.out.println("   [Thread " + threadId + "] Reprise: " + saveState.depth + " pièces placées");

                            // Résoudre
                            boolean solved = solver.solveWithHistory(board, allPieces, unusedIds,
                                                                     new ArrayList<>(saveState.placementOrder));

                            if (solved) {
                                System.out.println("✅ [Thread " + threadId + "] SOLUTION TROUVÉE!");
                            }

                            return solved;
                        }
                    }
                }

                // Démarrage depuis le début
                System.out.println("   [Thread " + threadId + "] Démarrage depuis le début");

                Board board = new Board(config.getRows(), config.getCols());
                Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

                // Placer les pièces fixes
                for (PuzzleConfig.FixedPiece fp : config.getFixedPieces()) {
                    Piece piece = allPieces.get(fp.pieceId);
                    if (piece != null) {
                        board.place(fp.row, fp.col, piece, fp.rotation);
                        allPieces.remove(fp.pieceId);  // Retirer de la copie locale, pas du config original
                    }
                }

                // Résoudre
                EternitySolver.resetGlobalState();
                EternitySolver solver = new EternitySolver();
                solver.setDisplayConfig(config.isVerbose(), config.getMinDepthToShowRecords());

                // Utiliser le configId déjà extrait
                solver.setPuzzleName(configId);
                solver.setSortOrder(config.getSortOrder());
                solver.setPrioritizeBorders(config.isPrioritizeBorders());
                solver.setThreadLabel(createThreadLabel(threadId, configId));
                solver.setMaxExecutionTime(timeoutMs); // Configurer le timeout

                System.out.println("   [Thread " + threadId + "] Pièces à placer: " + allPieces.size() + " pièces");
                System.out.println("   [Thread " + threadId + "] Pièces fixes sur le board: " + config.getFixedPieces().size());
                System.out.println("   [Thread " + threadId + "] Timeout configuré: " + (timeoutMs / 1000) + " secondes");
                System.out.println("   [Thread " + threadId + "] Démarrage du solver...");

                boolean solved = solver.solve(board, allPieces);

                System.out.println("   [Thread " + threadId + "] Solver terminé. Résultat: " + (solved ? "SOLUTION TROUVÉE" : "Pas de solution"));

                if (solved) {
                    System.out.println("✅ [Thread " + threadId + "] SOLUTION TROUVÉE!");
                }

                return solved;

            } catch (Exception e) {
                System.err.println("✗ [Thread " + threadId + "] Erreur: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }

    // Verrou pour éviter que plusieurs threads prennent la même config
    private static final Object configSelectionLock = new Object();

    // Tracker pour les configs en cours d'exécution
    private static final Set<String> runningConfigs = Collections.synchronizedSet(new HashSet<>());

    /**
     * Worker thread qui tourne en boucle avec rotation automatique
     */
    private static void runWorkerWithRotation(int threadId, long timeoutMs,
                                               ExecutorService executor,
                                               Set<String> solvedConfigs) throws Exception {
        while (true) {
            ConfigInfo nextConfig = null;
            String configId = null;

            // Sélection atomique de la prochaine config disponible
            synchronized (configSelectionLock) {
                // Recharger la liste des configurations pour obtenir les priorités à jour
                List<ConfigInfo> configs = findAllConfigurations();

                // Filtrer les configs déjà résolues ou en cours d'exécution
                for (ConfigInfo config : configs) {
                    String cid = extractConfigId(config.filepath);
                    if (!solvedConfigs.contains(cid) && !runningConfigs.contains(cid)) {
                        nextConfig = config;
                        configId = cid;
                        runningConfigs.add(configId); // Réserver cette config
                        break;
                    }
                }
            }

            if (nextConfig == null) {
                System.out.println("🎉 [Thread " + threadId + "] Toutes les configurations sont résolues ou en cours!");
                break;
            }

            try {
                // Afficher la rotation
                if (nextConfig.hasBeenStarted) {
                    long totalSeconds = nextConfig.totalComputeTimeMs / 1000;
                    long hours = totalSeconds / 3600;
                    long minutes = (totalSeconds % 3600) / 60;
                    System.out.println("🔄 [Thread " + threadId + "] Rotation vers: " + configId +
                        " (temps cumulé: " + String.format("%dh%02dm", hours, minutes) + ")");
                } else {
                    System.out.println("🔄 [Thread " + threadId + "] Rotation vers: " + configId + " (NOUVEAU)");
                }

                // Lancer la résolution directement (pas via executor pour éviter deadlock)
                SolverTask task = new SolverTask(nextConfig, threadId, timeoutMs);

                try {
                    // Exécuter directement dans le thread actuel
                    Boolean solved = task.call();

                    if (solved != null && solved) {
                        System.out.println("✅ [Thread " + threadId + "] SOLUTION TROUVÉE pour " + configId);
                        solvedConfigs.add(configId);
                    } else {
                        System.out.println("⏱️  [Thread " + threadId + "] Timeout atteint pour " + configId + " - rotation");
                    }

                } catch (Exception e) {
                    System.err.println("✗ [Thread " + threadId + "] Erreur lors de l'exécution: " + e.getMessage());
                }

            } finally {
                // Libérer la config pour les autres threads
                runningConfigs.remove(configId);
            }

            // Petite pause avant la prochaine itération
            Thread.sleep(1000);
        }
    }

    public static void main(String[] args) {
        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║          ETERNITY II - RÉSOLVEUR PARALLÈLE                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Nombre de threads (par défaut: nombre de processeurs disponibles)
        int numThreads = Runtime.getRuntime().availableProcessors();

        // Durée par configuration en minutes (par défaut: 60 minutes = 1 heure)
        double timePerConfigMinutes = 60.0;

        if (args.length > 0) {
            try {
                numThreads = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("⚠️  Argument 1 invalide, utilisation de " + numThreads + " threads");
            }
        }

        if (args.length > 1) {
            try {
                timePerConfigMinutes = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("⚠️  Argument 2 invalide, utilisation de " + timePerConfigMinutes + " minutes par config");
            }
        }

        System.out.println("⚙️  Nombre de threads: " + numThreads);
        System.out.println("⏱️  Durée par configuration: " + timePerConfigMinutes + " minute(s)");
        System.out.println();

        try {
            // Trouver toutes les configurations
            List<ConfigInfo> configs = findAllConfigurations();

            if (configs.isEmpty()) {
                System.out.println("✗ Aucune configuration disponible");
                return;
            }

            // Afficher les statistiques
            displayConfigStats(configs);

            // Créer le pool de threads
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
            System.out.println("║              LANCEMENT DES THREADS AVEC ROTATION                 ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("📋 Stratégie de rotation:");
            System.out.println("   1. Chaque thread travaille " + timePerConfigMinutes + " min sur une configuration");
            System.out.println("   2. Après timeout, le thread passe à la config moins avancée");
            System.out.println("   3. Rotation continue pour faire avancer toutes les configs");
            System.out.println();
            System.out.println("📋 Ordre de priorité:");
            System.out.println("   1. Configurations jamais commencées");
            System.out.println("   2. Sauvegardes avec le moins de temps cumulé");
            System.out.println();

            // Tracker pour les configs terminées (solution trouvée)
            Set<String> solvedConfigs = Collections.synchronizedSet(new HashSet<>());

            // Lancer les threads avec rotation
            long timeoutMs = (long)(timePerConfigMinutes * 60 * 1000);

            System.out.println("✓ Démarrage de " + numThreads + " thread(s) avec rotation automatique");
            System.out.println();
            System.out.println("═".repeat(70));
            System.out.println();

            // Lancer les threads initiaux
            for (int threadId = 1; threadId <= numThreads; threadId++) {
                final int tid = threadId;
                executor.submit(() -> {
                    try {
                        runWorkerWithRotation(tid, timeoutMs, executor, solvedConfigs);
                    } catch (Exception e) {
                        System.err.println("✗ [Thread " + tid + "] Erreur fatale: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }

            System.out.println("⏳ Les threads travaillent avec rotation automatique... (Ctrl+C pour arrêter)");
            System.out.println();

            // Attendre indéfiniment (les threads tournent en rotation)
            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            System.err.println("✗ Erreur fatale: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
