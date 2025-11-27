package solver;

import model.Board;
import model.Piece;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * SingletonDetector trouve les pièces qui ne peuvent être placées qu'à une seule position sur le plateau.
 *
 * Un singleton est une pièce qui n'a qu'une seule position valide sur le plateau (bien qu'elle puisse
 * avoir plusieurs rotations valides à cette position). Trouver et placer les singletons est
 * une optimisation puissante car si un singleton existe et n'est pas placé à sa position unique,
 * le puzzle devient insoluble.
 *
 * Cette classe implémente la détection de singletons en testant toutes les pièces disponibles contre
 * toutes les positions et rotations vides pour identifier les pièces avec exactement une position valide.
 *
 * @author Eternity Solver Team
 */
public class SingletonDetector {

    /**
     * Classe pour stocker les informations d'une pièce singleton.
     */
    public static class SingletonInfo {
        public final int pieceId;
        public final int row;
        public final int col;
        public final int rotation;

        public SingletonInfo(int pieceId, int row, int col, int rotation) {
            this.pieceId = pieceId;
            this.row = row;
            this.col = col;
            this.rotation = rotation;
        }
    }

    /**
     * Suivi des statistiques pour la détection de singletons.
     */
    public static class Statistics {
        public long singletonsFound = 0;
        public long singletonsPlaced = 0;
        public long deadEndsDetected = 0;

        public void incrementSingletonsFound() {
            singletonsFound++;
        }

        public void incrementSingletonsPlaced() {
            singletonsPlaced++;
        }

        public void incrementDeadEnds() {
            deadEndsDetected++;
        }
    }

    private final FitChecker fitChecker;
    private final Statistics stats;
    private final boolean verbose;

    /**
     * Interface pour vérifier si une pièce s'adapte à une position.
     */
    public interface FitChecker {
        boolean fits(Board board, int r, int c, int[] candidateEdges);
    }

    /**
     * Constructeur pour SingletonDetector.
     *
     * @param fitChecker le vérificateur d'ajustement à utiliser pour valider les placements
     * @param stats le suivi des statistiques
     * @param verbose indique s'il faut afficher une sortie détaillée
     */
    public SingletonDetector(FitChecker fitChecker, Statistics stats, boolean verbose) {
        this.fitChecker = fitChecker;
        this.stats = stats;
        this.verbose = verbose;
    }

    /**
     * Cherche une pièce qui ne peut aller qu'à un seul endroit (singleton).
     * C'est une optimisation puissante : si une pièce n'a qu'une position possible,
     * il FAUT la poser là, sinon la branche est vouée à l'échec.
     *
     * @param board grille actuelle
     * @param piecesById map des pièces par ID
     * @param pieceUsed tableau des pièces utilisées
     * @param totalPieces nombre total de pièces
     * @return info du singleton si trouvé, null sinon
     */
    public SingletonInfo findSingletonPiece(Board board, Map<Integer, Piece> piecesById,
                                           BitSet pieceUsed, int totalPieces) {
        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed.get(pid)) continue; // Pièce déjà utilisée
            Piece piece = piecesById.get(pid);
            List<int[]> possiblePositions = new ArrayList<>(); // [r, c, rotation]

            // Tester toutes les positions et rotations possibles pour cette pièce
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (board.isEmpty(r, c)) {
                        for (int rot = 0; rot < 4; rot++) {
                            int[] candidate = piece.edgesRotated(rot);
                            if (fitChecker.fits(board, r, c, candidate)) {
                                possiblePositions.add(new int[]{r, c, rot});
                            }
                        }
                    }
                }
            }

            // Vérifier si la pièce ne peut aller qu'à une seule POSITION (peu importe le nombre de rotations)
            if (possiblePositions.size() > 0) {
                // Grouper par position (r,c) pour voir si toutes les possibilités sont à la même position
                int firstRow = possiblePositions.get(0)[0];
                int firstCol = possiblePositions.get(0)[1];
                boolean samePosition = true;

                for (int[] pos : possiblePositions) {
                    if (pos[0] != firstRow || pos[1] != firstCol) {
                        samePosition = false;
                        break;
                    }
                }

                // Si toutes les possibilités sont à la même position → singleton !
                if (samePosition) {
                    // Choisir la première rotation possible (arbitraire, on testera les autres en backtracking si nécessaire)
                    int[] pos = possiblePositions.get(0);
                    stats.incrementSingletonsFound();
                    if (verbose) {
                        String rotInfo = possiblePositions.size() == 1 ?
                            " avec rotation " + (pos[2] * 90) + "°" :
                            " avec " + possiblePositions.size() + " rotations possibles";
                        System.out.println("🎯 SINGLETON trouvé ! Pièce " + pid + " ne peut aller qu'en (" + pos[0] + ", " + pos[1] + ")" + rotInfo);
                    }
                    return new SingletonInfo(pid, pos[0], pos[1], pos[2]);
                }
            }

            // Dead-end : cette pièce ne peut aller nulle part !
            if (possiblePositions.size() == 0) {
                stats.incrementDeadEnds();
                if (verbose) {
                    System.out.println("⚠ DEAD-END : Pièce " + pid + " ne peut aller nulle part !");
                }
                return null;
            }
        }

        return null; // Pas de singleton trouvé
    }

    /**
     * Vérifie s'il existe des pièces qui ne peuvent être placées nulle part (détection de dead-end).
     * C'est une vérification plus simple que la détection complète de singletons.
     *
     * @param board état actuel du plateau
     * @param piecesById carte de toutes les pièces
     * @param pieceUsed bitset suivant les pièces utilisées
     * @param totalPieces nombre total de pièces
     * @return true si un dead-end est détecté (une pièce n'a aucun placement valide)
     */
    public boolean hasDeadEnd(Board board, Map<Integer, Piece> piecesById,
                             BitSet pieceUsed, int totalPieces) {
        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed.get(pid)) continue;
            Piece piece = piecesById.get(pid);
            boolean hasValidPlacement = false;

            // Vérifier si la pièce a au moins un placement valide
            outerLoop:
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (board.isEmpty(r, c)) {
                        for (int rot = 0; rot < 4; rot++) {
                            int[] candidate = piece.edgesRotated(rot);
                            if (fitChecker.fits(board, r, c, candidate)) {
                                hasValidPlacement = true;
                                break outerLoop;
                            }
                        }
                    }
                }
            }

            if (!hasValidPlacement) {
                stats.incrementDeadEnds();
                if (verbose) {
                    System.out.println("⚠ DEAD-END : Pièce " + pid + " ne peut aller nulle part !");
                }
                return true;
            }
        }

        return false;
    }
}
