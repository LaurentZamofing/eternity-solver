# Contributing to Eternity Solver

Thank you for your interest in contributing to Eternity Solver! This document provides guidelines and instructions for contributing.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [How to Contribute](#how-to-contribute)
4. [Development Workflow](#development-workflow)
5. [Coding Standards](#coding-standards)
6. [Testing Guidelines](#testing-guidelines)
7. [Documentation](#documentation)
8. [Pull Request Process](#pull-request-process)
9. [Reporting Bugs](#reporting-bugs)
10. [Suggesting Features](#suggesting-features)

---

## Code of Conduct

###Our Pledge

We pledge to make participation in our project a harassment-free experience for everyone, regardless of:
- Age, body size, disability
- Ethnicity, gender identity
- Level of experience
- Nationality, personal appearance
- Race, religion
- Sexual identity and orientation

### Our Standards

**Positive behavior**:
✅ Using welcoming and inclusive language
✅ Being respectful of differing viewpoints
✅ Gracefully accepting constructive criticism
✅ Focusing on what is best for the community
✅ Showing empathy towards other community members

**Unacceptable behavior**:
❌ Trolling, insulting/derogatory comments
❌ Public or private harassment
❌ Publishing others' private information
❌ Other conduct which could reasonably be considered inappropriate

### Enforcement

Instances of abusive behavior may be reported to the project team. All complaints will be reviewed and investigated.

---

## Getting Started

### Prerequisites

- **Java**: JDK 11 or higher
- **Git**: For version control
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code (recommended)

### Setup Development Environment

1. **Fork the repository**

Click the "Fork" button on GitHub

2. **Clone your fork**

```bash
git clone https://github.com/YOUR_USERNAME/eternity.git
cd eternity
```

3. **Add upstream remote**

```bash
git remote add upstream https://github.com/ORIGINAL_OWNER/eternity.git
```

4. **Install dependencies**

Dependencies are in `lib/` folder (no need to download)

5. **Compile**

```bash
./compile.sh
```

6. **Run tests**

```bash
./run_tests.sh
```

7. **Verify everything works**

```bash
java -cp "bin:lib/*" MainCLI --version
```

---

## How to Contribute

### Types of Contributions

We welcome many types of contributions:

#### 🐛 Bug Fixes
- Fix existing bugs
- Improve error handling
- Fix edge cases

#### ✨ Features
- New optimizations
- CLI enhancements
- Performance improvements

#### 📚 Documentation
- Improve existing docs
- Add tutorials
- Create examples

#### 🧪 Tests
- Increase test coverage
- Add integration tests
- Add benchmarks

#### 🎨 Code Quality
- Refactoring
- Code cleanup
- Performance optimization

---

## Development Workflow

### 1. Create a Branch

```bash
# Update your fork
git checkout main
git pull upstream main

# Create feature branch
git checkout -b feature/my-awesome-feature

# Or for bug fix
git checkout -b fix/issue-123
```

### Branch Naming

- **Features**: `feature/description`
- **Bug fixes**: `fix/description` or `fix/issue-number`
- **Documentation**: `docs/description`
- **Refactoring**: `refactor/description`
- **Tests**: `test/description`

### 2. Make Changes

- Write clean, readable code
- Follow existing code style
- Add/update tests
- Update documentation

### 3. Test Your Changes

```bash
# Compile
./compile.sh

# Run all tests
./run_tests.sh

# Run specific tests
java -jar lib/junit-platform-console-standalone-1.10.1.jar \
  --class-path "bin:lib/*" \
  --select-class YourTestClass
```

### 4. Commit Changes

```bash
# Stage changes
git add .

# Commit with clear message
git commit -m "Add feature: brief description

Detailed explanation of what changed and why.

Fixes #123"
```

**Commit Message Format**:
```
<type>: <subject>

<body>

<footer>
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `test`: Tests
- `refactor`: Code refactoring
- `perf`: Performance improvement
- `chore`: Maintenance

**Example**:
```
feat: Add edge frequency analysis optimization

Implements EdgeFrequencyAnalyzer that prioritizes rare edges
during piece selection, reducing search space by ~15%.

- Added EdgeFrequencyAnalyzer class
- Integrated into EternitySolver
- Added 12 unit tests
- Updated documentation

Fixes #42
```

### 5. Push Changes

```bash
git push origin feature/my-awesome-feature
```

### 6. Create Pull Request

1. Go to GitHub
2. Click "New Pull Request"
3. Select your branch
4. Fill out PR template
5. Submit

---

## Coding Standards

### Java Style Guide

Follow standard Java conventions:

#### Naming

```java
// Classes and Interfaces: PascalCase
public class EdgeAnalyzer { }
public interface Optimizer { }

// Methods and variables: camelCase
public void analyzePiece() { }
private int pieceCount;

// Constants: UPPER_SNAKE_CASE
public static final int MAX_DEPTH = 1000;

// Packages: lowercase
package solver.optimization;
```

#### Formatting

```java
// Braces on same line
if (condition) {
    doSomething();
}

// Indentation: 4 spaces (no tabs)
public void method() {
    if (condition) {
        for (int i = 0; i < 10; i++) {
            process(i);
        }
    }
}

// Line length: max 120 characters
// Break long lines at logical points
```

#### Comments

```java
/**
 * Javadoc for public API.
 *
 * @param board the puzzle board
 * @return true if solved
 */
public boolean solve(Board board) {
    // Inline comment for implementation details
    return false;
}
```

### Code Quality

#### DRY (Don't Repeat Yourself)

❌ **Bad**:
```java
if (row == 0 && col == 0) {
    // Corner logic
}
// ... later ...
if (row == 0 && col == 0) {
    // Same logic repeated
}
```

✅ **Good**:
```java
private boolean isTopLeftCorner(int row, int col) {
    return row == 0 && col == 0;
}

if (isTopLeftCorner(row, col)) {
    // Corner logic
}
```

#### Single Responsibility

Each class/method should have one clear purpose.

❌ **Bad**:
```java
public class PuzzleSolver {
    public void solveAndDisplayAndSave() {
        solve();
        display();
        save();
    }
}
```

✅ **Good**:
```java
public class PuzzleSolver {
    public boolean solve() { }
}

public class PuzzleDisplay {
    public void display(Board board) { }
}

public class PuzzleSaver {
    public void save(Board board) { }
}
```

#### Error Handling

```java
// Always handle exceptions appropriately
try {
    process();
} catch (IOException e) {
    logger.error("Failed to process: {}", e.getMessage(), e);
    throw new RuntimeException("Processing failed", e);
}
```

### Logging

Use SLF4J for logging:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClass {
    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);

    public void method() {
        logger.debug("Debug details: {}", variable);
        logger.info("Operation completed");
        logger.warn("Potential issue detected");
        logger.error("Error occurred", exception);
    }
}
```

**Guidelines**:
- Use parameterized logging: `logger.info("Value: {}", value)`
- Don't log sensitive data
- Use appropriate levels
- Don't use `System.out.println` (except in Main classes and visualization)

---

## Testing Guidelines

### Test Structure

```java
package solver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

public class MyFeatureTest {

    private MyFeature feature;

    @BeforeEach
    public void setUp() {
        // Initialize test fixtures
        feature = new MyFeature();
    }

    @AfterEach
    public void tearDown() {
        // Cleanup if needed
    }

    @Test
    public void testBasicFunctionality() {
        // Arrange
        int input = 5;

        // Act
        int result = feature.compute(input);

        // Assert
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

### Test Coverage

- **Aim for 80%+ coverage** for new code
- Test happy paths and edge cases
- Test error conditions
- Include integration tests for features

### Running Tests

```bash
# All tests
./run_tests.sh

# Specific test
java -jar lib/junit-platform-console-standalone-1.10.1.jar \
  --class-path "bin:lib/*" \
  --select-class MyFeatureTest

# Coverage report
java -cp "bin:lib/*" coverage.CodeCoverageReport
```

---

## Documentation

### Code Documentation

#### Javadoc

```java
/**
 * Solves the puzzle using backtracking with optimizations.
 *
 * <p>This method implements constraint propagation and singleton
 * detection to reduce the search space. Time complexity is
 * O(n!) in worst case, but typically much faster.
 *
 * <p>Example usage:
 * <pre>{@code
 * EternitySolver solver = new EternitySolver();
 * boolean solved = solver.solve(board, pieces);
 * }</pre>
 *
 * @param board the puzzle board (must not be null)
 * @param pieces map of available pieces (must not be null)
 * @return true if puzzle is solved, false otherwise
 * @throws IllegalArgumentException if board or pieces is null
 * @see #solveParallel(Board, Map, Map, int)
 */
public boolean solve(Board board, Map<Integer, Piece> pieces) {
    // Implementation
}
```

#### Inline Comments

```java
// Use inline comments for non-obvious logic
private void complexAlgorithm() {
    // First, compute the edge frequencies to prioritize rare edges
    Map<Integer, Integer> frequencies = computeFrequencies();

    // Then apply MRV heuristic to select the most constrained cell
    int[] cell = findMostConstrainedCell();

    // ...
}
```

### User Documentation

When adding features, update:

- `docs/USER_MANUAL.md` - User-facing documentation
- `docs/ARCHITECTURE.md` - If architecture changes
- `docs/DEVELOPER_GUIDE.md` - For developer-facing changes
- `README.md` - For major features

---

## Pull Request Process

### Before Submitting

✅ **Code Quality**
- [ ] Code follows style guide
- [ ] No compiler warnings
- [ ] No commented-out code
- [ ] No debug `System.out.println`

✅ **Testing**
- [ ] All existing tests pass
- [ ] New tests added for new features
- [ ] Edge cases covered
- [ ] Manual testing performed

✅ **Documentation**
- [ ] Javadoc updated
- [ ] User documentation updated
- [ ] Architecture docs updated (if needed)
- [ ] CHANGELOG updated

✅ **Git**
- [ ] Commits are atomic and well-described
- [ ] Branch is up-to-date with main
- [ ] No merge conflicts

### PR Template

```markdown
## Description
Brief description of what this PR does

## Motivation and Context
Why is this change needed? What problem does it solve?
Fixes #(issue number)

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to change)
- [ ] Documentation update

## How Has This Been Tested?
Describe the tests you ran to verify your changes.
- Test A
- Test B

## Checklist
- [ ] My code follows the code style of this project
- [ ] I have updated the documentation accordingly
- [ ] I have added tests to cover my changes
- [ ] All new and existing tests passed
- [ ] My changes generate no new warnings

## Screenshots (if appropriate)

## Additional Notes
Any additional information
```

### Review Process

1. **Automated Checks**: Tests must pass
2. **Code Review**: At least one maintainer reviews
3. **Feedback**: Address review comments
4. **Approval**: Maintainer approves
5. **Merge**: Maintainer merges

### After Merge

- Delete your feature branch
- Update your fork: `git pull upstream main`
- Celebrate! 🎉

---

## Reporting Bugs

### Before Reporting

1. **Search existing issues** to avoid duplicates
2. **Verify the bug** on the latest version
3. **Collect information**:
   - Version of Eternity Solver
   - Java version
   - Operating system
   - Steps to reproduce

### Bug Report Template

```markdown
**Describe the Bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Run command '...'
2. With input '...'
3. See error

**Expected Behavior**
What you expected to happen.

**Actual Behavior**
What actually happened.

**Environment**
- Eternity Solver Version: [e.g., 1.0.0]
- Java Version: [e.g., 11.0.12]
- OS: [e.g., macOS 12.0]

**Stack Trace or Error Messages**
```
Paste error messages here
```

**Additional Context**
Any other context about the problem.

**Possible Solution**
If you have ideas on how to fix it.
```

---

## Suggesting Features

### Feature Request Template

```markdown
**Is your feature request related to a problem?**
A clear description of the problem.

**Describe the solution you'd like**
What you want to happen.

**Describe alternatives you've considered**
Other solutions you've thought about.

**Additional context**
Any other context, screenshots, examples.

**Implementation Idea**
If you have thoughts on how to implement it.

**Would you like to implement this feature?**
- [ ] Yes, I can work on this
- [ ] No, just suggesting
```

---

## Recognition

Contributors will be recognized in:
- `CONTRIBUTORS.md` file
- Release notes
- Project documentation

Thank you for contributing! 🙏

---

## Questions?

- **Documentation**: See `docs/` folder
- **Development**: See `DEVELOPER_GUIDE.md`
- **Architecture**: See `ARCHITECTURE.md`
- **Contact**: Open an issue for questions

---

**Happy Contributing!** 🚀
