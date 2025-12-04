package monitoring.service;

import monitoring.model.ConfigMetrics;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for SaveFileParser.
 * Tests parsing of save files, metric extraction, board state parsing, and error handling.
 */
public class SaveFileParserTest {

    private SaveFileParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new SaveFileParser();
    }

    /**
     * Test parsing a valid save file with all fields.
     */
    @Test
    @DisplayName("Parse valid save file with all metrics")
    void testParseValidSaveFile() throws IOException {
        String content = """
                # Timestamp: 1763546144171
                # Date: 2025-11-19_10-55-44
                # Puzzle: eternity2_p01_ascending
                # Dimensions: 6x12
                # Depth: 72
                # Progress: 8.85004471%
                # TotalComputeTime: 262685 ms (0h 04m 22s)
                """;

        File file = createTempSaveFile("current_123.txt", content);
        ConfigMetrics metrics = parser.parseSaveFile(file);

        assertNotNull(metrics, "Metrics should not be null");
        assertEquals("eternity2_p01_ascending", metrics.getConfigName());
        assertEquals(1763546144171L, metrics.getTimestamp());
        assertEquals(6, metrics.getRows());
        assertEquals(12, metrics.getCols());
        assertEquals(72, metrics.getTotalPieces());
        assertEquals(72, metrics.getDepth());
        assertEquals(8.85004471, metrics.getProgressPercentage(), 0.0001);
        assertEquals(262685, metrics.getTotalComputeTimeMs());
        assertEquals("active", metrics.getStatus());
    }

    /**
     * Test parsing file with French number format (comma decimal separator).
     */
    @Test
    @DisplayName("Parse file with French number format")
    void testParseFrenchNumberFormat() throws IOException {
        String content = """
                # Timestamp: 1763546144171
                # Puzzle: test_config
                # Dimensions: 4x4
                # Depth: 16
                # Progress: 12,34567890%
                # TotalComputeTime: 5000 ms
                """;

        File file = createTempSaveFile("current_456.txt", content);
        ConfigMetrics metrics = parser.parseSaveFile(file);

        assertNotNull(metrics);
        assertEquals(12.34567890, metrics.getProgressPercentage(), 0.0001);
    }

    /**
     * Test parsing file with extra text in metric lines.
     */
    @Test
    @DisplayName("Parse file with extra text after metrics")
    void testParseWithExtraText() throws IOException {
        String content = """
                # Timestamp: 1763546144171
                # Puzzle: test_config
                # Dimensions: 8x10
                # Depth: 80 pieces placed so far
                # Progress: 50.5% (estimation based on search space)
                # TotalComputeTime: 18402300 ms (5h 06m 42s)
                """;

        File file = createTempSaveFile("best_789.txt", content);
        ConfigMetrics metrics = parser.parseSaveFile(file);

        assertNotNull(metrics);
        assertEquals(80, metrics.getDepth());
        assertEquals(50.5, metrics.getProgressPercentage(), 0.0001);
        assertEquals(18402300, metrics.getTotalComputeTimeMs());
        assertEquals("milestone", metrics.getStatus());
    }

    /**
     * Test parsing board state from visual display.
     */
    @Test
    @DisplayName("Parse board state from visual display")
    void testParseBoardState() throws IOException {
        String content = """
                # Timestamp: 1763546144171
                # Puzzle: test_board
                # Dimensions: 3x3
                # Depth: 5
                # Progress: 55.55%
                # TotalComputeTime: 1000 ms
                #
                # ═══════════════════════════════════════════
                # VISUAL BOARD DISPLAY
                # ═══════════════════════════════════════════
                # 1_0 2_1 3_2
                # 4_3 . 6_0
                # 7_1 8_2 9_3
                # ═══════════════════════════════════════════
                """;

        File file = createTempSaveFile("current_board.txt", content);
        ConfigMetrics metrics = parser.parseSaveFile(file);

        assertNotNull(metrics);
        assertNotNull(metrics.getBoardState(), "Board state should not be null");

        String[][] board = metrics.getBoardState();
        assertEquals(3, board.length, "Should have 3 rows");
        assertEquals(3, board[0].length, "Should have 3 columns");

        // Check first row
        assertEquals("1_0", board[0][0]);
        assertEquals("2_1", board[0][1]);
        assertEquals("3_2", board[0][2]);

        // Check second row with empty cell
        assertEquals("4_3", board[1][0]);
        assertNull(board[1][1], "Empty cell should be null");
        assertEquals("6_0", board[1][2]);

        // Check third row
        assertEquals("7_1", board[2][0]);
        assertEquals("8_2", board[2][1]);
        assertEquals("9_3", board[2][2]);
    }

    /**
     * Test parsing larger board state.
     */
    @Test
    @DisplayName("Parse larger board (6x12)")
    void testParseLargerBoard() throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("""
                # Timestamp: 1763546144171
                # Puzzle: eternity2_large
                # Dimensions: 6x12
                # Depth: 72
                # Progress: 100.0%
                # TotalComputeTime: 5000 ms
                #
                # ═══════════════════════════════════════════
                # VISUAL BOARD DISPLAY
                # ═══════════════════════════════════════════
                """);

        // Generate 6 rows x 12 columns
        for (int row = 0; row < 6; row++) {
            content.append("# ");
            for (int col = 0; col < 12; col++) {
                int pieceId = row * 12 + col + 1;
                int rotation = (row + col) % 4;
                content.append(pieceId).append("_").append(rotation);
                if (col < 11) content.append(" ");
            }
            content.append("\n");
        }
        content.append("# ═══════════════════════════════════════════\n");

        File file = createTempSaveFile("current_large.txt", content.toString());
        ConfigMetrics metrics = parser.parseSaveFile(file);

        assertNotNull(metrics);
        assertNotNull(metrics.getBoardState());

        String[][] board = metrics.getBoardState();
        assertEquals(6, board.length);
        assertEquals(12, board[0].length);

        // Verify first piece
        assertEquals("1_0", board[0][0]);

        // Verify last piece (72 = 6*12)
        assertEquals("72_0", board[5][11]);

        // Verify no null cells (board is full)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 12; col++) {
                assertNotNull(board[row][col],
                    String.format("Cell [%d][%d] should not be null", row, col));
            }
        }
    }

    /**
     * Test derived metrics calculation (pieces per second, ETA, physical progress).
     */
    @Test
    @DisplayName("Calculate derived metrics correctly")
    void testDerivedMetrics() throws IOException {
        String content = """
                # Timestamp: 1763546144171
                # Puzzle: test_config
                # Dimensions: 10x10
                # Depth: 50
                # Progress: 25.0%
                # TotalComputeTime: 10000 ms
                """;

        File file = createTempSaveFile("current_metrics.txt", content);
        ConfigMetrics metrics = parser.parseSaveFile(file);

        assertNotNull(metrics);

        // Physical progress: 50/100 = 50%
        assertEquals(50.0, metrics.getPhysicalProgressPercentage(), 0.01);

        // Pieces per second: 50 pieces / 10 seconds = 5 p/s
        assertEquals(5.0, metrics.getPiecesPerSecond(), 0.01);

        // ETA: 50 remaining pieces / 5 p/s = 10 seconds = 10000 ms
        assertEquals(10000, metrics.getEstimatedTimeRemainingMs());

        // Not solved yet
        assertFalse(metrics.isSolved());
    }

    /**
     * Test solved detection.
     */
    @Test
    @DisplayName("Detect solved puzzle")
    void testSolvedDetection() throws IOException {
        String content = """
                # Timestamp: 1763546144171
                # Puzzle: solved_puzzle
                # Dimensions: 4x4
                # Depth: 16
                # Progress: 100.0%
                # TotalComputeTime: 5000 ms
                """;

        File file = createTempSaveFile("best_solved.txt", content);
        ConfigMetrics metrics = parser.parseSaveFile(file);

        assertNotNull(metrics);
        assertTrue(metrics.isSolved(), "Puzzle should be marked as solved");
        assertEquals(100.0, metrics.getPhysicalProgressPercentage(), 0.01);
    }

    /**
     * Test config name extraction from directory path.
     */
    @Test
    @DisplayName("Extract config name from directory structure")
    void testConfigNameExtraction() throws IOException {
        // Create subdirectory structure
        Path configDir = tempDir.resolve("eternity2").resolve("eternity2_p01_ascending");
        Files.createDirectories(configDir);

        String content = """
                # Timestamp: 1763546144171
                # Dimensions: 6x12
                # Depth: 10
                # Progress: 5.0%
                # TotalComputeTime: 1000 ms
                """;

        File file = configDir.resolve("current_123.txt").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }

        ConfigMetrics metrics = parser.parseSaveFile(file);

        assertNotNull(metrics);
        assertEquals("eternity2_p01_ascending", metrics.getConfigName());
    }

    /**
     * Test that Puzzle field overrides directory-based config name.
     */
    @Test
    @DisplayName("Puzzle field overrides directory name")
    void testPuzzleNameOverride() throws IOException {
        Path configDir = tempDir.resolve("old_directory_name");
        Files.createDirectories(configDir);

        String content = """
                # Timestamp: 1763546144171
                # Puzzle: correct_puzzle_name
                # Dimensions: 4x4
                # Depth: 8
                # Progress: 50.0%
                # TotalComputeTime: 2000 ms
                """;

        File file = configDir.resolve("current_456.txt").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }

        ConfigMetrics metrics = parser.parseSaveFile(file);

        assertNotNull(metrics);
        assertEquals("correct_puzzle_name", metrics.getConfigName(),
            "Puzzle field should override directory name");
    }

    /**
     * Test status determination from filename.
     */
    @Test
    @DisplayName("Status from filename (current vs best)")
    void testStatusFromFilename() throws IOException {
        String content = """
                # Timestamp: 1763546144171
                # Puzzle: test
                # Dimensions: 4x4
                # Depth: 8
                # Progress: 50.0%
                # TotalComputeTime: 1000 ms
                """;

        // Test "current" file -> active status
        File currentFile = createTempSaveFile("current_123.txt", content);
        ConfigMetrics currentMetrics = parser.parseSaveFile(currentFile);
        assertEquals("active", currentMetrics.getStatus());

        // Test "best" file -> milestone status
        File bestFile = createTempSaveFile("best_456.txt", content);
        ConfigMetrics bestMetrics = parser.parseSaveFile(bestFile);
        assertEquals("milestone", bestMetrics.getStatus());
    }

    /**
     * Test isSaveFile method.
     */
    @Test
    @DisplayName("Identify valid save files")
    void testIsSaveFile() throws IOException {
        Path current = tempDir.resolve("current_123.txt");
        Path best = tempDir.resolve("best_456.txt");
        Path invalid1 = tempDir.resolve("random.txt");
        Path invalid2 = tempDir.resolve("current.txt");
        Path invalid3 = tempDir.resolve("best_.txt");

        Files.createFile(current);
        Files.createFile(best);
        Files.createFile(invalid1);
        Files.createFile(invalid2);
        Files.createFile(invalid3);

        assertTrue(parser.isSaveFile(current), "current_123.txt should be valid");
        assertTrue(parser.isSaveFile(best), "best_456.txt should be valid");
        assertFalse(parser.isSaveFile(invalid1), "random.txt should be invalid");
        assertFalse(parser.isSaveFile(invalid2), "current.txt should be invalid");
        assertFalse(parser.isSaveFile(invalid3), "best_.txt should be invalid");
    }

    /**
     * Test isCurrentSave and isBestSave methods.
     */
    @Test
    @DisplayName("Distinguish current vs best saves")
    void testSaveTypeDetection() throws IOException {
        Path current = tempDir.resolve("current_123.txt");
        Path best = tempDir.resolve("best_456.txt");

        Files.createFile(current);
        Files.createFile(best);

        assertTrue(parser.isCurrentSave(current));
        assertFalse(parser.isBestSave(current));

        assertTrue(parser.isBestSave(best));
        assertFalse(parser.isCurrentSave(best));
    }

    /**
     * Test parsing file that doesn't exist.
     */
    @Test
    @DisplayName("Handle non-existent file gracefully")
    void testNonExistentFile() {
        File nonExistent = new File(tempDir.toFile(), "does_not_exist.txt");
        ConfigMetrics metrics = parser.parseSaveFile(nonExistent);

        assertNull(metrics, "Should return null for non-existent file");
    }

    /**
     * Test parsing empty file.
     */
    @Test
    @DisplayName("Handle empty file gracefully")
    void testEmptyFile() throws IOException {
        File file = createTempSaveFile("current_empty.txt", "");
        ConfigMetrics metrics = parser.parseSaveFile(file);

        // Should create metrics with config name, but all fields at defaults
        assertNotNull(metrics);
        assertEquals(0, metrics.getDepth());
        assertEquals(0.0, metrics.getProgressPercentage());
    }

    /**
     * Test parsing file with partial data.
     */
    @Test
    @DisplayName("Parse file with only some fields")
    void testPartialData() throws IOException {
        String content = """
                # Timestamp: 1763546144171
                # Puzzle: partial_config
                # Depth: 42
                """;

        File file = createTempSaveFile("current_partial.txt", content);
        ConfigMetrics metrics = parser.parseSaveFile(file);

        assertNotNull(metrics);
        assertEquals("partial_config", metrics.getConfigName());
        assertEquals(1763546144171L, metrics.getTimestamp());
        assertEquals(42, metrics.getDepth());
        assertEquals(0.0, metrics.getProgressPercentage()); // Not specified
        assertEquals(0, metrics.getTotalComputeTimeMs()); // Not specified
    }

    /**
     * Test parsing board with mixed empty and filled cells.
     */
    @Test
    @DisplayName("Parse board with mixed empty cells")
    void testBoardWithEmptyCells() throws IOException {
        String content = """
                # Timestamp: 1763546144171
                # Puzzle: sparse_board
                # Dimensions: 4x4
                # Depth: 8
                # Progress: 50.0%
                # TotalComputeTime: 1000 ms
                #
                # ═══════════════════════════════════════════
                # VISUAL BOARD DISPLAY
                # ═══════════════════════════════════════════
                # 1_0 2_1 . .
                # 5_2 . . 8_3
                # . 10_1 11_2 .
                # 13_3 14_0 . 16_1
                # ═══════════════════════════════════════════
                """;

        File file = createTempSaveFile("current_sparse.txt", content);
        ConfigMetrics metrics = parser.parseSaveFile(file);

        assertNotNull(metrics);
        String[][] board = metrics.getBoardState();
        assertNotNull(board);

        // Count non-null cells
        int filledCount = 0;
        for (String[] row : board) {
            for (String cell : row) {
                if (cell != null) filledCount++;
            }
        }

        assertEquals(9, filledCount, "Should have 9 filled cells");

        // Check specific cells
        assertEquals("1_0", board[0][0]);
        assertNull(board[0][2]);
        assertNull(board[1][1]);
        assertEquals("16_1", board[3][3]);
    }

    // Helper method to create temporary save files
    private File createTempSaveFile(String filename, String content) throws IOException {
        File file = tempDir.resolve(filename).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }
}
