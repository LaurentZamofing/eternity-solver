package monitoring.controller;

import monitoring.model.CellDetails;
import monitoring.model.ConfigMetrics;
import monitoring.model.HistoricalMetrics;
import monitoring.model.PieceDefinition;
import monitoring.repository.MetricsRepository;
import monitoring.service.CellDetailsService;
import monitoring.service.IFileWatcherService;
import monitoring.service.HistoricalCellDetailsService;
import monitoring.service.IMetricsAggregator;
import monitoring.service.MetricsAggregator;
import monitoring.service.PieceDefinitionService;
import monitoring.service.SaveFileParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API Controller for monitoring dashboard.
 *
 * Endpoints:
 * GET  /api/configs              - List all configs with current metrics
 * GET  /api/configs/{id}         - Get specific config details
 * GET  /api/configs/{id}/history - Get historical data for config
 * GET  /api/stats/global         - Get global statistics
 * GET  /api/health               - Health check
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow CORS for frontend development
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private IFileWatcherService fileWatcherService;

    @Autowired
    private MetricsRepository repository;

    @Autowired
    private IMetricsAggregator aggregator;

    @Autowired
    private PieceDefinitionService pieceDefinitionService;

    @Autowired
    private SaveFileParser saveFileParser;

    /**
     * GET /api/configs
     * Returns list of all configurations with their current metrics.
     *
     * Query params:
     * - sort: field to sort by (progress, depth, time, name)
     * - order: asc or desc (default: desc)
     * - status: filter by status (running, idle, solved, stuck)
     */
    @GetMapping("/configs")
    public ResponseEntity<List<ConfigMetrics>> getAllConfigs(
            @RequestParam(required = false, defaultValue = "progress") String sort,
            @RequestParam(required = false, defaultValue = "desc") String order,
            @RequestParam(required = false) String status
    ) {
        logger.debug("GET /api/configs - sort={}, order={}, status={}", sort, order, status);

        Map<String, ConfigMetrics> metricsMap = fileWatcherService.getAllMetrics();
        Collection<ConfigMetrics> metrics = metricsMap.values();

        // Filter by status if specified
        if (status != null && !status.isEmpty()) {
            metrics = aggregator.filterByStatus(metrics, status);
        }

        // Sort by specified field
        List<ConfigMetrics> sorted;
        boolean descending = "desc".equalsIgnoreCase(order);

        switch (sort.toLowerCase()) {
            case "depth":
                sorted = aggregator.sortByDepth(metrics, descending);
                break;
            case "bestdepthever":
                sorted = aggregator.sortByBestDepthEver(metrics, descending);
                break;
            case "time":
                sorted = aggregator.sortByComputeTime(metrics, descending);
                break;
            case "name":
                sorted = metrics.stream()
                        .sorted(Comparator.comparing(ConfigMetrics::getConfigName))
                        .collect(Collectors.toList());
                if (descending) Collections.reverse(sorted);
                break;
            case "progress":
            default:
                sorted = aggregator.sortByProgress(metrics, descending);
                break;
        }

        return ResponseEntity.ok(sorted);
    }

    /**
     * GET /api/configs/{configName}
     * Get detailed metrics for a specific configuration.
     */
    @GetMapping("/configs/{configName}")
    public ResponseEntity<ConfigMetrics> getConfig(@PathVariable String configName) {
        logger.debug("GET /api/configs/{}", configName);

        ConfigMetrics metrics = fileWatcherService.getMetrics(configName);

        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/configs/{configName}/best
     * Get the best ever result for a configuration (highest depth ever reached).
     */
    @GetMapping("/configs/{configName}/best")
    public ResponseEntity<ConfigMetrics> getBestConfig(@PathVariable String configName) {
        logger.debug("GET /api/configs/{}/best", configName);

        try {
            // Get current metrics to find bestDepthEver
            ConfigMetrics currentMetrics = fileWatcherService.getMetrics(configName);
            if (currentMetrics == null || currentMetrics.getBestDepthEver() == 0) {
                return ResponseEntity.notFound().build();
            }

            // Construct path to best file: saves/eternity2/configName/best_{depth}.txt
            String bestFileName = String.format("best_%d.txt", currentMetrics.getBestDepthEver());
            java.nio.file.Path bestFilePath = java.nio.file.Paths.get("./saves/eternity2", configName, bestFileName);

            if (!java.nio.file.Files.exists(bestFilePath)) {
                logger.warn("Best file not found: {}", bestFilePath);
                return ResponseEntity.notFound().build();
            }

            // Parse the best file
            ConfigMetrics bestMetrics = saveFileParser.parseSaveFile(bestFilePath);
            if (bestMetrics == null) {
                return ResponseEntity.notFound().build();
            }

            // Set bestDepthEver to match current
            bestMetrics.setBestDepthEver(currentMetrics.getBestDepthEver());
            bestMetrics.setStatus("best_record");

            return ResponseEntity.ok(bestMetrics);

        } catch (Exception e) {
            logger.error("Error loading best result for {}", configName, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /api/configs/{configName}/history
     * Get historical progression data for a configuration.
     *
     * Query params:
     * - hours: number of hours to look back (default: 24)
     * - limit: max number of data points (default: 1000)
     */
    @GetMapping("/configs/{configName}/history")
    public ResponseEntity<List<HistoricalMetrics>> getConfigHistory(
            @PathVariable String configName,
            @RequestParam(required = false, defaultValue = "24") int hours,
            @RequestParam(required = false, defaultValue = "1000") int limit
    ) {
        logger.debug("GET /api/configs/{}/history - hours={}, limit={}", configName, hours, limit);

        LocalDateTime since = LocalDateTime.now().minusHours(hours);

        List<HistoricalMetrics> history = repository.findByConfigNameAndTimestampBetweenOrderByTimestampAsc(
                configName,
                since,
                LocalDateTime.now()
        );

        // Limit results if needed
        if (history.size() > limit) {
            // Sample evenly to stay under limit
            int step = history.size() / limit;
            final List<HistoricalMetrics> finalHistory = history;
            history = java.util.stream.IntStream.range(0, finalHistory.size())
                    .filter(i -> i % step == 0)
                    .mapToObj(finalHistory::get)
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/stats/global
     * Get aggregated statistics across all configurations.
     */
    @GetMapping("/stats/global")
    public ResponseEntity<MetricsAggregator.GlobalStats> getGlobalStats() {
        logger.debug("GET /api/stats/global");

        Map<String, ConfigMetrics> metricsMap = fileWatcherService.getAllMetrics();
        MetricsAggregator.GlobalStats stats = aggregator.aggregateMetrics(metricsMap);

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/stats/recent
     * Get recent activity (metrics from last N hours).
     */
    @GetMapping("/stats/recent")
    public ResponseEntity<List<HistoricalMetrics>> getRecentActivity(
            @RequestParam(required = false, defaultValue = "1") int hours
    ) {
        logger.debug("GET /api/stats/recent - hours={}", hours);

        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<HistoricalMetrics> recent = repository.findRecentMetrics(since);

        return ResponseEntity.ok(recent);
    }

    /**
     * GET /api/health
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("configsMonitored", fileWatcherService.getAllMetrics().size());
        health.put("databaseRecords", repository.count());

        return ResponseEntity.ok(health);
    }

    /**
     * GET /api/configs/summary
     * Get a quick summary of all configs (lightweight version).
     */
    @GetMapping("/configs/summary")
    public ResponseEntity<Map<String, Object>> getConfigsSummary() {
        logger.debug("GET /api/configs/summary");

        Map<String, ConfigMetrics> metricsMap = fileWatcherService.getAllMetrics();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalConfigs", metricsMap.size());

        // Create simplified summary for each config
        List<Map<String, Object>> configList = metricsMap.values().stream()
                .map(m -> {
                    Map<String, Object> config = new HashMap<>();
                    config.put("name", m.getConfigName());
                    config.put("depth", m.getDepth());
                    config.put("progress", m.getProgressPercentage());
                    config.put("status", m.getStatus());
                    return config;
                })
                .collect(Collectors.toList());

        summary.put("configs", configList);

        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/pieces/{puzzleName}
     * Get piece definitions for a puzzle (edge colors/patterns).
     */
    @GetMapping("/pieces/{puzzleName}")
    public ResponseEntity<Map<Integer, PieceDefinition>> getPieceDefinitions(
            @PathVariable String puzzleName
    ) {
        logger.debug("GET /api/pieces/{}", puzzleName);

        Map<Integer, PieceDefinition> definitions = pieceDefinitionService.getPieceDefinitions(puzzleName);

        if (definitions.isEmpty()) {
            logger.warn("No piece definitions found for puzzle: {}", puzzleName);
        }

        return ResponseEntity.ok(definitions);
    }

    /**
     * GET /api/pieces/{puzzleName}/{pieceId}
     * Get a specific piece definition.
     */
    @GetMapping("/pieces/{puzzleName}/{pieceId}")
    public ResponseEntity<PieceDefinition> getPieceDefinition(
            @PathVariable String puzzleName,
            @PathVariable int pieceId
    ) {
        logger.debug("GET /api/pieces/{}/{}", puzzleName, pieceId);

        PieceDefinition definition = pieceDefinitionService.getPieceDefinition(puzzleName, pieceId);

        if (definition == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(definition);
    }

    /**
     * POST /api/refresh
     * Clear cache and rescan all save files.
     * Useful when files have been deleted or modified externally.
     *
     * Returns the number of configs found after refresh.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshCache() {
        logger.info("POST /api/refresh - Refreshing cache...");

        try {
            int configsFound = fileWatcherService.refreshCache();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cache refreshed successfully");
            response.put("configsFound", configsFound);
            response.put("timestamp", LocalDateTime.now().toString());

            logger.info("Cache refresh completed: {} configs found", configsFound);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to refresh cache", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to refresh cache: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.status(500).body(response);
        }
    }

    @Autowired
    private CellDetailsService cellDetailsService;

    @Autowired
    private HistoricalCellDetailsService historicalCellDetailsService;

    /**
     * GET /api/configs/{configName}/cell/{row}/{col}/details
     * Get detailed information about a specific cell in the puzzle board.
     * Returns current piece, all possible pieces with validity, and constraints.
     */
    @GetMapping("/configs/{configName}/cell/{row}/{col}/details")
    public ResponseEntity<CellDetails> getCellDetails(
            @PathVariable String configName,
            @PathVariable int row,
            @PathVariable int col
    ) {
        logger.debug("GET /api/configs/{}/cell/{}/{}/details", configName, row, col);

        CellDetails details = cellDetailsService.getCellDetails(configName, row, col);

        if (details == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(details);
    }

    /**
     * GET /api/configs/{configName}/cell/{row}/{col}/details/historical
     * Get historical cell details showing constraints as they were when the piece was placed.
     * Excludes constraints from pieces placed after this cell.
     */
    @GetMapping("/configs/{configName}/cell/{row}/{col}/details/historical")
    public ResponseEntity<CellDetails> getHistoricalCellDetails(
            @PathVariable String configName,
            @PathVariable int row,
            @PathVariable int col
    ) {
        logger.debug("GET /api/configs/{}/cell/{}/{}/details/historical", configName, row, col);

        CellDetails details = historicalCellDetailsService.getHistoricalCellDetails(configName, row, col);

        if (details == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(details);
    }
}
