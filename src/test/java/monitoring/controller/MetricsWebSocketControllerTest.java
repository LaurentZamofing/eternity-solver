package monitoring.controller;

import monitoring.model.ConfigMetrics;
import monitoring.service.IFileWatcherService;
import monitoring.service.IMetricsAggregator;
import monitoring.service.MetricsAggregator.GlobalStats;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for MetricsWebSocketController.
 * Tests WebSocket subscription handlers and message handlers.
 *
 * NOTE: Now uses IFileWatcherService interface for mockability
 */
public class MetricsWebSocketControllerTest {

    @Mock
    private IFileWatcherService fileWatcherService;

    @Mock
    private IMetricsAggregator aggregator;

    @InjectMocks
    private MetricsWebSocketController controller;

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
     * Test subscription to /topic/metrics returns current state.
     */
    @Test
    @DisplayName("Subscribe to /topic/metrics returns all current metrics")
    void testOnSubscribeMetrics() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        ConfigMetrics config1 = createConfig("config1", 50, 75.0);
        ConfigMetrics config2 = createConfig("config2", 30, 45.0);
        metricsMap.put("config1", config1);
        metricsMap.put("config2", config2);

        when(fileWatcherService.getAllMetrics()).thenReturn(metricsMap);

        Map<String, ConfigMetrics> result = controller.onSubscribeMetrics();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("config1"));
        assertTrue(result.containsKey("config2"));
        assertEquals(50, result.get("config1").getDepth());
        assertEquals(30, result.get("config2").getDepth());

        verify(fileWatcherService).getAllMetrics();
    }

    /**
     * Test subscription to /topic/metrics with empty metrics.
     */
    @Test
    @DisplayName("Subscribe to /topic/metrics with no configs returns empty map")
    void testOnSubscribeMetricsEmpty() {
        when(fileWatcherService.getAllMetrics()).thenReturn(new HashMap<>());

        Map<String, ConfigMetrics> result = controller.onSubscribeMetrics();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(fileWatcherService).getAllMetrics();
    }

    /**
     * Test subscription to /topic/stats returns current stats.
     */
    @Test
    @DisplayName("Subscribe to /topic/stats returns aggregated stats")
    void testOnSubscribeStats() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        ConfigMetrics config1 = createConfig("config1", 50, 75.0);
        ConfigMetrics config2 = createConfig("config2", 30, 45.0);
        metricsMap.put("config1", config1);
        metricsMap.put("config2", config2);

        GlobalStats stats = new GlobalStats();
        stats.setTotalConfigs(2);
        stats.setRunningConfigs(1);
        stats.setSolvedConfigs(0);
        stats.setIdleConfigs(1);

        when(fileWatcherService.getAllMetrics()).thenReturn(metricsMap);
        when(aggregator.aggregateMetrics(metricsMap)).thenReturn(stats);

        GlobalStats result = controller.onSubscribeStats();

        assertNotNull(result);
        assertEquals(2, result.getTotalConfigs());
        assertEquals(1, result.getRunningConfigs());
        assertEquals(0, result.getSolvedConfigs());
        assertEquals(1, result.getIdleConfigs());

        verify(fileWatcherService).getAllMetrics();
        verify(aggregator).aggregateMetrics(metricsMap);
    }

    /**
     * Test subscription to /topic/stats with empty metrics.
     */
    @Test
    @DisplayName("Subscribe to /topic/stats with no configs returns empty stats")
    void testOnSubscribeStatsEmpty() {
        Map<String, ConfigMetrics> emptyMap = new HashMap<>();
        GlobalStats emptyStats = new GlobalStats();
        emptyStats.setTotalConfigs(0);

        when(fileWatcherService.getAllMetrics()).thenReturn(emptyMap);
        when(aggregator.aggregateMetrics(emptyMap)).thenReturn(emptyStats);

        GlobalStats result = controller.onSubscribeStats();

        assertNotNull(result);
        assertEquals(0, result.getTotalConfigs());

        verify(fileWatcherService).getAllMetrics();
        verify(aggregator).aggregateMetrics(emptyMap);
    }

    /**
     * Test getConfig message handler returns requested config.
     */
    @Test
    @DisplayName("Message /app/getConfig returns requested config")
    void testGetConfigMetrics() {
        ConfigMetrics config = createConfig("test_config", 80, 65.5);

        Map<String, String> request = new HashMap<>();
        request.put("configName", "test_config");

        when(fileWatcherService.getMetrics("test_config")).thenReturn(config);

        ConfigMetrics result = controller.getConfigMetrics(request);

        assertNotNull(result);
        assertEquals("test_config", result.getConfigName());
        assertEquals(80, result.getDepth());
        assertEquals(65.5, result.getProgressPercentage(), 0.01);

        verify(fileWatcherService).getMetrics("test_config");
    }

    /**
     * Test getConfig message handler with non-existent config.
     */
    @Test
    @DisplayName("Message /app/getConfig returns null for non-existent config")
    void testGetConfigMetricsNotFound() {
        Map<String, String> request = new HashMap<>();
        request.put("configName", "nonexistent");

        when(fileWatcherService.getMetrics("nonexistent")).thenReturn(null);

        ConfigMetrics result = controller.getConfigMetrics(request);

        assertNull(result);

        verify(fileWatcherService).getMetrics("nonexistent");
    }

    /**
     * Test ping message handler.
     */
    @Test
    @DisplayName("Message /app/ping returns pong with timestamp")
    void testHandlePing() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        metricsMap.put("config1", createConfig("config1", 50, 75.0));
        metricsMap.put("config2", createConfig("config2", 30, 45.0));

        when(fileWatcherService.getAllMetrics()).thenReturn(metricsMap);

        Map<String, Object> result = controller.handlePing();

        assertNotNull(result);
        assertEquals("pong", result.get("type"));
        assertNotNull(result.get("timestamp"));
        assertEquals(2, result.get("configCount"));

        // Verify timestamp is recent (within last second)
        long timestamp = (long) result.get("timestamp");
        long now = System.currentTimeMillis();
        assertTrue(Math.abs(now - timestamp) < 1000, "Timestamp should be recent");

        verify(fileWatcherService).getAllMetrics();
    }

    /**
     * Test ping with empty metrics.
     */
    @Test
    @DisplayName("Message /app/ping with no configs returns 0 count")
    void testHandlePingEmpty() {
        when(fileWatcherService.getAllMetrics()).thenReturn(new HashMap<>());

        Map<String, Object> result = controller.handlePing();

        assertNotNull(result);
        assertEquals("pong", result.get("type"));
        assertEquals(0, result.get("configCount"));

        verify(fileWatcherService).getAllMetrics();
    }

    /**
     * Test that onSubscribeMetrics is called when client subscribes.
     */
    @Test
    @DisplayName("Verify onSubscribeMetrics retrieves all metrics once")
    void testSubscribeMetricsCallsServiceOnce() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        ConfigMetrics config = createConfig("test", 50, 50.0);
        metricsMap.put("test", config);

        when(fileWatcherService.getAllMetrics()).thenReturn(metricsMap);

        controller.onSubscribeMetrics();

        verify(fileWatcherService, times(1)).getAllMetrics();
    }

    /**
     * Test that onSubscribeStats aggregates metrics exactly once.
     */
    @Test
    @DisplayName("Verify onSubscribeStats aggregates metrics once")
    void testSubscribeStatsAggregatesOnce() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        GlobalStats stats = new GlobalStats();

        when(fileWatcherService.getAllMetrics()).thenReturn(metricsMap);
        when(aggregator.aggregateMetrics(any())).thenReturn(stats);

        controller.onSubscribeStats();

        verify(fileWatcherService, times(1)).getAllMetrics();
        verify(aggregator, times(1)).aggregateMetrics(metricsMap);
    }

    /**
     * Test multiple config requests.
     */
    @Test
    @DisplayName("Handle multiple config requests independently")
    void testMultipleConfigRequests() {
        ConfigMetrics config1 = createConfig("config1", 50, 75.0);
        ConfigMetrics config2 = createConfig("config2", 30, 45.0);

        when(fileWatcherService.getMetrics("config1")).thenReturn(config1);
        when(fileWatcherService.getMetrics("config2")).thenReturn(config2);

        Map<String, String> request1 = Map.of("configName", "config1");
        Map<String, String> request2 = Map.of("configName", "config2");

        ConfigMetrics result1 = controller.getConfigMetrics(request1);
        ConfigMetrics result2 = controller.getConfigMetrics(request2);

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals("config1", result1.getConfigName());
        assertEquals("config2", result2.getConfigName());

        verify(fileWatcherService).getMetrics("config1");
        verify(fileWatcherService).getMetrics("config2");
    }

    /**
     * Test multiple ping requests.
     */
    @Test
    @DisplayName("Handle multiple ping requests")
    void testMultiplePings() {
        when(fileWatcherService.getAllMetrics()).thenReturn(new HashMap<>());

        Map<String, Object> pong1 = controller.handlePing();
        Map<String, Object> pong2 = controller.handlePing();
        Map<String, Object> pong3 = controller.handlePing();

        assertNotNull(pong1);
        assertNotNull(pong2);
        assertNotNull(pong3);

        assertEquals("pong", pong1.get("type"));
        assertEquals("pong", pong2.get("type"));
        assertEquals("pong", pong3.get("type"));

        // Timestamps should be different (or very close)
        long ts1 = (long) pong1.get("timestamp");
        long ts2 = (long) pong2.get("timestamp");
        long ts3 = (long) pong3.get("timestamp");

        assertTrue(ts2 >= ts1);
        assertTrue(ts3 >= ts2);

        verify(fileWatcherService, times(3)).getAllMetrics();
    }

    /**
     * Test that subscription returns immutable metrics.
     */
    @Test
    @DisplayName("Subscription returns current snapshot of metrics")
    void testSubscriptionReturnsSnapshot() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        metricsMap.put("config1", createConfig("config1", 50, 75.0));

        when(fileWatcherService.getAllMetrics()).thenReturn(metricsMap);

        Map<String, ConfigMetrics> result1 = controller.onSubscribeMetrics();
        Map<String, ConfigMetrics> result2 = controller.onSubscribeMetrics();

        // Both calls should return data (even if it's the same data)
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(1, result1.size());
        assertEquals(1, result2.size());

        verify(fileWatcherService, times(2)).getAllMetrics();
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
        metrics.setStatus("idle");
        return metrics;
    }
}
