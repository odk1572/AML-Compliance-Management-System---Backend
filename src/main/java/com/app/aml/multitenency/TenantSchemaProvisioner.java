package com.app.aml.multitenency;

import com.app.aml.config.FlywayConfig;
import com.app.aml.exceptions.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@RequiredArgsConstructor
@Component
public class TenantSchemaProvisioner {

    private final DataSource dataSource;
    private final FlywayConfig flywayConfig;

    public void provision(String schemaName) {
        log.info("Starting provisioning process for schema: {}", schemaName);

        validateSchemaName(schemaName);

        createSchema(schemaName);

        flywayConfig.runTenantSchemaMigration(schemaName, dataSource);

        log.info("Successfully provisioned and migrated schema: {}", schemaName);
    }

    private void createSchema(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            String sql = "CREATE SCHEMA IF NOT EXISTS " + schemaName;
            log.debug("Executing DDL: {}", sql);

            statement.execute(sql);

        } catch (SQLException e) {
            log.error("Failed to create physical schema: {}", schemaName, e);
            throw new RuntimeException("Database error during schema provisioning", e);
        }
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }

        if (!schemaName.matches("^[a-z0-9_]{1,63}$")) {
            throw new BusinessRuleException(
                    "Invalid schema name format. Must be alphanumeric/underscores and under 63 chars."
            );
        }
    }
}