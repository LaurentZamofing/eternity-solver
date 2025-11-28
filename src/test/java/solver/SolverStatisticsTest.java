package solver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SolverStatistics class.
 * Tests statistics tracking, timing, and progress estimation.
 */
@DisplayName("SolverStatistics Tests")
public class SolverStatisticsTest {

    private SolverStatistics stats;

    @BeforeEach
    public void setUp() {
        stats = new SolverStatistics();
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
    public void testGetElapsedTime() {
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
    public void testGetElapsedTimeWithOffset() {
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

        double elapsedSec = stats.getElapsedTimeSec();
        assertTrue(elapsedSec >= 0.1, "Elapsed time should be at least 0.1 seconds");
        assertTrue(elapsedSec < 1.0, "Elapsed time should be less than 1 second (sanity check)");
    }

    @Test
    @DisplayName("Increment recursive calls")
    public void testIncrementRecursiveCalls() {
        assertEquals(0, stats.recursiveCalls);

        stats.recursiveCalls++;
        assertEquals(1, stats.recursiveCalls);

        stats.recursiveCalls += 99;
        assertEquals(100, stats.recursiveCalls);
    }

    @Test
    @DisplayName("Increment placements")
    public void testIncrementPlacements() {
        stats.placements++;
        assertEquals(1, stats.placements);
    }

    @Test
    @DisplayName("Increment backtracks")
    public void testIncrementBacktracks() {
        stats.backtracks++;
        assertEquals(1, stats.backtracks);
    }

    @Test
    @DisplayName("Increment singletons found")
    public void testIncrementSingletonsFound() {
        stats.singletonsFound++;
        assertEquals(1, stats.singletonsFound);
    }

    @Test
    @DisplayName("Increment singletons placed")
    public void testIncrementSingletonsPlaced() {
        stats.singletonsPlaced++;
        assertEquals(1, stats.singletonsPlaced);
    }

    @Test
    @DisplayName("Increment dead ends detected")
    public void testIncrementDeadEnds() {
        stats.deadEndsDetected++;
        assertEquals(1, stats.deadEndsDetected);
    }

    @Test
    @DisplayName("Increment fit checks")
    public void testIncrementFitChecks() {
        stats.fitChecks++;
        assertEquals(1, stats.fitChecks);
    }

    @Test
    @DisplayName("Increment forward check rejects")
    public void testIncrementForwardCheckRejects() {
        stats.forwardCheckRejects++;
        assertEquals(1, stats.forwardCheckRejects);
    }

    @Test
    @DisplayName("Register depth options")
    public void testRegisterDepthOptions() {
        assertDoesNotThrow(() -> {
            stats.registerDepthOptions(0, 10);
            stats.registerDepthOptions(1, 20);
            stats.registerDepthOptions(2, 15);
        });
    }

    @Test
    @DisplayName("Increment depth progress")
    public void testIncrementDepthProgress() {
        stats.registerDepthOptions(0, 10);

        assertDoesNotThrow(() -> {
            stats.incrementDepthProgress(0);
            stats.incrementDepthProgress(0);
            stats.incrementDepthProgress(0);
        });
    }

    @Test
    @DisplayName("Get progress percentage initially returns 0")
    public void testGetProgressPercentageInitial() {
        double progress = stats.getProgressPercentage();
        assertEquals(0.0, progress, "Initial progress should be 0%");
    }

    @Test
    @DisplayName("Get progress percentage at single depth")
    public void testGetProgressPercentageSingleDepth() {
        stats.registerDepthOptions(0, 10);

        // Increment 5 times (50% of 10 options)
        for (int i = 0; i < 5; i++) {
            stats.incrementDepthProgress(0);
        }

        double progress = stats.getProgressPercentage();
        assertTrue(progress > 40.0 && progress <= 60.0,
                "Progress should be around 50% (5/10)");
    }

    @Test
    @DisplayName("Get progress percentage at multiple depths")
    public void testGetProgressPercentageMultipleDepths() {
        stats.registerDepthOptions(0, 10);
        // Complete depth 0 (increment 10 times to reach 9, which is >= totalOptions-1)
        for (int i = 0; i < 10; i++) {
            stats.incrementDepthProgress(0);
        }

        stats.registerDepthOptions(1, 20);
        // Halfway through depth 1
        for (int i = 0; i < 10; i++) {
            stats.incrementDepthProgress(1);
        }

        double progress = stats.getProgressPercentage();
        // Depth 0 is complete, depth 1 is at 50%
        assertTrue(progress > 40.0 && progress <= 60.0,
                "Progress should reflect depth 1 completion (50%)");
    }

    @Test
    @DisplayName("Get progress percentage caps at 100%")
    public void testGetProgressPercentageMax() {
        // Complete 5 depths (the max tracked)
        for (int d = 0; d < 5; d++) {
            stats.registerDepthOptions(d, 10);
            for (int i = 0; i < 10; i++) {
                stats.incrementDepthProgress(d);
            }
        }

        double progress = stats.getProgressPercentage();
        assertEquals(100.0, progress, "Progress should cap at 100% when max depths completed");
    }

    @Test
    @DisplayName("Print statistics doesn't throw exceptions")
    public void testPrintStatistics() {
        stats.start();
        stats.recursiveCalls = 1000;
        stats.placements = 500;
        stats.backtracks = 300;
        stats.end();

        assertDoesNotThrow(() -> {
            stats.print();
        });
    }

    @Test
    @DisplayName("Print compact statistics doesn't throw exceptions")
    public void testPrintCompactStatistics() {
        stats.start();
        stats.recursiveCalls = 1000;
        stats.placements = 500;
        stats.backtracks = 300;
        stats.end();

        assertDoesNotThrow(() -> {
            stats.printCompact();
        });
    }

    @Test
    @DisplayName("Elapsed time continues if not ended")
    public void testElapsedTimeWithoutEnd() {
        stats.start();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // Ignore
        }

        // Don't call end(), should still calculate elapsed time
        long elapsed = stats.getElapsedTimeMs();
        assertTrue(elapsed >= 50, "Elapsed time should work even without calling end()");
    }

    @Test
    @DisplayName("Multiple stat increments work correctly")
    public void testMultipleIncrements() {
        for (int i = 0; i < 100; i++) {
            stats.recursiveCalls++;
            stats.placements++;
        }

        assertEquals(100, stats.recursiveCalls);
        assertEquals(100, stats.placements);
    }

    @Test
    @DisplayName("Progress tracking with uneven depth completion")
    public void testProgressTrackingUnevenDepths() {
        stats.registerDepthOptions(0, 5);
        stats.registerDepthOptions(1, 10);
        stats.registerDepthOptions(2, 15);

        // Complete depth 0
        for (int i = 0; i < 5; i++) {
            stats.incrementDepthProgress(0);
        }

        // Partially complete depth 1 (3 of 10)
        for (int i = 0; i < 3; i++) {
            stats.incrementDepthProgress(1);
        }

        double progress = stats.getProgressPercentage();
        // Should show progress at depth 1 (30%)
        assertTrue(progress > 20.0 && progress < 40.0,
                "Progress should reflect partial completion of depth 1");
    }
}
