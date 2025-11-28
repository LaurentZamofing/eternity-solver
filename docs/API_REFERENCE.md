# API Reference

This document describes how to use the Eternity Puzzle Solver as a Java library in your own projects.

## Table of Contents

- [Overview](#overview)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Core Classes](#core-classes)
- [Usage Examples](#usage-examples)
- [Configuration](#configuration)
- [Custom Puzzles](#custom-puzzles)
- [Advanced Features](#advanced-features)
- [Error Handling](#error-handling)
- [Best Practices](#best-practices)

## Overview

The Eternity Puzzle Solver can be used as a library to:
- Solve edge-matching puzzles programmatically
- Create custom puzzle generators
- Integrate puzzle solving into larger applications
- Build puzzle analysis tools
- Develop new heuristics and optimizations

### Package Structure

```
src/main/java/
├── cli/              # Command-line interface (optional)
├── model/            # Core domain models (Board, Piece, Placement)
├── runner/           # Orchestration layer (PuzzleRunner)
├── solver/           # Core solving algorithms
│   ├── heuristics/   # Heuristic strategies
│   └── ...          # Optimization components
└── util/             # Utilities (file I/O, logging)
```

## Installation

### Maven Dependency

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.eternity</groupId>
    <artifactId>eternity-solver</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle Dependency

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'com.eternity:eternity-solver:1.0.0'
}
```

### Manual JAR

Download the JAR and add to classpath:

```bash
java -cp "eternity-solver-1.0.0.jar:your-app.jar" YourMainClass
```

## Quick Start

### Minimal Example

```java
import model.Board;
import model.Piece;
import solver.EternitySolver;
import java.util.Map;

public class SolverExample {
    public static void main(String[] args) {
        // Create board and pieces
        Board board = new Board(3, 3);
        Map<Integer, Piece> pieces = loadPuzzle("example_3x3");

        // Create solver
        EternitySolver solver = new EternitySolver(board);

        // Solve
        boolean solved = solver.solve(pieces);

        if (solved) {
            System.out.println("Solution found!");
            board.display();
        } else {
            System.out.println("No solution exists.");
        }
    }

    private static Map<Integer, Piece> loadPuzzle(String name) {
        // Use PuzzleLoader utility
        return util.PuzzleLoader.load("data/" + name + ".txt");
    }
}
```

### With Configuration

```java
import solver.EternitySolver;
import solver.SolverConfiguration;

public class ConfiguredSolverExample {
    public static void main(String[] args) {
        Board board = new Board(4, 4);
        Map<Integer, Piece> pieces = loadPuzzle("example_4x4");

        // Configure solver
        SolverConfiguration config = new SolverConfiguration();
        config.setUseSingletons(true);
        config.setUseMRV(true);
        config.setUseSymmetryBreaking(true);
        config.setVerboseLevel(1);

        // Create solver with config
        EternitySolver solver = new EternitySolver(board, config);

        // Solve
        boolean solved = solver.solve(pieces);
    }
}
```

## Core Classes

### Board

Represents the puzzle board.

#### Constructor

```java
// Create empty board
Board board = new Board(int width, int height);
```

#### Key Methods

```java
// Place a piece
void placePiece(int x, int y, Piece piece);

// Remove a piece
void removePiece(int x, int y);

// Get piece at position
Piece getPiece(int x, int y);

// Check if position is empty
boolean isEmpty(int x, int y);

// Check if board is complete
boolean isComplete();

// Clone the board
Board clone();

// Display board (ASCII art)
void display();
```

#### Example

```java
Board board = new Board(3, 3);

// Place piece at (0, 0)
Piece piece = new Piece(1, 0, 1, 2, 3);
board.placePiece(0, 0, piece);

// Check position
if (board.isEmpty(1, 1)) {
    System.out.println("Position (1,1) is empty");
}

// Display board
board.display();
```

### Piece

Represents a puzzle piece with 4 colored edges.

#### Constructor

```java
// Create piece: (id, north, east, south, west)
Piece piece = new Piece(int id, int north, int east, int south, int west);
```

#### Key Methods

```java
// Get edge values
int getNorth();
int getEast();
int getSouth();
int getWest();

// Get piece ID
int getId();

// Rotate piece (returns new Piece)
Piece rotate(int rotation);  // 0, 1, 2, or 3 (0°, 90°, 180°, 270°)

// Check if piece can be placed at border
boolean canBePlacedAtBorder(boolean north, boolean east, boolean south, boolean west);

// Get edge by direction
int getEdge(Direction direction);
```

#### Example

```java
// Create piece with ID 1: north=0, east=1, south=2, west=3
Piece piece = new Piece(1, 0, 1, 2, 3);

// Rotate 90° clockwise
Piece rotated = piece.rotate(1);
// rotated: north=3, east=0, south=1, west=2

// Check edges
System.out.println("North edge: " + piece.getNorth());  // 0
System.out.println("East edge: " + piece.getEast());    // 1
```

### Placement

Represents a piece placement on the board.

#### Constructor

```java
Placement placement = new Placement(Piece piece, int x, int y);
```

#### Key Methods

```java
Piece getPiece();
int getX();
int getY();
```

### EternitySolver

Main solver class implementing the solving algorithm.

#### Constructor

```java
// With default configuration
EternitySolver solver = new EternitySolver(Board board);

// With custom configuration
EternitySolver solver = new EternitySolver(Board board, SolverConfiguration config);
```

#### Key Methods

```java
// Solve puzzle
boolean solve(Map<Integer, Piece> pieces);

// Get statistics
SolverStatistics getStatistics();

// Get current solution
Board getSolution();

// Reset solver
void reset();
```

#### Example

```java
Board board = new Board(4, 4);
Map<Integer, Piece> pieces = loadPuzzle("example_4x4");

EternitySolver solver = new EternitySolver(board);
boolean solved = solver.solve(pieces);

if (solved) {
    SolverStatistics stats = solver.getStatistics();
    System.out.println("Calls: " + stats.getTotalCalls());
    System.out.println("Backtracks: " + stats.getBacktrackCount());
    System.out.println("Time: " + stats.getSolveTime() + "ms");
}
```

### SolverConfiguration

Configuration options for the solver.

#### Constructor

```java
SolverConfiguration config = new SolverConfiguration();
```

#### Configuration Options

```java
// Enable/disable optimizations
config.setUseSingletons(boolean enable);          // Default: true
config.setUseMRV(boolean enable);                 // Default: true
config.setUseSymmetryBreaking(boolean enable);    // Default: true
config.setUseAC3(boolean enable);                 // Default: true

// Heuristic options
config.setPrioritizeBorders(boolean enable);      // Default: false
config.setPieceOrder(PieceOrder order);           // ASCENDING, DESCENDING

// Output options
config.setVerboseLevel(int level);                // 0=quiet, 1=normal, 2=verbose
config.setDisplayProgress(boolean enable);        // Default: true

// Advanced options
config.setMaxDepth(int depth);                    // Default: unlimited
config.setTimeout(long milliseconds);             // Default: unlimited
```

#### Example

```java
SolverConfiguration config = new SolverConfiguration();
config.setUseSingletons(true);
config.setUseMRV(true);
config.setPrioritizeBorders(true);
config.setVerboseLevel(2);

EternitySolver solver = new EternitySolver(board, config);
```

### PuzzleRunner

High-level orchestrator for running solver with configuration.

#### Constructor

```java
PuzzleRunner runner = new PuzzleRunner(String puzzleName, SolverConfiguration config);
```

#### Key Methods

```java
// Run solver
boolean run();

// Get result
Board getResult();

// Get statistics
SolverStatistics getStatistics();
```

#### Example

```java
SolverConfiguration config = new SolverConfiguration();
config.setVerboseLevel(1);

PuzzleRunner runner = new PuzzleRunner("example_4x4", config);
boolean solved = runner.run();

if (solved) {
    Board solution = runner.getResult();
    solution.display();
}
```

## Usage Examples

### Example 1: Simple Solver

```java
import model.Board;
import model.Piece;
import solver.EternitySolver;
import util.PuzzleLoader;

import java.util.Map;

public class SimpleSolverExample {
    public static void main(String[] args) {
        // Load puzzle
        Map<Integer, Piece> pieces = PuzzleLoader.load("data/example_3x3.txt");
        Board board = new Board(3, 3);

        // Solve
        EternitySolver solver = new EternitySolver(board);
        boolean solved = solver.solve(pieces);

        // Output result
        if (solved) {
            System.out.println("✓ Solution found!");
            board.display();
        } else {
            System.out.println("✗ No solution found.");
        }
    }
}
```

### Example 2: With Progress Callback

```java
import solver.EternitySolver;
import solver.SolverConfiguration;
import solver.ProgressListener;

public class ProgressExample {
    public static void main(String[] args) {
        Board board = new Board(4, 4);
        Map<Integer, Piece> pieces = loadPuzzle("example_4x4");

        // Create configuration with progress listener
        SolverConfiguration config = new SolverConfiguration();
        config.setProgressListener(new ProgressListener() {
            @Override
            public void onProgress(int depth, int totalCalls) {
                System.out.println("Depth: " + depth + ", Calls: " + totalCalls);
            }

            @Override
            public void onSolutionFound(Board solution) {
                System.out.println("Solution found!");
            }
        });

        EternitySolver solver = new EternitySolver(board, config);
        solver.solve(pieces);
    }
}
```

### Example 3: Parallel Solving

```java
import runner.ParallelSolverOrchestrator;
import solver.SolverConfiguration;

public class ParallelExample {
    public static void main(String[] args) {
        Board board = new Board(6, 6);
        Map<Integer, Piece> pieces = loadPuzzle("validation_6x6");

        // Configure parallel solver
        SolverConfiguration config = new SolverConfiguration();
        config.setParallelMode(true);
        config.setThreadCount(8);

        // Create orchestrator
        ParallelSolverOrchestrator orchestrator =
            new ParallelSolverOrchestrator(board, pieces, config);

        // Solve
        boolean solved = orchestrator.solve();

        if (solved) {
            Board solution = orchestrator.getSolution();
            solution.display();
        }
    }
}
```

### Example 4: Timeout Handling

```java
import solver.EternitySolver;
import solver.SolverConfiguration;
import solver.TimeoutException;

public class TimeoutExample {
    public static void main(String[] args) {
        Board board = new Board(5, 5);
        Map<Integer, Piece> pieces = loadPuzzle("example_5x5");

        // Set 30-second timeout
        SolverConfiguration config = new SolverConfiguration();
        config.setTimeout(30000);

        EternitySolver solver = new EternitySolver(board, config);

        try {
            boolean solved = solver.solve(pieces);
            if (solved) {
                System.out.println("Solved within time limit!");
            } else {
                System.out.println("No solution found.");
            }
        } catch (TimeoutException e) {
            System.out.println("Timeout after 30 seconds");
            System.out.println("Explored " + solver.getStatistics().getTotalCalls() + " nodes");
        }
    }
}
```

### Example 5: Statistics Collection

```java
import solver.EternitySolver;
import solver.SolverStatistics;

public class StatisticsExample {
    public static void main(String[] args) {
        Board board = new Board(4, 4);
        Map<Integer, Piece> pieces = loadPuzzle("example_4x4");

        EternitySolver solver = new EternitySolver(board);
        solver.solve(pieces);

        // Get detailed statistics
        SolverStatistics stats = solver.getStatistics();

        System.out.println("=== Solver Statistics ===");
        System.out.println("Total calls: " + stats.getTotalCalls());
        System.out.println("Backtracks: " + stats.getBacktrackCount());
        System.out.println("Singletons found: " + stats.getSingletonCount());
        System.out.println("Domain reductions: " + stats.getDomainReductions());
        System.out.println("Max depth: " + stats.getMaxDepth());
        System.out.println("Solve time: " + stats.getSolveTime() + "ms");

        // Calculate efficiency metrics
        double backtrackRate = (double) stats.getBacktrackCount() / stats.getTotalCalls();
        System.out.println("Backtrack rate: " + String.format("%.2f%%", backtrackRate * 100));
    }
}
```

## Configuration

### Complete Configuration Example

```java
SolverConfiguration config = new SolverConfiguration();

// Core optimizations
config.setUseSingletons(true);
config.setUseMRV(true);
config.setUseSymmetryBreaking(true);
config.setUseAC3(true);

// Heuristics
config.setPrioritizeBorders(false);
config.setPieceOrder(PieceOrder.ASCENDING);

// Parallel mode
config.setParallelMode(false);
config.setThreadCount(8);

// Limits
config.setTimeout(60000);  // 60 seconds
config.setMaxDepth(100);

// Output
config.setVerboseLevel(1);
config.setDisplayProgress(true);

// Create solver
EternitySolver solver = new EternitySolver(board, config);
```

### PieceOrder Enum

```java
public enum PieceOrder {
    ASCENDING,   // Smallest ID first (default)
    DESCENDING,  // Largest ID first
    RANDOM       // Random order
}
```

## Custom Puzzles

### Creating Puzzles Programmatically

```java
import model.Piece;
import java.util.HashMap;
import java.util.Map;

public class CustomPuzzleExample {
    public static void main(String[] args) {
        // Create pieces manually
        Map<Integer, Piece> pieces = new HashMap<>();

        // Piece format: (id, north, east, south, west)
        // Edge value 0 = gray (border)
        pieces.put(1, new Piece(1, 0, 1, 2, 0));  // Corner piece
        pieces.put(2, new Piece(2, 0, 2, 3, 1));  // Border piece
        pieces.put(3, new Piece(3, 0, 3, 4, 2));  // Corner piece
        pieces.put(4, new Piece(4, 2, 4, 0, 0));  // Corner piece
        pieces.put(5, new Piece(5, 3, 5, 0, 4));  // Border piece
        pieces.put(6, new Piece(6, 4, 6, 0, 5));  // Corner piece
        // ... add remaining pieces

        // Create board and solve
        Board board = new Board(3, 3);
        EternitySolver solver = new EternitySolver(board);
        boolean solved = solver.solve(pieces);
    }
}
```

### Puzzle File Format

Custom puzzles can be loaded from text files:

```
# example_puzzle.txt
# Format: id north east south west

1 0 1 2 0
2 0 2 3 1
3 0 3 4 2
4 2 4 0 0
5 3 5 0 4
6 4 6 0 5
# ... more pieces
```

Load with:

```java
Map<Integer, Piece> pieces = PuzzleLoader.load("path/to/example_puzzle.txt");
```

## Advanced Features

### Custom Heuristics

Implement custom cell selection heuristic:

```java
import solver.heuristics.CellSelector;
import model.Board;
import model.Position;

public class CustomCellSelector implements CellSelector {
    @Override
    public Position selectNextCell(Board board, DomainManager domainManager) {
        // Your custom logic here
        // Example: select cell closest to center
        int centerX = board.getWidth() / 2;
        int centerY = board.getHeight() / 2;

        Position closest = null;
        double minDistance = Double.MAX_VALUE;

        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                if (board.isEmpty(x, y)) {
                    double distance = Math.sqrt(
                        Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2)
                    );
                    if (distance < minDistance) {
                        minDistance = distance;
                        closest = new Position(x, y);
                    }
                }
            }
        }

        return closest;
    }
}

// Use custom heuristic
EternitySolver solver = new EternitySolver(board);
solver.setCellSelector(new CustomCellSelector());
```

### Save/Load State

Save and resume solving:

```java
import util.SaveManager;

public class SaveLoadExample {
    public static void main(String[] args) {
        Board board = new Board(6, 6);
        Map<Integer, Piece> pieces = loadPuzzle("validation_6x6");

        // Configure with timeout
        SolverConfiguration config = new SolverConfiguration();
        config.setTimeout(30000);

        EternitySolver solver = new EternitySolver(board, config);

        try {
            solver.solve(pieces);
        } catch (TimeoutException e) {
            // Save state
            SaveManager.save(board, pieces, "partial_solution.bin");
            System.out.println("State saved. Resume later.");
        }

        // Later: resume from saved state
        SaveManager.SaveState state = SaveManager.load("partial_solution.bin");
        Board resumedBoard = state.getBoard();
        Map<Integer, Piece> remainingPieces = state.getRemainingPieces();

        EternitySolver resumedSolver = new EternitySolver(resumedBoard);
        resumedSolver.solve(remainingPieces);
    }
}
```

### Validation and Analysis

```java
import util.BoardValidator;
import util.PuzzleAnalyzer;

public class AnalysisExample {
    public static void main(String[] args) {
        Board board = new Board(4, 4);
        // ... populate board ...

        // Validate solution
        BoardValidator validator = new BoardValidator();
        if (validator.isValid(board)) {
            System.out.println("✓ Valid solution");
        } else {
            List<String> errors = validator.getErrors();
            errors.forEach(System.out::println);
        }

        // Analyze puzzle difficulty
        Map<Integer, Piece> pieces = loadPuzzle("example_4x4");
        PuzzleAnalyzer analyzer = new PuzzleAnalyzer();
        int difficulty = analyzer.estimateDifficulty(pieces, 4, 4);
        System.out.println("Estimated difficulty: " + difficulty);
    }
}
```

## Error Handling

### Common Exceptions

```java
import solver.EternitySolver;
import solver.TimeoutException;
import solver.InvalidPuzzleException;

public class ErrorHandlingExample {
    public static void main(String[] args) {
        try {
            Board board = new Board(4, 4);
            Map<Integer, Piece> pieces = loadPuzzle("example_4x4");

            SolverConfiguration config = new SolverConfiguration();
            config.setTimeout(10000);

            EternitySolver solver = new EternitySolver(board, config);
            solver.solve(pieces);

        } catch (TimeoutException e) {
            System.err.println("Solver timed out: " + e.getMessage());
            // Handle timeout

        } catch (InvalidPuzzleException e) {
            System.err.println("Invalid puzzle: " + e.getMessage());
            // Puzzle has incorrect number of pieces, invalid edges, etc.

        } catch (IllegalArgumentException e) {
            System.err.println("Invalid configuration: " + e.getMessage());
            // Invalid parameters (negative timeout, etc.)

        } catch (OutOfMemoryError e) {
            System.err.println("Out of memory. Try reducing puzzle size or thread count.");
            // Handle OOM

        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

## Best Practices

### 1. Choose Appropriate Configuration

```java
// Small puzzles (≤ 4×4): sequential with all optimizations
SolverConfiguration smallConfig = new SolverConfiguration();
smallConfig.setParallelMode(false);

// Large puzzles (≥ 6×6): parallel with 6-8 threads
SolverConfiguration largeConfig = new SolverConfiguration();
largeConfig.setParallelMode(true);
largeConfig.setThreadCount(8);
```

### 2. Handle Long-Running Solves

```java
// Always set timeout for unknown puzzles
config.setTimeout(300000);  // 5 minutes

// Consider running in separate thread
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<Boolean> future = executor.submit(() -> solver.solve(pieces));

try {
    boolean solved = future.get(5, TimeUnit.MINUTES);
} catch (TimeoutException e) {
    future.cancel(true);
}
```

### 3. Reuse Solver Instances

```java
// Don't create new solver for each puzzle
EternitySolver solver = new EternitySolver(new Board(4, 4));

for (String puzzleName : puzzleNames) {
    Map<Integer, Piece> pieces = loadPuzzle(puzzleName);
    solver.reset();  // Reset state
    solver.solve(pieces);
}
```

### 4. Monitor Performance

```java
SolverConfiguration config = new SolverConfiguration();
config.setVerboseLevel(1);  // Enable basic statistics

EternitySolver solver = new EternitySolver(board, config);
solver.solve(pieces);

SolverStatistics stats = solver.getStatistics();
if (stats.getBacktrackCount() > 100000) {
    System.out.println("Warning: High backtrack count. Consider timeout.");
}
```

### 5. Validate Input

```java
// Validate puzzle before solving
if (pieces.size() != width * height) {
    throw new IllegalArgumentException(
        "Piece count mismatch: expected " + (width * height) + ", got " + pieces.size()
    );
}

// Validate edge colors
Set<Integer> edgeColors = new HashSet<>();
for (Piece piece : pieces.values()) {
    edgeColors.add(piece.getNorth());
    edgeColors.add(piece.getEast());
    edgeColors.add(piece.getSouth());
    edgeColors.add(piece.getWest());
}

if (!edgeColors.contains(0)) {
    throw new InvalidPuzzleException("No border pieces (gray edge = 0)");
}
```

## Integration Examples

### Spring Boot Integration

```java
import org.springframework.stereotype.Service;
import solver.EternitySolver;

@Service
public class PuzzleSolverService {

    public SolutionResponse solvePuzzle(PuzzleRequest request) {
        Board board = new Board(request.getWidth(), request.getHeight());
        Map<Integer, Piece> pieces = convertToPieces(request.getPieces());

        SolverConfiguration config = new SolverConfiguration();
        config.setTimeout(request.getTimeoutMs());

        EternitySolver solver = new EternitySolver(board, config);

        try {
            boolean solved = solver.solve(pieces);
            return new SolutionResponse(solved, board, solver.getStatistics());
        } catch (TimeoutException e) {
            return SolutionResponse.timeout(solver.getStatistics());
        }
    }
}
```

### REST API Example

```java
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/puzzles")
public class PuzzleController {

    @Autowired
    private PuzzleSolverService solverService;

    @PostMapping("/solve")
    public ResponseEntity<SolutionResponse> solvePuzzle(@RequestBody PuzzleRequest request) {
        SolutionResponse response = solverService.solvePuzzle(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/difficulty/{puzzleName}")
    public ResponseEntity<Integer> estimateDifficulty(@PathVariable String puzzleName) {
        // Analyze puzzle difficulty
        return ResponseEntity.ok(analyzer.estimate(puzzleName));
    }
}
```

## Javadoc

Complete Javadoc documentation is available:

```bash
# Generate Javadoc
./mvnw javadoc:javadoc -Pdocs

# View documentation
open target/site/apidocs/index.html
```

## See Also

- [Algorithm Guide](ALGORITHM_GUIDE.md) - Detailed algorithm explanations
- [Performance Tuning](PERFORMANCE_TUNING.md) - Optimization guidelines
- [README](../README.md) - Project overview
- [Contributing](../CONTRIBUTING.md) - Development guidelines
