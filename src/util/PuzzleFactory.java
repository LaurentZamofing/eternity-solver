package util;

import model.Piece;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Factory pour créer des puzzles prédéfinis et construire des pièces.
 */
public class PuzzleFactory {

    /**
     * Exemple de puzzle 3x3.
     * Format : {id, North, East, South, West}
     * Convention : bord extérieur = 0
     */
    public static final int[][] EXAMPLE_3x3 = new int[][]{
            {1, 0, 1, 1, 0},   // 0110
            {2, 1, 2, 3, 0},   // 1230
            {3, 3, 1, 0, 0},   // 3100

            {4, 0, 1, 3, 1},   // 0131
            {5, 3, 1, 3, 2},   // 3132
            {6, 3, 3, 0, 1},   // 3301

            {7, 0, 0, 2, 1},   // 0021
            {8, 2, 0, 1, 1},   // 2011
            {9, 1, 0, 0, 3}    // 1003
    };

    /**
     * Exemple de puzzle 4x4 (16 pièces) - VERSION ORIGINALE ORDONNÉE
     * Format : {id, North, East, South, West}
     * Convention : bord extérieur = 0
     *
     * Layout de la solution:
     * 1  2  3  4
     * 5  6  7  8
     * 9  10 11 12
     * 13 14 15 16
     */
    public static final int[][] EXAMPLE_4x4_ORDERED = new int[][]{
            // Ligne 0 (coins et bords haut)
            {1,  0, 1, 2, 0},   // Coin haut-gauche
            {2,  0, 2, 3, 1},   // Bord haut
            {3,  0, 3, 1, 2},   // Bord haut
            {4,  0, 0, 2, 3},   // Coin haut-droite

            // Ligne 1
            {5,  2, 1, 1, 0},   // Bord gauche
            {6,  3, 2, 2, 1},   // Centre
            {7,  1, 3, 3, 2},   // Centre
            {8,  2, 0, 1, 3},   // Bord droite

            // Ligne 2
            {9,  1, 2, 3, 0},   // Bord gauche
            {10, 2, 1, 1, 2},   // Centre
            {11, 3, 2, 2, 1},   // Centre
            {12, 1, 0, 3, 2},   // Bord droite

            // Ligne 3 (coins et bords bas)
            {13, 3, 1, 0, 0},   // Coin bas-gauche
            {14, 1, 2, 0, 1},   // Bord bas
            {15, 2, 3, 0, 2},   // Bord bas
            {16, 3, 0, 0, 3}    // Coin bas-droite
    };

    /**
     * Exemple de puzzle 4x4 (16 pièces) - VERSION FACILE
     * Ordre simple avec coins et bords dispersés.
     */
    public static final int[][] EXAMPLE_4x4_EASY = new int[][]{
            {1,  3, 1, 0, 0},   // Pièce originale #13 (coin bas-gauche)
            {2,  1, 3, 3, 2},   // Pièce originale #7  (centre)
            {3,  2, 1, 1, 0},   // Pièce originale #5  (bord gauche)
            {4,  0, 2, 3, 1},   // Pièce originale #2  (bord haut)

            {5,  3, 2, 2, 1},   // Pièce originale #6  (centre)
            {6,  1, 2, 0, 1},   // Pièce originale #14 (bord bas)
            {7,  0, 0, 2, 3},   // Pièce originale #4  (coin haut-droite)
            {8,  2, 1, 1, 2},   // Pièce originale #10 (centre)

            {9,  1, 2, 3, 0},   // Pièce originale #9  (bord gauche)
            {10, 3, 0, 0, 3},   // Pièce originale #16 (coin bas-droite)
            {11, 0, 1, 2, 0},   // Pièce originale #1  (coin haut-gauche)
            {12, 2, 3, 0, 2},   // Pièce originale #15 (bord bas)

            {13, 1, 0, 3, 2},   // Pièce originale #12 (bord droite)
            {14, 0, 3, 1, 2},   // Pièce originale #3  (bord haut)
            {15, 3, 2, 2, 1},   // Pièce originale #11 (centre)
            {16, 2, 0, 1, 3}    // Pièce originale #8  (bord droite)
    };

    /**
     * Exemple de puzzle 4x4 (16 pièces) - VERSION DIFFICILE V1
     * Centres groupés au début
     */
    public static final int[][] EXAMPLE_4x4_HARD_V1 = new int[][]{
            {1,  3, 2, 2, 1},   // Pièce originale #6  (centre)
            {2,  2, 1, 1, 2},   // Pièce originale #10 (centre)
            {3,  1, 3, 3, 2},   // Pièce originale #7  (centre)
            {4,  3, 2, 2, 1},   // Pièce originale #11 (centre)
            {5,  0, 3, 1, 2},   // Pièce originale #3  (bord haut)
            {6,  1, 2, 3, 0},   // Pièce originale #9  (bord gauche)
            {7,  2, 3, 0, 2},   // Pièce originale #15 (bord bas)
            {8,  2, 0, 1, 3},   // Pièce originale #8  (bord droite)
            {9,  0, 2, 3, 1},   // Pièce originale #2  (bord haut)
            {10, 2, 1, 1, 0},   // Pièce originale #5  (bord gauche)
            {11, 1, 0, 3, 2},   // Pièce originale #12 (bord droite)
            {12, 1, 2, 0, 1},   // Pièce originale #14 (bord bas)
            {13, 0, 1, 2, 0},   // Pièce originale #1  (coin haut-gauche)
            {14, 0, 0, 2, 3},   // Pièce originale #4  (coin haut-droite)
            {15, 3, 1, 0, 0},   // Pièce originale #13 (coin bas-gauche)
            {16, 3, 0, 0, 3}    // Pièce originale #16 (coin bas-droite)
    };

    /**
     * Exemple de puzzle 4x4 (16 pièces) - VERSION DIFFICILE V2
     * Alternance maximale entre types de pièces
     */
    public static final int[][] EXAMPLE_4x4_HARD_V2 = new int[][]{
            {1,  3, 2, 2, 1},   // Pièce originale #6  (centre)
            {2,  0, 3, 1, 2},   // Pièce originale #3  (bord haut)
            {3,  2, 1, 1, 2},   // Pièce originale #10 (centre)
            {4,  1, 2, 3, 0},   // Pièce originale #9  (bord gauche)
            {5,  0, 1, 2, 0},   // Pièce originale #1  (coin haut-gauche)
            {6,  1, 3, 3, 2},   // Pièce originale #7  (centre)
            {7,  2, 3, 0, 2},   // Pièce originale #15 (bord bas)
            {8,  0, 0, 2, 3},   // Pièce originale #4  (coin haut-droite)
            {9,  2, 0, 1, 3},   // Pièce originale #8  (bord droite)
            {10, 3, 2, 2, 1},   // Pièce originale #11 (centre)
            {11, 0, 2, 3, 1},   // Pièce originale #2  (bord haut)
            {12, 3, 1, 0, 0},   // Pièce originale #13 (coin bas-gauche)
            {13, 2, 1, 1, 0},   // Pièce originale #5  (bord gauche)
            {14, 1, 0, 3, 2},   // Pièce originale #12 (bord droite)
            {15, 1, 2, 0, 1},   // Pièce originale #14 (bord bas)
            {16, 3, 0, 0, 3}    // Pièce originale #16 (coin bas-droite)
    };

    /**
     * Exemple de puzzle 4x4 (16 pièces) - VERSION DIFFICILE V3
     * Pièces similaires groupées (confusion maximale)
     */
    public static final int[][] EXAMPLE_4x4_HARD_V3 = new int[][]{
            // Centres avec arêtes similaires
            {1,  3, 2, 2, 1},   // Pièce originale #6  (centre)
            {2,  3, 2, 2, 1},   // Pièce originale #11 (centre) - IDENTIQUE!
            {3,  2, 1, 1, 2},   // Pièce originale #10 (centre)
            {4,  1, 3, 3, 2},   // Pièce originale #7  (centre)
            // Coins dispersés
            {5,  3, 0, 0, 3},   // Pièce originale #16 (coin bas-droite)
            {6,  0, 2, 3, 1},   // Pièce originale #2  (bord haut)
            {7,  0, 1, 2, 0},   // Pièce originale #1  (coin haut-gauche)
            {8,  1, 2, 3, 0},   // Pièce originale #9  (bord gauche)
            // Bords droits et gauches mélangés
            {9,  2, 1, 1, 0},   // Pièce originale #5  (bord gauche)
            {10, 2, 0, 1, 3},   // Pièce originale #8  (bord droite)
            {11, 1, 0, 3, 2},   // Pièce originale #12 (bord droite)
            {12, 0, 3, 1, 2},   // Pièce originale #3  (bord haut)
            // Coins et bords bas
            {13, 3, 1, 0, 0},   // Pièce originale #13 (coin bas-gauche)
            {14, 1, 2, 0, 1},   // Pièce originale #14 (bord bas)
            {15, 2, 3, 0, 2},   // Pièce originale #15 (bord bas)
            {16, 0, 0, 2, 3}    // Pièce originale #4  (coin haut-droite)
    };

    /**
     * Exemple de puzzle 5x5 (25 pièces) - VERSION ORDONNÉE
     * Format : {id, North, East, South, West}
     * Convention : bord extérieur = 0
     *
     * Layout de la solution:
     * 1  2  3  4  5
     * 6  7  8  9  10
     * 11 12 13 14 15
     * 16 17 18 19 20
     * 21 22 23 24 25
     */
    public static final int[][] EXAMPLE_5x5_ORDERED = new int[][]{
            // Ligne 0 (coins et bords haut)
            {1,  0, 1, 1, 0},   // Coin haut-gauche (0,0)
            {2,  0, 2, 2, 1},   // Bord haut (0,1)
            {3,  0, 3, 3, 2},   // Bord haut (0,2)
            {4,  0, 4, 4, 3},   // Bord haut (0,3)
            {5,  0, 0, 5, 4},   // Coin haut-droite (0,4)

            // Ligne 1
            {6,  1, 1, 2, 0},   // Bord gauche (1,0)
            {7,  2, 2, 3, 1},   // Centre (1,1)
            {8,  3, 3, 4, 2},   // Centre (1,2)
            {9,  4, 4, 5, 3},   // Centre (1,3)
            {10, 5, 0, 1, 4},   // Bord droite (1,4)

            // Ligne 2
            {11, 2, 1, 3, 0},   // Bord gauche (2,0)
            {12, 3, 2, 4, 1},   // Centre (2,1)
            {13, 4, 3, 5, 2},   // Centre (2,2)
            {14, 5, 4, 1, 3},   // Centre (2,3)
            {15, 1, 0, 2, 4},   // Bord droite (2,4)

            // Ligne 3
            {16, 3, 1, 4, 0},   // Bord gauche (3,0)
            {17, 4, 2, 5, 1},   // Centre (3,1)
            {18, 5, 3, 1, 2},   // Centre (3,2)
            {19, 1, 4, 2, 3},   // Centre (3,3)
            {20, 2, 0, 3, 4},   // Bord droite (3,4)

            // Ligne 4 (coins et bords bas)
            {21, 4, 1, 0, 0},   // Coin bas-gauche (4,0)
            {22, 5, 2, 0, 1},   // Bord bas (4,1)
            {23, 1, 3, 0, 2},   // Bord bas (4,2)
            {24, 2, 4, 0, 3},   // Bord bas (4,3)
            {25, 3, 0, 0, 4}    // Coin bas-droite (4,4)
    };

    /**
     * Exemple de puzzle 4x4 (16 pièces) - VERSION UTILISÉE PAR DÉFAUT
     */
    public static final int[][] EXAMPLE_4x4 = EXAMPLE_4x4_HARD_V3;

    /**
     * Construit une map de pièces à partir de définitions au format tableau.
     * @param defs tableaux de définitions au format {id, N, E, S, W}
     * @return map des pièces indexées par ID
     */
    public static Map<Integer, Piece> buildPiecesFromDefs(int[][] defs) {
        Map<Integer, Piece> map = new LinkedHashMap<>();
        for (int[] d : defs) {
            if (d.length != 5) {
                throw new IllegalArgumentException(
                    "Each definition must have 5 elements: {id, N, E, S, W}"
                );
            }
            int id = d[0];
            int[] edges = new int[]{d[1], d[2], d[3], d[4]};
            map.put(id, new Piece(id, edges));
        }
        return map;
    }

    /**
     * Charge un puzzle depuis un fichier texte.
     * Format du fichier: id north east south west (un par ligne, # pour commentaires)
     *
     * @param filename chemin vers le fichier de données
     * @return map des pièces indexées par ID
     */
    public static Map<Integer, Piece> loadFromFile(String filename) {
        Map<Integer, Piece> pieces = new LinkedHashMap<>();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Ignorer les lignes vides et les commentaires
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                if (parts.length != 5) {
                    throw new IllegalArgumentException(
                        "Invalid line format (expected 5 values: id N E S W): " + line
                    );
                }

                int id = Integer.parseInt(parts[0]);
                int north = Integer.parseInt(parts[1]);
                int east = Integer.parseInt(parts[2]);
                int south = Integer.parseInt(parts[3]);
                int west = Integer.parseInt(parts[4]);

                int[] edges = new int[]{north, east, south, west};
                pieces.put(id, new Piece(id, edges));
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error reading puzzle file: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number format in puzzle file: " + e.getMessage(), e);
        }
        return pieces;
    }

    /**
     * Helper method: Load puzzle from file if available, otherwise use hardcoded fallback.
     * @param fileName Name of puzzle file (without path, e.g., "example_3x3.txt")
     * @param fallbackData Hardcoded puzzle array to use if file doesn't exist
     * @return map of pieces
     */
    private static Map<Integer, Piece> loadOrFallback(String fileName, int[][] fallbackData) {
        String dataFile = "data/puzzles/" + fileName;
        java.io.File file = new java.io.File(dataFile);
        if (file.exists()) {
            return loadFromFile(dataFile);
        }
        // Fallback to hardcoded data
        return buildPiecesFromDefs(fallbackData);
    }

    /**
     * Retourne les pièces de l'exemple 3x3.
     * Peut charger depuis un fichier si disponible, sinon utilise les données hardcodées.
     * @return map des pièces du puzzle 3x3
     */
    public static Map<Integer, Piece> createExample3x3() {
        return loadOrFallback("example_3x3.txt", EXAMPLE_3x3);
    }

    /**
     * Retourne les pièces de l'exemple 4x4 (version difficile par défaut).
     * Ordre optimisé pour maximiser la difficulté de résolution.
     * @return map des pièces du puzzle 4x4
     */
    public static Map<Integer, Piece> createExample4x4() {
        return loadOrFallback("example_4x4_hard_v3.txt", EXAMPLE_4x4);
    }

    /**
     * Retourne les pièces de l'exemple 4x4 (version difficile V1).
     * @return map des pièces du puzzle 4x4
     */
    public static Map<Integer, Piece> createExample4x4HardV1() {
        return loadOrFallback("example_4x4_hard_v1.txt", EXAMPLE_4x4_HARD_V1);
    }

    /**
     * Retourne les pièces de l'exemple 4x4 (version difficile V2).
     * @return map des pièces du puzzle 4x4
     */
    public static Map<Integer, Piece> createExample4x4HardV2() {
        return loadOrFallback("example_4x4_hard_v2.txt", EXAMPLE_4x4_HARD_V2);
    }

    /**
     * Retourne les pièces de l'exemple 4x4 (version difficile V3).
     * @return map des pièces du puzzle 4x4
     */
    public static Map<Integer, Piece> createExample4x4HardV3() {
        return loadOrFallback("example_4x4_hard_v3.txt", EXAMPLE_4x4_HARD_V3);
    }

    /**
     * Retourne les pièces de l'exemple 4x4 (version facile).
     * @return map des pièces du puzzle 4x4 en ordre facile
     */
    public static Map<Integer, Piece> createExample4x4Easy() {
        return loadOrFallback("example_4x4_easy.txt", EXAMPLE_4x4_EASY);
    }

    /**
     * Retourne les pièces de l'exemple 4x4 (version ordonnée, triviale).
     * Les pièces sont dans leur position finale, seules les rotations doivent être trouvées.
     * @return map des pièces du puzzle 4x4 ordonnées
     */
    public static Map<Integer, Piece> createExample4x4Ordered() {
        return loadOrFallback("example_4x4_ordered.txt", EXAMPLE_4x4_ORDERED);
    }

    /**
     * Exemple de puzzle 5x5 (25 pièces) - VERSION DIFFICILE V1
     * Utilise seulement 4 motifs (0,1,2,3) pour maximiser la confusion
     * Plusieurs pièces identiques ou très similaires
     */
    public static final int[][] EXAMPLE_5x5_HARD_V1 = new int[][]{
            // Centres similaires groupés (9 pièces)
            {1,  2, 2, 2, 2},   // Centre IDENTIQUE x3
            {2,  2, 2, 2, 2},   // Centre IDENTIQUE x3
            {3,  2, 2, 2, 2},   // Centre IDENTIQUE x3
            {4,  1, 2, 3, 1},   // Centre
            {5,  3, 1, 2, 3},   // Centre
            {6,  1, 3, 1, 2},   // Centre
            {7,  2, 1, 3, 2},   // Centre
            {8,  3, 2, 1, 3},   // Centre
            {9,  1, 1, 3, 3},   // Centre

            // Bords similaires (12 pièces)
            {10, 0, 1, 2, 1},   // Bord haut
            {11, 0, 2, 1, 2},   // Bord haut
            {12, 0, 1, 3, 1},   // Bord haut
            {13, 1, 1, 2, 0},   // Bord gauche
            {14, 2, 1, 3, 0},   // Bord gauche
            {15, 3, 1, 1, 0},   // Bord gauche
            {16, 2, 0, 1, 1},   // Bord droite
            {17, 1, 0, 2, 2},   // Bord droite
            {18, 3, 0, 3, 3},   // Bord droite
            {19, 1, 2, 0, 1},   // Bord bas
            {20, 2, 1, 0, 2},   // Bord bas
            {21, 3, 3, 0, 1},   // Bord bas

            // Coins dispersés (4 pièces)
            {22, 0, 1, 1, 0},   // Coin haut-gauche
            {23, 0, 0, 2, 1},   // Coin haut-droite
            {24, 1, 1, 0, 0},   // Coin bas-gauche
            {25, 3, 0, 0, 3}    // Coin bas-droite
    };

    /**
     * Retourne les pièces de l'exemple 5x5 (version ordonnée).
     * @return map des pièces du puzzle 5x5 ordonnées
     */
    public static Map<Integer, Piece> createExample5x5Ordered() {
        return loadOrFallback("example_5x5_ordered.txt", EXAMPLE_5x5_ORDERED);
    }

    /**
     * Retourne les pièces de l'exemple 5x5 (version difficile V1 - peu de motifs).
     * @return map des pièces du puzzle 5x5 avec confusion maximale
     */
    public static Map<Integer, Piece> createExample5x5HardV1() {
        return loadOrFallback("example_5x5_hard_v1.txt", EXAMPLE_5x5_HARD_V1);
    }

    /**
     * Retourne les pièces de l'exemple 5x5 (version par défaut = difficile V1).
     * @return map des pièces du puzzle 5x5
     */
    public static Map<Integer, Piece> createExample5x5() {
        return loadOrFallback("example_5x5_hard_v1.txt", EXAMPLE_5x5_HARD_V1);
    }

    /**
     * Charge les vraies pièces d'Eternity II à partir d'un fichier.
     * Format du fichier : chaque ligne contient 4 nombres séparés par des espaces
     * représentant les arêtes dans l'ordre : Nord, Sud, Ouest, Est
     * (format standard e2pieces.txt)
     *
     * @param filename chemin vers le fichier contenant les pièces
     * @return map des pièces Eternity II (256 pièces pour un puzzle 16x16)
     */
    public static Map<Integer, Piece> loadEternityIIFromFile(String filename) {
        Map<Integer, Piece> pieces = new LinkedHashMap<>();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(filename))) {
            String line;
            int id = 1;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length != 4) {
                    throw new IllegalArgumentException(
                        "Invalid line format at piece " + id + ": " + line
                    );
                }

                // Format fichier: N, S, W, E
                // Format interne: N, E, S, W
                int north = Integer.parseInt(parts[0]);
                int south = Integer.parseInt(parts[1]);
                int west = Integer.parseInt(parts[2]);
                int east = Integer.parseInt(parts[3]);

                int[] edges = new int[]{north, east, south, west};
                pieces.put(id, new Piece(id, edges));
                id++;
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error reading Eternity II file: " + e.getMessage(), e);
        }
        return pieces;
    }

    /**
     * Puzzle de validation 6x6 (36 pièces).
     * Format : {id, North, East, South, West}
     * Convention : bord extérieur = 0
     * Lignes : A-F (0-5)
     * Colonnes : 1-6 (0-5)
     */
    public static final int[][] VALIDATION_6x6 = new int[][]{
            {1,  1, 2, 2, 1},
            {2,  1, 2, 1, 2},
            {3,  1, 2, 1, 3},
            {4,  1, 2, 2, 3},
            {5,  1, 2, 1, 3},
            {6,  1, 1, 2, 3},
            {7,  2, 5, 0, 4},
            {8,  1, 2, 2, 1},
            {9,  2, 1, 1, 1},
            {10, 6, 2, 6, 0},

            {11, 6, 1, 4, 0},
            {12, 0, 5, 2, 6},
            {13, 6, 0, 7, 1},
            {14, 5, 7, 0, 0},
            {15, 0, 4, 2, 7},
            {16, 1, 6, 0, 4},
            {17, 7, 0, 7, 1},
            {18, 6, 2, 4, 0},
            {19, 2, 1, 2, 1},
            {20, 0, 6, 2, 7},

            {21, 2, 6, 0, 4},
            {22, 4, 1, 5, 0},
            {23, 2, 4, 0, 7},
            {24, 2, 2, 1, 1},
            {25, 5, 2, 4, 0},
            {26, 0, 5, 5, 0},
            {27, 2, 1, 1, 1},
            {28, 0, 7, 2, 5},
            {29, 3, 3, 1, 1},
            {30, 2, 2, 3, 2},

            {31, 7, 0, 6, 1},
            {32, 2, 2, 2, 1},
            {33, 2, 2, 3, 1},
            {34, 0, 7, 4, 0},
            {35, 2, 1, 2, 2},
            {36, 0, 0, 5, 5}
    };

    /**
     * Retourne les pièces du puzzle de validation 6x6.
     * @return map des pièces du puzzle 6x6
     */
    public static Map<Integer, Piece> createValidation6x6() {
        return loadOrFallback("validation_6x6.txt", VALIDATION_6x6);
    }

    /**
     * Puzzle 6x12 (72 pièces).
     * Format : {id, North, East, South, West}
     * Convention : bord extérieur = 0
     * Lignes : A-F (0-5)
     * Colonnes : 1-12 (0-11)
     */
    public static final int[][] PUZZLE_6x12 = new int[][]{
            {1,  4, 19, 22, 21},
            {2, 21,  4, 22,  4},
            {3, 21,  1,  0,  5},
            {4, 22, 21,  4, 22},
            {5, 21,  4,  4, 21},
            {6, 17,  0,  0,  9},
            {7, 22, 22,  4, 21},
            {8, 21, 21, 21, 22},
            {9, 21, 22,  4,  4},
            {10, 21,  4,  4,  4},

            {11, 21,  4, 22, 22},
            {12,  0, 13, 17,  0},
            {13,  4,  4, 22, 22},
            {14, 21, 21, 21, 22},
            {15,  4, 21, 21, 19},
            {16,  4, 21, 22, 22},
            {17,  4, 22, 22, 22},
            {18, 22,  4,  4,  4},
            {19,  5,  0,  9, 22},
            {20, 13,  0,  5,  4},

            {21, 19, 21, 21, 21},
            {22,  4,  4, 22, 22},
            {23, 21, 21, 22, 22},
            {24, 13,  0, 17, 21},
            {25, 21, 21, 21,  4},
            {26, 21, 22, 22, 19},
            {27,  0, 17, 22,  1},
            {28, 22,  4,  4,  4},
            {29,  0,  1, 22,  5},
            {30, 22,  4, 22,  4},

            {31, 21, 22,  4,  4},
            {32,  9,  0,  9, 21},
            {33, 21,  4, 21, 22},
            {34, 22,  4, 21, 21},
            {35,  4,  4, 21, 22},
            {36, 21,  5,  0,  5},
            {37, 21,  5,  0,  5},
            {38,  4, 21,  4,  4},
            {39, 21,  4, 22, 21},
            {40, 22,  4, 22, 22},

            {41,  1, 22,  5,  0},
            {42,  4,  9,  0,  1},
            {43,  4, 19, 21, 21},
            {44, 22, 13,  0, 13},
            {45, 21,  1,  0, 13},
            {46,  4, 21,  4, 22},
            {47, 21,  5,  0,  9},
            {48,  4,  4, 21, 22},
            {49, 17,  4,  1,  0},
            {50,  4, 17,  0,  5},

            {51, 21,  4, 22, 22},
            {52,  0,  9,  4,  1},
            {53,  4, 17,  0,  1},
            {54,  0, 17, 21, 13},
            {55, 21,  4, 21,  4},
            {56,  0,  0,  1,  1},
            {57, 17,  4, 13,  0},
            {58,  4,  4,  4, 21},
            {59, 21, 22,  4, 22},
            {60,  9,  0,  1, 22},

            {61, 22,  9,  0,  5},
            {62, 17, 21,  5,  0},
            {63, 22, 21, 22, 21},
            {64, 21,  9,  0, 13},
            {65,  0, 13, 22,  9},
            {66, 21, 22,  4, 22},
            {67, 13,  0, 13, 19},
            {68, 21, 22, 21,  4},
            {69,  0, 13, 17,  0},
            {70, 22, 21, 21, 22},

            {71,  4, 17,  0,  5},
            {72, 13,  0,  9, 22}
    };

    /**
     * Retourne les pièces du puzzle 6x12.
     * @return map des pièces du puzzle 6x12
     */
    public static Map<Integer, Piece> createPuzzle6x12() {
        return loadOrFallback("puzzle_6x12.txt", PUZZLE_6x12);
    }

    /**
     * Puzzle 16x16 personnalisé (256 pièces).
     * Format : {id, North, East, South, West}
     */
    public static final int[][] PUZZLE_16x16 = new int[][]{
            // Coins (4 pièces)
            {1, 1, 2, 0, 0},        // Coin haut-gauche
            {2, 1, 3, 0, 0},        // Coin haut-droite
            {3, 4, 2, 0, 0},        // Coin bas-gauche
            {4, 2, 4, 0, 0},        // Coin bas-droite

            // Bords (56 pièces: 5-60)
            {5, 1, 6, 1, 0},
            {6, 1, 7, 4, 0},
            {7, 1, 8, 1, 0},
            {8, 1, 8, 5, 0},
            {9, 1, 9, 2, 0},
            {10, 1, 10, 3, 0},
            {11, 1, 11, 4, 0},
            {12, 1, 12, 3, 0},
            {13, 1, 12, 5, 0},
            {14, 1, 13, 3, 0},
            {15, 4, 7, 1, 0},
            {16, 4, 14, 2, 0},
            {17, 4, 15, 5, 0},
            {18, 4, 16, 5, 0},
            {19, 4, 10, 4, 0},
            {20, 4, 11, 4, 0},
            {21, 4, 17, 3, 0},
            {22, 4, 18, 1, 0},
            {23, 4, 18, 5, 0},
            {24, 4, 19, 1, 0},
            {25, 4, 13, 1, 0},
            {26, 2, 6, 4, 0},
            {27, 2, 6, 2, 0},
            {28, 2, 7, 2, 0},
            {29, 2, 14, 2, 0},
            {30, 2, 8, 2, 0},
            {31, 2, 15, 4, 0},
            {32, 2, 9, 4, 0},
            {33, 2, 16, 2, 0},
            {34, 2, 17, 2, 0},
            {35, 2, 18, 2, 0},
            {36, 2, 19, 3, 0},
            {37, 2, 20, 2, 0},
            {38, 2, 21, 4, 0},
            {39, 2, 13, 3, 0},
            {40, 1, 2, 0, 4},
            {41, 2, 3, 0, 2},
            {42, 3, 4, 0, 2},
            {43, 4, 5, 0, 2},
            {44, 5, 6, 0, 2},
            {45, 6, 6, 0, 1},
            {46, 6, 7, 0, 1},
            {47, 6, 8, 0, 2},
            {48, 6, 9, 0, 1},
            {49, 6, 10, 0, 1},
            {50, 7, 11, 0, 4},
            {51, 7, 12, 0, 4},
            {52, 7, 5, 0, 1},
            {53, 7, 13, 0, 4},
            {54, 7, 14, 0, 1},
            {55, 7, 15, 0, 4},
            {56, 7, 8, 0, 4},
            {57, 7, 16, 0, 1},
            {58, 7, 17, 0, 4},
            {59, 7, 18, 0, 1},
            {60, 7, 19, 0, 4},

            // Pièces centrales (196 pièces: 61-256)
            {61, 6, 6, 8, 14},
            {62, 6, 9, 11, 17},
            {63, 6, 10, 19, 13},
            {64, 6, 11, 10, 12},
            {65, 6, 12, 20, 15},
            {66, 6, 14, 19, 13},
            {67, 6, 15, 12, 18},
            {68, 6, 17, 21, 13},
            {69, 8, 6, 16, 18},
            {70, 8, 8, 10, 14},
            {71, 8, 9, 18, 12},
            {72, 8, 11, 18, 17},
            {73, 8, 13, 10, 19},
            {74, 8, 16, 14, 16},
            {75, 8, 18, 17, 13},
            {76, 8, 19, 13, 21},
            {77, 9, 6, 15, 13},
            {78, 9, 9, 11, 13},
            {79, 9, 10, 21, 16},
            {80, 9, 11, 18, 12},
            {81, 9, 14, 19, 20},
            {82, 9, 15, 20, 14},
            {83, 9, 16, 21, 18},
            {84, 9, 20, 19, 17},
            {85, 10, 6, 19, 15},
            {86, 10, 7, 12, 19},
            {87, 10, 9, 17, 17},
            {88, 10, 10, 12, 14},
            {89, 10, 11, 19, 16},
            {90, 10, 12, 21, 18},
            {91, 10, 13, 21, 17},
            {92, 10, 17, 14, 14},
            {93, 11, 6, 17, 18},
            {94, 11, 8, 13, 18},
            {95, 11, 9, 12, 19},
            {96, 11, 10, 13, 14},
            {97, 11, 11, 17, 16},
            {98, 11, 12, 19, 17},
            {99, 11, 14, 19, 19},
            {100, 11, 15, 21, 17},
            {101, 12, 6, 15, 16},
            {102, 12, 8, 21, 13},
            {103, 12, 9, 20, 17},
            {104, 12, 10, 20, 16},
            {105, 12, 11, 14, 18},
            {106, 12, 12, 14, 19},
            {107, 12, 13, 21, 19},
            {108, 12, 14, 21, 17},
            {109, 13, 6, 16, 14},
            {110, 13, 7, 17, 14},
            {111, 13, 8, 16, 15},
            {112, 13, 11, 20, 16},
            {113, 13, 12, 17, 15},
            {114, 13, 13, 20, 19},
            {115, 13, 14, 20, 18},
            {116, 13, 20, 21, 18},
            {117, 14, 6, 13, 19},
            {118, 14, 9, 19, 16},
            {119, 14, 10, 17, 16},
            {120, 14, 11, 16, 19},
            {121, 14, 13, 19, 16},
            {122, 14, 15, 21, 19},
            {123, 14, 17, 21, 19},
            {124, 14, 20, 20, 17},
            {125, 15, 6, 19, 19},
            {126, 15, 8, 17, 18},
            {127, 15, 9, 19, 16},
            {128, 15, 11, 20, 17},
            {129, 15, 12, 19, 17},
            {130, 15, 16, 20, 19},
            {131, 15, 17, 20, 19},
            {132, 15, 21, 20, 18},
            {133, 16, 6, 20, 19},
            {134, 16, 7, 14, 18},
            {135, 16, 8, 17, 16},
            {136, 16, 9, 14, 17},
            {137, 16, 11, 20, 20},
            {138, 16, 12, 17, 17},
            {139, 16, 14, 21, 19},
            {140, 16, 17, 21, 17},
            {141, 17, 6, 21, 18},
            {142, 17, 7, 18, 15},
            {143, 17, 8, 20, 17},
            {144, 17, 9, 18, 18},
            {145, 17, 10, 21, 19},
            {146, 17, 11, 18, 18},
            {147, 17, 12, 20, 19},
            {148, 17, 13, 21, 18},
            {149, 18, 6, 14, 18},
            {150, 18, 7, 20, 17},
            {151, 18, 8, 19, 20},
            {152, 18, 9, 15, 18},
            {153, 18, 10, 17, 18},
            {154, 18, 11, 20, 19},
            {155, 18, 13, 20, 19},
            {156, 18, 14, 20, 19},
            {157, 19, 6, 17, 20},
            {158, 19, 7, 16, 16},
            {159, 19, 8, 20, 20},
            {160, 19, 9, 16, 19},
            {161, 19, 10, 18, 18},
            {162, 19, 11, 17, 19},
            {163, 19, 12, 18, 20},
            {164, 19, 15, 21, 20},
            {165, 20, 6, 13, 19},
            {166, 20, 7, 18, 18},
            {167, 20, 8, 17, 20},
            {168, 20, 9, 21, 20},
            {169, 20, 10, 19, 18},
            {170, 20, 11, 18, 21},
            {171, 20, 13, 21, 21},
            {172, 20, 14, 20, 20},
            {173, 21, 6, 18, 20},
            {174, 21, 7, 15, 19},
            {175, 21, 8, 19, 21},
            {176, 21, 9, 16, 20},
            {177, 21, 10, 13, 19},
            {178, 21, 11, 19, 21},
            {179, 21, 13, 20, 20},
            {180, 21, 16, 21, 20},
            {181, 5, 6, 9, 13},
            {182, 5, 8, 16, 14},
            {183, 5, 9, 15, 12},
            {184, 5, 10, 13, 16},
            {185, 5, 11, 15, 14},
            {186, 5, 12, 19, 15},
            {187, 5, 14, 21, 16},
            {188, 5, 17, 19, 18},
            {189, 1, 6, 6, 15},
            {190, 1, 7, 8, 14},
            {191, 1, 9, 11, 12},
            {192, 1, 10, 11, 14},
            {193, 1, 11, 13, 15},
            {194, 1, 12, 17, 16},
            {195, 1, 14, 19, 18},
            {196, 1, 16, 17, 18},
            {197, 3, 6, 13, 16},
            {198, 3, 7, 8, 15},
            {199, 3, 8, 9, 14},
            {200, 3, 9, 13, 12},
            {201, 3, 10, 15, 14},
            {202, 3, 12, 17, 17},
            {203, 3, 16, 20, 15},
            {204, 3, 19, 21, 18},
            {205, 4, 6, 9, 16},
            {206, 4, 7, 9, 15},
            {207, 4, 8, 13, 16},
            {208, 4, 9, 11, 14},
            {209, 4, 10, 13, 16},
            {210, 4, 11, 13, 17},
            {211, 4, 14, 21, 17},
            {212, 4, 19, 20, 19},
            {213, 5, 6, 12, 16},
            {214, 5, 7, 15, 14},
            {215, 5, 8, 14, 16},
            {216, 5, 10, 16, 15},
            {217, 5, 13, 18, 18},
            {218, 5, 15, 18, 17},
            {219, 5, 18, 20, 17},
            {220, 5, 19, 21, 19},
            {221, 6, 7, 9, 16},
            {222, 6, 8, 12, 16},
            {223, 6, 10, 13, 16},
            {224, 6, 12, 14, 17},
            {225, 6, 13, 15, 17},
            {226, 6, 15, 20, 17},
            {227, 6, 16, 19, 17},
            {228, 6, 20, 21, 19},
            {229, 7, 6, 14, 17},
            {230, 7, 7, 12, 16},
            {231, 7, 9, 15, 17},
            {232, 7, 10, 17, 17},
            {233, 7, 11, 15, 17},
            {234, 7, 13, 17, 18},
            {235, 7, 15, 19, 18},
            {236, 7, 20, 20, 18},
            {237, 8, 6, 11, 17},
            {238, 8, 7, 14, 17},
            {239, 8, 8, 12, 18},
            {240, 8, 10, 16, 17},
            {241, 8, 12, 18, 18},
            {242, 8, 14, 20, 18},
            {243, 8, 15, 19, 19},
            {244, 8, 20, 21, 20},
            {245, 9, 6, 14, 18},
            {246, 9, 7, 13, 18},
            {247, 11, 11, 13, 21},
            {248, 11, 17, 18, 17},
            {249, 11, 18, 17, 13},
            {250, 11, 19, 20, 13},
            {251, 17, 17, 21, 12},
            {252, 17, 18, 12, 18},
            {253, 18, 12, 20, 19},
            {254, 19, 20, 13, 20},
            {255, 19, 21, 20, 21},
            {256, 12, 21, 13, 21}
    };

    /**
     * Retourne les pièces du puzzle 16x16.
     * @return map des pièces du puzzle 16x16
     */
    public static Map<Integer, Piece> createPuzzle16x16() {
        return loadOrFallback("puzzle_16x16.txt", PUZZLE_16x16);
    }

    /**
     * Charge le vrai puzzle Eternity II (16x16, 256 pièces).
     * @return map des pièces Eternity II
     */
    public static Map<Integer, Piece> createEternityII() {
        return loadEternityIIFromFile("data/eternity2_256_pieces.txt");
    }
}
