package solver;

import model.Board;
import model.Piece;
import model.Placement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import util.PuzzleFactory;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Correctness gate for AC-3 changes.
 *
 * <p>Solves the 3x3 example puzzle end-to-end and verifies the result
 * is a complete, valid edge-matching solution (all cells filled, every
 * adjacent pair shares matching edges, borders are zero). If an AC-3
 * optimization drops valid placements silently, this test catches it
 * without needing a specific solution to compare against.</p>
 *
 * <p>Runs in the default suite (not tagged slow) — the 3x3 solve is
 * quick; {@code @Timeout(30)} prevents any regression from hanging
 * the suite.</p>
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class AC3CorrectnessTest {

    @Test
    void solve3x3ProducesValidSolution() {
        Board board = new Board(3, 3);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(25_000);

        boolean solved = solver.solve(board, PuzzleFactory.createExample3x3());

        assertTrue(solved, "3x3 example must be solvable");
        verifyCompleteValidSolution(board);
    }

    private void verifyCompleteValidSolution(Board board) {
        int rows = board.getRows();
        int cols = board.getCols();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                assertFalse(board.isEmpty(r, c),
                    "cell (" + r + "," + c + ") must be filled");
                Placement p = board.getPlacement(r, c);

                // Borders must be zero
                if (r == 0) assertZero(p.edges[0], r, c, "north border");
                if (r == rows - 1) assertZero(p.edges[2], r, c, "south border");
                if (c == 0) assertZero(p.edges[3], r, c, "west border");
                if (c == cols - 1) assertZero(p.edges[1], r, c, "east border");

                // Neighbor edges must match
                if (r > 0) {
                    Placement north = board.getPlacement(r - 1, c);
                    assertMatch(north.edges[2], p.edges[0], r, c, "north");
                }
                if (c > 0) {
                    Placement west = board.getPlacement(r, c - 1);
                    assertMatch(west.edges[1], p.edges[3], r, c, "west");
                }
            }
        }
    }

    private static void assertZero(int value, int r, int c, String label) {
        if (value != 0) {
            throw new AssertionError(
                "cell (" + r + "," + c + ") " + label + " edge expected 0 but was " + value);
        }
    }

    private static void assertMatch(int neighborEdge, int cellEdge, int r, int c, String dir) {
        if (neighborEdge != cellEdge) {
            throw new AssertionError(
                "cell (" + r + "," + c + ") " + dir + "-neighbor mismatch: " +
                neighborEdge + " vs " + cellEdge);
        }
    }
}
