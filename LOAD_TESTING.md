# 🚀 Event Orchestrator Load Testing Guide

## Overview

This guide provides comprehensive load testing capabilities for the Event Orchestrator Framework, capable of testing **1 million+ records** with both **PostgreSQL** and **MongoDB** backends.

## 🎯 Load Testing Capabilities

### **High-Volume Testing**
- **1 million+ records** support
- **Multi-threaded** Kafka producers
- **Configurable throughput** (records/sec)
- **Real-time metrics** collection
- **Performance comparison** between databases

### **Infrastructure**
- **Dockerized environment** with tuned configurations
- **Kafka cluster** with optimized settings
- **PostgreSQL** with performance tuning
- **MongoDB** with WiredTiger optimizations
- **Prometheus + Grafana** monitoring

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                Load Test Environment                     │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────┐    ┌──────────────┐    ┌─────────────┐ │
│  │ Load Gen    │───▶│    Kafka     │───▶│PostgreSQL   │ │
│  │ 1M Records  │    │   Cluster    │    │Orchestrator │ │
│  │ 10 Threads  │    │              │    │             │ │
│  └─────────────┘    └──────────────┘    └─────────────┘ │
│                              │                          │
│                              ▼                          │
│                      ┌─────────────┐                    │
│                      │   MongoDB   │                    │
│                      │Orchestrator │                    │
│                      └─────────────┘                    │
│                                                         │
│  ┌─────────────┐    ┌──────────────┐                   │
│  │ Prometheus  │    │   Grafana    │                    │
│  │ Monitoring  │    │  Dashboard   │                    │
│  └─────────────┘    └──────────────┘                    │
└─────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### 1. **Validate Setup**
```bash
./validate-setup.sh
```

### 2. **Run End-to-End Test**
```bash
./test-end-to-end.sh
```

### 3. **Run Full Load Test (1M Records)**
```bash
./load-test/run-load-test.sh
```

### 4. **Custom Load Test**
```bash
# 5 million records, 20 threads, 50k records/sec
LOAD_TEST_RECORDS=5000000 LOAD_TEST_THREADS=20 LOAD_TEST_RATE=50000 ./load-test/run-load-test.sh

# Time-bound test: 10 minutes
TEST_DURATION=10 ./load-test/run-load-test.sh
```

## 📊 Monitoring & Dashboards

### **Real-time URLs**
- **PostgreSQL Orchestrator**: http://localhost:8080
- **MongoDB Orchestrator**: http://localhost:8090  
- **Grafana Dashboard**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090

### **Key Metrics**
- **Throughput**: Records processed per second
- **Latency**: End-to-end processing time
- **Error Rate**: Failed message percentage
- **Database Performance**: Query response times
- **JVM Metrics**: Memory usage, GC performance

## 🎛️ Configuration Options

### **Environment Variables**

| Variable | Default | Description |
|----------|---------|-------------|
| `LOAD_TEST_RECORDS` | 1,000,000 | Total records to send |
| `LOAD_TEST_THREADS` | 10 | Producer threads |
| `LOAD_TEST_RATE` | 10,000 | Records per second limit |
| `TEST_DURATION` | 0 | Test duration (minutes, 0=record-bound) |

### **Database Strategies**
- **OUTBOX**: Maximum reliability, bulk operations
- **RELIABLE**: Balanced performance and reliability  
- **LIGHTWEIGHT**: Maximum throughput, minimal DB overhead

### **Performance Tuning**

#### **Kafka Settings**
```yaml
# High-throughput Kafka configuration
KAFKA_NUM_NETWORK_THREADS: 8
KAFKA_NUM_IO_THREADS: 16
KAFKA_SOCKET_SEND_BUFFER_BYTES: 102400
KAFKA_SOCKET_RECEIVE_BUFFER_BYTES: 102400
```

#### **PostgreSQL Tuning**
```yaml
# Optimized for load testing
shared_buffers: 512MB
effective_cache_size: 1536MB
work_mem: 4MB
max_connections: 200
```

#### **MongoDB Tuning**
```yaml
# WiredTiger optimizations
wiredTigerCacheSizeGB: 1
wiredTigerCollectionBlockCompressor: snappy
wiredTigerIndexPrefixCompression: true
```

## 📈 Load Test Scenarios

### **Scenario 1: Throughput Benchmark**
```bash
# Test maximum throughput
LOAD_TEST_RECORDS=1000000 LOAD_TEST_THREADS=20 LOAD_TEST_RATE=50000 ./load-test/run-load-test.sh
```

### **Scenario 2: Endurance Test**
```bash  
# 30-minute sustained load
TEST_DURATION=30 LOAD_TEST_THREADS=5 LOAD_TEST_RATE=5000 ./load-test/run-load-test.sh
```

### **Scenario 3: Burst Test**
```bash
# High burst throughput
LOAD_TEST_RECORDS=100000 LOAD_TEST_THREADS=50 LOAD_TEST_RATE=100000 ./load-test/run-load-test.sh
```

### **Scenario 4: Database Comparison**
```bash
# Run identical loads against both databases
./load-test/run-load-test.sh  # Tests both PostgreSQL and MongoDB
```

## 📊 Expected Performance

### **Typical Results** (1M records)

| Database | Throughput | Latency (avg) | Memory Usage |
|----------|-----------|---------------|--------------|
| PostgreSQL | 8,000-12,000 msg/sec | 15-25ms | 1-2GB |
| MongoDB | 6,000-10,000 msg/sec | 20-35ms | 1.5-2.5GB |

### **Factors Affecting Performance**
- **Message Size**: Larger payloads reduce throughput
- **Concurrency**: More threads can improve throughput up to a point
- **Database Strategy**: OUTBOX vs RELIABLE vs LIGHTWEIGHT
- **Hardware**: CPU cores, RAM, disk I/O speed
- **Network**: Latency between services

## 🔍 Troubleshooting

### **Common Issues**

#### **Out of Memory Errors**
```bash
# Increase JVM heap size
export JAVA_OPTS="-Xms4g -Xmx8g -XX:+UseG1GC"
```

#### **Kafka Connection Errors**
```bash
# Check Kafka is ready
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

#### **Database Connection Issues**
```bash  
# Check database health
curl http://localhost:8080/actuator/health
curl http://localhost:8090/actuator/health
```

### **Performance Tuning**

#### **Increase Throughput**
1. **More Producer Threads**: `LOAD_TEST_THREADS=20`
2. **Larger Batches**: Increase `max-poll-records` in config
3. **Database Bulk Size**: Increase `bulk-size` in config
4. **More Kafka Partitions**: 12+ partitions for parallelism

#### **Reduce Latency**
1. **Lower Batch Sizes**: Reduce `linger.ms` in Kafka
2. **More Consumer Threads**: Increase `concurrency`
3. **Faster Database**: Use LIGHTWEIGHT strategy
4. **Optimize Network**: Reduce network hops

## 📝 Results Analysis

### **Metrics Collection**
- **Load generator results**: `load-test/results/load-test-results.csv`
- **Application metrics**: `/actuator/prometheus` endpoints
- **System metrics**: Grafana dashboard

### **Key Performance Indicators**
- **End-to-end latency**: From Kafka produce to database commit
- **Processing rate**: Messages per second throughput
- **Error rate**: Percentage of failed messages
- **Resource utilization**: CPU, memory, network usage
- **Database performance**: Query response times, connection pools

## 🎯 Production Recommendations

Based on load test results:

### **Scaling Guidelines**
- **1-10K msg/sec**: Single instance per database
- **10-50K msg/sec**: 2-3 instances with load balancing
- **50K+ msg/sec**: Horizontal scaling with partitioning

### **Infrastructure Sizing**
- **CPU**: 4+ cores for high throughput
- **RAM**: 8GB+ for large message volumes  
- **Storage**: SSD recommended for database performance
- **Network**: 1Gb+ for sustained high throughput

### **Configuration Tuning**
- **Consumer concurrency**: 1 thread per 2 CPU cores
- **Database bulk size**: 500-1000 for optimal batch processing
- **Connection pools**: Size based on concurrency levels
- **JVM tuning**: G1GC with appropriate heap sizing

This comprehensive load testing framework ensures the Event Orchestrator can handle production-scale workloads with confidence! 🚀