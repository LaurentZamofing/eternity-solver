# Plan d'amélioration Eternity II Solver

> Plan généré 2026-04-17 après le fix AC-3 / sym-breaking (commits `34f693e`, `b8093e3`, `5e5d855`, `3d0f947`, `3de0d99`). Baseline : suite 1496 tests verte, flags lex+rotation activés par défaut.

## 1. Inventaire des problèmes

### 1A. Code trop long / responsabilités floues

| Fichier | LOC | Nature |
|---------|-----|--------|
| `solver/EternitySolver.java` | 537 | Façade obèse : ~80 setters/getters + builder logic |
| `util/SaveStateManager.java` | 513 | Factory statics + I/O + sérialisation |
| `solver/DomainManager.java` | 481 | AC-3 + cache + MRV index + TL restriction |
| `monitoring/controller/DashboardController.java` | 474 | ~20 endpoints Spring |
| `solver/ParallelSolverOrchestrator.java` | 457 | Orchestration multithread (complexité justifiée) |
| `util/state/SaveStateIO.java` | 456 | 4 writers privés avec même squelette |
| `monitoring/service/CellDetailsService.java` | 455 | Aggregation + cache + DB + formatting |
| `solver/heuristics/MRVCellSelector.java` | 450 | Complexité algo (tiebreakers) — accept |
| `solver/SymmetryBreakingManager.java` | 360 | Rules + compteurs diag + helpers |

**Méthodes > 50 LOC** : `EternitySolver.initializeManagers()`, `SaveStateManager.saveToDisk()`, `DomainManager.initializeAC3Domains()`, `DashboardController.getConfigs()`, `ParallelSolverOrchestrator.coordinateSearch()`.

### 1B. Duplication

- Pattern border-check `edges[0] == 0 && edges[3] == 0` répété (SymmetryBreakingManager, DomainManager, tests).
- `SaveStateIO` : 4 writers privés statiques avec même squelette (open/write/close).
- `DashboardController` : boilerplate `ResponseHelper.ok()` + mapping DTO par endpoint.

### 1C. Statics résiduels problématiques

- `util/SaveStateManager.java:52` — `private static boolean useBinaryFormat = false` (stateful singleton non thread-safe).
- `util/DebugHelper.java` — `private static boolean stepByStepEnabled` (flag global mutable).
- `solver/SharedSearchState.java` — utilisé comme global state via static factory.

### 1D. Tests & couverture

**@Disabled trouvés — 10 occurrences** :

| Fichier | Lignes | Raison |
|---------|--------|--------|
| `integration/SaveStateIntegrationTest.java` | 89, 126 | SaveStateIO.saveCurrentState() ne crée pas .bin ; getPuzzleSubDir retourne path différent |
| `integration/ParallelSolvingIntegrationTest.java` | 26, 103 | solveParallel() à investiguer |
| `integration/CLIIntegrationTest.java` | 107, 138, 169 | Requires Maven classpath — `mvn verify` only |
| `solver/ConstraintPropagatorTest.java` | 63, 77, 160 | Requires realistic FitChecker |

- 5 `@Tag("slow")` — OK, utilisés par CI.
- **Packages sans test** : `solver/debug/`, `solver/validation/` (PlacementValidator), `solver/output/`, `solver/timeout/`, `monitoring/util/`, `config/`.
- **Tests volumineux** : `SymmetryBreakingBugTrackingTest` 731 LOC (à scinder), `RecordManagerTest` 681, `MRVPlacementStrategyTest` 676.
- **Couverture réelle non mesurée** — gate fixé à LINE ≥ 20 % / BRANCH ≥ 18 %.

### 1E. Structure / dépendances

- **49 interfaces** — bonne base strategy patterns.
- **0 cycle d'import** détecté.
- **Interface `Solver` manquante** : EternitySolver/BacktrackingSolver/HistoricalSolver sans contrat commun.
- **Incohérence rendering** : `BoardFormatter` interface vs `AbstractBoardRenderer` abstract class.

### 1F. Performance algorithme

- `restoreAC3Domains` O(W·H) par backtrack (fix récent) — acceptable 4×4/8×8, coûteux 16×16.
- AC-3 : `piecesById.get(vp.pieceId)` via HashMap dans boucle chaude (`ConstraintPropagator.java:121`).
- MRV PriorityQueue désactivé par défaut (−30 % sur 4×4, non benché 8×8+).
- Allocations `new HashMap<>(availableDomain)` à chaque setDomain (CP ligne 155).

### 1G. Performance système

- Pas de pool `Placement`.
- `ArrayList<ValidPlacement>` instanciée dans computeDomain.
- `stats_history.jsonl` non batché (à confirmer).
- Spring Boot démarre même en mode solver pur (~2 s).

### 1H. CI / DX

- 3 jobs ci.yml : `build-and-test` (30 min), `slow-tests` (15 min), `code-quality` (15 min).
- `perf-gate.yml` (5 min) : JMH `solve3x3` vs baseline.
- JaCoCo LINE ≥ 20 % / BRANCH ≥ 18 % (blocking). PMD/Checkstyle informational.
- Pas de SpotBugs, pas de mutation testing.
- Pas de Makefile ; SCRIPTS.md existe.

### 1I. Algorithmie R&D

- DLX absent.
- `Piece.edgesRotated` pré-calculé (ok). Mais `piecesById.get()` HashMap chaud → remplacer par `Piece[]`.
- MRV tie-breaker : domain size + occupied neighbors. Alternative : "most-constraining variable".

---

## 2. Tableau priorisé

Score = (impact × confiance) / coût. I/C/C sur 1-5.

### Quick wins (< 1 h)

| ✔ | # | Action | Commits |
|:-:|---|--------|---------|
| ✅ | QW1 | Nettoyer @Disabled (document ou supprimer) | `e105353` |
| ✅ | QW2 | Mesurer coverage (`mvn jacoco:report`) | `e105353` |
| ✅ | QW3 | Cache edges sur ValidPlacement (+ helpers `Piece.isTopLeftFittable*`) | `8651d2e` |
| ✅ | QW4 | Extract `Piece.isTopLeftFittable(rot)` | `8651d2e` |
| ✅ | QW5 | Tests PlacementValidator/DebugPlacementLogger/TimeoutChecker | `e105353` |
| ✅ | QW6 | Baseline JMH publiée (`.github/perf-baseline.json`) | déjà à jour |
| ✅ | QW7 | PMD `printFailingErrors=true` (335 violations visibles en CI ; strict-gate ruleset reporté à cleanup dédié) | `b2dfd8e` |

### Medium (½ journée)

| ✔ | # | Action | Commits |
|:-:|---|--------|---------|
| ✅ | M1 | AC-3 undo-stack O(Δ) | `acff823` |
| ✅ | M2 | Interface `Solver` | `acff823` |
| ✅ | M3 | Bench MRV PQ 6×6/8×8 — `PuzzleGenerator` + benchmarks 5×5/6×6/8×8 dans `FullSolveBenchmark` | `f4f8389` + `5ec56aa` |
| ✅ | M4 | `EternitySolverBuilder` fluent (9 setters communs, factory `EternitySolver.builder()`) | `b2856aa` |
| ✅ | M5 | Dé-staticisation : `DebugHelper` instance-based (DEFAULT singleton, API deprecated), `useBinaryFormat` supprimé | `5ec56aa` + `b2856aa` |
| ✅ | M6 | Template `SaveStateIO.writeSection` | `e39a615` |
| ⏳ | M7 | Pool `ArrayList<ValidPlacement>` — dépend d'un profil JFR (BB2). Non prioritaire tant que 4×4 est rapide. | pending |
| ✅ | M8 | JaCoCo gate 20/18 → 24/22 → **35/30** | `7d12778` + `fb3ecb6` |
| ✅ | M9 | Split SymmetryBreakingBugTrackingTest | `27acd09` |
| ⏳ | M10 | Spring Boot profil `solver-only` — optionnel (monitoring déjà isolé de la CLI core) | pending |

### Big bets (> 1 jour) — green-lit 2026-04-18

| ✔ | # | Action | État |
|:-:|---|--------|------|
| 🔨 | BB1 | POC DLX (Dancing Links) | **en cours** — tasks #37..#42 créées (estim. 5h) |
| ⏳ | BB2 | Scaling 16×16 (pools, int[], JFR, GC tuning) | démarre après bench 8×8 avec outillage JFR |
| 🔶 | BB3 | Profil contention WorkStealingExecutor | fix `solveParallel` livré (`68c1947`), profil JFR à suivre |
| ⏳ | BB4 | Heuristique "most-constraining variable" | optionnel |
| ⏳ | BB5 | Mutation testing PITest | coverage 43% >= 30% ✅, peut démarrer |

**Légende** : ✅ fait · 🔶 partiel · 🔨 en cours · 🟢 green-lit · ⏳ pending

### Récap global

- **QW** : 7/7 ✅
- **Medium** : 8/10 ✅, 2 ⏳ (M7 attend JFR, M10 non-prioritaire)
- **Big bets** : 1 🔨 en cours (BB1), 1 🔶 (BB3), 3 ⏳

---

## 3. Plan d'exécution

### **Chantier 1 — Stabilisation & mesure** (1-2 j)
Tâches : QW1 + QW2 + QW5 + QW6 + M9.
- Nettoyer 10 `@Disabled` (supprimer ou tagguer).
- `mvn jacoco:report` + publier chiffres.
- +3 tests minimaux (PlacementValidator, Debugger, TimeoutChecker).
- Publier baseline JMH post-fix dans `.github/perf-baseline.json`.
- Split `SymmetryBreakingBugTrackingTest` (731 → regression + diag).
- **Risque** : faible. **Métrique** : 0 @Disabled orphelin, coverage publiée, baseline JMH en place.

### **Chantier 2 — Quick wins perf AC-3** (1 j)
Tâches : QW3 + QW4 + M1.
- `Piece[]` indexé par ID au lieu de HashMap dans ConstraintPropagator hot path.
- Helper `Piece.isTopLeftFittable(rot)` + remplacer duplicates.
- **AC-3 undo-stack O(Δ)** : log deltas pendant `propagateAC3`, pop au backtrack → remplace recompute O(W·H).
- Fichiers : `DomainManager.java`, `ConstraintPropagator.java`, `Piece.java`, `SymmetryBreakingManager.java`.
- **Risque** : moyen — hot path. Mitigation : AC3CorrectnessTest + bench avant/après.
- **Métrique** : bench 4×4 hard ≥ 15 % + ; 8×8 cible −25 %.

### **Chantier 3 — Structure & DI** (2 j)
Tâches : M2 + M4 + M5 + M6.
- Interface `Solver` unifie EternitySolver/BacktrackingSolver/HistoricalSolver.
- `EternitySolverBuilder` fluent extrait (50+ setters).
- Dé-statique : `SaveStateManager.useBinaryFormat`, `DebugHelper.stepByStepEnabled` → context/DI.
- Template method `SaveStateIO.writeSection(header, content)` factorise 4 writers.
- **Risque** : moyen — large surface. Mitigation : PRs séparées, 1 interface à la fois.
- **Métrique** : EternitySolver 537 → ≤ 350 LOC ; 0 static mutable.

### **Chantier 4 — Coverage & gates** (1 j)
Tâches : M3 + M8 + QW7.
- Bench MRV PQ 6×6/8×8 → seuil d'activation dynamique.
- JaCoCo 20 → 30 % LINE, 18 → 25 % BRANCH.
- PMD/Checkstyle `failOnViolation=true` règles critiques.
- **Risque** : moyen — relève gates bloque CI. Mitigation : combler packages d'abord.
- **Métrique** : JaCoCo 30/25 passant, MRV PQ bench publié, 0 PMD violation.

### **Chantier 5 — Scaling algorithme** (3-5 j, R&D) — ✅ **GO validé 2026-04-18**
Tâches : **BB1 + BB2 + BB3** (bench contention parallèle ajouté).

#### BB1 — POC Dancing Links (DLX)
- Ajouter `solver/experimental/dlx/` avec `DancingLinksSolver` indépendant.
- Conversion `(board, pieces)` → exact-cover matrix (cell constraints + piece-used constraints).
- Bench JMH `DLXBenchmark.solve4x4Hard` + `solve8x8` à créer.
- **Go/no-go** : si DLX ≥ 2× plus rapide qu'AC-3 sur 4×4 hard → continuer vers 8×8 ; sinon documenter no-go + supprimer.

#### BB2 — Scaling 16×16 (allocation + JFR)
- `jfr` profil sur solve 8×8 long (≥ 30 s) → identifier top allocations.
- Pools : si `Placement` ou `ArrayList<ValidPlacement>` > 20 % CPU, introduire pools thread-local.
- Migration hot path `HashMap<Integer, Piece>` → `Piece[] piecesByIndex` (déjà partiellement fait via cache `edges` dans C2).
- Bench gains cumulés : baseline 4×4 post-C2 vs post-BB2 — cible −30 % time.

#### BB3 — Contention WorkStealingExecutor
- JFR `lock-contention` profil sur solve parallèle 8×8, 2/4/8 threads.
- Identifier locks chauds sur `SaveStateIO` / `RecordManager`.
- Redesign : déplacer écriture saves hors du chemin chaud (queue + writer thread dédié).

- **Risque** : élevé, expérimental. Mitigation : tout sous flag off par défaut, PRs séparées.
- **Métrique** : 
  - DLX POC bench documenté ≥ 4×4 hard
  - Scaling : solve 8×8 (puzzle à créer) < 60 s  
  - Parallèle : scalabilité ≥ 1.8× sur 4 threads

---

## 4. Anti-features

1. **Ne pas activer MRV PQ par défaut** avant M3.
2. **Ne pas refactorer monitoring Spring Boot** — stable, isolé, gain faible.
3. **Ne pas ajouter framework CSP générique** (Choco/OptaPlanner).
4. **Pas de mutation testing** avant coverage > 30 %.
5. **Ne pas chasser tous les statics** — factory statics OK.
6. **Pas de pool `Placement`** avant profil JFR.
7. **Pas de DLX en prod** avant bench 8×8 validant le gain.

---

## 5. État d'avancement

| Chantier | Statut | Commits | Progress |
|----------|--------|---------|----------|
| C1 Stabilisation & mesure | **terminé** | `e105353`, `27acd09` | @Disabled 10→4, +3 tests, coverage baseline, split 731→175+375 |
| C2 Quick wins perf AC-3 | **terminé** | `8651d2e`, `acff823` | QW3+QW4 (cache edges, isTopLeftFittable) + M1 (undo-stack O(Δ)) |
| C3 Structure & DI | **partiel** | `acff823` | Interface `Solver` créée. M4/M5/M6 reportés (large surface, hors budget session). |
| C4 Coverage & gates | **partiel** | `7d12778`, `f4f8389` | Gate 20/18 → 24/22. +QuietPlacementOutputTest. M3 bench MRV bloqué sur puzzle 6×6+ à créer. |
| C5 Scaling (R&D) | **go validé** | — | BB1 + BB2 + BB3 approuvés. Démarre plus tard. |

### Étapes détaillées Chantier 1

#### Étape 1.1 — Nettoyer les `@Disabled` (10 trouvés, 3 traités)

Fichier par fichier :

**`src/test/java/solver/ConstraintPropagatorTest.java` — 3 `@Disabled` traités ✅**
- Cause : mock `FitChecker → true` rendait l'AC-3 trivial ; tests avaient pieces à 3 IDs random incompatibles avec bordures.
- Action :
  1. Remplacé mock par `realisticFitChecker()` (check bordures 0 + match edges voisins placés) — mirror `PlacementValidator.fits()`.
  2. Remplacé `createTestPieces()` 3 pieces random par `PuzzleFactory.createExample3x3()` (vrai puzzle, 9 pieces solvables).
  3. Remplacé hard-codés `pieces.get(1)` + totalPieces `3` par `tlPieceId` / `tlRotation` calculés via `findTLPlaceable()` (première piece avec N=0, W=0 dans le puzzle).
  4. `testFilterDomainEdgeMatching` utilise un `localPieces` Map avec les anciennes pieces {1,2,3,4}/{5,6,7,8} pour garder sa sémantique edge-exacte.
  5. Retiré les 3 `@Disabled` et l'import.
- Validation : `mvn test -Dtest=ConstraintPropagatorTest` → **16/16 passes**, 0 skipped (avant : 13 passes, 3 skipped).

**Résumé après traitement** : 10 → **4 restants**, tous documentés.
- `integration/SaveStateIntegrationTest.java:88-89, 125-126` — feature binary format non utilisée en prod ; comment éclairci.
- `integration/ParallelSolvingIntegrationTest.java:26, 103` — `solveParallel()` return false sur 2×2 solvable (testé en retirant `@Disabled` → confirmé bug indépendant). Remis en @Disabled avec ref plan.
- `integration/CLIIntegrationTest.java` — **3 subprocess tests supprimés** (path `bin:lib/*` inexistant) ; 1 @Disabled retiré car redondant avec `@Tag("slow")` classe-level.

#### Étape 1.2 — Coverage baseline

Commande : `mvn verify -B` (exit 0, 2 min 44 s).

| Métrique | Couverture | Missed / Total |
|----------|------------|----------------|
| **Instructions** | 26 % | 33 409 / 45 471 |
| **Branches** | 24 % | |
| **Lines** | 25 % | 7 808 / 10 369 |
| **Methods** | 25 % | 1 253 / 1 674 |
| **Classes** | 41 % | 123 / 208 |

Gate actuel JaCoCo : LINE ≥ 20 %, BRANCH ≥ 18 % → **passant avec marge** (~ +5 pts sur LINE, +6 pts sur BRANCH).
Target Chantier 4 : LINE 30 %, BRANCH 25 % — atteignable avec +5 points via tests sous-packages manquants.

#### Étape 1.3 — Nouveaux tests ajoutés

| Fichier | Tests | Scope |
|---------|-------|-------|
| `solver/timeout/TimeoutCheckerTest.java` | 6 | `isTimedOut`, `getElapsedTimeMs`, `getRemainingTimeMs`, `getTimeProgress` (y compris edge-case `maxExecutionTimeMs=0`) |
| `solver/validation/PlacementValidatorTest.java` | 6 | `ValidationResult.valid/rejected`, `RejectionReason` enum contract, nullable `symmetryBreakingManager`, `getDomainManager` |
| `solver/debug/DebugPlacementLoggerTest.java` | 3 | smoke tests via Logback ListAppender (marker "piece 7", no-throw sur constructors) |

Validation : `mvn test -Dtest=TimeoutCheckerTest,PlacementValidatorTest,DebugPlacementLoggerTest` → **15/15 passes** (nouveaux) ; suite complète **1511/1511 passes, 9 skipped** (stable vs 1496 avant).

#### Étape 1.4 — Baseline JMH ✅

`.github/perf-baseline.json` est déjà à jour (`last_updated: 2026-04-17`, `FullSolveBenchmark.solve3x3` baseline 45 ms/op avec 20 % threshold, notes mentionnant "post AC-3 incremental restore"). Les commits AC-3 récents ont déjà incorporé le ajustement, pas de modif nécessaire. Gate `perf-gate.yml` actif.

#### Étape 1.5 — Split SymmetryBreakingBugTrackingTest ✅

731 LOC → 175 (regression-only) + 375 (diagnostic).
- `SymmetryBreakingBugTrackingTest` garde : 4 TL-rule tests + nested `RegressionCombos` (4 tests) + 2 helpers.
- Nouveau `SymmetryBreakingDiagnosticTest` : 11 tests diagnostic [A1.x / A2.x] + 1 helper `realisticFit()`.
- Validation : `mvn test` des deux → **19/19 passes**.

### Historique commits liés au plan

```
(baseline — bug fixes pré-plan)
34f693e fix: TL domain pre-filter eliminates AC-3 ↔ sym-breaking conflict
b8093e3 fix: re-apply TL domain filter after backtrack/recompute
5e5d855 diag: isolate 4x4 hard bug to AC-3 propagation (pre-existing, not sym-breaking)
3d0f947 fix: restoreAC3Domains recomputes all cells, not just (r,c) + neighbors
3de0d99 chore: remove obsolete archive and root-level MD files

(C1 stabilisation & mesure)
e105353 chore(tests): C1 stabilisation — @Disabled cleanup, 3 new test classes, plan doc
27acd09 refactor(tests): split SymmetryBreakingBugTrackingTest (731 → 175 + 375 LOC)

(C2 quick wins perf AC-3)
8651d2e perf(ac3): cache rotated edges on ValidPlacement, add Piece.isTopLeftFittable
acff823 perf(ac3): undo-stack for restoreAC3Domains — O(Δ) instead of O(W·H)

(C3 structure & DI — partiel)
acff823 (idem : introduit l'interface Solver)

(C4 coverage & gates — partiel)
7d12778 chore(ci): raise JaCoCo gate to LINE 24% / BRANCH 22%
f4f8389 feat(solver): expose setMRVIndexEnabled on EternitySolver + test solver/output
```

**Total session (suite)** : 18 commits après la baseline, suite 1520/1520 verte, coverage LINE **43%** / BRANCH **42%** (gates bumped 24/22 → 35/30), **2 @Disabled** restants tous documentés (CLIIntegrationTest subprocess slow-tests).

### Commits suite après session close-out initial

```
(M5, M6, QW7, dead-code cleanup, puzzle generator)
e39a615 refactor(state): dedupe writeSection boilerplate in SaveStateIO
b2dfd8e chore(pmd): surface violations in CI logs (printFailingErrors=true)
5ec56aa feat(bench): PuzzleGenerator + 5×5/6×6/8×8 JMH benchmarks
a385442 chore(cleanup): remove dead code (5 items, -295 LOC)
```

### Tasks d'unblock créées

| # | Débloque | Description |
|---|----------|-------------|
| #32 | M3 bench MRV PQ | Créer `createExample6x6` + data file (ou utiliser `PuzzleGenerator.generate(6)` — déjà fait dans les benchmarks) |
| #33 | M4 | EternitySolverBuilder fluent |
| #34 | 2 @Disabled + BB3 | Fix `solveParallel()` bug (retourne false sur 2×2 solvable) |
| #35 | 2 @Disabled | Décider binary-save format (garder/wire CLI/supprimer) |
| #36 | (fait) | ✅ Dead code + TODO audit → commit `a385442` |

### Prochaines actions (après session courante)

**C3 (reste)** :
1. Extraire `EternitySolverBuilder` fluent depuis les ~15 setters sur `EternitySolver` (ciblé: 537 → ≤ 350 LOC). M4.
2. Dé-statique `SaveStateManager.useBinaryFormat` (utilisé seulement dans test @Disabled — à supprimer ou rendre instance-level) + `DebugHelper.stepByStepEnabled`. M5.
3. Template method `SaveStateIO.writeSection(header, content)` factorisant les 4 writers. M6.

**C4 (reste)** :
1. **M3 bench MRV PQ 6×6/8×8** : nécessite d'abord un puzzle 6×6 (non disponible dans `PuzzleFactory`). Créer `createExample6x6()` + bench comparatif dans `FullSolveBenchmark` avec `setMRVIndexEnabled(true/false)`.
2. Continuer à monter JaCoCo gate de 24/22 → 30/25 après +3-5 tests sur packages sous-couverts (`config/`, `monitoring/util/`).
3. PMD/Checkstyle `failOnViolation=true` sur règles critiques (wildcard imports déjà géré en hook séparé).

**C5 (green-lit, après C3/C4)** :
1. BB1 — DLX POC sous `solver/experimental/dlx/` + bench comparatif.
2. BB2 — JFR profil solve 8×8 → pools allocs.
3. BB3 — JFR lock-contention sur solveParallel (en parallèle avec fix `solveParallel` actuellement @Disabled).

### Tracker tâches
- #28 C1 Stabilisation — `in_progress`
- #29 C2 AC-3 perf — `pending`
- #30 C3 Structure & DI — `pending`
- #31 C4 Coverage & gates — `pending`
- #12 C5 DLX POC — `pending` (pre-existing)
