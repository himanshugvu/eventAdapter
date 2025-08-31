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
    private final TransactionalEventService transactionalEventService;
    
    public EventConsumerService(
            EventStore eventStore,
            EventPublisherService publisherService,
            MessageTransformer messageTransformer,
            OrchestratorProperties properties,
            LatencyTracker latencyTracker,
            TransactionalEventService transactionalEventService) {
        this.eventStore = eventStore;
        this.publisherService = publisherService;
        this.messageTransformer = messageTransformer;
        this.properties = properties;
        this.latencyTracker = latencyTracker;
        this.transactionalEventService = transactionalEventService;
    }
    
    @KafkaListener(
        topics = "${orchestrator.consumer.topic}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEvents(
            List<ConsumerRecord<String, String>> records,
            Acknowledgment acknowledgment) {
        
        Instant batchReceivedAt = Instant.now();
        logger.info("CONSUMER BATCH: Received {} records at {}", records.size(), batchReceivedAt);
        
        try {
            switch (properties.database().strategy()) {
                case ATOMIC_OUTBOX -> processAtomicOutboxBatch(records, acknowledgment);
                case AUDIT_PERSIST -> processAuditPersistBatch(records, acknowledgment);
                case FAIL_SAFE -> processFailSafeBatch(records, acknowledgment);
            }
            
        } catch (Exception e) {
            logger.error("CONSUMER BATCH ERROR: Failed to process {} records: {}", records.size(), e.getMessage(), e);
            throw e;
        }
    }
    
    private Long extractSendTimestamp(ConsumerRecord<String, String> record) {
        try {
            if (record.headers() != null) {
                // Check for orchestrator send time first (from producer)
                var orchestratorSendTime = record.headers().lastHeader("orchestrator_send_time");
                if (orchestratorSendTime != null) {
                    return Long.parseLong(new String(orchestratorSendTime.value()));
                }
                // Fallback to original send timestamp
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
        Event event = new Event(eventId, record.value(), record.topic(), record.partition(), record.offset());
        event.setSendTimestampNs(sendTimestampNs);
        event.setReceivedAt(receivedAt);
        event.setMessageSendTime(sendTimestampNs != null ? sendTimestampNs / 1_000_000 : null);
        return event;
    }
    
    private void processAtomicOutboxBatch(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        Instant receivedAt = Instant.now();
        List<Event> events = records.stream()
            .map(record -> createEventWithTiming(record, extractSendTimestamp(record), receivedAt))
            .toList();
            
        try {
            transactionalEventService.bulkInsertAndCommit(events, acknowledgment);
            
            for (Event event : events) {
                CompletableFuture.runAsync(() -> {
                    try {
                        String transformedMessage = messageTransformer.transform(event.getSourcePayload());
                        
                        publisherService.publishMessage(transformedMessage)
                            .thenAccept(result -> {
                                event.setTransformedPayload(transformedMessage);
                                event.setDestinationTopic(result.getRecordMetadata().topic());
                                event.setDestinationPartition(result.getRecordMetadata().partition());
                                event.setDestinationOffset(result.getRecordMetadata().offset());
                                event.setMessageFinalSentTime(System.currentTimeMillis());
                                eventStore.updateStatus(event.getId(), EventStatus.SUCCESS);
                            })
                            .exceptionally(throwable -> {
                                eventStore.updateStatus(event.getId(), EventStatus.FAILED, throwable.getMessage());
                                return null;
                            });
                            
                    } catch (Exception e) {
                        eventStore.updateStatus(event.getId(), EventStatus.FAILED, e.getMessage());
                    }
                });
            }
            
        } catch (Exception e) {
            logger.error("ATOMIC_OUTBOX: Failed to insert batch of {} events: {}", events.size(), e.getMessage());
            throw e;
        }
    }
    
    
    private void processAuditPersistBatch(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        for (ConsumerRecord<String, String> record : records) {
            try {
                String transformedMessage = messageTransformer.transform(record.value());
                
                publisherService.publishMessage(transformedMessage)
                    .thenAccept(result -> {
                        acknowledgment.acknowledge();
                        
                        CompletableFuture.runAsync(() -> {
                            Event successEvent = createEventForAuditPersist(record, transformedMessage, EventStatus.SUCCESS, null);
                            successEvent.setDestinationTopic(result.getRecordMetadata().topic());
                            successEvent.setDestinationPartition(result.getRecordMetadata().partition());
                            successEvent.setDestinationOffset(result.getRecordMetadata().offset());
                            successEvent.setMessageFinalSentTime(System.currentTimeMillis());
                            eventStore.bulkInsert(List.of(successEvent));
                        });
                    })
                    .exceptionally(throwable -> {
                        Event failedEvent = createEventForAuditPersist(record, null, EventStatus.FAILED, throwable.getMessage());
                        eventStore.bulkInsert(List.of(failedEvent));
                        acknowledgment.acknowledge();
                        return null;
                    });
                    
            } catch (Exception e) {
                Event failedEvent = createEventForAuditPersist(record, null, EventStatus.FAILED, e.getMessage());
                eventStore.bulkInsert(List.of(failedEvent));
                acknowledgment.acknowledge();
            }
        }
    }
    
    private Event createEventForAuditPersist(ConsumerRecord<String, String> record, String transformedPayload, EventStatus status, String errorMessage) {
        Event event = createEventWithTiming(record, extractSendTimestamp(record), Instant.now());
        event.setTransformedPayload(transformedPayload);
        event.setStatus(status);
        event.setErrorMessage(errorMessage);
        return event;
    }
    
    private void processFailSafeBatch(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        acknowledgment.acknowledge();
        
        for (ConsumerRecord<String, String> record : records) {
            try {
                String transformedMessage = messageTransformer.transform(record.value());
                
                publisherService.publishMessage(transformedMessage)
                    .exceptionally(throwable -> {
                        Event failedEvent = new Event(UUID.randomUUID().toString(), record.value(), 
                                                    record.topic(), record.partition(), record.offset());
                        failedEvent.setStatus(EventStatus.FAILED);
                        failedEvent.setErrorMessage(throwable.getMessage());
                        eventStore.bulkInsert(List.of(failedEvent));
                        logger.error("FAIL_SAFE: Logged failed event to dead letter: {}", failedEvent.getId());
                        return null;
                    });
                    
            } catch (Exception e) {
                Event failedEvent = new Event(UUID.randomUUID().toString(), record.value(), 
                                            record.topic(), record.partition(), record.offset());
                failedEvent.setStatus(EventStatus.FAILED);
                failedEvent.setErrorMessage(e.getMessage());
                eventStore.bulkInsert(List.of(failedEvent));
                logger.error("FAIL_SAFE: Logged processing failure to dead letter: {}", failedEvent.getId());
            }
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
            .supplyAsync(() -> messageTransformer.transform(event.getSourcePayload()))
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