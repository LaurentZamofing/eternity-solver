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
 * Test pour afficher la comparaison entre le meilleur record et le current
 */
public class TestDisplayComparison {

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         TEST D'AFFICHAGE AVEC COMPARAISON                        ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        String puzzleType = "eternity2";

        // Charger toutes les sauvegardes
        List<File> bestSaves = SaveStateManager.findAllSaves(puzzleType);

        if (bestSaves == null || bestSaves.size() < 2) {
            System.out.println("✗ Besoin d'au moins 2 sauvegardes pour comparer");
            return;
        }

        System.out.println("✓ " + bestSaves.size() + " sauvegarde(s) trouvée(s)");
        System.out.println();

        // Prendre les deux meilleures pour simuler best vs current
        File bestSave = bestSaves.get(0);
        File currentSave = bestSaves.size() > 1 ? bestSaves.get(1) : bestSaves.get(0);

        System.out.println("✓ RECORD: " + bestSave.getName());
        System.out.println("✓ CURRENT: " + currentSave.getName());
        System.out.println();

        // Charger les états
        SaveStateManager.SaveState bestState = SaveStateManager.loadStateFromFile(bestSave, puzzleType);
        SaveStateManager.SaveState currentState = SaveStateManager.loadStateFromFile(currentSave, puzzleType);

        if (bestState == null || currentState == null) {
            System.out.println("✗ Erreur lors du chargement des sauvegardes");
            return;
        }

        // Créer les boards
        Board bestBoard = new Board(16, 16);
        Board currentBoard = new Board(16, 16);

        Map<Integer, Piece> allPieces;
        try {
            PuzzleConfig config = PuzzleConfig.loadFromFile("data/puzzle_eternity2.txt");
            allPieces = config.getPieces();
        } catch (IOException e) {
            System.out.println("✗ Erreur lors du chargement du puzzle: " + e.getMessage());
            return;
        }

        Map<Integer, Piece> bestPieces = new HashMap<>(allPieces);
        Map<Integer, Piece> currentPieces = new HashMap<>(allPieces);

        // Restaurer les états
        boolean bestRestored = SaveStateManager.restoreState(bestState, bestBoard, bestPieces);
        boolean currentRestored = SaveStateManager.restoreState(currentState, currentBoard, currentPieces);

        if (!bestRestored || !currentRestored) {
            System.out.println("✗ Erreur lors de la restauration");
            return;
        }

        System.out.println("✓ États restaurés");
        System.out.println("  - RECORD: " + bestState.depth + " pièces");
        System.out.println("  - CURRENT: " + currentState.depth + " pièces");
        System.out.println();

        // Afficher la comparaison
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║              MEILLEURE SOLUTION ATTEINTE (RECORD)                ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Légende des couleurs (comparaison RECORD vs CURRENT):");
        System.out.println("  - \033[1;35mMagenta\033[0m: Case occupée dans RECORD mais vide dans CURRENT (régression)");
        System.out.println("  - \033[1;38;5;208mOrange\033[0m: Pièce différente entre RECORD et CURRENT (changement)");
        System.out.println("  - \033[1;33mJaune\033[0m: Case vide dans RECORD mais occupée dans CURRENT (progression)");
        System.out.println("  - \033[1;36mCyan\033[0m: Case identique dans RECORD et CURRENT (stabilité)");
        System.out.println();

        EternitySolver solver = new EternitySolver();
        List<Integer> bestUnusedIds = new ArrayList<>(bestState.unusedPieceIds);

        // Afficher le RECORD avec comparaison au CURRENT
        solver.printBoardWithComparison(bestBoard, currentBoard, bestPieces, bestUnusedIds);

        System.out.println();
        bestBoard.printScore();
        System.out.println();
        System.out.println("═".repeat(70));
    }
}
