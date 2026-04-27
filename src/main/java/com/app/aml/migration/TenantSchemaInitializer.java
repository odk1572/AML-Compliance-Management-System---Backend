package com.app.aml.migration;

import com.app.aml.config.FlywayConfig; // Import your new config
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
@DependsOn("commonSchemaFlyway")
public class TenantSchemaInitializer {

    private final DataSource dataSource;
    private final FlywayConfig flywayConfig;

    @PostConstruct
    public void migrateTenantSchemas() {
        log.info("Starting Startup Tenant Migrations...");
        List<String> activeSchemas = getActiveTenantSchemas();

        for (String schemaName : activeSchemas) {
            flywayConfig.runTenantSchemaMigration(schemaName, dataSource);
        }
    }

    private List<String> getActiveTenantSchemas() {
        List<String> schemas = new ArrayList<>();
        String sql = "SELECT schema_name FROM common_schema.tenants WHERE status = 'ACTIVE'";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                schemas.add(rs.getString("schema_name"));
            }
        } catch (Exception e) {
            log.error("Could not fetch tenants", e);
        }
        return schemas;
    }
}