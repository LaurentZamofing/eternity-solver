package solver;

import model.Board;
import model.Piece;
import model.Placement;

import java.util.BitSet;
import java.util.Map;

/**
 * Analyse les relations de voisinage et les motifs spatiaux sur le plateau de puzzle.
 *
 * Cette classe fournit des méthodes pour :
 * - Compter les voisins vides et occupés
 * - Détecter les gaps piégés et les configurations problématiques
 * - Calculer les scores de contrainte pour les placements
 * - Compter les options de pièces valides en tenant compte des voisins
 *
 * Ces analyses supportent les heuristiques comme :
 * - Sélection de cellule MRV (Minimum Remaining Values)
 * - Ordonnancement LCV (Least Constraining Value)
 * - Vérification prospective et détection de cul-de-sac
 *
 * Extrait de EternitySolver dans le Sprint 5 pour améliorer :
 * - La modularité et la testabilité
 * - La séparation de la logique d'analyse spatiale
 * - La réutilisabilité entre différentes heuristiques
 */
public class NeighborAnalyzer {

    private final CellConstraints[][] cellConstraints;
    private final PlacementValidator validator;
    private final EdgeCompatibilityIndex edgeIndex;

    /**
     * Constructeur
     * @param cellConstraints contraintes pré-calculées pour chaque cellule
     * @param validator validateur de placement
     * @param edgeIndex index de compatibilité des bords pour recherches rapides
     */
    public NeighborAnalyzer(CellConstraints[][] cellConstraints,
                           PlacementValidator validator,
                           EdgeCompatibilityIndex edgeIndex) {
        this.cellConstraints = cellConstraints;
        this.validator = validator;
        this.edgeIndex = edgeIndex;
    }

    /**
     * Compte les cellules voisines vides (adjacents directs : N, S, E, W).
     *
     * @param board état actuel du plateau
     * @param row position ligne
     * @param col position colonne
     * @return nombre de voisins vides (0-4)
     */
    public int countEmptyNeighbors(Board board, int row, int col) {
        int count = 0;
        int rows = board.getRows();
        int cols = board.getCols();

        // North
        if (row > 0 && board.isEmpty(row - 1, col)) count++;
        // South
        if (row < rows - 1 && board.isEmpty(row + 1, col)) count++;
        // East
        if (col < cols - 1 && board.isEmpty(row, col + 1)) count++;
        // West
        if (col > 0 && board.isEmpty(row, col - 1)) count++;

        return count;
    }

    /**
     * Compte les cellules voisines occupées (adjacents directs : N, S, E, W).
     *
     * @param board état actuel du plateau
     * @param row position ligne
     * @param col position colonne
     * @return nombre de voisins occupés (0-4)
     */
    public int countOccupiedNeighbors(Board board, int row, int col) {
        int count = 0;
        int rows = board.getRows();
        int cols = board.getCols();

        // North
        if (row > 0 && !board.isEmpty(row - 1, col)) count++;
        // South
        if (row < rows - 1 && !board.isEmpty(row + 1, col)) count++;
        // East
        if (col < cols - 1 && !board.isEmpty(row, col + 1)) count++;
        // West
        if (col > 0 && !board.isEmpty(row, col - 1)) count++;

        return count;
    }

    /**
     * Compte les cellules de bord remplies adjacentes pour une position donnée.
     * Les cellules de bord sont celles sur le périmètre du plateau (row=0, row=max, col=0, col=max).
     *
     * @param board état actuel du plateau
     * @param row position ligne
     * @param col position colonne
     * @return nombre de cellules de bord remplies adjacentes
     */
    public int countAdjacentFilledBorderCells(Board board, int row, int col) {
        int count = 0;
        int rows = board.getRows();
        int cols = board.getCols();

        // Check North neighbor
        if (row > 0) {
            int northRow = row - 1;
            boolean isNorthBorder = (northRow == 0);
            if (isNorthBorder && !board.isEmpty(northRow, col)) {
                count++;
            }
        }

        // Check South neighbor
        if (row < rows - 1) {
            int southRow = row + 1;
            boolean isSouthBorder = (southRow == rows - 1);
            if (isSouthBorder && !board.isEmpty(southRow, col)) {
                count++;
            }
        }

        // Check East neighbor
        if (col < cols - 1) {
            int eastCol = col + 1;
            boolean isEastBorder = (eastCol == cols - 1);
            if (isEastBorder && !board.isEmpty(row, eastCol)) {
                count++;
            }
        }

        // Check West neighbor
        if (col > 0) {
            int westCol = col - 1;
            boolean isWestBorder = (westCol == 0);
            if (isWestBorder && !board.isEmpty(row, westCol)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Vérifie si placer une pièce à cette position créerait un gap piégé.
     *
     * Un gap piégé est une région vide isolée qui ne peut pas être remplie complètement.
     * Cela se produit quand une cellule vide est entourée de cellules occupées d'une manière
     * qui rend impossible la satisfaction de toutes les contraintes de bords.
     *
     * Stratégie : Après avoir hypothétiquement placé une pièce, vérifier chaque voisin vide
     * pour voir s'il deviendrait insoluble (plus de pièces valides disponibles).
     *
     * @param board état actuel du plateau
     * @param row ligne où la pièce serait placée
     * @param col colonne où la pièce serait placée
     * @param candidateEdges bords de la pièce à placer
     * @param pieces map de toutes les pièces
     * @param pieceUsed bitset des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @param candidatePieceId ID de la pièce considérée
     * @return true si le placement créerait un gap piégé
     */
    public boolean wouldCreateTrappedGap(Board board, int row, int col, int[] candidateEdges,
                                        Map<Integer, Piece> pieces, BitSet pieceUsed,
                                        int totalPieces, int candidatePieceId) {
        // Si candidateEdges est null, on ne peut pas déterminer les gaps piégés
        // Retourner false (optimiste - supposer qu'il n'y a pas de piège)
        if (candidateEdges == null || pieces == null || pieceUsed == null) {
            return false;
        }

        int rows = board.getRows();
        int cols = board.getCols();

        // Vérifier chaque voisin vide
        // Voisin Nord
        if (row > 0 && board.isEmpty(row - 1, col)) {
            int requiredSouth = candidateEdges[0]; // Bord Nord du candidat
            if (countValidPieces(board, row - 1, col, requiredSouth, 2, pieces, pieceUsed, totalPieces, candidatePieceId) == 0) {
                return true;
            }
        }

        // Voisin Sud
        if (row < rows - 1 && board.isEmpty(row + 1, col)) {
            int requiredNorth = candidateEdges[2]; // Bord Sud du candidat
            if (countValidPieces(board, row + 1, col, requiredNorth, 0, pieces, pieceUsed, totalPieces, candidatePieceId) == 0) {
                return true;
            }
        }

        // Voisin Est
        if (col < cols - 1 && board.isEmpty(row, col + 1)) {
            int requiredWest = candidateEdges[1]; // Bord Est du candidat
            if (countValidPieces(board, row, col + 1, requiredWest, 3, pieces, pieceUsed, totalPieces, candidatePieceId) == 0) {
                return true;
            }
        }

        // Voisin Ouest
        if (col > 0 && board.isEmpty(row, col - 1)) {
            int requiredEast = candidateEdges[3]; // Bord Ouest du candidat
            if (countValidPieces(board, row, col - 1, requiredEast, 1, pieces, pieceUsed, totalPieces, candidatePieceId) == 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calcule à quel point un placement serait contraignant pour ses voisins.
     * Score plus élevé = plus contraignant (moins d'options restantes pour les voisins).
     *
     * Utilisé par l'heuristique LCV (Least Constraining Value) pour préférer les placements
     * qui laissent le plus de flexibilité pour les choix futurs.
     *
     * @param board état actuel du plateau
     * @param r position ligne
     * @param c position colonne
     * @param candidateEdges bords de la pièce considérée
     * @param pieces map de toutes les pièces
     * @param pieceUsed bitset des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @param excludePieceId ID de pièce à exclure des comptages
     * @return score de contrainte (plus élevé = plus contraignant)
     */
    public int calculateConstraintScore(Board board, int r, int c, int[] candidateEdges,
                                       Map<Integer, Piece> pieces, BitSet pieceUsed,
                                       int totalPieces, int excludePieceId) {
        int score = 0;
        int rows = board.getRows();
        int cols = board.getCols();

        // Pour chaque voisin vide, compter combien de pièces pourraient encore s'adapter
        // Comptages élevés = moins contraignant

        // Voisin Nord
        if (r > 0 && board.isEmpty(r - 1, c)) {
            int requiredSouth = candidateEdges[0];
            score += countValidPieces(board, r - 1, c, requiredSouth, 2, pieces, pieceUsed, totalPieces, excludePieceId);
        }

        // Voisin Sud
        if (r < rows - 1 && board.isEmpty(r + 1, c)) {
            int requiredNorth = candidateEdges[2];
            score += countValidPieces(board, r + 1, c, requiredNorth, 0, pieces, pieceUsed, totalPieces, excludePieceId);
        }

        // Voisin Est
        if (c < cols - 1 && board.isEmpty(r, c + 1)) {
            int requiredWest = candidateEdges[1];
            score += countValidPieces(board, r, c + 1, requiredWest, 3, pieces, pieceUsed, totalPieces, excludePieceId);
        }

        // Voisin Ouest
        if (c > 0 && board.isEmpty(r, c - 1)) {
            int requiredEast = candidateEdges[3];
            score += countValidPieces(board, r, c - 1, requiredEast, 1, pieces, pieceUsed, totalPieces, excludePieceId);
        }

        // Retourner le négatif pour préférer moins contraignant (comptage de pièces valides plus élevé)
        return -score;
    }

    /**
     * Compte les pièces valides qui pourraient être placées à une position avec une contrainte de bord optionnelle.
     *
     * @param board état actuel du plateau
     * @param r position ligne
     * @param c position colonne
     * @param requiredEdge valeur de bord qui doit correspondre (-1 si aucune contrainte)
     * @param edgeIndex quel bord doit correspondre (0=N, 1=E, 2=S, 3=W)
     * @param pieces map de toutes les pièces
     * @param pieceUsed bitset des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @param excludePieceId ID de pièce à exclure du comptage
     * @return nombre de pièces valides
     */
    public int countValidPieces(Board board, int r, int c, int requiredEdge, int edgeIndex,
                               Map<Integer, Piece> pieces, BitSet pieceUsed,
                               int totalPieces, int excludePieceId) {
        int count = 0;

        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed.get(pid) || pid == excludePieceId) continue;

            Piece piece = pieces.get(pid);
            if (piece == null) continue;

            int maxRotations = piece.getUniqueRotationCount();
            for (int rot = 0; rot < maxRotations; rot++) {
                int[] edges = piece.edgesRotated(rot);

                // Vérifier la contrainte de bord requise
                if (requiredEdge != -1 && edges[edgeIndex] != requiredEdge) {
                    continue;
                }

                // Vérifier si le placement satisfait toutes les contraintes
                if (validator.fits(board, r, c, edges)) {
                    count++;
                    break; // Compter la pièce une fois, indépendamment des rotations
                }
            }
        }

        return count;
    }
}
