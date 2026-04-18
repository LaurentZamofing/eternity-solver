package solver.experimental.bitmap;

/**
 * Forward checking for the bitmap solver: after a placement at a cell,
 * prune each orthogonal empty neighbour's domain to pidRots whose shared
 * edge matches the placed piece's matching edge.
 *
 * <p>All updates go through {@link SearchState#writeWord(int, int, long)}
 * so they're trail-recorded and can be rolled back by
 * {@link SearchState#rollbackDepth}.</p>
 *
 * <p>Returns {@code false} (dead-end) if any neighbour's domain becomes
 * empty. Callers must then trigger a backtrack.</p>
 */
public final class ForwardChecker {

    private final PiecesCatalog catalog;

    public ForwardChecker(PiecesCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Applies forward checking after {@code pidRot} has been placed at
     * {@code cell}. Returns {@code false} on dead-end (some neighbour's
     * domain became empty), {@code true} otherwise.
     */
    public boolean apply(SearchState state, int cell, int pidRot) {
        int r = cell / catalog.cols;
        int c = cell % catalog.cols;
        short[] edges = catalog.piecesEdges[pidRot];

        // Neighbour offsets: {dr, dc, placedSide, neighbourSide}.
        // Side 0=N, 1=E, 2=S, 3=W. When piece placed at (r,c) has edge `e`
        // on its SOUTH side, the cell below (r+1,c) must have edge `e` on
        // its NORTH side.
        int[][] offsets = {
            { -1,  0, 0, 2 }, // north neighbour: placed.N must match neighbour.S
            {  1,  0, 2, 0 }, // south
            {  0, -1, 3, 1 }, // west
            {  0,  1, 1, 3 }  // east
        };

        for (int[] off : offsets) {
            int nr = r + off[0];
            int nc = c + off[1];
            if (nr < 0 || nr >= catalog.rows || nc < 0 || nc >= catalog.cols) continue;
            int ncell = nr * catalog.cols + nc;
            // If neighbour already has a singleton domain (a piece placed
            // there already), its domain is collapsed and AND-mask is a no-op
            // if the placed piece was consistent, else clears it.
            int placedSide = off[2];
            int neighbourSide = off[3];
            int requiredColour = edges[placedSide];
            if (requiredColour < 0) return false; // defensive
            long[] mask = catalog.edgeCompatMask[neighbourSide][requiredColour];
            long[] dom = state.domain[ncell];
            long or = 0;
            for (int w = 0; w < catalog.words; w++) {
                long newVal = dom[w] & mask[w];
                state.writeWord(ncell, w, newVal);
                or |= newVal;
            }
            if (or == 0) return false; // neighbour domain empty → dead-end
        }
        return true;
    }
}
