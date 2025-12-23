# Eternity Solver - Architecture

## Vue d'ensemble

Le solveur Eternity est une application Java production-ready conçue pour résoudre des puzzles d'edge-matching complexes. L'architecture suit les principes SOLID et utilise des patterns modernes.

## Diagramme de l'Architecture Globale

```
┌─────────────────────────────────────────────────────────────────┐
│                        ETERNITY SOLVER                          │
│                     Production-Ready System                     │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│              │         │              │         │              │
│  User/Shell  │────────▶│   MainCLI    │────────▶│ PuzzleRunner │
│              │         │              │         │              │
└──────────────┘         └──────────────┘         └──────────────┘
                                │                        │
                                │                        │
                                ▼                        ▼
                         ┌──────────────┐         ┌──────────────┐
                         │ CommandLine  │         │   Eternity   │
                         │  Interface   │         │    Solver    │
                         └──────────────┘         └──────────────┘
                                                         │
                         ┌───────────────────────────────┤
                         │                               │
                         ▼                               ▼
                  ┌──────────────┐              ┌──────────────┐
                  │   Shutdown   │              │ Statistics   │
                  │   Manager    │              │   Manager    │
                  └──────────────┘              └──────────────┘
                         │
                         │
                         ▼
                  ┌──────────────┐
                  │  SLF4J +     │
                  │  Logback     │
                  └──────────────┘
```

## Flux d'Exécution Principal

```
USER INPUT
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│ 1. PARSING ARGUMENTS (CommandLineInterface)                  │
│    • Validation des options                                  │
│    • --help, --version, -v, -q, -p, -t, --timeout           │
└──────────────────────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│ 2. CONFIGURATION (PuzzleRunnerConfig)                        │
│    • Builder pattern                                         │
│    • Verbose, parallel, threads, timeout, singletons        │
└──────────────────────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│ 3. INITIALISATION (PuzzleRunner)                             │
│    • Création du solver                                      │
│    • Enregistrement shutdown hooks                           │
│    • Détermination nombre de threads                         │
└──────────────────────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│ 4. RÉSOLUTION (EternitySolver)                               │
│    • solve() ou solveParallel()                              │
│    • Backtracking avec optimisations                         │
│    • Singletons, AC-3, MRV heuristic                         │
└──────────────────────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│ 5. RÉSULTAT (PuzzleResult)                                   │
│    • solved (boolean)                                        │
│    • durationSeconds (double)                                │
│    • statistics (Statistics)                                 │
│    • board (Board)                                           │
└──────────────────────────────────────────────────────────────┘
    │
    ▼
OUTPUT TO USER
```

## Architecture en Couches

```
╔═════════════════════════════════════════════════════════════╗
║                    COUCHE PRÉSENTATION                      ║
║  • MainCLI.java                                             ║
║  • CommandLineInterface (cli/)                              ║
║  • BoardVisualizer (solver/visualizer/)                     ║
╚═════════════════════════════════════════════════════════════╝
                              │
                              ▼
╔═════════════════════════════════════════════════════════════╗
║                  COUCHE ORCHESTRATION                       ║
║  • PuzzleRunner (runner/)                                   ║
║  • PuzzleRunnerConfig (builder pattern)                     ║
║  • ShutdownManager (util/)                                  ║
╚═════════════════════════════════════════════════════════════╝
                              │
                              ▼
╔═════════════════════════════════════════════════════════════╗
║                   COUCHE MÉTIER (CORE)                      ║
║  • EternitySolver (solver/)                                 ║
║  • BacktrackingSolver (solver/backtracking/)                ║
║  • Optimizations (solver/optimization/)                     ║
║    - NeighborAnalyzer                                       ║
║    - EdgesAnalyzer                                          ║
║    - SingletonDetector                                      ║
║    - PlacementOrderTracker                                  ║
║  • ParallelSearchManager (solver/parallel/)                 ║
╚═════════════════════════════════════════════════════════════╝
                              │
                              ▼
╔═════════════════════════════════════════════════════════════╗
║                    COUCHE MODÈLE                            ║
║  • Board (model/)                                           ║
║  • Piece (model/)                                           ║
║  • Cell (model/)                                            ║
║  • PuzzleFactory (util/)                                    ║
╚═════════════════════════════════════════════════════════════╝
                              │
                              ▼
╔═════════════════════════════════════════════════════════════╗
║                  COUCHE UTILITAIRES                         ║
║  • SaveStateManager (util/)                                 ║
║  • StatisticsManager (util/)                                ║
║  • Logger (SLF4J + Logback)                                 ║
╚═════════════════════════════════════════════════════════════╝
```

## Patterns de Conception Utilisés

### 1. Builder Pattern
```
PuzzleRunnerConfig config = new PuzzleRunnerConfig()
    .setVerbose(true)
    .setParallel(true)
    .setThreads(8)
    .setTimeout(3600);
```

### 2. Strategy Pattern (Shutdown Hooks)
```
interface ShutdownHook {
    void onShutdown(String signal);
    String getName();
}

• SolverShutdownHook
• StatisticsShutdownHook
• CleanupShutdownHook
```

### 3. Singleton Pattern (ShutdownManager)
```
- Une seule instance du ShutdownManager
- Gestion centralisée des hooks JVM
- Thread-safe avec AtomicBoolean
```

### 4. Factory Pattern
```
PuzzleFactory:
  - createExample3x3()
  - createExample4x4()
  - createPuzzle6x12()
  - createEternityII()
```

### 5. Template Method (BacktrackingSolver)
```
solve() {
    initialize();
    while (!isSolved()) {
        selectPosition();
        selectPiece();
        placePiece();
        if (!isValid()) {
            backtrack();
        }
    }
}
```

## Modules Principaux

### Module CLI (cli/)
```
CommandLineInterface
├── parse(String[] args)
├── validate()
├── getters (isVerbose, isParallel, etc.)
└── help/version
```

**Responsabilités:**
- Parser les arguments de ligne de commande
- Valider les options
- Fournir aide et version

### Module Runner (runner/)
```
PuzzleRunner
├── PuzzleRunnerConfig (inner class)
├── PuzzleResult (inner class)
├── run()
├── configureSolver()
├── registerShutdownHooks()
└── determineThreadCount()
```

**Responsabilités:**
- Orchestrer la résolution
- Configurer le solver
- Gérer le timeout
- Gérer les threads

### Module Solver (solver/)
```
EternitySolver
├── solve(Board, Pieces)
├── solveParallel(Board, Pieces, threads)
├── placeNextPiece()
├── validatePlacement()
├── backtrack()
└── Statistics (inner class)
```

**Responsabilités:**
- Algorithme de résolution principal
- Backtracking avec optimisations
- Gestion des statistiques

### Module Util (util/)
```
ShutdownManager
├── registerShutdownHook()
├── addShutdownHook(hook)
├── requestShutdown()
└── ShutdownHook interface

SaveStateManager
├── saveState()
├── loadState()
└── checkpointState()

PlacementOrderTracker
├── recordPlacement()
├── getHistory()
└── initializeWithHistory()
```

**Responsabilités:**
- Shutdown gracieux
- Sauvegarde/chargement d'état
- Tracking des placements

## Gestion des Threads

### Mode Séquentiel
```
┌─────────────┐
│ Main Thread │
│             │
│  EternitySolver.solve()
│             │
└─────────────┘
```

### Mode Parallèle
```
┌─────────────┐
│ Main Thread │
│             │
└──────┬──────┘
       │
       ├─────────────────────────────────┐
       │                                 │
       ▼                                 ▼
┌──────────────┐                  ┌──────────────┐
│ Worker #1    │                  │ Worker #N    │
│              │                  │              │
│ Exploration  │  ...  ...  ...   │ Exploration  │
│ sous-arbre 1 │                  │ sous-arbre N │
└──────────────┘                  └──────────────┘
       │                                 │
       └─────────────────────────────────┘
                     │
                     ▼
              ┌──────────────┐
              │   Solution   │
              └──────────────┘
```

## Gestion du Shutdown Gracieux

```
USER: Ctrl+C (SIGINT/SIGTERM)
       │
       ▼
┌──────────────────────────────────────┐
│ JVM Runtime Hook                     │
│ (enregistré via                      │
│  Runtime.addShutdownHook())          │
└──────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ ShutdownManager.runShutdownHooks()   │
└──────────────────────────────────────┘
       │
       ├───────────────────┬───────────────────┐
       ▼                   ▼                   ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ SolverHook   │   │ StatsHook    │   │ CleanupHook  │
│              │   │              │   │              │
│ Interrupt    │   │ Print Stats  │   │ Close Files  │
│ Solver       │   │              │   │              │
└──────────────┘   └──────────────┘   └──────────────┘
```

## Optimisations du Solver

### 1. Singleton Detection
```
Avant chaque placement:
  Si une pièce ne peut aller qu'à une seule position
  OU qu'une position n'accepte qu'une seule pièce
  → Placer immédiatement (singleton)
  → Réduire l'espace de recherche
```

### 2. Arc Consistency (AC-3)
```
Après chaque placement:
  Propager les contraintes
  Réduire les domaines
  Détecter les dead-ends tôt
```

### 3. MRV Heuristic (Minimum Remaining Values)
```
Choisir la position avec le moins de pièces possibles
→ Fail-fast
→ Réduire le branching factor
```

### 4. Placement Order Tracking
```
PlacementOrderTracker:
  - Enregistre l'historique des placements
  - Permet la reprise depuis un checkpoint
  - Facilite le debugging
```

## Logging avec SLF4J

```
┌──────────────────┐
│ Code Application │
│                  │
│ logger.info()    │
│ logger.debug()   │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  SLF4J API       │  (Facade)
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Logback         │  (Implementation)
│                  │
│  - Console       │
│  - File (rotate) │
└──────────────────┘
```

**Configuration (logback.xml):**
- Console: INFO level
- File: DEBUG level
- Rotation: quotidienne
- Retention: 30 jours

## Tests

### Structure des Tests
```
test/
├── benchmark/          (Benchmarks de performance)
│   └── PerformanceBenchmark.java (7 benchmarks)
├── cli/                (Tests CLI)
│   └── CommandLineInterfaceTest.java (30 tests)
├── coverage/           (Rapport coverage)
│   └── CodeCoverageReport.java
├── integration/        (Tests end-to-end)
│   └── CLIIntegrationTest.java (12 tests)
├── model/              (Tests modèle)
│   ├── BoardTest.java
│   └── PieceTest.java
├── solver/             (Tests solver)
│   ├── EternitySolverTest.java
│   ├── optimization/
│   │   ├── NeighborAnalyzerTest.java
│   │   └── ...
│   └── ...
└── util/               (Tests utilitaires)
    └── ShutdownManagerTest.java (19 tests)
```

### Couverture
- **323 tests totaux** (100% succès)
- **Ratio test/source: 93%**
- Tous les modules ont des tests
- Tests end-to-end couvrent le CLI

## Diagramme de Séquence - Résolution Complète

```
User    MainCLI   CLI    Runner   Solver   Shutdown
 │         │       │       │        │         │
 │─args───▶│       │       │        │         │
 │         │──parse▶       │        │         │
 │         │◀──ok──│       │        │         │
 │         │                        │         │
 │         │─config────────▶│       │         │
 │         │                │──new─▶│         │
 │         │                │       │         │
 │         │                │──register──────▶│
 │         │                │◀──ok───────────│
 │         │                │       │         │
 │         │                │──run─▶│         │
 │         │                │       │         │
 │         │                │       │ (solving...)
 │         │                │       │         │
 │ Ctrl+C ┼────────────────┼───────┼────────▶│
 │         │                │       │◀interrupt
 │         │                │       │         │
 │         │                │       │ (cleanup)
 │         │                │◀result│         │
 │         │◀───result──────│       │         │
 │◀result─│                │       │         │
 │         │                │       │         │
```

## Points Clés de l'Architecture

✅ **Séparation des Préoccupations**
- CLI séparé de la logique métier
- Runner orchestre, Solver résout
- Utils réutilisables

✅ **Extensibilité**
- Nouveau shutdown hook: implémenter ShutdownHook
- Nouvelle optimisation: ajouter dans solver/optimization/
- Nouveau puzzle: ajouter dans PuzzleFactory

✅ **Testabilité**
- Chaque composant testé indépendamment
- Tests d'intégration end-to-end
- Mocks possibles grâce aux interfaces

✅ **Production-Ready**
- Logging professionnel (SLF4J)
- Shutdown gracieux (hooks JVM)
- CLI complet (help, version, options)
- Gestion d'erreurs robuste
- Documentation complète

✅ **Performance**
- Mode parallèle avec N threads
- Optimisations (singletons, AC-3, MRV)
- Benchmarks intégrés
- Timeout configurable

## Métriques du Projet

```
Code Source:        8,496 lignes (43 fichiers)
Code Test:          7,920 lignes (44 fichiers)
Total:             16,416 lignes

Tests:                323 tests (100% succès)
Couverture:           93% ratio test/source

Modules:              11 modules
Classes:              43 classes source
                      44 classes test

Performance:     < 30ms pour 3x3
                 Variable pour puzzles plus grands
```

---

**Architecture conçue pour:**
- Maintenabilité
- Extensibilité
- Testabilité
- Performance
- Production-readiness
