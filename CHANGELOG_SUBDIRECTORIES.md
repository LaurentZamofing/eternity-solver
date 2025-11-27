# Organisation des sauvegardes par sous-répertoires et affichage visuel

## 📅 Date : 2025-11-17

## 🎯 Problème

Avec l'augmentation du nombre de sauvegardes (48 configurations × plusieurs fichiers), le répertoire `saves/` devenait difficile à naviguer. De plus, il était compliqué de visualiser rapidement l'état d'une sauvegarde sans ouvrir un éditeur et analyser les coordonnées.

## ✅ Solution

### 1. Organisation par sous-répertoires

Les sauvegardes sont maintenant organisées par type de puzzle dans des sous-répertoires :

```
saves/
├── eternity2/
│   ├── eternity2_p01_ascending_current_1731868234567.txt
│   ├── eternity2_p01_ascending_best_176.txt
│   ├── eternity2_p01_descending_current_1731868234568.txt
│   └── ...
├── indice1/
│   ├── indice1_current_1731868234567.txt
│   ├── indice1_best_42.txt
│   └── ...
└── indice2/
    └── ...
```

**Avantages** :
- ✅ Répertoire `saves/` plus propre et organisé
- ✅ Facilité de navigation entre différents puzzles
- ✅ Possibilité de sauvegarder/archiver un puzzle spécifique
- ✅ Évite la confusion entre les sauvegardes de différents puzzles

### 2. Affichage visuel ASCII du plateau

Chaque fichier de sauvegarde contient maintenant un **affichage visuel ASCII** du plateau en haut du fichier :

```
# ═══════════════════════════════════════════════════════════
# AFFICHAGE VISUEL DU PLATEAU (176 pièces placées)
# ═══════════════════════════════════════════════════════════
#
#   10   21   32   43  ...
#   54   65   76   87  ...
#   98  109  1110 1221 ...
#   ...
#
# ═══════════════════════════════════════════════════════════
```

**Format** :
- Chaque cellule affiche : `pieceId` + `rotation`
  - Exemple : `176` = pièce 17, rotation 6
  - Exemple : `10` = pièce 1, rotation 0
- Les cellules vides sont représentées par `.`

**Avantages** :
- ✅ **Visualisation rapide** du plateau sans ouvrir un éditeur
- ✅ **Identification immédiate** de la progression
- ✅ **Analyse visuelle** des patterns et zones problématiques
- ✅ **Utile pour le debugging** et la compréhension des sauvegardes

## 🔧 Modifications techniques

### src/util/SaveStateManager.java

#### 1. Nouvelle méthode `getPuzzleSubDir()`

```java
private static String getPuzzleSubDir(String puzzleName) {
    String baseType = puzzleName.split("_p")[0]; // eternity2_p01 -> eternity2
    baseType = baseType.split("_")[0]; // indice1_xxx -> indice1
    return SAVE_DIR + baseType + "/";
}
```

Extrait le type de base du puzzle pour déterminer le sous-répertoire.

#### 2. Nouvelle méthode `generateBoardVisual()`

```java
private static void generateBoardVisual(PrintWriter writer, Board board) {
    int rows = board.getRows();
    int cols = board.getCols();

    for (int r = 0; r < rows; r++) {
        StringBuilder line = new StringBuilder("# ");
        for (int c = 0; c < cols; c++) {
            if (board.isEmpty(r, c)) {
                line.append("  .  ");
            } else {
                Placement p = board.getPlacement(r, c);
                String pieceStr = String.format("%3d", p.getPieceId());
                line.append(pieceStr).append(p.getRotation()).append(" ");
            }
        }
        writer.println(line.toString());
    }
}
```

Génère l'affichage ASCII du plateau.

#### 3. Modification de `saveState()`

```java
// Obtenir le sous-répertoire pour ce puzzle
String puzzleDir = getPuzzleSubDir(puzzleName);

// Créer le sous-répertoire s'il n'existe pas
File dir = new File(puzzleDir);
if (!dir.exists()) {
    dir.mkdirs();
}

// Sauvegarder dans le sous-répertoire
String currentFile = puzzleDir + baseName + "_current_" + timestamp + ".txt";
```

Utilise maintenant le sous-répertoire pour toutes les opérations de sauvegarde.

#### 4. Modification de `saveToFile()`

```java
// AFFICHAGE VISUEL DU PLATEAU
writer.println("# ═══════════════════════════════════════════════════════════");
writer.println("# AFFICHAGE VISUEL DU PLATEAU (" + depth + " pièces placées)");
writer.println("# ═══════════════════════════════════════════════════════════");
writer.println("#");
generateBoardVisual(writer, board);
writer.println("#");
writer.println("# ═══════════════════════════════════════════════════════════");
```

Ajoute l'affichage visuel en tête du fichier.

#### 5. Modifications des méthodes de recherche

Toutes les méthodes suivantes ont été mises à jour pour chercher dans les sous-répertoires :
- `cleanupOldCurrentSaves(puzzleDir, baseName, currentFile)`
- `cleanupOldBestSaves(puzzleDir, baseName, depth)`
- `isNewRecord(puzzleDir, baseName, depth)`
- `findCurrentSave(puzzleName)` - avec fallback vers le répertoire racine pour rétrocompatibilité
- `findAllSaves(puzzleName)`

## 📊 Exemple de fichier de sauvegarde

```
# Sauvegarde Eternity II
# Timestamp: 1763409569170
# Date: 2025-11-17_20-59-29
# Puzzle: eternity2_p01_ascending
# Dimensions: 16x16
# Depth: 176

# ═══════════════════════════════════════════════════════════
# AFFICHAGE VISUEL DU PLATEAU (176 pièces placées)
# ═══════════════════════════════════════════════════════════
#
#   10   21   32   43   54   65   76   87   98  109  1110 1221 1332 1443 1554 1665
#  170  181  192  203  ...
#   .    .    .    .    .    .    .    .    .    .    .    .    .    .    .    .
#   ...
#
# ═══════════════════════════════════════════════════════════

# Placement Order (row,col pieceId rotation) - ordre chronologique
0,0 1 0
0,1 2 1
...

# Placements (row,col pieceId rotation)
0,0 1 0
0,1 2 1
...

# Unused pieces
177 178 179 ... 256
```

## 🔄 Rétrocompatibilité

Les **anciennes sauvegardes** dans le répertoire racine `saves/` continuent de fonctionner :

```java
// Si aucun fichier dans le sous-répertoire, chercher dans le répertoire racine
File legacyFile = new File(SAVE_DIR + baseName + "_current.txt");
if (legacyFile.exists()) {
    return legacyFile;
}
```

Cependant, les **nouvelles sauvegardes** seront créées dans les sous-répertoires.

## ✅ Tests effectués

### Test automatisé : `test/TestSubdirectorySave.java`

```bash
javac -d bin -sourcepath src:test test/TestSubdirectorySave.java
java -cp bin TestSubdirectorySave
```

**Résultat** :
- ✓ Sous-répertoires créés automatiquement
- ✓ Fichiers sauvegardés dans les bons sous-répertoires
- ✓ Affichage visuel présent dans tous les fichiers
- ✓ Format d'affichage correct (pieceId + rotation)

### Test manuel avec MainParallel

```bash
./run_parallel.sh 2
# Attendre quelques minutes pour l'autosave
# Puis arrêter avec Ctrl+C
ls -la saves/eternity2/
```

**Résultat** :
- ✓ Sauvegardes créées dans `saves/eternity2/`
- ✓ Noms de fichiers corrects avec pXX et order
- ✓ Affichage visuel lisible dans les fichiers

## 🎁 Bénéfices

### Pour l'utilisateur
- ✅ **Navigation facilitée** : répertoire `saves/` organisé par puzzle
- ✅ **Visualisation rapide** : voir l'état d'une sauvegarde d'un coup d'œil
- ✅ **Analyse améliorée** : identifier visuellement les patterns et zones
- ✅ **Archivage simplifié** : sauvegarder/déplacer un puzzle complet

### Pour le développeur
- ✅ **Code plus propre** : séparation claire par puzzle
- ✅ **Debugging facilité** : visualisation rapide des états
- ✅ **Maintenance simplifiée** : structure claire et logique

### Pour le monitoring
```bash
# Voir les sauvegardes d'eternity2 uniquement
ls -lh saves/eternity2/

# Voir les meilleurs scores d'eternity2
ls -lh saves/eternity2/*_best_*.txt | sort -t_ -k3 -n

# Voir rapidement un plateau
head -30 saves/eternity2/eternity2_p01_ascending_best_176.txt
```

## 📚 Documentation mise à jour

- ✅ `CHANGELOG_SUBDIRECTORIES.md` : Ce document
- ✅ `README_PARALLEL.md` : Section "Structure des sauvegardes" mise à jour
- ✅ `PARALLELISATION.md` : Section "Système de sauvegarde" mise à jour

## 🚀 Utilisation

Aucun changement dans l'utilisation ! Le système crée automatiquement les sous-répertoires :

```bash
./run_parallel.sh
```

Les sauvegardes apparaîtront dans :
- `saves/eternity2/` pour Eternity II
- `saves/indice1/` pour Indice 1
- `saves/indice2/` pour Indice 2
- etc.

Et chaque fichier contiendra l'affichage visuel du plateau !

---

**Développé le** : 2025-11-17
**Status** : ✅ COMPLET ET TESTÉ
