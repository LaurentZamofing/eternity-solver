#!/bin/bash

# Eternity II Solver - Test Runner
# Compiles and runs JUnit 5 tests for refactored classes

set -e

echo "=== Eternity II Solver - Test Suite ==="
echo

# Clean previous test build
echo "1. Cleaning test build..."
rm -rf test-bin
mkdir -p test-bin

# Compile tests
echo "2. Compiling test classes..."
javac -d test-bin \
    -cp "bin:lib/*" \
    -sourcepath test \
    $(find test -name "*.java")

if [ $? -eq 0 ]; then
    echo "   ✓ All test classes compiled successfully"
else
    echo "   ✗ Compilation failed"
    exit 1
fi

echo

# Run tests
echo "3. Running JUnit 5 tests..."
echo
java -jar lib/junit-platform-console-standalone-1.10.1.jar \
    --class-path "test-bin:bin:lib/slf4j-api-2.0.9.jar:lib/logback-classic-1.4.11.jar:lib/logback-core-1.4.11.jar" \
    --scan-class-path

echo
echo "=== Test run complete ==="
