package benchmark;

import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.PuzzleFactory;
import util.PuzzleGenerator;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Phase-A baseline: runs the current solver on generated puzzles of every
 * size from 3×3 to 8×8 with 3 seeds each, records wall-clock time, and
 * writes a JSON summary used as the "wall identification" input for
 * Phase B/C/D decisions.
 *
 * <p>Not a JUnit test. Invoke as {@code java benchmark.BenchmarkGrid}
 * (classpath is the same as {@link JfrProfileRunner}).</p>
 *
 * <p>Sizes 3 and 4 also run the real examples from {@link PuzzleFactory}
 * so the baseline covers both generated and hand-curated puzzles.</p>
 */
public final class BenchmarkGrid {

    private static final int[] SIZES = {3, 4, 5, 6, 7, 8};
    private static final long[] SEEDS = {1L, 17L, 42L};
    private static final long PER_RUN_TIMEOUT_MS = 120_000;

    public static void main(String[] args) throws IOException {
        Path outFile = Path.of(args.length > 0 ? args[0] : ".github/perf-baseline-grid.json");

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("timestamp", System.currentTimeMillis());
        results.put("description", "Phase-A baseline: solve times 3×3 → 8×8 with generated puzzles");

        // Hand-curated reference runs
        Map<String, Object> curated = new LinkedHashMap<>();
        curated.put("example_3x3", timeSolve(3, PuzzleFactory.createExample3x3()));
        curated.put("example_4x4_easy", timeSolve(4, PuzzleFactory.createExample4x4Easy()));
        curated.put("example_4x4_hardV3", timeSolve(4, PuzzleFactory.createExample4x4HardV3()));
        results.put("curated", curated);

        // Generated grid
        Map<String, Object> grid = new LinkedHashMap<>();
        for (int size : SIZES) {
            Map<String, Object> sizeRow = new LinkedHashMap<>();
            long totalMs = 0;
            int solved = 0;
            for (long seed : SEEDS) {
                Map<Integer, Piece> pieces = PuzzleGenerator.generate(size, paletteFor(size), seed);
                RunResult r = timeSolve(size, pieces);
                sizeRow.put("seed_" + seed, r.toMap());
                if (r.solved) { solved++; totalMs += r.elapsedMs; }
            }
            sizeRow.put("seeds_solved", solved);
            sizeRow.put("avg_ms_solved", solved == 0 ? null : totalMs / solved);
            grid.put("size_" + size, sizeRow);
        }
        results.put("generated", grid);

        Files.createDirectories(outFile.getParent());
        try (PrintWriter w = new PrintWriter(outFile.toFile(), "UTF-8")) {
            w.write(toJson(results, 0));
        }

        // Short human summary to stdout
        System.out.println("\n=== BenchmarkGrid summary ===");
        System.out.println("Curated:");
        for (Map.Entry<String, Object> e : curated.entrySet()) {
            System.out.println("  " + e.getKey() + " → " + e.getValue());
        }
        System.out.println("Generated:");
        for (int size : SIZES) {
            @SuppressWarnings("unchecked")
            Map<String, Object> sizeRow = (Map<String, Object>) grid.get("size_" + size);
            System.out.printf("  %dx%d: %d/3 seeds solved, avg %s ms%n",
                size, size, sizeRow.get("seeds_solved"), sizeRow.get("avg_ms_solved"));
        }
        System.out.println("Wrote: " + outFile.toAbsolutePath());
    }

    /** Colour palette size grows with puzzle size so search stays non-trivial. */
    private static int paletteFor(int size) {
        return Math.max(4, size - 1);
    }

    private static RunResult timeSolve(int size, Map<Integer, Piece> pieces) {
        Board board = new Board(size, size);
        EternitySolver solver = EternitySolver.builder()
            .verbose(false)
            .maxExecutionTime(PER_RUN_TIMEOUT_MS)
            .symmetryBreakingFlags(false, false)
            .build();
        long t0 = System.currentTimeMillis();
        boolean solved = solver.solve(board, pieces);
        long elapsed = System.currentTimeMillis() - t0;
        return new RunResult(size, solved, elapsed);
    }

    private static final class RunResult {
        final int size;
        final boolean solved;
        final long elapsedMs;

        RunResult(int size, boolean solved, long elapsedMs) {
            this.size = size;
            this.solved = solved;
            this.elapsedMs = elapsedMs;
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("solved", solved);
            m.put("elapsed_ms", elapsedMs);
            return m;
        }

        @Override public String toString() {
            return "solved=" + solved + " in " + elapsedMs + "ms";
        }
    }

    // Tiny JSON serialiser (no dependency).
    @SuppressWarnings("unchecked")
    private static String toJson(Object o, int indent) {
        StringBuilder sb = new StringBuilder();
        if (o instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) o;
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                sb.append("  ".repeat(indent + 1)).append('"').append(e.getKey()).append("\": ")
                  .append(toJson(e.getValue(), indent + 1));
                if (++i < m.size()) sb.append(',');
                sb.append('\n');
            }
            sb.append("  ".repeat(indent)).append('}');
        } else if (o instanceof String) {
            sb.append('"').append(((String) o).replace("\"", "\\\"")).append('"');
        } else if (o == null) {
            sb.append("null");
        } else {
            sb.append(o);
        }
        return sb.toString();
    }
}
