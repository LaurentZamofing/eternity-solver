#!/bin/bash
# Test de la rotation automatique des puzzles (toutes les 10 minutes)

echo "=== Test de rotation des puzzles ==="
echo "Le programme va maintenant:"
echo "  - Sauvegarder l'état toutes les 1 minute"
echo "  - Changer de puzzle toutes les 10 minutes"
echo ""
echo "Appuyez sur Ctrl+C pour arrêter"
echo ""

java -cp bin MainSequential
