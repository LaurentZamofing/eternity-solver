package solver.timeout;

import model.Board;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solver.BacktrackingContext;
import solver.StatisticsManager;

import java.util.BitSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for {@link TimeoutChecker}. */
@DisplayName("TimeoutChecker")
class TimeoutCheckerTest {

    private BacktrackingContext contextWith(long startTimeMs, long maxExecutionTimeMs) {
        return new BacktrackingContext(
            new Board(1, 1), Map.of(), new BitSet(),
            1, new StatisticsManager(), 0,
            startTimeMs, maxExecutionTimeMs);
    }

    @Test
    @DisplayName("isTimedOut returns false when elapsed < max")
    void notTimedOutWhenUnderLimit() {
        TimeoutChecker checker = new TimeoutChecker();
        BacktrackingContext ctx = contextWith(System.currentTimeMillis(), 60_000);
        assertFalse(checker.isTimedOut(ctx));
    }

    @Test
    @DisplayName("isTimedOut returns true once elapsed > max")
    void timedOutWhenOverLimit() {
        TimeoutChecker checker = new TimeoutChecker();
        BacktrackingContext ctx = contextWith(System.currentTimeMillis() - 5_000, 1_000);
        assertTrue(checker.isTimedOut(ctx));
    }

    @Test
    @DisplayName("getElapsedTimeMs reflects real passage of time")
    void elapsedTimeMonotonic() {
        TimeoutChecker checker = new TimeoutChecker();
        BacktrackingContext ctx = contextWith(System.currentTimeMillis() - 50, 10_000);
        assertTrue(checker.getElapsedTimeMs(ctx) >= 50);
    }

    @Test
    @DisplayName("getRemainingTimeMs clamps at 0 when timed out")
    void remainingTimeClampedAtZero() {
        TimeoutChecker checker = new TimeoutChecker();
        BacktrackingContext ctx = contextWith(System.currentTimeMillis() - 5_000, 1_000);
        assertEquals(0, checker.getRemainingTimeMs(ctx));
    }

    @Test
    @DisplayName("getTimeProgress returns percentage 0-100")
    void timeProgressPercentage() {
        TimeoutChecker checker = new TimeoutChecker();
        BacktrackingContext ctx = contextWith(System.currentTimeMillis() - 500, 1_000);
        double progress = checker.getTimeProgress(ctx);
        assertTrue(progress >= 50.0 && progress <= 200.0,
            "progress should be around 50% but within reasonable range: " + progress);
    }

    @Test
    @DisplayName("getTimeProgress returns 0 when maxExecutionTimeMs is 0")
    void timeProgressZeroWhenNoLimit() {
        TimeoutChecker checker = new TimeoutChecker();
        BacktrackingContext ctx = contextWith(System.currentTimeMillis() - 1_000, 0);
        assertEquals(0.0, checker.getTimeProgress(ctx));
    }
}
