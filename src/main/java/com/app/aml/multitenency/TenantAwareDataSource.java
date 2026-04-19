package com.app.aml.multitenency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom DataSource that intercepts database connections and routes them
 * to the correct PostgreSQL schema based on the current thread's TenantContext.
 */
@Slf4j
public class TenantAwareDataSource extends AbstractRoutingDataSource {

    private final TenantSchemaResolver schemaResolver;

    public TenantAwareDataSource(DataSource defaultDataSource, TenantSchemaResolver schemaResolver) {
        this.schemaResolver = schemaResolver;

        // In a Schema-per-Tenant model, we use a single Hikari connection pool.
        // We set the main database connection as the default and only target.
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("default", defaultDataSource);

        super.setTargetDataSources(targetDataSources);
        super.setDefaultTargetDataSource(defaultDataSource);

        // This is required by Spring to initialize the DataSource map
        super.afterPropertiesSet();
    }

    /**
     * Spring calls this to determine which routing key to use.
     * We supply the tenant ID from the thread-local storage.
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getTenantId();
    }

    /**
     * Intercept the standard connection retrieval to inject the schema switch.
     */
    @Override
    public Connection getConnection() throws SQLException {
        // 1. Grab a connection from the shared Hikari pool
        Connection connection = super.getConnection();

        // 2. Execute the schema switch dynamically
        setSchemaPath(connection);

        return connection;
    }

    /**
     * Intercept the overloaded connection retrieval.
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        setSchemaPath(connection);
        return connection;
    }

    /**
     * The core isolation logic. Resolves the schema name and executes SET search_path.
     */
    private void setSchemaPath(Connection connection) throws SQLException {
        String tenantId = (String) determineCurrentLookupKey();

        // Use our cached resolver to get the actual Postgres schema name
        String schemaName = schemaResolver.resolveSchema(tenantId);

        try (Statement sql = connection.createStatement()) {
            // Force the connection into the isolated bank schema.
            // Appending 'public' ensures extensions like uuid-ossp or citext still work.
            sql.execute("SET search_path TO " + schemaName + ", public");
            log.trace("Connection routed to schema: {}", schemaName);
        }
    }
}