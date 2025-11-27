# Eternity Solver - Developer Guide

**Version**: 1.0.0
**For**: Contributors and Developers
**Last Updated**: 2025-11-27

---

## Table of Contents

1. [Development Setup](#development-setup)
2. [Project Structure](#project-structure)
3. [Architecture Overview](#architecture-overview)
4. [Core Components](#core-components)
5. [Adding Features](#adding-features)
6. [Testing](#testing)
7. [Code Style](#code-style)
8. [Debugging](#debugging)
9. [Performance Optimization](#performance-optimization)
10. [Contributing](#contributing)

---

## Development Setup

### Prerequisites

- **JDK**: 11 or higher
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code
- **Build Tools**: Maven (optional, using direct javac)
- **Git**: For version control

### Clone Repository

```bash
git clone <repository-url>
cd eternity
```

### IDE Setup

#### IntelliJ IDEA (Recommended)

1. **Open Project**: File → Open → Select `eternity` folder
2. **Add Libraries**:
   - File → Project Structure → Libraries
   - Click **+** → Java
   - Select `lib/` folder
   - Click OK
3. **Set SDK**: Project Structure → Project → SDK → JDK 11+
4. **Mark Directories**:
   - Right-click `src` → Mark Directory as → Sources Root
   - Right-click `test` → Mark Directory as → Test Sources Root

#### Eclipse

1. Import as Java Project
2. Add `lib/*` to Build Path
3. Configure Source Folders: `src`, `test`

#### VS Code

1. Install Java Extension Pack
2. Configure `classpath` in `.vscode/settings.json`:
```json
{
    "java.project.referencedLibraries": [
        "lib/**/*.jar"
    ]
}
```

### Build and Run

```bash
# Compile
./compile.sh

# Run tests
./run_tests.sh

# Run solver
java -cp "bin:lib/*" MainCLI example_3x3
```

---

## Project Structure

```
eternity/
├── src/                    # Source code
│   ├── cli/               # Command-line interface
│   │   └── CommandLineInterface.java
│   ├── model/             # Domain models
│   │   ├── Board.java
│   │   ├── Cell.java
│   │   └── Piece.java
│   ├── runner/            # Orchestration
│   │   └── PuzzleRunner.java
│   ├── solver/            # Core solving logic
│   │   ├── EternitySolver.java        (Main solver)
│   │   ├── BoardVisualizer.java
│   │   ├── ParallelSearchManager.java
│   │   ├── optimization/
│   │   │   ├── NeighborAnalyzer.java
│   │   │   ├── SingletonDetector.java
│   │   │   └── EdgesAnalyzer.java
│   │   └── ...
│   ├── util/              # Utilities
│   │   ├── ShutdownManager.java
│   │   ├── SaveStateManager.java
│   │   ├── PuzzleFactory.java
│   │   └── ...
│   ├── MainCLI.java       # CLI entry point
│   └── main/resources/
│       └── logback.xml    # Logging config
├── test/                  # Tests
│   ├── cli/
│   ├── integration/
│   ├── solver/
│   ├── benchmark/
│   └── coverage/
├── lib/                   # Dependencies
│   ├── slf4j-api-2.0.9.jar
│   ├── logback-*.jar
│   └── junit-*.jar
├── docs/                  # Documentation
│   ├── ARCHITECTURE.md
│   ├── USER_MANUAL.md
│   └── this file
├── bin/                   # Compiled classes
├── logs/                  # Log files
├── compile.sh            # Build script
├── run_tests.sh          # Test script
└── pom.xml               # Maven dependencies
```

---

## Architecture Overview

### High-Level Flow

```
User Input → CLI → Runner → Solver → Result
              ↓      ↓        ↓
           Parsing Config  Algorithm
           Validation     + Optimizations
```

### Key Layers

1. **Presentation Layer** (`cli/`, `MainCLI.java`)
   - Command-line parsing
   - User interaction
   - Output formatting

2. **Orchestration Layer** (`runner/`)
   - Configuration management
   - Thread management
   - Shutdown handling

3. **Business Logic** (`solver/`)
   - Core algorithms
   - Optimizations
   - Constraint propagation

4. **Domain Model** (`model/`)
   - Board, Piece, Cell
   - Data structures

5. **Utilities** (`util/`)
   - Logging, saving, factories
   - Cross-cutting concerns

See `ARCHITECTURE.md` for detailed diagrams.

---

## Core Components

### 1. EternitySolver

**Location**: `src/solver/EternitySolver.java`

**Responsibility**: Core backtracking algorithm with optimizations

**Key Methods**:
```java
// Main entry point
public boolean solve(Board board, Map<Integer, Piece> pieces)

// Parallel solving
public boolean solveParallel(Board board, Map<Integer, Piece> allPieces,
                             Map<Integer, Piece> availablePieces, int numThreads)

// Core backtracking
public boolean solveBacktracking(Board board, Map<Integer, Piece> piecesById,
                                BitSet pieceUsed, int totalPieces)

// Constraint checking
public boolean fits(Board board, int r, int c, int[] candidateEdges)

// Cell selection (MRV heuristic)
public int[] findNextCellMRV(Board board, Map<Integer, Piece> piecesById,
                            BitSet pieceUsed, int totalPieces)
```

**Optimizations**:
- Singleton detection
- Arc consistency (AC-3)
- MRV heuristic
- Early pruning
- Neighbor analysis

### 2. PuzzleRunner

**Location**: `src/runner/PuzzleRunner.java`

**Responsibility**: Orchestrates puzzle solving with configuration

**Usage**:
```java
PuzzleRunnerConfig config = new PuzzleRunnerConfig()
    .setVerbose(true)
    .setParallel(true)
    .setThreads(8)
    .setTimeoutSeconds(3600);

PuzzleRunner runner = new PuzzleRunner(board, pieces, config);
PuzzleResult result = runner.run();

if (result.isSolved()) {
    System.out.println("Solved in " + result.getDurationSeconds() + "s");
}
```

### 3. CommandLineInterface

**Location**: `src/cli/CommandLineInterface.java`

**Responsibility**: Parse and validate CLI arguments

**Usage**:
```java
CommandLineInterface cli = new CommandLineInterface();

if (!cli.parse(args)) {
    System.err.println(cli.getErrorMessage());
    cli.printHelp();
    return;
}

boolean verbose = cli.isVerbose();
boolean parallel = cli.isParallel();
Integer threads = cli.getThreads();
```

### 4. ShutdownManager

**Location**: `src/util/ShutdownManager.java`

**Responsibility**: Graceful shutdown handling

**Usage**:
```java
// Register JVM hook
ShutdownManager.registerShutdownHook();

// Add custom hook
ShutdownManager.addShutdownHook(new ShutdownManager.ShutdownHook() {
    @Override
    public void onShutdown(String signal) {
        // Custom cleanup
    }

    @Override
    public String getName() {
        return "MyCustomHook";
    }
});
```

---

## Adding Features

### Adding a New Optimization

**Example**: Add "EdgeFrequencyAnalysis" optimization

1. **Create the class**:
```java
package solver.optimization;

public class EdgeFrequencyAnalyzer {
    private Map<Integer, Integer> edgeFrequency;

    public EdgeFrequencyAnalyzer(Map<Integer, Piece> pieces) {
        this.edgeFrequency = computeFrequencies(pieces);
    }

    private Map<Integer, Integer> computeFrequencies(Map<Integer, Piece> pieces) {
        Map<Integer, Integer> freq = new HashMap<>();
        // Compute edge frequencies
        return freq;
    }

    public int getRarity(int edgeValue) {
        return edgeFrequency.getOrDefault(edgeValue, 0);
    }
}
```

2. **Integrate into EternitySolver**:
```java
public class EternitySolver {
    private EdgeFrequencyAnalyzer edgeAnalyzer;

    public boolean solve(Board board, Map<Integer, Piece> pieces) {
        // Initialize analyzer
        if (useEdgeFrequency) {
            edgeAnalyzer = new EdgeFrequencyAnalyzer(pieces);
        }
        // Use in solving...
    }
}
```

3. **Add tests**:
```java
package solver.optimization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EdgeFrequencyAnalyzerTest {
    @Test
    public void testFrequencyCalculation() {
        // Test implementation
    }
}
```

4. **Add CLI option**:
```java
// In CommandLineInterface
case "--edge-frequency":
    options.put("edge-frequency", "true");
    break;
```

5. **Document**:
- Add to USER_MANUAL.md
- Update ARCHITECTURE.md
- Add Javadoc

### Adding a New CLI Option

1. **Define in CommandLineInterface**:
```java
private boolean myNewOption = false;

public boolean parse(String[] args) {
    // In switch statement
    case "--my-option":
        myNewOption = true;
        break;
}

public boolean isMyOptionEnabled() {
    return myNewOption;
}
```

2. **Use in MainCLI**:
```java
if (cli.isMyOptionEnabled()) {
    config.setMyOption(true);
}
```

3. **Add tests**:
```java
@Test
public void testMyOption() {
    String[] args = {"--my-option", "puzzle"};
    assertTrue(cli.parse(args));
    assertTrue(cli.isMyOptionEnabled());
}
```

### Adding a New Puzzle

1. **Create in PuzzleFactory**:
```java
public static Map<Integer, Piece> createMyPuzzle() {
    Map<Integer, Piece> pieces = new HashMap<>();

    pieces.put(1, new Piece(1, new int[]{0, 1, 2, 3}));
    pieces.put(2, new Piece(2, new int[]{1, 2, 3, 0}));
    // ... more pieces

    return pieces;
}
```

2. **Register in MainCLI**:
```java
case "my_puzzle":
    return new PuzzleInfo("My Puzzle 4×4", new Board(4, 4),
                         PuzzleFactory.createMyPuzzle());
```

3. **Test**:
```java
@Test
public void testMyPuzzle() {
    Map<Integer, Piece> pieces = PuzzleFactory.createMyPuzzle();
    assertEquals(16, pieces.size());
    // Verify pieces...
}
```

---

## Testing

### Running Tests

```bash
# All tests
./run_tests.sh

# Specific test class
java -jar lib/junit-platform-console-standalone-1.10.1.jar \
  --class-path "bin:lib/*" \
  --select-class solver.EternitySolverTest

# Integration tests only
java -jar lib/junit-platform-console-standalone-1.10.1.jar \
  --class-path "bin:lib/*" \
  --select-package integration
```

### Writing Tests

#### Unit Test Example

```java
package solver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class MyFeatureTest {

    private MyFeature feature;

    @BeforeEach
    public void setUp() {
        feature = new MyFeature();
    }

    @Test
    public void testBasicFunctionality() {
        int result = feature.compute(5);
        assertEquals(10, result);
    }

    @Test
    public void testEdgeCase() {
        assertThrows(IllegalArgumentException.class, () -> {
            feature.compute(-1);
        });
    }
}
```

#### Integration Test Example

```java
package integration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EndToEndTest {

    @Test
    public void testFullSolvingPipeline() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "java", "-cp", "bin:lib/*", "MainCLI", "-q", "example_3x3"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        int exitCode = process.waitFor();
        assertEquals(0, exitCode, "Should solve successfully");
    }
}
```

### Test Coverage

Check coverage with:
```bash
javac -d bin -sourcepath test:src -cp "bin:lib/*" \
  test/coverage/CodeCoverageReport.java

java -cp "bin:lib/*" coverage.CodeCoverageReport
```

Current coverage: **93% test/source ratio**

---

## Code Style

### Naming Conventions

```java
// Classes: PascalCase
public class EternitySolver { }

// Methods: camelCase
public boolean solve() { }

// Variables: camelCase
int pieceCount = 0;

// Constants: UPPER_SNAKE_CASE
public static final int MAX_DEPTH = 1000;

// Packages: lowercase
package solver.optimization;
```

### Documentation

```java
/**
 * Brief description of what the class/method does.
 *
 * More detailed explanation if needed, including:
 * - Algorithm description
 * - Complexity analysis
 * - Usage examples
 *
 * @param board the puzzle board
 * @param pieces available pieces
 * @return true if puzzle is solved
 */
public boolean solve(Board board, Map<Integer, Piece> pieces) {
    // Implementation
}
```

### Code Organization

```java
public class MyClass {
    // 1. Constants
    private static final int MAX_SIZE = 100;

    // 2. Static fields
    private static Logger logger = LoggerFactory.getLogger(MyClass.class);

    // 3. Instance fields
    private int size;
    private Board board;

    // 4. Constructors
    public MyClass() { }

    // 5. Public methods
    public void doSomething() { }

    // 6. Private methods
    private void helper() { }

    // 7. Inner classes
    private static class Helper { }
}
```

### Logging

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClass {
    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);

    public void method() {
        logger.debug("Debug message with {}", variable);
        logger.info("Info message");
        logger.warn("Warning message");
        logger.error("Error occurred", exception);
    }
}
```

**Levels**:
- `TRACE`: Very detailed debugging
- `DEBUG`: Debugging information
- `INFO`: General information
- `WARN`: Warning messages
- `ERROR`: Error messages

---

## Debugging

### Enable Verbose Logging

```bash
# Run with verbose mode
java -cp "bin:lib/*" MainCLI -v example_4x4

# Or edit logback.xml
<root level="DEBUG">
```

### Debug in IDE

#### IntelliJ IDEA

1. Set breakpoint (click line number gutter)
2. Run → Debug 'MainCLI'
3. Pass arguments: Run → Edit Configurations → Program arguments

#### Command-line Debugging

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 \
  -cp "bin:lib/*" MainCLI example_3x3
```

Then attach debugger from IDE.

### Common Issues

#### Issue: NullPointerException

**Debug**: Add null checks
```java
if (piece == null) {
    logger.error("Piece is null at position ({}, {})", row, col);
    throw new IllegalStateException("Piece cannot be null");
}
```

#### Issue: Infinite Loop

**Debug**: Add iteration counter
```java
int iterations = 0;
while (condition) {
    if (++iterations > MAX_ITERATIONS) {
        logger.error("Exceeded max iterations");
        break;
    }
    // ...
}
```

#### Issue: Performance Problem

**Tools**:
- JProfiler
- VisualVM
- Java Flight Recorder

**Quick profiling**:
```bash
java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder \
  -XX:StartFlightRecording=duration=60s,filename=recording.jfr \
  -cp "bin:lib/*" MainCLI puzzle_6x12
```

---

## Performance Optimization

### Profiling

```bash
# Run benchmark
java -cp "bin:lib/*" benchmark.PerformanceBenchmark
```

### Optimization Checklist

✅ **Algorithm**: Use right data structures (BitSet, HashMap)
✅ **Pruning**: Early termination (singletons, AC-3)
✅ **Caching**: Memoize expensive computations
✅ **Parallelization**: Use multiple threads where possible
✅ **I/O**: Minimize console output in tight loops
✅ **Memory**: Reuse objects, avoid unnecessary allocations

### Example: Optimize Method

**Before**:
```java
public List<Integer> findValidPieces(Board board, int row, int col) {
    List<Integer> valid = new ArrayList<>();
    for (int i = 1; i <= totalPieces; i++) {
        if (!pieceUsed[i] && fits(board, row, col, pieces.get(i))) {
            valid.add(i);
        }
    }
    return valid;
}
```

**After** (optimized):
```java
// Use BitSet instead of array
// Cache fits() results
// Return count instead of list when possible
public int countValidPieces(Board board, int row, int col, BitSet pieceUsed) {
    int count = 0;
    for (int i = pieceUsed.nextClearBit(0); i < totalPieces;
         i = pieceUsed.nextClearBit(i + 1)) {
        if (fitsCache.getOrDefault(makeKey(row, col, i), false)) {
            count++;
        }
    }
    return count;
}
```

---

## Contributing

### Workflow

1. **Fork** the repository
2. **Create branch**: `git checkout -b feature/my-feature`
3. **Make changes**
4. **Test**: Ensure all tests pass
5. **Commit**: `git commit -m "Add my feature"`
6. **Push**: `git push origin feature/my-feature`
7. **Pull Request**: Submit PR with description

### Code Review

Before submitting PR:

✅ All tests pass (`./run_tests.sh`)
✅ Code follows style guide
✅ New features have tests
✅ Documentation updated
✅ No compiler warnings
✅ Commit messages are clear

### Pull Request Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Performance improvement
- [ ] Documentation update

## Testing
- [ ] All existing tests pass
- [ ] New tests added
- [ ] Manual testing performed

## Checklist
- [ ] Code follows style guide
- [ ] Documentation updated
- [ ] No compiler warnings
```

See `CONTRIBUTING.md` for full guidelines.

---

## Resources

### Documentation
- `ARCHITECTURE.md` - System architecture
- `USER_MANUAL.md` - User guide
- `MAINTENANCE_REPORT.md` - Code health report

### External Resources
- [Java SE 11 Docs](https://docs.oracle.com/en/java/javase/11/)
- [SLF4J Manual](http://www.slf4j.org/manual.html)
- [JUnit 5 Guide](https://junit.org/junit5/docs/current/user-guide/)

### Tools
- [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- [Maven](https://maven.apache.org/)
- [Git](https://git-scm.com/)

---

**Happy Coding!** 🚀

For questions, see `CONTRIBUTING.md` or open an issue.
