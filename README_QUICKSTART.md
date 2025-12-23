# ⚡ Eternity II Solver - Quick Start

## 🚀 Lancement Ultra-Rapide

### Option 1 : Système Complet (Recommandé)

```bash
./start-all.sh
```

Puis ouvrir : **http://localhost:5173**

---

### Option 2 : Juste le Solver (Sans Monitoring)

```bash
./run.sh
```

---

### Option 3 : Depuis IntelliJ

#### Solver Seul :
```
1. Ouvrir: src/main/java/app/MainParallel.java
2. Clic droit sur "public static void main"
3. Run 'MainParallel.main()'
```

#### Avec Dashboard :
```
Terminal 1: mvn spring-boot:run
Terminal 2: cd frontend && npm run dev
Terminal 3: mvn exec:java
```

**Dashboard** : http://localhost:5173

---

## 🛑 Arrêt

```bash
./stop-all.sh
```

Ou `Ctrl+C` dans chaque terminal.

---

## 📖 Documentation Complète

Voir **[GUIDE_LANCEMENT.md](GUIDE_LANCEMENT.md)** pour :
- Configurations détaillées
- Paramètres avancés
- Troubleshooting
- API documentation
- Monitoring

---

## 🎯 Configurations Disponibles

| Config | Pièces | Description |
|--------|--------|-------------|
| eternity2.txt | 256 | Standard |
| eternity2_p01_ascending.txt | 256 | Tri ascending |
| eternity2_from_edge_puzzle.txt | 256 | Source edge_puzzle |

---

## 📊 Paramètres du Solver

```bash
# Syntaxe
mvn exec:java -Dexec.args="[THREADS] [MINUTES]"

# Exemples
mvn exec:java -Dexec.args="8 30"   # 8 threads, 30 min
mvn exec:java -Dexec.args="16 60"  # 16 threads, 1 heure
mvn exec:java -Dexec.args="32 1440" # 32 threads, 24 heures
```

---

## 🎊 C'est Tout !

**3 manières de lancer** :
1. `./start-all.sh` → Tout en un
2. `./run.sh` → Juste le solver
3. IntelliJ → Run 'MainParallel.main()'

**Dashboard** : http://localhost:5173

**Arrêt** : `./stop-all.sh` ou Ctrl+C

🎉 **Bonne recherche !**
