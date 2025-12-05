package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized wrapper for Eternity solver logging.
 *
 * <p>This class provides a simplified interface for logging,
 * facilitating progressive migration from System.out.println to SLF4J.</p>
 *
 * <h2>Log levels:</h2>
 * <ul>
 *   <li><b>ERROR</b>: Critical errors requiring attention</li>
 *   <li><b>WARN</b>: Warnings, abnormal situations</li>
 *   <li><b>INFO</b>: Important information about execution</li>
 *   <li><b>DEBUG</b>: Detailed debugging information</li>
 *   <li><b>TRACE</b>: Very detailed information (performance)</li>
 * </ul>
 *
 * <h2>Configuration:</h2>
 * <p>The log level is configured via logback.xml in src/main/resources/</p>
 *
 * <h2>Migration from System.out:</h2>
 * <pre>
 * // Before:
 * System.out.println("Solution found!");
 * System.err.println("Error: " + message);
 *
 * // After:
 * SolverLogger.info("Solution found!");
 * SolverLogger.error("Error: {}", message);
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public final class SolverLogger {

    // Default logger for the solver
    private static final Logger logger = LoggerFactory.getLogger("EternitySolver");

    // Specialized loggers by component
    private static final Logger solverLogger = LoggerFactory.getLogger("solver");
    private static final Logger searchLogger = LoggerFactory.getLogger("search");
    private static final Logger statsLogger = LoggerFactory.getLogger("stats");
    private static final Logger saveLogger = LoggerFactory.getLogger("save");
    private static final Logger utilLogger = LoggerFactory.getLogger("util");

    // Prevent instantiation
    private SolverLogger() {
        throw new AssertionError("Cannot instantiate logger class");
    }

    // ═══════════════════════════════════════════════════════════════
    // LOGGING GÉNÉRAL (DEFAULT LOGGER)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Logs an INFO message.
     *
     * @param message The message to log
     */
    public static void info(String message) {
        logger.info(message);
    }

    /**
     * Logs an INFO message with parameters.
     *
     * @param format Message format (with {} for parameters)
     * @param args Arguments to insert in the message
     */
    public static void info(String format, Object... args) {
        logger.info(format, args);
    }

    /**
     * Logs a DEBUG message.
     *
     * @param message The message to log
     */
    public static void debug(String message) {
        logger.debug(message);
    }

    /**
     * Logs a DEBUG message with parameters.
     *
     * @param format Message format
     * @param args Arguments
     */
    public static void debug(String format, Object... args) {
        logger.debug(format, args);
    }

    /**
     * Logs a WARN message.
     *
     * @param message The message to log
     */
    public static void warn(String message) {
        logger.warn(message);
    }

    /**
     * Logs a WARN message with parameters.
     *
     * @param format Message format
     * @param args Arguments
     */
    public static void warn(String format, Object... args) {
        logger.warn(format, args);
    }

    /**
     * Logs an ERROR message.
     *
     * @param message The message to log
     */
    public static void error(String message) {
        logger.error(message);
    }

    /**
     * Logs an ERROR message with exception.
     *
     * @param message The message
     * @param throwable The exception
     */
    public static void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    /**
     * Logs an ERROR message with parameters.
     *
     * @param format Message format
     * @param args Arguments
     */
    public static void error(String format, Object... args) {
        logger.error(format, args);
    }

    /**
     * Logs a TRACE message (very detailed).
     *
     * @param message The message to log
     */
    public static void trace(String message) {
        logger.trace(message);
    }

    /**
     * Logs a TRACE message with parameters.
     *
     * @param format Message format
     * @param args Arguments
     */
    public static void trace(String format, Object... args) {
        logger.trace(format, args);
    }

    // ═══════════════════════════════════════════════════════════════
    // LOGGING SPÉCIALISÉ PAR COMPOSANT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Logs a message about the search algorithm.
     *
     * @param message The message
     */
    public static void search(String message) {
        searchLogger.info(message);
    }

    /**
     * Logs a message about the search algorithm with parameters.
     *
     * @param format Message format
     * @param args Arguments
     */
    public static void search(String format, Object... args) {
        searchLogger.info(format, args);
    }

    /**
     * Logs statistics.
     *
     * @param message The message
     */
    public static void stats(String message) {
        statsLogger.info(message);
    }

    /**
     * Logs statistics with parameters.
     *
     * @param format Message format
     * @param args Arguments
     */
    public static void stats(String format, Object... args) {
        statsLogger.info(format, args);
    }

    /**
     * Logs a save/load operation.
     *
     * @param message The message
     */
    public static void save(String message) {
        saveLogger.info(message);
    }

    /**
     * Logs a save/load operation with parameters.
     *
     * @param format Message format
     * @param args Arguments
     */
    public static void save(String format, Object... args) {
        saveLogger.info(format, args);
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Checks if DEBUG level is enabled.
     *
     * <p>Useful to avoid expensive calculations if the message won't be logged.</p>
     *
     * @return true if DEBUG is enabled
     */
    public static boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /**
     * Checks if TRACE level is enabled.
     *
     * @return true if TRACE is enabled
     */
    public static boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    /**
     * Checks if INFO level is enabled.
     *
     * @return true if INFO is enabled
     */
    public static boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /**
     * Gets the underlying SLF4J logger for advanced usage.
     *
     * @return The SLF4J logger
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Gets a logger for a specific component.
     *
     * @param name Component name
     * @return Logger for this component
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
}
