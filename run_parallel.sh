#!/bin/bash

# Script de lancement du résolveur parallèle Eternity II
# Usage: ./run_parallel.sh [nombre_de_threads]

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Vérifier que le code est compilé
if [ ! -f "bin/MainParallel.class" ]; then
    echo -e "${YELLOW}⚠️  Code non compilé, compilation en cours...${NC}"
    javac -d bin -sourcepath src src/MainParallel.java
    if [ $? -ne 0 ]; then
        echo -e "${RED}✗ Erreur de compilation${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Compilation réussie${NC}"
    echo
fi

# Nombre de threads (par défaut: nombre de CPUs)
NUM_THREADS=${1:-$(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 4)}

echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║          LANCEMENT DU RÉSOLVEUR PARALLÈLE                        ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════════╝${NC}"
echo
echo -e "  ⚙️  Threads: ${YELLOW}${NUM_THREADS}${NC}"
echo -e "  📂 Répertoire: ${YELLOW}$(pwd)${NC}"
echo
echo -e "${YELLOW}💡 Conseil: Utilisez Ctrl+C pour arrêter proprement${NC}"
echo
echo -e "═════════════════════════════════════════════════════════════════════"
echo

# Lancer le résolveur
java -cp bin MainParallel "$NUM_THREADS"

EXIT_CODE=$?

echo
echo -e "═════════════════════════════════════════════════════════════════════"
if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✓ Résolveur terminé normalement${NC}"
else
    echo -e "${YELLOW}⚠️  Résolveur interrompu (code: $EXIT_CODE)${NC}"
fi
echo

exit $EXIT_CODE
