# Test de Validation 6×6

Ce test permet de valider le code du solver avec un puzzle 6×6.

## Caractéristiques

- **Taille**: 6×6 (36 pièces)
- **Lignes**: A-F (0-5)
- **Colonnes**: 1-6 (0-5)
- **Score maximum**: 60 arêtes internes
  - 30 arêtes horizontales (5 × 6)
  - 30 arêtes verticales (6 × 5)

## Compilation

```bash
cd /Users/laurentzamofing/dev/eternity
javac -d bin -cp bin src/model/*.java src/util/*.java src/solver/*.java
javac -cp bin -d bin test/TestValidation6x6.java
```

## Exécution

```bash
java -cp bin TestValidation6x6
```

## Affichage

Le programme affiche:

1. **Liste des pièces** avec leurs arêtes (N, E, S, W)
2. **Board vide** avec coordonnées A-F et 1-6
3. **Score maximum théorique** (60 arêtes internes)
4. **Progression du solver** avec:
   - Nombre de pièces placées
   - Records de profondeur
   - Records de score
   - Affichage détaillé à partir de 26 pièces placées

## Format d'affichage

Le board utilise les coordonnées suivantes:
```
      1     2     3     4     5     6
   ─────┼─────┼─────┼─────┼─────┼─────
 A │ 12  │ --  │ --  │ --  │ --  │ 36  │
   ─────┼─────┼─────┼─────┼─────┼─────
 B │ 25→ │ --  │ --  │ --  │ --  │ 22↓ │
   ...
```

Les symboles de rotation:
- ` ` (espace) = 0° (rotation 0)
- `→` = 90° (rotation 1)
- `↓` = 180° (rotation 2)
- `←` = 270° (rotation 3)

## Pièces du puzzle

Le puzzle contient 36 pièces définies dans `PuzzleFactory.VALIDATION_6x6`:

| ID | N | E | S | W | Type |
|----|---|---|---|---|------|
| 1  | 1 | 2 | 2 | 1 | Centre |
| 2  | 1 | 2 | 1 | 2 | Centre |
| ... | ... | ... | ... | ... | ... |
| 12 | 0 | 5 | 2 | 6 | Bord haut |
| ... | ... | ... | ... | ... | ... |
| 34 | 0 | 7 | 4 | 0 | Coin |
| 36 | 0 | 0 | 5 | 5 | Coin |

## Modifications du solver

Pour ce test, le seuil d'affichage détaillé a été réduit de 192 à 25 pièces dans `EternitySolver.java:964`.

Pour revenir au comportement normal (Eternity II 16×16), remettez la valeur à 192.
