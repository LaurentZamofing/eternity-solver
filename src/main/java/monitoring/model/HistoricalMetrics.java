package monitoring.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for storing historical metrics in H2 database.
 * Tracks progression over time for analysis and charting.
 */
@Entity
@Table(name = "metrics_history", indexes = {
        @Index(name = "idx_config_timestamp", columnList = "configName,timestamp")
})
public class HistoricalMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String configName;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private int depth;

    @Column(nullable = false)
    private double progressPercentage;

    @Column(nullable = false)
    private long totalComputeTimeMs;

    @Column
    private String status;

    @Column
    private String threadId;

    @Column
    private double piecesPerSecond;

    // Constructors
    public HistoricalMetrics() {
    }

    public HistoricalMetrics(String configName, LocalDateTime timestamp, int depth,
                             double progressPercentage, long totalComputeTimeMs) {
        this.configName = configName;
        this.timestamp = timestamp;
        this.depth = depth;
        this.progressPercentage = progressPercentage;
        this.totalComputeTimeMs = totalComputeTimeMs;
    }

    /**
     * Factory method to create from ConfigMetrics
     */
    public static HistoricalMetrics fromConfigMetrics(ConfigMetrics metrics) {
        HistoricalMetrics historical = new HistoricalMetrics();
        historical.setConfigName(metrics.getConfigName());

        // Use the original timestamp from save file, not current time
        // Convert Unix epoch milliseconds to LocalDateTime
        if (metrics.getTimestamp() > 0) {
            java.time.Instant instant = java.time.Instant.ofEpochMilli(metrics.getTimestamp());
            java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
            historical.setTimestamp(LocalDateTime.ofInstant(instant, zoneId));
        } else {
            historical.setTimestamp(LocalDateTime.now());
        }

        historical.setDepth(metrics.getDepth());
        historical.setProgressPercentage(metrics.getProgressPercentage());
        historical.setTotalComputeTimeMs(metrics.getTotalComputeTimeMs());
        historical.setStatus(metrics.getStatus());
        historical.setThreadId(metrics.getThreadId());
        historical.setPiecesPerSecond(metrics.getPiecesPerSecond());
        return historical;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public long getTotalComputeTimeMs() {
        return totalComputeTimeMs;
    }

    public void setTotalComputeTimeMs(long totalComputeTimeMs) {
        this.totalComputeTimeMs = totalComputeTimeMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public double getPiecesPerSecond() {
        return piecesPerSecond;
    }

    public void setPiecesPerSecond(double piecesPerSecond) {
        this.piecesPerSecond = piecesPerSecond;
    }

    @Override
    public String toString() {
        return "HistoricalMetrics{" +
                "id=" + id +
                ", configName='" + configName + '\'' +
                ", timestamp=" + timestamp +
                ", depth=" + depth +
                ", progress=" + String.format("%.2f%%", progressPercentage) +
                '}';
    }
}
