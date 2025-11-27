# Rapport d'Extraction - Puzzle Indice 1 (6×6)

Date: 2025-11-16

## Résultats de l'Extraction depuis Images

### Statistiques

- **Images traitées**: 36 pièces (01.png à 36.png)
- **Triangles extraits**: 144/144 (100% - 4 triangles par pièce)
- **Motifs uniques identifiés**: 31 patterns
- **Méthode**: Clustering par histogramme de couleurs (RGB, 8 bins)
- **Seuil de similarité**: 0.08

### Qualité de l'Extraction

#### Points Forts
- ✅ **Extraction complète**: Tous les triangles ont été extraits correctement
- ✅ **Aucune donnée manquante**: Plus de valeurs -1
- ✅ **Amélioration de la géométrie**: Utilisation des distances aux diagonales pour des masques plus robustes
- ✅ **Clustering fonctionnel**: 31 patterns distincts identifiés

#### Limitations
- ⚠️ **Moins de motifs que l'officiel**: 31 patterns vs 76 dans les données CSV officielles
- ⚠️ **Clustering imparfait**: Certains patterns visuellement distincts peuvent être groupés ensemble
- ⚠️ **Signatures basées couleur uniquement**: N'utilise pas les détails de forme ou texture

## Comparaison avec Données Officielles

### Données Officielles (puzzle_indice1_36.txt)
- Source: CSV convertis depuis le projet C++
- **76 valeurs d'arêtes uniques** (0-76)
- Données complètes et validées

### Données Extraites (puzzle_indice1_36_extracted.txt)
- Source: Analyse d'images par vision par ordinateur
- **31 valeurs d'arêtes uniques** (0-30)
- Extraction automatique basée sur clustering

### Analyse

Les données extraites ne correspondent PAS aux données officielles car:

1. **Clustering trop permissif**: Des motifs visuellement différents mais avec des couleurs similaires sont groupés ensemble

2. **Méthode de signature**: L'histogramme de couleurs ne capture pas suffisamment de détails pour distinguer tous les motifs

3. **Variations d'images**: Les images peuvent avoir des variations de luminosité, contraste, ou des artefacts de compression

## Exemples de Pièces Extraites

```
Pièce 1:  N=0  E=1  S=2  W=3
Pièce 7:  N=6  E=7  S=8  W=9   (motifs uniques)
Pièce 13: N=15 E=16 S=17 W=10
```

## Distribution des Motifs

Motifs les plus fréquents:
- Motif 1: 23 occurrences (bordure grise)
- Motif 2: 23 occurrences (bordure grise)
- Motif 0: 10 occurrences (bordure grise - coin)
- Motif 12: 7 occurrences
- Motif 10: 7 occurrences

Motifs rares (1 occurrence):
- Motifs 7, 8, 9, 14, 22, 27, 28, 30

## Recommandations

### Pour Utilisation

**Si vous voulez résoudre le puzzle Indice 1:**
- ✅ **Utiliser**: `data/puzzle_indice1_36.txt` (données officielles)
- ❌ **Ne pas utiliser**: `data/puzzle_indice1_36_extracted.txt` (données expérimentales)

**Les données extraites sont utiles pour:**
- Validation visuelle des pièces
- Comparaison avec les données officielles
- Amélioration de l'algorithme d'extraction
- Recherche et expérimentation

### Améliorations Possibles

Pour améliorer la qualité d'extraction:

1. **Signatures plus riches**:
   - Ajouter des descripteurs de forme (contours, moments)
   - Utiliser des features SIFT/ORB pour texture
   - Analyser les gradients de couleur

2. **Clustering adaptatif**:
   - Tester différents seuils (0.05, 0.08, 0.10)
   - Utiliser DBSCAN ou Mean-Shift au lieu du clustering greedy
   - Validation par cohérence du puzzle

3. **Prétraitement d'images**:
   - Normalisation de luminosité
   - Suppression du bruit
   - Égalisation d'histogramme

4. **Validation croisée**:
   - Comparer avec les données CSV officielles
   - Vérifier la cohérence des arêtes (N/S et E/W doivent correspondre)

## Fichiers Générés

- `/Users/laurentzamofing/dev/eternity/pieces/extracted_pieces.txt` - Fichier original
- `/Users/laurentzamofing/dev/eternity/data/puzzle_indice1_36_extracted.txt` - Copie dans data/
- `/Users/laurentzamofing/dev/eternity/extract_pieces.py` - Script d'extraction

## Conclusion

L'extraction automatique depuis images fonctionne et extrait correctement la géométrie des pièces (4 triangles par pièce). Cependant, la méthode actuelle basée sur l'histogramme de couleurs ne capture pas assez de détails pour identifier tous les motifs uniques avec précision.

**Pour une utilisation en production**: Utiliser les données officielles CSV qui contiennent les 76 motifs distincts.

**Pour la recherche**: Les données extraites peuvent servir de base pour améliorer l'algorithme d'extraction.
