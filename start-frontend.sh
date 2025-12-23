#!/bin/bash

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║          🎨 DÉMARRAGE FRONTEND REACT                                 ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""

# Vérifier que frontend existe
if [ ! -d "frontend" ]; then
    echo "❌ Dossier frontend/ non trouvé !"
    exit 1
fi

# Vérifier si déjà en cours
if lsof -i :5173 > /dev/null 2>&1; then
    echo "⚠️  Port 5173 déjà utilisé ! Le frontend tourne peut-être déjà."
    echo ""
    read -p "Voulez-vous le tuer et relancer ? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        PID=$(lsof -ti :5173)
        kill $PID 2>/dev/null
        sleep 2
        echo "✓ Ancien frontend arrêté"
    else
        echo "Annulé."
        exit 1
    fi
fi

# Vérifier node_modules
if [ ! -d "frontend/node_modules" ]; then
    echo -e "${BLUE}📦 Installation des dépendances...${NC}"
    cd frontend
    npm install
    cd ..
    echo -e "${GREEN}✓ Dépendances installées${NC}"
    echo ""
fi

# Créer répertoire logs
mkdir -p logs

echo -e "${BLUE}🚀 Démarrage du frontend Vite + React...${NC}"
echo ""
echo "Port     : 5173"
echo "URL      : http://localhost:5173"
echo "Logs     : logs/frontend.log"
echo "Arrêter  : Ctrl+C ou ./stop-frontend.sh"
echo ""
echo -e "${YELLOW}Note: Le serveur backend doit tourner sur :8080${NC}"
echo "      Lancer avec : ./start-server.sh"
echo ""
echo "═══════════════════════════════════════════════════════════════════════"
echo ""

# Lancer (bloquant ou background)
read -p "Lancer en background (y) ou foreground/terminal (n) ? " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    # Background
    cd frontend
    npm run dev > ../logs/frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo "$FRONTEND_PID" > ../logs/frontend.pid
    cd ..

    echo -e "${GREEN}✓ Frontend démarré en background (PID: $FRONTEND_PID)${NC}"
    echo ""
    echo "Voir les logs : tail -f logs/frontend.log"
    echo "Arrêter      : ./stop-frontend.sh"
    echo ""

    # Attendre démarrage
    echo "⏳ Attente du démarrage (5 secondes)..."
    sleep 5

    echo -e "${GREEN}✓ Frontend opérationnel !${NC}"
    echo ""
    echo "Ouvrir : http://localhost:5173"
    echo ""
else
    # Foreground
    echo -e "${GREEN}✓ Frontend démarré (Ctrl+C pour arrêter)${NC}"
    echo ""
    cd frontend
    npm run dev
fi
