package solver.debug;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for {@link DebugPlacementLogger}.
 *
 * <p>Every log method delegates to {@code SolverLogger}; these tests verify
 * that the delegation path completes without throwing and that structured
 * markers appear in the captured output. Full behaviour is observed
 * indirectly through {@code MRVPlacementStrategyTest}; this file just
 * pins the package to a real unit test so future refactors in
 * {@code solver/debug/} stay covered.</p>
 */
@DisplayName("DebugPlacementLogger")
class DebugPlacementLoggerTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger root;

    @BeforeEach
    void attachAppender() {
        root = (Logger) LoggerFactory.getLogger("EternitySolver");
        appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        root.detachAppender(appender);
    }

    @Test
    @DisplayName("logAC3DeadEnd emits a marker mentioning the rejected piece")
    void logAC3DeadEndEmitsMarker() {
        DebugPlacementLogger logger = new DebugPlacementLogger(false);
        logger.logAC3DeadEnd(1, 2, 7, null, null);

        boolean mentionsPiece7 = appender.list.stream()
            .anyMatch(e -> e.getFormattedMessage().contains("piece 7"));
        assertTrue(mentionsPiece7, "log line should mention piece 7");
    }

    @Test
    @DisplayName("showBoard=false constructor does not throw on dead-end log")
    void quietConstructorSurvives() {
        DebugPlacementLogger logger = new DebugPlacementLogger(false);
        assertDoesNotThrow(() -> logger.logAC3DeadEnd(0, 0, 1, null, null));
    }

    @Test
    @DisplayName("showBoard=true constructor accepted (board rendering path is lazy)")
    void verboseConstructorAccepted() {
        assertDoesNotThrow(() -> new DebugPlacementLogger(true));
    }
}
