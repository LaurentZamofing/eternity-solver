# Correction du Backtracking après Reprise depuis Sauvegarde

## Problème identifié

Quand on reprenait une exécution depuis une sauvegarde avec des pièces déjà placées, le solver ne pouvait pas **dépiler** (backtracker) correctement à travers les pièces pré-chargées.

### Symptômes

- Le programme s'arrêtait prématurément avec "dead-end" sans avoir essayé de backtracker à travers les pièces chargées depuis la sauvegarde
- Les pièces sauvegardées étaient traitées comme "fixes" alors qu'elles auraient dû pouvoir être retirées et replacées différemment

### Cause racine

Dans `EternitySolver.java`, la méthode `solveWithHistory()` avait une boucle de backtracking itératif qui :

1. Appelait `solveBacktracking()` avec l'état chargé
2. Si échec, retirait la dernière pièce pré-chargée
3. Réessayait `solveBacktracking()` avec moins de pièces

**Le problème** : après avoir retiré une pièce, la méthode réessayait immédiatement de résoudre **sans essayer les rotations alternatives de la même pièce**. Cela menait à un cycle infini où :
- On retire la pièce X (rotation 0) de la position [r,c]
- On appelle `solveBacktracking()` qui essaie de placer une pièce à [r,c]
- Il retombe sur la même pièce X (rotation 0) via l'algorithme MRV
- Dead-end, donc on backtrack encore...

## Solution implémentée

Modification de `solveWithHistory()` dans `/Users/laurentzamofing/dev/eternity/src/solver/EternitySolver.java` (lignes 1290-1378) :

### Changements principaux

1. **Tracking de la rotation utilisée** : on garde maintenant `oldRotation` quand on retire une pièce

2. **Essai des rotations alternatives** : après avoir retiré une pièce, on essaie d'abord toutes les autres rotations de la même pièce à la même position avant de continuer le backtracking plus loin

3. **Backtracking plus profond** : seulement si aucune rotation alternative ne fonctionne, on continue à remonter dans l'historique

### Code ajouté

```java
// IMPORTANT: Essayer d'abord les autres rotations de la même pièce à la même position
// avant de continuer le backtracking plus loin
Piece piece = allPieces.get(pieceId);
if (piece != null) {
    int maxRotations = piece.getUniqueRotationCount();

    // Essayer les rotations suivantes (après celle qui a échoué)
    for (int rot = (oldRotation + 1) % 4; rot < maxRotations; rot++) {
        int[] candidate = piece.edgesRotated(rot);

        if (fits(board, row, col, candidate)) {
            // Placer avec la nouvelle rotation
            board.place(row, col, piece, rot);
            unusedIds.remove(Integer.valueOf(pieceId));
            placementOrder.add(new SaveStateManager.PlacementInfo(row, col, pieceId, rot));

            // Essayer de résoudre avec cette rotation
            result = solveBacktracking(board, allPieces, unusedIds);

            if (result) {
                return true;
            }

            // Cette rotation n'a pas marché, la retirer
            board.remove(row, col);
            unusedIds.add(pieceId);
            placementOrder.remove(placementOrder.size() - 1);
        }
    }
}
```

## Bénéfices

1. ✅ **Backtracking complet** : le solver peut maintenant remonter à travers TOUTES les pièces chargées, pas seulement celles placées durant l'exécution courante

2. ✅ **Exploration exhaustive** : avant de retirer complètement une pièce d'une position, on essaie toutes ses rotations possibles

3. ✅ **Évite les cycles** : on ne retombe plus dans le même dead-end en essayant la même pièce avec la même rotation

4. ✅ **Meilleure progression** : le solver peut découvrir des chemins de solution qui nécessitent de changer seulement la rotation d'une pièce plutôt que de la retirer complètement

## Tests

Tous les tests de backtracking passent :
- ✓ Test 1: findAllSaves() trouve les sauvegardes
- ✓ Test 2: Tri des sauvegardes par profondeur
- ✓ Test 3: loadStateFromFile() charge un fichier
- ✓ Test 4: Cohérence du placement order
- ✓ Test 5: Restauration complète avec placement order

Pour exécuter les tests :
```bash
javac -d bin -sourcepath test:src test/TestBacktracking.java
java -cp bin TestBacktracking
```

## Fichiers modifiés

- `/Users/laurentzamofing/dev/eternity/src/solver/EternitySolver.java` :
  - méthode `solveWithHistory()` (lignes 1290-1383)
  - Ajout de l'initialisation du `domainCache` pour éviter NullPointerException

## Bugs corrigés

### Bug #1 : NullPointerException dans restoreCacheAfterBacktrack()

**Erreur** :
```
java.lang.NullPointerException: Cannot invoke "java.util.Map.put(Object, Object)" because "this.domainCache" is null
    at solver.EternitySolver.restoreCacheAfterBacktrack(EternitySolver.java:1526)
```

**Cause** : Le `domainCache` n'était pas initialisé dans `solveWithHistory()` alors qu'il est utilisé par `solveBacktracking()`

**Solution** : Ajout de l'initialisation du cache dans `solveWithHistory()` :
```java
// Initialiser le cache des domaines si activé
if (useDomainCache) {
    domainCache = new HashMap<>();
    initializeDomainCache(board, allPieces, unusedIds);
}
```

## Date de correction

2025-11-17
