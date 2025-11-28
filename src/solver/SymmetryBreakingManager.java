package solver;

import model.Board;
import model.Piece;
import model.Placement;

import java.util.Map;

/** Manages symmetry-breaking constraints to prune redundant search branches using lexicographic ordering (corner pieces), rotation fixing (top-left at 0°), and reflection pruning (4x reduction). */
public class SymmetryBreakingManager {

    private final boolean verbose;
    private final int rows;
    private final int cols;

    // Drapeaux de stratégie de brisure de symétrie
    private boolean enableLexicographicOrdering = true;
    private boolean enableRotationalFixing = true;
    private boolean enableReflectionPruning = false; // Future : symétrie horizontale/verticale

    /** Creates symmetry-breaking manager with board dimensions and verbose flag for detailed logging. */
    public SymmetryBreakingManager(int rows, int cols, boolean verbose) {
        this.rows = rows;
        this.cols = cols;
        this.verbose = verbose;
    }

    /** Enables or disables lexicographic ordering constraint for corner pieces. */
    public void setLexicographicOrdering(boolean enabled) {
        this.enableLexicographicOrdering = enabled;
    }

    /** Enables or disables rotation fixing constraint (top-left corner at 0°). */
    public void setRotationalFixing(boolean enabled) {
        this.enableRotationalFixing = enabled;
    }

    /** Returns true if placement is allowed; called before placing piece to prune branches violating symmetry constraints (lexicographic ordering or rotation fixing). */
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

    /** Enforces lexicographic ordering on corner pieces; top-left must have smallest ID (eliminates horizontal/vertical reflection and 180° rotation, 4x reduction). */
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

    /** Fixes rotation of top-left corner piece to 0° to eliminate rotational symmetry (eliminates 3/4 of rotationally equivalent solutions). */
    private boolean checkRotationFixing(Board board, int row, int col, int rotation) {
        // Fixe la rotation uniquement pour le coin supérieur gauche
        if (row != 0 || col != 0) {
            return true; // Pas le coin supérieur gauche, pas de contrainte
        }

        // Fixe le coin supérieur gauche à la rotation 0
        // Cela brise la symétrie rotationnelle de la solution entière
        return rotation == 0;
    }

    /** Validates board state after placement; returns true if all corner pieces satisfy lexicographic ordering constraint. */
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

    /** Logs symmetry-breaking configuration at solver start. */
    public void logConfiguration() {
        if (verbose) {
            System.out.println("  🔄 Brisure de symétrie :");
            System.out.println("     - Ordre lexicographique : " + (enableLexicographicOrdering ? "✓" : "✗"));
            System.out.println("     - Fixation de rotation : " + (enableRotationalFixing ? "✓" : "✗"));
            System.out.println("     - Élagage par réflexion : " + (enableReflectionPruning ? "✓" : "✗"));
        }
    }

    /** Returns expected search space reduction factor from symmetry breaking (e.g., 4.0 means 1/4 of original space). */
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

    /** Returns true if any symmetry-breaking strategy is enabled. */
    public boolean isEnabled() {
        return enableLexicographicOrdering || enableRotationalFixing || enableReflectionPruning;
    }
}
