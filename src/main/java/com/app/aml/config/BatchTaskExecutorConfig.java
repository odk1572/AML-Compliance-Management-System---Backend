package com.app.aml.config;

import com.app.aml.multitenency.TenantContextTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BatchTaskExecutorConfig {

    @Bean(name = "batchTaskExecutor")
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(10); // Minimum threads always kept alive
        executor.setMaxPoolSize(20);  // Max threads spun up under heavy load
        executor.setQueueCapacity(200); // How many chunks wait in line before maxing out threads
        executor.setTaskDecorator(new TenantContextTaskDecorator());
        executor.setThreadNamePrefix("batch-worker-");


        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}