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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import solver.ConstraintPropagator;
import solver.DomainManager;
import util.PuzzleFactory;

import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Micro-benchmark for ConstraintPropagator.propagateAC3.
 *
 * Loads a 4x4 puzzle, places 2 corner pieces to create real constraints,
 * initializes AC-3 domains, then on each benchmark invocation triggers
 * a cascading propagation. The invocation-level setup rebuilds the
 * domain manager to guarantee identical input state across iterations.
 *
 * Run with:
 *   mvn -q compile exec:java -Dexec.mainClass=benchmark.AC3PropagationBenchmark -Dexec.classpathScope=compile
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(0) // run in-process so mvn exec:java has the classpath; perf numbers are indicative, not publication-grade
public class AC3PropagationBenchmark {

    private Map<Integer, Piece> pieces;
    private int totalPieces;
    private int placedPieceId;
    private int placedRotation;

    // Rebuilt per invocation to restore domain state
    private Board board;
    private DomainManager domainManager;
    private ConstraintPropagator propagator;
    private BitSet pieceUsed;

    @Setup(Level.Trial)
    public void loadPuzzle() {
        pieces = PuzzleFactory.createExample4x4Easy();
        totalPieces = pieces.size();
        // Pick first two pieces to place at (0,0) and (0,3) corners
        placedPieceId = pieces.keySet().iterator().next();
        placedRotation = 0;
    }

    @Setup(Level.Invocation)
    public void resetState() {
        board = new Board(4, 4);
        pieceUsed = new BitSet(totalPieces + 1);
        ConstraintPropagator.Statistics stats = new ConstraintPropagator.Statistics();
        domainManager = new DomainManager(new DomainManager.FitChecker() {
            @Override
            public boolean fits(Board b, int r, int c, int[] edges) {
                // Loose fit check: allow edges that match the board's border convention.
                // For a micro-benchmark this is intentionally permissive so that the
                // initial domain is realistically large.
                int rows = b.getRows(), cols = b.getCols();
                if (r == 0 && edges[0] != 0) return false;
                if (r == rows - 1 && edges[2] != 0) return false;
                if (c == 0 && edges[3] != 0) return false;
                if (c == cols - 1 && edges[1] != 0) return false;
                if (r != 0 && edges[0] == 0) return false;
                if (r != rows - 1 && edges[2] == 0) return false;
                if (c != 0 && edges[3] == 0) return false;
                if (c != cols - 1 && edges[1] == 0) return false;
                return true;
            }
        });
        domainManager.initializeAC3Domains(board, pieces, pieceUsed, totalPieces);
        propagator = new ConstraintPropagator(domainManager, stats);
    }

    @Benchmark
    public boolean propagateAfterPlacement() {
        // Simulate a placement followed by propagation. We don't actually mutate
        // the board (which would affect domainManager's state), we just call
        // propagateAC3 with the parameters.
        return propagator.propagateAC3(board, 0, 0, placedPieceId, placedRotation,
                                       pieces, pieceUsed, totalPieces);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(AC3PropagationBenchmark.class.getSimpleName())
            .build();
        new Runner(opt).run();
    }
}
