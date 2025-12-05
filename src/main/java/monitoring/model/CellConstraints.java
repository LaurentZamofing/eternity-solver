package monitoring.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the constraints for a specific cell position.
 * Includes border requirements and neighbor constraints.
 */
public class CellConstraints {

    private Map<String, Boolean> borders; // "north", "east", "south", "west" -> true if on border
    private Map<String, NeighborInfo> neighbors; // direction -> neighbor info

    public CellConstraints() {
        this.borders = new HashMap<>();
        this.neighbors = new HashMap<>();
    }

    public Map<String, Boolean> getBorders() {
        return borders;
    }

    public void setBorders(Map<String, Boolean> borders) {
        this.borders = borders;
    }

    public void setBorder(String direction, boolean onBorder) {
        this.borders.put(direction, onBorder);
    }

    public Map<String, NeighborInfo> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(Map<String, NeighborInfo> neighbors) {
        this.neighbors = neighbors;
    }

    public void setNeighbor(String direction, NeighborInfo info) {
        this.neighbors.put(direction, info);
    }

    /**
     * Information about a neighboring cell.
     */
    public static class NeighborInfo {
        private int row;
        private int col;
        private Integer pieceId; // null if neighbor is empty
        private Integer rotation; // null if neighbor is empty
        private Integer edgeValue; // Required edge value to match neighbor

        public NeighborInfo() {
        }

        public NeighborInfo(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public NeighborInfo(int row, int col, int pieceId, int rotation, int edgeValue) {
            this.row = row;
            this.col = col;
            this.pieceId = pieceId;
            this.rotation = rotation;
            this.edgeValue = edgeValue;
        }

        // Getters and Setters

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getCol() {
            return col;
        }

        public void setCol(int col) {
            this.col = col;
        }

        public Integer getPieceId() {
            return pieceId;
        }

        public void setPieceId(Integer pieceId) {
            this.pieceId = pieceId;
        }

        public Integer getRotation() {
            return rotation;
        }

        public void setRotation(Integer rotation) {
            this.rotation = rotation;
        }

        public Integer getEdgeValue() {
            return edgeValue;
        }

        public void setEdgeValue(Integer edgeValue) {
            this.edgeValue = edgeValue;
        }
    }
}
