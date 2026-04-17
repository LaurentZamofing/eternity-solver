# Mode Debug Step-by-Step

## Utilisation

### Option 1: Avec le script (recommandé - démarre from scratch)
```bash
./run_debug_clean.sh
```

Ce script supprime automatiquement les sauvegardes et démarre avec 0 pièces.

### Option 2: Avec le script simple (peut reprendre une sauvegarde)
```bash
./run_debug.sh
```

### Option 3: Depuis IntelliJ
1. **IMPORTANT**: Supprimer d'abord les sauvegardes :
   ```bash
   rm -rf saves/eternity2/eternity2_p01_ascending/*
   ```
2. Ouvrir la classe `app.MainParallel`
3. Run → Edit Configurations → Program arguments : `data/eternity2/eternity2_p01_ascending.txt`
4. Vérifier que `DebugStepByStep: true` dans le fichier config (ligne 13)
5. Run 'MainParallel.main()'
6. Le programme démarrera avec 0 pièces et s'arrêtera à chaque étape

### Option 4: En ligne de commande
```bash
# Supprimer les saves d'abord
rm -rf saves/eternity2/eternity2_p01_ascending/*

# Compiler et lancer
mvn compile
mvn exec:java -Dexec.mainClass="app.MainParallel" \
  -Dexec.args="data/eternity2/eternity2_p01_ascending.txt"
```

## Configuration

Le mode step-by-step est contrôlé par le paramètre dans le fichier de configuration :

```
# DebugStepByStep: true
```

### Autres options de debug disponibles

```
# DebugBacktracking: true      # Logs détaillés du backtracking
# DebugShowBoard: true          # Affiche le board après chaque placement
# DebugShowAlternatives: true   # Montre les alternatives considérées
# DebugMaxCandidates: 5        # Nombre max d'alternatives à afficher
# DebugStepByStep: true        # PAUSE après chaque étape (nécessite ENTER)
```

## Fonctionnement

Quand `DebugStepByStep: true` est activé :

1. **Le programme s'arrête** après chaque placement de pièce
2. **Vous voyez** :
   - La pièce placée
   - Les alternatives considérées
   - L'état du board
   - Les statistiques
3. **Appuyez sur ENTER** pour continuer vers l'étape suivante
4. **Tapez 'q'** pour désactiver les pauses et continuer sans arrêt

## Points d'arrêt automatiques

Le programme s'arrête automatiquement à :

- **Chaque placement de pièce** (MRVPlacementStrategy:234)
- **Détection de singleton** (SingletonDetector:169, 191)
- **Sélection de cellule MRV** (MRVCellSelector:368, 371)

## Utilisation avec IntelliJ Debugger

Pour combiner avec le debugger IntelliJ :

1. Mettre `DebugStepByStep: false` pour désactiver les pauses interactives
2. Placer des breakpoints dans le code
3. Lancer en mode Debug (Shift+F9)
4. Utiliser Step Over (F8) / Step Into (F7)

## Exemple de sortie

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
⏸  Press ENTER to continue (or 'q' to disable step-by-step):
```

## Troubleshooting

### Le programme affiche soudainement beaucoup de pièces sans pause
**Cause**: Le programme charge une sauvegarde avec des pièces déjà placées

**Solution**: Supprimer les sauvegardes avant de lancer
```bash
rm -rf saves/eternity2/eternity2_p01_ascending/*
```

Ou utiliser le script qui le fait automatiquement :
```bash
./run_debug_clean.sh
```

### Le programme ne s'arrête pas
- ✅ Vérifier que `DebugStepByStep: true` est bien dans le fichier config
- ✅ Recompiler avec `mvn clean compile`
- ✅ Vérifier que le fichier config est bien chargé (logs au démarrage)

### Trop de pauses
- Tapez 'q' pendant une pause pour désactiver le mode
- Ou modifiez le config et relancez
