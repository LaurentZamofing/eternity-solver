#!/bin/bash
# Compilation script for Eternity Solver using Maven
# Compiles all source files with dependencies

set -e

echo "═══════════════════════════════════════════════════════"
echo "Compilation Eternity Solver (Maven)"
echo "═══════════════════════════════════════════════════════"
echo

# Check if Maven wrapper exists
if [ -f "./mvnw" ]; then
    echo "Using Maven wrapper..."
    ./mvnw clean compile
else
    echo "Using system Maven..."
    mvn clean compile
fi

echo
echo "═══════════════════════════════════════════════════════"
echo "✓ Compilation completed successfully"
echo "═══════════════════════════════════════════════════════"
echo
echo "To run:"
echo "  ./mvnw exec:java -Dexec.mainClass=MainCLI -Dexec.args=\"--help\""
echo "  OR"
echo "  java -cp \"target/classes:lib/*\" MainCLI --help"
echo
