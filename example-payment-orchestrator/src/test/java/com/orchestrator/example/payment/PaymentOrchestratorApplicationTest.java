package com.orchestrator.example.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PaymentOrchestratorApplicationTest {

    @Test
    void contextLoads() {
        // This test ensures that the Spring application context loads correctly
        // with all beans properly configured and no circular dependencies
    }
}