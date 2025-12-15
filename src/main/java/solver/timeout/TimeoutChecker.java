package solver.timeout;

import solver.BacktrackingContext;

/**
 * Checks for timeout during solver execution.
 *
 * <h2>Purpose</h2>
 * Separates timeout checking logic from core algorithm. Allows:
 * <ul>
 *   <li>Graceful termination after time limit</li>
 *   <li>Saving stable configuration (not mid-backtrack)</li>
 *   <li>Resume from exact state without duplicate work</li>
 * </ul>
 *
 * <h2>Design Principle</h2>
 * Timeout is only checked <b>AFTER successful placement</b>, not during backtracking.
 * This ensures:
 * <pre>
 * ✓ Saved state always contains a stable configuration
 * ✓ Resume continues from piece N+1 (pieces 1..N implicitly tested)
 * ✓ No duplicate work on resume
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>
 * TimeoutChecker checker = new TimeoutChecker();
 *
 * // After successful placement
 * if (checker.isTimedOut(context)) {
 *     // Stop exploration, save current state
 *     return false;
 * }
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class TimeoutChecker {

    /**
     * Checks if execution has exceeded the maximum time limit.
     *
     * <p><b>Important:</b> Only call this AFTER successful placement,
     * never during backtracking or validation failures.</p>
     *
     * @param context Backtracking context with timing information
     * @return true if timeout exceeded, false otherwise
     */
    public boolean isTimedOut(BacktrackingContext context) {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - context.startTimeMs;
        return elapsedTime > context.maxExecutionTimeMs;
    }

    /**
     * Gets elapsed time in milliseconds.
     *
     * @param context Backtracking context
     * @return Elapsed time since start
     */
    public long getElapsedTimeMs(BacktrackingContext context) {
        return System.currentTimeMillis() - context.startTimeMs;
    }

    /**
     * Gets remaining time in milliseconds.
     *
     * @param context Backtracking context
     * @return Remaining time until timeout (0 if already timed out)
     */
    public long getRemainingTimeMs(BacktrackingContext context) {
        long elapsed = getElapsedTimeMs(context);
        long remaining = context.maxExecutionTimeMs - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Calculates progress as percentage (0.0 to 100.0).
     *
     * @param context Backtracking context
     * @return Percentage of time elapsed
     */
    public double getTimeProgress(BacktrackingContext context) {
        long elapsed = getElapsedTimeMs(context);
        if (context.maxExecutionTimeMs <= 0) {
            return 0.0;
        }
        return (elapsed * 100.0) / context.maxExecutionTimeMs;
    }
}
