package com.app.aml.feature.ingestion.service;

import com.app.aml.domain.enums.BatchFileType;
import com.app.aml.domain.enums.BatchStatus;
import com.app.aml.feature.ingestion.repository.TransactionBatchRepository;
import com.app.aml.multitenency.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class AsyncBatchLauncher {

    private final JobLauncher jobLauncher;
    private final Job customerJob;
    private final Job transactionJob;
    private final TransactionBatchRepository batchRepository;

    @Autowired
    public AsyncBatchLauncher(
            JobLauncher jobLauncher,
            @Qualifier("customerProfileIngestionJob") Job customerJob,
            @Qualifier("transactionIngestionJob") Job transactionJob,
            TransactionBatchRepository batchRepository) {
        this.jobLauncher = jobLauncher;
        this.customerJob = customerJob;
        this.transactionJob = transactionJob;
        this.batchRepository = batchRepository;
    }

    @Async("asyncExecutor")
    // 1. FIXED: Added 'schemaName' as an input parameter
    public void triggerTargetedBatchJobAsync(UUID batchId, String filePath, BatchFileType fileType, String tenantId, String schemaName) {

        // 2. REMOVED: TenantContext.getSchemaName() call from inside the thread
        log.info("Starting background {} batch job for batchId: {} in schema: {}", fileType, batchId, schemaName);

        try {
            // 3. ADDED: A null check just to be safe before building parameters
            if (schemaName == null) {
                log.error("Schema name is null for batchId: {}. Batch cannot proceed.", batchId);
                return;
            }

            JobParameters parameters = new JobParametersBuilder()
                    .addString("batchId", batchId.toString())
                    .addString("filePath", filePath)
                    .addString("tenantId", tenantId)
                    .addString("schemaName", schemaName)
                    .addLong("executionTime", System.currentTimeMillis())
                    .toJobParameters();

            Job targetJob = switch (fileType) {
                case CUSTOMER_PROFILE -> customerJob;
                case TRANSACTION -> transactionJob;
            };

            jobLauncher.run(targetJob, parameters);

        } catch (Exception e) {
            log.error("Spring Batch Job execution failed for batchId: {}", batchId, e);

            try {
                TenantContext.setTenantId(tenantId);
                TenantContext.setSchemaName(schemaName);

                batchRepository.findById(batchId).ifPresent(batch -> {
                    batch.setBatchStatus(BatchStatus.FAILED);
                    batch.setFailureDetails("{\"system_error\": \"Job failed to launch: " + e.getMessage() + "\"}");
                    batchRepository.save(batch);
                });
            } finally {
                TenantContext.clear();
            }
        }
    }
}