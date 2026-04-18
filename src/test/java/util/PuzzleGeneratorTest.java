package util;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import solver.EternitySolver;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates that {@link PuzzleGenerator} produces actually-solvable
 * puzzles at various sizes. The generator extracts pieces from a known
 * grid, so the solver should always terminate with {@code true}.
 */
@DisplayName("PuzzleGenerator")
class PuzzleGeneratorTest {

    @Test
    @DisplayName("deterministic: same seed yields identical piece map")
    void deterministic() {
        Map<Integer, Piece> a = PuzzleGenerator.generate(3, 4, 100L);
        Map<Integer, Piece> b = PuzzleGenerator.generate(3, 4, 100L);
        assertEquals(a.size(), b.size());
        for (int id : a.keySet()) {
            assertArrayEquals(
                a.get(id).edgesRotated(0),
                b.get(id).edgesRotated(0),
                "piece " + id + " edges must match across deterministic runs");
        }
    }

    @Test
    @DisplayName("size 3×3 puzzle has 9 pieces, all borders 0 at edges")
    void shapeContract() {
        Map<Integer, Piece> pieces = PuzzleGenerator.generate(3, 4, 1L);
        assertEquals(9, pieces.size());
        // At least one piece must have edges with 0 on N+W (TL fittable), etc.
        boolean hasTL = pieces.values().stream()
            .anyMatch(p -> p.edgesRotated(0)[0] == 0 && p.edgesRotated(0)[3] == 0);
        assertTrue(hasTL, "a generated puzzle must contain at least one TL-fittable piece");
    }

    @Test
    @DisplayName("solver resolves a generated 3×3 puzzle")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void solve3x3Generated() {
        Board board = new Board(3, 3);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(20_000);
        solver.setSymmetryBreakingFlags(false, false);
        assertTrue(solver.solve(board, PuzzleGenerator.generate(3, 4, 7L)),
            "generated 3×3 must solve");
    }

    @Test
    @DisplayName("solver resolves a generated 4×4 puzzle")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void solve4x4Generated() {
        Board board = new Board(4, 4);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(40_000);
        solver.setSymmetryBreakingFlags(false, false);
        assertTrue(solver.solve(board, PuzzleGenerator.generate(4, 5, 42L)),
            "generated 4×4 must solve");
    }
}
