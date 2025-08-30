# 🔧 Final Production Build Guide

## 🎯 Complete Build Instructions

### Prerequisites
- **Java 21+**
- **Maven 3.6+**
- **Docker** (optional, for containerized databases)

## 📦 Build Order (Individual Projects)

### 1. Build Core Adapter First
```bash
cd orchestrator-core-adapter
mvn clean install -DskipTests

# This installs orchestrator-core-adapter-1.0.0.jar to local Maven repo
```

### 2. Build Database Adapters
```bash
# MongoDB Adapter
cd ../orchestrator-mongo-adapter
mvn clean install -DskipTests

# PostgreSQL Adapter  
cd ../orchestrator-postgres-adapter
mvn clean install -DskipTests
```

### 3. Build Example Applications

**Payment Orchestrator:**
```bash
cd ../example-payment-orchestrator

# Build with MongoDB (default)
mvn clean package -Ddb.type=mongo

# Build with PostgreSQL
mvn clean package -Ddb.type=postgres
```

**Inventory Orchestrator:**
```bash
cd ../example-inventory-orchestrator

# Build with PostgreSQL (default)
mvn clean package -Ddb.type=postgres

# Build with MongoDB
mvn clean package -Ddb.type=mongo
```

## 🚀 Run Applications

### Start Infrastructure
```bash
# Start MongoDB
docker run -d --name mongodb -p 27017:27017 -e MONGO_INITDB_ROOT_USERNAME=root -e MONGO_INITDB_ROOT_PASSWORD=rootpassword mongo:7.0

# Start PostgreSQL
docker run -d --name postgres -p 5432:5432 -e POSTGRES_DB=inventory_orchestrator -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres postgres:16

# Start Kafka
docker run -d --name kafka -p 9092:9092 -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 confluentinc/cp-kafka:7.5.0
```

### Run Orchestrators
```bash
# Payment Orchestrator (Port 8080)
cd example-payment-orchestrator
java -jar target/example-payment-orchestrator-1.0.0.jar

# Inventory Orchestrator (Port 8081) 
cd example-inventory-orchestrator
java -jar target/example-inventory-orchestrator-1.0.0.jar
```

## 🧪 Verify Build Success

### Check JAR Dependencies
```bash
# Payment orchestrator should have exactly 2 internal JARs
jar tf example-payment-orchestrator/target/example-payment-orchestrator-1.0.0.jar | grep orchestrator

# Should show:
# - orchestrator-core-adapter classes
# - orchestrator-mongo-adapter classes (or postgres if built with -Ddb.type=postgres)
```

### Test Endpoints
```bash
# Health checks
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health

# Metrics
curl http://localhost:8080/api/metrics/summary
curl http://localhost:8081/api/metrics/summary
```

## 🔄 Creating New Orchestrators

### Step-by-Step Process
```bash
# 1. Copy example
cp -r example-payment-orchestrator my-custom-orchestrator

# 2. Update pom.xml
cd my-custom-orchestrator
# Edit pom.xml - change artifactId and mainClass

# 3. Update Java packages
mv src/main/java/com/orchestrator/example/payment src/main/java/com/orchestrator/example/custom

# 4. Update application.yml
# Edit src/main/resources/application.yml - change topics and port

# 5. Build
mvn clean package -Ddb.type=mongo  # or postgres

# 6. Run
java -jar target/my-custom-orchestrator-1.0.0.jar
```

## 🐳 Docker Build

### Build Docker Images
```bash
# Create Dockerfile in orchestrator directory
cat > Dockerfile << 'EOF'
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
EOF

# Build image
docker build -t my-orchestrator:mongo .
```

### Docker Compose Example
```yaml
version: '3.8'
services:
  my-orchestrator:
    image: my-orchestrator:mongo
    ports:
      - "8080:8080"
    environment:
      ORCHESTRATOR_CONSUMER_TOPIC: my-input
      ORCHESTRATOR_CONSUMER_GROUP_ID: my-group
      ORCHESTRATOR_CONSUMER_BOOTSTRAP_SERVERS: kafka:9092
      ORCHESTRATOR_PRODUCER_TOPIC: my-output
      ORCHESTRATOR_PRODUCER_BOOTSTRAP_SERVERS: kafka:9092
      ORCHESTRATOR_DATABASE_STRATEGY: RELIABLE
      SPRING_DATA_MONGODB_URI: mongodb://mongo:27017/mydb
```

## ⚡ Performance Tuning

### JVM Settings for Production
```bash
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=20 \
     -XX:+UseContainerSupport \
     -XX:MaxRAMPercentage=75.0 \
     -XX:+ExplicitGCInvokesConcurrent \
     -jar target/my-orchestrator-1.0.0.jar
```

### Application Properties for High Load
```yaml
spring.kafka:
  consumer:
    max-poll-records: 1000
    fetch-max-wait: 100ms
    concurrency: 20  # Match Kafka partition count
  producer:
    batch-size: 32768
    linger-ms: 1
    compression-type: snappy
    buffer-memory: 67108864
```

## 🔍 Troubleshooting

### Common Build Issues

**1. Core Adapter Not Found**
```bash
# Solution: Build and install core adapter first
cd orchestrator-core-adapter
mvn clean install
```

**2. Wrong Database Adapter**
```bash
# Check property
mvn help:effective-pom | grep db.type

# Rebuild with correct type
mvn clean package -Ddb.type=postgres
```

**3. Kafka Connection Issues**
```bash
# Check bootstrap servers in application.yml
# Verify Kafka is running on specified ports
```

### Build Verification Commands
```bash
# Check all JARs are built
find . -name "*.jar" -path "*/target/*" | grep -v original

# Verify dependencies
mvn dependency:tree -pl example-payment-orchestrator

# Test with different database
mvn clean package -Ddb.type=postgres -pl example-payment-orchestrator
```

## 🎯 Success Criteria

✅ **Core adapter builds with ALL Spring Boot dependencies**  
✅ **DB adapters build with ONLY database-specific JARs**  
✅ **Example apps build with exactly 2 internal dependencies**  
✅ **Property-based selection works**: `-Ddb.type=mongo|postgres`  
✅ **Applications start and respond to health checks**  
✅ **Mandatory configuration validation works**  

---

## 🏆 You're Ready!

With this build guide, you can create unlimited orchestrator applications:

1. **Build core components once**
2. **Copy example applications** 
3. **Customize configuration**
4. **Build with database choice**
5. **Deploy anywhere**

**Enterprise-scale event processing made simple!** 🚀