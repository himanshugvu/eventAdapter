package com.orchestrator.db.mongo;

import com.orchestrator.core.store.EventStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@AutoConfiguration
@ConditionalOnClass(MongoTemplate.class)
@EnableMongoRepositories(basePackages = "com.orchestrator.db.mongo")
public class MongoEventStoreAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(EventStore.class)
    public EventStore mongoEventStore(MongoEventRepository repository, MongoTemplate mongoTemplate) {
        return new MongoEventStore(repository, mongoTemplate);
    }
}