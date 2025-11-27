#!/bin/bash

# build_jar.sh - Build executable JAR with all dependencies
# Usage: ./build_jar.sh

set -e

VERSION="1.0.0"
JAR_NAME="eternity-solver-${VERSION}.jar"
BUILD_DIR="build"
MANIFEST="MANIFEST.MF"

echo "════════════════════════════════════════════════════════"
echo "Building Eternity Solver JAR v${VERSION}"
echo "════════════════════════════════════════════════════════"
echo ""

# Step 1: Clean and compile
echo "1. Cleaning and compiling..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

./compile.sh > /dev/null 2>&1
echo "   ✓ Compilation complete"

# Step 2: Extract dependencies
echo ""
echo "2. Extracting dependencies..."
cd "$BUILD_DIR"
for jar in ../lib/*.jar; do
    if [[ "$jar" != *"junit"* ]] && [[ "$jar" != *"test"* ]]; then
        jar xf "$jar" > /dev/null 2>&1
    fi
done
cd ..
echo "   ✓ Dependencies extracted"

# Step 3: Copy compiled classes
echo ""
echo "3. Copying compiled classes..."
cp -r bin/* "$BUILD_DIR/" 2>/dev/null || true
echo "   ✓ Classes copied"

# Step 4: Copy resources
echo ""
echo "4. Copying resources..."
if [ -d "src/main/resources" ]; then
    cp -r src/main/resources/* "$BUILD_DIR/" 2>/dev/null || true
fi
echo "   ✓ Resources copied"

# Step 5: Create manifest
echo ""
echo "5. Creating manifest..."
cat > "$BUILD_DIR/$MANIFEST" << EOF
Manifest-Version: 1.0
Main-Class: MainCLI
Implementation-Title: Eternity Solver
Implementation-Version: ${VERSION}
Implementation-Vendor: Eternity Solver Project
Built-By: ${USER}
Build-Date: $(date +"%Y-%m-%d %H:%M:%S")
EOF
echo "   ✓ Manifest created"

# Step 6: Create JAR
echo ""
echo "6. Creating JAR file..."
cd "$BUILD_DIR"
jar cfm "../${JAR_NAME}" "$MANIFEST" .
cd ..
echo "   ✓ JAR created: ${JAR_NAME}"

# Step 7: Test JAR
echo ""
echo "7. Testing JAR..."
java -jar "${JAR_NAME}" --version > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✓ JAR works correctly"
else
    echo "   ✗ JAR test failed"
    exit 1
fi

# Step 8: Get JAR size
echo ""
echo "8. JAR information..."
JAR_SIZE=$(du -h "${JAR_NAME}" | cut -f1)
echo "   Size: ${JAR_SIZE}"
echo "   Location: $(pwd)/${JAR_NAME}"

# Cleanup
echo ""
echo "9. Cleaning up..."
rm -rf "$BUILD_DIR"
echo "   ✓ Build directory cleaned"

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
