package com.app.aml.config;

import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
        return dataSource;
    }

    @Override
    protected PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    protected String getTablePrefix() {
        return "common_schema.BATCH_";
    }

    @Bean
    @Override
    public JobLauncher jobLauncher() {
        try {
            ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
            taskExecutor.setCorePoolSize(5);
            taskExecutor.setMaxPoolSize(10);
            taskExecutor.setQueueCapacity(20);
            taskExecutor.setThreadNamePrefix("Batch-Worker-");
            taskExecutor.initialize();

            TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
            jobLauncher.setJobRepository(jobRepository());
            jobLauncher.setTaskExecutor(taskExecutor);
            jobLauncher.afterPropertiesSet();

            return jobLauncher;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize async JobLauncher", e);
        }
    }
}