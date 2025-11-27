package solver;

import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Interface pour itérer sur les identifiants de pièces dans différents ordres.
 * Élimine la duplication de code entre l'itération ascendante et descendante des pièces.
 */
public interface PieceIterator extends Iterable<Integer> {

    /**
     * Crée un itérateur pour l'ordre spécifié.
     *
     * @param sortOrder "ascending" ou "descending"
     * @param totalPieces nombre total de pièces
     * @param pieceUsed BitSet indiquant quelles pièces sont déjà utilisées
     * @return instance de PieceIterator
     */
    static PieceIterator create(String sortOrder, int totalPieces, BitSet pieceUsed) {
        if ("descending".equals(sortOrder)) {
            return new DescendingPieceIterator(totalPieces, pieceUsed);
        } else {
            return new AscendingPieceIterator(totalPieces, pieceUsed);
        }
    }

    /**
     * Itérateur de pièces ascendant (de 1 à totalPieces).
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
                    // Sauter les pièces utilisées
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
     * Itérateur de pièces descendant (de totalPieces à 1).
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
                    // Sauter les pièces utilisées
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
