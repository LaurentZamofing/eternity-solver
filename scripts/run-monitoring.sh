#!/bin/bash
# Start monitoring dashboard

echo "🚀 Starting Eternity Solver Monitoring Dashboard..."
echo ""
echo "Building..."
mvn package -DskipTests -q

echo "Starting Spring Boot application..."
echo "Dashboard will be available at: http://localhost:8080"
echo ""

java -jar target/eternity-solver-*.jar --spring.profiles.active=monitoring
