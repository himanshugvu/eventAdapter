package com.orchestrator.postgres.store;

import com.orchestrator.core.store.Event;
import com.orchestrator.core.store.EventStatus;
import com.orchestrator.core.store.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * PostgreSQL implementation of EventStore using JDBC for optimal performance
 */
@Repository
public class PostgresEventStore implements EventStore {
    
    private static final Logger logger = LoggerFactory.getLogger(PostgresEventStore.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    public PostgresEventStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initializeSchema();
    }
    
    private void initializeSchema() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS events (
                    id VARCHAR(255) PRIMARY KEY,
                    payload TEXT NOT NULL,
                    topic_partition VARCHAR(255),
                    offset_value BIGINT,
                    status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
                    received_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    processed_at TIMESTAMP WITH TIME ZONE,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    error_message TEXT,
                    send_timestamp_ns BIGINT,
                    received_at_orchestrator TIMESTAMP WITH TIME ZONE,
                    published_at TIMESTAMP WITH TIME ZONE,
                    total_latency_ms BIGINT,
                    consumer_latency_ms BIGINT,
                    processing_latency_ms BIGINT,
                    publishing_latency_ms BIGINT,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                )
                """);
            
            // Create indexes for better performance
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_events_status ON events(status)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_events_received_at ON events(received_at)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_events_total_latency ON events(total_latency_ms)");
            
            logger.info("PostgreSQL events table and indexes created/verified");
            
        } catch (Exception e) {
            logger.error("Failed to initialize PostgreSQL schema", e);
            throw new RuntimeException("Schema initialization failed", e);
        }
    }
    
    @Override
    public void bulkInsert(List<Event> events) {
        if (events.isEmpty()) {
            return;
        }
        
        try {
            String sql = """
                INSERT INTO events (
                    id, payload, topic_partition, offset_value, status, received_at, 
                    send_timestamp_ns, received_at_orchestrator, total_latency_ms,
                    consumer_latency_ms, processing_latency_ms, publishing_latency_ms,
                    processed_at, published_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Event event = events.get(i);
                    
                    if (event.getReceivedAt() == null) {
                        event.setReceivedAt(Instant.now());
                    }
                    if (event.getStatus() == null) {
                        event.setStatus(EventStatus.RECEIVED);
                    }
                    
                    ps.setString(1, event.getId());
                    ps.setString(2, event.getPayload());
                    ps.setString(3, event.getTopicPartition());
                    ps.setLong(4, event.getOffsetValue());
                    ps.setString(5, event.getStatus().name());
                    ps.setTimestamp(6, Timestamp.from(event.getReceivedAt()));
                    
                    if (event.getSendTimestampNs() != null) {
                        ps.setLong(7, event.getSendTimestampNs());
                    } else {
                        ps.setNull(7, java.sql.Types.BIGINT);
                    }
                    
                    if (event.getReceivedAtOrchestrator() != null) {
                        ps.setTimestamp(8, Timestamp.from(event.getReceivedAtOrchestrator()));
                    } else {
                        ps.setNull(8, java.sql.Types.TIMESTAMP);
                    }
                    
                    setLongOrNull(ps, 9, event.getTotalLatencyMs());
                    setLongOrNull(ps, 10, event.getConsumerLatencyMs());
                    setLongOrNull(ps, 11, event.getProcessingLatencyMs());
                    setLongOrNull(ps, 12, event.getPublishingLatencyMs());
                    
                    if (event.getProcessedAt() != null) {
                        ps.setTimestamp(13, Timestamp.from(event.getProcessedAt()));
                    } else {
                        ps.setNull(13, java.sql.Types.TIMESTAMP);
                    }
                    
                    if (event.getPublishedAt() != null) {
                        ps.setTimestamp(14, Timestamp.from(event.getPublishedAt()));
                    } else {
                        ps.setNull(14, java.sql.Types.TIMESTAMP);
                    }
                }
                
                @Override
                public int getBatchSize() {
                    return events.size();
                }
            });
            
            int totalInserted = java.util.Arrays.stream(results).sum();
            logger.debug("Bulk inserted {} events into PostgreSQL", totalInserted);
            
        } catch (Exception e) {
            logger.error("Failed to bulk insert {} events into PostgreSQL", events.size(), e);
            throw new RuntimeException("Bulk insert failed", e);
        }
    }
    
    private void setLongOrNull(PreparedStatement ps, int parameterIndex, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(parameterIndex, value);
        } else {
            ps.setNull(parameterIndex, java.sql.Types.BIGINT);
        }
    }
    
    @Override
    public void updateStatus(String eventId, EventStatus status) {
        updateStatus(eventId, status, null);
    }
    
    @Override
    public void updateStatus(String eventId, EventStatus status, String errorMessage) {
        try {
            String sql;
            Object[] params;
            
            if (errorMessage != null) {
                sql = "UPDATE events SET status = ?, error_message = ?, updated_at = ? WHERE id = ?";
                params = new Object[]{status.name(), errorMessage, Timestamp.from(Instant.now()), eventId};
            } else {
                sql = "UPDATE events SET status = ?, updated_at = ? WHERE id = ?";
                params = new Object[]{status.name(), Timestamp.from(Instant.now()), eventId};
            }
            
            int rowsAffected = jdbcTemplate.update(sql, params);
            
            if (rowsAffected == 0) {
                logger.warn("No event found with id {} to update status to {}", eventId, status);
            } else {
                logger.debug("Updated event {} status to {}", eventId, status);
            }
            
        } catch (Exception e) {
            logger.error("Failed to update event {} status to {}", eventId, status, e);
            throw new RuntimeException("Status update failed", e);
        }
    }
    
    @Override
    public List<Event> findStaleEvents(Duration threshold) {
        try {
            Instant cutoff = Instant.now().minus(threshold);
            String sql = """
                SELECT id, payload, topic_partition, offset_value, status, received_at,
                       send_timestamp_ns, received_at_orchestrator, total_latency_ms,
                       consumer_latency_ms, processing_latency_ms, publishing_latency_ms,
                       processed_at, published_at, error_message
                FROM events 
                WHERE status = 'RECEIVED' AND received_at < ?
                """;
            
            List<Event> staleEvents = jdbcTemplate.query(sql, (rs, rowNum) -> mapResultSetToEvent(rs), 
                Timestamp.from(cutoff));
            
            logger.debug("Found {} stale events older than {}", staleEvents.size(), threshold);
            return staleEvents;
            
        } catch (Exception e) {
            logger.error("Failed to find stale events", e);
            return List.of();
        }
    }
    
    private Event mapResultSetToEvent(ResultSet rs) throws SQLException {
        Event event = new Event(
            rs.getString("id"),
            rs.getString("payload"),
            rs.getString("topic_partition"),
            rs.getLong("offset_value")
        );
        
        event.setStatus(EventStatus.valueOf(rs.getString("status")));
        event.setReceivedAt(rs.getTimestamp("received_at").toInstant());
        
        long sendTimestampNs = rs.getLong("send_timestamp_ns");
        if (!rs.wasNull()) {
            event.setSendTimestampNs(sendTimestampNs);
        }
        
        Timestamp receivedAtOrchestrator = rs.getTimestamp("received_at_orchestrator");
        if (receivedAtOrchestrator != null) {
            event.setReceivedAtOrchestrator(receivedAtOrchestrator.toInstant());
        }
        
        Timestamp processedAt = rs.getTimestamp("processed_at");
        if (processedAt != null) {
            event.setProcessedAt(processedAt.toInstant());
        }
        
        Timestamp publishedAt = rs.getTimestamp("published_at");
        if (publishedAt != null) {
            event.setPublishedAt(publishedAt.toInstant());
        }
        
        long totalLatency = rs.getLong("total_latency_ms");
        if (!rs.wasNull()) {
            event.setTotalLatencyMs(totalLatency);
        }
        
        long consumerLatency = rs.getLong("consumer_latency_ms");
        if (!rs.wasNull()) {
            event.setConsumerLatencyMs(consumerLatency);
        }
        
        long processingLatency = rs.getLong("processing_latency_ms");
        if (!rs.wasNull()) {
            event.setProcessingLatencyMs(processingLatency);
        }
        
        long publishingLatency = rs.getLong("publishing_latency_ms");
        if (!rs.wasNull()) {
            event.setPublishingLatencyMs(publishingLatency);
        }
        
        event.setErrorMessage(rs.getString("error_message"));
        
        return event;
    }
    
    @Override
    public long countPendingEvents() {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events WHERE status = 'RECEIVED'", Long.class);
        } catch (Exception e) {
            logger.error("Failed to count pending events", e);
            return 0;
        }
    }
    
    @Override
    public long countFailedEvents() {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events WHERE status = 'FAILED'", Long.class);
        } catch (Exception e) {
            logger.error("Failed to count failed events", e);
            return 0;
        }
    }
    
    @Override
    public long countProcessedEvents() {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events WHERE status = 'SUCCESS'", Long.class);
        } catch (Exception e) {
            logger.error("Failed to count processed events", e);
            return 0;
        }
    }
    
    @Override
    public long countSlowEvents() {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events WHERE total_latency_ms > 1000", Long.class);
        } catch (Exception e) {
            logger.error("Failed to count slow events", e);
            return 0;
        }
    }
    
    @Override
    public int cleanupOldEvents(Duration retentionPeriod) {
        try {
            Instant cutoff = Instant.now().minus(retentionPeriod);
            String sql = "DELETE FROM events WHERE created_at < ?";
            
            int deletedCount = jdbcTemplate.update(sql, Timestamp.from(cutoff));
            logger.info("Cleaned up {} old events older than {}", deletedCount, retentionPeriod);
            
            return deletedCount;
            
        } catch (Exception e) {
            logger.error("Failed to cleanup old events", e);
            return 0;
        }
    }
}