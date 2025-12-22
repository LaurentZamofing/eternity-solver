#!/bin/bash
# Pre-commit hook - runs quality checks before allowing commit
# To install: cp scripts/pre-commit-check.sh .git/hooks/pre-commit

echo "🔍 Running pre-commit checks..."

# Check 1: Wildcard imports
echo "Checking wildcard imports..."
WILDCARD_COUNT=$(git diff --cached --name-only | grep '\.java$' | xargs grep -h 'import [a-z].*\.\*;' 2>/dev/null | \
  grep -v 'springframework\|jakarta\|static solver.visualization' | wc -l | xargs)
if [ "$WILDCARD_COUNT" -gt "0" ]; then
  echo "❌ Found $WILDCARD_COUNT wildcard imports in staged files"
  echo "   Run: python3 fix_wildcard_imports.py"
  exit 1
fi

# Check 2: System.out/err
echo "Checking System.out/err usage..."
SYSOUT_COUNT=$(git diff --cached --name-only | grep '\.java$' | xargs grep -h 'System\.\(out\|err\)\.print' 2>/dev/null | \
  grep -v 'SolverLogger\|Formatter\|Renderer' | wc -l | xargs)
if [ "$SYSOUT_COUNT" -gt "0" ]; then
  echo "❌ Found $SYSOUT_COUNT System.out/err calls in staged files"
  echo "   Use SolverLogger instead"
  exit 1
fi

# Check 3: Compilation
echo "Checking compilation..."
if ! mvn compile -q 2>/dev/null; then
  echo "❌ Code does not compile"
  exit 1
fi

echo "✅ All pre-commit checks passed!"
