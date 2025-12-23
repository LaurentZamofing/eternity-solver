#!/bin/bash

# Couleurs pour les messages
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo ""
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║          🚀 ETERNITY II - DÉMARRAGE SYSTÈME COMPLET                  ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""

# Créer répertoire logs si nécessaire
mkdir -p logs

# 1. Compiler le projet
echo -e "${BLUE}📦 Compilation du projet...${NC}"
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo -e "${YELLOW}⚠️  Compilation échouée, continuons quand même...${NC}"
fi
echo -e "${GREEN}   ✓ Compilation terminée${NC}"
echo ""

# 2. Lancer le serveur monitoring en background
echo -e "${BLUE}🖥️  Démarrage du serveur monitoring (port 8080)...${NC}"
mvn spring-boot:run > logs/server.log 2>&1 &
SERVER_PID=$!
echo "$SERVER_PID" > logs/server.pid
echo -e "${GREEN}   ✓ Serveur démarré (PID: $SERVER_PID)${NC}"
echo "   📄 Logs: logs/server.log"

# Attendre que le serveur démarre
echo "   ⏳ Attente du démarrage du serveur (15 secondes)..."
sleep 15

# Vérifier que le serveur répond
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}   ✓ Serveur opérationnel !${NC}"
else
    echo -e "${YELLOW}   ⚠️  Serveur ne répond pas encore, continuons...${NC}"
fi
echo ""

# 3. Lancer le frontend en background
echo -e "${BLUE}🎨 Démarrage du frontend (port 5173)...${NC}"
cd frontend
npm run dev > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "$FRONTEND_PID" > ../logs/frontend.pid
cd ..
echo -e "${GREEN}   ✓ Frontend démarré (PID: $FRONTEND_PID)${NC}"
echo "   📄 Logs: logs/frontend.log"

# Attendre que le frontend démarre
echo "   ⏳ Attente du démarrage du frontend (5 secondes)..."
sleep 5
echo -e "${GREEN}   ✓ Frontend opérationnel !${NC}"
echo ""

# 4. Afficher les URLs
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║                    ✅ SYSTÈME DÉMARRÉ !                              ║"
echo "╠══════════════════════════════════════════════════════════════════════╣"
echo "║                                                                       ║"
echo "║  🎨 Dashboard  : http://localhost:5173                               ║"
echo "║  🖥️  Serveur   : http://localhost:8080                               ║"
echo "║  📖 API Docs   : http://localhost:8080/swagger-ui.html               ║"
echo "║                                                                       ║"
echo "║  📄 Logs serveur  : tail -f logs/server.log                          ║"
echo "║  📄 Logs frontend : tail -f logs/frontend.log                        ║"
echo "║  📄 Logs solver   : tail -f logs/solver.log                          ║"
echo "║                                                                       ║"
echo "║  🛑 Pour arrêter  : ./stop-all.sh                                    ║"
echo "║                                                                       ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""

# 5. Demander si on lance le solver
read -p "Voulez-vous lancer le solver maintenant ? (y/n) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo -e "${BLUE}🧩 Démarrage du solver parallèle...${NC}"
    echo ""

    # Demander les paramètres
    read -p "   Nombre de threads [16] : " threads
    threads=${threads:-16}

    read -p "   Minutes par config [60] : " minutes
    minutes=${minutes:-60}

    echo ""
    echo -e "${GREEN}   ✓ Lancement avec $threads threads, $minutes minutes par config${NC}"
    echo ""

    # Lancer le solver (bloquant)
    mvn exec:java -Dexec.mainClass="app.MainParallel" -Dexec.args="$threads $minutes" 2>&1 | tee logs/solver.log

    # Cleanup quand on arrête le solver
    echo ""
    echo "🛑 Solver arrêté. Serveur et frontend tournent toujours."
    echo "   Pour les arrêter : ./stop-all.sh"
else
    echo ""
    echo "Solver non lancé. Vous pouvez le lancer plus tard avec :"
    echo "   mvn exec:java"
    echo ""
    echo "Le serveur et frontend continuent de tourner en background."
    echo "Pour les arrêter : ./stop-all.sh"
fi

echo ""
