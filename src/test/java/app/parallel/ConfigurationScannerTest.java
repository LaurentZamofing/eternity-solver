package app.parallel;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigurationScanner.
 * Tests configuration discovery, priority sorting, and compute time calculation.
 */
@DisplayName("ConfigurationScanner Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfigurationScannerTest {

    private Path tempDataDir;
    private ConfigurationScanner scanner;

    @BeforeEach
    void setUp() throws IOException {
        // Create temporary test data directory
        tempDataDir = Files.createTempDirectory("test_scanner_");
        scanner = new ConfigurationScanner(tempDataDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        // Cleanup test files
        if (tempDataDir != null && Files.exists(tempDataDir)) {
            Files.walk(tempDataDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }

        // Cleanup test saves
        cleanupTestSaves("test_scan_1");
        cleanupTestSaves("test_scan_2");
        cleanupTestSaves("test_scan_3");
    }

    @Test
    @Order(1)
    @DisplayName("Should find all .txt configuration files")
    void testScanFindsAllConfigurations() throws IOException {
        // Arrange - Create 3 test configuration files
        createTestConfig("test_scan_1.txt", 3, 3, 9);
        createTestConfig("test_scan_2.txt", 4, 4, 16);
        createTestConfig("test_scan_3.txt", 5, 5, 25);

        // Create a non-config file that should be ignored
        File otherFile = new File(tempDataDir.toFile(), "README.md");
        try (FileWriter writer = new FileWriter(otherFile)) {
            writer.write("# Not a configuration file\n");
        }

        // Act
        List<ConfigurationScanner.ConfigInfo> configs = scanner.scanConfigurations();

        // Assert
        assertEquals(3, configs.size(), "Should find exactly 3 configuration files");

        // Verify all configs have required fields
        for (ConfigurationScanner.ConfigInfo configInfo : configs) {
            assertNotNull(configInfo.filepath, "Config filepath should not be null");
            assertNotNull(configInfo.config, "Config object should not be null");
            assertTrue(new File(configInfo.filepath).exists(), "Config file should exist");
            assertTrue(configInfo.filepath.endsWith(".txt"), "Config file should be .txt");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should prioritize new configs over resumed configs")
    void testPrioritySorting() throws IOException {
        // Arrange
        createTestConfig("never_started.txt", 3, 3, 9);
        createTestConfig("has_save.txt", 3, 3, 9);

        // Create a save file for "has_save" to make it resumed
        File saveDir = new File("saves/has_save/");
        saveDir.mkdirs();
        File saveFile = new File(saveDir, "current_" + System.currentTimeMillis() + ".txt");
        try (FileWriter writer = new FileWriter(saveFile)) {
            writer.write("# Test save\n");
            writer.write("# Depth: 10 pieces\n");
        }

        // Act
        List<ConfigurationScanner.ConfigInfo> configs = scanner.scanConfigurations();

        // Assert
        assertEquals(2, configs.size(), "Should find 2 configurations");

        // First config should be the never-started one
        ConfigurationScanner.ConfigInfo first = configs.get(0);
        assertTrue(first.filepath.contains("never_started"),
            "Never-started config should be prioritized first");
        assertFalse(first.hasBeenStarted, "First config should not have been started");

        // Second config should be the resumed one
        ConfigurationScanner.ConfigInfo second = configs.get(1);
        assertTrue(second.filepath.contains("has_save"),
            "Resumed config should come second");
        assertTrue(second.hasBeenStarted, "Second config should have been started");
    }

    @Test
    @Order(3)
    @DisplayName("Should correctly calculate total compute time from saves")
    void testComputeTimeCalculation() throws IOException {
        // Arrange
        createTestConfig("timed_config.txt", 3, 3, 9);

        // Create multiple save files with different compute times
        File saveDir = new File("saves/timed_config/");
        saveDir.mkdirs();

        // Save 1: 1000ms compute time
        createSaveFile(saveDir, "current_001.txt", 5, 1000L);

        // Save 2: 2000ms compute time
        createSaveFile(saveDir, "current_002.txt", 10, 2000L);

        // Save 3: 3000ms compute time
        createSaveFile(saveDir, "current_003.txt", 15, 3000L);

        // Act
        List<ConfigurationScanner.ConfigInfo> configs = scanner.scanConfigurations();

        // Assert
        assertEquals(1, configs.size(), "Should find 1 configuration");

        ConfigurationScanner.ConfigInfo configInfo = configs.get(0);
        assertTrue(configInfo.filepath.contains("timed_config"), "Config filepath should contain timed_config");

        // Total compute time should be from the most recent save (highest timestamp)
        // In our case, it should be 3000ms from current_003.txt
        assertTrue(configInfo.totalComputeTimeMs >= 3000L,
            "Total compute time should be at least 3000ms from latest save");
    }

    @Test
    @Order(4)
    @DisplayName("Should handle directory with no configuration files")
    void testEmptyDirectory() throws IOException {
        // Arrange - empty directory (no config files created)

        // Act
        List<ConfigurationScanner.ConfigInfo> configs = scanner.scanConfigurations();

        // Assert
        assertNotNull(configs, "Configs list should not be null");
        assertEquals(0, configs.size(), "Should find 0 configurations in empty directory");
    }

    @Test
    @Order(5)
    @DisplayName("Should handle configs with special characters in names")
    void testSpecialCharactersInNames() throws IOException {
        // Arrange
        createTestConfig("test-config_v2.txt", 3, 3, 9);
        createTestConfig("puzzle_2024-12-23.txt", 4, 4, 16);

        // Act
        List<ConfigurationScanner.ConfigInfo> configs = scanner.scanConfigurations();

        // Assert
        assertEquals(2, configs.size(), "Should find 2 configurations with special chars");

        boolean foundDash = configs.stream()
            .anyMatch(c -> c.filepath.contains("test-config_v2"));
        boolean foundDate = configs.stream()
            .anyMatch(c -> c.filepath.contains("puzzle_2024-12-23"));

        assertTrue(foundDash, "Should find config with dash and underscore");
        assertTrue(foundDate, "Should find config with date format");
    }

    // Helper methods

    private void createTestConfig(String filename, int rows, int cols, int numPieces) throws IOException {
        File configFile = new File(tempDataDir.toFile(), filename);
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("# Test configuration\n");
            writer.write("# Dimensions: " + rows + "x" + cols + "\n");
            writer.write(rows + " " + cols + "\n");
            writer.write("\n");
            writer.write("# Pieces (id north east south west)\n");
            for (int i = 1; i <= numPieces; i++) {
                writer.write(i + " 1 2 3 4\n");
            }
        }
    }

    private void createSaveFile(File saveDir, String filename, int depth, long computeTimeMs) throws IOException {
        File saveFile = new File(saveDir, filename);
        try (FileWriter writer = new FileWriter(saveFile)) {
            writer.write("# Save file\n");
            writer.write("# Timestamp: " + System.currentTimeMillis() + "\n");
            writer.write("# Depth: " + depth + " pieces\n");
            writer.write("# TotalComputeTime: " + computeTimeMs + " ms\n");
            writer.write("\n");
            writer.write("# Board state\n");
            // Minimal valid save file content
        }
    }

    private void cleanupTestSaves(String configName) {
        File saveDir = new File("saves/" + configName + "/");
        if (saveDir.exists()) {
            File[] files = saveDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            saveDir.delete();
        }
    }
}
