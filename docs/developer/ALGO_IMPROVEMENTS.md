# Algo-by-algo improvements — tracked execution

Per-algorithm audit lives in
`/Users/laurentzamofing/.claude/plans/bubbly-waddling-wirth.md`. This
document tracks the **execution** of each proposed optimisation, with
one row per optim and a measured gain line.

## Baseline — robust measurement (2026-04-20)

Reference bench: `benchmark.BenchMainSolverRobust` — JIT warmup (5
solves), 5-8 seeds, 2-3 repeats per (config, dim×palette) = 10-16
samples per row. Per-size timeouts: 5×5 = 30-60 s, 6×6 = 120-180 s,
7×7 = 300 s. Commit 4e37e73+ (#10 unify sizeTrail applied).

### Full matrix — all four configs × five (dim, palette) cases

| Config | 5×5/p4 | 5×5/p6 | 6×6/p5 | 6×6/p7 | 7×7/p6 |
|--------|-------:|-------:|-------:|-------:|-------:|
| **default (pieceId + features ON)** | **221** | 54 | **10 325** | 130 | timeout |
| LCV ON | 270 | 67 | 28 611 | 194 | timeout |
| features OFF | 428 | **48** | 48 996 | **90** | timeout |
| LCV + features OFF | 434 | 43 | 82 781 | 93 | timeout |

(Values = median ms over samples.)

### Solved counts

| Config | 5×5/p4 | 5×5/p6 | 6×6/p5 | 6×6/p7 | 7×7/p6 |
|--------|:------:|:------:|:------:|:------:|:------:|
| default | 16/16 | 10/10 | **14/16** | 10/10 | 0/3 |
| LCV ON | 16/16 | 10/10 | 13/16 | 10/10 | 0/3 |
| features OFF | 16/16 | 10/10 | 12/16 | 10/10 | 0/3 |
| LCV + features OFF | 16/16 | 10/10 | 10/16 | 10/10 | 0/3 |

### Key findings

1. **Features block gives ×4.7 on 6×6/p5 median** (10 325 vs 48 996)
   and converts 2 extra seeds from timeout to solved. This is the
   biggest measured gain.
2. **Features block *hurts* on easy puzzles** — 6×6/p7 median 130
   (ON) vs 90 (OFF) = −30 % regression from overhead > gain. 5×5/p6
   shows the same pattern (54 vs 48).
3. **LCV regresses consistently** — ~20 % on 5×5, ×2.8 on 6×6/p5.
   `sortOrder("lcv")` is correctly left off by default.
4. **7×7 is unreachable** by the main solver with or without features
   at 5 min. Portfolio-bitmap remains the 7×7+ path.

### Comparison to pre-ultraplan baseline

- 5×5: pre 1 030 ms mean → post 221 ms median ⇒ **×4.7 faster**
- 6×6: pre 25 269 ms mean (2/3 solved @ 120 s) → post 10 325 ms median
  (14/16 @ 120 s) ⇒ **×2.4 faster median + 3× better solve rate**

## Roadmap

Legend: status = TODO · IN PROGRESS · DONE · SKIPPED · WORSE
Gain = before → after (delta). Only the ROI #1 optim from the audit is
listed per algo; additional ones added as they become interesting.

| # | Algo / Optim | Effort | Expected | Status | Commit | Measured gain |
|--:|-------------|--------|----------|--------|--------|----------------|
| 1 | AC-3 — bitset domains | 2-3 d | ×3-5 | TODO | — | — |
| 2 | MRV — priority queue incremental | 4-8 h | ×2 | TODO | — | — |
| 3 | LCV — dynamic score on domains | 4-6 h | débloque 6×6 | TODO | — | — |
| 4 | Color budget — incremental supply/demand | 4-6 h | 10-30 % | TODO | — | — |
| 5 | Pre-commit lookahead — remove when AC-3 on | 30 min | 10-20 % | **DONE** | a22ef17 | noise-level (bench variance > gain) |
| 6 | Zobrist nogoods — conflict-directed minimal | 2-3 d | ×3-10 | TODO | — | — |
| 7 | Singleton — event-driven queue | 2-4 h | O(cells)→O(1) | TODO | — | — |
| 8 | EdgeCompatIndex — BitSet replace HashSet | 2-3 h | ×5-10 lookup | TODO | — | — |
| 9 | Bitmap DFS — CBJ (conflict-directed backjumping) | 3-5 d | ×2-10 | TODO | — | — |
| 10 | Trail undo — unify domainSize trail | 2 h | marginal | TODO | — | — |
| 11 | FC bitmap — full AC-3 cascade | 1-2 d | catche 2-hop | TODO | — | — |
| 12 | Luby restart — phase saving | 4-6 h | rec. progrès | TODO | — | — |
| 13 | Singleton propagation — delete (regression) | 30 min | simplicité | **DONE** | b70accf | dead-code removed (−153 LOC) |
| 14 | Portfolio — corner-anchor diversification | 4-6 h | ×2-4 | TODO | — | — |
| 15 | Shared nogoods — CAS lock-free | 1 d | ×2-3 contention | TODO | — | — |
| 16 | Best partial — edge-match score tracking | 1 h | observabilité | **DONE** | (no-op) | already via `writeBestTo` + `Board.calculateScore()` |

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
  Status: #5 ✅ · #13 ✅ · #3b ✅ (commit f583c9d, awaiting bench)
         · #3c ✅ (commit 46c5175, awaiting bench) · #16 ✅ (no-op)
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
