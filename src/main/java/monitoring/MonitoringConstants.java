package monitoring;

/**
 * Configuration constants for the monitoring system.
 *
 * <p>Centralizes magic numbers and configuration values used throughout
 * the monitoring package to improve maintainability and clarity.
 */
public final class MonitoringConstants {

    private MonitoringConstants() {
        // Utility class - prevent instantiation
    }

    /**
     * File parsing constants.
     */
    public static final class FileParsing {
        private FileParsing() {}

        /**
         * Maximum number of lines to read from a save file before stopping.
         * Limits memory usage when parsing large files.
         * Default: 500 lines (sufficient for headers + board + placement order)
         */
        public static final int MAX_LINES_TO_READ = 500;

        /**
         * Delay in milliseconds to wait after file modification before parsing.
         * Ensures file is fully written to disk.
         * Default: 100ms
         */
        public static final long FILE_WRITE_DELAY_MS = 100;
    }

    /**
     * API endpoint defaults.
     */
    public static final class Api {
        private Api() {}

        /**
         * Default maximum number of historical data points to return.
         * Prevents overwhelming clients with large datasets.
         * Default: 1000 entries
         */
        public static final int DEFAULT_HISTORY_LIMIT = 1000;

        /**
         * Default number of hours to look back for historical data.
         * Default: 24 hours (1 day)
         */
        public static final int DEFAULT_HISTORY_HOURS = 24;

        /**
         * Default number of hours for recent activity queries.
         * Default: 1 hour
         */
        public static final int DEFAULT_RECENT_HOURS = 1;
    }

    /**
     * Time-related constants.
     */
    public static final class Time {
        private Time() {}

        /**
         * Seconds in one day.
         * Used for time calculations and formatting.
         */
        public static final long SECONDS_PER_DAY = 86400;

        /**
         * Seconds in one hour.
         */
        public static final long SECONDS_PER_HOUR = 3600;

        /**
         * Seconds in one minute.
         */
        public static final long SECONDS_PER_MINUTE = 60;

        /**
         * Milliseconds in one second.
         */
        public static final long MILLIS_PER_SECOND = 1000;

        /**
         * Health check interval in milliseconds (5 minutes).
         */
        public static final long HEALTH_CHECK_INTERVAL_MS = 300000;

        /**
         * Shutdown timeout in seconds (5 seconds).
         * Maximum time to wait for graceful shutdown.
         */
        public static final long SHUTDOWN_TIMEOUT_SECONDS = 5;
    }

    /**
     * Database constants.
     */
    public static final class Database {
        private Database() {}

        /**
         * Maximum length for config name column.
         */
        public static final int CONFIG_NAME_MAX_LENGTH = 100;
    }

    /**
     * HTTP status codes used in monitoring endpoints.
     */
    public static final class HttpStatus {
        private HttpStatus() {}

        /**
         * HTTP 500 Internal Server Error.
         */
        public static final int INTERNAL_SERVER_ERROR = 500;

        /**
         * HTTP 400 Bad Request.
         */
        public static final int BAD_REQUEST = 400;
    }
}
