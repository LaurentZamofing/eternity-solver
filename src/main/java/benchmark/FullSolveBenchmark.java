package benchmark;

import model.Board;
import model.Piece;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import solver.EternitySolver;
import util.PuzzleFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * End-to-end benchmark: solve a complete 3x3 puzzle from an empty board.
 *
 * Exercises every hot path (MRV selection, AC-3 propagation, singleton
 * detection, placement/backtrack) in realistic proportions. Use this
 * to validate perf claims that a micro-benchmark can't — e.g. the
 * Piece.edgesRotated cache, which helps most when it's called millions
 * of times over a full solve.
 *
 * {@code SingleShotTime} with many iterations avoids amortizing the
 * per-solve JIT warmup across invocations.
 *
 * Run:
 *   mvn -q compile && mvn exec:java \
 *     -Dexec.mainClass=benchmark.FullSolveBenchmark \
 *     -Dexec.classpathScope=compile
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 10)
@Fork(0)
public class FullSolveBenchmark {

    private Map<Integer, Piece> pieces3x3;
    private Map<Integer, Piece> pieces4x4Hard;

    @Setup(Level.Trial)
    public void loadPuzzles() {
        pieces3x3 = PuzzleFactory.createExample3x3();
        pieces4x4Hard = PuzzleFactory.createExample4x4HardV3();
    }

    @Benchmark
    public boolean solve3x3() {
        Board board = new Board(3, 3);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(30_000);
        return solver.solve(board, pieces3x3);
    }

    /**
     * 4x4 hard variant — bigger board where MRV cell-selection cost actually
     * dominates the per-decision overhead. Used to validate the MRV
     * priority-queue optimization (gain on 3x3 is invisible since the legacy
     * scan only inspects 9 cells).
     */
    @Benchmark
    public boolean solve4x4Hard() {
        Board board = new Board(4, 4);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(30_000);
        return solver.solve(board, pieces4x4Hard);
    }

    /**
     * Entry point.
     *
     * <p>Accepts an optional path arg to write JSON results — the CI perf-gate
     * workflow passes a file path so it can compare against the baseline.
     * Without args, results go to stdout only.</p>
     */
    public static void main(String[] args) throws RunnerException {
        OptionsBuilder builder = (OptionsBuilder) new OptionsBuilder()
            .include(FullSolveBenchmark.class.getSimpleName());
        if (args.length > 0 && !args[0].isEmpty()) {
            builder.resultFormat(ResultFormatType.JSON).result(args[0]);
        }
        new Runner(builder.build()).run();
    }
}
