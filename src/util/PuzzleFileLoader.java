package util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Loader pour charger des puzzles depuis des fichiers texte.
 *
 * Format de fichier attendu:
 * <pre>
 * # Commentaires (lignes commençant par #)
 * id north east south west
 * 1 0 1 2 0
 * 2 0 2 3 1
 * ...
 * </pre>
 *
 * <p>Ce loader permet de déplacer progressivement les données de puzzle
 * depuis les tableaux hardcodés dans PuzzleFactory vers des fichiers externes,
 * facilitant la maintenance et l'ajout de nouveaux puzzles.</p>
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 * @see PuzzleFactory
 */
public class PuzzleFileLoader {

    /** Répertoire par défaut pour les fichiers de puzzle */
    private static final String DEFAULT_PUZZLE_DIR = "data/puzzles/";

    /** Extension de fichier par défaut */
    private static final String DEFAULT_EXTENSION = ".txt";

    /**
     * Charge un puzzle depuis un fichier.
     *
     * @param puzzleName Nom du puzzle (sans extension, ex: "example_3x3")
     * @return Tableau de pièces au format {id, north, east, south, west}
     * @throws IOException Si le fichier ne peut pas être lu
     * @throws IllegalArgumentException Si le format est invalide
     */
    public static int[][] loadPuzzle(String puzzleName) throws IOException {
        String filename = DEFAULT_PUZZLE_DIR + puzzleName + DEFAULT_EXTENSION;
        return loadPuzzleFromFile(filename);
    }

    /**
     * Charge un puzzle depuis un chemin de fichier spécifique.
     *
     * @param filepath Chemin complet vers le fichier
     * @return Tableau de pièces au format {id, north, east, south, west}
     * @throws IOException Si le fichier ne peut pas être lu
     * @throws IllegalArgumentException Si le format est invalide
     */
    public static int[][] loadPuzzleFromFile(String filepath) throws IOException {
        Path path = Paths.get(filepath);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("Puzzle file not found: " + filepath);
        }

        List<int[]> pieces = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Ignorer les lignes vides et les commentaires
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parser la ligne
                try {
                    int[] piece = parsePieceLine(line);
                    pieces.add(piece);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    throw new IllegalArgumentException(
                        "Invalid format at line " + lineNumber + " in " + filepath + ": " + line, e);
                }
            }
        }

        if (pieces.isEmpty()) {
            throw new IllegalArgumentException("No pieces found in file: " + filepath);
        }

        // Convertir List<int[]> en int[][]
        return pieces.toArray(new int[0][]);
    }

    /**
     * Parse une ligne de définition de pièce.
     *
     * Format attendu: "id north east south west"
     * Exemple: "1 0 1 2 0"
     *
     * @param line Ligne à parser
     * @return Tableau [id, north, east, south, west]
     * @throws NumberFormatException Si les valeurs ne sont pas des entiers
     * @throws ArrayIndexOutOfBoundsException Si le nombre de valeurs est incorrect
     */
    private static int[] parsePieceLine(String line) {
        String[] parts = line.split("\\s+");

        if (parts.length != 5) {
            throw new ArrayIndexOutOfBoundsException(
                "Expected 5 values (id north east south west), got " + parts.length);
        }

        int[] piece = new int[5];
        for (int i = 0; i < 5; i++) {
            piece[i] = Integer.parseInt(parts[i]);
        }

        return piece;
    }

    /**
     * Vérifie si un fichier de puzzle existe.
     *
     * @param puzzleName Nom du puzzle (sans extension)
     * @return true si le fichier existe, false sinon
     */
    public static boolean puzzleFileExists(String puzzleName) {
        String filename = DEFAULT_PUZZLE_DIR + puzzleName + DEFAULT_EXTENSION;
        return Files.exists(Paths.get(filename));
    }

    /**
     * Sauvegarde un puzzle dans un fichier.
     *
     * Utile pour externaliser les puzzles hardcodés existants.
     *
     * @param puzzleName Nom du puzzle (sans extension)
     * @param pieces Tableau de pièces au format {id, north, east, south, west}
     * @param comment Commentaire optionnel à ajouter en début de fichier
     * @throws IOException Si l'écriture échoue
     */
    public static void savePuzzleToFile(String puzzleName, int[][] pieces, String comment)
            throws IOException {
        String filename = DEFAULT_PUZZLE_DIR + puzzleName + DEFAULT_EXTENSION;
        Path dir = Paths.get(DEFAULT_PUZZLE_DIR);

        // Créer le répertoire si nécessaire
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Header
            writer.println("# Puzzle: " + puzzleName);
            writer.println("# Format: id north east south west");
            writer.println("# Convention: bord extérieur = 0");

            if (comment != null && !comment.isEmpty()) {
                writer.println("# " + comment);
            }

            writer.println();

            // Pièces
            for (int[] piece : pieces) {
                if (piece.length != 5) {
                    throw new IllegalArgumentException(
                        "Invalid piece format: expected 5 values, got " + piece.length);
                }
                writer.printf("%d %d %d %d %d%n",
                    piece[0], piece[1], piece[2], piece[3], piece[4]);
            }
        }
    }

    /**
     * Liste tous les fichiers de puzzle disponibles.
     *
     * @return Liste des noms de puzzle (sans extension)
     * @throws IOException Si le répertoire ne peut pas être lu
     */
    public static List<String> listAvailablePuzzles() throws IOException {
        Path dir = Paths.get(DEFAULT_PUZZLE_DIR);
        List<String> puzzles = new ArrayList<>();

        if (!Files.exists(dir)) {
            return puzzles; // Répertoire n'existe pas, retourner liste vide
        }

        try (var stream = Files.list(dir)) {
            stream.filter(path -> path.toString().endsWith(DEFAULT_EXTENSION))
                  .forEach(path -> {
                      String filename = path.getFileName().toString();
                      String puzzleName = filename.substring(0, filename.length() - DEFAULT_EXTENSION.length());
                      puzzles.add(puzzleName);
                  });
        }

        return puzzles;
    }
}
