# Eternity II - Fichiers de Ressources

Ce dossier contient les définitions des pièces pour différents puzzles Eternity II.

## Format des Fichiers

Chaque ligne représente une pièce avec le format:
```
piece_id north east south west
```

Où:
- `piece_id`: Numéro unique de la pièce (1-256)
- `north`, `east`, `south`, `west`: Numéros des arêtes (0-22)
- `0`: Représente une bordure (code "GG" dans les CSV originaux)

## Puzzles Disponibles

### 1. puzzle_256.txt
**Puzzle complet Eternity II (16×16 = 256 pièces)**

- Source: `infos_pieces.frm`
- Dimensions: 16×16
- Composition:
  - 4 coins (2 bordures par pièce)
  - 56 bordures (1 bordure par pièce)
  - 196 centres (0 bordure)
- Total: 256 pièces ✓

**PIÈCE INDICE OBLIGATOIRE:**
- Pièce #139: N=15, E=15, S=2, W=3
- Position fixe: row=8, col=7, rotation=3 (270° clockwise)
- Cette pièce DOIT être pré-placée avant de résoudre le puzzle

### 2. puzzle_indice1_36.txt
**Puzzle avec indice 1 (6×6 = 36 pièces)**

- Source: `EternityII/Documents/Puzzle indice 1 36/*.csv`
- Dimensions: 6×6
- Composition:
  - 4 coins
  - 16 bordures
  - 16 centres
- Total: 36 pièces ✓
- Format: Complet avec 4 arêtes par pièce

### 3. puzzle_indice3_36.txt
**Puzzle avec indice 3 (6×6 = 36 pièces)**

- Source: `EternityII/Documents/Puzzle indice 3 36/*.csv`
- Dimensions: 6×6
- Composition:
  - 4 coins
  - 32 centres (format incomplet)
- Total: 36 pièces
- ⚠️ **ATTENTION**: Format CSV incomplet dans les sources
  - Seules les arêtes N et E sont fournies
  - S et W sont mises à 0 par défaut
  - Il faut compléter manuellement en fonction des rotations possibles

### 4. puzzle_indice4_72.txt
**Puzzle avec indice 4 (8×9 = 72 pièces)**

- Source: `EternityII/Documents/Puzzle indice 4 72/*.csv`
- Dimensions: 8×9 ou 9×8
- Composition:
  - 4 coins
  - 68 centres (format incomplet)
- Total: 72 pièces
- ⚠️ **ATTENTION**: Format CSV incomplet dans les sources
  - Seules les arêtes N et E sont fournies
  - S et W sont mises à 0 par défaut
  - Il faut compléter manuellement en fonction des rotations possibles

## Mapping des Arêtes

Les arêtes sont numérotées de 0 à 22 dans le puzzle complet 256:
- `0`: Bordure (GG)
- `1-22`: Arêtes internes avec différents motifs

Pour les puzzles "indice", le mapping peut être différent car les CSV utilisent des codes différents.

## Notes Importantes

1. **Pièce 139**: C'est la pièce "clue" du puzzle officiel Eternity II. Elle doit être placée en position (8,7) avec rotation 3 avant de commencer à résoudre.

2. **Formats CSV**:
   - Le puzzle 256 et indice1_36 ont des données complètes
   - Les puzzles indice3_36 et indice4_72 ont des CSV incomplets (seules N et E sont fournies)

3. **Rotations**:
   - Rotation 0: Pièce dans sa position originale
   - Rotation 1: 90° dans le sens horaire
   - Rotation 2: 180°
   - Rotation 3: 270° dans le sens horaire

4. **Système de Coordonnées**:
   - Row 0-15 (pour 16×16) = Lignes A-P
   - Col 0-15 = Colonnes 1-16
   - Exemple: position (8,7) = I8 en notation humaine

## Utilisation dans le Code

Pour charger ces puzzles dans votre code Java:

```java
// Charger le puzzle 256 complet
Map<Integer, Piece> pieces = PuzzleFactory.loadFromFile("data/puzzle_256.txt");

// Placer la pièce indice obligatoire
Piece piece139 = pieces.get(139);
board.place(8, 7, piece139, 3);  // Position (8,7), rotation 3
```

## Fichiers Générés

Tous ces fichiers ont été générés à partir des sources suivantes:
- `infos_pieces.frm`: Puzzle 256 complet
- `EternityII/Documents/Puzzle 256/*.csv`: Format 2-lettres (non utilisé car incomplet)
- `EternityII/Documents/Puzzle indice */*.csv`: Puzzles plus petits

Date de génération: 2025-11-16
