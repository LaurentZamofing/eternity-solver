# 🧩 Eternity II - Résolveur Parallèle

## 🚀 Démarrage rapide

### Lancer avec tous les CPUs disponibles
```bash
./run_parallel.sh
```

### Lancer avec un nombre spécifique de threads
```bash
./run_parallel.sh 8
```

## 📋 Ce qui se passe

1. **Analyse** : Scan des 48 configurations disponibles
2. **Priorisation** :
   - ✅ Configs jamais commencées en priorité
   - ✅ Puis sauvegardes les plus anciennes
3. **Lancement** : N threads travaillent en parallèle
4. **Sauvegarde auto** : Toutes les 10 minutes avec timestamp

## 📊 Sortie typique

```
╔═══════════════════════════════════════════════════════════════════╗
║          ETERNITY II - RÉSOLVEUR PARALLÈLE                       ║
╚═══════════════════════════════════════════════════════════════════╝

⚙️  Nombre de threads: 8

📁 Analyse de 48 configurations disponibles...

╔═══════════════════════════════════════════════════════════════════╗
║              STATISTIQUES DES CONFIGURATIONS                     ║
╚═══════════════════════════════════════════════════════════════════╝

  📊 Total configurations : 48
  🆕 Jamais commencées    : 48
  🔄 En cours             : 0

✓ 8 thread(s) lancé(s)

🚀 [Thread 1] Démarrage: Eternity II (16×16) - Permutation 1 - ASCENDING
   Fichier: puzzle_eternity2_p01_1_2_3_4_ascending.txt
   Statut: NOUVEAU

🚀 [Thread 2] Démarrage: Eternity II (16×16) - Permutation 1 - DESCENDING
   Fichier: puzzle_eternity2_p01_1_2_3_4_descending.txt
   Statut: NOUVEAU

...

⏳ Les threads travaillent... (Ctrl+C pour arrêter)
```

## 🎯 Stratégie

### 48 configurations uniques

- **4 pièces de coin** → 4! = 24 permutations
- **2 ordres de tri** (ascending/descending) → ×2
- **Total** : 24 × 2 = 48 branches d'exploration

### Pièces fixes

Chaque configuration a **9 pièces fixes** :
- 4 coins (variables selon permutation)
- 5 pièces centrales (toujours aux mêmes positions)

### Pas de conflits

- Chaque thread travaille sur une configuration unique
- Les sauvegardes sont séparées par `puzzleType`
- Partage des meilleurs scores via fichiers `best_XXX.txt`

## 📁 Structure des sauvegardes

Les sauvegardes sont organisées par **sous-répertoires** selon le type de puzzle :

```
saves/
├── eternity2/
│   ├── eternity2_p01_ascending_current_1731868234567.txt   # Config p01 ascending
│   ├── eternity2_p01_descending_current_1731868234568.txt  # Config p01 descending
│   ├── eternity2_p12_ascending_current_1731868234569.txt   # Config p12 ascending
│   ├── eternity2_best_176.txt                              # Meilleur score global
│   ├── eternity2_best_175.txt                              # Deuxième meilleur
│   └── eternity2_best_170.txt                              # Troisième meilleur
├── indice1/
│   └── ...
└── indice2/
    └── ...
```

**Format des noms** : `eternity2_pXX_ORDER_current_TIMESTAMP.txt`
- Permet d'identifier facilement quelle config correspond à quelle sauvegarde
- Les `best` sont partagés entre toutes les configs

**Affichage visuel** : Chaque fichier contient un affichage ASCII du plateau pour visualisation rapide :
```
# AFFICHAGE VISUEL DU PLATEAU (176 pièces placées)
#   10   21   32   43  ...
#   54   65   76   87  ...
#   .    .    .    .   ...
```

## ⚙️ Configuration

### Nombre de threads recommandé

- **Workstation** : Nombre de CPUs (détection auto)
- **Serveur** : Selon RAM disponible (1-2 GB par thread)
- **Test** : 2-4 threads

### Limite pratique

- Maximum **48 threads** (1 par configuration)
- Au-delà, les threads se partagent les configs

## 🛑 Arrêt propre

Appuyez sur **Ctrl+C** pour arrêter tous les threads proprement.

Les sauvegardes sont déjà écrites automatiquement toutes les 10 minutes.

## 📚 Documentation complète

Voir [PARALLELISATION.md](PARALLELISATION.md) pour tous les détails techniques.

## 🐛 Dépannage

### Erreur "OutOfMemoryError"
→ Réduire le nombre de threads

### Aucune config trouvée
→ Vérifier que le répertoire `data/` contient les fichiers `puzzle_eternity2_p*.txt`

### Compilation nécessaire
```bash
javac -d bin -sourcepath src src/MainParallel.java
```

## 📊 Monitoring avancé

Pour surveiller l'utilisation des ressources :
```bash
# CPU et mémoire
top | grep java

# Nombre de threads Java actifs
ps aux | grep java | wc -l
```

## ✅ Vérification rapide

Pour tester que tout fonctionne avec 2 threads pendant 30 secondes :
```bash
# Lancer en arrière-plan
./run_parallel.sh 2 &
JAVA_PID=$!

# Attendre 30s
sleep 30

# Arrêter
kill $JAVA_PID
```

Vous devriez voir les threads démarrer et commencer le backtracking.
