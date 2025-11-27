#!/bin/bash

# Script pour lancer le test de validation 6x6
# Ce script compile et exécute le puzzle de validation

echo "╔════════════════════════════════════════════════════════╗"
echo "║        Compilation du test de validation 6×6          ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# Compilation des sources
echo "→ Compilation des classes principales..."
javac -d bin -cp bin src/model/*.java src/util/*.java src/solver/*.java

if [ $? -ne 0 ]; then
    echo "✗ Erreur de compilation des sources"
    exit 1
fi

echo "→ Compilation du test..."
javac -cp bin -d bin test/TestValidation6x6.java

if [ $? -ne 0 ]; then
    echo "✗ Erreur de compilation du test"
    exit 1
fi

echo "✓ Compilation réussie"
echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║        Lancement du test de validation 6×6            ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# Exécution
java -cp bin TestValidation6x6
