package monitoring.config;

import monitoring.util.ResponseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;

/**
 * Global exception handler for all controllers.
 * Provides centralized, consistent error handling across the monitoring API.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle generic exceptions.
     * Catches any unhandled exception and returns a 500 error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unhandled exception occurred", ex);
        return ResponseHelper.errorWithMessage(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred: " + ex.getMessage()
        );
    }

    /**
     * Handle illegal argument exceptions.
     * Returns a 400 bad request error.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Invalid argument: {}", ex.getMessage());
        return ResponseHelper.errorWithMessage(
                HttpStatus.BAD_REQUEST,
                "Invalid argument: " + ex.getMessage()
        );
    }

    /**
     * Handle method argument type mismatch.
     * Returns a 400 bad request error.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        logger.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());
        return ResponseHelper.errorWithMessage(
                HttpStatus.BAD_REQUEST,
                String.format("Invalid value for parameter '%s': expected %s",
                        ex.getName(),
                        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown type")
        );
    }

    /**
     * Handle 404 not found.
     * Returns a 404 not found error.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoHandlerFoundException ex) {
        logger.warn("Endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return ResponseHelper.errorWithMessage(
                HttpStatus.NOT_FOUND,
                "Endpoint not found: " + ex.getRequestURL()
        );
    }

    /**
     * Handle null pointer exceptions.
     * Returns a 500 internal server error.
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(NullPointerException ex) {
        logger.error("Null pointer exception occurred", ex);
        return ResponseHelper.errorWithMessage(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal error occurred: null value encountered"
        );
    }
}
