package monitoring.controller;

import monitoring.model.ConfigMetrics;
import monitoring.service.IFileWatcherService;
import monitoring.service.IMetricsAggregator;
import monitoring.service.MetricsAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket Controller for real-time metrics updates.
 *
 * WebSocket Endpoints:
 * - /ws - WebSocket connection endpoint (configured in WebSocketConfig)
 *
 * STOMP Topics:
 * - /topic/metrics - Real-time metrics updates (published by FileWatcherService)
 * - /topic/stats - Global stats updates
 * - /app/subscribe - Client subscription handler
 *
 * Usage from client:
 * ```javascript
 * const socket = new SockJS('http://localhost:8080/ws');
 * const stompClient = Stomp.over(socket);
 *
 * stompClient.connect({}, () => {
 *   stompClient.subscribe('/topic/metrics', (message) => {
 *     const metrics = JSON.parse(message.body);
 *     console.log('Received metrics:', metrics);
 *   });
 * });
 * ```
 */
@Controller
public class MetricsWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(MetricsWebSocketController.class);

    @Autowired
    private IFileWatcherService fileWatcherService;

    @Autowired
    private IMetricsAggregator aggregator;

    /**
     * Handle client subscription to /topic/metrics.
     * Immediately sends current state to the subscriber.
     */
    @SubscribeMapping("/metrics")
    public Map<String, ConfigMetrics> onSubscribeMetrics() {
        logger.info("Client subscribed to /topic/metrics");

        // Send current state immediately to new subscriber
        Map<String, ConfigMetrics> allMetrics = fileWatcherService.getAllMetrics();
        logger.debug("Sending {} configs to new subscriber", allMetrics.size());

        return allMetrics;
    }

    /**
     * Handle client subscription to /topic/stats.
     * Immediately sends current global stats to the subscriber.
     */
    @SubscribeMapping("/stats")
    public MetricsAggregator.GlobalStats onSubscribeStats() {
        logger.info("Client subscribed to /topic/stats");

        Map<String, ConfigMetrics> metricsMap = fileWatcherService.getAllMetrics();
        MetricsAggregator.GlobalStats stats = aggregator.aggregateMetrics(metricsMap);

        logger.debug("Sending global stats to new subscriber");

        return stats;
    }

    /**
     * Handle client request for specific config.
     * Client sends: {configName: "eternity2_p01_ascending"}
     * Server responds with current metrics for that config.
     */
    @MessageMapping("/getConfig")
    @SendTo("/topic/configResponse")
    public ConfigMetrics getConfigMetrics(Map<String, String> request) {
        String configName = request.get("configName");
        logger.debug("Client requested config: {}", configName);

        ConfigMetrics metrics = fileWatcherService.getMetrics(configName);

        if (metrics == null) {
            logger.warn("Config not found: {}", configName);
        }

        return metrics;
    }

    /**
     * Handle client ping for keep-alive.
     * Client sends: {type: "ping"}
     * Server responds: {type: "pong", timestamp: ...}
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public Map<String, Object> handlePing() {
        logger.trace("Received ping from client");

        return Map.of(
                "type", "pong",
                "timestamp", System.currentTimeMillis(),
                "configCount", fileWatcherService.getAllMetrics().size()
        );
    }
}
