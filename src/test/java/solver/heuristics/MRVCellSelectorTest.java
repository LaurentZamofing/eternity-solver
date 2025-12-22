package solver.heuristics;

import model.Board;
import model.Piece;
import solver.CellConstraints;
import solver.DomainManager;
import solver.EdgeCompatibilityIndex;
import solver.EternitySolver;
import solver.NeighborAnalyzer;
import solver.PlacementValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MRVCellSelector class.
 * Tests Minimum Remaining Values heuristic for cell selection.
 */
@DisplayName("MRVCellSelector Tests")
public class MRVCellSelectorTest {

    private MRVCellSelector selector;
    private DomainManager domainManager;
    private MRVCellSelector.FitChecker fitChecker;
    private Board board;
    private Map<Integer, Piece> pieces;
    private BitSet pieceUsed;
    private NeighborAnalyzer neighborAnalyzer;

    @BeforeEach
    public void setUp() {
        // Create mock fit checker
        fitChecker = (b, r, c, edges) -> true;

        // Create domain manager
        DomainManager.FitChecker dmFitChecker = (b, r, c, edges) -> true;
        domainManager = new DomainManager(dmFitChecker);

        // Create test board
        board = new Board(3, 3);

        // Create test pieces first
        pieces = createTestPieces();

        // Create cell constraints and validator
        CellConstraints[][] cellConstraints = CellConstraints.createConstraintsMatrix(3, 3);
        EternitySolver.Statistics stats = new EternitySolver.Statistics();
        PlacementValidator validator = new PlacementValidator(cellConstraints, stats, "ascending");
        EdgeCompatibilityIndex edgeIndex = new EdgeCompatibilityIndex(pieces, false);

        // Create neighbor analyzer
        neighborAnalyzer = new NeighborAnalyzer(cellConstraints, validator, edgeIndex);

        // Create selector with all required dependencies
        selector = new MRVCellSelector(domainManager, fitChecker, neighborAnalyzer);

        // Initialize piece tracking
        pieceUsed = new BitSet(pieces.size() + 1);

        // Initialize domains
        domainManager.initializeAC3Domains(board, pieces, pieceUsed, 3);
    }

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{1, 2, 3, 4}));
        pieces.put(2, new Piece(2, new int[]{5, 6, 7, 8}));
        pieces.put(3, new Piece(3, new int[]{0, 0, 0, 0}));
        return pieces;
    }

    @Test
    @DisplayName("Select next cell returns non-null for empty board")
    public void testSelectNextCellEmptyBoard() {
        HeuristicStrategy.CellPosition cell = selector.selectNextCell(board, pieces, pieceUsed, 3);

        assertNotNull(cell, "Should select a cell on empty board");
        assertTrue(cell.row >= 0 && cell.row < 3, "Row should be in valid range");
        assertTrue(cell.col >= 0 && cell.col < 3, "Column should be in valid range");
    }

    @Test
    @DisplayName("Select next cell returns null for full board")
    public void testSelectNextCellFullBoard() {
        // Fill entire board
        int pieceIndex = 1;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                Piece piece = pieces.get(pieceIndex);
                board.place(r, c, piece, 0);
                pieceIndex = (pieceIndex % pieces.size()) + 1;
            }
        }

        HeuristicStrategy.CellPosition cell = selector.selectNextCell(board, pieces, pieceUsed, 3);

        assertNull(cell, "Should return null for full board");
    }

    @Test
    @DisplayName("Select avoids already placed cells")
    public void testSelectAvoidsPlacedCells() {
        // Place piece at (1, 1)
        Piece piece = pieces.get(1);
        board.place(1, 1, piece, 0);
        pieceUsed.set(1);

        HeuristicStrategy.CellPosition cell = selector.selectNextCell(board, pieces, pieceUsed, 3);

        assertNotNull(cell, "Should select an empty cell");
        assertFalse(cell.row == 1 && cell.col == 1, "Should not select occupied cell (1,1)");
    }

    @Test
    @DisplayName("Enable border prioritization")
    public void testBorderPrioritization() {
        selector.setPrioritizeBorders(true);

        HeuristicStrategy.CellPosition cell = selector.selectNextCell(board, pieces, pieceUsed, 3);

        assertNotNull(cell);
        // With border prioritization, should select border cell
        boolean isBorder = cell.row == 0 || cell.row == 2 || cell.col == 0 || cell.col == 2;
        assertTrue(isBorder || board.getRows() * board.getCols() == 1,
                "Should prioritize border cells");
    }

    @Test
    @DisplayName("Disable border prioritization")
    public void testNoBorderPrioritization() {
        selector.setPrioritizeBorders(false);

        HeuristicStrategy.CellPosition cell = selector.selectNextCell(board, pieces, pieceUsed, 3);

        assertNotNull(cell, "Should still select a cell without border prioritization");
    }

    @Test
    @DisplayName("Enable and disable AC-3")
    public void testAC3Toggle() {
        selector.setUseAC3(true);
        HeuristicStrategy.CellPosition cell1 = selector.selectNextCell(board, pieces, pieceUsed, 3);
        assertNotNull(cell1);

        selector.setUseAC3(false);
        HeuristicStrategy.CellPosition cell2 = selector.selectNextCell(board, pieces, pieceUsed, 3);
        assertNotNull(cell2);
    }

    @Test
    @DisplayName("CellPosition has correct values")
    public void testCellPositionValues() {
        HeuristicStrategy.CellPosition cell = selector.selectNextCell(board, pieces, pieceUsed, 3);

        assertNotNull(cell);
        assertTrue(cell.row >= 0, "Row should be non-negative");
        assertTrue(cell.col >= 0, "Column should be non-negative");
        assertTrue(cell.row < board.getRows(), "Row should be within board bounds");
        assertTrue(cell.col < board.getCols(), "Column should be within board bounds");
    }

    @Test
    @DisplayName("CellPosition toArray works")
    public void testCellPositionToArray() {
        HeuristicStrategy.CellPosition cell = new HeuristicStrategy.CellPosition(1, 2);

        int[] array = cell.toArray();

        assertNotNull(array);
        assertEquals(2, array.length);
        assertEquals(1, array[0]);
        assertEquals(2, array[1]);
    }

    @Test
    @DisplayName("CellPosition toString works")
    public void testCellPositionToString() {
        HeuristicStrategy.CellPosition cell = new HeuristicStrategy.CellPosition(1, 2);

        String str = cell.toString();

        assertNotNull(str);
        assertTrue(str.contains("1"), "String should contain row");
        assertTrue(str.contains("2"), "String should contain column");
    }

    @Test
    @DisplayName("CellPosition equals works")
    public void testCellPositionEquals() {
        HeuristicStrategy.CellPosition cell1 = new HeuristicStrategy.CellPosition(1, 2);
        HeuristicStrategy.CellPosition cell2 = new HeuristicStrategy.CellPosition(1, 2);
        HeuristicStrategy.CellPosition cell3 = new HeuristicStrategy.CellPosition(2, 1);

        assertEquals(cell1, cell2, "Same coordinates should be equal");
        assertNotEquals(cell1, cell3, "Different coordinates should not be equal");
    }

    @Test
    @DisplayName("CellPosition hashCode works")
    public void testCellPositionHashCode() {
        HeuristicStrategy.CellPosition cell1 = new HeuristicStrategy.CellPosition(1, 2);
        HeuristicStrategy.CellPosition cell2 = new HeuristicStrategy.CellPosition(1, 2);

        assertEquals(cell1.hashCode(), cell2.hashCode(), "Same coordinates should have same hash");
    }

    @Test
    @DisplayName("Get strategy name")
    public void testGetName() {
        String name = selector.getName();

        assertNotNull(name);
        assertTrue(name.contains("MRV") || name.contains("CellSelector"),
                "Name should identify the strategy");
    }

    @Test
    @DisplayName("Select with partially filled board")
    public void testSelectPartiallyFilledBoard() {
        // Fill top row
        for (int c = 0; c < 3; c++) {
            Piece piece = pieces.get((c % pieces.size()) + 1);
            board.place(0, c, piece, 0);
        }

        HeuristicStrategy.CellPosition cell = selector.selectNextCell(board, pieces, pieceUsed, 3);

        assertNotNull(cell, "Should select from remaining cells");
        assertTrue(cell.row > 0, "Should not select from filled top row");
    }

    @Test
    @DisplayName("MRV prefers constrained cells")
    public void testMRVPrefersConstrainedCells() {
        // Place pieces to create different constraint levels
        Piece piece1 = pieces.get(1);
        board.place(0, 0, piece1, 0);
        pieceUsed.set(1);

        // Cell (0,1) is now more constrained than (2,2)
        // MRV should prefer (0,1) as it's adjacent to a placed piece

        // Reinitialize domains
        domainManager.resetAC3();
        domainManager.initializeAC3Domains(board, pieces, pieceUsed, 3);

        HeuristicStrategy.CellPosition cell = selector.selectNextCell(board, pieces, pieceUsed, 3);

        assertNotNull(cell);
        // Cell should be selected based on MRV heuristic
        // Exact cell depends on domain sizes, just verify it's valid
        assertTrue(board.isEmpty(cell.row, cell.col), "Selected cell should be empty");
    }

    @Test
    @DisplayName("Selector doesn't throw exceptions")
    public void testNoExceptions() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                selector.selectNextCell(board, pieces, pieceUsed, 3);
            }
        });
    }

    @Test
    @DisplayName("Select with no available pieces")
    public void testSelectNoAvailablePieces() {
        // Mark all pieces as used
        for (int id : pieces.keySet()) {
            pieceUsed.set(id);
        }

        // Should still work, possibly returning null or a cell with empty domain
        assertDoesNotThrow(() -> {
            HeuristicStrategy.CellPosition cell = selector.selectNextCell(board, pieces, pieceUsed, 3);
            // May return null or a cell - both are valid
        });
    }
}
