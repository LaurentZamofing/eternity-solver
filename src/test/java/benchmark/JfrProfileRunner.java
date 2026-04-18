package benchmark;

import model.Board;
import solver.EternitySolver;
import util.PuzzleGenerator;

/**
 * Tiny runner that solves an 8×8 generated puzzle under Java Flight
 * Recorder. Intended to be invoked manually with
 * {@code -XX:StartFlightRecording=filename=solve-8x8.jfr,duration=30s,settings=profile}
 * so BB2 (scaling/allocation) and BB3 (parallel contention) can be
 * investigated with real data.
 *
 * <p>Not a JUnit test — {@code java benchmark.JfrProfileRunner} runs it
 * as a regular main. It's under src/test to stay out of the production
 * artifact.</p>
 */
public final class JfrProfileRunner {

    public static void main(String[] args) {
        int seed = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        int size = args.length > 1 ? Integer.parseInt(args[1]) : 8;
        int colours = args.length > 2 ? Integer.parseInt(args[2]) : 7;

        System.out.printf("Solving generated %dx%d puzzle (seed=%d, colours=%d)%n",
            size, size, seed, colours);

        Board board = new Board(size, size);
        EternitySolver solver = EternitySolver.builder()
            .verbose(false)
            .maxExecutionTime(300_000L)
            .symmetryBreakingFlags(false, false)
            .build();

        long t0 = System.currentTimeMillis();
        boolean solved = solver.solve(board, PuzzleGenerator.generate(size, colours, seed));
        long elapsed = System.currentTimeMillis() - t0;
        System.out.printf("solved=%s elapsed=%dms%n", solved, elapsed);
    }
}
