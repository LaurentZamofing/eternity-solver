package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ConfigurationUtils.
 * Tests configuration ID extraction, piece sorting, thread labels, and name normalization.
 */
@DisplayName("ConfigurationUtils Tests")
public class ConfigurationUtilsTest {

    // ===== extractConfigId tests =====

    @Test
    @DisplayName("extractConfigId removes .txt extension")
    void testExtractConfigIdRemovesTxtExtension() {
        assertEquals("example_4x4", ConfigurationUtils.extractConfigId("example_4x4.txt"));
        assertEquals("puzzle", ConfigurationUtils.extractConfigId("puzzle.txt"));
        assertEquals("test", ConfigurationUtils.extractConfigId("test.txt"));
    }

    @Test
    @DisplayName("extractConfigId removes directory path")
    void testExtractConfigIdRemovesPath() {
        assertEquals("example_4x4", ConfigurationUtils.extractConfigId("data/puzzles/example_4x4.txt"));
        assertEquals("config", ConfigurationUtils.extractConfigId("/absolute/path/to/config.txt"));
        assertEquals("file", ConfigurationUtils.extractConfigId("../../relative/file.txt"));
    }

    @Test
    @DisplayName("extractConfigId handles filename without extension")
    void testExtractConfigIdNoExtension() {
        assertEquals("puzzle", ConfigurationUtils.extractConfigId("puzzle"));
        assertEquals("config", ConfigurationUtils.extractConfigId("data/config"));
    }

    @Test
    @DisplayName("extractConfigId handles other extensions")
    void testExtractConfigIdOtherExtensions() {
        // Non-.txt extensions are kept
        assertEquals("file.json", ConfigurationUtils.extractConfigId("file.json"));
        assertEquals("data.csv", ConfigurationUtils.extractConfigId("data.csv"));
    }

    @Test
    @DisplayName("extractConfigId handles multiple dots")
    void testExtractConfigIdMultipleDots() {
        assertEquals("config.backup", ConfigurationUtils.extractConfigId("config.backup.txt"));
        assertEquals("file.v2.final", ConfigurationUtils.extractConfigId("path/file.v2.final.txt"));
    }

    @Test
    @DisplayName("extractConfigId handles empty path components")
    void testExtractConfigIdEmptyPathComponents() {
        assertEquals("file", ConfigurationUtils.extractConfigId("///file.txt"));
        assertEquals("config", ConfigurationUtils.extractConfigId("./config.txt"));
    }

    // ===== sortPiecesByOrder tests =====

    @Test
    @DisplayName("sortPiecesByOrder ascending sorts correctly")
    void testSortPiecesAscending() {
        List<Integer> pieces = new ArrayList<>(Arrays.asList(5, 2, 8, 1, 9, 3));
        ConfigurationUtils.sortPiecesByOrder(pieces, "ascending");

        assertEquals(Arrays.asList(1, 2, 3, 5, 8, 9), pieces);
    }

    @Test
    @DisplayName("sortPiecesByOrder descending sorts correctly")
    void testSortPiecesDescending() {
        List<Integer> pieces = new ArrayList<>(Arrays.asList(5, 2, 8, 1, 9, 3));
        ConfigurationUtils.sortPiecesByOrder(pieces, "descending");

        assertEquals(Arrays.asList(9, 8, 5, 3, 2, 1), pieces);
    }

    @Test
    @DisplayName("sortPiecesByOrder is case insensitive")
    void testSortPiecesCaseInsensitive() {
        List<Integer> pieces1 = new ArrayList<>(Arrays.asList(5, 2, 8));
        List<Integer> pieces2 = new ArrayList<>(Arrays.asList(5, 2, 8));
        List<Integer> pieces3 = new ArrayList<>(Arrays.asList(5, 2, 8));
        List<Integer> pieces4 = new ArrayList<>(Arrays.asList(5, 2, 8));

        ConfigurationUtils.sortPiecesByOrder(pieces1, "ASCENDING");
        ConfigurationUtils.sortPiecesByOrder(pieces2, "Ascending");
        ConfigurationUtils.sortPiecesByOrder(pieces3, "DESCENDING");
        ConfigurationUtils.sortPiecesByOrder(pieces4, "Descending");

        assertEquals(Arrays.asList(2, 5, 8), pieces1);
        assertEquals(Arrays.asList(2, 5, 8), pieces2);
        assertEquals(Arrays.asList(8, 5, 2), pieces3);
        assertEquals(Arrays.asList(8, 5, 2), pieces4);
    }

    @Test
    @DisplayName("sortPiecesByOrder null does not sort")
    void testSortPiecesNull() {
        List<Integer> pieces = new ArrayList<>(Arrays.asList(5, 2, 8, 1));
        List<Integer> original = new ArrayList<>(pieces);

        ConfigurationUtils.sortPiecesByOrder(pieces, null);

        assertEquals(original, pieces);
    }

    @Test
    @DisplayName("sortPiecesByOrder invalid value does not sort")
    void testSortPiecesInvalidValue() {
        List<Integer> pieces = new ArrayList<>(Arrays.asList(5, 2, 8, 1));
        List<Integer> original = new ArrayList<>(pieces);

        ConfigurationUtils.sortPiecesByOrder(pieces, "invalid");
        assertEquals(original, pieces);

        ConfigurationUtils.sortPiecesByOrder(pieces, "random");
        assertEquals(original, pieces);

        ConfigurationUtils.sortPiecesByOrder(pieces, "");
        assertEquals(original, pieces);
    }

    @Test
    @DisplayName("sortPiecesByOrder handles empty list")
    void testSortPiecesEmptyList() {
        List<Integer> pieces = new ArrayList<>();
        ConfigurationUtils.sortPiecesByOrder(pieces, "ascending");
        assertTrue(pieces.isEmpty());
    }

    @Test
    @DisplayName("sortPiecesByOrder handles single element")
    void testSortPiecesSingleElement() {
        List<Integer> pieces = new ArrayList<>(Collections.singletonList(42));
        ConfigurationUtils.sortPiecesByOrder(pieces, "descending");

        assertEquals(Collections.singletonList(42), pieces);
    }

    @Test
    @DisplayName("sortPiecesByOrder handles duplicates")
    void testSortPiecesWithDuplicates() {
        List<Integer> pieces = new ArrayList<>(Arrays.asList(3, 1, 3, 2, 1));
        ConfigurationUtils.sortPiecesByOrder(pieces, "ascending");

        assertEquals(Arrays.asList(1, 1, 2, 3, 3), pieces);
    }

    // ===== createThreadLabel tests =====

    @Test
    @DisplayName("createThreadLabel formats basic label")
    void testCreateThreadLabelBasic() {
        assertEquals("[T0-puzzle]", ConfigurationUtils.createThreadLabel(0, "puzzle"));
        assertEquals("[T1-config]", ConfigurationUtils.createThreadLabel(1, "config"));
        assertEquals("[T5-test]", ConfigurationUtils.createThreadLabel(5, "test"));
    }

    @Test
    @DisplayName("createThreadLabel shortens eternity2_ prefix")
    void testCreateThreadLabelShortensEternity() {
        assertEquals("[T0-e2_p01]", ConfigurationUtils.createThreadLabel(0, "eternity2_p01"));
        assertEquals("[T1-e2_puzzle]", ConfigurationUtils.createThreadLabel(1, "eternity2_puzzle"));
    }

    @Test
    @DisplayName("createThreadLabel shortens ascending/descending")
    void testCreateThreadLabelShortensOrder() {
        assertEquals("[T0-test_asc]", ConfigurationUtils.createThreadLabel(0, "test_ascending"));
        assertEquals("[T1-test_desc]", ConfigurationUtils.createThreadLabel(1, "test_descending"));
    }

    @Test
    @DisplayName("createThreadLabel combines all shortenings")
    void testCreateThreadLabelCombined() {
        String result = ConfigurationUtils.createThreadLabel(2, "eternity2_p01_ascending");
        assertEquals("[T2-e2_p01_asc]", result);

        String result2 = ConfigurationUtils.createThreadLabel(3, "eternity2_config_descending");
        assertEquals("[T3-e2_config_desc]", result2);
    }

    @Test
    @DisplayName("createThreadLabel handles large thread IDs")
    void testCreateThreadLabelLargeThreadId() {
        assertEquals("[T100-config]", ConfigurationUtils.createThreadLabel(100, "config"));
        assertEquals("[T999-test]", ConfigurationUtils.createThreadLabel(999, "test"));
    }

    @Test
    @DisplayName("createThreadLabel handles empty config ID")
    void testCreateThreadLabelEmptyConfigId() {
        assertEquals("[T0-]", ConfigurationUtils.createThreadLabel(0, ""));
    }

    @Test
    @DisplayName("createThreadLabel preserves unmatched text")
    void testCreateThreadLabelPreservesUnmatched() {
        assertEquals("[T0-puzzle_random_text]",
            ConfigurationUtils.createThreadLabel(0, "puzzle_random_text"));
    }

    // ===== normalizeName tests =====

    @Test
    @DisplayName("normalizeName converts to lowercase")
    void testNormalizeNameLowercase() {
        assertEquals("puzzle", ConfigurationUtils.normalizeName("Puzzle"));
        assertEquals("config", ConfigurationUtils.normalizeName("CONFIG"));
        assertEquals("test", ConfigurationUtils.normalizeName("TeSt"));
    }

    @Test
    @DisplayName("normalizeName replaces special characters with underscore")
    void testNormalizeNameReplaceSpecialChars() {
        assertEquals("puzzle_config", ConfigurationUtils.normalizeName("puzzle-config"));
        assertEquals("test_file", ConfigurationUtils.normalizeName("test file"));
        assertEquals("data_2024_01", ConfigurationUtils.normalizeName("data@2024#01"));
    }

    @Test
    @DisplayName("normalizeName preserves alphanumeric and underscore")
    void testNormalizeNamePreservesValid() {
        assertEquals("puzzle_123", ConfigurationUtils.normalizeName("puzzle_123"));
        assertEquals("config_v2_final", ConfigurationUtils.normalizeName("config_v2_final"));
        assertEquals("test123abc", ConfigurationUtils.normalizeName("TEST123ABC"));
    }

    @Test
    @DisplayName("normalizeName handles multiple special characters")
    void testNormalizeNameMultipleSpecialChars() {
        assertEquals("test___file", ConfigurationUtils.normalizeName("test!!!file"));
        assertEquals("a_b_c_d", ConfigurationUtils.normalizeName("a-b.c/d"));
    }

    @Test
    @DisplayName("normalizeName handles empty string")
    void testNormalizeNameEmpty() {
        assertEquals("", ConfigurationUtils.normalizeName(""));
    }

    @Test
    @DisplayName("normalizeName handles only special characters")
    void testNormalizeNameOnlySpecialChars() {
        assertEquals("____", ConfigurationUtils.normalizeName("!@#$"));
        assertEquals("___", ConfigurationUtils.normalizeName("- ."));
    }

    @Test
    @DisplayName("normalizeName handles Unicode characters")
    void testNormalizeNameUnicode() {
        assertEquals("puzzle___", ConfigurationUtils.normalizeName("puzzle_éà"));
        assertEquals("test___config", ConfigurationUtils.normalizeName("test_ñ_config"));
    }

    @Test
    @DisplayName("normalizeName is idempotent for valid names")
    void testNormalizeNameIdempotent() {
        String name = "valid_config_123";
        assertEquals(name, ConfigurationUtils.normalizeName(name));
        assertEquals(name, ConfigurationUtils.normalizeName(
            ConfigurationUtils.normalizeName(name)));
    }

    @Test
    @DisplayName("normalizeName handles path-like strings")
    void testNormalizeNamePathLike() {
        assertEquals("data_puzzles_config_txt",
            ConfigurationUtils.normalizeName("data/puzzles/config.txt"));
        assertEquals("c__users_doc_file_json",
            ConfigurationUtils.normalizeName("C:\\Users\\doc\\file.json"));
    }

    // ===== Integration tests =====

    @Test
    @DisplayName("Extract and normalize config ID from path")
    void testExtractAndNormalize() {
        String filepath = "data/puzzles/Test Puzzle-2024.txt";
        String configId = ConfigurationUtils.extractConfigId(filepath);
        String normalized = ConfigurationUtils.normalizeName(configId);

        assertEquals("Test Puzzle-2024", configId);
        assertEquals("test_puzzle_2024", normalized);
    }

    @Test
    @DisplayName("Sort and verify order consistency")
    void testSortConsistency() {
        List<Integer> pieces = new ArrayList<>(Arrays.asList(5, 2, 8, 1, 9, 3));
        ConfigurationUtils.sortPiecesByOrder(pieces, "ascending");
        List<Integer> firstSort = new ArrayList<>(pieces);

        ConfigurationUtils.sortPiecesByOrder(pieces, "ascending");
        assertEquals(firstSort, pieces, "Multiple sorts should be consistent");
    }

    @Test
    @DisplayName("Thread label with normalized config")
    void testThreadLabelWithNormalizedConfig() {
        String rawConfig = "Eternity2 P01-Ascending";
        String normalized = ConfigurationUtils.normalizeName(rawConfig);
        String label = ConfigurationUtils.createThreadLabel(0, normalized);

        // normalizeName just replaces special chars and lowercases
        assertEquals("eternity2_p01_ascending", normalized);
        // createThreadLabel then applies shortenings
        assertEquals("[T0-e2_p01_asc]", label);
    }
}
