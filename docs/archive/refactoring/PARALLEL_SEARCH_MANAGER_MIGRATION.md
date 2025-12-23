# ParallelSearchManager Migration Guide

## Overview

`ParallelSearchManager` currently uses both **deprecated static state** (legacy) and **instance-based SharedSearchState** (modern). This document outlines the migration path to fully eliminate the deprecated static methods.

## Current State

### Deprecated Static Methods (To Remove)

Located in `src/main/java/solver/ParallelSearchManager.java`:

```java
@Deprecated
private static AtomicBoolean solutionFound = new AtomicBoolean(false);
@Deprecated
private static AtomicInteger globalMaxDepth = new AtomicInteger(0);
@Deprecated
private static AtomicInteger globalBestScore = new AtomicInteger(0);
@Deprecated
private static AtomicInteger globalBestThreadId = new AtomicInteger(-1);
@Deprecated
private static AtomicReference<Board> globalBestBoard = new AtomicReference<>(null);
@Deprecated
private static AtomicReference<Map<Integer, Piece>> globalBestPieces = new AtomicReference<>(null);
@Deprecated
private static final Object lockObject = new Object();
@Deprecated
private static ForkJoinPool workStealingPool = null;
```

### Modern Instance-Based Approach

```java
private final SharedSearchState sharedState;
private final DomainManager domainManager;
```

## Why Migrate?

1. **Thread Safety**: Static state is shared across all solver instances, causing conflicts
2. **Testability**: Cannot run multiple solver instances in parallel tests
3. **Isolation**: Each solver instance should have independent state
4. **Memory Leaks**: Static references prevent garbage collection
5. **Best Practices**: Instance-based design is more maintainable

## Impact Analysis

### Main Code References (16 total)

**EternitySolver.java** (15 references):
- Line 69: `ParallelSearchManager.resetGlobalState()`
- Lines 169-174: Constructor passing static getters to BacktrackingManager
- Line 234: `ParallelSearchManager.getSolutionFound()` in solve()
- Lines 340-346: Creating BacktrackingManager with static getters

**SolverConstants.java** (1 reference):
- Line 49: Documentation comment referencing `WORK_STEALING_DEPTH_THRESHOLD`

### Test Code References (67 total)

Located in:
- `src/test/java/solver/ParallelSearchManagerTest.java`
- Various integration tests

## Migration Strategy

### Phase 1: Refactor EternitySolver (High Priority)

**Objective**: Add SharedSearchState as instance variable instead of using static methods

**Steps**:

1. **Add SharedSearchState field to EternitySolver**:
```java
public class EternitySolver {
    private SharedSearchState sharedState;

    public EternitySolver() {
        this.sharedState = new SharedSearchState();
        // ...
    }
}
```

2. **Remove resetGlobalState() call**:
```java
// BEFORE:
public static void resetGlobalState() {
    ParallelSearchManager.resetGlobalState();
}

// AFTER:
public static void resetGlobalState() {
    // State now managed per-instance, no global reset needed
}
```

3. **Pass sharedState to BacktrackingManager**:
```java
// BEFORE:
BacktrackingManager mgr = new BacktrackingManager(
    this, stats,
    ParallelSearchManager.getSolutionFound(),
    ParallelSearchManager.getLockObject(),
    // ... more static getters
);

// AFTER:
BacktrackingManager mgr = new BacktrackingManager(
    this, stats,
    sharedState.getSolutionFound(),
    sharedState.getLockObject(),
    // ... instance methods
);
```

### Phase 2: Update BacktrackingManager Constructor

**Objective**: Accept SharedSearchState instead of individual atomics

**Before**:
```java
public BacktrackingManager(
    EternitySolver solver,
    SolverStatistics stats,
    AtomicBoolean solutionFound,
    Object lockObject,
    AtomicInteger globalMaxDepth,
    // ... more parameters
) {
    this.solutionFound = solutionFound;
    // ...
}
```

**After**:
```java
public BacktrackingManager(
    EternitySolver solver,
    SolverStatistics stats,
    SharedSearchState sharedState
) {
    this.sharedState = sharedState;
    // Access via sharedState.getSolutionFound(), etc.
}
```

### Phase 3: Update Tests

**Objective**: Update 67 test references to use instance-based approach

**Pattern**:
```java
// BEFORE:
ParallelSearchManager.resetGlobalState();
boolean found = ParallelSearchManager.getSolutionFound().get();

// AFTER:
SharedSearchState state = new SharedSearchState();
ParallelSearchManager manager = new ParallelSearchManager(domainManager, state);
boolean found = state.getSolutionFound().get();
```

### Phase 4: Remove Deprecated Methods

**Objective**: Delete all @Deprecated static methods once migration is complete

**Methods to remove**:
- `resetGlobalState()`
- `getSolutionFound()`
- `getGlobalMaxDepth()`
- `getGlobalBestScore()`
- `getGlobalBestThreadId()`
- `getGlobalBestBoard()`
- `getGlobalBestPieces()`
- `getLockObject()`
- `getWorkStealingPool()`
- `initWorkStealingPool()`
- `shutdownWorkStealingPool()`

## Testing Strategy

### 1. Pre-Migration Tests

Run full test suite to establish baseline:
```bash
mvn test
```

### 2. Incremental Migration

Migrate one component at a time:
- EternitySolver first
- BacktrackingManager second
- Update tests last

### 3. Regression Testing

After each phase:
```bash
mvn test -Dtest=ParallelSearchManagerTest
mvn test -Dtest=EternitySolverTest
mvn test  # Full suite
```

### 4. Integration Testing

Test parallel solving with multiple instances:
```java
@Test
void testMultipleSolverInstances() {
    EternitySolver solver1 = new EternitySolver();
    EternitySolver solver2 = new EternitySolver();

    // Should not interfere with each other
    CompletableFuture<Boolean> result1 = CompletableFuture.supplyAsync(() -> solver1.solve());
    CompletableFuture<Boolean> result2 = CompletableFuture.supplyAsync(() -> solver2.solve());

    // Both should have independent state
    assertNotSame(solver1.getSharedState(), solver2.getSharedState());
}
```

## Benefits After Migration

1. **Parallel Test Execution**: Tests can run concurrently without state conflicts
2. **Multiple Solver Instances**: Can run multiple puzzles simultaneously
3. **Better Encapsulation**: Each solver has its own state
4. **Easier Debugging**: No hidden global state to track
5. **Memory Management**: Solver instances can be garbage collected properly

## Risks

1. **Breaking Changes**: Signature changes in BacktrackingManager affect many call sites
2. **Test Failures**: 67 test references need updating
3. **Behavioral Changes**: Ensure parallel search still works correctly
4. **Performance Impact**: Verify no performance regression from instance state

## Estimated Effort

- **Phase 1 (EternitySolver)**: 2-3 hours
- **Phase 2 (BacktrackingManager)**: 1-2 hours
- **Phase 3 (Test Updates)**: 3-4 hours
- **Phase 4 (Cleanup)**: 1 hour
- **Testing & Verification**: 2-3 hours

**Total**: ~10-15 hours

## Rollback Plan

If migration causes issues:

1. Revert commits using git:
```bash
git log --oneline | grep "ParallelSearchManager"
git revert <commit-hash>
```

2. Keep deprecated methods temporarily:
```java
@Deprecated(forRemoval = true, since = "2.0")
public static AtomicBoolean getSolutionFound() {
    // Keep for backward compatibility
}
```

3. Add migration timeline to deprecation notice:
```java
/**
 * @deprecated Use {@link SharedSearchState#getSolutionFound()} instead.
 * This method will be removed in version 3.0 (Q2 2025).
 */
```

## Future Work

After migration:

1. **Parallel Solver Pool**: Create managed pool of solver instances
2. **State Persistence**: Save/restore SharedSearchState for checkpoint/resume
3. **Metrics Dashboard**: Real-time monitoring of all solver instances
4. **Load Balancing**: Distribute work across solver instances

## References

- [Java Concurrency in Practice](https://jcip.net/) - Best practices for thread-safe design
- [Effective Java, 3rd Edition](https://www.oreilly.com/library/view/effective-java-3rd/9780134686097/) - Item 83: Use lazy initialization judiciously
- SharedSearchState.java - Modern replacement for static state
- BacktrackingManager.java - Consumer of search state

## Change Log

| Date | Author | Change |
|------|--------|--------|
| 2025-12-10 | Claude | Initial migration guide created |

---

**Status**: Migration not started (Phase 1.3 deferred due to complexity)
**Priority**: Medium (improves architecture but not critical for functionality)
**Complexity**: High (83 references across main and test code)
