package solver.display;

import config.PuzzleConfig;
import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.SaveStateManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test pour afficher la meilleure solution sauvegardée
 */
public class DisplayBestSolutionTest {

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         TEST D'AFFICHAGE DE LA MEILLEURE SOLUTION                ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        String puzzleType = "eternity2";

        // Charger toutes les sauvegardes
        List<File> bestSaves = SaveStateManager.findAllSaves(puzzleType);

        if (bestSaves == null || bestSaves.isEmpty()) {
            System.out.println("✗ Aucune sauvegarde trouvée pour " + puzzleType);
            return;
        }

        System.out.println("✓ " + bestSaves.size() + " sauvegarde(s) trouvée(s)");
        System.out.println();

        // Prendre la meilleure (première dans la liste)
        File bestSave = bestSaves.get(0);
        System.out.println("✓ Meilleure sauvegarde: " + bestSave.getName());
        System.out.println();

        // Charger l'état
        SaveStateManager.SaveState bestState = SaveStateManager.loadStateFromFile(bestSave, puzzleType);

        if (bestState == null) {
            System.out.println("✗ Erreur lors du chargement de la sauvegarde");
            return;
        }

        System.out.println("✓ État chargé:");
        System.out.println("  - Profondeur: " + bestState.depth + " pièces");
        System.out.println("  - Pièces non utilisées: " + bestState.unusedPieceIds.size());
        System.out.println();

        // Créer le board et charger toutes les pièces
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
        boolean restored = SaveStateManager.restoreState(bestState, board, allPieces);

        if (!restored) {
            System.out.println("✗ Erreur lors de la restauration");
            return;
        }

        System.out.println("✓ État restauré sur le board");
        System.out.println();

        // Afficher la meilleure solution
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║              MEILLEURE SOLUTION ATTEINTE (RECORD)                ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("État avec le plus de pièces placées jusqu'à présent:");
        System.out.println();

        // Créer le solver pour utiliser sa méthode d'affichage
        EternitySolver solver = new EternitySolver();
        List<Integer> unusedIds = new ArrayList<>(bestState.unusedPieceIds);

        solver.printBoardWithLabels(board, allPieces, unusedIds);

        System.out.println();
        board.printScore();
        System.out.println();
        System.out.println("═".repeat(70));
    }
}
