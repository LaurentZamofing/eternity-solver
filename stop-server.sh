#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo "🛑 Arrêt du serveur Spring Boot..."
echo ""

# Méthode 1 : Via PID file
if [ -f logs/server.pid ]; then
    SERVER_PID=$(cat logs/server.pid)
    if ps -p $SERVER_PID > /dev/null 2>&1; then
        kill $SERVER_PID 2>/dev/null
        sleep 2
        if ps -p $SERVER_PID > /dev/null 2>&1; then
            echo -e "${YELLOW}Force kill...${NC}"
            kill -9 $SERVER_PID 2>/dev/null
        fi
        rm logs/server.pid
        echo -e "${GREEN}✓ Serveur arrêté (PID: $SERVER_PID)${NC}"
    else
        echo "ℹ️  Serveur déjà arrêté (PID obsolète)"
        rm logs/server.pid
    fi
else
    # Méthode 2 : Via port
    if lsof -i :8080 > /dev/null 2>&1; then
        PID=$(lsof -ti :8080)
        kill $PID 2>/dev/null
        sleep 2
        echo -e "${GREEN}✓ Serveur arrêté (port 8080)${NC}"
    else
        echo "ℹ️  Aucun serveur sur le port 8080"
    fi
fi

echo ""
