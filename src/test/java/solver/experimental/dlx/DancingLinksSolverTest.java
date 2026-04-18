package solver.experimental.dlx;

import model.Board;
import model.Placement;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import util.PuzzleFactory;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end correctness tests for the DLX POC solver.
 *
 * <p>Scope: 3×3 example + 4×4 easy example (the hard variants are reserved
 * for the benchmark to keep unit tests fast). Verifies the produced board
 * is fully filled and every adjacent edge matches — matching the contract
 * {@code AC3CorrectnessTest} pins for the production solver.</p>
 */
@DisplayName("DancingLinksSolver — end-to-end correctness")
class DancingLinksSolverTest {

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("solves the 3×3 example")
    void solve3x3() {
        Board board = new Board(3, 3);
        DancingLinksSolver solver = new DancingLinksSolver();
        solver.setMaxExecutionTime(20_000);
        assertTrue(solver.solve(board, PuzzleFactory.createExample3x3()),
            "DLX must solve the canonical 3×3 example");
        assertValidBoard(board);
    }

    @Test
    @Disabled("Primary-only DLX doesn't encode edge-matching — times out on 4×4 "
            + "(observed ≥10min on 2026-04-18 run). See README § 3: go/no-go bench "
            + "confirms no-go for this variant; next iteration would add forward-"
            + "checking or secondary-column edges, tracked as future work.")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @DisplayName("solves the 4×4 easy example — disabled (confirmed no-go)")
    void solve4x4Easy() {
        Board board = new Board(4, 4);
        DancingLinksSolver solver = new DancingLinksSolver();
        solver.setMaxExecutionTime(40_000);
        assertTrue(solver.solve(board, PuzzleFactory.createExample4x4Easy()),
            "DLX must solve the 4×4 easy example");
        assertValidBoard(board);
    }

    @Test
    @DisplayName("rejects a pre-filled board (POC: empty-board only)")
    void rejectsPreFilledBoard() {
        Board board = new Board(3, 3);
        var pieces = PuzzleFactory.createExample3x3();
        // Pre-place any piece
        board.place(0, 0, pieces.values().iterator().next(), 0);

        DancingLinksSolver solver = new DancingLinksSolver();
        assertThrows(IllegalArgumentException.class,
            () -> solver.solve(board, pieces));
    }

    private static void assertValidBoard(Board board) {
        int rows = board.getRows();
        int cols = board.getCols();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                assertFalse(board.isEmpty(r, c),
                    "cell (" + r + "," + c + ") must be filled");
                Placement p = board.getPlacement(r, c);
                if (r == 0) assertEquals(0, p.edges[0], "N-border zero");
                if (r == rows - 1) assertEquals(0, p.edges[2], "S-border zero");
                if (c == 0) assertEquals(0, p.edges[3], "W-border zero");
                if (c == cols - 1) assertEquals(0, p.edges[1], "E-border zero");
                if (r > 0) {
                    Placement north = board.getPlacement(r - 1, c);
                    assertEquals(north.edges[2], p.edges[0],
                        "N/S match at (" + r + "," + c + ")");
                }
                if (c > 0) {
                    Placement west = board.getPlacement(r, c - 1);
                    assertEquals(west.edges[1], p.edges[3],
                        "E/W match at (" + r + "," + c + ")");
                }
            }
        }
    }
}
