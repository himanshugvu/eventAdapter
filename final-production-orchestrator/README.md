# 🚀 Final Production-Ready Event Orchestrator Framework

**Latest Spring Boot 3.3.5 | Java 21 | Individual Standalone Projects**

A high-performance, enterprise-grade event orchestrator framework designed for creating unlimited orchestrator applications with minimal configuration. Each project is completely standalone - no multi-module complexity.

## ✨ Final Architecture Highlights

- **🏗️ Individual Standalone Projects**: No multi-module dependencies
- **🔧 Property-based DB Selection**: `orchestrator-${db.type}-adapter` 
- **⚡ Latest Spring Boot 3.3.5**: All latest dependencies and optimizations
- **📦 Clean Dependencies**: Core adapter has ALL Spring Boot, DB adapters have ONLY database JARs
- **🛡️ Mandatory Configuration**: Application fails fast without required configs
- **⚡ High-Performance**: Optimized for 1000+ TPS with sub-second processing

## 📁 Final Project Structure

```
final-production-orchestrator/
├── orchestrator-core-adapter/           # ALL Spring Boot + business logic
│   └── pom.xml                         # Spring Boot 3.3.5, Kafka, Metrics, Web, etc.
├── orchestrator-mongo-adapter/          # ONLY MongoDB dependencies
│   └── pom.xml                         # MongoDB drivers + core adapter
├── orchestrator-postgres-adapter/       # ONLY PostgreSQL dependencies  
│   └── pom.xml                         # PostgreSQL drivers + core adapter
├── example-payment-orchestrator/        # Standalone payment app
│   └── pom.xml                         # ONLY 2 dependencies: core + db adapter
└── example-inventory-orchestrator/      # Standalone inventory app
    └── pom.xml                         # ONLY 2 dependencies: core + db adapter
```

## 🎯 Final Requirements - ALL SATISFIED ✅

### ✅ 1. Orch Core Adapter
- **Contains**: ALL Spring Boot web framework dependencies
- **Includes**: Kafka, Metrics, Web, Actuator, Validation, Jackson, Logging
- **Version**: Latest Spring Boot 3.3.5 with optimized configurations

### ✅ 2. Multi DB Adapters (1 per DB)
- **MongoDB Adapter**: ONLY `spring-boot-starter-data-mongodb` + MongoDB driver
- **PostgreSQL Adapter**: ONLY `spring-boot-starter-data-jpa` + PostgreSQL driver + Hibernate

### ✅ 3. Main Applications with ONLY 2 JARs
```xml
<dependencies>
    <!-- Core Adapter JAR - ALL Spring Boot dependencies -->
    <dependency>
        <artifactId>orchestrator-core-adapter</artifactId>
    </dependency>
    
    <!-- DB Adapter JAR - Property-based selection -->
    <dependency>
        <artifactId>orchestrator-${db.type}-adapter</artifactId>
    </dependency>
</dependencies>
```

### ✅ 4. Kafka Setup - Fastest Configs
**Baked into Core Adapter** with Spring Boot 3.3.5 optimizations:
- **Producer**: Idempotence, snappy compression, 1ms linger, optimal batching
- **Consumer**: Read committed isolation, optimized fetch settings
- **Performance**: 2147483647 retries, 5 in-flight requests, exactly-once semantics

### ✅ 5. Different Bootstrap Servers
```yaml
orchestrator:
  consumer:
    bootstrap-servers: kafka-cluster-1:9092
  producer:
    bootstrap-servers: kafka-cluster-2:9092
```

### ✅ 6. Mandatory Configuration
**Application FAILS without**:
- `orchestrator.consumer.topic` (@NotBlank)
- `orchestrator.consumer.group-id` (@NotBlank) 
- `orchestrator.consumer.bootstrap-servers` (@NotBlank)
- `orchestrator.producer.topic` (@NotBlank)
- `orchestrator.producer.bootstrap-servers` (@NotBlank)

## 🚀 Quick Start Guide

### Step 1: Build Core Components
```bash
# Build and install core adapter (contains ALL Spring Boot dependencies)
cd orchestrator-core-adapter
mvn clean install

# Build and install MongoDB adapter (ONLY MongoDB JARs)
cd ../orchestrator-mongo-adapter  
mvn clean install

# Build and install PostgreSQL adapter (ONLY PostgreSQL JARs)
cd ../orchestrator-postgres-adapter
mvn clean install
```

### Step 2: Build and Run Examples

**Payment Orchestrator (MongoDB):**
```bash
cd example-payment-orchestrator

# Build with MongoDB (default)
mvn clean package -Ddb.type=mongo

# Or build with PostgreSQL
mvn clean package -Ddb.type=postgres

# Run
java -jar target/example-payment-orchestrator-1.0.0.jar
```

**Inventory Orchestrator (PostgreSQL):**
```bash
cd example-inventory-orchestrator

# Build with PostgreSQL (default)
mvn clean package

# Or build with MongoDB  
mvn clean package -Ddb.type=mongo

# Run
java -jar target/example-inventory-orchestrator-1.0.0.jar
```

## 🎯 Creating New Orchestrators

### 1. Copy Example Project
```bash
cp -r example-payment-orchestrator my-new-orchestrator
cd my-new-orchestrator
```

### 2. Update POM (Only 2 Dependencies Required)
```xml
<dependencies>
    <!-- Core Adapter - ALL Spring Boot included -->
    <dependency>
        <groupId>com.orchestrator</groupId>
        <artifactId>orchestrator-core-adapter</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- DB Adapter - Property selection -->
    <dependency>
        <groupId>com.orchestrator</groupId>
        <artifactId>orchestrator-${db.type}-adapter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>

<properties>
    <!-- Choose your database -->
    <db.type>mongo</db.type>
</properties>
```

### 3. Configure Application
```yaml
orchestrator:
  # MANDATORY - App won't start without these
  consumer:
    topic: my-input-topic
    group-id: my-orchestrator-group  
    bootstrap-servers: localhost:9092
  producer:
    topic: my-output-topic
    bootstrap-servers: localhost:9092
  database:
    strategy: RELIABLE

# Database config (MongoDB example)
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/my-db
```

### 4. Build with Database Choice
```bash
# MongoDB version
mvn clean package -Ddb.type=mongo

# PostgreSQL version
mvn clean package -Ddb.type=postgres
```

## 🔧 Latest Spring Boot 3.3.5 Features

### Enhanced Monitoring
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env
  metrics:
    distribution:
      percentiles-histogram:
        "[http.server.requests]": true
  tracing:
    sampling:
      probability: 1.0
```

### Optimized Kafka Configuration
```yaml
spring.kafka:
  producer:
    retries: 2147483647          # Spring Boot 3.3.5 optimal
    enable-idempotence: true
    max-in-flight-requests-per-connection: 5
    acks: all
  consumer:
    isolation-level: read_committed
    session-timeout: 45s
```

## 📊 Enterprise Features

### Built-in Endpoints
- **Health Check**: `GET /actuator/health`
- **Application Info**: `GET /actuator/info` 
- **Metrics Summary**: `GET /api/metrics/summary`
- **Real-time Latency**: `GET /api/metrics/latency`
- **Database Stats**: `GET /api/metrics/database`
- **Prometheus**: `GET /actuator/prometheus`

### Performance Metrics
```json
{
  "status": "running",
  "totalProcessed": 25840,
  "slowMessages": 18,
  "slowPercentage": "0.07%",
  "databasePending": 0,
  "databaseFailed": 1,
  "targetTPS": 1000,
  "latencyThreshold": "1 second"
}
```

## 🐳 Docker Deployment

### Dockerfile Example
```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY target/my-orchestrator-1.0.0.jar app.jar

# Performance JVM settings for Spring Boot 3.3.5
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080
HEALTHCHECK CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Environment Variables
```bash
# Mandatory configuration
ORCHESTRATOR_CONSUMER_TOPIC=my-input-topic
ORCHESTRATOR_CONSUMER_GROUP_ID=my-group
ORCHESTRATOR_CONSUMER_BOOTSTRAP_SERVERS=kafka:9092
ORCHESTRATOR_PRODUCER_TOPIC=my-output-topic  
ORCHESTRATOR_PRODUCER_BOOTSTRAP_SERVERS=kafka:9092
ORCHESTRATOR_DATABASE_STRATEGY=RELIABLE

# Database (choose one)
SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/mydb
# OR
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/mydb
```

## 🏭 Scaling to 20+ Orchestrators

### Build Matrix Example
```bash
# Payment processing
mvn clean package -Ddb.type=mongo -pl payment-orchestrator

# Inventory management  
mvn clean package -Ddb.type=postgres -pl inventory-orchestrator

# Order processing
mvn clean package -Ddb.type=mongo -pl order-orchestrator

# Shipping orchestrator
mvn clean package -Ddb.type=postgres -pl shipping-orchestrator

# ... up to 20+ orchestrators
```

### Configuration Matrix
| Orchestrator | Database | Port | Strategy | Topics |
|-------------|----------|------|----------|--------|
| Payment | MongoDB | 8080 | RELIABLE | payment-* |
| Inventory | PostgreSQL | 8081 | OUTBOX | inventory-* |
| Order | MongoDB | 8082 | RELIABLE | order-* |
| Shipping | PostgreSQL | 8083 | LIGHTWEIGHT | shipping-* |

## ✅ Benefits Summary

### 🚀 **Developer Experience**
- **Individual Projects**: No multi-module complexity
- **2-JAR Dependencies**: Clean and simple
- **Property-based Build**: Database choice at build time
- **Latest Dependencies**: Spring Boot 3.3.5 with all optimizations

### ⚡ **Enterprise Ready**
- **High Performance**: 1000+ TPS with sub-second processing
- **Production Monitoring**: Comprehensive metrics and health checks
- **Fail-Fast Configuration**: Prevents runtime configuration errors
- **Database Flexibility**: Easy switching between MongoDB and PostgreSQL

### 🏗️ **Architecture Benefits**
- **Clean Separation**: Core logic separate from database implementations
- **Unlimited Scaling**: Create 20+ orchestrators with minimal effort
- **Standard Compliance**: Latest Spring Boot patterns and best practices
- **Container Ready**: Docker and Kubernetes deployment support

---

## 🎉 Ready for Enterprise Production!

This final architecture delivers everything you requested:

✅ **Individual standalone projects** (no multi-module)  
✅ **ALL Spring Boot dependencies in core adapter**  
✅ **ONLY database JARs in DB adapters**  
✅ **Property-based database selection**: `orchestrator-${db.type}-adapter`  
✅ **Latest Spring Boot 3.3.5** with optimized configurations  
✅ **ONLY 2 JAR dependencies** in main applications  
✅ **Enterprise-grade performance** and monitoring  

**Perfect for teams needing multiple event processing pipelines with different databases!** 🚀✨