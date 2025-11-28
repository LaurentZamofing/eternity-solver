import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.PuzzleFactory;
import util.SaveStateManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test pour afficher l'état d'une sauvegarde chargée
 */
public class TestDisplaySavedState {

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         TEST D'AFFICHAGE D'UNE SAUVEGARDE CHARGÉE                ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Charger la sauvegarde current d'Eternity II
        File currentSave = SaveStateManager.findCurrentSave("eternity2");

        if (currentSave == null) {
            System.out.println("✗ Aucune sauvegarde current trouvée pour eternity2");
            return;
        }

        System.out.println("✓ Sauvegarde trouvée: " + currentSave.getName());
        System.out.println();

        // Charger l'état
        SaveStateManager.SaveState saveState = SaveStateManager.loadStateFromFile(currentSave, "eternity2");

        if (saveState == null) {
            System.out.println("✗ Erreur lors du chargement de la sauvegarde");
            return;
        }

        System.out.println("✓ État chargé:");
        System.out.println("  - Profondeur: " + saveState.depth + " pièces");
        System.out.println("  - Pièces non utilisées: " + saveState.unusedPieceIds.size());
        System.out.println();

        // Créer le board et charger toutes les pièces via PuzzleConfig
        Board board = new Board(16, 16);
        Map<Integer, Piece> allPieces;
        try {
            PuzzleConfig config = PuzzleConfig.loadFromFile("data/puzzle_eternity2.txt");
            allPieces = config.getPieces();
        } catch (IOException e) {
            System.out.println("✗ Erreur lors du chargement du puzzle: " + e.getMessage());
            return;
        }

        // Restaurer l'état
        boolean restored = SaveStateManager.restoreState(saveState, board, allPieces);

        if (!restored) {
            System.out.println("✗ Erreur lors de la restauration");
            return;
        }

        System.out.println("✓ État restauré sur le board");
        System.out.println();

        // Créer le solver pour utiliser sa méthode d'affichage
        EternitySolver solver = new EternitySolver();
        List<Integer> unusedIds = new ArrayList<>(saveState.unusedPieceIds);

        // Afficher l'état complet
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║              ÉTAT DU PUZZLE CHARGÉ (VALIDATION)                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Légende:");
        System.out.println("  - Pièces placées: ID de la pièce avec valeurs d'arêtes (N/E/S/W)");
        System.out.println("  - Cases vides: (XXX) = nombre de pièces valides possibles");
        System.out.println("  - \033[93mJaune\033[0m: cases critiques (≤20 possibilités)");
        System.out.println("  - \033[1;91mRouge\033[0m: dead-end (0 possibilités)");
        System.out.println();

        solver.printBoardWithLabels(board, allPieces, unusedIds);

        System.out.println();
        board.printScore();
        System.out.println();
        System.out.println("═".repeat(70));
    }
}
