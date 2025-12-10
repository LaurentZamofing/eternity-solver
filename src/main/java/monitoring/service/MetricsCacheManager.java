package monitoring.service;

import monitoring.model.ConfigMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages in-memory cache of latest metrics for each configuration.
 * Thread-safe implementation using ConcurrentHashMap.
 *
 * This service provides fast access to current metrics without database queries,
 * essential for real-time monitoring dashboard performance.
 */
@Service
public class MetricsCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(MetricsCacheManager.class);

    /**
     * Cache of latest metrics for each config (configName -> ConfigMetrics)
     */
    private final Map<String, ConfigMetrics> metricsCache = new ConcurrentHashMap<>();

    /**
     * Store or update metrics for a configuration.
     *
     * @param configName the configuration name
     * @param metrics the metrics to cache
     */
    public void put(String configName, ConfigMetrics metrics) {
        metricsCache.put(configName, metrics);
        logger.trace("Updated cache for config: {}", configName);
    }

    /**
     * Get current metrics for a specific configuration.
     *
     * @param configName the configuration name
     * @return the cached metrics, or null if not found
     */
    public ConfigMetrics get(String configName) {
        return metricsCache.get(configName);
    }

    /**
     * Get all current metrics (latest for each config).
     *
     * @return a copy of all cached metrics
     */
    public Map<String, ConfigMetrics> getAll() {
        return new HashMap<>(metricsCache);
    }

    /**
     * Clear all cached metrics.
     * Used during cache refresh operations.
     *
     * @return the number of configs that were cleared
     */
    public int clear() {
        int size = metricsCache.size();
        metricsCache.clear();
        logger.debug("Cleared {} configs from cache", size);
        return size;
    }

    /**
     * Get the current size of the cache.
     *
     * @return number of configs currently cached
     */
    public int size() {
        return metricsCache.size();
    }

    /**
     * Check if the cache contains metrics for a specific configuration.
     *
     * @param configName the configuration name
     * @return true if metrics exist in cache
     */
    public boolean contains(String configName) {
        return metricsCache.containsKey(configName);
    }

    /**
     * Check if the cache is empty.
     *
     * @return true if no configs are cached
     */
    public boolean isEmpty() {
        return metricsCache.isEmpty();
    }
}
