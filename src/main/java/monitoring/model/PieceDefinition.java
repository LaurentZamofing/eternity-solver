package monitoring.model;

/**
 * Represents the definition of a puzzle piece with its 4 edges.
 * Each edge has a pattern/color identified by a number.
 */
public class PieceDefinition {
    private int pieceId;
    private int north;  // Top edge
    private int east;   // Right edge
    private int south;  // Bottom edge
    private int west;   // Left edge

    public PieceDefinition() {
    }

    public PieceDefinition(int pieceId, int north, int east, int south, int west) {
        this.pieceId = pieceId;
        this.north = north;
        this.east = east;
        this.south = south;
        this.west = west;
    }

    // Getters and setters
    public int getPieceId() {
        return pieceId;
    }

    public void setPieceId(int pieceId) {
        this.pieceId = pieceId;
    }

    public int getNorth() {
        return north;
    }

    public void setNorth(int north) {
        this.north = north;
    }

    public int getEast() {
        return east;
    }

    public void setEast(int east) {
        this.east = east;
    }

    public int getSouth() {
        return south;
    }

    public void setSouth(int south) {
        this.south = south;
    }

    public int getWest() {
        return west;
    }

    public void setWest(int west) {
        this.west = west;
    }

    /**
     * Get edge value after applying rotation.
     * Rotation: 0=0°, 1=90°, 2=180°, 3=270° clockwise
     */
    public int getEdge(String direction, int rotation) {
        // Normalize rotation
        rotation = rotation % 4;

        // Calculate which physical edge corresponds to the requested direction after rotation
        switch (direction.toLowerCase()) {
            case "north":
            case "n":
                switch (rotation) {
                    case 0: return north;
                    case 1: return west;
                    case 2: return south;
                    case 3: return east;
                }
            case "east":
            case "e":
                switch (rotation) {
                    case 0: return east;
                    case 1: return north;
                    case 2: return west;
                    case 3: return south;
                }
            case "south":
            case "s":
                switch (rotation) {
                    case 0: return south;
                    case 1: return east;
                    case 2: return north;
                    case 3: return west;
                }
            case "west":
            case "w":
                switch (rotation) {
                    case 0: return west;
                    case 1: return south;
                    case 2: return east;
                    case 3: return north;
                }
        }
        return 0;
    }

    @Override
    public String toString() {
        return String.format("Piece %d: N=%d E=%d S=%d W=%d",
            pieceId, north, east, south, west);
    }
}
