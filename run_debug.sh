#!/bin/bash
# Script pour lancer le solver en mode debug step-by-step
# Usage: ./run_debug.sh

# Compile first
echo "🔨 Compilation..."
mvn compile -q

if [ $? -ne 0 ]; then
    echo "❌ Erreur de compilation"
    exit 1
fi

echo "✅ Compilation réussie"
echo ""
echo "🚀 Lancement du solver en mode step-by-step..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Configuration: eternity2_p01_ascending"
echo "Mode: DEBUG STEP-BY-STEP activé"
echo ""
echo "À chaque étape, le programme s'arrêtera."
echo "Appuyez sur ENTER pour continuer ou tapez 'q' pour désactiver les pauses."
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Run with the configuration file
mvn exec:java -q \
  -Dexec.mainClass="app.MainParallel" \
  -Dexec.args="data/eternity2/eternity2_p01_ascending.txt"
