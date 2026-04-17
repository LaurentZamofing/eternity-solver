package solver;

import model.Board;
import model.Piece;

import java.util.Map;

/**
 * Common contract for every puzzle solver.
 *
 * <p>Introduced by Chantier 3 of IMPROVEMENT_PLAN.md to decouple tests
 * and callers from the concrete {@link EternitySolver} and
 * {@link HistoricalSolver}. Both implement this interface so callers can
 * swap implementations (e.g. a future DLX-based solver — see task #12)
 * without editing the wiring.</p>
 *
 * <p>Implementations are expected to be <b>single-use per solve</b>:
 * stateful setters (verbose, max-execution-time, symmetry flags…) are
 * declared on the concrete class and must be configured before calling
 * {@link #solve}. The interface itself stays minimal.</p>
 */
public interface Solver {

    /**
     * Attempts to place every piece in {@code pieces} on {@code board}.
     *
     * @param board  the puzzle board (may be partially pre-filled with fixed pieces)
     * @param pieces the piece catalogue indexed by id
     * @return {@code true} if the board was fully and validly filled, {@code false}
     *         if the search exhausted the tree or hit a configured limit
     */
    boolean solve(Board board, Map<Integer, Piece> pieces);
}
