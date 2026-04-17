package solver;

import model.Board;
import model.Piece;
import solver.heuristics.LeastConstrainingValueOrderer;
import solver.heuristics.MRVCellSelector;
import util.DebugHelper;
import util.SaveStateManager;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for initializing solver helper classes and their dependencies.
 * Extracted from EternitySolver to eliminate code duplication.
 */
public class SolverInitializer {

    /**
     * Container for all initialized helper objects.
     */
    public static class InitializedComponents {
        public DomainManager domainManager;
        public ConstraintPropagator constraintPropagator;
        public SingletonDetector singletonDetector;
        public MRVCellSelector cellSelector;
        public LeastConstrainingValueOrderer valueOrderer;
        public PlacementValidator validator;
        public BoardDisplayManager displayManager;
        public CellConstraints[][] cellConstraints;
        public NeighborAnalyzer neighborAnalyzer;
        public PieceOrderingOptimizer pieceOrderingOptimizer;
    }

    private final EternitySolver solver;
    private final EternitySolver.Statistics stats;
    private final String sortOrder;
    private final boolean verbose;
    private final boolean prioritizeBorders;
    private final java.util.Set<util.PositionKey> fixedPositions;
    private final boolean debugBacktracking;
    private final boolean debugShowBoard;
    private final boolean debugShowAlternatives;
    private final int debugMaxCandidates;

    /**
     * Constructor for SolverInitializer.
     *
     * @param solver the EternitySolver instance (for fit checking)
     * @param stats statistics manager
     * @param sortOrder piece sorting order ("ascending" or "descending")
     * @param verbose enable verbose output or not
     * @param prioritizeBorders prioritize border cells or not
     * @param fixedPositions set of fixed positions
     */
    public SolverInitializer(EternitySolver solver, EternitySolver.Statistics stats, String sortOrder,
                            boolean verbose, boolean prioritizeBorders, java.util.Set<util.PositionKey> fixedPositions) {
        this(solver, stats, sortOrder, verbose, prioritizeBorders, fixedPositions,
             false, false, false, 5, false);
    }

    /**
     * Constructor for SolverInitializer with debug options.
     *
     * @param solver the EternitySolver instance (for fit checking)
     * @param stats statistics manager
     * @param sortOrder piece sorting order ("ascending" or "descending")
     * @param verbose enable verbose output or not
     * @param prioritizeBorders prioritize border cells or not
     * @param fixedPositions set of fixed positions
     * @param debugBacktracking enable debug backtracking logs
     * @param debugShowBoard show board after each placement
     * @param debugShowAlternatives show alternative candidates
     * @param debugMaxCandidates max number of alternatives to show
     * @param debugStepByStep enable step-by-step mode with pauses
     */
    public SolverInitializer(EternitySolver solver, EternitySolver.Statistics stats, String sortOrder,
                            boolean verbose, boolean prioritizeBorders, java.util.Set<util.PositionKey> fixedPositions,
                            boolean debugBacktracking, boolean debugShowBoard, boolean debugShowAlternatives,
                            int debugMaxCandidates, boolean debugStepByStep) {
        this.solver = solver;
        this.stats = stats;
        this.sortOrder = sortOrder;
        this.verbose = verbose;
        this.prioritizeBorders = prioritizeBorders;
        this.fixedPositions = fixedPositions != null ? fixedPositions : new java.util.HashSet<>();
        this.debugBacktracking = debugBacktracking;
        this.debugShowBoard = debugShowBoard;
        this.debugShowAlternatives = debugShowAlternatives;
        this.debugMaxCandidates = debugMaxCandidates;

        // Initialize DebugHelper with step-by-step mode
        DebugHelper.setStepByStepMode(debugStepByStep);
    }

    /**
     * Initializes all helper components for solving.
     *
     * @param board the puzzle board
     * @param pieces map of all pieces
     * @param pieceUsed bitset tracking piece usage
     * @param totalPieces total number of pieces
     * @return container of initialized components
     */
    public InitializedComponents initializeComponents(Board board, Map<Integer, Piece> pieces,
                                                      BitSet pieceUsed, int totalPieces) {
        InitializedComponents components = new InitializedComponents();

        // Pre-compute cell constraints for optimization (CRITICAL: must be before AC-3 init!)
        components.cellConstraints = CellConstraints.createConstraintsMatrix(board.getRows(), board.getCols());

        // Initialize PlacementValidator with constraints
        components.validator = new PlacementValidator(components.cellConstraints, stats, sortOrder);

        // Initialize BoardDisplayManager
        components.displayManager = new BoardDisplayManager(fixedPositions, components.validator);

        // Initialize extracted helper classes with proper dependency injection
        // Create FitChecker lambda that delegates to solver's fits() method
        DomainManager.FitChecker fitChecker = solver::fits;

        components.domainManager = new DomainManager(fitChecker);
        components.domainManager.setSortOrder(sortOrder);
        // MRV priority-queue index disabled by default. Empirically a 30%
        // regression on 4x4 (PQ overhead > scan cost on small boards). The
        // expected crossover is 6x6+. Benchmarks / callers flip it via
        // EternitySolver.setMRVIndexEnabled(true).
        components.domainManager.setMRVIndexEnabled(solver.isMRVIndexEnabled());

        // Create Statistics adapter for ConstraintPropagator
        ConstraintPropagator.Statistics cpStats = new ConstraintPropagator.Statistics() {
            public void incrementDeadEnds() {
                stats.deadEndsDetected++;
            }
            public long getDeadEndsDetected() {
                return stats.deadEndsDetected;
            }
        };
        components.constraintPropagator = new ConstraintPropagator(components.domainManager, cpStats);

        // Create Statistics adapter for SingletonDetector
        SingletonDetector.Statistics sdStats = new SingletonDetector.Statistics() {
            public void incrementSingletonsFound() {
                stats.singletonsFound++;
            }
            public void incrementDeadEnds() {
                stats.deadEndsDetected++;
            }
            public long getSingletonsFound() {
                return stats.singletonsFound;
            }
            public long getDeadEndsDetected() {
                return stats.deadEndsDetected;
            }
        };

        // Create FitChecker adapter for SingletonDetector
        SingletonDetector.FitChecker sdFitChecker = solver::fits;
        components.singletonDetector = new SingletonDetector(sdFitChecker, sdStats, verbose);
        components.singletonDetector.setDebugBacktracking(debugBacktracking);

        components.valueOrderer = new LeastConstrainingValueOrderer(verbose);

        // Build edge compatibility tables (optimization)
        components.valueOrderer.buildEdgeCompatibilityTables(pieces);

        // Calculate piece difficulty scores (optimization)
        components.valueOrderer.computePieceDifficulty(pieces);

        // Create EdgeCompatibilityIndex for NeighborAnalyzer and PieceOrderingOptimizer
        EdgeCompatibilityIndex edgeIndex = new EdgeCompatibilityIndex(pieces, false);

        // Initialize NeighborAnalyzer
        components.neighborAnalyzer = new NeighborAnalyzer(components.cellConstraints,
            components.validator, edgeIndex);

        // Initialize PieceOrderingOptimizer
        components.pieceOrderingOptimizer = new PieceOrderingOptimizer(
            edgeIndex, components.validator, components.neighborAnalyzer);

        components.cellSelector = new MRVCellSelector(components.domainManager,
            (b, row, col, edges) -> solver.fits(b, row, col, edges),
            components.neighborAnalyzer);
        components.cellSelector.setPrioritizeBorders(prioritizeBorders);
        components.cellSelector.setDebugBacktracking(debugBacktracking);
        components.cellSelector.setDebugShowAlternatives(debugShowAlternatives);
        components.cellSelector.setDebugMaxCandidates(debugMaxCandidates);

        // Note: AC-3 initialization is done by the caller after component assignment
        // to ensure that this.validator is defined before calling fits()

        return components;
    }
}
