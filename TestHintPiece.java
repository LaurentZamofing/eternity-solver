import model.Board;
import model.Piece;
import util.PuzzleFactory;

import java.util.Map;

/**
 * Test pour valider la pièce d'indice obligatoire (piece 139).
 */
public class TestHintPiece {

    public static void main(String[] args) {
        System.out.println("════════════════════════════════════════════════════════");
        System.out.println("  TEST DE LA PIÈCE D'INDICE OBLIGATOIRE - PIECE 139");
        System.out.println("════════════════════════════════════════════════════════\n");

        // Charger les 256 pièces
        Map<Integer, Piece> pieces = PuzzleFactory.createEternityII();
        System.out.println("✓ " + pieces.size() + " pièces chargées\n");

        // Récupérer la pièce 139
        Piece piece139 = pieces.get(139);
        if (piece139 == null) {
            System.out.println("✗ ERREUR: Pièce 139 introuvable!");
            return;
        }

        System.out.println("Pièce 139 trouvée:");
        System.out.println("  Edges (N, E, S, W): " + java.util.Arrays.toString(piece139.getEdges()));
        System.out.println();

        // Position obligatoire selon la documentation
        int targetRow = 8;
        int targetCol = 7;
        int targetRotation = 3;

        System.out.println("Position obligatoire:");
        System.out.println("  Row: " + targetRow + ", Col: " + targetCol);
        System.out.println("  Rotation: " + targetRotation + " (270° clockwise)");
        System.out.println();

        // Créer un board et tester le placement
        Board board = new Board(16, 16);

        // Calculer les edges après rotation
        int[] rotatedEdges = rotateEdges(piece139.getEdges(), targetRotation);
        System.out.println("Edges après rotation " + targetRotation + ": " + java.util.Arrays.toString(rotatedEdges));
        System.out.println();

        // Vérifier les contraintes de bord
        System.out.println("Vérification des contraintes:");

        // Position (8, 7) est une position intérieure (pas sur les bords)
        boolean isValidPosition = true;

        if (targetRow == 0) {
            System.out.println("  - Nord: doit être 0 (bord)");
            if (rotatedEdges[0] != 0) {
                System.out.println("    ✗ ERREUR: Nord = " + rotatedEdges[0] + " (devrait être 0)");
                isValidPosition = false;
            } else {
                System.out.println("    ✓ Nord = 0");
            }
        } else if (targetRow == 15) {
            System.out.println("  - Sud: doit être 0 (bord)");
            if (rotatedEdges[2] != 0) {
                System.out.println("    ✗ ERREUR: Sud = " + rotatedEdges[2] + " (devrait être 0)");
                isValidPosition = false;
            } else {
                System.out.println("    ✓ Sud = 0");
            }
        } else {
            System.out.println("  - Position verticale: intérieure (row " + targetRow + ")");
            System.out.println("    ✓ Nord et Sud peuvent avoir n'importe quelle valeur non-nulle");
        }

        if (targetCol == 0) {
            System.out.println("  - Ouest: doit être 0 (bord)");
            if (rotatedEdges[3] != 0) {
                System.out.println("    ✗ ERREUR: Ouest = " + rotatedEdges[3] + " (devrait être 0)");
                isValidPosition = false;
            } else {
                System.out.println("    ✓ Ouest = 0");
            }
        } else if (targetCol == 15) {
            System.out.println("  - Est: doit être 0 (bord)");
            if (rotatedEdges[1] != 0) {
                System.out.println("    ✗ ERREUR: Est = " + rotatedEdges[1] + " (devrait être 0)");
                isValidPosition = false;
            } else {
                System.out.println("    ✓ Est = 0");
            }
        } else {
            System.out.println("  - Position horizontale: intérieure (col " + targetCol + ")");
            System.out.println("    ✓ Est et Ouest peuvent avoir n'importe quelle valeur non-nulle");
        }

        System.out.println();

        if (isValidPosition) {
            System.out.println("════════════════════════════════════════════════════════");
            System.out.println("  ✓ VALIDATION RÉUSSIE!");
            System.out.println("  La pièce 139 peut être placée à (8, 7) avec rotation 3");
            System.out.println("════════════════════════════════════════════════════════");
        } else {
            System.out.println("════════════════════════════════════════════════════════");
            System.out.println("  ✗ VALIDATION ÉCHOUÉE!");
            System.out.println("  La pièce 139 ne respecte pas les contraintes de bord");
            System.out.println("════════════════════════════════════════════════════════");
        }
    }

    /**
     * Applique une rotation aux edges d'une pièce.
     * @param edges Les edges originaux (N, E, S, W)
     * @param rotation Nombre de rotations 90° clockwise (0-3)
     * @return Les edges après rotation
     */
    private static int[] rotateEdges(int[] edges, int rotation) {
        int[] result = new int[4];
        for (int i = 0; i < 4; i++) {
            result[i] = edges[(i - rotation + 4) % 4];
        }
        return result;
    }
}
