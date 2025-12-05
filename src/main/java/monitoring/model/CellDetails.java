package monitoring.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete details about a specific cell in the puzzle board.
 * Includes current placement, all possible pieces, and constraints.
 */
public class CellDetails {

    private Position position;
    private PieceOption currentPiece; // null if cell is empty
    private List<PieceOption> possiblePieces;
    private CellConstraints constraints;
    private Statistics statistics;

    public CellDetails() {
        this.possiblePieces = new ArrayList<>();
    }

    public CellDetails(int row, int col) {
        this.position = new Position(row, col);
        this.possiblePieces = new ArrayList<>();
    }

    // Getters and Setters

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public PieceOption getCurrentPiece() {
        return currentPiece;
    }

    public void setCurrentPiece(PieceOption currentPiece) {
        this.currentPiece = currentPiece;
    }

    public List<PieceOption> getPossiblePieces() {
        return possiblePieces;
    }

    public void setPossiblePieces(List<PieceOption> possiblePieces) {
        this.possiblePieces = possiblePieces;
    }

    public void addPossiblePiece(PieceOption option) {
        this.possiblePieces.add(option);
    }

    public CellConstraints getConstraints() {
        return constraints;
    }

    public void setConstraints(CellConstraints constraints) {
        this.constraints = constraints;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    /**
     * Cell position (row, col).
     */
    public static class Position {
        private int row;
        private int col;

        public Position() {
        }

        public Position(int row, int col) {
            this.row = row;
            this.col = col;
        }

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
    }

    /**
     * Statistics about the possibilities for this cell.
     */
    public static class Statistics {
        private int totalPieces; // Total pieces in puzzle
        private int usedPieces; // Pieces already placed
        private int availablePieces; // Pieces not yet placed
        private int validOptions; // Number of valid piece+rotation combinations
        private int invalidOptions; // Number of invalid combinations tested

        public Statistics() {
        }

        public Statistics(int totalPieces, int usedPieces, int validOptions, int invalidOptions) {
            this.totalPieces = totalPieces;
            this.usedPieces = usedPieces;
            this.availablePieces = totalPieces - usedPieces;
            this.validOptions = validOptions;
            this.invalidOptions = invalidOptions;
        }

        // Getters and Setters

        public int getTotalPieces() {
            return totalPieces;
        }

        public void setTotalPieces(int totalPieces) {
            this.totalPieces = totalPieces;
        }

        public int getUsedPieces() {
            return usedPieces;
        }

        public void setUsedPieces(int usedPieces) {
            this.usedPieces = usedPieces;
        }

        public int getAvailablePieces() {
            return availablePieces;
        }

        public void setAvailablePieces(int availablePieces) {
            this.availablePieces = availablePieces;
        }

        public int getValidOptions() {
            return validOptions;
        }

        public void setValidOptions(int validOptions) {
            this.validOptions = validOptions;
        }

        public int getInvalidOptions() {
            return invalidOptions;
        }

        public void setInvalidOptions(int invalidOptions) {
            this.invalidOptions = invalidOptions;
        }
    }
}
