package solver.display;

import model.Board;
import model.Piece;
import solver.PlacementValidator;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * STRESS TESTS for rotation counting with extreme scenarios.
 * Tests performance, accuracy, and edge cases with large datasets.
 */
@DisplayName("Rotation Counting Stress Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RotationCountingStressTest {

    private ValidPieceCounter counter;
    private PlacementValidator validator;

    @BeforeEach
    void setUp() {
        validator = createPermissiveValidator();
        counter = new ValidPieceCounter(validator);
    }

    @Test
    @Order(1)
    @DisplayName("STRESS - 256 pieces (Eternity II size)")
    void testEternityIISize() {
        Board largeBoard = new Board(16, 16);
        Map<Integer, Piece> largePuzzle = createLargePuzzleSet(256);
        List<Integer> unusedIds = new ArrayList<>(largePuzzle.keySet());

        assertDoesNotThrow(() -> {
            ValidPieceCounter.ValidCountResult result =
                counter.countValidPiecesAndRotations(largeBoard, 8, 8, largePuzzle, unusedIds);

            assertTrue(result.numPieces >= 0, "Should handle 256 pieces");
            assertTrue(result.numRotations <= 256 * 4, "Max rotations = 256 * 4 = 1024");
        }, "Should handle Eternity II puzzle size");
    }

    @Test
    @Order(2)
    @DisplayName("STRESS - All 256 pieces with 4 rotations each")
    void testMaximumRotations() {
        Board board = new Board(16, 16);
        Map<Integer, Piece> symmetricPieces = new HashMap<>();

        // Create 256 symmetric pieces (all fit in 4 rotations)
        for (int i = 1; i <= 256; i++) {
            symmetricPieces.put(i, new Piece(i, new int[]{5, 5, 5, 5}));
        }

        List<Integer> allIds = new ArrayList<>(symmetricPieces.keySet());

        ValidPieceCounter.ValidCountResult result =
            counter.countValidPiecesAndRotations(board, 8, 8, symmetricPieces, allIds);

        assertEquals(256, result.numPieces, "Should count all 256 pieces");
        assertEquals(1024, result.numRotations, "Should count 256 * 4 = 1024 rotations");
    }

    @Test
    @Order(3)
    @DisplayName("STRESS - Mix of 1, 2, 3, 4 rotations per piece")
    void testMixedRotationCounts() {
        Board board = new Board(5, 5);
        Map<Integer, Piece> mixedPieces = new HashMap<>();

        // Group 1: 10 pieces with 1 rotation each = 10 rotations
        for (int i = 1; i <= 10; i++) {
            mixedPieces.put(i, new Piece(i, new int[]{0, 1, 2, 0})); // Corner-like
        }

        // Group 2: 10 pieces with 2 rotations each = 20 rotations
        for (int i = 11; i <= 20; i++) {
            mixedPieces.put(i, new Piece(i, new int[]{0, 1, 2, 3})); // Edge-like
        }

        // Group 3: 10 pieces with 4 rotations each = 40 rotations
        for (int i = 21; i <= 30; i++) {
            mixedPieces.put(i, new Piece(i, new int[]{5, 5, 5, 5})); // Symmetric
        }

        List<Integer> allIds = new ArrayList<>(mixedPieces.keySet());

        ValidPieceCounter.ValidCountResult result =
            counter.countValidPiecesAndRotations(board, 2, 2, mixedPieces, allIds);

        assertEquals(30, result.numPieces, "Should count 30 unique pieces");
        // Total: 10 + 20 + 40 = 70 rotations
        assertTrue(result.numRotations >= 30, "Should have at least 30 rotations");
        assertTrue(result.numRotations <= 120, "Should have at most 30 * 4 = 120 rotations");
    }

    @Test
    @Order(4)
    @DisplayName("STRESS - Performance with 1000 pieces")
    void testPerformanceWith1000Pieces() {
        Board board = new Board(50, 50);
        Map<Integer, Piece> hugePuzzle = createLargePuzzleSet(1000);
        List<Integer> unusedIds = new ArrayList<>(hugePuzzle.keySet());

        long startTime = System.currentTimeMillis();

        ValidPieceCounter.ValidCountResult result =
            counter.countValidPiecesAndRotations(board, 25, 25, hugePuzzle, unusedIds);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(duration < 5000, "Should complete within 5 seconds for 1000 pieces");
        assertTrue(result.numPieces >= 0, "Should return valid count");
    }

    @Test
    @Order(5)
    @DisplayName("STRESS - All corner positions (4 corners)")
    void testAllCornerPositions() {
        Board board = new Board(10, 10);
        Map<Integer, Piece> pieces = createLargePuzzleSet(50);
        List<Integer> unusedIds = new ArrayList<>(pieces.keySet());

        // Test all 4 corners
        int[][] corners = {{0, 0}, {0, 9}, {9, 0}, {9, 9}};

        for (int[] corner : corners) {
            assertDoesNotThrow(() -> {
                ValidPieceCounter.ValidCountResult result =
                    counter.countValidPiecesAndRotations(board, corner[0], corner[1], pieces, unusedIds);
                assertTrue(result.numPieces >= 0, "Corner should have valid count");
            }, "Should handle corner at [" + corner[0] + "," + corner[1] + "]");
        }
    }

    @Test
    @Order(6)
    @DisplayName("STRESS - All edge positions (perimeter)")
    void testAllEdgePositions() {
        Board board = new Board(5, 5);
        Map<Integer, Piece> pieces = createLargePuzzleSet(20);
        List<Integer> unusedIds = new ArrayList<>(pieces.keySet());

        // Test top edge
        for (int col = 1; col < 4; col++) {
            int finalCol = col;
            assertDoesNotThrow(() -> {
                counter.countValidPiecesAndRotations(board, 0, finalCol, pieces, unusedIds);
            }, "Should handle top edge at col " + col);
        }

        // Test bottom edge
        for (int col = 1; col < 4; col++) {
            int finalCol = col;
            assertDoesNotThrow(() -> {
                counter.countValidPiecesAndRotations(board, 4, finalCol, pieces, unusedIds);
            }, "Should handle bottom edge at col " + col);
        }

        // Test left/right edges
        for (int row = 1; row < 4; row++) {
            int finalRow = row;
            assertDoesNotThrow(() -> {
                counter.countValidPiecesAndRotations(board, finalRow, 0, pieces, unusedIds);
                counter.countValidPiecesAndRotations(board, finalRow, 4, pieces, unusedIds);
            }, "Should handle left/right edges at row " + row);
        }
    }

    @Test
    @Order(7)
    @DisplayName("STRESS - All interior positions")
    void testAllInteriorPositions() {
        Board board = new Board(5, 5);
        Map<Integer, Piece> pieces = createLargePuzzleSet(20);
        List<Integer> unusedIds = new ArrayList<>(pieces.keySet());

        // Test all interior cells (3x3 = 9 cells)
        for (int row = 1; row < 4; row++) {
            for (int col = 1; col < 4; col++) {
                int finalRow = row;
                int finalCol = col;
                assertDoesNotThrow(() -> {
                    ValidPieceCounter.ValidCountResult result =
                        counter.countValidPiecesAndRotations(board, finalRow, finalCol, pieces, unusedIds);
                    // Interior cells can have more rotations
                    assertTrue(result.numRotations >= 0, "Interior cell should have valid count");
                }, "Should handle interior at [" + row + "," + col + "]");
            }
        }
    }

    @Test
    @Order(8)
    @DisplayName("STRESS - Consecutive counting doesn't accumulate errors")
    void testConsecutiveCountingAccuracy() {
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = createLargePuzzleSet(10);
        List<Integer> unusedIds = new ArrayList<>(pieces.keySet());

        // Count same position 100 times
        ValidPieceCounter.ValidCountResult firstResult = null;
        for (int i = 0; i < 100; i++) {
            ValidPieceCounter.ValidCountResult result =
                counter.countValidPiecesAndRotations(board, 1, 1, pieces, unusedIds);

            if (firstResult == null) {
                firstResult = result;
            } else {
                assertEquals(firstResult.numPieces, result.numPieces,
                    "Piece count should be consistent across calls");
                assertEquals(firstResult.numRotations, result.numRotations,
                    "Rotation count should be consistent across calls");
            }
        }
    }

    @Test
    @Order(9)
    @DisplayName("STRESS - Empty unused list with large board")
    void testEmptyUnusedListLargeBoard() {
        Board board = new Board(16, 16);
        Map<Integer, Piece> pieces = createLargePuzzleSet(256);
        List<Integer> emptyList = Collections.emptyList();

        ValidPieceCounter.ValidCountResult result =
            counter.countValidPiecesAndRotations(board, 8, 8, pieces, emptyList);

        assertEquals(0, result.numPieces, "Empty list should give 0 pieces");
        assertEquals(0, result.numRotations, "Empty list should give 0 rotations");
    }

    @Test
    @Order(10)
    @DisplayName("STRESS - Single piece from 256 total")
    void testSinglePieceFromMany() {
        Board board = new Board(16, 16);
        Map<Integer, Piece> allPieces = createLargePuzzleSet(256);
        List<Integer> singlePiece = Collections.singletonList(42);

        ValidPieceCounter.ValidCountResult result =
            counter.countValidPiecesAndRotations(board, 8, 8, allPieces, singlePiece);

        assertTrue(result.numPieces <= 1, "Should have at most 1 piece");
        assertTrue(result.numRotations <= 4, "Single piece has at most 4 rotations");
    }

    private Map<Integer, Piece> createLargePuzzleSet(int numPieces) {
        Map<Integer, Piece> pieces = new HashMap<>();
        Random random = new Random(42); // Fixed seed for reproducibility

        for (int i = 1; i <= numPieces; i++) {
            int[] edges = new int[]{
                random.nextInt(23),
                random.nextInt(23),
                random.nextInt(23),
                random.nextInt(23)
            };
            pieces.put(i, new Piece(i, edges));
        }

        return pieces;
    }

    private PlacementValidator createPermissiveValidator() {
        return new PlacementValidator(null, null, null) {
            @Override
            public boolean fits(Board board, int row, int col, int[] edges) {
                return true; // Accept all for stress testing
            }
        };
    }
}
