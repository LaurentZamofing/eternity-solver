#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo "🛑 Arrêt du frontend React/Vite..."
echo ""

# Méthode 1 : Via PID file
if [ -f logs/frontend.pid ]; then
    FRONTEND_PID=$(cat logs/frontend.pid)
    if ps -p $FRONTEND_PID > /dev/null 2>&1; then
        kill $FRONTEND_PID 2>/dev/null
        sleep 2
        if ps -p $FRONTEND_PID > /dev/null 2>&1; then
            echo -e "${YELLOW}Force kill...${NC}"
            kill -9 $FRONTEND_PID 2>/dev/null
        fi
        rm logs/frontend.pid
        echo -e "${GREEN}✓ Frontend arrêté (PID: $FRONTEND_PID)${NC}"
    else
        echo "ℹ️  Frontend déjà arrêté (PID obsolète)"
        rm logs/frontend.pid
    fi
else
    # Méthode 2 : Via port
    if lsof -i :5173 > /dev/null 2>&1; then
        PID=$(lsof -ti :5173)
        kill $PID 2>/dev/null
        sleep 2
        echo -e "${GREEN}✓ Frontend arrêté (port 5173)${NC}"
    else
        echo "ℹ️  Aucun frontend sur le port 5173"
    fi
fi

echo ""
