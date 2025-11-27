import model.Piece;
import java.io.*;
import java.util.*;

/**
 * Configuration d'un puzzle avec métadonnées
 */
public class PuzzleConfig {

    /**
     * Classe interne pour représenter une pièce fixe
     */
    public static class FixedPiece {
        public final int pieceId;
        public final int row;
        public final int col;
        public final int rotation;

        public FixedPiece(int pieceId, int row, int col, int rotation) {
            this.pieceId = pieceId;
            this.row = row;
            this.col = col;
            this.rotation = rotation;
        }
    }

    private String name;
    private String type;
    private int rows;
    private int cols;
    private String difficulty;
    private Integer fixedPiece;  // Maintenu pour compatibilité
    private Integer fixedPieceRow;  // Maintenu pour compatibilité
    private Integer fixedPieceCol;  // Maintenu pour compatibilité
    private Integer fixedPieceRotation;  // Maintenu pour compatibilité
    private List<FixedPiece> fixedPieces = new ArrayList<>();  // Nouvelle approche
    private Map<Integer, Piece> pieces;
    private boolean verbose = false; // Affichage détaillé (par défaut: désactivé)
    private int minDepthToShowRecords = Integer.MAX_VALUE; // Seuil pour afficher records (par défaut: jamais)
    private String sortOrder = "ascending"; // Ordre de tri des pièces: "ascending" ou "descending"
    private boolean prioritizeBorders = false; // Prioriser le remplissage des bords (par défaut: désactivé)

    public PuzzleConfig(String name, String type, int rows, int cols, String difficulty) {
        this.name = name;
        this.type = type;
        this.rows = rows;
        this.cols = cols;
        this.difficulty = difficulty;
        this.pieces = new HashMap<>();
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public String getDifficulty() { return difficulty; }
    public Integer getFixedPiece() { return fixedPiece; }
    public void setFixedPiece(Integer piece) { this.fixedPiece = piece; }
    public Integer getFixedPieceRow() { return fixedPieceRow; }
    public void setFixedPieceRow(Integer row) { this.fixedPieceRow = row; }
    public Integer getFixedPieceCol() { return fixedPieceCol; }
    public void setFixedPieceCol(Integer col) { this.fixedPieceCol = col; }
    public Integer getFixedPieceRotation() { return fixedPieceRotation; }
    public void setFixedPieceRotation(Integer rotation) { this.fixedPieceRotation = rotation; }
    public Map<Integer, Piece> getPieces() { return pieces; }
    public boolean isVerbose() { return verbose; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }
    public int getMinDepthToShowRecords() { return minDepthToShowRecords; }
    public void setMinDepthToShowRecords(int minDepth) { this.minDepthToShowRecords = minDepth; }
    public List<FixedPiece> getFixedPieces() { return fixedPieces; }
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
    public boolean isPrioritizeBorders() { return prioritizeBorders; }
    public void setPrioritizeBorders(boolean prioritizeBorders) { this.prioritizeBorders = prioritizeBorders; }

    /**
     * Charge un puzzle depuis un fichier avec métadonnées
     */
    public static PuzzleConfig loadFromFile(String filename) throws IOException {
        PuzzleConfig config = null;
        Map<Integer, Piece> pieces = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            String name = null, type = null, difficulty = null;
            int rows = 0, cols = 0;
            Integer fixedPiece = null;
            Integer fixedPieceRow = null, fixedPieceCol = null, fixedPieceRotation = null;
            List<FixedPiece> fixedPiecesList = new ArrayList<>();
            boolean verbose = false;
            int minDepthToShowRecords = Integer.MAX_VALUE;
            String sortOrder = "ascending";
            boolean prioritizeBorders = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Ignorer les lignes vides
                if (line.isEmpty()) continue;

                // Parser les métadonnées
                if (line.startsWith("#")) {
                    String metadata = line.substring(1).trim();

                    if (metadata.startsWith("Type:")) {
                        type = metadata.substring(5).trim();
                    } else if (metadata.startsWith("Dimensions:")) {
                        String dims = metadata.substring(11).trim();
                        String[] parts = dims.split("x");
                        rows = Integer.parseInt(parts[0]);
                        cols = Integer.parseInt(parts[1]);
                    } else if (metadata.startsWith("Difficulté:")) {
                        difficulty = metadata.substring(11).trim();
                    } else if (metadata.startsWith("Pièce fixe:")) {
                        fixedPiece = Integer.parseInt(metadata.substring(11).trim());
                    } else if (metadata.startsWith("Verbose:")) {
                        String val = metadata.substring(8).trim().toLowerCase();
                        verbose = val.equals("true") || val.equals("oui") || val.equals("yes");
                    } else if (metadata.startsWith("SeuillAffichage:")) {
                        minDepthToShowRecords = Integer.parseInt(metadata.substring(16).trim());
                    } else if (metadata.startsWith("SortOrder:")) {
                        sortOrder = metadata.substring(10).trim().toLowerCase();
                    } else if (metadata.startsWith("PrioritizeBorders:")) {
                        String val = metadata.substring(18).trim().toLowerCase();
                        prioritizeBorders = val.equals("true") || val.equals("oui") || val.equals("yes");
                    } else if (metadata.startsWith("PieceFixePosition:")) {
                        String pos = metadata.substring(18).trim();
                        String[] coords = pos.split("[,\\s]+");
                        if (coords.length >= 3) {
                            // Pour compatibilité avec l'ancien système (pièce unique)
                            fixedPieceRow = Integer.parseInt(coords[0]);
                            fixedPieceCol = Integer.parseInt(coords[1]);
                            fixedPieceRotation = Integer.parseInt(coords[2]);
                        }
                        // Nouvelle approche: supporter plusieurs pièces  "pieceId row col rotation"
                        if (coords.length >= 4) {
                            int pieceId = Integer.parseInt(coords[0]);
                            int row = Integer.parseInt(coords[1]);
                            int col = Integer.parseInt(coords[2]);
                            int rotation = Integer.parseInt(coords[3]);
                            fixedPiecesList.add(new FixedPiece(pieceId, row, col, rotation));
                        }
                    } else if (!metadata.contains("=") && !metadata.startsWith("Format") &&
                               !metadata.startsWith("Total") && !metadata.startsWith("Mapping")) {
                        // C'est probablement le nom
                        if (name == null && metadata.length() > 0) {
                            name = metadata;
                        }
                    }
                    continue;
                }

                // Parser les pièces
                String[] parts = line.split("\\s+");
                if (parts.length >= 5) {
                    int id = Integer.parseInt(parts[0]);
                    int north = Integer.parseInt(parts[1]);
                    int east = Integer.parseInt(parts[2]);
                    int south = Integer.parseInt(parts[3]);
                    int west = Integer.parseInt(parts[4]);

                    int[] edges = {north, east, south, west};
                    pieces.put(id, new Piece(id, edges));
                }
            }

            // Créer la configuration
            if (name != null && type != null && rows > 0 && cols > 0) {
                config = new PuzzleConfig(name, type, rows, cols, difficulty != null ? difficulty : "unknown");
                config.pieces = pieces;
                config.fixedPiece = fixedPiece;
                config.fixedPieceRow = fixedPieceRow;
                config.fixedPieceCol = fixedPieceCol;
                config.fixedPieceRotation = fixedPieceRotation;
                config.fixedPieces = fixedPiecesList;
                config.verbose = verbose;
                config.minDepthToShowRecords = minDepthToShowRecords;
                config.sortOrder = sortOrder;
                config.prioritizeBorders = prioritizeBorders;
            }
        }

        return config;
    }

    /**
     * Affiche les informations du puzzle
     */
    public void printInfo() {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║ " + String.format("%-65s", name) + " ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Type: " + String.format("%-59s", type) + " ║");
        System.out.println("║ Dimensions: " + String.format("%-54s", rows + "×" + cols) + " ║");
        System.out.println("║ Pièces: " + String.format("%-57s", pieces.size()) + " ║");
        System.out.println("║ Difficulté: " + String.format("%-54s", difficulty) + " ║");
        if (fixedPiece != null) {
            System.out.println("║ Pièce fixe: " + String.format("%-54s", fixedPiece) + " ║");
        }
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
    }

    /**
     * Affiche un résumé des résultats
     */
    public void printSummary(long duration, boolean solved) {
        System.out.println("\n┌───────────────────────────────────────────────────────────────────┐");
        System.out.println("│ RÉSUMÉ: " + String.format("%-58s", name) + " │");
        System.out.println("├───────────────────────────────────────────────────────────────────┤");
        System.out.println("│ Résultat: " + String.format("%-56s", solved ? "✓ RÉSOLU" : "✗ NON RÉSOLU") + " │");
        System.out.println("│ Temps: " + String.format("%-59s", formatDuration(duration)) + " │");
        System.out.println("└───────────────────────────────────────────────────────────────────┘");
    }

    private String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + " ms";
        } else if (ms < 60000) {
            return String.format("%.2f s", ms / 1000.0);
        } else if (ms < 3600000) {
            return String.format("%.2f min", ms / 60000.0);
        } else {
            return String.format("%.2f h", ms / 3600000.0);
        }
    }
}
