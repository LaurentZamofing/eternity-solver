package solver;

import model.Board;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.PuzzleFactory;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for {@link EternitySolverBuilder}. */
@DisplayName("EternitySolverBuilder")
class EternitySolverBuilderTest {

    @Test
    @DisplayName("builder() returns a fresh, non-null builder")
    void factoryReturnsBuilder() {
        assertNotNull(EternitySolver.builder());
    }

    @Test
    @DisplayName("default build produces a runnable solver with standard flags")
    void defaultBuild() {
        EternitySolver solver = EternitySolver.builder().build();
        assertFalse(solver.isMRVIndexEnabled(), "MRV index off by default");
        // Smoke: the builder output must actually solve a 3×3.
        Board board = new Board(3, 3);
        assertTrue(solver.solve(board, PuzzleFactory.createExample3x3()));
    }

    @Test
    @DisplayName("fluent chain configures every exposed knob")
    void fluentChain() {
        EternitySolver solver = EternitySolver.builder()
            .verbose(false)
            .maxExecutionTime(10_000)
            .useAC3(true)
            .mrvIndexEnabled(true)
            .symmetryBreakingFlags(false, false)
            .sortOrder("descending")
            .puzzleName("builder-test")
            .threadLabel("builder-thread")
            .build();
        assertTrue(solver.isMRVIndexEnabled());

        Board board = new Board(3, 3);
        assertTrue(solver.solve(board, PuzzleFactory.createExample3x3()),
            "builder with flags off and MRV index on must still solve 3×3");
    }

    @Test
    @DisplayName("each builder call returns `this` for chaining")
    void returnsThis() {
        EternitySolverBuilder b = EternitySolver.builder();
        assertSame(b, b.verbose(true));
        assertSame(b, b.maxExecutionTime(1));
        assertSame(b, b.useAC3(false));
        assertSame(b, b.mrvIndexEnabled(true));
        assertSame(b, b.symmetryBreakingFlags(false, false));
        assertSame(b, b.sortOrder("ascending"));
        assertSame(b, b.puzzleName("n"));
        assertSame(b, b.threadLabel("t"));
    }
}
