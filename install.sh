#!/bin/bash

# install.sh - Install Eternity Solver system-wide
# Usage: sudo ./install.sh

set -e

VERSION="1.0.0"
JAR_NAME="eternity-solver-${VERSION}.jar"
INSTALL_DIR="/usr/local/lib/eternity-solver"
BIN_DIR="/usr/local/bin"
WRAPPER_NAME="eternity-solver"

echo "════════════════════════════════════════════════════════"
echo "Eternity Solver Installation"
echo "════════════════════════════════════════════════════════"
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "⚠️  This script must be run as root (use sudo)"
    echo ""
    echo "Usage: sudo ./install.sh"
    exit 1
fi

# Check if JAR exists
if [ ! -f "$JAR_NAME" ]; then
    echo "✗ Error: ${JAR_NAME} not found"
    echo ""
    echo "Please build the JAR first:"
    echo "  ./build_jar.sh"
    exit 1
fi

# Step 1: Create installation directory
echo "1. Creating installation directory..."
mkdir -p "$INSTALL_DIR"
echo "   ✓ Directory created: $INSTALL_DIR"

# Step 2: Copy JAR
echo ""
echo "2. Installing JAR file..."
cp "$JAR_NAME" "$INSTALL_DIR/"
chmod 644 "$INSTALL_DIR/$JAR_NAME"
echo "   ✓ JAR installed: $INSTALL_DIR/$JAR_NAME"

# Step 3: Create wrapper script
echo ""
echo "3. Creating command-line wrapper..."
cat > "$BIN_DIR/$WRAPPER_NAME" << EOF
#!/bin/bash
# Eternity Solver wrapper script
exec java -jar "$INSTALL_DIR/$JAR_NAME" "\$@"
EOF

chmod 755 "$BIN_DIR/$WRAPPER_NAME"
echo "   ✓ Wrapper created: $BIN_DIR/$WRAPPER_NAME"

# Step 4: Verify installation
echo ""
echo "4. Verifying installation..."
if "$BIN_DIR/$WRAPPER_NAME" --version > /dev/null 2>&1; then
    echo "   ✓ Installation verified"
else
    echo "   ✗ Verification failed"
    exit 1
fi

echo ""
echo "════════════════════════════════════════════════════════"
echo "✓ Installation Complete!"
echo "════════════════════════════════════════════════════════"
echo ""
echo "Eternity Solver has been installed to:"
echo "  $INSTALL_DIR/$JAR_NAME"
echo ""
echo "You can now run it from anywhere:"
echo "  ${WRAPPER_NAME} --help"
echo "  ${WRAPPER_NAME} example_3x3"
echo "  ${WRAPPER_NAME} -v -p example_4x4"
echo ""
echo "To uninstall:"
echo "  sudo rm -rf $INSTALL_DIR"
echo "  sudo rm $BIN_DIR/$WRAPPER_NAME"
echo ""
