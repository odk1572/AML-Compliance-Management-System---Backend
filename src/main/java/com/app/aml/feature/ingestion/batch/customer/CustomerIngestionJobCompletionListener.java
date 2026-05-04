package com.app.aml.feature.ingestion.batch.customer;

import com.app.aml.enums.BatchStatus;
import com.app.aml.feature.ingestion.entity.TransactionBatch;
import com.app.aml.feature.ingestion.repository.TransactionBatchRepository;
import com.app.aml.feature.notification.service.interfaces.MailService;
import com.app.aml.multitenency.TenantContext;
import com.app.aml.utils.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerIngestionJobCompletionListener implements JobExecutionListener {

    private final TransactionBatchRepository batchRepository;
    private final CustomerValidationSkipListener skipListener;
    private final ObjectMapper objectMapper;
    private final MailService mailService;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        skipListener.clearErrors();
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String batchIdStr = jobExecution.getJobParameters().getString("batchId");
        String filePath = jobExecution.getJobParameters().getString("filePath");
        String tenantId = jobExecution.getJobParameters().getString("tenantId");
        String schemaName = jobExecution.getJobParameters().getString("schemaName");

        if (batchIdStr == null) return;

        try {
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
            if (schemaName != null) {
                TenantContext.setSchemaName(schemaName);
            }

            updateBatchStatus(UUID.fromString(batchIdStr), jobExecution);

        } finally {
            TenantContext.clear();
            cleanupBatchFile(filePath);
        }
    }

    @Transactional
    protected void updateBatchStatus(UUID batchId, JobExecution jobExecution) {
        TransactionBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalStateException("Batch not found: " + batchId));

        Map<Integer, List<String>> errors = skipListener.getValidationErrors();

        if (!errors.isEmpty()) {
            try {
                batch.setFailureDetails(objectMapper.writeValueAsString(errors));
            } catch (JsonProcessingException e) {
                log.error("Failed to parse errors to JSON", e);
            }
            batch.setBatchStatus(BatchStatus.FAILED);
            batch.setProcessedAt(Instant.now());
            batchRepository.save(batch);

            jobExecution.setExitStatus(ExitStatus.FAILED);

            mailService.sendEmail(
                    SecurityUtils.getCurrentUserEmail(),
                    "Customer Batch Failed: " + batch.getFileName(),
                    "Validation errors found in customer ingestion. Please check the dashboard."
            );
        } else {
            batch.setBatchStatus(BatchStatus.PROCESSED);
            batch.setProcessedAt(Instant.now());
            batchRepository.save(batch);
            log.info("Customer Batch {} processed successfully.", batch.getBatchReference());
        }
    }

    private void cleanupBatchFile(String filePath) {
        if (filePath != null) {
            if (filePath.toLowerCase().startsWith("http")) {
                log.info("Batch source is remote. Skipping local file cleanup.");
            } else {
                try {
                    Files.deleteIfExists(Path.of(filePath));
                    log.info("Successfully deleted temporary customer batch file: {}", filePath);
                } catch (Exception e) {
                    log.error("Failed to delete local temp file: {}", filePath, e);
                }
            }
        }
    }
}