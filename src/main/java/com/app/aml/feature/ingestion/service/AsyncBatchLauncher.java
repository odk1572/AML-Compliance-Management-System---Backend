package com.app.aml.feature.ingestion.service;


import com.app.aml.domain.enums.BatchFileType;
import com.app.aml.domain.enums.BatchStatus;
import com.app.aml.feature.ingestion.repository.TransactionBatchRepository;
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

    @Async("asyncExecutor") // Use the thread pool you created earlier!
    public void triggerTargetedBatchJobAsync(UUID batchId, String filePath, BatchFileType fileType, String tenantId) {
        log.info("Starting background {} batch job for batchId: {}", fileType, batchId);
        try {
            JobParameters parameters = new JobParametersBuilder()
                    .addString("batchId", batchId.toString())
                    .addString("filePath", filePath)
                    .addString("tenantId", tenantId) // Pass the tenant ID down to the job!
                    .addLong("executionTime", System.currentTimeMillis())
                    .toJobParameters();

            Job targetJob = switch (fileType) {
                case CUSTOMER_PROFILE -> customerJob;
                case TRANSACTION -> transactionJob;
            };

            jobLauncher.run(targetJob, parameters);

        } catch (Exception e) {
            log.error("Spring Batch Job execution failed for batchId: {}", batchId, e);
            batchRepository.findById(batchId).ifPresent(batch -> {
                batch.setBatchStatus(BatchStatus.FAILED);
                batch.setFailureDetails("{\"system_error\": \"Job failed to launch.\"}");
                batchRepository.save(batch);
            });
        }
    }
}