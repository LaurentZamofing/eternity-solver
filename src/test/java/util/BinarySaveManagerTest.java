package util;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;
import util.state.BinarySaveManager;
import util.state.BinarySaveManager.SaveData;
import util.state.BinarySaveManager.PlacementData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for BinarySaveManager.
 * Tests binary save/load operations for fast state persistence.
 *
 * Critical for ensuring binary format integrity and performance.
 */
@DisplayName("BinarySaveManager Tests")
class BinarySaveManagerTest {

    private Path tempDir;
    private Board board;
    private Map<Integer, Piece> pieces;
    private boolean[] pieceUsed;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("binary_save_test_");

        // Create test board (3x3)
        board = new Board(3, 3);
        pieces = createTestPieces();
        pieceUsed = new boolean[10]; // Support piece IDs 1-9
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    // ==================== Save/Load Round-Trip Tests ====================

    @Test
    @DisplayName("Should save and load board state successfully")
    void testSaveLoadRoundTrip() throws IOException {
        // Place some pieces
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(2), 1);
        board.place(1, 0, pieces.get(3), 2);
        pieceUsed[1] = true;
        pieceUsed[2] = true;
        pieceUsed[3] = true;

        String filename = tempDir.resolve("test_save.bin").toString();

        // Save
        BinarySaveManager.saveBinary(filename, board, pieces, pieceUsed, 9);

        // Verify file exists
        File savedFile = new File(filename);
        assertTrue(savedFile.exists(), "Binary file should exist");
        assertTrue(savedFile.length() > 0, "Binary file should not be empty");

        // Load
        SaveData loaded = BinarySaveManager.loadBinary(filename);

        // Verify loaded data
        assertNotNull(loaded, "Loaded data should not be null");
        assertEquals(3, loaded.rows, "Rows should match");
        assertEquals(3, loaded.cols, "Cols should match");
        assertEquals(3, loaded.placements.size(), "Should have 3 placements");

        // Verify placements
        Map<String, PlacementData> placementMap = new HashMap<>();
        for (PlacementData pd : loaded.placements) {
            placementMap.put(pd.row + "," + pd.col, pd);
        }

        assertTrue(placementMap.containsKey("0,0"), "Should have placement at (0,0)");
        assertTrue(placementMap.containsKey("0,1"), "Should have placement at (0,1)");
        assertTrue(placementMap.containsKey("1,0"), "Should have placement at (1,0)");

        assertEquals(1, placementMap.get("0,0").pieceId, "Piece at (0,0) should match");
        assertEquals(0, placementMap.get("0,0").rotation, "Rotation at (0,0) should match");

        // Verify piece usage
        assertTrue(loaded.pieceUsed[1], "Piece 1 should be used");
        assertTrue(loaded.pieceUsed[2], "Piece 2 should be used");
        assertTrue(loaded.pieceUsed[3], "Piece 3 should be used");
        assertFalse(loaded.pieceUsed[4], "Piece 4 should not be used");
    }

    @Test
    @DisplayName("Should handle empty board save and load")
    void testEmptyBoardRoundTrip() throws IOException {
        String filename = tempDir.resolve("empty_save.bin").toString();

        // Save empty board
        BinarySaveManager.saveBinary(filename, board, pieces, pieceUsed, 9);

        // Load
        SaveData loaded = BinarySaveManager.loadBinary(filename);

        assertNotNull(loaded, "Loaded data should not be null");
        assertEquals(3, loaded.rows, "Rows should match");
        assertEquals(3, loaded.cols, "Cols should match");
        assertEquals(0, loaded.placements.size(), "Should have no placements");
    }

    @Test
    @DisplayName("Should preserve all rotations")
    void testAllRotations() throws IOException {
        // Place pieces with different rotations
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(2), 1);
        board.place(0, 2, pieces.get(3), 2);
        board.place(1, 0, pieces.get(4), 3);
        pieceUsed[1] = pieceUsed[2] = pieceUsed[3] = pieceUsed[4] = true;

        String filename = tempDir.resolve("rotations.bin").toString();

        BinarySaveManager.saveBinary(filename, board, pieces, pieceUsed, 9);
        SaveData loaded = BinarySaveManager.loadBinary(filename);

        Map<String, PlacementData> placementMap = new HashMap<>();
        for (PlacementData pd : loaded.placements) {
            placementMap.put(pd.row + "," + pd.col, pd);
        }

        assertEquals(0, placementMap.get("0,0").rotation, "Rotation 0 should be preserved");
        assertEquals(1, placementMap.get("0,1").rotation, "Rotation 1 should be preserved");
        assertEquals(2, placementMap.get("0,2").rotation, "Rotation 2 should be preserved");
        assertEquals(3, placementMap.get("1,0").rotation, "Rotation 3 should be preserved");
    }

    @Test
    @DisplayName("Should preserve timestamp")
    void testTimestampPreservation() throws IOException {
        String filename = tempDir.resolve("timestamp.bin").toString();

        long beforeSave = System.currentTimeMillis();
        BinarySaveManager.saveBinary(filename, board, pieces, pieceUsed, 9);
        long afterSave = System.currentTimeMillis();

        SaveData loaded = BinarySaveManager.loadBinary(filename);

        assertTrue(loaded.timestamp >= beforeSave, "Timestamp should be after save start");
        assertTrue(loaded.timestamp <= afterSave, "Timestamp should be before save end");
    }

    // ==================== Validation Tests ====================

    @Test
    @DisplayName("Should validate correct binary save file")
    void testIsValidBinarySave() throws IOException {
        String filename = tempDir.resolve("valid.bin").toString();

        board.place(0, 0, pieces.get(1), 0);
        pieceUsed[1] = true;

        BinarySaveManager.saveBinary(filename, board, pieces, pieceUsed, 9);

        assertTrue(BinarySaveManager.isValidBinarySave(filename),
                  "Valid binary file should be recognized");
    }

    @Test
    @DisplayName("Should reject non-existent file")
    void testIsValidBinarySaveNonExistent() {
        String filename = tempDir.resolve("does_not_exist.bin").toString();

        assertFalse(BinarySaveManager.isValidBinarySave(filename),
                   "Non-existent file should not be valid");
    }

    @Test
    @DisplayName("Should reject file with wrong magic number")
    void testIsValidBinarySaveWrongMagic() throws IOException {
        String filename = tempDir.resolve("wrong_magic.bin").toString();

        // Write file with wrong magic number
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(
                new java.io.FileOutputStream(filename))) {
            out.writeInt(0x12345678); // Wrong magic
            out.writeInt(1); // Version
        }

        assertFalse(BinarySaveManager.isValidBinarySave(filename),
                   "File with wrong magic number should not be valid");
    }

    @Test
    @DisplayName("Should reject file with wrong version")
    void testIsValidBinarySaveWrongVersion() throws IOException {
        String filename = tempDir.resolve("wrong_version.bin").toString();

        // Write file with wrong version
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(
                new java.io.FileOutputStream(filename))) {
            out.writeInt(0x45544552); // Correct magic "ETER"
            out.writeInt(999); // Wrong version
        }

        assertFalse(BinarySaveManager.isValidBinarySave(filename),
                   "File with wrong version should not be valid");
    }

    // ==================== Timestamp Extraction Tests ====================

    @Test
    @DisplayName("Should extract timestamp without loading full file")
    void testGetTimestamp() throws IOException {
        String filename = tempDir.resolve("timestamp_extract.bin").toString();

        long beforeSave = System.currentTimeMillis();
        BinarySaveManager.saveBinary(filename, board, pieces, pieceUsed, 9);
        long afterSave = System.currentTimeMillis();

        long timestamp = BinarySaveManager.getTimestamp(filename);

        assertTrue(timestamp >= beforeSave, "Extracted timestamp should be valid");
        assertTrue(timestamp <= afterSave, "Extracted timestamp should be valid");
    }

    @Test
    @DisplayName("Should throw exception for non-existent file when getting timestamp")
    void testGetTimestampNonExistent() {
        String filename = tempDir.resolve("does_not_exist.bin").toString();

        assertThrows(IOException.class, () -> {
            BinarySaveManager.getTimestamp(filename);
        }, "Should throw IOException for non-existent file");
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should throw exception when loading invalid magic number")
    void testLoadInvalidMagicNumber() throws IOException {
        String filename = tempDir.resolve("invalid_magic.bin").toString();

        try (java.io.DataOutputStream out = new java.io.DataOutputStream(
                new java.io.FileOutputStream(filename))) {
            out.writeInt(0xBADBAD); // Wrong magic
            out.writeInt(1);
        }

        Exception exception = assertThrows(IOException.class, () -> {
            BinarySaveManager.loadBinary(filename);
        });
        assertTrue(exception.getMessage().contains("magic number"),
                  "Error message should mention magic number");
    }

    @Test
    @DisplayName("Should throw exception when loading unsupported version")
    void testLoadUnsupportedVersion() throws IOException {
        String filename = tempDir.resolve("unsupported_version.bin").toString();

        try (java.io.DataOutputStream out = new java.io.DataOutputStream(
                new java.io.FileOutputStream(filename))) {
            out.writeInt(0x45544552); // Correct magic
            out.writeInt(999); // Unsupported version
        }

        Exception exception = assertThrows(IOException.class, () -> {
            BinarySaveManager.loadBinary(filename);
        });
        assertTrue(exception.getMessage().contains("version"),
                  "Error message should mention version");
    }

    @Test
    @DisplayName("Should throw exception when loading non-existent file")
    void testLoadNonExistentFile() {
        String filename = tempDir.resolve("does_not_exist.bin").toString();

        assertThrows(IOException.class, () -> {
            BinarySaveManager.loadBinary(filename);
        }, "Should throw IOException for non-existent file");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle large board (16x16)")
    void testLargeBoard() throws IOException {
        Board largeBoard = new Board(16, 16);
        Map<Integer, Piece> largePieces = new HashMap<>();
        boolean[] largePieceUsed = new boolean[257]; // For 256 pieces

        // Create pieces and place first row
        for (int i = 1; i <= 16; i++) {
            largePieces.put(i, new Piece(i, new int[]{0, 1, 2, 3}));
            largeBoard.place(0, i - 1, largePieces.get(i), 0);
            largePieceUsed[i] = true;
        }

        String filename = tempDir.resolve("large_board.bin").toString();

        BinarySaveManager.saveBinary(filename, largeBoard, largePieces, largePieceUsed, 256);
        SaveData loaded = BinarySaveManager.loadBinary(filename);

        assertEquals(16, loaded.rows, "Large board rows should match");
        assertEquals(16, loaded.cols, "Large board cols should match");
        assertEquals(16, loaded.placements.size(), "Should have 16 placements");
    }

    @Test
    @DisplayName("Should handle full board")
    void testFullBoard() throws IOException {
        // Fill entire 3x3 board
        int pieceId = 1;
        boolean[] fullPieceUsed = new boolean[10];
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                board.place(r, c, pieces.get(pieceId), pieceId % 4);
                fullPieceUsed[pieceId] = true;
                pieceId++;
            }
        }

        String filename = tempDir.resolve("full_board.bin").toString();

        BinarySaveManager.saveBinary(filename, board, pieces, fullPieceUsed, 9);
        SaveData loaded = BinarySaveManager.loadBinary(filename);

        assertEquals(9, loaded.placements.size(), "Should have all 9 placements");

        // Verify all positions are filled
        Set<String> positions = new HashSet<>();
        for (PlacementData pd : loaded.placements) {
            positions.add(pd.row + "," + pd.col);
        }
        assertEquals(9, positions.size(), "Should have 9 unique positions");
    }

    @Test
    @DisplayName("Should handle piece IDs with gaps")
    void testPieceIdsWithGaps() throws IOException {
        // Use non-consecutive piece IDs
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(5), 1);
        board.place(1, 0, pieces.get(9), 2);

        boolean[] gappedUsage = new boolean[10];
        gappedUsage[1] = true;
        gappedUsage[5] = true;
        gappedUsage[9] = true;

        String filename = tempDir.resolve("gapped_ids.bin").toString();

        BinarySaveManager.saveBinary(filename, board, pieces, gappedUsage, 9);
        SaveData loaded = BinarySaveManager.loadBinary(filename);

        assertEquals(3, loaded.placements.size(), "Should have 3 placements");
        assertTrue(loaded.pieceUsed[1], "Piece 1 should be used");
        assertFalse(loaded.pieceUsed[2], "Piece 2 should not be used");
        assertTrue(loaded.pieceUsed[5], "Piece 5 should be used");
        assertFalse(loaded.pieceUsed[8], "Piece 8 should not be used");
        assertTrue(loaded.pieceUsed[9], "Piece 9 should be used");
    }

    @Test
    @DisplayName("Should handle binary file size efficiently")
    void testBinaryFileSize() throws IOException {
        // Binary should be much smaller than text format
        board.place(0, 0, pieces.get(1), 0);
        board.place(0, 1, pieces.get(2), 1);
        pieceUsed[1] = pieceUsed[2] = true;

        String filename = tempDir.resolve("size_test.bin").toString();

        BinarySaveManager.saveBinary(filename, board, pieces, pieceUsed, 9);

        File file = new File(filename);
        long fileSize = file.length();

        // Binary header (4+4+8=16) + dimensions (4+4=8) + count (4)
        // + 2 placements (2+2+2+1=7 each) + pieceUsed (4 + 9 bytes)
        // = 16 + 8 + 4 + 14 + 13 = 55 bytes approximately
        assertTrue(fileSize < 200, "Binary file should be compact (under 200 bytes)");
        assertTrue(fileSize > 40, "Binary file should contain all necessary data");
    }

    // ==================== Helper Methods ====================

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> testPieces = new HashMap<>();

        // Create simple test pieces (id, [N, E, S, W])
        testPieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));
        testPieces.put(2, new Piece(2, new int[]{0, 1, 2, 1}));
        testPieces.put(3, new Piece(3, new int[]{0, 0, 1, 1}));
        testPieces.put(4, new Piece(4, new int[]{2, 1, 3, 0}));
        testPieces.put(5, new Piece(5, new int[]{2, 1, 3, 1}));
        testPieces.put(6, new Piece(6, new int[]{2, 0, 3, 1}));
        testPieces.put(7, new Piece(7, new int[]{3, 1, 0, 0}));
        testPieces.put(8, new Piece(8, new int[]{3, 1, 0, 1}));
        testPieces.put(9, new Piece(9, new int[]{3, 0, 0, 1}));

        return testPieces;
    }
}
