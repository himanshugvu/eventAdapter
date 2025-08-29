@echo off
echo Starting Event Orchestrator Load Test
echo ========================================

set LOAD_TEST_RECORDS=%LOAD_TEST_RECORDS:1000000%
set LOAD_TEST_THREADS=%LOAD_TEST_THREADS:10%
set LOAD_TEST_RATE=%LOAD_TEST_RATE:10000%
set TEST_DURATION=%TEST_DURATION:0%

echo Configuration:
echo   Target Records: %LOAD_TEST_RECORDS%
echo   Producer Threads: %LOAD_TEST_THREADS%
echo   Rate Limit: %LOAD_TEST_RATE% records/sec
echo   Test Duration: %TEST_DURATION% minutes (0 = record-bound)
echo.

echo Step 1: Building load test generator...
cd load-test
call mvn clean package -DskipTests -q
cd ..

echo Step 2: Building application Docker images...
docker build -f Dockerfile.postgres -t payments-orchestrator-postgres:latest . --quiet
docker build -f Dockerfile.mongo -t payments-orchestrator-mongo:latest . --quiet

echo Step 3: Starting infrastructure services...
cd load-test
docker-compose -f docker-compose-load-test.yml up -d zookeeper kafka postgres mongodb prometheus grafana

echo Waiting for Kafka to be ready...
timeout /t 60 /nobreak > nul

echo Creating Kafka topics...
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic payment-requests --partitions 12 --replication-factor 1 --config retention.ms=3600000
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic processed-payments --partitions 12 --replication-factor 1 --config retention.ms=3600000

echo Step 4: Starting orchestrator applications...
docker-compose -f docker-compose-load-test.yml up -d postgres-orchestrator mongo-orchestrator

echo Waiting for orchestrators to be ready...
timeout /t 120 /nobreak > nul

echo Step 5: Starting load test...
docker-compose -f docker-compose-load-test.yml run --rm kafka-load-generator

echo Step 6: Waiting for processing to complete...
timeout /t 30 /nobreak > nul

echo Step 7: Collecting results...
curl -s http://localhost:8080/actuator/prometheus | findstr "orchestrator_events"
curl -s http://localhost:8090/actuator/prometheus | findstr "orchestrator_events"

echo.
echo ========================================
echo Load Test Complete!
echo ========================================
echo Infrastructure will remain running for analysis.
echo To stop: cd load-test ^&^& docker-compose -f docker-compose-load-test.yml down

cd ..