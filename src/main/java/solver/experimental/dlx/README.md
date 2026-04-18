# Dancing Links (DLX) POC for Eternity II

> **Status**: experimental, not wired to production. Build +` run via the `mvn test -Dtest='solver.experimental.dlx.*'` target.
> **Goal**: measure whether Knuth's Algorithm X beats the current AC-3 + MRV backtracker on 4×4 / 6×6 / 8×8 boards.

## 1. Why DLX?

Knuth's DLX is the reference algorithm for exact cover. It shines on problems
where every placement covers a disjoint subset of a fixed column space:

- **N-queens**: each queen covers 1 row, 1 column, 2 diagonals.
- **Polyominos / pentomino tiling**: each piece placement covers its cells.
- **Sudoku**: each digit covers (cell, row, col, box).

These problems have a pure "place X to cover these columns" structure with **no
pairwise constraints between unplaced elements**.

## 2. Why Eternity is NOT a natural fit

Eternity II's hard constraint is edge-matching: when cell A gets a piece with
east edge = c, cell B (east neighbour of A) must get a piece whose west edge
is also c. This is a **binary constraint between adjacent placements**.

DLX models cover constraints ("every column covered exactly once"). Edge-
matching doesn't fit:
- Modelled as secondary columns (one per (position, side, colour) triple) it
  blows up the matrix: 2 × N × (N-1) inner edges × C colours per board.
- Modelled as primary columns it requires each row to cover its edges, but
  rows for neighbour cells would then conflict on their shared edge column —
  exact cover assumes row columns are disjoint.

The canonical workaround ("DLX + forward checking") is to enumerate rows that
already satisfy border constraints (outer edges = 0) then let DLX pick pieces
for cells, enforcing edge-matching via a **secondary constraint with pairing**.
That's not stock DLX — it's DLX + a custom propagator, which defeats the main
benefit (compact data structure, tight inner loop).

## 3. Decision for this POC

Build the **primary-only** version first:
- Primary columns: `(cell_r_c)` for all 16 cells + `(piece_id)` for all 16
  pieces. That guarantees cell & piece uniqueness — the trivial part.
- Rows: `(piece, rotation, r, c)` where `piece.edgesRotated(rot)` satisfies
  border constraints (zeros on outer sides matching border). Inner edges
  ignored at row selection time.
- Check edge-matching **at leaf time**: when DLX reports a cover, verify the
  board is a valid Eternity solution before claiming success.

This is **"DLX + solution validator"**, not "DLX + forward checker". Expected
behaviour:
- On small boards (3×3, 4×4 easy) where the search tree is thin, this may win.
- On hard variants or 6×6+, it will explode: DLX explores many covers that
  the validator then rejects.

The POC exists to **measure** this, not to win. The go/no-go criteria:

| Outcome | Action |
|---------|--------|
| DLX ≥ 2× faster than AC-3 on 4×4 hard | 🟢 continue, design DLX + FC variant |
| DLX 0.5–2× | 🔶 document, pause, revisit with DancingCells variant |
| DLX > 10× slower on 4×4 hard | 🔴 document no-go, close BB1 |

## 4. Components

| File | Role |
|------|------|
| `DancingLinksMatrix.java` | Generic sparse matrix with column/row nodes + cover/uncover primitives |
| `AlgorithmX.java` | Pure recursive search, emits solutions as `List<Row>` |
| `ExactCoverProblem.java` | Minimal builder for rows/columns — reusable by tests |
| `EternityExactCoverBuilder.java` | Translates `(Board shape, pieces)` to an ExactCoverProblem |
| `DancingLinksSolver.java` | Implements `solver.Solver`, adapts AlgorithmX result back to a filled `Board` with edge validation |
| `DancingLinksMatrixTest.java` | Validates DLX core on the classic Knuth example (7-col / 6-row matrix) |
| `EternityExactCoverBuilderTest.java` | Sanity: row count = pieces × positions × rotations after border-pruning |
| `DancingLinksSolverTest.java` | Correctness: solve 3×3, then 4×4 easy, compare solution to AC-3 |

## 5. Time budget & sequencing

Break the POC into 6 commits so each step is reviewable:

1. `feat(dlx): DancingLinksMatrix + AlgorithmX + test on Knuth example` (~1h)
2. `feat(dlx): EternityExactCoverBuilder + unit test` (~45 min)
3. `feat(dlx): DancingLinksSolver solves 3×3` (~30 min)
4. `feat(dlx): DancingLinksSolver solves 4×4 easy + correctness vs AC-3` (~30 min)
5. `bench(dlx): DLXBenchmark — compare to AC-3 on 3×3/4×4/4×4-hard` (~40 min)
6. `docs(dlx): record bench + go/no-go decision` (~15 min)

Total: ≈ 3h30–4h of work. Each step ends at a green `mvn test` point so the
branch can be paused without losing work.

## 6. Future: DLX + Forward Checking variant (deferred)

If the primary-only POC shows promise, the next iteration is to add a
forward-checking layer that prunes rows inconsistent with already-covered
edges. That's closer to AC-3 itself so the comparison becomes "DLX-FC vs AC-3
backtracker with MRV" — likely neck-and-neck, but DLX's cache-friendly
data structure may still win on large boards.

Decision deferred until the primary-only POC numbers are in.
