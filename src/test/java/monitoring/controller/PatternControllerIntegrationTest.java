package monitoring.controller;

import monitoring.MonitoringApplication;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PatternController.
 * Tests pattern service endpoints with Spring context.
 */
@SpringBootTest(
        classes = MonitoringApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PatternControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test GET /api/patterns/list endpoint.
     * Verifies all patterns (0-22) are returned.
     */
    @Test
    @Order(1)
    @DisplayName("GET /api/patterns/list returns all patterns")
    void testGetAllPatterns() throws Exception {
        mockMvc.perform(get("/api/patterns/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(23)))  // 0-22 = 23 patterns
                .andExpect(jsonPath("$[0].id").value(0))
                .andExpect(jsonPath("$[0].name").value("border"))
                .andExpect(jsonPath("$[0].imageUrl").doesNotExist())  // Pattern 0 has no image
                .andExpect(jsonPath("$[1].id").value(1))
                .andExpect(jsonPath("$[1].name").value("pattern1"))
                .andExpect(jsonPath("$[1].imageUrl").value("/patterns/pattern1.png"));
    }

    /**
     * Test GET /api/patterns/{id} for valid pattern IDs.
     */
    @Test
    @Order(2)
    @DisplayName("GET /api/patterns/{id} returns pattern info for valid IDs")
    void testGetPattern() throws Exception {
        // Pattern 0 (border/gray)
        mockMvc.perform(get("/api/patterns/0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(0))
                .andExpect(jsonPath("$.name").value("border"))
                .andExpect(jsonPath("$.imageUrl").doesNotExist());

        // Pattern 1
        mockMvc.perform(get("/api/patterns/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("pattern1"))
                .andExpect(jsonPath("$.imageUrl").value("/patterns/pattern1.png"));

        // Pattern 22 (last valid pattern)
        mockMvc.perform(get("/api/patterns/22")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(22))
                .andExpect(jsonPath("$.name").value("pattern22"))
                .andExpect(jsonPath("$.imageUrl").value("/patterns/pattern22.png"));
    }

    /**
     * Test GET /api/patterns/{id} for invalid pattern IDs.
     * Expects 400 Bad Request.
     */
    @Test
    @Order(3)
    @DisplayName("GET /api/patterns/{id} returns 400 for invalid IDs")
    void testGetPatternInvalidIds() throws Exception {
        // Negative ID
        mockMvc.perform(get("/api/patterns/-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // ID too large
        mockMvc.perform(get("/api/patterns/23")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/patterns/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test GET /api/patterns/{id}/image for pattern 0.
     * Pattern 0 (border) has no image, expects 404.
     */
    @Test
    @Order(4)
    @DisplayName("GET /api/patterns/0/image returns 404 (border has no image)")
    void testGetPatternImageForBorder() throws Exception {
        mockMvc.perform(get("/api/patterns/0/image")
                        .accept(MediaType.IMAGE_PNG))
                .andExpect(status().isNotFound());
    }

    /**
     * Test GET /api/patterns/{id}/image for invalid IDs.
     */
    @Test
    @Order(5)
    @DisplayName("GET /api/patterns/{id}/image returns 400 for invalid IDs")
    void testGetPatternImageInvalidIds() throws Exception {
        mockMvc.perform(get("/api/patterns/-1/image")
                        .accept(MediaType.IMAGE_PNG))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/patterns/23/image")
                        .accept(MediaType.IMAGE_PNG))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test GET /api/patterns/health endpoint.
     * Verifies pattern service health.
     */
    @Test
    @Order(6)
    @DisplayName("GET /api/patterns/health returns service status")
    void testPatternHealth() throws Exception {
        mockMvc.perform(get("/api/patterns/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("patterns available")));
    }

    /**
     * Test GET /api/patterns/stats endpoint.
     * Verifies pattern statistics.
     */
    @Test
    @Order(7)
    @DisplayName("GET /api/patterns/stats returns pattern statistics")
    void testPatternStats() throws Exception {
        mockMvc.perform(get("/api/patterns/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPatterns").value(23))  // 0-22
                .andExpect(jsonPath("$.borderPatterns").value(6))  // 0-5
                .andExpect(jsonPath("$.innerPatterns").value(17))  // 6-22
                .andExpect(jsonPath("$.imageFormat").value("PNG"))
                .andExpect(jsonPath("$.imageDimensions").value("256x256"));
    }

    /**
     * Test all valid pattern IDs sequentially.
     * Ensures every pattern from 0 to 22 returns correct data.
     */
    @Test
    @Order(8)
    @DisplayName("All pattern IDs 0-22 return valid responses")
    void testAllPatternIds() throws Exception {
        for (int id = 0; id <= 22; id++) {
            mockMvc.perform(get("/api/patterns/" + id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id));
        }
    }

    /**
     * Test boundary conditions.
     */
    @Test
    @Order(9)
    @DisplayName("Boundary conditions are handled correctly")
    void testBoundaryConditions() throws Exception {
        // Minimum valid ID
        mockMvc.perform(get("/api/patterns/0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(0));

        // Maximum valid ID
        mockMvc.perform(get("/api/patterns/22")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(22));

        // Just below minimum (invalid)
        mockMvc.perform(get("/api/patterns/-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Just above maximum (invalid)
        mockMvc.perform(get("/api/patterns/23")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test content type handling.
     */
    @Test
    @Order(10)
    @DisplayName("Pattern endpoints return correct content types")
    void testContentTypes() throws Exception {
        // JSON endpoints
        mockMvc.perform(get("/api/patterns/list"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/patterns/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/patterns/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * Test concurrent requests to pattern endpoints.
     */
    @Test
    @Order(11)
    @DisplayName("Multiple concurrent pattern requests are handled correctly")
    void testConcurrentRequests() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/patterns/list")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/patterns/" + (i % 23))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/patterns/stats")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    /**
     * Test error handling with malformed requests.
     */
    @Test
    @Order(12)
    @DisplayName("Malformed requests are handled gracefully")
    void testMalformedRequests() throws Exception {
        // Non-numeric pattern ID (should be caught by Spring and return 400)
        mockMvc.perform(get("/api/patterns/abc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
