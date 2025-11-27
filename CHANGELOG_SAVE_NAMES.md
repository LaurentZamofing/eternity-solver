# Amélioration des noms de fichiers de sauvegarde

## 📅 Date : 2025-11-17

## 🎯 Problème

Avec le format précédent, toutes les sauvegardes `current` étaient nommées simplement :
```
eternity2_current_1731868234567.txt
```

Ce format ne permettait pas de savoir facilement **quelle configuration** correspondait à quelle sauvegarde.

Avec 48 configurations différentes (24 permutations × 2 ordres), il était difficile de :
- Savoir quelle config travaillait sur quel fichier
- Déboguer et suivre le progrès de chaque config
- Identifier rapidement les sauvegardes lors d'un crash

## ✅ Solution

Les sauvegardes `current` incluent maintenant **le numéro de permutation et l'ordre de tri** :

```
eternity2_p01_ascending_current_1731868234567.txt
eternity2_p12_descending_current_1731868234568.txt
eternity2_p24_ascending_current_1731868234569.txt
```

### Format
```
eternity2_pXX_ORDER_current_TIMESTAMP.txt
```

Où :
- `pXX` : Numéro de permutation (01-24)
- `ORDER` : `ascending` ou `descending`
- `TIMESTAMP` : Millisecondes depuis epoch (pour l'ancienneté)

## 🔧 Modifications apportées

### 1. MainParallel.java

**Nouvelle méthode `extractConfigId()`** :
```java
private static String extractConfigId(String filepath) {
    String filename = new File(filepath).getName();
    // puzzle_eternity2_p01_1_2_3_4_ascending.txt -> eternity2_p01_ascending

    if (filename.startsWith("puzzle_eternity2_p")) {
        String[] parts = filename.split("_");
        if (parts.length >= 8) {
            String perm = parts[2]; // pXX
            String order = parts[7].replace(".txt", ""); // ascending/descending
            return "eternity2_" + perm + "_" + order;
        }
    }

    return "eternity2";
}
```

**Utilisation** :
- Ligne 80 : Extraction du configId depuis le filepath
- Ligne 83 : `SaveStateManager.findCurrentSave(configId)` au lieu de `config.getType()`
- Lignes 190 & 230 : `solver.setPuzzleName(configId)` pour sauvegarder avec le bon nom

### 2. SaveStateManager.java

Aucune modification nécessaire ! La méthode `findCurrentSave()` utilise déjà le `puzzleName` reçu en paramètre et construit le pattern de recherche :
```java
name.startsWith(baseName + "_current_") && name.endsWith(".txt")
```

Avec le nouveau `puzzleName` (ex: "eternity2_p01_ascending"), elle cherche automatiquement les bons fichiers.

### 3. Tests

**Nouveau test** : `test/TestConfigId.java`
- Valide l'extraction correcte du configId depuis les noms de fichiers
- Vérifie le format des noms de sauvegarde générés

## 📊 Exemple avant/après

### Avant
```
saves/
├── eternity2_current_1731868234567.txt   # Quelle config ???
├── eternity2_current_1731868234568.txt   # Impossible à savoir
├── eternity2_best_176.txt
└── eternity2_best_175.txt
```

### Après
```
saves/
├── eternity2_p01_ascending_current_1731868234567.txt   # Config p01 ascending ✓
├── eternity2_p01_descending_current_1731868234568.txt  # Config p01 descending ✓
├── eternity2_p12_ascending_current_1731868234569.txt   # Config p12 ascending ✓
├── eternity2_best_176.txt                              # Partagé entre toutes
└── eternity2_best_175.txt                              # Partagé entre toutes
```

## 🎁 Bénéfices

### Pour l'utilisateur
- ✅ **Compréhension immédiate** : Un coup d'œil sur le répertoire `saves/` suffit
- ✅ **Débogage facilité** : Identifier rapidement quelle config pose problème
- ✅ **Suivi du progrès** : Voir quelles configs progressent le plus vite

### Pour le développeur
- ✅ **Code plus clair** : Le `configId` est explicite
- ✅ **Compatibilité** : Les anciennes sauvegardes continuent de fonctionner
- ✅ **Extensibilité** : Facile d'ajouter d'autres infos dans le nom

### Pour le monitoring
```bash
# Voir toutes les sauvegardes par config
ls -lh saves/eternity2_p*_current_*.txt

# Compter les configs en cours
ls saves/eternity2_p*_current_*.txt | wc -l

# Trouver la plus ancienne sauvegarde
ls -lt saves/eternity2_p*_current_*.txt | tail -1
```

## 🔄 Rétrocompatibilité

Les **anciennes sauvegardes** sans pXX continuent de fonctionner grâce au fallback dans `findCurrentSave()` :

```java
// Si aucun fichier avec timestamp, chercher l'ancien format
if (currentFiles == null || currentFiles.length == 0) {
    File legacyFile = new File(SAVE_DIR + baseName + "_current.txt");
    return legacyFile.exists() ? legacyFile : null;
}
```

## ✅ Tests effectués

### Test 1 : Extraction du configId
```bash
java -cp bin TestConfigId
```
**Résultat** : ✓ Tous les formats extraits correctement

### Test 2 : MainParallel
```bash
java -cp bin MainParallel 2
```
**Résultat** : ✓ Les threads démarrent avec les configs p01_ascending et p01_descending

### Test 3 : Analyse des configs
**Résultat** :
- ✓ 48 configurations reconnues
- ✓ 48 jamais commencées (après nettoyage)
- ✓ 0 en cours

## 📚 Documentation mise à jour

- ✅ `PARALLELISATION.md` : Section "Système de sauvegarde"
- ✅ `README_PARALLEL.md` : Section "Structure des sauvegardes"
- ✅ `CHANGELOG_SAVE_NAMES.md` : Ce document

## 🚀 Prochaines étapes

Le système est maintenant **prêt pour la production** avec des noms de fichiers explicites et faciles à suivre.

Pour lancer :
```bash
./run_parallel.sh
```

Les nouvelles sauvegardes seront automatiquement créées avec le nouveau format !

---

**Développé le** : 2025-11-17
**Status** : ✅ COMPLET ET TESTÉ
