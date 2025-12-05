package monitoring.model;

import java.time.LocalDateTime;

/**
 * Represents the status of a solver thread.
 * Tracks which configuration a thread is working on and its activity.
 */
public class ThreadStatus {
    private String threadId;
    private String threadName;
    private String currentConfigName;
    private LocalDateTime startTime;
    private LocalDateTime lastActivity;
    private String status; // "running", "idle", "rotating", "finished"
    private long tasksCompleted;
    private int currentDepth;

    public ThreadStatus() {
    }

    public ThreadStatus(String threadId, String threadName) {
        this.threadId = threadId;
        this.threadName = threadName;
        this.status = "idle";
        this.tasksCompleted = 0;
    }

    // Getters and Setters
    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getCurrentConfigName() {
        return currentConfigName;
    }

    public void setCurrentConfigName(String currentConfigName) {
        this.currentConfigName = currentConfigName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTasksCompleted() {
        return tasksCompleted;
    }

    public void setTasksCompleted(long tasksCompleted) {
        this.tasksCompleted = tasksCompleted;
    }

    public void incrementTasksCompleted() {
        this.tasksCompleted++;
    }

    public int getCurrentDepth() {
        return currentDepth;
    }

    public void setCurrentDepth(int currentDepth) {
        this.currentDepth = currentDepth;
    }

    @Override
    public String toString() {
        return "ThreadStatus{" +
                "threadId='" + threadId + '\'' +
                ", threadName='" + threadName + '\'' +
                ", currentConfig='" + currentConfigName + '\'' +
                ", status='" + status + '\'' +
                ", depth=" + currentDepth +
                '}';
    }
}
