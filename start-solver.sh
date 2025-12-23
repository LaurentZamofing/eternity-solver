#!/bin/bash

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║          🧩 DÉMARRAGE SOLVER ETERNITY II                             ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""

# Compiler
echo -e "${BLUE}📦 Compilation...${NC}"
mvn compile -q
echo -e "${GREEN}✓ Compilé${NC}"
echo ""

# Paramètres
echo "⚙️  Configuration du solver :"
echo ""

read -p "   Nombre de threads (défaut: tous les CPU = 16) : " threads
threads=${threads:-16}

read -p "   Minutes par configuration (défaut: 60) : " minutes
minutes=${minutes:-60}

echo ""
echo -e "${GREEN}✓ Configuration${NC}"
echo "   Threads  : $threads"
echo "   Timeout  : $minutes minutes par config"
echo ""

# Vérifier les configurations disponibles
CONFIGS=$(ls data/eternity2/*.txt 2>/dev/null | wc -l | tr -d ' ')
if [ "$CONFIGS" -gt 0 ]; then
    echo "📁 Configurations détectées : $CONFIGS"
    ls -1 data/eternity2/*.txt | sed 's/.*\//  - /'
else
    echo "⚠️  Aucune configuration trouvée dans data/eternity2/"
    echo "   Vérifiez que les fichiers .txt existent"
    exit 1
fi
echo ""

# Vérifier les sauvegardes existantes
SAVES=$(find saves/eternity2/ -name "best_*.txt" 2>/dev/null | wc -l | tr -d ' ')
if [ "$SAVES" -gt 0 ]; then
    echo "💾 Sauvegardes existantes : $SAVES fichiers best_*.txt"
    echo "   Le solver va reprendre où il s'était arrêté"
else
    echo "🆕 Pas de sauvegarde - Démarrage from scratch"
fi
echo ""

echo "═══════════════════════════════════════════════════════════════════════"
echo ""
echo -e "${BLUE}🚀 Lancement du solver...${NC}"
echo ""
echo "Logs        : Affichés directement + logs/solver.log"
echo "Sauvegardes : saves/eternity2/*/best_*.txt"
echo "Arrêter     : Ctrl+C (sauvegarde automatique)"
echo ""
echo -e "${YELLOW}Tip: Pour monitoring en temps réel, lancez aussi :${NC}"
echo "     ./start-server.sh (backend)"
echo "     ./start-frontend.sh (dashboard)"
echo "     Puis ouvrez : http://localhost:5173"
echo ""
echo "═══════════════════════════════════════════════════════════════════════"
echo ""

read -p "Appuyez sur Entrée pour démarrer..."

# Créer logs directory
mkdir -p logs

# Lancer le solver
echo ""
mvn exec:java -Dexec.mainClass="app.MainParallel" -Dexec.args="$threads $minutes" 2>&1 | tee logs/solver.log

echo ""
echo "🛑 Solver arrêté"
echo ""
echo "Voir les résultats : ls -lh saves/eternity2/*/best_*.txt"
echo ""
