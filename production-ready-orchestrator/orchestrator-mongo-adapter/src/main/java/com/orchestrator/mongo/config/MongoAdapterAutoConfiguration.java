package com.orchestrator.mongo.config;

import com.orchestrator.mongo.store.MongoEventStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Auto-configuration for MongoDB-specific EventStore implementation
 */
@AutoConfiguration(after = MongoDataAutoConfiguration.class)
public class MongoAdapterAutoConfiguration {
    
    @Bean
    @ConditionalOnClass(MongoTemplate.class)
    public MongoEventStore mongoEventStore(MongoTemplate mongoTemplate) {
        return new MongoEventStore(mongoTemplate);
    }
}