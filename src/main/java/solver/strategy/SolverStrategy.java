package solver.strategy;

import model.Board;
import model.Piece;

import java.util.Map;

/**
 * Strategy pattern for different puzzle solving approaches.
 *
 * Implementations can use different algorithms:
 * - Sequential backtracking
 * - Parallel search
 * - Historical/resume from saved state
 * - Hybrid approaches
 *
 * Usage:
 * <pre>
 * SolverStrategy strategy = new SequentialStrategy(solverContext);
 * SolverResult result = strategy.solve();
 * if (result.isSolved()) {
 *     Board solution = result.getSolution();
 * }
 * </pre>
 */
public interface SolverStrategy {

    /**
     * Executes the solving strategy.
     *
     * @return SolverResult containing solution status and board if solved
     */
    SolverResult solve();

    /**
     * Gets the name of this strategy for logging/display.
     *
     * @return Human-readable strategy name
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Result of a solving attempt.
     */
    class SolverResult {
        private final boolean solved;
        private final Board solution;
        private final Map<Integer, Piece> pieces;
        private final long timeMs;
        private final int nodesExplored;

        private SolverResult(boolean solved, Board solution, Map<Integer, Piece> pieces,
                            long timeMs, int nodesExplored) {
            this.solved = solved;
            this.solution = solution;
            this.pieces = pieces;
            this.timeMs = timeMs;
            this.nodesExplored = nodesExplored;
        }

        public static SolverResult success(Board solution, Map<Integer, Piece> pieces,
                                          long timeMs, int nodesExplored) {
            return new SolverResult(true, solution, pieces, timeMs, nodesExplored);
        }

        public static SolverResult failure(long timeMs, int nodesExplored) {
            return new SolverResult(false, null, null, timeMs, nodesExplored);
        }

        public static SolverResult timeout(long timeMs, int nodesExplored) {
            return new SolverResult(false, null, null, timeMs, nodesExplored);
        }

        public boolean isSolved() {
            return solved;
        }

        public Board getSolution() {
            return solution;
        }

        public Map<Integer, Piece> getPieces() {
            return pieces;
        }

        public long getTimeMs() {
            return timeMs;
        }

        public int getNodesExplored() {
            return nodesExplored;
        }
    }
}
