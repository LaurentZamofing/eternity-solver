package solver.experimental.dlx;

import java.util.ArrayList;
import java.util.List;

/**
 * Knuth's Dancing Links (DLX) matrix for exact-cover problems.
 *
 * <p>Supports {@code N} primary columns, each of which must be covered
 * exactly once. Rows are added via {@link #addRow(int, int[])}; every
 * {@code columnIndex} referenced must be in {@code [0, N)}.</p>
 *
 * <p>The standard DLX node structure is used: a circular header row
 * (plus a root sentinel) and each row linked bi-directionally into its
 * columns. Cover/uncover run in O(col size × row size / col); Algorithm X
 * (implemented in {@link AlgorithmX}) walks the matrix without any
 * memory allocation.</p>
 *
 * <p>This is a <b>primary-only</b> implementation. Secondary columns
 * (covered at most once) are not supported — the POC doesn't need them
 * (see {@code README.md}).</p>
 */
final class DancingLinksMatrix {

    /** Node in the doubly-linked DLX structure. Column headers are also Nodes. */
    static class Node {
        Node left, right, up, down;
        Column column;
        int rowId = -1;

        Node() {
            left = right = up = down = this;
        }
    }

    /** Column header — extends Node with a size counter and a name. */
    static final class Column extends Node {
        int size = 0;
        final int index;

        Column(int index) {
            super();
            this.index = index;
            this.column = this;
        }
    }

    private final Column root;
    private final Column[] columns;
    private int nextRowId = 0;

    DancingLinksMatrix(int columnCount) {
        root = new Column(-1);
        columns = new Column[columnCount];
        Node prev = root;
        for (int i = 0; i < columnCount; i++) {
            Column c = new Column(i);
            columns[i] = c;
            // Link column into header row after prev.
            c.left = prev;
            c.right = prev.right;
            prev.right.left = c;
            prev.right = c;
            prev = c;
        }
    }

    int columnCount() {
        return columns.length;
    }

    /**
     * Adds a row to the matrix.
     *
     * @param externalRowId  caller-chosen identifier returned by {@link AlgorithmX}
     *                       on solutions — e.g. an index into the caller's row list.
     * @param columnIndices  primary columns this row covers. Must be non-empty and
     *                       all valid column indices.
     */
    void addRow(int externalRowId, int[] columnIndices) {
        if (columnIndices == null || columnIndices.length == 0) {
            throw new IllegalArgumentException("row must cover at least one column");
        }
        Node first = null;
        for (int colIdx : columnIndices) {
            Column col = columns[colIdx];
            Node n = new Node();
            n.column = col;
            n.rowId = externalRowId;

            // Insert at bottom of column (col.up is the last node).
            n.up = col.up;
            n.down = col;
            col.up.down = n;
            col.up = n;
            col.size++;

            // Chain into row (horizontally to `first`).
            if (first == null) {
                first = n;
            } else {
                n.left = first.left;
                n.right = first;
                first.left.right = n;
                first.left = n;
            }
        }
        nextRowId++;
    }

    Column root() {
        return root;
    }

    Column column(int index) {
        return columns[index];
    }

    int totalRowsAdded() {
        return nextRowId;
    }

    /** Removes a column from the header list and every row touching it from
     *  their respective columns. Standard DLX cover primitive. */
    static void cover(Column col) {
        col.right.left = col.left;
        col.left.right = col.right;
        for (Node row = col.down; row != col; row = row.down) {
            for (Node n = row.right; n != row; n = n.right) {
                n.down.up = n.up;
                n.up.down = n.down;
                n.column.size--;
            }
        }
    }

    /** Inverse of {@link #cover(Column)} — restores the column and its rows. */
    static void uncover(Column col) {
        for (Node row = col.up; row != col; row = row.up) {
            for (Node n = row.left; n != row; n = n.left) {
                n.column.size++;
                n.down.up = n;
                n.up.down = n;
            }
        }
        col.right.left = col;
        col.left.right = col;
    }

    /** Diagnostic: collect the row ids currently present in the matrix.
     *  Not used by Algorithm X but handy for tests. */
    List<Integer> livingRowIds() {
        List<Integer> seen = new ArrayList<>();
        for (Column col = (Column) root.right; col != root; col = (Column) col.right) {
            for (Node n = col.down; n != col; n = n.down) {
                seen.add(n.rowId);
            }
        }
        return seen;
    }
}
