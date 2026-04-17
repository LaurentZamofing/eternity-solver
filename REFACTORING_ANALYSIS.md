# Code Refactoring Analysis - Today's Changes

## Executive Summary
Analysis of 7 modified files identifies **3 critical refactoring opportunities** with concrete line numbers and solutions. Primary issues involve code duplication in MRVPlacementStrategy and missing null checks across multiple files.

---

## 1. MRVPlacementStrategy.java - CRITICAL: Code Duplication

### Issue 1a: Unused Pieces List Building (Lines 137-142 and 216-221, 256-261)

**Location:** Lines 137-142 (initial board), 216-221 (dead-end), 256-261 (successful placement)

**Problem:** The same loop appears THREE times to build unused pieces list:
```java
List<Integer> unusedPieces = new ArrayList<>();
for (int i = 1; i <= context.totalPieces; i++) {
    if (!context.pieceUsed.get(i)) {
        unusedPieces.add(i);
    }
}
```

**Impact:** 
- Violates DRY principle
- Makes maintenance harder (any bug fix requires 3 updates)
- Adds 5 lines of boilerplate code in 3 different contexts

**Refactoring Suggestion:**

Extract to a helper method in `BacktrackingContext`:
```java
public List<Integer> getUnusedPieces() {
    List<Integer> unused = new ArrayList<>();
    for (int i = 1; i <= totalPieces; i++) {
        if (!pieceUsed.get(i)) {
            unused.add(i);
        }
    }
    return unused;
}
```

Then replace all three blocks with: `List<Integer> unusedPieces = context.getUnusedPieces();`

---

### Issue 1b: Placement Order Map Building (Lines 145-153, 269-277)

**Location:** Lines 145-153 (initial board) and 269-277 (successful placement)

**Problem:** Nearly identical code appears TWICE to build placement order map:
```java
java.util.Map<util.PositionKey, Integer> placementOrderMap = new java.util.HashMap<>();
java.util.List<util.SaveStateManager.PlacementInfo> allPlacements = solver.getPlacementHistory();
if (allPlacements != null) {
    int step = 1;
    for (util.SaveStateManager.PlacementInfo info : allPlacements) {
        util.PositionKey key = new util.PositionKey(info.row, info.col);
        placementOrderMap.put(key, step++);
    }
}
```

**Impact:**
- 8 lines of duplicated initialization code
- Inconsistency risk if logic needs changes
- Verbose with fully-qualified type names

**Refactoring Suggestion:**

Extract to helper method in `EternitySolver`:
```java
public Map<PositionKey, Integer> buildPlacementOrderMap() {
    Map<PositionKey, Integer> map = new HashMap<>();
    List<SaveStateManager.PlacementInfo> placements = getPlacementHistory();
    if (placements != null) {
        int step = 1;
        for (SaveStateManager.PlacementInfo info : placements) {
            map.put(new PositionKey(info.row, info.col), step++);
        }
    }
    return map;
}
```

Then replace with: `Map<PositionKey, Integer> placementOrderMap = solver.buildPlacementOrderMap();`

---

### Issue 1c: Board Display with Unused Pieces (Lines 158-159, 226, 280-281)

**Location:** Lines 158-159 (initial), 226 (dead-end), 280-281 (successful)

**Problem:** Three similar calls to `printBoardWithLabels()` with slightly different parameter combinations, making it unclear what each displays.

**Refactoring Suggestion:**

Create dedicated helper methods in EternitySolver:
```java
public void displayBoardBefore(Board board, int[] nextCell) {
    Map<PositionKey, Integer> orderMap = buildPlacementOrderMap();
    List<Integer> unused = context.getUnusedPieces();
    printBoardWithLabels(board, piecesById, unused, null, nextCell, orderMap);
}

public void displayBoardAfterPlacement(Board board, SaveStateManager.PlacementInfo lastPlacement) {
    Map<PositionKey, Integer> orderMap = buildPlacementOrderMap();
    List<Integer> unused = context.getUnusedPieces();
    printBoardWithLabels(board, piecesById, unused, lastPlacement, null, orderMap);
}

public void displayBoardDuringDeadEnd(Board board) {
    List<Integer> unused = context.getUnusedPieces();
    printBoardWithLabels(board, piecesById, unused);
}
```

This replaces 30+ lines of setup code with clear, single-purpose method calls.

---

## 2. ConstraintPropagator.java - Missing Null Checks

### Issue 2a: Unchecked Logging on Line 92, 144, 164, 178

**Locations:** Lines 92, 144, 164, 178 (multiple `SolverLogger.error/warn` calls)

**Problem:** Logging calls don't verify that `cellLabel` construction is safe, but this is actually acceptable since row/col are validated. However, there's a more subtle issue:

Lines 91-92:
```java
String cellLabel = String.valueOf((char) ('A' + cellRow)) + (cellCol + 1);
util.SolverLogger.error("       ❌ AC-3 DEAD-END: Cell " + cellLabel + ...);
```

**Risk:** `SolverLogger` is accessed via fully-qualified name but not null-checked. While unlikely to be null, best practice is to verify.

**Refactoring Suggestion:**

Add null-check guard or extract logging to a dedicated helper method in DomainManager:
```java
private void logDeadEndAtCell(int row, int col, String reason) {
    String cellLabel = String.valueOf((char) ('A' + row)) + (col + 1);
    util.SolverLogger.error("       ❌ AC-3 DEAD-END: Cell " + cellLabel + " - " + reason);
}
```

Then replace all three dead-end logs with: `logDeadEndAtCell(cellRow, cellCol, "has EMPTY domain!")`

---

## 3. DomainManager.java - Code Quality Issues

### Issue 3a: Inconsistent Domain Restoration Logic (Lines 84-106)

**Location:** Lines 83-106 in `restoreAC3Domains()`

**Problem:** Same domain recomputation loop appears twice (cell itself, then neighbors):
```java
// For cell itself (lines 87-91)
domains[r][c] = new HashMap<>();
List<ValidPlacement> validPlacements = computeDomain(...);
for (ValidPlacement vp : validPlacements) {
    domains[r][c].computeIfAbsent(...).add(vp);
}

// For neighbors (lines 100-104) - nearly identical
domains[nr][nc] = new HashMap<>();
validPlacements = computeDomain(...);
for (ValidPlacement vp : validPlacements) {
    domains[nr][nc].computeIfAbsent(...).add(vp);
}
```

**Impact:** Harder to maintain, violates DRY

**Refactoring Suggestion:**

Extract to helper method:
```java
private void recomputeDomainAt(int r, int c, Board board, Map<Integer, Piece> piecesById, 
                               BitSet pieceUsed, int totalPieces) {
    domains[r][c] = new HashMap<>();
    List<ValidPlacement> validPlacements = computeDomain(board, r, c, piecesById, pieceUsed, totalPieces);
    for (ValidPlacement vp : validPlacements) {
        domains[r][c].computeIfAbsent(vp.pieceId, k -> new ArrayList<>()).add(vp);
    }
}
```

Then simplify to:
```java
public void restoreAC3Domains(Board board, int r, int c, ...) {
    recomputeDomainAt(r, c, board, piecesById, pieceUsed, totalPieces);
    
    int[][] neighbors = {{r-1, c}, {r+1, c}, {r, c-1}, {r, c+1}};
    for (int[] nbr : neighbors) {
        int nr = nbr[0], nc = nbr[1];
        if (nr < 0 || nr >= board.getRows() || nc < 0 || nc >= board.getCols()) continue;
        if (!board.isEmpty(nr, nc)) continue;
        recomputeDomainAt(nr, nc, board, piecesById, pieceUsed, totalPieces);
    }
}
```

---

### Issue 3b: Duplicate Cache Update Logic (Lines 203-214, 229-240)

**Locations:** `updateCacheAfterPlacement()` lines 203-214 and `restoreCacheAfterBacktrack()` lines 229-240

**Problem:** Identical neighbor update pattern appears in two methods:
```java
// Lines 203-214 in updateCacheAfterPlacement()
if (r > 0 && board.isEmpty(r - 1, c)) {
    domainCache.put((r-1) * cols + c, computeDomain(...));
}
if (r < rows - 1 && board.isEmpty(r + 1, c)) {
    domainCache.put((r+1) * cols + c, computeDomain(...));
}
if (c > 0 && board.isEmpty(r, c - 1)) {
    domainCache.put(r * cols + (c-1), computeDomain(...));
}
if (c < cols - 1 && board.isEmpty(r, c + 1)) {
    domainCache.put(r * cols + (c+1), computeDomain(...));
}

// Lines 229-240 - IDENTICAL LOGIC
```

**Impact:** 12 lines of duplicated conditional cache updates

**Refactoring Suggestion:**

Extract to shared helper:
```java
private void updateCacheForNeighbors(Board board, int r, int c, 
                                     Map<Integer, Piece> piecesById, BitSet pieceUsed, int totalPieces) {
    int rows = board.getRows();
    int cols = board.getCols();
    int[][] neighbors = {{r-1, c}, {r+1, c}, {r, c-1}, {r, c+1}};
    
    for (int[] nbr : neighbors) {
        int nr = nbr[0], nc = nbr[1];
        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && board.isEmpty(nr, nc)) {
            int key = nr * cols + nc;
            domainCache.put(key, computeDomain(board, nr, nc, piecesById, pieceUsed, totalPieces));
        }
    }
}
```

Then replace both 12-line blocks with: `updateCacheForNeighbors(board, r, c, piecesById, pieceUsed, totalPieces);`

---

## 4. LabeledBoardRenderer.java - Constructor Overload Chain

### Issue 4a: Excessive Constructor Overloading (Lines 43-95)

**Problem:** Four constructors with a confusing overload chain:
- Line 53-57: Base constructor (6 params)
- Line 70-75: Constructor 2 (7 params, calls constructor 1 with null)
- Line 89-95: Constructor 3 (8 params, calls constructor 2 with null)
- Line 110-114: Constructor 4 (9 params, calls constructor 3 with null)

**Root Cause:** Trying to provide optional parameter support through overloading rather than using a builder or default values pattern.

**Refactoring Suggestion:**

Use Java's builder pattern or consolidate to single constructor with null parameters:

Option A - Use Builder Pattern:
```java
public static class Builder {
    private final Board board;
    private final Map<Integer, Piece> piecesById;
    private final List<Integer> unusedIds;
    private final PlacementValidator validator;
    private final ValidPieceCounter validPieceCounter;
    private final Set<PositionKey> fixedPositions;
    private Set<PositionKey> highlightedPositions = null;
    private Set<PositionKey> nextCellPositions = null;
    private Map<PositionKey, Integer> placementOrderMap = null;
    
    public Builder(Board board, Map<Integer, Piece> piecesById, List<Integer> unusedIds,
                   PlacementValidator validator, ValidPieceCounter counter, Set<PositionKey> fixed) {
        // store required params
    }
    
    public Builder highlightPositions(Set<PositionKey> positions) {
        this.highlightedPositions = positions;
        return this;
    }
    
    public LabeledBoardRenderer build() {
        return new LabeledBoardRenderer(board, piecesById, unusedIds, validator, 
                                       validPieceCounter, fixedPositions, highlightedPositions,
                                       nextCellPositions, placementOrderMap);
    }
}
```

Option B - Keep single constructor, use null parameters everywhere:
```java
public LabeledBoardRenderer(Board board, Map<Integer, Piece> piecesById,
                            List<Integer> unusedIds, PlacementValidator validator,
                            ValidPieceCounter validPieceCounter, Set<PositionKey> fixedPositions,
                            Set<PositionKey> highlightedPositions, Set<PositionKey> nextCellPositions,
                            Map<PositionKey, Integer> placementOrderMap) {
    // Implementation...
}
```

Then callers use: `new LabeledBoardRenderer(..., null, null, null)` when optional params not needed.

---

## 5. EdgeMatchingColorStrategy.java - Repeated Null Checks

### Issue 5a: Redundant Null Checks (Lines 90-91, 95-96 and 118-119, 123-124)

**Locations:** `getEdgeColor()` method lines 118-125

**Problem:** Duplicate null-check patterns appear across both methods:

```java
// getCellColor() - Lines 90-96
if (nextCellPositions != null && nextCellPositions.contains(positionKey)) {
    return BRIGHT_BLUE;
}
if (highlightedPositions != null && highlightedPositions.contains(positionKey)) {
    return BRIGHT_MAGENTA;
}

// getEdgeColor() - Lines 118-124 - IDENTICAL PATTERN
if (nextCellPositions != null && nextCellPositions.contains(positionKey)) {
    return BRIGHT_BLUE;
}
if (highlightedPositions != null && highlightedPositions.contains(positionKey)) {
    return BRIGHT_MAGENTA;
}
```

**Impact:** 6 lines of duplicated null-check logic

**Refactoring Suggestion:**

Extract to helper method:
```java
private String checkHighlightColor(PositionKey positionKey) {
    if (nextCellPositions != null && nextCellPositions.contains(positionKey)) {
        return BRIGHT_BLUE;
    }
    if (highlightedPositions != null && highlightedPositions.contains(positionKey)) {
        return BRIGHT_MAGENTA;
    }
    return null;  // No highlight color
}
```

Then both methods simplify:
```java
@Override
public String getCellColor(Board board, int row, int col) {
    PositionKey key = new PositionKey(row, col);
    String highlightColor = checkHighlightColor(key);
    if (highlightColor != null) return highlightColor;
    return fixedPositions.contains(key) ? BRIGHT_CYAN : "";
}

@Override
public String getEdgeColor(Board board, int row, int col, int direction) {
    PositionKey key = new PositionKey(row, col);
    String highlightColor = checkHighlightColor(key);
    if (highlightColor != null) return highlightColor;
    
    if (fixedPositions.contains(key)) return "";
    // Rest of edge matching logic...
}
```

---

## 6. ValidCountColorStrategy.java - Similar Issue

### Issue 6a: Duplicate Null Checks (Lines 117-124)

**Location:** Lines 117-124 in `getCellColor()`

**Problem:** Nearly identical to EdgeMatchingColorStrategy:
```java
if (nextCellPositions != null && nextCellPositions.contains(positionKey)) {
    return BRIGHT_BLUE;
}
if (highlightedPositions != null && highlightedPositions.contains(positionKey)) {
    return BRIGHT_MAGENTA;
}
```

**Refactoring Suggestion:**

Same as Issue 5a - extract shared logic to base class or utility:

Create a base helper class or utility method in ColorStrategy interface:
```java
public interface ColorStrategy {
    // Existing methods...
    
    // New default helper method for subclasses
    default String getHighlightColor(PositionKey key, Set<PositionKey> nextCells, Set<PositionKey> highlighted) {
        if (nextCells != null && nextCells.contains(key)) return BRIGHT_BLUE;
        if (highlighted != null && highlighted.contains(key)) return BRIGHT_MAGENTA;
        return null;
    }
}
```

Then both strategies use: `String highlight = getHighlightColor(positionKey, nextCellPositions, highlightedPositions);`

---

## 7. BoardDisplayManager.java - Minor Issue

### Issue 7a: Inconsistent Null Handling (Lines 117-126)

**Location:** Lines 117-126 in `printBoardWithLabels()`

**Problem:** Manual null-check pattern repeated for two different position sets:
```java
Set<PositionKey> highlightedPositions = new HashSet<>();
if (lastPlacement != null) {
    highlightedPositions.add(new PositionKey(lastPlacement.row, lastPlacement.col));
}

Set<PositionKey> nextCellPositions = new HashSet<>();
if (nextCell != null) {
    nextCellPositions.add(new PositionKey(nextCell[0], nextCell[1]));
}
```

**Refactoring Suggestion:**

Extract to helper method:
```java
private Set<PositionKey> toPositionSet(Object source) {
    Set<PositionKey> set = new HashSet<>();
    if (source instanceof SaveStateManager.PlacementInfo) {
        SaveStateManager.PlacementInfo p = (SaveStateManager.PlacementInfo) source;
        set.add(new PositionKey(p.row, p.col));
    } else if (source instanceof int[]) {
        int[] cell = (int[]) source;
        set.add(new PositionKey(cell[0], cell[1]));
    }
    return set;
}

// Or separate methods for clarity:
private Set<PositionKey> createHighlightSet(SaveStateManager.PlacementInfo placement) {
    Set<PositionKey> set = new HashSet<>();
    if (placement != null) {
        set.add(new PositionKey(placement.row, placement.col));
    }
    return set;
}

private Set<PositionKey> createNextCellSet(int[] cell) {
    Set<PositionKey> set = new HashSet<>();
    if (cell != null) {
        set.add(new PositionKey(cell[0], cell[1]));
    }
    return set;
}
```

---

## Summary Table

| File | Issue | Type | Lines | Severity | Fix Complexity |
|------|-------|------|-------|----------|-----------------|
| MRVPlacementStrategy | Unused pieces loop | Duplication | 137-142, 216-221, 256-261 | HIGH | Low (extract method) |
| MRVPlacementStrategy | Placement order map | Duplication | 145-153, 269-277 | HIGH | Low (extract method) |
| MRVPlacementStrategy | Board display calls | Duplication | 158-159, 226, 280-281 | MEDIUM | Medium (3 methods) |
| ConstraintPropagator | Logging logic | Code smell | 91-92, 144, 164, 178 | LOW | Low (extract method) |
| DomainManager | Domain restoration | Duplication | 84-106 | MEDIUM | Low (extract method) |
| DomainManager | Cache updates | Duplication | 203-214, 229-240 | MEDIUM | Low (extract method) |
| LabeledBoardRenderer | Constructor chain | Overengineering | 43-114 | MEDIUM | High (builder pattern) |
| EdgeMatchingColorStrategy | Duplicate checks | Duplication | 90-96, 118-124 | MEDIUM | Low (extract method) |
| ValidCountColorStrategy | Duplicate checks | Duplication | 117-124 | MEDIUM | Low (extract method) |
| BoardDisplayManager | Set creation | Code smell | 117-126 | LOW | Low (extract method) |

---

## Recommended Priority Order

1. **HIGH PRIORITY (Fix First)**
   - MRVPlacementStrategy: Unused pieces duplication (3 instances)
   - MRVPlacementStrategy: Placement order map duplication (2 instances)

2. **MEDIUM PRIORITY (Fix Next)**
   - LabeledBoardRenderer: Constructor overloading chain
   - DomainManager: Domain restoration and cache update duplication
   - EdgeMatchingColorStrategy & ValidCountColorStrategy: Null check duplication

3. **LOW PRIORITY (Nice-to-Have)**
   - ConstraintPropagator: Logging optimization
   - BoardDisplayManager: Set creation helper

