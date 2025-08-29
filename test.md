# ⚡ Spring Boot Kafka Consumer Config – Best Practices

This guide explains **each Kafka consumer config** in detail, with tradeoffs and recommendations for **high throughput, safety, and resilience**.

---

## 🔹 Kafka Connection

<details>
<summary><code>spring.kafka.bootstrap-servers: ${KAFKA_BOOTSTRAP}</code></summary>

**What it is:**  
The Kafka cluster address (comma-separated brokers).

**Why it matters:**  
All consumers need this to connect. If one broker goes down, clients can still reach the cluster through others.

**Best Practice:**  
Always configure via environment variable (`KAFKA_BOOTSTRAP`) and list multiple brokers for HA.
</details>

---

## 🔹 Consumer Group

<details>
<summary><code>spring.kafka.consumer.group-id: orders-consumer</code></summary>

**What it is:**  
Logical consumer group name. Consumers in the same group share partitions.

**Why it matters:**  
Guarantees that each partition is consumed by only one consumer in the group → preserves order within a partition.

**Tip:**  
Changing group-id resets offsets unless you use committed offsets.
</details>

---

## 🔹 Offset Management

<details>
<summary><code>spring.kafka.consumer.enable-auto-commit: false</code></summary>

**What it is:**  
Disables Kafka auto-committing offsets.

**Why it matters:**
- Auto-commit risks losing data (offset may advance before processing finishes).
- With manual commit (`AckMode.MANUAL_IMMEDIATE`), you commit only after successful processing.

**Guarantee:**  
At-least-once delivery (no data loss, but duplicates possible).
</details>

---

## 🔹 Transaction Safety

<details>
<summary><code>spring.kafka.consumer.isolation-level: read_committed</code></summary>

**What it is:**  
Controls visibility of transactional messages.

- `read_uncommitted` (default) → consumer may see aborted/dirty records.
- `read_committed` → hides aborted records.

**Why it matters:**  
Ensures correctness when producers use transactions (e.g., exactly-once semantics).
</details>

---

## 🔹 Polling & Batching

<details>
<summary><code>spring.kafka.consumer.max-poll-records: 2000</code></summary>

**What it is:**  
Max records returned in a single poll.

**Tradeoff:**
- Higher → better throughput.
- Lower → lower latency, more network/CPU overhead.

`2000` is good for high-throughput workloads (tune per SLA).
</details>

<details>
<summary><code>spring.kafka.consumer.fetch-min-bytes: 1048576 (1 MB)</code></summary>

**What it is:**  
Broker waits until it has at least this many bytes before responding.

**Why it matters:**
- Improves batching efficiency.
- May increase latency under low traffic.

Best for heavy workloads.
</details>

<details>
<summary><code>spring.kafka.consumer.fetch-max-wait: 50ms</code></summary>

**What it is:**  
How long broker waits before returning data even if `fetch.min.bytes` not reached.

**Why it matters:**  
Balances latency and throughput. `50ms` is a sweet spot.
</details>

<details>
<summary><code>spring.kafka.consumer.max-partition-fetch-bytes: 16 MB</code></summary>

**What it is:**  
Max bytes per partition in a fetch.

**Why it matters:**  
Prevents consumer from being overloaded. Must be ≥ largest message size.
</details>

<details>
<summary><code>spring.kafka.consumer.fetch-max-bytes: 64 MB</code></summary>

**What it is:**  
Total max bytes per fetch (across partitions).

**Why it matters:**  
Enables multi-partition consumers to handle large batches.  
(Default is only 50 MB).
</details>

---

## 🔹 Consumer Liveness

<details>
<summary><code>spring.kafka.consumer.session-timeout: 30s</code></summary>

**What it is:**  
Time broker waits before declaring a consumer “dead”.

**Why it matters:**
- Longer → resilient to hiccups.
- Shorter → faster rebalances.

`30s` is safe.
</details>

<details>
<summary><code>spring.kafka.consumer.heartbeat-interval: 3s</code></summary>

**What it is:**  
How often consumer sends heartbeats.

**Why it matters:**
- Keeps group membership alive.
- Must be < session-timeout.

`3s` balances network overhead with safety.
</details>

---

## 🔹 Deserialization & Error Handling

<details>
<summary><code>spring.kafka.consumer.key-deserializer</code> / <code>value-deserializer</code></summary>

**What it is:**  
Classes for deserialization. Wrapped with `ErrorHandlingDeserializer`.

**Why it matters:**
- Prevents bad messages (poison pills) from crashing consumer.
- Failed deserializations can go to DLT/error handler.

**Delegate classes:**
- Key → `StringDeserializer`
- Value → `JsonDeserializer` (with package whitelist)
</details>

---

## 🔹 Partition Assignment & Rebalancing

<details>
<summary><code>spring.kafka.consumer.properties.partition.assignment.strategy: CooperativeStickyAssignor</code></summary>

**What it is:**  
Partition assignment strategy.

**Why it matters:**
- **Sticky** → keeps partitions with same consumer across rebalances.
- **Cooperative** → incremental rebalances (avoids stop-the-world).

**Best Choice:**  
For large groups or frequent scaling, **CooperativeStickyAssignor** reduces churn dramatically.
</details>

<details>
<summary><code>spring.kafka.consumer.properties.group.instance.id: ${POD_NAME:${HOSTNAME:}}</code></summary>

**What it is:**  
Unique, stable ID for consumer instance.

**Why it matters:**
- Enables **static membership** → broker treats restart as the same member.
- Prevents unnecessary rebalances on pod restarts.

**Kubernetes Tip:**  
Set to pod name or hostname for uniqueness.
</details>

---

## 🔹 Listener Settings

<details>
<summary><code>spring.kafka.listener.ack-mode: MANUAL_IMMEDIATE</code></summary>

**What it is:**  
Commit strategy.

**Why it matters:**
- Manual → commit only after processing.
- Immediate → commit right away (not batched).

Ensures **at-least-once** semantics, no premature commits.
</details>

<details>
<summary><code>spring.kafka.listener.concurrency: 8</code></summary>

**What it is:**  
Parallel Kafka listener threads.

**Why it matters:**
- Enables concurrent partition consumption.
- Must not exceed number of partitions.

Use `concurrency = partition count` for max throughput.
</details>

<details>
<summary><code>spring.kafka.listener.poll-timeout: 1500ms</code></summary>

**What it is:**  
How long poll() waits for records.

**Why it matters:**
- Too small → CPU busy looping.
- Too big → slower shutdowns.

`1500ms` is a balanced value.
</details>

<details>
<summary><code>spring.kafka.listener.async-acks: true</code></summary>

**What it is:**  
Commits offsets asynchronously.

**Why it matters:**
- Faster than sync commits → higher throughput.
- Risk: If crash before ack completes, duplicate events may reprocess (still safe due to at-least-once).
</details>

---

## ✅ Why This Setup Is Best

- **High throughput** → big batches, async commits.
- **Data safety** → manual commits, read_committed, error handling.
- **Stability** → cooperative rebalancing + static membership.
- **Resilience** → prevents poison-pill crashes, avoids rebalancing storms.
- **Scalability** → concurrency tuned to partitions.

---
