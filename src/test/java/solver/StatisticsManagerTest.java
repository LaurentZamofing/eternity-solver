package solver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StatisticsManager class.
 * Tests statistics tracking, timing, and progress estimation.
 */
@DisplayName("StatisticsManager Tests")
public class StatisticsManagerTest {

    private StatisticsManager stats;

    @BeforeEach
    public void setUp() {
        stats = new StatisticsManager();
    }

    @Test
    @DisplayName("Initialize with zero values")
    public void testInitialValues() {
        assertEquals(0, stats.recursiveCalls);
        assertEquals(0, stats.placements);
        assertEquals(0, stats.backtracks);
        assertEquals(0, stats.singletonsFound);
        assertEquals(0, stats.singletonsPlaced);
        assertEquals(0, stats.deadEndsDetected);
        assertEquals(0, stats.fitChecks);
        assertEquals(0, stats.forwardCheckRejects);
        assertEquals(0, stats.previousTimeOffset);
    }

    @Test
    @DisplayName("Start timer sets start time")
    public void testStartTimer() {
        long before = System.currentTimeMillis();
        stats.start();
        long after = System.currentTimeMillis();

        assertTrue(stats.startTime >= before && stats.startTime <= after,
                "Start time should be set to current time");
    }

    @Test
    @DisplayName("Start timer with offset")
    public void testStartTimerWithOffset() {
        long offset = 5000; // 5 seconds
        stats.start(offset);

        assertEquals(offset, stats.previousTimeOffset, "Offset should be stored");
        assertTrue(stats.startTime > 0, "Start time should be set");
    }

    @Test
    @DisplayName("End timer sets end time")
    public void testEndTimer() {
        stats.start();
        try {
            Thread.sleep(10); // Wait a bit
        } catch (InterruptedException e) {
            // Ignore
        }
        stats.end();

        assertTrue(stats.endTime > stats.startTime, "End time should be after start time");
    }

    @Test
    @DisplayName("Get elapsed time without offset")
    public void testGetElapsedTimeMs() {
        stats.start();
        try {
            Thread.sleep(50); // Wait 50ms
        } catch (InterruptedException e) {
            // Ignore
        }
        stats.end();

        long elapsed = stats.getElapsedTimeMs();
        assertTrue(elapsed >= 50, "Elapsed time should be at least 50ms");
        assertTrue(elapsed < 200, "Elapsed time should be less than 200ms (sanity check)");
    }

    @Test
    @DisplayName("Get elapsed time with offset")
    public void testGetElapsedTimeMsWithOffset() {
        long offset = 1000; // 1 second offset
        stats.start(offset);
        try {
            Thread.sleep(50); // Wait 50ms
        } catch (InterruptedException e) {
            // Ignore
        }
        stats.end();

        long elapsed = stats.getElapsedTimeMs();
        assertTrue(elapsed >= 1050, "Elapsed time should include offset (1000ms + 50ms)");
    }

    @Test
    @DisplayName("Get elapsed time in seconds")
    public void testGetElapsedTimeSec() {
        stats.start();
        try {
            Thread.sleep(100); // Wait 100ms
        } catch (InterruptedException e) {
            // Ignore
        }
        stats.end();

        double elapsed = stats.getElapsedTimeSec();
        assertTrue(elapsed >= 0.1, "Elapsed time should be at least 0.1 seconds");
        assertTrue(elapsed < 1.0, "Elapsed time should be less than 1 second (sanity check)");
    }

    @Test
    @DisplayName("Get elapsed time while running (no end)")
    public void testGetElapsedTimeWhileRunning() {
        stats.start();
        try {
            Thread.sleep(50); // Wait 50ms
        } catch (InterruptedException e) {
            // Ignore
        }
        // Don't call end() - should still calculate elapsed time

        long elapsed = stats.getElapsedTimeMs();
        assertTrue(elapsed >= 50, "Should calculate elapsed time even without calling end()");
    }

    @Test
    @DisplayName("Register depth options")
    public void testRegisterDepthOptions() {
        // Should not throw exception
        stats.registerDepthOptions(0, 10);
        stats.registerDepthOptions(1, 20);
        stats.registerDepthOptions(2, 15);
    }

    @Test
    @DisplayName("Register depth options multiple times for same depth")
    public void testRegisterDepthOptionsMultipleTimes() {
        stats.registerDepthOptions(0, 10);
        // Registering again should not change the first registration
        stats.registerDepthOptions(0, 20);

        // Should not throw exception
    }

    @Test
    @DisplayName("Increment depth progress")
    public void testIncrementDepthProgress() {
        stats.registerDepthOptions(0, 10);

        // Should not throw exception
        stats.incrementDepthProgress(0);
        stats.incrementDepthProgress(0);
        stats.incrementDepthProgress(0);
    }

    @Test
    @DisplayName("Increment depth progress for unregistered depth")
    public void testIncrementDepthProgressUnregistered() {
        // Should not throw exception even if depth not registered
        stats.incrementDepthProgress(99);
    }

    @Test
    @DisplayName("Get progress percentage with no data")
    public void testGetProgressPercentageNoData() {
        double progress = stats.getProgressPercentage();
        assertEquals(0.0, progress, "Progress should be 0% with no data");
    }

    @Test
    @DisplayName("Get progress percentage at depth 0")
    public void testGetProgressPercentageDepth0() {
        stats.registerDepthOptions(0, 10);

        // No progress yet
        double progress = stats.getProgressPercentage();
        assertEquals(0.0, progress, 0.01, "Progress should be 0% at start");

        // Advance 5 of 10 options
        for (int i = 0; i < 5; i++) {
            stats.incrementDepthProgress(0);
        }

        progress = stats.getProgressPercentage();
        assertTrue(progress > 40.0 && progress < 60.0,
                "Progress should be around 50% (5/10 options)");
    }

    @Test
    @DisplayName("Get progress percentage at multiple depths")
    public void testGetProgressPercentageMultipleDepths() {
        // Depth 0: 10 options, complete 5 (50%)
        stats.registerDepthOptions(0, 10);
        for (int i = 0; i < 5; i++) {
            stats.incrementDepthProgress(0);
        }

        // Depth 1: 20 options, complete 10 (50%)
        stats.registerDepthOptions(1, 20);
        for (int i = 0; i < 10; i++) {
            stats.incrementDepthProgress(1);
        }

        double progress = stats.getProgressPercentage();
        assertTrue(progress > 40.0, "Progress should be > 40%");
        assertTrue(progress <= 100.0, "Progress should not exceed 100%");
    }

    @Test
    @DisplayName("Get progress percentage caps at 100%")
    public void testGetProgressPercentageCapped() {
        stats.registerDepthOptions(0, 10);

        // Complete all options and more
        for (int i = 0; i < 15; i++) {
            stats.incrementDepthProgress(0);
        }

        double progress = stats.getProgressPercentage();
        assertTrue(progress <= 100.0, "Progress should not exceed 100%");
    }

    @Test
    @DisplayName("Increment counters")
    public void testIncrementCounters() {
        stats.recursiveCalls++;
        stats.placements++;
        stats.backtracks++;
        stats.singletonsFound++;
        stats.singletonsPlaced++;
        stats.deadEndsDetected++;
        stats.fitChecks++;
        stats.forwardCheckRejects++;

        assertEquals(1, stats.recursiveCalls);
        assertEquals(1, stats.placements);
        assertEquals(1, stats.backtracks);
        assertEquals(1, stats.singletonsFound);
        assertEquals(1, stats.singletonsPlaced);
        assertEquals(1, stats.deadEndsDetected);
        assertEquals(1, stats.fitChecks);
        assertEquals(1, stats.forwardCheckRejects);
    }

    @Test
    @DisplayName("Print methods don't throw exceptions")
    public void testPrintMethods() {
        stats.start();
        stats.recursiveCalls = 100;
        stats.placements = 50;
        stats.backtracks = 25;
        stats.end();

        // Should not throw exceptions
        assertDoesNotThrow(() -> stats.print());
        assertDoesNotThrow(() -> stats.printCompact());
    }

    @Test
    @DisplayName("Print with no timing data")
    public void testPrintWithNoTiming() {
        stats.recursiveCalls = 100;

        // Should not throw exception even without start/end
        assertDoesNotThrow(() -> stats.print());
        assertDoesNotThrow(() -> stats.printCompact());
    }

    @Test
    @DisplayName("Complex progress tracking scenario")
    public void testComplexProgressTracking() {
        // Simulate a solving scenario with 5 depths
        for (int depth = 0; depth < 5; depth++) {
            int numOptions = 10 + depth * 5; // Increasing options per depth
            stats.registerDepthOptions(depth, numOptions);

            // Complete half of the options at each depth
            for (int i = 0; i < numOptions / 2; i++) {
                stats.incrementDepthProgress(depth);
            }
        }

        double progress = stats.getProgressPercentage();
        assertTrue(progress > 0.0 && progress <= 100.0,
                "Progress should be between 0% and 100%");
    }
}
