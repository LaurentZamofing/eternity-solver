package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Step-by-step debug pauses.
 *
 * <p>Instance-based (2026-04-18, M5): tests / parallel solvers can own
 * their own helper instead of sharing mutable static state. A shared
 * {@link #DEFAULT} singleton is kept for production callers that used
 * the previous static API unchanged.</p>
 */
public class DebugHelper {

    /** Convenience singleton for production callers; tests may build their own. */
    public static final DebugHelper DEFAULT = new DebugHelper();

    private boolean stepByStepEnabled = false;
    private final BufferedReader reader;

    public DebugHelper() {
        this(new BufferedReader(new InputStreamReader(System.in)));
    }

    /** Visible for tests: inject a fake reader to simulate user input. */
    DebugHelper(BufferedReader reader) {
        this.reader = reader;
    }

    /** Enable or disable step-by-step mode on this instance. */
    public void setStepByStep(boolean enabled) {
        this.stepByStepEnabled = enabled;
    }

    public boolean isStepByStep() {
        return stepByStepEnabled;
    }

    /** Pause and wait for the user to press ENTER. Only pauses when step-by-step
     *  is enabled AND an interactive console is available — otherwise disables
     *  itself (prevents JVM blocking in CI/forked tests). */
    public void pause() {
        if (!stepByStepEnabled) return;

        if (System.console() == null) {
            stepByStepEnabled = false;
            SolverLogger.warn("Step-by-step mode disabled: no interactive console (headless/test run).");
            return;
        }

        try {
            System.out.println();
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.print("⏸  Press ENTER to continue (or 'q' to disable step-by-step): ");
            System.out.flush();

            String input = reader.readLine();
            if (input != null && (input.trim().equalsIgnoreCase("q") || input.trim().equalsIgnoreCase("quit"))) {
                stepByStepEnabled = false;
                SolverLogger.info("Step-by-step mode disabled. Continuing without pauses...");
            }
            System.out.println();
        } catch (IOException e) {
            SolverLogger.warn("Failed to read user input: " + e.getMessage());
            stepByStepEnabled = false;
        }
    }

    public void pause(String message) {
        if (!stepByStepEnabled) return;
        if (message != null && !message.isEmpty()) {
            SolverLogger.info(message);
        }
        pause();
    }

    // ─── Backwards-compat static API ────────────────────────────────────
    // Delegates to DEFAULT so existing callers don't break during migration.

    /** @deprecated Use {@link #DEFAULT}.{@code setStepByStep(boolean)} or a local instance. */
    @Deprecated
    public static void setStepByStepMode(boolean enabled) { DEFAULT.setStepByStep(enabled); }

    /** @deprecated Use {@link #DEFAULT}.{@code isStepByStep()} or a local instance. */
    @Deprecated
    public static boolean isStepByStepEnabled() { return DEFAULT.isStepByStep(); }

    /** @deprecated Use {@link #DEFAULT}.{@code pause()} or a local instance. */
    @Deprecated
    public static void waitForUserInput() { DEFAULT.pause(); }

    /** @deprecated Use {@link #DEFAULT}.{@code pause(String)} or a local instance. */
    @Deprecated
    public static void waitForUserInput(String message) { DEFAULT.pause(message); }
}
