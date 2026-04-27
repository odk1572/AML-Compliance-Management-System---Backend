package com.app.aml.multitenency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TenantAwareDataSource extends AbstractRoutingDataSource {

    private final TenantSchemaResolver schemaResolver;

    public TenantAwareDataSource(DataSource defaultDataSource,   @Lazy TenantSchemaResolver schemaResolver) {
        this.schemaResolver = schemaResolver;
         Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("default", defaultDataSource);

        super.setTargetDataSources(targetDataSources);
        super.setDefaultTargetDataSource(defaultDataSource);

        super.afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getTenantId();
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();

        setSchemaPath(connection);

        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        setSchemaPath(connection);
        return connection;
    }


    private void setSchemaPath(Connection connection) throws SQLException {
        String tenantId = (String) determineCurrentLookupKey();

        String schemaName = schemaResolver.resolveSchema(tenantId);

        try (Statement sql = connection.createStatement()) {
            sql.execute("SET search_path TO " + schemaName + ", public");
            log.trace("Connection routed to schema: {}", schemaName);
        }
    }

}