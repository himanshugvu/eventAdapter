package com.orchestrator.db.postgres;

import com.orchestrator.core.store.EventStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.persistence.EntityManager;

@AutoConfiguration
@ConditionalOnClass({EntityManager.class, JdbcTemplate.class})
@EnableJpaRepositories(basePackages = "com.orchestrator.db.postgres")
@EntityScan("com.orchestrator.db.postgres")
public class PostgresEventStoreAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(EventStore.class)
    public EventStore postgresEventStore(PostgresEventRepository repository, JdbcTemplate jdbcTemplate) {
        return new PostgresEventStore(repository, jdbcTemplate);
    }
}