import model.Board;
import model.Piece;
import solver.EternitySolver;
import java.io.IOException;
import java.util.*;

/**
 * Test rapide pour vérifier que le code de prévention des gaps fonctionne
 * sur un petit puzzle avec PrioritizeBorders activé.
 */
public class TestIndice1Border {

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         TEST: Résolution Indice1 avec PrioritizeBorders         ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        try {
            // Charger le puzzle
            String filepath = "data/indice1/indice1_border.txt";
            PuzzleConfig config = PuzzleConfig.loadFromFile(filepath);

            if (config == null) {
                System.out.println("✗ Impossible de charger le puzzle");
                return;
            }

            // Afficher les informations
            config.printInfo();
            System.out.println();

            System.out.println("Configuration:");
            System.out.println("  - PrioritizeBorders: " + config.isPrioritizeBorders());
            System.out.println("  - SortOrder: " + config.getSortOrder());
            System.out.println("  - Verbose: " + config.isVerbose());
            System.out.println();

            // Créer le board
            Board board = new Board(config.getRows(), config.getCols());
            Map<Integer, Piece> allPieces = new HashMap<>(config.getPieces());

            // Créer et configurer le solver
            EternitySolver.resetGlobalState();
            EternitySolver solver = new EternitySolver();
            solver.setDisplayConfig(config.isVerbose(), config.getMinDepthToShowRecords());
            solver.setPuzzleName("indice1_border");
            solver.setSortOrder(config.getSortOrder());
            solver.setPrioritizeBorders(config.isPrioritizeBorders());

            System.out.println("Démarrage de la résolution...");
            System.out.println("═".repeat(70));
            System.out.println();

            // Résoudre
            long startTime = System.currentTimeMillis();
            boolean solved = solver.solve(board, allPieces);
            long duration = System.currentTimeMillis() - startTime;

            System.out.println();
            System.out.println("═".repeat(70));
            System.out.println();

            if (solved) {
                System.out.println("✓ SOLUTION TROUVÉE !");
                System.out.println();
                System.out.println("Temps: " + formatDuration(duration));
                System.out.println();

                // Afficher la solution
                displaySolutionWithBorderCheck(board, allPieces);

                // Vérifier les gaps sur les bords
                boolean hasGaps = checkForBorderGaps(board);
                System.out.println();

                if (!hasGaps) {
                    System.out.println("✓ VALIDATION: Aucun gap détecté sur les bords!");
                } else {
                    System.out.println("✗ ERREUR: Des gaps ont été détectés sur les bords!");
                }
            } else {
                System.out.println("✗ Pas de solution trouvée");
                System.out.println("Temps: " + formatDuration(duration));
            }

            System.out.println();

        } catch (IOException e) {
            System.out.println("✗ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Affiche la solution avec les bords mis en évidence
     */
    private static void displaySolutionWithBorderCheck(Board board, Map<Integer, Piece> allPieces) {
        int rows = board.getRows();
        int cols = board.getCols();

        System.out.println("Solution (les bords sont mis en évidence):");
        System.out.println();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                boolean isBorder = (r == 0 || r == rows - 1 || c == 0 || c == cols - 1);

                if (board.isEmpty(r, c)) {
                    if (isBorder) {
                        System.out.print("\033[1;31m  ·  \033[0m"); // Rouge pour les vides sur bords
                    } else {
                        System.out.print("  ·  ");
                    }
                } else {
                    model.Placement p = board.getPlacement(r, c);
                    if (isBorder) {
                        System.out.print("\033[1;32m" + String.format("%3d_%d", p.getPieceId(), p.getRotation()) + "\033[0m ");
                    } else {
                        System.out.print(String.format("%3d_%d", p.getPieceId(), p.getRotation()) + " ");
                    }
                }
            }
            System.out.println();
        }

        System.out.println();
        board.printScore();
    }

    /**
     * Vérifie s'il y a des gaps sur les bords
     */
    private static boolean checkForBorderGaps(Board board) {
        int rows = board.getRows();
        int cols = board.getCols();
        boolean hasGaps = false;

        // Vérifier le bord supérieur (ligne 0)
        for (int c = 1; c < cols - 1; c++) {
            if (board.isEmpty(0, c) && !board.isEmpty(0, c - 1) && !board.isEmpty(0, c + 1)) {
                System.out.println("  ⚠ Gap détecté sur bord supérieur à la position [0," + c + "]");
                hasGaps = true;
            }
        }

        // Vérifier le bord inférieur (ligne rows-1)
        for (int c = 1; c < cols - 1; c++) {
            if (board.isEmpty(rows - 1, c) && !board.isEmpty(rows - 1, c - 1) && !board.isEmpty(rows - 1, c + 1)) {
                System.out.println("  ⚠ Gap détecté sur bord inférieur à la position [" + (rows - 1) + "," + c + "]");
                hasGaps = true;
            }
        }

        // Vérifier le bord gauche (colonne 0)
        for (int r = 1; r < rows - 1; r++) {
            if (board.isEmpty(r, 0) && !board.isEmpty(r - 1, 0) && !board.isEmpty(r + 1, 0)) {
                System.out.println("  ⚠ Gap détecté sur bord gauche à la position [" + r + ",0]");
                hasGaps = true;
            }
        }

        // Vérifier le bord droit (colonne cols-1)
        for (int r = 1; r < rows - 1; r++) {
            if (board.isEmpty(r, cols - 1) && !board.isEmpty(r - 1, cols - 1) && !board.isEmpty(r + 1, cols - 1)) {
                System.out.println("  ⚠ Gap détecté sur bord droit à la position [" + r + "," + (cols - 1) + "]");
                hasGaps = true;
            }
        }

        return hasGaps;
    }

    private static String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + " ms";
        } else if (ms < 60000) {
            return String.format("%.2f s", ms / 1000.0);
        } else {
            return String.format("%.2f min", ms / 60000.0);
        }
    }
}
