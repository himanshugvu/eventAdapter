package com.orchestrator.db.postgres;

import com.orchestrator.core.store.Event;
import com.orchestrator.core.store.EventStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "orchestrator_events", indexes = {
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_source_topic_partition", columnList = "source_topic_partition")
})
public class PostgresEvent {
    
    @Id
    private String id;
    
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;
    
    @Column(name = "source_topic_partition")
    private String sourceTopicPartition;
    
    @Column(name = "offset_value")
    private Long offset;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EventStatus status;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "retry_count")
    private int retryCount;
    
    public PostgresEvent() {}
    
    public PostgresEvent(Event event) {
        this.id = event.getId();
        this.payload = event.getPayload();
        this.sourceTopicPartition = event.getSourceTopicPartition();
        this.offset = event.getOffset();
        this.status = event.getStatus();
        this.createdAt = event.getCreatedAt();
        this.updatedAt = event.getUpdatedAt();
        this.errorMessage = event.getErrorMessage();
        this.retryCount = event.getRetryCount();
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
}