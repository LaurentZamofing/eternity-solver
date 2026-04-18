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

## 5. Observed results (2026-04-18)

Primary-only DLX was built as planned. Results on the POC test suite:

| Test         | Puzzle          | Time     | Outcome |
|--------------|-----------------|----------|---------|
| 3×3 example  | 9 pieces        | ~50 ms   | ✅ solves |
| 4×4 easy     | 16 pieces       | > 10 min | ❌ timeout |

The 4×4 timeout is exactly what the analysis in §2 predicted: without
edge-matching encoded in the matrix, DLX enumerates billions of
(cell-covered, piece-used) exact covers and the leaf-time validator
rejects almost all of them. The 3×3 case is small enough that the blow-up
stays tractable.

### Go/no-go decision

**No-go** on the primary-only variant for anything past 3×3. It will
never catch up to AC-3 + MRV, which prunes edge-incompatible placements
in O(1) during the search instead of at leaf time.

### Next steps (if/when we pick this back up)

1. **DLX + Forward Checking**: instead of validating at leaf time, prune
   rows inconsistent with already-covered edges after each column is
   covered. That's roughly AC-3's arc-consistency but with DLX's
   cache-friendly node structure. Likely to be competitive, not
   dominant.
2. **DLX with secondary columns for edge colours**: model each inner
   edge (position × side × colour) as a "covered at most once" column.
   Requires extending {@link DancingLinksMatrix} to support secondary
   columns; matrix blow-up is significant.

Neither is on the immediate roadmap — BB2 (scaling 16×16 with object
pools + JFR) is a cheaper next bet to move forward.

## 6. Artifacts kept on main

The experimental code stays on `main` so anyone picking the work up has
a starting point. It's:

- isolated under `solver.experimental.dlx` — zero dependency into it
  from production code;
- tested at the DLX-core level on Knuth's canonical example (5/5);
- smoke-tested at the solver level on 3×3 (1/3 passes, 1 @Disabled for
  4×4 as documented above, 1 for the pre-filled-board rejection
  contract).
