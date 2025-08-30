package com.orchestrator.postgres.config;

import com.orchestrator.postgres.store.PostgresEventStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PostgresAdapterAutoConfiguration {
    
    @Bean
    public PostgresEventStore postgresEventStore(JdbcTemplate jdbcTemplate) {
        return new PostgresEventStore(jdbcTemplate);
    }
}