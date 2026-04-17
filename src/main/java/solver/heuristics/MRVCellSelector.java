package solver.heuristics;

import model.Board;
import model.Piece;
import solver.DomainManager;
import solver.DomainManager.ValidPlacement;
import util.CellLabelFormatter;
import util.DebugHelper;
import util.SolverLogger;

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
    private boolean debugBacktracking = false;
    private boolean debugShowAlternatives = false;
    private int debugMaxCandidates = 5;
    private boolean silentMode = false; // Temporarily disable logs for internal calls

    /**
     * Interface for checking if a piece fits at a position.
     */
    public interface FitChecker {
        boolean fits(Board board, int r, int c, int[] candidateEdges);
    }

    /**
     * Internal class to track candidate cells for debug logging.
     */
    private static class CellCandidate {
        final int row;
        final int col;
        final int possibilities;
        final boolean isBorder;
        final int borderNeighbors;
        final boolean wouldTrap;
        final String rejectionReason;

        CellCandidate(int row, int col, int possibilities, boolean isBorder,
                     int borderNeighbors, boolean wouldTrap, String rejectionReason) {
            this.row = row;
            this.col = col;
            this.possibilities = possibilities;
            this.isBorder = isBorder;
            this.borderNeighbors = borderNeighbors;
            this.wouldTrap = wouldTrap;
            this.rejectionReason = rejectionReason;
        }

        String getCellLabel() {
            return CellLabelFormatter.format(row, col);
        }
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

    /**
     * Enables or disables debug backtracking logs.
     *
     * @param enabled true to enable debug logs
     */
    public void setDebugBacktracking(boolean enabled) {
        this.debugBacktracking = enabled;
    }

    /**
     * Enables or disables showing alternative cell candidates in debug logs.
     *
     * @param enabled true to show alternatives
     */
    public void setDebugShowAlternatives(boolean enabled) {
        this.debugShowAlternatives = enabled;
    }

    /**
     * Sets maximum number of alternative candidates to show in debug logs.
     *
     * @param max maximum number of candidates
     */
    public void setDebugMaxCandidates(int max) {
        this.debugMaxCandidates = max;
    }

    /**
     * Temporarily enables/disables logging (for internal calls like save file generation).
     *
     * @param silent true to suppress debug logs
     */
    public void setSilentMode(boolean silent) {
        this.silentMode = silent;
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

        // Debug: Track all candidates for detailed logging
        List<CellCandidate> allCandidates = (debugShowAlternatives && !silentMode) ? new ArrayList<>() : null;
        int cellsEvaluated = 0;

        if (debugBacktracking && !silentMode) {
            SolverLogger.info("📊 MRV Cell Selection:");
        }

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.isEmpty(r, c)) {
                    // Detect if this is a border cell
                    boolean isBorder = (r == 0 || r == board.getRows() - 1 || c == 0 || c == board.getCols() - 1);

                    // Use AC-3 domains if available for more efficient MRV
                    // IMPORTANT: Count TOTAL ROTATIONS, not just unique pieces
                    // This gives more accurate MRV: a cell with 1 piece having 4 rotations
                    // should be ranked LOWER than a cell with 1 piece having 1 rotation
                    int totalValidRotations;
                    if (useAC3 && domainManager.isAC3Initialized()) {
                        Map<Integer, List<ValidPlacement>> domain = domainManager.getDomain(r, c);
                        // Count total number of valid placements (piece + rotation combinations)
                        if (domain != null) {
                            totalValidRotations = 0;
                            for (List<ValidPlacement> placements : domain.values()) {
                                totalValidRotations += placements.size();
                            }
                        } else {
                            totalValidRotations = 0;
                        }
                    } else {
                        // Fall back to computing from scratch
                        List<ValidPlacement> validPlacements = getValidPlacements(board, r, c, piecesById, pieceUsed, totalPieces);
                        // Count total valid placements (each entry is a piece + rotation combination)
                        totalValidRotations = validPlacements.size();
                    }

                    // If no possibilities, it's an immediate dead-end
                    if (totalValidRotations == 0) {
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
                                    if (totalValidRotations * 2 <= minUniquePieces) {
                                        shouldUpdate = true; // Current has ≤50% options -> accept despite gap
                                    }
                                } else {
                                    // Both have border neighbors: compare neighbor count first
                                    if (currentBorderNeighbors > bestBorderNeighbors) {
                                        // More neighbors = better continuity -> ALWAYS prefer
                                        shouldUpdate = true;
                                    } else if (currentBorderNeighbors == bestBorderNeighbors) {
                                        // Same neighbor count: use MRV to break tie (based on rotation count)
                                        shouldUpdate = (totalValidRotations < minUniquePieces);
                                    }
                                    // If currentBorderNeighbors < bestBorderNeighbors -> don't update, keep best
                                }
                            } else {
                                // BOTH cells trap a gap: choose lesser evil (MRV based on rotations)
                                shouldUpdate = (totalValidRotations < minUniquePieces);
                            }
                        } else if (!isBorder && !bestIsBorder) {
                            // Two interior cells: normal MRV (based on rotation count)
                            shouldUpdate = (totalValidRotations < minUniquePieces);
                        }
                        // Otherwise (!isBorder && bestIsBorder) -> don't update, keep border
                    } else {
                        // Normal mode without border prioritization
                        shouldUpdate = (totalValidRotations < minUniquePieces);
                    }

                    // Track candidates for debug logging
                    cellsEvaluated++;
                    if (debugShowAlternatives && allCandidates != null) {
                        boolean wouldTrap = isBorder && neighborAnalyzer.wouldCreateTrappedGap(board, r, c, null, null, null, 0, -1);
                        int borderNeighbors = isBorder ? neighborAnalyzer.countAdjacentFilledBorderCells(board, r, c) : 0;
                        String reason = shouldUpdate ? "SELECTED" : "rejected";
                        allCandidates.add(new CellCandidate(r, c, totalValidRotations, isBorder, borderNeighbors, wouldTrap, reason));

                        // Extra debug: log selection decision for cells with few possibilities
                        if (debugBacktracking && totalValidRotations <= 10) {
                            String cellLabel = CellLabelFormatter.format(r, c);
                            SolverLogger.info("      Eval " + cellLabel + ": " + totalValidRotations + " poss, " +
                                            "border=" + isBorder + ", neighbors=" + borderNeighbors + ", trap=" + wouldTrap +
                                            " → " + (shouldUpdate ? "UPDATE" : "skip"));
                        }
                    }

                    // Choose cell with minimum rotations (with border prioritization if enabled)
                    if (shouldUpdate) {
                        minUniquePieces = totalValidRotations;
                        bestCell = new int[]{r, c};
                        bestIsBorder = isBorder;
                        // IMPORTANT: Update border neighbor count for border cells
                        if (isBorder) {
                            bestBorderNeighbors = neighborAnalyzer.countAdjacentFilledBorderCells(board, r, c);
                        }
                    } else if (totalValidRotations == minUniquePieces && bestCell != null && (isBorder == bestIsBorder || !prioritizeBorders)) {
                        // Tie-breaking: use degree heuristic
                        // Count OCCUPIED neighbors for this cell (more constraints = better)
                        int currentConstraints = neighborAnalyzer.countOccupiedNeighbors(board, r, c);
                        int bestConstraints = neighborAnalyzer.countOccupiedNeighbors(board, bestCell[0], bestCell[1]);

                        // Choose cell with most occupied neighbors (most constraining)
                        if (currentConstraints > bestConstraints) {
                            if (debugBacktracking && !silentMode) {
                                String cellLabel = CellLabelFormatter.format(r, c);
                                String bestLabel = CellLabelFormatter.format(bestCell[0], bestCell[1]);
                                SolverLogger.info("      🎯 TIE-BREAK: " + cellLabel + " (" + currentConstraints + " neighbors) > " +
                                                 bestLabel + " (" + bestConstraints + " neighbors) → choosing " + cellLabel);
                            }
                            bestCell = new int[]{r, c};
                            // BUGFIX: Update bestBorderNeighbors when bestCell changes in tie-breaking
                            if (isBorder) {
                                bestBorderNeighbors = neighborAnalyzer.countAdjacentFilledBorderCells(board, r, c);
                            }
                        } else if (currentConstraints == bestConstraints) {
                            // Second tie-breaking: prefer central positions
                            int centerR = board.getRows() / 2;
                            int centerC = board.getCols() / 2;
                            int currentDistToCenter = Math.abs(r - centerR) + Math.abs(c - centerC);
                            int bestDistToCenter = Math.abs(bestCell[0] - centerR) + Math.abs(bestCell[1] - centerC);

                            if (currentDistToCenter < bestDistToCenter) {
                                if (debugBacktracking && !silentMode) {
                                    String cellLabel = CellLabelFormatter.format(r, c);
                                    String bestLabel = CellLabelFormatter.format(bestCell[0], bestCell[1]);
                                    SolverLogger.info("      🎯 TIE-BREAK: " + cellLabel + " (dist=" + currentDistToCenter + ") closer to center than " +
                                                     bestLabel + " (dist=" + bestDistToCenter + ") → choosing " + cellLabel);
                                }
                                bestCell = new int[]{r, c};
                                // BUGFIX: Update bestBorderNeighbors when bestCell changes in tie-breaking
                                if (isBorder) {
                                    bestBorderNeighbors = neighborAnalyzer.countAdjacentFilledBorderCells(board, r, c);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Debug logging: Show selection result and alternatives
        if (debugBacktracking && !silentMode) {
            if (bestCell != null) {
                String cellLabel = CellLabelFormatter.format(bestCell[0], bestCell[1]);
                SolverLogger.info("   Evaluated " + cellsEvaluated + " empty cells");
                SolverLogger.info("   ⭐ SELECTED: " + cellLabel + " (" + bestCell[0] + "," + bestCell[1] + ") with " +
                                 minUniquePieces + " possibilities" +
                                 (bestIsBorder ? " [border, " + bestBorderNeighbors + " neighbors]" : " [interior]"));

                // Show top alternatives if enabled
                if (debugShowAlternatives && allCandidates != null && allCandidates.size() > 1) {
                    // Sort by possibilities (ascending) to show most constrained first
                    allCandidates.sort((a, b) -> {
                        if (a.possibilities != b.possibilities) return Integer.compare(a.possibilities, b.possibilities);
                        if (a.borderNeighbors != b.borderNeighbors) return Integer.compare(b.borderNeighbors, a.borderNeighbors);
                        return 0;
                    });

                    SolverLogger.info("");
                    SolverLogger.info("   📊 TOP " + Math.min(debugMaxCandidates, allCandidates.size()) + " CANDIDATES:");
                    SolverLogger.info("   ┌──────┬────────┬──────────┬─────────┬─────────┐");
                    SolverLogger.info("   │ Cell │  P/R   │ Neighbor │ DistCtr │ Status  │");
                    SolverLogger.info("   ├──────┼────────┼──────────┼─────────┼─────────┤");

                    int centerR = board.getRows() / 2;
                    int centerC = board.getCols() / 2;

                    for (int i = 0; i < Math.min(debugMaxCandidates, allCandidates.size()); i++) {
                        CellCandidate cand = allCandidates.get(i);
                        boolean isSelected = (cand.row == bestCell[0] && cand.col == bestCell[1]);

                        // Calculate occupied neighbors for display
                        int occupiedNeighbors = neighborAnalyzer.countOccupiedNeighbors(board, cand.row, cand.col);
                        int distToCenter = Math.abs(cand.row - centerR) + Math.abs(cand.col - centerC);

                        // Format each column to exact width: Cell=6, P/R=8, Neighbor=10, DistCtr=9, Status=9
                        String candLabel = String.format("%-4s  ", cand.getCellLabel());  // "H9  " + 2 spaces = 6 chars
                        String piecesRot = String.format("%3d/%-3d ", cand.possibilities, cand.possibilities);  // "  1/1   " = 8 chars
                        String neighbors = String.format("   %2d     ", occupiedNeighbors);  // "    2     " = 10 chars
                        String distance = String.format("   %2d    ", distToCenter);  // "    1    " = 9 chars
                        String status = isSelected ? " <* SEL> " : "         ";  // 9 chars (ASCII only)

                        SolverLogger.info("   │" + candLabel + "│" + piecesRot + "│" + neighbors + "│" + distance + "│" + status + "│");
                    }
                    SolverLogger.info("   └──────┴────────┴──────────┴─────────┴─────────┘");
                }

                // No pause here - let the caller decide when to pause
            } else {
                SolverLogger.info("   ⚠ No valid cell found (board complete or dead-end)");
                // No pause here - let the caller decide when to pause
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
