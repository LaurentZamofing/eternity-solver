package solver;

import util.SolverLogger;

import model.Board;
import model.Piece;
import model.Placement;
import util.SaveStateManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Manages all solver configuration: flags, puzzle metadata, fixed pieces detection, and manager initialization. */
public class ConfigurationManager {

    // Main configuration flags
    private boolean useSingletons = true;
    private boolean verbose = true;
    private boolean useAC3 = true;
    private boolean useDomainCache = true;
    private boolean prioritizeBorders = false;

    // Display and logging configuration
    private int minDepthToShowRecords = 0;

    // Timeout configuration
    private long maxExecutionTimeMs = Long.MAX_VALUE;

    // Puzzle metadata
    private String puzzleName = "eternity2";
    private String threadLabel = "";
    private String sortOrder = "ascending";
    private int threadId = -1;

    // Fixed pieces state
    private int numFixedPieces = 0;
    private Set<String> fixedPositions = new HashSet<>();
    private List<SaveStateManager.PlacementInfo> initialFixedPieces = new ArrayList<>();

    // Thread management and saving
    private long randomSeed = 0;
    private static final long THREAD_SAVE_INTERVAL = SolverConstants.THREAD_SAVE_INTERVAL_MS;

    public ConfigurationManager() {
    }

    // ============ Configuration Setters ============

    public void setDisplayConfig(boolean verbose, int minDepth) {
        this.verbose = verbose;
        this.minDepthToShowRecords = minDepth;
    }

    public void setMinDepthToShowRecords(int minDepth) {
        this.minDepthToShowRecords = minDepth;
    }

    public void setPuzzleName(String name) {
        this.puzzleName = name;
    }

    public void setSortOrder(String order) {
        this.sortOrder = order;
    }

    public void setNumFixedPieces(int num) {
        this.numFixedPieces = num;
    }

    public void setMaxExecutionTime(long timeMs) {
        this.maxExecutionTimeMs = timeMs;
    }

    public void setThreadLabel(String label) {
        this.threadLabel = label;
    }

    public void setThreadId(int id) {
        this.threadId = id;
    }

    public void setUseSingletons(boolean enabled) {
        this.useSingletons = enabled;
    }

    public void setPrioritizeBorders(boolean enabled) {
        this.prioritizeBorders = enabled;
        if (verbose && enabled) {
            SolverLogger.info("  🔲 Border prioritization enabled - borders will be filled first");
        }
    }

    public void setVerbose(boolean enabled) {
        this.verbose = enabled;
    }

    public void setUseAC3(boolean enabled) {
        this.useAC3 = enabled;
    }

    public void setUseDomainCache(boolean enabled) {
        this.useDomainCache = enabled;
    }

    public void setRandomSeed(long seed) {
        this.randomSeed = seed;
    }

    // ============ Configuration Getters ============

    public boolean isUseSingletons() {
        return useSingletons;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isUseAC3() {
        return useAC3;
    }

    public boolean isUseDomainCache() {
        return useDomainCache;
    }

    public boolean isPrioritizeBorders() {
        return prioritizeBorders;
    }

    public int getMinDepthToShowRecords() {
        return minDepthToShowRecords;
    }

    public long getMaxExecutionTimeMs() {
        return maxExecutionTimeMs;
    }

    public String getPuzzleName() {
        return puzzleName;
    }

    public String getThreadLabel() {
        return threadLabel;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public int getThreadId() {
        return threadId;
    }

    public int getNumFixedPieces() {
        return numFixedPieces;
    }

    public Set<String> getFixedPositions() {
        return new HashSet<>(fixedPositions);
    }

    public List<SaveStateManager.PlacementInfo> getInitialFixedPieces() {
        return new ArrayList<>(initialFixedPieces);
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public static long getThreadSaveInterval() {
        return THREAD_SAVE_INTERVAL;
    }

    // ============ Fixed Pieces Detection ============

    /** Detects and initializes fixed pieces from board state; used when starting with pre-placed pieces.
     * @return List of fixed piece placements found on the board
     */
    public List<SaveStateManager.PlacementInfo> detectFixedPiecesFromBoard(Board board, BitSet pieceUsed) {
        fixedPositions.clear();
        numFixedPieces = 0;
        initialFixedPieces.clear();
        List<SaveStateManager.PlacementInfo> fixedPiecesList = new ArrayList<>();

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (!board.isEmpty(r, c)) {
                    fixedPositions.add(r + "," + c);
                    numFixedPieces++;

                    Placement placement = board.getPlacement(r, c);
                    int placedPieceId = placement.getPieceId();
                    int placedRotation = placement.getRotation();
                    pieceUsed.set(placedPieceId);

                    SaveStateManager.PlacementInfo fixedPiece =
                        new SaveStateManager.PlacementInfo(r, c, placedPieceId, placedRotation);
                    initialFixedPieces.add(fixedPiece);
                    fixedPiecesList.add(fixedPiece);
                }
            }
        }

        return fixedPiecesList;
    }

    /** Calculates number of fixed pieces based on puzzle name; used when resuming from saved state. */
    public int calculateNumFixedPieces(String puzzleName) {
        if (puzzleName.startsWith("eternity2")) {
            return 9; // 4 corners + 5 hints for Eternity II
        } else if (puzzleName.startsWith("indice")) {
            return 0; // No fixed pieces for hint puzzles
        } else {
            return 0; // Default: no fixed pieces
        }
    }

    /** Builds initial fixed pieces list from preloaded placement order; used when resuming from saved state. */
    public void buildInitialFixedPieces(List<SaveStateManager.PlacementInfo> preloadedOrder,
                                       int numFixedPieces) {
        this.fixedPositions = new HashSet<>();
        this.numFixedPieces = numFixedPieces;
        this.initialFixedPieces.clear();

        for (int i = 0; i < Math.min(numFixedPieces, preloadedOrder.size()); i++) {
            initialFixedPieces.add(preloadedOrder.get(i));
        }
    }

    // ============ Manager Initialization ============

    /** Creates and initializes AutoSaveManager with current configuration. */
    public AutoSaveManager createAutoSaveManager(
            PlacementOrderTracker placementOrderTracker,
            Map<Integer, Piece> allPieces) {

        AutoSaveManager manager = new AutoSaveManager(
            puzzleName,
            numFixedPieces,
            initialFixedPieces,
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

        return new RecordManager(
            puzzleName,
            threadId,
            minDepthToShowRecords,
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
        if (verbose) {
            SolverLogger.info("\n═══════════════════════════════════════");
            SolverLogger.info("  Configuration:");
            SolverLogger.info("  - Puzzle: " + puzzleName);
            SolverLogger.info("  - Thread: " + threadLabel);
            System.out.println("  - Singletons: " + (useSingletons ? "✓" : "✗"));
            System.out.println("  - AC-3: " + (useAC3 ? "✓" : "✗"));
            System.out.println("  - Domain Cache: " + (useDomainCache ? "✓" : "✗"));
            System.out.println("  - Prioritize Borders: " + (prioritizeBorders ? "✓" : "✗"));
            SolverLogger.info("  - Fixed Pieces: " + numFixedPieces);
            SolverLogger.info("═══════════════════════════════════════\n");
        }
    }
}
