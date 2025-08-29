#!/bin/bash
set -e

echo "🚀 Starting Event Orchestrator Load Test"
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
LOAD_TEST_RECORDS=${LOAD_TEST_RECORDS:-1000000}
LOAD_TEST_THREADS=${LOAD_TEST_THREADS:-10}
LOAD_TEST_RATE=${LOAD_TEST_RATE:-10000}
TEST_DURATION=${TEST_DURATION:-0} # 0 means record-bound, >0 means time-bound (minutes)

echo -e "${BLUE}Configuration:${NC}"
echo "  Target Records: $LOAD_TEST_RECORDS"
echo "  Producer Threads: $LOAD_TEST_THREADS" 
echo "  Rate Limit: $LOAD_TEST_RATE records/sec"
echo "  Test Duration: $TEST_DURATION minutes (0 = record-bound)"
echo ""

# Step 1: Build the load generator
echo -e "${YELLOW}Step 1: Building load test generator...${NC}"
cd load-test
mvn clean package -DskipTests -q
cd ..

# Step 2: Build application images
echo -e "${YELLOW}Step 2: Building application Docker images...${NC}"
docker build -f Dockerfile.postgres -t payments-orchestrator-postgres:latest . --quiet
docker build -f Dockerfile.mongo -t payments-orchestrator-mongo:latest . --quiet

echo -e "${GREEN}✓ Docker images built${NC}"

# Step 3: Start infrastructure
echo -e "${YELLOW}Step 3: Starting infrastructure services...${NC}"
cd load-test
docker-compose -f docker-compose-load-test.yml up -d zookeeper kafka postgres mongodb prometheus grafana

# Wait for Kafka to be ready
echo -e "${BLUE}Waiting for Kafka to be ready...${NC}"
timeout 60s bash -c 'while ! docker exec $(docker-compose -f docker-compose-load-test.yml ps -q kafka) kafka-topics --bootstrap-server localhost:9092 --list &>/dev/null; do sleep 2; done' || {
    echo -e "${RED}✗ Kafka failed to start within 60 seconds${NC}"
    exit 1
}

# Create topics
echo -e "${BLUE}Creating Kafka topics...${NC}"
docker exec $(docker-compose -f docker-compose-load-test.yml ps -q kafka) kafka-topics --bootstrap-server localhost:9092 --create --topic payment-requests --partitions 12 --replication-factor 1 --config retention.ms=3600000
docker exec $(docker-compose -f docker-compose-load-test.yml ps -q kafka) kafka-topics --bootstrap-server localhost:9092 --create --topic processed-payments --partitions 12 --replication-factor 1 --config retention.ms=3600000

echo -e "${GREEN}✓ Infrastructure services started${NC}"

# Step 4: Start orchestrator applications
echo -e "${YELLOW}Step 4: Starting orchestrator applications...${NC}"
docker-compose -f docker-compose-load-test.yml up -d postgres-orchestrator mongo-orchestrator

# Wait for applications to be ready
echo -e "${BLUE}Waiting for orchestrators to be ready...${NC}"
for service in postgres-orchestrator:8080 mongo-orchestrator:8090; do
    timeout 120s bash -c "while ! curl -sf http://localhost:${service#*:}/actuator/health >/dev/null 2>&1; do sleep 3; done" || {
        echo -e "${RED}✗ Service $service failed to start${NC}"
        docker-compose -f docker-compose-load-test.yml logs ${service%:*}
        exit 1
    }
done

echo -e "${GREEN}✓ Orchestrator applications started${NC}"
echo -e "${BLUE}PostgreSQL Orchestrator: http://localhost:8080${NC}"
echo -e "${BLUE}MongoDB Orchestrator: http://localhost:8090${NC}"
echo -e "${BLUE}Grafana Dashboard: http://localhost:3000 (admin/admin)${NC}"
echo -e "${BLUE}Prometheus: http://localhost:9090${NC}"

# Step 5: Run load test
echo -e "${YELLOW}Step 5: Starting load test...${NC}"
start_time=$(date +%s)

# Update the compose file with environment variables
export LOAD_TEST_RECORDS
export LOAD_TEST_THREADS  
export LOAD_TEST_RATE_LIMIT=$LOAD_TEST_RATE
export TEST_DURATION

# Run the load test
docker-compose -f docker-compose-load-test.yml run --rm kafka-load-generator || {
    echo -e "${RED}✗ Load test failed${NC}"
    exit 1
}

end_time=$(date +%s)
duration=$((end_time - start_time))

echo -e "${GREEN}✓ Load test completed in ${duration} seconds${NC}"

# Step 6: Wait for processing to complete
echo -e "${YELLOW}Step 6: Waiting for message processing to complete...${NC}"
sleep 30

# Step 7: Collect results
echo -e "${YELLOW}Step 7: Collecting results...${NC}"

# Get orchestrator metrics
echo -e "${BLUE}PostgreSQL Orchestrator Metrics:${NC}"
curl -s http://localhost:8080/actuator/prometheus | grep -E "(orchestrator_events|jvm_memory)" | head -20

echo -e "\n${BLUE}MongoDB Orchestrator Metrics:${NC}"
curl -s http://localhost:8090/actuator/prometheus | grep -E "(orchestrator_events|jvm_memory)" | head -20

# Check final message counts in topics
echo -e "\n${BLUE}Final Kafka Topic Status:${NC}"
docker exec $(docker-compose -f docker-compose-load-test.yml ps -q kafka) kafka-run-class kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic payment-requests --time -1
docker exec $(docker-compose -f docker-compose-load-test.yml ps -q kafka) kafka-run-class kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic processed-payments --time -1

# Copy results
if [ -f results/load-test-results.csv ]; then
    echo -e "\n${GREEN}Load test results saved to: load-test/results/load-test-results.csv${NC}"
    echo -e "${BLUE}Results summary:${NC}"
    cat results/load-test-results.csv
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}🎉 Load Test Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "${BLUE}Infrastructure will remain running for analysis.${NC}"
echo -e "${BLUE}To stop: cd load-test && docker-compose -f docker-compose-load-test.yml down${NC}"
echo -e "${BLUE}To view logs: docker-compose -f docker-compose-load-test.yml logs -f [service-name]${NC}"

cd ..