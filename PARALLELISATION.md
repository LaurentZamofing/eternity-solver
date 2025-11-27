# Stratégie de Parallélisation pour Eternity II

## 📋 Vue d'ensemble

Ce document décrit la stratégie mise en place pour permettre l'exécution parallèle de plusieurs instances du solveur Eternity II sans conflit.

## 🎯 Objectif

Permettre à plusieurs threads/processus de travailler simultanément sur la résolution d'Eternity II en explorant différentes branches de l'arbre de recherche de manière complémentaire.

## 🔑 Principes clés

### 1. Pièces de coin fixes (4!)

Eternity II possède **4 pièces de coin** (pièces 1, 2, 3, 4) qui doivent être placées dans les 4 coins du puzzle.

- Il existe **4! = 24 permutations possibles** pour placer ces pièces de coin
- Chaque permutation définit un point de départ différent pour la recherche
- Ces permutations sont **mutuellement exclusives** : aucun conflit n'est possible

**Note** : En plus des 4 coins, Eternity II possède **5 pièces fixes supplémentaires** qui sont toujours placées aux mêmes positions :
- Pièce 139 à (8, 7) - position centrale
- Pièce 181 à (13, 2)
- Pièce 255 à (2, 13)
- Pièce 249 à (13, 13)
- Pièce 208 à (2, 2)

Au total : **9 pièces fixes** par configuration (4 coins + 5 fixes)

### 2. Ordre de tri des pièces (×2)

Pour chaque permutation de coins, on peut explorer l'arbre de recherche dans deux ordres différents :

- **Ascending** : les pièces sont essayées dans l'ordre croissant (5, 6, 7, ..., 256)
- **Descending** : les pièces sont essayées dans l'ordre décroissant (256, ..., 7, 6, 5)

Cela double le nombre de branches explorables : **24 × 2 = 48 configurations uniques**

### 3. Reprise intelligente des sauvegardes

Chaque instance du solveur :
1. Cherche la sauvegarde `current` **la plus ancienne**
2. Reprend le travail depuis ce point
3. Crée une nouvelle sauvegarde avec **timestamp** lors de la prochaine sauvegarde
4. Nettoie automatiquement les anciennes sauvegardes

**Bénéfice** : Les threads s'auto-répartissent naturellement sur les travaux les plus anciens (donc les moins avancés), sans coordination explicite.

## 📁 Fichiers de configuration

### Structure des noms

```
puzzle_eternity2_pXX_C1_C2_C3_C4_ORDER.txt
```

Où :
- `XX` : numéro de permutation (01-24)
- `C1`, `C2`, `C3`, `C4` : IDs des pièces de coin pour chaque position
- `ORDER` : `ascending` ou `descending`

### Exemples

```
puzzle_eternity2_p01_1_2_3_4_ascending.txt   # Permutation 1, ordre croissant
puzzle_eternity2_p01_1_2_3_4_descending.txt  # Permutation 1, ordre décroissant
puzzle_eternity2_p12_2_4_3_1_ascending.txt   # Permutation 12, ordre croissant
puzzle_eternity2_p24_4_3_2_1_descending.txt  # Permutation 24, ordre décroissant
```

### Positions des coins

- **Position 1** : Coin haut-gauche (0, 0) - rotation 0
- **Position 2** : Coin haut-droit (0, 15) - rotation 1
- **Position 3** : Coin bas-gauche (15, 0) - rotation 3
- **Position 4** : Coin bas-droit (15, 15) - rotation 2

### Métadonnées de configuration

Chaque fichier contient :

```
# SortOrder: ascending (ou descending)
# PieceFixePosition: <pieceId> <row> <col> <rotation>
```

## 💾 Système de sauvegarde

### Organisation par sous-répertoires

Les sauvegardes sont organisées dans des **sous-répertoires** par type de puzzle :

```
saves/
├── eternity2/
│   ├── eternity2_p01_ascending_current_1731868234567.txt
│   ├── eternity2_p01_descending_current_1731868234567.txt
│   ├── eternity2_best_176.txt
│   └── ...
├── indice1/
│   └── ...
└── indice2/
    └── ...
```

### Noms de fichiers

Les sauvegardes `current` incluent le **numéro de permutation, l'ordre de tri et un timestamp** :

**Format** : `eternity2_pXX_ORDER_current_TIMESTAMP.txt`

Le nouveau format permet de :
- **Identifier facilement** quelle config correspond à quelle sauvegarde
- Connaître l'ancienneté du travail (via le timestamp)
- Éviter les conflits entre threads (chaque config a son propre espace)
- Charger la sauvegarde la plus ancienne en priorité

### Affichage visuel

Chaque fichier de sauvegarde contient un **affichage ASCII** du plateau en tête :

```
# ═══════════════════════════════════════════════════════════
# AFFICHAGE VISUEL DU PLATEAU (176 pièces placées)
# ═══════════════════════════════════════════════════════════
#
#   10   21   32   43  ...
#   54   65   76   87  ...
#   .    .    .    .   ...
#
# ═══════════════════════════════════════════════════════════
```

**Format** : `pieceId` + `rotation` (ex: "176" = pièce 17, rotation 6)

Ceci permet de visualiser rapidement l'état d'une sauvegarde sans ouvrir un éditeur.

### Stratégie de chargement

1. **Recherche** : Lister tous les fichiers `eternity2_current_*.txt`
2. **Tri** : Trier par timestamp (ordre croissant)
3. **Sélection** : Charger le fichier avec le plus petit timestamp (= le plus ancien)
4. **Nettoyage** : Supprimer les anciennes sauvegardes après création d'une nouvelle

### Auto-sauvegarde

- **Sauvegarde principale** : Toutes les 10 minutes
- **Sauvegarde thread** : Toutes les 5 minutes (pour les threads parallèles)
- **Sauvegarde best** : Lors de chaque nouveau record (tous les 5 niveaux)

## 🚀 Utilisation pratique

### ⭐ Méthode recommandée : MainParallel (multi-thread intelligent)

**Utilisation automatique** (détecte le nombre de CPUs) :
```bash
./run_parallel.sh
```

**Utilisation avec un nombre spécifique de threads** :
```bash
./run_parallel.sh 8
```

Ou directement en Java :
```bash
java -cp bin MainParallel 8
```

**Avantages de MainParallel :**
- ✅ Gestion automatique de la priorité des configurations
- ✅ Lance d'abord les configs jamais commencées
- ✅ Reprend ensuite les sauvegardes les plus anciennes
- ✅ Pool de threads optimisé
- ✅ Pas besoin de gérer manuellement les configs

### Méthode manuelle : Lancement de plusieurs instances séparées

Si vous préférez contrôler manuellement chaque instance dans des terminaux séparés :

**Terminal 1:**
```bash
java -cp bin MainSequential data/puzzle_eternity2_p01_1_2_3_4_ascending.txt
```

**Terminal 2:**
```bash
java -cp bin MainSequential data/puzzle_eternity2_p01_1_2_3_4_descending.txt
```

**Terminal 3:**
```bash
java -cp bin MainSequential data/puzzle_eternity2_p02_1_2_4_3_ascending.txt
```

... et ainsi de suite pour les 48 configurations.

### Lancement d'une seule configuration

Pour tester ou déboguer une configuration spécifique :
```bash
java -cp bin MainSequential data/puzzle_eternity2_p01_1_2_3_4_ascending.txt
```

## 🎯 Système de priorisation intelligent (MainParallel)

MainParallel implémente un système de priorisation automatique pour optimiser l'exploration :

### Algorithme de sélection

1. **Priorité 1 : Configurations jamais commencées**
   - Les configs sans sauvegarde `current` sont lancées en premier
   - Garantit qu'aucune branche n'est laissée de côté

2. **Priorité 2 : Sauvegardes les plus anciennes**
   - Parmi les configs en cours, les plus anciennes sont reprises
   - Utilise le timestamp dans le nom du fichier `current_TIMESTAMP.txt`
   - Évite qu'une config ne soit "abandonnée" trop longtemps

3. **Tri alphabétique pour les non commencées**
   - Ordre déterministe entre configs jamais lancées

### Exemple de sortie

```
╔═══════════════════════════════════════════════════════════════════╗
║              STATISTIQUES DES CONFIGURATIONS                     ║
╚═══════════════════════════════════════════════════════════════════╝

  📊 Total configurations : 48
  🆕 Jamais commencées    : 12
  🔄 En cours             : 36

📋 Ordre de priorité:
   1. Configurations jamais commencées
   2. Sauvegardes les plus anciennes

🚀 [Thread 1] Démarrage: Permutation 1 - ASCENDING
   Statut: NOUVEAU

🚀 [Thread 2] Démarrage: Permutation 12 - DESCENDING
   Statut: REPRISE (sauvegarde du Mon Nov 17 19:45:37 CET 2025)
```

### Comportement

- **Au démarrage** : Analyse toutes les 48 configurations
- **Affiche les stats** : Nombre de configs neuves vs en cours
- **Lance N threads** : N = nombre spécifié (ou nb de CPUs)
- **Chaque thread** : Travaille indéfiniment sur sa config assignée
- **Sauvegardes auto** : Chaque 10 minutes, avec nouveau timestamp

## 📊 Monitoring

Chaque instance affiche :

```
→ Ordre de tri: ascending
→ Reprise depuis: 176 pièces (TOUTES les pièces peuvent être backtractées)
→ Le backtracking pourra remonter à travers TOUTES les 176 pièces pré-chargées
```

Les meilleurs scores sont partagés via les sauvegardes `best_XXX.txt` qui sont consultables par toutes les instances.

## 🔧 Modifications techniques

### Fichiers modifiés

1. **PuzzleConfig.java**
   - Ajout du champ `sortOrder`
   - Parsing de `SortOrder:` dans les fichiers de configuration

2. **SaveStateManager.java**
   - Ajout de timestamp dans les noms de fichiers `current`
   - Méthode `findCurrentSave()` retourne la sauvegarde la plus ancienne
   - Nettoyage automatique des anciennes sauvegardes `current`

3. **EternitySolver.java**
   - Ajout du champ `sortOrder`
   - Méthode `setSortOrder()`
   - Tri de la liste `unused` selon l'ordre configuré dans `solve()`

4. **MainSequential.java**
   - Méthode `sortPiecesByOrder()` pour trier les pièces
   - Appel à `setSortOrder()` sur le solver
   - Affichage de l'ordre de tri dans les logs

### Script de génération

**scripts/generate_eternity2_configs.py**
- Génère les 48 fichiers de configuration automatiquement
- Calcule les permutations des coins
- Ajoute les bonnes rotations pour chaque position
- Inclut la pièce centrale fixe (139)

## ✅ Validation

Le test `TestNewConfigFormat.java` vérifie :
- Chargement correct des 48 configurations
- Parsing du champ `sortOrder`
- Pièces fixes aux bonnes positions
- Rotations correctes pour chaque coin

```bash
javac -d bin -sourcepath src:test test/TestNewConfigFormat.java
java -cp bin TestNewConfigFormat
```

## 🎯 Résultats attendus

Avec cette stratégie :
- ✅ Aucun conflit entre threads
- ✅ Exploration complémentaire de l'espace de recherche
- ✅ Auto-répartition du travail sur les branches les plus anciennes
- ✅ Partage des meilleurs scores via les sauvegardes `best`
- ✅ Utilisation optimale des CPUs multi-core

## 📅 Date de mise en œuvre

2025-11-17
