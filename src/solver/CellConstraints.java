package solver;

/**
 * Contraintes pré-calculées pour une position de cellule dans le plateau de puzzle.
 * Stocke les informations de bord et les valeurs de bords requises pour éviter les calculs répétés.
 */
public class CellConstraints {
    // Drapeaux de bord (empaquetés dans un seul byte pour l'efficacité du cache)
    public final byte borderMask;  // bits: 0=nord, 1=est, 2=sud, 3=ouest

    // Positions des voisins (ligne, colonne) - pré-calculées pour éviter les recherches dans les tableaux
    public final int northRow, northCol;
    public final int eastRow, eastCol;
    public final int southRow, southCol;
    public final int westRow, westCol;

    // Masque des exigences de bords : si le bit est activé, ce bord DOIT être 0
    // bits: 0=nord, 1=est, 2=sud, 3=ouest
    public final byte requiredZeroMask;

    private CellConstraints(int row, int col, int rows, int cols) {
        // Calculer les drapeaux de bord
        boolean isNorthBorder = (row == 0);
        boolean isEastBorder = (col == cols - 1);
        boolean isSouthBorder = (row == rows - 1);
        boolean isWestBorder = (col == 0);

        this.borderMask = (byte) (
            (isNorthBorder ? 1 : 0) |
            (isEastBorder ? 2 : 0) |
            (isSouthBorder ? 4 : 0) |
            (isWestBorder ? 8 : 0)
        );

        this.requiredZeroMask = borderMask;

        // Pré-calculer les positions des voisins
        this.northRow = row - 1;
        this.northCol = col;
        this.eastRow = row;
        this.eastCol = col + 1;
        this.southRow = row + 1;
        this.southCol = col;
        this.westRow = row;
        this.westCol = col - 1;
    }

    /**
     * Vérifier si cette cellule est sur le bord nord
     */
    public boolean isNorthBorder() {
        return (borderMask & 1) != 0;
    }

    /**
     * Vérifier si cette cellule est sur le bord est
     */
    public boolean isEastBorder() {
        return (borderMask & 2) != 0;
    }

    /**
     * Vérifier si cette cellule est sur le bord sud
     */
    public boolean isSouthBorder() {
        return (borderMask & 4) != 0;
    }

    /**
     * Vérifier si cette cellule est sur le bord ouest
     */
    public boolean isWestBorder() {
        return (borderMask & 8) != 0;
    }

    /**
     * Méthode fabrique pour créer une matrice de contraintes pour un plateau
     */
    public static CellConstraints[][] createConstraintsMatrix(int rows, int cols) {
        CellConstraints[][] constraints = new CellConstraints[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                constraints[r][c] = new CellConstraints(r, c, rows, cols);
            }
        }
        return constraints;
    }
}
