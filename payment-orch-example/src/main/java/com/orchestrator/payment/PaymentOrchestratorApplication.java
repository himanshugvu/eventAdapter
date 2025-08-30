package com.orchestrator.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Production-ready Payment Orchestrator Application
 * 
 * This application depends ONLY on 2 internal JAR files:
 * 1. orchestrator-core-adapter - Contains Spring Boot web framework and core business logic
 * 2. orchestrator-db-adapter - Contains database-specific code and implementations
 * 
 * All producer/consumer logic is in the Core Adapter JAR with configuration overrides
 * Bootstrap servers, topic names, and consumer group names are mandatory
 * Supports separate bootstrap servers for producer and consumer
 */
@SpringBootApplication
public class PaymentOrchestratorApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PaymentOrchestratorApplication.class, args);
    }
}