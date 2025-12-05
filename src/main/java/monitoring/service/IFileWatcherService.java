package monitoring.service;

import monitoring.model.ConfigMetrics;

import java.util.Map;

/**
 * Interface for file watcher service that monitors the saves/ directory.
 * This interface enables Mockito mocking in unit tests.
 *
 * <p>The file watcher service monitors save files, parses metrics,
 * and publishes updates via WebSocket and database.</p>
 *
 * @see FileWatcherServiceImpl for the implementation
 */
public interface IFileWatcherService {

    /**
     * Get current metrics for a specific configuration.
     *
     * @param configName the name of the configuration (e.g., "eternity2_p01_ascending")
     * @return the metrics for the configuration, or null if not found
     */
    ConfigMetrics getMetrics(String configName);

    /**
     * Get all current metrics (latest for each config).
     *
     * @return a map of config name to metrics
     */
    Map<String, ConfigMetrics> getAllMetrics();

    /**
     * Clear cache and rescan all files.
     * Useful when files have been deleted or modified externally.
     *
     * @return number of configs found after refresh
     */
    int refreshCache();
}
