package solver;

import model.Board;
import model.Piece;
import util.SaveStateManager;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * DEPRECATED: Legacy facade for solver configuration.
 *
 * This class has been refactored into:
 * - {@link SolverConfiguration} - Immutable configuration data (use Builder pattern)
 * - {@link FixedPieceDetector} - Fixed piece detection logic
 * - Manager creation moved to managers themselves
 *
 * This class remains for backward compatibility but delegates to the new classes.
 * New code should use SolverConfiguration.builder() instead.
 *
 * @deprecated Use {@link SolverConfiguration} and {@link FixedPieceDetector} instead
 */
@Deprecated
public class ConfigurationManager {

    private final SolverConfiguration.Builder configBuilder;
    private final FixedPieceDetector fixedPieceDetector;

    public ConfigurationManager() {
        this.configBuilder = SolverConfiguration.builder();
        this.fixedPieceDetector = new FixedPieceDetector();
    }

    // ============ Configuration Setters (delegate to builder) ============

    public void setDisplayConfig(boolean verbose, int minDepth) {
        configBuilder.verbose(verbose).minDepthToShowRecords(minDepth);
    }

    public void setMinDepthToShowRecords(int minDepth) {
        configBuilder.minDepthToShowRecords(minDepth);
    }

    public void setPuzzleName(String name) {
        configBuilder.puzzleName(name);
    }

    public void setSortOrder(String order) {
        configBuilder.sortOrder(order);
    }

    public void setNumFixedPieces(int num) {
        configBuilder.numFixedPieces(num);
    }

    public void setMaxExecutionTime(long timeMs) {
        configBuilder.maxExecutionTimeMs(timeMs);
    }

    public void setThreadLabel(String label) {
        configBuilder.threadLabel(label);
    }

    public void setThreadId(int id) {
        configBuilder.threadId(id);
    }

    public void setUseSingletons(boolean enabled) {
        configBuilder.useSingletons(enabled);
    }

    public void setPrioritizeBorders(boolean enabled) {
        configBuilder.prioritizeBorders(enabled);
    }

    public void setVerbose(boolean enabled) {
        configBuilder.verbose(enabled);
    }

    public void setUseAC3(boolean enabled) {
        configBuilder.useAC3(enabled);
    }

    public void setUseDomainCache(boolean enabled) {
        configBuilder.useDomainCache(enabled);
    }

    public void setRandomSeed(long seed) {
        configBuilder.randomSeed(seed);
    }

    // ============ Configuration Getters (build and delegate) ============

    private SolverConfiguration getConfig() {
        return configBuilder.build();
    }

    public boolean isUseSingletons() {
        return getConfig().isUseSingletons();
    }

    public boolean isVerbose() {
        return getConfig().isVerbose();
    }

    public boolean isUseAC3() {
        return getConfig().isUseAC3();
    }

    public boolean isUseDomainCache() {
        return getConfig().isUseDomainCache();
    }

    public boolean isPrioritizeBorders() {
        return getConfig().isPrioritizeBorders();
    }

    public int getMinDepthToShowRecords() {
        return getConfig().getMinDepthToShowRecords();
    }

    public long getMaxExecutionTimeMs() {
        return getConfig().getMaxExecutionTimeMs();
    }

    public String getPuzzleName() {
        return getConfig().getPuzzleName();
    }

    public String getThreadLabel() {
        return getConfig().getThreadLabel();
    }

    public String getSortOrder() {
        return getConfig().getSortOrder();
    }

    public int getThreadId() {
        return getConfig().getThreadId();
    }

    public int getNumFixedPieces() {
        return getConfig().getNumFixedPieces();
    }

    public Set<String> getFixedPositions() {
        return getConfig().getFixedPositions();
    }

    public List<SaveStateManager.PlacementInfo> getInitialFixedPieces() {
        return getConfig().getInitialFixedPieces();
    }

    public long getRandomSeed() {
        return getConfig().getRandomSeed();
    }

    public static long getThreadSaveInterval() {
        return SolverConfiguration.getThreadSaveInterval();
    }

    // ============ Fixed Pieces Detection (delegate to detector) ============

    /**
     * Detects and initializes fixed pieces from board state.
     * @deprecated Use {@link FixedPieceDetector#detectFromBoard} instead
     */
    @Deprecated
    public List<SaveStateManager.PlacementInfo> detectFixedPiecesFromBoard(Board board, BitSet pieceUsed) {
        FixedPieceDetector.FixedPieceInfo info = fixedPieceDetector.detectFromBoard(board, pieceUsed);

        // Update internal state
        configBuilder.numFixedPieces(info.numFixedPieces);
        configBuilder.fixedPositions(info.fixedPositions);
        configBuilder.initialFixedPieces(info.fixedPiecesList);

        return info.fixedPiecesList;
    }

    /**
     * Calculates number of fixed pieces based on puzzle name.
     * @deprecated Use {@link FixedPieceDetector#calculateNumFixedPieces} instead
     */
    @Deprecated
    public int calculateNumFixedPieces(String puzzleName) {
        return fixedPieceDetector.calculateNumFixedPieces(puzzleName);
    }

    /**
     * Builds initial fixed pieces list from preloaded placement order.
     * @deprecated Use {@link FixedPieceDetector#buildFromPreloadedOrder} instead
     */
    @Deprecated
    public void buildInitialFixedPieces(List<SaveStateManager.PlacementInfo> preloadedOrder,
                                       int numFixedPieces) {
        FixedPieceDetector.FixedPieceInfo info =
            fixedPieceDetector.buildFromPreloadedOrder(preloadedOrder, numFixedPieces);

        configBuilder.numFixedPieces(info.numFixedPieces);
        configBuilder.fixedPositions(info.fixedPositions);
        configBuilder.initialFixedPieces(info.fixedPiecesList);
    }

    // ============ Manager Initialization ============

    /** Creates and initializes AutoSaveManager with current configuration. */
    public AutoSaveManager createAutoSaveManager(
            PlacementOrderTracker placementOrderTracker,
            Map<Integer, Piece> allPieces) {

        SolverConfiguration config = getConfig();
        AutoSaveManager manager = new AutoSaveManager(
            config.getPuzzleName(),
            config.getNumFixedPieces(),
            config.getInitialFixedPieces(),
            placementOrderTracker
        );
        manager.initializePiecesMap(allPieces);
        return manager;
    }

    /** Creates and initializes RecordManager with current configuration and global state references. */
    public RecordManager createRecordManager(
            Object lockObject,
            AtomicInteger globalMaxDepth,
            AtomicInteger globalBestScore,
            AtomicInteger globalBestThreadId,
            AtomicReference<Board> globalBestBoard,
            AtomicReference<Map<Integer, Piece>> globalBestPieces) {

        SolverConfiguration config = getConfig();
        return new RecordManager(
            config.getPuzzleName(),
            config.getThreadId(),
            config.getMinDepthToShowRecords(),
            lockObject,
            globalMaxDepth,
            globalBestScore,
            globalBestThreadId,
            globalBestBoard,
            globalBestPieces
        );
    }

    /** Logs current configuration parameters if verbose enabled. */
    public void logConfiguration() {
        getConfig().logConfiguration();
    }

    /**
     * Get the immutable configuration.
     * Recommended way to access configuration in new code.
     */
    public SolverConfiguration toConfiguration() {
        return getConfig();
    }
}
