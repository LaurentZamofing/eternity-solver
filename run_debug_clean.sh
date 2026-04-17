#!/bin/bash
# Script pour lancer le solver en mode debug step-by-step FROM SCRATCH
# Supprime les sauvegardes avant de démarrer
# Usage: ./run_debug_clean.sh

CONFIG_FILE="data/eternity2/eternity2_p01_ascending.txt"
CONFIG_NAME="eternity2_p01_ascending"

echo "🧹 Nettoyage complet des sauvegardes et métadonnées..."
rm -rf "saves/eternity2/${CONFIG_NAME}"
mkdir -p "saves/eternity2/${CONFIG_NAME}"
echo "✅ Sauvegardes et métadonnées supprimées"
echo "✅ Répertoire recréé vide"
echo ""

echo "🔨 Compilation..."
mvn compile -q

if [ $? -ne 0 ]; then
    echo "❌ Erreur de compilation"
    exit 1
fi

echo "✅ Compilation réussie"
echo ""
echo "🚀 Lancement du solver en mode step-by-step FROM SCRATCH..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Configuration: ${CONFIG_NAME}"
echo "Mode: DEBUG STEP-BY-STEP activé"
echo "État: FRESH START (0 pièces)"
echo ""
echo "À chaque étape, le programme s'arrêtera."
echo "Appuyez sur ENTER pour continuer ou tapez 'q' pour désactiver les pauses."
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Run with the configuration file
java -cp "target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
  app.MainParallel "${CONFIG_FILE}"
