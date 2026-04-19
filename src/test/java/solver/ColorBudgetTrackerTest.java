package solver;

import model.Board;
import model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ColorBudgetTracker")
class ColorBudgetTrackerTest {

    @Test
    @DisplayName("empty board + all pieces available → check is satisfied")
    void emptyBoardSatisfied() {
        Map<Integer, Piece> pieces = simplePieces();
        ColorBudgetTracker t = new ColorBudgetTracker(pieces);
        Board b = new Board(2, 2);
        BitSet used = new BitSet();
        assertTrue(t.check(b, pieces, used, pieces.size()));
    }

    @Test
    @DisplayName("maxColour reflects the highest colour across pieces")
    void maxColourComputed() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 2, 3, 0}));
        pieces.put(2, new Piece(2, new int[]{0, 5, 7, 0}));
        ColorBudgetTracker t = new ColorBudgetTracker(pieces);
        assertEquals(7, t.getMaxColour());
    }

    @Test
    @DisplayName("demand > supply on a real frontier → prune")
    void frontierExceedsSupply() {
        // Two pieces that each have a single colour-5 edge on the east side:
        // put one of them on (0,0). Its east edge forces colour 5 into the
        // (0,1) demand. With only one piece left unused that can provide
        // colour 5 on its west side, the tracker should still return true.
        // We make it fail by marking that one remaining piece as used → no
        // supply of colour 5 for (0,1).
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 5, 0, 0})); // TL piece
        pieces.put(2, new Piece(2, new int[]{0, 0, 0, 5})); // TR piece (provides 5 on west)
        pieces.put(3, new Piece(3, new int[]{0, 0, 0, 0})); // unrelated

        Board b = new Board(1, 2);
        b.place(0, 0, pieces.get(1), 0); // places piece 1 at TL, east=5 exposed to (0,1)
        BitSet used = new BitSet();
        used.set(1); // piece 1 is placed
        used.set(2); // pretend piece 2 is also used elsewhere → no supply of 5

        ColorBudgetTracker t = new ColorBudgetTracker(pieces);
        // Demand[5] = 1 (from frontier (0,1) west), supply[5] = 0 → fail
        assertFalse(t.check(b, pieces, used, pieces.size()));
    }

    @Test
    @DisplayName("demand <= supply → pass")
    void demandWithinSupply() {
        Map<Integer, Piece> pieces = new HashMap<>();
        pieces.put(1, new Piece(1, new int[]{0, 5, 0, 0}));
        pieces.put(2, new Piece(2, new int[]{0, 0, 0, 5}));

        Board b = new Board(1, 2);
        b.place(0, 0, pieces.get(1), 0);
        BitSet used = new BitSet();
        used.set(1); // only piece 1 placed

        ColorBudgetTracker t = new ColorBudgetTracker(pieces);
        // Demand[5] = 1, supply[5] = 1 (piece 2 still unused) → pass
        assertTrue(t.check(b, pieces, used, pieces.size()));
    }

    private static Map<Integer, Piece> simplePieces() {
        Map<Integer, Piece> m = new HashMap<>();
        m.put(1, new Piece(1, new int[]{0, 1, 0, 0}));
        m.put(2, new Piece(2, new int[]{0, 0, 0, 1}));
        m.put(3, new Piece(3, new int[]{0, 2, 0, 0}));
        m.put(4, new Piece(4, new int[]{0, 0, 0, 2}));
        return m;
    }
}
