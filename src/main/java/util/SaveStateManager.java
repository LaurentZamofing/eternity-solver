package util;

import model.Board;
import model.Piece;
import model.Placement;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Manager for saving and loading puzzle state.
 * Saves the current board state and allows restoring it.
 *
 * TODO: REFACTORING NEEDED (Current: 508 lines, Target: ~130 lines per class)
 * See REFACTORING_ROADMAP.md for detailed plan.
 *
 * God Class Issues:
 * - Multiple responsibilities: I/O, serialization, discovery, backup rotation
 * - Complex save/load logic mixed with file management
 * - Many delegation wrapper methods that could be inlined
 *
 * Refactoring Plan:
 * 1. Extract util/state/SaveStateWriter.java (~150 lines) - Write operations
 * 2. Extract util/state/SaveStateReader.java (~120 lines) - Read operations
 * 3. Extract util/state/SaveStateLocator.java (~100 lines) - File discovery
 * 4. Extract util/state/BackupManager.java (~90 lines) - Backup rotation
 * 5. Inline or remove delegation wrapper methods
 * 6. Add tests for each component
 *
 * Progress:
 * - ✅ Removed 3 @Deprecated wrapper methods (31 lines)
 *
 * Estimated remaining effort: 3-5 hours
 * Priority: MEDIUM
 * See: REFACTORING_ROADMAP.md Section 3
 */
public class SaveStateManager {

    private static final String SAVE_DIR = "saves/";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private static final int MAX_BACKUP_SAVES = 10; // Keep the last 10 saves
    private static final int SAVE_LEVEL_INTERVAL = 1; // Save each level (each new piece placed)
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
     * Gets the subdirectory for a puzzle configuration
     * Ex: "eternity2_p01_ascending" -> "saves/eternity2/p01_asc/"
     * Ex: "indice1" -> "saves/indice1/"
     */
    private static String getPuzzleSubDir(String puzzleName) {
        // Delegate to SaveFileManager (refactored for better code organization)
        return SaveFileManager.getPuzzleSubDir(puzzleName);
    }

    /**
     * Class to store the complete puzzle state with placement order
     */
    public static class SaveState {
        public final String puzzleName;
        public final int rows;
        public final int cols;
        public final Map<String, PlacementInfo> placements; // "row,col" -> PlacementInfo
        public final java.util.List<PlacementInfo> placementOrder; // Piece placement order (for backtracking)
        public final Set<Integer> unusedPieceIds;
        public final long timestamp;
        public final int depth; // Number of pieces placed
        public final long totalComputeTimeMs; // Total compute time accumulated in milliseconds

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

        // Compatibility constructor (for old format without totalComputeTime)
        public SaveState(String puzzleName, int rows, int cols,
                        Map<String, PlacementInfo> placements,
                        java.util.List<PlacementInfo> placementOrder,
                        Set<Integer> unusedPieceIds,
                        long timestamp, int depth) {
            this(puzzleName, rows, cols, placements, placementOrder, unusedPieceIds, timestamp, depth, 0L);
        }
    }

    /**
     * Information about a placed piece (with position for placement order)
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

        // Constructor for compatibility (without position)
        public PlacementInfo(int pieceId, int rotation) {
            this(-1, -1, pieceId, rotation);
        }
    }

    /**
     * Saves the current puzzle state with backtracking
     * Two types of saves:
     * - current: ongoing save (can be overwritten)
     * - best_XXX: best scores by milestones (never overwritten)
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
                    System.out.println("  💾 Saving new best file: " + bestFile + " (" + state.depth + " pieces) at " +
                                     java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
                    SaveStateIO.writeToFile(bestFile, puzzleName, board, state.depth,
                                           placementOrder, unusedIds, progressPercentage,
                                           elapsedTimeMs, numFixedPieces, initialFixedPieces);

                    if (isNewRecord(puzzleDir, state.depth)) {
                        System.out.println("  🏆 New record confirmed: best_" + state.depth + ".txt written successfully");
                    }
                } else {
                    // File already exists - this depth was already reached before
                    System.out.println("  ℹ️  Depth " + state.depth + " already has best file (skipping duplicate save)");
                }
            }

        } catch (IOException e) {
            System.err.println("  ⚠️  Error during save: " + e.getMessage());
        }
    }

    /**
     * Retrieves the number of fixed pieces from the configuration file
     */
    private static int getNumFixedPiecesFromConfig(String puzzleName) {
        // Try first with exact name (new format without "puzzle_")
        File configFile = new File("data/" + puzzleName + ".txt");

        // If the file doesn't exist, search for a file matching the pattern
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

                // Count lines with format "# PieceFixePosition:"
                if (line.startsWith("# PieceFixePosition:")) {
                    count++;
                }
            }

            return count;
        } catch (Exception e) {
            // If we can't load the config, return 0
            System.err.println("Warning: unable to load config for " + puzzleName + ", fixed pieces = 0");
            return 0;
        }
    }

    /**
     * Checks if this is a new record
     */
    private static boolean isNewRecord(String puzzleDir, int depth) {
        // Delegate to SaveFileManager (refactored for better code organization)
        return SaveFileManager.isNewRecord(puzzleDir, depth);
    }

    /**
     * Simplified version without placement order (for compatibility)
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
     * Cleans up old saves by keeping only the MAX_BACKUP_SAVES most recent ones
     */
    private static void cleanupOldSaves(String baseName, int currentDepth) {
        // Delegate to SaveFileManager (refactored for better code organization)
        SaveFileManager.cleanupOldSaves(baseName, currentDepth);
    }

    /**
     * Cleans up old best saves by keeping only the MAX_BACKUP_SAVES best ones
     */
    private static void cleanupOldBestSaves(String puzzleDir, int currentDepth) {
        // Delegate to SaveFileManager (refactored for better code organization)
        SaveFileManager.cleanupOldBestSaves(puzzleDir, currentDepth);
    }

    /**
     * Cleans up old "current" files by keeping only the most recent one
     * (except the one just created)
     */
    private static void cleanupOldCurrentSaves(String puzzleDir, String currentFileToKeep) {
        // Delegate to SaveFileManager (refactored for better code organization)
        SaveFileManager.cleanupOldCurrentSaves(puzzleDir, currentFileToKeep);
    }

    /**
     * Extracts the depth from best filename
     */
    private static int extractDepthFromBestFilename(String filename) {
        // Delegate to SaveFileManager (refactored for better code organization)
        return SaveFileManager.extractDepthFromBestFilename(filename);
    }

    /**
     * Extracts the level (depth) from filename
     */
    private static int extractDepthFromFilename(String filename, String baseName) {
        // Delegate to SaveFileManager (refactored for better code organization)
        return SaveFileManager.extractDepthFromFilename(filename, baseName);
    }

    /**
     * Loads the saved state for a given puzzle
     * Returns null if no save exists
     * NEW: Also loads the piece placement order for backtracking
     */
    public static SaveState loadState(String puzzleName) {
        // Find the most recent save
        String baseName = puzzleName.replaceAll("[^a-zA-Z0-9]", "_");
        File saveDir = new File(SAVE_DIR);

        if (!saveDir.exists()) {
            return null;
        }

        // Find all save files for this puzzle
        File[] saveFiles = saveDir.listFiles((dir, name) ->
            name.startsWith(baseName + "_save_") && name.endsWith(".txt")
        );

        if (saveFiles == null || saveFiles.length == 0) {
            return null;
        }

        // Sort by level (most recent first)
        java.util.Arrays.sort(saveFiles, (f1, f2) -> {
            int depth1 = extractDepthFromFilename(f1.getName(), baseName);
            int depth2 = extractDepthFromFilename(f2.getName(), baseName);
            return Integer.compare(depth2, depth1);
        });

        // Load the most recent save
        File saveFile = saveFiles[0];

        // Delegate to SaveStateIO for actual file reading (refactored for better code organization)
        return SaveStateIO.readFromFile(saveFile, puzzleName);
    }

    /**
     * Restores the state on a board from a save
     */
    public static boolean restoreState(SaveState state, Board board, Map<Integer, Piece> allPieces) {
        // Delegate to SaveStateSerializer (refactored for better code organization)
        return SaveStateSerializer.restoreStateToBoard(state, board, allPieces);
    }

    /**
     * Finds all available saves for a puzzle, sorted by decreasing depth
     * @param puzzleName puzzle name
     * @return list of save files, from most recent to oldest
     */
    /**
     * Finds all "best" files for a given puzzle
     * Returns files sorted by decreasing depth (best first)
     */
    public static List<File> findAllSaves(String puzzleName) {
        String puzzleDir = getPuzzleSubDir(puzzleName);
        File saveDir = new File(puzzleDir);

        if (!saveDir.exists()) {
            return new ArrayList<>();
        }

        // Find all "best" files for this puzzle
        File[] saveFiles = saveDir.listFiles((dir, name) ->
            name.startsWith("best_") && name.endsWith(".txt")
        );

        if (saveFiles == null || saveFiles.length == 0) {
            return new ArrayList<>();
        }

        // Sort by level (most recent first)
        java.util.Arrays.sort(saveFiles, (f1, f2) -> {
            int depth1 = extractDepthFromBestFilename(f1.getName());
            int depth2 = extractDepthFromBestFilename(f2.getName());
            return Integer.compare(depth2, depth1); // Descending order
        });

        return java.util.Arrays.asList(saveFiles);
    }

    /**
     * Finds the OLDEST "current" save for a given puzzle
     * This enables parallelization: each thread resumes the oldest work
     * @param puzzleName puzzle name
     * @return the oldest current file if it exists, null otherwise
     */
    /**
     * Reads the total compute time from the existing current file
     * Returns 0 if no file or no TotalComputeTime field
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
                        return Long.parseLong(parts[1].trim().split(" ")[0]); // Extract just the number
                    }
                }
            }
        } catch (Exception e) {
            // Old format without TotalComputeTime
            return 0L;
        }
        return 0L;
    }

    public static File findCurrentSave(String puzzleName) {
        String puzzleDir = getPuzzleSubDir(puzzleName);

        // Search for all current files with timestamp in the subdirectory
        File saveDir = new File(puzzleDir);
        if (!saveDir.exists()) {
            return null;
        }

        File[] currentFiles = saveDir.listFiles((dir, name) ->
            name.startsWith("current_") && name.endsWith(".txt")
        );

        // If no file with timestamp, search for the old format
        if (currentFiles == null || currentFiles.length == 0) {
            File legacyFile = new File(puzzleDir + "current.txt");
            return legacyFile.exists() ? legacyFile : null;
        }

        // Find the file with the smallest timestamp (the oldest)
        File oldest = null;
        long oldestTimestamp = Long.MAX_VALUE;

        for (File f : currentFiles) {
            try {
                // Extract the timestamp from filename
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
                // Ignore malformed files
            }
        }

        return oldest;
    }

    /**
     * Load a specific save from a file
     * @param saveFile save file
     * @param puzzleName puzzle name
     * @return the loaded state or null if failure
     */
    public static SaveState loadStateFromFile(File saveFile, String puzzleName) {
        // Delegate to SaveStateIO (refactored for better code organization)
        return SaveStateIO.readFromFile(saveFile, puzzleName);
    }

    /**
     * Deletes the save for a given puzzle
     */
    public static void deleteSave(String puzzleName) {
        String filename = SAVE_DIR + puzzleName.replaceAll("[^a-zA-Z0-9]", "_") + "_save.txt";
        File saveFile = new File(filename);
        if (saveFile.exists()) {
            saveFile.delete();
            System.out.println("  🗑️  Save deleted: " + filename);
        }
    }
}
