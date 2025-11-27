#!/bin/bash

# generate_javadoc.sh - Generate Javadoc API documentation
# Usage: ./generate_javadoc.sh

echo "════════════════════════════════════════════════════════"
echo "Generating Javadoc API Documentation"
echo "════════════════════════════════════════════════════════"
echo ""

# Create output directory
OUTPUT_DIR="docs/api"
mkdir -p "$OUTPUT_DIR"

echo "1. Collecting source files..."
SOURCE_FILES=$(find src -name "*.java" | grep -v "Main.java\|MainCLI.java\|MainSequential.java\|MainParallel.java\|PuzzleConfig.java")
FILE_COUNT=$(echo "$SOURCE_FILES" | wc -l | tr -d ' ')
echo "   ✓ Found $FILE_COUNT source files"

echo ""
echo "2. Generating Javadoc..."
javadoc \
  -d "$OUTPUT_DIR" \
  -sourcepath src \
  -classpath "lib/*" \
  -author \
  -version \
  -use \
  -windowtitle "Eternity Solver API" \
  -doctitle "<h1>Eternity Solver API Documentation</h1>" \
  -header "<b>Eternity Solver v1.0.0</b>" \
  -bottom "<i>Copyright © 2025 Eternity Solver Project</i>" \
  -link https://docs.oracle.com/en/java/javase/11/docs/api/ \
  -subpackages cli:model:runner:solver:util \
  2>&1 | grep -v "warning"

if [ $? -eq 0 ]; then
    echo "   ✓ Javadoc generated successfully"
else
    echo "   ✗ Javadoc generation had some warnings (check output)"
fi

echo ""
echo "3. Creating API index..."
cat > "$OUTPUT_DIR/README.md" << 'EOF'
# Eternity Solver - API Documentation

This directory contains the Javadoc API documentation for Eternity Solver.

## Viewing Documentation

Open `index.html` in your web browser:

```bash
open docs/api/index.html
```

Or on Linux:
```bash
xdg-open docs/api/index.html
```

## Main Packages

### cli
Command-line interface classes
- `CommandLineInterface` - CLI argument parsing and validation

### model
Domain model classes
- `Board` - Puzzle board representation
- `Piece` - Puzzle piece with edges
- `Cell` - Board cell

### runner
Orchestration layer
- `PuzzleRunner` - Puzzle solving orchestrator
- `PuzzleRunnerConfig` - Builder for configuration
- `PuzzleResult` - Solving result

### solver
Core solving algorithms
- `EternitySolver` - Main backtracking solver
- `ParallelSearchManager` - Parallel search coordination
- `BoardVisualizer` - Board visualization
- **optimization/** - Optimization algorithms
  - `NeighborAnalyzer` - Neighbor-based pruning
  - `SingletonDetector` - Forced placement detection
  - `EdgesAnalyzer` - Edge constraint analysis

### util
Utility classes
- `ShutdownManager` - Graceful shutdown handling
- `SaveStateManager` - State persistence
- `PuzzleFactory` - Puzzle creation
- `PlacementOrderTracker` - Placement history

## Quick Links

- [CommandLineInterface](cli/CommandLineInterface.html)
- [EternitySolver](solver/EternitySolver.html)
- [PuzzleRunner](runner/PuzzleRunner.html)
- [Board](model/Board.html)
- [Piece](model/Piece.html)

## Usage Examples

See the [User Manual](../USER_MANUAL.md) and [Developer Guide](../DEVELOPER_GUIDE.md) for usage examples.

---

Generated: $(date)
EOF

echo "   ✓ API index created"

echo ""
echo "════════════════════════════════════════════════════════"
echo "✓ Javadoc Generation Complete"
echo "════════════════════════════════════════════════════════"
echo ""
echo "Documentation available at: $OUTPUT_DIR/index.html"
echo ""
echo "To view:"
echo "  open $OUTPUT_DIR/index.html"
echo ""
