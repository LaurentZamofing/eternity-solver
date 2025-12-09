package solver;

import util.SolverLogger;

import model.Board;
import model.Piece;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * SingletonDetector finds pieces that can only be placed in a single position on the board.
 *
 * A singleton is a piece that has only one valid position on the board (although it may
 * have multiple valid rotations at that position). Finding and placing singletons is
 * a powerful optimization because if a singleton exists and is not placed at its unique position,
 * the puzzle becomes unsolvable.
 *
 * This class implements singleton detection by testing all available pieces against
 * all empty positions and rotations to identify pieces with exactly one valid position.
 *
 * @author Eternity Solver Team
 */
public class SingletonDetector {

    /**
     * Class to store information about a singleton piece.
     */
    public static class SingletonInfo {
        public final int pieceId;
        public final int row;
        public final int col;
        public final int rotation;

        public SingletonInfo(int pieceId, int row, int col, int rotation) {
            this.pieceId = pieceId;
            this.row = row;
            this.col = col;
            this.rotation = rotation;
        }
    }

    /**
     * Statistics tracking for singleton detection.
     */
    public static class Statistics {
        public long singletonsFound = 0;
        public long singletonsPlaced = 0;
        public long deadEndsDetected = 0;

        public void incrementSingletonsFound() {
            singletonsFound++;
        }

        public void incrementSingletonsPlaced() {
            singletonsPlaced++;
        }

        public void incrementDeadEnds() {
            deadEndsDetected++;
        }
    }

    private final FitChecker fitChecker;
    private final Statistics stats;
    private final boolean verbose;

    /**
     * Interface to check if a piece fits at a position.
     */
    public interface FitChecker {
        boolean fits(Board board, int r, int c, int[] candidateEdges);
    }

    /**
     * Constructor for SingletonDetector.
     *
     * @param fitChecker the fit checker to use for validating placements
     * @param stats the statistics tracker
     * @param verbose whether to display verbose output
     */
    public SingletonDetector(FitChecker fitChecker, Statistics stats, boolean verbose) {
        this.fitChecker = fitChecker;
        this.stats = stats;
        this.verbose = verbose;
    }

    /**
     * Searches for a piece that can only go in one place (singleton).
     * This is a powerful optimization: if a piece has only one possible position,
     * it MUST be placed there, otherwise the branch is doomed to fail.
     *
     * @param board current board
     * @param piecesById map of pieces by ID
     * @param pieceUsed bitset of used pieces
     * @param totalPieces total number of pieces
     * @return singleton info if found, null otherwise
     */
    public SingletonInfo findSingletonPiece(Board board, Map<Integer, Piece> piecesById,
                                           BitSet pieceUsed, int totalPieces) {
        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed.get(pid)) continue; // Piece already used
            Piece piece = piecesById.get(pid);
            List<int[]> possiblePositions = new ArrayList<>(); // [r, c, rotation]

            // Test all possible positions and rotations for this piece
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (board.isEmpty(r, c)) {
                        for (int rot = 0; rot < 4; rot++) {
                            int[] candidate = piece.edgesRotated(rot);
                            if (fitChecker.fits(board, r, c, candidate)) {
                                possiblePositions.add(new int[]{r, c, rot});
                            }
                        }
                    }
                }
            }

            // Check if the piece can only go in one POSITION (regardless of number of rotations)
            if (possiblePositions.size() > 0) {
                // Group by position (r,c) to see if all possibilities are at the same position
                int firstRow = possiblePositions.get(0)[0];
                int firstCol = possiblePositions.get(0)[1];
                boolean samePosition = true;

                for (int[] pos : possiblePositions) {
                    if (pos[0] != firstRow || pos[1] != firstCol) {
                        samePosition = false;
                        break;
                    }
                }

                // If all possibilities are at the same position → singleton!
                if (samePosition) {
                    // Choose first possible rotation (arbitrary, we'll test others during backtracking if needed)
                    int[] pos = possiblePositions.get(0);
                    stats.incrementSingletonsFound();
                    if (verbose) {
                        String rotInfo = possiblePositions.size() == 1 ?
                            " with rotation " + (pos[2] * 90) + "°" :
                            " with " + possiblePositions.size() + " possible rotations";
                        System.out.println("🎯 SINGLETON found! Piece " + pid + " can only go at (" + pos[0] + ", " + pos[1] + ")" + rotInfo);
                    }
                    return new SingletonInfo(pid, pos[0], pos[1], pos[2]);
                }
            }

            // Dead-end: this piece cannot go anywhere!
            if (possiblePositions.size() == 0) {
                stats.incrementDeadEnds();
                if (verbose) {
                    SolverLogger.info("⚠ DEAD-END: Piece " + pid + " cannot go anywhere!");
                }
                return null;
            }
        }

        return null; // No singleton found
    }

    /**
     * Checks if there are pieces that cannot be placed anywhere (dead-end detection).
     * This is a simpler check than full singleton detection.
     *
     * @param board current board state
     * @param piecesById map of all pieces
     * @param pieceUsed bitset tracking used pieces
     * @param totalPieces total number of pieces
     * @return true if a dead-end is detected (a piece has no valid placement)
     */
    public boolean hasDeadEnd(Board board, Map<Integer, Piece> piecesById,
                             BitSet pieceUsed, int totalPieces) {
        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed.get(pid)) continue;
            Piece piece = piecesById.get(pid);
            boolean hasValidPlacement = false;

            // Check if the piece has at least one valid placement
            outerLoop:
            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    if (board.isEmpty(r, c)) {
                        for (int rot = 0; rot < 4; rot++) {
                            int[] candidate = piece.edgesRotated(rot);
                            if (fitChecker.fits(board, r, c, candidate)) {
                                hasValidPlacement = true;
                                break outerLoop;
                            }
                        }
                    }
                }
            }

            if (!hasValidPlacement) {
                stats.incrementDeadEnds();
                if (verbose) {
                    SolverLogger.info("⚠ DEAD-END: Piece " + pid + " cannot go anywhere!");
                }
                return true;
            }
        }

        return false;
    }
}
