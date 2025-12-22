package monitoring.service;

import monitoring.model.ConfigMetrics;
import monitoring.service.MetricsAggregator.GlobalStats;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for MetricsAggregator.
 * Tests aggregation, statistics calculation, sorting, and filtering.
 */
public class MetricsAggregatorTest {

    private MetricsAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new MetricsAggregator();
    }

    /**
     * Test aggregating empty metrics map.
     */
    @Test
    @DisplayName("Aggregate empty metrics map")
    void testAggregateEmptyMap() {
        GlobalStats stats = aggregator.aggregateMetrics(new HashMap<>());

        assertNotNull(stats);
        assertEquals(0, stats.getTotalConfigs());
        assertEquals(0, stats.getRunningConfigs());
        assertEquals(0, stats.getSolvedConfigs());
        assertEquals(0, stats.getIdleConfigs());
        assertEquals(0, stats.getStuckConfigs());
        assertEquals(0, stats.getTotalComputeTimeMs());
    }

    /**
     * Test aggregating null metrics map.
     */
    @Test
    @DisplayName("Aggregate null metrics map")
    void testAggregateNullMap() {
        GlobalStats stats = aggregator.aggregateMetrics(null);

        assertNotNull(stats);
        assertEquals(0, stats.getTotalConfigs());
    }

    /**
     * Test basic metrics aggregation.
     */
    @Test
    @DisplayName("Aggregate basic metrics correctly")
    void testBasicAggregation() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();

        // Config 1: Running
        ConfigMetrics config1 = createConfig("config1", 50, 25.0, 10000, true, false, "running");
        metricsMap.put("config1", config1);

        // Config 2: Idle
        ConfigMetrics config2 = createConfig("config2", 30, 15.0, 5000, false, false, "idle");
        metricsMap.put("config2", config2);

        // Config 3: Solved
        ConfigMetrics config3 = createConfig("config3", 100, 100.0, 20000, false, true, "solved");
        metricsMap.put("config3", config3);

        GlobalStats stats = aggregator.aggregateMetrics(metricsMap);

        assertNotNull(stats);
        assertEquals(3, stats.getTotalConfigs());
        assertEquals(1, stats.getRunningConfigs());
        assertEquals(1, stats.getSolvedConfigs());
        assertEquals(1, stats.getIdleConfigs());
        assertEquals(0, stats.getStuckConfigs());
        assertEquals(35000, stats.getTotalComputeTimeMs()); // 10000 + 5000 + 20000
    }

    /**
     * Test best progress tracking.
     */
    @Test
    @DisplayName("Track best progress correctly")
    void testBestProgress() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        metricsMap.put("config1", createConfig("config1", 50, 25.5, 10000, false, false, "idle"));
        metricsMap.put("config2", createConfig("config2", 30, 75.8, 5000, false, false, "idle"));
        metricsMap.put("config3", createConfig("config3", 100, 42.3, 20000, false, false, "idle"));

        GlobalStats stats = aggregator.aggregateMetrics(metricsMap);

        assertEquals(75.8, stats.getBestProgressPercentage(), 0.01);
        assertEquals("config2", stats.getBestProgressConfigName());
    }

    /**
     * Test average progress calculation.
     */
    @Test
    @DisplayName("Calculate average progress correctly")
    void testAverageProgress() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        metricsMap.put("config1", createConfig("config1", 50, 10.0, 1000, false, false, "idle"));
        metricsMap.put("config2", createConfig("config2", 30, 20.0, 1000, false, false, "idle"));
        metricsMap.put("config3", createConfig("config3", 100, 30.0, 1000, false, false, "idle"));

        GlobalStats stats = aggregator.aggregateMetrics(metricsMap);

        // Average: (10 + 20 + 30) / 3 = 20
        assertEquals(20.0, stats.getAverageProgressPercentage(), 0.01);
    }

    /**
     * Test max depth tracking.
     */
    @Test
    @DisplayName("Track max depth correctly")
    void testMaxDepth() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        metricsMap.put("config1", createConfig("config1", 50, 25.0, 1000, false, false, "idle"));
        metricsMap.put("config2", createConfig("config2", 120, 75.0, 1000, false, false, "idle"));
        metricsMap.put("config3", createConfig("config3", 80, 42.0, 1000, false, false, "idle"));

        GlobalStats stats = aggregator.aggregateMetrics(metricsMap);

        assertEquals(120, stats.getMaxDepth());
        assertEquals("config2", stats.getMaxDepthConfigName());
    }

    /**
     * Test fastest pieces per second tracking.
     */
    @Test
    @DisplayName("Track fastest pieces per second correctly")
    void testFastestPiecesPerSecond() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();

        ConfigMetrics config1 = createConfig("config1", 50, 25.0, 1000, false, false, "idle");
        config1.setPiecesPerSecond(5.0);

        ConfigMetrics config2 = createConfig("config2", 100, 50.0, 1000, false, false, "idle");
        config2.setPiecesPerSecond(12.5);

        ConfigMetrics config3 = createConfig("config3", 80, 40.0, 1000, false, false, "idle");
        config3.setPiecesPerSecond(8.3);

        metricsMap.put("config1", config1);
        metricsMap.put("config2", config2);
        metricsMap.put("config3", config3);

        GlobalStats stats = aggregator.aggregateMetrics(metricsMap);

        assertEquals(12.5, stats.getFastestPiecesPerSecond(), 0.01);
        assertEquals("config2", stats.getFastestConfigName());
    }

    /**
     * Test fastest tracking ignores zero values.
     */
    @Test
    @DisplayName("Ignore zero pieces per second in fastest tracking")
    void testFastestIgnoresZero() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();

        ConfigMetrics config1 = createConfig("config1", 50, 25.0, 1000, false, false, "idle");
        config1.setPiecesPerSecond(0.0);

        ConfigMetrics config2 = createConfig("config2", 100, 50.0, 1000, false, false, "idle");
        config2.setPiecesPerSecond(5.0);

        metricsMap.put("config1", config1);
        metricsMap.put("config2", config2);

        GlobalStats stats = aggregator.aggregateMetrics(metricsMap);

        assertEquals(5.0, stats.getFastestPiecesPerSecond(), 0.01);
        assertEquals("config2", stats.getFastestConfigName());
    }

    /**
     * Test top configs list.
     */
    @Test
    @DisplayName("Generate top configs list correctly")
    void testTopConfigs() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();

        // Create 15 configs with different progress values
        for (int i = 0; i < 15; i++) {
            String name = "config" + i;
            double progress = i * 5.0; // 0, 5, 10, 15, ..., 70
            metricsMap.put(name, createConfig(name, 50, progress, 1000, false, false, "idle"));
        }

        GlobalStats stats = aggregator.aggregateMetrics(metricsMap);

        List<ConfigMetrics> topConfigs = stats.getTopConfigs();
        assertNotNull(topConfigs);
        assertEquals(10, topConfigs.size(), "Should have top 10 configs");

        // Should be sorted by progress descending
        assertEquals(70.0, topConfigs.get(0).getProgressPercentage(), 0.01);
        assertEquals(65.0, topConfigs.get(1).getProgressPercentage(), 0.01);
        assertEquals(25.0, topConfigs.get(9).getProgressPercentage(), 0.01);
    }

    /**
     * Test top configs with fewer than 10 total.
     */
    @Test
    @DisplayName("Top configs list with fewer than 10 configs")
    void testTopConfigsLessThan10() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();
        metricsMap.put("config1", createConfig("config1", 50, 25.0, 1000, false, false, "idle"));
        metricsMap.put("config2", createConfig("config2", 30, 15.0, 1000, false, false, "idle"));
        metricsMap.put("config3", createConfig("config3", 100, 75.0, 1000, false, false, "idle"));

        GlobalStats stats = aggregator.aggregateMetrics(metricsMap);

        List<ConfigMetrics> topConfigs = stats.getTopConfigs();
        assertNotNull(topConfigs);
        assertEquals(3, topConfigs.size(), "Should have all 3 configs");

        // Should be sorted by progress descending
        assertEquals(75.0, topConfigs.get(0).getProgressPercentage(), 0.01);
        assertEquals(25.0, topConfigs.get(1).getProgressPercentage(), 0.01);
        assertEquals(15.0, topConfigs.get(2).getProgressPercentage(), 0.01);
    }

    /**
     * Test stuck configs detection (no update in 10+ minutes).
     */
    @Test
    @DisplayName("Detect stuck configs correctly")
    void testStuckConfigsDetection() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();

        // Config 1: Updated 15 minutes ago (stuck)
        ConfigMetrics config1 = createConfig("stuck1", 50, 25.0, 10000, false, false, "idle");
        config1.setLastUpdate(LocalDateTime.now().minusMinutes(15));
        metricsMap.put("stuck1", config1);

        // Config 2: Updated 5 minutes ago (not stuck)
        ConfigMetrics config2 = createConfig("active", 30, 15.0, 5000, false, false, "idle");
        config2.setLastUpdate(LocalDateTime.now().minusMinutes(5));
        metricsMap.put("active", config2);

        // Config 3: Updated 20 minutes ago (stuck)
        ConfigMetrics config3 = createConfig("stuck2", 100, 42.0, 20000, false, false, "idle");
        config3.setLastUpdate(LocalDateTime.now().minusMinutes(20));
        metricsMap.put("stuck2", config3);

        // Config 4: Solved (not stuck even if old)
        ConfigMetrics config4 = createConfig("solved", 100, 100.0, 15000, false, true, "solved");
        config4.setLastUpdate(LocalDateTime.now().minusMinutes(30));
        metricsMap.put("solved", config4);

        GlobalStats stats = aggregator.aggregateMetrics(metricsMap);

        List<ConfigMetrics> stuckConfigs = stats.getStuckConfigsList();
        assertNotNull(stuckConfigs);
        assertEquals(2, stuckConfigs.size(), "Should have 2 stuck configs");

        // Verify stuck configs are the right ones
        Set<String> stuckNames = new HashSet<>();
        for (ConfigMetrics m : stuckConfigs) {
            stuckNames.add(m.getConfigName());
        }
        assertTrue(stuckNames.contains("stuck1"));
        assertTrue(stuckNames.contains("stuck2"));
        assertFalse(stuckNames.contains("active"));
        assertFalse(stuckNames.contains("solved"));
    }

    /**
     * Test stuck count in global stats.
     */
    @Test
    @DisplayName("Count stuck configs in global stats")
    void testStuckCountInStats() {
        Map<String, ConfigMetrics> metricsMap = new HashMap<>();

        // Create configs with explicit "stuck" status
        metricsMap.put("stuck1", createConfig("stuck1", 50, 25.0, 10000, false, false, "stuck"));
        metricsMap.put("stuck2", createConfig("stuck2", 30, 15.0, 5000, false, false, "stuck"));
        metricsMap.put("active", createConfig("active", 100, 42.0, 20000, true, false, "running"));

        GlobalStats stats = aggregator.aggregateMetrics(metricsMap);

        assertEquals(2, stats.getStuckConfigs());
    }

    /**
     * Test sorting by progress ascending.
     */
    @Test
    @DisplayName("Sort by progress ascending")
    void testSortByProgressAscending() {
        List<ConfigMetrics> metrics = Arrays.asList(
            createConfig("config1", 50, 75.0, 1000, false, false, "idle"),
            createConfig("config2", 30, 15.0, 1000, false, false, "idle"),
            createConfig("config3", 100, 42.0, 1000, false, false, "idle")
        );

        List<ConfigMetrics> sorted = aggregator.sortByProgress(metrics, false);

        assertEquals(15.0, sorted.get(0).getProgressPercentage(), 0.01);
        assertEquals(42.0, sorted.get(1).getProgressPercentage(), 0.01);
        assertEquals(75.0, sorted.get(2).getProgressPercentage(), 0.01);
    }

    /**
     * Test sorting by progress descending.
     */
    @Test
    @DisplayName("Sort by progress descending")
    void testSortByProgressDescending() {
        List<ConfigMetrics> metrics = Arrays.asList(
            createConfig("config1", 50, 75.0, 1000, false, false, "idle"),
            createConfig("config2", 30, 15.0, 1000, false, false, "idle"),
            createConfig("config3", 100, 42.0, 1000, false, false, "idle")
        );

        List<ConfigMetrics> sorted = aggregator.sortByProgress(metrics, true);

        assertEquals(75.0, sorted.get(0).getProgressPercentage(), 0.01);
        assertEquals(42.0, sorted.get(1).getProgressPercentage(), 0.01);
        assertEquals(15.0, sorted.get(2).getProgressPercentage(), 0.01);
    }

    /**
     * Test sorting by depth.
     */
    @Test
    @DisplayName("Sort by depth ascending and descending")
    void testSortByDepth() {
        List<ConfigMetrics> metrics = Arrays.asList(
            createConfig("config1", 80, 75.0, 1000, false, false, "idle"),
            createConfig("config2", 30, 15.0, 1000, false, false, "idle"),
            createConfig("config3", 120, 42.0, 1000, false, false, "idle")
        );

        // Ascending
        List<ConfigMetrics> ascending = aggregator.sortByDepth(metrics, false);
        assertEquals(30, ascending.get(0).getDepth());
        assertEquals(80, ascending.get(1).getDepth());
        assertEquals(120, ascending.get(2).getDepth());

        // Descending
        List<ConfigMetrics> descending = aggregator.sortByDepth(metrics, true);
        assertEquals(120, descending.get(0).getDepth());
        assertEquals(80, descending.get(1).getDepth());
        assertEquals(30, descending.get(2).getDepth());
    }

    /**
     * Test sorting by compute time.
     */
    @Test
    @DisplayName("Sort by compute time ascending and descending")
    void testSortByComputeTime() {
        List<ConfigMetrics> metrics = Arrays.asList(
            createConfig("config1", 50, 75.0, 5000, false, false, "idle"),
            createConfig("config2", 30, 15.0, 20000, false, false, "idle"),
            createConfig("config3", 100, 42.0, 10000, false, false, "idle")
        );

        // Ascending
        List<ConfigMetrics> ascending = aggregator.sortByComputeTime(metrics, false);
        assertEquals(5000, ascending.get(0).getTotalComputeTimeMs());
        assertEquals(10000, ascending.get(1).getTotalComputeTimeMs());
        assertEquals(20000, ascending.get(2).getTotalComputeTimeMs());

        // Descending
        List<ConfigMetrics> descending = aggregator.sortByComputeTime(metrics, true);
        assertEquals(20000, descending.get(0).getTotalComputeTimeMs());
        assertEquals(10000, descending.get(1).getTotalComputeTimeMs());
        assertEquals(5000, descending.get(2).getTotalComputeTimeMs());
    }

    /**
     * Test filtering by status.
     */
    @Test
    @DisplayName("Filter by status correctly")
    void testFilterByStatus() {
        List<ConfigMetrics> metrics = Arrays.asList(
            createConfig("config1", 50, 75.0, 1000, true, false, "running"),
            createConfig("config2", 30, 15.0, 1000, false, false, "idle"),
            createConfig("config3", 100, 42.0, 1000, false, true, "solved"),
            createConfig("config4", 80, 60.0, 1000, true, false, "running"),
            createConfig("config5", 40, 20.0, 1000, false, false, "stuck")
        );

        // Filter running
        List<ConfigMetrics> running = aggregator.filterByStatus(metrics, "running");
        assertEquals(2, running.size());
        assertTrue(running.stream().allMatch(m -> m.getStatus().equals("running")));

        // Filter idle
        List<ConfigMetrics> idle = aggregator.filterByStatus(metrics, "idle");
        assertEquals(1, idle.size());
        assertEquals("idle", idle.get(0).getStatus());

        // Filter solved
        List<ConfigMetrics> solved = aggregator.filterByStatus(metrics, "solved");
        assertEquals(1, solved.size());
        assertEquals("solved", solved.get(0).getStatus());

        // Filter stuck
        List<ConfigMetrics> stuck = aggregator.filterByStatus(metrics, "stuck");
        assertEquals(1, stuck.size());
        assertEquals("stuck", stuck.get(0).getStatus());
    }

    /**
     * Test time formatting in GlobalStats.
     */
    @Test
    @DisplayName("Format compute time correctly")
    void testTimeFormatting() {
        GlobalStats stats = new GlobalStats();

        // Test minutes only
        stats.setTotalComputeTimeMs(120000); // 2 minutes
        assertEquals("2m", stats.getTotalComputeTimeFormatted());

        // Test hours and minutes
        stats.setTotalComputeTimeMs(3661000); // 1h 1m 1s
        assertEquals("1h 1m", stats.getTotalComputeTimeFormatted());

        // Test days, hours, minutes
        stats.setTotalComputeTimeMs(90061000); // 1d 1h 1m 1s
        assertEquals("1d 1h 1m", stats.getTotalComputeTimeFormatted());

        // Test large values
        stats.setTotalComputeTimeMs(259200000); // 3 days
        assertEquals("3d 0h 0m", stats.getTotalComputeTimeFormatted());
    }

    // Helper method to create test ConfigMetrics
    private ConfigMetrics createConfig(String name, int depth, double progress,
                                       long computeTime, boolean running,
                                       boolean solved, String status) {
        ConfigMetrics metrics = new ConfigMetrics(name);
        metrics.setDepth(depth);
        metrics.setProgressPercentage(progress);
        metrics.setTotalComputeTimeMs(computeTime);
        metrics.setRunning(running);
        metrics.setSolved(solved);
        metrics.setStatus(status);
        metrics.setRows(10);
        metrics.setCols(10);
        metrics.setTotalPieces(100);
        metrics.setLastUpdate(LocalDateTime.now());
        return metrics;
    }
}
