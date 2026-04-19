# Perf wall — baseline Phase A (2026-04-18)

## Setup

- Runner : `benchmark.BenchmarkGrid`
- Machine : local MacBook, JDK 24, single-threaded solver (`symmetryBreakingFlags(false, false)`)
- Per-run timeout : 120 s
- Generated puzzles : `PuzzleGenerator.generate(size, max(4, size-1), seed)` — palette size grows with board to keep the search non-trivial
- 3 seeds per generated size (seeds `1, 17, 42`)

## Results

| Size | Seeds solved | Avg time | Notes |
|------|--------------|----------|-------|
| 3×3 example (curated) | ✅ | 430 ms | First run includes JIT warmup |
| 4×4 easy (curated) | ✅ | 74 ms | |
| 4×4 hard V3 (curated) | ✅ | 85 ms | |
| 3×3 generated | **3/3** | **19 ms** | Baseline |
| 4×4 generated | **3/3** | **35 ms** | ×1.8 |
| 5×5 generated | **3/3** | **1 030 ms** | **×30** |
| 6×6 generated | **2/3** | **25 269 ms** | **×25**, 1 seed times out |
| 7×7 generated | **0/3** | — | All 3 seeds time out at 120 s |
| 8×8 generated | **0/3** | — | Confirmed, cf. JFR (`/tmp/solve-8x8.jfr`) |

## Cliff location

**Between 6×6 and 7×7.** The 4×4→5×5 and 5×5→6×6 transitions are already ×25-30 in
wall time (exponential, consistent with search space growth). 7×7 crosses the
120 s timeout on every seed → the curve goes at least ×5 on that step.

If we extrapolate the trend (~×25 per step), solving a random 7×7 should take
~10 min, and 8×8 several hours.

## Consequences for the phased plan

- **Phase B** (int-keyed domains) targets AC-3 allocation pressure. Expected
  gain: 20-50 % wall time on 6×6. That brings 6×6 from 25 s → ~13 s, and 7×7
  from ~10 min → ~5 min. **Useful but doesn't cross the 7×7 cliff alone**.
- **Phase C** (thread diversification on 4 cores) could give ~3× on top of B.
  7×7 from ~5 min → ~100 s. Still marginal for 7×7.
- **Phase D** (CP-SAT) is the honest path for 7×7 sub-60s. Modern CP-SAT
  solvers leverage clause learning / LCG that a handwritten backtracker can't
  match by a constant-factor optimisation.

**Updated target** : realistic goal is **6×6 reliably under 10 s**, 7×7 best-effort.
Anyone wanting 8×8+ in production should use Phase D (CP-SAT) as the primary
path.

## Next steps

1. **Phase B now** — int-keyed domains, gate: 6×6 ≥ 30 % faster (≤ 17 s avg).
2. If B hits gate → **Phase C** (parallel diversification, target 6×6 ≤ 10 s on 4 threads).
3. Regardless of B+C outcomes, **spike Phase D in a branch** once B finishes: CP-SAT is the only realistic path to 7×7 sub-minute.

## Raw data

See `.github/perf-baseline-grid.json` for the full JSON dump (timestamps, per-seed
times). Re-run via `java benchmark.BenchmarkGrid` with the standard classpath.

---

## Bitmap solver P1 results (2026-04-18 soir)

`BitmapSolver` (ULTRA_PLAN P1 skeleton) vs `EternitySolver` on the same
generated puzzles (see `.github/bitmap-p1-bench.log`):

| Size | Seed | ref ms | bitmap ms | Speedup |
|------|------|--------|-----------|---------|
| 3×3 | 1 | 447 | 2 | **223×** |
| 3×3 | 17 | 24 | <1 | – |
| 3×3 | 42 | 25 | <1 | – |
| 4×4 | 1 | 69 | <1 | – |
| 4×4 | 17 | 49 | <1 | – |
| 4×4 | 42 | 45 | <1 | – |
| 5×5 | 1 | 513 | 3 | **171×** |
| 5×5 | 17 | 2 457 | 5 | **491×** |
| 5×5 | 42 | 168 | <1 | – |
| **6×6** | 1 | 23 552 | **79** | **298×** |
| **6×6** | 17 | 46 995 | **227** | **207×** |
| **6×6** | 42 | 59 856 | **493** | **121×** |
| 7×7 | 1 | 60 000 (timeout) | **4 646** | ≥ 12.9× |
| 7×7 | 17 | 60 006 (timeout) | 61 992 (timeout) | – |
| 7×7 | 42 | 60 003 (timeout) | **12 705** | ≥ 4.7× |

### Consequences

- **P1 gate (6×6 <5 s)** massively achieved: avg ~266 ms on 3 seeds → **×18 margin**.
- **P2 gate (7×7 <60 s)** 2/3 already achieved without nogood/restart.
- Seed 17 at 7×7 times out even in bitmap — this is where Phase P2 (Zobrist nogoods + Luby restart) should bite.
- The bitmap rewrite alone gave ~120-500× on 5-6×5-6 because:
  1. No HashMap allocation in hot loop (JFR 22 % → 0 % for bitmap allocations).
  2. Trail-based undo O(Δ) per backtrack vs full recompute.
  3. `AND`-mask filtering over `long[]` is vector-friendly.
  4. MRV via cached `domainSize[]` array (O(cells), no Long.bitCount per step).

---

## Bitmap solver P2 results — Zobrist nogoods + Luby restart (2026-04-18 22:50)

P2 integration: open-addressing nogood cache keyed by incremental Zobrist hash,
Luby restart sequence with randomized MRV tiebreaker (reservoir sampling).
Defaults: `useNogoods=true`, `useRestart=true`, `restartUnit=128`, seed=0xEA7E811E2L.

| Size | Seed | ref ms | P1 bitmap | P2 bitmap | P2 vs P1 |
|------|------|--------|-----------|-----------|----------|
| 6×6 | 1 | 29 338 | 79 | **28** | **×2.8 better** |
| 6×6 | 17 | 60 008 (timeout) | 227 | **116** | **×2.0 better** |
| 6×6 | 42 | 60 002 (timeout) | 493 | **357** | **×1.4 better** |
| 6×6 avg | — | — | 266 | **167** | **×1.6 better** |
| 7×7 | 1 | 60 000 (timeout) | **4 646** | **60 051 (timeout)** | ❌ **×13 regression** |
| 7×7 | 17 | 60 002 (timeout) | timeout | **40 722** | ✅ **newly solved** |
| 7×7 | 42 | 60 003 (timeout) | **12 705** | **40 558** | ❌ **×3 regression** |

### Consequences

- **P2 gate (2/3 seeds on 7×7 <60s)**: still held (seeds 17 & 42), but the
  subset changed — seed 1 regressed from 4.6s to timeout.
- **Randomized MRV tiebreaker hurts happy-path seeds**. When the static MRV
  order happens to hit a productive branch quickly, randomization pushes off it
  and Luby restart wipes useful work before the accumulated nogoods can amortize.
- **Restart unit 128 is too aggressive** for 7×7. First restart fires after
  128 dead-ends — nowhere near enough to build useful nogoods on a 49-cell
  search tree.
- **The fix is portfolio, not parameter tuning.** Run P1 (deterministic,
  no restart) and P2 (randomized, with restart) concurrently on different
  threads; the first to finish wins. That's phase P3 — the P2 regression
  actually *motivates* P3.
- **6×6 still improves** — nogoods help on smaller trees where the hash
  collisions converge faster than on 7×7.

---

## Bitmap solver P3 results — 4-worker portfolio (2026-04-18 23:15)

`ParallelBitmapSolver` runs 4 BitmapSolver workers concurrently with
diversified configs (1 deterministic P1-style, 3 randomized with different
seeds × restart units: 128, 256, 1024). First to find a solution wins;
others abort via shared `AtomicBoolean`.

| Size | Seed | P2 single ms | P3 portfolio ms | Speedup |
|------|------|--------------|-----------------|---------|
| 5×5 | 1 | 23 | 28 | 0.8× |
| 5×5 | 17 | 30 | 14 | 2.1× |
| 5×5 | 42 | 2 | 8 | 0.3× |
| 6×6 | 1 | 18 | 59 | 0.3× |
| 6×6 | 17 | 82 | 53 | 1.5× |
| 6×6 | 42 | 451 | 121 | 3.7× |
| **7×7** | **1** | **60 060 (timeout)** | **3 191** | **×18.8** |
| **7×7** | **17** | **39 808** | **27 281** | **×1.5** |
| **7×7** | **42** | **36 703** | **8 208** | **×4.5** |

### Consequences — gates

- **P3 gate (7×7 3/3 seeds <60s) ✅ achieved.** Portfolio resolves the P2
  seed=1 regression (×18.8 speedup) while keeping the P2 gains on seeds 17/42.
- 7×7 avg: P1 ~38s (2/3 seeds solved) → P3 **~13s (3/3 seeds solved)**.
- **Small-board penalty is real but acceptable.** 5×5 and 6×6 easy cases
  pay a thread-spawn overhead (~10-40 ms) that dominates when the puzzle
  solves in <100 ms single-thread. Choose `ParallelBitmapSolver` only
  when the single-thread time is expected to exceed ~500 ms.
- **P3 validates the portfolio strategy hypothesis:** restart + randomization
  is a *complement* to deterministic search, not a replacement. The
  deterministic worker catches happy-path puzzles; the randomized workers
  catch pathological branches.

### Next targets

- **8×8 generated**: not yet benchmarked. Extrapolating from 7×7 (~8-30 s
  on 4 threads) with ×25 per size step → expect 8×8 in the 200-750 s
  range on current hardware. P4 (CP-SAT fallback) remains the honest
  path for sub-60s 8×8.
- **Nogood sharing across workers** (sharded cache): deferred until
  measurements show thread-local caches are the bottleneck. Current 4 ×
  32 MB = 128 MB footprint is fine for local dev.

---

## Large-puzzle reference corpus (2026-04-19 00:47)

`BenchLargePuzzles` with 10-min per-run budget, `ParallelBitmapSolver`
4 workers (with shared nogood store enabled from commit 7f57050).
Every TIMEOUT row reports best-partial-depth + edges matched, so
future optimisations can be measured even on unsolved cases.

| Dim | Seed | Time | Status | Best | Partial edges | fp |
|-----|------|------|--------|------|---------------|----|
| 7×7 | 1 | 6 976 | SOLVED | 49/49 | 72/84 | 5ebce9d037bc720c |
| 7×7 | 17 | 42 624 | SOLVED | 49/49 | 72/84 | 26d03d08fba6bd24 |
| 7×7 | 42 | 10 709 | SOLVED | 49/49 | 72/84 | 67a8815a5113003c |
| 7×8 | 1 | 816 | SOLVED | 56/56 | 83/97 | 8a04ac26ecbf8ca8 |
| 7×8 | 17 | 5 360 | SOLVED | 56/56 | 83/97 | 505e761da18830e4 |
| 7×8 | 42 | 3 038 | SOLVED | 56/56 | 83/97 | 425226af308bda40 |
| 8×8 | 1 | 57 963 | SOLVED | 64/64 | 96/112 | 78d51fbf4f834028 |
| 8×8 | 17 | 453 805 | SOLVED | 64/64 | 96/112 | 9337a1e1a352cf2a |
| 8×8 | 42 | 601 506 | TIMEOUT | **60/64** | **99/112 (88 %)** | — |
| 8×9 | 1 | 235 423 | SOLVED | 72/72 | 110/127 | 6d5ce6520c48b6a8 |
| 8×9 | 17 | 601 511 | TIMEOUT | **66/72** | **110/127 (87 %)** | — |
| 8×9 | 42 | 601 508 | TIMEOUT | **66/72** | **110/127 (87 %)** | — |
| 9×9 | 1 | 601 002 | TIMEOUT | **77/81** | **132/144 (92 %)** | — |
| 9×9 | 17 | 601 004 | TIMEOUT | **76/81** | **128/144 (89 %)** | — |
| 9×9 | 42 | 601 005 | TIMEOUT | **75/81** | **127/144 (88 %)** | — |

### Summary

- **Solved 9/15** at 10-min budget (all 7×7, all 7×8, 2/3 8×8, 1/3 8×9, 0/3 9×9).
- **First 8×8 seed=17 and 8×9 seed=1 ever solved** in this repo.
- **TIMEOUT cases cluster** at 75-95 % of pieces placed — the portfolio
  consistently gets deep into the search but the final few placements
  form a combinatorial wall.
- All **fingerprints stable** across re-runs — the portfolio is
  reproducible per seed despite thread scheduling variance.

### Retry with 60-min budget (2026-04-19 06:27)

`BenchRetryTimeouts` re-ran the 6 TIMEOUT cases with a 60-min per-run
budget. Net effect:

| Dim | Seed | 10 min result | 60 min result | Δ |
|-----|------|---------------|---------------|---|
| 8×8 | 42 | TIMEOUT 60/64 (88 %) | TIMEOUT **62/64** (93 %) | +2 pieces, +6 edges |
| 8×9 | 17 | TIMEOUT 66/72 (87 %) | TIMEOUT **67/72** (88 %) | +1 piece, +2 edges |
| 8×9 | 42 | TIMEOUT 66/72 (87 %) | **SOLVED 25 min** | ✅ |
| 9×9 | 1 | TIMEOUT 77/81 (92 %) | TIMEOUT **77/81** (92 %) | **0** |
| 9×9 | 17 | TIMEOUT 76/81 (89 %) | TIMEOUT **76/81** (90 %) | +1 edge |
| 9×9 | 42 | TIMEOUT 75/81 (88 %) | TIMEOUT **76/81** (90 %) | +1 piece, +2 edges |

### Conclusions

- **6× more time bought 1 extra SOLVED** (8×9 seed=42). The budget
  elasticity is minimal on this regime.
- **9×9 seed=1 literally plateaued** — same best partial after 10 and
  60 min. The portfolio is fully stuck in dead branches.
- **Budget alone won't move the needle further.** The next wins
  require algorithmic changes: stronger propagation (AC-3 worklist
  replacing forward-checking), smarter value ordering (LCV), constraint
  weighting / VSIDS-style variable ordering, shared-nogood validation
  via A/B bench (ParallelBitmapSolver.setShareNogoods), or a different
  search topology (CP-SAT fallback — Phase P4).

### Full corpus status (2026-04-19 06:27)

10/15 seeds solved at 60-min budget; 5 TIMEOUTs clustered on 8×8/8×9
tail-end and all three 9×9 seeds. These 5 rows are the test corpus for
the next wave of optimisations — any change that solves even one of
them (or pushes best-partial by more than the +0-2 pieces we got from
6× budget) is a real algorithmic win, not time.

---

## Shared nogood store A/B (2026-04-19 09:23)

`BenchSharedNogoods` — same 3 hardest TIMEOUTs run with
`setShareNogoods(false)` vs `setShareNogoods(true)`, 30 min each side.

| Case | Shared OFF best | Shared ON best | Δ |
|------|----------------:|---------------:|--:|
| 8×8 seed=42 | 62/64 | 62/64 | **0** |
| 8×9 seed=17 | 66/72 | 67/72 | +1 |
| 9×9 seed=1 | 77/81 | 77/81 | **0** |

### Verdict

- **Shared nogood store does not break any wall.** +1 piece on one case,
  noise on the other two. The 4 portfolio workers explore sufficiently
  different regions that their Zobrist hashes rarely overlap enough to
  amortise through a shared cache.
- **Cost is minimal** (per-shard `synchronized`, contention low), so
  keeping it on by default doesn't hurt. But it's not a win either —
  the algorithmic wall is downstream.
- Next A/B target: fail-first heuristic (commit 4015f5c) on the same
  three cases, then AC-3 worklist.

---

## Fail-first A/B result (2026-04-19 12:30)

`BenchFailFirst` — 30-min budget per side, 3 hardest TIMEOUT cases.

| Case | OFF best | ON best | Δ |
|------|---------:|--------:|--:|
| 8×8 seed=42 | 62/64 | **61/64** | **−1** (regression) |
| 8×9 seed=17 | 66/72 | 66/72 | 0 |
| 9×9 seed=1 | 77/81 | 77/81 | 0 |

### Verdict

Fail-first is a dead end on this problem. "Pick the cell that fails
most often first" pushes the search into denser branches that, on
Eternity-style puzzles, have no reachable solution anyway. The −1
piece on 8×8 seed=42 confirms the regression signal.

Default reverted to `useFailFirst=false` (commit cb220df). Code and
setter kept for future experiments.

Next A/B target: singleton propagation (commit 5f4e97b) with 60-min
budget + time-to-best instrumentation (commit 27da17f).

---

## Main solver ultraplan ablation (2026-04-19 21:40)

After shipping the 5-item ultraplan (color budget, border-first,
nogoods, lookahead, LCV) on the main `EternitySolver` path,
`BenchmarkGrid` showed 6×6 regression 25 s → 98 s. Ablation
(`BenchMainSolverAblation`) isolated the culprit: the LCV sort was
re-running on every `tryPlacement` call, O(n log n) + Integer boxing
per cell, per recursion level. Fix in commit `86661f5`:
precompute the LCV order once, filter-by-order O(n) per call.

| Config | 5×5 | 6×6 |
|--------|----:|----:|
| default (pieceId asc) — features ON | 816 ms | **16 843 ms (2/3)** |
| LCV ON (precomputed) — features ON | **376 ms** | 58 664 ms (1/3) |
| LCV — features OFF | 2 150 ms | TIMEOUT (0/3) |
| pieceId — features OFF | 2 028 ms | 54 018 ms (1/3) |

### Verdict

- **LCV helps on 5×5** once the per-call overhead is eliminated
  (−54 % vs pieceId). 
- **LCV hurts on 6×6** even with precomputation — the fail-first
  ordering itself is the wrong choice for this puzzle structure,
  not the compute cost. Search dives into denser branches that the
  other prunings can't save.
- **Default kept as pieceId ascending**; LCV opt-in via `sortOrder("lcv")`.

All five ultraplan items now behave as opt-in toggles:
  - `useColorBudget` (default on — part of the ×2.5 speedup from features ON)
  - `setStrictBorderFirst` (default off)
  - `useNogoods` (default on — +33 % on 5×5 confirmed)
  - `usePreCheckLookahead` (default on — part of the ×2.5 features block)
  - `sortOrder("lcv")` (default off — −54 % on 5×5, +3.5× regression on 6×6)

**Features block net effect**: `pieceId + features OFF` vs default on 5×5
is 2 028 ms vs 816 ms (×2.5 slower without features) and on 6×6 is
54 018 ms (1/3) vs 16 843 ms (2/3) — the features give the main win,
LCV just shifts the trade-off. Overall **baseline 6×6 went from 25 s
(pre-ultraplan) to 16.8 s (post-ultraplan default, 2/3 solved)**, a
solid −33 % gain on the main solver hot path.
