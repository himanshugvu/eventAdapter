package com.orchestrator.core.store;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface EventStore {
    
    void bulkInsert(List<Event> events);
    
    void updateStatus(String eventId, EventStatus status);
    
    void updateStatus(String eventId, EventStatus status, String errorMessage);
    
    List<Event> findStaleEvents(Duration threshold);
    
    List<Event> findFailedEvents(int maxRetries);
    
    Optional<Event> findById(String eventId);
    
    void incrementRetryCount(String eventId);
    
    long countByStatus(EventStatus status);
    
    void deleteOldEvents(Duration retentionPeriod);
}