#!/bin/bash
set -e

echo "🔍 Validating Event Orchestrator Setup"
echo "====================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

echo "✅ Docker is running"

# Build everything
echo "📦 Building project..."
mvn clean package -DskipTests -q

echo "✅ Maven build successful"

# Check JAR files exist
if [ ! -f "payments-orchestrator-example/target/payments-orchestrator-example-1.0.0.jar" ]; then
    echo "❌ PostgreSQL example JAR not found"
    exit 1
fi

if [ ! -f "payments-orchestrator-mongo-example/target/payments-orchestrator-mongo-example-1.0.0.jar" ]; then
    echo "❌ MongoDB example JAR not found"
    exit 1
fi

if [ ! -f "load-test/target/kafka-load-generator-1.0.0.jar" ]; then
    echo "❌ Load test generator JAR not found"
    exit 1
fi

echo "✅ All JAR files present"

# Build Docker images
echo "🐳 Building Docker images..."
docker build -f Dockerfile.postgres -t payments-orchestrator-postgres:test . > /dev/null
docker build -f Dockerfile.mongo -t payments-orchestrator-mongo:test . > /dev/null

echo "✅ Docker images built"

# Test basic functionality
echo "🧪 Testing basic setup..."
cd load-test

# Start minimal infrastructure
docker-compose -f docker-compose-load-test.yml up -d zookeeper kafka > /dev/null

# Wait for Kafka
echo "   Waiting for Kafka..."
timeout 60s bash -c 'while ! docker exec $(docker-compose -f docker-compose-load-test.yml ps -q kafka) kafka-topics --bootstrap-server localhost:9092 --list &>/dev/null; do sleep 2; done' || {
    echo "❌ Kafka failed to start"
    docker-compose -f docker-compose-load-test.yml down
    exit 1
}

# Create test topic
docker exec $(docker-compose -f docker-compose-load-test.yml ps -q kafka) kafka-topics --bootstrap-server localhost:9092 --create --topic test-topic --partitions 1 --replication-factor 1 > /dev/null 2>&1

# Test load generator with minimal load
echo "   Testing load generator..."
export LOAD_TEST_RECORDS=10
export LOAD_TEST_THREADS=1
export LOAD_TEST_RATE_LIMIT=10

docker-compose -f docker-compose-load-test.yml run --rm -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 -e LOAD_TEST_RECORDS=10 -e LOAD_TEST_THREADS=1 -e LOAD_TEST_RATE_LIMIT=10 kafka-load-generator --topic test-topic > /dev/null 2>&1

# Clean up
docker-compose -f docker-compose-load-test.yml down > /dev/null

cd ..

echo "✅ Basic functionality test passed"

echo ""
echo "🎉 Setup Validation Complete!"
echo "============================="
echo ""
echo "✅ All systems are ready for load testing!"
echo ""
echo "Next steps:"
echo "1. Run end-to-end test: ./test-end-to-end.sh"
echo "2. Run full load test: ./load-test/run-load-test.sh"
echo "3. Custom load test: LOAD_TEST_RECORDS=1000000 ./load-test/run-load-test.sh"
echo ""
echo "Load test options:"
echo "- LOAD_TEST_RECORDS: Number of records to send (default: 1,000,000)"
echo "- LOAD_TEST_THREADS: Producer threads (default: 10)"
echo "- LOAD_TEST_RATE: Records per second (default: 10,000)"
echo "- TEST_DURATION: Duration in minutes (0 = record-bound, >0 = time-bound)"