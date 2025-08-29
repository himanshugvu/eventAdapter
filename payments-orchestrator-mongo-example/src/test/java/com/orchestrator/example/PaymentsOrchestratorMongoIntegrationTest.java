package com.orchestrator.example;

import com.orchestrator.core.store.EventStore;
import com.orchestrator.core.store.EventStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"payment-requests", "processed-payments"})
@DirtiesContext
public class PaymentsOrchestratorMongoIntegrationTest {
    
    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
        
        registry.add("orchestrator.consumer.topic", () -> "payment-requests");
        registry.add("orchestrator.producer.topic", () -> "processed-payments");
        registry.add("orchestrator.database.strategy", () -> "RELIABLE");
    }
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private EventStore eventStore;
    
    @Test
    public void testPaymentMessageProcessing() {
        String paymentMessage = """
            {
                "paymentId": "pay_123",
                "amount": 1500.00,
                "currency": "USD",
                "paymentMethod": "credit_card",
                "merchantId": "merchant_456"
            }
            """;
        
        kafkaTemplate.send("payment-requests", paymentMessage);
        
        await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> eventStore.countByStatus(EventStatus.SUCCESS) > 0);
        
        assertEquals(1, eventStore.countByStatus(EventStatus.SUCCESS));
        assertEquals(0, eventStore.countByStatus(EventStatus.FAILED));
    }
}