# CP-SAT spike — Phase D (Eternity II Solver)

> **Status**: experimental. Dependency not added to the main classpath until
> a bench gate is hit. This directory just pins the design.
> **Goal**: measure whether Google OR-Tools CP-SAT can solve generated
> puzzles the hand-rolled AC-3 backtracker can't.

## Why CP-SAT

The Phase A perf wall showed the hand-rolled solver plateaus between
6×6 (25 s) and 7×7 (timeout at 120 s). Further hand-tuning of AC-3 /
MRV / allocation won't cross this cliff — it's a constant-factor game
against an exponential search space.

Modern CP-SAT (Google OR-Tools, CP-SAT-LP as of 2024) does:
- **Lazy clause generation (LCG)** — learns nogoods from failed search,
  prunes whole branches instead of one assignment at a time.
- **Restart + randomised branching** — escapes local search traps.
- **Portfolio parallelism** — multiple configurations race in parallel.
- **Integer cuts** — when edges have small domains, encodes them as
  bit-vectors and uses propagators we don't have.

Empirically, CP-SAT is 10×-100× faster than hand-rolled backtrackers on
comparable constraint problems. A 10× gain on 6×6 = 2.5 s; on 7×7 that
means ~1 min instead of timeout.

## Design

### Variables
For each cell (r, c), one integer variable `cell[r][c] ∈ {1..N}`
identifying which piece sits there. Plus a rotation variable
`rot[r][c] ∈ {0..3}` (can be collapsed to one combined variable
`placement[r][c] ∈ {0..4N-1}` encoding both).

### Constraints
1. **AllDifferent** on `cell[r][c]` — each piece used exactly once.
2. **Edge-matching** — for every inner pair of adjacent cells (A, B):
   `east_edge(cell[A], rot[A]) == west_edge(cell[B], rot[B])`.
   Modelled via lookup tables turned into `element` constraints (CP-SAT
   native) or `AllowedAssignments` if small enough.
3. **Border constraints** — for every border cell, the corresponding
   outer edge must be 0. Again an `element` + equality.

### Solver config
- `cp_model.CpSolver().num_search_workers = 4` (parallel search).
- Time limit 60 s matching the realistic goal.

## Integration plan

1. **Branch** `experimental/cpsat-spike` (not merged to main until gate).
2. **Dependency** `com.google.ortools:ortools-java:9.x` — bundles native
   libs; CI must install them via apt-get or Maven package.
3. **Class** `CpSatSolver implements solver.Solver` — same API, swap-in.
4. **Test** reuse `AC3CorrectnessTest` to verify solutions are valid.
5. **Bench** `CpSatBenchmark` running 3×3 → 8×8, apples-to-apples with
   `BenchmarkGrid`.

## Go/no-go criteria

| Observation on 7×7 generated | Decision |
|------------------------------|----------|
| CP-SAT solves < 60 s (≥ 2/3 seeds) | 🟢 **Go** — merge as alternative `Solver` behind flag, document in README. |
| CP-SAT solves < 5 min but > 60 s | 🔶 Partial — document gain, keep branch alive, don't merge. |
| CP-SAT ≥ 5 min or timeout | 🔴 **No-go** — fold up, note the Eternity structure is too specialised for generic CP-SAT. |

## Risk

- **JNI native libs** complicate CI (different binary per OS/arch). Mitigation: keep CP-SAT code in a dedicated Maven profile (`-P cpsat`) so the default build stays portable.
- **License** — OR-Tools is Apache 2.0, compatible.
- **Learning curve** — OR-Tools Java API is verbose; a small POC first to validate the approach before scaling.

## Status

**Design pinned 2026-04-18. Implementation not started in this session.**
Next session can go straight to:
1. `git checkout -b experimental/cpsat-spike`
2. Add `<dependency>...ortools-java...</dependency>` in a `-P cpsat` profile.
3. Write `CpSatSolver` + the edge-matching encoder.
4. Run on 5×5 / 6×6 / 7×7 generated and report.
