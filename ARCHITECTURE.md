# 🏗️ Production-Ready Event Orchestrator Architecture

## ✅ Corrected 2-JAR Architecture

### Core Design Principle
Each orchestrator application depends on **exactly 2 internal JAR files only**:

1. **orchestrator-core-adapter** - Spring Boot web framework + core business logic
2. **Database-specific adapter** - Choose ONE based on your database:
   - `orchestrator-mongo-adapter` - MongoDB implementation
   - `orchestrator-postgres-adapter` - PostgreSQL implementation

### 📁 Project Structure

```
event-orchestrator-framework/
├── orchestrator-core-adapter/          # Core business logic + Spring Boot
│   ├── src/main/java/com/orchestrator/core/
│   │   ├── config/                     # Kafka, Properties, AutoConfiguration
│   │   ├── controller/                 # Metrics REST endpoints
│   │   ├── metrics/                    # Latency tracking
│   │   ├── service/                    # Consumer, Publisher services
│   │   ├── store/                      # EventStore interface, Event entity
│   │   └── transformer/                # MessageTransformer interface
│   └── pom.xml                        # Spring Boot + Kafka + Metrics deps
│
├── orchestrator-mongo-adapter/          # MongoDB-specific implementation
│   ├── src/main/java/com/orchestrator/mongo/
│   │   ├── config/                     # MongoAdapterAutoConfiguration
│   │   └── store/                      # MongoEventStore implementation
│   └── pom.xml                        # MongoDB dependencies included
│
├── orchestrator-postgres-adapter/       # PostgreSQL-specific implementation
│   ├── src/main/java/com/orchestrator/postgres/
│   │   ├── config/                     # PostgresAdapterAutoConfiguration  
│   │   └── store/                      # PostgresEventStore implementation
│   └── pom.xml                        # PostgreSQL + JDBC dependencies included
│
├── payment-orch-example/               # MongoDB-based orchestrator
│   ├── pom.xml                        # Depends on: core + mongo adapters
│   └── src/main/java/.../PaymentOrchestratorApplication.java
│
└── inventory-orch-postgres-example/    # PostgreSQL-based orchestrator
    ├── pom.xml                        # Depends on: core + postgres adapters
    └── src/main/java/.../InventoryOrchestratorApplication.java
```

## 🎯 Usage Examples

### Creating a MongoDB-based Orchestrator

**pom.xml:**
```xml
<dependencies>
    <!-- Core adapter with Spring Boot + business logic -->
    <dependency>
        <groupId>com.orchestrator</groupId>
        <artifactId>orchestrator-core-adapter</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- MongoDB adapter with MongoDB dependencies -->
    <dependency>
        <groupId>com.orchestrator</groupId>
        <artifactId>orchestrator-mongo-adapter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

**application.yml:**
```yaml
orchestrator:
  consumer:
    topic: payment-input-topic
    group-id: payment-group
    bootstrap-servers: localhost:9092
  producer:
    topic: payment-output-topic
    bootstrap-servers: localhost:9092
  database:
    strategy: RELIABLE

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/payment-db
```

### Creating a PostgreSQL-based Orchestrator

**pom.xml:**
```xml
<dependencies>
    <!-- Same core adapter -->
    <dependency>
        <groupId>com.orchestrator</groupId>
        <artifactId>orchestrator-core-adapter</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Different database adapter -->
    <dependency>
        <groupId>com.orchestrator</groupId>
        <artifactId>orchestrator-postgres-adapter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

**application.yml:**
```yaml
orchestrator:
  consumer:
    topic: inventory-input-topic
    group-id: inventory-group
    bootstrap-servers: localhost:9092
  producer:
    topic: inventory-output-topic
    bootstrap-servers: localhost:9092
  database:
    strategy: OUTBOX

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/inventory_db
    username: postgres
    password: postgres
```

## 🔄 Database Adapter Swapping

To switch from MongoDB to PostgreSQL (or vice versa):

1. **Update pom.xml** - Replace the database adapter dependency
2. **Update application.yml** - Change database connection settings
3. **No code changes needed!**

### Before (MongoDB):
```xml
<dependency>
    <artifactId>orchestrator-mongo-adapter</artifactId>
</dependency>
```

### After (PostgreSQL):
```xml
<dependency>
    <artifactId>orchestrator-postgres-adapter</artifactId>
</dependency>
```

## 🚀 Benefits of This Architecture

### ✅ Clean Separation
- **Core adapter**: Contains all business logic, completely database-agnostic
- **DB adapters**: Focused only on database-specific implementations
- **Main apps**: Just configuration + optional custom transformers

### ✅ Easy Scaling
- Create 20+ orchestrators by copying configuration
- Swap database technologies without changing core logic
- Each adapter includes all necessary dependencies

### ✅ Production Ready
- Mandatory configuration validation (app won't start without required settings)
- Separate bootstrap servers for producer/consumer
- Comprehensive metrics and monitoring
- High-performance Kafka configuration (1000+ TPS)

### ✅ Developer Experience
```bash
# Build all modules
mvn clean package

# Run MongoDB orchestrator
java -jar payment-orch-example/target/payment-orch-example-1.0.0.jar

# Run PostgreSQL orchestrator  
java -jar inventory-orch-postgres-example/target/inventory-orch-postgres-example-1.0.0.jar

# Or use Docker
docker-compose -f docker-compose-multi-orchestrator.yml up
```

## 📊 Live Demo

The `docker-compose-multi-orchestrator.yml` demonstrates both orchestrators running simultaneously:

- **Payment Orchestrator** (MongoDB) - Port 8080
  - Topics: `payment-input-topic` → `payment-output-topic`
  - Database: MongoDB
  - Strategy: RELIABLE

- **Inventory Orchestrator** (PostgreSQL) - Port 8081
  - Topics: `inventory-input-topic` → `inventory-output-topic`
  - Database: PostgreSQL  
  - Strategy: OUTBOX

- **Monitoring**:
  - Kafka UI: http://localhost:8090
  - Payment Metrics: http://localhost:8080/api/metrics/summary
  - Inventory Metrics: http://localhost:8081/api/metrics/summary

This architecture perfectly achieves your goal: **Main applications have exactly 2 internal JAR dependencies only**, with complete database adapter swappability! 🎉