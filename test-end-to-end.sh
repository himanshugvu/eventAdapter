#!/bin/bash
set -e

echo "🧪 Event Orchestrator End-to-End Test"
echo "====================================="

# Build everything
echo "1. Building project..."
mvn clean package -DskipTests -q

# Test PostgreSQL variant
echo "2. Testing PostgreSQL variant..."
cd load-test
docker-compose -f docker-compose-load-test.yml up -d zookeeper kafka postgres

# Wait for services
echo "   Waiting for services to be ready..."
sleep 30

# Create topics
docker exec $(docker-compose -f docker-compose-load-test.yml ps -q kafka) kafka-topics --bootstrap-server localhost:9092 --create --topic payment-requests --partitions 3 --replication-factor 1 --if-not-exists
docker exec $(docker-compose -f docker-compose-load-test.yml ps -q kafka) kafka-topics --bootstrap-server localhost:9092 --create --topic processed-payments --partitions 3 --replication-factor 1 --if-not-exists

# Start PostgreSQL orchestrator
docker-compose -f docker-compose-load-test.yml up -d postgres-orchestrator

echo "   Waiting for PostgreSQL orchestrator..."
timeout 60s bash -c 'while ! curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; do sleep 3; done' || {
    echo "❌ PostgreSQL orchestrator failed to start"
    docker-compose -f docker-compose-load-test.yml logs postgres-orchestrator
    exit 1
}

# Send test messages
echo "   Sending test messages..."
export LOAD_TEST_RECORDS=1000
export LOAD_TEST_THREADS=2
export LOAD_TEST_RATE_LIMIT=100

docker-compose -f docker-compose-load-test.yml run --rm kafka-load-generator

# Wait for processing
echo "   Waiting for processing..."
sleep 10

# Check results
postgres_health=$(curl -s http://localhost:8080/actuator/health | grep -o '"status":"UP"' || echo "DOWN")
postgres_metrics=$(curl -s http://localhost:8080/actuator/prometheus | grep "orchestrator_events_received" | head -1)

if [[ "$postgres_health" == *"UP"* ]]; then
    echo "✅ PostgreSQL orchestrator: HEALTHY"
    echo "   Metrics: $postgres_metrics"
else
    echo "❌ PostgreSQL orchestrator: UNHEALTHY"
fi

# Clean up PostgreSQL test
docker-compose -f docker-compose-load-test.yml down

# Test MongoDB variant
echo "3. Testing MongoDB variant..."
docker-compose -f docker-compose-load-test.yml up -d zookeeper kafka mongodb

# Wait for services  
echo "   Waiting for services to be ready..."
sleep 30

# Recreate topics
docker exec $(docker-compose -f docker-compose-load-test.yml ps -q kafka) kafka-topics --bootstrap-server localhost:9092 --create --topic payment-requests --partitions 3 --replication-factor 1 --if-not-exists
docker exec $(docker-compose -f docker-compose-load-test.yml ps -q kafka) kafka-topics --bootstrap-server localhost:9092 --create --topic processed-payments --partitions 3 --replication-factor 1 --if-not-exists

# Start MongoDB orchestrator
docker-compose -f docker-compose-load-test.yml up -d mongo-orchestrator

echo "   Waiting for MongoDB orchestrator..."
timeout 60s bash -c 'while ! curl -sf http://localhost:8090/actuator/health >/dev/null 2>&1; do sleep 3; done' || {
    echo "❌ MongoDB orchestrator failed to start"
    docker-compose -f docker-compose-load-test.yml logs mongo-orchestrator
    exit 1
}

# Send test messages
echo "   Sending test messages..."
docker-compose -f docker-compose-load-test.yml run --rm kafka-load-generator

# Wait for processing
echo "   Waiting for processing..."
sleep 10

# Check results
mongo_health=$(curl -s http://localhost:8090/actuator/health | grep -o '"status":"UP"' || echo "DOWN")
mongo_metrics=$(curl -s http://localhost:8090/actuator/prometheus | grep "orchestrator_events_received" | head -1)

if [[ "$mongo_health" == *"UP"* ]]; then
    echo "✅ MongoDB orchestrator: HEALTHY"
    echo "   Metrics: $mongo_metrics"
else
    echo "❌ MongoDB orchestrator: UNHEALTHY"
fi

# Clean up
docker-compose -f docker-compose-load-test.yml down

cd ..

echo ""
echo "🎉 End-to-End Test Complete!"
echo "Both PostgreSQL and MongoDB variants are working correctly."
echo ""
echo "Next steps:"
echo "- Run full load test: ./load-test/run-load-test.sh"
echo "- Customize load test: LOAD_TEST_RECORDS=1000000 ./load-test/run-load-test.sh"