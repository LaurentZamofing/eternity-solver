package solver.debug;

import model.Board;
import model.Piece;
import solver.BacktrackingContext;
import solver.EternitySolver;
import util.CellLabelFormatter;
import util.DebugHelper;
import util.PositionKey;
import util.SaveStateManager;
import util.SolverLogger;

import java.util.List;
import java.util.Map;

/**
 * Centralized debug logging for placement operations.
 * 
 * Extracts all debug logging logic from MRVPlacementStrategy to improve
 * separation of concerns and reduce code duplication.
 */
public class DebugPlacementLogger {

    private final boolean showBoard;

    public DebugPlacementLogger(boolean showBoard) {
        this.showBoard = showBoard;
    }

    /**
     * Logs the start of placement attempts for a cell.
     */
    public void logCellSelection(int row, int col, int depth, int availablePieces,
                                 EternitySolver solver, BacktrackingContext context) {
        String cellLabel = CellLabelFormatter.format(row, col);
        SolverLogger.info("");
        SolverLogger.info("╔═══════════════════════════════════════════════════════════╗");
        SolverLogger.info("║  TRYING CELL: " + cellLabel + " (depth " + depth + ", " + availablePieces + " pieces available)");
        SolverLogger.info("╚═══════════════════════════════════════════════════════════╝");

        if (showBoard) {
            int[] nextCellToHighlight = new int[]{row, col};
            List<Integer> unusedPieces = context.getUnusedPieces();
            Map<PositionKey, Integer> placementOrderMap = solver.buildPlacementOrderMap();

            SolverLogger.info("");
            SolverLogger.info("Board BEFORE attempting cell " + cellLabel + " (shown in BLUE):");
            SolverLogger.info("");
            solver.printBoardWithLabels(context.board, context.piecesById, unusedPieces,
                                       null, nextCellToHighlight, placementOrderMap);
        }
    }

    /**
     * Logs AC-3 dead-end detection.
     */
    public void logAC3DeadEnd(int row, int col, int pieceId,
                             EternitySolver solver, BacktrackingContext context) {
        String cellLabel = CellLabelFormatter.format(row, col);
        SolverLogger.info("       ✗ Failed: AC-3 dead-end detected after placing piece " + pieceId + " at " + cellLabel);

        if (showBoard) {
            List<Integer> unusedPieces = context.getUnusedPieces();
            SolverLogger.info("");
            SolverLogger.info("       📊 Board state when dead-end detected:");
            SolverLogger.info("");
            solver.printBoardWithLabels(context.board, context.piecesById, unusedPieces);
            SolverLogger.info("");
        }
    }

    /**
     * Logs successful placement of a piece.
     */
    public void logPlacementSuccess(int row, int col, int pieceId, int rotation,
                                    int prevDepth, int newDepth, long backtracks,
                                    EternitySolver solver, BacktrackingContext context) {
        String cellLabel = CellLabelFormatter.format(row, col);
        SolverLogger.info("");
        SolverLogger.info("╔═══════════════════════════════════════════════════════════╗");
        SolverLogger.info("║  ✓ PLACEMENT SUCCESS: Piece " + pieceId + " at " + cellLabel + " rotation " + (rotation * 90) + "°");
        SolverLogger.info("║  Depth: " + prevDepth + " → " + newDepth);
        SolverLogger.info("║  Backtracks so far: " + backtracks);
        SolverLogger.info("╚═══════════════════════════════════════════════════════════╝");

        if (showBoard) {
            SolverLogger.info("");
            SolverLogger.info("Board AFTER placement (last piece in MAGENTA):");
            SolverLogger.info("");

            List<Integer> unusedPieces = context.getUnusedPieces();
            SaveStateManager.PlacementInfo lastPlacement =
                new SaveStateManager.PlacementInfo(row, col, pieceId, rotation);
            Map<PositionKey, Integer> placementOrderMap = solver.buildPlacementOrderMap();

            solver.printBoardWithLabels(context.board, context.piecesById, unusedPieces,
                                       lastPlacement, null, placementOrderMap);
        }

        SolverLogger.info("");
        DebugHelper.waitForUserInput("Press ENTER to continue to next step...");
    }

    /**
     * Logs backtracking from a cell.
     */
    public void logBacktrack(int row, int col, int pieceId, int currentDepth,
                            EternitySolver solver, BacktrackingContext context) {
        String cellLabel = CellLabelFormatter.format(row, col);
        SolverLogger.info("      ⬅ BACKTRACK from " + cellLabel + " (removed piece " + pieceId + ") - dead-end at depth " + currentDepth);

        if (showBoard) {
            Map<PositionKey, Integer> placementOrderMapBefore = solver.buildPlacementOrderMap();
            int piecesOnBoard = placementOrderMapBefore.size();

            SolverLogger.info("");
            SolverLogger.info("╔═══════════════════════════════════════════════════════════╗");
            SolverLogger.info("║  ⬅ BACKTRACK: Removed piece " + pieceId + " from " + cellLabel);
            SolverLogger.info("║  Pieces now on board: " + piecesOnBoard);
            SolverLogger.info("║  Note: Placement # auto-updated (were #1-" + (piecesOnBoard + 1) + ", now #1-" + piecesOnBoard + ")");
            SolverLogger.info("╚═══════════════════════════════════════════════════════════╝");
            SolverLogger.info("");
            SolverLogger.info("Board AFTER backtrack (removed cell shown in RED ⬅):");
            SolverLogger.info("");

            List<Integer> unusedPieces = context.getUnusedPieces();
            Map<PositionKey, Integer> placementOrderMap = solver.buildPlacementOrderMap();
            int[] removedCell = new int[]{row, col};

            solver.printBoardWithLabels(context.board, context.piecesById, unusedPieces,
                                       null, null, placementOrderMap, removedCell);

            SolverLogger.info("");
            DebugHelper.waitForUserInput("Press ENTER to continue after backtrack...");
        }
    }

    /**
     * Logs dead-end when no piece can be placed.
     */
    public void logDeadEnd(int row, int col, int attemptCount,
                          EternitySolver solver, BacktrackingContext context) {
        String cellLabel = CellLabelFormatter.format(row, col);
        SolverLogger.info("");
        SolverLogger.info("╔═══════════════════════════════════════════════════════════╗");
        SolverLogger.info("║  ⚠️  DEAD-END at " + cellLabel + " - No valid piece found!");
        SolverLogger.info("║  Tried: " + attemptCount + " placement attempts");
        SolverLogger.info("║  Action: Backtracking to previous cell...");
        SolverLogger.info("╚═══════════════════════════════════════════════════════════╝");

        if (showBoard) {
            SolverLogger.info("");
            SolverLogger.info("Board at DEAD-END (failed cell " + cellLabel + " in RED):");
            SolverLogger.info("");

            List<Integer> unusedPieces = context.getUnusedPieces();
            Map<PositionKey, Integer> placementOrderMap = solver.buildPlacementOrderMap();
            int[] deadEndCell = new int[]{row, col};

            solver.printBoardWithLabels(context.board, context.piecesById, unusedPieces,
                                       null, null, placementOrderMap, deadEndCell);

            SolverLogger.info("");
            DebugHelper.waitForUserInput("Press ENTER to backtrack from dead-end...");
        }
    }
}
