package com.app.aml.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.sql.DataSource;

@Configuration
@Slf4j

public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway commonSchemaFlyway(@Lazy DataSource dataSource) {
        log.info("Initializing Flyway for common_schema...");
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas("common_schema")
                .locations("classpath:db/migration/common")
                .baselineOnMigrate(true)
                .load();
    }

    public void runTenantSchemaMigration(String schemaName, DataSource dataSource) {
        log.info("Running Flyway migration for tenant schema: {}", schemaName);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .validateMigrationNaming(true)
                .load();

        flyway.repair();
        flyway.migrate();

        log.info("Migration successful for {}", schemaName);
    }
}