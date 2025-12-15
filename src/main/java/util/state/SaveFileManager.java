package util.state;

import util.SolverLogger;

import java.io.File;

/**
 * Responsible for managing save file operations and cleanup.
 * Extracted from SaveStateManager for better code organization.
 */
public class SaveFileManager {

    public static final String SAVE_DIR = "saves/";
    private static final int MAX_BACKUP_SAVES = 50; // Maximum number of saves to keep

    /**
     * Determines the save subdirectory for a given puzzle.
     *
     * @param puzzleName puzzle name
     * @return subdirectory path (ex: "saves/eternity2/eternity2_p01_ascending/")
     */
    public static String getPuzzleSubDir(String puzzleName) {
        // Extract the base type for the root folder
        String baseType = puzzleName.split("_")[0]; // eternity2 or indice1

        // Return the path with the full puzzle name
        // Ex: "eternity2_p01_1_2_3_4_ascending_border" -> "saves/eternity2/eternity2_p01_1_2_3_4_ascending_border/"
        return SAVE_DIR + baseType + "/" + puzzleName + "/";
    }

    /**
     * Cleans up old saves by keeping only the MAX_BACKUP_SAVES most recent ones.
     *
     * @param baseName puzzle base name
     * @param currentDepth current depth
     */
    public static void cleanupOldSaves(String baseName, int currentDepth) {
        try {
            File saveDir = new File(SAVE_DIR);
            if (!saveDir.exists()) {
                return;
            }

            // Find all save files for this puzzle
            File[] saveFiles = saveDir.listFiles((dir, name) ->
                name.startsWith(baseName + "_save_") && name.endsWith(".txt")
            );

            if (saveFiles == null || saveFiles.length <= MAX_BACKUP_SAVES) {
                return; // No need to clean up
            }

            // Sort by level (extract the number from filename)
            java.util.Arrays.sort(saveFiles, (f1, f2) -> {
                int depth1 = extractDepthFromFilename(f1.getName(), baseName);
                int depth2 = extractDepthFromFilename(f2.getName(), baseName);
                return Integer.compare(depth2, depth1); // Descending order (most recent first)
            });

            // Delete old saves beyond MAX_BACKUP_SAVES
            for (int i = MAX_BACKUP_SAVES; i < saveFiles.length; i++) {
                saveFiles[i].delete();
            }
        } catch (RuntimeException e) {
            SolverLogger.warn("Error cleaning up saves: {}", e.getMessage());
        }
    }

    /**
     * Cleans up old best saves by keeping only the MAX_BACKUP_SAVES best ones.
     *
     * @param puzzleDir puzzle directory
     * @param currentDepth current depth
     */
    public static void cleanupOldBestSaves(String puzzleDir, int currentDepth) {
        try {
            File saveDir = new File(puzzleDir);
            if (!saveDir.exists()) {
                return;
            }

            // Find all "best" files for this puzzle
            File[] bestFiles = saveDir.listFiles((dir, name) ->
                name.startsWith("best_") && name.endsWith(".txt")
            );

            if (bestFiles == null || bestFiles.length <= MAX_BACKUP_SAVES) {
                return; // No need to clean up
            }

            // Sort by depth - from lowest to highest
            java.util.Arrays.sort(bestFiles, (f1, f2) -> {
                int depth1 = extractDepthFromBestFilename(f1.getName());
                int depth2 = extractDepthFromBestFilename(f2.getName());
                return Integer.compare(depth1, depth2);
            });

            // Delete the oldest to keep only MAX_BACKUP_SAVES
            int toDelete = bestFiles.length - MAX_BACKUP_SAVES;
            for (int i = 0; i < toDelete; i++) {
                bestFiles[i].delete();
            }
        } catch (RuntimeException e) {
            SolverLogger.warn("Error cleaning up best saves: {}", e.getMessage());
        }
    }

    /**
     * Cleans up old "current" files by keeping only the most recent one.
     *
     * @param puzzleDir puzzle directory
     * @param currentFileToKeep current file to keep
     */
    public static void cleanupOldCurrentSaves(String puzzleDir, String currentFileToKeep) {
        try {
            File saveDir = new File(puzzleDir);
            if (!saveDir.exists()) {
                return;
            }

            // Find all "current" files for this puzzle
            File[] currentFiles = saveDir.listFiles((dir, name) ->
                name.startsWith("current_") && name.endsWith(".txt")
            );

            if (currentFiles == null || currentFiles.length <= 1) {
                return; // Nothing to clean up
            }

            // Delete all current files except the most recent (currentFileToKeep)
            for (File f : currentFiles) {
                if (!f.getAbsolutePath().equals(new File(currentFileToKeep).getAbsolutePath())) {
                    f.delete();
                }
            }
        } catch (Exception e) {
            SolverLogger.warn("Error cleaning up current saves: {}", e.getMessage());
        }
    }

    /**
     * Extracts the level (depth) from a filename "best_X.txt".
     *
     * @param filename filename
     * @return extracted depth, or 0 on error
     */
    public static int extractDepthFromBestFilename(String filename) {
        try {
            String prefix = "best_";
            String suffix = ".txt";
            int start = filename.indexOf(prefix) + prefix.length();
            int end = filename.indexOf(suffix);
            return Integer.parseInt(filename.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Extracts the level (depth) from save filename.
     *
     * @param filename filename
     * @param baseName puzzle base name
     * @return extracted depth, or 0 on error
     */
    public static int extractDepthFromFilename(String filename, String baseName) {
        try {
            String prefix = baseName + "_save_";
            String suffix = ".txt";
            int start = filename.indexOf(prefix) + prefix.length();
            int end = filename.indexOf(suffix);
            return Integer.parseInt(filename.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Checks if a given depth represents a new record.
     *
     * @param puzzleDir puzzle directory
     * @param depth depth to check
     * @return true if it's a new record
     */
    public static boolean isNewRecord(String puzzleDir, int depth) {
        File saveDir = new File(puzzleDir);
        File[] bestFiles = saveDir.listFiles((dir, name) ->
            name.startsWith("best_") && name.endsWith(".txt")
        );

        if (bestFiles == null || bestFiles.length == 0) {
            return true; // First record
        }

        // Find the best existing depth
        int maxDepth = 0;
        for (File f : bestFiles) {
            try {
                String name = f.getName();
                String depthStr = name.replace("best_", "").replace(".txt", "");
                int d = Integer.parseInt(depthStr);
                if (d > maxDepth) {
                    maxDepth = d;
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        return depth > maxDepth;
    }
}
