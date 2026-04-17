# Eternity Solver - Code Quality Analysis Report

## Executive Summary

Analyzed 7 core files across recently modified features (PlacementOrderTracker, CellFormatter, BoardDisplayService, LabeledBoardRenderer, Board, MRVPlacementStrategy, SingletonPlacementStrategy). Identified **5 major code quality issues** with specific line numbers and refactoring recommendations.

---

## 1. CODE DUPLICATION

### 1.1 Position Key Construction Duplication (HIGH PRIORITY)

**Pattern**: `row + "," + col` is constructed **17+ times** across the codebase

**Files affected**:
- `/src/main/java/solver/PlacementOrderTracker.java` - Lines 62, 94, 109
- `/src/main/java/solver/display/CellFormatter.java` - Line 83
- `/src/main/java/solver/display/EdgeMatchingColorStrategy.java` - Lines 71, 94
- `/src/main/java/solver/display/ValidCountColorStrategy.java` - Line 94
- `/src/main/java/solver/ConstraintPropagator.java` - Line 68
- `/src/main/java/solver/visualization/LabeledBoardFormatter.java` - Lines 110, 146, 204
- `/src/main/java/solver/BacktrackingHistoryManager.java` - Lines 111, 156, 224

**Impact**:
- Error-prone (typos in format string)
- Inconsistent if changed in one place
- Pollutes logic with string formatting
- Reduces readability

**Recommendation**:
Create a `PositionKey` utility class:
```java
public final class PositionKey {
    private final int row;
    private final int col;
    
    public PositionKey(int row, int col) { ... }
    
    @Override
    public String toString() { return row + "," + col; }
    
    @Override
    public int hashCode() { ... }
    @Override
    public boolean equals(Object o) { ... }
}
```
Use as Map key instead of String, eliminating parsing/formatting.

---

### 1.2 Border Checking Logic Duplication

**Pattern**: Border detection repeated in multiple validation methods

**Files and contexts**:
- `/src/main/java/solver/display/BoardDisplayService.java` - Lines 409-418
  ```java
  boolean isTopBorder = (row == 0);
  boolean isBottomBorder = (row == board.getRows() - 1);
  boolean isLeftBorder = (col == 0);
  boolean isRightBorder = (col == board.getCols() - 1);
  ```
- `/src/main/java/solver/visualization/LabeledBoardFormatter.java` - Similar pattern
- `/src/main/java/solver/heuristics/MRVCellSelector.java` - Line 179+ with different implementation

**Impact**:
- Inconsistent implementations (some use row-1 vs getRows()-1)
- Maintenance burden across multiple files
- 61 direct `edges[0]`, `edges[1]`, `edges[2]`, `edges[3]` accesses

**Recommendation**:
Add utility methods to `Board` class:
```java
public boolean isBorderCell(int r, int c)
public boolean isTopBorder(int r)
public boolean isBottomBorder(int r)
public boolean isLeftBorder(int c)
public boolean isRightBorder(int c)
```

---

### 1.3 Edge Access and Neighbor Validation Duplication

**Pattern**: Repetitive neighbor checking with `isEmpty()` and `getPlacement()` calls

**Location**: `/src/main/java/solver/display/BoardDisplayService.java` - Lines 420-478
- Lines 421-429: Check north neighbor (isEmpty → getPlacement)
- Lines 436-448: Check east neighbor (isEmpty → getPlacement)
- Lines 451-463: Check south neighbor (isEmpty → getPlacement)
- Lines 466-478: Check west neighbor (isEmpty → getPlacement)

**Code pattern** (repeated 4 times):
```java
if (col < board.getCols() - 1) {
    if (board.isEmpty(row, col + 1)) {
        // No constraint yet
    } else {
        Placement eastPlacement = board.getPlacement(row, col + 1);
        int eastWest = eastPlacement.edges[3];
        if (eastWest != east) return false;
    }
} else {
    // Right border
    if (east != 0) return false;
}
```

**Also exists in**:
- `/src/main/java/solver/visualization/LabeledBoardFormatter.java` - Lines 248-277 (similar edge matching)

**Impact**:
- 50+ lines of boilerplate
- Same validation logic scattered across 2+ files
- Difficult to maintain consistency

**Recommendation**:
Extract into `Board` utility method:
```java
public boolean validateEdgeMatch(int r, int c, int[] edges)
```
Or create `EdgeValidator` utility class with:
```java
public static boolean matchesNeighbors(Board board, int r, int c, int[] edges)
```

---

## 2. MISSING UTILITY CLASSES

### 2.1 Edge/Direction Constants (HIGH PRIORITY)

**Issue**: Edge indices are magic numbers throughout codebase (61 occurrences of `edges[0]` through `edges[3]`)

**Affected files**:
- `/src/main/java/solver/display/BoardDisplayService.java` - Lines 251, 297-298, 312, 427-428, etc.
- `/src/main/java/solver/display/CellFormatter.java` - Lines 77-78, 150-151, 183-184
- `/src/main/java/solver/visualization/LabeledBoardFormatter.java` - Lines 248-277
- Multiple solver files accessing edges[0/1/2/3]

**Current understanding required**:
- edges[0] = North
- edges[1] = East
- edges[2] = South
- edges[3] = West

**Recommendation**:
Create `EdgeDirection` enum:
```java
public enum EdgeDirection {
    NORTH(0, "North"),
    EAST(1, "East"),
    SOUTH(2, "South"),
    WEST(3, "West");
    
    public final int index;
    public final String label;
    
    // Methods to get opposite, rotate, etc.
    public EdgeDirection opposite() { ... }
    public EdgeDirection rotateClockwise() { ... }
}
```

Then:
```java
int northEdge = placement.edges[EdgeDirection.NORTH.index];
// or better: placement.getEdge(EdgeDirection.NORTH)
```

---

### 2.2 Coordinate/Position Handling

**Issue**: Row/col coordinate validation scattered

**Location**: `/src/main/java/model/Board.java` - Line 267-273
```java
private void validateCoordinates(int r, int c) {
    if (r < 0 || r >= rows || c < 0 || c >= cols) {
        throw new IndexOutOfBoundsException(...);
    }
}
```

Also in `/src/main/java/solver/display/BoardDisplayService.java` - canPlacePiece() performs similar checks

**Recommendation**:
Create `Coordinate` or `BoardPosition` value object:
```java
public final class BoardPosition {
    public final int row;
    public final int col;
    
    public BoardPosition(int row, int col) {
        if (row < 0 || col < 0) throw new IllegalArgumentException(...);
    }
    
    public String toKey() { return row + "," + col; }
    public BoardPosition north() { return new BoardPosition(row - 1, col); }
    public BoardPosition south() { return new BoardPosition(row + 1, col); }
    public BoardPosition east() { return new BoardPosition(row, col + 1); }
    public BoardPosition west() { return new BoardPosition(row, col - 1); }
}
```

---

### 2.3 Cell Constraint Checking

**Issue**: `board.hasConstraints()` is a new method (newly added) but constraint logic is duplicated elsewhere

**Location**: 
- `/src/main/java/model/Board.java` - Lines 233-262 (NEW)
  - Checks if border OR has occupied neighbor
  - Used in `/src/main/java/solver/display/CellFormatter.java` - Line 265

**Problem**: 
- CellFormatter assumes it should hide cells with no constraints (Line 265-266)
- This logic should be centralized
- Other files may duplicate this pattern

**Recommendation**:
Document when to use `hasConstraints()` and ensure all constraint-checking code routes through Board methods.

---

## 3. COMPLEX METHODS NEEDING REFACTORING

### 3.1 MRVPlacementStrategy.tryPlacement() - TOO LONG (HIGH PRIORITY)

**Location**: `/src/main/java/solver/MRVPlacementStrategy.java` - Lines 87-287 (200+ lines)

**Responsibilities** (violates SRP):
1. Cell selection with MRV (lines 89-91)
2. Available pieces counting (lines 102-105)
3. Piece sorting (lines 107-121)
4. Debug logging and depth tracking (lines 123-133)
5. Piece iteration loop (lines 135-277)
6. AC-3 constraint propagation (lines 175-189)
7. Debug board display (lines 199-238)
8. Backtracking management (lines 259-270)

**Recommendation**: Extract into smaller methods:
```java
private void logCellSelection(...)
private List<Integer> getAvailablePieces(...)
private boolean tryPieceRotations(BacktrackingContext, EternitySolver, int r, int c, int pid, ...)
private void handleAC3Failure(...)
private void handleSuccessfulPlacement(...)
private void handleBacktrack(...)
```

---

### 3.2 BoardDisplayService.writeToSaveFileDetailed() - COMPLEX

**Location**: `/src/main/java/solver/display/BoardDisplayService.java` - Lines 199-342 (143 lines)

**Issues**:
1. Pre-computes row candidate counts (lines 228-241)
2. Handles three separate line types (lines 243-275, 278-302, 305-317)
3. Renders separators (lines 319-328)
4. Logging mixed with rendering (lines 204-216)

**Repeated sections** (lines 243-317):
- Line 1 (north edges): isEmpty check, format north edge + order, separator
- Line 2 (middle): isEmpty check, format middle with candidates or piece ID+edges, separator
- Line 3 (south edges): isEmpty check, format south edge, separator
- Separator rendering repeated

**Recommendation**: Extract into column/line renderers:
```java
private void renderNorthEdgeLine(PrintWriter, int row, CandidateCount[])
private void renderMiddleLine(PrintWriter, int row, CandidateCount[])
private void renderSouthEdgeLine(PrintWriter, int row)
private void renderRowSeparator(PrintWriter, int cols)
```

---

### 3.3 CellFormatter.formatNorthEdge() - Format Logic Complexity

**Location**: `/src/main/java/solver/display/CellFormatter.java` - Lines 72-134 (62 lines)

**Issues**:
- Lines 96-99: Algorithm comments inline but unclear
- Lines 100-107: Complex spacing calculation
- Lines 111-118: Conditional string building
- Lines 121-131: Conditional color application

**Problem**: Format logic is duplicated for:
- North edge formatting (lines 72-134)
- South edge formatting (lines 145-164) - different but related
- Middle line formatting (lines 175-224) - yet another pattern

**Recommendation**: Create `EdgeFormatter` helper:
```java
public class EdgeFormatter {
    public String formatEdge(int edgeValue, String color, String orderSuffix, boolean isHorizontal)
    
    // Encapsulate positioning logic
    private String buildSpacedString(int edgeValue, String orderSuffix)
}
```

---

## 4. MISSING TESTS (CRITICAL)

### 4.1 Board.hasConstraints() - NO TESTS

**File**: `/src/main/java/model/Board.java` - Lines 233-262 (NEWLY ADDED)

**Status**: Method exists but has ZERO test coverage in `/src/test/java/model/BoardTest.java`

**Test gaps**:
- Border cells (top, bottom, left, right)
- Interior cells with no occupied neighbors
- Interior cells with occupied north neighbor
- Interior cells with occupied east neighbor
- Interior cells with occupied south neighbor
- Interior cells with occupied west neighbor
- Corner cells
- Edge cases (1x1, 2x2 boards)

**Recommendation**: Add comprehensive test class
```java
@Test
void testHasConstraints_TopBorder() { ... }
@Test
void testHasConstraints_BottomBorder() { ... }
@Test
void testHasConstraints_InteriorNoNeighbors() { ... }
@Test
void testHasConstraints_InteriorWithNorthNeighbor() { ... }
@Test
void testHasConstraints_InteriorWithAllNeighbors() { ... }
// etc., 15+ test cases
```

---

### 4.2 PlacementOrderTracker - PARTIAL TEST COVERAGE

**Status**: 
- File: `/src/test/java/solver/PlacementOrderTrackerTest.java` - EXISTS with comprehensive tests ✓
- But **missing test for new removePlacement(row, col)** method (not deprecated removal)

**New method** (Lines 107-120):
```java
public SaveStateManager.PlacementInfo removePlacement(int row, int col)
```

**Test gaps**:
- removePlacement(row, col) - no dedicated tests
- Behavior when placement doesn't exist
- Updates history correctly
- Works with fixed pieces

**Note**: Tests for deprecated `removeLastPlacement()` exist (lines 194-240) but use Map-based removal which has different semantics.

**Recommendation**: Add tests
```java
@Test
void testRemovePlacement_ByCoordinate() { ... }
@Test
void testRemovePlacement_NonexistentPosition() { ... }
@Test
void testRemovePlacement_AfterMultiplePlacements() { ... }
```

---

### 4.3 Display Formatting with Placement Order - LIMITED TESTS

**Status**: 
- CellFormatter tests exist (Lines 1-273 in CellFormatterTest.java)
- BoardDisplayService tests exist (Lines 1-177 in BoardDisplayServiceTest.java)
- **But: No integration tests for placement order display**

**What's missing**:
1. LabeledBoardRenderer with placement order map - no dedicated tests
2. Integration test: PlacementOrderTracker → Map → CellFormatter → LabeledBoardRenderer
3. Test placement order display in save files (BoardDisplayService.writeToSaveFileDetailed with placementOrderMap)

**Affected code**:
- `/src/main/java/solver/display/CellFormatter.java` - Lines 80-88 (placement order in north edge)
- `/src/main/java/solver/display/BoardDisplayService.java` - Lines 253-259 (placement order in save files)
- `/src/main/java/solver/display/LabeledBoardRenderer.java` - Lines 88-103 (placementOrderMap parameter)

**Recommendation**: Create integration test
```java
@Test
void testPlacementOrderDisplayInBoard() {
    // Record placements
    PlacementOrderTracker tracker = new PlacementOrderTracker();
    tracker.recordPlacement(0, 0, 1, 0);
    tracker.recordPlacement(0, 1, 2, 1);
    
    // Build map
    Map<String, Integer> orderMap = new HashMap<>();
    List<PlacementInfo> history = tracker.getPlacementHistory();
    for (int i = 0; i < history.size(); i++) {
        PlacementInfo info = history.get(i);
        orderMap.put(info.row + "," + info.col, i + 1);
    }
    
    // Render board with order
    CellFormatter formatter = new CellFormatter(colorStrategy, orderMap);
    String north = formatter.formatNorthEdge(board, 0, 0);
    
    // Assert order number appears
    assertTrue(north.contains("#1"));
}
```

---

## 5. ARCHITECTURE & SINGLE RESPONSIBILITY PRINCIPLE VIOLATIONS

### 5.1 MRVPlacementStrategy - Mixed Concerns

**Location**: `/src/main/java/solver/MRVPlacementStrategy.java` - Lines 87-287

**Violations**:
1. **Placement logic** (lines 135-277) - Core algorithm
2. **Output/logging** (lines 101-106, 129-133, 152, 158-161, etc.) - Display concerns
3. **Debug board display** (lines 199-238) - User interaction
4. **History tracking** (line 173) - State management
5. **Domain restoration** (line 188) - Constraint propagation concern

**Should delegate to**:
- Dedicated output strategy ✓ (already has `outputStrategy`)
- PlacementOrderTracker ✓ (exists)
- But still mixed with debug display and user input

**Recommendation**: Extract to separate methods with single responsibilities

---

### 5.2 BoardDisplayService - Multiple Concerns

**Location**: `/src/main/java/solver/display/BoardDisplayService.java`

**Current responsibilities**:
1. Simple board formatting (formatBoardSimple, lines 75-92)
2. Frame/border rendering (printBoardWithFrame, lines 100-117)
3. Save file visualization (writeToSaveFile, lines 143-187)
4. Detailed save file with counts (writeToSaveFileDetailed, lines 199-342)
5. Candidate counting (countCandidates, countCandidatesWithRotations, lines 354-541)
6. Piece placement validation (canPlacePiece, lines 401-481)

**Should be separated into**:
- `BoardFormatter` (simple display)
- `SaveFileRenderer` (save file visualization)
- `CandidateCounter` (piece counting logic)
- `EdgeValidator` (edge validation)

**Current state**: Too many responsibilities = difficult to test, hard to reuse

---

### 5.3 CellFormatter - Growing Responsibilities

**Location**: `/src/main/java/solver/display/CellFormatter.java`

**Current methods**:
- formatNorthEdge (with placement order)
- formatSouthEdge
- formatMiddleLine
- formatEmptyCell
- formatEmptyCellWithRotations (NEW)

**Issue**: Growing placement order handling throughout
- Lines 80-88: Placement order lookup in north edge
- Line 102: Order suffix in format string
- Similar pattern needed in south edge (but NOT implemented)
- Pattern needed in save file rendering (but in BoardDisplayService)

**Should separate**: 
- Cell content formatting (current)
- Placement order overlay (new concern)

**Recommendation**: Create `PlacementOrderOverlay` class:
```java
public class PlacementOrderOverlay {
    public String applyOrderSuffix(String formattedEdge, int row, int col, Map<String, Integer> orderMap)
}
```

---

## 6. SUMMARY TABLE

| Issue | Severity | Type | Location | Impact |
|-------|----------|------|----------|--------|
| Position key duplication (17+ times) | HIGH | Code Duplication | Multiple files | Maintenance burden, error-prone |
| Border checking logic duplication | HIGH | Code Duplication | 3+ files | Inconsistency, scattered logic |
| Edge validation duplication | HIGH | Code Duplication | 2+ files | 50+ lines of boilerplate |
| Magic numbers (edges[0/1/2/3]) | MEDIUM | Missing Utility | 61 occurrences | Readability, maintainability |
| Missing position/coordinate utility | MEDIUM | Missing Utility | Scattered | Type safety, convenience |
| MRVPlacementStrategy too long | HIGH | Complex Method | 200+ lines | SRP violation |
| BoardDisplayService.writeToSaveFileDetailed too long | MEDIUM | Complex Method | 143 lines | Maintainability |
| CellFormatter formatting logic | MEDIUM | Complex Method | Multiple methods | Duplication potential |
| Board.hasConstraints() no tests | CRITICAL | Missing Tests | BoardTest.java | Zero coverage |
| PlacementOrderTracker removePlacement(r,c) not tested | HIGH | Missing Tests | PlacementOrderTrackerTest | Untested method |
| Placement order display integration tests | MEDIUM | Missing Tests | Multiple | No end-to-end validation |
| MRVPlacementStrategy mixed concerns | HIGH | Architecture | 200 lines | SRP violation |
| BoardDisplayService mixed concerns | MEDIUM | Architecture | Multiple methods | Difficult to test/reuse |
| CellFormatter growing concerns | MEDIUM | Architecture | Multiple | Placement order intertwined |

---

## 7. QUICK FIXES (Priority Order)

1. **Add tests for Board.hasConstraints()** (30 min)
   - Low effort, critical coverage gap

2. **Create PositionKey utility** (2 hours)
   - Replace 17 string concatenations
   - Reduces errors and improves performance

3. **Extract border checking methods in Board** (1 hour)
   - Add public utility methods
   - Centralize logic

4. **Refactor MRVPlacementStrategy.tryPlacement()** (4 hours)
   - Break into 5-6 smaller methods
   - Each with single responsibility

5. **Create EdgeDirection enum** (2 hours)
   - Replace 61 magic index accesses
   - Improve code readability significantly

