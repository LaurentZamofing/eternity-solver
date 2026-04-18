package solver.experimental.dlx;

import model.Board;
import model.Piece;
import solver.Solver;

import java.util.List;
import java.util.Map;

/**
 * Dancing Links implementation of the {@link Solver} contract.
 *
 * <p>Builds the exact-cover matrix for the given board / pieces, runs
 * {@link AlgorithmX} with a leaf-time edge-matching validator, and writes
 * the first valid solution back to the board.</p>
 *
 * <p>POC / experimental — off by default, never wired into the production
 * CLI. Compared against {@code EternitySolver} only in
 * {@code DLXBenchmark} and {@code DancingLinksSolverTest}.</p>
 */
public final class DancingLinksSolver implements Solver {

    private long maxExecutionTimeMs = 60_000;

    public DancingLinksSolver() { }

    public void setMaxExecutionTime(long ms) { this.maxExecutionTimeMs = ms; }

    @Override
    public boolean solve(Board board, Map<Integer, Piece> pieces) {
        // Accept only an already-empty board for the POC — resuming from a
        // pre-filled board would need separate row filtering.
        int rows = board.getRows();
        int cols = board.getCols();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!board.isEmpty(r, c)) {
                    throw new IllegalArgumentException(
                        "DLX POC requires an empty board (cell " + r + "," + c + " is pre-filled)");
                }
            }
        }

        EternityExactCover problem = new EternityExactCover(rows, cols, pieces);
        long deadline = System.currentTimeMillis() + maxExecutionTimeMs;

        List<Integer> solutionRows = AlgorithmX.findFirstSolution(
            problem.matrix(),
            candidate -> {
                if (System.currentTimeMillis() > deadline) return false;
                return problem.isValidEternitySolution(candidate);
            });

        if (solutionRows == null) return false;
        problem.applyTo(board, solutionRows);
        return true;
    }
}
