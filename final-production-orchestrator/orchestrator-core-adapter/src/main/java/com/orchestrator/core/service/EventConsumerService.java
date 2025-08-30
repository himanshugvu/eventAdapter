package com.orchestrator.core.service;

import com.orchestrator.core.config.OrchestratorProperties;
import com.orchestrator.core.metrics.LatencyTracker;
import com.orchestrator.core.store.Event;
import com.orchestrator.core.store.EventStatus;
import com.orchestrator.core.store.EventStore;
import com.orchestrator.core.transformer.MessageTransformer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class EventConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventConsumerService.class);
    
    private final EventStore eventStore;
    private final EventPublisherService publisherService;
    private final MessageTransformer messageTransformer;
    private final OrchestratorProperties properties;
    private final LatencyTracker latencyTracker;
    
    public EventConsumerService(
            EventStore eventStore,
            EventPublisherService publisherService,
            MessageTransformer messageTransformer,
            OrchestratorProperties properties,
            LatencyTracker latencyTracker) {
        this.eventStore = eventStore;
        this.publisherService = publisherService;
        this.messageTransformer = messageTransformer;
        this.properties = properties;
        this.latencyTracker = latencyTracker;
    }
    
    @KafkaListener(
        topics = "${orchestrator.consumer.topic}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeEvents(
            ConsumerRecord<String, String> record,
            Acknowledgment acknowledgment) {
        
        Instant receivedAt = Instant.now();
        
        Long sendTimestampNs = extractSendTimestamp(record);
        String messageId = extractMessageId(record);
        String source = extractSource(record);
        
        logger.info("CONSUMER RECEIVED: messageId={}, source={}, topic={}, partition={}, offset={}, receivedAt={}", 
                   messageId, source, record.topic(), record.partition(), record.offset(), receivedAt);
        
        if (sendTimestampNs != null && sendTimestampNs > 0) {
            latencyTracker.recordConsumerLatency(sendTimestampNs, receivedAt);
        }
        
        try {
            Event event = createEventWithTiming(record, sendTimestampNs, receivedAt);
            
            switch (properties.database().strategy()) {
                case OUTBOX -> processOutboxModeWithTiming(event);
                case RELIABLE -> processReliableModeWithTiming(event);
                case LIGHTWEIGHT -> processLightweightModeWithTiming(event);
            }
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("CONSUMER ERROR: Failed to process messageId={} from topic={}: {}", 
                        messageId, record.topic(), e.getMessage(), e);
            throw e;
        }
    }
    
    private Long extractSendTimestamp(ConsumerRecord<String, String> record) {
        try {
            if (record.headers() != null) {
                var timestampHeader = record.headers().lastHeader("send_timestamp_ns");
                if (timestampHeader != null) {
                    return Long.parseLong(new String(timestampHeader.value()));
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract send timestamp from headers: {}", e.getMessage());
        }
        return null;
    }
    
    private String extractMessageId(ConsumerRecord<String, String> record) {
        try {
            if (record.headers() != null) {
                var messageIdHeader = record.headers().lastHeader("message_id");
                if (messageIdHeader != null) {
                    return new String(messageIdHeader.value());
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract message ID from headers: {}", e.getMessage());
        }
        return record.key(); // Fallback to record key
    }
    
    private String extractSource(ConsumerRecord<String, String> record) {
        try {
            if (record.headers() != null) {
                var sourceHeader = record.headers().lastHeader("source");
                if (sourceHeader != null) {
                    return new String(sourceHeader.value());
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract source from headers: {}", e.getMessage());
        }
        return "unknown";
    }
    
    private Event createEventWithTiming(ConsumerRecord<String, String> record, Long sendTimestampNs, Instant receivedAt) {
        String eventId = UUID.randomUUID().toString();
        String topicPartition = record.topic() + "-" + record.partition();
        Event event = new Event(eventId, record.value(), topicPartition, record.offset());
        event.setSendTimestampNs(sendTimestampNs);
        event.setReceivedAtOrchestrator(receivedAt);
        return event;
    }
    
    private void processOutboxModeWithTiming(Event event) {
        Instant processingStart = Instant.now();
        
        eventStore.bulkInsert(List.of(event));
        
        CompletableFuture<Void> future = transformAndPublishAsyncWithTiming(event, processingStart);
        
        future.whenComplete((result, throwable) -> {
            if (throwable == null) {
                logger.debug("Successfully processed event {} in OUTBOX mode", event.getId());
            } else {
                logger.error("Failed to process event {} in OUTBOX mode: {}", event.getId(), throwable.getMessage());
            }
        });
    }
    
    private void processReliableModeWithTiming(Event event) {
        Instant processingStart = Instant.now();
        
        try {
            eventStore.bulkInsert(List.of(event));
            
            String transformedMessage = messageTransformer.transform(event.getPayload());
            
            Instant publishStart = Instant.now();
            latencyTracker.recordProcessingLatency(processingStart, publishStart);
            
            publisherService.publishMessage(transformedMessage)
                .thenAccept(result -> {
                    Instant publishEnd = Instant.now();
                    event.setProcessedAt(publishStart);
                    event.setPublishedAt(publishEnd);
                    event.calculateTimingMetrics();
                    
                    updateEventStatusWithTiming(event.getId(), EventStatus.SUCCESS, event);
                    latencyTracker.recordPublishingLatency(publishStart, publishEnd);
                    
                    if (event.getTotalLatencyMs() != null) {
                        latencyTracker.recordEndToEndLatency(event.getTotalLatencyMs(), event.getSendTimestampNs());
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("PRODUCER ERROR: Failed to publish event {}: {}", event.getId(), throwable.getMessage());
                    updateEventStatusWithTiming(event.getId(), EventStatus.FAILED, event, throwable.getMessage());
                    return null;
                });
                
        } catch (Exception e) {
            logger.error("Failed to process event in RELIABLE mode: {}", event.getId(), e);
            updateEventStatusWithTiming(event.getId(), EventStatus.FAILED, event, e.getMessage());
        }
    }
    
    private void processLightweightModeWithTiming(Event event) {
        Instant processingStart = Instant.now();
        
        try {
            String transformedMessage = messageTransformer.transform(event.getPayload());
            
            Instant publishStart = Instant.now();
            latencyTracker.recordProcessingLatency(processingStart, publishStart);
            
            publisherService.publishMessage(transformedMessage)
                .thenAccept(result -> {
                    Instant publishEnd = Instant.now();
                    event.setProcessedAt(publishStart);
                    event.setPublishedAt(publishEnd);
                    event.calculateTimingMetrics();
                    
                    latencyTracker.recordPublishingLatency(publishStart, publishEnd);
                    
                    if (event.getTotalLatencyMs() != null) {
                        latencyTracker.recordEndToEndLatency(event.getTotalLatencyMs(), event.getSendTimestampNs());
                    }
                })
                .exceptionally(throwable -> {
                    Event failedEvent = event;
                    failedEvent.setStatus(EventStatus.FAILED);
                    failedEvent.setErrorMessage(throwable.getMessage());
                    eventStore.bulkInsert(List.of(failedEvent));
                    logger.error("PRODUCER ERROR: Failed to publish event, logged to DB: {}", failedEvent.getId(), throwable);
                    return null;
                });
                
        } catch (Exception e) {
            Event failedEvent = event;
            failedEvent.setStatus(EventStatus.FAILED);
            failedEvent.setErrorMessage(e.getMessage());
            eventStore.bulkInsert(List.of(failedEvent));
            logger.error("Failed to process event in LIGHTWEIGHT mode: {}", failedEvent.getId(), e);
        }
    }
    
    private void updateEventStatusWithTiming(String eventId, EventStatus status, Event event) {
        updateEventStatusWithTiming(eventId, status, event, null);
    }
    
    private void updateEventStatusWithTiming(String eventId, EventStatus status, Event event, String errorMessage) {
        CompletableFuture.runAsync(() -> {
            if (errorMessage != null) {
                eventStore.updateStatus(eventId, status, errorMessage);
            } else {
                eventStore.updateStatus(eventId, status);
            }
        });
    }
    
    private CompletableFuture<Void> transformAndPublishAsyncWithTiming(Event event, Instant processingStart) {
        return CompletableFuture
            .supplyAsync(() -> messageTransformer.transform(event.getPayload()))
            .thenCompose(transformedMessage -> {
                Instant publishStart = Instant.now();
                latencyTracker.recordProcessingLatency(processingStart, publishStart);
                
                return publisherService.publishMessage(transformedMessage)
                    .thenAccept(result -> {
                        Instant publishEnd = Instant.now();
                        event.setProcessedAt(publishStart);
                        event.setPublishedAt(publishEnd);
                        event.calculateTimingMetrics();
                        
                        updateEventStatusWithTiming(event.getId(), EventStatus.SUCCESS, event);
                        latencyTracker.recordPublishingLatency(publishStart, publishEnd);
                        
                        if (event.getTotalLatencyMs() != null) {
                            latencyTracker.recordEndToEndLatency(event.getTotalLatencyMs(), event.getSendTimestampNs());
                        }
                    });
            })
            .exceptionally(throwable -> {
                logger.error("Failed to transform and publish event: {}", event.getId(), throwable);
                updateEventStatusWithTiming(event.getId(), EventStatus.FAILED, event, throwable.getMessage());
                return null;
            });
    }
}