package com.orchestrator.core.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "orchestrator")
@Validated
public record OrchestratorProperties(
    @NotNull @Valid ConsumerConfig consumer,
    @NotNull @Valid ProducerConfig producer,
    @NotNull @Valid DatabaseConfig database,
    @Valid ResilienceConfig resilience,
    @Valid MonitoringConfig monitoring
) {
    
    public record ConsumerConfig(
        @NotBlank(message = "Consumer topic is mandatory") 
        String topic,
        
        @NotBlank(message = "Consumer group ID is mandatory") 
        String groupId,
        
        @NotBlank(message = "Consumer bootstrap servers is mandatory") 
        String bootstrapServers,
        
        @Positive int concurrency,
        @Positive int maxPollRecords,
        Duration pollTimeout,
        boolean enableAutoCommit,
        Duration heartbeatInterval,
        Duration sessionTimeout,
        int fetchMinBytes,
        Duration fetchMaxWait,
        int maxPartitionFetchBytes,
        int receiveBufferBytes,
        int sendBufferBytes
    ) {
        // Default constructor with sensible defaults
        public ConsumerConfig(String topic, String groupId, String bootstrapServers) {
            this(
                topic, 
                groupId, 
                bootstrapServers,
                20,  // concurrency - match partition count
                100, // maxPollRecords 
                Duration.ofSeconds(1), // pollTimeout - low latency
                false, // enableAutoCommit
                Duration.ofSeconds(3), // heartbeatInterval
                Duration.ofSeconds(10), // sessionTimeout
                1, // fetchMinBytes - low latency
                Duration.ofMillis(1), // fetchMaxWait - very low wait
                1048576, // maxPartitionFetchBytes - 1MB
                131072, // receiveBufferBytes - 128KB
                131072  // sendBufferBytes - 128KB
            );
        }
    }
    
    public record ProducerConfig(
        @NotBlank(message = "Producer topic is mandatory")
        String topic,
        
        String bootstrapServers, // Can be different from consumer
        
        String acks,
        @Positive int retries,
        Duration requestTimeout,
        boolean enableIdempotence,
        String transactionIdPrefix,
        int batchSize,
        Duration lingerMs,
        String compressionType,
        long bufferMemory,
        int maxInFlightRequestsPerConnection,
        Duration deliveryTimeout
    ) {
        // Default constructor with high-performance defaults
        public ProducerConfig(String topic, String bootstrapServers) {
            this(
                topic,
                bootstrapServers,
                "1", // acks - fast acknowledgment for low latency
                3, // retries
                Duration.ofSeconds(5), // requestTimeout
                true, // enableIdempotence
                "orchestrator-tx-", // transactionIdPrefix
                65536, // batchSize - 64KB batches
                Duration.ofMillis(1), // lingerMs - very low latency
                "snappy", // compressionType - fast compression
                134217728L, // bufferMemory - 128MB buffer
                5, // maxInFlightRequestsPerConnection
                Duration.ofSeconds(5) // deliveryTimeout
            );
        }
    }
    
    public record DatabaseConfig(
        DatabaseStrategy strategy,
        Duration staleEventThreshold,
        int maxRetries,
        Duration retentionPeriod,
        int bulkSize
    ) {
        // Default constructor
        public DatabaseConfig() {
            this(
                DatabaseStrategy.LIGHTWEIGHT, // Default to fastest strategy
                Duration.ofMinutes(30),
                3,
                Duration.ofDays(14),
                200
            );
        }
    }
    
    public record ResilienceConfig(
        Duration initialBackoff,
        Duration maxBackoff,
        double backoffMultiplier,
        boolean enableCircuitBreaker,
        int circuitBreakerFailureThreshold,
        Duration circuitBreakerRecoveryTimeout
    ) {
        // Default constructor
        public ResilienceConfig() {
            this(
                Duration.ofSeconds(2),
                Duration.ofMinutes(2),
                2.0,
                true,
                5,
                Duration.ofMinutes(1)
            );
        }
    }
    
    public record MonitoringConfig(
        boolean enableMetrics,
        boolean enableHealthChecks,
        String metricsPrefix
    ) {
        // Default constructor
        public MonitoringConfig() {
            this(true, true, "orchestrator");
        }
    }
    
    public enum DatabaseStrategy {
        OUTBOX,     // Bulk consume → bulk insert → process → update
        RELIABLE,   // Insert before publish → update after
        LIGHTWEIGHT // Only log failures, fastest option
    }
}