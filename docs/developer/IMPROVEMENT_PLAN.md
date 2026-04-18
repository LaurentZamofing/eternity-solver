# Plan d'amélioration Eternity II Solver — Strategic edition

> Réécrit 2026-04-18 après analyse JFR + bilan session de hardening.
> Le plan précédent (détail QW/M/BB) est archivé en bas du document.

> **Mise à jour 2026-04-18 (soir)** : Phase A mesurée (wall 6×6→7×7 ~×5),
> Phase B pivoted (micro-opts non-mesurables), Phase D design pinned.
> **Direction confirmée : rewrite bitmap ground-up.** Voir :
> - `docs/developer/ULTRA_PLAN.md` — blueprint complet (11 sections, data
>   representation, heuristics, estimations).
> - `/Users/laurentzamofing/.claude/plans/bubbly-waddling-wirth.md` — plan
>   d'exécution concret avec P1-P5, phases gates, fichiers critiques, corrections
>   baked in (trail-undo, Zobrist incrémental, WORDS paramétrisé, border
>   segregation, JMH methodology).
>
> **Le plan A→D ci-dessous reste utile comme historique** mais est **superseded
> par les phases P1-P5 du nouveau plan** pour tout nouveau travail.

## 1. État actuel (honnête)

**Ce qui marche** :
- Solver résout 4×4 easy/hard en <1 s.
- AC-3 + MRV + sym-breaking + parallel + save/resume fonctionnels.
- 1553 tests verts, coverage LINE 43 %, mutation 65 %, gates CI stricts.
- Bugs critiques corrigés (AC-3 restore O(Δ), TL domain filter, solveParallel init).

**Où on bute** :
- **8×8 généré ne résout pas en 5 min** (observation JFR 2026-04-18). Le solver plafonne quelque part entre 6×6 et 8×8 sur les puzzles générés.
- Top allocs sous charge : `HashMap$Node` 22 %, `int[]` 20 %, `Object[]` 18 %. GC actif (19.5k `GCPhaseParallel` events en 30 s).
- DLX primary-only : no-go mesuré (>10 min sur 4×4 easy).

**Scope réaliste** :
- ❌ **16×16 Eternity II réel** : hors portée. Puzzle a résisté 14 ans au public, solution trouvée une fois avec techniques spécialisées (backbone propagation, apprentissage, MitM). Un solver Java CSP générique n'y arrivera pas.
- ✅ **Cible actuelle** : **solver de référence pour Eternity-style jusqu'à 8×8**, avec un chemin d'escape optionnel (CP-SAT) vers 10×10+.

## 2. Stratégie

Partir du **goulot mesuré** (allocation HashMap, GC, taille du search space) plutôt que de polir le code existant. Chaque phase a un **gate mesurable**. Si une phase ne tient pas ses promesses, **pivot immédiat** vers la suivante, pas d'acharnement.

### Critères de succès (mesurables)

| Phase | Critère | Si échec → |
|-------|---------|------------|
| A — Baseline | Temps mesurés pour 3×3 → 8×8, mur identifié | — |
| B — Domaines int-keyed | Solve 6×6 généré ≥ 30 % plus rapide | Direct → D |
| C — Parallel diversif. | Solve 6×6 sur 4 threads ≥ 2.5× speedup | Direct → D |
| D — CP-SAT POC | Résout 8×8 généré < 60 s | Clôture honnête |

---

## 3. Phases

### Phase A — Baseline & mur (2 j, commit par étape)

**Objectif** : chiffrer précisément où le solver actuel décroche, avant d'optimiser.

- **A1** Ajouter `BenchmarkGrid` dans `FullSolveBenchmark` : boucle 3×3 → 8×8, 3 seeds chacun, timeout 120 s par run, enregistre temps + placements/s.
- **A2** Run local + publier `.github/perf-baseline-grid.json` avec les chiffres.
- **A3** Identifier précisément : à quelle taille le solver passe de <1 s à >60 s ? Est-ce un cliff (6×6 OK, 7×7 explose) ou linéaire ?
- **A4** Doc `PERF_WALL.md` avec la courbe + observations JFR par taille.

**Livrable** : un fichier avec les chiffres. C'est ça qui guide B/C/D.

### Phase B — AC-3 allocation surgery (3-5 j)

**Hypothèse** : HashMap$Node 22 % + Object[] 18 % = 40 % des allocs = cause majeure du GC pressure. Si on réduit ça de moitié, on gagne ~25 % sur les solves longs.

- **B1** Remplacer `Map<Integer, List<ValidPlacement>>` par `int[][]` layout par cellule :
  - `int[][] domainByCell` où `domainByCell[cellIndex][rotationBits] = 1`.
  - Alternative : `long[] cellBitmaps` (bitmap 64-bit) si ≤ 64 pieces × 4 rotations = 256 placements. Ça ne tient pas en `long` simple, mais `long[4]` oui.
- **B2** Adapter `ConstraintPropagator` : filtrage devient `& bitmap`, compte de domain devient `Long.bitCount()`.
- **B3** Adapter `SingletonDetector` : détection via `bitCount == 1`.
- **B4** Bench avant/après sur le grid de A. **Gate : gain ≥ 30 % sur 6×6**. Si non-atteint, revert + passer à D.

**Risque** : gros refactor touchant AC-3, DomainManager, ConstraintPropagator, PlacementValidator. Tests correctness sont critiques. Mitigation : garder la version HashMap derrière un flag pour A/B.

### Phase C — Thread diversification (1-2 j)

**Hypothèse** : `solveParallel` fait marcher les threads sur le même arbre — gain marginal. Si chaque thread démarre avec un placement initial différent, les arbres sont disjoints → speedup linéaire.

- **C1** Ajouter `DiversifiedWorkerSeed` : thread `i` pré-place un piece différent à TL (ou une cellule non-TL pour éviter conflit avec sym-breaking canonical).
- **C2** Bench 1/2/4/8 threads sur 6×6. **Gate : ≥ 2.5× sur 4 threads**.
- **C3** Si gain réel, retirer `@Disabled` de `testWorkStealingPerformance` et activer par défaut.

**Risque** : déjà un bug parallel fixé (`68c1947`). Re-introduire de la concurrence dans l'état partagé peut recasser. Mitigation : tests existants `ThreadSafetyTest` couvrent pas mal.

### Phase D — CP-SAT exploration (5-10 j, optionnel)

**Déclencheur** : si B et C combinés ne font pas passer 8×8 sous 60 s, ou si le user veut viser 10×10+.

- **D1** Branche `experimental/cpsat`. Ajouter `com.google.ortools:ortools-java` en dépendance.
- **D2** `OrToolsCpSatSolver implements Solver` : encoder Eternity en variables integer (piece par cellule) + contraintes d'edge-matching.
- **D3** Bench 4×4 → 8×8 vs best-of-current.
- **D4** Go/no-go doc : si CP-SAT ≥ 5× plus rapide sur 8×8 → migration recommandée ; sinon → fermer.

**Risque** : OR-Tools binaires natifs (JNI) compliquent la CI. Mitigation : branche isolée, bench only, pas de merge main tant que gate non-validé.

### Phase E — Clôture honnête (1 j)

Peu importe le résultat :
- `PERF_FINAL.md` : courbe de perf finale, jusqu'où ça résout, comment reprendre.
- Drop des chemins morts (ex: si CP-SAT est un go, archiver le DLX plus définitivement).
- Mise à jour `README.md` avec les vraies capacités du solver.

---

## 4. Anti-goals (on NE fait PAS)

1. **Pas de poursuite aveugle du 16×16 Eternity II réel**. Besoin d'algorithmes spécialisés hors scope.
2. **Pas de chasse à la mutation score 80 %+**. 65 % est solide, rendement décroissant.
3. **Pas de MCV full wiring** tant qu'un bench A/B ne prouve pas un gain > 10 %. Le MRV + tiebreakers actuel fait 90 % du job.
4. **Pas de pool `ArrayList<ValidPlacement>`** (M7 ancien) : 5 % des allocs, pas la cible.
5. **Pas de DLX revival** (même avec forward-checking). Le no-go est mesuré ; CP-SAT est l'escape mature.
6. **Pas de réécriture Rust / C++** : ROI insuffisant vs CP-SAT.
7. **Pas de continuer à polir le monitoring Spring Boot** : déjà isolé, stable, pas la cible.

---

## 5. Risk register

| Risque | Impact | Probabilité | Mitigation |
|--------|--------|-------------|------------|
| Refacto B casse AC-3 correctness | Bloquant | Moyen | `AC3CorrectnessTest` + garde le flag HashMap le temps de la migration |
| Phase B donne gain < 30 % | Perte de 3-5 j | Moyen | Cut-off strict, pivot D sans regret |
| CP-SAT JNI casse la CI | Haute friction | Haute | Branche isolée, pas de merge tant que D validée |
| Puzzles générés non représentatifs de Eternity II | Gate bench faux | Faible | Garder aussi 4×4 hard V3 (Eternity-style) |
| User veut absolument 16×16 | Désalignement | Moyen | Plan dit explicitement non ; ouvrir discussion scope |

---

## 6. Ordre d'exécution

```
A (2j)  →  B (3-5j)  →  [bench gate]  →  C (1-2j)  →  [bench gate]  →  D? (5-10j, conditionnel)  →  E (1j)
```

**Prochaine action** : démarrer **Phase A — Baseline**. Créer `BenchmarkGrid` + capture JFR par taille + commit chiffres. C'est la base de décision pour tout le reste.

---

---

# ARCHIVE — session précédente (QW/M/BB mindset)

> Conservé pour traçabilité. Les items ci-dessous sont tous traités (voir statuts).
> La direction a changé 2026-04-18 : arrêter l'approche bottom-up par items, démarrer la phase A ci-dessus.

## Anciennes sections (résumé condensé)

**Quick wins — 7/7 ✅** : @Disabled cleanup, coverage baseline, cache edges ValidPlacement, Piece.isTopLeftFittable, tests + JMH baseline + PMD visibility.

**Medium — 9/10 ✅, 1 🔶 repivoté** :
- M1 AC-3 undo-stack O(Δ) ✅
- M2 Interface Solver ✅
- M3 Bench MRV PQ + PuzzleGenerator ✅
- M4 EternitySolverBuilder ✅
- M5 Dé-staticisation DebugHelper + binary-save removed ✅
- M6 Template SaveStateIO.writeSection ✅
- M7 Pool ArrayList → repivoté vers Phase B (HashMap est la vraie cible)
- M8 JaCoCo gate 20/18 → 35/30 ✅
- M9 Split SymmetryBreakingBugTrackingTest ✅
- M10 Spring Boot solver-only → déjà OK structurellement

**Big bets** :
- BB1 DLX primary-only ✅ **no-go mesuré** (>10 min sur 4×4)
- BB2 Scaling 16×16 → **repivoté vers Phase B** (les allocs HashMap guident)
- BB3 Contention parallel → fix `solveParallel` livré, le reste intégré dans Phase C
- BB4 MCV heuristic → scorer standalone livré ; wiring reporté (anti-goal #3)
- BB5 Mutation testing PITest ✅ (baseline 65 % killed, gate 58 %)

## Métriques baseline → fin session précédente

| Métrique | Baseline | Fin session |
|----------|----------|-------------|
| Tests | 1484 | **1553** |
| Skipped | 12 | **6** |
| LINE coverage | 25 % | **43 %** |
| BRANCH coverage | 24 % | **42 %** |
| Mutation score | n/a | **65 %** |
| JaCoCo gate | 20/18 | **35/30** |
| Mutation gate | n/a | **58** |
| Dead code retiré | — | **~500 LOC** |

## Historique commits de la session précédente

```
(Bug fixes pré-plan)
34f693e · b8093e3 · 5e5d855 · 3d0f947 · 3de0d99

(C1 Stabilisation)
e105353 · 27acd09

(C2 Perf AC-3)
8651d2e · acff823

(C3 Structure + C4 Gates)
f4f8389 · 7d12778 · 4b1415b · e39a615 · b2dfd8e

(Cleanup + bench)
5ec56aa · a385442 · 06b38fb · b2856aa · 68c1947 · fb3ecb6 · 014a929

(DLX POC)
6e63859 · ab56553 · f14d3bb

(ResponseHelper + BB5 + BB4 design)
b484824 · b467130 · 293a300 · 52cd3bf · 1009ff8

(Wave 5 — DebugHelper hardening + JFR + mutation push)
2154f99 · e1aecd5 · 2918fd1
```
