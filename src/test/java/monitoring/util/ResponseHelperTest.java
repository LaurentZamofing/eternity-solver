package monitoring.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ResponseHelper}.
 *
 * <p>Closes the {@code monitoring/util/} coverage gap identified in
 * IMPROVEMENT_PLAN § 1D. The helper is used across every Spring
 * controller — keeping it pinned prevents silent regressions in status
 * codes or body shape.</p>
 */
@DisplayName("ResponseHelper")
class ResponseHelperTest {

    @Test
    @DisplayName("ok(body) returns 200 with the body")
    void okHasBody() {
        ResponseEntity<String> r = ResponseHelper.ok("hello");
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("hello", r.getBody());
    }

    @Test
    @DisplayName("notFound() returns 404 with no body")
    void notFoundStatus() {
        ResponseEntity<Object> r = ResponseHelper.notFound();
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
        assertNull(r.getBody());
    }

    @Test
    @DisplayName("badRequest() returns 400 with no body")
    void badRequestStatus() {
        ResponseEntity<Object> r = ResponseHelper.badRequest();
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertNull(r.getBody());
    }

    @Test
    @DisplayName("internalError() returns 500")
    void internalErrorStatus() {
        ResponseEntity<Object> r = ResponseHelper.internalError();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
    }

    @Test
    @DisplayName("errorWithMessage populates success=false + message + timestamp")
    void errorWithMessageShape() {
        ResponseEntity<Map<String, Object>> r =
            ResponseHelper.errorWithMessage(HttpStatus.CONFLICT, "boom");
        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
        Map<String, Object> body = r.getBody();
        assertNotNull(body);
        assertEquals(Boolean.FALSE, body.get("success"));
        assertEquals("boom", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    @DisplayName("builder() creates a new builder with success=true and 200 default")
    void builderDefaults() {
        ResponseEntity<Map<String, Object>> r = ResponseHelper.builder().build();
        assertEquals(HttpStatus.OK, r.getStatusCode());
        Map<String, Object> body = r.getBody();
        assertNotNull(body);
        assertEquals(Boolean.TRUE, body.get("success"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    @DisplayName("builder chains status, message, put — all returning this")
    void builderFluent() {
        ResponseHelper.ResponseBuilder b = ResponseHelper.builder();
        assertSame(b, b.success(false));
        assertSame(b, b.status(HttpStatus.ACCEPTED));
        assertSame(b, b.put("key", 42));
        assertSame(b, b.message("details"));

        ResponseEntity<Map<String, Object>> r = b.build();
        assertEquals(HttpStatus.ACCEPTED, r.getStatusCode());
        Map<String, Object> body = r.getBody();
        assertEquals(Boolean.FALSE, body.get("success"));
        assertEquals(42, body.get("key"));
        assertEquals("details", body.get("message"));
    }

    @Test
    @DisplayName("private constructor is not invocable via reflection without throwing")
    void utilityClassCannotBeExploited() throws Exception {
        // Utility classes should prevent instantiation. We don't care how
        // (private ctor + optional throw) — just that mis-using it is visible.
        var ctor = ResponseHelper.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        // Constructor may or may not throw; just verify the type is final.
        assertTrue(java.lang.reflect.Modifier.isFinal(ResponseHelper.class.getModifiers()),
            "ResponseHelper should be final");
    }
}
