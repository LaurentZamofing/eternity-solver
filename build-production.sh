#!/bin/bash

# Eternity Solver - Production Build Script
# Builds a single JAR with embedded frontend

set -e

echo "╔═══════════════════════════════════════════════════════╗"
echo "║   Eternity Solver - Production Build                 ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

# Build frontend
echo -e "${BLUE}[1/3]${NC} Building frontend..."
cd frontend
npm run build
cd ..
echo -e "${GREEN}✓ Frontend built to src/main/resources/static/${NC}"
echo ""

# Build backend
echo -e "${BLUE}[2/3]${NC} Building backend with embedded frontend..."
mvn clean package -DskipTests
echo -e "${GREEN}✓ Backend JAR created${NC}"
echo ""

# Test the build
echo -e "${BLUE}[3/3]${NC} Testing production build..."
JAR_FILE="target/eternity-solver-1.0.0-monitoring.jar"
if [ -f "$JAR_FILE" ]; then
    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    echo -e "${GREEN}✓ Production JAR ready: $JAR_FILE ($JAR_SIZE)${NC}"
else
    echo -e "\033[0;31m✗ JAR file not found${NC}"
    exit 1
fi
echo ""

# Instructions
echo "╔═══════════════════════════════════════════════════════╗"
echo "║              🎉 Build Complete!                       ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""
echo "To run the production build:"
echo ""
echo "  java -jar $JAR_FILE"
echo ""
echo "Then access at: http://localhost:8080"
echo ""
echo "Notes:"
echo "  - Frontend is embedded in the JAR"
echo "  - Single port (8080) for both backend + frontend"
echo "  - No need for separate frontend server"
echo ""
