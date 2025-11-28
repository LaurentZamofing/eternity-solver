package solver;

import model.Board;
import model.Piece;
import model.Placement;

import java.util.BitSet;
import java.util.Map;

/** Analyzes neighbor relationships and spatial patterns; supports MRV cell selection, LCV ordering, and trapped gap detection for puzzle solving heuristics. */
public class NeighborAnalyzer {

    private final CellConstraints[][] cellConstraints;
    private final PlacementValidator validator;
    private final EdgeCompatibilityIndex edgeIndex;

    /** Creates analyzer with pre-calculated cell constraints, placement validator, and edge compatibility index. */
    public NeighborAnalyzer(CellConstraints[][] cellConstraints,
                           PlacementValidator validator,
                           EdgeCompatibilityIndex edgeIndex) {
        this.cellConstraints = cellConstraints;
        this.validator = validator;
        this.edgeIndex = edgeIndex;
    }

    /** Counts empty neighbor cells (direct adjacents: N, S, E, W); returns 0-4. */
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

    /** Counts occupied neighbor cells (direct adjacents: N, S, E, W); returns 0-4. */
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

    /** Counts adjacent filled border cells (perimeter cells at row=0, row=max, col=0, col=max) for given position. */
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

    /** Checks if placing piece would create trapped gap (isolated empty region unsolvable due to edge constraints); returns true if any neighbor becomes unsolvable. */
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

    /** Calculates constraint score for placement (higher = more constraining); used by LCV heuristic to prefer placements leaving most flexibility for future choices. */
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

    /** Counts valid pieces that could be placed at position with optional edge constraint (requiredEdge=-1 for none, edgeIndex: 0=N, 1=E, 2=S, 3=W). */
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
