#!/bin/bash
set -e

echo "Building Orchestrator Framework with MongoDB support..."

echo ""
echo "Building Maven modules..."
mvn clean install -DskipTests

echo ""
echo "Building MongoDB Docker image..."
docker build -f Dockerfile.mongo -t payments-orchestrator-mongo:latest .

echo ""
echo "Build completed successfully!"
echo "Run: docker-compose -f docker-compose.mongo.yml up"