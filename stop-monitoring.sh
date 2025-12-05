#!/bin/bash

# Eternity Solver - Monitoring System Stop Script

echo "╔═══════════════════════════════════════════════════════╗"
echo "║   Eternity Solver - Stopping Monitoring System       ║"
echo "╚═══════════════════════════════════════════════════════╝"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Read PIDs
if [ -f logs/backend.pid ]; then
    BACKEND_PID=$(cat logs/backend.pid)
    if kill -0 $BACKEND_PID 2>/dev/null; then
        echo -e "${YELLOW}Stopping backend (PID: $BACKEND_PID)...${NC}"
        kill $BACKEND_PID
        sleep 2
        if kill -0 $BACKEND_PID 2>/dev/null; then
            echo -e "${YELLOW}Force killing backend...${NC}"
            kill -9 $BACKEND_PID
        fi
        echo -e "${GREEN}✓ Backend stopped${NC}"
    else
        echo -e "${YELLOW}Backend already stopped${NC}"
    fi
    rm logs/backend.pid
else
    echo -e "${YELLOW}No backend PID file found${NC}"
fi

if [ -f logs/frontend.pid ]; then
    FRONTEND_PID=$(cat logs/frontend.pid)
    if kill -0 $FRONTEND_PID 2>/dev/null; then
        echo -e "${YELLOW}Stopping frontend (PID: $FRONTEND_PID)...${NC}"
        kill $FRONTEND_PID
        sleep 1
        if kill -0 $FRONTEND_PID 2>/dev/null; then
            echo -e "${YELLOW}Force killing frontend...${NC}"
            kill -9 $FRONTEND_PID
        fi
        echo -e "${GREEN}✓ Frontend stopped${NC}"
    else
        echo -e "${YELLOW}Frontend already stopped${NC}"
    fi
    rm logs/frontend.pid
else
    echo -e "${YELLOW}No frontend PID file found${NC}"
fi

echo ""
echo -e "${GREEN}✓ Monitoring system stopped${NC}"
echo ""
