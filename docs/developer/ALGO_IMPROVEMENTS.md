# Algo-by-algo improvements — tracked execution

Per-algorithm audit lives in
`/Users/laurentzamofing/.claude/plans/bubbly-waddling-wirth.md`. This
document tracks the **execution** of each proposed optimisation, with
one row per optim and a measured gain line.

## Baseline — robust measurement (2026-04-19 22:30)

Reference bench: `benchmark.BenchMainSolverRobust` — JIT warmup (5
solves), 5 seeds, 3 repeats per (config, size, seed) = 15 samples per
row. Config `default (pieceId + features ON)`, 30 s per-run timeout.
Commit 4e37e73. The baseline is **after commit `4e37e73`** (which
includes #5 — lookahead skipped when AC-3 on).

| Config | 5×5 median | 5×5 min | 6×6 median | 6×6 solved |
|--------|-----------:|--------:|-----------:|-----------:|
| **default (features ON)** | **264 ms** | 183 | **9 964 ms** | **9/15** |
| LCV ON | 294 | 56 | 30 000 (timeout) | 6/15 |
| features OFF | 587 | 227 | 30 000 | 0/15 |
| LCV + features OFF | 1019 | 77 | 30 000 | 3/15 |

**Net gain from the ultraplan features block**:
- 5×5: features OFF median 587 → features ON median 264 ⇒ **×2.2**
- 6×6: features OFF 0/15 solved → features ON 9/15 ⇒ **critical**
- pre-ultraplan baseline (commit e83babe) 6×6 mean ≈ 25 s (2/3 at 120 s) →
  current median 10 s at 30 s budget ⇒ **~×2.5 faster median**

Each future improvement re-runs this bench and records the new
median/solved in the "Measured gain" column. Cumulative delta — each
row builds on the previous tree state.

## Roadmap

Legend: status = TODO · IN PROGRESS · DONE · SKIPPED · WORSE
Gain = before → after (delta). Only the ROI #1 optim from the audit is
listed per algo; additional ones added as they become interesting.

| # | Algo / Optim | Effort | Expected | Status | Commit | Measured gain (5×5 / 6×6) |
|--:|-------------|--------|----------|--------|--------|---------------------------|
| 1 | AC-3 — bitset domains | 2-3 d | ×3-5 | TODO | — | — |
| 2 | MRV — priority queue incremental | 4-8 h | ×2 | TODO | — | — |
| 3 | LCV — dynamic score on domains | 4-6 h | débloque 6×6 | TODO | — | — |
| 4 | Color budget — incremental supply/demand | 4-6 h | 10-30 % | TODO | — | — |
| 5 | Pre-commit lookahead — remove when AC-3 on | 30 min | 10-20 % | TODO | — | — |
| 6 | Zobrist nogoods — conflict-directed minimal | 2-3 d | ×3-10 | TODO | — | — |
| 7 | Singleton — event-driven queue | 2-4 h | O(cells)→O(1) | TODO | — | — |
| 8 | EdgeCompatIndex — BitSet replace HashSet | 2-3 h | ×5-10 lookup | TODO | — | — |
| 9 | Bitmap DFS — CBJ (conflict-directed backjumping) | 3-5 d | ×2-10 | TODO | — | — |
| 10 | Trail undo — unify domainSize trail | 2 h | marginal | TODO | — | — |
| 11 | FC bitmap — full AC-3 cascade | 1-2 d | catche 2-hop | TODO | — | — |
| 12 | Luby restart — phase saving | 4-6 h | rec. progrès | TODO | — | — |
| 13 | Singleton propagation — delete (regression) | 30 min | simplicité | TODO | — | — |
| 14 | Portfolio — corner-anchor diversification | 4-6 h | ×2-4 | TODO | — | — |
| 15 | Shared nogoods — CAS lock-free | 1 d | ×2-3 contention | TODO | — | — |
| 16 | Best partial — edge-match score tracking | 1 h | observabilité | TODO | — | — |

## Priority bands (ROI-ranked)

| Band | # | Optim | Gain | Effort | ROI | Notes |
|------|--:|-------|-----:|-------:|----:|-------|
| **P0** | 5 | Remove lookahead when AC-3 ON | 10-20 % | 0.5 h | 30 | Duplication avec AC-3 |
| | 13 | Delete singleton propagation | cleanup | 0.5 h | — | Déjà off default |
| | 3b | LCV border-only | +50 % 5×5 | 1 h | 50 | Bypass intérieur |
| | 3c | Tester MCV (invert LCV) | spéculatif | 0.2 h | test | Cheap experiment |
| **P1** | 8 | EdgeCompatIndex → BitSet | ×5-10 lookup | 3 h | 200-400 | Local chaud |
| | 16 | Best partial: edge-match score | observabilité | 1 h | — | |
| | 10 | Unify domainSize trail | marginal | 2 h | low | |
| **P2** | 14 | Portfolio corner-anchor | ×2-4 effectif | 5 h | 50-80 | Sous-arbres disjoints |
| | 4 | Color-budget incrémental | 10-30 % | 5 h | 20-60 | |
| | 2 | MRV priority-queue | ×2 hot loop | 6 h | 33 | Infra partielle |
| | 12 | Luby phase saving | récup progrès | 5 h | — | |
| | 7 | Singleton event-queue | O(cells)→O(1) | 3 h | local | |
| | 3a | LCV dynamique | débloque 6×6+ ? | 5 h | spéculatif | |
| **P3** | 1 | **AC-3 bitset domains** | **×3-5 global** | 20 h | 15-25 | **Foundation #6, #8, #11** |
| | 9 | Bitmap CBJ | ×2-10 | 30 h | 7-33 | |
| | 6 | Nogood minimal | ×3-10 | 20 h | 15-50 | |
| | 15 | Shared nogoods CAS | ×2-3 contention | 8 h | 25-37 | |
| | 11 | Full AC-3 bitmap | 2-hop cascade | 12 h | specific | Débloqué par #1 |

## Sprint plan

**Sprint 1 (today)** — P0 + P1 en série: #5 → #13 → #3b → #3c → #8 → #10 → #16
**Sprint 2** — P2 prioritisé gain: #14 → #4 → #2
**Sprint 3** — P3 #1 AC-3 bitset (foundation)

## Per-item log

### #5 — Pre-commit lookahead: remove when AC-3 is enabled

Status: TODO
Commit: —
Hypothesis: `wouldCauseDeadEnd` detects the same dead-ends as AC-3's
first propagation pass. When AC-3 is on, the lookahead is duplicate
work — disabling it saves one pre-commit scan per tryPlacement
attempt.
Measurement: re-run `BenchMainSolverAblation` default config.

### #13 — Singleton propagation: delete

Status: TODO
Commit: —
Hypothesis: feature is off by default since commit `2223fc4` due to
measured regression. Removing the code simplifies `BitmapSolver` with
no perf impact (default state unchanged).
Measurement: run `BitmapSolverTest` + `BitmapDifferentialTest` to
confirm no correctness regression.

(Other items added as they move to IN PROGRESS.)
