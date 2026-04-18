# Most-Constraining Variable (MCV) heuristic — BB4

> **Status**: experimental. Off in production.
> **Goal**: measure whether picking the cell that *constrains* the most
> empty neighbours beats pure MRV (smallest domain) on large boards.

## Idea

MRV (Minimum Remaining Values) picks the cell with the smallest domain —
minimises branching locally.

MCV (Most-Constraining Variable) picks the cell whose placement would
eliminate the most options from *other* cells — minimises branching
globally on a horizon of one placement.

For Eternity, a cell's "constraining power" is proxied by the number of
empty orthogonal neighbours it has: placing a piece there pins 4 edges
that neighbours must match.

## Hypothesis

- On small boards (3×3, 4×4), MCV ≈ MRV (the tiebreaker rarely fires).
- On 6×6+, MCV should prune deeper because border/corner placements
  propagate constraints faster.
- The common MRV-then-MCV-tiebreak variant in {@code MRVCellSelector}
  already approximates this — the experiment measures the delta of a
  *pure* MCV selection.

## Scope

Implement a minimal {@code MCVCellSelector} that:
1. Scans empty cells.
2. For each, counts non-empty neighbours + border edges = "pressure".
3. Returns the cell with the highest pressure (ties broken by domain size).

Wire it behind a boolean flag on {@code EternitySolver} so the bench can
A/B test. No production default change.

## Measurement

`FullSolveBenchmark` can grow a `solve6x6GeneratedWithMCV` pair to the
existing `solve6x6GeneratedWithMRVIndex`. Run both via JMH, decide
go/no-go from relative timing.

## Status

**Not implemented yet** — this README pins the design decision so a
follow-up can just wire the selector without re-deriving the rationale.
Target: a couple of hours of work + JFR profile to confirm.
