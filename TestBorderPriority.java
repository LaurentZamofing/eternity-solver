import model.*;
import solver.EternitySolver;
import util.PuzzleLoader;
import util.SaveStateManager;

import java.util.*;

/**
 * Test de la priorisation des bords
 */
public class TestBorderPriority {
    public static void main(String[] args) {
        // Charger un puzzle de test
        String puzzleFile = "data/puzzle_eternity2_p17_3_4_1_2_ascending.txt";

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("TEST: Priorisation des bords");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println();

        // Charger le puzzle
        Map<Integer, Piece> pieces = PuzzleLoader.loadPiecesFromFile(puzzleFile);
        if (pieces == null || pieces.isEmpty()) {
            System.err.println("Erreur: Impossible de charger le puzzle");
            return;
        }

        Board board = new Board(16, 16);

        // Créer le solver avec priorisation des bords
        EternitySolver solver = new EternitySolver(board, pieces);
        solver.setPrioritizeBorders(true); // ACTIVER LA PRIORISATION DES BORDS
        solver.setDisplayConfig(true, 0);
        solver.setPuzzleName("eternity2_p17_ascending_BORDERS");

        System.out.println("Démarrage de la résolution avec priorisation des bords...");
        System.out.println();

        // Lancer la résolution avec timeout de 60 secondes
        boolean solved = solver.solve(60000);

        System.out.println();
        if (solved) {
            System.out.println("✓ Solution trouvée!");
        } else {
            System.out.println("⏱ Timeout ou pas de solution trouvée");
            System.out.println("Record de profondeur: " + solver.getMaxDepth() + " pièces");
        }
    }
}
