package util;

import model.Board;
import model.Piece;
import model.Placement;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Gestionnaire de sauvegarde et chargement de l'état du puzzle
 * Sauvegarde l'état actuel du board et permet de le restaurer
 */
public class SaveStateManager {

    private static final String SAVE_DIR = "saves/";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private static final int MAX_BACKUP_SAVES = 10; // Garder les 10 dernières sauvegardes
    private static final int SAVE_LEVEL_INTERVAL = 1; // Sauvegarder chaque niveau (chaque nouvelle pièce placée)
    private static boolean useBinaryFormat = false; // Enable/disable binary save format

    /**
     * Enable binary format for faster save/load operations
     */
    public static void enableBinaryFormat() {
        useBinaryFormat = true;
    }

    /**
     * Disable binary format and use text format
     */
    public static void disableBinaryFormat() {
        useBinaryFormat = false;
    }

    /**
     * Check if binary format is enabled
     */
    public static boolean isBinaryFormatEnabled() {
        return useBinaryFormat;
    }

    /**
     * Obtient le sous-répertoire pour une configuration de puzzle
     * Ex: "eternity2_p01_ascending" -> "saves/eternity2/p01_asc/"
     * Ex: "indice1" -> "saves/indice1/"
     */
    private static String getPuzzleSubDir(String puzzleName) {
        // Delegate to SaveFileManager (refactored for better code organization)
        return SaveFileManager.getPuzzleSubDir(puzzleName);
    }

    /**
     * Classe pour stocker l'état complet du puzzle avec l'ordre de pose
     */
    public static class SaveState {
        public final String puzzleName;
        public final int rows;
        public final int cols;
        public final Map<String, PlacementInfo> placements; // "row,col" -> PlacementInfo
        public final java.util.List<PlacementInfo> placementOrder; // Ordre de pose des pièces (pour backtracking)
        public final Set<Integer> unusedPieceIds;
        public final long timestamp;
        public final int depth; // Nombre de pièces placées
        public final long totalComputeTimeMs; // Temps total de calcul cumulé en millisecondes

        public SaveState(String puzzleName, int rows, int cols,
                        Map<String, PlacementInfo> placements,
                        java.util.List<PlacementInfo> placementOrder,
                        Set<Integer> unusedPieceIds,
                        long timestamp, int depth, long totalComputeTimeMs) {
            this.puzzleName = puzzleName;
            this.rows = rows;
            this.cols = cols;
            this.placements = placements;
            this.placementOrder = placementOrder;
            this.unusedPieceIds = unusedPieceIds;
            this.timestamp = timestamp;
            this.depth = depth;
            this.totalComputeTimeMs = totalComputeTimeMs;
        }

        // Constructeur de compatibilité (pour ancien format sans totalComputeTime)
        public SaveState(String puzzleName, int rows, int cols,
                        Map<String, PlacementInfo> placements,
                        java.util.List<PlacementInfo> placementOrder,
                        Set<Integer> unusedPieceIds,
                        long timestamp, int depth) {
            this(puzzleName, rows, cols, placements, placementOrder, unusedPieceIds, timestamp, depth, 0L);
        }
    }

    /**
     * Information sur une pièce placée (avec position pour l'ordre de pose)
     */
    public static class PlacementInfo {
        public final int row;
        public final int col;
        public final int pieceId;
        public final int rotation;

        public PlacementInfo(int row, int col, int pieceId, int rotation) {
            this.row = row;
            this.col = col;
            this.pieceId = pieceId;
            this.rotation = rotation;
        }

        // Constructeur pour compatibilité (sans position)
        public PlacementInfo(int pieceId, int rotation) {
            this(-1, -1, pieceId, rotation);
        }
    }

    /**
     * Sauvegarde l'état actuel du puzzle avec backtracking
     * Deux types de sauvegardes:
     * - current: sauvegarde en cours (écrasable)
     * - best_XXX: meilleurs scores par paliers (jamais écrasés)
     */
    public static void saveState(String puzzleName, Board board, Map<Integer, Piece> allPieces,
                                 List<Integer> unusedIds, List<PlacementInfo> placementOrder, double progressPercentage, long elapsedTimeMs,
                                 int numFixedPieces, List<PlacementInfo> initialFixedPieces) {
        try {
            // Create save directories
            File saveDir = new File(SAVE_DIR);
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            // Create SaveState using serializer
            SaveState state = SaveStateSerializer.createSaveState(puzzleName, board, unusedIds,
                                                                 placementOrder, numFixedPieces, elapsedTimeMs);

            // Get puzzle subdirectory
            String puzzleDir = getPuzzleSubDir(puzzleName);
            File dir = new File(puzzleDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Save current state with timestamp
            long timestamp = System.currentTimeMillis();
            String currentFile = puzzleDir + "current_" + timestamp + ".txt";
            SaveStateIO.writeToFile(currentFile, puzzleName, board, state.depth,
                                   placementOrder, unusedIds, progressPercentage,
                                   elapsedTimeMs, numFixedPieces, initialFixedPieces);

            // Save in binary format if enabled
            if (useBinaryFormat) {
                String binaryFile = puzzleDir + "current_" + timestamp + ".bin";
                try {
                    int maxPieceId = allPieces.keySet().stream().max(Integer::compareTo).orElse(0);
                    boolean[] pieceUsed = new boolean[maxPieceId + 1];
                    for (int i = 1; i <= maxPieceId; i++) {
                        pieceUsed[i] = !unusedIds.contains(i);
                    }
                    BinarySaveManager.saveBinary(binaryFile, board, allPieces, pieceUsed, maxPieceId);
                } catch (IOException e) {
                    System.err.println("Warning: Failed to save binary format: " + e.getMessage());
                }
            }

            // Cleanup old current saves
            cleanupOldCurrentSaves(puzzleDir, currentFile);

            // Save best if depth >= 10
            if (state.depth >= 10) {
                String bestFile = puzzleDir + "best_" + state.depth + ".txt";
                File best = new File(bestFile);
                if (!best.exists()) {
                    SaveStateIO.writeToFile(bestFile, puzzleName, board, state.depth,
                                           placementOrder, unusedIds, progressPercentage,
                                           elapsedTimeMs, numFixedPieces, initialFixedPieces);

                    if (isNewRecord(puzzleDir, state.depth)) {
                        System.out.println("  🏆 Nouveau record: " + bestFile + " (" + state.depth + " pièces)");
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("  ⚠️  Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Génère un affichage visuel ASCII du plateau AVEC les arêtes détaillées
     * @deprecated Use SaveBoardRenderer.generateBoardVisualDetailed() instead
     */
    @Deprecated
    private static void generateBoardVisualDetailed(PrintWriter writer, Board board, Map<Integer, Piece> allPieces) {
        SaveBoardRenderer.generateBoardVisualDetailed(writer, board, allPieces);
    }

    /**
     * Génère un affichage visuel ASCII du plateau (simple, sans arêtes)
     * @deprecated Use SaveBoardRenderer.generateBoardVisual() instead
     */
    @Deprecated
    private static void generateBoardVisual(PrintWriter writer, Board board) {
        SaveBoardRenderer.generateBoardVisual(writer, board);
    }

    /**
     * Récupère le nombre de pièces fixes depuis le fichier de configuration
     */
    private static int getNumFixedPiecesFromConfig(String puzzleName) {
        // Essayer d'abord avec le nom exact (nouveau format sans "puzzle_")
        File configFile = new File("data/" + puzzleName + ".txt");

        // Si le fichier n'existe pas, chercher un fichier qui correspond au pattern
        if (!configFile.exists()) {
            File dataDir = new File("data");
            if (dataDir.exists() && dataDir.isDirectory()) {
                File[] files = dataDir.listFiles((dir, name) ->
                    name.startsWith(puzzleName.replaceAll("_ascending|_descending|_border", "")) &&
                    name.endsWith(".txt") &&
                    (puzzleName.contains("ascending") ? name.contains("ascending") : name.contains("descending")) &&
                    (puzzleName.contains("border") ? name.contains("border") : !name.contains("border"))
                );

                if (files != null && files.length > 0) {
                    configFile = files[0];
                }
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Compter les lignes avec le format "# PieceFixePosition:"
                if (line.startsWith("# PieceFixePosition:")) {
                    count++;
                }
            }

            return count;
        } catch (Exception e) {
            // Si on ne peut pas charger la config, on retourne 0
            System.err.println("Attention: impossible de charger la config pour " + puzzleName + ", pièces fixes = 0");
            return 0;
        }
    }

    /**
     * Sauvegarde dans un fichier spécifique
     * @deprecated Use SaveStateIO.writeToFile() instead
     */
    @Deprecated
    private static void saveToFile(String filename, String puzzleName, Board board, int depth,
                                   List<PlacementInfo> placementOrder, List<Integer> unusedIds, double progressPercentage, long totalComputeTimeMs,
                                   int numFixedPieces, List<PlacementInfo> initialFixedPieces) throws IOException {
        // Delegated to SaveStateIO (refactored for better code organization)
        SaveStateIO.writeToFile(filename, puzzleName, board, depth, placementOrder, unusedIds,
                               progressPercentage, totalComputeTimeMs, numFixedPieces, initialFixedPieces);
    }

    /**
     * Vérifie si c'est un nouveau record
     */
    private static boolean isNewRecord(String puzzleDir, int depth) {
        // Delegate to SaveFileManager (refactored for better code organization)
        return SaveFileManager.isNewRecord(puzzleDir, depth);
    }

    /**
     * Version simplifiée sans placement order (pour compatibilité)
     */
    public static void saveState(String puzzleName, Board board, Map<Integer, Piece> allPieces,
                                 List<Integer> unusedIds, List<PlacementInfo> placementOrder) {
        saveState(puzzleName, board, allPieces, unusedIds, placementOrder, -1.0, 0L, 0, null);
    }

    public static void saveState(String puzzleName, Board board, Map<Integer, Piece> allPieces,
                                 List<Integer> unusedIds) {
        saveState(puzzleName, board, allPieces, unusedIds, null, -1.0, 0L, 0, null);
    }

    // Compatibility overload: without elapsedTimeMs
    public static void saveState(String puzzleName, Board board, Map<Integer, Piece> allPieces,
                                 List<Integer> unusedIds, List<PlacementInfo> placementOrder,
                                 double progressPercentage) {
        saveState(puzzleName, board, allPieces, unusedIds, placementOrder, progressPercentage, 0L, 0, null);
    }

    /**
     * Nettoie les anciennes sauvegardes en ne gardant que les MAX_BACKUP_SAVES plus récentes
     */
    private static void cleanupOldSaves(String baseName, int currentDepth) {
        // Delegate to SaveFileManager (refactored for better code organization)
        SaveFileManager.cleanupOldSaves(baseName, currentDepth);
    }

    /**
     * Nettoie les anciennes sauvegardes best en ne gardant que les MAX_BACKUP_SAVES meilleures
     */
    private static void cleanupOldBestSaves(String puzzleDir, int currentDepth) {
        // Delegate to SaveFileManager (refactored for better code organization)
        SaveFileManager.cleanupOldBestSaves(puzzleDir, currentDepth);
    }

    /**
     * Nettoie les anciens fichiers "current" en ne gardant que le plus récent
     * (sauf celui qui vient d'être créé)
     */
    private static void cleanupOldCurrentSaves(String puzzleDir, String currentFileToKeep) {
        // Delegate to SaveFileManager (refactored for better code organization)
        SaveFileManager.cleanupOldCurrentSaves(puzzleDir, currentFileToKeep);
    }

    /**
     * Extrait le depth du nom de fichier best
     */
    private static int extractDepthFromBestFilename(String filename) {
        // Delegate to SaveFileManager (refactored for better code organization)
        return SaveFileManager.extractDepthFromBestFilename(filename);
    }

    /**
     * Extrait le niveau (depth) du nom de fichier
     */
    private static int extractDepthFromFilename(String filename, String baseName) {
        // Delegate to SaveFileManager (refactored for better code organization)
        return SaveFileManager.extractDepthFromFilename(filename, baseName);
    }

    /**
     * Charge l'état sauvegardé pour un puzzle donné
     * Retourne null si aucune sauvegarde n'existe
     * NOUVEAU: Charge aussi l'ordre de pose des pièces pour le backtracking
     */
    public static SaveState loadState(String puzzleName) {
        // Chercher la sauvegarde la plus récente
        String baseName = puzzleName.replaceAll("[^a-zA-Z0-9]", "_");
        File saveDir = new File(SAVE_DIR);

        if (!saveDir.exists()) {
            return null;
        }

        // Trouver tous les fichiers de sauvegarde pour ce puzzle
        File[] saveFiles = saveDir.listFiles((dir, name) ->
            name.startsWith(baseName + "_save_") && name.endsWith(".txt")
        );

        if (saveFiles == null || saveFiles.length == 0) {
            return null;
        }

        // Trier par niveau (plus récent en premier)
        java.util.Arrays.sort(saveFiles, (f1, f2) -> {
            int depth1 = extractDepthFromFilename(f1.getName(), baseName);
            int depth2 = extractDepthFromFilename(f2.getName(), baseName);
            return Integer.compare(depth2, depth1);
        });

        // Charger la sauvegarde la plus récente
        File saveFile = saveFiles[0];

        // Delegate to SaveStateIO for actual file reading (refactored for better code organization)
        return SaveStateIO.readFromFile(saveFile, puzzleName);
    }

    /**
     * Restaure l'état sur un board à partir d'une sauvegarde
     */
    public static boolean restoreState(SaveState state, Board board, Map<Integer, Piece> allPieces) {
        // Delegate to SaveStateSerializer (refactored for better code organization)
        return SaveStateSerializer.restoreStateToBoard(state, board, allPieces);
    }

    /**
     * Trouve toutes les sauvegardes disponibles pour un puzzle, triées par profondeur décroissante
     * @param puzzleName nom du puzzle
     * @return liste des fichiers de sauvegarde, du plus récent au plus ancien
     */
    /**
     * Trouve tous les fichiers "best" pour un puzzle donné
     * Retourne les fichiers triés par profondeur décroissante (meilleur en premier)
     */
    public static List<File> findAllSaves(String puzzleName) {
        String puzzleDir = getPuzzleSubDir(puzzleName);
        File saveDir = new File(puzzleDir);

        if (!saveDir.exists()) {
            return new ArrayList<>();
        }

        // Trouver tous les fichiers "best" pour ce puzzle
        File[] saveFiles = saveDir.listFiles((dir, name) ->
            name.startsWith("best_") && name.endsWith(".txt")
        );

        if (saveFiles == null || saveFiles.length == 0) {
            return new ArrayList<>();
        }

        // Trier par niveau (plus récent en premier)
        java.util.Arrays.sort(saveFiles, (f1, f2) -> {
            int depth1 = extractDepthFromBestFilename(f1.getName());
            int depth2 = extractDepthFromBestFilename(f2.getName());
            return Integer.compare(depth2, depth1); // Ordre décroissant
        });

        return java.util.Arrays.asList(saveFiles);
    }

    /**
     * Trouve la sauvegarde "current" la plus ANCIENNE pour un puzzle donné
     * Ceci permet la parallélisation: chaque thread reprend le travail le plus ancien
     * @param puzzleName nom du puzzle
     * @return le fichier current le plus ancien s'il existe, null sinon
     */
    /**
     * Lit le temps total de calcul depuis le fichier current existant
     * Retourne 0 si pas de fichier ou pas de champ TotalComputeTime
     */
    public static long readTotalComputeTime(String puzzleName) {
        File currentSave = findCurrentSave(puzzleName);
        if (currentSave == null || !currentSave.exists()) {
            return 0L;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(currentSave))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("# TotalComputeTime:")) {
                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        return Long.parseLong(parts[1].trim().split(" ")[0]); // Extraire juste le nombre
                    }
                }
            }
        } catch (Exception e) {
            // Ancien format sans TotalComputeTime
            return 0L;
        }
        return 0L;
    }

    public static File findCurrentSave(String puzzleName) {
        String puzzleDir = getPuzzleSubDir(puzzleName);

        // Chercher tous les fichiers current avec timestamp dans le sous-répertoire
        File saveDir = new File(puzzleDir);
        if (!saveDir.exists()) {
            return null;
        }

        File[] currentFiles = saveDir.listFiles((dir, name) ->
            name.startsWith("current_") && name.endsWith(".txt")
        );

        // Si aucun fichier avec timestamp, chercher l'ancien format
        if (currentFiles == null || currentFiles.length == 0) {
            File legacyFile = new File(puzzleDir + "current.txt");
            return legacyFile.exists() ? legacyFile : null;
        }

        // Trouver le fichier avec le plus petit timestamp (le plus ancien)
        File oldest = null;
        long oldestTimestamp = Long.MAX_VALUE;

        for (File f : currentFiles) {
            try {
                // Extraire le timestamp du nom de fichier
                String name = f.getName();
                String prefix = "current_";
                String suffix = ".txt";
                int start = name.indexOf(prefix) + prefix.length();
                int end = name.indexOf(suffix);
                long timestamp = Long.parseLong(name.substring(start, end));

                if (timestamp < oldestTimestamp) {
                    oldestTimestamp = timestamp;
                    oldest = f;
                }
            } catch (Exception e) {
                // Ignorer les fichiers mal formatés
            }
        }

        return oldest;
    }

    /**
     * Charge une sauvegarde spécifique depuis un fichier
     * @param saveFile fichier de sauvegarde
     * @param puzzleName nom du puzzle
     * @return l'état chargé ou null si échec
     */
    public static SaveState loadStateFromFile(File saveFile, String puzzleName) {
        // Delegate to SaveStateIO (refactored for better code organization)
        return SaveStateIO.readFromFile(saveFile, puzzleName);
    }

    /**
     * Supprime la sauvegarde pour un puzzle donné
     */
    public static void deleteSave(String puzzleName) {
        String filename = SAVE_DIR + puzzleName.replaceAll("[^a-zA-Z0-9]", "_") + "_save.txt";
        File saveFile = new File(filename);
        if (saveFile.exists()) {
            saveFile.delete();
            System.out.println("  🗑️  Sauvegarde supprimée: " + filename);
        }
    }
}
