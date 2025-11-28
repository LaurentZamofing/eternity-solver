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

    /** Stores valid placement details (piece ID and rotation). */
    public static class ValidPlacement {
        public final int pieceId;
        public final int rotation;

        public ValidPlacement(int pieceId, int rotation) {
            this.pieceId = pieceId;
            this.rotation = rotation;
        }
    }

    // AC-3 domain data structure: domains[r][c] = Map<pieceId, List<ValidPlacement>>
    private Map<Integer, List<ValidPlacement>>[][] domains;
    private boolean ac3Initialized = false;

    // Domain cache for non-AC-3 mode
    private Map<Integer, List<ValidPlacement>> domainCache = null;
    private boolean useDomainCache = true;

    // Fit checker interface to validate placements
    private final FitChecker fitChecker;

    // Sort order for piece iteration: "ascending" or "descending"
    private String sortOrder = "ascending";

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
                }
            }
        }
        ac3Initialized = true;
    }

    /** Restores AC-3 domains after backtracking by recomputing domains for (r,c) and neighbors. */
    public void restoreAC3Domains(Board board, int r, int c, Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        if (!ac3Initialized) return;

        // Recompute domains for the cell itself
        domains[r][c] = new HashMap<>();
        List<ValidPlacement> validPlacements = computeDomain(board, r, c, piecesById, pieceUsed, totalPieces);
        for (ValidPlacement vp : validPlacements) {
            domains[r][c].computeIfAbsent(vp.pieceId, k -> new ArrayList<>()).add(vp);
        }

        // Recompute domains for all neighbors
        int[][] neighbors = {{r-1, c}, {r+1, c}, {r, c-1}, {r, c+1}};
        for (int[] nbr : neighbors) {
            int nr = nbr[0], nc = nbr[1];
            if (nr < 0 || nr >= board.getRows() || nc < 0 || nc >= board.getCols()) continue;
            if (!board.isEmpty(nr, nc)) continue;

            domains[nr][nc] = new HashMap<>();
            validPlacements = computeDomain(board, nr, nc, piecesById, pieceUsed, totalPieces);
            for (ValidPlacement vp : validPlacements) {
                domains[nr][nc].computeIfAbsent(vp.pieceId, k -> new ArrayList<>()).add(vp);
            }
        }
    }

    /** Computes valid placements for cell (r,c) by testing all available pieces in all rotations. */
    public List<ValidPlacement> computeDomain(Board board, int r, int c,
                                              Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
        List<ValidPlacement> domain = new ArrayList<>();

        // Iterate pieces in order specified by sortOrder
        if ("descending".equals(sortOrder)) {
            // Descending: from totalPieces down to 1
            for (int pid = totalPieces; pid >= 1; pid--) {
                if (pieceUsed.get(pid)) continue; // Pièce déjà utilisée
                Piece piece = piecesById.get(pid);
                int maxRotations = piece.getUniqueRotationCount();
                for (int rot = 0; rot < maxRotations; rot++) {
                    int[] edges = piece.edgesRotated(rot);
                    if (fitChecker.fits(board, r, c, edges)) {
                        domain.add(new ValidPlacement(pid, rot));
                    }
                }
            }
        } else {
            // Ascending (default): from 1 to totalPieces
            for (int pid = 1; pid <= totalPieces; pid++) {
                if (pieceUsed.get(pid)) continue; // Pièce déjà utilisée
                Piece piece = piecesById.get(pid);
                if (piece == null) continue; // Skip if piece doesn't exist (sparse IDs)
                int maxRotations = piece.getUniqueRotationCount();
                for (int rot = 0; rot < maxRotations; rot++) {
                    int[] edges = piece.edgesRotated(rot);
                    if (fitChecker.fits(board, r, c, edges)) {
                        domain.add(new ValidPlacement(pid, rot));
                    }
                }
            }
        }

        return domain;
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
}
