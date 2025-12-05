#!/bin/bash

# Eternity Solver - Monitoring System Startup Script
# This script starts the monitoring backend and frontend

set -e

echo "╔═══════════════════════════════════════════════════════╗"
echo "║   Eternity Solver - Monitoring System Launcher       ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check prerequisites
echo -e "${BLUE}[1/5]${NC} Checking prerequisites..."

if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java not found. Please install Java 11+${NC}"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}✗ Maven not found. Please install Maven 3.6+${NC}"
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo -e "${RED}✗ Node.js not found. Please install Node.js 18+${NC}"
    exit 1
fi

echo -e "${GREEN}✓ All prerequisites met${NC}"
echo ""

# Build backend
echo -e "${BLUE}[2/5]${NC} Building backend..."
mvn clean compile -DskipTests > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Backend compiled successfully${NC}"
else
    echo -e "${RED}✗ Backend compilation failed${NC}"
    exit 1
fi
echo ""

# Install frontend dependencies if needed
echo -e "${BLUE}[3/5]${NC} Checking frontend dependencies..."
if [ ! -d "frontend/node_modules" ]; then
    echo "Installing frontend dependencies..."
    cd frontend && npm install && cd ..
    echo -e "${GREEN}✓ Frontend dependencies installed${NC}"
else
    echo -e "${GREEN}✓ Frontend dependencies already installed${NC}"
fi
echo ""

# Start backend in background
echo -e "${BLUE}[4/5]${NC} Starting monitoring backend..."
mvn spring-boot:run > logs/backend.log 2>&1 &
BACKEND_PID=$!

# Wait for backend to start
echo "Waiting for backend to start on port 8080..."
for i in {1..30}; do
    if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Backend started successfully (PID: $BACKEND_PID)${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}✗ Backend failed to start within 30 seconds${NC}"
        kill $BACKEND_PID 2>/dev/null
        exit 1
    fi
    sleep 1
done
echo ""

# Start frontend
echo -e "${BLUE}[5/5]${NC} Starting frontend..."
cd frontend
npm run dev > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..

sleep 2
echo -e "${GREEN}✓ Frontend started successfully (PID: $FRONTEND_PID)${NC}"
echo ""

# Save PIDs
mkdir -p logs
echo $BACKEND_PID > logs/backend.pid
echo $FRONTEND_PID > logs/frontend.pid

# Print status
echo "╔═══════════════════════════════════════════════════════╗"
echo "║              🚀 System Started Successfully!          ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""
echo -e "${GREEN}Backend:${NC}  http://localhost:8080"
echo -e "${GREEN}Frontend:${NC} http://localhost:3000"
echo -e "${GREEN}API Docs:${NC} http://localhost:8080/api/health"
echo ""
echo -e "${YELLOW}Process IDs:${NC}"
echo "  Backend:  $BACKEND_PID"
echo "  Frontend: $FRONTEND_PID"
echo ""
echo -e "${YELLOW}Logs:${NC}"
echo "  Backend:  logs/backend.log"
echo "  Frontend: logs/frontend.log"
echo ""
echo -e "${BLUE}To stop the system:${NC} ./stop-monitoring.sh"
echo ""
echo "Press Ctrl+C to view logs (processes will continue in background)"
echo ""

# Follow logs
tail -f logs/backend.log logs/frontend.log
