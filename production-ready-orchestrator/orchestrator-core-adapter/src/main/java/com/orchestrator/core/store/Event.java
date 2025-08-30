package com.orchestrator.core.store;

import java.time.Instant;
import java.util.Objects;

/**
 * Core event entity representing a message in the orchestrator pipeline.
 * Contains comprehensive timing metrics for latency tracking.
 */
public class Event {
    
    private String id;
    private String payload;
    private String sourceTopicPartition;
    private Long offset;
    
    // Additional fields for compatibility
    private Instant receivedAt;
    private String topicPartition;
    private Long offsetValue;
    private Long consumerLatencyMs;
    private Long processingLatencyMs;
    private Long publishingLatencyMs;
    private EventStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String errorMessage;
    private int retryCount;
    
    // Timing metrics for latency tracking
    private Long sendTimestampNs;           // Original send timestamp from load test
    private Instant receivedAtOrchestrator; // When message arrived at orchestrator
    private Instant processedAt;            // When message processing completed
    private Instant publishedAt;            // When message was published to output topic
    private Long totalLatencyMs;            // Total time from send to completion
    private Boolean exceededOneSecond;      // Flag for messages > 1 second
    
    public Event() {}
    
    public Event(String id, String payload, String sourceTopicPartition, Long offset) {
        this.id = id;
        this.payload = payload;
        this.sourceTopicPartition = sourceTopicPartition;
        this.topicPartition = sourceTopicPartition;
        this.offset = offset;
        this.offsetValue = offset;
        this.status = EventStatus.RECEIVED;
        this.createdAt = Instant.now();
        this.receivedAt = Instant.now();
        this.updatedAt = Instant.now();
        this.retryCount = 0;
    }
    
    // Basic getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    
    public String getSourceTopicPartition() { return sourceTopicPartition; }
    public void setSourceTopicPartition(String sourceTopicPartition) { this.sourceTopicPartition = sourceTopicPartition; }
    
    public Long getOffset() { return offset; }
    public void setOffset(Long offset) { this.offset = offset; }
    
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { 
        this.status = status; 
        this.updatedAt = Instant.now();
    }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = Instant.now();
    }
    
    // Getters and setters for timing metrics
    public Long getSendTimestampNs() { return sendTimestampNs; }
    public void setSendTimestampNs(Long sendTimestampNs) { this.sendTimestampNs = sendTimestampNs; }
    
    public Instant getReceivedAtOrchestrator() { return receivedAtOrchestrator; }
    public void setReceivedAtOrchestrator(Instant receivedAtOrchestrator) { this.receivedAtOrchestrator = receivedAtOrchestrator; }
    
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    
    public Long getTotalLatencyMs() { return totalLatencyMs; }
    public void setTotalLatencyMs(Long totalLatencyMs) { this.totalLatencyMs = totalLatencyMs; }
    
    public Boolean getExceededOneSecond() { return exceededOneSecond; }
    public void setExceededOneSecond(Boolean exceededOneSecond) { this.exceededOneSecond = exceededOneSecond; }
    
    // Additional compatibility getters and setters
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    
    public String getTopicPartition() { return topicPartition; }
    public void setTopicPartition(String topicPartition) { this.topicPartition = topicPartition; }
    
    public Long getOffsetValue() { return offsetValue; }
    public void setOffsetValue(Long offsetValue) { this.offsetValue = offsetValue; }
    
    public Long getConsumerLatencyMs() { return consumerLatencyMs; }
    public void setConsumerLatencyMs(Long consumerLatencyMs) { this.consumerLatencyMs = consumerLatencyMs; }
    
    public Long getProcessingLatencyMs() { return processingLatencyMs; }
    public void setProcessingLatencyMs(Long processingLatencyMs) { this.processingLatencyMs = processingLatencyMs; }
    
    public Long getPublishingLatencyMs() { return publishingLatencyMs; }
    public void setPublishingLatencyMs(Long publishingLatencyMs) { this.publishingLatencyMs = publishingLatencyMs; }
    
    /**
     * Helper method to calculate and set timing metrics
     */
    public void calculateTimingMetrics() {
        if (sendTimestampNs != null && publishedAt != null) {
            long sendTimeMs = sendTimestampNs / 1_000_000; // Convert nanoseconds to milliseconds
            long publishedTimeMs = publishedAt.toEpochMilli();
            this.totalLatencyMs = publishedTimeMs - sendTimeMs;
            this.exceededOneSecond = this.totalLatencyMs > 1000;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", retryCount=" + retryCount +
                ", totalLatencyMs=" + totalLatencyMs +
                '}';
    }
}