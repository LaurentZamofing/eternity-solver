# Parallelization Strategy for Eternity II

## 📋 Overview

This document describes the strategy implemented to enable parallel execution of multiple Eternity II solver instances without conflicts.

## 🎯 Objective

Allow multiple threads/processes to work simultaneously on solving Eternity II by exploring different branches of the search tree in a complementary manner.

## 🔑 Key Principles

### 1. Fixed Corner Pieces (4!)

Eternity II has **4 corner pieces** (pieces 1, 2, 3, 4) that must be placed in the 4 corners of the puzzle.

- There are **4! = 24 possible permutations** to place these corner pieces
- Each permutation defines a different starting point for the search
- These permutations are **mutually exclusive**: no conflict is possible

**Note**: In addition to the 4 corners, Eternity II has **5 additional fixed pieces** that are always placed at the same positions:
- Piece 139 at (8, 7) - central position
- Piece 181 at (13, 2)
- Piece 255 at (2, 13)
- Piece 249 at (13, 13)
- Piece 208 at (2, 2)

In total: **9 fixed pieces** per configuration (4 corners + 5 fixed)

### 2. Piece Sorting Order (×2)

For each corner permutation, we can explore the search tree in two different orders:

- **Ascending**: pieces are tried in increasing order (5, 6, 7, ..., 256)
- **Descending**: pieces are tried in decreasing order (256, ..., 7, 6, 5)

This doubles the number of explorable branches: **24 × 2 = 48 unique configurations**

### 3. Intelligent Save File Resumption

Each solver instance:
1. Looks for the **oldest** `current` save file
2. Resumes work from that point
3. Creates a new save with **timestamp** during next save
4. Automatically cleans up old save files

**Benefit**: Threads naturally distribute themselves over the oldest (thus least advanced) work, without explicit coordination.

## 📁 Configuration Files

### Naming Structure

```
puzzle_eternity2_pXX_C1_C2_C3_C4_ORDER.txt
```

Where:
- `XX`: permutation number (01-24)
- `C1`, `C2`, `C3`, `C4`: Corner piece IDs for each position
- `ORDER`: `ascending` or `descending`

### Examples

```
puzzle_eternity2_p01_1_2_3_4_ascending.txt   # Permutation 1, ascending order
puzzle_eternity2_p01_1_2_3_4_descending.txt  # Permutation 1, descending order
puzzle_eternity2_p12_2_4_3_1_ascending.txt   # Permutation 12, ascending order
puzzle_eternity2_p24_4_3_2_1_descending.txt  # Permutation 24, descending order
```

### Corner Positions

- **Position 1**: Top-left corner (0, 0) - rotation 0
- **Position 2**: Top-right corner (0, 15) - rotation 1
- **Position 3**: Bottom-left corner (15, 0) - rotation 3
- **Position 4**: Bottom-right corner (15, 15) - rotation 2

### Configuration Metadata

Each file contains:

```
# SortOrder: ascending (or descending)
# PieceFixePosition: <pieceId> <row> <col> <rotation>
```

## 💾 Save System

### Subdirectory Organization

Saves are organized in **subdirectories** by puzzle type:

```
saves/
├── eternity2/
│   ├── eternity2_p01_ascending_current_1731868234567.txt
│   ├── eternity2_p01_descending_current_1731868234567.txt
│   ├── eternity2_best_176.txt
│   └── ...
├── indice1/
│   └── ...
└── indice2/
    └── ...
```

### File Names

`current` saves include the **permutation number, sort order, and timestamp**:

**Format**: `eternity2_pXX_ORDER_current_TIMESTAMP.txt`

The new format allows to:
- **Easily identify** which config corresponds to which save
- Know the age of the work (via timestamp)
- Avoid conflicts between threads (each config has its own space)
- Load the oldest save with priority

### Visual Display

Each save file contains an **ASCII display** of the board at the top:

```
# ═══════════════════════════════════════════════════════════
# VISUAL BOARD DISPLAY (176 pieces placed)
# ═══════════════════════════════════════════════════════════
#
#   10   21   32   43  ...
#   54   65   76   87  ...
#   .    .    .    .   ...
#
# ═══════════════════════════════════════════════════════════
```

**Format**: `pieceId` + `rotation` (e.g., "176" = piece 17, rotation 6)

This allows quick visualization of a save's state without opening an editor.

### Loading Strategy

1. **Search**: List all `eternity2_current_*.txt` files
2. **Sort**: Sort by timestamp (ascending order)
3. **Selection**: Load file with smallest timestamp (= oldest)
4. **Cleanup**: Delete old saves after creating a new one

### Auto-save

- **Main save**: Every 10 minutes
- **Thread save**: Every 5 minutes (for parallel threads)
- **Best save**: Upon each new record (every 5 levels)

## 🚀 Practical Usage

### ⭐ Recommended Method: MainParallel (intelligent multi-threading)

**Automatic usage** (detects number of CPUs):
```bash
./run_parallel.sh
```

**Usage with specific number of threads**:
```bash
./run_parallel.sh 8
```

Or directly in Java:
```bash
java -cp bin MainParallel 8
```

**Advantages of MainParallel:**
- ✅ Automatic configuration priority management
- ✅ Launches never-started configs first
- ✅ Then resumes oldest saves
- ✅ Optimized thread pool
- ✅ No need to manually manage configs

### Manual Method: Launching Multiple Separate Instances

If you prefer to manually control each instance in separate terminals:

**Terminal 1:**
```bash
java -cp bin MainSequential data/puzzle_eternity2_p01_1_2_3_4_ascending.txt
```

**Terminal 2:**
```bash
java -cp bin MainSequential data/puzzle_eternity2_p01_1_2_3_4_descending.txt
```

**Terminal 3:**
```bash
java -cp bin MainSequential data/puzzle_eternity2_p02_1_2_4_3_ascending.txt
```

... and so on for all 48 configurations.

### Launching a Single Configuration

To test or debug a specific configuration:
```bash
java -cp bin MainSequential data/puzzle_eternity2_p01_1_2_3_4_ascending.txt
```

## 🎯 Intelligent Prioritization System (MainParallel)

MainParallel implements an automatic prioritization system to optimize exploration:

### Selection Algorithm

1. **Priority 1: Never-started Configurations**
   - Configs without `current` save are launched first
   - Guarantees no branch is left behind

2. **Priority 2: Oldest Saves**
   - Among ongoing configs, oldest ones are resumed
   - Uses timestamp in filename `current_TIMESTAMP.txt`
   - Prevents any config from being "abandoned" too long

3. **Alphabetical Sorting for Never-started**
   - Deterministic order among never-launched configs

### Example Output

```
╔═══════════════════════════════════════════════════════════════════╗
║              CONFIGURATION STATISTICS                             ║
╚═══════════════════════════════════════════════════════════════════╝

  📊 Total configurations : 48
  🆕 Never started        : 12
  🔄 In progress          : 36

📋 Priority order:
   1. Never-started configurations
   2. Oldest saves

🚀 [Thread 1] Starting: Permutation 1 - ASCENDING
   Status: NEW

🚀 [Thread 2] Starting: Permutation 12 - DESCENDING
   Status: RESUMING (save from Mon Nov 17 19:45:37 CET 2025)
```

### Behavior

- **At startup**: Analyzes all 48 configurations
- **Displays stats**: Number of new vs ongoing configs
- **Launches N threads**: N = specified number (or nb of CPUs)
- **Each thread**: Works indefinitely on assigned config
- **Auto-saves**: Every 10 minutes, with new timestamp

## 📊 Monitoring

Each instance displays:

```
→ Sort order: ascending
→ Resuming from: 176 pieces (ALL pieces can be backtracked)
→ Backtracking can go through ALL 176 pre-loaded pieces
```

Best scores are shared via `best_XXX.txt` saves which are accessible by all instances.

## 🔧 Technical Modifications

### Modified Files

1. **PuzzleConfig.java**
   - Added `sortOrder` field
   - Parsing of `SortOrder:` in configuration files

2. **SaveStateManager.java**
   - Added timestamp in `current` filenames
   - Method `findCurrentSave()` returns oldest save
   - Automatic cleanup of old `current` saves

3. **EternitySolver.java**
   - Added `sortOrder` field
   - Method `setSortOrder()`
   - Sorting of `unused` list according to configured order in `solve()`

4. **MainSequential.java**
   - Method `sortPiecesByOrder()` to sort pieces
   - Call to `setSortOrder()` on solver
   - Display of sort order in logs

### Generation Script

**scripts/generate_eternity2_configs.py**
- Automatically generates all 48 configuration files
- Calculates corner permutations
- Adds correct rotations for each position
- Includes fixed central piece (139)

## ✅ Validation

The test `TestNewConfigFormat.java` verifies:
- Correct loading of all 48 configurations
- Parsing of `sortOrder` field
- Fixed pieces at correct positions
- Correct rotations for each corner

```bash
javac -d bin -sourcepath src:test test/TestNewConfigFormat.java
java -cp bin TestNewConfigFormat
```

## 🎯 Expected Results

With this strategy:
- ✅ No conflicts between threads
- ✅ Complementary exploration of search space
- ✅ Automatic work distribution on oldest branches
- ✅ Sharing of best scores via `best` saves
- ✅ Optimal use of multi-core CPUs

## 📅 Implementation Date

2025-11-17
