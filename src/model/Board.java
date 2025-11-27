package model;

import java.util.Arrays;
import java.util.Map;

/**
 * Représente la grille de jeu du puzzle.
 * Gère le placement et le retrait de pièces.
 */
public class Board {
    private final int rows;
    private final int cols;
    private final Placement[][] grid;

    /**
     * Constructeur
     * @param rows nombre de lignes
     * @param cols nombre de colonnes
     */
    public Board(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("rows and cols must be positive");
        }
        this.rows = rows;
        this.cols = cols;
        this.grid = new Placement[rows][cols];
    }

    /**
     * Retourne le nombre de lignes.
     */
    public int getRows() {
        return rows;
    }

    /**
     * Retourne le nombre de colonnes.
     */
    public int getCols() {
        return cols;
    }

    /**
     * Vérifie si une case est vide.
     * @param r ligne
     * @param c colonne
     * @return true si la case est vide
     */
    public boolean isEmpty(int r, int c) {
        validateCoordinates(r, c);
        return grid[r][c] == null;
    }

    /**
     * Retourne le placement à la position (r, c).
     * @param r ligne
     * @param c colonne
     * @return le placement ou null si vide
     */
    public Placement getPlacement(int r, int c) {
        validateCoordinates(r, c);
        return grid[r][c];
    }

    /**
     * Place une pièce à la position (r, c) avec une rotation donnée.
     * @param r ligne
     * @param c colonne
     * @param piece pièce à placer
     * @param rotation rotation à appliquer (0-3)
     */
    public void place(int r, int c, Piece piece, int rotation) {
        validateCoordinates(r, c);
        int[] edges = piece.edgesRotated(rotation);
        grid[r][c] = new Placement(piece.getId(), rotation, edges);
    }

    /**
     * Retire la pièce à la position (r, c).
     * @param r ligne
     * @param c colonne
     */
    public void remove(int r, int c) {
        validateCoordinates(r, c);
        grid[r][c] = null;
    }

    /**
     * Affiche une représentation simple de la grille.
     * @param piecesById map des pièces par ID (optionnel pour affichage détaillé)
     */
    public void prettyPrint(Map<Integer, Piece> piecesById) {
        System.out.println("Board " + rows + "x" + cols + ":");
        for (int r = 0; r < rows; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == null) {
                    sb.append("---- ");
                } else {
                    sb.append(String.format("%s ", grid[r][c].toString()));
                }
            }
            System.out.println(sb.toString());
        }
        System.out.println();

        if (piecesById != null) {
            System.out.println("Détails (coord -> id,rot,edges après rotation):");
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Placement p = grid[r][c];
                    if (p != null) {
                        System.out.printf("(%d,%d) => id=%d, rot=%d, edges=%s%n",
                                r, c, p.getPieceId(), p.getRotation(), Arrays.toString(p.edges));
                    }
                }
            }
            System.out.println();
        }
    }

    /**
     * Calcule le score du board basé sur le nombre d'arêtes internes correctes.
     *
     * Score = nombre d'arêtes internes matchées (bordures non comptées car obligatoires)
     *
     * Pour un board 16x16:
     * - Arêtes internes horizontales: (cols-1) × rows = 15 × 16 = 240
     * - Arêtes internes verticales: cols × (rows-1) = 16 × 15 = 240
     * - Total max: 480 arêtes internes
     *
     * Les arêtes de bordure (qui doivent être 0) ne sont pas comptées dans le score
     * car elles sont des contraintes obligatoires, pas un objectif à maximiser.
     *
     * @return [score actuel, score maximum possible]
     */
    public int[] calculateScore() {
        int correctEdges = 0;

        // Calculer le score maximum théorique (seulement les arêtes internes)
        int maxInternalEdges = (rows - 1) * cols + rows * (cols - 1);
        int maxScore = maxInternalEdges;

        // Compter les pièces placées
        int placedPieces = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] != null) {
                    placedPieces++;
                }
            }
        }

        // Si aucune pièce placée, retourner 0
        if (placedPieces == 0) {
            return new int[]{0, maxScore};
        }

        // Vérifier les arêtes internes horizontales
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols - 1; c++) {
                Placement left = grid[r][c];
                Placement right = grid[r][c + 1];

                // Si les deux pièces sont placées
                if (left != null && right != null) {
                    // L'arête Est de la pièce gauche doit matcher l'arête Ouest de la pièce droite
                    if (left.edges[1] == right.edges[3]) {
                        correctEdges++;
                    }
                }
            }
        }

        // Vérifier les arêtes internes verticales
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols; c++) {
                Placement top = grid[r][c];
                Placement bottom = grid[r + 1][c];

                // Si les deux pièces sont placées
                if (top != null && bottom != null) {
                    // L'arête Sud de la pièce haute doit matcher l'arête Nord de la pièce basse
                    if (top.edges[2] == bottom.edges[0]) {
                        correctEdges++;
                    }
                }
            }
        }

        // Les arêtes de bordure ne sont pas comptées dans le score
        // (ce sont des contraintes obligatoires, pas un objectif)

        return new int[]{correctEdges, maxScore};
    }

    /**
     * Calcule et affiche le score du board.
     */
    public void printScore() {
        int[] score = calculateScore();
        int current = score[0];
        int max = score[1];
        double percentage = max > 0 ? (current * 100.0 / max) : 0.0;

        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║                    SCORE DU BOARD                      ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.printf("Arêtes internes correctes: %d / %d (%.1f%%)%n", current, max, percentage);

        // Décomposition du score
        int internalH = (rows - 1) * cols;
        int internalV = rows * (cols - 1);

        System.out.printf("  - Arêtes horizontales: %d max%n", internalH);
        System.out.printf("  - Arêtes verticales: %d max%n", internalV);
        System.out.println("  (Les bordures ne comptent pas dans le score)");
        System.out.println();
    }

    /**
     * Valide que les coordonnées sont dans les limites de la grille.
     */
    private void validateCoordinates(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) {
            throw new IndexOutOfBoundsException(
                String.format("Invalid coordinates (%d, %d) for board %dx%d", r, c, rows, cols)
            );
        }
    }
}
