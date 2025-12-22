package app;

import config.PuzzleConfig;
import util.SolverLogger;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.SaveStateManager;
import util.TimeConstants;

import java.io.IOException;
import java.util.Map;

/**
 * Quick test for p02_descending_border configuration
 */
public class TestP02 {

    public static void main(String[] args) {
        try {
            SolverLogger.info("Loading eternity2_p02_descending_border...");

            // Load the configuration
            PuzzleConfig config = PuzzleConfig.loadFromFile("data/eternity2/eternity2_p02_descending_border.txt");

            if (config == null) {
                SolverLogger.error("Failed to load configuration");
                System.exit(1);
            }

            // Create board
            Board board = new Board(config.getRows(), config.getCols());
            Map<Integer, Piece> pieces = config.getPieces();

            // Place fixed pieces
            for (PuzzleConfig.FixedPiece fixed : config.getFixedPieces()) {
                Piece piece = pieces.get(fixed.pieceId);
                if (piece != null) {
                    board.place(fixed.row, fixed.col, piece, fixed.rotation);
                }
            }

            SolverLogger.info("Configuration loaded successfully");
            SolverLogger.info("Board size: " + board.getRows() + "x" + board.getCols());
            SolverLogger.info("Pieces: " + pieces.size());
            SolverLogger.info("Fixed pieces: " + config.getFixedPieces().size());
            SolverLogger.info("");

            // Create solver with AC-3
            EternitySolver solver = new EternitySolver();
            solver.setUseSingletons(true);

            SolverLogger.info("Starting solver (will run for 30 seconds max)...");
            SolverLogger.info("");

            long startTime = System.currentTimeMillis();

            // Simple solve - the solver should detect dead ends quickly with AC-3
            boolean solved = solver.solve(board, pieces);

            long duration = System.currentTimeMillis() - startTime;

            SolverLogger.info("");
            SolverLogger.info("═══════════════════════════════════════");
            SolverLogger.info("Solver finished");
            SolverLogger.info("Time: " + (duration / (double)TimeConstants.MILLIS_PER_SECOND) + " seconds");
            SolverLogger.info("Solved: " + solved);

            EternitySolver.Statistics stats = solver.getStatistics();
            SolverLogger.info("");
            SolverLogger.info("Statistics:");
            SolverLogger.info("  Recursive calls: " + stats.recursiveCalls);
            SolverLogger.info("  Placements: " + stats.placements);
            SolverLogger.info("  Backtracks: " + stats.backtracks);
            SolverLogger.info("  Dead ends detected: " + stats.deadEndsDetected);
            SolverLogger.info("═══════════════════════════════════════");

        } catch (IOException e) {
            SolverLogger.error("Error: " + e.getMessage());
            SolverLogger.error("Error occurred", e);
            System.exit(1);
        }
    }
}
