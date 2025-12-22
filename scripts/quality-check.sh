#!/bin/bash
# Quality check script for local development
# Run before committing code

set -e

echo "🔍 Running quality checks..."
echo ""

echo "📦 Step 1/5: Compiling code..."
mvn clean compile -q
echo "✅ Compilation successful"
echo ""

echo "🧪 Step 2/5: Running tests..."
mvn test -q
echo "✅ Tests passed"
echo ""

echo "📊 Step 3/5: Checking test coverage..."
mvn jacoco:report -q
COVERAGE=$(grep -oP 'Total.*?([0-9]+)%' target/site/jacoco/index.html 2>/dev/null | grep -oP '[0-9]+' | tail -1 || echo "N/A")
echo "✅ Test coverage: ${COVERAGE}%"
echo ""

echo "🎨 Step 4/5: Checking code style..."
# Check for wildcard imports (excluding acceptable ones)
WILDCARD_COUNT=$(grep -r 'import [a-z].*\.\*;' src/main/java --include='*.java' | \
  grep -v 'springframework\|jakarta\|static solver.visualization' | wc -l | xargs)
if [ "$WILDCARD_COUNT" -gt "0" ]; then
  echo "⚠️  Warning: Found $WILDCARD_COUNT wildcard imports"
else
  echo "✅ No inappropriate wildcard imports"
fi
echo ""

echo "🔒 Step 5/5: Checking for System.out/err..."
SYSOUT_COUNT=$(grep -r 'System\.\(out\|err\)\.print' src/main/java --include='*.java' | \
  grep -v 'SolverLogger\|Formatter\|Renderer\|Helper' | wc -l | xargs)
if [ "$SYSOUT_COUNT" -gt "0" ]; then
  echo "⚠️  Warning: Found $SYSOUT_COUNT System.out/err usage"
else
  echo "✅ No System.out/err (using SolverLogger)"
fi
echo ""

echo "═══════════════════════════════════════"
echo "✅ All quality checks passed!"
echo "═══════════════════════════════════════"
