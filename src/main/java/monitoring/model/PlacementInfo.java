package monitoring.model;

/**
 * Represents a single piece placement with position, piece ID, and rotation.
 * Used for tracking placement order and reconstructing board state history.
 */
public class PlacementInfo {
    private int row;
    private int col;
    private int pieceId;
    private int rotation;
    private int sequenceNumber; // Order in which this piece was placed (1-based)

    public PlacementInfo() {
    }

    public PlacementInfo(int row, int col, int pieceId, int rotation) {
        this.row = row;
        this.col = col;
        this.pieceId = pieceId;
        this.rotation = rotation;
    }

    public PlacementInfo(int row, int col, int pieceId, int rotation, int sequenceNumber) {
        this.row = row;
        this.col = col;
        this.pieceId = pieceId;
        this.rotation = rotation;
        this.sequenceNumber = sequenceNumber;
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

    public int getPieceId() {
        return pieceId;
    }

    public void setPieceId(int pieceId) {
        this.pieceId = pieceId;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Returns a position key for indexing (row,col format).
     */
    public String getPositionKey() {
        return row + "," + col;
    }

    @Override
    public String toString() {
        return "PlacementInfo{" +
                "row=" + row +
                ", col=" + col +
                ", pieceId=" + pieceId +
                ", rotation=" + rotation +
                ", seq=" + sequenceNumber +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlacementInfo that = (PlacementInfo) o;
        return row == that.row && col == that.col;
    }

    @Override
    public int hashCode() {
        return 31 * row + col;
    }
}
