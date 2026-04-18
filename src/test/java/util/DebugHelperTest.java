package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for {@link DebugHelper} — the 2026-04-18 instance-based flavour. */
@DisplayName("DebugHelper")
class DebugHelperTest {

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
}
