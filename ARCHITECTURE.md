# 🏗️ Event Orchestrator Framework Architecture

## 📦 Module Overview

### 1. **orchestrator-core** 
**Pure framework without database dependencies**
```
Dependencies: Spring Boot + Kafka + Micrometer + Jackson
Purpose: Core orchestration logic, interfaces, auto-configuration
Database: NONE - only interfaces
```

### 2. **orchestrator-db-postgres**
**PostgreSQL adapter module**
```
Dependencies: orchestrator-core + Spring Data JPA + PostgreSQL + Flyway
Purpose: PostgreSQL-specific EventStore implementation
Database: PostgreSQL with JPA/Hibernate
```

### 3. **orchestrator-db-mongo** 
**MongoDB adapter module**
```
Dependencies: orchestrator-core + Spring Data MongoDB
Purpose: MongoDB-specific EventStore implementation  
Database: MongoDB with Spring Data MongoDB
```

### 4. **payments-orchestrator-example**
**PostgreSQL example application**
```
Dependencies: orchestrator-core + orchestrator-db-postgres + web + actuator
Purpose: Complete working example with PostgreSQL
Database: Uses PostgreSQL adapter
```

### 5. **payments-orchestrator-mongo-example**
**MongoDB example application**
```
Dependencies: orchestrator-core + orchestrator-db-mongo + web + actuator
Purpose: Complete working example with MongoDB
Database: Uses MongoDB adapter
```

## 🔄 Dependency Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    orchestrator-core                        │
│  ┌─────────────┐ ┌──────────────┐ ┌─────────────────────┐  │
│  │ Interfaces  │ │ Services     │ │ Auto-Configuration  │  │
│  │ - EventStore│ │ - Consumer   │ │ - Kafka Config      │  │
│  │ - Transform │ │ - Publisher  │ │ - Metrics Setup     │  │
│  └─────────────┘ └──────────────┘ └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
           ▲                                      ▲
           │                                      │
  ┌────────▼──────────┐                ┌─────────▼──────────┐
  │ orchestrator-db-  │                │ orchestrator-db-   │
  │ postgres          │                │ mongo              │
  │ ┌───────────────┐ │                │ ┌────────────────┐ │
  │ │PostgresEvent  │ │                │ │MongoEvent      │ │
  │ │PostgresEventS.│ │                │ │MongoEventStore │ │
  │ │JPA Repository │ │                │ │Mongo Repository│ │
  │ └───────────────┘ │                │ └────────────────┘ │
  └───────────────────┘                └────────────────────┘
           ▲                                      ▲
           │                                      │
┌──────────▼──────────────┐              ┌───────▼───────────────┐
│payments-orchestrator-   │              │payments-orchestrator- │
│example                  │              │mongo-example          │
│┌──────────────────────┐ │              │┌─────────────────────┐│
││Application + Config  │ │              ││Application + Config ││
││Custom Transformer    │ │              ││Custom Transformer   ││
││PostgreSQL Runtime    │ │              ││MongoDB Runtime      ││
│└──────────────────────┘ │              │└─────────────────────┘│
└─────────────────────────┘              └───────────────────────┘
```

## 🎯 Clean Separation Principles

### **No Cross-Contamination**
- PostgreSQL example **NEVER** imports MongoDB dependencies
- MongoDB example **NEVER** imports PostgreSQL dependencies  
- Core module has **ZERO** database-specific dependencies
- Each adapter is **completely independent**

### **Auto-Detection Strategy**
```java
@Configuration
@ConditionalOnClass(MongoTemplate.class)  // Only active if MongoDB on classpath
public class MongoEventStoreAutoConfiguration { ... }

@Configuration  
@ConditionalOnClass({EntityManager.class, JdbcTemplate.class})  // Only if PostgreSQL
public class PostgresEventStoreAutoConfiguration { ... }
```

### **Runtime Adapter Selection**
```
IF MongoDB JAR in classpath:
   → MongoEventStore activated
   → PostgreSQL adapter ignored

IF PostgreSQL JAR in classpath:  
   → PostgresEventStore activated
   → MongoDB adapter ignored

IF both JARs (error):
   → Spring Boot fails fast with clear message
   → Forces user to choose one adapter
```

## 🚀 Usage Patterns

### **For PostgreSQL Projects:**
```xml
<dependency>
    <groupId>com.orchestrator</groupId>
    <artifactId>orchestrator-core</artifactId>
</dependency>
<dependency>
    <groupId>com.orchestrator</groupId>
    <artifactId>orchestrator-db-postgres</artifactId>
</dependency>
```

### **For MongoDB Projects:**  
```xml
<dependency>
    <groupId>com.orchestrator</groupId>
    <artifactId>orchestrator-core</artifactId>
</dependency>
<dependency>
    <groupId>com.orchestrator</groupId>
    <artifactId>orchestrator-db-mongo</artifactId>
</dependency>
```

## 🏭 Build & Deployment

### **Database-Specific Builds**
```bash
# PostgreSQL variant
./build-postgres.sh
docker-compose -f docker-compose.postgres.yml up

# MongoDB variant  
./build-mongo.sh
docker-compose -f docker-compose.mongo.yml up
```

### **Kubernetes Deployment**
```bash
# PostgreSQL deployment
helm install payments-pg ./helm/orchestrator -f values-postgres.yaml

# MongoDB deployment  
helm install payments-mongo ./helm/orchestrator -f values-mongo.yaml
```

## ✅ Architecture Benefits

1. **Zero Dependencies Pollution**: Core framework stays database-agnostic
2. **Easy Database Migration**: Swap adapters without changing core logic  
3. **Minimal JAR Size**: Applications only include needed database drivers
4. **Plugin Architecture**: New database adapters can be added independently
5. **Production Flexibility**: Same core framework, different persistence layers
6. **Clean Testing**: Mock EventStore interface for unit tests
7. **Enterprise Ready**: Separate teams can maintain different database adapters

This architecture ensures the framework can scale to support **20+ orchestrators** with complete database flexibility while maintaining clean separation of concerns.