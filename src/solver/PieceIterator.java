package solver;

import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Interface for iterating over piece identifiers in different orders.
 * Eliminates code duplication between ascending and descending piece iteration.
 */
public interface PieceIterator extends Iterable<Integer> {

    /**
     * Creates an iterator for the specified order.
     *
     * @param sortOrder "ascending" or "descending"
     * @param totalPieces total number of pieces
     * @param pieceUsed BitSet indicating which pieces are already used
     * @return PieceIterator instance
     */
    static PieceIterator create(String sortOrder, int totalPieces, BitSet pieceUsed) {
        if ("descending".equals(sortOrder)) {
            return new DescendingPieceIterator(totalPieces, pieceUsed);
        } else {
            return new AscendingPieceIterator(totalPieces, pieceUsed);
        }
    }

    /**
     * Ascending piece iterator (from 1 to totalPieces).
     */
    class AscendingPieceIterator implements PieceIterator {
        private final int totalPieces;
        private final BitSet pieceUsed;

        public AscendingPieceIterator(int totalPieces, BitSet pieceUsed) {
            this.totalPieces = totalPieces;
            this.pieceUsed = pieceUsed;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<Integer>() {
                private int current = 1;

                @Override
                public boolean hasNext() {
                    // Skip used pieces
                    while (current <= totalPieces && pieceUsed.get(current)) {
                        current++;
                    }
                    return current <= totalPieces;
                }

                @Override
                public Integer next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return current++;
                }
            };
        }
    }

    /**
     * Descending piece iterator (from totalPieces to 1).
     */
    class DescendingPieceIterator implements PieceIterator {
        private final int totalPieces;
        private final BitSet pieceUsed;

        public DescendingPieceIterator(int totalPieces, BitSet pieceUsed) {
            this.totalPieces = totalPieces;
            this.pieceUsed = pieceUsed;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<Integer>() {
                private int current = totalPieces;

                @Override
                public boolean hasNext() {
                    // Skip used pieces
                    while (current >= 1 && pieceUsed.get(current)) {
                        current--;
                    }
                    return current >= 1;
                }

                @Override
                public Integer next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return current--;
                }
            };
        }
    }
}
