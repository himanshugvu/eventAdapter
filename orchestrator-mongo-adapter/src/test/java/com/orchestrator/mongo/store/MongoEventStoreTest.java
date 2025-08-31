package com.orchestrator.mongo.store;

import com.orchestrator.core.store.Event;
import com.orchestrator.core.store.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MongoEventStoreTest {

    @Mock
    private MongoTemplate mongoTemplate;
    
    private MongoEventStore mongoEventStore;

    @BeforeEach
    void setUp() {
        mongoEventStore = new MongoEventStore(mongoTemplate);
    }

    @Test
    void testBulkInsert() {
        Event event1 = new Event("id1", "payload1", "topic", 0, 100L);
        Event event2 = new Event("id2", "payload2", "topic", 0, 101L);
        List<Event> events = List.of(event1, event2);
        
        mongoEventStore.bulkInsert(events);
        
        verify(mongoTemplate).insertAll(events);
    }

    @Test
    void testUpdateStatus() {
        mongoEventStore.updateStatus("test-id", EventStatus.SUCCESS);
        
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Event.class));
    }

    @Test
    void testUpdateStatusWithError() {
        mongoEventStore.updateStatus("test-id", EventStatus.FAILED, "error message");
        
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Event.class));
    }

    @Test
    void testFindStaleEvents() {
        Duration threshold = Duration.ofMinutes(30);
        when(mongoTemplate.find(any(Query.class), eq(Event.class)))
            .thenReturn(List.of(new Event("id", "payload", "topic", 0, 100L)));
        
        List<Event> staleEvents = mongoEventStore.findStaleEvents(threshold);
        
        assertNotNull(staleEvents);
        verify(mongoTemplate).find(any(Query.class), eq(Event.class));
    }

    @Test
    void testCountPendingEvents() {
        when(mongoTemplate.count(any(Query.class), eq(Event.class))).thenReturn(10L);
        
        long count = mongoEventStore.countPendingEvents();
        
        assertEquals(10L, count);
        verify(mongoTemplate).count(any(Query.class), eq(Event.class));
    }

    @Test
    void testCountFailedEvents() {
        when(mongoTemplate.count(any(Query.class), eq(Event.class))).thenReturn(5L);
        
        long count = mongoEventStore.countFailedEvents();
        
        assertEquals(5L, count);
        verify(mongoTemplate).count(any(Query.class), eq(Event.class));
    }

    @Test
    void testCountProcessedEvents() {
        when(mongoTemplate.count(any(Query.class), eq(Event.class))).thenReturn(100L);
        
        long count = mongoEventStore.countProcessedEvents();
        
        assertEquals(100L, count);
        verify(mongoTemplate).count(any(Query.class), eq(Event.class));
    }

    @Test
    void testCountSlowEvents() {
        when(mongoTemplate.count(any(Query.class), eq(Event.class))).thenReturn(3L);
        
        long count = mongoEventStore.countSlowEvents();
        
        assertEquals(3L, count);
        verify(mongoTemplate).count(any(Query.class), eq(Event.class));
    }

    @Test
    void testCleanupOldEvents() {
        Duration retentionPeriod = Duration.ofDays(7);
        when(mongoTemplate.remove(any(Query.class), eq(Event.class)))
            .thenReturn(org.springframework.data.mongodb.core.query.Query.query(org.springframework.data.mongodb.core.query.Criteria.where("id").is("test")));
        
        int deletedCount = mongoEventStore.cleanupOldEvents(retentionPeriod);
        
        verify(mongoTemplate).remove(any(Query.class), eq(Event.class));
    }
}