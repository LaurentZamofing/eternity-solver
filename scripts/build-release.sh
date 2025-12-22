#!/bin/bash
# Build release artifacts

set -e

VERSION=${1:-"1.0.0"}

echo "🏗️  Building Eternity Solver v${VERSION}..."
echo ""

echo "Step 1/4: Clean and compile..."
mvn clean compile -q

echo "Step 2/4: Run all tests..."
mvn test -q

echo "Step 3/4: Package JAR..."
mvn package -DskipTests -q

echo "Step 4/4: Generate Javadoc..."
mvn javadoc:javadoc -q

echo ""
echo "✅ Build complete!"
echo ""
echo "Artifacts:"
echo "  - JAR: target/eternity-solver-${VERSION}.jar"
echo "  - Javadoc: target/site/apidocs/"
echo "  - Test reports: target/surefire-reports/"
echo ""
