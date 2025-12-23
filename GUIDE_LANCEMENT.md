# 🚀 Guide de Lancement - Eternity II Solver

Guide complet pour lancer tous les composants du système Eternity II : Backend, Frontend, et Serveur de Monitoring.

---

## 📋 Table des Matières

1. [Vue d'Ensemble](#vue-densemble)
2. [Prérequis](#prérequis)
3. [Lancement Rapide (Tout en Une Fois)](#lancement-rapide)
4. [Lancement Composant par Composant](#lancement-composant-par-composant)
5. [Configurations Avancées](#configurations-avancées)
6. [Monitoring et Visualisation](#monitoring-et-visualisation)
7. [Troubleshooting](#troubleshooting)

---

## 🏗️ Vue d'Ensemble

Le système Eternity II est composé de **3 composants** :

```
┌─────────────────────────────────────────────────────────────┐
│                    ARCHITECTURE SYSTÈME                      │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐      ┌──────────────┐      ┌───────────┐ │
│  │   BACKEND    │      │   SERVEUR    │      │ FRONTEND  │ │
│  │   SOLVER     │─────→│  MONITORING  │←─────│  REACT    │ │
│  │ (MainParallel)│      │ Spring Boot  │      │   VITE    │ │
│  └──────────────┘      └──────────────┘      └───────────┘ │
│         ↓                      ↓                     ↑       │
│    Résout le                 API                WebSocket   │
│    puzzle en              REST/WS                  UI       │
│    parallèle              :8080                  :5173      │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Composants :

| Composant | Description | Port | Technologie |
|-----------|-------------|------|-------------|
| **Backend Solver** | Résolution parallèle du puzzle | - | Java Multi-thread |
| **Serveur Monitoring** | API et WebSocket pour métriques | 8080 | Spring Boot |
| **Frontend Dashboard** | Interface web temps réel | 5173 | React + Vite |

---

## ✅ Prérequis

### Java
```bash
java --version
# Requis: Java 17 ou supérieur
```

### Maven
```bash
mvn --version
# Requis: Maven 3.8 ou supérieur
```

### Node.js (pour le frontend)
```bash
node --version
npm --version
# Requis: Node 18 ou supérieur, npm 9 ou supérieur
```

---

## 🚀 Lancement Rapide (Tout en Une Fois)

### Option 1 : Lancement Complet Automatique

Créez un script `start-all.sh` :

```bash
#!/bin/bash

echo "🚀 Démarrage du système Eternity II Solver..."
echo ""

# 1. Compiler le projet
echo "📦 Compilation du projet..."
mvn clean compile -q

# 2. Lancer le serveur monitoring en background
echo "🖥️  Démarrage du serveur monitoring (port 8080)..."
mvn spring-boot:run > logs/server.log 2>&1 &
SERVER_PID=$!
echo "   PID serveur: $SERVER_PID"

# Attendre que le serveur démarre
sleep 10

# 3. Lancer le frontend en background
echo "🎨 Démarrage du frontend (port 5173)..."
cd frontend
npm run dev > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..
echo "   PID frontend: $FRONTEND_PID"

# Attendre que le frontend démarre
sleep 5

# 4. Lancer le solver
echo "🧩 Démarrage du solver parallèle..."
echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║  Système démarré !                                       ║"
echo "║                                                           ║"
echo "║  🖥️  Serveur   : http://localhost:8080                   ║"
echo "║  🎨 Dashboard : http://localhost:5173                    ║"
echo "║  🧩 Solver    : En cours...                              ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# Lancer le solver (bloquant)
mvn exec:java -Dexec.mainClass="app.MainParallel" -Dexec.args="16 60"

# Cleanup quand on arrête
echo ""
echo "🛑 Arrêt du système..."
kill $SERVER_PID $FRONTEND_PID 2>/dev/null
echo "✅ Système arrêté"
```

Puis :
```bash
chmod +x start-all.sh
./start-all.sh
```

---

## 🔧 Lancement Composant par Composant

### 1️⃣ BACKEND : Serveur de Monitoring Spring Boot

**Port** : 8080
**Fonction** : API REST + WebSocket pour métriques en temps réel

#### Lancement depuis IntelliJ :

```
1. Ouvrir : src/main/java/monitoring/MonitoringApplication.java
2. Clic droit sur "public static void main"
3. Run 'MonitoringApplication.main()'
```

#### Lancement depuis Terminal :

```bash
# Option A : Maven Spring Boot
mvn spring-boot:run

# Option B : Maven exec
mvn exec:java -Dexec.mainClass="monitoring.MonitoringApplication"

# Option C : JAR compilé
mvn package -DskipTests
java -jar target/eternity-solver-1.0.0.jar
```

#### Vérification :

```bash
# Tester que le serveur répond
curl http://localhost:8080/api/metrics

# Ou ouvrir dans le navigateur
open http://localhost:8080/swagger-ui.html
```

**Logs attendus** :
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.1.5)

Started MonitoringApplication in 3.2 seconds
```

---

### 2️⃣ FRONTEND : Dashboard React

**Port** : 5173
**Fonction** : Interface web pour visualiser la progression

#### Première Installation :

```bash
cd frontend
npm install
```

#### Lancement depuis Terminal :

```bash
cd frontend
npm run dev
```

#### Lancement depuis IntelliJ :

```
1. Ouvrir : frontend/package.json
2. Dans l'onglet "npm scripts", double-cliquer sur "dev"
3. Ou Terminal IntelliJ : cd frontend && npm run dev
```

#### Vérification :

```bash
# Ouvrir dans le navigateur
open http://localhost:5173
```

**Logs attendus** :
```
  VITE v5.0.8  ready in 234 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
  ➜  press h to show help
```

---

### 3️⃣ SOLVER : MainParallel (Résolution Parallèle)

**Fonction** : Résout le puzzle Eternity II avec plusieurs threads

#### Lancement depuis IntelliJ :

**Méthode 1 - Direct** :
```
1. Ouvrir : src/main/java/app/MainParallel.java
2. Clic droit sur "public static void main"
3. Run 'MainParallel.main()'
```

**Méthode 2 - Terminal IntelliJ** :
```bash
# Alt+F12 pour ouvrir le terminal
./run.sh

# Ou avec paramètres personnalisés
mvn exec:java -Dexec.args="8 30"
```

**Méthode 3 - Configuration Run** :
```
Run → Edit Configurations → + → Application
Name: Eternity Solver
Main class: app.MainParallel
Program arguments: 16 60
→ Apply → OK → Run
```

#### Paramètres :

```bash
mvn exec:java -Dexec.args="[NUM_THREADS] [MINUTES_PAR_CONFIG]"

# Exemples :
mvn exec:java -Dexec.args="4 10"   # 4 threads, 10 min par config
mvn exec:java -Dexec.args="16 60"  # 16 threads, 1h par config
mvn exec:java -Dexec.args="32 1440" # 32 threads, 24h par config
```

**Sans arguments** : Utilise tous les CPU et 1 minute par config

#### Arrêt Propre :

```
Ctrl+C → Le solver sauvegarde automatiquement avant de quitter
```

---

## 🎯 Scénarios d'Utilisation

### Scénario 1 : Test Rapide (5 minutes)

**But** : Vérifier que tout fonctionne

```bash
# Terminal 1 : Serveur
mvn spring-boot:run

# Terminal 2 : Frontend
cd frontend && npm run dev

# Terminal 3 : Solver (test rapide)
mvn exec:java -Dexec.args="2 0.1"

# Navigateur :
open http://localhost:5173
```

---

### Scénario 2 : Développement avec Monitoring

**But** : Développer en voyant les métriques en temps réel

```bash
# Terminal 1 : Serveur (REQUIS pour monitoring)
mvn spring-boot:run

# Terminal 2 : Frontend (hot reload activé)
cd frontend && npm run dev

# Terminal 3 : Solver
mvn exec:java -Dexec.args="4 5"

# IntelliJ : Éditer le code
# Le frontend se recharge automatiquement
```

---

### Scénario 3 : Production - Recherche Longue Durée

**But** : Laisser tourner pendant des jours

```bash
# Option A : Solver seul (sans monitoring)
nohup mvn exec:java -Dexec.args="16 1440" > logs/solver.log 2>&1 &

# Option B : Avec monitoring (recommandé)
# Terminal 1
nohup mvn spring-boot:run > logs/server.log 2>&1 &

# Terminal 2
cd frontend && nohup npm run dev > ../logs/frontend.log 2>&1 &

# Terminal 3
mvn exec:java -Dexec.args="16 1440"

# Déconnecter et laisser tourner
# Reconnecter plus tard : http://localhost:5173
```

---

## 📊 Monitoring et Visualisation

### Dashboard Web (Recommandé)

**URL** : http://localhost:5173

**Fonctionnalités** :
- 📈 Graphiques temps réel de progression
- 🎯 Vue du plateau avec pièces placées
- 📊 Statistiques par configuration
- ⏱️ Temps de calcul cumulé
- 🏆 Meilleurs records
- 🔄 Mise à jour automatique via WebSocket

### API REST

**Base URL** : http://localhost:8080

**Endpoints** :
```bash
# Métriques globales
curl http://localhost:8080/api/metrics

# Métriques par configuration
curl http://localhost:8080/api/metrics/eternity2

# Historique
curl http://localhost:8080/api/metrics/history

# Plateau actuel
curl http://localhost:8080/api/board/current

# Documentation complète
open http://localhost:8080/swagger-ui.html
```

### Logs et Fichiers

**Sauvegardes** :
```bash
# Voir les meilleurs résultats
ls -lht saves/eternity2/*/best_*.txt

# Dernière sauvegarde par config
ls -lht saves/eternity2/*/current_*.txt | head -3
```

**Logs** :
```bash
# Logs du solver (si lancé en background)
tail -f logs/solver.log

# Logs du serveur
tail -f logs/server.log

# Logs du frontend
tail -f logs/frontend.log
```

---

## ⚙️ Configurations Avancées

### 1. Configuration du Solver (MainParallel)

**Fichier** : N/A (paramètres en ligne de commande)

**Paramètres disponibles** :
```
args[0] : Nombre de threads (défaut: tous les CPU)
args[1] : Minutes par configuration (défaut: 1.0)
```

**Exemples** :
```bash
# Test rapide - 2 threads, 30 secondes
mvn exec:java -Dexec.args="2 0.5"

# Équilibré - 8 threads, 10 minutes
mvn exec:java -Dexec.args="8 10"

# Performance - tous les CPU, 1 heure
mvn exec:java -Dexec.args="16 60"

# Longue durée - 32 threads, 24 heures
mvn exec:java -Dexec.args="32 1440"
```

### 2. Configuration du Serveur Spring Boot

**Fichier** : `src/main/resources/application.properties`

```properties
# Port du serveur
server.port=8080

# Base de données H2 (métriques)
spring.datasource.url=jdbc:h2:mem:monitoring
spring.jpa.hibernate.ddl-auto=update

# CORS (pour le frontend)
cors.allowed-origins=http://localhost:5173
```

**Changer le port** :
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

### 3. Configuration du Frontend

**Fichier** : `frontend/vite.config.ts`

```typescript
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': {
        target: 'http://localhost:8080',
        ws: true
      }
    }
  }
})
```

**Changer le port** :
```bash
cd frontend
npm run dev -- --port 3000
```

---

## 🎨 Monitoring et Visualisation

### 1. Dashboard Web (Interface Principale)

**Accès** : http://localhost:5173

**Écrans disponibles** :

#### 📊 Vue d'Ensemble
- Progression globale (pièces placées)
- Temps de calcul total
- Nombre de configurations actives
- Vitesse (pièces/seconde)

#### 🎯 Vue Détaillée par Configuration
- Graphique de progression dans le temps
- Profondeur max atteinte
- Temps cumulé
- Vitesse moyenne
- Prochaine rotation

#### 🔍 Vue Plateau
- Visualisation du plateau 16×16
- Pièces placées en temps réel
- Zoom sur régions problématiques
- Export PNG

#### 📈 Graphiques Historiques
- Évolution de la profondeur
- Courbe de vitesse
- Comparaison entre configurations
- Prédictions de temps restant

### 2. Logs Console

Le solver affiche des logs détaillés :

```
🚀 [Thread 1] Starting: eternity2_p01_ascending
   File: eternity2_p01_ascending.txt
   Status: RESUME (cumulative time: 2h 15m 30s)

   [Thread 1] Resuming from save (170 pieces placed)
   📂 Save loaded: current_1766503025220.txt (170 pieces)
   📅 Date: 2025-12-23_16-10-25
   📋 Placement order: 170 placements tracked

   [Thread 1] Pieces to place: 86 pieces remaining
   [Thread 1] Fixed pieces: 0
   [Thread 1] Timeout: 60s
   [Thread 1] Starting solver...

📊 [Depth 171] New placement at (5,7) piece #45 rotation 2
💾 Saving state... (171/256 pieces)
🏆 New record! best_171.txt saved
```

### 3. Fichiers de Sauvegarde

```bash
saves/eternity2/
├── eternity2/
│   ├── current_1766503025220.txt  # État actuel (timestamp)
│   ├── best_150.txt                # Record à 150 pièces
│   ├── best_160.txt                # Record à 160 pièces
│   └── best_170.txt                # Record à 170 pièces
├── eternity2_p01_ascending/
│   ├── current_*.txt
│   └── best_*.txt
└── eternity2_from_edge_puzzle/
    ├── current_*.txt
    └── best_*.txt
```

**Visualiser une sauvegarde** :
```bash
# Afficher le meilleur résultat
cat saves/eternity2/eternity2/best_170.txt
```

---

## 🛠️ Troubleshooting

### Problème : "Could not find or load main class MainParallel"

**Solution** :
```bash
# 1. Recompiler
mvn clean compile

# 2. Vérifier que la classe existe
ls -la target/classes/app/MainParallel.class

# 3. Dans IntelliJ : File → Invalidate Caches → Restart

# 4. Utiliser le nom complet : app.MainParallel
```

---

### Problème : "Port 8080 already in use"

**Solution** :
```bash
# Trouver le processus
lsof -i :8080

# Tuer le processus
kill -9 [PID]

# Ou changer le port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

---

### Problème : "No configurations found"

**Causes possibles** :
1. Métadonnées manquantes dans les fichiers de config
2. Fichiers de config dans le mauvais répertoire

**Solution** :
```bash
# Vérifier que les fichiers existent
ls -la data/eternity2/*.txt

# Vérifier qu'ils ont les métadonnées requises
head -10 data/eternity2/eternity2.txt

# Doit contenir :
# Type: Eternity II
# Dimensions: 16x16
```

---

### Problème : Frontend ne se connecte pas au serveur

**Solution** :
```bash
# 1. Vérifier que le serveur tourne
curl http://localhost:8080/api/metrics

# 2. Vérifier le proxy dans vite.config.ts
cat frontend/vite.config.ts

# 3. Redémarrer le frontend
cd frontend
npm run dev
```

---

### Problème : "NullPointerException" dans le solver

**Cause** : Données corrompues dans les fichiers de configuration

**Solution** :
```bash
# Vérifier que toutes les lignes ont 5 colonnes
grep "^[0-9]" data/eternity2/eternity2.txt | awk '{print NF}' | sort -u

# Doit afficher seulement "5"
# Si vous voyez "4", il y a des lignes corrompues

# Trouver les lignes problématiques
grep "^[0-9]" data/eternity2/eternity2.txt | awk 'NF != 5 {print NR": "$0}'
```

---

## 📖 Configurations Disponibles

Les configurations sont dans `data/eternity2/` :

| Fichier | Description | Pièces | Status |
|---------|-------------|--------|--------|
| `eternity2.txt` | Configuration standard | 256 | ✅ |
| `eternity2_p01_ascending.txt` | Tri ascending | 256 | ✅ |
| `eternity2_from_edge_puzzle.txt` | Source edge_puzzle | 256 | ✅ |

**Ajouter une configuration** :
```bash
# Copier un fichier existant
cp data/eternity2/eternity2.txt data/eternity2/eternity2_custom.txt

# Éditer et modifier les métadonnées
# Le solver détectera automatiquement le nouveau fichier
```

---

## 🎯 Workflow Recommandé

### Pour le Développement :

```bash
# Terminal 1 : Serveur
mvn spring-boot:run

# Terminal 2 : Frontend
cd frontend && npm run dev

# IntelliJ :
# - Éditer le code
# - Lancer MainParallel quand prêt
# - Visualiser dans http://localhost:5173
```

### Pour la Résolution :

```bash
# Juste le solver (sans monitoring)
./run.sh

# OU avec monitoring complet
./start-all.sh
```

### Pour Reprendre Après Arrêt :

```bash
# Le solver reprend automatiquement
# Juste relancer :
mvn exec:java

# Il détectera les sauvegardes et reprendra
# Message : "RESUME (cumulative time: ...)"
```

---

## 📝 Résumé des Commandes

### Démarrage Complet (3 terminaux)

```bash
# Terminal 1
mvn spring-boot:run

# Terminal 2
cd frontend && npm run dev

# Terminal 3
mvn exec:java -Dexec.args="16 60"
```

### Démarrage Solver Seul

```bash
./run.sh
```

### Arrêt

```bash
Ctrl+C dans chaque terminal
```

---

## 🔗 URLs Importantes

| Service | URL | Description |
|---------|-----|-------------|
| Dashboard | http://localhost:5173 | Interface principale |
| API REST | http://localhost:8080/api | Endpoints REST |
| Swagger | http://localhost:8080/swagger-ui.html | Documentation API |
| H2 Console | http://localhost:8080/h2-console | Base de données |
| WebSocket | ws://localhost:8080/ws | Métriques temps réel |

---

## 🎊 Récapitulatif

**Pour lancer le système complet** :

1. **Serveur** : `mvn spring-boot:run` (port 8080)
2. **Frontend** : `cd frontend && npm run dev` (port 5173)
3. **Solver** : `mvn exec:java` ou `./run.sh`

**Ou simplement** : `./start-all.sh` (si vous créez le script)

**Dashboard** : http://localhost:5173

**Arrêter** : Ctrl+C partout

🎉 **Bonne résolution d'Eternity II !** 🎉
