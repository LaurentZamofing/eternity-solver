package solver.display;

import model.Board;
import model.Piece;
import model.Placement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BoardDisplayService - Board visualization and save file generation.
 */
@DisplayName("BoardDisplayService Tests")
public class BoardDisplayServiceTest {

    /**
     * Regression test for bug where fixed pieces displayed as "???" in save files.
     *
     * The bug occurred when pieces were placed on the board but not available
     * in the allPieces map passed to writeToSaveFileDetailed(). This happened
     * when pieces were fixed at startup but the map only contained unused pieces.
     *
     * The fix: Use edges directly from Placement.edges instead of looking up
     * the Piece from the map and recalculating edges.
     */
    @Test
    @DisplayName("Fixed pieces should display correctly even when not in allPieces map")
    void testFixedPiecesDisplayWithoutPieceMap() {
        // Create a simple 2x2 board with one piece placed
        Board board = new Board(2, 2);

        // Place piece 42 at (0, 0) with rotation 2
        // Original edges: [1, 2, 3, 4] (N, E, S, W)
        // After rotation 2 (180°): [3, 4, 1, 2]
        int[] originalEdges = {1, 2, 3, 4};
        Piece piece42 = new Piece(42, originalEdges);
        board.place(0, 0, piece42, 2);

        // Create an empty pieces map (simulating the bug scenario)
        // In the original bug, fixed pieces were not in this map
        Map<Integer, Piece> emptyPiecesMap = new HashMap<>();

        // Write to save file
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        // This should NOT throw NPE or display "???"
        BoardDisplayService.writeToSaveFileDetailed(writer, board, emptyPiecesMap, null);
        writer.flush();

        String output = stringWriter.toString();

        // Verify output contains the edges from the Placement (after rotation)
        // Rotated edges: [3, 4, 1, 2] (N, E, S, W)
        assertTrue(output.contains(" 3"), "Should contain north edge value 3");
        assertTrue(output.contains(" 4"), "Should contain east edge value 4");
        assertTrue(output.contains(" 1"), "Should contain south edge value 1");
        assertTrue(output.contains(" 2"), "Should contain west edge value 2");
        assertTrue(output.contains("42"), "Should contain piece ID 42");

        // Verify output does NOT contain the bug markers
        assertFalse(output.contains("???"), "Should NOT contain ??? markers");
        assertFalse(output.contains("??"), "Should NOT contain ?? markers");
    }

    @Test
    @DisplayName("Empty board should display candidate counts correctly")
    void testEmptyBoardDisplay() {
        Board board = new Board(2, 2);

        // Create pieces map with 3 pieces
        Map<Integer, Piece> piecesMap = new HashMap<>();
        piecesMap.put(1, new Piece(1, new int[]{0, 1, 2, 3}));
        piecesMap.put(2, new Piece(2, new int[]{1, 2, 3, 4}));
        piecesMap.put(3, new Piece(3, new int[]{2, 3, 4, 5}));

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        BoardDisplayService.writeToSaveFileDetailed(writer, board, piecesMap, null);
        writer.flush();

        String output = stringWriter.toString();

        // The output shows counts but NOT in [] format - it shows like [  3] for 3 candidates
        // Check that output doesn't contain ??? or ?? which would indicate errors
        assertFalse(output.contains("???"), "Should NOT contain ??? error markers");
        assertFalse(output.contains("? ??"), "Should NOT contain broken formatting");
    }

    @Test
    @DisplayName("Mixed board with placed and empty cells should display correctly")
    void testMixedBoardDisplay() {
        Board board = new Board(2, 2);

        // Place one piece
        int[] edges = {5, 10, 15, 20};
        Piece piece99 = new Piece(99, edges);
        board.place(0, 0, piece99, 0);

        // Create pieces map
        Map<Integer, Piece> piecesMap = new HashMap<>();
        piecesMap.put(99, piece99);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        BoardDisplayService.writeToSaveFileDetailed(writer, board, piecesMap, null);
        writer.flush();

        String output = stringWriter.toString();

        // Verify placed piece shows edges
        assertTrue(output.contains("5"), "Should contain north edge 5");
        assertTrue(output.contains("99"), "Should contain piece ID 99");

        // Verify no error markers
        assertFalse(output.contains("???"), "Should NOT contain ??? error markers");
        assertFalse(output.contains("??"), "Should NOT contain ?? error markers");
    }

    @Test
    @DisplayName("Piece with missing piece map entry should display correctly")
    void testPieceDisplayWithPartialPieceMap() {
        Board board = new Board(2, 2);

        // Place two pieces
        Piece piece1 = new Piece(1, new int[]{5, 10, 15, 20});
        Piece piece2 = new Piece(2, new int[]{6, 11, 16, 21});
        board.place(0, 0, piece1, 0);
        board.place(0, 1, piece2, 1);  // rotation 1

        // Create pieces map with only piece1 (piece2 is missing - simulates the bug)
        Map<Integer, Piece> partialPiecesMap = new HashMap<>();
        partialPiecesMap.put(1, piece1);
        // Piece 2 is NOT in the map

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        BoardDisplayService.writeToSaveFileDetailed(writer, board, partialPiecesMap, null);
        writer.flush();

        String output = stringWriter.toString();

        // Verify both pieces display correctly
        assertTrue(output.contains(" 5"), "Should contain piece1 north edge");
        assertTrue(output.contains(" 1 "), "Should contain piece1 ID");

        // Piece 2 should also display correctly even though it's not in the map
        assertTrue(output.contains(" 2 "), "Should contain piece2 ID");
        // After rotation 1 (90°): [21, 5, 11, 16] (original [6, 11, 16, 21])
        assertTrue(output.contains("21"), "Should contain piece2 rotated north edge");

        // Verify NO bug markers
        assertFalse(output.contains("???"), "Should NOT contain ??? markers");
        assertFalse(output.contains("??"), "Should NOT contain ?? markers");
    }
}
