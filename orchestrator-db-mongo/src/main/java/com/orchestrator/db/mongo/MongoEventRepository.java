package com.orchestrator.db.mongo;

import com.orchestrator.core.store.EventStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MongoEventRepository extends MongoRepository<MongoEvent, String> {
    
    @Query("{ 'status': ?0, 'createdAt': { $lt: ?1 } }")
    List<MongoEvent> findStaleEvents(EventStatus status, Instant threshold);
    
    @Query("{ 'status': 'FAILED', 'retryCount': { $lt: ?0 } }")
    List<MongoEvent> findFailedEventsEligibleForRetry(int maxRetries);
    
    long countByStatus(EventStatus status);
    
    @Query(value = "{ 'createdAt': { $lt: ?0 } }", delete = true)
    void deleteByCreatedAtBefore(Instant threshold);
}