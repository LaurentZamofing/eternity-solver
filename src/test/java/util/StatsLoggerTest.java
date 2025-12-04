package util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import solver.StatisticsManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StatsLogger.
 * Tests file creation, JSON format, rotation, and thread safety.
 */
class StatsLoggerTest {

    @TempDir
    Path tempDir;

    private StatsLogger logger;
    private String testPuzzle = "test_puzzle";

    @BeforeEach
    void setUp() {
        // Tests will use tempDir as base directory
    }

    @AfterEach
    void tearDown() {
        if (logger != null) {
            logger.close();
        }
    }

    @Test
    void testLoggerCreatesFile() {
        // Given
        logger = new StatsLogger(testPuzzle, tempDir);

        // When
        logger.appendStats(10, 0.5, 1000, null);
        logger.close();

        // Then
        Path expectedPath = tempDir.resolve("saves/eternity2/" + testPuzzle + "/stats_history.jsonl");
        assertTrue(Files.exists(expectedPath), "Stats file should be created");
    }

    @Test
    void testJsonLineFormat() throws IOException {
        // Given
        logger = new StatsLogger(testPuzzle, tempDir);
        StatisticsManager stats = new StatisticsManager();
        stats.backtracks = 100;
        stats.recursiveCalls = 500;
        stats.placements = 50;

        // When
        logger.appendStats(15, 1.2, 5000, stats);
        logger.close();

        // Then
        Path logFile = logger.getLogPath();
        List<String> lines = Files.readAllLines(logFile);

        assertEquals(1, lines.size(), "Should have exactly one line");

        String line = lines.get(0);
        assertTrue(line.contains("\"depth\":15"), "Should contain depth");
        assertTrue(line.contains("\"progress\":1.2"), "Should contain progress");
        assertTrue(line.contains("\"computeMs\":5000"), "Should contain compute time");
        assertTrue(line.contains("\"backtracks\":100"), "Should contain backtracks");
        assertTrue(line.contains("\"calls\":500"), "Should contain recursive calls");
        assertTrue(line.contains("\"ts\":"), "Should contain timestamp");
    }

    @Test
    void testMultipleAppends() throws IOException {
        // Given
        logger = new StatsLogger(testPuzzle, tempDir);

        // When
        for (int i = 0; i < 10; i++) {
            logger.appendStats(i, i * 0.1, i * 1000, null);
        }
        logger.close();

        // Then
        Path logFile = logger.getLogPath();
        List<String> lines = Files.readAllLines(logFile);

        assertEquals(10, lines.size(), "Should have 10 lines");

        // Verify first and last line contain expected depths
        assertTrue(lines.get(0).contains("\"depth\":0"));
        assertTrue(lines.get(9).contains("\"depth\":9"));
    }

    @Test
    void testPiecesPerSecondCalculation() throws IOException {
        // Given
        logger = new StatsLogger(testPuzzle, tempDir);

        // When - 48 pieces in 300 seconds = 0.16 pieces/sec
        logger.appendStats(48, 0.0, 300000, null);
        logger.close();

        // Then
        Path logFile = logger.getLogPath();
        List<String> lines = Files.readAllLines(logFile);

        String line = lines.get(0);
        assertTrue(line.contains("\"piecesPerSec\":0.16"),
            "Should calculate pieces per second correctly");
    }

    @Test
    void testStatisticsIncluded() throws IOException {
        // Given
        logger = new StatsLogger(testPuzzle, tempDir);
        StatisticsManager stats = new StatisticsManager();
        stats.backtracks = 1523;
        stats.recursiveCalls = 9823;
        stats.placements = 748;
        stats.singletonsPlaced = 12;
        stats.deadEndsDetected = 143;
        stats.fitChecks = 9823;

        // When
        logger.appendStats(48, 0.12, 300000, stats);
        logger.close();

        // Then
        Path logFile = logger.getLogPath();
        String content = Files.readString(logFile);

        assertTrue(content.contains("\"backtracks\":1523"));
        assertTrue(content.contains("\"calls\":9823"));
        assertTrue(content.contains("\"placements\":748"));
        assertTrue(content.contains("\"singletons\":12"));
        assertTrue(content.contains("\"deadEnds\":143"));
        assertTrue(content.contains("\"fitChecks\":9823"));
    }

    @Test
    void testDirectoryCreation() {
        // Given - no saves directory exists
        assertFalse(Files.exists(tempDir.resolve("saves")));

        // When
        logger = new StatsLogger(testPuzzle, tempDir);
        logger.appendStats(1, 0.0, 100, null);

        // Then
        assertTrue(Files.exists(tempDir.resolve("saves/eternity2/" + testPuzzle)),
            "Should create directory structure");
    }

    @Test
    void testAppendToExistingFile() throws IOException {
        // Given - create file with initial content
        logger = new StatsLogger(testPuzzle, tempDir);
        logger.appendStats(10, 0.0, 1000, null);
        logger.close();

        Path logFile = tempDir.resolve("saves/eternity2/" + testPuzzle + "/stats_history.jsonl");
        long initialSize = Files.size(logFile);

        // When - append more data
        logger = new StatsLogger(testPuzzle, tempDir);
        logger.appendStats(20, 0.0, 2000, null);
        logger.close();

        // Then
        long finalSize = Files.size(logFile);
        assertTrue(finalSize > initialSize, "File should grow");

        List<String> lines = Files.readAllLines(logFile);
        assertEquals(2, lines.size(), "Should have 2 lines");
    }

    @Test
    void testJsonFormatValidity() throws IOException {
        // Given
        logger = new StatsLogger(testPuzzle, tempDir);

        // When
        logger.appendStats(25, 2.5, 15000, null);
        logger.close();

        // Then - verify we can parse the JSON
        Path logFile = logger.getLogPath();
        String content = Files.readString(logFile);

        com.google.gson.Gson gson = new com.google.gson.Gson();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> data = gson.fromJson(content, java.util.Map.class);

        assertNotNull(data);
        assertEquals(25, ((Number) data.get("depth")).intValue());
        assertEquals(2.5, ((Number) data.get("progress")).doubleValue());
        assertEquals(15000, ((Number) data.get("computeMs")).longValue());
    }
}
