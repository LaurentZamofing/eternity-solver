package monitoring.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for creating standardized HTTP responses.
 * Reduces duplication across controllers and ensures consistent response formats.
 */
public final class ResponseHelper {

    private ResponseHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Create a successful response with body.
     *
     * @param body Response body
     * @param <T> Body type
     * @return ResponseEntity with 200 OK status
     */
    public static <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok(body);
    }

    /**
     * Create a not found response.
     *
     * @param <T> Body type
     * @return ResponseEntity with 404 NOT_FOUND status
     */
    public static <T> ResponseEntity<T> notFound() {
        return ResponseEntity.notFound().build();
    }

    /**
     * Create a bad request response.
     *
     * @param <T> Body type
     * @return ResponseEntity with 400 BAD_REQUEST status
     */
    public static <T> ResponseEntity<T> badRequest() {
        return ResponseEntity.badRequest().build();
    }

    /**
     * Create an internal server error response.
     *
     * @param <T> Body type
     * @return ResponseEntity with 500 INTERNAL_SERVER_ERROR status
     */
    public static <T> ResponseEntity<T> internalError() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Create a service unavailable response.
     *
     * @param <T> Body type
     * @return ResponseEntity with 503 SERVICE_UNAVAILABLE status
     */
    public static <T> ResponseEntity<T> serviceUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    /**
     * Create a successful response with a message map.
     * Useful for simple success messages.
     *
     * @param message Success message
     * @return ResponseEntity with 200 OK and message body
     */
    public static ResponseEntity<Map<String, Object>> okWithMessage(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Create an error response with a message map.
     * Useful for error messages with consistent format.
     *
     * @param status HTTP status
     * @param message Error message
     * @return ResponseEntity with specified status and error body
     */
    public static ResponseEntity<Map<String, Object>> errorWithMessage(HttpStatus status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Create a success response with custom data.
     *
     * @param data Data map to include in response
     * @return ResponseEntity with 200 OK and data body
     */
    public static ResponseEntity<Map<String, Object>> okWithData(Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("timestamp", LocalDateTime.now().toString());
        response.putAll(data);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a builder for complex responses.
     *
     * @return ResponseBuilder instance
     */
    public static ResponseBuilder builder() {
        return new ResponseBuilder();
    }

    /**
     * Builder for creating complex responses with multiple fields.
     */
    public static class ResponseBuilder {
        private final Map<String, Object> data = new HashMap<>();
        private boolean success = true;
        private HttpStatus status = HttpStatus.OK;

        public ResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public ResponseBuilder status(HttpStatus status) {
            this.status = status;
            return this;
        }

        public ResponseBuilder put(String key, Object value) {
            data.put(key, value);
            return this;
        }

        public ResponseBuilder message(String message) {
            data.put("message", message);
            return this;
        }

        public ResponseEntity<Map<String, Object>> build() {
            data.put("success", success);
            data.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.status(status).body(data);
        }
    }
}
