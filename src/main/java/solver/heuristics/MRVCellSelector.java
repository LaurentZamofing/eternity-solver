package solver.heuristics;

import model.Board;
import model.Piece;
import solver.DomainManager;
import solver.DomainManager.ValidPlacement;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * MRVCellSelector implements MRV (Minimum Remaining Values) heuristic.
 *
 * MRV heuristic selects the empty cell with fewest valid placements possible.
 * Also known as "most constrained variable" or "fail-first" heuristic.
 * By choosing the most constrained cell first, we detect failures earlier in
 * the search tree, enabling more efficient pruning.
 *
 * Additional features:
 * - Border prioritization: Fills border cells before interior
 * - Degree heuristic for tie-breaking: Chooses cells with more filled neighbors
 * - Gap detection: Avoids creating trapped gaps on borders
 *
 * @author Eternity Solver Team
 */
public class MRVCellSelector implements HeuristicStrategy {

    private final DomainManager domainManager;
    private final FitChecker fitChecker;
    private final solver.NeighborAnalyzer neighborAnalyzer;
    private boolean prioritizeBorders = false;
    private boolean useAC3 = true;

    /**
     * Interface for checking if a piece fits at a position.
     */
    public interface FitChecker {
        boolean fits(Board board, int r, int c, int[] candidateEdges);
    }

    /**
     * Constructor for MRVCellSelector.
     *
     * @param domainManager domain manager for AC-3 domains
     * @param fitChecker fit checker to validate placements
     * @param neighborAnalyzer neighbor analyzer for spatial analysis
     */
    public MRVCellSelector(DomainManager domainManager, FitChecker fitChecker, solver.NeighborAnalyzer neighborAnalyzer) {
        this.domainManager = domainManager;
        this.fitChecker = fitChecker;
        this.neighborAnalyzer = neighborAnalyzer;
    }

    /**
     * Enables or disables border prioritization.
     *
     * @param enabled true to prioritize border cells
     */
    public void setPrioritizeBorders(boolean enabled) {
        this.prioritizeBorders = enabled;
    }

    /**
     * Enables or disables use of AC-3 domains.
     *
     * @param enabled true to use AC-3 domains
     */
    public void setUseAC3(boolean enabled) {
        this.useAC3 = enabled;
    }

    @Override
    public CellPosition selectNextCell(Board board, Map<Integer, Piece> piecesById,
                                       BitSet pieceUsed, int totalPieces) {
        int[] result = findNextCellMRV(board, piecesById, pieceUsed, totalPieces);
        if (result == null) return null;
        return new CellPosition(result[0], result[1]);
    }

    /**
     * Finds next empty cell using MRV (Minimum Remaining Values) heuristic.
     * Chooses cell with fewest valid placements possible.
     *
     * @param board current grid
     * @param piecesById map of pieces by ID
     * @param pieceUsed array of used pieces
     * @param totalPieces total number of pieces
     * @return coordinates [r, c] of most constrained cell, or null if none
     */
    public int[] findNextCellMRV(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        int[] bestCell = null;
        int minUniquePieces = Integer.MAX_VALUE;
        boolean bestIsBorder = false;
        int bestBorderNeighbors = 0; // Track number of border neighbors of best cell

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.isEmpty(r, c)) {
                    // Detect if this is a border cell
                    boolean isBorder = (r == 0 || r == board.getRows() - 1 || c == 0 || c == board.getCols() - 1);

                    // Use AC-3 domains if available for more efficient MRV
                    int uniquePiecesCount;
                    if (useAC3 && domainManager.isAC3Initialized()) {
                        Map<Integer, List<ValidPlacement>> domain = domainManager.getDomain(r, c);
                        // Count unique pieces from AC-3 domains
                        uniquePiecesCount = (domain != null) ? domain.size() : 0;
                    } else {
                        // Fall back to computing from scratch
                        List<ValidPlacement> validPlacements = getValidPlacements(board, r, c, piecesById, pieceUsed, totalPieces);

                        // Extract unique piece IDs
                        List<Integer> uniquePieceIds = new ArrayList<>();
                        for (ValidPlacement vp : validPlacements) {
                            if (!uniquePieceIds.contains(vp.pieceId)) {
                                uniquePieceIds.add(vp.pieceId);
                            }
                        }
                        uniquePiecesCount = uniquePieceIds.size();
                    }

                    // If no possibilities, it's an immediate dead-end
                    if (uniquePiecesCount == 0) {
                        // Dead-end detected - silent backtrack
                        return new int[]{r, c}; // Return immediately to backtrack
                    }

                    // BORDER PRIORITIZATION: if enabled, always choose border before interior
                    // NEW LOGIC: Penalize gaps (cells without neighbors) and favor continuous sequences
                    boolean shouldUpdate = false;
                    if (prioritizeBorders) {
                        if (isBorder && !bestIsBorder) {
                            // This cell is border and current best is not -> update
                            shouldUpdate = true;
                        } else if (isBorder == bestIsBorder && isBorder) {
                            // Two border cells: apply continuous filling strategy

                            // CRITICAL CHECK: Would this cell create a trapped gap?
                            boolean wouldTrap = neighborAnalyzer.wouldCreateTrappedGap(board, r, c, null, null, null, 0, -1);
                            boolean bestWouldTrap = (bestCell != null) ? neighborAnalyzer.wouldCreateTrappedGap(board, bestCell[0], bestCell[1], null, null, null, 0, -1) : false;

                            // RULE 0 (PRIORITY): NEVER choose a cell that traps a gap
                            if (!wouldTrap && bestWouldTrap) {
                                // Current cell doesn't trap, but best does -> prefer current
                                shouldUpdate = true;
                            } else if (wouldTrap && !bestWouldTrap) {
                                // Current cell traps, but best doesn't -> keep best
                                shouldUpdate = false;
                            } else if (!wouldTrap && !bestWouldTrap) {
                                // Neither traps: apply normal continuity rules

                                // Count filled border neighbors for current cell
                                int currentBorderNeighbors = neighborAnalyzer.countAdjacentFilledBorderCells(board, r, c);

                                // RULE 1: Always prefer cells with border neighbors (avoid gaps)
                                if (currentBorderNeighbors > 0 && bestBorderNeighbors == 0) {
                                    // Current has neighbors, best doesn't -> prefer current
                                    shouldUpdate = true;
                                } else if (currentBorderNeighbors == 0 && bestBorderNeighbors > 0) {
                                    // Best has neighbors, current doesn't -> keep best
                                    shouldUpdate = false;
                                } else if (currentBorderNeighbors == 0 && bestBorderNeighbors == 0) {
                                    // RULE 2: If both cells have no neighbors (start or potential gap)
                                    // Accept current ONLY if it has significantly fewer options (≤50%)
                                    if (uniquePiecesCount * 2 <= minUniquePieces) {
                                        shouldUpdate = true; // Current has ≤50% options -> accept despite gap
                                    }
                                } else {
                                    // Both have border neighbors: compare neighbor count first
                                    if (currentBorderNeighbors > bestBorderNeighbors) {
                                        // More neighbors = better continuity -> ALWAYS prefer
                                        shouldUpdate = true;
                                    } else if (currentBorderNeighbors == bestBorderNeighbors) {
                                        // Same neighbor count: use MRV to break tie
                                        shouldUpdate = (uniquePiecesCount < minUniquePieces);
                                    }
                                    // If currentBorderNeighbors < bestBorderNeighbors -> don't update, keep best
                                }
                            } else {
                                // BOTH cells trap a gap: choose lesser evil (MRV)
                                shouldUpdate = (uniquePiecesCount < minUniquePieces);
                            }
                        } else if (!isBorder && !bestIsBorder) {
                            // Two interior cells: normal MRV
                            shouldUpdate = (uniquePiecesCount < minUniquePieces);
                        }
                        // Otherwise (!isBorder && bestIsBorder) -> don't update, keep border
                    } else {
                        // Normal mode without border prioritization
                        shouldUpdate = (uniquePiecesCount < minUniquePieces);
                    }

                    // Choose cell with minimum unique pieces (with border prioritization if enabled)
                    if (shouldUpdate) {
                        minUniquePieces = uniquePiecesCount;
                        bestCell = new int[]{r, c};
                        bestIsBorder = isBorder;
                        // IMPORTANT: Update border neighbor count for border cells
                        if (isBorder) {
                            bestBorderNeighbors = neighborAnalyzer.countAdjacentFilledBorderCells(board, r, c);
                        }
                    } else if (uniquePiecesCount == minUniquePieces && bestCell != null && (isBorder == bestIsBorder || !prioritizeBorders)) {
                        // Tie-breaking: use degree heuristic
                        // Count OCCUPIED neighbors for this cell (more constraints = better)
                        int currentConstraints = neighborAnalyzer.countOccupiedNeighbors(board, r, c);
                        int bestConstraints = neighborAnalyzer.countOccupiedNeighbors(board, bestCell[0], bestCell[1]);

                        // Choose cell with most occupied neighbors (most constraining)
                        if (currentConstraints > bestConstraints) {
                            bestCell = new int[]{r, c};
                        } else if (currentConstraints == bestConstraints) {
                            // Second tie-breaking: prefer central positions
                            int centerR = board.getRows() / 2;
                            int centerC = board.getCols() / 2;
                            int currentDistToCenter = Math.abs(r - centerR) + Math.abs(c - centerC);
                            int bestDistToCenter = Math.abs(bestCell[0] - centerR) + Math.abs(bestCell[1] - centerC);

                            if (currentDistToCenter < bestDistToCenter) {
                                bestCell = new int[]{r, c};
                            }
                        }
                    }
                }
            }
        }

        return bestCell;
    }

    /**
     * Gets valid placements for a cell.
     */
    private List<ValidPlacement> getValidPlacements(Board board, int r, int c, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        List<ValidPlacement> validPlacements = new ArrayList<>();

        for (int pid = 1; pid <= totalPieces; pid++) {
            if (pieceUsed.get(pid)) continue; // Piece already used

            Piece piece = piecesById.get(pid);
            for (int rot = 0; rot < 4; rot++) {
                int[] candidate = piece.edgesRotated(rot);
                if (fitChecker.fits(board, r, c, candidate)) {
                    validPlacements.add(new ValidPlacement(pid, rot));
                }
            }
        }
        return validPlacements;
    }

    @Override
    public String getName() {
        return "MRV (Minimum Remaining Values)";
    }
}
