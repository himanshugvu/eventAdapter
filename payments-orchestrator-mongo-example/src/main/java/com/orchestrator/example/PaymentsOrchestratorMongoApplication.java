package com.orchestrator.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.orchestrator"})
@ConfigurationPropertiesScan("com.orchestrator")
public class PaymentsOrchestratorMongoApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PaymentsOrchestratorMongoApplication.class, args);
    }
}