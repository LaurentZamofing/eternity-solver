package solver.experimental.dlx;

import model.Board;
import model.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds an exact-cover matrix for an Eternity-style board.
 *
 * <p>Schema (primary-only, see {@code README.md}):</p>
 * <ul>
 *   <li>Columns 0..(N²-1) : cell_r_c indexed by {@code r * cols + c}</li>
 *   <li>Columns N²..(N² + P - 1) : piece_id indexed by {@code N² + (pieceId - 1)}</li>
 * </ul>
 *
 * <p>One row per {@code (piece, rotation, r, c)} whose rotated edges satisfy
 * the border constraint (outer edges = 0). Edge-matching between inner
 * neighbours is NOT encoded — the solver validates it at leaf time via the
 * {@link AlgorithmX} solutionAcceptor.</p>
 *
 * <p>The list returned by {@link #rowDescriptors()} is aligned with the
 * row ids used in {@link AlgorithmX}: index i → descriptor i.</p>
 */
final class EternityExactCover {

    /** Tuple describing a single row : where the piece goes and how it's turned. */
    static final class RowDescriptor {
        final int pieceId;
        final int rotation;
        final int row;
        final int col;
        final int[] rotatedEdges;

        RowDescriptor(int pieceId, int rotation, int row, int col, int[] rotatedEdges) {
            this.pieceId = pieceId;
            this.rotation = rotation;
            this.row = row;
            this.col = col;
            this.rotatedEdges = rotatedEdges;
        }
    }

    private final int rows;
    private final int cols;
    private final Map<Integer, Piece> pieces;
    private final int maxPieceId;

    private final List<RowDescriptor> descriptors = new ArrayList<>();
    private final DancingLinksMatrix matrix;

    EternityExactCover(int rows, int cols, Map<Integer, Piece> pieces) {
        this.rows = rows;
        this.cols = cols;
        this.pieces = pieces;
        this.maxPieceId = pieces.keySet().stream().max(Integer::compareTo).orElse(0);
        int cellCount = rows * cols;
        this.matrix = new DancingLinksMatrix(cellCount + maxPieceId);
        build();
    }

    private int cellCol(int r, int c) {
        return r * cols + c;
    }

    private int pieceCol(int pieceId) {
        return rows * cols + (pieceId - 1);
    }

    private void build() {
        for (Map.Entry<Integer, Piece> e : pieces.entrySet()) {
            int pid = e.getKey();
            Piece piece = e.getValue();
            int rotCount = piece.getUniqueRotationCount();
            for (int rot = 0; rot < rotCount; rot++) {
                int[] edges = piece.edgesRotated(rot);
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        if (!fitsBorders(edges, r, c)) continue;
                        int rowId = descriptors.size();
                        descriptors.add(new RowDescriptor(pid, rot, r, c, edges));
                        matrix.addRow(rowId, new int[] { cellCol(r, c), pieceCol(pid) });
                    }
                }
            }
        }
    }

    private boolean fitsBorders(int[] edges, int r, int c) {
        if (r == 0 && edges[0] != 0) return false;
        if (r == rows - 1 && edges[2] != 0) return false;
        if (c == 0 && edges[3] != 0) return false;
        if (c == cols - 1 && edges[1] != 0) return false;
        return true;
    }

    DancingLinksMatrix matrix() { return matrix; }

    List<RowDescriptor> rowDescriptors() { return descriptors; }

    int boardRows() { return rows; }

    int boardCols() { return cols; }

    /**
     * Verifies that the rows in {@code solution} describe a valid Eternity
     * solution: every cell covered, every piece used once (enforced by DLX),
     * and every adjacent pair of inner edges matching (the part DLX does not
     * encode — used as the leaf-time acceptor).
     */
    boolean isValidEternitySolution(List<Integer> solutionRowIds) {
        if (solutionRowIds.size() != rows * cols) return false;
        int[][] placedN = new int[rows][cols];
        int[][] placedE = new int[rows][cols];
        int[][] placedS = new int[rows][cols];
        int[][] placedW = new int[rows][cols];
        boolean[][] placed = new boolean[rows][cols];

        for (int rowId : solutionRowIds) {
            RowDescriptor d = descriptors.get(rowId);
            if (placed[d.row][d.col]) return false; // DLX should have prevented
            placed[d.row][d.col] = true;
            placedN[d.row][d.col] = d.rotatedEdges[0];
            placedE[d.row][d.col] = d.rotatedEdges[1];
            placedS[d.row][d.col] = d.rotatedEdges[2];
            placedW[d.row][d.col] = d.rotatedEdges[3];
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!placed[r][c]) return false;
                if (c < cols - 1 && placedE[r][c] != placedW[r][c + 1]) return false;
                if (r < rows - 1 && placedS[r][c] != placedN[r + 1][c]) return false;
            }
        }
        return true;
    }

    /** Applies the rows in {@code solutionRowIds} to {@code board}. */
    void applyTo(Board board, List<Integer> solutionRowIds) {
        for (int rowId : solutionRowIds) {
            RowDescriptor d = descriptors.get(rowId);
            board.place(d.row, d.col, pieces.get(d.pieceId), d.rotation);
        }
    }
}
