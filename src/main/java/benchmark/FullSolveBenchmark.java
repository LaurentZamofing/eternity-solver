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
import util.PuzzleGenerator;

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
    private Map<Integer, Piece> pieces5x5;
    private Map<Integer, Piece> pieces6x6;
    private Map<Integer, Piece> pieces8x8;

    @Setup(Level.Trial)
    public void loadPuzzles() {
        pieces3x3 = PuzzleFactory.createExample3x3();
        pieces4x4Hard = PuzzleFactory.createExample4x4HardV3();
        // Generated puzzles for scaling bench — deterministic via seed so
        // results are reproducible across runs. Colour palette grows with
        // size to keep the search non-trivial (more colours = harder).
        pieces5x5 = PuzzleGenerator.generate(5, 5, 5L);
        pieces6x6 = PuzzleGenerator.generate(6, 6, 6L);
        pieces8x8 = PuzzleGenerator.generate(8, 7, 8L);
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

    /** Generated 5×5 — smooth scaling curve between 4×4 and 6×6. */
    @Benchmark
    public boolean solve5x5Generated() {
        Board board = new Board(5, 5);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(60_000);
        return solver.solve(board, pieces5x5);
    }

    /** Generated 6×6 — the expected crossover where MRV priority-queue
     *  index starts to beat the linear scan (M3 in IMPROVEMENT_PLAN.md). */
    @Benchmark
    public boolean solve6x6Generated() {
        Board board = new Board(6, 6);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(120_000);
        return solver.solve(board, pieces6x6);
    }

    /** Generated 6×6 with MRV priority-queue index ON — pair this with
     *  solve6x6Generated to measure the PQ speedup in JMH output. */
    @Benchmark
    public boolean solve6x6GeneratedWithMRVIndex() {
        Board board = new Board(6, 6);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(120_000);
        solver.setMRVIndexEnabled(true);
        return solver.solve(board, pieces6x6);
    }

    /** Generated 8×8 — stress test approaching the 16×16 target. Large
     *  search space, used to validate Chantier 5 BB2 scaling work. */
    @Benchmark
    public boolean solve8x8Generated() {
        Board board = new Board(8, 8);
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(false);
        solver.setMaxExecutionTime(300_000);
        return solver.solve(board, pieces8x8);
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
