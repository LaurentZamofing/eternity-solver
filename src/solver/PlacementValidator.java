package solver;

import model.Board;
import model.Piece;
import model.Placement;

import java.util.BitSet;
import java.util.Map;

/**
 * Responsable de la validation des placements de pièces sur la grille.
 * Extrait de EternitySolver pour améliorer l'organisation du code.
 */
public class PlacementValidator {
    private final CellConstraints[][] cellConstraints;
    private final EternitySolver.Statistics stats;
    private final String sortOrder;

    public PlacementValidator(CellConstraints[][] cellConstraints, EternitySolver.Statistics stats, String sortOrder) {
        this.cellConstraints = cellConstraints;
        this.stats = stats;
        this.sortOrder = sortOrder;
    }

    /**
     * Vérifie si une pièce avec les arêtes données peut s'adapter à la position (r, c) sur la grille.
     *
     * @param board grille actuelle
     * @param r ligne
     * @param c colonne
     * @param candidateEdges arêtes de la pièce candidate
     * @return true si la pièce peut être placée à cette position
     */
    public boolean fits(Board board, int r, int c, int[] candidateEdges) {
        stats.fitChecks++;

        // Récupérer les contraintes pré-calculées pour cette cellule
        CellConstraints constraints = cellConstraints[r][c];

        // Vérification rapide : si une arête de bordure requise est non-nulle, rejeter immédiatement
        // Vérifier la bordure nord
        if (constraints.isNorthBorder()) {
            if (candidateEdges[0] != 0) return false;
        } else {
            Placement north = board.getPlacement(constraints.northRow, constraints.northCol);
            if (north != null && north.edges[2] != candidateEdges[0]) return false;
            // Contrainte inverse : si l'arête est 0, elle doit être sur la bordure
            if (candidateEdges[0] == 0) return false;
        }

        // Vérifier la bordure ouest
        if (constraints.isWestBorder()) {
            if (candidateEdges[3] != 0) return false;
        } else {
            Placement west = board.getPlacement(constraints.westRow, constraints.westCol);
            if (west != null && west.edges[1] != candidateEdges[3]) return false;
            // Contrainte inverse
            if (candidateEdges[3] == 0) return false;
        }

        // Vérifier la bordure sud
        if (constraints.isSouthBorder()) {
            if (candidateEdges[2] != 0) return false;
        } else {
            Placement south = board.getPlacement(constraints.southRow, constraints.southCol);
            if (south != null && south.edges[0] != candidateEdges[2]) return false;
            // Contrainte inverse
            if (candidateEdges[2] == 0) return false;
        }

        // Vérifier la bordure est
        if (constraints.isEastBorder()) {
            if (candidateEdges[1] != 0) return false;
        } else {
            Placement east = board.getPlacement(constraints.eastRow, constraints.eastCol);
            if (east != null && east.edges[3] != candidateEdges[1]) return false;
            // Contrainte inverse
            if (candidateEdges[1] == 0) return false;
        }

        return true;
    }

    /**
     * Vérification anticipée : vérifie qu'un placement ne va pas créer d'impasse chez les voisins vides.
     *
     * @param board grille actuelle
     * @param r ligne où on veut placer
     * @param c colonne où on veut placer
     * @param candidateEdges arêtes de la pièce qu'on veut placer
     * @param piecesById toutes les pièces disponibles
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @param excludePieceId ID de la pièce qu'on est en train de tester (à exclure)
     * @return true si le placement est sûr, false s'il créerait une impasse
     */
    public boolean forwardCheck(Board board, int r, int c, int[] candidateEdges,
                                 Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces, int excludePieceId) {
        int rows = board.getRows();
        int cols = board.getCols();

        // Vérifier chaque voisin vide
        // Voisin du haut
        if (r > 0 && board.isEmpty(r - 1, c)) {
            if (!hasValidPiece(board, r - 1, c, candidateEdges[0], 2, piecesById, pieceUsed, totalPieces, excludePieceId)) {
                return false; // Le voisin du haut n'aurait aucune pièce valide
            }
        }

        // Voisin du bas
        if (r < rows - 1 && board.isEmpty(r + 1, c)) {
            if (!hasValidPiece(board, r + 1, c, candidateEdges[2], 0, piecesById, pieceUsed, totalPieces, excludePieceId)) {
                return false; // Le voisin du bas n'aurait aucune pièce valide
            }
        }

        // Voisin de gauche
        if (c > 0 && board.isEmpty(r, c - 1)) {
            if (!hasValidPiece(board, r, c - 1, candidateEdges[3], 1, piecesById, pieceUsed, totalPieces, excludePieceId)) {
                return false; // Le voisin de gauche n'aurait aucune pièce valide
            }
        }

        // Voisin de droite
        if (c < cols - 1 && board.isEmpty(r, c + 1)) {
            if (!hasValidPiece(board, r, c + 1, candidateEdges[1], 3, piecesById, pieceUsed, totalPieces, excludePieceId)) {
                return false; // Le voisin de droite n'aurait aucune pièce valide
            }
        }

        return true; // Tous les voisins ont au moins une pièce valide
    }

    /**
     * Vérifie si une case voisine aura au moins une pièce valide après un placement.
     *
     * @param board grille actuelle
     * @param r ligne de la case voisine
     * @param c colonne de la case voisine
     * @param requiredEdge valeur de l'arête que la pièce voisine doit correspondre
     * @param edgeIndex index de l'arête (0=N, 1=E, 2=S, 3=O)
     * @param piecesById toutes les pièces
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @param excludePieceId pièce à exclure (celle qu'on teste)
     * @return true s'il existe au moins une pièce valide pour cette case
     */
    private boolean hasValidPiece(Board board, int r, int c, int requiredEdge, int edgeIndex,
                                   Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces, int excludePieceId) {
        // Itérer sur les pièces dans l'ordre spécifié par sortOrder
        if ("descending".equals(sortOrder)) {
            // Décroissant : de totalPieces vers 1
            for (int pid = totalPieces; pid >= 1; pid--) {
                if (pieceUsed.get(pid)) continue; // Pièce déjà utilisée
                if (pid == excludePieceId) continue; // Ignorer la pièce qu'on est en train de placer

                Piece piece = piecesById.get(pid);
                // Essayer les 4 rotations
                for (int rot = 0; rot < 4; rot++) {
                    int[] edges = piece.edgesRotated(rot);

                    // Vérifier que l'arête requise correspond
                    if (edges[edgeIndex] != requiredEdge) continue;

                    // Vérifier si cette pièce/rotation serait valide à cette position
                    if (fits(board, r, c, edges)) {
                        return true; // On a trouvé au moins une pièce valide
                    }
                }
            }
        } else {
            // Croissant (par défaut) : de 1 vers totalPieces
            for (int pid = 1; pid <= totalPieces; pid++) {
                if (pieceUsed.get(pid)) continue; // Pièce déjà utilisée
                if (pid == excludePieceId) continue; // Ignorer la pièce qu'on est en train de placer

                Piece piece = piecesById.get(pid);
                // Essayer les 4 rotations
                for (int rot = 0; rot < 4; rot++) {
                    int[] edges = piece.edgesRotated(rot);

                    // Vérifier que l'arête requise correspond
                    if (edges[edgeIndex] != requiredEdge) continue;

                    // Vérifier si cette pièce/rotation serait valide à cette position
                    if (fits(board, r, c, edges)) {
                        return true; // On a trouvé au moins une pièce valide
                    }
                }
            }
        }

        return false; // Aucune pièce valide trouvée
    }
}
