package com.orchestrator.db.postgres;

import com.orchestrator.core.store.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PostgresEventRepository extends JpaRepository<PostgresEvent, String> {
    
    @Query("SELECT e FROM PostgresEvent e WHERE e.status = :status AND e.createdAt < :threshold")
    List<PostgresEvent> findStaleEvents(@Param("status") EventStatus status, @Param("threshold") Instant threshold);
    
    @Query("SELECT e FROM PostgresEvent e WHERE e.status = 'FAILED' AND e.retryCount < :maxRetries")
    List<PostgresEvent> findFailedEventsEligibleForRetry(@Param("maxRetries") int maxRetries);
    
    long countByStatus(EventStatus status);
    
    @Modifying
    @Query("DELETE FROM PostgresEvent e WHERE e.createdAt < :threshold")
    void deleteByCreatedAtBefore(@Param("threshold") Instant threshold);
    
}