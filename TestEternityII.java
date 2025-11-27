import model.Piece;
import util.PuzzleFactory;
import java.util.Map;

/**
 * Test pour vérifier le chargement d'Eternity II
 */
public class TestEternityII {
    public static void main(String[] args) {
        System.out.println("Chargement des pièces Eternity II...");

        try {
            Map<Integer, Piece> pieces = PuzzleFactory.createEternityII();

            System.out.println("Nombre de pièces chargées : " + pieces.size());

            if (pieces.size() != 256) {
                System.err.println("ERREUR: Devrait avoir 256 pièces!");
                return;
            }

            // Afficher les 10 premières pièces
            System.out.println("\nPremières 10 pièces:");
            for (int i = 1; i <= 10; i++) {
                Piece p = pieces.get(i);
                int[] edges = p.getEdges();
                System.out.printf("Pièce %3d: N=%2d E=%2d S=%2d W=%2d%n",
                    i, edges[0], edges[1], edges[2], edges[3]);
            }

            // Compter les pièces de coin (2 bords = 0)
            int corners = 0;
            int borders = 0;
            int centers = 0;

            for (Piece p : pieces.values()) {
                int[] edges = p.getEdges();
                int zeroCount = 0;
                for (int e : edges) {
                    if (e == 0) zeroCount++;
                }

                if (zeroCount == 2) corners++;
                else if (zeroCount == 1) borders++;
                else if (zeroCount == 0) centers++;
            }

            System.out.println("\nAnalyse des pièces:");
            System.out.println("- Coins (2 bords)   : " + corners + " (attendu: 4)");
            System.out.println("- Bords (1 bord)    : " + borders + " (attendu: 56 pour 16x16)");
            System.out.println("- Centres (0 bords) : " + centers + " (attendu: 196 pour 16x16)");
            System.out.println("\nTotal               : " + (corners + borders + centers));

            // Vérifier qu'on a bien les bonnes proportions pour un 16x16
            if (corners == 4 && borders == 56 && centers == 196) {
                System.out.println("\n✓ Configuration correcte pour un puzzle 16x16!");
            } else {
                System.out.println("\n⚠ Configuration inhabituelle");
            }

        } catch (Exception e) {
            System.err.println("ERREUR lors du chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
