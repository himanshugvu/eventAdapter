#!/bin/bash
set -e

echo "Validating Event Orchestrator Framework Build..."

echo ""
echo "1. Testing Maven build..."
mvn clean compile -q
echo "✅ PASSED: Maven compilation successful"

echo ""
echo "2. Testing Maven package..."
mvn clean package -DskipTests -q
echo "✅ PASSED: Maven packaging successful"

echo ""
echo "3. Checking JAR files..."
if [ ! -f "payments-orchestrator-example/target/payments-orchestrator-example-1.0.0.jar" ]; then
    echo "❌ FAILED: PostgreSQL example JAR not found!"
    exit 1
fi
echo "✅ PASSED: PostgreSQL JAR exists"

if [ ! -f "payments-orchestrator-mongo-example/target/payments-orchestrator-mongo-example-1.0.0.jar" ]; then
    echo "❌ FAILED: MongoDB example JAR not found!"
    exit 1
fi
echo "✅ PASSED: MongoDB JAR exists"

echo ""
echo "4. Validating Docker build context..."
if [ ! -f "Dockerfile.postgres" ]; then
    echo "❌ FAILED: PostgreSQL Dockerfile missing!"
    exit 1
fi
echo "✅ PASSED: PostgreSQL Dockerfile exists"

if [ ! -f "Dockerfile.mongo" ]; then
    echo "❌ FAILED: MongoDB Dockerfile missing!"
    exit 1
fi
echo "✅ PASSED: MongoDB Dockerfile exists"

echo ""
echo "5. Checking deployment configurations..."
if [ ! -f "docker-compose.postgres.yml" ]; then
    echo "❌ FAILED: PostgreSQL Docker Compose missing!"
    exit 1
fi
echo "✅ PASSED: PostgreSQL Docker Compose exists"

if [ ! -f "docker-compose.mongo.yml" ]; then
    echo "❌ FAILED: MongoDB Docker Compose missing!"
    exit 1
fi
echo "✅ PASSED: MongoDB Docker Compose exists"

if [ ! -f "k8s/deployment.yaml" ]; then
    echo "❌ FAILED: Kubernetes deployment missing!"
    exit 1
fi
echo "✅ PASSED: Kubernetes manifests exist"

if [ ! -f "helm/orchestrator/Chart.yaml" ]; then
    echo "❌ FAILED: Helm chart missing!"
    exit 1
fi
echo "✅ PASSED: Helm chart exists"

echo ""
echo "🎉 All validations passed! The Event Orchestrator Framework is ready."
echo ""
echo "Next steps:"
echo "  PostgreSQL variant: ./build-postgres.sh && docker-compose -f docker-compose.postgres.yml up"
echo "  MongoDB variant:    ./build-mongo.sh && docker-compose -f docker-compose.mongo.yml up"