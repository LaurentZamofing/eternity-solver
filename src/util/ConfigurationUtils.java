package util;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Utility methods for configuration management across Main classes.
 * Extracted from Main*, MainSequential, and MainParallel to eliminate duplication.
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public class ConfigurationUtils {

    /**
     * Extracts a clean configuration ID from a file path.
     * Removes directory paths and .txt extension.
     *
     * @param filepath Full or relative file path (e.g., "data/puzzles/example_4x4.txt")
     * @return Configuration ID (e.g., "example_4x4")
     */
    public static String extractConfigId(String filepath) {
        String filename = new File(filepath).getName();
        if (filename.endsWith(".txt")) {
            return filename.substring(0, filename.length() - 4);
        }
        return filename;
    }

    /**
     * Sorts a list of piece IDs according to the specified order.
     * Modifies the list in place.
     *
     * @param pieces List of piece IDs to sort
     * @param sortOrder Sort order: "ascending", "descending", or null/other for no sort
     */
    public static void sortPiecesByOrder(List<Integer> pieces, String sortOrder) {
        if ("descending".equalsIgnoreCase(sortOrder)) {
            Collections.sort(pieces, Collections.reverseOrder());
        } else if ("ascending".equalsIgnoreCase(sortOrder)) {
            Collections.sort(pieces);
        }
        // null or other values: no sorting
    }

    /**
     * Creates a compact thread label for display in logs.
     * Shortens common patterns for readability.
     *
     * @param threadId Thread number
     * @param configId Configuration identifier
     * @return Compact label like "[T0-e2_p01_asc]"
     */
    public static String createThreadLabel(int threadId, String configId) {
        String shortLabel = configId
            .replace("eternity2_", "e2_")
            .replace("ascending", "asc")
            .replace("descending", "desc");
        return "[T" + threadId + "-" + shortLabel + "]";
    }

    /**
     * Normalizes a puzzle/configuration name for consistent usage.
     * Removes special characters and converts to lowercase.
     *
     * @param name Raw configuration name
     * @return Normalized name suitable for file names and identifiers
     */
    public static String normalizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
    }
}
