#!/bin/bash

# Test script for debug mode with tiny puzzle

echo "Testing debug backtracking mode..."
echo "=================================="
echo ""

# Run with debug enabled (logs will be at DEBUG level, but we'll see INFO at least)
timeout 30 java -Dlogback.configurationFile=src/main/resources/logback-debug.xml \
    -jar target/eternity-solver-1.0.0.jar \
    parallel \
    --configs puzzles/test_debug_3x3.txt \
    --threads 1 \
    --timeout 20

echo ""
echo "Test complete!"
