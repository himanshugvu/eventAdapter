# 🚀 Production-Ready Event Orchestrator Framework

A high-performance, pluggable event orchestrator framework built with Spring Boot 3 and Java 21, designed for creating multiple orchestrator applications with minimal configuration.

## ✨ Key Features

- **🏗️ 2-JAR Architecture**: Main applications depend on exactly 2 internal JARs only
- **🔧 Property-based DB Selection**: Choose database adapter at build time with `-Ddb.type=mongo|postgres`
- **⚡ High-Performance Kafka**: Optimized for 1000+ TPS with sub-second processing
- **🛡️ Mandatory Configuration**: Application fails fast if required configs are missing
- **📊 Comprehensive Monitoring**: Metrics, health checks, and Prometheus integration
- **🔄 Multiple Persistence Strategies**: OUTBOX, RELIABLE, and LIGHTWEIGHT modes

## 📁 Project Structure

```
production-orchestrator-framework/
├── orchestrator-core-adapter/           # Core business logic + Spring Boot
├── orchestrator-mongo-adapter/          # MongoDB-specific implementation
├── orchestrator-postgres-adapter/       # PostgreSQL-specific implementation
├── example-payment-orchestrator/        # Example MongoDB-based orchestrator
└── example-inventory-orchestrator/      # Example PostgreSQL-based orchestrator
```

## 🚀 Quick Start

### 1. Build the Framework
```bash
mvn clean install
```

### 2. Run Example Applications

**Payment Orchestrator (MongoDB):**
```bash
# Build with MongoDB adapter
mvn clean package -Ddb.type=mongo

# Run
java -jar example-payment-orchestrator/target/example-payment-orchestrator-1.0.0.jar
```

**Inventory Orchestrator (PostgreSQL):**
```bash
# Build with PostgreSQL adapter (default)
mvn clean package

# Run
java -jar example-inventory-orchestrator/target/example-inventory-orchestrator-1.0.0.jar
```

## 🎯 Creating New Orchestrators

### Step 1: Create New Maven Project
```xml
<dependencies>
    <!-- Core Adapter JAR -->
    <dependency>
        <groupId>com.orchestrator</groupId>
        <artifactId>orchestrator-core-adapter</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- DB Adapter JAR - Property-based selection -->
    <dependency>
        <groupId>com.orchestrator</groupId>
        <artifactId>orchestrator-${db.type}-adapter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>

<properties>
    <!-- Default database type -->
    <db.type>mongo</db.type>
</properties>
```

### Step 2: Create Application Class
```java
@SpringBootApplication
public class MyOrchestratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyOrchestratorApplication.class, args);
    }
}
```

### Step 3: Configure Application
```yaml
orchestrator:
  # MANDATORY - App won't start without these
  consumer:
    topic: my-input-topic
    group-id: my-orchestrator-group
    bootstrap-servers: localhost:9092
  producer:
    topic: my-output-topic
    bootstrap-servers: localhost:9092  # Can be different from consumer
  database:
    strategy: RELIABLE

# Database configuration (MongoDB example)
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/my-orchestrator
```

### Step 4: Build with Different Databases
```bash
# MongoDB version
mvn clean package -Ddb.type=mongo

# PostgreSQL version
mvn clean package -Ddb.type=postgres
```

## 🔧 Configuration Reference

### Mandatory Configuration
These configurations are **required** - the application will fail to start without them:

```yaml
orchestrator:
  consumer:
    topic: "input-topic"              # ✅ Required
    group-id: "consumer-group"        # ✅ Required
    bootstrap-servers: "kafka:9092"   # ✅ Required
  producer:
    topic: "output-topic"             # ✅ Required
    bootstrap-servers: "kafka:9092"   # ✅ Required (can be different from consumer)
  database:
    strategy: RELIABLE                # ✅ Required: OUTBOX, RELIABLE, or LIGHTWEIGHT
```

### Optional Kafka Overrides
The core adapter includes optimized defaults, but you can override them:

```yaml
spring.kafka:
  consumer:
    max-poll-records: 500
    fetch-max-wait: 500ms
    heartbeat-interval: 3s
  producer:
    batch-size: 16384
    linger-ms: 1
    compression-type: snappy
    acks: all
```

## 🗄️ Database Strategies

### OUTBOX Pattern
```yaml
orchestrator:
  database:
    strategy: OUTBOX
```
- Bulk insert → async processing → status updates
- Best for high throughput with eventual consistency

### RELIABLE Pattern
```yaml
orchestrator:
  database:
    strategy: RELIABLE
```
- Insert before processing → guaranteed persistence
- Best for critical data that cannot be lost

### LIGHTWEIGHT Pattern
```yaml
orchestrator:
  database:
    strategy: LIGHTWEIGHT
```
- Only logs failures → minimal database overhead
- Best for non-critical data processing

## 📊 Monitoring & Observability

### Built-in Endpoints
- **Health**: `GET /actuator/health`
- **Metrics Summary**: `GET /api/metrics/summary`
- **Latency Stats**: `GET /api/metrics/latency`
- **Database Stats**: `GET /api/metrics/database`
- **Prometheus**: `GET /actuator/prometheus`

### Example Response
```json
{
  "status": "running",
  "totalProcessed": 15420,
  "slowMessages": 12,
  "slowPercentage": "0.08%",
  "databasePending": 0,
  "databaseFailed": 2,
  "targetTPS": 1000,
  "latencyThreshold": "1 second"
}
```

## 🎨 Custom Message Transformation

Create custom transformers by implementing the `MessageTransformer` interface:

```java
@Component
public class MyCustomTransformer implements MessageTransformer {
    
    @Override
    public String transform(String input) {
        // Your transformation logic here
        return transformedMessage;
    }
}
```

## 🐳 Docker Deployment

### Build Docker Images
```bash
# Payment orchestrator with MongoDB
docker build -t payment-orchestrator:mongo \
  --build-arg DB_TYPE=mongo \
  example-payment-orchestrator/

# Inventory orchestrator with PostgreSQL  
docker build -t inventory-orchestrator:postgres \
  --build-arg DB_TYPE=postgres \
  example-inventory-orchestrator/
```

### Environment Variables
```bash
# Mandatory environment variables
ORCHESTRATOR_CONSUMER_TOPIC=my-input-topic
ORCHESTRATOR_CONSUMER_GROUP_ID=my-consumer-group
ORCHESTRATOR_CONSUMER_BOOTSTRAP_SERVERS=kafka:9092
ORCHESTRATOR_PRODUCER_TOPIC=my-output-topic
ORCHESTRATOR_PRODUCER_BOOTSTRAP_SERVERS=kafka:9092
ORCHESTRATOR_DATABASE_STRATEGY=RELIABLE

# Database-specific variables
SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/mydb
# OR
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/mydb
```

## 🏭 Production Deployment

### Performance Tuning
```yaml
# JVM settings
JAVA_OPTS: "-XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Kafka optimization
spring.kafka:
  consumer:
    max-poll-records: 1000
    concurrency: 20        # Match Kafka partition count
  producer:
    batch-size: 32768
    linger-ms: 1
    compression-type: snappy
```

### Scaling Considerations
- **Kafka Partitions**: Match consumer concurrency to partition count
- **Database Connections**: Configure connection pool based on load
- **JVM Memory**: Allocate sufficient heap for high throughput
- **Monitoring**: Set up alerts on slow message percentage

## 📝 Build Commands Reference

```bash
# Build all modules
mvn clean install

# Build specific orchestrator with MongoDB
mvn clean package -Ddb.type=mongo -pl example-payment-orchestrator -am

# Build specific orchestrator with PostgreSQL
mvn clean package -Ddb.type=postgres -pl example-inventory-orchestrator -am

# Run tests
mvn test

# Skip tests
mvn clean package -DskipTests
```

## 🎯 Benefits

### ✅ **Rapid Development**
Create new orchestrators in minutes by copying configuration

### ✅ **Database Flexibility** 
Switch between MongoDB and PostgreSQL without code changes

### ✅ **Production Ready**
Built-in monitoring, health checks, and performance optimization

### ✅ **Fail-Fast Configuration**
Mandatory validation prevents runtime configuration errors

### ✅ **High Performance**
Optimized for 1000+ TPS with sub-second latency tracking

---

## 🚀 Ready to Scale!

This framework supports creating **20+ orchestrators** with minimal effort. Each orchestrator can:
- Use different databases (MongoDB or PostgreSQL)
- Connect to different Kafka clusters
- Implement custom message transformations
- Use different persistence strategies
- Run on different ports and environments

**Perfect for microservices architectures requiring multiple event processing pipelines!** 🎉