package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for TimeConstants utility class.
 * Tests time conversion constants, timeout values, and helper methods.
 */
@DisplayName("TimeConstants Tests")
public class TimeConstantsTest {

    // ========== Constructor Tests ==========

    @Test
    @DisplayName("Constructor throws UnsupportedOperationException")
    void testConstructorThrowsException() {
        try {
            // Use reflection to access private constructor
            var constructor = TimeConstants.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
            fail("Constructor should throw UnsupportedOperationException");
        } catch (Exception e) {
            // Reflection wraps the exception in InvocationTargetException
            Throwable cause = e.getCause();
            assertNotNull(cause);
            assertEquals(UnsupportedOperationException.class, cause.getClass());
        }
    }

    // ========== Time Conversion Constants Tests ==========

    @Test
    @DisplayName("MILLIS_PER_SECOND equals 1000")
    void testMillisPerSecond() {
        assertEquals(1000L, TimeConstants.MILLIS_PER_SECOND);
    }

    @Test
    @DisplayName("MILLIS_PER_MINUTE equals 60000")
    void testMillisPerMinute() {
        assertEquals(60_000L, TimeConstants.MILLIS_PER_MINUTE);
        assertEquals(60 * TimeConstants.MILLIS_PER_SECOND, TimeConstants.MILLIS_PER_MINUTE);
    }

    @Test
    @DisplayName("MILLIS_PER_HOUR equals 3600000")
    void testMillisPerHour() {
        assertEquals(3_600_000L, TimeConstants.MILLIS_PER_HOUR);
        assertEquals(60 * TimeConstants.MILLIS_PER_MINUTE, TimeConstants.MILLIS_PER_HOUR);
    }

    @Test
    @DisplayName("SECONDS_PER_MINUTE equals 60")
    void testSecondsPerMinute() {
        assertEquals(60, TimeConstants.SECONDS_PER_MINUTE);
    }

    @Test
    @DisplayName("SECONDS_PER_HOUR equals 3600")
    void testSecondsPerHour() {
        assertEquals(3600, TimeConstants.SECONDS_PER_HOUR);
        assertEquals(60 * TimeConstants.SECONDS_PER_MINUTE, TimeConstants.SECONDS_PER_HOUR);
    }

    @Test
    @DisplayName("MINUTES_PER_HOUR equals 60")
    void testMinutesPerHour() {
        assertEquals(60, TimeConstants.MINUTES_PER_HOUR);
    }

    // ========== Timeout Values Tests ==========

    @Test
    @DisplayName("DEFAULT_PUZZLE_TIMEOUT_MS equals 10 minutes")
    void testDefaultPuzzleTimeout() {
        assertEquals(10 * TimeConstants.MILLIS_PER_MINUTE, TimeConstants.DEFAULT_PUZZLE_TIMEOUT_MS);
        assertEquals(600_000L, TimeConstants.DEFAULT_PUZZLE_TIMEOUT_MS);
    }

    @Test
    @DisplayName("DEFAULT_THREAD_JOIN_TIMEOUT_MS equals 5 seconds")
    void testDefaultThreadJoinTimeout() {
        assertEquals(5 * TimeConstants.MILLIS_PER_SECOND, TimeConstants.DEFAULT_THREAD_JOIN_TIMEOUT_MS);
        assertEquals(5_000L, TimeConstants.DEFAULT_THREAD_JOIN_TIMEOUT_MS);
    }

    @Test
    @DisplayName("DEFAULT_THREAD_SLEEP_MS equals 1 second")
    void testDefaultThreadSleep() {
        assertEquals(1 * TimeConstants.MILLIS_PER_SECOND, TimeConstants.DEFAULT_THREAD_SLEEP_MS);
        assertEquals(1_000L, TimeConstants.DEFAULT_THREAD_SLEEP_MS);
    }

    @Test
    @DisplayName("SHORT_TIMEOUT_MS equals 100 milliseconds")
    void testShortTimeout() {
        assertEquals(100L, TimeConstants.SHORT_TIMEOUT_MS);
    }

    @Test
    @DisplayName("MEDIUM_TIMEOUT_MS equals 1 second")
    void testMediumTimeout() {
        assertEquals(1_000L, TimeConstants.MEDIUM_TIMEOUT_MS);
        assertEquals(TimeConstants.MILLIS_PER_SECOND, TimeConstants.MEDIUM_TIMEOUT_MS);
    }

    @Test
    @DisplayName("LONG_TIMEOUT_MS equals 30 seconds")
    void testLongTimeout() {
        assertEquals(30_000L, TimeConstants.LONG_TIMEOUT_MS);
        assertEquals(30 * TimeConstants.MILLIS_PER_SECOND, TimeConstants.LONG_TIMEOUT_MS);
    }

    // ========== Monitoring Interval Tests ==========

    @Test
    @DisplayName("AUTOSAVE_INTERVAL_MS equals 5 seconds")
    void testAutosaveInterval() {
        assertEquals(5_000L, TimeConstants.AUTOSAVE_INTERVAL_MS);
        assertEquals(5 * TimeConstants.MILLIS_PER_SECOND, TimeConstants.AUTOSAVE_INTERVAL_MS);
    }

    @Test
    @DisplayName("METRICS_INTERVAL_MS equals 1 second")
    void testMetricsInterval() {
        assertEquals(1_000L, TimeConstants.METRICS_INTERVAL_MS);
    }

    @Test
    @DisplayName("FILE_WATCH_POLL_INTERVAL_MS equals 1 second")
    void testFileWatchPollInterval() {
        assertEquals(1_000L, TimeConstants.FILE_WATCH_POLL_INTERVAL_MS);
    }

    @Test
    @DisplayName("WEBSOCKET_HEARTBEAT_MS equals 10 seconds")
    void testWebsocketHeartbeat() {
        assertEquals(10_000L, TimeConstants.WEBSOCKET_HEARTBEAT_MS);
        assertEquals(10 * TimeConstants.MILLIS_PER_SECOND, TimeConstants.WEBSOCKET_HEARTBEAT_MS);
    }

    // ========== Helper Method Tests - toSeconds ==========

    @Test
    @DisplayName("toSeconds converts 1000ms to 1.0s")
    void testToSeconds() {
        assertEquals(1.0, TimeConstants.toSeconds(1000L), 0.001);
    }

    @Test
    @DisplayName("toSeconds converts 0ms to 0.0s")
    void testToSecondsZero() {
        assertEquals(0.0, TimeConstants.toSeconds(0L), 0.001);
    }

    @Test
    @DisplayName("toSeconds converts 500ms to 0.5s")
    void testToSecondsHalf() {
        assertEquals(0.5, TimeConstants.toSeconds(500L), 0.001);
    }

    @Test
    @DisplayName("toSeconds converts 5000ms to 5.0s")
    void testToSecondsFive() {
        assertEquals(5.0, TimeConstants.toSeconds(5000L), 0.001);
    }

    // ========== Helper Method Tests - toMinutes ==========

    @Test
    @DisplayName("toMinutes converts 60000ms to 1.0min")
    void testToMinutes() {
        assertEquals(1.0, TimeConstants.toMinutes(60_000L), 0.001);
    }

    @Test
    @DisplayName("toMinutes converts 0ms to 0.0min")
    void testToMinutesZero() {
        assertEquals(0.0, TimeConstants.toMinutes(0L), 0.001);
    }

    @Test
    @DisplayName("toMinutes converts 30000ms to 0.5min")
    void testToMinutesHalf() {
        assertEquals(0.5, TimeConstants.toMinutes(30_000L), 0.001);
    }

    @Test
    @DisplayName("toMinutes converts 120000ms to 2.0min")
    void testToMinutesTwo() {
        assertEquals(2.0, TimeConstants.toMinutes(120_000L), 0.001);
    }

    // ========== Helper Method Tests - toHours ==========

    @Test
    @DisplayName("toHours converts 3600000ms to 1.0h")
    void testToHours() {
        assertEquals(1.0, TimeConstants.toHours(3_600_000L), 0.001);
    }

    @Test
    @DisplayName("toHours converts 0ms to 0.0h")
    void testToHoursZero() {
        assertEquals(0.0, TimeConstants.toHours(0L), 0.001);
    }

    @Test
    @DisplayName("toHours converts 1800000ms to 0.5h")
    void testToHoursHalf() {
        assertEquals(0.5, TimeConstants.toHours(1_800_000L), 0.001);
    }

    @Test
    @DisplayName("toHours converts 7200000ms to 2.0h")
    void testToHoursTwo() {
        assertEquals(2.0, TimeConstants.toHours(7_200_000L), 0.001);
    }

    // ========== Helper Method Tests - toMillis ==========

    @Test
    @DisplayName("toMillis converts 1s to 1000ms")
    void testToMillis() {
        assertEquals(1000L, TimeConstants.toMillis(1L));
    }

    @Test
    @DisplayName("toMillis converts 0s to 0ms")
    void testToMillisZero() {
        assertEquals(0L, TimeConstants.toMillis(0L));
    }

    @Test
    @DisplayName("toMillis converts 5s to 5000ms")
    void testToMillisFive() {
        assertEquals(5000L, TimeConstants.toMillis(5L));
    }

    @Test
    @DisplayName("toMillis converts 60s to 60000ms")
    void testToMillisSixty() {
        assertEquals(60_000L, TimeConstants.toMillis(60L));
    }

    // ========== Helper Method Tests - formatDuration ==========

    @Test
    @DisplayName("formatDuration formats milliseconds")
    void testFormatDurationMillis() {
        assertEquals("500ms", TimeConstants.formatDuration(500L));
        assertEquals("999ms", TimeConstants.formatDuration(999L));
    }

    @Test
    @DisplayName("formatDuration formats seconds")
    void testFormatDurationSeconds() {
        assertEquals("1s", TimeConstants.formatDuration(1_000L));
        assertEquals("5s", TimeConstants.formatDuration(5_000L));
        assertEquals("59s", TimeConstants.formatDuration(59_000L));
    }

    @Test
    @DisplayName("formatDuration formats minutes and seconds")
    void testFormatDurationMinutesSeconds() {
        assertEquals("1m 0s", TimeConstants.formatDuration(60_000L));
        assertEquals("1m 30s", TimeConstants.formatDuration(90_000L));
        assertEquals("2m 5s", TimeConstants.formatDuration(125_000L));
        assertEquals("5m 45s", TimeConstants.formatDuration(345_000L));
    }

    @Test
    @DisplayName("formatDuration formats hours, minutes and seconds")
    void testFormatDurationHours() {
        assertEquals("1h 0m 0s", TimeConstants.formatDuration(3_600_000L));
        assertEquals("1h 30m 0s", TimeConstants.formatDuration(5_400_000L));
        assertEquals("2h 15m 30s", TimeConstants.formatDuration(8_130_000L));
    }

    @Test
    @DisplayName("formatDuration handles zero")
    void testFormatDurationZero() {
        assertEquals("0ms", TimeConstants.formatDuration(0L));
    }

    @Test
    @DisplayName("formatDuration handles edge cases")
    void testFormatDurationEdgeCases() {
        assertEquals("1ms", TimeConstants.formatDuration(1L));
        assertEquals("1h 0m 1s", TimeConstants.formatDuration(3_601_000L));
        assertEquals("10h 30m 45s", TimeConstants.formatDuration(37_845_000L));
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Round-trip conversion: seconds -> millis -> seconds")
    void testRoundTripSecondsMillis() {
        long originalSeconds = 42L;
        long millis = TimeConstants.toMillis(originalSeconds);
        double backToSeconds = TimeConstants.toSeconds(millis);

        assertEquals(originalSeconds, backToSeconds, 0.001);
    }

    @Test
    @DisplayName("Timeout values are properly ordered")
    void testTimeoutOrdering() {
        assertTrue(TimeConstants.SHORT_TIMEOUT_MS < TimeConstants.MEDIUM_TIMEOUT_MS);
        assertTrue(TimeConstants.MEDIUM_TIMEOUT_MS < TimeConstants.LONG_TIMEOUT_MS);
        assertTrue(TimeConstants.LONG_TIMEOUT_MS < TimeConstants.DEFAULT_PUZZLE_TIMEOUT_MS);
    }

    @Test
    @DisplayName("All timeout values are positive")
    void testTimeoutValuesPositive() {
        assertTrue(TimeConstants.DEFAULT_PUZZLE_TIMEOUT_MS > 0);
        assertTrue(TimeConstants.DEFAULT_THREAD_JOIN_TIMEOUT_MS > 0);
        assertTrue(TimeConstants.DEFAULT_THREAD_SLEEP_MS > 0);
        assertTrue(TimeConstants.SHORT_TIMEOUT_MS > 0);
        assertTrue(TimeConstants.MEDIUM_TIMEOUT_MS > 0);
        assertTrue(TimeConstants.LONG_TIMEOUT_MS > 0);
        assertTrue(TimeConstants.AUTOSAVE_INTERVAL_MS > 0);
        assertTrue(TimeConstants.METRICS_INTERVAL_MS > 0);
        assertTrue(TimeConstants.FILE_WATCH_POLL_INTERVAL_MS > 0);
        assertTrue(TimeConstants.WEBSOCKET_HEARTBEAT_MS > 0);
    }

    @Test
    @DisplayName("Conversion constants are consistent")
    void testConversionConsistency() {
        // 1 hour = 60 minutes = 3600 seconds
        assertEquals(TimeConstants.MILLIS_PER_HOUR,
                    TimeConstants.MILLIS_PER_MINUTE * 60);
        assertEquals(TimeConstants.MILLIS_PER_MINUTE,
                    TimeConstants.MILLIS_PER_SECOND * 60);

        assertEquals(TimeConstants.SECONDS_PER_HOUR,
                    TimeConstants.SECONDS_PER_MINUTE * 60);
    }

    @Test
    @DisplayName("formatDuration consistency with conversion methods")
    void testFormatDurationConsistency() {
        long millis = 125_000L; // 2m 5s
        String formatted = TimeConstants.formatDuration(millis);

        double minutes = TimeConstants.toMinutes(millis);
        double seconds = TimeConstants.toSeconds(millis);

        assertTrue(formatted.contains("2m"));
        assertTrue(formatted.contains("5s"));
        assertEquals(2.0833, minutes, 0.01); // ~2.08 minutes
        assertEquals(125.0, seconds, 0.01);
    }
}
