package monitoring.model;

/**
 * Represents a possible piece placement option for a specific cell.
 * Includes the piece ID, rotation, computed edges, and validity status.
 */
public class PieceOption {

    private int pieceId;
    private int rotation; // 0-3 (number of 90° clockwise rotations)
    private int[] edges; // [N, E, S, W] after rotation
    private boolean isValid;
    private boolean isCurrent; // true if this is the currently placed piece
    private boolean alreadyTried; // true if this piece was already tested (based on placement strategy)
    private String invalidReason; // null if valid, otherwise reason (border_conflict, neighbor_conflict, etc.)

    public PieceOption() {
    }

    public PieceOption(int pieceId, int rotation, int[] edges, boolean isValid) {
        this.pieceId = pieceId;
        this.rotation = rotation;
        this.edges = edges;
        this.isValid = isValid;
        this.isCurrent = false;
    }

    public PieceOption(int pieceId, int rotation, int[] edges, boolean isValid, String invalidReason) {
        this.pieceId = pieceId;
        this.rotation = rotation;
        this.edges = edges;
        this.isValid = isValid;
        this.invalidReason = invalidReason;
        this.isCurrent = false;
    }

    // Getters and Setters

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

    public int[] getEdges() {
        return edges;
    }

    public void setEdges(int[] edges) {
        this.edges = edges;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }

    public String getInvalidReason() {
        return invalidReason;
    }

    public void setInvalidReason(String invalidReason) {
        this.invalidReason = invalidReason;
    }

    public boolean isAlreadyTried() {
        return alreadyTried;
    }

    public void setAlreadyTried(boolean alreadyTried) {
        this.alreadyTried = alreadyTried;
    }
}
