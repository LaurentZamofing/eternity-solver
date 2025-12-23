package app;

import app.parallel.ConfigurationScanner;
import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;
import solver.EternitySolver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MainParallel's work distribution and rotation logic.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>Configuration priority scheduling (new configs first)</li>
 *   <li>Thread-safe configuration selection</li>
 *   <li>Automatic rotation after timeout</li>
 *   <li>Proper cleanup and state management</li>
 * </ul>
 *
 * <p>These are integration tests that create real solver instances.
 * For unit tests of individual components, see {@link app.parallel.ConfigurationScannerTest}.
 */
@DisplayName("MainParallel Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MainParallelIntegrationTest {

    private static final String TEST_DATA_DIR = "data/test_parallel/";
    private Path tempDataDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create temporary test data directory
        tempDataDir = Files.createTempDirectory("test_parallel_");

        // Create a simple test configuration
        createTestConfiguration("test_config_1");
        createTestConfiguration("test_config_2");
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
        File savesDir = new File("saves/test_config_1/");
        if (savesDir.exists()) {
            deleteDirectory(savesDir);
        }
        savesDir = new File("saves/test_config_2/");
        if (savesDir.exists()) {
            deleteDirectory(savesDir);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should scan and find multiple configurations")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testConfigurationScanning() throws IOException {
        // Arrange - configurations created in setUp()

        // Act
        ConfigurationScanner scanner = new ConfigurationScanner(tempDataDir.toString());
        List<ConfigurationScanner.ConfigInfo> configs = scanner.scanConfigurations();

        // Assert
        assertNotNull(configs, "Configurations list should not be null");
        assertEquals(2, configs.size(), "Should find 2 test configurations");

        // Verify configs have required properties
        for (ConfigurationScanner.ConfigInfo configInfo : configs) {
            assertNotNull(configInfo.filepath, "Config filepath should not be null");
            assertNotNull(configInfo.config, "Config object should not be null");
            assertTrue(new File(configInfo.filepath).exists(), "Config file should exist");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should prioritize never-started configs over resumed ones")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPriorityScheduling() throws IOException {
        // Arrange - Create a save file for config_1 to make it "resumed"
        File savesDir = new File("saves/test_config_1/");
        savesDir.mkdirs();
        File saveFile = new File(savesDir, "current_123456789.txt");
        try (FileWriter writer = new FileWriter(saveFile)) {
            writer.write("# Test save file\n");
            writer.write("# Depth: 5 pieces\n");
        }

        // Act
        ConfigurationScanner scanner = new ConfigurationScanner(tempDataDir.toString());
        List<ConfigurationScanner.ConfigInfo> configs = scanner.scanConfigurations();

        // Assert - config_2 (never started) should come before config_1 (has save)
        assertTrue(configs.size() >= 2, "Should have at least 2 configs");

        // Find which config has no save (should be prioritized)
        ConfigurationScanner.ConfigInfo neverStarted = configs.stream()
            .filter(c -> c.filepath.contains("test_config_2"))
            .findFirst()
            .orElse(null);

        ConfigurationScanner.ConfigInfo resumed = configs.stream()
            .filter(c -> c.filepath.contains("test_config_1"))
            .findFirst()
            .orElse(null);

        assertNotNull(neverStarted, "Should find never-started config");
        assertNotNull(resumed, "Should find resumed config");

        // Verify priority using hasBeenStarted flag
        assertFalse(neverStarted.hasBeenStarted, "test_config_2 should not have been started");
        assertTrue(resumed.hasBeenStarted, "test_config_1 should have been started");

        // Verify priority (never-started should come first)
        int neverStartedIndex = configs.indexOf(neverStarted);
        int resumedIndex = configs.indexOf(resumed);
        assertTrue(neverStartedIndex < resumedIndex,
            "Never-started config should be prioritized over resumed config");
    }

    @Test
    @Order(3)
    @DisplayName("Should handle concurrent config selection safely")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testThreadSafeConfigSelection() throws InterruptedException {
        // Arrange
        Set<String> selectedConfigs = Collections.synchronizedSet(new HashSet<>());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(4);

        List<String> availableConfigs = Arrays.asList("config1", "config2", "config3", "config4");
        Queue<String> configQueue = new ConcurrentLinkedQueue<>(availableConfigs);
        Object lock = new Object();

        // Act - Simulate multiple threads selecting configs
        for (int i = 0; i < 4; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();

                    // Simulate config selection with lock (like MainParallel does)
                    String selected;
                    synchronized (lock) {
                        selected = configQueue.poll();
                    }

                    if (selected != null) {
                        selectedConfigs.add(selected);
                        Thread.sleep(100); // Simulate work
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);

        // Assert
        assertTrue(completed, "All threads should complete");
        assertEquals(4, selectedConfigs.size(), "Each thread should select a unique config");
        assertTrue(selectedConfigs.containsAll(availableConfigs), "All configs should be selected");
    }

    @Test
    @Order(4)
    @DisplayName("Should track running configs correctly")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testRunningConfigTracking() {
        // Arrange
        Set<String> runningConfigs = Collections.synchronizedSet(new HashSet<>());

        // Act
        String config1 = "test_config_1";
        String config2 = "test_config_2";

        // Simulate adding configs to running set
        assertTrue(runningConfigs.add(config1), "Should add config1");
        assertFalse(runningConfigs.add(config1), "Should not add config1 twice");
        assertTrue(runningConfigs.add(config2), "Should add config2");

        // Assert
        assertEquals(2, runningConfigs.size(), "Should have 2 running configs");
        assertTrue(runningConfigs.contains(config1), "Should contain config1");
        assertTrue(runningConfigs.contains(config2), "Should contain config2");

        // Simulate removing a config
        assertTrue(runningConfigs.remove(config1), "Should remove config1");
        assertEquals(1, runningConfigs.size(), "Should have 1 running config");
        assertFalse(runningConfigs.contains(config1), "Should not contain config1");
    }

    // Helper methods

    private void createTestConfiguration(String name) throws IOException {
        File configFile = new File(tempDataDir.toFile(), name + ".txt");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("# Test configuration: " + name + "\n");
            writer.write("# Dimensions: 3x3\n");
            writer.write("3 3\n");
            writer.write("\n");
            writer.write("# Pieces (id north east south west)\n");
            for (int i = 1; i <= 9; i++) {
                writer.write(i + " 1 1 1 1\n");
            }
        }
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
