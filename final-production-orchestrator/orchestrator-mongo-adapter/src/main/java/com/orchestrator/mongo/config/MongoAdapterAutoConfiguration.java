package com.orchestrator.mongo.config;

import com.orchestrator.mongo.store.MongoEventStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoAdapterAutoConfiguration {
    
    @Bean
    public MongoEventStore mongoEventStore(MongoTemplate mongoTemplate) {
        return new MongoEventStore(mongoTemplate);
    }
}