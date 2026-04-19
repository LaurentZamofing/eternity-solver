# Search space reduction plan — main solver (2026-04-19)

Plan validé via /ultraplan pour passer d'un solver qui timeout à un
solver qui prune massivement — uniquement coupes de l'espace de
recherche, pas de micro-optim CPU.

Cible : chemin principal (`EternitySolver`, `MRVPlacementStrategy`,
`MRVCellSelector`, `ConstraintPropagator`), pas le `BitmapSolver`
expérimental.

## Contexte

Le solver principal a déjà : bitset de pièces, AC-3 incrémental
(`ConstraintPropagator`), MRV (`MRVCellSelector`), détection de
singletons, `EdgeCompatibilityIndex`. Les timeouts viennent d'un
branching factor explosif dans la partie intérieure.

Le plan importe les techniques x10+ connues sur Eternity II
(color-count frontier pruning, border-first, nogoods globaux,
two-step lookahead) dans les extension points existants.

## Les 5 leviers (ordre ROI décroissant)

### 1. Color-budget frontier pruning — plus gros ROI

Chaque couleur intérieure a un nombre fini de demi-arêtes sur les
pièces restantes. Si `demand[c] > supply[c]` pour un c, la branche
est morte — détecté avant d'essayer une pièce.

Impl : `ColorBudgetTracker` maintenu incrémentalement par `pieceUsed`.
Check dans `ConstraintPropagator.propagateAC3`.

### 2. Border-first puis interior snake

Le bord est quasi-1D (2 contraintes), résolu en secondes. Une fois
fixé, l'intérieur démarre avec haut facteur de contrainte.

Impl : champ `phase` dans `BacktrackingContext`. En `BORDER`, MRV
ne considère que les cellules-bord. En `INTERIOR`, MRV classique
avec tie-break = distance à la frontière. Les 4 coins pré-placés
via `SolverConfiguration.fixedPositions`.

### 3. Nogoods Zobrist — import depuis BitmapSolver

Déjà implémenté dans `experimental/bitmap/NogoodCache.java`. Non
branché au solver principal.

Impl : `ZobristHasher` dans `BacktrackingContext`, XOR au place/remove,
check dans `MRVPlacementStrategy` avant chaque descente.

### 4. Two-step lookahead sur frontière critique

AC-3 détecte domaines vides à profondeur 1. Beaucoup de dead-ends
apparaissent à profondeur 2. Simuler le placement de la cellule la
plus contrainte (domaine == 1) et vérifier.

Impl : extension de `PlacementValidator.validate`, réutilise
`ConstraintPropagator.wouldCauseDeadEnd` existant.

### 5. LCV réel — remplacer sort par pieceId

`MRVPlacementStrategy` trie actuellement par `pieceId`.
`LeastConstrainingValueOrderer` existe mais n'est pas branché.

Impl : remplacer le sort ligne 119-123 de `MRVPlacementStrategy`
par appel à LCV. Score = somme domaines voisins après placement,
pondéré par rareté globale couleur.

## Ordre d'implémentation

1. #2 Border-first + coins fixes (1-2h, indépendant)
2. #1 Color-budget pruning (2-3h, utilise `pieceUsed` existant)
3. #3 Nogoods Zobrist (2h, plumbing depuis experimental/)
4. #5 LCV réel (30min — quick win)
5. #4 Two-step lookahead (1h, désactivable par flag)

## Vérification

- `run_tests.sh` doit passer (garde-fou `AC3CorrectnessTest`)
- `run_validation_6x6.sh` avant/après chaque changement — attendu
  `stats.backtracks` ÷10 par étape
- `./run.sh` sur 8×8 timeout 60s : profondeur max doit passer de
  ~40 pièces à ~60+
- Sanity : les 4 puzzles fixés dans `puzzles/` gardent leur solution
  connue
