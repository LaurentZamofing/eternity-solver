package solver.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solver.validation.PlacementValidator.RejectionReason;
import solver.validation.PlacementValidator.ValidationResult;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for {@link PlacementValidator} helper types.
 *  The validate() / propagateConstraints() paths are exercised end-to-end via
 *  MRVPlacementStrategyTest and SymmetryBreakingBugTrackingTest; this file
 *  covers the small value types and builders which had no direct coverage. */
@DisplayName("PlacementValidator")
class PlacementValidatorTest {

    @Test
    @DisplayName("ValidationResult.valid() has no rejection reason")
    void validResultHasNoReason() {
        ValidationResult r = ValidationResult.valid();
        assertTrue(r.isValid());
        assertNull(r.getReason());
    }

    @Test
    @DisplayName("ValidationResult.rejected(EDGE_MISMATCH) exposes the reason")
    void rejectedEdgeMismatch() {
        ValidationResult r = ValidationResult.rejected(RejectionReason.EDGE_MISMATCH);
        assertFalse(r.isValid());
        assertEquals(RejectionReason.EDGE_MISMATCH, r.getReason());
    }

    @Test
    @DisplayName("ValidationResult.rejected(SYMMETRY_BREAKING) exposes the reason")
    void rejectedSymmetry() {
        ValidationResult r = ValidationResult.rejected(RejectionReason.SYMMETRY_BREAKING);
        assertFalse(r.isValid());
        assertEquals(RejectionReason.SYMMETRY_BREAKING, r.getReason());
    }

    @Test
    @DisplayName("RejectionReason enum contract")
    void enumContract() {
        RejectionReason[] values = RejectionReason.values();
        assertEquals(3, values.length);
        assertNotNull(RejectionReason.valueOf("EDGE_MISMATCH"));
        assertNotNull(RejectionReason.valueOf("SYMMETRY_BREAKING"));
        assertNotNull(RejectionReason.valueOf("AC3_DEAD_END"));
    }

    @Test
    @DisplayName("Constructor with null symmetryBreakingManager is allowed (treated as no-constraint)")
    void nullSymmetryManagerAllowed() {
        // The contract says symmetryBreakingManager "can be null" — creation must not throw.
        assertDoesNotThrow(() ->
            new PlacementValidator(null, null, null));
    }

    @Test
    @DisplayName("getDomainManager returns the injected instance")
    void getDomainManagerReturnsInjected() {
        solver.DomainManager dm = new solver.DomainManager((b, r, c, edges) -> true);
        PlacementValidator v = new PlacementValidator(null, null, dm);
        assertSame(dm, v.getDomainManager());
    }
}
