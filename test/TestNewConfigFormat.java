import java.io.*;

/**
 * Test pour vérifier le chargement des nouvelles configurations avec SortOrder
 */
public class TestNewConfigFormat {

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         TEST DU NOUVEAU FORMAT DE CONFIGURATION                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        String[] testFiles = {
            "data/puzzle_eternity2_p01_1_2_3_4_ascending.txt",
            "data/puzzle_eternity2_p01_1_2_3_4_descending.txt",
            "data/puzzle_eternity2_p12_2_4_3_1_ascending.txt",
            "data/puzzle_eternity2_p24_4_3_2_1_descending.txt"
        };

        for (String filepath : testFiles) {
            System.out.println("─".repeat(70));
            System.out.println("Test: " + filepath);
            System.out.println("─".repeat(70));

            try {
                PuzzleConfig config = PuzzleConfig.loadFromFile(filepath);

                if (config == null) {
                    System.out.println("✗ Erreur: impossible de charger le fichier");
                    continue;
                }

                System.out.println("✓ Configuration chargée avec succès");
                System.out.println("  - Nom: " + config.getName());
                System.out.println("  - Type: " + config.getType());
                System.out.println("  - Dimensions: " + config.getRows() + "×" + config.getCols());
                System.out.println("  - Pièces: " + config.getPieces().size());
                System.out.println("  - Ordre de tri: " + config.getSortOrder());
                System.out.println("  - Pièces fixes: " + config.getFixedPieces().size());

                // Afficher les pièces fixes
                if (!config.getFixedPieces().isEmpty()) {
                    System.out.println("  - Coins fixés:");
                    for (PuzzleConfig.FixedPiece fp : config.getFixedPieces()) {
                        if (fp.row == 0 && fp.col == 0) {
                            System.out.println("    * Coin haut-gauche (0,0): pièce " + fp.pieceId + " rotation " + fp.rotation);
                        } else if (fp.row == 0 && fp.col == 15) {
                            System.out.println("    * Coin haut-droit (0,15): pièce " + fp.pieceId + " rotation " + fp.rotation);
                        } else if (fp.row == 15 && fp.col == 0) {
                            System.out.println("    * Coin bas-gauche (15,0): pièce " + fp.pieceId + " rotation " + fp.rotation);
                        } else if (fp.row == 15 && fp.col == 15) {
                            System.out.println("    * Coin bas-droit (15,15): pièce " + fp.pieceId + " rotation " + fp.rotation);
                        } else {
                            System.out.println("    * Position (" + fp.row + "," + fp.col + "): pièce " + fp.pieceId + " rotation " + fp.rotation);
                        }
                    }
                }

                System.out.println();

            } catch (IOException e) {
                System.out.println("✗ Erreur lors du chargement: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("═".repeat(70));
        System.out.println("✓ Tests terminés");
    }
}
