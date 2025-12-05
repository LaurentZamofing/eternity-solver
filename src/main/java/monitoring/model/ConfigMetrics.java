package monitoring.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents real-time metrics for a single puzzle configuration.
 * This is the primary data model for monitoring solver progress.
 */
public class ConfigMetrics {
    private String configName;
    private long timestamp;
    private LocalDateTime lastUpdate;
    private String lastSaveDate; // Formatted last save date
    private String lastSaveRelative; // Relative time since last save
    private int depth;
    private int bestDepthEver; // Highest depth ever reached (from best_*.txt files)
    private double progressPercentage; // Search space estimation from solver
    private double physicalProgressPercentage; // Actual pieces placed ratio
    private long totalComputeTimeMs;
    private String computeTimeFormatted;
    private int rows;
    private int cols;
    private int totalPieces;
    private boolean isRunning;
    private boolean isSolved;
    private String threadId;
    private String status; // "running", "idle", "solved", "stuck"

    // Performance metrics
    private double piecesPerSecond;
    private long estimatedTimeRemainingMs;

    // Board visualization
    private String[][] boardState; // [row][col] = "pieceId_rotation" or null

    // Placement order tracking
    private List<PlacementInfo> placementOrder; // Chronological order of piece placements
    private Map<String, Integer> positionToSequence; // Map from "row,col" to sequence number

    // Constructors
    public ConfigMetrics() {
    }

    public ConfigMetrics(String configName) {
        this.configName = configName;
        this.status = "idle";
    }

    // Getters and Setters
    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.lastUpdate = LocalDateTime.now();

        // Calculate formatted dates
        if (timestamp > 0) {
            java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
            java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, zoneId);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            this.lastSaveDate = dateTime.format(formatter);

            // Calculate relative time
            long now = System.currentTimeMillis();
            long diffMs = now - timestamp;
            long diffSeconds = diffMs / 1000;

            if (diffSeconds < 60) {
                this.lastSaveRelative = diffSeconds + "s ago";
            } else if (diffSeconds < 3600) {
                long minutes = diffSeconds / 60;
                this.lastSaveRelative = minutes + "m ago";
            } else if (diffSeconds < 86400) {
                long hours = diffSeconds / 3600;
                long minutes = (diffSeconds % 3600) / 60;
                this.lastSaveRelative = String.format("%dh %02dm ago", hours, minutes);
            } else {
                long days = diffSeconds / 86400;
                this.lastSaveRelative = days + "d ago";
            }
        } else {
            this.lastSaveDate = "N/A";
            this.lastSaveRelative = "N/A";
        }
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public String getLastSaveDate() {
        return lastSaveDate;
    }

    public void setLastSaveDate(String lastSaveDate) {
        this.lastSaveDate = lastSaveDate;
    }

    public String getLastSaveRelative() {
        return lastSaveRelative;
    }

    public void setLastSaveRelative(String lastSaveRelative) {
        this.lastSaveRelative = lastSaveRelative;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getBestDepthEver() {
        return bestDepthEver;
    }

    public void setBestDepthEver(int bestDepthEver) {
        this.bestDepthEver = bestDepthEver;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public double getPhysicalProgressPercentage() {
        return physicalProgressPercentage;
    }

    public void setPhysicalProgressPercentage(double physicalProgressPercentage) {
        this.physicalProgressPercentage = physicalProgressPercentage;
    }

    public long getTotalComputeTimeMs() {
        return totalComputeTimeMs;
    }

    public void setTotalComputeTimeMs(long totalComputeTimeMs) {
        this.totalComputeTimeMs = totalComputeTimeMs;
        this.computeTimeFormatted = formatTime(totalComputeTimeMs);
    }

    public String getComputeTimeFormatted() {
        return computeTimeFormatted;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public int getTotalPieces() {
        return totalPieces;
    }

    public void setTotalPieces(int totalPieces) {
        this.totalPieces = totalPieces;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public boolean isSolved() {
        return isSolved;
    }

    public void setSolved(boolean solved) {
        isSolved = solved;
        if (solved) {
            this.status = "solved";
        }
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getPiecesPerSecond() {
        return piecesPerSecond;
    }

    public void setPiecesPerSecond(double piecesPerSecond) {
        this.piecesPerSecond = piecesPerSecond;
    }

    public long getEstimatedTimeRemainingMs() {
        return estimatedTimeRemainingMs;
    }

    public void setEstimatedTimeRemainingMs(long estimatedTimeRemainingMs) {
        this.estimatedTimeRemainingMs = estimatedTimeRemainingMs;
    }

    public String[][] getBoardState() {
        return boardState;
    }

    public void setBoardState(String[][] boardState) {
        this.boardState = boardState;
    }

    public List<PlacementInfo> getPlacementOrder() {
        return placementOrder;
    }

    public void setPlacementOrder(List<PlacementInfo> placementOrder) {
        this.placementOrder = placementOrder;

        // Build position-to-sequence map for quick lookup
        if (placementOrder != null) {
            this.positionToSequence = new HashMap<>();
            for (int i = 0; i < placementOrder.size(); i++) {
                PlacementInfo info = placementOrder.get(i);
                info.setSequenceNumber(i + 1); // 1-based sequence
                positionToSequence.put(info.getPositionKey(), i + 1);
            }
        }
    }

    public Map<String, Integer> getPositionToSequence() {
        return positionToSequence;
    }

    /**
     * Get the sequence number for a given position.
     * Returns 0 if position not found in placement order.
     */
    public int getSequenceNumber(int row, int col) {
        if (positionToSequence == null) {
            return 0;
        }
        return positionToSequence.getOrDefault(row + "," + col, 0);
    }

    // Utility methods
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    @Override
    public String toString() {
        return "ConfigMetrics{" +
                "configName='" + configName + '\'' +
                ", depth=" + depth +
                ", progress=" + String.format("%.2f%%", progressPercentage) +
                ", time=" + computeTimeFormatted +
                ", status='" + status + '\'' +
                '}';
    }
}
