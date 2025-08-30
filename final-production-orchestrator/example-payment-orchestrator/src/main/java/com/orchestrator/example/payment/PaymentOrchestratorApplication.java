package com.orchestrator.example.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Final Production-Ready Payment Orchestrator Application
 * 
 * Clean Architecture:
 * - Standalone project (not multi-module)
 * - ONLY 2 internal JAR dependencies:
 *   1. orchestrator-core-adapter (ALL Spring Boot + business logic)
 *   2. orchestrator-${db.type}-adapter (database-specific implementation)
 * 
 * Build Commands:
 * - MongoDB: mvn clean package -Ddb.type=mongo
 * - PostgreSQL: mvn clean package -Ddb.type=postgres
 * 
 * Features:
 * - Latest Spring Boot 3.3.5
 * - High-performance Kafka configuration (1000+ TPS)
 * - Mandatory configuration validation
 * - Property-based database adapter selection
 * - Comprehensive monitoring and metrics
 */
@SpringBootApplication
public class PaymentOrchestratorApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PaymentOrchestratorApplication.class, args);
    }
}