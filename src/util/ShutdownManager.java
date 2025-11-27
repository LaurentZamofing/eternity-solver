package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gestionnaire d'arrêt gracieux de l'application.
 *
 * Enregistre un hook JVM pour intercepter SIGINT (Ctrl+C) et SIGTERM,
 * permettant de sauvegarder l'état avant l'arrêt et d'afficher les statistiques.
 */
public class ShutdownManager {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownManager.class);
    private static final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private static final List<ShutdownHook> hooks = new ArrayList<>();
    private static boolean hookRegistered = false;

    /**
     * Interface pour les hooks d'arrêt personnalisés
     */
    public interface ShutdownHook {
        /**
         * Exécuté lors de l'arrêt de l'application
         * @param signal le signal reçu (SIGINT, SIGTERM, etc.)
         */
        void onShutdown(String signal);

        /**
         * Nom du hook (pour logging)
         */
        String getName();
    }

    /**
     * Enregistre le hook d'arrêt JVM si ce n'est pas déjà fait
     */
    public static synchronized void registerShutdownHook() {
        if (hookRegistered) {
            logger.debug("Shutdown hook déjà enregistré");
            return;
        }

        Thread shutdownThread = new Thread(() -> {
            try {
                logger.info("═══════════════════════════════════════════════════════");
                logger.info("Arrêt gracieux en cours...");
                logger.info("═══════════════════════════════════════════════════════");

                shutdownRequested.set(true);

                // Exécuter tous les hooks enregistrés
                for (ShutdownHook hook : hooks) {
                    try {
                        logger.info("Exécution hook: {}", hook.getName());
                        hook.onShutdown("JVM_SHUTDOWN");
                    } catch (Exception e) {
                        logger.error("Erreur dans hook {}: {}", hook.getName(), e.getMessage(), e);
                    }
                }

                logger.info("Arrêt gracieux terminé");
                logger.info("═══════════════════════════════════════════════════════");

            } catch (Exception e) {
                logger.error("Erreur lors de l'arrêt gracieux", e);
            }
        }, "ShutdownHook-Thread");

        Runtime.getRuntime().addShutdownHook(shutdownThread);
        hookRegistered = true;
        logger.info("Shutdown hook JVM enregistré");
    }

    /**
     * Ajoute un hook d'arrêt personnalisé
     * @param hook le hook à ajouter
     */
    public static synchronized void addShutdownHook(ShutdownHook hook) {
        if (hook == null) {
            throw new IllegalArgumentException("Hook ne peut pas être null");
        }
        hooks.add(hook);
        logger.debug("Hook ajouté: {}", hook.getName());
    }

    /**
     * Supprime un hook d'arrêt
     * @param hook le hook à supprimer
     */
    public static synchronized void removeShutdownHook(ShutdownHook hook) {
        hooks.remove(hook);
        logger.debug("Hook supprimé: {}", hook.getName());
    }

    /**
     * Vérifie si un arrêt a été demandé
     * @return true si l'arrêt a été demandé
     */
    public static boolean isShutdownRequested() {
        return shutdownRequested.get();
    }

    /**
     * Demande un arrêt gracieux (depuis le code)
     */
    public static void requestShutdown() {
        logger.info("Arrêt gracieux demandé depuis le code");
        shutdownRequested.set(true);
    }

    /**
     * Réinitialise l'état (utile pour les tests)
     */
    static synchronized void reset() {
        shutdownRequested.set(false);
        hooks.clear();
        hookRegistered = false;
    }

    /**
     * Affiche un message d'arrêt formaté
     */
    public static void printShutdownMessage(String message) {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("⚠️  INTERRUPTION DÉTECTÉE");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(message);
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println();
    }

    /**
     * Affiche les statistiques finales lors de l'arrêt
     */
    public static void printFinalStatistics(Object stats) {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("STATISTIQUES FINALES");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(stats.toString());
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println();
    }

    /**
     * Hook standard pour sauvegarder l'état du solveur
     */
    public static class SolverShutdownHook implements ShutdownHook {
        private final Runnable saveAction;
        private final String description;

        public SolverShutdownHook(Runnable saveAction, String description) {
            this.saveAction = saveAction;
            this.description = description;
        }

        @Override
        public void onShutdown(String signal) {
            if (saveAction != null) {
                printShutdownMessage("Sauvegarde de l'état en cours...");
                saveAction.run();
                System.out.println("✓ État sauvegardé avec succès");
            }
        }

        @Override
        public String getName() {
            return "SolverShutdown: " + description;
        }
    }

    /**
     * Hook standard pour afficher les statistiques
     */
    public static class StatisticsShutdownHook implements ShutdownHook {
        private final Object statistics;

        public StatisticsShutdownHook(Object statistics) {
            this.statistics = statistics;
        }

        @Override
        public void onShutdown(String signal) {
            if (statistics != null) {
                printFinalStatistics(statistics);
            }
        }

        @Override
        public String getName() {
            return "StatisticsDisplay";
        }
    }

    /**
     * Hook standard pour nettoyer les ressources
     */
    public static class CleanupShutdownHook implements ShutdownHook {
        private final Runnable cleanupAction;
        private final String description;

        public CleanupShutdownHook(Runnable cleanupAction, String description) {
            this.cleanupAction = cleanupAction;
            this.description = description;
        }

        @Override
        public void onShutdown(String signal) {
            if (cleanupAction != null) {
                logger.info("Nettoyage: {}", description);
                cleanupAction.run();
            }
        }

        @Override
        public String getName() {
            return "Cleanup: " + description;
        }
    }
}
