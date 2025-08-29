package com.orchestrator.core.service;

import com.orchestrator.core.config.OrchestratorProperties;
import com.orchestrator.core.store.Event;
import com.orchestrator.core.store.EventStatus;
import com.orchestrator.core.store.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class EventMaintenanceService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventMaintenanceService.class);
    
    private final EventStore eventStore;
    private final OrchestratorProperties properties;
    
    public EventMaintenanceService(EventStore eventStore, OrchestratorProperties properties) {
        this.eventStore = eventStore;
        this.properties = properties;
    }
    
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void markStaleEvents() {
        try {
            Duration threshold = properties.database().staleEventThreshold();
            List<Event> staleEvents = eventStore.findStaleEvents(threshold);
            
            if (!staleEvents.isEmpty()) {
                logger.warn("Found {} stale events older than {}", staleEvents.size(), threshold);
                
                for (Event event : staleEvents) {
                    eventStore.updateStatus(event.getId(), EventStatus.FAILED, "Event timeout - exceeded threshold");
                }
                
                logger.info("Marked {} stale events as FAILED", staleEvents.size());
            }
            
        } catch (Exception e) {
            logger.error("Error during stale event cleanup", e);
        }
    }
    
    @Scheduled(fixedDelay = 3600000) // Every hour
    public void cleanupOldEvents() {
        try {
            Duration retentionPeriod = properties.database().retentionPeriod();
            eventStore.deleteOldEvents(retentionPeriod);
            logger.debug("Cleaned up events older than {}", retentionPeriod);
            
        } catch (Exception e) {
            logger.error("Error during old event cleanup", e);
        }
    }
    
    @Scheduled(fixedDelay = 600000) // Every 10 minutes
    public void retryFailedEvents() {
        try {
            int maxRetries = properties.database().maxRetries();
            List<Event> failedEvents = eventStore.findFailedEvents(maxRetries);
            
            if (!failedEvents.isEmpty()) {
                logger.info("Found {} failed events eligible for retry", failedEvents.size());
                
                for (Event event : failedEvents) {
                    if (event.getRetryCount() < maxRetries) {
                        eventStore.updateStatus(event.getId(), EventStatus.RETRY);
                        eventStore.incrementRetryCount(event.getId());
                        logger.debug("Marked event {} for retry (attempt {})", event.getId(), event.getRetryCount() + 1);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during failed event retry", e);
        }
    }
}