package com.orchestrator.example.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Production-Ready Inventory Orchestrator Application
 * 
 * This example demonstrates PostgreSQL adapter usage by default:
 * 
 * Build with PostgreSQL (default): mvn clean package
 * Build with MongoDB: mvn clean package -Ddb.type=mongo
 * 
 * Architecture:
 * - Core Adapter JAR: orchestrator-core-adapter (Spring Boot + business logic)
 * - DB Adapter JAR: orchestrator-postgres-adapter (PostgreSQL-specific implementation)
 * 
 * Features:
 * - Different database strategy (OUTBOX) compared to payment orchestrator
 * - Different port (8081) to run alongside payment orchestrator
 * - Inventory-specific message transformation
 * - Same high-performance Kafka configuration
 */
@SpringBootApplication
public class InventoryOrchestratorApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(InventoryOrchestratorApplication.class, args);
    }
}