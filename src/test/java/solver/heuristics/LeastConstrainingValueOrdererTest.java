package solver.heuristics;

import model.Piece;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for LeastConstrainingValueOrderer.
 * Tests edge compatibility tables, piece difficulty scoring, and candidate piece filtering.
 */
@DisplayName("LeastConstrainingValueOrderer Tests")
class LeastConstrainingValueOrdererTest {

    private LeastConstrainingValueOrderer orderer;
    private Map<Integer, Piece> testPieces;

    @BeforeEach
    void setUp() {
        orderer = new LeastConstrainingValueOrderer(false);
        testPieces = createTestPieces();
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Should create orderer with non-verbose mode")
    void testConstructorNonVerbose() {
        LeastConstrainingValueOrderer orderer = new LeastConstrainingValueOrderer(false);
        assertNotNull(orderer, "Orderer should be created");
    }

    @Test
    @DisplayName("Should create orderer with verbose mode")
    void testConstructorVerbose() {
        LeastConstrainingValueOrderer orderer = new LeastConstrainingValueOrderer(true);
        assertNotNull(orderer, "Orderer should be created with verbose mode");
    }

    // ==================== Initialization Tests ====================

    @Test
    @DisplayName("Should not be initialized initially")
    void testNotInitiallyInitialized() {
        assertFalse(orderer.isInitialized(), "Orderer should not be initialized initially");
    }

    @Test
    @DisplayName("Should be initialized after building tables")
    void testInitializedAfterBuildingTables() {
        orderer.buildEdgeCompatibilityTables(testPieces);
        orderer.computePieceDifficulty(testPieces);

        assertTrue(orderer.isInitialized(), "Orderer should be initialized after building tables");
    }

    // ==================== Edge Compatibility Table Tests ====================

    @Test
    @DisplayName("Should build edge compatibility tables")
    void testBuildEdgeCompatibilityTables() {
        assertDoesNotThrow(() -> orderer.buildEdgeCompatibilityTables(testPieces),
                          "Should build tables without errors");
    }

    @Test
    @DisplayName("Should find pieces compatible with north edge value")
    void testNorthCompatiblePieces() {
        orderer.buildEdgeCompatibilityTables(testPieces);

        // Edge value 0 should be on borders (north on top row)
        Set<Integer> compatiblePieces = orderer.getNorthCompatiblePieces(0);

        assertNotNull(compatiblePieces, "Should return a set");
        assertFalse(compatiblePieces.isEmpty(), "Should find pieces with edge value 0 on north");
    }

    @Test
    @DisplayName("Should find pieces compatible with east edge value")
    void testEastCompatiblePieces() {
        orderer.buildEdgeCompatibilityTables(testPieces);

        // Edge value 1 exists in test pieces
        Set<Integer> compatiblePieces = orderer.getEastCompatiblePieces(1);

        assertNotNull(compatiblePieces, "Should return a set");
        assertFalse(compatiblePieces.isEmpty(), "Should find pieces with edge value 1");
    }

    @Test
    @DisplayName("Should find pieces compatible with south edge value")
    void testSouthCompatiblePieces() {
        orderer.buildEdgeCompatibilityTables(testPieces);

        // Edge value 2 exists in test pieces
        Set<Integer> compatiblePieces = orderer.getSouthCompatiblePieces(2);

        assertNotNull(compatiblePieces, "Should return a set");
        assertFalse(compatiblePieces.isEmpty(), "Should find pieces with edge value 2");
    }

    @Test
    @DisplayName("Should find pieces compatible with west edge value")
    void testWestCompatiblePieces() {
        orderer.buildEdgeCompatibilityTables(testPieces);

        // Edge value 0 should be on borders (west on left column)
        Set<Integer> compatiblePieces = orderer.getWestCompatiblePieces(0);

        assertNotNull(compatiblePieces, "Should return a set");
        assertFalse(compatiblePieces.isEmpty(), "Should find pieces with edge value 0 on west");
    }

    @Test
    @DisplayName("Should return empty set for non-existent edge value")
    void testNonExistentEdgeValue() {
        orderer.buildEdgeCompatibilityTables(testPieces);

        // Edge value 999 doesn't exist
        Set<Integer> compatiblePieces = orderer.getNorthCompatiblePieces(999);

        assertNotNull(compatiblePieces, "Should return a set");
        assertTrue(compatiblePieces.isEmpty(), "Should return empty set for non-existent edge");
    }

    // ==================== Piece Difficulty Tests ====================

    @Test
    @DisplayName("Should compute piece difficulty scores")
    void testComputePieceDifficulty() {
        orderer.buildEdgeCompatibilityTables(testPieces);

        assertDoesNotThrow(() -> orderer.computePieceDifficulty(testPieces),
                          "Should compute difficulty without errors");
    }

    @Test
    @DisplayName("Should get difficulty score for piece")
    void testGetDifficultyScore() {
        orderer.buildEdgeCompatibilityTables(testPieces);
        orderer.computePieceDifficulty(testPieces);

        int score = orderer.getDifficultyScore(1);

        assertTrue(score >= 0, "Difficulty score should be non-negative");
        assertNotEquals(Integer.MAX_VALUE, score, "Should have computed score for piece 1");
    }

    @Test
    @DisplayName("Should return MAX_VALUE for non-existent piece")
    void testDifficultyScoreNonExistentPiece() {
        orderer.buildEdgeCompatibilityTables(testPieces);
        orderer.computePieceDifficulty(testPieces);

        int score = orderer.getDifficultyScore(999);

        assertEquals(Integer.MAX_VALUE, score, "Should return MAX_VALUE for non-existent piece");
    }

    @Test
    @DisplayName("Should get all difficulty scores")
    void testGetAllDifficultyScores() {
        orderer.buildEdgeCompatibilityTables(testPieces);
        orderer.computePieceDifficulty(testPieces);

        Map<Integer, Integer> scores = orderer.getAllDifficultyScores();

        assertNotNull(scores, "Should return a map");
        assertEquals(testPieces.size(), scores.size(), "Should have scores for all pieces");
    }

    @Test
    @DisplayName("Should return copy of difficulty scores")
    void testDifficultyScoresIsCopy() {
        orderer.buildEdgeCompatibilityTables(testPieces);
        orderer.computePieceDifficulty(testPieces);

        Map<Integer, Integer> scores1 = orderer.getAllDifficultyScores();
        Map<Integer, Integer> scores2 = orderer.getAllDifficultyScores();

        assertNotSame(scores1, scores2, "Should return copy, not same instance");
        assertEquals(scores1, scores2, "Copies should have same content");
    }

    // ==================== Candidate Pieces Tests ====================

    @Test
    @DisplayName("Should get candidate pieces with single constraint")
    void testGetCandidatePiecesSingleConstraint() {
        orderer.buildEdgeCompatibilityTables(testPieces);

        // Only north constraint
        Set<Integer> candidates = orderer.getCandidatePieces(0, -1, -1, -1);

        assertNotNull(candidates, "Should return a set");
        assertFalse(candidates.isEmpty(), "Should find candidates with north edge 0");
    }

    @Test
    @DisplayName("Should get candidate pieces with multiple constraints")
    void testGetCandidatePiecesMultipleConstraints() {
        orderer.buildEdgeCompatibilityTables(testPieces);

        // North and west constraints (top-left corner)
        Set<Integer> candidates = orderer.getCandidatePieces(0, -1, -1, 0);

        assertNotNull(candidates, "Should return a set");
        // Corner pieces should satisfy this
        assertFalse(candidates.isEmpty(), "Should find corner pieces");
    }

    @Test
    @DisplayName("Should get empty set when no candidates match")
    void testGetCandidatePiecesNoMatch() {
        orderer.buildEdgeCompatibilityTables(testPieces);

        // Impossible constraints
        Set<Integer> candidates = orderer.getCandidatePieces(999, 999, 999, 999);

        assertNotNull(candidates, "Should return a set");
        assertTrue(candidates.isEmpty(), "Should return empty set for impossible constraints");
    }

    @Test
    @DisplayName("Should get candidate pieces with all constraints")
    void testGetCandidatePiecesAllConstraints() {
        orderer.buildEdgeCompatibilityTables(testPieces);

        // All four edge constraints
        Set<Integer> candidates = orderer.getCandidatePieces(0, 1, 2, 0);

        assertNotNull(candidates, "Should return a set");
        // Result depends on whether test pieces match exactly
    }

    @Test
    @DisplayName("Should return empty set with no constraints")
    void testGetCandidatePiecesNoConstraints() {
        orderer.buildEdgeCompatibilityTables(testPieces);

        // No constraints (-1 for all edges)
        Set<Integer> candidates = orderer.getCandidatePieces(-1, -1, -1, -1);

        assertNotNull(candidates, "Should return a set");
        assertTrue(candidates.isEmpty(), "Should return empty set when no constraints specified");
    }

    // ==================== Clear Tests ====================

    @Test
    @DisplayName("Should clear all data structures")
    void testClear() {
        orderer.buildEdgeCompatibilityTables(testPieces);
        orderer.computePieceDifficulty(testPieces);

        assertTrue(orderer.isInitialized(), "Should be initialized");

        orderer.clear();

        // After clear, should still be "initialized" (objects exist but are empty)
        // but no data should be present
        Set<Integer> pieces = orderer.getNorthCompatiblePieces(0);
        assertTrue(pieces.isEmpty(), "Edge compatibility should be cleared");
    }

    @Test
    @DisplayName("Should handle clear on uninitialized orderer")
    void testClearUninitialized() {
        assertDoesNotThrow(() -> orderer.clear(),
                          "Should handle clear on uninitialized orderer");
    }

    // ==================== Statistics Tests ====================

    @Test
    @DisplayName("Should return not initialized message before building tables")
    void testStatisticsNotInitialized() {
        String stats = orderer.getStatistics();

        assertNotNull(stats, "Should return a string");
        assertTrue(stats.contains("not initialized"), "Should indicate not initialized");
    }

    @Test
    @DisplayName("Should return statistics after building tables")
    void testStatisticsAfterBuilding() {
        orderer.buildEdgeCompatibilityTables(testPieces);
        orderer.computePieceDifficulty(testPieces);

        String stats = orderer.getStatistics();

        assertNotNull(stats, "Should return a string");
        assertTrue(stats.contains("unique edge values"), "Should include edge count");
        assertTrue(stats.contains("pieces/edge"), "Should include average pieces per edge");
    }

    // ==================== Verbose Mode Tests ====================

    @Test
    @DisplayName("Should execute with verbose mode enabled")
    void testVerboseMode() {
        LeastConstrainingValueOrderer verboseOrderer = new LeastConstrainingValueOrderer(true);

        // Should execute without throwing exceptions even with verbose output
        assertDoesNotThrow(() -> {
            verboseOrderer.buildEdgeCompatibilityTables(testPieces);
            verboseOrderer.computePieceDifficulty(testPieces);
        }, "Verbose mode should not cause errors");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle empty piece map")
    void testEmptyPieceMap() {
        Map<Integer, Piece> emptyPieces = new HashMap<>();

        assertDoesNotThrow(() -> {
            orderer.buildEdgeCompatibilityTables(emptyPieces);
            orderer.computePieceDifficulty(emptyPieces);
        }, "Should handle empty piece map");

        assertTrue(orderer.isInitialized(), "Should be initialized even with empty pieces");
    }

    @Test
    @DisplayName("Should handle single piece")
    void testSinglePiece() {
        Map<Integer, Piece> singlePiece = new HashMap<>();
        singlePiece.put(1, testPieces.get(1));

        orderer.buildEdgeCompatibilityTables(singlePiece);
        orderer.computePieceDifficulty(singlePiece);

        assertTrue(orderer.isInitialized(), "Should be initialized with single piece");

        Map<Integer, Integer> scores = orderer.getAllDifficultyScores();
        assertEquals(1, scores.size(), "Should have score for single piece");
    }

    @Test
    @DisplayName("Should handle pieces with same edge values")
    void testPiecesWithSameEdges() {
        Map<Integer, Piece> duplicatePieces = new HashMap<>();
        duplicatePieces.put(1, new Piece(1, new int[]{1, 2, 3, 4}));
        duplicatePieces.put(2, new Piece(2, new int[]{1, 2, 3, 4}));  // Same edges

        orderer.buildEdgeCompatibilityTables(duplicatePieces);
        orderer.computePieceDifficulty(duplicatePieces);

        Set<Integer> candidates = orderer.getNorthCompatiblePieces(1);
        assertTrue(candidates.size() >= 2, "Should find both pieces with same edge");
    }

    @Test
    @DisplayName("Should handle rebuild after clear")
    void testRebuildAfterClear() {
        orderer.buildEdgeCompatibilityTables(testPieces);
        orderer.computePieceDifficulty(testPieces);

        Map<Integer, Integer> scores1 = orderer.getAllDifficultyScores();

        orderer.clear();
        orderer.buildEdgeCompatibilityTables(testPieces);
        orderer.computePieceDifficulty(testPieces);

        Map<Integer, Integer> scores2 = orderer.getAllDifficultyScores();

        assertEquals(scores1.size(), scores2.size(), "Should rebuild same number of scores");
    }

    // ==================== Helper Methods ====================

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));   // Top-left corner
        pieces.put(2, new Piece(2, new int[]{0, 2, 3, 1}));   // Top edge
        pieces.put(3, new Piece(3, new int[]{0, 3, 4, 2}));   // Top-right corner
        pieces.put(4, new Piece(4, new int[]{2, 4, 5, 0}));   // Left edge
        pieces.put(5, new Piece(5, new int[]{3, 5, 6, 4}));   // Center
        pieces.put(6, new Piece(6, new int[]{4, 6, 7, 5}));   // Right edge
        pieces.put(7, new Piece(7, new int[]{5, 7, 0, 0}));   // Bottom-left corner
        pieces.put(8, new Piece(8, new int[]{6, 8, 0, 7}));   // Bottom edge
        pieces.put(9, new Piece(9, new int[]{7, 0, 0, 8}));   // Bottom-right corner
        return pieces;
    }
}
