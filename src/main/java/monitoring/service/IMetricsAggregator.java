package monitoring.service;

import monitoring.model.ConfigMetrics;
import monitoring.service.MetricsAggregator.GlobalStats;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Interface for metrics aggregation service.
 * This interface enables Mockito mocking in unit tests.
 *
 * @see MetricsAggregatorImpl for the implementation
 */
public interface IMetricsAggregator {

    /**
     * Aggregate metrics from all configurations into global statistics.
     *
     * @param metricsMap map of config name to metrics
     * @return global statistics
     */
    GlobalStats aggregateMetrics(Map<String, ConfigMetrics> metricsMap);

    /**
     * Filter configurations by status.
     *
     * @param metrics collection of metrics
     * @param status status to filter by (running, idle, solved, stuck)
     * @return filtered collection
     */
    Collection<ConfigMetrics> filterByStatus(Collection<ConfigMetrics> metrics, String status);

    /**
     * Sort configurations by depth.
     *
     * @param metrics collection of metrics
     * @param descending true for descending order
     * @return sorted list
     */
    List<ConfigMetrics> sortByDepth(Collection<ConfigMetrics> metrics, boolean descending);

    /**
     * Sort configurations by best depth ever reached.
     *
     * @param metrics collection of metrics
     * @param descending true for descending order
     * @return sorted list
     */
    List<ConfigMetrics> sortByBestDepthEver(Collection<ConfigMetrics> metrics, boolean descending);

    /**
     * Sort configurations by compute time.
     *
     * @param metrics collection of metrics
     * @param descending true for descending order
     * @return sorted list
     */
    List<ConfigMetrics> sortByComputeTime(Collection<ConfigMetrics> metrics, boolean descending);

    /**
     * Sort configurations by progress percentage.
     *
     * @param metrics collection of metrics
     * @param descending true for descending order
     * @return sorted list
     */
    List<ConfigMetrics> sortByProgress(Collection<ConfigMetrics> metrics, boolean descending);
}
