package monitoring.controller;

import monitoring.model.ConfigMetrics;
import monitoring.model.HistoricalMetrics;
import monitoring.repository.MetricsRepository;
import monitoring.service.IFileWatcherService;
import monitoring.service.IMetricsAggregator;
import monitoring.service.MetricsAggregator.GlobalStats;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for DashboardController.
 * Tests all REST API endpoints with various scenarios.
 *
 * NOTE: Now uses IFileWatcherService interface for mockability
 */
public class DashboardControllerTest {

    @Mock
    private IFileWatcherService fileWatcherService;

    @Mock
    private MetricsRepository repository;

    @Mock
    private IMetricsAggregator aggregator;

    @InjectMocks
    private DashboardController controller;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    /**
     * Test GET /api/configs with default parameters.
     */
    @Test
    @DisplayName("GET /api/configs returns all configs sorted by progress desc")
    void testGetAllConfigsDefault() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        ConfigMetrics config1 = createConfig("config1", 50, 75.0);
        ConfigMetrics config2 = createConfig("config2", 30, 45.0);
        ConfigMetrics config3 = createConfig("config3", 100, 90.0);
        metricsMap.put("config1", config1);
        metricsMap.put("config2", config2);
        metricsMap.put("config3", config3);

        List<ConfigMetrics> sorted = Arrays.asList(config3, config1, config2);

        when(fileWatcherService.getAllMetrics()).thenReturn(metricsMap);
        when(aggregator.sortByProgress(anyCollection(), eq(true))).thenReturn(sorted);

        ResponseEntity<List<ConfigMetrics>> response = controller.getAllConfigs("progress", "desc", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
        assertEquals(90.0, response.getBody().get(0).getProgressPercentage(), 0.01);

        verify(fileWatcherService).getAllMetrics();
        verify(aggregator).sortByProgress(anyCollection(), eq(true));
    }

    /**
     * Test GET /api/configs with sort by depth.
     */
    @Test
    @DisplayName("GET /api/configs sorted by depth")
    void testGetAllConfigsSortByDepth() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        ConfigMetrics config1 = createConfig("config1", 50, 75.0);
        ConfigMetrics config2 = createConfig("config2", 30, 45.0);
        metricsMap.put("config1", config1);
        metricsMap.put("config2", config2);

        List<ConfigMetrics> sorted = Arrays.asList(config1, config2);

        when(fileWatcherService.getAllMetrics()).thenReturn(metricsMap);
        when(aggregator.sortByDepth(anyCollection(), eq(false))).thenReturn(sorted);

        ResponseEntity<List<ConfigMetrics>> response = controller.getAllConfigs("depth", "asc", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(aggregator).sortByDepth(anyCollection(), eq(false));
    }

    /**
     * Test GET /api/configs with sort by time.
     */
    @Test
    @DisplayName("GET /api/configs sorted by compute time")
    void testGetAllConfigsSortByTime() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        ConfigMetrics config1 = createConfig("config1", 50, 75.0);
        config1.setTotalComputeTimeMs(5000);
        metricsMap.put("config1", config1);

        List<ConfigMetrics> sorted = Arrays.asList(config1);

        when(fileWatcherService.getAllMetrics()).thenReturn(metricsMap);
        when(aggregator.sortByComputeTime(anyCollection(), eq(true))).thenReturn(sorted);

        ResponseEntity<List<ConfigMetrics>> response = controller.getAllConfigs("time", "desc", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(aggregator).sortByComputeTime(anyCollection(), eq(true));
    }

    /**
     * Test GET /api/configs with sort by name.
     */
    @Test
    @DisplayName("GET /api/configs sorted by name")
    void testGetAllConfigsSortByName() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        ConfigMetrics config1 = createConfig("config_a", 50, 75.0);
        ConfigMetrics config2 = createConfig("config_b", 30, 45.0);
        ConfigMetrics config3 = createConfig("config_c", 100, 90.0);
        metricsMap.put("config_a", config1);
        metricsMap.put("config_b", config2);
        metricsMap.put("config_c", config3);

        when(fileWatcherService.getAllMetrics()).thenReturn(metricsMap);

        // Ascending
        ResponseEntity<List<ConfigMetrics>> response = controller.getAllConfigs("name", "asc", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        List<ConfigMetrics> body = response.getBody();
        assertEquals("config_a", body.get(0).getConfigName());
        assertEquals("config_b", body.get(1).getConfigName());
        assertEquals("config_c", body.get(2).getConfigName());

        // Descending
        ResponseEntity<List<ConfigMetrics>> responseDesc = controller.getAllConfigs("name", "desc", null);
        List<ConfigMetrics> bodyDesc = responseDesc.getBody();
        assertNotNull(bodyDesc);
        assertEquals("config_c", bodyDesc.get(0).getConfigName());
        assertEquals("config_b", bodyDesc.get(1).getConfigName());
        assertEquals("config_a", bodyDesc.get(2).getConfigName());
    }

    /**
     * Test GET /api/configs with status filter.
     */
    @Test
    @DisplayName("GET /api/configs filtered by status")
    void testGetAllConfigsWithStatusFilter() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        ConfigMetrics config1 = createConfig("config1", 50, 75.0);
        config1.setStatus("running");
        ConfigMetrics config2 = createConfig("config2", 30, 45.0);
        config2.setStatus("idle");
        metricsMap.put("config1", config1);
        metricsMap.put("config2", config2);

        List<ConfigMetrics> filtered = Arrays.asList(config1);
        List<ConfigMetrics> sorted = Arrays.asList(config1);

        when(fileWatcherService.getAllMetrics()).thenReturn(metricsMap);
        when(aggregator.filterByStatus(anyCollection(), eq("running"))).thenReturn(filtered);
        when(aggregator.sortByProgress(anyCollection(), eq(true))).thenReturn(sorted);

        ResponseEntity<List<ConfigMetrics>> response = controller.getAllConfigs("progress", "desc", "running");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("running", response.getBody().get(0).getStatus());

        verify(aggregator).filterByStatus(anyCollection(), eq("running"));
    }

    /**
     * Test GET /api/configs with empty metrics.
     */
    @Test
    @DisplayName("GET /api/configs returns empty list when no metrics")
    void testGetAllConfigsEmpty() {
        when(fileWatcherService.getAllMetrics()).thenReturn(new HashMap<>());
        when(aggregator.sortByProgress(anyCollection(), eq(true))).thenReturn(new ArrayList<>());

        ResponseEntity<List<ConfigMetrics>> response = controller.getAllConfigs("progress", "desc", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    /**
     * Test GET /api/configs/{configName} success.
     */
    @Test
    @DisplayName("GET /api/configs/{configName} returns config when found")
    void testGetConfigSuccess() {
        ConfigMetrics config = createConfig("test_config", 50, 75.0);

        when(fileWatcherService.getMetrics("test_config")).thenReturn(config);

        ResponseEntity<ConfigMetrics> response = controller.getConfig("test_config");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test_config", response.getBody().getConfigName());
        assertEquals(50, response.getBody().getDepth());

        verify(fileWatcherService).getMetrics("test_config");
    }

    /**
     * Test GET /api/configs/{configName} not found.
     */
    @Test
    @DisplayName("GET /api/configs/{configName} returns 404 when not found")
    void testGetConfigNotFound() {
        when(fileWatcherService.getMetrics("nonexistent")).thenReturn(null);

        ResponseEntity<ConfigMetrics> response = controller.getConfig("nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        verify(fileWatcherService).getMetrics("nonexistent");
    }

    /**
     * Test GET /api/configs/{configName}/history with default parameters.
     */
    @Test
    @DisplayName("GET /api/configs/{configName}/history returns history")
    void testGetConfigHistory() {
        List<HistoricalMetrics> history = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            HistoricalMetrics hm = new HistoricalMetrics();
            hm.setConfigName("test_config");
            hm.setDepth(i * 10);
            hm.setProgressPercentage(i * 5.0);
            hm.setTimestamp(LocalDateTime.now().minusHours(i));
            history.add(hm);
        }

        when(repository.findByConfigNameAndTimestampBetweenOrderByTimestampAsc(
            eq("test_config"), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(history);

        ResponseEntity<List<HistoricalMetrics>> response =
            controller.getConfigHistory("test_config", 24, 1000);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().size());

        verify(repository).findByConfigNameAndTimestampBetweenOrderByTimestampAsc(
            eq("test_config"), any(LocalDateTime.class), any(LocalDateTime.class)
        );
    }

    /**
     * Test GET /api/configs/{configName}/history with sampling.
     */
    @Test
    @DisplayName("GET /api/configs/{configName}/history samples when exceeding limit")
    void testGetConfigHistoryWithSampling() {
        List<HistoricalMetrics> history = new ArrayList<>();
        // Create 1500 data points
        for (int i = 0; i < 1500; i++) {
            HistoricalMetrics hm = new HistoricalMetrics();
            hm.setConfigName("test_config");
            hm.setDepth(i);
            hm.setTimestamp(LocalDateTime.now().minusMinutes(i));
            history.add(hm);
        }

        when(repository.findByConfigNameAndTimestampBetweenOrderByTimestampAsc(
            eq("test_config"), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(history);

        ResponseEntity<List<HistoricalMetrics>> response =
            controller.getConfigHistory("test_config", 24, 500);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() <= 500, "Should sample to limit");

        verify(repository).findByConfigNameAndTimestampBetweenOrderByTimestampAsc(
            eq("test_config"), any(LocalDateTime.class), any(LocalDateTime.class)
        );
    }

    /**
     * Test GET /api/stats/global.
     */
    @Test
    @DisplayName("GET /api/stats/global returns aggregated stats")
    void testGetGlobalStats() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        metricsMap.put("config1", createConfig("config1", 50, 75.0));
        metricsMap.put("config2", createConfig("config2", 30, 45.0));

        GlobalStats stats = new GlobalStats();
        stats.setTotalConfigs(2);
        stats.setRunningConfigs(1);
        stats.setSolvedConfigs(0);

        when(fileWatcherService.getAllMetrics()).thenReturn(metricsMap);
        when(aggregator.aggregateMetrics(metricsMap)).thenReturn(stats);

        ResponseEntity<GlobalStats> response = controller.getGlobalStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getTotalConfigs());
        assertEquals(1, response.getBody().getRunningConfigs());

        verify(fileWatcherService).getAllMetrics();
        verify(aggregator).aggregateMetrics(metricsMap);
    }

    /**
     * Test GET /api/stats/recent.
     */
    @Test
    @DisplayName("GET /api/stats/recent returns recent activity")
    void testGetRecentActivity() {
        List<HistoricalMetrics> recentMetrics = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            HistoricalMetrics hm = new HistoricalMetrics();
            hm.setConfigName("config" + i);
            hm.setDepth(i * 10);
            hm.setTimestamp(LocalDateTime.now().minusMinutes(i * 5));
            recentMetrics.add(hm);
        }

        when(repository.findRecentMetrics(any(LocalDateTime.class))).thenReturn(recentMetrics);

        ResponseEntity<List<HistoricalMetrics>> response = controller.getRecentActivity(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().size());

        verify(repository).findRecentMetrics(any(LocalDateTime.class));
    }

    /**
     * Test GET /api/health.
     */
    @Test
    @DisplayName("GET /api/health returns health status")
    void testHealthCheck() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        metricsMap.put("config1", createConfig("config1", 50, 75.0));
        metricsMap.put("config2", createConfig("config2", 30, 45.0));

        when(fileWatcherService.getAllMetrics()).thenReturn(metricsMap);
        when(repository.count()).thenReturn(100L);

        ResponseEntity<Map<String, Object>> response = controller.healthCheck();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> health = response.getBody();
        assertEquals("UP", health.get("status"));
        assertEquals(2, health.get("configsMonitored"));
        assertEquals(100L, health.get("databaseRecords"));
        assertNotNull(health.get("timestamp"));

        verify(fileWatcherService).getAllMetrics();
        verify(repository).count();
    }

    /**
     * Test GET /api/configs/summary.
     */
    @Test
    @DisplayName("GET /api/configs/summary returns lightweight summary")
    void testGetConfigsSummary() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        ConfigMetrics config1 = createConfig("config1", 50, 75.0);
        config1.setStatus("running");
        ConfigMetrics config2 = createConfig("config2", 30, 45.0);
        config2.setStatus("idle");
        metricsMap.put("config1", config1);
        metricsMap.put("config2", config2);

        when(fileWatcherService.getAllMetrics()).thenReturn(metricsMap);

        ResponseEntity<Map<String, Object>> response = controller.getConfigsSummary();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> summary = response.getBody();
        assertEquals(2, summary.get("totalConfigs"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> configs = (List<Map<String, Object>>) summary.get("configs");
        assertNotNull(configs);
        assertEquals(2, configs.size());

        // Verify summary structure
        Map<String, Object> firstConfig = configs.get(0);
        assertTrue(firstConfig.containsKey("name"));
        assertTrue(firstConfig.containsKey("depth"));
        assertTrue(firstConfig.containsKey("progress"));
        assertTrue(firstConfig.containsKey("status"));

        verify(fileWatcherService).getAllMetrics();
    }

    // Helper method to create test ConfigMetrics
    private ConfigMetrics createConfig(String name, int depth, double progress) {
        ConfigMetrics metrics = new ConfigMetrics(name);
        metrics.setDepth(depth);
        metrics.setProgressPercentage(progress);
        metrics.setRows(10);
        metrics.setCols(10);
        metrics.setTotalPieces(100);
        metrics.setTotalComputeTimeMs(1000);
        metrics.setLastUpdate(LocalDateTime.now());
        return metrics;
    }
}
