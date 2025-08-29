package com.orchestrator.db.postgres;

import com.orchestrator.core.store.Event;
import com.orchestrator.core.store.EventStatus;
import com.orchestrator.core.store.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostgresEventStore implements EventStore {
    
    private static final Logger logger = LoggerFactory.getLogger(PostgresEventStore.class);
    
    private final PostgresEventRepository repository;
    private final JdbcTemplate jdbcTemplate;
    
    public PostgresEventStore(PostgresEventRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    @Transactional
    public void bulkInsert(List<Event> events) {
        if (events.isEmpty()) {
            return;
        }
        
        try {
            String sql = """
                INSERT INTO orchestrator_events 
                (id, payload, source_topic_partition, offset_value, status, created_at, updated_at, retry_count) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            jdbcTemplate.batchUpdate(sql, events, events.size(), (ps, event) -> {
                ps.setString(1, event.getId());
                ps.setString(2, event.getPayload());
                ps.setString(3, event.getSourceTopicPartition());
                ps.setObject(4, event.getOffset());
                ps.setString(5, event.getStatus().name());
                ps.setObject(6, event.getCreatedAt());
                ps.setObject(7, event.getUpdatedAt());
                ps.setInt(8, event.getRetryCount());
            });
            
            logger.debug("Bulk inserted {} events into PostgreSQL", events.size());
            
        } catch (Exception e) {
            logger.error("Failed to bulk insert {} events", events.size(), e);
            throw new RuntimeException("Failed to bulk insert events", e);
        }
    }
    
    @Override
    @Transactional
    public void updateStatus(String eventId, EventStatus status) {
        try {
            String sql = "UPDATE orchestrator_events SET status = ?, updated_at = ? WHERE id = ?";
            int updated = jdbcTemplate.update(sql, status.name(), Instant.now(), eventId);
            
            if (updated > 0) {
                logger.debug("Updated status for event {} to {}", eventId, status);
            } else {
                logger.warn("No event found with ID: {}", eventId);
            }
            
        } catch (Exception e) {
            logger.error("Failed to update status for event: {}", eventId, e);
            throw new RuntimeException("Failed to update event status", e);
        }
    }
    
    @Override
    @Transactional
    public void updateStatus(String eventId, EventStatus status, String errorMessage) {
        try {
            String sql = "UPDATE orchestrator_events SET status = ?, error_message = ?, updated_at = ? WHERE id = ?";
            int updated = jdbcTemplate.update(sql, status.name(), errorMessage, Instant.now(), eventId);
            
            if (updated > 0) {
                logger.debug("Updated status for event {} to {} with error: {}", eventId, status, errorMessage);
            } else {
                logger.warn("No event found with ID: {}", eventId);
            }
            
        } catch (Exception e) {
            logger.error("Failed to update status for event: {}", eventId, e);
            throw new RuntimeException("Failed to update event status", e);
        }
    }
    
    @Override
    public List<Event> findStaleEvents(Duration threshold) {
        try {
            Instant cutoff = Instant.now().minus(threshold);
            List<PostgresEvent> staleEvents = repository.findStaleEvents(EventStatus.RECEIVED, cutoff);
            
            return staleEvents.stream()
                .map(PostgresEvent::toEvent)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Failed to find stale events", e);
            throw new RuntimeException("Failed to find stale events", e);
        }
    }
    
    @Override
    public List<Event> findFailedEvents(int maxRetries) {
        try {
            List<PostgresEvent> failedEvents = repository.findFailedEventsEligibleForRetry(maxRetries);
            
            return failedEvents.stream()
                .map(PostgresEvent::toEvent)
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
                .map(PostgresEvent::toEvent);
                
        } catch (Exception e) {
            logger.error("Failed to find event by ID: {}", eventId, e);
            throw new RuntimeException("Failed to find event", e);
        }
    }
    
    @Override
    @Transactional
    public void incrementRetryCount(String eventId) {
        try {
            String sql = "UPDATE orchestrator_events SET retry_count = retry_count + 1, updated_at = ? WHERE id = ?";
            int updated = jdbcTemplate.update(sql, Instant.now(), eventId);
            
            if (updated > 0) {
                logger.debug("Incremented retry count for event: {}", eventId);
            } else {
                logger.warn("No event found with ID: {}", eventId);
            }
            
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
    @Transactional
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