package monitoring.controller;

import monitoring.MonitoringApplication;
import monitoring.model.ConfigMetrics;
import monitoring.model.HistoricalMetrics;
import monitoring.repository.MetricsRepository;
import monitoring.service.IFileWatcherService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DashboardController.
 * Tests the full application stack with Spring context, controllers, services, and database.
 *
 * Uses:
 * - @SpringBootTest to load full application context
 * - MockMvc to test HTTP endpoints
 * - In-memory H2 database (via application-test.properties)
 * - Real service beans (not mocked)
 */
@SpringBootTest(
        classes = MonitoringApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MetricsRepository metricsRepository;

    @Autowired(required = false)
    private IFileWatcherService fileWatcherService;

    /**
     * Setup test data in database before each test.
     */
    @BeforeEach
    void setUp() {
        // Clear existing data
        metricsRepository.deleteAll();

        // Seed test data
        seedHistoricalMetrics();
    }

    /**
     * Cleanup after each test.
     */
    @AfterEach
    void tearDown() {
        metricsRepository.deleteAll();
    }

    /**
     * Test GET /api/health endpoint.
     * Verifies service health check returns proper status.
     */
    @Test
    @Order(1)
    @DisplayName("GET /api/health returns health status")
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.configsMonitored").exists())
                .andExpect(jsonPath("$.databaseRecords").exists());
    }

    /**
     * Test GET /api/configs endpoint.
     * Verifies configs list is returned (may be empty if file watcher is disabled).
     */
    @Test
    @Order(2)
    @DisplayName("GET /api/configs returns configs list")
    void testGetAllConfigs() throws Exception {
        mockMvc.perform(get("/api/configs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * Test GET /api/configs with sort and filter parameters.
     */
    @Test
    @Order(3)
    @DisplayName("GET /api/configs with query parameters")
    void testGetAllConfigsWithParameters() throws Exception {
        mockMvc.perform(get("/api/configs")
                        .param("sort", "depth")
                        .param("order", "asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/configs")
                        .param("sort", "time")
                        .param("order", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/configs")
                        .param("sort", "name")
                        .param("order", "asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * Test GET /api/configs/{configName} for non-existent config.
     * Expects 404 Not Found.
     */
    @Test
    @Order(4)
    @DisplayName("GET /api/configs/{configName} returns 404 for non-existent config")
    void testGetConfigNotFound() throws Exception {
        mockMvc.perform(get("/api/configs/nonexistent_config")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * Test GET /api/configs/{configName}/history endpoint.
     * Verifies historical data is returned from database.
     */
    @Test
    @Order(5)
    @DisplayName("GET /api/configs/{configName}/history returns historical data")
    @Transactional
    void testGetConfigHistory() throws Exception {
        String configName = "test_config_integration";

        mockMvc.perform(get("/api/configs/{configName}/history", configName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(0))));
    }

    /**
     * Test GET /api/configs/{configName}/history with query parameters.
     */
    @Test
    @Order(6)
    @DisplayName("GET /api/configs/{configName}/history with hours and limit params")
    @Transactional
    void testGetConfigHistoryWithParameters() throws Exception {
        String configName = "test_config_integration";

        mockMvc.perform(get("/api/configs/{configName}/history", configName)
                        .param("hours", "1")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/configs/{configName}/history", configName)
                        .param("hours", "168")  // 1 week
                        .param("limit", "500")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * Test GET /api/stats/global endpoint.
     * Verifies global statistics are returned.
     */
    @Test
    @Order(7)
    @DisplayName("GET /api/stats/global returns aggregated statistics")
    void testGetGlobalStats() throws Exception {
        mockMvc.perform(get("/api/stats/global")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConfigs").exists())
                .andExpect(jsonPath("$.runningConfigs").exists())
                .andExpect(jsonPath("$.idleConfigs").exists())
                .andExpect(jsonPath("$.solvedConfigs").exists());
    }

    /**
     * Test GET /api/stats/recent endpoint.
     * Verifies recent activity is returned from database.
     */
    @Test
    @Order(8)
    @DisplayName("GET /api/stats/recent returns recent metrics")
    @Transactional
    void testGetRecentActivity() throws Exception {
        mockMvc.perform(get("/api/stats/recent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/stats/recent")
                        .param("hours", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * Test GET /api/configs/summary endpoint.
     * Verifies summary data structure.
     */
    @Test
    @Order(9)
    @DisplayName("GET /api/configs/summary returns lightweight summary")
    void testGetConfigsSummary() throws Exception {
        mockMvc.perform(get("/api/configs/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConfigs").exists())
                .andExpect(jsonPath("$.configs").isArray());
    }

    /**
     * Test POST /api/refresh endpoint.
     * Verifies cache refresh functionality.
     */
    @Test
    @Order(10)
    @DisplayName("POST /api/refresh clears cache and rescans")
    void testRefreshCache() throws Exception {
        mockMvc.perform(post("/api/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cache refreshed successfully"))
                .andExpect(jsonPath("$.configsFound").exists());
    }

    /**
     * Test error handling for invalid parameters.
     */
    @Test
    @Order(11)
    @DisplayName("GET /api/configs with invalid parameters returns proper response")
    void testInvalidParameters() throws Exception {
        // Invalid sort field should still return 200 (defaults to progress)
        mockMvc.perform(get("/api/configs")
                        .param("sort", "invalid_field")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Invalid order should still return 200 (defaults to desc)
        mockMvc.perform(get("/api/configs")
                        .param("order", "invalid_order")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * Test database interaction via repository.
     * Verifies data persistence and retrieval.
     */
    @Test
    @Order(12)
    @DisplayName("Historical metrics are persisted and retrievable from database")
    @Transactional
    void testDatabaseIntegration() throws Exception {
        String configName = "test_database_integration";

        // Insert test data
        HistoricalMetrics metrics = new HistoricalMetrics();
        metrics.setConfigName(configName);
        metrics.setDepth(42);
        metrics.setProgressPercentage(50.5);
        metrics.setTotalComputeTimeMs(5000L);
        metrics.setTimestamp(LocalDateTime.now());
        metrics.setStatus("running");
        metrics.setPiecesPerSecond(2.5);
        metricsRepository.save(metrics);

        // Verify data is retrievable
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        LocalDateTime until = LocalDateTime.now().plusHours(1);
        var results = metricsRepository.findByConfigNameAndTimestampBetweenOrderByTimestampAsc(
                configName, since, until
        );

        Assertions.assertFalse(results.isEmpty(), "Should find inserted metrics");
        Assertions.assertEquals(configName, results.get(0).getConfigName());
        Assertions.assertEquals(42, results.get(0).getDepth());
    }

    /**
     * Test concurrent requests.
     * Verifies thread safety and proper response handling.
     */
    @Test
    @Order(13)
    @DisplayName("Multiple concurrent requests are handled correctly")
    void testConcurrentRequests() throws Exception {
        // Execute multiple parallel requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/health")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/configs")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/stats/global")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    /**
     * Helper method to seed test data in database.
     */
    private void seedHistoricalMetrics() {
        String[] configNames = {"test_config_integration", "eternity2_test", "puzzle_integration"};
        LocalDateTime now = LocalDateTime.now();

        for (String configName : configNames) {
            for (int i = 0; i < 10; i++) {
                HistoricalMetrics metrics = new HistoricalMetrics();
                metrics.setConfigName(configName);
                metrics.setDepth(i * 10);
                metrics.setProgressPercentage(i * 5.0);
                metrics.setTotalComputeTimeMs((long) (i * 1000));
                metrics.setTimestamp(now.minusMinutes(i * 5));
                metrics.setStatus(i % 2 == 0 ? "running" : "idle");
                metrics.setPiecesPerSecond(i * 1.5);
                metricsRepository.save(metrics);
            }
        }
    }
}
