package com.orchestrator.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import com.orchestrator.core.config.OrchestratorProperties;
import com.orchestrator.core.store.EventStatus;
import com.orchestrator.core.store.EventStore;
import org.springframework.stereotype.Component;

@Component
public class OrchestratorMetrics {
    
    private final Counter eventsReceivedCounter;
    private final Counter eventsProcessedCounter;
    private final Counter eventsPublishedCounter;
    private final Counter processingErrorsCounter;
    private final Counter publishErrorsCounter;
    private final Timer processingTimer;
    private final Timer publishingTimer;
    private final EventStore eventStore;
    
    public OrchestratorMetrics(MeterRegistry meterRegistry, 
                              OrchestratorProperties properties,
                              EventStore eventStore) {
        this.eventStore = eventStore;
        String prefix = properties.monitoring().metricsPrefix();
        
        this.eventsReceivedCounter = Counter.builder(prefix + ".events.received")
            .description("Total number of events received from source topic")
            .register(meterRegistry);
            
        this.eventsProcessedCounter = Counter.builder(prefix + ".events.processed")
            .description("Total number of events successfully processed")
            .register(meterRegistry);
            
        this.eventsPublishedCounter = Counter.builder(prefix + ".events.published")
            .description("Total number of events successfully published to target topic")
            .register(meterRegistry);
            
        this.processingErrorsCounter = Counter.builder(prefix + ".processing.errors")
            .description("Total number of processing errors")
            .register(meterRegistry);
            
        this.publishErrorsCounter = Counter.builder(prefix + ".publishing.errors")
            .description("Total number of publishing errors")
            .register(meterRegistry);
            
        this.processingTimer = Timer.builder(prefix + ".processing.time")
            .description("Time taken to process events")
            .register(meterRegistry);
            
        this.publishingTimer = Timer.builder(prefix + ".publishing.time")
            .description("Time taken to publish events")
            .register(meterRegistry);
        
        Gauge.builder(prefix + ".events.pending", this, metrics -> metrics.eventStore.countByStatus(EventStatus.RECEIVED))
            .description("Number of events with RECEIVED status")
            .register(meterRegistry);
            
        Gauge.builder(prefix + ".events.failed", this, metrics -> metrics.eventStore.countByStatus(EventStatus.FAILED))
            .description("Number of events with FAILED status")
            .register(meterRegistry);
    }
    
    public void incrementEventsReceived() {
        eventsReceivedCounter.increment();
    }
    
    public void incrementEventsReceived(int count) {
        eventsReceivedCounter.increment(count);
    }
    
    public void incrementEventsProcessed() {
        eventsProcessedCounter.increment();
    }
    
    public void incrementEventsProcessed(int count) {
        eventsProcessedCounter.increment(count);
    }
    
    public void incrementEventsPublished() {
        eventsPublishedCounter.increment();
    }
    
    public void incrementProcessingErrors() {
        processingErrorsCounter.increment();
    }
    
    public void incrementPublishErrors() {
        publishErrorsCounter.increment();
    }
    
    public Timer.Sample startProcessingTimer() {
        return Timer.start();
    }
    
    public void recordProcessingTime(Timer.Sample sample) {
        sample.stop(processingTimer);
    }
    
    public Timer.Sample startPublishingTimer() {
        return Timer.start();
    }
    
    public void recordPublishingTime(Timer.Sample sample) {
        sample.stop(publishingTimer);
    }
}