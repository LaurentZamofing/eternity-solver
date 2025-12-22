package solver.strategy;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import solver.SolverConfiguration;
import solver.SharedSearchState;

import java.util.BitSet;
import java.util.Map;

/**
 * Encapsulates all context needed for solving a puzzle.
 * Avoids passing 10+ parameters to constructors.
 *
 * Use Builder pattern for flexible construction:
 * <pre>
 * SolverContext context = SolverContext.builder()
 *     .board(board)
 *     .pieces(pieces)
 *     .solver(solver)
 *     .configuration(config)
 *     .build();
 * </pre>
 */
public class SolverContext {

    private final Board board;
    private final Map<Integer, Piece> pieces;
    private final BitSet pieceUsed;
    private final EternitySolver solver;
    private final SolverConfiguration configuration;
    private final SharedSearchState sharedState;
    private final int totalPieces;
    private final long startTimeMs;

    private SolverContext(Builder builder) {
        this.board = builder.board;
        this.pieces = builder.pieces;
        this.pieceUsed = builder.pieceUsed;
        this.solver = builder.solver;
        this.configuration = builder.configuration;
        this.sharedState = builder.sharedState;
        this.totalPieces = builder.totalPieces;
        this.startTimeMs = builder.startTimeMs;
    }

    public Board getBoard() {
        return board;
    }

    public Map<Integer, Piece> getPieces() {
        return pieces;
    }

    public BitSet getPieceUsed() {
        return pieceUsed;
    }

    public EternitySolver getSolver() {
        return solver;
    }

    public SolverConfiguration getConfiguration() {
        return configuration;
    }

    public SharedSearchState getSharedState() {
        return sharedState;
    }

    public int getTotalPieces() {
        return totalPieces;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Board board;
        private Map<Integer, Piece> pieces;
        private BitSet pieceUsed;
        private EternitySolver solver;
        private SolverConfiguration configuration;
        private SharedSearchState sharedState;
        private int totalPieces;
        private long startTimeMs;

        public Builder board(Board board) {
            this.board = board;
            return this;
        }

        public Builder pieces(Map<Integer, Piece> pieces) {
            this.pieces = pieces;
            return this;
        }

        public Builder pieceUsed(BitSet pieceUsed) {
            this.pieceUsed = pieceUsed;
            return this;
        }

        public Builder solver(EternitySolver solver) {
            this.solver = solver;
            return this;
        }

        public Builder configuration(SolverConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder sharedState(SharedSearchState sharedState) {
            this.sharedState = sharedState;
            return this;
        }

        public Builder totalPieces(int totalPieces) {
            this.totalPieces = totalPieces;
            return this;
        }

        public Builder startTimeMs(long startTimeMs) {
            this.startTimeMs = startTimeMs;
            return this;
        }

        public SolverContext build() {
            if (board == null || pieces == null || solver == null) {
                throw new IllegalStateException("board, pieces, and solver are required");
            }
            if (pieceUsed == null) {
                pieceUsed = new BitSet(pieces.size() + 1);
            }
            if (totalPieces == 0) {
                totalPieces = pieces.size();
            }
            if (startTimeMs == 0) {
                startTimeMs = System.currentTimeMillis();
            }
            return new SolverContext(this);
        }
    }
}
