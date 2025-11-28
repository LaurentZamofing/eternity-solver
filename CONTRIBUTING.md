# Contributing to Eternity Puzzle Solver

Thank you for your interest in contributing! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Code Style Guide](#code-style-guide)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)
- [Issue Guidelines](#issue-guidelines)
- [Release Process](#release-process)

## Code of Conduct

### Our Standards

- Be respectful and inclusive
- Welcome newcomers and help them learn
- Accept constructive criticism gracefully
- Focus on what is best for the community
- Show empathy towards other community members

### Unacceptable Behavior

- Harassment, discrimination, or offensive comments
- Trolling, insulting comments, or personal attacks
- Publishing others' private information
- Any conduct that could reasonably be considered inappropriate

## Getting Started

### Prerequisites

- Java 11+ (JDK 11, 17, or 21)
- Maven 3.6+ (or use included wrapper)
- Git
- A GitHub account

### Fork & Clone

```bash
# Fork the repository on GitHub
# Then clone your fork
git clone https://github.com/YOUR-USERNAME/eternity-solver.git
cd eternity-solver

# Add upstream remote
git remote add upstream https://github.com/original-owner/eternity-solver.git
```

### Build & Test

```bash
# Build the project
./mvnw clean compile

# Run tests
./mvnw test

# Create JAR
./mvnw package
```

## Development Setup

### IDE Configuration

#### IntelliJ IDEA

1. Open project as Maven project
2. Enable annotation processing
3. Set Java SDK to 11+
4. Code style: Import from `.editorconfig`

#### Eclipse

1. Import as "Existing Maven Project"
2. Right-click project → Maven → Update Project
3. Set compiler compliance to 11

#### VS Code

1. Install Java Extension Pack
2. Install Maven for Java
3. Open project folder

### Project Structure

```
src/main/java/
├── cli/              # Command-line interface
├── model/            # Domain models
├── runner/           # Execution orchestration
├── solver/           # Core algorithms
│   ├── heuristics/   # Heuristic strategies
│   └── ...
└── util/             # Utilities

src/test/java/        # Tests (mirror main structure)
```

## Code Style Guide

### Java Conventions

```java
// Class names: PascalCase
public class EdgeCompatibilityIndex { }

// Method names: camelCase
public int calculateScore() { }

// Constants: UPPER_SNAKE_CASE
public static final int MAX_DEPTH = 100;

// Variables: camelCase
int pieceCount = 10;
```

### Documentation

```java
/**
 * Brief description (one sentence).
 * 
 * Detailed explanation if needed.
 * 
 * @param board the puzzle board
 * @param pieces map of available pieces
 * @return true if solution found
 */
public boolean solve(Board board, Map<Integer, Piece> pieces) {
    // Implementation
}
```

### Code Organization

- **One class per file**
- **Keep methods focused** (< 30 lines ideally)
- **Extract magic numbers** to constants
- **Avoid deep nesting** (max 3 levels)
- **Use meaningful names** (avoid abbreviations)

### Formatting

- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters max
- **Blank lines**: 1 between methods, 2 between classes
- **Braces**: K&R style (opening brace same line)

```java
public void example() {
    if (condition) {
        // code
    } else {
        // code
    }
}
```

## Testing Requirements

### Test Coverage

- **Maintain coverage above 90%**
- Write tests for all new features
- Update tests when modifying existing code
- Include edge cases and error conditions

### Test Structure

```java
@Test
@DisplayName("Should detect singleton when only one position valid")
void testSingletonDetection() {
    // Arrange
    Board board = new Board(3, 3);
    Map<Integer, Piece> pieces = createTestPieces();
    
    // Act
    boolean found = singletonDetector.detectAndPlace(board, pieces);
    
    // Assert
    assertTrue(found);
    assertFalse(board.isEmpty(0, 0));
}
```

### Running Tests

```bash
# All tests
./mvnw test

# Specific test class
./mvnw test -Dtest=EternitySolverTest

# Specific test method
./mvnw test -Dtest=EternitySolverTest#testSpecificMethod

# With coverage
./mvnw test jacoco:report
```

## Pull Request Process

### Branch Naming

```
feature/add-new-heuristic
fix/singleton-detection-bug
docs/update-algorithm-guide
refactor/simplify-domain-manager
```

### Commit Messages

```
Format: <type>: <description>

Types:
- feat: New feature
- fix: Bug fix
- docs: Documentation only
- style: Formatting, missing semicolons, etc.
- refactor: Code restructuring
- test: Adding tests
- chore: Maintenance tasks

Examples:
feat: Add LCV heuristic for piece ordering
fix: Correct edge matching in corner cells
docs: Update performance benchmarks in README
refactor: Extract symmetry logic to dedicated class
```

### PR Checklist

Before submitting a pull request:

- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Documentation updated
- [ ] Tests added/updated
- [ ] All tests pass (`./mvnw test`)
- [ ] No compiler warnings
- [ ] Branch up to date with main

### PR Description Template

```markdown
## Description
Brief description of changes

## Motivation
Why is this change needed?

## Changes Made
- Change 1
- Change 2
- Change 3

## Testing
How was this tested?

## Screenshots (if applicable)
Add screenshots for UI changes

## Checklist
- [ ] Tests pass
- [ ] Documentation updated
- [ ] Code reviewed
```

### Review Process

1. **Automated checks**: CI must pass
2. **Code review**: At least one approval required
3. **Testing**: Reviewer verifies functionality
4. **Merge**: Squash and merge to keep history clean

## Issue Guidelines

### Bug Reports

Use the bug report template:

```markdown
**Describe the bug**
Clear description of the bug

**To Reproduce**
Steps to reproduce:
1. Run command '...'
2. See error

**Expected behavior**
What should happen

**Environment**
- OS: [e.g. Ubuntu 20.04]
- Java version: [e.g. OpenJDK 11]
- Solver version: [e.g. 1.0.0]

**Additional context**
Any other relevant information
```

### Feature Requests

```markdown
**Problem Statement**
What problem does this solve?

**Proposed Solution**
How should it work?

**Alternatives Considered**
Other approaches you've thought about

**Additional Context**
Mockups, examples, etc.
```

### Questions

- Search existing issues first
- Use discussion forum for general questions
- Provide context and what you've tried

## Release Process

### Versioning

We use [Semantic Versioning](https://semver.org/):

- **MAJOR**: Incompatible API changes
- **MINOR**: Backwards-compatible functionality
- **PATCH**: Backwards-compatible bug fixes

### Release Checklist

1. Update version in `pom.xml`
2. Update `CHANGELOG.md`
3. Run full test suite
4. Build and test JAR
5. Create git tag: `git tag v1.0.0`
6. Push tag: `git push origin v1.0.0`
7. GitHub Actions creates release automatically

### Changelog Format

```markdown
## [1.1.0] - 2024-12-01

### Added
- New LCV heuristic implementation
- Docker support for easy deployment

### Changed
- Improved singleton detection performance
- Updated documentation

### Fixed
- Edge matching bug in corner cells
- Memory leak in parallel search

### Deprecated
- Old configuration format (use new format)
```

## Additional Resources

- [README.md](README.md) - Project overview
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - System design
- [docs/ALGORITHM_GUIDE.md](docs/ALGORITHM_GUIDE.md) - Algorithm details
- [GitHub Issues](https://github.com/owner/eternity-solver/issues) - Bug tracker

## Getting Help

- **Issues**: For bugs and feature requests
- **Discussions**: For questions and general discussion
- **Email**: maintainer@example.com (for sensitive issues)

## Recognition

Contributors will be recognized in:
- README.md contributors section
- Release notes
- Git commit history

Thank you for contributing! 🎉
