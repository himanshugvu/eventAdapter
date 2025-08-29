package com.orchestrator.db.mongo;

import com.orchestrator.core.store.Event;
import com.orchestrator.core.store.EventStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "orchestrator_events")
public class MongoEvent {
    
    @Id
    private String id;
    
    private String payload;
    
    @Indexed
    private String sourceTopicPartition;
    
    private Long offset;
    
    @Indexed
    private EventStatus status;
    
    @Indexed
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
    @Indexed
    private Boolean exceededOneSecond;      // Flag for messages > 1 second
    
    public MongoEvent() {}
    
    public MongoEvent(Event event) {
        this.id = event.getId();
        this.payload = event.getPayload();
        this.sourceTopicPartition = event.getSourceTopicPartition();
        this.offset = event.getOffset();
        this.status = event.getStatus();
        this.createdAt = event.getCreatedAt();
        this.updatedAt = event.getUpdatedAt();
        this.errorMessage = event.getErrorMessage();
        this.retryCount = event.getRetryCount();
        
        // Copy timing metrics
        this.sendTimestampNs = event.getSendTimestampNs();
        this.receivedAtOrchestrator = event.getReceivedAtOrchestrator();
        this.processedAt = event.getProcessedAt();
        this.publishedAt = event.getPublishedAt();
        this.totalLatencyMs = event.getTotalLatencyMs();
        this.exceededOneSecond = event.getExceededOneSecond();
    }
    
    public Event toEvent() {
        Event event = new Event();
        event.setId(this.id);
        event.setPayload(this.payload);
        event.setSourceTopicPartition(this.sourceTopicPartition);
        event.setOffset(this.offset);
        event.setStatus(this.status);
        event.setCreatedAt(this.createdAt);
        event.setUpdatedAt(this.updatedAt);
        event.setErrorMessage(this.errorMessage);
        event.setRetryCount(this.retryCount);
        
        // Copy timing metrics
        event.setSendTimestampNs(this.sendTimestampNs);
        event.setReceivedAtOrchestrator(this.receivedAtOrchestrator);
        event.setProcessedAt(this.processedAt);
        event.setPublishedAt(this.publishedAt);
        event.setTotalLatencyMs(this.totalLatencyMs);
        event.setExceededOneSecond(this.exceededOneSecond);
        
        return event;
    }
    
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
}