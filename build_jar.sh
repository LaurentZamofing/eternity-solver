#!/bin/bash

# build_jar.sh - Build executable JAR with Maven Shade Plugin
# Usage: ./build_jar.sh

set -e

VERSION="1.0.0"
JAR_NAME="eternity-solver-${VERSION}.jar"

echo "════════════════════════════════════════════════════════"
echo "Building Eternity Solver JAR v${VERSION} (Maven)"
echo "════════════════════════════════════════════════════════"
echo ""

# Step 1: Build with Maven
echo "1. Building with Maven (compile + test + package)..."
if [ -f "./mvnw" ]; then
    ./mvnw clean package
else
    mvn clean package
fi
echo "   ✓ Maven build complete"

# Step 2: Check if JAR exists
echo ""
echo "2. Verifying JAR..."
if [ -f "target/${JAR_NAME}" ]; then
    echo "   ✓ JAR found: target/${JAR_NAME}"
else
    echo "   ✗ JAR not found!"
    exit 1
fi

# Step 3: Test JAR
echo ""
echo "3. Testing JAR..."
java -jar "target/${JAR_NAME}" --version > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✓ JAR works correctly"
else
    echo "   ✗ JAR test failed"
    exit 1
fi

# Step 4: Get JAR size
echo ""
echo "4. JAR information..."
JAR_SIZE=$(du -h "target/${JAR_NAME}" | cut -f1)
echo "   Size: ${JAR_SIZE}"
echo "   Location: $(pwd)/target/${JAR_NAME}"

# Step 5: Copy to root (optional, for convenience)
echo ""
echo "5. Copying JAR to project root..."
cp "target/${JAR_NAME}" "./${JAR_NAME}"
echo "   ✓ JAR copied to: $(pwd)/${JAR_NAME}"

echo ""
echo "════════════════════════════════════════════════════════"
echo "✓ Build Complete!"
echo "════════════════════════════════════════════════════════"
echo ""
echo "Executable JAR: ${JAR_NAME}"
echo "Size: ${JAR_SIZE}"
echo ""
echo "Usage:"
echo "  java -jar ${JAR_NAME} --help"
echo "  java -jar ${JAR_NAME} example_3x3"
echo "  java -jar ${JAR_NAME} -v -p example_4x4"
echo ""
