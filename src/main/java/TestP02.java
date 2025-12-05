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
            System.out.println("Loading eternity2_p02_descending_border...");

            // Load the configuration
            PuzzleConfig config = PuzzleConfig.loadFromFile("data/eternity2/eternity2_p02_descending_border.txt");

            if (config == null) {
                System.err.println("Failed to load configuration");
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

            System.out.println("Configuration loaded successfully");
            System.out.println("Board size: " + board.getRows() + "x" + board.getCols());
            System.out.println("Pieces: " + pieces.size());
            System.out.println("Fixed pieces: " + config.getFixedPieces().size());
            System.out.println();

            // Create solver with AC-3
            EternitySolver solver = new EternitySolver();
            solver.setUseSingletons(true);

            System.out.println("Starting solver (will run for 30 seconds max)...");
            System.out.println();

            long startTime = System.currentTimeMillis();

            // Simple solve - the solver should detect dead ends quickly with AC-3
            boolean solved = solver.solve(board, pieces);

            long duration = System.currentTimeMillis() - startTime;

            System.out.println();
            System.out.println("═══════════════════════════════════════");
            System.out.println("Solver finished");
            System.out.println("Time: " + (duration / (double)TimeConstants.MILLIS_PER_SECOND) + " seconds");
            System.out.println("Solved: " + solved);

            EternitySolver.Statistics stats = solver.getStatistics();
            System.out.println();
            System.out.println("Statistics:");
            System.out.println("  Recursive calls: " + stats.recursiveCalls);
            System.out.println("  Placements: " + stats.placements);
            System.out.println("  Backtracks: " + stats.backtracks);
            System.out.println("  Dead ends detected: " + stats.deadEndsDetected);
            System.out.println("═══════════════════════════════════════");

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
