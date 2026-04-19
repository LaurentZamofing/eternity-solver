package solver;

/**
 * Fluent builder for {@link EternitySolver}.
 *
 * <p>Created 2026-04-18 (IMPROVEMENT_PLAN.md M4) to make solver
 * construction readable at call sites without moving 20 setters around.
 * The underlying {@link EternitySolver} keeps its regular setter API
 * for advanced knobs and for the many existing callers.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * EternitySolver solver = EternitySolver.builder()
 *     .verbose(false)
 *     .maxExecutionTime(20_000)
 *     .symmetryBreakingFlags(true, true)
 *     .build();
 * solver.solve(board, pieces);
 * }</pre>
 */
public final class EternitySolverBuilder {

    private boolean verbose = false;
    private long maxExecutionTimeMs = 30_000;
    private boolean useAC3 = true;
    private boolean mrvIndexEnabled = false;
    private boolean lexFlag = true;
    private boolean rotationFlag = true;
    private boolean useNogoods = true;
    private String sortOrder;
    private String puzzleName;
    private String threadLabel;

    EternitySolverBuilder() { /* created via EternitySolver.builder() */ }

    public EternitySolverBuilder verbose(boolean v) { this.verbose = v; return this; }
    public EternitySolverBuilder maxExecutionTime(long ms) { this.maxExecutionTimeMs = ms; return this; }
    public EternitySolverBuilder useAC3(boolean on) { this.useAC3 = on; return this; }
    public EternitySolverBuilder mrvIndexEnabled(boolean on) { this.mrvIndexEnabled = on; return this; }
    public EternitySolverBuilder useNogoods(boolean on) { this.useNogoods = on; return this; }

    /** Toggle both symmetry-breaking flags in one call. */
    public EternitySolverBuilder symmetryBreakingFlags(boolean lex, boolean rotation) {
        this.lexFlag = lex;
        this.rotationFlag = rotation;
        return this;
    }

    public EternitySolverBuilder sortOrder(String order) { this.sortOrder = order; return this; }
    public EternitySolverBuilder puzzleName(String name) { this.puzzleName = name; return this; }
    public EternitySolverBuilder threadLabel(String label) { this.threadLabel = label; return this; }

    /** Builds a configured {@link EternitySolver} instance. */
    public EternitySolver build() {
        EternitySolver solver = new EternitySolver();
        solver.setVerbose(verbose);
        solver.setMaxExecutionTime(maxExecutionTimeMs);
        solver.setUseAC3(useAC3);
        solver.setMRVIndexEnabled(mrvIndexEnabled);
        solver.setUseNogoods(useNogoods);
        solver.setSymmetryBreakingFlags(lexFlag, rotationFlag);
        if (sortOrder != null) solver.setSortOrder(sortOrder);
        if (puzzleName != null) solver.setPuzzleName(puzzleName);
        if (threadLabel != null) solver.setThreadLabel(threadLabel);
        return solver;
    }
}
