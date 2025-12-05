package monitoring.service;

import monitoring.model.ConfigMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to aggregate and analyze metrics across all configurations.
 * Provides global statistics and insights.
 */
@Service
public class MetricsAggregator {

    private static final Logger logger = LoggerFactory.getLogger(MetricsAggregator.class);

    /**
     * Global statistics model.
     */
    public static class GlobalStats {
        private int totalConfigs;
        private int runningConfigs;
        private int solvedConfigs;
        private int idleConfigs;
        private int stuckConfigs;

        private long totalComputeTimeMs;
        private String totalComputeTimeFormatted;

        private double bestProgressPercentage;
        private String bestProgressConfigName;

        private double averageProgressPercentage;

        private int maxDepth;
        private String maxDepthConfigName;

        private double fastestPiecesPerSecond;
        private String fastestConfigName;

        private List<ConfigMetrics> topConfigs;
        private List<ConfigMetrics> stuckConfigsList;

        // Getters and setters
        public int getTotalConfigs() {
            return totalConfigs;
        }

        public void setTotalConfigs(int totalConfigs) {
            this.totalConfigs = totalConfigs;
        }

        public int getRunningConfigs() {
            return runningConfigs;
        }

        public void setRunningConfigs(int runningConfigs) {
            this.runningConfigs = runningConfigs;
        }

        public int getSolvedConfigs() {
            return solvedConfigs;
        }

        public void setSolvedConfigs(int solvedConfigs) {
            this.solvedConfigs = solvedConfigs;
        }

        public int getIdleConfigs() {
            return idleConfigs;
        }

        public void setIdleConfigs(int idleConfigs) {
            this.idleConfigs = idleConfigs;
        }

        public int getStuckConfigs() {
            return stuckConfigs;
        }

        public void setStuckConfigs(int stuckConfigs) {
            this.stuckConfigs = stuckConfigs;
        }

        public long getTotalComputeTimeMs() {
            return totalComputeTimeMs;
        }

        public void setTotalComputeTimeMs(long totalComputeTimeMs) {
            this.totalComputeTimeMs = totalComputeTimeMs;
            this.totalComputeTimeFormatted = formatTime(totalComputeTimeMs);
        }

        public String getTotalComputeTimeFormatted() {
            return totalComputeTimeFormatted;
        }

        public double getBestProgressPercentage() {
            return bestProgressPercentage;
        }

        public void setBestProgressPercentage(double bestProgressPercentage) {
            this.bestProgressPercentage = bestProgressPercentage;
        }

        public String getBestProgressConfigName() {
            return bestProgressConfigName;
        }

        public void setBestProgressConfigName(String bestProgressConfigName) {
            this.bestProgressConfigName = bestProgressConfigName;
        }

        public double getAverageProgressPercentage() {
            return averageProgressPercentage;
        }

        public void setAverageProgressPercentage(double averageProgressPercentage) {
            this.averageProgressPercentage = averageProgressPercentage;
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }

        public String getMaxDepthConfigName() {
            return maxDepthConfigName;
        }

        public void setMaxDepthConfigName(String maxDepthConfigName) {
            this.maxDepthConfigName = maxDepthConfigName;
        }

        public double getFastestPiecesPerSecond() {
            return fastestPiecesPerSecond;
        }

        public void setFastestPiecesPerSecond(double fastestPiecesPerSecond) {
            this.fastestPiecesPerSecond = fastestPiecesPerSecond;
        }

        public String getFastestConfigName() {
            return fastestConfigName;
        }

        public void setFastestConfigName(String fastestConfigName) {
            this.fastestConfigName = fastestConfigName;
        }

        public List<ConfigMetrics> getTopConfigs() {
            return topConfigs;
        }

        public void setTopConfigs(List<ConfigMetrics> topConfigs) {
            this.topConfigs = topConfigs;
        }

        public List<ConfigMetrics> getStuckConfigsList() {
            return stuckConfigsList;
        }

        public void setStuckConfigsList(List<ConfigMetrics> stuckConfigsList) {
            this.stuckConfigsList = stuckConfigsList;
        }

        private String formatTime(long milliseconds) {
            long seconds = milliseconds / 1000;
            long days = seconds / 86400;
            long hours = (seconds % 86400) / 3600;
            long minutes = (seconds % 3600) / 60;

            if (days > 0) {
                return String.format("%dd %dh %dm", days, hours, minutes);
            } else if (hours > 0) {
                return String.format("%dh %dm", hours, minutes);
            } else {
                return String.format("%dm", minutes);
            }
        }
    }

    /**
     * Aggregate metrics from a map of config metrics.
     */
    public GlobalStats aggregateMetrics(Map<String, ConfigMetrics> metricsMap) {
        GlobalStats stats = new GlobalStats();

        if (metricsMap == null || metricsMap.isEmpty()) {
            return stats;
        }

        Collection<ConfigMetrics> allMetrics = metricsMap.values();

        // Basic counts
        stats.setTotalConfigs(allMetrics.size());

        long runningCount = allMetrics.stream()
                .filter(ConfigMetrics::isRunning)
                .count();
        stats.setRunningConfigs((int) runningCount);

        long solvedCount = allMetrics.stream()
                .filter(ConfigMetrics::isSolved)
                .count();
        stats.setSolvedConfigs((int) solvedCount);

        long idleCount = allMetrics.stream()
                .filter(m -> "idle".equals(m.getStatus()))
                .count();
        stats.setIdleConfigs((int) idleCount);

        long stuckCount = allMetrics.stream()
                .filter(m -> "stuck".equals(m.getStatus()))
                .count();
        stats.setStuckConfigs((int) stuckCount);

        // Total compute time
        long totalTime = allMetrics.stream()
                .mapToLong(ConfigMetrics::getTotalComputeTimeMs)
                .sum();
        stats.setTotalComputeTimeMs(totalTime);

        // Best progress
        Optional<ConfigMetrics> bestProgress = allMetrics.stream()
                .max(Comparator.comparingDouble(ConfigMetrics::getProgressPercentage));
        bestProgress.ifPresent(metrics -> {
            stats.setBestProgressPercentage(metrics.getProgressPercentage());
            stats.setBestProgressConfigName(metrics.getConfigName());
        });

        // Average progress
        double avgProgress = allMetrics.stream()
                .mapToDouble(ConfigMetrics::getProgressPercentage)
                .average()
                .orElse(0.0);
        stats.setAverageProgressPercentage(avgProgress);

        // Max depth
        Optional<ConfigMetrics> maxDepth = allMetrics.stream()
                .max(Comparator.comparingInt(ConfigMetrics::getDepth));
        maxDepth.ifPresent(metrics -> {
            stats.setMaxDepth(metrics.getDepth());
            stats.setMaxDepthConfigName(metrics.getConfigName());
        });

        // Fastest pieces per second
        Optional<ConfigMetrics> fastest = allMetrics.stream()
                .filter(m -> m.getPiecesPerSecond() > 0)
                .max(Comparator.comparingDouble(ConfigMetrics::getPiecesPerSecond));
        fastest.ifPresent(metrics -> {
            stats.setFastestPiecesPerSecond(metrics.getPiecesPerSecond());
            stats.setFastestConfigName(metrics.getConfigName());
        });

        // Top 10 configs by progress
        List<ConfigMetrics> topConfigs = allMetrics.stream()
                .sorted(Comparator.comparingDouble(ConfigMetrics::getProgressPercentage).reversed())
                .limit(10)
                .collect(Collectors.toList());
        stats.setTopConfigs(topConfigs);

        // Stuck configs (no update in last 10 minutes)
        List<ConfigMetrics> stuckConfigs = allMetrics.stream()
                .filter(this::isStuck)
                .sorted(Comparator.comparing(ConfigMetrics::getLastUpdate))
                .collect(Collectors.toList());
        stats.setStuckConfigsList(stuckConfigs);

        logger.debug("Aggregated stats: {} configs, {} solved, {} running",
                stats.getTotalConfigs(), stats.getSolvedConfigs(), stats.getRunningConfigs());

        return stats;
    }

    /**
     * Determine if a config is "stuck" (no recent updates).
     */
    private boolean isStuck(ConfigMetrics metrics) {
        if (metrics.getLastUpdate() == null) {
            return false;
        }

        long minutesSinceUpdate = java.time.Duration.between(
                metrics.getLastUpdate(),
                java.time.LocalDateTime.now()
        ).toMinutes();

        // Consider stuck if no update for 10+ minutes and not solved
        return minutesSinceUpdate >= 10 && !metrics.isSolved();
    }

    /**
     * Get configs sorted by a specific criterion.
     */
    public List<ConfigMetrics> sortByProgress(Collection<ConfigMetrics> metrics, boolean descending) {
        Comparator<ConfigMetrics> comparator = Comparator.comparingDouble(ConfigMetrics::getProgressPercentage);
        if (descending) {
            comparator = comparator.reversed();
        }
        return metrics.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    public List<ConfigMetrics> sortByDepth(Collection<ConfigMetrics> metrics, boolean descending) {
        Comparator<ConfigMetrics> comparator = Comparator.comparingInt(ConfigMetrics::getDepth);
        if (descending) {
            comparator = comparator.reversed();
        }
        return metrics.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    public List<ConfigMetrics> sortByBestDepthEver(Collection<ConfigMetrics> metrics, boolean descending) {
        Comparator<ConfigMetrics> comparator = Comparator.comparingInt(ConfigMetrics::getBestDepthEver);
        if (descending) {
            comparator = comparator.reversed();
        }
        return metrics.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    public List<ConfigMetrics> sortByComputeTime(Collection<ConfigMetrics> metrics, boolean descending) {
        Comparator<ConfigMetrics> comparator = Comparator.comparingLong(ConfigMetrics::getTotalComputeTimeMs);
        if (descending) {
            comparator = comparator.reversed();
        }
        return metrics.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Filter configs by status.
     */
    public List<ConfigMetrics> filterByStatus(Collection<ConfigMetrics> metrics, String status) {
        return metrics.stream()
                .filter(m -> status.equals(m.getStatus()))
                .collect(Collectors.toList());
    }
}
