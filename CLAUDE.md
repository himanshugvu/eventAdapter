# 🚀 Cursor/Claude Prompt – Pluggable Event Orchestrator Framework (Spring Boot 3, Java 21, Kafka)

You are an expert Spring Boot architect with 30+ years of experience.  
Generate a **production-grade event orchestrator framework** in **Spring Boot 3 + Java 21** with **Kafka**, designed to be reused across multiple orchestrator apps via a **pluggable JAR** approach.  

---

## 1. Core Requirements

1. **Generic Orchestrator**
   - Application should consume events from a **source topic** and publish them to a **target topic**.
   - Source and target topics should be **configurable per app** (via `application.yml` or env variables).
   - Transformation logic should be **pluggable**:
     - Provide an interface `MessageTransformer` with `transform(String input)` method.
     - If not implemented → default identity transform (send message as-is).
   - For 20+ orchestrators, I should be able to **spin up a new app just by changing config + transformer class**.

2. **Library-first Architecture**
   - Package the orchestrator as a **core JAR**.
   - Each new orchestrator app:
     - Depends on the orchestrator JAR.
     - Passes the required config (topics, groupId, DB settings).
     - Optionally implements `MessageTransformer` for custom mapping.

3. **Resilience & Reliability**
   - Use **Spring Kafka** with **exactly-once semantics**.
   - Configure **retries, backoff**, and **dead-letter handling**.
   - Handle **graceful shutdown** so offsets are committed properly.
   - Use **idempotency** to avoid duplicate publishing.

4. **Performance**
   - Support **high throughput** (large number of events/sec).
   - Configure consumer concurrency and partition-based scaling.
   - Support **bulk processing** (especially for DB outbox).
   - Use non-blocking I/O (Project Reactor / CompletableFuture where applicable).

---

## 2. Dead Letter Strategy → DB-based

Replace Kafka DLT with a **DB-backed failure tracking mechanism**.  
Provide **configurable modes**:

### 2.1 Outbox Mode
- Bulk consume → bulk insert events into DB with status `RECEIVED`.
- After transformation + successful Kafka publish:
  - Update status → `SUCCESS`.
- On publish failure:
  - Update status → `FAILED`.
- If status remains `RECEIVED` for **>30 minutes** → mark as `FAILED` (timeout safeguard).

### 2.2 Non-Outbox Mode
Controlled by **flags** in config:

- **ReliablePersistence** (better name than "defensive persistence"):  
  - Insert event in DB before publishing.  
  - On success → async status update.  
  - On failure → sync status update.  

- **LightweightPersistence** (better name than "aggressive"):  
  - Only log failed publish attempts into DB (no inserts on success).  

---

## 3. Database Abstraction Layer

- Define `EventStore` interface:
  - `bulkInsert(List<Event>)`
  - `updateStatus(eventId, status)`
  - `findStaleEvents(Duration threshold)`
- Core orchestrator JAR ships only with interface + wiring.
- DB-specific adapters shipped as **separate JARs**:
  - `orchestrator-db-mongo.jar`
  - `orchestrator-db-postgres.jar`
- Auto-detect correct adapter at runtime (based on classpath).
- This ensures **hexagonal architecture** → persistence decoupled from orchestration.

---

## 4. Observability

- Add **Micrometer metrics + Prometheus integration**:
  - Consumer lag
  - Throughput
  - Error counts
- Add **structured logging (JSON logs)** for traceability.
- Add **health/liveness probes** for Kubernetes/OpenShift deployment.

---

## 5. Config & Deployment

- Use `application.yml` to configure:
  - `consumer.topic`
  - `producer.topic`
  - `group.id`
  - `db.strategy` (outbox / reliable / lightweight)
  - `error.db.table`
- Provide:
  - `Dockerfile` for containerization
  - Kubernetes/OpenShift deployment YAML
  - Helm chart template to deploy **multiple orchestrators with different configs**

---

## 6. Developer Experience

- With this design, creating a new orchestrator should require only:
  1. Add orchestrator JAR + DB adapter JAR to the new Spring Boot app.
  2. Provide `application.yml` with consumer/producer topics + DB settings.
  3. Optionally implement `MessageTransformer`.

- Provide a **starter template**:
  - Example:
    ```bash
    ./gradlew createOrchestrator -Pname=PaymentsOrchestrator
    ```
    should scaffold a new app with updated topic configs.

---

## 7. Deliverables

1. **Core Orchestrator JAR**
   - `OrchestratorAutoConfiguration.java`
   - `MessageTransformer` interface (default = identity transform)
   - `EventConsumerService.java`
   - `EventPublisherService.java`
   - `EventStore` interface
   - Config classes (Kafka + Orchestrator properties)
   - DB strategy handling (Outbox / Reliable / Lightweight)

2. **Adapter JARs**
   - `orchestrator-db-mongo.jar` (Spring Data Mongo / native bulk ops)
   - `orchestrator-db-postgres.jar` (JDBC / JPA batch ops)

3. **Example App**
   - `PaymentsOrchestratorApplication.java`
   - Adds orchestrator JAR + one DB adapter JAR.
   - Provides config + optional transformer.

---

⚡ **Ensure:**
- Exactly-once semantics
- No message loss
- High throughput
- Clean separation of concerns (core vs. adapters)
- Simple onboarding for 20+ orchestrators