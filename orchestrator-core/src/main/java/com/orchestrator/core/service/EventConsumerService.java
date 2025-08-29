package com.orchestrator.core.service;

import com.orchestrator.core.config.OrchestratorProperties;
import com.orchestrator.core.metrics.OrchestratorMetrics;
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
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class EventConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventConsumerService.class);
    
    private final EventStore eventStore;
    private final EventPublisherService publisherService;
    private final MessageTransformer messageTransformer;
    private final OrchestratorProperties properties;
    private final OrchestratorMetrics metrics;
    private final LatencyTracker latencyTracker;
    
    public EventConsumerService(
            EventStore eventStore,
            EventPublisherService publisherService,
            MessageTransformer messageTransformer,
            OrchestratorProperties properties,
            OrchestratorMetrics metrics,
            LatencyTracker latencyTracker) {
        this.eventStore = eventStore;
        this.publisherService = publisherService;
        this.messageTransformer = messageTransformer;
        this.properties = properties;
        this.metrics = metrics;
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
        
        // Extract timing headers
        Long sendTimestampNs = extractSendTimestamp(record);
        String messageId = extractMessageId(record);
        String source = extractSource(record);
        
        logger.info("CONSUMER RECEIVED: messageId={}, source={}, topic={}, partition={}, offset={}, receivedAt={}", 
                   messageId, source, record.topic(), record.partition(), record.offset(), receivedAt);
        
        // Log consumer latency
        if (sendTimestampNs != null && sendTimestampNs > 0) {
            latencyTracker.recordConsumerLatency(sendTimestampNs, receivedAt);
        }
        
        metrics.incrementEventsReceived(1);
        
        try {
            Event event = createEventWithTiming(record, sendTimestampNs, receivedAt);
            
            switch (properties.database().strategy()) {
                case OUTBOX -> processOutboxModeWithTiming(event);
                case RELIABLE -> processReliableModeWithTiming(event);
                case LIGHTWEIGHT -> processLightweightModeWithTiming(event);
            }
            
            acknowledgment.acknowledge();
            metrics.incrementEventsProcessed(1);
            
        } catch (Exception e) {
            logger.error("CONSUMER ERROR: Failed to process messageId={} from topic={}: {}", 
                        messageId, record.topic(), e.getMessage(), e);
            metrics.incrementProcessingErrors();
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
    
    private void processOutboxMode(List<ConsumerRecord<String, String>> records) {
        List<Event> events = records.stream()
            .map(this::createEvent)
            .collect(Collectors.toList());
        
        eventStore.bulkInsert(events);
        
        List<CompletableFuture<Void>> futures = events.stream()
            .map(this::transformAndPublishAsync)
            .collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .whenComplete((result, throwable) -> {
                if (throwable == null) {
                    logger.debug("Successfully processed batch of {} events in OUTBOX mode", events.size());
                }
            });
    }
    
    private void processReliableMode(List<ConsumerRecord<String, String>> records) {
        for (ConsumerRecord<String, String> record : records) {
            Event event = createEvent(record);
            
            try {
                eventStore.bulkInsert(List.of(event));
                
                String transformedMessage = messageTransformer.transform(record.value());
                publisherService.publishMessage(transformedMessage)
                    .thenAccept(result -> updateEventStatusAsync(event.getId(), EventStatus.SUCCESS))
                    .exceptionally(throwable -> {
                        logger.error("Failed to publish event: {}", event.getId(), throwable);
                        updateEventStatusAsync(event.getId(), EventStatus.FAILED, throwable.getMessage());
                        return null;
                    });
                    
            } catch (Exception e) {
                logger.error("Failed to process event in RELIABLE mode: {}", event.getId(), e);
                updateEventStatusAsync(event.getId(), EventStatus.FAILED, e.getMessage());
            }
        }
    }
    
    private void processLightweightMode(List<ConsumerRecord<String, String>> records) {
        for (ConsumerRecord<String, String> record : records) {
            try {
                String transformedMessage = messageTransformer.transform(record.value());
                publisherService.publishMessage(transformedMessage)
                    .exceptionally(throwable -> {
                        Event failedEvent = createEvent(record);
                        failedEvent.setStatus(EventStatus.FAILED);
                        failedEvent.setErrorMessage(throwable.getMessage());
                        eventStore.bulkInsert(List.of(failedEvent));
                        logger.error("Failed to publish event, logged to DB: {}", failedEvent.getId(), throwable);
                        return null;
                    });
                    
            } catch (Exception e) {
                Event failedEvent = createEvent(record);
                failedEvent.setStatus(EventStatus.FAILED);
                failedEvent.setErrorMessage(e.getMessage());
                eventStore.bulkInsert(List.of(failedEvent));
                logger.error("Failed to process event in LIGHTWEIGHT mode: {}", failedEvent.getId(), e);
            }
        }
    }
    
    private Event createEvent(ConsumerRecord<String, String> record) {
        String eventId = UUID.randomUUID().toString();
        String topicPartition = record.topic() + "-" + record.partition();
        return new Event(eventId, record.value(), topicPartition, record.offset());
    }
    
    private Event createEventWithTiming(ConsumerRecord<String, String> record, Long sendTimestampNs, Instant receivedAt) {
        String eventId = UUID.randomUUID().toString();
        String topicPartition = record.topic() + "-" + record.partition();
        Event event = new Event(eventId, record.value(), topicPartition, record.offset());
        
        // Set timing information
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
    
    private CompletableFuture<Void> transformAndPublishAsync(Event event) {
        return CompletableFuture
            .supplyAsync(() -> messageTransformer.transform(event.getPayload()))
            .thenCompose(publisherService::publishMessage)
            .thenAccept(result -> updateEventStatusAsync(event.getId(), EventStatus.SUCCESS))
            .exceptionally(throwable -> {
                logger.error("Failed to transform and publish event: {}", event.getId(), throwable);
                updateEventStatusAsync(event.getId(), EventStatus.FAILED, throwable.getMessage());
                return null;
            });
    }
    
    private void updateEventStatusAsync(String eventId, EventStatus status) {
        CompletableFuture.runAsync(() -> eventStore.updateStatus(eventId, status));
    }
    
    private void updateEventStatusAsync(String eventId, EventStatus status, String errorMessage) {
        CompletableFuture.runAsync(() -> eventStore.updateStatus(eventId, status, errorMessage));
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