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
