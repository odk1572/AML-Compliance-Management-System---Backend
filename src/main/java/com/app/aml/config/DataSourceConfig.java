package com.app.aml.config;

import com.app.aml.multitenency.TenantAwareDataSource;
import com.app.aml.multitenency.TenantSchemaResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Lazy;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    /**
     * 1. Manually builds the Hikari pool using @Value strings.
     * This avoids the need for DataSourceProperties entirely.
     */
    @Bean
    public DataSource defaultHikariDataSource() {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    /**
     * 2. Wraps the manual pool in our Multi-Tenant routing logic.
     */
    @Bean
    @Primary
    public DataSource tenantAwareDataSource(
            DataSource defaultHikariDataSource,
            @Lazy TenantSchemaResolver resolver) {

        return new TenantAwareDataSource(defaultHikariDataSource, resolver);
    }
}