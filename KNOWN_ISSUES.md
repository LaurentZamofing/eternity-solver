# Known Issues - Eternity Puzzle Solver

**Last Updated**: 2025-12-04
**Status**: Documented for future resolution

---

## 1. BoardVisualizer Alignment Issues in Verbose Mode

**Severity**: LOW (cosmetic)
**Priority**: MEDIUM
**Estimated Effort**: 2-3 hours

### Problem Description

When running the solver in verbose mode, the board display has alignment issues.
Cells with different piece counts (e.g., "9" vs "141") cause columns to misalign.

**Example of Misalignment:**
```
|   9   ||   35   ||   141   ||   141   ||
```
Should be consistent width for all cells.

### Root Cause

`BoardVisualizer.printBoardWithCounts()` and `printBoardWithLabels()` use
fixed-width formatting that doesn't account for variable-length piece counts.

**File**: `src/main/java/solver/BoardVisualizer.java` (lines 120-238, 239-392)

### Requested Enhancements

In addition to fixing alignment, add color coding for:
1. **Fixed pieces** (pre-placed hints) - e.g., CYAN
2. **Placement order** - Gradient showing sequence
3. **Dead ends** (0 possible pieces) - RED
4. **Last placed piece** - GREEN/highlighted

### Proposed Solution

**Short-term** (2-3 hours):
- Fix printf format strings to use max-width calculations
- Add ANSI color codes for different cell states
- Test with various board sizes

**Long-term** (from REFACTORING_ROADMAP.md):
- Refactor BoardVisualizer into modular formatters
- Extract AnsiColorHelper utility
- Add comprehensive tests (currently 0%)
- Create different display modes (compact, detailed, colored)

### Workaround

For now, verbose mode still works functionally (shows all information),
just with cosmetic alignment issues on large boards.

### Related

- See REFACTORING_ROADMAP.md Section 2 (BoardVisualizer refactoring)
- BoardVisualizer is a God class (569 lines) scheduled for refactoring
- Would benefit from comprehensive tests before modifications

---

## 2. Mockito Cannot Mock FileWatcherService

**Severity**: MEDIUM (blocks 27 tests)
**Priority**: HIGH
**Estimated Effort**: 2-3 hours

### Problem Description

27 monitoring controller tests are disabled because Mockito cannot mock
FileWatcherService class.

**Error:**
```
org.mockito.exceptions.base.MockitoException:
Mockito cannot mock this class: class monitoring.service.FileWatcherService
```

### Root Cause

Mockito inline mocking limitations with certain JVM classes.

**Affected Tests:**
- `DashboardControllerTest.java` (14 tests @Disabled)
- `MetricsWebSocketControllerTest.java` (13 tests @Disabled)

### Proposed Solution

**Extract Interface** (2-3 hours):
1. Create `IFileWatcherService` interface
2. Rename current class to `FileWatcherServiceImpl`
3. Update dependency injection to use interface
4. Enable Mockito to mock the interface
5. Re-enable 27 tests

**Expected Result:**
- 27 additional tests enabled
- Coverage for monitoring package: 65% → 80%
- Better design (dependency on interface, not implementation)

### Related

- See TECHNICAL_DOCUMENTATION.md "Known Limitations" section
- Documented in test files with @Disabled annotations

---

## 3. Remaining God Classes

**Severity**: LOW (code quality)
**Priority**: MEDIUM
**Estimated Effort**: 10-15 hours total

### BoardVisualizer (569 lines)

**Status**: Not yet refactored
**Target**: ~150 lines per class (4 formatters + 2 helpers)
**Effort**: 6-8 hours

See REFACTORING_ROADMAP.md Section 2 for detailed plan.

### SaveStateManager (519 lines)

**Status**: Not yet refactored
**Target**: ~130 lines per class (Writer/Reader/Locator/BackupManager)
**Effort**: 4-6 hours

See REFACTORING_ROADMAP.md Section 3 for detailed plan.

---

## 4. Deprecated Code Accumulation

**Severity**: LOW (technical debt)
**Priority**: MEDIUM
**Estimated Effort**: 3-4 hours

### Items to Remove

**ParallelSearchManager.java** (11 @Deprecated items):
- Static state for backward compatibility
- All deprecated getters/setters
- Scheduled for removal in version 2.0

**SaveStateManager.java** (3 @Deprecated items):
- Old save/load methods
- Will be removed during refactoring

**SolverLogger.java** (2 @Deprecated items):
- Old logging methods
- Can be removed after updating all usages

### Action Plan

See REFACTORING_ROADMAP.md Section 5 for migration strategy.

---

## 5. Remaining Magic Numbers

**Severity**: LOW (code quality)
**Priority**: LOW
**Estimated Effort**: 1-2 hours

### Current State

Reduced from 50+ to ~20-25 occurrences.

**Remaining locations:**
- FormattingUtils.java (intentional in formatting logic)
- PuzzleConfig.java (intentional in duration formatting)
- util/SaveStateIO.java (time conversions)
- Some documentation/comments

### Note

Some magic numbers in formatting utilities may be acceptable for readability.
Should evaluate case-by-case whether extraction improves or harms clarity.

---

## Summary

**Critical Issues**: NONE ✅
**High Priority**: FileWatcherService interface extraction
**Medium Priority**: BoardVisualizer alignment + colors, God class refactoring
**Low Priority**: Remaining magic numbers, deprecated code cleanup

**Overall Code Health**: EXCELLENT
- 909 tests, 0 failures
- 80% coverage
- Production-ready

These issues are quality improvements, not blockers.

---

**For solutions, see:**
- REFACTORING_ROADMAP.md - Detailed refactoring plans
- TECHNICAL_DOCUMENTATION.md - Architecture and known limitations
- SESSION_SUMMARY.md - Recent improvements and context
