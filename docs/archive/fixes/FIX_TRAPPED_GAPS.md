# Correction finale : Empêcher les gaps piégés sur les bords

## Problème des gaps piégés

### Situation observée dans eternity2_p06_descending_border

Même après la première correction, des gaps apparaissaient toujours sur le bord gauche :

```
Bord gauche (colonne 0):
Ligne 0:  1   ✓
Ligne 1: 35   ✓
Ligne 2:  .   ← GAP PIÉGÉ !
Ligne 3: 46   ✓
Ligne 4:  .   ← GAP PIÉGÉ !
Ligne 5: 59   ✓
```

### Scénario problématique

**Ordre de placement qui crée le gap piégé** :
1. Étape 1 : Pièce 1 placée en (0,0) - coin
2. Étape 35 : Pièce 35 placée en (1,0) - bord gauche
3. Étapes 36-62 : Remplissage d'autres zones (intérieur, autres bords)
4. Étape 63 : Pièce 46 placée en (3,0) - **PIÈGE la case (2,0) entre 35 et 46 !**

### Différence avec le problème précédent

| Type de gap | Description | Détection précédente | Nouvelle détection |
|-------------|-------------|---------------------|-------------------|
| **Gap isolé** | Pièce placée sans voisins (`· X ·`) | ✅ Empêché | ✅ Empêché |
| **Gap piégé** | Case vide entre deux pièces (`X · Y`) | ❌ Non détecté | ✅ Détecté |

**Le problème** : Notre première correction empêchait de **créer** un gap isolé, mais ne détectait pas qu'en plaçant une pièce, on **piégerait** une case vide entre deux pièces déjà placées.

## Solution implémentée

### 1. Nouvelle méthode `wouldCreateTrappedGap()`

**Fichier** : `src/solver/EternitySolver.java` (lignes 1172-1239)

Cette méthode détecte si placer une pièce à une position donnée créerait un gap piégé sur le même bord.

#### Logique de détection

**Pour les bords horizontaux (haut/bas)** :
```java
// Vérifier à gauche : y a-t-il une case vide puis une case remplie ?
if (col >= 2) {
    boolean leftNeighborEmpty = board.isEmpty(row, col - 1);
    boolean leftNeighborNeighborFilled = !board.isEmpty(row, col - 2);
    if (leftNeighborEmpty && leftNeighborNeighborFilled) {
        return true; // On piégerait la case (row, col-1)
    }
}
```

**Schéma** :
```
Avant placement en (0,5) :
Position:  0    1    2    3    4    5    6    7
État:     [X]  [Y]  [·]  [Z]  [·]  [ ]  [W]  [Q]
                                    ↑
                              Considère (0,5)

Vérification à gauche de (0,5) :
- col-1 = 4 : VIDE ✓
- col-2 = 3 : REMPLIE (Z) ✓
→ Placer en (0,5) piégerait (0,4) entre Z et la nouvelle pièce !
→ REJETÉ
```

**Pour les bords verticaux (gauche/droite)** :
```java
// Vérifier en haut : y a-t-il une case vide puis une case remplie ?
if (row >= 2) {
    boolean topNeighborEmpty = board.isEmpty(row - 1, col);
    boolean topNeighborNeighborFilled = !board.isEmpty(row - 2, col);
    if (topNeighborEmpty && topNeighborNeighborFilled) {
        return true; // On piégerait la case (row-1, col)
    }
}
```

**Schéma** :
```
Colonne 0 (bord gauche):
Ligne 0:  [1]  ← remplie
Ligne 1: [35]  ← remplie
Ligne 2:  [·]  ← vide
Ligne 3:  [ ]  ← on considère de placer ici
          ↑
Vérification :
- row-1 = 2 : VIDE ✓
- row-2 = 1 : REMPLIE (35) ✓
→ Placer en (3,0) piégerait (2,0) entre 35 et la nouvelle pièce !
→ REJETÉ
```

### 2. Intégration dans `findNextCellMRV()`

**Fichier** : `src/solver/EternitySolver.java` (lignes 830-874)

**RÈGLE 0 (NOUVELLE - PRIORITÉ MAXIMALE)** : Ne JAMAIS choisir une case qui piège un gap

```java
// VÉRIFICATION CRITIQUE : Cette case créerait-elle un gap piégé ?
boolean wouldTrap = wouldCreateTrappedGap(board, r, c);
boolean bestWouldTrap = (bestCell != null) ? wouldCreateTrappedGap(board, bestCell[0], bestCell[1]) : false;

// RÈGLE 0 (PRIORITAIRE) : Ne JAMAIS choisir une case qui piège un gap
if (!wouldTrap && bestWouldTrap) {
    // Case actuelle ne piège pas, mais meilleure piège -> privilégier case actuelle
    shouldUpdate = true;
} else if (wouldTrap && !bestWouldTrap) {
    // Case actuelle piège, mais meilleure ne piège pas -> garder meilleure
    shouldUpdate = false;
} else if (!wouldTrap && !bestWouldTrap) {
    // Aucune des deux ne piège : appliquer les règles normales de continuité
    // [règles 1-3 existantes]
} else {
    // Les DEUX cases piègent un gap : choisir le moindre mal (MRV)
    shouldUpdate = (uniquePiecesCount < minUniquePieces);
}
```

### Hiérarchie complète des règles

Avec cette correction finale, voici la hiérarchie complète des règles de sélection de case (par ordre de priorité décroissante) :

1. **RÈGLE 0** : Ne JAMAIS piéger un gap (critère ABSOLU)
2. **RÈGLE 1** : Privilégier les cases avec voisins de bord (continuité)
3. **RÈGLE 2** : Pénalité pour cases sans voisins (règle des 50%)
4. **RÈGLE 3** : Comparer le nombre de voisins (plus = mieux)
5. **RÈGLE 4** : MRV (Minimum Remaining Values) pour départager

## Comportement attendu

### Avant la correction

```
Étape 35: Place 35 en (1,0)
Étape 63: Place 46 en (3,0) ← ACCEPTÉ malgré le gap créé

Résultat:
Ligne 0:  1
Ligne 1: 35
Ligne 2:  .  ← GAP PIÉGÉ !
Ligne 3: 46
```

### Après la correction

```
Étape 35: Place 35 en (1,0)
Étape 63: Considère (3,0)
          → wouldCreateTrappedGap() détecte que ça piégerait (2,0)
          → REJETÉ
          → Cherche une autre case
          → Trouve et place en (2,0) à la place ← CONTINUITÉ !

Résultat:
Ligne 0:  1
Ligne 1: 35
Ligne 2: XX  ← REMPLI !
Ligne 3:  .  ou autre case choisie
```

## Cas particuliers gérés

### 1. Début du remplissage d'un bord
La première case d'un bord n'a naturellement pas de voisins. Ce n'est pas un gap piégé car il n'y a pas de case remplie de l'autre côté.

### 2. Les deux cases piègent un gap
Si toutes les cases disponibles piègent un gap (situation rare), l'algorithme choisit celle avec le moins de pièces valides (MRV) pour minimiser les dégâts.

### 3. Coins du puzzle
Les coins sont traités normalement. La vérification de gap piégé s'applique uniquement le long du bord (pas en diagonal).

### 4. Ordre descendant vs ascendant
Cette correction fonctionne pour **les deux ordres** :
- **Ascending** : remplit généralement dans un sens unique, moins de risques
- **Descending** : travaille depuis les deux extrémités, plus de risques de gaps piégés

## Tests et validation

### Test de régression

Le test `TestNoGapsOnBorders.java` continue de passer avec la nouvelle logique.

### Test manuel recommandé

Pour vérifier que la correction fonctionne sur `eternity2_p06_descending_border` :

```bash
# Supprimer les anciennes sauvegardes avec gaps
rm -rf saves/eternity2/eternity2_p06_descending_border/*.txt

# Relancer la résolution
java -cp bin MainSequential

# Attendre au moins une sauvegarde (1 minute)
# Vérifier les nouvelles sauvegardes :
cat saves/eternity2/eternity2_p06_descending_border/current*.txt | grep -A 20 "AFFICHAGE VISUEL"
```

**Vérification** : Le bord gauche (colonne 0) ne devrait plus avoir de gaps du type `X · Y`.

## Fichiers modifiés

1. **`src/solver/EternitySolver.java`**
   - Ajout de `wouldCreateTrappedGap()` (lignes 1172-1239)
   - Modification de `findNextCellMRV()` pour intégrer la vérification (lignes 830-874)

2. **Documentation**
   - `FIX_GAPS_BORDERS.md` - Documentation de la première correction
   - `FIX_TRAPPED_GAPS.md` - Ce document (correction finale)

## Compilation

```bash
# Recompiler le solver
javac -d bin -cp bin src/solver/EternitySolver.java

# Compiler le test
javac -d bin -cp bin test/TestNoGapsOnBorders.java

# Exécuter le test
java -cp bin TestNoGapsOnBorders
```

## Impact sur les performances

### Avantages
1. **Moins de backtracking** : Les gaps piégés sont souvent des dead-ends
2. **Meilleure exploration** : Le solver explore des branches plus prometteuses
3. **Solutions plus rapides** : Moins de temps perdu sur des configurations impossibles

### Coût
1. **Vérifications supplémentaires** : Chaque case est vérifiée pour les gaps piégés (~8 comparaisons max)
2. **Négligeable** : La vérification est O(1) et très rapide

## Conclusion

Cette correction finale complète le système de prévention des gaps sur les bords en ajoutant la détection des **gaps piégés**. Combinée avec la correction précédente (gaps isolés), le système devrait maintenant :

✅ Empêcher les placements isolés (`. X .`)
✅ Empêcher les placements qui piègent des cases vides (`X . Y`)
✅ Privilégier les séquences continues sur les bords
✅ Fonctionner avec les ordres ascending ET descending

Les bords devraient maintenant se remplir proprement sans gaps, que ce soit pour `eternity2_p01_ascending_border` ou `eternity2_p06_descending_border`.
