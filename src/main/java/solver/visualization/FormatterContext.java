package solver.visualization;

import model.Board;
import model.Piece;
import solver.BoardVisualizer.FitsChecker;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context object containing all data needed for board formatting.
 *
 * This class encapsulates the parameters required by board formatters,
 * reducing the number of parameters passed to formatting methods and
 * making the code more maintainable.
 *
 * Usage:
 * <pre>
 * FormatterContext ctx = FormatterContext.builder()
 *     .board(board)
 *     .pieces(piecesById)
 *     .fitsChecker(fitsChecker)
 *     .build();
 *
 * formatter.format(ctx);
 * </pre>
 */
public class FormatterContext {

    // ===== Required Fields =====

    /** The board to visualize. */
    private final Board board;

    /** Map of piece ID to Piece object. */
    private final Map<Integer, Piece> pieces;

    /** Functional interface for checking if a piece fits at a position. */
    private final FitsChecker fitsChecker;

    // ===== Optional Fields (for specific formatters) =====

    /** BitSet marking which pieces have been used (for CompactFormatter). */
    private final BitSet pieceUsed;

    /** Total number of pieces in the puzzle. */
    private final int totalPieces;

    /** Row coordinate of last placed piece (for highlighting in DetailedFormatter). */
    private final int lastPlacedRow;

    /** Column coordinate of last placed piece (for highlighting in DetailedFormatter). */
    private final int lastPlacedCol;

    /** List of unused piece IDs (for LabeledFormatter and ComparisonFormatter). */
    private final List<Integer> unusedIds;

    /** Set of fixed positions in format "row,col" (for LabeledFormatter). */
    private final Set<String> fixedPositions;

    /** Reference board for comparison (for ComparisonFormatter). */
    private final Board referenceBoard;

    // ===== Private Constructor (use Builder) =====

    private FormatterContext(Builder builder) {
        this.board = builder.board;
        this.pieces = builder.pieces;
        this.fitsChecker = builder.fitsChecker;
        this.pieceUsed = builder.pieceUsed;
        this.totalPieces = builder.totalPieces;
        this.lastPlacedRow = builder.lastPlacedRow;
        this.lastPlacedCol = builder.lastPlacedCol;
        this.unusedIds = builder.unusedIds;
        this.fixedPositions = builder.fixedPositions;
        this.referenceBoard = builder.referenceBoard;
    }

    // ===== Getters =====

    public Board getBoard() {
        return board;
    }

    public Map<Integer, Piece> getPieces() {
        return pieces;
    }

    public FitsChecker getFitsChecker() {
        return fitsChecker;
    }

    public BitSet getPieceUsed() {
        return pieceUsed;
    }

    public int getTotalPieces() {
        return totalPieces;
    }

    public int getLastPlacedRow() {
        return lastPlacedRow;
    }

    public int getLastPlacedCol() {
        return lastPlacedCol;
    }

    public List<Integer> getUnusedIds() {
        return unusedIds;
    }

    public Set<String> getFixedPositions() {
        return fixedPositions;
    }

    public Board getReferenceBoard() {
        return referenceBoard;
    }

    // ===== Builder =====

    /**
     * Creates a new builder instance.
     *
     * @return New FormatterContext builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for FormatterContext.
     * Provides fluent API for constructing context objects.
     */
    public static class Builder {
        // Required
        private Board board;
        private Map<Integer, Piece> pieces;
        private FitsChecker fitsChecker;

        // Optional (defaults)
        private BitSet pieceUsed;
        private int totalPieces = 0;
        private int lastPlacedRow = -1;
        private int lastPlacedCol = -1;
        private List<Integer> unusedIds;
        private Set<String> fixedPositions;
        private Board referenceBoard;

        private Builder() {
        }

        /**
         * Sets the board (required).
         *
         * @param board The board to visualize
         * @return This builder
         */
        public Builder board(Board board) {
            this.board = board;
            return this;
        }

        /**
         * Sets the pieces map (required).
         *
         * @param pieces Map of piece ID to Piece object
         * @return This builder
         */
        public Builder pieces(Map<Integer, Piece> pieces) {
            this.pieces = pieces;
            return this;
        }

        /**
         * Sets the fits checker (required).
         *
         * @param fitsChecker Functional interface for checking piece placement
         * @return This builder
         */
        public Builder fitsChecker(FitsChecker fitsChecker) {
            this.fitsChecker = fitsChecker;
            return this;
        }

        /**
         * Sets the piece usage BitSet (optional).
         *
         * @param pieceUsed BitSet marking used pieces
         * @return This builder
         */
        public Builder pieceUsed(BitSet pieceUsed) {
            this.pieceUsed = pieceUsed;
            return this;
        }

        /**
         * Sets the total number of pieces (optional).
         *
         * @param totalPieces Total piece count
         * @return This builder
         */
        public Builder totalPieces(int totalPieces) {
            this.totalPieces = totalPieces;
            return this;
        }

        /**
         * Sets the last placed cell coordinates (optional).
         *
         * @param row Row index
         * @param col Column index
         * @return This builder
         */
        public Builder lastPlaced(int row, int col) {
            this.lastPlacedRow = row;
            this.lastPlacedCol = col;
            return this;
        }

        /**
         * Sets the list of unused piece IDs (optional).
         *
         * @param unusedIds List of unused piece IDs
         * @return This builder
         */
        public Builder unusedIds(List<Integer> unusedIds) {
            this.unusedIds = unusedIds;
            return this;
        }

        /**
         * Sets the fixed positions set (optional).
         *
         * @param fixedPositions Set of fixed positions ("row,col" format)
         * @return This builder
         */
        public Builder fixedPositions(Set<String> fixedPositions) {
            this.fixedPositions = fixedPositions;
            return this;
        }

        /**
         * Sets the reference board for comparison (optional).
         *
         * @param referenceBoard Reference board to compare against
         * @return This builder
         */
        public Builder referenceBoard(Board referenceBoard) {
            this.referenceBoard = referenceBoard;
            return this;
        }

        /**
         * Builds the FormatterContext instance.
         *
         * @return New FormatterContext with configured parameters
         * @throws IllegalStateException if required fields are null
         */
        public FormatterContext build() {
            if (board == null) {
                throw new IllegalStateException("Board is required");
            }
            if (pieces == null) {
                throw new IllegalStateException("Pieces map is required");
            }
            if (fitsChecker == null) {
                throw new IllegalStateException("FitsChecker is required");
            }

            return new FormatterContext(this);
        }
    }
}
