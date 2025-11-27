package model;

import java.util.Arrays;

/**
 * Représente une pièce du puzzle avec ses arêtes.
 * Les pièces sont immuables - les rotations génèrent de nouveaux tableaux.
 */
public class Piece {
    private final int id;            // identifiant unique de la pièce
    private final int[] edges;       // arêtes dans l'ordre [N, E, S, W]

    /**
     * Constructeur
     * @param id identifiant unique de la pièce
     * @param edges tableau d'entiers de longueur 4 représentant [N, E, S, W]
     * @throws IllegalArgumentException si edges n'a pas exactement 4 éléments
     */
    public Piece(int id, int[] edges) {
        if (edges.length != 4) {
            throw new IllegalArgumentException("edges must have length 4");
        }
        this.id = id;
        this.edges = Arrays.copyOf(edges, 4);
    }

    /**
     * Retourne l'identifiant de la pièce.
     */
    public int getId() {
        return id;
    }

    /**
     * Retourne une copie défensive des arêtes de la pièce.
     * La copie garantit l'immutabilité de la pièce.
     */
    public int[] getEdges() {
        return Arrays.copyOf(edges, 4);
    }

    /**
     * Retourne un nouveau tableau représentant les arêtes après rotation clockwise k*90°.
     * k est réduit modulo 4.
     * Mapping (90° cw) : newN = W, newE = N, newS = E, newW = S
     *
     * @param k nombre de rotations de 90° dans le sens horaire
     * @return nouveau tableau d'arêtes après rotation (ou tableau interne si k=0)
     */
    public int[] edgesRotated(int k) {
        k = ((k % 4) + 4) % 4;
        if (k == 0) return Arrays.copyOf(edges, 4);  // Return defensive copy for immutability

        // Optimisation: rotation directe sans boucle
        int n = edges[0], e = edges[1], s = edges[2], w = edges[3];
        switch (k) {
            case 1:  // 90° clockwise: N<-W, E<-N, S<-E, W<-S
                return new int[]{w, n, e, s};
            case 2:  // 180°: N<-S, E<-W, S<-N, W<-E
                return new int[]{s, w, n, e};
            case 3:  // 270° clockwise: N<-E, E<-S, S<-W, W<-N
                return new int[]{e, s, w, n};
            default:
                return Arrays.copyOf(edges, 4);  // Should never happen, but return copy
        }
    }

    /**
     * Retourne le nombre de rotations uniques pour cette pièce.
     * Une pièce peut avoir 1, 2 ou 4 rotations uniques selon sa symétrie.
     *
     * @return nombre de rotations distinctes (1, 2 ou 4)
     */
    public int getUniqueRotationCount() {
        int n = edges[0], e = edges[1], s = edges[2], w = edges[3];

        // Cas 1: Toutes les arêtes identiques (symétrie 4-fold) -> 1 rotation unique
        if (n == e && e == s && s == w) {
            return 1;
        }

        // Cas 2: Symétrie 2-fold (opposés identiques) -> 2 rotations uniques
        if (n == s && e == w) {
            return 2;
        }

        // Cas 3: Aucune symétrie -> 4 rotations uniques
        return 4;
    }

    @Override
    public String toString() {
        return "Piece(id=" + id + ", edges=" + Arrays.toString(edges) + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Piece other = (Piece) obj;
        return id == other.id && Arrays.equals(edges, other.edges);
    }

    @Override
    public int hashCode() {
        return 31 * id + Arrays.hashCode(edges);
    }
}
