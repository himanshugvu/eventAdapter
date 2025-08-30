package com.orchestrator.core.store;

import java.time.Duration;
import java.util.List;

/**
 * Database abstraction interface for event storage operations.
 * Implementations provide database-specific storage logic.
 */
public interface EventStore {
    
    /**
     * Bulk insert events into the database for optimal performance
     * @param events List of events to insert
     */
    void bulkInsert(List<Event> events);
    
    /**
     * Update event status by ID
     * @param eventId Event identifier
     * @param status New status
     */
    void updateStatus(String eventId, EventStatus status);
    
    /**
     * Update event status with error message
     * @param eventId Event identifier  
     * @param status New status
     * @param errorMessage Error details
     */
    void updateStatus(String eventId, EventStatus status, String errorMessage);
    
    /**
     * Find events that have been in RECEIVED status longer than threshold
     * @param threshold Duration threshold for stale events
     * @return List of stale events
     */
    List<Event> findStaleEvents(Duration threshold);
    
    /**
     * Get count of pending events
     * @return Number of events in RECEIVED/PROCESSING status
     */
    long countPendingEvents();
    
    /**
     * Get count of failed events  
     * @return Number of events in FAILED status
     */
    long countFailedEvents();
    
    /**
     * Get count of successfully processed events
     * @return Number of events in SUCCESS status
     */
    long countProcessedEvents();
    
    /**
     * Delete events older than retention period
     * @param retentionPeriod Age threshold for cleanup
     * @return Number of deleted events
     */
    int cleanupOldEvents(Duration retentionPeriod);
    
    /**
     * Get count of events exceeding latency threshold
     * @return Number of events with totalLatencyMs > 1000
     */
    long countSlowEvents();
}