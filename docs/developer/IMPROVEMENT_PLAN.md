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

| # | Action | I | C | Cost | Score |
|---|--------|---|---|------|-------|
| QW1 | Nettoyer @Disabled (document ou supprimer) | 4 | 5 | 1 | 20 |
| QW2 | Mesurer coverage (`mvn jacoco:report`) | 3 | 5 | 1 | 15 |
| QW3 | Piece[] au lieu de HashMap dans hot paths | 4 | 4 | 1 | 16 |
| QW4 | Extract `Piece.isTopLeftFittable(rot)` | 2 | 5 | 1 | 10 |
| QW5 | Tests PlacementValidator/Debugger/TimeoutChecker | 3 | 5 | 1 | 15 |
| QW6 | Baseline JMH publiée | 3 | 5 | 1 | 15 |
| QW7 | PMD/Checkstyle failOnViolation sur règles critiques | 2 | 4 | 1 | 8 |

### Medium (½ journée)

| # | Action | I | C | Cost | Score |
|---|--------|---|---|------|-------|
| M1 | AC-3 undo-stack O(Δ) remplacent recompute O(W·H) | 5 | 4 | 3 | 6.7 |
| M2 | Interface `Solver` (mockabilité) | 3 | 5 | 3 | 5 |
| M3 | Bench MRV PQ 6×6/8×8 — seuil de basculement | 4 | 4 | 3 | 5.3 |
| M4 | `EternitySolverBuilder` fluent | 3 | 4 | 3 | 4 |
| M5 | Dé-staticiser SaveStateManager/DebugHelper flags | 3 | 5 | 3 | 5 |
| M6 | Template `SaveStateIO.writeSection` | 2 | 5 | 3 | 3.3 |
| M7 | Pool `ArrayList<ValidPlacement>` | 3 | 3 | 3 | 3 |
| M8 | JaCoCo 20→30 %, 18→25 % progressif | 3 | 5 | 2 | 7.5 |
| M9 | Split SymmetryBreakingBugTrackingTest (731 LOC) | 2 | 5 | 2 | 5 |
| M10 | Spring Boot profil `solver-only` auto-skip | 3 | 3 | 3 | 3 |

### Big bets (> 1 jour)

| # | Action | I | C | Cost | Score |
|---|--------|---|---|------|-------|
| BB1 | POC DLX (Dancing Links) | 5 | 3 | 5 | 3 |
| BB2 | Scaling 16×16 (pools, int[], JFR, GC tuning) | 5 | 3 | 5 | 3 |
| BB3 | Profil contention WorkStealingExecutor | 4 | 3 | 5 | 2.4 |
| BB4 | Heuristique "most-constraining variable" | 3 | 2 | 5 | 1.2 |
| BB5 | Mutation testing PITest | 3 | 4 | 4 | 3 |

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

### **Chantier 5 — Scaling algorithme** (3-5 j, R&D)
Tâches : BB1 + BB2.
- JFR sur solve 8×8 → identifier hot alloc.
- Pools si allocation > 20 % CPU.
- DLX POC indépendant dans `solver/experimental/dlx/`.
- Bench comparatif JMH DLX vs AC-3 (4×4 → 8×8).
- **Risque** : élevé, expérimental. Mitigation : flag off par défaut.
- **Métrique** : DLX ≥ 2× plus vite sur 4×4 hard OU décision no-go documentée.

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
| C1 Stabilisation & mesure | **terminé** | `e105353` + split | 1.1→1.5 ✅ — @Disabled 10→4, +3 tests, coverage baseline, JMH baseline à jour, test split 731→175+375 |
| C2 Quick wins perf AC-3 | **à démarrer** | — | |
| C3 Structure & DI | pending | — | |
| C4 Coverage & gates | pending | — | |
| C5 Scaling (R&D) | pending | — | Hors scope session actuelle |

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
(baseline)
3de0d99 chore: remove obsolete archive and root-level MD files
3d0f947 fix: restoreAC3Domains recomputes all cells, not just (r,c) + neighbors
5e5d855 diag: isolate 4x4 hard bug to AC-3 propagation (pre-existing, not sym-breaking)
b8093e3 fix: re-apply TL domain filter after backtrack/recompute
34f693e fix: TL domain pre-filter eliminates AC-3 ↔ sym-breaking conflict

(WIP C1, pas encore committé)
— ConstraintPropagatorTest : 3 @Disabled retirés, realistic FitChecker
```

### Prochaine action
1. Traiter les 7 `@Disabled` restants (`integration/*`).
2. `mvn jacoco:report` + publier chiffres dans ce fichier.
3. Créer les 3 tests manquants (PlacementValidatorTest, DebuggerTest, TimeoutCheckerTest).
4. Baseline JMH dans `.github/perf-baseline.json`.
5. Split `SymmetryBreakingBugTrackingTest` (731 LOC → regression + diagnostic).
6. Commit + push Chantier 1.

### Tracker tâches
- #28 C1 Stabilisation — `in_progress`
- #29 C2 AC-3 perf — `pending`
- #30 C3 Structure & DI — `pending`
- #31 C4 Coverage & gates — `pending`
- #12 C5 DLX POC — `pending` (pre-existing)
