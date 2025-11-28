package solver;

import model.Board;
import model.Piece;
import java.util.Map;

/**
 * Wrapper to add timeout to the solver
 */
public class SolverWithTimeout extends EternitySolver {

    /**
     * Solves the puzzle with a timeout
     *
     * @param board empty grid to fill
     * @param pieces map of pieces by ID
     * @param timeoutMs timeout in milliseconds
     * @return true if the puzzle was solved
     */
    public boolean solveWithTimeout(Board board, Map<Integer, Piece> pieces, long timeoutMs) {
        final boolean[] solved = {false};
        final boolean[] timedOut = {false};

        Thread solverThread = new Thread(() -> {
            try {
                solved[0] = solve(board, pieces);
            } catch (Exception e) {
                // Ignore exceptions during timeout
            }
        });

        solverThread.start();

        try {
            solverThread.join(timeoutMs);
            if (solverThread.isAlive()) {
                timedOut[0] = true;
                solverThread.interrupt();
                solverThread.join(1000); // Wait 1 sec for interruption
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return solved[0] && !timedOut[0];
    }
}
