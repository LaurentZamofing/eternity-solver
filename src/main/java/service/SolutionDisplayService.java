package service;

import config.PuzzleConfig;

// SolutionDisplayService.java (moved to default package for MainSequential access)

import model.Board;
import model.Piece;
import model.Placement;
import solver.EternitySolver;
import util.SolverLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for displaying puzzle solutions.
 * Provides both simple and detailed display formats.
 */
public class SolutionDisplayService {

    /**
     * Display solution based on puzzle size.
     * Small puzzles (≤72 pieces) get detailed display with edges.
     * Large puzzles get simple ID-only display.
     *
     * @param board Board with solution
     * @param allPieces All pieces in the puzzle
     * @param totalPieces Total number of pieces
     */
    public void displaySolution(Board board, Map<Integer, Piece> allPieces, int totalPieces) {
        if (totalPieces <= 72) {
            displayDetailedSolution(board, allPieces);
        } else {
            displaySimpleSolution(board);
        }
    }

    /**
     * Display simple solution (piece IDs only in grid).
     * Used for large puzzles to avoid cluttering output.
     *
     * @param board Board with solution
     */
    public void displaySimpleSolution(Board board) {
        int rows = board.getRows();
        int cols = board.getCols();

        SolverLogger.info("  Solution:");
        SolverLogger.info("  ┌" + "─".repeat(cols * 4 + 1) + "┐");

        for (int r = 0; r < rows; r++) {
            StringBuilder line = new StringBuilder("  │");
            for (int c = 0; c < cols; c++) {
                Placement p = board.getPlacement(r, c);
                if (p != null) {
                    line.append(String.format(" %3d", p.getPieceId()));
                } else {
                    line.append("  · ");
                }
            }
            line.append(" │");
            SolverLogger.info(line.toString());
        }

        SolverLogger.info("  └" + "─".repeat(cols * 4 + 1) + "┘");
    }

    /**
     * Display detailed solution with piece edges.
     * Shows each piece's ID and edge values (N/E/S/W).
     * Color-codes edges: green for matches, red for mismatches.
     *
     * @param board Board with solution
     * @param allPieces All pieces in the puzzle
     */
    public void displayDetailedSolution(Board board, Map<Integer, Piece> allPieces) {
        SolverLogger.info("╔═══════════════════════════════════════════════════════════════════╗");
        SolverLogger.info("║                        SOLUTION FOUND                            ║");
        SolverLogger.info("╚═══════════════════════════════════════════════════════════════════╝");
        SolverLogger.info("");
        SolverLogger.info("Legend:");
        SolverLogger.info("  - Each piece displays: Piece ID with edge values (N/E/S/W)");
        SolverLogger.info("  - \033[32mGreen\033[0m: edges that match with neighbors");
        SolverLogger.info("  - \033[91mRed\033[0m: edges that do NOT match (error!)");
        SolverLogger.info("");

        // Create a temporary solver to use its display method
        EternitySolver tempSolver = new EternitySolver();
        List<Integer> emptyList = new ArrayList<>();
        tempSolver.printBoardWithLabels(board, allPieces, emptyList);

        SolverLogger.info("");
        board.printScore();
        SolverLogger.info("");
        SolverLogger.info("═".repeat(70));
    }

    /**
     * Display best solution reached with comparison to current state.
     * Shows differences between record state and current state using color coding.
     *
     * @param bestBoard Best board state reached
     * @param currentBoard Current board state
     * @param bestPieces Pieces for best state
     * @param bestUnusedIds Unused pieces in best state
     */
    public void displayBestSolution(Board bestBoard, Board currentBoard,
                                     Map<Integer, Piece> bestPieces,
                                     List<Integer> bestUnusedIds) {
        SolverLogger.info("╔═══════════════════════════════════════════════════════════════════╗");
        SolverLogger.info("║              BEST SOLUTION REACHED (RECORD)                      ║");
        SolverLogger.info("╚═══════════════════════════════════════════════════════════════════╝");
        SolverLogger.info("");
        SolverLogger.info("State with the most pieces placed so far:");
        SolverLogger.info("");
        SolverLogger.info("Color legend (RECORD vs CURRENT comparison):");
        SolverLogger.info("  - \033[1;35mMagenta\033[0m: Cell occupied in RECORD but empty in CURRENT (regression)");
        SolverLogger.info("  - \033[1;38;5;208mOrange\033[0m: Different piece between RECORD and CURRENT (change)");
        SolverLogger.info("  - \033[1;33mYellow\033[0m: Cell empty in RECORD but occupied in CURRENT (progression)");
        SolverLogger.info("  - \033[1;36mCyan\033[0m: Identical cell in RECORD and CURRENT (stability)");
        SolverLogger.info("");

        // Create a temporary solver for display with comparison
        EternitySolver tempSolver = new EternitySolver();
        tempSolver.printBoardWithComparison(bestBoard, currentBoard, bestPieces, bestUnusedIds);

        SolverLogger.info("");
        bestBoard.printScore();
        SolverLogger.info("");
        SolverLogger.info("═".repeat(70));
        SolverLogger.info("");
    }

    /**
     * Display current puzzle state for validation.
     * Shows all placed pieces with edge values and empty cells with possibility counts.
     *
     * @param board Current board state
     * @param allPieces All pieces in the puzzle
     * @param unusedIds Unused piece IDs
     */
    public void displayPuzzleStateForValidation(Board board, Map<Integer, Piece> allPieces,
                                                 List<Integer> unusedIds) {
        SolverLogger.info("╔═══════════════════════════════════════════════════════════════════╗");
        SolverLogger.info("║              LOADED PUZZLE STATE (VALIDATION)                    ║");
        SolverLogger.info("╚═══════════════════════════════════════════════════════════════════╝");
        SolverLogger.info("");
        SolverLogger.info("Legend:");
        SolverLogger.info("  - Placed pieces: Piece ID with edge values (N/E/S/W)");
        SolverLogger.info("  - Empty cells: (XXX) = number of valid pieces possible");
        SolverLogger.info("  - \033[93mYellow\033[0m: critical cells (≤20 possibilities)");
        SolverLogger.info("  - \033[1;91mRed\033[0m: dead-end (0 possibilities)");
        SolverLogger.info("");

        EternitySolver tempSolver = new EternitySolver();
        tempSolver.printBoardWithLabels(board, allPieces, unusedIds);

        SolverLogger.info("");
        board.printScore();
        SolverLogger.info("");
        SolverLogger.info("═".repeat(70));
        SolverLogger.info("");
    }
}
