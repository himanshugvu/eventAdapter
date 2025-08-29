package com.orchestrator.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.orchestrator"})
@ConfigurationPropertiesScan("com.orchestrator")
public class PaymentsOrchestratorApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PaymentsOrchestratorApplication.class, args);
    }
}