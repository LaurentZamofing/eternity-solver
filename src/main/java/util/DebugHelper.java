package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Helper class for debug functionality.
 * Provides utilities like step-by-step execution with user pauses.
 */
public class DebugHelper {

    private static boolean stepByStepEnabled = false;
    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    /**
     * Enable or disable step-by-step mode.
     *
     * @param enabled true to enable step-by-step mode
     */
    public static void setStepByStepMode(boolean enabled) {
        stepByStepEnabled = enabled;
    }

    /**
     * Check if step-by-step mode is enabled.
     *
     * @return true if step-by-step mode is enabled
     */
    public static boolean isStepByStepEnabled() {
        return stepByStepEnabled;
    }

    /**
     * Pause and wait for user input before continuing.
     * Only pauses if step-by-step mode is enabled.
     *
     * Displays a prompt asking the user to press Enter to continue.
     * User can also type 'q' or 'quit' to disable step-by-step mode.
     */
    public static void waitForUserInput() {
        if (!stepByStepEnabled) {
            return;
        }

        // If stdin is not attached to an interactive console (CI, mvn test, piped
        // input), readLine() would block the entire JVM forever. Skip the pause
        // and disable step-by-step mode so subsequent calls also short-circuit.
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
            stepByStepEnabled = false; // Disable if we can't read input
        }
    }

    /**
     * Pause with a custom message.
     *
     * @param message custom message to display before the pause prompt
     */
    public static void waitForUserInput(String message) {
        if (!stepByStepEnabled) {
            return;
        }

        if (message != null && !message.isEmpty()) {
            SolverLogger.info(message);
        }
        waitForUserInput();
    }
}
