#!/bin/bash

echo "🚀 Verifying Production-Ready Event Orchestrator Setup"
echo "======================================================"

# Build the project
echo "📦 Building the project..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Build successful!"

# Start services
echo "🔄 Starting services with Docker Compose..."
docker-compose -f docker-compose-production.yml up -d

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 60

# Check if all services are healthy
echo "🔍 Checking service health..."

# Check Kafka
kafka_health=$(docker-compose -f docker-compose-production.yml exec kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "✅ Kafka is healthy"
else
    echo "❌ Kafka is not healthy"
fi

# Check MongoDB
mongo_health=$(docker-compose -f docker-compose-production.yml exec mongodb mongosh --eval "db.adminCommand('ping')" --quiet 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "✅ MongoDB is healthy"
else
    echo "❌ MongoDB is not healthy"
fi

# Check Payment Orchestrator
orchestrator_health=$(curl -s http://localhost:8080/actuator/health 2>/dev/null | grep "UP")
if [ $? -eq 0 ]; then
    echo "✅ Payment Orchestrator is healthy"
else
    echo "❌ Payment Orchestrator is not healthy"
fi

# Create topics with 20 partitions
echo "📋 Creating Kafka topics with 20 partitions..."
docker-compose -f docker-compose-production.yml exec kafka kafka-topics --create --bootstrap-server localhost:9092 --topic payment-input-topic --partitions 20 --replication-factor 1 --if-not-exists
docker-compose -f docker-compose-production.yml exec kafka kafka-topics --create --bootstrap-server localhost:9092 --topic payment-output-topic --partitions 20 --replication-factor 1 --if-not-exists

# Verify topics
echo "🔍 Verifying topics..."
docker-compose -f docker-compose-production.yml exec kafka kafka-topics --describe --bootstrap-server localhost:9092 --topic payment-input-topic
docker-compose -f docker-compose-production.yml exec kafka kafka-topics --describe --bootstrap-server localhost:9092 --topic payment-output-topic

# Send test messages
echo "📨 Sending test messages..."
for i in {1..10}; do
    echo "{\"paymentId\": \"$i\", \"amount\": 100.00, \"timestamp\": $(date +%s)}" | \
    docker-compose -f docker-compose-production.yml exec -T kafka kafka-console-producer --bootstrap-server localhost:9092 --topic payment-input-topic
done

echo "⏳ Waiting for message processing..."
sleep 10

# Check metrics
echo "📊 Checking orchestrator metrics..."
curl -s http://localhost:8080/api/metrics/summary | jq . || echo "Metrics endpoint response: $(curl -s http://localhost:8080/api/metrics/summary)"

echo ""
echo "🎉 Production setup verification complete!"
echo ""
echo "📊 Access Points:"
echo "   • Payment Orchestrator: http://localhost:8080"
echo "   • Orchestrator Metrics: http://localhost:8080/api/metrics/summary"
echo "   • Kafka UI: http://localhost:8090"
echo "   • MongoDB: mongodb://root:rootpassword@localhost:27017/payment-orchestrator"
echo ""
echo "🧪 To run load test:"
echo "   • Locust UI: http://localhost:8089"
echo "   • Or use: docker-compose -f docker-compose-production.yml exec locust locust -f /mnt/locust/locustfile.py --host=http://kafka:29092 --users 100 --spawn-rate 10 --run-time 60s --headless"
echo ""
echo "🛑 To stop services:"
echo "   docker-compose -f docker-compose-production.yml down -v"