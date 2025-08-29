package com.orchestrator.core.config;

// import com.orchestrator.core.health.OrchestratorHealthIndicator;
import com.orchestrator.core.metrics.LatencyTracker;
import com.orchestrator.core.metrics.OrchestratorMetrics;
import com.orchestrator.core.service.EventConsumerService;
import com.orchestrator.core.service.EventMaintenanceService;
import com.orchestrator.core.service.EventPublisherService;
import com.orchestrator.core.store.EventStore;
import com.orchestrator.core.transformer.DefaultMessageTransformer;
import com.orchestrator.core.transformer.MessageTransformer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableConfigurationProperties(OrchestratorProperties.class)
@EnableRetry
@EnableScheduling
@Import({KafkaConfig.class})
public class OrchestratorAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MessageTransformer defaultMessageTransformer() {
        return new DefaultMessageTransformer();
    }
    
    @Bean
    @ConditionalOnProperty(value = "orchestrator.monitoring.enable-metrics", havingValue = "true", matchIfMissing = true)
    public OrchestratorMetrics orchestratorMetrics(
            MeterRegistry meterRegistry,
            OrchestratorProperties properties,
            EventStore eventStore) {
        return new OrchestratorMetrics(meterRegistry, properties, eventStore);
    }
    
    @Bean
    public LatencyTracker latencyTracker(MeterRegistry meterRegistry) {
        return new LatencyTracker(meterRegistry);
    }
    
    @Bean
    public EventPublisherService eventPublisherService(
            KafkaTemplate<String, String> kafkaTemplate,
            OrchestratorProperties properties,
            OrchestratorMetrics metrics) {
        return new EventPublisherService(kafkaTemplate, properties, metrics);
    }
    
    @Bean
    public EventConsumerService eventConsumerService(
            EventStore eventStore,
            EventPublisherService publisherService,
            MessageTransformer messageTransformer,
            OrchestratorProperties properties,
            OrchestratorMetrics metrics,
            LatencyTracker latencyTracker) {
        return new EventConsumerService(eventStore, publisherService, messageTransformer, properties, metrics, latencyTracker);
    }
    
    // Health indicator temporarily disabled for Maven compatibility
    // @Bean
    // @ConditionalOnProperty(value = "orchestrator.monitoring.enable-health-checks", havingValue = "true", matchIfMissing = true)
    // public OrchestratorHealthIndicator orchestratorHealthIndicator(
    //         EventStore eventStore,
    //         KafkaTemplate<String, String> kafkaTemplate) {
    //     return new OrchestratorHealthIndicator(eventStore, kafkaTemplate);
    // }
    
    @Bean
    public EventMaintenanceService eventMaintenanceService(
            EventStore eventStore,
            OrchestratorProperties properties) {
        return new EventMaintenanceService(eventStore, properties);
    }
}