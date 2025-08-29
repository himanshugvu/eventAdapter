package com.orchestrator.core.service;

import com.orchestrator.core.config.OrchestratorProperties;
import com.orchestrator.core.metrics.OrchestratorMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EventPublisherService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventPublisherService.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OrchestratorProperties properties;
    private final OrchestratorMetrics metrics;
    
    public EventPublisherService(
            KafkaTemplate<String, String> kafkaTemplate,
            OrchestratorProperties properties,
            OrchestratorMetrics metrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.metrics = metrics;
    }
    
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000L)
    )
    public CompletableFuture<Void> publishMessage(String message) {
        String targetTopic = properties.producer().topic();
        
        logger.debug("Publishing message to topic: {}", targetTopic);
        
        return kafkaTemplate.send(targetTopic, message)
            .thenApply(this::handleSuccess)
            .exceptionally(this::handleFailure);
    }
    
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000L)
    )
    public CompletableFuture<Void> publishMessage(String key, String message) {
        String targetTopic = properties.producer().topic();
        
        logger.debug("Publishing message with key {} to topic: {}", key, targetTopic);
        
        return kafkaTemplate.send(targetTopic, key, message)
            .thenApply(this::handleSuccess)
            .exceptionally(this::handleFailure);
    }
    
    private Void handleSuccess(SendResult<String, String> result) {
        logger.debug("Message published successfully to topic: {} at offset: {}", 
                    result.getRecordMetadata().topic(), 
                    result.getRecordMetadata().offset());
        metrics.incrementEventsPublished();
        return null;
    }
    
    private Void handleFailure(Throwable throwable) {
        logger.error("Failed to publish message", throwable);
        metrics.incrementPublishErrors();
        throw new RuntimeException("Failed to publish message", throwable);
    }
}