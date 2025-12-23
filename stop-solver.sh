#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo "🛑 Arrêt du solver Eternity II..."
echo ""

# Chercher le processus MainParallel
SOLVER_PID=$(ps aux | grep "app.MainParallel" | grep -v grep | awk '{print $2}')

if [ ! -z "$SOLVER_PID" ]; then
    echo "Arrêt du solver (PID: $SOLVER_PID)..."

    # Envoyer SIGTERM pour arrêt gracieux (sauvegarde)
    kill $SOLVER_PID 2>/dev/null

    # Attendre max 10 secondes pour sauvegarde
    for i in {1..10}; do
        if ! ps -p $SOLVER_PID > /dev/null 2>&1; then
            break
        fi
        echo "   Sauvegarde en cours... ($i/10)"
        sleep 1
    done

    # Force kill si toujours là
    if ps -p $SOLVER_PID > /dev/null 2>&1; then
        echo -e "${YELLOW}Force kill (pas de réponse)${NC}"
        kill -9 $SOLVER_PID 2>/dev/null
    fi

    echo -e "${GREEN}✓ Solver arrêté${NC}"
    echo ""
    echo "Sauvegardes dans : saves/eternity2/"
else
    echo "ℹ️  Aucun solver en cours d'exécution"
fi

echo ""
