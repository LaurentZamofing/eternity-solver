package util;

import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Centralized exception handling utility providing consistent patterns
 * for error handling, logging, and recovery across the application.
 *
 * This class provides static methods for common exception handling scenarios:
 * - IO operations with fallback values
 * - Operations that should never fail
 * - Validation with descriptive errors
 * - Resource cleanup with error suppression
 *
 * Example usage:
 * <pre>
 * // IO operation with default value
 * Board board = ExceptionHandler.handleIO(
 *     () -> saveManager.load(path),
 *     null,
 *     logger,
 *     "loading board from {}", path
 * );
 *
 * // Operation that should succeed
 * ExceptionHandler.requireSuccess(
 *     () -> Files.createDirectories(path),
 *     logger,
 *     "creating directory {}", path
 * );
 * </pre>
 */
public final class ExceptionHandler {

    private ExceptionHandler() {
        // Utility class
    }

    /**
     * Handle IO operations that may fail, returning a default value on error.
     * Logs the error and returns the fallback value if an exception occurs.
     *
     * @param operation the operation to execute
     * @param defaultValue the value to return if operation fails
     * @param logger the logger to use for error logging
     * @param messageTemplate the log message template (SLF4J format)
     * @param messageArgs arguments for the log message
     * @param <T> the return type
     * @return the operation result, or defaultValue if operation fails
     */
    public static <T> T handleIO(
            Supplier<T> operation,
            T defaultValue,
            Logger logger,
            String messageTemplate,
            Object... messageArgs) {
        try {
            return operation.get();
        } catch (Exception e) {
            logger.error("IO error " + messageTemplate + ": {}",
                appendArgs(messageArgs, e.getMessage()));
            logger.debug("Exception details", e);
            return defaultValue;
        }
    }

    /**
     * Execute an operation that should never fail in production.
     * If it fails, logs an error and throws RuntimeException.
     *
     * Use this for operations that indicate serious system problems if they fail
     * (e.g., creating required directories, parsing configuration).
     *
     * @param operation the operation to execute
     * @param logger the logger to use for error logging
     * @param messageTemplate the log message template (SLF4J format)
     * @param messageArgs arguments for the log message
     * @throws RuntimeException if the operation fails
     */
    public static void requireSuccess(
            RunnableWithException operation,
            Logger logger,
            String messageTemplate,
            Object... messageArgs) {
        try {
            operation.run();
        } catch (Exception e) {
            String message = String.format(messageTemplate.replace("{}", "%s"), messageArgs);
            logger.error("Critical operation failed: " + message, e);
            throw new RuntimeException("Critical operation failed: " + message, e);
        }
    }

    /**
     * Validate a condition and throw IllegalArgumentException with context if false.
     *
     * @param condition the condition to check
     * @param messageTemplate the error message template
     * @param messageArgs arguments for the error message
     * @throws IllegalArgumentException if condition is false
     */
    public static void validate(boolean condition, String messageTemplate, Object... messageArgs) {
        if (!condition) {
            String message = String.format(messageTemplate.replace("{}", "%s"), messageArgs);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate that a value is not null and throw IllegalArgumentException if it is.
     *
     * @param value the value to check
     * @param name the name of the value (for error message)
     * @param <T> the value type
     * @return the value if not null
     * @throws IllegalArgumentException if value is null
     */
    public static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        return value;
    }

    /**
     * Execute cleanup operation, suppressing any exceptions.
     * Logs exceptions at debug level but doesn't throw.
     *
     * Use this in finally blocks or shutdown hooks where you want to
     * ensure cleanup runs but don't want to mask the original exception.
     *
     * @param cleanup the cleanup operation to execute
     * @param logger the logger to use for error logging
     * @param description description of what is being cleaned up
     */
    public static void safeCleanup(
            RunnableWithException cleanup,
            Logger logger,
            String description) {
        try {
            cleanup.run();
        } catch (Exception e) {
            logger.debug("Error during cleanup of {}: {}", description, e.getMessage());
        }
    }

    /**
     * Wrap a checked exception into a RuntimeException with context.
     *
     * @param e the checked exception
     * @param messageTemplate the context message template
     * @param messageArgs arguments for the message
     * @return a RuntimeException wrapping the original exception
     */
    public static RuntimeException wrapChecked(
            Exception e,
            String messageTemplate,
            Object... messageArgs) {
        String message = String.format(messageTemplate.replace("{}", "%s"), messageArgs);
        return new RuntimeException(message, e);
    }

    /**
     * Helper to append exception message to message args array.
     */
    private static Object[] appendArgs(Object[] args, Object lastArg) {
        Object[] result = new Object[args.length + 1];
        System.arraycopy(args, 0, result, 0, args.length);
        result[args.length] = lastArg;
        return result;
    }

    /**
     * Functional interface for operations that may throw checked exceptions.
     */
    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }
}
