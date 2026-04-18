# 🚀 Scripts de Lancement - Guide Rapide

## 📋 Tous les Scripts Disponibles

### 🎯 Démarrage

| Script | Description | Usage |
|--------|-------------|-------|
| `./start-all.sh` | **Démarre TOUT** (serveur + frontend + solver) | Système complet |
| `./start-server.sh` | Démarre le serveur Spring Boot (port 8080) | Backend seul |
| `./start-frontend.sh` | Démarre le dashboard React (port 5173) | Interface seule |
| `./start-solver.sh` | Démarre le solver avec config interactive | Résolution seule |
| `./run.sh` | Démarre le solver (version simple) | Quick start |

### 🛑 Arrêt

| Script | Description |
|--------|-------------|
| `./stop-all.sh` | **Arrête TOUT** (serveur + frontend + solver) |
| `./stop-server.sh` | Arrête le serveur Spring Boot |
| `./stop-frontend.sh` | Arrête le dashboard React |
| `./stop-solver.sh` | Arrête le solver (avec sauvegarde) |

---

## ⚡ Utilisation Rapide

### Scénario 1 : Système Complet avec Dashboard

```bash
./start-all.sh
# Choisir 'y' pour lancer le solver
# Ouvrir : http://localhost:5173
```

**Arrêt** :
```bash
./stop-all.sh
```

---

### Scénario 2 : Composants Séparés (Contrôle Total)

**Terminal 1 - Serveur Backend** :
```bash
./start-server.sh
# Choisir background (y) ou foreground (n)
```

**Terminal 2 - Frontend Dashboard** :
```bash
./start-frontend.sh
# Choisir background (y) ou foreground (n)
```

**Terminal 3 - Solver** :
```bash
./start-solver.sh
# Configurer : threads et minutes
```

**Arrêt sélectif** :
```bash
./stop-solver.sh     # Arrête juste le solver
./stop-frontend.sh   # Arrête juste le frontend
./stop-server.sh     # Arrête juste le serveur
```

---

### Scénario 3 : Solver Seul (Sans Monitoring)

```bash
./run.sh
# Ou
./start-solver.sh
```

**Arrêt** : `Ctrl+C` ou `./stop-solver.sh`

---

### Scénario 4 : Backend + Frontend (Sans Solver)

```bash
./start-server.sh    # Terminal 1
./start-frontend.sh  # Terminal 2

# Dashboard vide : http://localhost:5173
# Lancer le solver plus tard avec ./start-solver.sh
```

---

## 🎛️ Options de Démarrage

### start-server.sh

**Ports** :
- 8080 : API REST
- 8080/ws : WebSocket

**Logs** : `logs/server.log`

**Options** :
- Foreground : Logs dans le terminal (Ctrl+C pour arrêter)
- Background : Tourne en arrière-plan (./stop-server.sh pour arrêter)

---

### start-frontend.sh

**Port** : 5173

**Logs** : `logs/frontend.log`

**Prérequis** :
- Serveur backend sur :8080 (pour l'API)
- Si pas de serveur : dashboard vide

**Options** :
- Foreground : Hot reload visible (Ctrl+C pour arrêter)
- Background : Tourne en arrière-plan (./stop-frontend.sh pour arrêter)

---

### start-solver.sh

**Configuration interactive** :
```
Nombre de threads (défaut: 16) : 8
Minutes par configuration (défaut: 60) : 30
```

**Résultat** : 8 threads, 30 minutes par config

**Logs** :
- Console + `logs/solver.log`

**Sauvegardes** :
- `saves/eternity2/*/best_*.txt`
- `saves/eternity2/*/current_*.txt`

**Arrêt** : `Ctrl+C` → Sauvegarde automatique

---

### run.sh

Version simple de `start-solver.sh` :
- Pas de configuration interactive
- Utilise tous les CPU
- 1 minute par config par défaut

---

## 📊 Monitoring

### Avec Dashboard (Recommandé)

```bash
# Terminal 1
./start-server.sh → background (y)

# Terminal 2
./start-frontend.sh → background (y)

# Terminal 3
./start-solver.sh
```

**Voir progression** : http://localhost:5173

---

### Sans Dashboard (Logs seulement)

```bash
./run.sh
# Ou
./start-solver.sh
```

**Voir progression** :
```bash
# Logs temps réel
tail -f logs/solver.log

# Sauvegardes
ls -lht saves/eternity2/*/best_*.txt
```

---

## 🔧 Troubleshooting

### Port déjà utilisé

Les scripts détectent automatiquement et proposent de tuer le processus.

**Manuellement** :
```bash
# Trouver le processus
lsof -i :8080   # Serveur
lsof -i :5173   # Frontend

# Tuer
kill [PID]
```

---

### Serveur ne démarre pas

```bash
# Recompiler
mvn clean compile

# Vérifier les logs
tail -f logs/server.log
```

---

### Frontend ne se connecte pas

```bash
# 1. Vérifier que le serveur tourne
curl http://localhost:8080/api/metrics

# 2. Redémarrer le frontend
./stop-frontend.sh
./start-frontend.sh
```

---

## 🎯 Workflows Recommandés

### Développement

```bash
# Background pour serveur et frontend
./start-server.sh → y (background)
./start-frontend.sh → y (background)

# Foreground pour solver (voir les logs)
./start-solver.sh → configurer → voir les logs

# Éditer le code dans IntelliJ
# Le frontend se recharge automatiquement
```

---

### Test Rapide

```bash
./run.sh
# Ou
./start-solver.sh → 2 threads → 0.1 minutes
```

---

### Production / Longue Durée

```bash
./start-all.sh
# Choisir : 16 threads, 1440 minutes (24h)

# Laisser tourner
# Reconnecter : http://localhost:5173
```

---

## 📝 Résumé

| Besoin | Commande |
|--------|----------|
| **Tout en un** | `./start-all.sh` |
| **Serveur seul** | `./start-server.sh` |
| **Frontend seul** | `./start-frontend.sh` |
| **Solver seul** | `./start-solver.sh` ou `./run.sh` |
| **Arrêt total** | `./stop-all.sh` |
| **Arrêt sélectif** | `./stop-[component].sh` |

---

## 🎊 C'est Tout !

**Quick Start** : `./start-all.sh`

**Dashboard** : http://localhost:5173

**Arrêt** : `./stop-all.sh`

🎉 **Bonne résolution !**

---

## 🔬 Profiling & Benchmarks

### JMH benchmarks

```bash
# Compile first
mvn -q compile

# Run FullSolveBenchmark (3×3, 4×4 hard, 5×5/6×6/8×8 generated)
mvn exec:java \
  -Dexec.mainClass=benchmark.FullSolveBenchmark \
  -Dexec.classpathScope=compile

# Run AC3PropagationBenchmark (micro-level AC-3)
mvn exec:java \
  -Dexec.mainClass=benchmark.AC3PropagationBenchmark \
  -Dexec.classpathScope=compile
```

### Java Flight Recorder (JFR) — BB2 / BB3 profiling

```bash
# Record a solve under JFR — heap alloc + lock contention
java -XX:StartFlightRecording=filename=solve.jfr,duration=60s,settings=profile \
     -cp target/classes:target/dependency/* \
     app.MainCLI -v example_4x4_hard_v3

# Open in JDK Mission Control (jmc) or use jfr CLI:
jfr print --events 'jdk.JavaMonitorWait,jdk.ObjectAllocationInNewTLAB' solve.jfr | head -200
```

Use cases:
- **BB2** (scaling 16×16): look at top `ObjectAllocationInNewTLAB` — if `Placement` / `ArrayList<ValidPlacement>` appear >20% → pool them (M7).
- **BB3** (parallel contention): look at `JavaMonitorWait` — identify locks on `SaveStateIO` / `RecordManager` in the solve-hot-path.

### Mutation testing (PITest, BB5)

```bash
# Run against solver core classes (~2-5 min locally)
mvn -P pit org.pitest:pitest-maven:mutationCoverage

# Open the HTML report
open target/pit-reports/$(ls target/pit-reports | tail -1)/index.html
```

Current gate: 30% mutation score on `solver.*`. Raise after tests strengthen.
