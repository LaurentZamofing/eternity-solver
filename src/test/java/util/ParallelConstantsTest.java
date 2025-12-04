package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ParallelConstants utility class.
 * Tests parallel processing constants and helper methods.
 */
@DisplayName("ParallelConstants Tests")
public class ParallelConstantsTest {

    // ========== Constructor Tests ==========

    @Test
    @DisplayName("Constructor throws UnsupportedOperationException")
    void testConstructorThrowsException() {
        try {
            // Use reflection to access private constructor
            var constructor = ParallelConstants.class.getDeclaredConstructor();
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

    // ========== Thread Pool Configuration Tests ==========

    @Test
    @DisplayName("MIN_THREADS equals 4")
    void testMinThreads() {
        assertEquals(4, ParallelConstants.MIN_THREADS);
    }

    @Test
    @DisplayName("MAX_THREADS equals 32")
    void testMaxThreads() {
        assertEquals(32, ParallelConstants.MAX_THREADS);
    }

    @Test
    @DisplayName("MIN_THREADS is less than MAX_THREADS")
    void testThreadRangeValid() {
        assertTrue(ParallelConstants.MIN_THREADS < ParallelConstants.MAX_THREADS);
    }

    @Test
    @DisplayName("CORE_UTILIZATION_FACTOR equals 0.75")
    void testCoreUtilizationFactor() {
        assertEquals(0.75, ParallelConstants.CORE_UTILIZATION_FACTOR, 0.001);
    }

    @Test
    @DisplayName("CORE_UTILIZATION_FACTOR is between 0 and 1")
    void testCoreUtilizationFactorRange() {
        assertTrue(ParallelConstants.CORE_UTILIZATION_FACTOR > 0.0);
        assertTrue(ParallelConstants.CORE_UTILIZATION_FACTOR <= 1.0);
    }

    @Test
    @DisplayName("THREAD_KEEP_ALIVE_SECONDS equals 60")
    void testThreadKeepAlive() {
        assertEquals(60L, ParallelConstants.THREAD_KEEP_ALIVE_SECONDS);
    }

    // ========== Work Stealing Tests ==========

    @Test
    @DisplayName("WORK_STEALING_DEPTH equals 2")
    void testWorkStealingDepth() {
        assertEquals(2, ParallelConstants.WORK_STEALING_DEPTH);
    }

    @Test
    @DisplayName("MIN_WORK_SIZE equals 10")
    void testMinWorkSize() {
        assertEquals(10, ParallelConstants.MIN_WORK_SIZE);
    }

    @Test
    @DisplayName("LOAD_BALANCE_INTERVAL_MS equals 1000")
    void testLoadBalanceInterval() {
        assertEquals(1000L, ParallelConstants.LOAD_BALANCE_INTERVAL_MS);
    }

    // ========== Configuration Rotation Tests ==========

    @Test
    @DisplayName("NUM_SEARCH_CONFIGURATIONS equals 4")
    void testNumSearchConfigurations() {
        assertEquals(4, ParallelConstants.NUM_SEARCH_CONFIGURATIONS);
    }

    @Test
    @DisplayName("TIMEOUT_PER_CONFIG_MINUTES equals 10")
    void testTimeoutPerConfig() {
        assertEquals(10, ParallelConstants.TIMEOUT_PER_CONFIG_MINUTES);
    }

    @Test
    @DisplayName("CONFIG_SWITCH_SLEEP_MS equals 1000")
    void testConfigSwitchSleep() {
        assertEquals(1000L, ParallelConstants.CONFIG_SWITCH_SLEEP_MS);
    }

    // ========== Diversification Tests ==========

    @Test
    @DisplayName("NUM_CORNER_POSITIONS equals 4")
    void testNumCornerPositions() {
        assertEquals(4, ParallelConstants.NUM_CORNER_POSITIONS);
    }

    @Test
    @DisplayName("BORDER_PRIORITY_DEPTH equals 10")
    void testBorderPriorityDepth() {
        assertEquals(10, ParallelConstants.BORDER_PRIORITY_DEPTH);
    }

    // ========== Synchronization Tests ==========

    @Test
    @DisplayName("TASK_QUEUE_CAPACITY equals 1000")
    void testTaskQueueCapacity() {
        assertEquals(1000, ParallelConstants.TASK_QUEUE_CAPACITY);
    }

    @Test
    @DisplayName("MAX_PENDING_TASKS_PER_THREAD equals 10")
    void testMaxPendingTasksPerThread() {
        assertEquals(10, ParallelConstants.MAX_PENDING_TASKS_PER_THREAD);
    }

    @Test
    @DisplayName("SPIN_WAIT_ITERATIONS equals 100")
    void testSpinWaitIterations() {
        assertEquals(100, ParallelConstants.SPIN_WAIT_ITERATIONS);
    }

    // ========== Helper Method Tests - getOptimalThreadCount ==========

    @Test
    @DisplayName("getOptimalThreadCount returns value in valid range")
    void testGetOptimalThreadCount() {
        int threads = ParallelConstants.getOptimalThreadCount();

        assertTrue(threads >= ParallelConstants.MIN_THREADS,
                  "Thread count should be at least MIN_THREADS");
        assertTrue(threads <= ParallelConstants.MAX_THREADS,
                  "Thread count should not exceed MAX_THREADS");
    }

    @Test
    @DisplayName("getOptimalThreadCount is consistent across calls")
    void testGetOptimalThreadCountConsistent() {
        int threads1 = ParallelConstants.getOptimalThreadCount();
        int threads2 = ParallelConstants.getOptimalThreadCount();

        assertEquals(threads1, threads2,
                    "Should return same value on repeated calls");
    }

    @Test
    @DisplayName("getOptimalThreadCount respects MIN_THREADS on single-core")
    void testGetOptimalThreadCountMinimum() {
        // Even if we have 1 core, should return at least MIN_THREADS
        int threads = ParallelConstants.getOptimalThreadCount();
        assertTrue(threads >= ParallelConstants.MIN_THREADS);
    }

    // ========== Helper Method Tests - getThreadCount ==========

    @Test
    @DisplayName("getThreadCount with 0.5 factor")
    void testGetThreadCountHalfUtilization() {
        int threads = ParallelConstants.getThreadCount(0.5);

        assertTrue(threads >= ParallelConstants.MIN_THREADS);
        assertTrue(threads <= ParallelConstants.MAX_THREADS);
    }

    @Test
    @DisplayName("getThreadCount with 1.0 factor")
    void testGetThreadCountFullUtilization() {
        int threads = ParallelConstants.getThreadCount(1.0);

        assertTrue(threads >= ParallelConstants.MIN_THREADS);
        assertTrue(threads <= ParallelConstants.MAX_THREADS);
    }

    @Test
    @DisplayName("getThreadCount with 0.25 factor")
    void testGetThreadCountQuarterUtilization() {
        int threads = ParallelConstants.getThreadCount(0.25);

        assertTrue(threads >= ParallelConstants.MIN_THREADS);
        assertTrue(threads <= ParallelConstants.MAX_THREADS);
    }

    @Test
    @DisplayName("getThreadCount throws exception for factor 0.0")
    void testGetThreadCountZeroFactor() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParallelConstants.getThreadCount(0.0);
        });
    }

    @Test
    @DisplayName("getThreadCount throws exception for negative factor")
    void testGetThreadCountNegativeFactor() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParallelConstants.getThreadCount(-0.5);
        });
    }

    @Test
    @DisplayName("getThreadCount throws exception for factor > 1.0")
    void testGetThreadCountFactorTooHigh() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParallelConstants.getThreadCount(1.5);
        });
    }

    @Test
    @DisplayName("getThreadCount with very small valid factor")
    void testGetThreadCountSmallFactor() {
        int threads = ParallelConstants.getThreadCount(0.01);
        assertEquals(ParallelConstants.MIN_THREADS, threads,
                    "Very small factor should result in MIN_THREADS");
    }

    @Test
    @DisplayName("Higher utilization factor gives more or equal threads")
    void testGetThreadCountFactorComparison() {
        int threads50 = ParallelConstants.getThreadCount(0.5);
        int threads75 = ParallelConstants.getThreadCount(0.75);
        int threads100 = ParallelConstants.getThreadCount(1.0);

        assertTrue(threads75 >= threads50,
                  "0.75 factor should give >= threads than 0.5");
        assertTrue(threads100 >= threads75,
                  "1.0 factor should give >= threads than 0.75");
    }

    // ========== Helper Method Tests - shouldEnableWorkStealing ==========

    @Test
    @DisplayName("shouldEnableWorkStealing is false at depth 0")
    void testShouldEnableWorkStealingDepthZero() {
        assertFalse(ParallelConstants.shouldEnableWorkStealing(0));
    }

    @Test
    @DisplayName("shouldEnableWorkStealing is false at depth 1")
    void testShouldEnableWorkStealingDepthOne() {
        assertFalse(ParallelConstants.shouldEnableWorkStealing(1));
    }

    @Test
    @DisplayName("shouldEnableWorkStealing is true at WORK_STEALING_DEPTH")
    void testShouldEnableWorkStealingAtThreshold() {
        assertTrue(ParallelConstants.shouldEnableWorkStealing(
            ParallelConstants.WORK_STEALING_DEPTH));
    }

    @Test
    @DisplayName("shouldEnableWorkStealing is true at depth 10")
    void testShouldEnableWorkStealingDepthTen() {
        assertTrue(ParallelConstants.shouldEnableWorkStealing(10));
    }

    @Test
    @DisplayName("shouldEnableWorkStealing is true at depth 100")
    void testShouldEnableWorkStealingDepthHundred() {
        assertTrue(ParallelConstants.shouldEnableWorkStealing(100));
    }

    @Test
    @DisplayName("shouldEnableWorkStealing threshold behavior")
    void testShouldEnableWorkStealingThreshold() {
        // Just below threshold
        assertFalse(ParallelConstants.shouldEnableWorkStealing(
            ParallelConstants.WORK_STEALING_DEPTH - 1));

        // At threshold
        assertTrue(ParallelConstants.shouldEnableWorkStealing(
            ParallelConstants.WORK_STEALING_DEPTH));

        // Above threshold
        assertTrue(ParallelConstants.shouldEnableWorkStealing(
            ParallelConstants.WORK_STEALING_DEPTH + 1));
    }

    // ========== Helper Method Tests - shouldPrioritizeBorder ==========

    @Test
    @DisplayName("shouldPrioritizeBorder is true at depth 0")
    void testShouldPrioritizeBorderDepthZero() {
        assertTrue(ParallelConstants.shouldPrioritizeBorder(0));
    }

    @Test
    @DisplayName("shouldPrioritizeBorder is true at depth 5")
    void testShouldPrioritizeBorderDepthFive() {
        assertTrue(ParallelConstants.shouldPrioritizeBorder(5));
    }

    @Test
    @DisplayName("shouldPrioritizeBorder is true at depth 9")
    void testShouldPrioritizeBorderDepthNine() {
        assertTrue(ParallelConstants.shouldPrioritizeBorder(9));
    }

    @Test
    @DisplayName("shouldPrioritizeBorder is false at BORDER_PRIORITY_DEPTH")
    void testShouldPrioritizeBorderAtThreshold() {
        assertFalse(ParallelConstants.shouldPrioritizeBorder(
            ParallelConstants.BORDER_PRIORITY_DEPTH));
    }

    @Test
    @DisplayName("shouldPrioritizeBorder is false at depth 20")
    void testShouldPrioritizeBorderDepthTwenty() {
        assertFalse(ParallelConstants.shouldPrioritizeBorder(20));
    }

    @Test
    @DisplayName("shouldPrioritizeBorder threshold behavior")
    void testShouldPrioritizeBorderThreshold() {
        // Just below threshold
        assertTrue(ParallelConstants.shouldPrioritizeBorder(
            ParallelConstants.BORDER_PRIORITY_DEPTH - 1));

        // At threshold
        assertFalse(ParallelConstants.shouldPrioritizeBorder(
            ParallelConstants.BORDER_PRIORITY_DEPTH));

        // Above threshold
        assertFalse(ParallelConstants.shouldPrioritizeBorder(
            ParallelConstants.BORDER_PRIORITY_DEPTH + 1));
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("All constant values are positive")
    void testAllConstantsPositive() {
        assertTrue(ParallelConstants.MIN_THREADS > 0);
        assertTrue(ParallelConstants.MAX_THREADS > 0);
        assertTrue(ParallelConstants.CORE_UTILIZATION_FACTOR > 0);
        assertTrue(ParallelConstants.THREAD_KEEP_ALIVE_SECONDS > 0);
        assertTrue(ParallelConstants.WORK_STEALING_DEPTH >= 0);
        assertTrue(ParallelConstants.MIN_WORK_SIZE > 0);
        assertTrue(ParallelConstants.LOAD_BALANCE_INTERVAL_MS > 0);
        assertTrue(ParallelConstants.NUM_SEARCH_CONFIGURATIONS > 0);
        assertTrue(ParallelConstants.TIMEOUT_PER_CONFIG_MINUTES > 0);
        assertTrue(ParallelConstants.CONFIG_SWITCH_SLEEP_MS > 0);
        assertTrue(ParallelConstants.NUM_CORNER_POSITIONS > 0);
        assertTrue(ParallelConstants.BORDER_PRIORITY_DEPTH > 0);
        assertTrue(ParallelConstants.TASK_QUEUE_CAPACITY > 0);
        assertTrue(ParallelConstants.MAX_PENDING_TASKS_PER_THREAD > 0);
        assertTrue(ParallelConstants.SPIN_WAIT_ITERATIONS > 0);
    }

    @Test
    @DisplayName("Work stealing and border priority are complementary")
    void testWorkStealingBorderPriorityComplementary() {
        // At early depths, prioritize border, don't use work stealing
        for (int depth = 0; depth < 2; depth++) {
            if (ParallelConstants.shouldPrioritizeBorder(depth)) {
                // Early depths may or may not enable work stealing
                // Just verify the methods work
                ParallelConstants.shouldEnableWorkStealing(depth);
            }
        }

        // At later depths, don't prioritize border
        for (int depth = 20; depth < 30; depth++) {
            assertFalse(ParallelConstants.shouldPrioritizeBorder(depth));
            assertTrue(ParallelConstants.shouldEnableWorkStealing(depth));
        }
    }

    @Test
    @DisplayName("Queue capacity exceeds max pending tasks")
    void testQueueCapacityVsPendingTasks() {
        int maxPendingTotal = ParallelConstants.MAX_PENDING_TASKS_PER_THREAD
                            * ParallelConstants.MAX_THREADS;

        assertTrue(ParallelConstants.TASK_QUEUE_CAPACITY >= maxPendingTotal,
                  "Queue capacity should accommodate max pending tasks from all threads");
    }

    @Test
    @DisplayName("Configuration rotation is reasonable")
    void testConfigurationRotation() {
        // Should have multiple configurations to try
        assertTrue(ParallelConstants.NUM_SEARCH_CONFIGURATIONS >= 2);

        // Timeout per config should be reasonable
        assertTrue(ParallelConstants.TIMEOUT_PER_CONFIG_MINUTES >= 1);
        assertTrue(ParallelConstants.TIMEOUT_PER_CONFIG_MINUTES <= 60);
    }

    @Test
    @DisplayName("getOptimalThreadCount matches expected calculation")
    void testGetOptimalThreadCountCalculation() {
        int availableCores = Runtime.getRuntime().availableProcessors();
        int expected = Math.max(
            ParallelConstants.MIN_THREADS,
            (int) (availableCores * ParallelConstants.CORE_UTILIZATION_FACTOR)
        );
        expected = Math.min(expected, ParallelConstants.MAX_THREADS);

        int actual = ParallelConstants.getOptimalThreadCount();

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Thread count with default factor matches optimal")
    void testThreadCountDefaultFactor() {
        int optimal = ParallelConstants.getOptimalThreadCount();
        int withDefaultFactor = ParallelConstants.getThreadCount(
            ParallelConstants.CORE_UTILIZATION_FACTOR);

        assertEquals(optimal, withDefaultFactor);
    }
}
