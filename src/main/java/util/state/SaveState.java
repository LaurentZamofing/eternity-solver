package util.state;

import model.Board;
import model.Piece;
import util.SaveStateManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable value object representing complete puzzle save state.
 * Contains all information needed to resume solving.
 *
 * <h2>Design</h2>
 * Uses Builder pattern for flexible construction and immutability for thread safety.
 *
 * <h2>Usage</h2>
 * <pre>
 * SaveState state = SaveState.builder()
 *     .configId("eternity2_p01")
 *     .board(board)
 *     .pieces(pieces)
 *     .placementOrder(order)
 *     .depth(42)
 *     .elapsedTimeMs(12000)
 *     .build();
 * </pre>
 *
 * @author Eternity Solver Team
 * @version 2.0.0 (Phase 7 refactoring)
 */
public class SaveState {

    private final String configId;
    private final Board board;
    private final Map<Integer, Piece> pieces;
    private final List<SaveStateManager.PlacementInfo> placementOrder;
    private final int depth;
    private final long elapsedTimeMs;
    private final long timestamp;

    // Optional statistics
    private final Long recursiveCalls;
    private final Long placements;
    private final Long backtracks;

    private SaveState(Builder builder) {
        this.configId = builder.configId;
        this.board = builder.board;
        this.pieces = new HashMap<>(builder.pieces);
        this.placementOrder = new ArrayList<>(builder.placementOrder);
        this.depth = builder.depth;
        this.elapsedTimeMs = builder.elapsedTimeMs;
        this.timestamp = builder.timestamp != 0 ? builder.timestamp : System.currentTimeMillis();
        this.recursiveCalls = builder.recursiveCalls;
        this.placements = builder.placements;
        this.backtracks = builder.backtracks;
    }

    // Getters
    public String getConfigId() { return configId; }
    public Board getBoard() { return board; }
    public Map<Integer, Piece> getPieces() { return new HashMap<>(pieces); }
    public List<SaveStateManager.PlacementInfo> getPlacementOrder() {
        return new ArrayList<>(placementOrder);
    }
    public int getDepth() { return depth; }
    public long getElapsedTimeMs() { return elapsedTimeMs; }
    public long getTimestamp() { return timestamp; }

    // Optional statistics
    public Long getRecursiveCalls() { return recursiveCalls; }
    public Long getPlacements() { return placements; }
    public Long getBacktracks() { return backtracks; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String configId;
        private Board board;
        private Map<Integer, Piece> pieces = new HashMap<>();
        private List<SaveStateManager.PlacementInfo> placementOrder = new ArrayList<>();
        private int depth = 0;
        private long elapsedTimeMs = 0;
        private long timestamp = 0;
        private Long recursiveCalls;
        private Long placements;
        private Long backtracks;

        public Builder configId(String configId) {
            this.configId = configId;
            return this;
        }

        public Builder board(Board board) {
            this.board = board;
            return this;
        }

        public Builder pieces(Map<Integer, Piece> pieces) {
            this.pieces = new HashMap<>(pieces);
            return this;
        }

        public Builder placementOrder(List<SaveStateManager.PlacementInfo> order) {
            this.placementOrder = new ArrayList<>(order);
            return this;
        }

        public Builder depth(int depth) {
            this.depth = depth;
            return this;
        }

        public Builder elapsedTimeMs(long ms) {
            this.elapsedTimeMs = ms;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder recursiveCalls(Long calls) {
            this.recursiveCalls = calls;
            return this;
        }

        public Builder placements(Long placements) {
            this.placements = placements;
            return this;
        }

        public Builder backtracks(Long backtracks) {
            this.backtracks = backtracks;
            return this;
        }

        public SaveState build() {
            if (configId == null || configId.isEmpty()) {
                throw new IllegalStateException("configId is required");
            }
            if (board == null) {
                throw new IllegalStateException("board is required");
            }
            return new SaveState(this);
        }
    }

    @Override
    public String toString() {
        return String.format("SaveState{config=%s, depth=%d, time=%dms}",
            configId, depth, elapsedTimeMs);
    }
}
