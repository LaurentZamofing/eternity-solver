#!/bin/bash

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║          🛑 ETERNITY II - ARRÊT SYSTÈME COMPLET                      ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""

# 1. Arrêter le serveur
if [ -f logs/server.pid ]; then
    SERVER_PID=$(cat logs/server.pid)
    if ps -p $SERVER_PID > /dev/null 2>&1; then
        echo -e "${YELLOW}🖥️  Arrêt du serveur (PID: $SERVER_PID)...${NC}"
        kill $SERVER_PID 2>/dev/null
        sleep 2
        if ps -p $SERVER_PID > /dev/null 2>&1; then
            echo -e "${RED}   ⚠️  Force kill du serveur${NC}"
            kill -9 $SERVER_PID 2>/dev/null
        fi
        rm logs/server.pid
        echo -e "${GREEN}   ✓ Serveur arrêté${NC}"
    else
        echo "   ℹ️  Serveur déjà arrêté"
        rm logs/server.pid
    fi
else
    # Chercher et tuer tous les process Spring Boot
    SPRING_PID=$(ps aux | grep "spring-boot:run" | grep -v grep | awk '{print $2}')
    if [ ! -z "$SPRING_PID" ]; then
        echo -e "${YELLOW}🖥️  Arrêt du serveur Spring Boot...${NC}"
        kill $SPRING_PID 2>/dev/null
        echo -e "${GREEN}   ✓ Serveur arrêté${NC}"
    else
        echo "   ℹ️  Serveur non trouvé"
    fi
fi
echo ""

# 2. Arrêter le frontend
if [ -f logs/frontend.pid ]; then
    FRONTEND_PID=$(cat logs/frontend.pid)
    if ps -p $FRONTEND_PID > /dev/null 2>&1; then
        echo -e "${YELLOW}🎨 Arrêt du frontend (PID: $FRONTEND_PID)...${NC}"
        kill $FRONTEND_PID 2>/dev/null
        sleep 2
        if ps -p $FRONTEND_PID > /dev/null 2>&1; then
            echo -e "${RED}   ⚠️  Force kill du frontend${NC}"
            kill -9 $FRONTEND_PID 2>/dev/null
        fi
        rm logs/frontend.pid
        echo -e "${GREEN}   ✓ Frontend arrêté${NC}"
    else
        echo "   ℹ️  Frontend déjà arrêté"
        rm logs/frontend.pid
    fi
else
    # Chercher et tuer Vite
    VITE_PID=$(ps aux | grep "vite" | grep -v grep | awk '{print $2}' | head -1)
    if [ ! -z "$VITE_PID" ]; then
        echo -e "${YELLOW}🎨 Arrêt du frontend Vite...${NC}"
        kill $VITE_PID 2>/dev/null
        echo -e "${GREEN}   ✓ Frontend arrêté${NC}"
    else
        echo "   ℹ️  Frontend non trouvé"
    fi
fi
echo ""

# 3. Arrêter le solver (s'il tourne en background)
SOLVER_PID=$(ps aux | grep "app.MainParallel" | grep -v grep | awk '{print $2}')
if [ ! -z "$SOLVER_PID" ]; then
    echo -e "${YELLOW}🧩 Arrêt du solver...${NC}"
    kill $SOLVER_PID 2>/dev/null
    echo -e "${GREEN}   ✓ Solver arrêté${NC}"
    echo ""
fi

# 4. Nettoyer les fichiers PID orphelins
rm -f logs/*.pid 2>/dev/null

echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║                    ✅ SYSTÈME ARRÊTÉ                                 ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""
echo "📊 Vos sauvegardes sont dans : saves/eternity2/"
echo "📄 Les logs sont dans : logs/"
echo ""
echo "Pour relancer : ./start-all.sh"
echo ""
