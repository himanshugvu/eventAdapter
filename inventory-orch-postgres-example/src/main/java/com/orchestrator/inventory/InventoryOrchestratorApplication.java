package com.orchestrator.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Inventory Orchestrator Application using PostgreSQL
 * 
 * This application demonstrates the swappable DB adapter architecture:
 * - Uses orchestrator-core-adapter (same as payment orchestrator)
 * - Uses orchestrator-postgres-adapter instead of mongo adapter
 * 
 * To switch to MongoDB, simply replace orchestrator-postgres-adapter 
 * with orchestrator-mongo-adapter in pom.xml - no code changes needed!
 */
@SpringBootApplication
public class InventoryOrchestratorApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(InventoryOrchestratorApplication.class, args);
    }
}