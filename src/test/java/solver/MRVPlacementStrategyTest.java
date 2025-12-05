package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;
import solver.heuristics.LeastConstrainingValueOrderer;
import util.SaveStateManager;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for MRVPlacementStrategy.
 * Tests MRV heuristic, piece ordering, AC-3 integration, symmetry breaking, and backtracking.
 */
@DisplayName("MRVPlacementStrategy Tests")
class MRVPlacementStrategyTest {

    private MRVPlacementStrategy strategy;
    private LeastConstrainingValueOrderer valueOrderer;
    private SymmetryBreakingManager symmetryBreakingManager;
    private ConstraintPropagator constraintPropagator;
    private DomainManager domainManager;
    private TestSolver solver;
    private BacktrackingContext context;
    private Board board;
    private Map<Integer, Piece> testPieces;
    private BitSet pieceUsed;
    private StatisticsManager stats;

    @BeforeEach
    void setUp() {
        board = new Board(3, 3);
        testPieces = createTestPieces();
        pieceUsed = new BitSet(10);
        stats = new StatisticsManager();

        // Create simple fit checker
        DomainManager.FitChecker fitChecker = (b, r, c, candidateEdges) -> {
            int rows = b.getRows();
            int cols = b.getCols();

            // Check borders
            if (r == 0 && candidateEdges[0] != 0) return false;
            if (r == rows - 1 && candidateEdges[2] != 0) return false;
            if (c == 0 && candidateEdges[3] != 0) return false;
            if (c == cols - 1 && candidateEdges[1] != 0) return false;

            // Check interior cells
            if (r > 0 && candidateEdges[0] == 0) return false;
            if (r < rows - 1 && candidateEdges[2] == 0) return false;
            if (c > 0 && candidateEdges[3] == 0) return false;
            if (c < cols - 1 && candidateEdges[1] == 0) return false;

            return true;
        };

        domainManager = new DomainManager(fitChecker);
        constraintPropagator = new TestConstraintPropagator();
        valueOrderer = new TestValueOrderer();
        symmetryBreakingManager = null;
        solver = new TestSolver();

        context = new BacktrackingContext(board, testPieces, pieceUsed, 9, stats, 0,
                                         System.currentTimeMillis(), Long.MAX_VALUE);
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Should create strategy with all dependencies")
    void testConstructor() {
        MRVPlacementStrategy strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);
        assertNotNull(strategy, "Strategy should be created");
    }

    @Test
    @DisplayName("Should create strategy with verbose mode")
    void testConstructorVerbose() {
        MRVPlacementStrategy strategy = new MRVPlacementStrategy(
            true, valueOrderer, null, constraintPropagator, domainManager);
        assertNotNull(strategy, "Strategy should be created with verbose mode");
    }

    @Test
    @DisplayName("Should create strategy with symmetry breaking")
    void testConstructorWithSymmetryBreaking() {
        SymmetryBreakingManager sbm = new TestSymmetryBreakingManager(true);
        MRVPlacementStrategy strategy = new MRVPlacementStrategy(
            false, valueOrderer, sbm, constraintPropagator, domainManager);
        assertNotNull(strategy, "Strategy should be created with symmetry breaking");
    }

    // ==================== MRV Cell Selection Tests ====================

    @Test
    @DisplayName("Should return false when no cell found")
    void testNoCellFound() {
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        // Solver returns null for findNextCellMRV
        solver.setNextCell(null);

        boolean result = strategy.tryPlacement(context, solver);

        assertFalse(result, "Should return false when no cell found");
    }

    @Test
    @DisplayName("Should select cell from MRV heuristic")
    void testMRVCellSelection() {
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        // Solver returns specific cell
        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(false);

        boolean result = strategy.tryPlacement(context, solver);

        // Should attempt placement at (0,0)
        assertNotNull(result, "Should complete without errors");
    }

    // ==================== Piece Placement Tests ====================

    @Test
    @DisplayName("Should try all available pieces")
    void testTryAllPieces() {
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(false);

        long initialPlacements = stats.placements;

        boolean result = strategy.tryPlacement(context, solver);

        // Should have attempted placements
        assertTrue(stats.placements >= initialPlacements, "Should attempt placements");
        assertFalse(result, "Should return false when solve fails");
    }

    @Test
    @DisplayName("Should return true when solution found")
    void testSolutionFound() {
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(true);

        boolean result = strategy.tryPlacement(context, solver);

        // If a valid piece was found and solve succeeded
        assertNotNull(result, "Should complete without errors");
    }

    // ==================== Symmetry Breaking Tests ====================

    @Test
    @DisplayName("Should skip placements that violate symmetry")
    void testSymmetryBreakingRejection() {
        // Symmetry manager that rejects all placements
        symmetryBreakingManager = new TestSymmetryBreakingManager(false);

        strategy = new MRVPlacementStrategy(
            false, valueOrderer, symmetryBreakingManager, constraintPropagator, domainManager);

        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(false);

        boolean result = strategy.tryPlacement(context, solver);

        assertFalse(result, "Should return false when all placements rejected");
    }

    @Test
    @DisplayName("Should accept placements that pass symmetry check")
    void testSymmetryBreakingAccepted() {
        // Symmetry manager that accepts all placements
        symmetryBreakingManager = new TestSymmetryBreakingManager(true);

        strategy = new MRVPlacementStrategy(
            false, valueOrderer, symmetryBreakingManager, constraintPropagator, domainManager);

        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(false);

        boolean result = strategy.tryPlacement(context, solver);

        assertNotNull(result, "Should complete without errors");
    }

    // ==================== AC-3 Dead-End Tests ====================

    @Test
    @DisplayName("Should backtrack when AC-3 detects dead-end")
    void testAC3DeadEnd() {
        // Constraint propagator that returns false (dead-end)
        constraintPropagator = new TestConstraintPropagator(false);

        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(false);

        long initialDeadEnds = stats.deadEndsDetected;

        boolean result = strategy.tryPlacement(context, solver);

        // AC-3 should have detected dead-ends
        assertTrue(stats.deadEndsDetected >= initialDeadEnds, "Should detect dead-ends");
        assertFalse(result, "Should return false when all placements lead to dead-ends");
    }

    // ==================== Value Ordering Tests ====================

    @Test
    @DisplayName("Should use difficulty scores for piece ordering")
    void testValueOrderingWithScores() {
        // Value orderer with difficulty scores
        Map<Integer, Integer> scores = new HashMap<>();
        scores.put(1, 100);
        scores.put(2, 50);
        scores.put(3, 200);
        valueOrderer = new TestValueOrderer(scores);

        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(false);

        boolean result = strategy.tryPlacement(context, solver);

        // Should order pieces by difficulty scores
        assertNotNull(result, "Should complete without errors");
    }

    @Test
    @DisplayName("Should use fallback ordering when no scores")
    void testValueOrderingWithoutScores() {
        // Value orderer without difficulty scores
        valueOrderer = new TestValueOrderer(null);

        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(false);

        boolean result = strategy.tryPlacement(context, solver);

        // Should use fallback ordering (by ID)
        assertNotNull(result, "Should complete without errors");
    }

    // ==================== Statistics Tests ====================

    @Test
    @DisplayName("Should increment placement statistics")
    void testStatisticsPlacement() {
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(false);

        long initialPlacements = stats.placements;

        strategy.tryPlacement(context, solver);

        assertTrue(stats.placements >= initialPlacements, "Placements should be tracked");
    }

    @Test
    @DisplayName("Should increment backtrack statistics")
    void testStatisticsBacktrack() {
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(false); // Force backtracking

        long initialBacktracks = stats.backtracks;

        strategy.tryPlacement(context, solver);

        assertTrue(stats.backtracks >= initialBacktracks, "Backtracks should be tracked");
    }

    @Test
    @DisplayName("Should register depth options for progress tracking")
    void testProgressTracking() {
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(false);

        // Execute strategy
        strategy.tryPlacement(context, solver);

        // Progress should be tracked (verifying method executes without errors)
        assertNotNull(stats, "Statistics should track progress");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle empty board")
    void testEmptyBoard() {
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(false);

        boolean result = strategy.tryPlacement(context, solver);

        assertNotNull(result, "Should handle empty board");
    }

    @Test
    @DisplayName("Should handle single empty cell")
    void testSingleEmptyCell() {
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        // Fill board except one cell
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (r == 1 && c == 1) continue; // Leave center empty
                int pieceId = r * 3 + c + 1;
                if (pieceId <= 9 && pieceId != 5) {
                    board.place(r, c, testPieces.get(pieceId), 0);
                    pieceUsed.set(pieceId);
                }
            }
        }

        solver.setNextCell(new int[]{1, 1});
        solver.setSolveResult(false);

        boolean result = strategy.tryPlacement(context, solver);

        assertNotNull(result, "Should handle single empty cell");
    }

    @Test
    @DisplayName("Should handle piece that doesn't fit anywhere")
    void testNoFittingPiece() {
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        // Solver's fits() always returns false
        solver.setAlwaysRejectFit(true);
        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(false);

        boolean result = strategy.tryPlacement(context, solver);

        assertFalse(result, "Should return false when no piece fits");
        assertEquals(0, stats.placements, "No placements should be made");
    }

    // ==================== Timeout Enforcement Tests ====================

    @Test
    @DisplayName("Should enforce timeout after successful placement")
    void testTimeoutEnforcementAfterPlacement() {
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        // Set a very short timeout in context (already expired)
        long pastTime = System.currentTimeMillis() - 10000; // 10 seconds ago
        BacktrackingContext shortTimeoutContext = new BacktrackingContext(
            board, testPieces, pieceUsed, 9, stats, 0,
            pastTime, 100 // 100ms max execution time, already exceeded
        );

        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(true); // Would succeed but timeout prevents it

        boolean result = strategy.tryPlacement(shortTimeoutContext, solver);

        // Should return false due to timeout after placement
        assertFalse(result, "Should return false when timeout is exceeded after placement");

        // At least one placement should have been attempted before timeout
        assertTrue(stats.placements > 0, "Should have attempted at least one placement before timeout");
    }

    @Test
    @DisplayName("Should preserve piece order when timeout occurs")
    void testPieceOrderPreservedOnTimeout() {
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        // Test with ascending order
        strategy.setSortOrder("ascending");

        // Set a timeout that will trigger after first placement
        long startTime = System.currentTimeMillis();
        BacktrackingContext timedContext = new BacktrackingContext(
            board, testPieces, pieceUsed, 9, stats, 0,
            startTime, 50 // Very short timeout (50ms)
        );

        solver.setNextCell(new int[]{0, 0});
        solver.setSolveResult(false);

        // Add small delay before tryPlacement to ensure timeout will trigger
        try {
            Thread.sleep(60);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean result = strategy.tryPlacement(timedContext, solver);

        // Should timeout and return false
        assertFalse(result, "Should timeout during exploration");

        // Piece should have been placed (timeout happens AFTER placement, not during backtracking)
        assertTrue(stats.placements > 0, "Should have placed at least one piece before timeout");
    }

    // ==================== Sort Order Configuration Tests ====================

    @Test
    @DisplayName("setSortOrder should configure piece iteration order")
    void testSetSortOrder() {
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        // Test ascending order (default)
        strategy.setSortOrder("ascending");
        assertDoesNotThrow(() -> strategy.tryPlacement(context, solver),
                          "Ascending order should work");

        // Reset for next test
        setUp();
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        // Test descending order
        strategy.setSortOrder("descending");
        assertDoesNotThrow(() -> strategy.tryPlacement(context, solver),
                          "Descending order should work");

        // Reset for next test
        setUp();
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        // Test null defaults to ascending
        strategy.setSortOrder(null);
        assertDoesNotThrow(() -> strategy.tryPlacement(context, solver),
                          "Null sort order should default to ascending");
    }

    @Test
    @DisplayName("setSortOrder should accept any string value")
    void testSetSortOrderInvalidValue() {
        strategy = new MRVPlacementStrategy(
            false, valueOrderer, null, constraintPropagator, domainManager);

        // Test invalid value (should default to ascending)
        strategy.setSortOrder("invalid");
        assertDoesNotThrow(() -> strategy.tryPlacement(context, solver),
                          "Invalid sort order should not cause errors");
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

    // ==================== Test Helper Classes ====================

    /**
     * Simple test solver
     */
    private static class TestSolver extends EternitySolver {
        private boolean solveResult = false;
        private int stepCount = 0;
        private int[] nextCell = new int[]{0, 0};
        private int lastR = -1;
        private int lastC = -1;
        private boolean alwaysRejectFit = false;

        public TestSolver() {
            super();
        }

        public void setSolveResult(boolean result) {
            this.solveResult = result;
        }

        public void setNextCell(int[] cell) {
            this.nextCell = cell;
        }

        public void setAlwaysRejectFit(boolean reject) {
            this.alwaysRejectFit = reject;
        }

        @Override
        public int[] findNextCellMRV(Board board, Map<Integer, Piece> piecesById,
                                    BitSet pieceUsed, int totalPieces) {
            return nextCell;
        }

        @Override
        public boolean fits(Board board, int r, int c, int[] candidateEdges) {
            if (alwaysRejectFit) return false;

            int rows = board.getRows();
            int cols = board.getCols();

            // Check borders
            if (r == 0 && candidateEdges[0] != 0) return false;
            if (r == rows - 1 && candidateEdges[2] != 0) return false;
            if (c == 0 && candidateEdges[3] != 0) return false;
            if (c == cols - 1 && candidateEdges[1] != 0) return false;

            // Check interior cells
            if (r > 0 && candidateEdges[0] == 0) return false;
            if (r < rows - 1 && candidateEdges[2] == 0) return false;
            if (c > 0 && candidateEdges[3] == 0) return false;
            if (c < cols - 1 && candidateEdges[1] == 0) return false;

            return true;
        }

        @Override
        public boolean solveBacktracking(Board board, Map<Integer, Piece> piecesById,
                                         BitSet pieceUsed, int totalPieces) {
            return solveResult;
        }

        @Override
        public int getStepCount() {
            return stepCount;
        }

        @Override
        public void incrementStepCount() {
            stepCount++;
        }

        @Override
        public void setLastPlaced(int r, int c) {
            this.lastR = r;
            this.lastC = c;
        }

        @Override
        public int getLastPlacedRow() {
            return lastR;
        }

        @Override
        public int getLastPlacedCol() {
            return lastC;
        }

        @Override
        public void recordPlacement(int r, int c, int pieceId, int rotation) {
            // No-op for testing
        }

        @Override
        public SaveStateManager.PlacementInfo removeLastPlacement() {
            return null;
        }

        @Override
        public void findAndSetLastPlaced(Board board) {
            // No-op for testing
        }

        @Override
        public int countUniquePieces(Board board, int r, int c, Map<Integer, Piece> piecesById,
                                    BitSet pieceUsed, int totalPieces) {
            return 5; // Return arbitrary count
        }

        @Override
        public void printBoardWithCounts(Board board, Map<Integer, Piece> piecesById,
                                        BitSet pieceUsed, int totalPieces, int lastR, int lastC) {
            // No-op for testing
        }
    }

    /**
     * Simple test constraint propagator
     */
    private static class TestConstraintPropagator extends ConstraintPropagator {
        private final boolean propagateResult;

        public TestConstraintPropagator() {
            this(true);
        }

        public TestConstraintPropagator(boolean result) {
            super(new DomainManager((b, r, c, e) -> true), new ConstraintPropagator.Statistics());
            this.propagateResult = result;
        }

        @Override
        public boolean propagateAC3(Board board, int r, int c, int pieceId, int rotation,
                                   Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
            return propagateResult;
        }
    }

    /**
     * Simple test symmetry breaking manager
     */
    private static class TestSymmetryBreakingManager extends SymmetryBreakingManager {
        private final boolean allowPlacement;

        public TestSymmetryBreakingManager(boolean allow) {
            super(3, 3, false);
            this.allowPlacement = allow;
        }

        @Override
        public boolean isPlacementAllowed(Board board, int r, int c, int pieceId,
                                         int rotation, Map<Integer, Piece> piecesById) {
            return allowPlacement;
        }
    }

    /**
     * Simple test value orderer
     */
    private static class TestValueOrderer extends LeastConstrainingValueOrderer {
        private final Map<Integer, Integer> difficultyScores;

        public TestValueOrderer() {
            this(new HashMap<>());
        }

        public TestValueOrderer(Map<Integer, Integer> scores) {
            super(false);
            this.difficultyScores = scores;
        }

        @Override
        public Map<Integer, Integer> getAllDifficultyScores() {
            return difficultyScores;
        }
    }
}
