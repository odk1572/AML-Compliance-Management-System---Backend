package com.app.aml.config;

import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchInfrastructureConfig extends DefaultBatchConfiguration {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    protected DataSource getDataSource() {
        return dataSource; // Forces Batch to use your TenantAwareDataSource
    }

    @Override
    protected PlatformTransactionManager getTransactionManager() {
        return transactionManager; // Forces Batch to use your custom transaction manager
    }
}