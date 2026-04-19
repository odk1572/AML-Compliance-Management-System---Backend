package com.app.aml.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FlywayConfig {

    private final DataSource dataSource;

    @Bean(initMethod = "migrate")
    public Flyway commonSchemaFlyway() {
        log.info("Initializing Flyway for common_schema...");
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas("common_schema")
                .locations("classpath:db/migration/common")
                .baselineOnMigrate(true)
                .load();
    }
   public void runTenantSchemaMigration(String schemaName) {
        log.info("Running Flyway migration for tenant schema: {}", schemaName);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
        log.info("Migration successful for {}", schemaName);
    }
}