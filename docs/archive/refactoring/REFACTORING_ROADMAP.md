# Refactoring Roadmap - Eternity Puzzle Solver

**Status**: In Progress
**Last Updated**: 2025-12-04
**Current State**: 3 of 4 God classes remaining

---

## Overview

This document provides a detailed roadmap for completing the refactoring of "God classes" (classes >500 lines that violate Single Responsibility Principle).

### Current Status

| Class | Lines | Status | Target | Priority |
|-------|-------|--------|--------|----------|
| Main.java | 538 | 🔄 Partial (-23%) | ~100L | HIGH |
| BoardVisualizer.java | 569 | ⏳ Not started | ~150L/class | HIGH |
| SaveStateManager.java | 519 | ⏳ Not started | ~130L/class | MEDIUM |
| ~~ParallelSearchManager~~ | ~~516~~ | ✅ Done (prev) | - | - |

---

## 1. Main.java Refactoring

**Current**: 538 lines
**Target**: ~100 lines
**Reduction Needed**: ~440 lines
**Priority**: HIGH
**Estimated Effort**: 4-6 hours

### Current State

**Already Extracted** ✅:
- BoardRenderer.java (111L) - `printBoardWithCoordinates()`
- ComparisonAnalyzer.java (130L) - `compareWithAndWithoutSingletons()`

**Still Remaining** ⏳:
- 8 run* methods (~400 lines total)
- Duplicate puzzle execution logic

### Refactoring Strategy

#### Step 1: Use Existing PuzzleRunner (2 hours)

**File**: `runner/PuzzleRunner.java` (already exists - 248 lines)

**Action**:
```java
// Instead of:
private static void runExample3x3() {
    int rows = 3, cols = 3;
    Map<Integer, Piece> pieces = PuzzleFactory.createExample3x3();
    Board board = new Board(rows, cols);
    EternitySolver solver = new EternitySolver();
    boolean solved = solver.solve(board, pieces);
    // ...
}

// Use:
private static void runExample3x3() {
    Map<Integer, Piece> pieces = PuzzleFactory.createExample3x3();
    Board board = new Board(3, 3);
    PuzzleRunnerConfig config = new PuzzleRunnerConfig();
    PuzzleRunner runner = new PuzzleRunner(board, pieces, config);
    PuzzleResult result = runner.run();
    // Display result
}
```

**Files to Modify**:
- Main.java (update 8 methods to delegate to PuzzleRunner)

**Tests**:
- Existing tests should still pass
- Add integration test for PuzzleRunner if missing

**Expected Reduction**: ~200 lines

#### Step 2: Extract ExamplePuzzles Utility (1 hour)

**Create**: `util/ExamplePuzzles.java`

**Purpose**: Static convenience methods for running example puzzles

```java
public class ExamplePuzzles {
    public static void run3x3() { /* ... */ }
    public static void run4x4Hard() { /* ... */ }
    public static void run4x4Easy() { /* ... */ }
    public static void run4x4Ordered() { /* ... */ }
}
```

**Expected Reduction**: ~100 lines from Main.java

#### Step 3: Extract LargePuzzleRunner (1 hour)

**Create**: `runner/LargePuzzleRunner.java`

**Purpose**: Handle large puzzles with save/load logic

```java
public class LargePuzzleRunner {
    public static void runEternityII() { /* ... */ }
    public static void runPuzzle16x16() { /* ... */ }
    public static void runPuzzle6x12() { /* ... */ }
    public static void runValidation6x6() { /* ... */ }
}
```

**Expected Reduction**: ~140 lines from Main.java

#### Step 4: Simplify Main.main() (30 min)

**Final Main.java**:
```java
public class Main {
    public static void main(String[] args) {
        // Simple dispatcher
        if (args.length > 0) {
            String puzzle = args[0];
            switch (puzzle) {
                case "3x3" -> ExamplePuzzles.run3x3();
                case "4x4" -> ExamplePuzzles.run4x4Hard();
                case "eternity2" -> LargePuzzleRunner.runEternityII();
                // ...
                default -> printUsage();
            }
        } else {
            // Default: run 6x12
            LargePuzzleRunner.runPuzzle6x12();
        }
    }

    private static void printUsage() { /* ... */ }
}
```

**Final Size**: ~100 lines ✅

---

## 2. BoardVisualizer.java Refactoring

**Current**: 569 lines
**Target**: ~150 lines per class
**Priority**: HIGH
**Estimated Effort**: 6-8 hours

### Current State

**Structure**:
- 4 visualization methods (avg ~120 lines each)
- Heavy duplication in ANSI colors and grid drawing
- Complex method signatures (6-7 parameters)

**Methods**:
1. `printBoardCompact()` - 73 lines
2. `printBoardWithCounts()` - 118 lines
3. `printBoardWithLabels()` - 153 lines
4. `printBoardWithComparison()` - 119 lines

### Refactoring Strategy

#### Step 1: Create BoardFormatter Interface (1 hour)

**Create**: `solver/visualization/BoardFormatter.java`

```java
public interface BoardFormatter {
    void format(Board board, Map<Integer, Piece> pieces,
                FormatterContext context);
}

public class FormatterContext {
    public BitSet pieceUsed;
    public int totalPieces;
    public FitsChecker fitsChecker;
    public int highlightRow = -1;
    public int highlightCol = -1;
}
```

#### Step 2: Extract Formatter Implementations (3-4 hours)

**Create**:
1. `solver/visualization/CompactBoardFormatter.java` (~100 lines)
   - Implements printBoardCompact logic

2. `solver/visualization/DetailedBoardFormatter.java` (~150 lines)
   - Implements printBoardWithCounts logic

3. `solver/visualization/LabeledBoardFormatter.java` (~180 lines)
   - Implements printBoardWithLabels logic

4. `solver/visualization/ComparisonBoardFormatter.java` (~150 lines)
   - Implements printBoardWithComparison logic

#### Step 3: Extract Common Utilities (2 hours)

**Create**:
1. `solver/visualization/AnsiColorHelper.java` (~80 lines)
   - ANSI color codes (RED, GREEN, BLUE, RESET, etc.)
   - Conditional coloring based on configuration

2. `solver/visualization/GridDrawingHelper.java` (~100 lines)
   - Box drawing characters
   - Grid line generation
   - Cell formatting utilities

#### Step 4: Update BoardVisualizer (1 hour)

**Final BoardVisualizer.java** (~150 lines):
```java
public class BoardVisualizer {
    private static final BoardFormatter compactFormatter =
        new CompactBoardFormatter();
    private static final BoardFormatter detailedFormatter =
        new DetailedBoardFormatter();
    // etc.

    public static void printBoardCompact(...) {
        FormatterContext ctx = new FormatterContext(...);
        compactFormatter.format(board, pieces, ctx);
    }

    // Similar delegation for other methods
}
```

#### Step 5: Add Comprehensive Tests (2 hours)

**Create**: `solver/visualization/*Test.java`
- Each formatter gets its own test class
- AnsiColorHelper tests
- GridDrawingHelper tests
- Coverage: 0% → 85%+

**Expected Result**:
- BoardVisualizer: 569L → 150L (-74%)
- 5 new focused classes
- 100+ new tests
- Maintainable and testable

---

## 3. SaveStateManager.java Refactoring

**Current**: 519 lines
**Target**: ~130 lines per class
**Priority**: MEDIUM
**Estimated Effort**: 4-6 hours

### Current State

**Responsibilities** (violates SRP):
- File I/O (reading/writing)
- State serialization
- File discovery and filtering
- Backup rotation
- Directory management

**Issues**:
- Multiple @Deprecated methods
- Complex save/load logic
- Mixed concerns

### Refactoring Strategy

#### Step 1: Extract SaveStateWriter (2 hours)

**Create**: `util/state/SaveStateWriter.java` (~150 lines)

**Responsibilities**:
- Writing board state to files
- Atomic writes
- Backup creation

```java
public class SaveStateWriter {
    public void saveState(String configId, Board board,
                         Set<Integer> usedPieces, int depth) { /* ... */ }
    public void saveWithBackup(String filename, String content) { /* ... */ }
}
```

#### Step 2: Extract SaveStateReader (1.5 hours)

**Create**: `util/state/SaveStateReader.java` (~120 lines)

**Responsibilities**:
- Loading board state from files
- Parsing save files
- State reconstruction

```java
public class SaveStateReader {
    public Object[] loadBestState(Map<Integer, Piece> pieces) { /* ... */ }
    public Object[] loadState(File file, Map<Integer, Piece> pieces) { /* ... */ }
}
```

#### Step 3: Extract SaveStateLocator (1 hour)

**Create**: `util/state/SaveStateLocator.java` (~100 lines)

**Responsibilities**:
- Finding save files
- Filtering by pattern
- Sorting by timestamp/depth

```java
public class SaveStateLocator {
    public List<File> findCurrentSaves(String configId) { /* ... */ }
    public List<File> findBestSaves(String configId) { /* ... */ }
    public File findMostRecentSave(String configId) { /* ... */ }
}
```

#### Step 4: Extract BackupManager (1 hour)

**Create**: `util/state/BackupManager.java` (~90 lines)

**Responsibilities**:
- Backup rotation
- Old file cleanup
- Backup naming conventions

```java
public class BackupManager {
    public void rotateBackups(String baseFilename, int maxBackups) { /* ... */ }
    public void cleanupOldBackups(String directory, int keepCount) { /* ... */ }
}
```

#### Step 5: Update SaveStateManager (30 min)

**Final SaveStateManager.java** (~130 lines):
```java
public class SaveStateManager {
    private final SaveStateWriter writer = new SaveStateWriter();
    private final SaveStateReader reader = new SaveStateReader();
    private final SaveStateLocator locator = new SaveStateLocator();
    private final BackupManager backupManager = new BackupManager();

    // Simple delegation methods
    public void saveState(...) { writer.saveState(...); }
    public Object[] loadBestState(...) { return reader.loadBestState(...); }
    // etc.

    // Remove all @Deprecated methods
}
```

#### Step 6: Tests and Cleanup (1.5 hours)

- Add tests for each new class
- Remove @Deprecated methods
- Update all references
- Integration tests for save/load workflow

**Expected Result**:
- SaveStateManager: 519L → 130L (-75%)
- 4 focused classes
- No @Deprecated code
- 60+ new tests

---

## 4. Remaining Magic Numbers

**Current State**: ~30 occurrences remaining
**Target**: <10 occurrences
**Priority**: LOW-MEDIUM
**Effort**: 2-3 hours

### Known Locations

**Search patterns**:
```bash
grep -rn "1000\|5000\|10000\|60000\|3600" src/main/java/ --include="*.java"
```

**Categories**:
1. Time conversions (/ 1000, / 60000, etc.) → Use TimeConstants
2. Thread counts (Math.max(4, ...)) → Use ParallelConstants
3. Timeouts (600000, etc.) → Use TimeConstants constants
4. Intervals (5000, 10000, etc.) → Use TimeConstants or define new

### Replacement Strategy

**Batch 1**: Solver package (1 hour)
- ✅ BacktrackingSolver.java (done)
- ✅ SolverStatistics.java (done)
- ✅ StatisticsManager.java (done)
- AutoSaveManager.java
- ConfigurationManager.java

**Batch 2**: Util package (30 min)
- SolverLogger.java
- SaveStateManager.java (during refactoring)

**Batch 3**: Monitoring package (30 min)
- FileWatcherService.java
- MetricsAggregator.java

**Batch 4**: Verification (30 min)
- Search for remaining occurrences
- Document intentional magic numbers
- Final verification

---

## 5. Remove @Deprecated Code

**Current**: 16 @Deprecated items
**Priority**: MEDIUM
**Effort**: 3-4 hours

### Deprecated Items Inventory

#### ParallelSearchManager.java (11 items)
```
Lines 18-78: Static state for backward compatibility
- All getters/setters
- resetGlobalState()
```

**Action**:
1. Create migration guide
2. Document breaking changes
3. Remove static state
4. Update all usages (grep for ParallelSearchManager.get)
5. Version as 2.0.0 (breaking change)

#### SaveStateManager.java (3 items)
```
Lines 197, 206, 260: Old save/load methods
```

**Action**:
- Remove during Step 5 of SaveStateManager refactoring
- Already covered in refactoring plan above

#### SolverLogger.java (2 items)
```
Lines 239, 254: Old logging methods
```

**Action**:
1. Find all usages: `grep -r "SolverLogger\." src/`
2. Update to new methods
3. Remove deprecated methods

### Migration Strategy

**Phase 1: Documentation** (1 hour)
- Create MIGRATION_GUIDE.md
- Document all breaking changes
- Provide before/after code examples

**Phase 2: Update Internal Usage** (1 hour)
- Update all internal calls to deprecated methods
- Verify no internal code uses deprecated API

**Phase 3: Removal** (1 hour)
- Remove @Deprecated methods
- Update version to 2.0.0
- Add release notes

**Phase 4: Verification** (30 min)
- Full test suite
- Check for compilation warnings
- Verify no @Deprecated remains

---

## Implementation Timeline

### Week 1: Main.java Completion
**Days 1-2**: Use PuzzleRunner (4-6 hours)
- Update all run* methods to use PuzzleRunner
- Extract ExamplePuzzles utility
- Extract LargePuzzleRunner
- **Result**: Main.java: 538L → ~100L ✅

### Week 2: BoardVisualizer Refactoring
**Days 3-5**: Extract formatters (6-8 hours)
- Create BoardFormatter interface
- Implement 4 formatter classes
- Extract AnsiColorHelper and GridDrawingHelper
- Add comprehensive tests
- **Result**: BoardVisualizer: 569L → 150L ✅

### Week 3: SaveStateManager & Cleanup
**Days 6-8**: SaveStateManager (4-6 hours)
- Extract Writer, Reader, Locator, BackupManager
- Remove @Deprecated methods
- Add tests for each component
- **Result**: SaveStateManager: 519L → 130L ✅

**Days 9-10**: Final Cleanup (3-4 hours)
- Remove remaining @Deprecated code
- Eliminate remaining magic numbers
- Final verification
- **Result**: 0 @Deprecated, <10 magic numbers ✅

---

## Success Criteria

### Main.java
- [ ] Size reduced to ~100 lines
- [ ] All run* methods extracted or delegated
- [ ] No duplication remaining
- [ ] All tests passing

### BoardVisualizer
- [ ] Split into 4-5 focused classes
- [ ] Each class <200 lines
- [ ] Test coverage >80%
- [ ] All visualization working correctly

### SaveStateManager
- [ ] Split into 4 classes (Writer/Reader/Locator/Backup)
- [ ] Each class <150 lines
- [ ] All @Deprecated removed
- [ ] Test coverage >85%

### Overall
- [ ] 0 God classes (all <200 lines)
- [ ] 0 @Deprecated methods
- [ ] <10 magic numbers
- [ ] >85% test coverage
- [ ] All tests passing (909+)

---

## Risk Assessment

### Low Risk (Can do anytime)
- ✅ Magic number replacement (done mostly)
- ⏳ @Deprecated removal (with migration guide)
- ⏳ Adding TODOs and documentation

### Medium Risk (Need good test coverage)
- ⏳ Main.java simplification (good test coverage exists)
- ⏳ BoardVisualizer split (need tests first)

### High Risk (Complex refactoring)
- ⏳ SaveStateManager split (I/O heavy, save format changes)
- ⏳ ParallelSearchManager static state removal (threading)

### Mitigation Strategies

1. **Always test-first**: Add tests before refactoring
2. **Small increments**: Extract one method at a time
3. **Keep tests green**: Never commit with failing tests
4. **Backward compatibility**: Use @Deprecated during transition
5. **Code reviews**: Have another developer review large changes

---

## Tracking Progress

### Completed ✅
- [x] Analyze codebase and create roadmap
- [x] Fix all failing tests (41 → 0)
- [x] Add comprehensive test coverage (+226 tests)
- [x] Create TimeConstants & ParallelConstants
- [x] Replace magic numbers in Main classes
- [x] Extract BoardRenderer from Main.java
- [x] Extract ComparisonAnalyzer from Main.java
- [x] Add comprehensive documentation

### In Progress 🔄
- [ ] Replace remaining magic numbers in solver package (partially done)
- [ ] Main.java refactoring (partial: 697L → 538L)

### Not Started ⏳
- [ ] BoardVisualizer refactoring
- [ ] SaveStateManager refactoring
- [ ] Remove @Deprecated code
- [ ] Extract FileWatcherService interface (for Mockito)

---

## Quick Wins (Can be done independently)

### 1. Replace Remaining Magic Numbers (2 hours)
**Files**: AutoSaveManager, ConfigurationManager, SolverLogger, monitoring/*
**Impact**: HIGH
**Risk**: LOW

### 2. Add TODO Comments in Code (1 hour)
**Files**: Main.java, BoardVisualizer.java, SaveStateManager.java
**Impact**: MEDIUM (clarity for future developers)
**Risk**: NONE

### 3. Create Migration Guide (1 hour)
**File**: MIGRATION_GUIDE.md
**Impact**: HIGH (enables @Deprecated removal)
**Risk**: NONE

### 4. Extract More Test Utilities (1-2 hours)
**Create**: TestFixtures.java, TestBoards.java, TestPieces.java
**Impact**: MEDIUM (makes writing tests easier)
**Risk**: LOW

---

## Notes for Future Sessions

### Lessons from Current Session

**What Worked Well** ✅:
- Phased approach (tests first, then refactoring)
- Quick wins before big refactorings (constants)
- Comprehensive documentation
- Small, focused commits

**Challenges** ⚠️:
- God classes are highly coupled (need careful extraction)
- Mockito limitations (need interface extraction)
- Locale-aware testing (French vs English)
- Console output testing is difficult

**Recommendations** 📝:
1. Always create tests BEFORE refactoring
2. Extract one method/class at a time
3. Keep commits small and focused
4. Document architectural decisions
5. Use @Deprecated for gradual migration

---

## References

- TECHNICAL_DOCUMENTATION.md - Complete architecture
- SESSION_SUMMARY.md - Current session accomplishments
- Analysis report (in session conversation) - Detailed analysis of all issues

---

**This roadmap will be updated as refactoring progresses.**

**Last Update**: 2025-12-04 - After exceptional improvement session
**Next Review**: When starting God class refactorings
