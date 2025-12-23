package app.parallel;

import config.PuzzleConfig;
import util.ConfigurationUtils;
import util.SaveStateManager;
import util.SolverLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scans and analyzes available puzzle configurations.
 * Extracted from MainParallel for better separation of concerns.
 *
 * Responsibilities:
 * - Discovers configuration files in data directory
 * - Loads and validates configurations
 * - Checks for existing save states
 * - Calculates cumulative compute time
 * - Sorts by priority (new configs first, then by time)
 */
public class ConfigurationScanner {

    private final String dataDirectory;

    public ConfigurationScanner(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    /**
     * Information about an available configuration.
     */
    public static class ConfigInfo implements Comparable<ConfigInfo> {
        public final String filepath;
        public final PuzzleConfig config;
        public final File currentSave;
        public final long totalComputeTimeMs;
        public final boolean hasBeenStarted;

        public ConfigInfo(String filepath, PuzzleConfig config, File currentSave, long totalComputeTimeMs) {
            this.filepath = filepath;
            this.config = config;
            this.currentSave = currentSave;
            this.totalComputeTimeMs = totalComputeTimeMs;
            this.hasBeenStarted = (currentSave != null);
        }

        @Override
        public int compareTo(ConfigInfo other) {
            // 1. Priority to never-started configs
            if (!this.hasBeenStarted && other.hasBeenStarted) return -1;
            if (this.hasBeenStarted && !other.hasBeenStarted) return 1;

            // 2. Among started ones, sort by cumulative time (less time = priority)
            if (this.hasBeenStarted && other.hasBeenStarted) {
                return Long.compare(this.totalComputeTimeMs, other.totalComputeTimeMs);
            }

            // 3. Among non-started, alphabetical order
            return this.filepath.compareTo(other.filepath);
        }
    }

    /**
     * Finds and analyzes all available Eternity II configurations.
     *
     * @return Sorted list of configurations (priority order)
     * @throws IOException if directory cannot be read
     */
    public List<ConfigInfo> scanConfigurations() throws IOException {
        List<ConfigInfo> configs = new ArrayList<>();

        File dataDir = new File(dataDirectory + "eternity2/");
        File[] configFiles = dataDir.listFiles((dir, name) ->
            name.startsWith("eternity2") && name.endsWith(".txt")
        );

        if (configFiles == null || configFiles.length == 0) {
            SolverLogger.info("✗ No configuration found in " + dataDirectory);
            return configs;
        }

        SolverLogger.info("📁 Analyzing " + configFiles.length + " available configurations...");
        SolverLogger.info("");

        for (File file : configFiles) {
            try {
                SolverLogger.info("  🔍 Analyzing: " + file.getName());
                ConfigInfo info = analyzeConfiguration(file);
                if (info != null) {
                    configs.add(info);
                    SolverLogger.info("     ✓ Loaded successfully");
                } else {
                    SolverLogger.warn("     ✗ Failed to load (PuzzleConfig.loadFromFile returned null)");
                }
            } catch (IOException | RuntimeException e) {
                SolverLogger.error("     ✗ Error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Sort by priority
        Collections.sort(configs);

        return configs;
    }

    /**
     * Analyzes a single configuration file.
     */
    private ConfigInfo analyzeConfiguration(File file) throws IOException {
        // Load the config
        SolverLogger.debug("     Loading config from: " + file.getAbsolutePath());
        PuzzleConfig config = PuzzleConfig.loadFromFile(file.getAbsolutePath());
        if (config == null) {
            SolverLogger.warn("     PuzzleConfig.loadFromFile returned null for: " + file.getName());
            return null;
        }

        // Extract configId from file name
        String configId = ConfigurationUtils.extractConfigId(file.getAbsolutePath());

        // Look for existing save
        File currentSave = SaveStateManager.findCurrentSave(configId);

        // Read total cumulative compute time
        long totalComputeTimeMs = 0;
        if (currentSave != null) {
            totalComputeTimeMs = SaveStateManager.readTotalComputeTime(configId);
        }

        return new ConfigInfo(file.getAbsolutePath(), config, currentSave, totalComputeTimeMs);
    }

    /**
     * Displays statistics about scanned configurations.
     */
    public void displayStatistics(List<ConfigInfo> configs) {
        int notStarted = 0;
        int inProgress = 0;

        for (ConfigInfo info : configs) {
            if (!info.hasBeenStarted) {
                notStarted++;
            } else {
                inProgress++;
            }
        }

        SolverLogger.info("╔═══════════════════════════════════════════════════════════════════╗");
        SolverLogger.info("║              CONFIGURATION STATISTICS                     ║");
        SolverLogger.info("╚═══════════════════════════════════════════════════════════════════╝");
        SolverLogger.info("");
        SolverLogger.info("  📊 Total configurations : " + configs.size());
        SolverLogger.info("  🆕 Never started        : " + notStarted);
        SolverLogger.info("  🔄 In progress          : " + inProgress);
        SolverLogger.info("");
    }
}
