# Configuration de la rotation automatique des puzzles

## Modifications effectuées

### 1. Sauvegarde automatique (toutes les 1 minute)
- **Fichier**: `src/solver/EternitySolver.java`
- **Ligne**: 248
- **Valeur**: `AUTO_SAVE_INTERVAL = 60000` (60 000 ms = 1 minute)
- **Fonction**: Sauvegarde l'état actuel dans le fichier "current" toutes les 1 minute

### 2. Changement automatique de puzzle (toutes les 10 minutes)
- **Fichier**: `src/MainSequential.java`
- **Ligne**: 47
- **Valeur**: `PUZZLE_TIMEOUT = 600000` (600 000 ms = 10 minutes)
- **Fonction**: Change automatiquement de puzzle après 10 minutes, même si non résolu

## Comportement du système

Le programme fonctionne maintenant en boucle continue :

1. **Cycle de résolution** :
   - Charge un puzzle de la liste (dans l'ordre)
   - Tente de le résoudre pendant **10 minutes maximum**
   - Sauvegarde l'état actuel toutes les **1 minute** dans le fichier "current"

2. **Après 10 minutes** :
   - Le programme interrompt la résolution du puzzle actuel
   - Sauvegarde l'état final dans le fichier "current"
   - Passe automatiquement au puzzle suivant

3. **Cycle infini** :
   - Une fois tous les puzzles parcourus, recommence depuis le premier
   - Continue indéfiniment jusqu'à interruption manuelle (Ctrl+C)

## Liste des puzzles (ordre de rotation)

1. `puzzle_online.txt`
2. `puzzle_indice1.txt`
3. `puzzle_indice2.txt`
4. `puzzle_indice3.txt`
5. `puzzle_indice4.txt`
6. `puzzle_eternity2.txt` (256 pièces)

Puis retour au puzzle 1...

## Utilisation

### Démarrer le programme avec rotation :
```bash
java -cp bin MainSequential
```

Ou utiliser le script de test :
```bash
./test_rotation.sh
```

### Arrêter le programme :
```
Ctrl+C
```

## Fichiers de sauvegarde

Chaque puzzle a ses propres fichiers de sauvegarde dans le répertoire `saves/` :

- **Fichier "current"** : État actuel de la résolution (ex: `online_current.txt`)
  - Mis à jour toutes les 1 minute
  - Utilisé pour reprendre la résolution du même puzzle

- **Fichiers "best"** : Meilleurs scores atteints (ex: `online_023.txt`)
  - Créés lorsqu'un nouveau record de profondeur est atteint
  - Conservent l'historique des meilleurs états

## Modification des intervalles

### Pour changer l'intervalle de sauvegarde (actuellement 1 minute) :
Éditer `src/solver/EternitySolver.java` ligne 248 :
```java
private static final long AUTO_SAVE_INTERVAL = 60000; // 1 minute
```

### Pour changer l'intervalle de rotation des puzzles (actuellement 10 minutes) :
Éditer `src/MainSequential.java` ligne 47 :
```java
private static final long PUZZLE_TIMEOUT = 600000; // 10 minutes
```

Puis recompiler :
```bash
javac -d bin -cp bin src/MainSequential.java src/solver/EternitySolver.java
```

## Exemples de valeurs (en millisecondes)

- 30 secondes : `30000`
- 1 minute : `60000`
- 5 minutes : `300000`
- 10 minutes : `600000`
- 30 minutes : `1800000`
- 1 heure : `3600000`
