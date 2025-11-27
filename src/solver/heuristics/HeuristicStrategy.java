package solver.heuristics;

import model.Board;
import model.Piece;

import java.util.BitSet;
import java.util.Map;

/**
 * HeuristicStrategy définit l'interface pour les stratégies de sélection de cellule dans le solveur Eternity.
 *
 * Différentes heuristiques peuvent être implémentées pour choisir quelle cellule vide remplir ensuite pendant
 * la recherche par retour sur trace. Le choix de la cellule peut affecter dramatiquement les performances de recherche.
 *
 * Les heuristiques courantes incluent :
 * - MRV (Minimum Remaining Values) : Choisir la cellule avec le moins de placements valides
 * - Heuristique de degré : Choisir la cellule avec le plus de voisins
 * - Ordre ligne-majeur : Remplir les cellules de haut-gauche à bas-droite
 *
 * @author Eternity Solver Team
 */
public interface HeuristicStrategy {

    /**
     * Représente une position sur le plateau.
     */
    class CellPosition {
        public final int row;
        public final int col;

        public CellPosition(int row, int col) {
            this.row = row;
            this.col = col;
        }

        /**
         * Convertit au format tableau [row, col] pour la compatibilité rétroactive.
         */
        public int[] toArray() {
            return new int[]{row, col};
        }

        @Override
        public String toString() {
            return "(" + row + ", " + col + ")";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CellPosition)) return false;
            CellPosition other = (CellPosition) obj;
            return this.row == other.row && this.col == other.col;
        }

        @Override
        public int hashCode() {
            return 31 * row + col;
        }
    }

    /**
     * Sélectionne la prochaine cellule à remplir en fonction de la stratégie heuristique.
     *
     * @param board l'état actuel du plateau
     * @param piecesById carte de toutes les pièces par ID
     * @param pieceUsed bitset suivant quelles pièces sont déjà placées
     * @param totalPieces nombre total de pièces dans le puzzle
     * @return la position de la prochaine cellule à remplir, ou null si aucune cellule vide ne reste
     */
    CellPosition selectNextCell(Board board, Map<Integer, Piece> piecesById,
                                BitSet pieceUsed, int totalPieces);

    /**
     * Obtient le nom de cette stratégie heuristique.
     *
     * @return un nom descriptif pour cette stratégie
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
