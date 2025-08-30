package com.orchestrator.core.config;

import com.orchestrator.core.controller.MetricsController;
import com.orchestrator.core.metrics.LatencyTracker;
import com.orchestrator.core.service.EventConsumerService;
import com.orchestrator.core.service.EventPublisherService;
import com.orchestrator.core.store.EventStore;
import com.orchestrator.core.transformer.DefaultMessageTransformer;
import com.orchestrator.core.transformer.MessageTransformer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration for the Orchestrator Core Adapter
 * Provides all core services and configurations needed for event orchestration
 */
@AutoConfiguration
@EnableConfigurationProperties(OrchestratorProperties.class)
@EnableRetry
@EnableScheduling
@Import({KafkaConfig.class})
public class OrchestratorCoreAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MessageTransformer defaultMessageTransformer() {
        return new DefaultMessageTransformer();
    }
    
    @Bean
    public LatencyTracker latencyTracker(MeterRegistry meterRegistry) {
        return new LatencyTracker(meterRegistry);
    }
    
    @Bean
    public EventPublisherService eventPublisherService(
            KafkaTemplate<String, String> kafkaTemplate,
            OrchestratorProperties properties,
            LatencyTracker latencyTracker) {
        return new EventPublisherService(kafkaTemplate, properties, latencyTracker);
    }
    
    @Bean
    public EventConsumerService eventConsumerService(
            EventStore eventStore,
            EventPublisherService publisherService,
            MessageTransformer messageTransformer,
            OrchestratorProperties properties,
            LatencyTracker latencyTracker) {
        return new EventConsumerService(eventStore, publisherService, messageTransformer, properties, latencyTracker);
    }
    
    @Bean
    public MetricsController metricsController(
            LatencyTracker latencyTracker,
            EventStore eventStore) {
        return new MetricsController(latencyTracker, eventStore);
    }
}