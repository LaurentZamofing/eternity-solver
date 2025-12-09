package util;

import util.SolverLogger;

import model.Board;
import model.Piece;
import solver.EternitySolver;

import java.util.Map;

/**
 * Utility class for comparing solver performance with different configurations.
 * Extracted from Main.java to improve maintainability.
 *
 * Provides methods to run comparative benchmarks between different
 * solver optimization strategies (e.g., with/without singletons).
 *
 * @author Eternity Solver Team
 * @version 1.0.0
 */
public final class ComparisonAnalyzer {

    // Prevent instantiation
    private ComparisonAnalyzer() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    /**
     * Compares performance with and without singleton optimization on a 5x5 puzzle.
     * Runs two solver instances and displays comparative statistics.
     */
    public static void compareWithAndWithoutSingletons() {
        int rows = 5, cols = 5;
        Map<Integer, Piece> pieces = PuzzleFactory.createExample5x5();

        SolverLogger.info("╔══════════════════════════════════════════════════════════╗");
        SolverLogger.info("║   COMPARISON 5x5 : WITH vs WITHOUT SINGLETON optimization ║");
        SolverLogger.info("╚══════════════════════════════════════════════════════════╝\n");

        // ===== WITH SINGLETONS =====
        SolverLogger.info("\n█████████████████████████████████████████████████████████");
        SolverLogger.info("█  TEST 1 : WITH SINGLETON optimization                 █");
        SolverLogger.info("█████████████████████████████████████████████████████████\n");

        Board board1 = new Board(rows, cols);
        EternitySolver solver1 = new EternitySolver();
        solver1.setUseSingletons(true);

        boolean solved1 = solver1.solve(board1, pieces);
        EternitySolver.Statistics stats1 = solver1.getStatistics();

        if (!solved1) {
            System.out.println("⚠ No solution found (with singletons)");
        }

        // ===== WITHOUT SINGLETONS =====
        SolverLogger.info("\n\n█████████████████████████████████████████████████████████");
        System.out.println("█  TEST 2 : WITHOUT SINGLETON optimization (MRV only)      █");
        SolverLogger.info("█████████████████████████████████████████████████████████\n");

        Board board2 = new Board(rows, cols);
        EternitySolver solver2 = new EternitySolver();
        solver2.setUseSingletons(false);

        boolean solved2 = solver2.solve(board2, pieces);
        EternitySolver.Statistics stats2 = solver2.getStatistics();

        if (!solved2) {
            System.out.println("⚠ No solution found (without singletons)");
        }

        // ===== COMPARISON =====
        SolverLogger.info("\n\n╔══════════════════════════════════════════════════════════╗");
        SolverLogger.info("║                   COMPARATIVE SUMMARY                      ║");
        SolverLogger.info("╚══════════════════════════════════════════════════════════╝\n");

        SolverLogger.info("┌──────────────────────────┬─────────────────┬─────────────────┬─────────────┐");
        SolverLogger.info("│ Metric                   │ WITH Singleton  │ WITHOUT Singleton  │ Gain        │");
        SolverLogger.info("├──────────────────────────┼─────────────────┼─────────────────┼─────────────┤");

        printComparisonRow("Time (seconds)",
            stats1.getElapsedTimeSec(), stats2.getElapsedTimeSec());
        printComparisonRow("Recursive calls",
            stats1.recursiveCalls, stats2.recursiveCalls);
        printComparisonRow("Placements tested",
            stats1.placements, stats2.placements);
        printComparisonRow("Backtracks",
            stats1.backtracks, stats2.backtracks);
        printComparisonRow("fit() checks",
            stats1.fitChecks, stats2.fitChecks);

        SolverLogger.info("└──────────────────────────┴─────────────────┴─────────────────┴─────────────┘\n");

        System.out.println("Singletons detected (test 1) : " + stats1.singletonsFound);
        System.out.println("Singletons placed (test 1)    : " + stats1.singletonsPlaced);
        System.out.println("\nDead-ends (test 1) : " + stats1.deadEndsDetected);
        System.out.println("Dead-ends (test 2) : " + stats2.deadEndsDetected);
    }

    /**
     * Displays a comparison row in the table.
     *
     * @param label Metric label
     * @param val1 Value for first test (WITH optimization)
     * @param val2 Value for second test (WITHOUT optimization)
     */
    private static void printComparisonRow(String label, double val1, double val2) {
        double gain = val2 > 0 ? ((val2 - val1) / val2) * 100 : 0;
        String gainStr = String.format("%.1f%%", gain);
        if (gain > 0) {
            gainStr = "↓ " + gainStr;
        } else if (gain < 0) {
            gainStr = "↑ " + String.format("%.1f%%", Math.abs(gain));
        } else {
            gainStr = "=";
        }

        System.out.printf("│ %-24s │ %15.2f │ %15.2f │ %11s │%n",
            label, val1, val2, gainStr);
    }

    /**
     * Displays a comparison row in the table (int version).
     *
     * @param label Metric label
     * @param val1 Value for first test
     * @param val2 Value for second test
     */
    private static void printComparisonRow(String label, int val1, int val2) {
        printComparisonRow(label, (double)val1, (double)val2);
    }
}
