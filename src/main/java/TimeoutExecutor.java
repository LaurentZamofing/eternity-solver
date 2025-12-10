// TimeoutExecutor.java (moved to default package for MainSequential access)

import util.SolverLogger;
import util.TimeConstants;

import java.util.concurrent.Callable;

/**
 * Generic timeout execution utility.
 * Executes tasks with a timeout, interrupting if time limit is exceeded.
 */
public class TimeoutExecutor {

    /**
     * Result of timeout execution.
     *
     * @param <T> Result type
     */
    public static class TimeoutResult<T> {
        public final boolean timedOut;
        public final T result;
        public final Exception exception;

        private TimeoutResult(boolean timedOut, T result, Exception exception) {
            this.timedOut = timedOut;
            this.result = result;
            this.exception = exception;
        }

        public static <T> TimeoutResult<T> success(T result) {
            return new TimeoutResult<>(false, result, null);
        }

        public static <T> TimeoutResult<T> timeout() {
            return new TimeoutResult<>(true, null, null);
        }

        public static <T> TimeoutResult<T> error(Exception exception) {
            return new TimeoutResult<>(false, null, exception);
        }

        public boolean isSuccess() {
            return !timedOut && exception == null;
        }
    }

    /**
     * Execute a task with a timeout.
     * If the task exceeds the timeout, the thread is interrupted.
     *
     * @param task Task to execute
     * @param timeoutMs Timeout in milliseconds
     * @param <T> Result type
     * @return TimeoutResult with outcome
     */
    public <T> TimeoutResult<T> executeWithTimeout(Callable<T> task, long timeoutMs) {
        final Object[] resultHolder = {null};
        final Exception[] exceptionHolder = {null};

        Thread workerThread = new Thread(() -> {
            try {
                resultHolder[0] = task.call();
            } catch (Exception e) {
                exceptionHolder[0] = e;
            }
        });

        workerThread.start();

        try {
            workerThread.join(timeoutMs);

            if (workerThread.isAlive()) {
                // Timeout reached - interrupt the thread
                SolverLogger.info("\n  → ⏱ Timeout reached");
                SolverLogger.info("  → Interrupting task and saving state...");
                workerThread.interrupt();
                workerThread.join(TimeConstants.DEFAULT_THREAD_JOIN_TIMEOUT_MS);

                if (workerThread.isAlive()) {
                    SolverLogger.info("  → Forced stop of worker thread");
                }

                return TimeoutResult.timeout();
            }

            if (exceptionHolder[0] != null) {
                return TimeoutResult.error(exceptionHolder[0]);
            }

            @SuppressWarnings("unchecked")
            T result = (T) resultHolder[0];
            return TimeoutResult.success(result);

        } catch (InterruptedException e) {
            SolverLogger.info("  → Main thread interrupted");
            workerThread.interrupt();
            return TimeoutResult.error(e);
        }
    }

    /**
     * Execute a boolean-returning task with a timeout.
     * Convenience method for tasks that return boolean.
     *
     * @param task Task to execute
     * @param timeoutMs Timeout in milliseconds
     * @return TimeoutResult with boolean result
     */
    public TimeoutResult<Boolean> executeBooleanWithTimeout(Callable<Boolean> task, long timeoutMs) {
        return executeWithTimeout(task, timeoutMs);
    }
}
