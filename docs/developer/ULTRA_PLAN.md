# Ultra-plan — Competition-grade Eternity II solver

> Conception ground-up, **pas une amélioration incrémentale du solver actuel**.
> Cible : résoudre fiablement Eternity-style 8×8 sous 60 s, avec une porte
> ouverte vers 10×10+.

## 0. Constat de départ

Le solver actuel plateau entre 6×6 (25 s) et 7×7 (timeout). JFR montre :
`HashMap$Node 22 %`, `int[] 20 %`, `Object[] 18 %`, GC sous pression. La
structure OO (Placement, ValidPlacement, Map<Integer, List<…>>) est la
cause racine : **allocation dans la hot loop**, lookups HashMap, dispatch
virtuel, mauvaise localité cache.

Un solver de niveau compétition (Brian Borowski, Eternity II winners,
modernes CP-SAT) est **d'un autre monde** — souvent 100-1000× plus
rapide sur les mêmes problèmes, grâce à des choix fondamentaux que
le solver actuel n'a pas.

---

## 1. Architecture globale

```
┌──────────────────────────────────────────────────────────────────┐
│ PrecomputedTables  (piece×rotation×edge quad index, edge compat) │
└─────────────┬────────────────────────────────────────────────────┘
              │  long[][] compatibilityMask[edgeColour][side]
              ↓
┌──────────────────────────────────────────────────────────────────┐
│ DomainBitmaps  (long[][] = bitmap of candidate placements/cell)  │
└─────────────┬────────────────────────────────────────────────────┘
              ↓
┌──────────────────────────────────────────────────────────────────┐
│ SearchEngine (iterative DFS, tight int loop, no allocation)      │
│   ├─ branching: fail-first = smallest-domain-cell (MRV)          │
│   ├─ value ordering: least-constraining-value (LCV)              │
│   ├─ propagation: forward checking + optional AC-3 as a helper   │
│   ├─ nogood store: LRU cache of dead partial assignments         │
│   └─ restart + luby: escape local traps                          │
└─────────────┬────────────────────────────────────────────────────┘
              ↓
┌──────────────────────────────────────────────────────────────────┐
│ ParallelOrchestrator (portfolio + partition)                     │
│   threads run the SAME search on disjoint sub-trees, communicate │
│   only via solutionFound flag + shared nogood store (optional)   │
└──────────────────────────────────────────────────────────────────┘
```

### Flux de données

1. **PrecomputedTables** : au démarrage, on construit `piece×rotation`
   tables, `edgeCompat[colour][side] = bitmap of (piece×rot) whose edge
   on that side equals colour`. Zéro lookup pendant la recherche.
2. **DomainBitmaps** : chaque cellule a un `long[]` de 256 bits
   (64 pieces × 4 rotations) indiquant les placements encore possibles.
   Un `long[] saved[depth]` sauvegarde pour undo O(1).
3. **SearchEngine** : boucle iterative (pas de récursion Java), tout
   en `int[]` / `long[]`. Zéro allocation.
4. **ParallelOrchestrator** : workers avec boards distincts, seul lien
   = AtomicBoolean + shared nogood store pour éviter duplication.

### Composants clés (détail)

| Composant | Rôle | Structure |
|-----------|------|-----------|
| `PiecesCatalog` | Table immuable piece+rotation → edges | `short[]` (4 shorts / piece / rotation) |
| `EdgeCompatibilityIndex` | Pour chaque (side, colour) → bitmap | `long[side][colour]` avec `long = 64 placements` |
| `DomainStack` | Bitmap de candidates par cellule × profondeur | `long[depth][cell][word]` |
| `PlacementTrail` | Historique des placements pour undo | `int[depth]` (placement ID) |
| `NogoodCache` | Hash → bool died-here | `long[hashTableSize]` cuckoo hash |
| `Heuristics` | Fail-first + LCV | En ligne, lit les bitmaps |

---

## 2. Représentation des données (critique)

### Pas d'objets dans la boucle chaude

Remplacer :
```java
class Placement { int pieceId; int rotation; int[] edges; }
class ValidPlacement { int pieceId; int rotation; int[] edges; }
Map<Integer, List<ValidPlacement>>[][] domains;
```

Par :
```java
// One int encodes placement: bits 0-5 piece id (64 pieces max), bits 6-7 rotation.
// => int pid_rot = (pieceId << 2) | rotation
// Max placements for 8×8 = 64×4 = 256 → fits in long[4] (256 bits).

short[][] piecesEdges;       // [pid_rot][side] = edge colour. 4×256×2 = 2048 bytes, fits L1.
long[][] domainByCell;       // [cell][word] — 4 longs = 32 bytes per cell × 64 cells = 2 KB.
long[][][] domainStack;      // [depth][cell][word] — 32 bytes × 64 × 64 = 128 KB.
int[] placementTrail;        // [depth] = pid_rot chosen at each depth.
int[] cellAtDepth;           // [depth] = cell index chosen at each depth.
long[][] edgeCompatMask;     // [side][colour] = placements whose edge on that side == colour.
```

### Pourquoi c'est x10-x100 plus rapide

1. **Zero GC pressure** : aucune alloc pendant la recherche. Le GC
   actuellement prend > 10 % CPU (JFR `19.5k GCPhaseParallel events / 30 s`).
2. **Cache-friendly** : toutes les tables `long[]` tiennent en L1/L2.
   Le layout Java HashMap éclate les Node[] à travers le tas.
3. **Bitmap filtering = SIMD-friendly** : JIT peut vectoriser
   `newDomain[w] = oldDomain[w] & compatMask[w]` sur AVX.
4. **Plus de boxing Integer** : les pid_rot sont des `int` tout court.
5. **Branch prediction** : la boucle itérative a moins de appels virtuels
   que le backtracking récursif via interfaces.

### Exemple de filtering (propagation AC-3 style)

```java
// Neighbour (r,c) a placé placement pr. Je veux restreindre (r+1,c)
// à ce qui match sur l'edge S/N.
short requiredEdge = piecesEdges[pr][2]; // south edge of placed
long[] mask = edgeCompatMask[/*north side*/ 0][requiredEdge];
long[] target = domainByCell[cellBelow];
// 4 ops SIMD-vectorisables:
target[0] &= mask[0];
target[1] &= mask[1];
target[2] &= mask[2];
target[3] &= mask[3];
// Dead-end si cardinality zero:
if ((target[0] | target[1] | target[2] | target[3]) == 0) return FAIL;
```

---

## 3. Moteur de recherche

### DFS itératif, pas récursif

Le récursif Java a deux coûts : la stack frame + le dispatch virtuel des
Strategy / Selector. Un itératif en `int[]` / `long[]` avec un `for` +
continuation élimine les deux.

```java
int depth = 0;
int[] trail = new int[numCells];
int[] currentChoice = new int[numCells]; // bit position reached per depth
while (true) {
    if (depth == numCells) return SOLVED;
    int cell = pickCell();             // smallest-domain cell
    long[] dom = domainByCell[cell];
    int bit = nextSetBit(dom, currentChoice[depth]);
    if (bit < 0) {                     // cell exhausted → backtrack
        undo(depth);
        if (--depth < 0) return NO_SOLUTION;
        continue;
    }
    trail[depth] = bit;
    currentChoice[depth] = bit + 1;
    saveState(depth);                  // copy long[] — cheap
    apply(cell, bit);                  // update bitmaps via AND-masks
    propagate();                       // forward checking
    if (deadEnd()) { undo(depth); continue; }
    depth++;
}
```

### Pourquoi supérieur au backtracking classique

- **Plus de récursion** : inline-able par JIT C2.
- **État mutable** : pas de copie de domain (juste pointer vers le
  `depth-th` niveau du stack).
- **Branch prediction** : même forme à chaque itération → CPU pre-fetch efficace.

### Alternatives hybrides

**DFS + random restart (Luby sequence)** : redémarre avec un random
TL piece toutes les N nogoods trouvés. Empiriquement donne ×2-5 sur
les puzzles coincés dans un bassin.

**DFS + tabu on recent cells** : petite heuristique stochastique pour
éviter les mêmes chemins lors d'un restart.

---

## 4. Réduction du search space

### Le plus gros levier : pré-assigner TL + coins

- 4 coins pincés à l'avance (par symétrie rotative) → 1/4 du search space.
- Réduction à : puzzle canonical où TL = piece-min-id-TL-fittable rot 0.
- Déjà partiellement fait, mais pas systématique sur corners TR/BL/BR.

### Heuristiques globales

1. **Rarest piece goes first** : compter pour chaque (pieceId, side,
   colour) combien d'autres pieces ont la même couleur opposée. Places
   les plus rares en priorité. Ça brise les tree-width.
2. **Bottleneck cells first** : cellule à l'intersection de deux
   régions à faible domaine (bottleneck-first, pas pure MRV).
3. **Colour-rarity weighted MRV** : pondère le domain size par la
   rareté des couleurs requises. Une cellule à 10 options mais
   toutes avec couleur rare = plus contraignante qu'une à 10 options
   avec couleurs fréquentes.

### Pattern pruning

- Pre-compute les "impossible patterns" 2×2 dans un `HashSet<long>`
  (chaque patch 2×2 = 4 pid_rot = 32 bits). Lors d'un placement, check
  si l'état actuel forme un patch impossible avec ses voisins.
- Sur les puzzles standards, ~5 % des patches sont impossibles en
  théorie. Catching them précocement shave ~20-30 % du search.

---

## 5. Heuristiques avancées — triées par ROI

| # | Heuristique | Impact | Coût d'implémentation |
|---|-------------|--------|------------------------|
| 1 | **Fail-first dynamique** (MRV avec tiebreaker degree) | ×2-3 | Faible |
| 2 | **LCV value ordering** (least-constraining rotation) | ×1.5 | Moyen (table pré-calc) |
| 3 | **Nogood learning + caching** | ×3-10 | Moyen (hash + trail) |
| 4 | **Restart Luby avec random kick** | ×2-5 | Faible (timer + re-init) |
| 5 | **Constraint weighting** (alourdit poids des contraintes souvent violées) | ×1.5 | Moyen |
| 6 | **Backjumping** (conflict-directed, pas chronological) | ×2-4 | Élevé (trace conflicts) |
| 7 | **Symmetry breaking progressif** (pas juste TL) | ×1.5 | Moyen |

**ROI total combiné** : potentiel ×20-×100 vs backtracking chronologique
naïf, sous réserve d'une structure mémoire propre (prérequis section 2).

---

## 6. Pré-calcul massif

### Tables obligatoires

1. `short[256][4] piecesEdges` — edges par (pid, rot, side). 2 KB, L1.
2. `long[4][maxColour] edgeCompatMask` — par side×colour → bitmap
   placements qui matchent. 4 × ~20 colours × 4 longs = 2.5 KB.
3. `long[256][4] cornerMask` — bitmap des placements valides pour
   chaque corner. 8 KB.
4. `int[256] uniqueRotCount` — cached from `Piece.getUniqueRotationCount`.

### Pré-calcul optionnel

5. **2-piece join table** : pour chaque paire (pid_rot_a, side), liste
   les pid_rot_b compatibles. Taille = 256 × 4 × 256 / 8 = 32 KB. Permet
   un forward-checking en 1 lookup.
6. **3-cell pattern frequency** : compter tous les triples (A-B-C)
   possibles. Rarissimes patterns suggèrent un ordering de propagation.

Aller plus loin (4+ pieces) explose en mémoire sans gain marginal —
le sweet-spot est 2-3 pieces.

---

## 7. Parallélisation avancée

### Partition du search space

- Thread 0 : TL = piece-option-0 (canonical)
- Thread 1 : TL = piece-option-1
- Thread 2 : TL = piece-option-2
- Thread 3 : TL = piece-option-3

Pour 8×8 avec 4 TL-fittable pieces, c'est une partition exacte (pas
de travail dupliqué). Speedup théorique ×4.

Si moins de TL options que de threads, partitionner sur la 2ème
placement (TL × TR combos).

### Shared nogood store

Un seul `long[] sharedNogoods` (AtomicLong array) partagé entre
threads. Chaque thread ajoute des nogoods (hash de l'état dead),
tous consultent. **Speedup super-linéaire** possible : un thread
apprend du dead-end d'un autre.

Trade-off : contention sur le cuckoo hash. Mitigation : hash sharding
(N locks, pas un seul).

### Work stealing — overkill pour Eternity

Work-stealing est utile quand la charge est déséquilibrée et que
les sous-tâches sont créées dynamiquement. Ici, les sous-arbres sont
définis à l'avance par partition. **Préférer partition statique + shared nogoods**.

---

## 8. Mémoisation & pruning

### Nogood cache

Un état partiel qui a mené à un dead-end peut être ré-exploré via un
chemin différent. Cacher les hash de ces états évite la re-exploration.

```java
// Hash: XOR des pid_rot placés, rotation-invariant.
long stateHash = 0;
for (int d = 0; d < depth; d++) {
    stateHash ^= CELL_HASH[cellAtDepth[d]] * trail[d];
}
if (nogoodCache.contains(stateHash)) return DEAD_END_KNOWN;
```

Cuckoo hash avec 2-3 hash functions, taille 2^22 entries = 32 MB —
tient en cache L3. Eviction FIFO pour éviter la saturation.

### Subsumption

Un nogood A = {cell=X→piece=Y} est subsumé par B = {cell=X→piece=Y, cell=Z→piece=W}.
Détecter et ne garder que les plus spécifiques. Réduit la taille du cache.

### Trade-off

- Memory footprint : 32-128 MB pour le nogood store.
- Gain : ×3-10 typique en CSP/SAT contest benchmarks.
- Pas adapté si < 4×4 (search trop petit pour amortir la mise en cache).

---

## 9. Alternatives radicales

| Approche | Quand supérieur | Pourquoi |
|----------|-----------------|----------|
| **SAT solver** (MiniSat, Glucose) | Jamais seul | Eternity a trop de contraintes globales (edges) ; encoding SAT explose en clauses. |
| **CP-SAT (OR-Tools)** | 7×7+ | Lazy clause generation, restart, parallel portfolio ; solver de production mature. |
| **Exact cover + DLX** | Jamais pour Eternity | Déjà prouvé no-go : edge-matching est une contrainte pairwise, pas cover. |
| **Custom C++ avec bitboards** | 10×10+ | Gain constant ×3-5 vs Java ; mais seulement après avoir optimisé l'algorithme. Ordre : algo d'abord, langage après. |
| **GPU massivement parallèle (CUDA)** | 14×14+ (Eternity II réel) | Exploration de millions de sub-trees en parallèle. Nécessite re-design complet. Ce que les winners ont fait pour le vrai Eternity II. |

**Recommandation** : **CP-SAT en branche séparée** comme filet de sécurité
pour 7×7+, pas en remplacement. Le solver custom ci-dessus doit battre
CP-SAT sur les tailles ≤ 6×6 parce qu'on a plus de contexte domaine.

---

## 10. Plan d'exécution

### Ordre chronologique

**Phase 1 — Squelette bitmap (1 semaine)**
- `PiecesCatalog` (tables pré-calc).
- `EdgeCompatibilityIndex`.
- `DomainBitmaps` + `DomainStack` avec save/restore bitmap.
- `SearchEngine` itératif + MRV + forward-checking.
- **Gate** : résout 6×6 en < 5 s (×5 vs actuel).

**Phase 2 — Nogood + restart (3 j)**
- Cuckoo hash nogoods.
- Luby restart avec random TL kick.
- **Gate** : résout 7×7 en < 60 s.

**Phase 3 — Parallélisation (2 j)**
- Partition par TL piece.
- Shared nogood store (sharded).
- **Gate** : ×3 speedup sur 4 cores, 8×8 sous 60 s.

**Phase 4 — CP-SAT fallback (3-5 j)**
- Encoder Eternity en CP-SAT.
- Expose comme `Solver` alternatif.
- **Gate** : CP-SAT résout 8×8 sous 60 s comme filet.

**Phase 5 — Heuristiques avancées (1-2 sem)**
- Rarest-piece-first.
- LCV + constraint weighting.
- Optionnel si Phase 1-4 ont déjà atteint l'objectif.

### Quick wins vs gros chantiers

| Effort | Gain attendu | Quoi |
|--------|--------------|------|
| **Quick wins** (< 1 sem) | ×3-5 | Phase 1 squelette bitmap |
| | ×2 | Restart Luby (Phase 2 partiel) |
| **Gros chantiers** (1-2 sem) | ×10-20 | Phase 1 + 2 + 3 |
| | ×20-50 | Nogood learning + constraint weighting |
| **R&D long** | ×50-100 | GPU / RL-based branching (hors scope) |

---

## 11. Estimation réaliste

### Jusqu'où ça peut aller ?

| Taille | Search space | Solver niveau compétition | Solver actuel |
|--------|--------------|---------------------------|---------------|
| 4×4 | ~10^6 | <1 ms | 35 ms ✅ |
| 6×6 | ~10^15 | <1 s | 25 s 🔶 |
| 8×8 | ~10^30 | <1 min | timeout 🔴 |
| 10×10 | ~10^50 | <1 h, depends | N/A 🔴 |
| 12×12 | ~10^75 | days | N/A 🔴 |
| **16×16 (Eternity II réel)** | ~10^145 | **only ever solved once** (Brian Borowski 2010, 3 ans CPU) | impossible |

### Limites fondamentales

1. **Exponentielle intrinsèque** : chaque pas +1 en taille multiplie le
   search space par ~50-100. Aucune constante-factor magique ne compense.
2. **Edge-matching = NP-hard** : démontré dans la littérature.
   Pas d'algorithme polynomial.
3. **Structure locale** : pas de "décomposition" propre comme
   dans TSP ou SAT — chaque cellule dépend de ses 4 voisins.
4. **Mémoire** : nogood cache > 10^9 entries nécessaire sur 10×10+, pas tenable en RAM.

### Réaliste vs ambitieux

- **Réaliste 2-4 semaines** : solver Java compétitif jusqu'à 8×8
  (sub-minute), 10×10 accessible 1-10 min avec CP-SAT.
- **Ambitieux 2-3 mois** : jusqu'à 12×12 avec techniques spécialisées
  (GPU, apprentissage), mais zone R&D publishable.
- **Hors scope** : Eternity II 16×16 vrai. Pas pour un solo dev.

---

## Résumé exécutif — ce qu'on change

**Ce qu'on jette** :
- OO Placement / ValidPlacement.
- Map<Integer, List<…>> dans hot paths.
- Récursion Strategy/Selector avec dispatch virtuel.
- HashMap pour tracker les rotations valides.

**Ce qu'on garde** :
- Le concept AC-3 (utile comme propagateur secondaire).
- Les PuzzleGenerator / PuzzleFactory (fixtures).
- L'infra bench (JMH, JFR tooling).
- Les tests de correctness (`AC3CorrectnessTest`).

**Ce qu'on ajoute** :
- `PiecesCatalog` + `EdgeCompatibilityIndex` (tables pré-calc).
- `DomainBitmaps` (`long[][]` par cellule × profondeur).
- `SearchEngine` itératif sans allocation.
- `NogoodCache` avec cuckoo hash.
- `LubyRestart` pour échapper aux bassins locaux.

**Cible d'ordre de grandeur** : **×10-×50 sur 6×6**, ouvre 7×7 et 8×8.
Au-delà, CP-SAT en backup.
