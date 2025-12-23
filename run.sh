#!/bin/bash
cd "$(dirname "$0")"
echo "🚀 Lancement du solver Eternity II..."
echo ""
mvn -q exec:java
