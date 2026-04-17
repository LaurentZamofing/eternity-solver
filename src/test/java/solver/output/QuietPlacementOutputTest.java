package solver.output;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke coverage for {@link QuietPlacementOutput}.
 *
 * <p>The class exists to silence all output when users want a minimal
 * console — every method is a no-op. We pin that contract so a future
 * refactor doesn't accidentally re-enable logging in "quiet" mode.</p>
 */
@DisplayName("QuietPlacementOutput")
class QuietPlacementOutputTest {

    @Test
    @DisplayName("every log method runs without throwing and returns nothing")
    void allMethodsAreNoOps() {
        QuietPlacementOutput q = new QuietPlacementOutput();
        // None of these should throw — contract is "completely quiet".
        assertDoesNotThrow(() -> {
            q.logCellSelection(null, null, 0, 0, 0, 0);
            q.logPlacementAttempt(1, 0, 0, 0, 0, 1, new int[]{0, 0, 0, 0});
            q.logEdgeRejection();
            q.logSymmetryRejection();
            q.logConstraintsSatisfied();
            q.logSuccessfulPlacement(null, null, 0, 0);
            q.logAC3DeadEnd(1);
            q.logTimeout(1, 0, 0);
            q.logBacktrack(1, 0, 0, 0);
            q.logExhaustedOptions(0, 0, 0);
            q.waitForUser();
        });
    }

    @Test
    @DisplayName("implements PlacementOutputStrategy contract")
    void implementsInterface() {
        PlacementOutputStrategy strategy = new QuietPlacementOutput();
        assertNotNull(strategy);
    }
}
