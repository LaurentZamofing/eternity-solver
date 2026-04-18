package solver.experimental.bitmap;

import model.Piece;

import java.util.Map;
import java.util.Random;

/**
 * Immutable tables precomputed once at solver construction; read-only in the
 * search hot loop. The bitmap solver reads these directly — no lookups, no
 * allocations.
 *
 * <h3>Placement encoding</h3>
 * A placement is {@code pidRot = pieceId * 4 + rotation}. We reserve bit
 * positions {@code 0..pidRotMax-1} where {@code pidRotMax = maxPieceId * 4 + 4}.
 * Dead bits (pieces with fewer than 4 unique rotations, or rotation indices
 * over {@link Piece#getUniqueRotationCount()}) are simply left cleared in all
 * domain bitmaps — they never appear and cost nothing.
 *
 * <h3>Layout</h3>
 * <ul>
 *   <li>{@link #piecesEdges} — {@code [pidRot][side]} edge colour (short).
 *       Fits L1 cache.</li>
 *   <li>{@link #edgeCompatMask} — {@code [side][colour]} → bitmap of pidRots
 *       whose edge on that side equals the colour. Used by forward checking
 *       to filter a neighbour's domain with N {@code &} operations.</li>
 *   <li>{@link #borderMask} — {@code [side]} → bitmap of pidRots whose edge
 *       on that side is 0 (i.e. the piece can sit on that border). Applied
 *       once at init to segregate border vs interior.</li>
 *   <li>{@link #zobristTable} — {@code [cell][pidRot]} random {@code long}
 *       for the nogood hash (P2).</li>
 * </ul>
 *
 * <h3>Sides</h3>
 * {@code 0 = N}, {@code 1 = E}, {@code 2 = S}, {@code 3 = W} (matches
 * {@link Piece#edgesRotated(int)}).
 */
public final class PiecesCatalog {

    public final int rows;
    public final int cols;
    public final int numCells;
    public final int maxPieceId;     // max pieceId seen (1-based)
    public final int pidRotMax;      // exclusive upper bound: maxPieceId*4 + 4
    public final int words;          // = (pidRotMax + 63) >>> 6

    /** [pidRot][side] — edge colour; {@code short} because colours in typical puzzles are < 256. */
    public final short[][] piecesEdges;

    /**
     * [side][colour] — bitmap (length {@code words}) of pidRots whose edge on
     * {@code side} equals {@code colour}. Used to filter a neighbour.
     * Size: 4 sides × maxColour × words longs.
     */
    public final long[][][] edgeCompatMask;

    /**
     * [side] — bitmap of pidRots valid on a board edge facing {@code side}
     * (i.e. whose {@code side} edge is 0). Used to initialise border-cell
     * domains without runtime checks.
     */
    public final long[][] borderMask;

    /** {@code [cell][pidRot]} Zobrist random longs for incremental state hashing. */
    public final long[][] zobristTable;

    public final int maxColour;

    /**
     * Builds the catalog from the puzzle definition.
     *
     * @param rows        board rows
     * @param cols        board cols
     * @param pieces      pieces indexed by id (1-based; gaps tolerated)
     * @param zobristSeed deterministic seed for zobrist table
     */
    public PiecesCatalog(int rows, int cols, Map<Integer, Piece> pieces, long zobristSeed) {
        this.rows = rows;
        this.cols = cols;
        this.numCells = rows * cols;
        this.maxPieceId = pieces.keySet().stream().max(Integer::compareTo).orElse(0);
        this.pidRotMax = (maxPieceId + 1) * 4;
        this.words = (pidRotMax + 63) >>> 6;

        // Edges — fill dead slots with -1 so any stray access is visible in debuggers.
        piecesEdges = new short[pidRotMax][4];
        int maxColourSeen = 0;
        for (short[] row : piecesEdges) {
            row[0] = row[1] = row[2] = row[3] = -1;
        }
        for (Map.Entry<Integer, Piece> e : pieces.entrySet()) {
            Piece p = e.getValue();
            int rotCount = p.getUniqueRotationCount();
            for (int rot = 0; rot < rotCount; rot++) {
                int pidRot = pidRot(e.getKey(), rot);
                int[] edges = p.edgesRotated(rot);
                for (int side = 0; side < 4; side++) {
                    piecesEdges[pidRot][side] = (short) edges[side];
                    if (edges[side] > maxColourSeen) maxColourSeen = edges[side];
                }
            }
        }
        this.maxColour = maxColourSeen;

        // Edge compat masks — for each (side, colour) a bitmap of matching pidRots.
        edgeCompatMask = new long[4][maxColourSeen + 1][words];
        for (int pidRot = 0; pidRot < pidRotMax; pidRot++) {
            if (!hasValidEdges(pidRot)) continue;
            for (int side = 0; side < 4; side++) {
                int colour = piecesEdges[pidRot][side];
                setBit(edgeCompatMask[side][colour], pidRot);
            }
        }

        // Border masks: for each side, bitmap of pidRots whose edge on that side is 0.
        borderMask = edgeCompatMask.length == 0 ? new long[4][0] :
            new long[][] {
                edgeCompatMask[0][0].clone(),
                edgeCompatMask[1][0].clone(),
                edgeCompatMask[2][0].clone(),
                edgeCompatMask[3][0].clone()
            };

        // Zobrist
        Random rng = new Random(zobristSeed);
        zobristTable = new long[numCells][pidRotMax];
        for (int c = 0; c < numCells; c++) {
            for (int p = 0; p < pidRotMax; p++) {
                zobristTable[c][p] = rng.nextLong();
            }
        }
    }

    /** Encoded placement id for {@code (pieceId, rotation)}. */
    public static int pidRot(int pieceId, int rotation) {
        return pieceId * 4 + rotation;
    }

    /** Inverse of {@link #pidRot}. */
    public static int pieceIdOf(int pidRot) { return pidRot >>> 2; }

    /** Inverse of {@link #pidRot}. */
    public static int rotationOf(int pidRot) { return pidRot & 3; }

    private boolean hasValidEdges(int pidRot) {
        return piecesEdges[pidRot][0] >= 0;
    }

    /** Returns the cell index for {@code (r, c)} — row-major. */
    public int cellIndex(int r, int c) {
        return r * cols + c;
    }

    /** Whether a given cell sits on a given board side. */
    public boolean cellOnSide(int cell, int side) {
        int r = cell / cols;
        int c = cell % cols;
        switch (side) {
            case 0: return r == 0;
            case 1: return c == cols - 1;
            case 2: return r == rows - 1;
            case 3: return c == 0;
            default: throw new IllegalArgumentException("side=" + side);
        }
    }

    /** Produces an initial domain bitmap for {@code cell} with border-piece
     *  segregation baked in: pieces whose required border edges are all 0 only. */
    public long[] initialDomainForCell(int cell) {
        long[] dom = new long[words];
        // Start with "all pidRots valid" (every bit below pidRotMax set).
        for (int w = 0; w < words; w++) {
            dom[w] = -1L;
        }
        // Clear any bits beyond pidRotMax in the last word.
        int extra = words * 64 - pidRotMax;
        if (extra > 0) {
            dom[words - 1] &= (1L << (64 - extra)) - 1;
        }
        // Clear dead bits (pieces with <4 rotations, gap pieceIds).
        for (int pidRot = 0; pidRot < pidRotMax; pidRot++) {
            if (!hasValidEdges(pidRot)) {
                clearBit(dom, pidRot);
            }
        }
        // Apply border constraints: AND with border mask for every side the cell touches.
        for (int side = 0; side < 4; side++) {
            if (cellOnSide(cell, side)) {
                long[] bm = borderMask[side];
                for (int w = 0; w < words; w++) dom[w] &= bm[w];
            }
        }
        return dom;
    }

    /** Writes bit {@code bitIdx} into {@code bitmap}. */
    public static void setBit(long[] bitmap, int bitIdx) {
        bitmap[bitIdx >>> 6] |= 1L << (bitIdx & 63);
    }

    /** Clears bit {@code bitIdx} in {@code bitmap}. */
    public static void clearBit(long[] bitmap, int bitIdx) {
        bitmap[bitIdx >>> 6] &= ~(1L << (bitIdx & 63));
    }

    /** True iff bit {@code bitIdx} is set. */
    public static boolean hasBit(long[] bitmap, int bitIdx) {
        return (bitmap[bitIdx >>> 6] & (1L << (bitIdx & 63))) != 0;
    }

    /** Cardinality of {@code bitmap} — how many placements remain. */
    public static int cardinality(long[] bitmap) {
        int n = 0;
        for (long w : bitmap) n += Long.bitCount(w);
        return n;
    }

    /** Index of the lowest set bit at or above {@code start}, or -1 if none. */
    public static int nextSetBit(long[] bitmap, int start) {
        int w = start >>> 6;
        if (w >= bitmap.length) return -1;
        long word = bitmap[w] & (-1L << (start & 63));
        while (true) {
            if (word != 0) return (w << 6) + Long.numberOfTrailingZeros(word);
            if (++w == bitmap.length) return -1;
            word = bitmap[w];
        }
    }
}
