package com.wificaptive.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);

    @Bean
    public FlywayMigrationStrategy repairAndMigrateStrategy() {
        return flyway -> {
            try {
                log.info("[Flyway Auto-Repair] Checking and repairing schema history...");
                flyway.repair();
            } catch (Exception e) {
                log.warn("[Flyway Auto-Repair] Repair warning: {}", e.getMessage());
            }
            log.info("[Flyway Migration] Executing pending database migrations...");
            flyway.migrate();
            log.info("[Flyway Migration] All database migrations completed successfully!");
        };
    }
}
