package solver;

import model.Board;
import model.Piece;
import solver.heuristics.LeastConstrainingValueOrderer;
import solver.heuristics.MRVCellSelector;
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
    private final Set<String> fixedPositions;

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
                            boolean verbose, boolean prioritizeBorders, Set<String> fixedPositions) {
        this.solver = solver;
        this.stats = stats;
        this.sortOrder = sortOrder;
        this.verbose = verbose;
        this.prioritizeBorders = prioritizeBorders;
        this.fixedPositions = fixedPositions != null ? fixedPositions : new HashSet<>();
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

        // Note: AC-3 initialization is done by the caller after component assignment
        // to ensure that this.validator is defined before calling fits()

        return components;
    }
}
