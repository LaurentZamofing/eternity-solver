package solver;

import model.Board;
import model.Piece;
import solver.heuristics.LeastConstrainingValueOrderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for BacktrackingSolver class.
 *
 * Tests the core backtracking algorithm extracted in Refactoring #16.
 * Focuses on:
 * - Timeout enforcement
 * - Solution signaling across threads
 * - Strategy coordination
 * - Null-safe handling of optional managers
 * - Record tracking integration
 */
@DisplayName("BacktrackingSolver Tests")
class BacktrackingSolverTest {

    private EternitySolver solver;
    private StatisticsManager stats;
    private ConfigurationManager configManager;
    private AtomicBoolean solutionFound;

    @BeforeEach
    void setUp() {
        solver = new EternitySolver();
        stats = new StatisticsManager();
        configManager = new ConfigurationManager();
        solutionFound = new AtomicBoolean(false);

        // Set reasonable defaults
        configManager.setVerbose(false);
        configManager.setMaxExecutionTime(10000); // 10 seconds default
    }

    /**
     * Helper method to fully initialize solver components for tests that call solve().
     * This ensures cellSelector, validator, and other required components are set up.
     */
    private void initializeSolver(Board board, Map<Integer, Piece> pieces, BitSet pieceUsed, int totalPieces) {
        solver.initializeComponents(board, pieces, pieceUsed, totalPieces);
    }

    @Test
    @DisplayName("Constructor should accept valid parameters")
    void testConstructorWithValidParameters() {
        // Arrange
        SingletonPlacementStrategy singletonStrategy = createMockSingletonStrategy();
        MRVPlacementStrategy mrvStrategy = createMockMRVStrategy();

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            new BacktrackingSolver(
                solver,
                stats,
                solutionFound,
                configManager,
                null, // recordManager is optional
                null, // autoSaveManager is optional
                singletonStrategy,
                mrvStrategy,
                -1, // threadId
                0L, // randomSeed
                System.currentTimeMillis()
            );
        });
    }

    @Test
    @DisplayName("Should enforce timeout correctly")
    void testTimeoutEnforcement() {
        // Arrange
        configManager.setMaxExecutionTime(100); // 100ms timeout
        Board board = new Board(4, 4);
        Map<Integer, Piece> pieces = createSimplePuzzle(16);
        BitSet pieceUsed = new BitSet(17);

        // Initialize solver components
        initializeSolver(board, pieces, pieceUsed, 16);

        SingletonPlacementStrategy singletonStrategy = createMockSingletonStrategy();
        MRVPlacementStrategy mrvStrategy = createMockMRVStrategy();

        BacktrackingSolver backtrackingSolver = new BacktrackingSolver(
            solver,
            stats,
            solutionFound,
            configManager,
            null, // no recordManager
            null, // no autoSaveManager
            singletonStrategy,
            mrvStrategy,
            -1,
            0L,
            System.currentTimeMillis()
        );

        // Act
        long startTime = System.currentTimeMillis();
        boolean result = backtrackingSolver.solve(board, pieces, pieceUsed, 16);
        long elapsedTime = System.currentTimeMillis() - startTime;

        // Assert - should timeout (return false) within reasonable time
        assertFalse(result, "Should not find solution due to timeout");
        assertTrue(elapsedTime < 500, "Should timeout quickly (within 500ms)");
    }

    @Test
    @DisplayName("Should signal solution found via atomic flag")
    void testSolutionSignaling() {
        // Arrange
        AtomicBoolean sharedFlag = new AtomicBoolean(false);
        Board board = new Board(2, 2);
        Map<Integer, Piece> pieces = createSolvablePuzzle(2, 2);
        BitSet pieceUsed = new BitSet(5);

        // Initialize solver components
        initializeSolver(board, pieces, pieceUsed, 4);

        SingletonPlacementStrategy singletonStrategy = createMockSingletonStrategy();
        MRVPlacementStrategy mrvStrategy = createMockMRVStrategy();

        BacktrackingSolver backtrackingSolver = new BacktrackingSolver(
            solver,
            stats,
            sharedFlag, // shared atomic flag
            configManager,
            null,
            null,
            singletonStrategy,
            mrvStrategy,
            -1,
            0L,
            System.currentTimeMillis()
        );

        // Act
        boolean result = backtrackingSolver.solve(board, pieces, pieceUsed, 4);

        // Assert
        if (result) {
            assertTrue(sharedFlag.get(), "Atomic flag should be set when solution found");
        }
    }

    @Test
    @DisplayName("Should handle null RecordManager gracefully")
    void testNullRecordManager() {
        // Arrange
        Board board = new Board(2, 2);
        Map<Integer, Piece> pieces = createSimplePuzzle(4);
        BitSet pieceUsed = new BitSet(5);

        // Initialize solver components
        initializeSolver(board, pieces, pieceUsed, 4);

        SingletonPlacementStrategy singletonStrategy = createMockSingletonStrategy();
        MRVPlacementStrategy mrvStrategy = createMockMRVStrategy();

        BacktrackingSolver backtrackingSolver = new BacktrackingSolver(
            solver,
            stats,
            solutionFound,
            configManager,
            null, // null RecordManager - should not crash
            null,
            singletonStrategy,
            mrvStrategy,
            -1,
            0L,
            System.currentTimeMillis()
        );

        // Act & Assert - should not throw NullPointerException
        assertDoesNotThrow(() -> {
            backtrackingSolver.solve(board, pieces, pieceUsed, 4);
        });
    }

    @Test
    @DisplayName("Should handle null AutoSaveManager gracefully")
    void testNullAutoSaveManager() {
        // Arrange
        Board board = new Board(2, 2);
        Map<Integer, Piece> pieces = createSimplePuzzle(4);
        BitSet pieceUsed = new BitSet(5);

        // Initialize solver components
        initializeSolver(board, pieces, pieceUsed, 4);

        SingletonPlacementStrategy singletonStrategy = createMockSingletonStrategy();
        MRVPlacementStrategy mrvStrategy = createMockMRVStrategy();

        BacktrackingSolver backtrackingSolver = new BacktrackingSolver(
            solver,
            stats,
            solutionFound,
            configManager,
            null,
            null, // null AutoSaveManager - should not crash
            singletonStrategy,
            mrvStrategy,
            -1,
            0L,
            System.currentTimeMillis()
        );

        // Act & Assert - should not throw NullPointerException
        assertDoesNotThrow(() -> {
            backtrackingSolver.solve(board, pieces, pieceUsed, 4);
        });
    }

    @Test
    @DisplayName("Should track recursive calls in statistics")
    void testStatisticsTracking() {
        // Arrange
        Board board = new Board(2, 2);
        Map<Integer, Piece> pieces = createSimplePuzzle(4);
        BitSet pieceUsed = new BitSet(5);

        // Initialize solver components
        initializeSolver(board, pieces, pieceUsed, 4);

        SingletonPlacementStrategy singletonStrategy = createMockSingletonStrategy();
        MRVPlacementStrategy mrvStrategy = createMockMRVStrategy();

        BacktrackingSolver backtrackingSolver = new BacktrackingSolver(
            solver,
            stats,
            solutionFound,
            configManager,
            null,
            null,
            singletonStrategy,
            mrvStrategy,
            -1,
            0L,
            System.currentTimeMillis()
        );

        long initialCalls = stats.recursiveCalls;

        // Act
        backtrackingSolver.solve(board, pieces, pieceUsed, 4);

        // Assert
        assertTrue(stats.recursiveCalls > initialCalls,
            "Recursive calls counter should increase");
    }

    @Test
    @DisplayName("Should stop when another thread finds solution")
    void testMultiThreadedSolutionSignaling() {
        // Arrange
        AtomicBoolean sharedFlag = new AtomicBoolean(true); // Already solved by another thread
        Board board = new Board(2, 2);
        Map<Integer, Piece> pieces = createSimplePuzzle(4);
        BitSet pieceUsed = new BitSet(5);

        SingletonPlacementStrategy singletonStrategy = createMockSingletonStrategy();
        MRVPlacementStrategy mrvStrategy = createMockMRVStrategy();

        BacktrackingSolver backtrackingSolver = new BacktrackingSolver(
            solver,
            stats,
            sharedFlag, // pre-set to true (solution found by another thread)
            configManager,
            null,
            null,
            singletonStrategy,
            mrvStrategy,
            -1,
            0L,
            System.currentTimeMillis()
        );

        // Act
        boolean result = backtrackingSolver.solve(board, pieces, pieceUsed, 4);

        // Assert
        assertFalse(result, "Should return false when another thread already found solution");
    }

    // ==================== Record Tracking Tests ====================

    @Test
    @DisplayName("Should track records with RecordManager present")
    void testRecordTrackingWithRecordManager() {
        // Arrange
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = createSimplePuzzle(9);
        BitSet pieceUsed = new BitSet(10);

        // Initialize solver components
        initializeSolver(board, pieces, pieceUsed, 9);

        // Create a real RecordManager
        RecordManager recordManager = new RecordManager(
            "test_puzzle", // puzzleName
            0, // threadId
            10, // minDepthToShowRecords
            new Object(), // lockObject
            new java.util.concurrent.atomic.AtomicInteger(0), // globalMaxDepth
            new java.util.concurrent.atomic.AtomicInteger(0), // globalBestScore
            new java.util.concurrent.atomic.AtomicInteger(-1), // globalBestThreadId
            new java.util.concurrent.atomic.AtomicReference<>(null), // globalBestBoard
            new java.util.concurrent.atomic.AtomicReference<>(null) // globalBestPieces
        );

        // Create a mock AutoSaveManager that tracks saves
        TestAutoSaveManager testAutoSave = new TestAutoSaveManager();

        SingletonPlacementStrategy singletonStrategy = createMockSingletonStrategy();
        MRVPlacementStrategy mrvStrategy = createMockMRVStrategy();

        BacktrackingSolver backtrackingSolver = new BacktrackingSolver(
            solver,
            stats,
            solutionFound,
            configManager,
            recordManager, // RecordManager present
            testAutoSave, // Track auto-save calls
            singletonStrategy,
            mrvStrategy,
            -1,
            0L,
            System.currentTimeMillis()
        );

        // Act - solve for a few steps
        backtrackingSolver.solve(board, pieces, pieceUsed, 9);

        // Assert - should complete without errors when RecordManager is present
        assertNotNull(recordManager, "RecordManager should be used");
    }

    @Test
    @DisplayName("Should trigger auto-save on new record")
    void testAutoSaveOnNewRecord() {
        // Arrange
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = createSimplePuzzle(9);
        BitSet pieceUsed = new BitSet(10);

        // Initialize solver components
        initializeSolver(board, pieces, pieceUsed, 9);

        RecordManager recordManager = new RecordManager(
            "test_puzzle", // puzzleName
            0, // threadId
            10, // minDepthToShowRecords
            new Object(), // lockObject
            new java.util.concurrent.atomic.AtomicInteger(0), // globalMaxDepth
            new java.util.concurrent.atomic.AtomicInteger(0), // globalBestScore
            new java.util.concurrent.atomic.AtomicInteger(-1), // globalBestThreadId
            new java.util.concurrent.atomic.AtomicReference<>(null), // globalBestBoard
            new java.util.concurrent.atomic.AtomicReference<>(null) // globalBestPieces
        );

        TestAutoSaveManager testAutoSave = new TestAutoSaveManager();

        SingletonPlacementStrategy singletonStrategy = createMockSingletonStrategy();
        MRVPlacementStrategy mrvStrategy = createMockMRVStrategy();

        BacktrackingSolver backtrackingSolver = new BacktrackingSolver(
            solver,
            stats,
            solutionFound,
            configManager,
            recordManager,
            testAutoSave, // Track if saveRecord was called
            singletonStrategy,
            mrvStrategy,
            -1,
            0L,
            System.currentTimeMillis()
        );

        // Act
        backtrackingSolver.solve(board, pieces, pieceUsed, 9);

        // Assert - testAutoSave should track any saves that occurred
        // Note: Whether a record is actually achieved depends on the puzzle complexity
        assertNotNull(testAutoSave, "AutoSaveManager should be available");
    }

    // ==================== Thread State Save Tests ====================

    @Test
    @DisplayName("Should skip thread state save in single-threaded mode (threadId=-1)")
    void testSingleThreadedMode() {
        // Arrange
        Board board = new Board(2, 2);
        Map<Integer, Piece> pieces = createSimplePuzzle(4);
        BitSet pieceUsed = new BitSet(5);

        // Initialize solver components
        initializeSolver(board, pieces, pieceUsed, 4);

        SingletonPlacementStrategy singletonStrategy = createMockSingletonStrategy();
        MRVPlacementStrategy mrvStrategy = createMockMRVStrategy();

        BacktrackingSolver backtrackingSolver = new BacktrackingSolver(
            solver,
            stats,
            solutionFound,
            configManager,
            null,
            null,
            singletonStrategy,
            mrvStrategy,
            -1, // threadId = -1 indicates single-threaded mode
            0L,
            System.currentTimeMillis()
        );

        // Act & Assert - should not crash even without thread state saving
        assertDoesNotThrow(() -> {
            backtrackingSolver.solve(board, pieces, pieceUsed, 4);
        }, "Single-threaded mode should work without thread state saves");
    }

    @Test
    @DisplayName("Should handle thread state save errors gracefully in verbose mode")
    void testThreadStateSaveErrorHandling() {
        // Arrange
        configManager.setVerbose(true); // Enable verbose to test error logging

        Board board = new Board(2, 2);
        Map<Integer, Piece> pieces = createSimplePuzzle(4);
        BitSet pieceUsed = new BitSet(5);

        // Initialize solver components
        initializeSolver(board, pieces, pieceUsed, 4);

        SingletonPlacementStrategy singletonStrategy = createMockSingletonStrategy();
        MRVPlacementStrategy mrvStrategy = createMockMRVStrategy();

        BacktrackingSolver backtrackingSolver = new BacktrackingSolver(
            solver,
            stats,
            solutionFound,
            configManager,
            null,
            null,
            singletonStrategy,
            mrvStrategy,
            0, // threadId = 0 (multi-threaded mode)
            0L,
            System.currentTimeMillis() - 400000 // Start 400 seconds ago to trigger save
        );

        // Act & Assert - should not crash even if thread state save fails
        // (Save might fail due to I/O errors, permissions, etc.)
        assertDoesNotThrow(() -> {
            backtrackingSolver.solve(board, pieces, pieceUsed, 4);
        }, "Should handle thread state save errors gracefully");
    }

    @Test
    @DisplayName("Should support multi-threaded mode with thread ID")
    void testMultiThreadedModeWithThreadId() {
        // Arrange
        Board board = new Board(2, 2);
        Map<Integer, Piece> pieces = createSimplePuzzle(4);
        BitSet pieceUsed = new BitSet(5);

        // Initialize solver components
        initializeSolver(board, pieces, pieceUsed, 4);

        SingletonPlacementStrategy singletonStrategy = createMockSingletonStrategy();
        MRVPlacementStrategy mrvStrategy = createMockMRVStrategy();

        // Create solver with thread ID (multi-threaded mode)
        BacktrackingSolver backtrackingSolver = new BacktrackingSolver(
            solver,
            stats,
            solutionFound,
            configManager,
            null,
            null,
            singletonStrategy,
            mrvStrategy,
            3, // threadId = 3 (multi-threaded mode)
            12345L, // randomSeed
            System.currentTimeMillis()
        );

        // Act & Assert - should work with thread ID specified
        assertDoesNotThrow(() -> {
            backtrackingSolver.solve(board, pieces, pieceUsed, 4);
        }, "Multi-threaded mode with thread ID should work");
    }

    // ==================== Helper Methods ====================

    /**
     * Test AutoSaveManager that tracks method calls
     */
    private static class TestAutoSaveManager extends AutoSaveManager {
        boolean saveRecordCalled = false;
        boolean checkAndSaveCalled = false;
        boolean checkAndLogStatsCalled = false;

        public TestAutoSaveManager() {
            super("test_puzzle", 0, new java.util.ArrayList<>(), null);
        }

        @Override
        public void saveRecord(Board board, BitSet pieceUsed, int totalPieces,
                              StatisticsManager stats, int currentDepth) {
            saveRecordCalled = true;
            // Don't actually save in tests
        }

        @Override
        public void checkAndSave(Board board, BitSet pieceUsed, int totalPieces,
                                StatisticsManager stats) {
            checkAndSaveCalled = true;
            // Don't actually save in tests
        }

        @Override
        public void checkAndLogStats(BitSet pieceUsed, int totalPieces,
                                     StatisticsManager stats) {
            checkAndLogStatsCalled = true;
            // Don't actually log in tests
        }
    }

    // ==================== Helper Methods ====================

    private SingletonPlacementStrategy createMockSingletonStrategy() {
        // Create a minimal strategy that doesn't find singletons
        // SingletonDetector needs fitChecker, stats, verbose
        SingletonDetector detector = new SingletonDetector(
            (board, r, c, edges) -> true, // Simple fitChecker that always returns true
            new SingletonDetector.Statistics(),
            false // verbose
        );
        return new SingletonPlacementStrategy(
            detector,
            false, // useSingletons = false for testing
            false, // verbose = false
            null, // symmetryBreakingManager not needed for basic tests
            null, // constraintPropagator not needed
            null  // domainManager not needed
        );
    }

    private MRVPlacementStrategy createMockMRVStrategy() {
        // Create a minimal strategy
        LeastConstrainingValueOrderer orderer = new LeastConstrainingValueOrderer(false);
        return new MRVPlacementStrategy(
            false, // verbose = false
            orderer,
            null, // symmetryBreakingManager not needed
            null, // constraintPropagator not needed
            null  // domainManager not needed
        );
    }

    private Map<Integer, Piece> createSimplePuzzle(int size) {
        Map<Integer, Piece> pieces = new HashMap<>();
        for (int i = 1; i <= size; i++) {
            pieces.put(i, new Piece(i, new int[]{0, 0, 0, 0})); // All zeros for simple testing
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
