package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper centralisé pour le logging du solveur Eternity.
 *
 * <p>Cette classe fournit une interface simplifiée pour le logging,
 * facilitant la migration progressive depuis System.out.println vers SLF4J.</p>
 *
 * <h2>Niveaux de log:</h2>
 * <ul>
 *   <li><b>ERROR</b>: Erreurs critiques nécessitant attention</li>
 *   <li><b>WARN</b>: Avertissements, situations anormales</li>
 *   <li><b>INFO</b>: Informations importantes sur le déroulement</li>
 *   <li><b>DEBUG</b>: Informations de débogage détaillées</li>
 *   <li><b>TRACE</b>: Informations très détaillées (performance)</li>
 * </ul>
 *
 * <h2>Configuration:</h2>
 * <p>Le niveau de log est configuré via logback.xml dans src/main/resources/</p>
 *
 * <h2>Migration depuis System.out:</h2>
 * <pre>
 * // Avant:
 * System.out.println("Solution trouvée!");
 * System.err.println("Erreur: " + message);
 *
 * // Après:
 * SolverLogger.info("Solution trouvée!");
 * SolverLogger.error("Erreur: {}", message);
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public final class SolverLogger {

    // Logger par défaut pour le solver
    private static final Logger logger = LoggerFactory.getLogger("EternitySolver");

    // Loggers spécialisés par composant
    private static final Logger solverLogger = LoggerFactory.getLogger("solver");
    private static final Logger searchLogger = LoggerFactory.getLogger("search");
    private static final Logger statsLogger = LoggerFactory.getLogger("stats");
    private static final Logger saveLogger = LoggerFactory.getLogger("save");
    private static final Logger utilLogger = LoggerFactory.getLogger("util");

    // Empêche l'instanciation
    private SolverLogger() {
        throw new AssertionError("Cannot instantiate logger class");
    }

    // ═══════════════════════════════════════════════════════════════
    // LOGGING GÉNÉRAL (DEFAULT LOGGER)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Log un message INFO.
     *
     * @param message Le message à logger
     */
    public static void info(String message) {
        logger.info(message);
    }

    /**
     * Log un message INFO avec paramètres.
     *
     * @param format Format du message (avec {} pour les paramètres)
     * @param args Arguments à insérer dans le message
     */
    public static void info(String format, Object... args) {
        logger.info(format, args);
    }

    /**
     * Log un message DEBUG.
     *
     * @param message Le message à logger
     */
    public static void debug(String message) {
        logger.debug(message);
    }

    /**
     * Log un message DEBUG avec paramètres.
     *
     * @param format Format du message
     * @param args Arguments
     */
    public static void debug(String format, Object... args) {
        logger.debug(format, args);
    }

    /**
     * Log un message WARN.
     *
     * @param message Le message à logger
     */
    public static void warn(String message) {
        logger.warn(message);
    }

    /**
     * Log un message WARN avec paramètres.
     *
     * @param format Format du message
     * @param args Arguments
     */
    public static void warn(String format, Object... args) {
        logger.warn(format, args);
    }

    /**
     * Log un message ERROR.
     *
     * @param message Le message à logger
     */
    public static void error(String message) {
        logger.error(message);
    }

    /**
     * Log un message ERROR avec exception.
     *
     * @param message Le message
     * @param throwable L'exception
     */
    public static void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    /**
     * Log un message ERROR avec paramètres.
     *
     * @param format Format du message
     * @param args Arguments
     */
    public static void error(String format, Object... args) {
        logger.error(format, args);
    }

    /**
     * Log un message TRACE (très détaillé).
     *
     * @param message Le message à logger
     */
    public static void trace(String message) {
        logger.trace(message);
    }

    /**
     * Log un message TRACE avec paramètres.
     *
     * @param format Format du message
     * @param args Arguments
     */
    public static void trace(String format, Object... args) {
        logger.trace(format, args);
    }

    // ═══════════════════════════════════════════════════════════════
    // LOGGING SPÉCIALISÉ PAR COMPOSANT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Log un message concernant l'algorithme de recherche.
     *
     * @param message Le message
     */
    public static void search(String message) {
        searchLogger.info(message);
    }

    /**
     * Log un message concernant l'algorithme de recherche avec paramètres.
     *
     * @param format Format du message
     * @param args Arguments
     */
    public static void search(String format, Object... args) {
        searchLogger.info(format, args);
    }

    /**
     * Log des statistiques.
     *
     * @param message Le message
     */
    public static void stats(String message) {
        statsLogger.info(message);
    }

    /**
     * Log des statistiques avec paramètres.
     *
     * @param format Format du message
     * @param args Arguments
     */
    public static void stats(String format, Object... args) {
        statsLogger.info(format, args);
    }

    /**
     * Log une opération de sauvegarde/chargement.
     *
     * @param message Le message
     */
    public static void save(String message) {
        saveLogger.info(message);
    }

    /**
     * Log une opération de sauvegarde/chargement avec paramètres.
     *
     * @param format Format du message
     * @param args Arguments
     */
    public static void save(String format, Object... args) {
        saveLogger.info(format, args);
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS POUR LA MIGRATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Affiche un message directement sur la console (pour compatibilité).
     *
     * <p><b>Attention:</b> Cette méthode est fournie uniquement pour faciliter
     * la migration progressive. Elle devrait être remplacée par les méthodes
     * de logging appropriées (info, debug, etc.).</p>
     *
     * @param message Le message à afficher
     * @deprecated Utiliser {@link #info(String)} ou {@link #debug(String)} à la place
     */
    @Deprecated
    public static void print(String message) {
        System.out.println(message);
        logger.debug("[CONSOLE] {}", message);
    }

    /**
     * Affiche un message d'erreur directement sur la console (pour compatibilité).
     *
     * <p><b>Attention:</b> Cette méthode est fournie uniquement pour faciliter
     * la migration progressive. Elle devrait être remplacée par {@link #error(String)}.</p>
     *
     * @param message Le message d'erreur
     * @deprecated Utiliser {@link #error(String)} à la place
     */
    @Deprecated
    public static void printError(String message) {
        System.err.println(message);
        logger.error("[CONSOLE] {}", message);
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Vérifie si le niveau DEBUG est activé.
     *
     * <p>Utile pour éviter des calculs coûteux si le message ne sera pas logé.</p>
     *
     * @return true si DEBUG est activé
     */
    public static boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /**
     * Vérifie si le niveau TRACE est activé.
     *
     * @return true si TRACE est activé
     */
    public static boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    /**
     * Vérifie si le niveau INFO est activé.
     *
     * @return true si INFO est activé
     */
    public static boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /**
     * Obtient le logger SLF4J sous-jacent pour usage avancé.
     *
     * @return Le logger SLF4J
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Obtient un logger pour un composant spécifique.
     *
     * @param name Nom du composant
     * @return Logger pour ce composant
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
}
