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
