package util;

import java.io.File;

/**
 * Responsible for managing save file operations and cleanup.
 * Extracted from SaveStateManager for better code organization.
 */
public class SaveFileManager {

    public static final String SAVE_DIR = "saves/";
    private static final int MAX_BACKUP_SAVES = 50; // Nombre max de sauvegardes à conserver

    /**
     * Détermine le sous-répertoire de sauvegarde pour un puzzle donné.
     *
     * @param puzzleName nom du puzzle
     * @return chemin du sous-répertoire (ex: "saves/eternity2/eternity2_p01_ascending/")
     */
    public static String getPuzzleSubDir(String puzzleName) {
        // Extraire le type de base pour le dossier racine
        String baseType = puzzleName.split("_")[0]; // eternity2 ou indice1

        // Retourner le chemin avec le nom complet du puzzle
        // Ex: "eternity2_p01_1_2_3_4_ascending_border" -> "saves/eternity2/eternity2_p01_1_2_3_4_ascending_border/"
        return SAVE_DIR + baseType + "/" + puzzleName + "/";
    }

    /**
     * Nettoie les anciennes sauvegardes en ne gardant que les MAX_BACKUP_SAVES plus récentes.
     *
     * @param baseName nom de base du puzzle
     * @param currentDepth profondeur actuelle
     */
    public static void cleanupOldSaves(String baseName, int currentDepth) {
        try {
            File saveDir = new File(SAVE_DIR);
            if (!saveDir.exists()) {
                return;
            }

            // Trouver tous les fichiers de sauvegarde pour ce puzzle
            File[] saveFiles = saveDir.listFiles((dir, name) ->
                name.startsWith(baseName + "_save_") && name.endsWith(".txt")
            );

            if (saveFiles == null || saveFiles.length <= MAX_BACKUP_SAVES) {
                return; // Pas besoin de nettoyer
            }

            // Trier par niveau (extraire le nombre du nom de fichier)
            java.util.Arrays.sort(saveFiles, (f1, f2) -> {
                int depth1 = extractDepthFromFilename(f1.getName(), baseName);
                int depth2 = extractDepthFromFilename(f2.getName(), baseName);
                return Integer.compare(depth2, depth1); // Ordre décroissant (plus récent en premier)
            });

            // Supprimer les anciennes sauvegardes au-delà de MAX_BACKUP_SAVES
            for (int i = MAX_BACKUP_SAVES; i < saveFiles.length; i++) {
                saveFiles[i].delete();
            }
        } catch (Exception e) {
            System.err.println("  ⚠️  Erreur lors du nettoyage des sauvegardes: " + e.getMessage());
        }
    }

    /**
     * Nettoie les anciennes sauvegardes best en ne gardant que les MAX_BACKUP_SAVES meilleures.
     *
     * @param puzzleDir répertoire du puzzle
     * @param currentDepth profondeur actuelle
     */
    public static void cleanupOldBestSaves(String puzzleDir, int currentDepth) {
        try {
            File saveDir = new File(puzzleDir);
            if (!saveDir.exists()) {
                return;
            }

            // Trouver tous les fichiers "best" pour ce puzzle
            File[] bestFiles = saveDir.listFiles((dir, name) ->
                name.startsWith("best_") && name.endsWith(".txt")
            );

            if (bestFiles == null || bestFiles.length <= MAX_BACKUP_SAVES) {
                return; // Pas besoin de nettoyer
            }

            // Trier par depth - du plus bas au plus élevé
            java.util.Arrays.sort(bestFiles, (f1, f2) -> {
                int depth1 = extractDepthFromBestFilename(f1.getName());
                int depth2 = extractDepthFromBestFilename(f2.getName());
                return Integer.compare(depth1, depth2);
            });

            // Supprimer les plus anciennes pour ne garder que MAX_BACKUP_SAVES
            int toDelete = bestFiles.length - MAX_BACKUP_SAVES;
            for (int i = 0; i < toDelete; i++) {
                bestFiles[i].delete();
            }
        } catch (Exception e) {
            System.err.println("  ⚠️  Erreur lors du nettoyage des best saves: " + e.getMessage());
        }
    }

    /**
     * Nettoie les anciens fichiers "current" en ne gardant que le plus récent.
     *
     * @param puzzleDir répertoire du puzzle
     * @param currentFileToKeep fichier actuel à conserver
     */
    public static void cleanupOldCurrentSaves(String puzzleDir, String currentFileToKeep) {
        try {
            File saveDir = new File(puzzleDir);
            if (!saveDir.exists()) {
                return;
            }

            // Trouver tous les fichiers "current" pour ce puzzle
            File[] currentFiles = saveDir.listFiles((dir, name) ->
                name.startsWith("current_") && name.endsWith(".txt")
            );

            if (currentFiles == null || currentFiles.length <= 1) {
                return; // Rien à nettoyer
            }

            // Supprimer tous les fichiers current sauf le plus récent (currentFileToKeep)
            for (File f : currentFiles) {
                if (!f.getAbsolutePath().equals(new File(currentFileToKeep).getAbsolutePath())) {
                    f.delete();
                }
            }
        } catch (Exception e) {
            System.err.println("  ⚠️  Erreur lors du nettoyage des current saves: " + e.getMessage());
        }
    }

    /**
     * Extrait le niveau (depth) d'un nom de fichier "best_X.txt".
     *
     * @param filename nom du fichier
     * @return depth extrait, ou 0 en cas d'erreur
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
     * Extrait le niveau (depth) du nom de fichier de sauvegarde.
     *
     * @param filename nom du fichier
     * @param baseName nom de base du puzzle
     * @return depth extrait, ou 0 en cas d'erreur
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
     * Vérifie si un depth donné représente un nouveau record.
     *
     * @param puzzleDir répertoire du puzzle
     * @param depth profondeur à vérifier
     * @return true si c'est un nouveau record
     */
    public static boolean isNewRecord(String puzzleDir, int depth) {
        File saveDir = new File(puzzleDir);
        File[] bestFiles = saveDir.listFiles((dir, name) ->
            name.startsWith("best_") && name.endsWith(".txt")
        );

        if (bestFiles == null || bestFiles.length == 0) {
            return true; // Premier record
        }

        // Trouver le meilleur depth existant
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
                // Ignorer
            }
        }

        return depth > maxDepth;
    }
}
