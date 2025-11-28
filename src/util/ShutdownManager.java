package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Graceful shutdown manager for the application.
 *
 * Registers a JVM hook to intercept SIGINT (Ctrl+C) and SIGTERM,
 * allowing state saving before shutdown and displaying statistics.
 */
public class ShutdownManager {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownManager.class);
    private static final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private static final List<ShutdownHook> hooks = new ArrayList<>();
    private static boolean hookRegistered = false;

    /**
     * Interface for custom shutdown hooks
     */
    public interface ShutdownHook {
        /**
         * Executed during application shutdown
         * @param signal the signal received (SIGINT, SIGTERM, etc.)
         */
        void onShutdown(String signal);

        /**
         * Hook name (for logging)
         */
        String getName();
    }

    /**
     * Registers the JVM shutdown hook if not already done
     */
    public static synchronized void registerShutdownHook() {
        if (hookRegistered) {
            logger.debug("Shutdown hook already registered");
            return;
        }

        Thread shutdownThread = new Thread(() -> {
            try {
                logger.info("═══════════════════════════════════════════════════════");
                logger.info("Graceful shutdown in progress...");
                logger.info("═══════════════════════════════════════════════════════");

                shutdownRequested.set(true);

                // Execute all registered hooks
                for (ShutdownHook hook : hooks) {
                    try {
                        logger.info("Executing hook: {}", hook.getName());
                        hook.onShutdown("JVM_SHUTDOWN");
                    } catch (Exception e) {
                        logger.error("Error in hook {}: {}", hook.getName(), e.getMessage(), e);
                    }
                }

                logger.info("Graceful shutdown completed");
                logger.info("═══════════════════════════════════════════════════════");

            } catch (Exception e) {
                logger.error("Error during graceful shutdown", e);
            }
        }, "ShutdownHook-Thread");

        Runtime.getRuntime().addShutdownHook(shutdownThread);
        hookRegistered = true;
        logger.info("JVM shutdown hook registered");
    }

    /**
     * Adds a custom shutdown hook
     * @param hook the hook to add
     */
    public static synchronized void addShutdownHook(ShutdownHook hook) {
        if (hook == null) {
            throw new IllegalArgumentException("Hook cannot be null");
        }
        hooks.add(hook);
        logger.debug("Hook added: {}", hook.getName());
    }

    /**
     * Removes a shutdown hook
     * @param hook the hook to remove
     */
    public static synchronized void removeShutdownHook(ShutdownHook hook) {
        hooks.remove(hook);
        logger.debug("Hook removed: {}", hook.getName());
    }

    /**
     * Checks if a shutdown has been requested
     * @return true if shutdown has been requested
     */
    public static boolean isShutdownRequested() {
        return shutdownRequested.get();
    }

    /**
     * Requests a graceful shutdown (from code)
     */
    public static void requestShutdown() {
        logger.info("Graceful shutdown requested from code");
        shutdownRequested.set(true);
    }

    /**
     * Resets state (useful for tests)
     */
    static synchronized void reset() {
        shutdownRequested.set(false);
        hooks.clear();
        hookRegistered = false;
    }

    /**
     * Displays a formatted shutdown message
     */
    public static void printShutdownMessage(String message) {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("⚠️  INTERRUPTION DETECTED");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(message);
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println();
    }

    /**
     * Displays final statistics during shutdown
     */
    public static void printFinalStatistics(Object stats) {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("FINAL STATISTICS");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(stats.toString());
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println();
    }

    /**
     * Standard hook for saving solver state
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
                printShutdownMessage("Saving state in progress...");
                saveAction.run();
                System.out.println("✓ State saved successfully");
            }
        }

        @Override
        public String getName() {
            return "SolverShutdown: " + description;
        }
    }

    /**
     * Standard hook for displaying statistics
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
     * Standard hook for cleaning up resources
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
                logger.info("Cleanup: {}", description);
                cleanupAction.run();
            }
        }

        @Override
        public String getName() {
            return "Cleanup: " + description;
        }
    }
}
