#!/bin/bash
# Script de compilation pour le projet Eternity Solver
# Compile tous les fichiers sources avec les dépendances

set -e

echo "═══════════════════════════════════════════════════════"
echo "Compilation Eternity Solver"
echo "═══════════════════════════════════════════════════════"
echo

# Créer le répertoire bin si nécessaire
mkdir -p bin

# Compiler les sources
echo "1. Compilation des sources..."
javac -d bin -sourcepath src -cp "lib/*" \
    $(find src -name "*.java")

if [ $? -eq 0 ]; then
    echo "   ✓ Sources compilées avec succès"
else
    echo "   ✗ Erreur de compilation"
    exit 1
fi

# Copier les ressources
echo
echo "2. Copie des ressources..."
if [ -d "src/main/resources" ]; then
    cp -r src/main/resources/* bin/ 2>/dev/null || true
    echo "   ✓ Ressources copiées"
else
    mkdir -p src/main/resources
    echo "   ⚠ Pas de ressources à copier"
fi

echo
echo "═══════════════════════════════════════════════════════"
echo "✓ Compilation terminée avec succès"
echo "═══════════════════════════════════════════════════════"
echo
echo "Pour exécuter:"
echo "  java -cp \"bin:lib/*\" MainCLI --help"
echo
