# Eternity Solver - User Manual

**Version**: 1.0.0
**Date**: 2025-11-27
**Status**: Production Ready

---

## Table of Contents

1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Quick Start](#quick-start)
4. [Command Line Interface](#command-line-interface)
5. [Examples](#examples)
6. [Configuration](#configuration)
7. [Troubleshooting](#troubleshooting)
8. [Advanced Usage](#advanced-usage)
9. [FAQ](#faq)

---

## Introduction

### What is Eternity Solver?

Eternity Solver is a high-performance puzzle solver for edge-matching puzzles, inspired by the Eternity II puzzle. It uses advanced algorithms including:

- **Backtracking** with constraint propagation
- **Arc Consistency** (AC-3) for early pruning
- **Singleton Detection** for forced placements
- **MRV Heuristic** (Minimum Remaining Values)
- **Parallel Search** with multiple threads

### Key Features

✅ **Production-Ready**: Professional CLI, logging, error handling
✅ **High Performance**: Optimized backtracking with multiple strategies
✅ **Flexible**: Supports various puzzle sizes and configurations
✅ **Well-Tested**: 323 tests with 100% success rate
✅ **Documented**: Complete architecture and API documentation
✅ **Graceful Shutdown**: Ctrl+C shows statistics and saves state

---

## Installation

### Prerequisites

- **Java**: JDK 11 or higher
- **Operating System**: Windows, macOS, or Linux

### Verify Java Installation

```bash
java -version
```

Expected output:
```
java version "11.0.x" or higher
```

### Download

1. Clone or download the repository
2. Navigate to the project directory

```bash
cd /path/to/eternity
```

### Compile

#### Option 1: Use the compile script (Recommended)

```bash
./compile.sh
```

#### Option 2: Manual compilation

```bash
mkdir -p bin
javac -d bin -sourcepath src -cp "lib/*" $(find src -name "*.java")
cp -r src/main/resources/* bin/ 2>/dev/null || true
```

### Verify Installation

```bash
java -cp "bin:lib/*" MainCLI --version
```

Expected output:
```
Eternity Puzzle Solver v1.0.0
Solveur de puzzle d'edge-matching avec backtracking optimisé
```

---

## Quick Start

### Solve a 3x3 Puzzle

```bash
java -cp "bin:lib/*" MainCLI example_3x3
```

### View Help

```bash
java -cp "bin:lib/*" MainCLI --help
```

### Solve with Verbose Output

```bash
java -cp "bin:lib/*" MainCLI -v example_4x4
```

---

## Command Line Interface

### Basic Syntax

```bash
java -cp "bin:lib/*" MainCLI [OPTIONS] [PUZZLE_NAME]
```

### Options

#### Help and Information

```bash
-h, --help              Display help message
--version               Display version information
```

#### Output Control

```bash
-v, --verbose           Enable verbose output (show progress)
-q, --quiet             Quiet mode (errors only, minimal output)
```

#### Solving Options

```bash
-p, --parallel          Enable parallel search (multi-threaded)
-t, --threads <N>       Number of threads (default: auto-detect)
--timeout <SECONDS>     Maximum solving time in seconds
--min-depth <N>         Minimum depth to show progress records
--no-singletons         Disable singleton optimization
```

#### Puzzle Selection

```bash
--puzzle <NAME>         Specify puzzle name (or use positional argument)
```

### Examples

```bash
# Get help
java -cp "bin:lib/*" MainCLI --help

# Check version
java -cp "bin:lib/*" MainCLI --version

# Solve simple puzzle
java -cp "bin:lib/*" MainCLI example_3x3

# Verbose output
java -cp "bin:lib/*" MainCLI -v example_4x4

# Parallel with 8 threads
java -cp "bin:lib/*" MainCLI -p -t 8 puzzle_6x12

# With timeout (1 hour)
java -cp "bin:lib/*" MainCLI --timeout 3600 puzzle_16x16

# Quiet mode
java -cp "bin:lib/*" MainCLI -q example_5x5

# Combined options
java -cp "bin:lib/*" MainCLI -v -p -t 4 --timeout 600 puzzle_6x12
```

---

## Examples

### Example 1: Simple 3x3 Puzzle

```bash
java -cp "bin:lib/*" MainCLI example_3x3
```

**Output**:
```
╔═══════════════════════════════════════════════════════╗
║          ETERNITY PUZZLE SOLVER                       ║
╚═══════════════════════════════════════════════════════╝

Puzzle: Exemple 3×3
Taille: 3×3 (9 pièces)

═══════════════════════════════════════════════════════
RÉSULTAT
═══════════════════════════════════════════════════════
✓ PUZZLE RÉSOLU!
Temps: 0.03 secondes
```

### Example 2: Verbose Mode

```bash
java -cp "bin:lib/*" MainCLI -v example_4x4
```

**Output shows**:
- Edge compatibility analysis
- Singleton detections
- Placement attempts
- Backtracking steps
- Progress records

### Example 3: Parallel Solving

```bash
java -cp "bin:lib/*" MainCLI -p -t 8 puzzle_6x12
```

**Benefits**:
- Faster solving on multi-core CPUs
- Explores multiple branches simultaneously
- Auto-detects optimal thread count if -t omitted

### Example 4: With Timeout

```bash
java -cp "bin:lib/*" MainCLI --timeout 300 puzzle_16x16
```

**Behavior**:
- Runs for maximum 300 seconds (5 minutes)
- Automatically stops after timeout
- Displays statistics and partial progress

### Example 5: Quiet Mode

```bash
java -cp "bin:lib/*" MainCLI -q example_5x5
```

**Output**:
- No fancy headers
- Only result and time
- Useful for scripting

---

## Configuration

### Available Puzzles

#### Built-in Examples

| Puzzle Name | Size | Pieces | Difficulty | Typical Time |
|-------------|------|--------|------------|--------------|
| example_3x3 | 3×3  | 9      | Easy       | < 1 second   |
| example_4x4 | 4×4  | 16     | Medium     | 1-10 seconds |
| example_4x4_easy | 4×4 | 16  | Easy       | < 1 second   |
| example_5x5 | 5×5  | 25     | Hard       | 10-60 seconds|
| validation_6x6 | 6×6 | 36   | Medium     | Varies       |
| puzzle_6x12 | 6×12 | 72     | Hard       | Minutes-Hours|
| puzzle_16x16 | 16×16 | 256  | Very Hard  | Hours-Days   |

#### Aliases

Most puzzles support short names:
```bash
example_3x3  = 3x3
example_4x4  = 4x4
puzzle_6x12  = 6x12
validation_6x6 = 6x6
```

### Logging Configuration

Logs are written to `logs/eternity-solver.log` with daily rotation.

**Configuration file**: `src/main/resources/logback.xml`

```xml
<root level="INFO">
    <appender-ref ref="CONSOLE" />
    <appender-ref ref="FILE" />
</root>
```

**Log levels**: TRACE, DEBUG, INFO, WARN, ERROR

**Rotation**: Daily, keeps 30 days

### Performance Tuning

#### Thread Count

```bash
# Auto-detect (recommended)
java -cp "bin:lib/*" MainCLI -p puzzle_6x12

# Specific number
java -cp "bin:lib/*" MainCLI -p -t 4 puzzle_6x12

# All cores
java -cp "bin:lib/*" MainCLI -p -t $(nproc) puzzle_6x12
```

**Recommendation**:
- Small puzzles (≤ 4x4): 1-2 threads
- Medium puzzles (5x5 - 6x6): 4 threads
- Large puzzles (≥ 6x12): 8+ threads

#### Memory Settings

For large puzzles, increase JVM heap:

```bash
java -Xmx4g -cp "bin:lib/*" MainCLI puzzle_16x16
```

---

## Troubleshooting

### Problem: "Cannot find or load main class MainCLI"

**Cause**: Classpath not set correctly

**Solution**:
```bash
# Make sure you're in the project directory
cd /path/to/eternity

# Compile first
./compile.sh

# Use correct classpath separator
# macOS/Linux: use colon (:)
java -cp "bin:lib/*" MainCLI --help

# Windows: use semicolon (;)
java -cp "bin;lib/*" MainCLI --help
```

### Problem: "package org.slf4j does not exist" in IDE

**Cause**: IDE hasn't loaded the lib/ folder

**Solution**:
1. Open IntelliJ IDEA
2. File → Project Structure → Libraries
3. Click **+** → **Java**
4. Select the `lib/` folder
5. Click **OK**

See `FIX_IDE_COMPILATION.md` for detailed instructions.

### Problem: Puzzle Takes Too Long

**Solutions**:

1. **Use timeout**:
```bash
java -cp "bin:lib/*" MainCLI --timeout 600 puzzle_6x12
```

2. **Enable parallel**:
```bash
java -cp "bin:lib/*" MainCLI -p -t 8 puzzle_6x12
```

3. **Use quiet mode** (reduces I/O overhead):
```bash
java -cp "bin:lib/*" MainCLI -q puzzle_6x12
```

4. **Try smaller puzzle first**:
```bash
java -cp "bin:lib/*" MainCLI example_5x5
```

### Problem: Out of Memory Error

**Solution**: Increase heap size

```bash
java -Xmx8g -cp "bin:lib/*" MainCLI puzzle_16x16
```

### Problem: Solver Hangs or Freezes

**Solution**: Use Ctrl+C for graceful shutdown

```
Press Ctrl+C

═══════════════════════════════════════════════════════
⚠️  INTERRUPTION DÉTECTÉE
═══════════════════════════════════════════════════════

═══════════════════════════════════════════════════════
STATISTIQUES FINALES
═══════════════════════════════════════════════════════
... (statistics displayed)
```

---

## Advanced Usage

### Running Tests

```bash
# Run all tests
./run_tests.sh

# Run specific test class
java -jar lib/junit-platform-console-standalone-1.10.1.jar \
  --class-path "bin:lib/*" \
  --select-class solver.EternitySolverTest
```

### Performance Benchmarks

```bash
javac -d bin -sourcepath test:src -cp "bin:lib/*" \
  test/benchmark/PerformanceBenchmark.java

java -cp "bin:lib/*" benchmark.PerformanceBenchmark
```

### Code Coverage Report

```bash
javac -d bin -sourcepath test:src -cp "bin:lib/*" \
  test/coverage/CodeCoverageReport.java

java -cp "bin:lib/*" coverage.CodeCoverageReport
```

### Custom Puzzles

To add custom puzzles, modify `src/util/PuzzleFactory.java`:

```java
public static Map<Integer, Piece> createMyPuzzle() {
    Map<Integer, Piece> pieces = new HashMap<>();

    // Add pieces
    pieces.put(1, new Piece(1, new int[]{0, 1, 2, 3}));
    // ... more pieces

    return pieces;
}
```

Then add to `MainCLI.java`:

```java
case "my_puzzle":
    return new PuzzleInfo("My Puzzle", new Board(4, 4),
                         PuzzleFactory.createMyPuzzle());
```

---

## FAQ

### Q: How fast is the solver?

**A**: Depends on puzzle complexity:
- 3×3: < 1 second
- 4×4: 1-10 seconds
- 5×5: 10-60 seconds
- 6×12: Minutes to hours
- 16×16: Hours to days (if solvable)

### Q: Can it solve Eternity II?

**A**: The original Eternity II (16×16 with 256 pieces) is computationally infeasible to solve exhaustively. However, the solver can:
- Explore partial solutions
- Find solutions to smaller regions
- Verify proposed solutions

### Q: Does it support custom constraints?

**A**: Yes, modify `EternitySolver.java` to add:
- Fixed piece positions
- Forbidden positions
- Additional constraints

### Q: Can I interrupt the solver?

**A**: Yes! Press **Ctrl+C** for graceful shutdown:
- Shows statistics
- Saves progress
- Displays best partial solution

### Q: Is parallel mode always faster?

**A**: Not always:
- Small puzzles: Sequential may be faster (less overhead)
- Medium/Large puzzles: Parallel usually faster
- Overhead: Thread creation and synchronization
- Recommendation: Try both!

### Q: How much memory does it use?

**A**: Typical usage:
- 3×3: ~50 MB
- 4×4: ~100 MB
- 6×12: ~500 MB - 1 GB
- 16×16: 2-8 GB (depends on search depth)

### Q: What output formats are supported?

**A**: Currently:
- Console (ASCII art)
- Logs (text files)
- Statistics (structured output)

Coming soon:
- JSON export
- CSV export
- Graphical visualization

### Q: Can I use it in my project?

**A**: Check the project license. For integration:
```java
import solver.EternitySolver;
import model.Board;
import model.Piece;

EternitySolver solver = new EternitySolver();
boolean solved = solver.solve(board, pieces);
```

### Q: How do I report bugs?

**A**: See `CONTRIBUTING.md` for bug report guidelines.

---

## Support

### Getting Help

1. **Documentation**: Start with this manual
2. **Architecture**: See `ARCHITECTURE.md`
3. **Troubleshooting**: See `FIX_IDE_COMPILATION.md`
4. **Quick Start**: See `QUICK_START.md`

### Community

- **Issues**: Report bugs or feature requests
- **Contributions**: See `CONTRIBUTING.md`
- **Updates**: Check release notes

---

## Appendix

### Exit Codes

| Code | Meaning |
|------|---------|
| 0    | Puzzle solved successfully |
| 1    | No solution found OR error occurred |

### Environment Variables

```bash
# Increase logging verbosity
export LOGBACK_LEVEL=DEBUG

# Custom log location
export LOG_PATH=/custom/path/logs
```

### Keyboard Shortcuts

| Key   | Action |
|-------|--------|
| Ctrl+C | Graceful shutdown (shows statistics) |
| Ctrl+Z | Suspend process (not recommended) |

---

**Last Updated**: 2025-11-27
**Version**: 1.0.0
**Author**: Eternity Solver Team
