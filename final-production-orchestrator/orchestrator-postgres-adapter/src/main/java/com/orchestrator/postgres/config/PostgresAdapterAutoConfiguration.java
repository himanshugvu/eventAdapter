package com.orchestrator.postgres.config;

import com.orchestrator.postgres.store.PostgresEventStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Auto-configuration for PostgreSQL-specific EventStore implementation
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
public class PostgresAdapterAutoConfiguration {
    
    @Bean
    @ConditionalOnClass(JdbcTemplate.class)
    public PostgresEventStore postgresEventStore(JdbcTemplate jdbcTemplate) {
        return new PostgresEventStore(jdbcTemplate);
    }
}