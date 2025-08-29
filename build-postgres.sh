#!/bin/bash
set -e

echo "Building Orchestrator Framework with PostgreSQL support..."

echo ""
echo "Building Maven modules..."
mvn clean install -DskipTests

echo ""
echo "Building PostgreSQL Docker image..."
docker build -f Dockerfile.postgres -t payments-orchestrator-postgres:latest .

echo ""
echo "Build completed successfully!"
echo "Run: docker-compose -f docker-compose.postgres.yml up"