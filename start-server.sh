#!/bin/bash

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║          🖥️  DÉMARRAGE SERVEUR SPRING BOOT                          ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""

# Créer répertoire logs
mkdir -p logs

# Vérifier si déjà en cours
if lsof -i :8080 > /dev/null 2>&1; then
    echo "⚠️  Port 8080 déjà utilisé ! Un serveur tourne peut-être déjà."
    echo ""
    read -p "Voulez-vous le tuer et relancer ? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        PID=$(lsof -ti :8080)
        kill $PID 2>/dev/null
        sleep 2
        echo "✓ Ancien serveur arrêté"
    else
        echo "Annulé."
        exit 1
    fi
fi

# Compiler
echo -e "${BLUE}📦 Compilation...${NC}"
mvn compile -q
echo -e "${GREEN}✓ Compilé${NC}"
echo ""

# Lancer le serveur
echo -e "${BLUE}🚀 Démarrage du serveur Spring Boot...${NC}"
echo ""
echo "Port     : 8080"
echo "API      : http://localhost:8080/api"
echo "Swagger  : http://localhost:8080/swagger-ui.html"
echo "H2 Console: http://localhost:8080/h2-console"
echo ""
echo "Logs     : logs/server.log"
echo "Arrêter  : Ctrl+C ou ./stop-server.sh"
echo ""
echo "═══════════════════════════════════════════════════════════════════════"
echo ""

# Lancer (bloquant ou background)
read -p "Lancer en background (y) ou foreground/terminal (n) ? " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    # Background
    mvn spring-boot:run > logs/server.log 2>&1 &
    SERVER_PID=$!
    echo "$SERVER_PID" > logs/server.pid

    echo -e "${GREEN}✓ Serveur démarré en background (PID: $SERVER_PID)${NC}"
    echo ""
    echo "Voir les logs : tail -f logs/server.log"
    echo "Arrêter      : ./stop-server.sh"
    echo ""

    # Attendre démarrage
    echo "⏳ Attente du démarrage (10 secondes)..."
    sleep 10

    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Serveur opérationnel !${NC}"
    else
        echo "⚠️  Le serveur démarre encore... Patientez."
    fi
    echo ""
else
    # Foreground
    echo -e "${GREEN}✓ Serveur démarré (Ctrl+C pour arrêter)${NC}"
    echo ""
    mvn spring-boot:run
fi
