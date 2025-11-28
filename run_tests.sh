#!/bin/bash

# Eternity II Solver - Test Runner (Maven)
# Compiles and runs JUnit 5 tests

set -e

echo "=== Eternity II Solver - Test Suite (Maven) ==="
echo

# Check if Maven wrapper exists
if [ -f "./mvnw" ]; then
    echo "Using Maven wrapper to run tests..."
    ./mvnw test
else
    echo "Using system Maven to run tests..."
    mvn test
fi

echo
echo "=== Test run complete ==="
