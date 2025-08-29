package com.orchestrator.db.mongo;

import com.orchestrator.core.store.Event;
import com.orchestrator.core.store.EventStatus;
import com.orchestrator.core.store.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MongoEventStore implements EventStore {
    
    private static final Logger logger = LoggerFactory.getLogger(MongoEventStore.class);
    
    private final MongoEventRepository repository;
    private final MongoTemplate mongoTemplate;
    
    public MongoEventStore(MongoEventRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }
    
    @Override
    public void bulkInsert(List<Event> events) {
        if (events.isEmpty()) {
            return;
        }
        
        try {
            List<MongoEvent> mongoEvents = events.stream()
                .map(MongoEvent::new)
                .collect(Collectors.toList());
            
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, MongoEvent.class);
            bulkOps.insert(mongoEvents);
            bulkOps.execute();
            
            logger.debug("Bulk inserted {} events into MongoDB", events.size());
            
        } catch (Exception e) {
            logger.error("Failed to bulk insert {} events", events.size(), e);
            throw new RuntimeException("Failed to bulk insert events", e);
        }
    }
    
    @Override
    public void updateStatus(String eventId, EventStatus status) {
        updateStatus(eventId, status, null);
    }
    
    @Override
    public void updateStatus(String eventId, EventStatus status, String errorMessage) {
        try {
            Query query = new Query(Criteria.where("id").is(eventId));
            Update update = new Update()
                .set("status", status)
                .set("updatedAt", Instant.now());
            
            if (errorMessage != null) {
                update.set("errorMessage", errorMessage);
            }
            
            mongoTemplate.updateFirst(query, update, MongoEvent.class);
            logger.debug("Updated status for event {} to {}", eventId, status);
            
        } catch (Exception e) {
            logger.error("Failed to update status for event: {}", eventId, e);
            throw new RuntimeException("Failed to update event status", e);
        }
    }
    
    @Override
    public List<Event> findStaleEvents(Duration threshold) {
        try {
            Instant cutoff = Instant.now().minus(threshold);
            List<MongoEvent> staleEvents = repository.findStaleEvents(EventStatus.RECEIVED, cutoff);
            
            return staleEvents.stream()
                .map(MongoEvent::toEvent)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Failed to find stale events", e);
            throw new RuntimeException("Failed to find stale events", e);
        }
    }
    
    @Override
    public List<Event> findFailedEvents(int maxRetries) {
        try {
            List<MongoEvent> failedEvents = repository.findFailedEventsEligibleForRetry(maxRetries);
            
            return failedEvents.stream()
                .map(MongoEvent::toEvent)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Failed to find failed events", e);
            throw new RuntimeException("Failed to find failed events", e);
        }
    }
    
    @Override
    public Optional<Event> findById(String eventId) {
        try {
            return repository.findById(eventId)
                .map(MongoEvent::toEvent);
                
        } catch (Exception e) {
            logger.error("Failed to find event by ID: {}", eventId, e);
            throw new RuntimeException("Failed to find event", e);
        }
    }
    
    @Override
    public void incrementRetryCount(String eventId) {
        try {
            Query query = new Query(Criteria.where("id").is(eventId));
            Update update = new Update()
                .inc("retryCount", 1)
                .set("updatedAt", Instant.now());
            
            mongoTemplate.updateFirst(query, update, MongoEvent.class);
            logger.debug("Incremented retry count for event: {}", eventId);
            
        } catch (Exception e) {
            logger.error("Failed to increment retry count for event: {}", eventId, e);
            throw new RuntimeException("Failed to increment retry count", e);
        }
    }
    
    @Override
    public long countByStatus(EventStatus status) {
        try {
            return repository.countByStatus(status);
            
        } catch (Exception e) {
            logger.error("Failed to count events by status: {}", status, e);
            return 0;
        }
    }
    
    @Override
    public void deleteOldEvents(Duration retentionPeriod) {
        try {
            Instant threshold = Instant.now().minus(retentionPeriod);
            repository.deleteByCreatedAtBefore(threshold);
            logger.debug("Deleted events older than {}", threshold);
            
        } catch (Exception e) {
            logger.error("Failed to delete old events", e);
        }
    }
}