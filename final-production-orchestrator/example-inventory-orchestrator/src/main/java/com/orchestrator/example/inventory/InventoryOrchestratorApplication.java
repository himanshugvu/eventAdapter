package com.orchestrator.example.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Final Production-Ready Inventory Orchestrator Application
 * 
 * Clean Architecture:
 * - Standalone project (not multi-module)
 * - ONLY 2 internal JAR dependencies:
 *   1. orchestrator-core-adapter (ALL Spring Boot + business logic)
 *   2. orchestrator-postgres-adapter (PostgreSQL-specific implementation by default)
 * 
 * Build Commands:
 * - PostgreSQL (default): mvn clean package
 * - MongoDB: mvn clean package -Ddb.type=mongo
 * 
 * Features:
 * - Latest Spring Boot 3.3.5
 * - Different database strategy (OUTBOX) compared to payment orchestrator
 * - Different port (8081) to run alongside other orchestrators
 * - Inventory-specific message transformation
 * - Same high-performance Kafka configuration
 */
@SpringBootApplication
public class InventoryOrchestratorApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(InventoryOrchestratorApplication.class, args);
    }
}