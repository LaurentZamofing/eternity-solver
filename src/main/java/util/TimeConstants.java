package util;

/**
 * Time-related constants used throughout the application.
 * Centralizes magic numbers for time conversions and timeouts.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public final class TimeConstants {

    // Prevent instantiation
    private TimeConstants() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    // ========== Time Conversion Constants ==========

    /** Milliseconds in one second */
    public static final long MILLIS_PER_SECOND = 1000L;

    /** Milliseconds in one minute (60 seconds) */
    public static final long MILLIS_PER_MINUTE = 60_000L;

    /** Milliseconds in one hour (3600 seconds) */
    public static final long MILLIS_PER_HOUR = 3_600_000L;

    /** Seconds in one minute */
    public static final int SECONDS_PER_MINUTE = 60;

    /** Seconds in one hour */
    public static final int SECONDS_PER_HOUR = 3600;

    /** Seconds in one day (24 hours) */
    public static final int SECONDS_PER_DAY = 86400;

    /** Minutes in one hour */
    public static final int MINUTES_PER_HOUR = 60;

    // ========== Common Timeout Values ==========

    /** Default puzzle solving timeout: 10 minutes */
    public static final long DEFAULT_PUZZLE_TIMEOUT_MS = 10 * MILLIS_PER_MINUTE;

    /** Default thread join timeout: 5 seconds */
    public static final long DEFAULT_THREAD_JOIN_TIMEOUT_MS = 5 * MILLIS_PER_SECOND;

    /** Default thread sleep interval: 1 second */
    public static final long DEFAULT_THREAD_SLEEP_MS = 1 * MILLIS_PER_SECOND;

    /** Short timeout for quick operations: 100 milliseconds */
    public static final long SHORT_TIMEOUT_MS = 100L;

    /** Medium timeout: 1 second */
    public static final long MEDIUM_TIMEOUT_MS = 1 * MILLIS_PER_SECOND;

    /** Long timeout: 30 seconds */
    public static final long LONG_TIMEOUT_MS = 30 * MILLIS_PER_SECOND;

    // ========== Monitoring and Auto-save Intervals ==========

    /** Auto-save interval: 5 seconds */
    public static final long AUTOSAVE_INTERVAL_MS = 5 * MILLIS_PER_SECOND;

    /** Metrics collection interval: 1 second */
    public static final long METRICS_INTERVAL_MS = 1 * MILLIS_PER_SECOND;

    /** File watcher poll interval: 1 second */
    public static final long FILE_WATCH_POLL_INTERVAL_MS = 1 * MILLIS_PER_SECOND;

    /** WebSocket heartbeat interval: 10 seconds */
    public static final long WEBSOCKET_HEARTBEAT_MS = 10 * MILLIS_PER_SECOND;

    // ========== Helper Methods ==========

    /**
     * Converts milliseconds to seconds (double precision).
     *
     * @param millis Time in milliseconds
     * @return Time in seconds
     */
    public static double toSeconds(long millis) {
        return millis / (double) MILLIS_PER_SECOND;
    }

    /**
     * Converts milliseconds to minutes (double precision).
     *
     * @param millis Time in milliseconds
     * @return Time in minutes
     */
    public static double toMinutes(long millis) {
        return millis / (double) MILLIS_PER_MINUTE;
    }

    /**
     * Converts milliseconds to hours (double precision).
     *
     * @param millis Time in milliseconds
     * @return Time in hours
     */
    public static double toHours(long millis) {
        return millis / (double) MILLIS_PER_HOUR;
    }

    /**
     * Converts seconds to milliseconds.
     *
     * @param seconds Time in seconds
     * @return Time in milliseconds
     */
    public static long toMillis(long seconds) {
        return seconds * MILLIS_PER_SECOND;
    }

    /**
     * Formats milliseconds as a human-readable duration string.
     * Example: 125000ms → "2m 5s"
     *
     * @param millis Duration in milliseconds
     * @return Formatted duration string
     */
    public static String formatDuration(long millis) {
        if (millis < MILLIS_PER_SECOND) {
            return millis + "ms";
        }

        long hours = millis / MILLIS_PER_HOUR;
        long minutes = (millis % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE;
        long seconds = (millis % MILLIS_PER_MINUTE) / MILLIS_PER_SECOND;

        StringBuilder result = new StringBuilder();
        if (hours > 0) {
            result.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            result.append(minutes).append("m ");
        }
        result.append(seconds).append("s");

        return result.toString().trim();
    }
}
