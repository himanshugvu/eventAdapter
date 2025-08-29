# 🚀 Event Orchestrator Framework

A production-grade, pluggable event orchestrator framework built with **Spring Boot 3**, **Java 21**, and **Kafka**. Designed for reuse across multiple orchestrator applications via a modular JAR approach.

## 🏗️ Architecture

The framework provides:

- **Core Orchestrator JAR** - Common functionality and interfaces
- **Database Adapters** - Pluggable persistence layers (MongoDB & PostgreSQL)
- **Example Applications** - Ready-to-use implementations
- **Production Deployment** - Docker, Kubernetes, and Helm configurations

## 📦 Project Structure

```
event-orchestrator-framework/
├── orchestrator-core/                    # Core framework module
├── orchestrator-db-mongo/                # MongoDB adapter
├── orchestrator-db-postgres/             # PostgreSQL adapter
├── payments-orchestrator-example/        # PostgreSQL example app
├── payments-orchestrator-mongo-example/  # MongoDB example app
├── k8s/                                  # Kubernetes manifests
├── helm/                                 # Helm charts
├── monitoring/                           # Prometheus & Grafana configs
├── Dockerfile.postgres                   # PostgreSQL variant image
├── Dockerfile.mongo                      # MongoDB variant image
└── docker-compose.*.yml                  # Docker Compose files
```

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- Kafka cluster (or use provided Docker Compose)

### Option 1: PostgreSQL Variant

1. **Build and run with PostgreSQL:**
   ```bash
   # Windows
   build-postgres.bat
   
   # Linux/macOS
   chmod +x build-postgres.sh
   ./build-postgres.sh
   ```

2. **Start the services:**
   ```bash
   docker-compose -f docker-compose.postgres.yml up
   ```

3. **Access the application:**
   - Application: http://localhost:8080
   - Health: http://localhost:8080/actuator/health
   - Metrics: http://localhost:8080/actuator/prometheus
   - Grafana: http://localhost:3000 (admin/admin)

### Option 2: MongoDB Variant

1. **Build and run with MongoDB:**
   ```bash
   # Windows
   build-mongo.bat
   
   # Linux/macOS
   chmod +x build-mongo.sh
   ./build-mongo.sh
   ```

2. **Start the services:**
   ```bash
   docker-compose -f docker-compose.mongo.yml up
   ```

## 🔧 Configuration

### Database Strategies

The framework supports three persistence strategies:

#### 1. **OUTBOX Mode** (Recommended for high reliability)
- Bulk insert events → Process → Update status
- Guarantees no message loss
- Best for critical business events

#### 2. **RELIABLE Mode** (Balanced approach)
- Insert before processing
- Async status updates on success
- Good balance of performance and reliability

#### 3. **LIGHTWEIGHT Mode** (High performance)
- Only logs failures to database
- Minimal database overhead
- Best for non-critical events

### Example Configuration

```yaml
orchestrator:
  consumer:
    topic: payment-requests
    group-id: payments-orchestrator-group
    concurrency: 3
  
  producer:
    topic: processed-payments
    
  database:
    strategy: RELIABLE  # OUTBOX, RELIABLE, or LIGHTWEIGHT
    max-retries: 3
    retention-period: P14D
```

## 🧩 Creating New Orchestrators

### Step 1: Create Maven Project

```xml
<dependency>
    <groupId>com.orchestrator</groupId>
    <artifactId>orchestrator-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Choose your database adapter -->
<dependency>
    <groupId>com.orchestrator</groupId>
    <artifactId>orchestrator-db-postgres</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Step 2: Implement Custom Transformer (Optional)

```java
@Component
public class MyMessageTransformer implements MessageTransformer {
    
    @Override
    public String transform(String input) {
        // Your custom transformation logic
        return processedMessage;
    }
}
```

### Step 3: Configure Application

```yaml
orchestrator:
  consumer:
    topic: my-input-topic
    group-id: my-orchestrator-group
  producer:
    topic: my-output-topic
```

## 🐳 Docker Deployment

### Build Images

```bash
# PostgreSQL variant
docker build -f Dockerfile.postgres -t my-orchestrator-postgres .

# MongoDB variant  
docker build -f Dockerfile.mongo -t my-orchestrator-mongo .
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `localhost:9092` |
| `SPRING_DATASOURCE_URL` | PostgreSQL URL | - |
| `SPRING_DATA_MONGODB_URI` | MongoDB URI | - |
| `SPRING_PROFILES_ACTIVE` | Active profile | `postgres` or `mongo` |

## ☸️ Kubernetes Deployment

### Using Helm

```bash
# PostgreSQL variant
helm install my-orchestrator ./helm/orchestrator \
  -f helm/orchestrator/values-postgres.yaml \
  --set orchestrator.consumer.topic=my-input-topic \
  --set orchestrator.producer.topic=my-output-topic

# MongoDB variant
helm install my-orchestrator ./helm/orchestrator \
  -f helm/orchestrator/values-mongo.yaml \
  --set orchestrator.consumer.topic=my-input-topic \
  --set orchestrator.producer.topic=my-output-topic
```

### Using kubectl

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
```

## 📊 Monitoring

### Metrics

The framework exposes comprehensive metrics:

- `orchestrator.events.received` - Events received from source topic
- `orchestrator.events.processed` - Events successfully processed
- `orchestrator.events.published` - Events published to target topic  
- `orchestrator.events.pending` - Events in RECEIVED status
- `orchestrator.events.failed` - Events in FAILED status

### Health Checks

Health endpoint provides:
- Database connectivity
- Kafka producer status
- Event processing statistics

### Grafana Dashboards

Pre-configured dashboards available at `monitoring/grafana/dashboards/`

## 🔧 Development

### Build from Source

```bash
mvn clean install
```

### Run Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn verify
```

## 🧪 Testing & Load Testing

### **Quick Validation**
```bash
./validate-setup.sh          # Verify setup works
```

### **End-to-End Testing**
```bash
./test-end-to-end.sh          # Test both PostgreSQL and MongoDB variants
```

### **Load Testing (1M+ Records)**
```bash
./load-test/run-load-test.sh  # Full load test with monitoring
```

### **Custom Load Tests**
```bash
# 5 million records, 20 threads, 50k msg/sec
LOAD_TEST_RECORDS=5000000 LOAD_TEST_THREADS=20 LOAD_TEST_RATE=50000 ./load-test/run-load-test.sh

# 30-minute endurance test
TEST_DURATION=30 LOAD_TEST_RATE=5000 ./load-test/run-load-test.sh
```

### **Monitoring During Tests**
- **PostgreSQL Orchestrator**: http://localhost:8080
- **MongoDB Orchestrator**: http://localhost:8090
- **Grafana Dashboard**: http://localhost:3000 (admin/admin)
- **Prometheus Metrics**: http://localhost:9090

For detailed testing information, see [LOAD_TESTING.md](LOAD_TESTING.md) and [TESTING_SUMMARY.md](TESTING_SUMMARY.md).

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the Apache License 2.0.

## 🆘 Support

For issues and questions:

1. Check the documentation
2. Search existing issues
3. Create a new issue with detailed information

## 🔗 Related Projects

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Apache Kafka](https://kafka.apache.org/)
- [Spring Kafka](https://spring.io/projects/spring-kafka)
- [Micrometer](https://micrometer.io/)