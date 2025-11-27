package solver;

import model.Board;
import model.Piece;
import java.util.Map;

/**
 * Wrapper pour ajouter un timeout au solveur
 */
public class SolverWithTimeout extends EternitySolver {

    /**
     * Résout le puzzle avec un timeout
     *
     * @param board grille vide à remplir
     * @param pieces map des pièces par ID
     * @param timeoutMs timeout en millisecondes
     * @return true si le puzzle a été résolu
     */
    public boolean solveWithTimeout(Board board, Map<Integer, Piece> pieces, long timeoutMs) {
        final boolean[] solved = {false};
        final boolean[] timedOut = {false};

        Thread solverThread = new Thread(() -> {
            try {
                solved[0] = solve(board, pieces);
            } catch (Exception e) {
                // Ignorer les exceptions pendant le timeout
            }
        });

        solverThread.start();

        try {
            solverThread.join(timeoutMs);
            if (solverThread.isAlive()) {
                timedOut[0] = true;
                solverThread.interrupt();
                solverThread.join(1000); // Attendre 1 sec pour l'interruption
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return solved[0] && !timedOut[0];
    }
}
