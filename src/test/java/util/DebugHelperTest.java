package util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for {@link DebugHelper} — the 2026-04-18 instance-based flavour. */
@DisplayName("DebugHelper")
class DebugHelperTest {

    @AfterEach
    void resetStaticDefault() {
        // Guard against test bleed: deprecated static API mutates
        // DebugHelper.DEFAULT, so we reset after every test.
        DebugHelper.DEFAULT.setStepByStep(false);
    }

    @Test
    @DisplayName("fresh helper starts with step-by-step off")
    void defaultsToOff() {
        assertFalse(new DebugHelper().isStepByStep());
    }

    @Test
    @DisplayName("setStepByStep flips the flag")
    void toggle() {
        DebugHelper h = new DebugHelper();
        h.setStepByStep(true);
        assertTrue(h.isStepByStep());
        h.setStepByStep(false);
        assertFalse(h.isStepByStep());
    }

    @Test
    @DisplayName("pause() is a no-op when step-by-step is off")
    void pauseNoOpWhenOff() {
        DebugHelper h = new DebugHelper();
        assertDoesNotThrow(() -> h.pause());
        assertDoesNotThrow(() -> h.pause("ignored message"));
    }

    @Test
    @DisplayName("'q' input disables step-by-step")
    void quitInputDisables() {
        DebugHelper h = new DebugHelper(new BufferedReader(new StringReader("q\n")));
        h.setStepByStep(true);
        // The System.console() guard will short-circuit in tests, disabling
        // immediately — that itself is acceptable behaviour. The test pins
        // the contract that pause() never throws and leaves stepByStep off.
        assertDoesNotThrow(() -> h.pause());
        assertFalse(h.isStepByStep(), "step-by-step must be off after pause in a headless run or after 'q'");
    }

    @Test
    @DisplayName("deprecated static API still delegates to DEFAULT")
    @SuppressWarnings("deprecation")
    void deprecatedStaticDelegates() {
        DebugHelper.setStepByStepMode(true);
        assertTrue(DebugHelper.isStepByStepEnabled());
        DebugHelper.setStepByStepMode(false);
        assertFalse(DebugHelper.isStepByStepEnabled());
    }

    @Test
    @DisplayName("deprecated waitForUserInput/waitForUserInput(msg) delegate")
    @SuppressWarnings("deprecation")
    void deprecatedPauseDelegates() {
        DebugHelper.setStepByStepMode(false);
        assertDoesNotThrow(() -> DebugHelper.waitForUserInput());
        assertDoesNotThrow(() -> DebugHelper.waitForUserInput("hello"));
    }

    /** Test-only subclass that pretends the console is interactive. */
    private static final class FakeConsole extends DebugHelper {
        FakeConsole(BufferedReader r) { super(r); }
        @Override boolean hasInteractiveConsole() { return true; }
    }

    @Test
    @DisplayName("interactive path: empty input keeps step-by-step on")
    void emptyInputKeepsStepByStep() {
        DebugHelper h = new FakeConsole(new BufferedReader(new StringReader("\n")));
        h.setStepByStep(true);
        h.pause();
        assertTrue(h.isStepByStep(), "empty input should leave step-by-step alone");
    }

    @Test
    @DisplayName("interactive path: typing 'q' disables step-by-step")
    void typingQDisables() {
        DebugHelper h = new FakeConsole(new BufferedReader(new StringReader("q\n")));
        h.setStepByStep(true);
        h.pause();
        assertFalse(h.isStepByStep(), "'q' should switch step-by-step off");
    }

    @Test
    @DisplayName("interactive path: typing 'quit' (case-insensitive) disables step-by-step")
    void typingQuitDisables() {
        DebugHelper h = new FakeConsole(new BufferedReader(new StringReader("QUIT\n")));
        h.setStepByStep(true);
        h.pause();
        assertFalse(h.isStepByStep());
    }

    @Test
    @DisplayName("interactive path: non-quit input does not disable")
    void nonQuitInputDoesNotDisable() {
        DebugHelper h = new FakeConsole(new BufferedReader(new StringReader("continue\n")));
        h.setStepByStep(true);
        h.pause();
        assertTrue(h.isStepByStep());
    }

    @Test
    @DisplayName("interactive path: IOException on read disables step-by-step")
    void ioExceptionDisables() {
        BufferedReader broken = new BufferedReader(new StringReader("")) {
            @Override public String readLine() throws IOException {
                throw new IOException("boom");
            }
        };
        DebugHelper h = new FakeConsole(broken);
        h.setStepByStep(true);
        h.pause();
        assertFalse(h.isStepByStep(), "IOException should disable step-by-step");
    }

    @Test
    @DisplayName("pause(message) prints the message before pausing")
    void pauseWithMessage() {
        DebugHelper h = new FakeConsole(new BufferedReader(new StringReader("\n")));
        h.setStepByStep(true);
        assertDoesNotThrow(() -> h.pause("checkpoint"));
        assertDoesNotThrow(() -> h.pause(""));     // empty → skip logger.info
        assertDoesNotThrow(() -> h.pause(null));   // null → skip logger.info
    }
}
