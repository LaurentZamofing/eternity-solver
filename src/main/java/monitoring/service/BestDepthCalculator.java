package monitoring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Calculates the best (maximum) depth ever reached for a configuration.
 * Scans all best_*.txt files in the config directory to find the highest depth achieved.
 *
 * This service is used to display historical best achievements even when the current
 * solver run hasn't reached that depth yet.
 */
@Service
public class BestDepthCalculator {

    private static final Logger logger = LoggerFactory.getLogger(BestDepthCalculator.class);

    @Value("${monitoring.saves-directory:./saves}")
    private String savesDirectory;

    /**
     * Calculate the best depth ever reached for a configuration by scanning all best_*.txt files.
     *
     * The method looks for files named "best_N.txt" where N is the depth number,
     * and returns the maximum N found.
     *
     * @param configName the configuration name (e.g., "eternity2_p01_ascending")
     * @return the maximum depth found, or 0 if no best files exist
     */
    public int calculateBestDepthEver(String configName) {
        int maxDepth = 0;
        try {
            // Construct path to config directory: saves/eternity2/configName/
            Path configDir = Paths.get(savesDirectory, "eternity2", configName);

            if (Files.exists(configDir) && Files.isDirectory(configDir)) {
                // Find all best_*.txt files and extract depths
                maxDepth = Files.list(configDir)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.startsWith("best_") && fileName.endsWith(".txt");
                    })
                    .mapToInt(path -> {
                        String fileName = path.getFileName().toString();
                        try {
                            // Extract depth from "best_123.txt" -> 123
                            String depthStr = fileName.replace("best_", "").replace(".txt", "");
                            return Integer.parseInt(depthStr);
                        } catch (NumberFormatException e) {
                            logger.trace("Could not parse depth from filename: {}", fileName);
                            return 0;
                        }
                    })
                    .max()
                    .orElse(0);
            }
        } catch (IOException e) {
            logger.debug("Could not calculate best depth for {}: {}", configName, e.getMessage());
        }
        return maxDepth;
    }

    /**
     * Check if a config has any best_*.txt files (i.e., has reached some depth).
     *
     * @param configName the configuration name
     * @return true if at least one best_*.txt file exists
     */
    public boolean hasBestFiles(String configName) {
        try {
            Path configDir = Paths.get(savesDirectory, "eternity2", configName);
            if (Files.exists(configDir) && Files.isDirectory(configDir)) {
                return Files.list(configDir)
                    .anyMatch(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.startsWith("best_") && fileName.endsWith(".txt");
                    });
            }
        } catch (IOException e) {
            logger.debug("Could not check best files for {}: {}", configName, e.getMessage());
        }
        return false;
    }
}
