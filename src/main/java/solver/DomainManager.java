package solver;

import model.Board;
import model.Piece;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages AC-3 domains (valid placements) for puzzle cells with caching and backtracking support.
 */
public class DomainManager {

    /**
     * Stores valid placement details (piece ID, rotation, and the rotated edges).
     *
     * <p>The {@code edges} field caches {@code piece.edgesRotated(rotation)} so
     * AC-3's hot loop can compare an edge without a {@code HashMap.get()} and
     * a method dispatch per candidate — on large boards (16×16 target) this
     * saves tens of millions of map lookups per solve.</p>
     *
     * <p>Tests that don't care about edges can use the {@code (pid, rot)}
     * constructor, which leaves {@code edges == null}; production creation
     * paths always supply the rotated edges.</p>
     */
    public static class ValidPlacement {
        public final int pieceId;
        public final int rotation;
        public final int[] edges;

        public ValidPlacement(int pieceId, int rotation) {
            this(pieceId, rotation, null);
        }

        public ValidPlacement(int pieceId, int rotation, int[] edges) {
            this.pieceId = pieceId;
            this.rotation = rotation;
            this.edges = edges;
        }
    }

    // AC-3 domain data structure: domains[r][c] = Map<pieceId, List<ValidPlacement>>
    private Map<Integer, List<ValidPlacement>>[][] domains;
    private boolean ac3Initialized = false;

    // TL restriction remembered so it can be re-applied after backtrack.
    // Without this, restoreAC3Domains rebuilds (0,0) from scratch and drops
    // the sym-breaking constraint, which re-lets AC-3 produce infeasible
    // singletons / rotations at TL — the same class of bug the initial
    // restrictTopLeftDomain was meant to fix. See SymmetryBreakingBugTrackingTest.
    private Integer tlRestrictionPieceId = null;
    private Integer tlRestrictionRotation = null;

    // Domain cache for non-AC-3 mode
    private Map<Integer, List<ValidPlacement>> domainCache = null;
    private boolean useDomainCache = true;

    // Fit checker interface to validate placements
    private final FitChecker fitChecker;

    // Sort order for piece iteration: "ascending" or "descending"
    private String sortOrder = "ascending";

    // MRV index — optional priority queue for O(log N) cell selection.
    // Owned here because every domain mutation flows through this class.
    // The Board ref is captured at initializeAC3Domains for occupied-neighbor
    // computation; it stays valid for the duration of a solve.
    private solver.heuristics.MRVIndex mrvIndex;
    private Board boardRef;

    /** Interface for checking if a piece fits at a position. */
    public interface FitChecker {
        boolean fits(Board board, int r, int c, int[] candidateEdges);
    }

    /** Creates DomainManager with the specified fit checker. */
    public DomainManager(FitChecker fitChecker) {
        this.fitChecker = fitChecker;
        this.domainCache = new HashMap<>();
    }

    /** Sets piece iteration sort order ("ascending" or "descending"). */
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder : "ascending";
    }

    /** Initializes AC-3 domains for all empty cells, computing valid placements grouped by piece ID. */
    @SuppressWarnings("unchecked")
    public void initializeAC3Domains(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        int rows = board.getRows();
        int cols = board.getCols();
        domains = (Map<Integer, List<ValidPlacement>>[][]) new Map[rows][cols];
        this.boardRef = board;

        // Lazily build the MRV index. setMRVIndexEnabled(true) toggles it on
        // before initialize is called; otherwise the legacy MRV scan runs.
        if (mrvIndex != null) {
            mrvIndex = new solver.heuristics.MRVIndex(rows, cols);
            mrvIndex.setEmptinessProbe(board::isEmpty);
        }

        // Compute initial domains for all empty cells
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    domains[r][c] = new HashMap<>();
                    List<ValidPlacement> validPlacements = computeDomain(board, r, c, piecesById, pieceUsed, totalPieces);

                    // Group by pieceId for efficient AC-3
                    for (ValidPlacement vp : validPlacements) {
                        domains[r][c].computeIfAbsent(vp.pieceId, k -> new ArrayList<>()).add(vp);
                    }
                    if (mrvIndex != null) {
                        mrvIndex.onDomainChanged(r, c, totalRotationsIn(domains[r][c]), countOccupiedNeighbors(board, r, c));
                    }
                }
            }
        }
        ac3Initialized = true;
    }

    /** Enable / disable the MRV priority-queue index. Must be called before
     *  {@link #initializeAC3Domains} so the index is allocated with the right
     *  board dimensions. */
    public void setMRVIndexEnabled(boolean enabled) {
        if (enabled && mrvIndex == null) {
            // Sentinel non-null instance; replaced with the real one in
            // initializeAC3Domains once we know the board dimensions.
            this.mrvIndex = new solver.heuristics.MRVIndex(1, 1);
        } else if (!enabled) {
            this.mrvIndex = null;
        }
    }

    /** Returns the next empty cell with the smallest domain via the MRV index,
     *  or {@code null} if the index is disabled / empty. */
    public int[] peekMRVCell() {
        return mrvIndex == null ? null : mrvIndex.peek();
    }

    /** Returns true when the MRV priority queue is wired up. */
    public boolean isMRVIndexEnabled() {
        return mrvIndex != null;
    }

    /** Sums sizes of every per-piece rotation list in a domain. */
    private static int totalRotationsIn(Map<Integer, List<ValidPlacement>> domain) {
        if (domain == null || domain.isEmpty()) return 0;
        int total = 0;
        for (List<ValidPlacement> list : domain.values()) total += list.size();
        return total;
    }

    /** Counts how many of the 4 orthogonal neighbors are occupied (for the
     *  MRV index tie-breaker). */
    private static int countOccupiedNeighbors(Board board, int r, int c) {
        int n = 0;
        if (r > 0 && !board.isEmpty(r - 1, c)) n++;
        if (r < board.getRows() - 1 && !board.isEmpty(r + 1, c)) n++;
        if (c > 0 && !board.isEmpty(r, c - 1)) n++;
        if (c < board.getCols() - 1 && !board.isEmpty(r, c + 1)) n++;
        return n;
    }

    /**
     * Restores AC-3 domains after backtracking.
     *
     * <p>Previously recomputed only (r,c) and its 4 neighbors. That's
     * insufficient because AC-3 propagation is cascading — a single
     * placement can reduce domains of cells several hops away via the
     * queue in {@link ConstraintPropagator#propagateAC3}. Restoring only
     * the local neighborhood left distant cells with stale, over-reduced
     * domains, which caused deterministic dead-ends on search paths that
     * should have solutions (surfaced by 4x4 hard with piece 7 rot 0
     * forced at TL — the pre-existing bug documented in
     * {@code SymmetryBreakingBugTrackingTest}).</p>
     *
     * <p>Now recomputes every empty cell. Simple and correct; the cost
     * is O(W·H) per backtrack, acceptable for small boards and still a
     * small fraction of the placement attempt cost.</p>
     */
    public void restoreAC3Domains(Board board, int r, int c, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        if (!ac3Initialized) return;

        for (int rr = 0; rr < board.getRows(); rr++) {
            for (int cc = 0; cc < board.getCols(); cc++) {
                if (board.isEmpty(rr, cc)) {
                    recomputeDomainAt(board, rr, cc, piecesById, pieceUsed, totalPieces);
                }
            }
        }
    }

    /**
     * Rebuilds the domain at (r,c) by reusing the existing HashMap when present
     * (clear + refill) instead of allocating a new one. Each backtrack restoration
     * thus avoids one HashMap allocation per cell touched (1 + up to 4 neighbors).
     *
     * <p>Safe because the old domain reference is never read again after a
     * backtrack: the solver either continues down a new branch (calling
     * {@code propagateAC3} which produces fresh maps) or backtracks further
     * (which calls this method again). Single-threaded per DomainManager.</p>
     */
    private void recomputeDomainAt(Board board, int r, int c,
                                   Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        Map<Integer, List<ValidPlacement>> target = domains[r][c];
        if (target == null) {
            target = new HashMap<>();
            domains[r][c] = target;
        } else {
            target.clear();
        }
        List<ValidPlacement> validPlacements = computeDomain(board, r, c, piecesById, pieceUsed, totalPieces);
        for (ValidPlacement vp : validPlacements) {
            target.computeIfAbsent(vp.pieceId, k -> new ArrayList<>()).add(vp);
        }
        if (r == 0 && c == 0) {
            applyTopLeftRestriction();
        }
        if (mrvIndex != null && board.isEmpty(r, c)) {
            mrvIndex.onDomainChanged(r, c, totalRotationsIn(target), countOccupiedNeighbors(board, r, c));
        }
    }

    /** Computes valid placements for cell (r,c) by testing all available pieces in all rotations. */
    public List<ValidPlacement> computeDomain(Board board, int r, int c,
                                              Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        // Validate inputs at the boundary. Without this, an empty piecesById
        // silently skips every iteration and never dereferences `board`, so a
        // null board goes undetected until much later in the solver.
        java.util.Objects.requireNonNull(board, "board");
        java.util.Objects.requireNonNull(piecesById, "piecesById");
        java.util.Objects.requireNonNull(pieceUsed, "pieceUsed");

        List<ValidPlacement> domain = new ArrayList<>();

        // Iterate pieces in order specified by sortOrder
        if ("descending".equals(sortOrder)) {
            // Descending: from totalPieces down to 1
            for (int pid = totalPieces; pid >= 1; pid--) {
                if (pieceUsed.get(pid)) continue; // Piece already used
                Piece piece = piecesById.get(pid);
                int maxRotations = piece.getUniqueRotationCount();
                for (int rot = 0; rot < maxRotations; rot++) {
                    int[] edges = piece.edgesRotated(rot);
                    if (fitChecker.fits(board, r, c, edges)) {
                        domain.add(new ValidPlacement(pid, rot, edges));
                    }
                }
            }
        } else {
            // Ascending (default): from 1 to totalPieces
            for (int pid = 1; pid <= totalPieces; pid++) {
                if (pieceUsed.get(pid)) continue; // Piece already used
                Piece piece = piecesById.get(pid);
                if (piece == null) continue; // Skip if piece doesn't exist (sparse IDs)
                int maxRotations = piece.getUniqueRotationCount();
                for (int rot = 0; rot < maxRotations; rot++) {
                    int[] edges = piece.edgesRotated(rot);
                    if (fitChecker.fits(board, r, c, edges)) {
                        domain.add(new ValidPlacement(pid, rot, edges));
                    }
                }
            }
        }

        return domain;
    }

    /**
     * Restricts the AC-3 domain of the top-left cell (0,0) to a single
     * canonical piece and/or a required rotation, matching the symmetry-
     * breaking constraints. Must be called after {@link #initializeAC3Domains}.
     *
     * <p>Without this pre-filter, AC-3 can reduce (0,0) to a singleton
     * {@code (piece, rot)} that the symmetry-breaking rule later rejects,
     * producing a dead-end that the solver cannot recover from (see the
     * bug tracked by {@code SymmetryBreakingBugTrackingTest}). Aligning the
     * domain up-front keeps AC-3 and sym-breaking in the same state.</p>
     *
     * @param canonicalPieceId the only piece ID allowed at (0,0), or {@code null} to not constrain the piece
     * @param requiredRotation the only rotation allowed at (0,0), or {@code null} to not constrain rotation
     */
    public void restrictTopLeftDomain(Integer canonicalPieceId, Integer requiredRotation) {
        this.tlRestrictionPieceId = canonicalPieceId;
        this.tlRestrictionRotation = requiredRotation;
        applyTopLeftRestriction();
    }

    /** Re-applies the stored TL restriction to {@code domains[0][0]}. Called
     *  after backtrack rebuilds, since {@link #recomputeDomainAt} would
     *  otherwise restore every rotation/piece and drop sym-breaking alignment. */
    private void applyTopLeftRestriction() {
        if (!ac3Initialized || domains == null) return;
        if (tlRestrictionPieceId == null && tlRestrictionRotation == null) return;
        Map<Integer, List<ValidPlacement>> tl = domains[0][0];
        if (tl == null || tl.isEmpty()) return;

        if (tlRestrictionPieceId != null) {
            List<ValidPlacement> kept = tl.get(tlRestrictionPieceId);
            tl.clear();
            if (kept != null) {
                tl.put(tlRestrictionPieceId, new ArrayList<>(kept));
            }
        }
        if (tlRestrictionRotation != null) {
            for (Map.Entry<Integer, List<ValidPlacement>> entry : new ArrayList<>(tl.entrySet())) {
                List<ValidPlacement> filtered = new ArrayList<>();
                for (ValidPlacement vp : entry.getValue()) {
                    if (vp.rotation == tlRestrictionRotation) filtered.add(vp);
                }
                if (filtered.isEmpty()) {
                    tl.remove(entry.getKey());
                } else {
                    tl.put(entry.getKey(), filtered);
                }
            }
        }
        if (mrvIndex != null && boardRef != null && boardRef.isEmpty(0, 0)) {
            mrvIndex.onDomainChanged(0, 0, totalRotationsIn(tl), countOccupiedNeighbors(boardRef, 0, 0));
        }
    }

    /** Returns AC-3 domain for cell (r,c), or null if not initialized. */
    public Map<Integer, List<ValidPlacement>> getDomain(int r, int c) {
        if (!ac3Initialized || domains == null) return null;
        return domains[r][c];
    }

    /** Sets domain for cell (r,c), used during constraint propagation. */
    public void setDomain(int r, int c, Map<Integer, List<ValidPlacement>> domain) {
        if (domains != null && r >= 0 && r < domains.length && c >= 0 && c < domains[0].length) {
            domains[r][c] = domain;
            if (r == 0 && c == 0) {
                applyTopLeftRestriction();
            }
            if (mrvIndex != null && boardRef != null && boardRef.isEmpty(r, c)) {
                mrvIndex.onDomainChanged(r, c, totalRotationsIn(domain), countOccupiedNeighbors(boardRef, r, c));
            }
        }
    }

    /** Returns true if AC-3 domains are initialized. */
    public boolean isAC3Initialized() {
        return ac3Initialized;
    }

    /** Resets AC-3 initialization state and clears domains. */
    public void resetAC3() {
        ac3Initialized = false;
        domains = null;
        tlRestrictionPieceId = null;
        tlRestrictionRotation = null;
    }

    /** Enables or disables domain caching. */
    public void setUseDomainCache(boolean enabled) {
        this.useDomainCache = enabled;
        if (!enabled) {
            domainCache = null;
        } else if (domainCache == null) {
            domainCache = new HashMap<>();
        }
    }

    /** Returns cached domain for cell (r,c), or computes if cache disabled. */
    public List<ValidPlacement> getCachedDomain(Board board, int r, int c,
                                                 Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        if (!useDomainCache) {
            return computeDomain(board, r, c, piecesById, pieceUsed, totalPieces);
        }

        int key = r * board.getCols() + c;
        return domainCache.getOrDefault(key, new ArrayList<>());
    }

    /** Updates cache after placement at (r,c) by invalidating and recomputing neighbor domains. */
    public void updateCacheAfterPlacement(Board board, int r, int c,
                                          Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        if (!useDomainCache || domainCache == null) return;

        int rows = board.getRows();
        int cols = board.getCols();

        // Remove the placed cell from cache
        domainCache.remove(r * cols + c);

        // Update neighbors
        if (r > 0 && board.isEmpty(r - 1, c)) {
            domainCache.put((r-1) * cols + c, computeDomain(board, r - 1, c, piecesById, pieceUsed, totalPieces));
        }
        if (r < rows - 1 && board.isEmpty(r + 1, c)) {
            domainCache.put((r+1) * cols + c, computeDomain(board, r + 1, c, piecesById, pieceUsed, totalPieces));
        }
        if (c > 0 && board.isEmpty(r, c - 1)) {
            domainCache.put(r * cols + (c-1), computeDomain(board, r, c - 1, piecesById, pieceUsed, totalPieces));
        }
        if (c < cols - 1 && board.isEmpty(r, c + 1)) {
            domainCache.put(r * cols + (c+1), computeDomain(board, r, c + 1, piecesById, pieceUsed, totalPieces));
        }
    }

    /** Restores cache after backtracking by recomputing domains for (r,c) and neighbors. */
    public void restoreCacheAfterBacktrack(Board board, int r, int c,
                                           Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        if (!useDomainCache) return;

        int rows = board.getRows();
        int cols = board.getCols();

        // Recompute domain for the removed cell
        domainCache.put(r * cols + c, computeDomain(board, r, c, piecesById, pieceUsed, totalPieces));

        // Update neighbors
        if (r > 0 && board.isEmpty(r - 1, c)) {
            domainCache.put((r-1) * cols + c, computeDomain(board, r - 1, c, piecesById, pieceUsed, totalPieces));
        }
        if (r < rows - 1 && board.isEmpty(r + 1, c)) {
            domainCache.put((r+1) * cols + c, computeDomain(board, r + 1, c, piecesById, pieceUsed, totalPieces));
        }
        if (c > 0 && board.isEmpty(r, c - 1)) {
            domainCache.put(r * cols + (c-1), computeDomain(board, r, c - 1, piecesById, pieceUsed, totalPieces));
        }
        if (c < cols - 1 && board.isEmpty(r, c + 1)) {
            domainCache.put(r * cols + (c+1), computeDomain(board, r, c + 1, piecesById, pieceUsed, totalPieces));
        }
    }

    /** Initializes domain cache by computing domains for all empty cells. */
    public void initializeDomainCache(Board board, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        if (!useDomainCache) return;
        if (domainCache == null) {
            domainCache = new HashMap<>();
        }

        int rows = board.getRows();
        int cols = board.getCols();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board.isEmpty(r, c)) {
                    int key = r * cols + c;
                    domainCache.put(key, computeDomain(board, r, c, piecesById, pieceUsed, totalPieces));
                }
            }
        }
    }

    /** Clears the domain cache. */
    public void clearCache() {
        if (domainCache != null) {
            domainCache.clear();
        }
    }

    /**
     * Logs critical domains (cells with few pieces) for debugging.
     * Shows cells with 0-5 pieces in their domain.
     *
     * @param board Current board
     * @param maxPiecesToShow Only show cells with this many pieces or fewer (e.g., 5)
     */
    public void logCriticalDomains(Board board, int maxPiecesToShow) {
        if (!ac3Initialized) {
            util.SolverLogger.info("       ℹ️  AC-3 not initialized - cannot show domains");
            return;
        }

        util.SolverLogger.info("       🔍 Critical cells (≤" + maxPiecesToShow + " pieces in domain):");

        int count = 0;
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.isEmpty(r, c)) {
                    Map<Integer, List<ValidPlacement>> domain = getDomain(r, c);
                    if (domain != null && domain.size() <= maxPiecesToShow) {
                        String cellLabel = String.valueOf((char) ('A' + r)) + (c + 1);
                        String status = domain.size() == 0 ? "❌ EMPTY" :
                                      domain.size() == 1 ? "⭐ SINGLETON" :
                                      domain.size() <= 3 ? "🔴 CRITICAL" : "⚠️  WARNING";

                        int totalRotations = domain.values().stream().mapToInt(List::size).sum();
                        util.SolverLogger.info("          " + status + " " + cellLabel + ": " +
                                             domain.size() + " pieces, " + totalRotations + " rotations");
                        count++;
                    }
                }
            }
        }

        if (count == 0) {
            util.SolverLogger.info("          ✅ No critical cells found");
        }
    }
}
