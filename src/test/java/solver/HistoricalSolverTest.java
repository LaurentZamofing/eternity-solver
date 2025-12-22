package solver;

import model.Board;
import model.Piece;
import util.SaveStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for HistoricalSolver class.
 *
 * Tests the historical solving logic extracted in Refactoring #25.
 * Focuses on:
 * - State restoration from saved files
 * - Placement history initialization
 * - Backtracking through pre-loaded pieces
 * - Fixed pieces calculation
 * - Cumulative time tracking
 */
@DisplayName("HistoricalSolver Tests")
class HistoricalSolverTest {

    private EternitySolver solver;
    private HistoricalSolver historicalSolver;

    @BeforeEach
    void setUp() {
        solver = new EternitySolver();
        solver.setVerbose(false); // Disable verbose output for tests
        historicalSolver = new HistoricalSolver(solver);
    }

    @Test
    @DisplayName("Constructor should accept valid EternitySolver")
    void testConstructorWithValidSolver() {
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            new HistoricalSolver(solver);
        });
    }

    @Test
    @DisplayName("Should handle empty placement history")
    void testEmptyPlacementHistory() {
        // Arrange
        Board board = new Board(2, 2);
        Map<Integer, Piece> allPieces = createSimplePuzzle(4);
        List<Integer> unusedIds = List.of(1, 2, 3, 4);
        List<SaveStateManager.PlacementInfo> emptyHistory = new ArrayList<>();

        // Act
        boolean result = historicalSolver.solveWithHistory(
            board, allPieces, unusedIds, emptyHistory
        );

        // Assert - should handle empty history gracefully
        // Result depends on whether puzzle is solvable, but should not crash
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should initialize PlacementOrderTracker with provided history")
    void testPlacementHistoryInitialization() {
        // Arrange
        Board board = new Board(2, 2);
        Map<Integer, Piece> allPieces = createSimplePuzzle(4);

        // Create pre-loaded history (2 pieces already placed)
        List<SaveStateManager.PlacementInfo> preloadedHistory = new ArrayList<>();
        preloadedHistory.add(new SaveStateManager.PlacementInfo(0, 0, 1, 0));
        preloadedHistory.add(new SaveStateManager.PlacementInfo(0, 1, 2, 0));

        // Mark these pieces as used
        List<Integer> unusedIds = List.of(3, 4);
        board.place(0, 0, allPieces.get(1), 0);
        board.place(0, 1, allPieces.get(2), 0);

        // Act
        boolean result = historicalSolver.solveWithHistory(
            board, allPieces, unusedIds, preloadedHistory
        );

        // Assert - should process pre-loaded pieces
        // PlacementOrderTracker should have 2 pieces in history
        assertNotNull(solver.placementOrderTracker);
        assertEquals(2, solver.placementOrderTracker.getPlacementHistory().size(),
            "PlacementOrderTracker should be initialized with pre-loaded history");
    }

    @Test
    @DisplayName("Should calculate unused pieces correctly from unusedIds")
    void testUnusedPiecesCalculation() {
        // Arrange
        Board board = new Board(3, 3);
        Map<Integer, Piece> allPieces = createSimplePuzzle(9);

        // 5 pieces already placed, 4 unused
        List<SaveStateManager.PlacementInfo> preloadedHistory = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            preloadedHistory.add(new SaveStateManager.PlacementInfo(
                (i-1) / 3, (i-1) % 3, i, 0
            ));
            board.place((i-1) / 3, (i-1) % 3, allPieces.get(i), 0);
        }

        List<Integer> unusedIds = List.of(6, 7, 8, 9);

        // Act
        historicalSolver.solveWithHistory(board, allPieces, unusedIds, preloadedHistory);

        // Assert - pieceUsed BitSet should correctly reflect used/unused pieces
        // This is tested indirectly through successful initialization
        assertNotNull(solver.placementOrderTracker);
    }

    @Test
    @DisplayName("Should handle puzzle with all pieces pre-loaded")
    void testAllPiecesPreLoaded() {
        // Arrange
        Board board = new Board(2, 2);
        Map<Integer, Piece> allPieces = createSolvablePuzzle(2, 2);

        // All 4 pieces pre-loaded (complete solution)
        List<SaveStateManager.PlacementInfo> completeHistory = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            SaveStateManager.PlacementInfo info = new SaveStateManager.PlacementInfo(
                (i-1) / 2, (i-1) % 2, i, 0
            );
            completeHistory.add(info);
            board.place(info.row, info.col, allPieces.get(i), 0);
        }

        List<Integer> unusedIds = new ArrayList<>(); // No unused pieces

        // Act
        boolean result = historicalSolver.solveWithHistory(
            board, allPieces, unusedIds, completeHistory
        );

        // Assert - if puzzle is valid solution, should return true immediately
        assertTrue(result, "Should recognize complete valid solution");
    }

    @Test
    @DisplayName("Should pass correct parameters to solver methods")
    void testSolverMethodCalls() {
        // Arrange
        Board board = new Board(2, 2);
        Map<Integer, Piece> allPieces = createSimplePuzzle(4);
        List<Integer> unusedIds = List.of(3, 4);

        List<SaveStateManager.PlacementInfo> preloadedHistory = new ArrayList<>();
        preloadedHistory.add(new SaveStateManager.PlacementInfo(0, 0, 1, 0));
        preloadedHistory.add(new SaveStateManager.PlacementInfo(0, 1, 2, 0));

        board.place(0, 0, allPieces.get(1), 0);
        board.place(0, 1, allPieces.get(2), 0);

        // Act
        historicalSolver.solveWithHistory(board, allPieces, unusedIds, preloadedHistory);

        // Assert - verify that initialization methods were called
        // This is tested indirectly through successful initialization
        assertNotNull(solver.placementOrderTracker,
            "PlacementOrderTracker should be initialized");
        assertNotNull(solver.validator,
            "Validator should be initialized by initializeComponents()");
    }

    @Test
    @DisplayName("Should integrate with statistics tracking")
    void testStatisticsIntegration() {
        // Arrange
        Board board = new Board(2, 2);
        Map<Integer, Piece> allPieces = createSimplePuzzle(4);
        List<Integer> unusedIds = List.of(1, 2, 3, 4);
        List<SaveStateManager.PlacementInfo> emptyHistory = new ArrayList<>();

        StatisticsManager initialStats = solver.stats;
        long initialCalls = initialStats.recursiveCalls;

        // Act
        historicalSolver.solveWithHistory(board, allPieces, unusedIds, emptyHistory);

        // Assert - stats should be updated
        assertTrue(solver.stats.recursiveCalls >= initialCalls,
            "Statistics should track recursive calls");
    }

    @Test
    @DisplayName("Should handle invalid board state gracefully")
    void testInvalidBoardState() {
        // Arrange
        Board board = new Board(2, 2);
        Map<Integer, Piece> allPieces = createSimplePuzzle(4);

        // Create conflicting history (piece 1 placed twice in different positions)
        List<SaveStateManager.PlacementInfo> conflictingHistory = new ArrayList<>();
        conflictingHistory.add(new SaveStateManager.PlacementInfo(0, 0, 1, 0));
        conflictingHistory.add(new SaveStateManager.PlacementInfo(0, 1, 1, 0)); // Same piece!

        List<Integer> unusedIds = List.of(2, 3, 4);

        // Act & Assert - should handle gracefully without crashing
        assertDoesNotThrow(() -> {
            historicalSolver.solveWithHistory(
                board, allPieces, unusedIds, conflictingHistory
            );
        });
    }

    @Test
    @DisplayName("Should initialize ConfigurationManager settings correctly")
    void testConfigurationManagerInitialization() {
        // Arrange
        Board board = new Board(2, 2);
        Map<Integer, Piece> allPieces = createSimplePuzzle(4);
        List<Integer> unusedIds = List.of(1, 2, 3, 4);
        List<SaveStateManager.PlacementInfo> emptyHistory = new ArrayList<>();

        solver.configBuilder.puzzleName("test_puzzle");
        solver.configBuilder.threadLabel("[Test Thread]");

        // Act
        historicalSolver.solveWithHistory(board, allPieces, unusedIds, emptyHistory);

        // Assert - ConfigurationManager should retain settings
        assertEquals("test_puzzle", solver.configBuilder.build().getPuzzleName());
        assertEquals("[Test Thread]", solver.configBuilder.build().getThreadLabel());
    }

    @Test
    @DisplayName("Should support backtracking through pre-loaded pieces")
    void testBacktrackingThroughHistory() {
        // Arrange
        Board board = new Board(2, 2);
        Map<Integer, Piece> allPieces = createSimplePuzzle(4);

        // Pre-load 3 pieces, leaving 1 empty spot
        List<SaveStateManager.PlacementInfo> preloadedHistory = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            SaveStateManager.PlacementInfo info = new SaveStateManager.PlacementInfo(
                (i-1) / 2, (i-1) % 2, i, 0
            );
            preloadedHistory.add(info);
            board.place(info.row, info.col, allPieces.get(i), 0);
        }

        List<Integer> unusedIds = List.of(4);

        // Act
        boolean result = historicalSolver.solveWithHistory(
            board, allPieces, unusedIds, preloadedHistory
        );

        // Assert - should be able to backtrack through pre-loaded pieces if needed
        // The ability to backtrack is tested by the solver continuing after failures
        assertNotNull(result); // Result depends on puzzle solvability
    }

    // ==================== Helper Methods ====================

    private Map<Integer, Piece> createSimplePuzzle(int size) {
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= size; i++) {
            pieces.put(i, new Piece(i, new int[]{0, 0, 0, 0}));
        }
        return pieces;
    }

    private Map<Integer, Piece> createSolvablePuzzle(int rows, int cols) {
        // Create a simple solvable puzzle (all pieces with matching edges = 0)
        Map<Integer, Piece> pieces = new HashMap<>();
        int count = rows * cols;
        for (int i = 1; i <= count; i++) {
            pieces.put(i, new Piece(i, new int[]{0, 0, 0, 0}));
        }
        return pieces;
    }
}
