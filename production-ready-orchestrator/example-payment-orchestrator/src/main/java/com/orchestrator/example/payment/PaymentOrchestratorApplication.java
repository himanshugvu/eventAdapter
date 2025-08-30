package com.orchestrator.example.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Production-Ready Payment Orchestrator Application
 * 
 * This example demonstrates the property-based database adapter selection:
 * 
 * Build with MongoDB: mvn clean package -Ddb.type=mongo
 * Build with PostgreSQL: mvn clean package -Ddb.type=postgres
 * 
 * Architecture:
 * - Core Adapter JAR: orchestrator-core-adapter (Spring Boot + business logic)
 * - DB Adapter JAR: orchestrator-${db.type}-adapter (database-specific implementation)
 * 
 * Features:
 * - Mandatory configuration validation (app won't start without required configs)
 * - Separate bootstrap servers for producer/consumer
 * - High-performance Kafka configuration (1000+ TPS)
 * - Comprehensive metrics and monitoring
 * - Three persistence strategies: OUTBOX, RELIABLE, LIGHTWEIGHT
 */
@SpringBootApplication
public class PaymentOrchestratorApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PaymentOrchestratorApplication.class, args);
    }
}