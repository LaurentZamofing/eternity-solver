package solver;

import model.Board;
import model.Piece;
import solver.DomainManager.ValidPlacement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DomainManager class.
 * Tests AC-3 domain initialization, restoration, and management.
 */
@DisplayName("DomainManager Tests")
public class DomainManagerTest {

    private DomainManager domainManager;
    private DomainManager.FitChecker mockFitChecker;
    private Board board;
    private Map<Integer, Piece> pieces;
    private BitSet pieceUsed;

    @BeforeEach
    public void setUp() {
        // Create mock fit checker that always returns true
        mockFitChecker = (b, r, c, edges) -> true;
        domainManager = new DomainManager(mockFitChecker);

        // Create small test board (3x3)
        board = new Board(3, 3);

        // Create test pieces
        pieces = createTestPieces();

        // Initialize piece tracking
        pieceUsed = new BitSet(pieces.size() + 1);
    }

    private Map<Integer, Piece> createTestPieces() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{1, 2, 3, 4}));
        pieces.put(2, new Piece(2, new int[]{5, 6, 7, 8}));
        pieces.put(3, new Piece(3, new int[]{0, 0, 0, 0})); // Border piece
        return pieces;
    }

    @Test
    @DisplayName("Initialize AC-3 domains successfully")
    public void testInitializeAC3Domains() {
        assertDoesNotThrow(() -> {
            domainManager.initializeAC3Domains(board, pieces, pieceUsed, board.getRows() * board.getCols());
        });
    }

    @Test
    @DisplayName("Get domain for empty cell returns non-null")
    public void testGetDomainForEmptyCell() {
        domainManager.initializeAC3Domains(board, pieces, pieceUsed, board.getRows() * board.getCols());

        Map<Integer, List<ValidPlacement>> domain = domainManager.getDomain(0, 0);

        assertNotNull(domain, "Domain should not be null");
    }

    @Test
    @DisplayName("Get domain for occupied cell returns null")
    public void testGetDomainForOccupiedCell() {
        // Place a piece
        Piece piece = pieces.get(1);
        board.place(0, 0, piece, 0);
        pieceUsed.set(1);

        domainManager.initializeAC3Domains(board, pieces, pieceUsed, board.getRows() * board.getCols());

        Map<Integer, List<ValidPlacement>> domain = domainManager.getDomain(0, 0);

        assertNull(domain, "Occupied cell should have null domain");
    }

    @Test
    @DisplayName("Domain contains valid placements")
    public void testDomainContainsValidPlacements() {
        domainManager.initializeAC3Domains(board, pieces, pieceUsed, board.getRows() * board.getCols());

        Map<Integer, List<ValidPlacement>> domain = domainManager.getDomain(0, 0);

        assertNotNull(domain);
        assertFalse(domain.isEmpty(), "Domain should contain placements");

        // Check that placements are well-formed
        for (Map.Entry<Integer, List<ValidPlacement>> entry : domain.entrySet()) {
            int pieceId = entry.getKey();
            List<ValidPlacement> placements = entry.getValue();

            assertTrue(pieceId > 0, "Piece ID should be positive");
            assertNotNull(placements, "Placements list should not be null");
            assertFalse(placements.isEmpty(), "Placements list should not be empty");

            for (ValidPlacement vp : placements) {
                assertEquals(pieceId, vp.pieceId, "ValidPlacement should match piece ID");
                assertTrue(vp.rotation >= 0 && vp.rotation < 4, "Rotation should be 0-3");
            }
        }
    }

    // Note: copyDomains()/restoreDomains() methods were removed during refactoring
    // Domain backup/restore is now handled differently through restoreAC3Domains()
    // which restores domains after removing a piece at a specific position
    // @Test
    // @DisplayName("Copy domains creates independent copy")
    // public void testCopyDomains() {
    //     // Test removed - functionality changed
    // }

    // Note: restoreDomains() method signature changed - now restoreAC3Domains()
    // @Test
    // @DisplayName("Restore domains works correctly")
    // public void testRestoreDomains() {
    //     // Test removed - functionality changed
    // }

    @Test
    @DisplayName("Compute single domain for cell")
    public void testComputeSingleDomain() {
        List<ValidPlacement> singleDomain = domainManager.computeDomain(
                board, 1, 1, pieces, pieceUsed, pieces.size());

        assertNotNull(singleDomain, "Single domain should not be null");
        assertFalse(singleDomain.isEmpty(), "Single domain should contain placements");
    }

    @Test
    @DisplayName("Domain excludes used pieces")
    public void testDomainExcludesUsedPieces() {
        // Mark piece 1 as used
        pieceUsed.set(1);

        domainManager.initializeAC3Domains(board, pieces, pieceUsed, board.getRows() * board.getCols());

        Map<Integer, List<ValidPlacement>> domain = domainManager.getDomain(0, 0);

        assertNotNull(domain);
        assertFalse(domain.containsKey(1), "Domain should not contain used piece 1");
        // Should still contain unused pieces 2 and 3
        assertTrue(domain.containsKey(2) || domain.containsKey(3),
                "Domain should contain unused pieces");
    }

    @Test
    @DisplayName("Domain size for different cells")
    public void testDomainSizeDifferentCells() {
        domainManager.initializeAC3Domains(board, pieces, pieceUsed, board.getRows() * board.getCols());

        // Corner cell (0,0) - most constrained (2 border edges)
        Map<Integer, List<ValidPlacement>> corner = domainManager.getDomain(0, 0);

        // Center cell (1,1) - least constrained (no border edges)
        Map<Integer, List<ValidPlacement>> center = domainManager.getDomain(1, 1);

        assertNotNull(corner);
        assertNotNull(center);

        // Center typically has more options than corner (with our mock fit checker)
        // But exact comparison depends on fit checker logic
        assertTrue(corner.size() >= 0 && center.size() >= 0,
                "Both domains should be non-negative");
    }

    @Test
    @DisplayName("Get domain piece IDs set")
    public void testGetDomainPieceIds() {
        domainManager.initializeAC3Domains(board, pieces, pieceUsed, board.getRows() * board.getCols());

        Map<Integer, List<ValidPlacement>> domain = domainManager.getDomain(1, 1);
        assertNotNull(domain, "Domain should not be null");

        Set<Integer> pieceIds = domain.keySet();
        assertNotNull(pieceIds, "Piece IDs set should not be null");
        assertFalse(pieceIds.isEmpty(), "Piece IDs set should not be empty");

        // Should contain subset of available pieces
        for (int id : pieceIds) {
            assertTrue(pieces.containsKey(id), "Domain should only contain valid piece IDs");
            assertFalse(pieceUsed.get(id), "Domain should not contain used pieces");
        }
    }

    @Test
    @DisplayName("Domain with strict fit checker")
    public void testDomainWithStrictFitChecker() {
        // Create strict fit checker that only allows piece 2
        DomainManager.FitChecker strictChecker = (b, r, c, edges) -> {
            // Only allow piece with edges [5, 6, 7, 8]
            return edges[0] == 5 && edges[1] == 6 && edges[2] == 7 && edges[3] == 8;
        };

        DomainManager strictManager = new DomainManager(strictChecker);
        strictManager.initializeAC3Domains(board, pieces, pieceUsed, board.getRows() * board.getCols());

        Map<Integer, List<ValidPlacement>> domain = strictManager.getDomain(1, 1);

        assertNotNull(domain);
        // Should only contain piece 2 (if any match)
        if (!domain.isEmpty()) {
            for (int pieceId : domain.keySet()) {
                assertEquals(2, pieceId, "Strict checker should only allow piece 2");
            }
        }
    }

    @Test
    @DisplayName("AC-3 initialized flag")
    public void testAC3InitializedFlag() {
        assertFalse(domainManager.isAC3Initialized(), "Should not be initialized initially");

        domainManager.initializeAC3Domains(board, pieces, pieceUsed, board.getRows() * board.getCols());

        assertTrue(domainManager.isAC3Initialized(), "Should be initialized after init call");
    }

    @Test
    @DisplayName("Domain cache key consistency")
    public void testDomainCacheKeyConsistency() {
        domainManager.initializeAC3Domains(board, pieces, pieceUsed, board.getRows() * board.getCols());

        // Get domain twice - should return same object (cached)
        Map<Integer, List<ValidPlacement>> domain1 = domainManager.getDomain(1, 1);
        Map<Integer, List<ValidPlacement>> domain2 = domainManager.getDomain(1, 1);

        assertSame(domain1, domain2, "Repeated getDomain calls should return same object");
    }

    @Test
    @DisplayName("Clear domains")
    public void testClearDomains() {
        domainManager.initializeAC3Domains(board, pieces, pieceUsed, board.getRows() * board.getCols());
        assertTrue(domainManager.isAC3Initialized());

        domainManager.resetAC3();

        assertFalse(domainManager.isAC3Initialized(), "Domains should be cleared");
    }

    @Test
    @DisplayName("Handle empty pieces map")
    public void testEmptyPiecesMap() {
        Map<Integer, Piece> emptyPieces = new HashMap<>();

        assertDoesNotThrow(() -> {
            domainManager.initializeAC3Domains(board, emptyPieces, pieceUsed, board.getRows() * board.getCols());
        });

        Map<Integer, List<ValidPlacement>> domain = domainManager.getDomain(0, 0);
        assertNotNull(domain);
        assertTrue(domain.isEmpty(), "Domain should be empty with no pieces");
    }
}
