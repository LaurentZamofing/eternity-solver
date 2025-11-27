package solver;

import model.Board;
import model.Piece;
import model.Placement;

import java.util.Map;

/**
 * Gère les contraintes de brisure de symétrie pour éliminer les branches de recherche redondantes.
 *
 * Les puzzles d'assemblage de bords ont des symétries inhérentes :
 * - Symétrie rotationnelle (4 orientations)
 * - Symétrie de réflexion (miroirs horizontal/vertical)
 *
 * Sans brisure de symétrie, le solveur explore des solutions équivalentes qui diffèrent
 * seulement par rotation ou réflexion, gaspillant du temps de calcul.
 *
 * Ce gestionnaire applique des contraintes pour élaguer les branches symétriques tôt dans la recherche.
 *
 * Stratégies clés :
 * 1. Ordre lexicographique : Force les pièces de coin à suivre un ordre spécifique
 * 2. Rotation fixe : Fixe la rotation de la première pièce placée
 * 3. Contraintes de coin : Assure que le coin supérieur gauche a le plus petit ID de pièce
 *
 * Extrait de EternitySolver pour améliorer la modularité et la testabilité.
 */
public class SymmetryBreakingManager {

    private final boolean verbose;
    private final int rows;
    private final int cols;

    // Drapeaux de stratégie de brisure de symétrie
    private boolean enableLexicographicOrdering = true;
    private boolean enableRotationalFixing = true;
    private boolean enableReflectionPruning = false; // Future : symétrie horizontale/verticale

    /**
     * Constructeur
     * @param rows nombre de lignes dans le puzzle
     * @param cols nombre de colonnes dans le puzzle
     * @param verbose activer la journalisation détaillée
     */
    public SymmetryBreakingManager(int rows, int cols, boolean verbose) {
        this.rows = rows;
        this.cols = cols;
        this.verbose = verbose;
    }

    /**
     * Active ou désactive la contrainte d'ordre lexicographique
     * @param enabled true pour activer
     */
    public void setLexicographicOrdering(boolean enabled) {
        this.enableLexicographicOrdering = enabled;
    }

    /**
     * Active ou désactive la contrainte de fixation rotationnelle
     * @param enabled true pour activer
     */
    public void setRotationalFixing(boolean enabled) {
        this.enableRotationalFixing = enabled;
    }

    /**
     * Vérifie si un placement violerait les contraintes de brisure de symétrie.
     * Ceci est appelé AVANT de placer une pièce pour élaguer les branches invalides.
     *
     * @param board état actuel du plateau
     * @param row ligne où placer la pièce
     * @param col colonne où placer la pièce
     * @param pieceId ID de la pièce à placer
     * @param rotation rotation de la pièce (0-3)
     * @param allPieces carte de toutes les pièces
     * @return true si le placement est autorisé, false s'il viole les contraintes de symétrie
     */
    public boolean isPlacementAllowed(Board board, int row, int col, int pieceId,
                                     int rotation, Map<Integer, Piece> allPieces) {
        // Vérifie l'ordre lexicographique sur les coins
        if (enableLexicographicOrdering) {
            if (!checkLexicographicOrdering(board, row, col, pieceId)) {
                if (verbose) {
                    System.out.println("  ⛔ Symétrie : Rejet de la pièce " + pieceId + " à (" + row + "," + col + ") - viole l'ordre lexicographique");
                }
                return false;
            }
        }

        // Vérifie la fixation de rotation pour la première pièce (si applicable)
        if (enableRotationalFixing) {
            if (!checkRotationFixing(board, row, col, rotation)) {
                if (verbose) {
                    System.out.println("  ⛔ Symétrie : Rejet de la rotation " + rotation + " à (" + row + "," + col + ") - viole la fixation de rotation");
                }
                return false;
            }
        }

        return true;
    }

    /**
     * Applique l'ordre lexicographique sur les pièces de coin.
     *
     * Stratégie : Le coin supérieur gauche doit avoir un ID de pièce plus petit que :
     * - Le coin supérieur droit (élimine la réflexion horizontale)
     * - Le coin inférieur gauche (élimine la réflexion verticale)
     * - Le coin inférieur droit (élimine la rotation de 180°)
     *
     * Cela réduit l'espace de recherche d'un facteur allant jusqu'à 4.
     *
     * @param board état actuel du plateau
     * @param row ligne où la pièce est placée
     * @param col colonne où la pièce est placée
     * @param pieceId ID de la pièce à placer
     * @return true si le placement respecte l'ordre, false sinon
     */
    private boolean checkLexicographicOrdering(Board board, int row, int col, int pieceId) {
        // Applique l'ordre uniquement sur les positions de coin
        boolean isTopLeft = (row == 0 && col == 0);
        boolean isTopRight = (row == 0 && col == cols - 1);
        boolean isBottomLeft = (row == rows - 1 && col == 0);
        boolean isBottomRight = (row == rows - 1 && col == cols - 1);

        if (!isTopLeft && !isTopRight && !isBottomLeft && !isBottomRight) {
            return true; // Pas un coin, pas de contrainte
        }

        Placement topLeft = board.getPlacement(0, 0);

        // Si on place le coin supérieur gauche en premier, toujours autoriser (il devient la référence)
        if (isTopLeft) {
            return true;
        }

        // Si le coin supérieur gauche n'est pas encore placé, autoriser les autres coins (seront contraints plus tard)
        if (topLeft == null) {
            return true;
        }

        int topLeftId = topLeft.getPieceId();

        // Applique : Tous les autres coins doivent avoir un ID de pièce >= ID du coin supérieur gauche
        // Cela élimine les duplicatas rotationnels/réflexionnels
        if (isTopRight || isBottomLeft || isBottomRight) {
            if (pieceId < topLeftId) {
                return false; // Viole la contrainte d'ordre
            }
        }

        return true;
    }

    /**
     * Applique la fixation de rotation pour des cellules spécifiques afin d'éliminer la symétrie rotationnelle.
     *
     * Stratégie : Fixe la rotation de la pièce du coin supérieur gauche à 0°.
     * Cela élimine 3/4 des solutions rotationnellement équivalentes.
     *
     * Note : S'applique uniquement si la pièce a plusieurs rotations uniques.
     *
     * @param board état actuel du plateau
     * @param row ligne où la pièce est placée
     * @param col colonne où la pièce est placée
     * @param rotation rotation appliquée (0-3)
     * @return true si la rotation est autorisée, false sinon
     */
    private boolean checkRotationFixing(Board board, int row, int col, int rotation) {
        // Fixe la rotation uniquement pour le coin supérieur gauche
        if (row != 0 || col != 0) {
            return true; // Pas le coin supérieur gauche, pas de contrainte
        }

        // Fixe le coin supérieur gauche à la rotation 0
        // Cela brise la symétrie rotationnelle de la solution entière
        return rotation == 0;
    }

    /**
     * Applique la validation post-placement pour les contraintes de symétrie.
     * Appelé APRÈS qu'une pièce est placée pour vérifier l'état du plateau.
     *
     * @param board état actuel du plateau
     * @return true si l'état du plateau est valide sous les contraintes de symétrie
     */
    public boolean validateBoardState(Board board) {
        if (!enableLexicographicOrdering) {
            return true;
        }

        Placement topLeft = board.getPlacement(0, 0);
        if (topLeft == null) {
            return true; // Impossible de valider pour l'instant
        }

        int topLeftId = topLeft.getPieceId();

        // Vérifie que tous les coins respectent l'ordre lexicographique
        Placement topRight = board.getPlacement(0, cols - 1);
        if (topRight != null && topRight.getPieceId() < topLeftId) {
            return false;
        }

        Placement bottomLeft = board.getPlacement(rows - 1, 0);
        if (bottomLeft != null && bottomLeft.getPieceId() < topLeftId) {
            return false;
        }

        Placement bottomRight = board.getPlacement(rows - 1, cols - 1);
        if (bottomRight != null && bottomRight.getPieceId() < topLeftId) {
            return false;
        }

        return true;
    }

    /**
     * Affiche les informations de brisure de symétrie au début de la résolution
     */
    public void logConfiguration() {
        if (verbose) {
            System.out.println("  🔄 Brisure de symétrie :");
            System.out.println("     - Ordre lexicographique : " + (enableLexicographicOrdering ? "✓" : "✗"));
            System.out.println("     - Fixation de rotation : " + (enableRotationalFixing ? "✓" : "✗"));
            System.out.println("     - Élagage par réflexion : " + (enableReflectionPruning ? "✓" : "✗"));
        }
    }

    /**
     * Obtient la réduction attendue de l'espace de recherche grâce à la brisure de symétrie
     * @return facteur de réduction de l'espace de recherche (ex: 4.0 signifie 1/4 de l'espace original)
     */
    public double getExpectedReductionFactor() {
        double factor = 1.0;

        if (enableLexicographicOrdering) {
            factor *= 4.0; // Élimine la symétrie rotationnelle 4-voies
        }

        if (enableRotationalFixing) {
            // Déjà compté dans le lexicographique
        }

        if (enableReflectionPruning) {
            factor *= 2.0; // Élimine la réflexion horizontale/verticale
        }

        return factor;
    }

    /**
     * Vérifie si la brisure de symétrie est activée
     * @return true si une stratégie de brisure de symétrie est active
     */
    public boolean isEnabled() {
        return enableLexicographicOrdering || enableRotationalFixing || enableReflectionPruning;
    }
}
