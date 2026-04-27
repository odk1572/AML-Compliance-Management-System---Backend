package com.app.aml.feature.ingestion.batch.transaction;

import com.app.aml.enums.BatchStatus;
import com.app.aml.feature.ingestion.entity.TransactionBatch;
import com.app.aml.feature.ingestion.repository.TransactionBatchRepository;
import com.app.aml.feature.notification.service.interfaces.MailService;
import com.app.aml.multitenency.TenantContext;
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
public class TransactionJobCompletionListener implements JobExecutionListener {

    private final TransactionBatchRepository batchRepository;
    private final TransactionValidationSkipListener skipListener;
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

            processJobResults(UUID.fromString(batchIdStr), jobExecution);

        } finally {
            TenantContext.clear();
            if (filePath != null && !filePath.toLowerCase().startsWith("http")) {
                try {
                    Files.deleteIfExists(Path.of(filePath));
                    log.info("Deleted temporary file: {}", filePath);
                } catch (java.io.IOException e) {
                    log.error("Failed to delete local temp file: {}", filePath, e);
                }
            }
        }
    }

    @Transactional
    protected void processJobResults(UUID batchId, JobExecution jobExecution) {
        TransactionBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalStateException("Batch not found: " + batchId));

        Map<Integer, List<String>> errors = skipListener.getValidationErrors();

        if (!errors.isEmpty()) {
            try {
                String failureJson = objectMapper.writeValueAsString(errors);
                batch.setFailureDetails(failureJson);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse Transaction batch errors to JSON", e);
            }
            batch.setBatchStatus(BatchStatus.FAILED);
            batch.setProcessedAt(Instant.now());
            batchRepository.save(batch);

            jobExecution.setExitStatus(ExitStatus.FAILED);

            mailService.sendEmail(
                    "admin@yourdomain.com",
                    "Transaction Batch Failed: " + batch.getFileName(),
                    "Validation failed for " + errors.size() + " lines."
            );
        } else {
            batch.setBatchStatus(BatchStatus.PROCESSED);
            batch.setProcessedAt(Instant.now());
            batchRepository.save(batch);
            log.info("Transaction Batch {} successfully processed.", batch.getBatchReference());
        }
    }
}