package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.*;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for DomainManager.
 * Tests AC-3 domain initialization, domain computation, and backtracking support.
 */
@DisplayName("DomainManager Tests")
class DomainManagerTest {

    private DomainManager domainManager;
    private DomainManager.FitChecker fitChecker;
    private Board testBoard;
    private Map<Integer, Piece> testPieces;
    private BitSet pieceUsed;

    @BeforeEach
    void setUp() {
        testBoard = new Board(3, 3);
        testPieces = createTestPieces();
        pieceUsed = new BitSet(10);

        // Create fit checker that validates border constraints
        fitChecker = (board, r, c, candidateEdges) -> {
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
        };

        domainManager = new DomainManager(fitChecker);
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Should create DomainManager with fit checker")
    void testConstructor() {
        DomainManager manager = new DomainManager(fitChecker);
        assertNotNull(manager, "Manager should be created");
        assertFalse(manager.isAC3Initialized(), "Should not be initialized");
    }

    // ==================== AC-3 Initialization Tests ====================

    @Test
    @DisplayName("Should not be initialized initially")
    void testNotInitiallyInitialized() {
        assertFalse(domainManager.isAC3Initialized(),
                   "Should not be initialized initially");
    }

    @Test
    @DisplayName("Should initialize AC-3 domains")
    void testInitializeAC3Domains() {
        domainManager.initializeAC3Domains(testBoard, testPieces, pieceUsed, 9);

        assertTrue(domainManager.isAC3Initialized(),
                  "Should be initialized after initializeAC3Domains");
    }

    @Test
    @DisplayName("Should initialize domains for all empty cells")
    void testInitializeDomainsAllCells() {
        domainManager.initializeAC3Domains(testBoard, testPieces, pieceUsed, 9);

        // Check that domains exist for all cells
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                Map<Integer, List<DomainManager.ValidPlacement>> domain =
                    domainManager.getDomain(r, c);
                assertNotNull(domain, "Domain should exist for cell (" + r + ", " + c + ")");
            }
        }
    }

    // ==================== Domain Computation Tests ====================

    @Test
    @DisplayName("Should compute domain for corner cell")
    void testComputeDomainCorner() {
        List<DomainManager.ValidPlacement> domain =
            domainManager.computeDomain(testBoard, 0, 0, testPieces, pieceUsed, 9);

        assertNotNull(domain, "Domain should be computed");
        assertFalse(domain.isEmpty(), "Corner should have valid placements");
    }

    @Test
    @DisplayName("Should exclude used pieces from domain")
    void testComputeDomainExcludeUsedPieces() {
        // Mark pieces 1-5 as used
        for (int i = 1; i <= 5; i++) {
            pieceUsed.set(i);
        }

        List<DomainManager.ValidPlacement> domain =
            domainManager.computeDomain(testBoard, 0, 0, testPieces, pieceUsed, 9);

        assertNotNull(domain, "Domain should be computed");

        // Verify no used pieces in domain
        for (DomainManager.ValidPlacement vp : domain) {
            assertFalse(pieceUsed.get(vp.pieceId),
                       "Used piece " + vp.pieceId + " should not be in domain");
        }
    }

    // ==================== Domain Restoration Tests ====================

    @Test
    @DisplayName("Should restore AC-3 domains after backtracking")
    void testRestoreAC3Domains() {
        domainManager.initializeAC3Domains(testBoard, testPieces, pieceUsed, 9);

        // Place a piece
        testBoard.place(0, 0, testPieces.get(1), 0);
        pieceUsed.set(1);

        // Remove piece (backtrack)
        testBoard.remove(0, 0);
        pieceUsed.clear(1);

        // Restore domains
        domainManager.restoreAC3Domains(testBoard, 0, 0, testPieces, pieceUsed, 9);

        // Domain should be restored
        Map<Integer, List<DomainManager.ValidPlacement>> domain = domainManager.getDomain(0, 0);
        assertNotNull(domain, "Domain should be restored");
        assertFalse(domain.isEmpty(), "Restored domain should have placements");
    }

    // ==================== ValidPlacement Tests ====================

    @Test
    @DisplayName("Should create ValidPlacement")
    void testValidPlacementCreation() {
        DomainManager.ValidPlacement vp = new DomainManager.ValidPlacement(5, 2);

        assertEquals(5, vp.pieceId, "Piece ID should match");
        assertEquals(2, vp.rotation, "Rotation should match");
    }

    // ==================== Helper Methods ====================

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 1, 2, 0}));
        pieces.put(2, new Piece(2, new int[]{0, 2, 3, 1}));
        pieces.put(3, new Piece(3, new int[]{0, 3, 4, 2}));
        pieces.put(4, new Piece(4, new int[]{2, 4, 5, 0}));
        pieces.put(5, new Piece(5, new int[]{3, 5, 6, 4}));
        pieces.put(6, new Piece(6, new int[]{4, 6, 7, 5}));
        pieces.put(7, new Piece(7, new int[]{5, 7, 0, 0}));
        pieces.put(8, new Piece(8, new int[]{6, 8, 0, 7}));
        pieces.put(9, new Piece(9, new int[]{7, 0, 0, 8}));
        return pieces;
    }
}
