package solver;

import model.Board;
import model.Piece;
import solver.heuristics.*;
import util.SaveStateManager;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Responsable de l'initialisation des classes auxiliaires du solveur et de leurs dépendances.
 * Extrait de EternitySolver pour éliminer la duplication de code.
 */
public class SolverInitializer {

    /**
     * Conteneur pour tous les objets auxiliaires initialisés.
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
     * Constructeur pour SolverInitializer.
     *
     * @param solver l'instance EternitySolver (pour la vérification d'ajustement)
     * @param stats gestionnaire de statistiques
     * @param sortOrder ordre de tri des pièces ("ascending" ou "descending")
     * @param verbose activer la sortie détaillée ou non
     * @param prioritizeBorders prioriser les cellules de bord ou non
     * @param fixedPositions ensemble des positions fixes
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
     * Initialise tous les composants auxiliaires pour la résolution.
     *
     * @param board le plateau de puzzle
     * @param pieces map de toutes les pièces
     * @param pieceUsed bitset suivant l'utilisation des pièces
     * @param totalPieces nombre total de pièces
     * @return conteneur de composants initialisés
     */
    public InitializedComponents initializeComponents(Board board, Map<Integer, Piece> pieces,
                                                      BitSet pieceUsed, int totalPieces) {
        InitializedComponents components = new InitializedComponents();

        // Pré-calculer les contraintes de cellule pour optimisation (CRITIQUE : doit être avant l'init AC-3 !)
        components.cellConstraints = CellConstraints.createConstraintsMatrix(board.getRows(), board.getCols());

        // Initialiser PlacementValidator avec les contraintes
        components.validator = new PlacementValidator(components.cellConstraints, stats, sortOrder);

        // Initialiser BoardDisplayManager
        components.displayManager = new BoardDisplayManager(fixedPositions, components.validator);

        // Initialiser les classes auxiliaires extraites avec injection de dépendances appropriée
        // Créer lambda FitChecker qui délègue à la méthode fits() du solveur
        DomainManager.FitChecker fitChecker = solver::fits;

        components.domainManager = new DomainManager(fitChecker);
        components.domainManager.setSortOrder(sortOrder);

        // Créer l'adaptateur Statistics pour ConstraintPropagator
        ConstraintPropagator.Statistics cpStats = new ConstraintPropagator.Statistics() {
            public void incrementDeadEnds() {
                stats.deadEndsDetected++;
            }
            public long getDeadEndsDetected() {
                return stats.deadEndsDetected;
            }
        };
        components.constraintPropagator = new ConstraintPropagator(components.domainManager, cpStats);

        // Créer l'adaptateur Statistics pour SingletonDetector
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

        // Créer l'adaptateur FitChecker pour SingletonDetector
        SingletonDetector.FitChecker sdFitChecker = solver::fits;
        components.singletonDetector = new SingletonDetector(sdFitChecker, sdStats, verbose);

        components.valueOrderer = new LeastConstrainingValueOrderer(verbose);

        // Construire les tables de compatibilité des bords (optimisation)
        components.valueOrderer.buildEdgeCompatibilityTables(pieces);

        // Calculer les scores de difficulté des pièces (optimisation)
        components.valueOrderer.computePieceDifficulty(pieces);

        // Créer EdgeCompatibilityIndex pour NeighborAnalyzer et PieceOrderingOptimizer
        EdgeCompatibilityIndex edgeIndex = new EdgeCompatibilityIndex(pieces, false);

        // Initialiser NeighborAnalyzer
        components.neighborAnalyzer = new NeighborAnalyzer(components.cellConstraints,
            components.validator, edgeIndex);

        // Initialiser PieceOrderingOptimizer
        components.pieceOrderingOptimizer = new PieceOrderingOptimizer(
            edgeIndex, components.validator, components.neighborAnalyzer);

        components.cellSelector = new MRVCellSelector(components.domainManager,
            (b, row, col, edges) -> solver.fits(b, row, col, edges),
            components.neighborAnalyzer);
        components.cellSelector.setPrioritizeBorders(prioritizeBorders);

        // Note : L'initialisation AC-3 est faite par l'appelant après l'assignation des composants
        // pour garantir que this.validator est défini avant d'appeler fits()

        return components;
    }
}
