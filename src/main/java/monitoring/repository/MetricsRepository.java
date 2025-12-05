package monitoring.repository;

import monitoring.model.HistoricalMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for HistoricalMetrics.
 * Provides automatic CRUD operations and custom queries.
 */
@Repository
public interface MetricsRepository extends JpaRepository<HistoricalMetrics, Long> {

    /**
     * Find all metrics for a specific configuration, ordered by timestamp
     */
    List<HistoricalMetrics> findByConfigNameOrderByTimestampAsc(String configName);

    /**
     * Find metrics for a config within a time range
     */
    List<HistoricalMetrics> findByConfigNameAndTimestampBetweenOrderByTimestampAsc(
            String configName,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * Get the latest metric for each configuration
     */
    @Query("SELECT h FROM HistoricalMetrics h WHERE h.timestamp = " +
            "(SELECT MAX(h2.timestamp) FROM HistoricalMetrics h2 WHERE h2.configName = h.configName)")
    List<HistoricalMetrics> findLatestForAllConfigs();

    /**
     * Get the latest metric for a specific configuration
     */
    @Query("SELECT h FROM HistoricalMetrics h WHERE h.configName = :configName " +
            "ORDER BY h.timestamp DESC LIMIT 1")
    HistoricalMetrics findLatestByConfigName(@Param("configName") String configName);

    /**
     * Find configs that haven't been updated since a given time (stuck detection)
     */
    @Query("SELECT h FROM HistoricalMetrics h WHERE h.timestamp = " +
            "(SELECT MAX(h2.timestamp) FROM HistoricalMetrics h2 WHERE h2.configName = h.configName) " +
            "AND h.timestamp < :thresholdTime")
    List<HistoricalMetrics> findStuckConfigs(@Param("thresholdTime") LocalDateTime thresholdTime);

    /**
     * Get metrics since a specific time for all configs (for recent activity)
     */
    @Query("SELECT h FROM HistoricalMetrics h WHERE h.timestamp >= :since ORDER BY h.timestamp ASC")
    List<HistoricalMetrics> findRecentMetrics(@Param("since") LocalDateTime since);

    /**
     * Count total data points for a config (for cleanup/maintenance)
     */
    long countByConfigName(String configName);

    /**
     * Delete old metrics before a certain date (for database maintenance)
     */
    void deleteByTimestampBefore(LocalDateTime thresholdTime);

    /**
     * Check if a milestone already exists for a given config and depth.
     * Used during backfill to avoid inserting duplicate milestones.
     * Since each best_N.txt file represents a unique achievement (reaching depth N),
     * we only need to check config + depth, not timestamp.
     */
    boolean existsByConfigNameAndDepth(String configName, int depth);

    /**
     * Check if a stats entry already exists with exact config name, depth, and timestamp.
     * Used when parsing stats_history.jsonl files to avoid duplicates.
     */
    boolean existsByConfigNameAndDepthAndTimestamp(
            String configName,
            int depth,
            LocalDateTime timestamp
    );
}
