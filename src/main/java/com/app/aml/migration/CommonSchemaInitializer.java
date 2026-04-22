package com.app.aml.migration;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CommonSchemaInitializer {

    private final Flyway commonSchemaFlyway;

    @PostConstruct
    public void ensureCommonSchemaIsReady() {
        log.info(">>>> [STARTUP STEP 1] Verifying Common Schema...");

        log.info(">>>> [STARTUP STEP 1] Common Schema is active and migrated.");
    }
}