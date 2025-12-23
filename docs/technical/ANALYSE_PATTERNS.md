# 🔍 ANALYSE DES PATTERNS - Sauvegardes Eternity II

Date: 2025-11-17
Puzzle: 16×16 (256 pièces, dont 9 fixes)

## 📊 DÉCOUVERTES MAJEURES

### 1. 🏆 MEILLEURES CONFIGURATIONS

Les configurations **p02** et **p09** sont **significativement meilleures** :

```
p02_desc : 195 pièces ⭐⭐⭐ (MEILLEURE)
p02_asc  : 195 pièces ⭐⭐⭐ (MEILLEURE)
p09_desc : 195 pièces ⭐⭐⭐ (MEILLEURE)
p09_asc  : 194 pièces ⭐⭐⭐

p06_asc  : 182 pièces ⭐⭐
p08_x    : 178 pièces ⭐⭐
p07_asc  : 177 pièces ⭐⭐
p03_asc  : 172 pièces ⭐

p01_desc : 139 pièces ⚠️ (MOINS BONNE)
p01_asc  : 136 pièces ⚠️ (MOINS BONNE)
p10_asc  : 116 pièces ⚠️ (MOINS BONNE)
```

**Écart**: 195 vs 136 = **59 pièces de différence** (43% de mieux!)

### 2. 🔄 ORDRE ASC vs DESC

**Pas de pattern clair** : aucun ordre n'est systématiquement meilleur.

```
DESC meilleur : p01 (+3), p04 (+3), p09 (+1)
ASC meilleur  : p03 (-1), p05 (-2), p06 (-3), p07 (-3)
Égalité       : p02 (0), p08 (0)
```

**Conclusion** : L'ordre de tri (ascending/descending) a un impact **mineur** (±3 pièces max).
La **permutation des coins** a un impact **MAJEUR** (±59 pièces).

### 3. 📍 PERMUTATIONS DES COINS

**p01** (moins bonne : 136-139) :
- TL=1, TR=2, BL=3, BR=4
- Config: 1-2-3-4 (ordre naturel)

**p02** (MEILLEURE : 195) :
- TL=1, TR=2, BL=**4**, BR=**3**
- Config: 1-2-**4-3** (swap des coins du bas)

**p09** (MEILLEURE : 194-195) :
- TL=**2**, TR=**3**, BL=**1**, BR=4
- Config: **2-3-1**-4 (rotation des 3 premiers)

**Pièces corner** (format: N E S W) :
```
Pièce 1 : [0, 0, 13, 15]
Pièce 2 : [0, 0, 13, 19]
Pièce 3 : [0, 0,  2, 15]
Pièce 4 : [0, 0, 15,  2]
```

### 4. 🎯 PATTERN VISUEL (p02_desc à 195 pièces)

**Zone complète** :
- Toute la ligne du bas (row 15) : **COMPLÈTE** ✓
- Toute la colonne de droite (col 15) : **COMPLÈTE** ✓
- Moitié droite du puzzle : **TRÈS REMPLIE** (cols 12-15)
- Moitié gauche : **VIDE** aux rows 0-4

**Stratégie observée** :
Le solver remplit de **droite à gauche** et de **bas en haut**.
Les lignes 13-15 sont presque complètes, mais rows 0-4 cols 3-11 sont vides.

**Problème potentiel** :
Une **zone vide au milieu-haut** (rows 0-4, cols 3-12) suggère que le solver s'est retrouvé bloqué après avoir rempli le bas/droite.

## 🔬 HYPOTHÈSES À TESTER

### Hypothèse 1 : Coins compatibles
Les permutations **p02** et **p09** créent des **bords plus compatibles** avec le reste des pièces.

**À vérifier** :
- Compter combien de pièces edge peuvent se connecter aux bords south de coins 3 vs 4
- Compter combien de pièces edge peuvent se connecter aux bords south de coins 1 vs 2

### Hypothèse 2 : MRV order
Le MRV (Minimum Remaining Values) pourrait être **affecté** par les coins.
Si un coin crée moins de contraintes, MRV choisira des cases différentes.

**À vérifier** :
- Logger l'ordre MRV pour p01 vs p02 dans les premières profondeurs
- Voir si p02 explore des cases différentes au début

### Hypothèse 3 : Stratégie de remplissage
Le remplissage **droite-bas-gauche-haut** pourrait ne pas être optimal.

**À tester** :
- Essayer une stratégie **spirale** (bord → centre)
- Essayer une stratégie **par quadrants**

## 🎲 RECOMMANDATIONS

### 1. FOCUS sur p02 et p09
**URGENT** : Concentrer tous les threads sur p02 et p09 uniquement :
- `p02_asc`, `p02_desc`, `p09_asc`, `p09_desc`
- Ignorer p01, p04, p05, p10 pour l'instant

### 2. AUGMENTER le temps sur les bonnes configs
Au lieu de 48 configs en parallèle, lancer :
- **8 threads** sur `p02_desc` (meilleure config)
- **8 threads** sur `p09_asc`

### 3. ANALYSER les bloquages
À 195 pièces, p02 est bloqué à la zone rows 0-4, cols 3-12.
**À investiguer** :
- Pourquoi cette zone précise est-elle problématique ?
- Y a-t-il des contraintes impossibles créées par les coins 1,2,4,3 ?

## 📈 PROGRESSION ESTIMÉE

**Avancement actuel** :
- p02/p09 : **~1.65%** des 5 premières profondeurs explorées
- Temps écoulé : plusieurs heures

**Projection** :
- À 1.65%, il reste **~98.35%** à explorer
- Si 195 pièces = 1.65%, alors 100% ≈ impossible à atteindre en temps raisonnable
- **L'espace de recherche est GIGANTESQUE**

## ⚠️ ANOMALIES DÉTECTÉES

### Anomalie 1 : Stagnation à 195
Les configs p02 et p09 sont **toutes deux bloquées à 194-195**.
C'est peut-être une **barrière naturelle** pour cette approche.

### Anomalie 2 : Ordre de remplissage
Le plateau montre un remplissage **très déséquilibré** :
- Bas-droite : dense
- Haut-gauche : vide

Cela suggère que MRV **ne distribue pas uniformément** les placements.

### Anomalie 3 : p10 très mauvaise
p10_asc a seulement **116 pièces** et **2 sauvegardes** seulement.
Elle a peut-être **très vite** trouvé un dead-end.

**À vérifier** : Quelle est la permutation de p10 ?

## 📝 ACTIONS CONCRÈTES

### Immédiat (aujourd'hui)
1. ✅ Confirmer que p02 et p09 sont vraiment les meilleures
2. ⏳ Recentrer tous les threads sur p02_desc uniquement
3. ⏳ Laisser tourner 24h pour voir si on dépasse 195

### Court terme (cette semaine)
1. ⏳ Analyser le placement order dans les saves pour comprendre le blocage à 195
2. ⏳ Implémenter des métriques sur les "zones mortes" du plateau
3. ⏳ Tester une heuristique alternative au MRV

### Long terme
1. ⏳ Implémenter un algorithme de **branch-and-bound** avec pruning agressif
2. ⏳ Essayer des **méta-heuristiques** (simulated annealing, genetic algorithms)
3. ⏳ Paralléliser au niveau des **premières profondeurs** pour explorer plus de branches

## 🎯 CONCLUSION

**Pattern principal identifié** :
La **permutation des coins** est **LE facteur déterminant** pour la profondeur atteignable.

**p02 (1-2-4-3) et p09 (2-3-1-4)** permettent d'atteindre **195 pièces**.
**p01 (1-2-3-4)** plafonne à **136 pièces**.

**Prochaine étape** : Analyser **pourquoi** ces permutations sont meilleures en examinant les connexions possibles entre les bords des coins et les pièces edge disponibles.
