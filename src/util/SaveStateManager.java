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
            // Créer le répertoire de sauvegarde s'il n'existe pas
            File saveDir = new File(SAVE_DIR);
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            // Collecter les placements
            Map<String, PlacementInfo> placements = new HashMap<>();
            int totalPieces = 0;
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (!board.isEmpty(r, c)) {
                        Placement p = board.getPlacement(r, c);
                        placements.put(r + "," + c, new PlacementInfo(r, c, p.getPieceId(), p.getRotation()));
                        totalPieces++;
                    }
                }
            }

            // Depth = pièces placées par backtracking (hors fixes)
            int depth = totalPieces - numFixedPieces;

            // Obtenir le sous-répertoire pour ce puzzle
            String puzzleDir = getPuzzleSubDir(puzzleName);

            // Créer le sous-répertoire s'il n'existe pas
            File dir = new File(puzzleDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Le elapsedTimeMs passé en paramètre est déjà le temps total cumulé
            // (il est calculé dans EternitySolver en lisant les sauvegardes au démarrage)
            // Donc on l'utilise directement sans ajouter quoi que ce soit
            long totalComputeTime = elapsedTimeMs;

            // TOUJOURS sauvegarder l'état actuel dans "current" avec timestamp
            long timestamp = System.currentTimeMillis();
            String currentFile = puzzleDir + "current_" + timestamp + ".txt";
            saveToFile(currentFile, puzzleName, board, depth, placementOrder, unusedIds, progressPercentage, totalComputeTime, numFixedPieces, initialFixedPieces);

            // Also save in binary format if enabled (for faster loading)
            if (useBinaryFormat) {
                String binaryFile = puzzleDir + "current_" + timestamp + ".bin";
                try {
                    // Create pieceUsed array from unusedIds
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

            // Nettoyer les anciens fichiers "current" (garder seulement le plus récent)
            cleanupOldCurrentSaves(puzzleDir, currentFile);

            // Sauvegarder dans "best" TOUJOURS si depth >= 10
            if (depth >= 10) {
                String bestFile = puzzleDir + "best_" + depth + ".txt";
                // Ne créer la sauvegarde "best" que si elle n'existe pas déjà
                File best = new File(bestFile);
                if (!best.exists()) {
                    saveToFile(bestFile, puzzleName, board, depth, placementOrder, unusedIds, progressPercentage, totalComputeTime, numFixedPieces, initialFixedPieces);

                    // Afficher message seulement si c'est vraiment un nouveau record
                    if (isNewRecord(puzzleDir, depth)) {
                        System.out.println("  🏆 Nouveau record: " + bestFile + " (" + depth + " pièces)");
                    }
                }
            }

            // Ne JAMAIS nettoyer les fichiers best_*.txt - on les garde tous pour validation

        } catch (IOException e) {
            System.err.println("  ⚠️  Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Génère un affichage visuel ASCII du plateau AVEC les arêtes détaillées
     */
    private static void generateBoardVisualDetailed(PrintWriter writer, Board board, Map<Integer, Piece> allPieces) {
        // Delegate to BoardTextRenderer (refactored for better code organization)
        BoardTextRenderer.generateBoardVisualDetailed(writer, board, allPieces);
    }

    /**
     * Génère un affichage visuel ASCII du plateau (simple, sans arêtes)
     */
    private static void generateBoardVisual(PrintWriter writer, Board board) {
        // Delegate to BoardTextRenderer (refactored for better code organization)
        BoardTextRenderer.generateBoardVisual(writer, board);
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
     */
    private static void saveToFile(String filename, String puzzleName, Board board, int depth,
                                   List<PlacementInfo> placementOrder, List<Integer> unusedIds, double progressPercentage, long totalComputeTimeMs,
                                   int numFixedPieces, List<PlacementInfo> initialFixedPieces) throws IOException {
        // Charger toutes les pièces pour l'affichage détaillé
        Map<Integer, Piece> allPieces = new HashMap<>();
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.isEmpty(r, c)) {
                    Placement p = board.getPlacement(r, c);
                    // Reconstruire la pièce à partir du placement
                    int[] edges = new int[4];
                    // On ne peut pas récupérer les arêtes originales depuis Placement
                    // On va donc seulement afficher les IDs pour l'instant
                }
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Header
            writer.println("# Sauvegarde Eternity II");
            writer.println("# Timestamp: " + System.currentTimeMillis());
            writer.println("# Date: " + DATE_FORMAT.format(new Date()));
            writer.println("# Puzzle: " + puzzleName);
            writer.println("# Dimensions: " + board.getRows() + "x" + board.getCols());
            writer.println("# Depth: " + depth + " (pièces placées par backtracking, hors fixes)");
            if (progressPercentage >= 0.0) {
                writer.println("# Progress: " + String.format("%.8f%%", progressPercentage) + " (estimation basée sur les 5 premières profondeurs)");
            }
            // Écrire le temps total de calcul en millisecondes et en format lisible
            long totalSeconds = totalComputeTimeMs / 1000;
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            writer.println("# TotalComputeTime: " + totalComputeTimeMs + " ms (" +
                          String.format("%dh %02dm %02ds", hours, minutes, seconds) + ")");
            writer.println();

            // AFFICHAGE VISUEL SIMPLE DU PLATEAU
            int numFixedPiecesLocal = (initialFixedPieces != null) ? initialFixedPieces.size() : 0;
            writer.println("# ═══════════════════════════════════════════════════════════");
            writer.println("# AFFICHAGE VISUEL DU PLATEAU (" + (depth + numFixedPiecesLocal) + " pièces: " + numFixedPiecesLocal + " fixes + " + depth + " backtracking)");
            writer.println("# ═══════════════════════════════════════════════════════════");
            writer.println("#");
            generateBoardVisual(writer, board);
            writer.println("#");
            writer.println("# ═══════════════════════════════════════════════════════════");
            writer.println();

            // Utiliser le nombre de pièces fixes passé en paramètre (depuis la configuration originale)
            // Si numFixedPieces est 0 (cas de compatibilité), le récupérer depuis la config
            if (numFixedPieces == 0) {
                numFixedPieces = getNumFixedPiecesFromConfig(puzzleName);
            }

            // Pièces fixes (coins + hints)
            writer.println("# ═══════════════════════════════════════════════════════════");
            writer.println("# PIÈCES FIXES (pré-placées au démarrage)");
            writer.println("# ═══════════════════════════════════════════════════════════");
            writer.println("#");
            if (numFixedPieces > 0) {
                writer.println("# " + numFixedPieces + " pièces fixes (coins + hints - voir fichier de configuration)");
            } else {
                writer.println("# (aucune pièce fixe)");
            }
            writer.println("#");
            writer.println("# ═══════════════════════════════════════════════════════════");
            writer.println();

            // Ordre de pose des pièces PAR LE BACKTRACKING (toutes les pièces de placementOrder)
            int fixedCountHeader = (initialFixedPieces != null) ? initialFixedPieces.size() : 0;
            int backtrackingPiecesCount = (placementOrder != null) ? Math.max(0, placementOrder.size() - fixedCountHeader) : 0;
            writer.println("# ═══════════════════════════════════════════════════════════");
            writer.println("# ORDRE DE POSE (backtracking) - " + backtrackingPiecesCount + " pièces");
            writer.println("# ═══════════════════════════════════════════════════════════");
            writer.println("#");
            if (placementOrder != null && placementOrder.size() > fixedCountHeader) {
                writer.println("# Étape  Position    Pièce  Rotation");
                writer.println("# ────── ─────────── ────── ────────");
                for (int i = fixedCountHeader; i < placementOrder.size(); i++) {
                    PlacementInfo info = placementOrder.get(i);
                    writer.println(String.format("# %4d   (%2d,%2d)     %3d      %d (×90°)",
                        (i - fixedCountHeader + 1), info.row, info.col, info.pieceId, info.rotation));
                }
            } else {
                writer.println("# (aucune pièce placée par backtracking)");
            }
            writer.println("#");
            writer.println("# ═══════════════════════════════════════════════════════════");
            writer.println();

            // Format de données pour le parser (ancien format conservé pour compatibilité)
            writer.println("# Fixed Pieces (row,col pieceId rotation) - pré-placées");
            // Utiliser initialFixedPieces (les vraies pièces fixes du config) au lieu des premières de placementOrder
            if (initialFixedPieces != null && !initialFixedPieces.isEmpty()) {
                for (PlacementInfo info : initialFixedPieces) {
                    writer.println(info.row + "," + info.col + " " + info.pieceId + " " + info.rotation);
                }
            }
            writer.println();

            // Ordre de pose des pièces PAR LE BACKTRACKING (hors fixes)
            writer.println("# Placement Order (row,col pieceId rotation) - ordre chronologique du backtracking");
            // Utiliser le nombre réel de pièces fixes initiales pour calculer l'offset
            int fixedCount = (initialFixedPieces != null) ? initialFixedPieces.size() : 0;
            if (placementOrder != null && placementOrder.size() > fixedCount) {
                for (int i = fixedCount; i < placementOrder.size(); i++) {
                    PlacementInfo info = placementOrder.get(i);
                    writer.println(info.row + "," + info.col + " " + info.pieceId + " " + info.rotation);
                }
            }
            writer.println();

            // Placements (positions actuelles)
            writer.println("# Placements (row,col pieceId rotation)");
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (!board.isEmpty(r, c)) {
                        Placement p = board.getPlacement(r, c);
                        writer.println(r + "," + c + " " + p.getPieceId() + " " + p.getRotation());
                    }
                }
            }
            writer.println();

            // Pièces non utilisées
            writer.println("# Unused pieces");
            for (int id : unusedIds) {
                writer.print(id + " ");
            }
            writer.println();
        }
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

        try (BufferedReader reader = new BufferedReader(new FileReader(saveFile))) {
            String line;
            int rows = 0, cols = 0;
            long timestamp = 0;
            int depth = 0;
            Map<String, PlacementInfo> placements = new HashMap<>();
            java.util.List<PlacementInfo> placementOrder = new ArrayList<>();
            Set<Integer> unusedPieceIds = new HashSet<>();

            boolean readingPlacementOrder = false;
            boolean readingPlacements = false;
            boolean readingUnused = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Ignorer les lignes vides
                if (line.isEmpty()) {
                    continue;
                }

                // Parser les métadonnées
                if (line.startsWith("# Timestamp:")) {
                    timestamp = Long.parseLong(line.substring(12).trim());
                } else if (line.startsWith("# Dimensions:")) {
                    String dims = line.substring(13).trim();
                    String[] parts = dims.split("x");
                    rows = Integer.parseInt(parts[0]);
                    cols = Integer.parseInt(parts[1]);
                } else if (line.startsWith("# Depth:")) {
                    // Extract depth number (may have description after, e.g. "54 (pièces...)")
                    String depthStr = line.substring(8).trim();
                    int spaceIdx = depthStr.indexOf(' ');
                    if (spaceIdx > 0) {
                        depthStr = depthStr.substring(0, spaceIdx);
                    }
                    depth = Integer.parseInt(depthStr);
                } else if (line.startsWith("# Placement Order")) {
                    readingPlacementOrder = true;
                    readingPlacements = false;
                    readingUnused = false;
                } else if (line.startsWith("# Placements")) {
                    readingPlacementOrder = false;
                    readingPlacements = true;
                    readingUnused = false;
                } else if (line.startsWith("# Unused")) {
                    readingPlacementOrder = false;
                    readingPlacements = false;
                    readingUnused = true;
                } else if (!line.startsWith("#")) {
                    // Données
                    if (readingPlacementOrder) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            String[] coords = parts[0].split(",");
                            if (coords.length == 2) {
                                int r = Integer.parseInt(coords[0]);
                                int c = Integer.parseInt(coords[1]);
                                int pieceId = Integer.parseInt(parts[1]);
                                int rotation = Integer.parseInt(parts[2]);
                                placementOrder.add(new PlacementInfo(r, c, pieceId, rotation));
                            }
                        }
                    } else if (readingPlacements) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            // Format: "row,col pieceId rotation"
                            String[] coords = parts[0].split(",");
                            if (coords.length == 2) {
                                int r = Integer.parseInt(coords[0]);
                                int c = Integer.parseInt(coords[1]);
                                int pieceId = Integer.parseInt(parts[1]);
                                int rotation = Integer.parseInt(parts[2]);
                                placements.put(r + "," + c, new PlacementInfo(r, c, pieceId, rotation));
                            }
                        }
                    } else if (readingUnused) {
                        String[] parts = line.split("\\s+");
                        for (String part : parts) {
                            if (!part.isEmpty()) {
                                unusedPieceIds.add(Integer.parseInt(part));
                            }
                        }
                    }
                }
            }

            System.out.println("  📂 Sauvegarde chargée: " + saveFile.getName() + " (" + depth + " pièces)");
            System.out.println("  📅 Date: " + DATE_FORMAT.format(new Date(timestamp)));
            System.out.println("  📋 Ordre de pose: " + placementOrder.size() + " placements trackés");

            return new SaveState(puzzleName, rows, cols, placements, placementOrder, unusedPieceIds, timestamp, depth);

        } catch (IOException e) {
            System.err.println("  ⚠️  Erreur lors du chargement: " + e.getMessage());
            return null;
        }
    }

    /**
     * Restaure l'état sur un board à partir d'une sauvegarde
     */
    public static boolean restoreState(SaveState state, Board board, Map<Integer, Piece> allPieces) {
        try {
            // Vérifier les dimensions
            if (board.getRows() != state.rows || board.getCols() != state.cols) {
                System.err.println("  ⚠️  Dimensions incompatibles!");
                return false;
            }

            // Placer les pièces
            for (Map.Entry<String, PlacementInfo> entry : state.placements.entrySet()) {
                String[] coords = entry.getKey().split(",");
                int r = Integer.parseInt(coords[0]);
                int c = Integer.parseInt(coords[1]);
                PlacementInfo info = entry.getValue();

                Piece piece = allPieces.get(info.pieceId);
                if (piece != null) {
                    board.place(r, c, piece, info.rotation);
                } else {
                    System.err.println("  ⚠️  Pièce " + info.pieceId + " introuvable!");
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            System.err.println("  ⚠️  Erreur lors de la restauration: " + e.getMessage());
            return false;
        }
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
        try (BufferedReader reader = new BufferedReader(new FileReader(saveFile))) {
            String line;
            int rows = 0, cols = 0;
            long timestamp = 0;
            int depth = 0;
            Map<String, PlacementInfo> placements = new HashMap<>();
            java.util.List<PlacementInfo> placementOrder = new ArrayList<>();
            Set<Integer> unusedPieceIds = new HashSet<>();

            boolean readingPlacementOrder = false;
            boolean readingPlacements = false;
            boolean readingUnused = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("# Timestamp:")) {
                    timestamp = Long.parseLong(line.substring(12).trim());
                } else if (line.startsWith("# Dimensions:")) {
                    String dims = line.substring(13).trim();
                    String[] parts = dims.split("x");
                    rows = Integer.parseInt(parts[0]);
                    cols = Integer.parseInt(parts[1]);
                } else if (line.startsWith("# Depth:")) {
                    // Extract depth number (may have description after, e.g. "54 (pièces...)")
                    String depthStr = line.substring(8).trim();
                    int spaceIdx = depthStr.indexOf(' ');
                    if (spaceIdx > 0) {
                        depthStr = depthStr.substring(0, spaceIdx);
                    }
                    depth = Integer.parseInt(depthStr);
                } else if (line.startsWith("# Placement Order")) {
                    readingPlacementOrder = true;
                    readingPlacements = false;
                    readingUnused = false;
                } else if (line.startsWith("# Placements")) {
                    readingPlacementOrder = false;
                    readingPlacements = true;
                    readingUnused = false;
                } else if (line.startsWith("# Unused")) {
                    readingPlacementOrder = false;
                    readingPlacements = false;
                    readingUnused = true;
                } else if (!line.startsWith("#")) {
                    if (readingPlacementOrder) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            String[] coords = parts[0].split(",");
                            if (coords.length == 2) {
                                int r = Integer.parseInt(coords[0]);
                                int c = Integer.parseInt(coords[1]);
                                int pieceId = Integer.parseInt(parts[1]);
                                int rotation = Integer.parseInt(parts[2]);
                                placementOrder.add(new PlacementInfo(r, c, pieceId, rotation));
                            }
                        }
                    } else if (readingPlacements) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            String[] coords = parts[0].split(",");
                            if (coords.length == 2) {
                                int r = Integer.parseInt(coords[0]);
                                int c = Integer.parseInt(coords[1]);
                                int pieceId = Integer.parseInt(parts[1]);
                                int rotation = Integer.parseInt(parts[2]);
                                placements.put(r + "," + c, new PlacementInfo(r, c, pieceId, rotation));
                            }
                        }
                    } else if (readingUnused) {
                        String[] parts = line.split("\\s+");
                        for (String part : parts) {
                            if (!part.isEmpty()) {
                                unusedPieceIds.add(Integer.parseInt(part));
                            }
                        }
                    }
                }
            }

            // CORRECTION DU BUG: Reconstruire placementOrder complet depuis placements si incomplet
            // Le placementOrder parsé peut être incomplet (ne contient que certaines pièces trackées).
            // On reconstruit un ordre complet en incluant TOUTES les pièces du plateau.
            // Note: L'ordre ne sera pas le chronologique exact (on utilise row,col croissant),
            // mais c'est mieux qu'un ordre incomplet qui empêche le backtracking.

            List<PlacementInfo> finalOrder = placementOrder;

            // Vérifier si l'ordre est incomplet
            if (placements.size() > placementOrder.size()) {
                System.out.println("  ⚠️  PlacementOrder incomplet: " + placementOrder.size() +
                                 " entrées vs " + placements.size() + " pièces sur plateau");
                System.out.println("  ✓  Reconstruction ordre complet (approximatif row,col)...");

                // Créer un Set des pièces déjà dans placementOrder
                Set<String> existingKeys = new HashSet<>();
                for (PlacementInfo p : placementOrder) {
                    existingKeys.add(p.row + "," + p.col);
                }

                // Trier tous les placements par (row, col)
                List<PlacementInfo> sortedAll = new ArrayList<>(placements.values());
                sortedAll.sort((p1, p2) -> {
                    if (p1.row != p2.row) return Integer.compare(p1.row, p2.row);
                    return Integer.compare(p1.col, p2.col);
                });

                // Reconstruire: garder l'ordre existant autant que possible, ajouter manquants
                List<PlacementInfo> reconstructed = new ArrayList<>(placementOrder);
                for (PlacementInfo p : sortedAll) {
                    String key = p.row + "," + p.col;
                    if (!existingKeys.contains(key)) {
                        reconstructed.add(p);
                    }
                }

                finalOrder = reconstructed;
                System.out.println("  ✓  Ordre reconstruit: " + finalOrder.size() + " pièces");
            }

            return new SaveState(puzzleName, rows, cols, placements, finalOrder, unusedPieceIds, timestamp, depth);

        } catch (IOException e) {
            System.err.println("  ⚠️  Erreur lors du chargement de " + saveFile.getName() + ": " + e.getMessage());
            return null;
        }
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
