# Puzzles Eternity II - Indices et Extractions

## Fichiers Générés

### Indice 1 (6×6 = 36 pièces)
- **`puzzle_indice1_36.txt`** - Données officielles depuis CSV (76 valeurs d'arêtes)
- **`puzzle_indice1_36_extracted.txt`** - Extraction depuis images (31 motifs identifiés)

### Indice 2 (72 pièces)
- **`puzzle_indice2_72_from_xls.txt`** - ✅ Converti depuis fichiers XLS
  - **72 pièces** avec mapping lettre→chiffre
  - **9 lettres uniques**: G=0, B, C, F, I, O, R, U, V
  - Format: `piece_id N E S W`

### Indice 3 (6×6 = 36 pièces)
- **`puzzle_indice3_36_from_csv.txt`** - ✅ Converti depuis CSV
  - **36 pièces** avec mapping lettre→chiffre
  - **10 lettres uniques**: G=0, B, C, F, J, L, O, R, V, Y
  - Format: `piece_id N E S W`

### Indice 4 (72 pièces)
- **`puzzle_indice4_72_from_csv.txt`** - ✅ Converti depuis CSV
  - **72 pièces** avec mapping lettre→chiffre
  - **10 lettres uniques**: G=0, A, B, L, O, R, S, T, V, X
  - Format: `piece_id N E S W`

### Version Online
- **`puzzle_versionOnline_extracted.txt`** - Extraction depuis images
  - **15 pièces** extraites (04.png manquant, 1x4.png ignoré)
  - **6 motifs uniques** identifiés
  - Format: `piece_id N E S W`

## Mapping Lettres → Chiffres

### Principe
Tous les codes à 4 lettres (ex: "GOCV") ont été convertis en chiffres:
- **G (Gris)** = **0** (bordure)
- Autres lettres = numéros séquentiels uniques par indice

### Exemple Indice 2
```
GOCV → N=0 E=5 S=2 W=8
   G=0 (bordure)
   O=5 (orange)
   C=2 (cyan)
   V=8 (violet)
```

### Mappings par Indice

**Indice 2 (9 lettres):**
```
G→0, B→1, C→2, F→3, I→4, O→5, R→6, U→7, V→8
```

**Indice 3 (10 lettres):**
```
G→0, B→1, C→2, F→3, J→4, L→5, O→6, R→7, V→8, Y→9
```

**Indice 4 (10 lettres):**
```
G→0, A→1, B→2, L→3, O→4, R→5, S→6, T→7, V→8, X→9
```

## Format des Fichiers

Tous les fichiers txt suivent le format:
```
# Commentaires et informations
piece_id north east south west
```

Exemple:
```
1 0 5 2 8
2 7 7 7 2
3 3 2 4 4
```

Où:
- `piece_id` = numéro de la pièce
- `north, east, south, west` = valeurs des arêtes (0 = bordure)

## Scripts Utilisés

1. **`/tmp/parse_indices_csv.py`** - Parse CSV des indices 3 et 4
2. **`/tmp/parse_indice2_xls.py`** - Parse XLS de l'indice 2
3. **`extract_pieces.py`** - Extrait motifs depuis images (indice 1, versionOnline)

## Notes Importantes

### ✅ Fichiers Prêts à Utiliser
Les fichiers `*_from_csv.txt` et `*_from_xls.txt` sont directement utilisables pour résoudre les puzzles.

### ⚠️ Fichiers Extraits depuis Images
Les fichiers `*_extracted.txt` sont **expérimentaux**:
- Moins de motifs que les données officielles
- Basés sur clustering de couleurs (peut grouper des motifs distincts)
- À utiliser pour validation visuelle uniquement

### Données Manquantes
- **Indice 2**: Les CSV n'existent pas, utilisation des XLS réussie
- **Version Online**: Fichier 04.png manquant, 1x4.png ignoré (nom invalide)

## Utilisation en Java

```java
// Charger un puzzle indice
Map<Integer, Piece> pieces = PuzzleFactory.loadFromFile("data/puzzle_indice2_72_from_xls.txt");

// Résoudre
EternitySolver solver = new EternitySolver();
Board board = new Board(9, 8);  // 9×8 pour 72 pièces
solver.solve(board, pieces);
```

## Voir Aussi

- `COMPARISON_REPORT.md` - Rapport détaillé sur le puzzle 256
- `EXTRACTION_REPORT_INDICE1.md` - Rapport d'extraction indice 1
- `README.md` - Documentation générale des puzzles
