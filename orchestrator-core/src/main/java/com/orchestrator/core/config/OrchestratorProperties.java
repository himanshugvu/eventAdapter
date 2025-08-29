package com.orchestrator.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Duration;

@ConfigurationProperties(prefix = "orchestrator")
@Validated
public record OrchestratorProperties(
    
    Consumer consumer,
    Producer producer,
    Database database,
    Resilience resilience,
    Monitoring monitoring
) {
    
    public record Consumer(
        @NotBlank String topic,
        @NotBlank String groupId,
        @DefaultValue("10") @Positive int concurrency,
        @DefaultValue("100") @Positive int maxPollRecords,
        @DefaultValue("PT30S") Duration pollTimeout,
        @DefaultValue("true") boolean enableAutoCommit
    ) {}
    
    public record Producer(
        @NotBlank String topic,
        @DefaultValue("all") String acks,
        @DefaultValue("3") @Positive int retries,
        @DefaultValue("PT5S") Duration requestTimeout,
        @DefaultValue("true") boolean enableIdempotence,
        @DefaultValue("exactly_once_v2") String transactionIdPrefix
    ) {}
    
    public record Database(
        @DefaultValue("RELIABLE") Strategy strategy,
        @DefaultValue("PT30M") Duration staleEventThreshold,
        @DefaultValue("3") @Positive int maxRetries,
        @DefaultValue("P7D") Duration retentionPeriod,
        @DefaultValue("100") @Positive int bulkSize
    ) {
        public enum Strategy {
            OUTBOX,
            RELIABLE, 
            LIGHTWEIGHT
        }
    }
    
    public record Resilience(
        @DefaultValue("PT1S") Duration initialBackoff,
        @DefaultValue("PT60S") Duration maxBackoff,
        @DefaultValue("2.0") double backoffMultiplier,
        @DefaultValue("true") boolean enableCircuitBreaker,
        @DefaultValue("10") @Positive int circuitBreakerFailureThreshold,
        @DefaultValue("PT30S") Duration circuitBreakerRecoveryTimeout
    ) {}
    
    public record Monitoring(
        @DefaultValue("true") boolean enableMetrics,
        @DefaultValue("true") boolean enableHealthChecks,
        @DefaultValue("orchestrator") String metricsPrefix
    ) {}
}