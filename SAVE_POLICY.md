# Politique de Sauvegarde des Records

## Configuration (Simplifiée)

**Politique de conservation des fichiers:**
- **`current_*.txt`**: Un seul fichier conservé (le plus récent) - les anciens sont automatiquement supprimés
- **`best_*.txt`**: Tous les fichiers sont conservés pour validation

Cette politique permet de:
1. Limiter l'espace disque utilisé par les fichiers temporaires
2. Conserver tous les records pour validation de la logique
3. Avoir toujours l'état actuel de la recherche disponible

## Types de fichiers sauvegardés

### 1. `current_[timestamp].txt`
- **État actuel** de la recherche à chaque étape
- **Horodatage** en millisecondes (timestamp Unix)
- **Exemple**: `current_1763677656733.txt`
- **Nettoyage automatique**: Seul le fichier le plus récent est conservé

### 2. `best_[depth].txt`
- **Meilleur état** pour chaque profondeur atteinte
- **Sauvegardé** dès que depth >= 10
- **Exemple**: `best_137.txt`, `best_138.txt`, `best_150.txt`
- **Conservation**: Tous les fichiers sont conservés

## Structure des dossiers

```
saves/
├── eternity2/
│   ├── eternity2_p01_ascending/
│   │   ├── current_1763677656733.txt    ← Seul fichier current présent
│   │   ├── best_137.txt                 ← Tous conservés
│   │   ├── best_138.txt
│   │   └── best_150.txt
│   └── ...
├── indice1/
└── ...
```

## Détection de Nouveaux Records

Un nouveau record est détecté quand:
1. Le depth (nombre de pièces placées par backtracking) >= 10
2. Ce depth n'a jamais été atteint auparavant
3. Le fichier `best_[depth].txt` n'existe pas encore

**Message affiché:**
```
🏆 Nouveau record: saves/puzzle_name/best_150.txt (150 pièces)
```

## Validation de la Logique

Avec cette politique, vous pouvez:

1. **Suivre la progression actuelle**
   - Le fichier `current_*.txt` montre l'état actuel de la recherche
   - L'horodatage permet de vérifier quand la dernière sauvegarde a eu lieu

2. **Analyser tous les records**
   - Tous les fichiers `best_*.txt` sont conservés
   - Comparer les solutions à différentes profondeurs
   - Historique complet des records atteints

3. **Vérifier la cohérence**
   - Aucune perte de records
   - Historique complet des meilleurs états pour debugging

## Espace Disque

**Estimation de l'espace requis:**

- Fichier `current_*.txt`: ~5-10 KB (1 seul fichier)
- Fichier `best_*.txt`: ~5-10 KB par fichier
- Pour une recherche longue (plusieurs heures):
  - 1 fichier `current_*.txt` = ~5-10 KB
  - ~20-50 fichiers `best_*.txt` = ~100 KB - 500 KB
  - **Total: ~100 KB - 1 MB par puzzle**

Pour les 25 puzzles eternity2: **~2.5-25 MB total**

## Nettoyage Manuel

Si vous souhaitez nettoyer manuellement les anciennes sauvegardes:

### Supprimer tous les fichiers current (ils seront recréés):
```bash
rm saves/puzzle_name/current_*.txt
```

### Supprimer tous les fichiers best (attention, perte de données!):
```bash
rm saves/puzzle_name/best_*.txt
```

### Garder uniquement les 10 meilleurs best:
```bash
cd saves/puzzle_name/
ls -v best_*.txt | head -n -10 | xargs rm
```

## Implémentation Technique

**Fichier modifié:** `src/util/SaveStateManager.java`

**Changements apportés:**
1. Suppression du flag `enableAutoCleanup` (comportement simplifié)
2. Nettoyage automatique des `current_*.txt` (ligne 188)
3. Conservation de tous les `best_*.txt` (pas de nettoyage)

**Code clé:**

```java
// Nettoyer les anciens fichiers "current" (garder seulement le plus récent)
cleanupOldCurrentSaves(puzzleDir, currentFile);

// Ne JAMAIS nettoyer les fichiers best_*.txt - on les garde tous pour validation
```

## Historique des Modifications

**Version:** 2025-01-20 (v2)

**Modifications:**
1. Simplification de la politique de sauvegarde
2. Suppression du flag `enableAutoCleanup` et des méthodes associées
3. Nettoyage automatique des `current_*.txt` (toujours actif)
4. Conservation de tous les `best_*.txt` (jamais nettoyés)
5. Mise à jour de la documentation

**Compatibilité:** Changement de comportement par rapport à la version précédente, mais plus simple et plus prévisible.
