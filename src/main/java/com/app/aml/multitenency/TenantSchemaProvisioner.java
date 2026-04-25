package com.app.aml.multitenency;

import com.app.aml.config.FlywayConfig;
import com.app.aml.domain.exceptions.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class responsible for physically creating a new PostgreSQL schema
 * and triggering the Flyway migrations to populate it.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class TenantSchemaProvisioner {

    private final DataSource dataSource;
    private final FlywayConfig flywayConfig;

    /**
     * Provisions a physical database schema for a new tenant and runs migrations.
     *
     * @param schemaName The target schema name (e.g., "tenant_bank001_schema")
     */
    public void provision(String schemaName) {
        log.info("Starting provisioning process for schema: {}", schemaName);

        // 1. Strict Validation to prevent SQL Injection
        validateSchemaName(schemaName);

        // 2. Physically create the schema in PostgreSQL
        createSchema(schemaName);

        // 3. Trigger Flyway to build the tables inside the new schema
        flywayConfig.runTenantSchemaMigration(schemaName, dataSource);

        log.info("Successfully provisioned and migrated schema: {}", schemaName);
    }

    /**
     * Executes the CREATE SCHEMA SQL command.
     */
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

    /**
     * Validates that the schema name contains only safe characters.
     * This is critical because we are concatenating the string directly into a DDL statement.
     */
    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }

        // Only allow lowercase letters, numbers, and underscores. Max 63 characters.
        if (!schemaName.matches("^[a-z0-9_]{1,63}$")) {
            throw new BusinessRuleException(
                    "Invalid schema name format. Must be alphanumeric/underscores and under 63 chars."
            );
        }
    }
}