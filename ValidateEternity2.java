import model.Piece;
import java.io.*;
import java.util.*;

/**
 * Validation du puzzle Eternity II
 * Vérifie que chaque couleur/motif apparaît un nombre pair de fois
 * pour garantir qu'une solution est théoriquement possible
 */
public class ValidateEternity2 {

    private static class EdgeStats {
        Map<Integer, Integer> edgeCounts = new HashMap<>();
        Map<Integer, Integer> borderCounts = new HashMap<>();

        void addEdge(int color, boolean isBorder) {
            if (isBorder) {
                borderCounts.put(color, borderCounts.getOrDefault(color, 0) + 1);
            } else {
                edgeCounts.put(color, edgeCounts.getOrDefault(color, 0) + 1);
            }
        }

        int getTotalEdges() {
            return edgeCounts.values().stream().mapToInt(Integer::intValue).sum();
        }

        int getTotalBorders() {
            return borderCounts.values().stream().mapToInt(Integer::intValue).sum();
        }

        List<Integer> getOddColors() {
            List<Integer> oddColors = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : edgeCounts.entrySet()) {
                if (entry.getValue() % 2 != 0) {
                    oddColors.add(entry.getKey());
                }
            }
            return oddColors;
        }

        boolean isValid() {
            return getOddColors().isEmpty();
        }
    }

    public static void main(String[] args) {
        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║          VALIDATION DU PUZZLE ETERNITY II                        ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        String puzzleFile = "data/eternity2/eternity2.txt";
        if (args.length > 0) {
            puzzleFile = args[0];
        }

        try {
            System.out.println("📁 Chargement du puzzle: " + puzzleFile);
            PuzzleConfig config = PuzzleConfig.loadFromFile(puzzleFile);

            if (config == null) {
                System.out.println("✗ Impossible de charger le puzzle");
                return;
            }

            System.out.println("✓ Puzzle chargé: " + config.getRows() + "×" + config.getCols() +
                             " (" + config.getPieces().size() + " pièces)");
            System.out.println();
            System.out.println("═".repeat(70));
            System.out.println();

            // Analyser les pièces
            EdgeStats stats = new EdgeStats();
            Map<Integer, Piece> pieces = config.getPieces();

            System.out.println("🔍 Analyse des facettes...");
            System.out.println();

            int rows = config.getRows();
            int cols = config.getCols();
            int borderPieces = 2 * (rows + cols) - 4; // Coins comptés une fois
            int interiorPieces = pieces.size() - borderPieces;

            System.out.println("📊 Structure du puzzle:");
            System.out.println("   • Taille: " + rows + "×" + cols);
            System.out.println("   • Total pièces: " + pieces.size());
            System.out.println("   • Pièces de bord: " + borderPieces);
            System.out.println("   • Pièces intérieures: " + interiorPieces);
            System.out.println();

            // Compter toutes les facettes
            for (Piece piece : pieces.values()) {
                int[] edges = piece.getEdges(); // [N, E, S, W]

                stats.addEdge(edges[0], edges[0] == 0); // North
                stats.addEdge(edges[1], edges[1] == 0); // East
                stats.addEdge(edges[2], edges[2] == 0); // South
                stats.addEdge(edges[3], edges[3] == 0); // West
            }

            System.out.println("📈 Statistiques des facettes:");
            System.out.println("   • Total facettes intérieures: " + stats.getTotalEdges());
            System.out.println("   • Total facettes de bord (0): " + stats.getTotalBorders());
            System.out.println("   • Couleurs/motifs différents: " + stats.edgeCounts.size());
            System.out.println();

            // Vérifier le nombre attendu de bords (0)
            int expectedBorders = 2 * (rows + cols);
            if (stats.getTotalBorders() == expectedBorders) {
                System.out.println("✓ Nombre de facettes de bord correct: " + expectedBorders);
            } else {
                System.out.println("✗ ERREUR: Nombre de facettes de bord incorrect!");
                System.out.println("   Attendu: " + expectedBorders);
                System.out.println("   Trouvé: " + stats.getTotalBorders());
            }
            System.out.println();

            // Afficher les détails des couleurs
            System.out.println("═".repeat(70));
            System.out.println();
            System.out.println("🎨 Détails par couleur/motif:");
            System.out.println();

            List<Integer> colors = new ArrayList<>(stats.edgeCounts.keySet());
            Collections.sort(colors);

            System.out.println("Couleur │ Occurrences │ Parité │ Statut");
            System.out.println("────────┼─────────────┼────────┼─────────────");

            int totalOdd = 0;
            for (int color : colors) {
                int count = stats.edgeCounts.get(color);
                String parity = (count % 2 == 0) ? "PAIRE ✓" : "IMPAIRE ✗";
                String status = (count % 2 == 0) ? "OK" : "PROBLÈME";

                if (count % 2 != 0) {
                    totalOdd++;
                }

                System.out.println(String.format("  %4d  │    %5d     │ %s │ %s",
                    color, count, parity.substring(0, 7), status));
            }

            System.out.println();
            System.out.println("═".repeat(70));
            System.out.println();

            // Validation finale
            if (stats.isValid()) {
                System.out.println("✅ PUZZLE VALIDE!");
                System.out.println();
                System.out.println("   Toutes les couleurs apparaissent un nombre pair de fois.");
                System.out.println("   Le puzzle est théoriquement résoluble.");
            } else {
                System.out.println("❌ PUZZLE INVALIDE!");
                System.out.println();
                System.out.println("   ⚠️  " + totalOdd + " couleur(s) apparaissent un nombre impair de fois:");
                System.out.println();

                List<Integer> oddColors = stats.getOddColors();
                for (int color : oddColors) {
                    int count = stats.edgeCounts.get(color);
                    System.out.println("      • Couleur " + color + ": " + count + " occurrences");
                }

                System.out.println();
                System.out.println("   Cela signifie que certaines facettes ne pourront jamais être");
                System.out.println("   appariées, rendant le puzzle impossible à résoudre complètement.");
            }

            System.out.println();

            // Vérifications supplémentaires
            System.out.println("═".repeat(70));
            System.out.println();
            System.out.println("🔬 Vérifications supplémentaires:");
            System.out.println();

            // Vérifier les pièces d'angle (doivent avoir 2 bords à 0)
            int cornerPieces = 0;
            int edgePieces = 0;
            int interiorPiecesCount = 0;

            for (Piece piece : pieces.values()) {
                int[] edges = piece.getEdges(); // [N, E, S, W]
                int borderCount = 0;
                if (edges[0] == 0) borderCount++; // North
                if (edges[1] == 0) borderCount++; // East
                if (edges[2] == 0) borderCount++; // South
                if (edges[3] == 0) borderCount++; // West

                if (borderCount == 2) cornerPieces++;
                else if (borderCount == 1) edgePieces++;
                else if (borderCount == 0) interiorPiecesCount++;
            }

            System.out.println("   • Pièces d'angle (2 bords): " + cornerPieces + " (attendu: 4)");
            if (cornerPieces == 4) {
                System.out.println("     ✓ Nombre de pièces d'angle correct");
            } else {
                System.out.println("     ✗ ERREUR: Devrait avoir exactement 4 pièces d'angle!");
            }

            int expectedEdgePieces = 2 * (rows - 2) + 2 * (cols - 2);
            System.out.println("   • Pièces de bord (1 bord): " + edgePieces + " (attendu: " + expectedEdgePieces + ")");
            if (edgePieces == expectedEdgePieces) {
                System.out.println("     ✓ Nombre de pièces de bord correct");
            } else {
                System.out.println("     ✗ ERREUR: Nombre de pièces de bord incorrect!");
            }

            System.out.println("   • Pièces intérieures (0 bord): " + interiorPiecesCount + " (attendu: " + interiorPieces + ")");
            if (interiorPiecesCount == interiorPieces) {
                System.out.println("     ✓ Nombre de pièces intérieures correct");
            } else {
                System.out.println("     ✗ ERREUR: Nombre de pièces intérieures incorrect!");
            }

            System.out.println();

            // Résumé final
            boolean allChecksPass = stats.isValid() &&
                                   cornerPieces == 4 &&
                                   edgePieces == expectedEdgePieces &&
                                   interiorPiecesCount == interiorPieces &&
                                   stats.getTotalBorders() == expectedBorders;

            System.out.println("═".repeat(70));
            System.out.println();

            if (allChecksPass) {
                System.out.println("🎉 TOUTES LES VALIDATIONS SONT PASSÉES!");
                System.out.println();
                System.out.println("   Le puzzle Eternity II est correctement formé et");
                System.out.println("   devrait être théoriquement résoluble.");
            } else {
                System.out.println("⚠️  CERTAINES VALIDATIONS ONT ÉCHOUÉ!");
                System.out.println();
                System.out.println("   Le puzzle pourrait avoir des erreurs de configuration.");
            }

            System.out.println();

        } catch (Exception e) {
            System.err.println("✗ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
